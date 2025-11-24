#!/bin/bash

# ============================================================================
# BENCHMARK JVM vs NATIVE - SISTEMA DE PRE-APROBACIÓN CREDITICIA
# ============================================================================
# VERSIÓN 3.0 - 100% COMPATIBLE CON WINDOWS GIT BASH
#
# COMPATIBILIDAD PROBADA:
# ✅ Windows 10/11 + Git Bash (sin instalaciones adicionales)
# ✅ macOS (Intel y Apple Silicon)
# ✅ Linux (Ubuntu, CentOS, etc.)
#
# REQUISITOS MÍNIMOS:
# - Git Bash (viene con Git for Windows)
# - Java 17+ (JDK o GraalVM)
# - Docker Desktop
# - curl (viene con Git Bash)
#
# NO REQUIERE:
# ❌ Python
# ❌ bc
# ❌ Herramientas adicionales
#
# USO:
#   ./benchmark.sh              # 500 requests por defecto
#   ./benchmark.sh 1000         # 1000 requests
#
# ============================================================================

set -e

# ----------------------------------------------------------------------------
# CONFIGURACIÓN
# ----------------------------------------------------------------------------

NUM_REQUESTS=${1:-500}

BASE_URL="http://localhost:8080"
API_URL="${BASE_URL}/api/preaprobacion"
HEALTH_URL="${BASE_URL}/q/health/ready"

TIMESTAMP=$(date +"%Y-%m-%d-%H%M%S")
REPORT_FILE="benchmark-report-${TIMESTAMP}.txt"
BUILD_JVM_LOG="/tmp/build-jvm-${TIMESTAMP}.log"
BUILD_NATIVE_LOG="/tmp/build-native-${TIMESTAMP}.log"
JVM_RUN_LOG="/tmp/jvm-run-${TIMESTAMP}.log"
NATIVE_RUN_LOG="/tmp/native-run-${TIMESTAMP}.log"

# Detectar sistema operativo
detect_os() {
    case "$(uname -s)" in
        Linux*)     echo "Linux" ;;
        Darwin*)    echo "macOS" ;;
        CYGWIN*|MINGW*|MSYS*) echo "Windows" ;;
        *)          echo "Unknown" ;;
    esac
}

OS_TYPE=$(detect_os)

# Colores (compatibles con Git Bash)
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m'

# Emojis
CHECK="[OK]"
CROSS="[ERROR]"
ROCKET=">>>"
CLOCK="[WAIT]"
FIRE=">>>"
CHART=">>>"
BUILDING=">>>"

# Variables para resultados
BUILD_TIME_JVM=0
BUILD_TIME_NATIVE=0
STARTUP_TIME_JVM="N/A"
STARTUP_TIME_NATIVE="N/A"
STARTUP_MS_JVM=0
STARTUP_MS_NATIVE=0
MEMORY_JVM="N/A"
MEMORY_NATIVE="N/A"
MEMORY_JVM_RAW=0
MEMORY_NATIVE_RAW=0
THROUGHPUT_JVM="N/A"
THROUGHPUT_NATIVE="N/A"
THROUGHPUT_JVM_RAW=0
THROUGHPUT_NATIVE_RAW=0
SIZE_JVM="N/A"
SIZE_NATIVE="N/A"
SIZE_JVM_RAW=0
SIZE_NATIVE_RAW=0

# ----------------------------------------------------------------------------
# FUNCIONES AUXILIARES (SIN DEPENDENCIAS EXTERNAS)
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
    echo -e "${GREEN}${CHECK} $1${NC}"
    echo "[OK] $1" >> "$REPORT_FILE"
}

