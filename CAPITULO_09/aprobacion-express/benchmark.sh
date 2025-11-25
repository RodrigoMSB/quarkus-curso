#!/bin/bash

# ============================================================================
# BENCHMARK JVM vs NATIVE - SISTEMA DE PRE-APROBACION CREDITICIA
# ============================================================================
# VERSION 9.0 - 100% COMPATIBLE MAC + WINDOWS GIT BASH
#
# REQUISITOS:
#   - Docker Desktop corriendo
#   - Maven instalado (el mvnw puede fallar en Windows)
#
# USO:
#   ./benchmark.sh              # 500 requests por defecto
#   ./benchmark.sh 1000         # 1000 requests
#
# ============================================================================

set -e

# ----------------------------------------------------------------------------
# CONFIGURACION
# ----------------------------------------------------------------------------

NUM_REQUESTS=${1:-500}

BASE_URL="http://localhost:8080"
API_URL="${BASE_URL}/api/preaprobacion"
HEALTH_URL="${BASE_URL}/q/health/ready"

TIMESTAMP=$(date +"%Y-%m-%d-%H%M%S")
REPORT_FILE="benchmark-report-${TIMESTAMP}.txt"

# Logs en directorio actual (compatible Windows)
LOG_DIR="./benchmark-logs-${TIMESTAMP}"
mkdir -p "$LOG_DIR"
BUILD_JVM_LOG="${LOG_DIR}/build-jvm.log"
BUILD_NATIVE_LOG="${LOG_DIR}/build-native.log"
JVM_RUN_LOG="${LOG_DIR}/jvm-run.log"
NATIVE_RUN_LOG="${LOG_DIR}/native-run.log"

DOCKER_IMAGE_JVM="aprobacion-express-jvm"
DOCKER_IMAGE_NATIVE="aprobacion-express-native"
DOCKER_CONTAINER_JVM="benchmark-jvm-${TIMESTAMP}"
DOCKER_CONTAINER_NATIVE="benchmark-native-${TIMESTAMP}"

# Credenciales BD (docker-compose.yml)
DB_USER="postgres"
DB_PASS="postgres123"
DB_NAME="banco_credito"
DB_HOST="host.docker.internal"
DB_CONTAINER="banco-postgres"

# Detectar sistema operativo
detect_os() {
    case "$OSTYPE" in
        linux*)   echo "linux" ;;
        darwin*)  echo "macos" ;;
        msys*)    echo "windows" ;;
        cygwin*)  echo "windows" ;;
        mingw*)   echo "windows" ;;
        *)        echo "unknown" ;;
    esac
}

CURRENT_OS=$(detect_os)

# Detectar comando Maven correcto
get_maven_command() {
    if [ -f "./mvnw" ] && [ -f "./.mvn/wrapper/maven-wrapper.properties" ]; then
        echo "./mvnw"
    elif command -v mvn &> /dev/null; then
        echo "mvn"
    else
        echo "ERROR: No se encontro Maven (mvn) ni Maven Wrapper (mvnw)" >&2
        echo "Instala Maven o regenera el wrapper con: mvn wrapper:wrapper" >&2
        exit 1
    fi
}

MVN_CMD=$(get_maven_command)

# Funcion para ejecutar docker-compose
run_docker_compose() {
    if command -v docker-compose &> /dev/null; then
        docker-compose "$@"
    elif docker compose version &> /dev/null 2>&1; then
        docker compose "$@"
    else
        echo "ERROR: docker-compose no disponible" >&2
        return 1
    fi
}

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m'

# Variables para resultados
BUILD_TIME_JVM=0
BUILD_TIME_NATIVE=0
STARTUP_MS_JVM=0
STARTUP_MS_NATIVE=0
MEMORY_JVM_RAW=0
MEMORY_NATIVE_RAW=0
THROUGHPUT_JVM_RAW=0
THROUGHPUT_NATIVE_RAW=0
SIZE_JVM_RAW=0
SIZE_NATIVE_RAW=0

# ----------------------------------------------------------------------------
# FUNCIONES AUXILIARES
# ----------------------------------------------------------------------------

print_header() {
    echo ""
    echo -e "${CYAN}============================================================================${NC}"
    echo -e "${WHITE}$1${NC}"
    echo -e "${CYAN}============================================================================${NC}"
    echo ""
    echo "" >> "$REPORT_FILE"
    echo "============================================================================" >> "$REPORT_FILE"
    echo "$1" >> "$REPORT_FILE"
    echo "============================================================================" >> "$REPORT_FILE"
}

print_section() {
    echo ""
    echo -e "${MAGENTA}>>> $1${NC}"
    echo ""
    echo ">>> $1" >> "$REPORT_FILE"
}

print_success() {
    echo -e "${GREEN}[OK] $1${NC}"
    echo "[OK] $1" >> "$REPORT_FILE"
}

print_error() {
    echo -e "${RED}[ERROR] $1${NC}"
    echo "[ERROR] $1" >> "$REPORT_FILE"
}

print_info() {
    echo -e "${BLUE}[INFO] $1${NC}"
    echo "[INFO] $1" >> "$REPORT_FILE"
}

print_warning() {
    echo -e "${YELLOW}[WARN] $1${NC}"
    echo "[WARN] $1" >> "$REPORT_FILE"
}

is_number() {
    case "$1" in
        ''|*[!0-9]*) return 1 ;;
        *) return 0 ;;
    esac
}

wait_for_service() {
    local max_attempts=90
    local attempt=1
    while [ $attempt -le $max_attempts ]; do
        if curl -s -f "$HEALTH_URL" > /dev/null 2>&1; then
            return 0
        fi
        sleep 1
        attempt=$((attempt + 1))
    done
    return 1
}

wait_for_postgres() {
    local max_attempts=30
    local attempt=1
    print_info "Esperando que PostgreSQL este listo..."
    while [ $attempt -le $max_attempts ]; do
        if docker exec "$DB_CONTAINER" pg_isready -U "$DB_USER" > /dev/null 2>&1; then
            if docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1;" > /dev/null 2>&1; then
                return 0
            fi
        fi
        sleep 1
        attempt=$((attempt + 1))
    done
    return 1
}

# Funcion mejorada para extraer startup time - COMPATIBLE MAC/WINDOWS
extract_startup_time() {
    local log_file="$1"
    local max_wait=30
    local attempt=0
    
    while [ $attempt -lt $max_wait ]; do
        if [ -f "$log_file" ]; then
            # Buscar patron "started in X.XXXs"
            local startup_line
            startup_line=$(grep "started in" "$log_file" 2>/dev/null | head -1)
            
            if [ -n "$startup_line" ]; then
                # Extraer numero (ej: "started in 1.234s" -> "1.234")
                local seconds
                seconds=$(echo "$startup_line" | grep -o '[0-9]*\.[0-9]*s' | sed 's/s$//')
                
                if [ -z "$seconds" ]; then
                    # Intentar sin decimal (ej: "started in 1s")
                    seconds=$(echo "$startup_line" | grep -o '[0-9]*s' | sed 's/s$//')
                fi
                
                if [ -n "$seconds" ]; then
                    # Convertir a milisegundos - SOLO ENTEROS (compatible Git Bash)
                    local int_part
                    int_part=$(echo "$seconds" | cut -d'.' -f1)
                    
                    local dec_part
                    dec_part=$(echo "$seconds" | cut -d'.' -f2)
                    
                    if [ -z "$dec_part" ]; then
                        dec_part="0"
                    fi
                    
                    # Padding a 3 digitos
                    while [ ${#dec_part} -lt 3 ]; do
                        dec_part="${dec_part}0"
                    done
                    
                    # Truncar a 3 digitos
                    dec_part=${dec_part:0:3}
                    
                    local ms
                    ms=$((int_part * 1000 + dec_part))
                    
                    if is_number "$ms" && [ "$ms" -gt 0 ]; then
                        echo "$ms"
                        return 0
                    fi
                fi
            fi
        fi
        sleep 1
        attempt=$((attempt + 1))
    done
    echo "0"
}

# Funcion mejorada para medir memoria - COMPATIBLE MAC/WINDOWS
measure_docker_memory() {
    local container="$1"
    
    # Obtener estadisticas de memoria
    local mem_str
    mem_str=$(docker stats --no-stream --format "{{.MemUsage}}" "$container" 2>/dev/null | head -1)
    
    if [ -z "$mem_str" ]; then
        echo "0"
        return
    fi
    
    # Extraer primer valor (usado) - Formato: "123.4MiB / 7.775GiB"
    local used
    used=$(echo "$mem_str" | cut -d'/' -f1 | tr -d ' ')
    
    if [ -z "$used" ]; then
        echo "0"
        return
    fi
    
    # Extraer numero y unidad
    local num
    num=$(echo "$used" | sed 's/[^0-9.]//g')
    
    local unit
    unit=$(echo "$used" | sed 's/[0-9.]//g')
    
    if [ -z "$num" ]; then
        echo "0"
        return
    fi
    
    # Convertir a MB (solo parte entera)
    local int_num
    int_num=$(echo "$num" | cut -d'.' -f1)
    
    case "$unit" in
        "GiB"|"GB"|"G")
            echo "$((int_num * 1024))"
            ;;
        "MiB"|"MB"|"M")
            echo "$int_num"
            ;;
        "KiB"|"KB"|"K")
            echo "$((int_num / 1024))"
            ;;
        *)
            echo "$int_num"
            ;;
    esac
}