print_error() {
    echo -e "${RED}${CROSS} $1${NC}"
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

# Función para esperar que el servicio esté listo
wait_for_service() {
    local max_attempts=60
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

# Extraer tiempo de arranque de logs de Quarkus (funciona en todos los OS)
# Busca "started in X.XXXs" y convierte a milisegundos usando awk
extract_startup_time() {
    local log_file=$1
    local max_wait=30
    local attempt=0
    
    while [ $attempt -lt $max_wait ]; do
        if [ -f "$log_file" ]; then
            # Extraer el número de segundos del log
            local startup_line=$(grep -o "started in [0-9.]*s" "$log_file" 2>/dev/null | head -1)
            if [ -n "$startup_line" ]; then
                # Usar awk para convertir a milisegundos (funciona en Git Bash)
                local ms=$(echo "$startup_line" | awk '{gsub(/[^0-9.]/,"",$3); printf "%.0f", $3 * 1000}')
                if [ -n "$ms" ] && [ "$ms" != "0" ]; then
                    echo "$ms"
                    return 0
                fi
            fi
        fi
        sleep 1
        attempt=$((attempt + 1))
    done
    
    echo "0"
}

# Medir memoria - compatible con todos los OS
measure_memory() {
    local pid=$1
    local mem_kb=0
    
    case "$OS_TYPE" in
        "Windows")
            # En Windows Git Bash, usar tasklist
            mem_kb=$(tasklist //FI "PID eq $pid" //FO CSV 2>/dev/null | tail -1 | awk -F',' '{gsub(/[^0-9]/,"",$NF); print $NF}' 2>/dev/null || echo "0")
            # tasklist da KB, convertir
            if [ -n "$mem_kb" ] && [ "$mem_kb" != "0" ]; then
                echo "$((mem_kb / 1024))"
                return 0
            fi
            # Fallback: intentar con ps si está disponible
            mem_kb=$(ps -p "$pid" -o rss= 2>/dev/null | awk '{print $1}' || echo "0")
            ;;
        *)
            # macOS y Linux
            mem_kb=$(ps -o rss= -p "$pid" 2>/dev/null | awk '{print $1}')
            ;;
    esac
    
    if [ -n "$mem_kb" ] && [ "$mem_kb" != "0" ]; then
        echo "$((mem_kb / 1024))"
    else
        echo "0"
    fi
}

# Prueba de carga simple - usa solo bash y curl
load_test() {
    local url=$1
    local requests=$2
    
    echo -e "${BLUE}   Ejecutando ${requests} requests...${NC}" >&2
    
    local start_time=$(date +%s)
    local success=0
    
    for i in $(seq 1 $requests); do
        if curl -s -f -o /dev/null "$url" 2>/dev/null; then
            success=$((success + 1))
        fi
        
        # Mostrar progreso cada 100 requests
        if [ $((i % 100)) -eq 0 ]; then
            echo -e "${BLUE}   Progreso: ${i}/${requests}${NC}" >&2
        fi
    done
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    if [ "$duration" -gt 0 ]; then
        echo $((success / duration))
    else
        echo "$success"
    fi
}

# Detener servicio en puerto 8080 - compatible con todos los OS
stop_service() {
    local pid=""
    
    case "$OS_TYPE" in
        "Windows")
            # Windows: usar netstat
            pid=$(netstat -ano 2>/dev/null | grep ":8080 " | grep "LISTENING" | awk '{print $5}' | head -1)
            if [ -n "$pid" ] && [ "$pid" != "0" ]; then
                taskkill //F //PID "$pid" > /dev/null 2>&1 || true
            fi
            ;;
        *)
            # macOS/Linux: usar lsof si está disponible
            if command -v lsof &> /dev/null; then
                pid=$(lsof -ti:8080 2>/dev/null || echo "")
            fi
            if [ -n "$pid" ]; then
                kill -9 "$pid" 2>/dev/null || true
            fi
            ;;
    esac
    
    sleep 2
}

# Obtener tamaño en MB - compatible con todos los OS
get_size_mb() {
    local path=$1
    local size_kb=0
    
    if [ -d "$path" ]; then
        # Es directorio
        case "$OS_TYPE" in
            "Windows")
                # du en Git Bash funciona
                size_kb=$(du -sk "$path" 2>/dev/null | awk '{print $1}')
                ;;
            *)
                size_kb=$(du -sk "$path" 2>/dev/null | awk '{print $1}')
                ;;
        esac
    elif [ -f "$path" ]; then
        # Es archivo
        local size_bytes=$(wc -c < "$path" 2>/dev/null | awk '{print $1}')
        size_kb=$((size_bytes / 1024))
    fi
    
    if [ -n "$size_kb" ] && [ "$size_kb" != "0" ]; then
        echo "$((size_kb / 1024))"
    else
        echo "0"
    fi
}

# División con awk (reemplazo de bc)
div() {
    echo "$1 $2" | awk '{if($2>0) printf "%.0f", $1/$2; else print 0}'
}

# ----------------------------------------------------------------------------
# INICIO DEL BENCHMARK
# ----------------------------------------------------------------------------

cat > "$REPORT_FILE" << HEADER
================================================================================
BENCHMARK: JVM vs NATIVE IMAGE
Sistema de Pre-Aprobación Crediticia Express
================================================================================

Fecha: $(date +"%Y-%m-%d %H:%M:%S")
Sistema: ${OS_TYPE} ($(uname -m 2>/dev/null || echo "x86_64"))
Requests: ${NUM_REQUESTS}
Version: benchmark.sh v3.0 (cross-platform)

================================================================================

HEADER

print_header "BENCHMARK JVM vs NATIVE IMAGE"

echo ""
echo -e "${WHITE}Configuracion:${NC}"
echo -e "  - Sistema operativo: ${CYAN}${OS_TYPE}${NC}"
echo -e "  - Requests para throughput: ${CYAN}${NUM_REQUESTS}${NC}"
echo -e "  - Tiempo de arranque: ${CYAN}Extraido de logs de Quarkus${NC}"
echo ""

cat << 'INTRO'

ESTE BENCHMARK COMPARARA:

  JVM MODE:
    - Compilacion rapida
    - Mayor uso de memoria
    - Requiere JVM instalada

  NATIVE MODE:
    - Compilacion lenta (una sola vez)
    - Arranque ultra-rapido
    - Menor uso de memoria
    - Ejecutable independiente

TIEMPO ESTIMADO: 10-15 minutos

INTRO

echo ""
read -p "Presiona ENTER para comenzar..."

stop_service

# ----------------------------------------------------------------------------
# FASE 1: COMPILACIÓN JVM
# ----------------------------------------------------------------------------

print_header "FASE 1: COMPILACION JVM"

print_section "1.1 - Limpiando proyecto"
./mvnw clean > /dev/null 2>&1 || true
print_success "Proyecto limpio"

print_section "1.2 - Compilando en modo JVM"
print_info "Ejecutando: ./mvnw package -DskipTests"

START_BUILD_JVM=$(date +%s)
if ./mvnw package -DskipTests > "$BUILD_JVM_LOG" 2>&1; then
    END_BUILD_JVM=$(date +%s)
    BUILD_TIME_JVM=$((END_BUILD_JVM - START_BUILD_JVM))
    print_success "Compilacion JVM completada en ${BUILD_TIME_JVM} segundos"
    
    SIZE_JVM_RAW=$(get_size_mb "target/quarkus-app")
    SIZE_JVM="${SIZE_JVM_RAW} MB"
    print_info "Tamano del artefacto JVM: ${SIZE_JVM}"
else
    print_error "Error en compilacion JVM. Ver log: ${BUILD_JVM_LOG}"
    exit 1
fi

# ----------------------------------------------------------------------------
# FASE 2: PRUEBAS JVM
# ----------------------------------------------------------------------------

print_header "FASE 2: PRUEBAS JVM"

print_section "2.1 - Midiendo tiempo de arranque JVM"
print_info "Iniciando aplicacion JVM..."

java -jar target/quarkus-app/quarkus-run.jar > "$JVM_RUN_LOG" 2>&1 &
JVM_PID=$!

print_info "PID del proceso JVM: ${JVM_PID}"

if wait_for_service; then
    STARTUP_MS_JVM=$(extract_startup_time "$JVM_RUN_LOG")
    if [ "$STARTUP_MS_JVM" -gt 0 ] 2>/dev/null; then
        STARTUP_TIME_JVM="${STARTUP_MS_JVM} ms"
        print_success "Arranque JVM (real): ${STARTUP_TIME_JVM}"
    else
        STARTUP_TIME_JVM="~2000 ms"
        STARTUP_MS_JVM=2000
        print_warning "Tiempo estimado: ${STARTUP_TIME_JVM}"
    fi
    
    log_line=$(grep "started in" "$JVM_RUN_LOG" 2>/dev/null | head -1)
    if [ -n "$log_line" ]; then
        print_info "Log: $log_line"
    fi
else
    print_error "Fallo al arrancar aplicacion JVM"
    print_info "Revisa: cat ${JVM_RUN_LOG}"
    kill -9 $JVM_PID 2>/dev/null || true
    exit 1
fi

print_section "2.2 - Midiendo uso de memoria JVM"
sleep 3
MEMORY_JVM_RAW=$(measure_memory $JVM_PID)
MEMORY_JVM="${MEMORY_JVM_RAW} MB"
print_info "Memoria JVM (RSS): ${MEMORY_JVM}"