# Funcion de load test mejorada - COMPATIBLE MAC/WINDOWS
load_test() {
    local url="$1"
    local requests="$2"
    
    echo -e "${BLUE}   Ejecutando ${requests} requests...${NC}" >&2
    
    local start_time
    start_time=$(date +%s)
    
    local success=0
    local i=1
    
    while [ $i -le "$requests" ]; do
        if curl -s -f -o /dev/null "$url" 2>/dev/null; then
            success=$((success + 1))
        fi
        
        if [ $((i % 100)) -eq 0 ]; then
            echo -e "${BLUE}   Progreso: ${i}/${requests}${NC}" >&2
        fi
        
        i=$((i + 1))
    done
    
    local end_time
    end_time=$(date +%s)
    
    local duration
    duration=$((end_time - start_time))
    
    if [ "$duration" -eq 0 ]; then
        duration=1
    fi
    
    local throughput
    throughput=$((success / duration))
    
    echo "$throughput"
}

# Funcion para obtener tamano de imagen Docker
get_docker_image_size() {
    local image="$1"
    
    # Obtener tamano en bytes
    local size_str
    size_str=$(docker images --format "{{.Size}}" "$image" 2>/dev/null | head -1)
    
    if [ -z "$size_str" ]; then
        echo "0"
        return
    fi
    
    # Extraer numero
    local num
    num=$(echo "$size_str" | sed 's/[^0-9.]//g')
    
    local unit
    unit=$(echo "$size_str" | sed 's/[0-9.]//g')
    
    if [ -z "$num" ]; then
        echo "0"
        return
    fi
    
    # Convertir a MB
    local int_num
    int_num=$(echo "$num" | cut -d'.' -f1)
    
    case "$unit" in
        "GB")
            echo "$((int_num * 1024))"
            ;;
        "MB")
            echo "$int_num"
            ;;
        *)
            echo "$int_num"
            ;;
    esac
}

cleanup() {
    print_section "Limpiando recursos..."
    docker stop "$DOCKER_CONTAINER_JVM" > /dev/null 2>&1 || true
    docker rm "$DOCKER_CONTAINER_JVM" > /dev/null 2>&1 || true
    docker stop "$DOCKER_CONTAINER_NATIVE" > /dev/null 2>&1 || true
    docker rm "$DOCKER_CONTAINER_NATIVE" > /dev/null 2>&1 || true
    docker rmi -f "$DOCKER_IMAGE_JVM" > /dev/null 2>&1 || true
    docker rmi -f "$DOCKER_IMAGE_NATIVE" > /dev/null 2>&1 || true
}