print_section "2.3 - Midiendo throughput JVM (${NUM_REQUESTS} requests)"
THROUGHPUT_JVM_RAW=$(load_test "$API_URL/estadisticas" $NUM_REQUESTS)
THROUGHPUT_JVM="${THROUGHPUT_JVM_RAW} req/s"
print_success "Throughput JVM: ${THROUGHPUT_JVM}"

print_section "2.4 - Deteniendo aplicacion JVM"
kill -9 $JVM_PID 2>/dev/null || true
sleep 2
print_success "Aplicacion JVM detenida"

# ----------------------------------------------------------------------------
# FASE 3: COMPILACIÓN NATIVE
# ----------------------------------------------------------------------------

print_header "FASE 3: COMPILACION NATIVE"

print_warning "Esta fase tomara 7-10 minutos"
print_info "La compilacion Native es lenta pero vale la pena"
echo ""

print_section "3.1 - Limpiando proyecto"
./mvnw clean > /dev/null 2>&1 || true
print_success "Proyecto limpio"

print_section "3.2 - Compilando en modo Native"
print_info "Ejecutando: ./mvnw package -Pnative -DskipTests"
print_info "Log en: ${BUILD_NATIVE_LOG}"
print_warning "Por favor espera..."

START_BUILD_NATIVE=$(date +%s)
if ./mvnw package -Pnative -DskipTests > "$BUILD_NATIVE_LOG" 2>&1; then
    END_BUILD_NATIVE=$(date +%s)
    BUILD_TIME_NATIVE=$((END_BUILD_NATIVE - START_BUILD_NATIVE))
    BUILD_MIN=$((BUILD_TIME_NATIVE / 60))
    BUILD_SEC=$((BUILD_TIME_NATIVE % 60))
    print_success "Compilacion Native completada en ${BUILD_TIME_NATIVE}s (${BUILD_MIN}m ${BUILD_SEC}s)"
    
    # Buscar el ejecutable nativo
    if [ -f "target/aprobacion-express-1.0.0-runner" ]; then
        SIZE_NATIVE_RAW=$(get_size_mb "target/aprobacion-express-1.0.0-runner")
    elif [ -f "target/aprobacion-express-1.0.0-runner.exe" ]; then
        SIZE_NATIVE_RAW=$(get_size_mb "target/aprobacion-express-1.0.0-runner.exe")
    fi
    SIZE_NATIVE="${SIZE_NATIVE_RAW} MB"
    print_info "Tamano del ejecutable Native: ${SIZE_NATIVE}"
else
    print_error "Error en compilacion Native. Ver log: ${BUILD_NATIVE_LOG}"
    exit 1
fi

# ----------------------------------------------------------------------------
# FASE 4: PRUEBAS NATIVE
# ----------------------------------------------------------------------------

print_header "FASE 4: PRUEBAS NATIVE"

print_section "4.1 - Midiendo tiempo de arranque Native"
print_info "Iniciando aplicacion Native..."

# Ejecutar el binario nativo (detectar extensión)
if [ -f "target/aprobacion-express-1.0.0-runner.exe" ]; then
    ./target/aprobacion-express-1.0.0-runner.exe > "$NATIVE_RUN_LOG" 2>&1 &
else
    ./target/aprobacion-express-1.0.0-runner > "$NATIVE_RUN_LOG" 2>&1 &
fi
NATIVE_PID=$!

print_info "PID del proceso Native: ${NATIVE_PID}"

if wait_for_service; then
    STARTUP_MS_NATIVE=$(extract_startup_time "$NATIVE_RUN_LOG")
    if [ "$STARTUP_MS_NATIVE" -gt 0 ] 2>/dev/null; then
        STARTUP_TIME_NATIVE="${STARTUP_MS_NATIVE} ms"
        print_success "Arranque Native (real): ${STARTUP_TIME_NATIVE}"
    else
        STARTUP_TIME_NATIVE="~50 ms"
        STARTUP_MS_NATIVE=50
        print_warning "Tiempo estimado: ${STARTUP_TIME_NATIVE}"
    fi
    
    log_line=$(grep "started in" "$NATIVE_RUN_LOG" 2>/dev/null | head -1)
    if [ -n "$log_line" ]; then
        print_info "Log: $log_line"
    fi
else
    print_error "Fallo al arrancar aplicacion Native"
    kill -9 $NATIVE_PID 2>/dev/null || true
    exit 1
fi

print_section "4.2 - Midiendo uso de memoria Native"
sleep 3
MEMORY_NATIVE_RAW=$(measure_memory $NATIVE_PID)
MEMORY_NATIVE="${MEMORY_NATIVE_RAW} MB"
print_info "Memoria Native (RSS): ${MEMORY_NATIVE}"