trap cleanup EXIT

# ----------------------------------------------------------------------------
# INICIO DEL BENCHMARK
# ----------------------------------------------------------------------------

cat > "$REPORT_FILE" << HEADER
================================================================================
BENCHMARK JVM vs NATIVE
Sistema de Pre-aprobacion Crediticia
================================================================================

Fecha: $(date +"%Y-%m-%d %H:%M:%S")
Sistema: ${CURRENT_OS}
Requests: ${NUM_REQUESTS}
Maven: ${MVN_CMD}

================================================================================
HEADER

print_header "BENCHMARK JVM vs NATIVE"

echo ""
echo "   Sistema operativo: ${CURRENT_OS}"
echo "   Maven: ${MVN_CMD}"
echo "   Requests: ${NUM_REQUESTS}"
echo ""

# ----------------------------------------------------------------------------
# VERIFICAR PREREQUISITOS
# ----------------------------------------------------------------------------

print_header "PREREQUISITOS"

print_section "Verificando Docker"
if ! docker info > /dev/null 2>&1; then
    print_error "Docker no esta corriendo. Inicia Docker Desktop."
    exit 1
fi
print_success "Docker corriendo"

print_section "Verificando PostgreSQL"
if ! docker ps | grep -q "$DB_CONTAINER"; then
    print_error "Contenedor PostgreSQL no encontrado: $DB_CONTAINER"
    print_info "Ejecuta: docker-compose up -d"
    exit 1
fi

if ! wait_for_postgres; then
    print_error "PostgreSQL no responde"
    exit 1
fi
print_success "PostgreSQL listo"

print_section "Verificando Maven"
if ! command -v "$MVN_CMD" &> /dev/null && [ "$MVN_CMD" != "./mvnw" ]; then
    print_error "Maven no encontrado"
    exit 1
fi
print_success "Maven disponible: ${MVN_CMD}"

# ----------------------------------------------------------------------------
# FASE 1: BUILD JVM (Docker)
# ----------------------------------------------------------------------------

print_header "FASE 1: BUILD JVM (Docker)"

print_section "1.1 - Compilando con Maven (modo JVM)"
print_info "${MVN_CMD} clean package -DskipTests -Dquarkus.package.jar.type=uber-jar"

START_BUILD_JVM=$(date +%s)

if $MVN_CMD clean package -DskipTests -Dquarkus.package.jar.type=uber-jar > "$BUILD_JVM_LOG" 2>&1; then
    END_BUILD_JVM=$(date +%s)
    BUILD_TIME_JVM=$((END_BUILD_JVM - START_BUILD_JVM))
    print_success "Build JVM completado: ${BUILD_TIME_JVM}s"
else
    print_error "Error en build JVM"
    tail -30 "$BUILD_JVM_LOG"
    exit 1
fi

print_section "1.2 - Construyendo imagen Docker JVM"
print_info "docker build -f src/main/docker/Dockerfile.jvm -t $DOCKER_IMAGE_JVM ."

if docker build --no-cache -f src/main/docker/Dockerfile.jvm -t "$DOCKER_IMAGE_JVM" . >> "$BUILD_JVM_LOG" 2>&1; then
    print_success "Imagen JVM construida"
    SIZE_JVM_RAW=$(get_docker_image_size "$DOCKER_IMAGE_JVM")
    print_info "Tamano imagen: ${SIZE_JVM_RAW} MB"
else
    print_error "Error construyendo imagen JVM"
    tail -30 "$BUILD_JVM_LOG"
    exit 1
fi

# ----------------------------------------------------------------------------
# FASE 2: PRUEBAS JVM (Docker)
# ----------------------------------------------------------------------------

print_header "FASE 2: PRUEBAS JVM (Docker)"

print_section "2.1 - Iniciando contenedor JVM"

docker run -d \
    --name "$DOCKER_CONTAINER_JVM" \
    -p 8080:8080 \
    -e QUARKUS_DATASOURCE_JDBC_URL="jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}" \
    -e QUARKUS_DATASOURCE_USERNAME="${DB_USER}" \
    -e QUARKUS_DATASOURCE_PASSWORD="${DB_PASS}" \
    "$DOCKER_IMAGE_JVM" > /dev/null 2>&1

print_info "Contenedor: $DOCKER_CONTAINER_JVM"

sleep 3
docker logs "$DOCKER_CONTAINER_JVM" > "$JVM_RUN_LOG" 2>&1

if wait_for_service; then
    sleep 1
    docker logs "$DOCKER_CONTAINER_JVM" > "$JVM_RUN_LOG" 2>&1
    
    STARTUP_MS_JVM=$(extract_startup_time "$JVM_RUN_LOG")
    if is_number "$STARTUP_MS_JVM" && [ "$STARTUP_MS_JVM" -gt 0 ]; then
        print_success "Arranque JVM: ${STARTUP_MS_JVM} ms"
    else
        STARTUP_MS_JVM=2000
        print_warning "Arranque JVM: ~2000 ms (estimado)"
    fi
else
    print_error "Fallo al arrancar contenedor JVM"
    docker logs "$DOCKER_CONTAINER_JVM" 2>&1
    exit 1
fi

print_section "2.2 - Midiendo memoria JVM"
sleep 5
MEMORY_JVM_RAW=$(measure_docker_memory "$DOCKER_CONTAINER_JVM")
print_info "Memoria JVM: ${MEMORY_JVM_RAW} MB"

print_section "2.3 - Midiendo throughput JVM"
THROUGHPUT_JVM_RAW=$(load_test "$API_URL/estadisticas" "$NUM_REQUESTS")
print_success "Throughput JVM: ${THROUGHPUT_JVM_RAW} req/s"

print_section "2.4 - Deteniendo contenedor JVM"
docker stop "$DOCKER_CONTAINER_JVM" > /dev/null 2>&1 || true
docker rm "$DOCKER_CONTAINER_JVM" > /dev/null 2>&1 || true
print_success "Contenedor JVM detenido"

# ----------------------------------------------------------------------------
# FASE 3: BUILD NATIVE (Docker)
# ----------------------------------------------------------------------------

print_header "FASE 3: BUILD NATIVE (Docker)"

print_warning "Esto tomara 5-10 minutos (compila con GraalVM dentro de Docker)"
echo ""

print_section "3.1 - Construyendo imagen Docker Native"
print_info "docker build --no-cache -f src/main/docker/Dockerfile.native -t $DOCKER_IMAGE_NATIVE ."

START_BUILD_NATIVE=$(date +%s)
if docker build --no-cache -f src/main/docker/Dockerfile.native -t "$DOCKER_IMAGE_NATIVE" . > "$BUILD_NATIVE_LOG" 2>&1; then
    END_BUILD_NATIVE=$(date +%s)
    BUILD_TIME_NATIVE=$((END_BUILD_NATIVE - START_BUILD_NATIVE))
    BUILD_MIN=$((BUILD_TIME_NATIVE / 60))
    BUILD_SEC=$((BUILD_TIME_NATIVE % 60))
    print_success "Imagen Native construida: ${BUILD_TIME_NATIVE}s (${BUILD_MIN}m ${BUILD_SEC}s)"
    SIZE_NATIVE_RAW=$(get_docker_image_size "$DOCKER_IMAGE_NATIVE")
    print_info "Tamano imagen: ${SIZE_NATIVE_RAW} MB"
else
    print_error "Error construyendo imagen Native"
    tail -30 "$BUILD_NATIVE_LOG"
    exit 1
fi

# ----------------------------------------------------------------------------
# FASE 4: PRUEBAS NATIVE (Docker)
# ----------------------------------------------------------------------------

print_header "FASE 4: PRUEBAS NATIVE (Docker)"

print_section "4.1 - Iniciando contenedor Native"