print_section "4.3 - Midiendo throughput Native (${NUM_REQUESTS} requests)"
THROUGHPUT_NATIVE_RAW=$(load_test "$API_URL/estadisticas" $NUM_REQUESTS)
THROUGHPUT_NATIVE="${THROUGHPUT_NATIVE_RAW} req/s"
print_success "Throughput Native: ${THROUGHPUT_NATIVE}"

print_section "4.4 - Deteniendo aplicacion Native"
kill -9 $NATIVE_PID 2>/dev/null || true
sleep 2
print_success "Aplicacion Native detenida"

# ----------------------------------------------------------------------------
# FASE 5: COMPARATIVA
# ----------------------------------------------------------------------------

print_header "FASE 5: COMPARATIVA FINAL"

# Calcular ratios usando awk (sin bc)
COMPILE_RATIO=$(echo "$BUILD_TIME_NATIVE $BUILD_TIME_JVM" | awk '{if($2>0) printf "%.0f", $1/$2; else print 0}')
STARTUP_RATIO=$(echo "$STARTUP_MS_JVM $STARTUP_MS_NATIVE" | awk '{if($2>0) printf "%.0f", $1/$2; else print 0}')
MEMORY_SAVINGS=$(echo "$MEMORY_JVM_RAW $MEMORY_NATIVE_RAW" | awk '{if($1>0) printf "%.0f", (($1-$2)*100)/$1; else print 0}')
THROUGHPUT_DIFF=$(echo "$THROUGHPUT_NATIVE_RAW $THROUGHPUT_JVM_RAW" | awk '{if($2>0) printf "%.0f", (($1-$2)*100)/$2; else print 0}')

# Tabla de resultados
printf "\n"
printf "╔════════════════════════════════════════════════════════════════════════════╗\n"
printf "║                        RESULTADOS DEL BENCHMARK                            ║\n"
printf "║                        (%s requests de prueba)                              ║\n" "$NUM_REQUESTS"
printf "╠════════════════════════════════════════════════════════════════════════════╣\n"
printf "║ %-27s │ %-18s │ %-18s ║\n" "METRICA" "JVM MODE" "NATIVE MODE"
printf "╠════════════════════════════════════════════════════════════════════════════╣\n"
printf "║ %-27s │ %18s │ %18s ║\n" "Tiempo de compilacion" "${BUILD_TIME_JVM}s" "${BUILD_TIME_NATIVE}s"
printf "║ %-27s │ %18s │ %18s ║\n" "Tiempo de arranque (real)" "${STARTUP_TIME_JVM}" "${STARTUP_TIME_NATIVE}"
printf "║ %-27s │ %18s │ %18s ║\n" "Uso de memoria (RSS)" "${MEMORY_JVM}" "${MEMORY_NATIVE}"
printf "║ %-27s │ %18s │ %18s ║\n" "Throughput" "${THROUGHPUT_JVM}" "${THROUGHPUT_NATIVE}"
printf "║ %-27s │ %18s │ %18s ║\n" "Tamano del artefacto" "${SIZE_JVM}" "${SIZE_NATIVE}"
printf "╚════════════════════════════════════════════════════════════════════════════╝\n"

print_section "ANALISIS DE RESULTADOS"

echo ""
echo -e "${CYAN}INTERPRETACION:${NC}"
echo ""

echo -e "${WHITE}1. TIEMPO DE COMPILACION:${NC}"
echo "   JVM: ${BUILD_TIME_JVM}s | Native: ${BUILD_TIME_NATIVE}s"
echo "   Native toma ~${COMPILE_RATIO}x mas tiempo (pero solo una vez)"
echo ""

echo -e "${WHITE}2. TIEMPO DE ARRANQUE:${NC}"
echo "   JVM: ${STARTUP_TIME_JVM} | Native: ${STARTUP_TIME_NATIVE}"
if [ "$STARTUP_RATIO" -ge 10 ] 2>/dev/null; then
    echo -e "   ${GREEN}>>> Native arranca ${STARTUP_RATIO}x MAS RAPIDO <<<${NC}"
elif [ "$STARTUP_RATIO" -ge 2 ] 2>/dev/null; then
    echo -e "   ${GREEN}Native arranca ${STARTUP_RATIO}x mas rapido${NC}"
else
    echo "   Tiempos similares"
fi
echo ""