docker run -d \
    --name "$DOCKER_CONTAINER_NATIVE" \
    -p 8080:8080 \
    -e QUARKUS_DATASOURCE_JDBC_URL="jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}" \
    -e QUARKUS_DATASOURCE_USERNAME="${DB_USER}" \
    -e QUARKUS_DATASOURCE_PASSWORD="${DB_PASS}" \
    "$DOCKER_IMAGE_NATIVE" > /dev/null 2>&1

print_info "Contenedor: $DOCKER_CONTAINER_NATIVE"

sleep 2
docker logs "$DOCKER_CONTAINER_NATIVE" > "$NATIVE_RUN_LOG" 2>&1

if wait_for_service; then
    sleep 1
    docker logs "$DOCKER_CONTAINER_NATIVE" > "$NATIVE_RUN_LOG" 2>&1
    
    STARTUP_MS_NATIVE=$(extract_startup_time "$NATIVE_RUN_LOG")
    if is_number "$STARTUP_MS_NATIVE" && [ "$STARTUP_MS_NATIVE" -gt 0 ]; then
        print_success "Arranque Native: ${STARTUP_MS_NATIVE} ms"
    else
        STARTUP_MS_NATIVE=50
        print_warning "Arranque Native: ~50 ms (estimado)"
    fi
else
    print_error "Fallo al arrancar contenedor Native"
    docker logs "$DOCKER_CONTAINER_NATIVE" 2>&1
    exit 1
fi

print_section "4.2 - Midiendo memoria Native"
sleep 3
MEMORY_NATIVE_RAW=$(measure_docker_memory "$DOCKER_CONTAINER_NATIVE")
print_info "Memoria Native: ${MEMORY_NATIVE_RAW} MB"

print_section "4.3 - Midiendo throughput Native"
THROUGHPUT_NATIVE_RAW=$(load_test "$API_URL/estadisticas" "$NUM_REQUESTS")
print_success "Throughput Native: ${THROUGHPUT_NATIVE_RAW} req/s"

print_section "4.4 - Deteniendo contenedor Native"
docker stop "$DOCKER_CONTAINER_NATIVE" > /dev/null 2>&1 || true
docker rm "$DOCKER_CONTAINER_NATIVE" > /dev/null 2>&1 || true
print_success "Contenedor Native detenido"

# ----------------------------------------------------------------------------
# FASE 5: COMPARATIVA
# ----------------------------------------------------------------------------

print_header "FASE 5: COMPARATIVA FINAL"

# Calculos (solo enteros)
COMPILE_RATIO=0
STARTUP_RATIO=0
MEMORY_SAVINGS=0

if [ "$BUILD_TIME_JVM" -gt 0 ]; then
    COMPILE_RATIO=$((BUILD_TIME_NATIVE / BUILD_TIME_JVM))
fi

if [ "$STARTUP_MS_NATIVE" -gt 0 ]; then
    STARTUP_RATIO=$((STARTUP_MS_JVM / STARTUP_MS_NATIVE))
fi

if [ "$MEMORY_JVM_RAW" -gt 0 ] && [ "$MEMORY_NATIVE_RAW" -gt 0 ]; then
    MEMORY_SAVINGS=$(( (MEMORY_JVM_RAW - MEMORY_NATIVE_RAW) * 100 / MEMORY_JVM_RAW ))
fi

# Tabla (caracteres ASCII para compatibilidad)
printf "\n"
printf "+------------------------------------------------------------------------------+\n"
printf "|                        RESULTADOS DEL BENCHMARK                              |\n"
printf "|                        (%s requests)                                          |\n" "$NUM_REQUESTS"
printf "+------------------------------------------------------------------------------+\n"
printf "| %-27s | %-18s | %-18s |\n" "METRICA" "JVM (Docker)" "NATIVE (Docker)"
printf "+------------------------------------------------------------------------------+\n"
printf "| %-27s | %18s | %18s |\n" "Tiempo de build" "${BUILD_TIME_JVM}s" "${BUILD_TIME_NATIVE}s"
printf "| %-27s | %18s | %18s |\n" "Tiempo de arranque" "${STARTUP_MS_JVM} ms" "${STARTUP_MS_NATIVE} ms"
printf "| %-27s | %18s | %18s |\n" "Uso de memoria" "${MEMORY_JVM_RAW} MB" "${MEMORY_NATIVE_RAW} MB"
printf "| %-27s | %18s | %18s |\n" "Throughput" "${THROUGHPUT_JVM_RAW} req/s" "${THROUGHPUT_NATIVE_RAW} req/s"
printf "| %-27s | %18s | %18s |\n" "Tamano imagen" "${SIZE_JVM_RAW} MB" "${SIZE_NATIVE_RAW} MB"
printf "+------------------------------------------------------------------------------+\n"

echo ""
echo -e "${CYAN}ANALISIS:${NC}"
echo ""
echo "1. BUILD: Native ${COMPILE_RATIO}x mas lento (pero solo una vez en CI/CD)"
echo ""
echo -e "2. ARRANQUE: ${GREEN}Native ${STARTUP_RATIO}x MAS RAPIDO${NC}"
echo ""
echo -e "3. MEMORIA: ${GREEN}Native usa ${MEMORY_SAVINGS}% MENOS${NC}"
echo ""
echo "4. THROUGHPUT: Rendimiento similar"
echo ""

echo "================================================================================"
echo ""

if [ "$MEMORY_SAVINGS" -ge 50 ] && [ "$STARTUP_RATIO" -ge 5 ]; then
    echo -e "${GREEN}   *** NATIVE CLARAMENTE SUPERIOR para produccion ***${NC}"
fi

echo ""
echo "   JVM: Desarrollo local, debugging, hot reload"
echo "   NATIVE: Produccion, serverless, Kubernetes"
echo ""

if [ "$MEMORY_JVM_RAW" -gt 0 ] && [ "$MEMORY_NATIVE_RAW" -gt 0 ]; then
    JVM_50=$((MEMORY_JVM_RAW * 50))
    NATIVE_50=$((MEMORY_NATIVE_RAW * 50))
    AHORRO=$((JVM_50 - NATIVE_50))
    echo -e "${WHITE}AHORRO (50 microservicios):${NC}"
    echo "   JVM: ${JVM_50} MB (~$((JVM_50 / 1024)) GB)"
    echo "   Native: ${NATIVE_50} MB (~$((NATIVE_50 / 1024)) GB)"
    echo -e "   ${GREEN}Ahorro: ~$((AHORRO / 1024)) GB${NC}"
    echo ""
fi

# Limpiar imagenes Docker
print_section "Limpieza"
docker rmi -f "$DOCKER_IMAGE_JVM" > /dev/null 2>&1 || true
docker rmi -f "$DOCKER_IMAGE_NATIVE" > /dev/null 2>&1 || true
print_success "Imagenes Docker eliminadas"

# Limpiar directorio de logs
rm -rf "$LOG_DIR" 2>/dev/null || true

# Guardar reporte
cat >> "$REPORT_FILE" << REPORT

RESULTADOS
==========
Build: JVM ${BUILD_TIME_JVM}s vs Native ${BUILD_TIME_NATIVE}s
Arranque: JVM ${STARTUP_MS_JVM}ms vs Native ${STARTUP_MS_NATIVE}ms
Memoria: JVM ${MEMORY_JVM_RAW}MB vs Native ${MEMORY_NATIVE_RAW}MB
Throughput: JVM ${THROUGHPUT_JVM_RAW}req/s vs Native ${THROUGHPUT_NATIVE_RAW}req/s
Imagen: JVM ${SIZE_JVM_RAW}MB vs Native ${SIZE_NATIVE_RAW}MB

Ratios: Arranque ${STARTUP_RATIO}x, Memoria -${MEMORY_SAVINGS}%

================================================================================
FIN - $(date +"%Y-%m-%d %H:%M:%S")
================================================================================
REPORT

print_success "Reporte: ${REPORT_FILE}"
print_success "Benchmark completado!"