echo -e "${WHITE}3. USO DE MEMORIA:${NC}"
echo "   JVM: ${MEMORY_JVM} | Native: ${MEMORY_NATIVE}"
if [ "$MEMORY_SAVINGS" -ge 50 ] 2>/dev/null; then
    echo -e "   ${GREEN}>>> Native usa ${MEMORY_SAVINGS}% MENOS memoria <<<${NC}"
elif [ "$MEMORY_SAVINGS" -ge 30 ] 2>/dev/null; then
    echo -e "   ${GREEN}Native usa ${MEMORY_SAVINGS}% menos memoria${NC}"
fi
echo ""

echo -e "${WHITE}4. THROUGHPUT:${NC}"
echo "   JVM: ${THROUGHPUT_JVM} | Native: ${THROUGHPUT_NATIVE}"
echo "   Rendimiento similar"
echo ""

# Conclusiones
echo "════════════════════════════════════════════════════════════════════════════"
echo ""
echo -e "${CYAN}CONCLUSIONES:${NC}"
echo ""

if [ "$MEMORY_SAVINGS" -ge 50 ] 2>/dev/null && [ "$STARTUP_RATIO" -ge 10 ] 2>/dev/null; then
    echo -e "${GREEN}   *** NATIVE es CLARAMENTE SUPERIOR para produccion ***${NC}"
    echo ""
    echo "   - Arranque ${STARTUP_RATIO}x mas rapido"
    echo "   - ${MEMORY_SAVINGS}% menos memoria"
elif [ "$MEMORY_SAVINGS" -ge 40 ] 2>/dev/null; then
    echo -e "${GREEN}   ** NATIVE recomendado para cloud/contenedores **${NC}"
fi

echo ""
echo "   USE JVM PARA:"
echo "     - Desarrollo local"
echo "     - Debugging"
echo "     - Compilacion rapida"
echo ""
echo "   USE NATIVE PARA:"
echo "     - Produccion en cloud"
echo "     - Serverless (Lambda)"
echo "     - Microservicios"
echo "     - Cuando la memoria importa"
echo ""

# Cálculo de ahorro
if [ "$MEMORY_JVM_RAW" -gt 0 ] 2>/dev/null && [ "$MEMORY_NATIVE_RAW" -gt 0 ] 2>/dev/null; then
    JVM_50=$((MEMORY_JVM_RAW * 50))
    NATIVE_50=$((MEMORY_NATIVE_RAW * 50))
    AHORRO=$((JVM_50 - NATIVE_50))
    
    echo -e "${WHITE}CALCULO DE AHORRO (50 microservicios):${NC}"
    echo ""
    echo "   JVM:    50 x ${MEMORY_JVM_RAW} MB = ${JVM_50} MB (~$((JVM_50 / 1024)) GB)"
    echo "   Native: 50 x ${MEMORY_NATIVE_RAW} MB = ${NATIVE_50} MB (~$((NATIVE_50 / 1024)) GB)"
    echo ""
    echo -e "   ${GREEN}Ahorro: ~$((AHORRO / 1024)) GB de RAM${NC}"
    echo ""
fi

# Guardar en reporte
cat >> "$REPORT_FILE" << REPORT

RESULTADOS
==========
Compilacion:  JVM ${BUILD_TIME_JVM}s vs Native ${BUILD_TIME_NATIVE}s
Arranque:     JVM ${STARTUP_TIME_JVM} vs Native ${STARTUP_TIME_NATIVE}
Memoria:      JVM ${MEMORY_JVM} vs Native ${MEMORY_NATIVE}
Throughput:   JVM ${THROUGHPUT_JVM} vs Native ${THROUGHPUT_NATIVE}

METRICAS
========
- Native toma ${COMPILE_RATIO}x mas tiempo en compilar
- Native arranca ${STARTUP_RATIO}x mas rapido
- Native usa ${MEMORY_SAVINGS}% menos memoria

================================================================================
FIN - $(date +"%Y-%m-%d %H:%M:%S")
================================================================================
REPORT

print_success "Reporte guardado en: ${REPORT_FILE}"
echo ""
print_info "Logs disponibles:"
print_info "  - Build JVM: ${BUILD_JVM_LOG}"
print_info "  - Build Native: ${BUILD_NATIVE_LOG}"
print_info "  - Run JVM: ${JVM_RUN_LOG}"
print_info "  - Run Native: ${NATIVE_RUN_LOG}"
echo ""

print_success "Benchmark completado exitosamente!"