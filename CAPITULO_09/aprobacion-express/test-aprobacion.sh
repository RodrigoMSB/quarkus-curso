#!/bin/bash

# ============================================================================
# SCRIPT DE PRUEBAS FUNCIONALES - SISTEMA DE PRE-APROBACIÓN CREDITICIA
# ============================================================================
# VERSION 2.2 - BAJA TODO POR DEFECTO, --keep-db OPCIONAL
# COMPATIBLE CON: Windows (Git Bash), macOS, Linux
#
# REQUISITOS:
#   - Docker Desktop corriendo
#   - Java 17+ (solo si no usa Docker para Quarkus)
#   - Maven (solo si no usa Docker para Quarkus)
#
# USO:
#   ./test-aprobacion.sh                  # JVM local, baja todo al terminar
#   ./test-aprobacion.sh --docker         # Docker para Quarkus
#   ./test-aprobacion.sh --keep-db        # Mantiene PostgreSQL corriendo
#   ./test-aprobacion.sh --docker --keep-db
#
# NOTA WINDOWS: Si hay errores de sintaxis, convertir a LF:
#   sed -i 's/\r$//' test-aprobacion.sh
#
# ============================================================================

set -e

# ----------------------------------------------------------------------------
# CONFIGURACIÓN
# ----------------------------------------------------------------------------

# Modo de ejecución
USE_DOCKER_QUARKUS=false
KEEP_DB=false

for arg in "$@"; do
    case "$arg" in
        --docker) USE_DOCKER_QUARKUS=true ;;
        --keep-db) KEEP_DB=true ;;
    esac
done

# URL base del microservicio
BASE_URL="http://localhost:8080"
API_URL="${BASE_URL}/api/preaprobacion"
HEALTH_URL="${BASE_URL}/q/health/ready"

# Credenciales BD (docker-compose.yml)
DB_USER="postgres"
DB_PASS="postgres123"
DB_NAME="banco_credito"
DB_HOST="host.docker.internal"
DB_CONTAINER="banco-postgres"

# Docker
DOCKER_IMAGE_JVM="aprobacion-express-jvm"
DOCKER_CONTAINER_APP="test-quarkus-app"

# Colores para output en consola
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m'

# Contadores
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_TOTAL=0

# Archivo de reporte
TIMESTAMP=$(date +"%Y-%m-%d-%H%M%S")
REPORT_FILE="test-report-${TIMESTAMP}.txt"

# PID del proceso Quarkus (para cleanup)
QUARKUS_PID=""

# ----------------------------------------------------------------------------
# DETECCIÓN DE SISTEMA OPERATIVO Y HERRAMIENTAS
# ----------------------------------------------------------------------------

detect_os() {
    case "$(uname -s)" in
        Linux*)     echo "Linux" ;;
        Darwin*)    echo "macOS" ;;
        CYGWIN*|MINGW*|MSYS*) echo "Windows" ;;
        *)          echo "Unknown" ;;
    esac
}

OS_TYPE=$(detect_os)

# Función para ejecutar docker-compose
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

# Detectar Maven
detect_maven() {
    if [ -f "./mvnw" ]; then
        echo "./mvnw"
    elif command -v mvn &> /dev/null; then
        echo "mvn"
    else
        echo ""
    fi
}

MVN_CMD=$(detect_maven)

# ----------------------------------------------------------------------------
# FUNCIONES DE CURL CROSS-PLATFORM
# ----------------------------------------------------------------------------

do_curl_get() {
    local url="$1"
    curl -s -w "\n---HTTP_CODE---\n%{http_code}" "$url" 2>/dev/null
}

do_curl_post() {
    local url="$1"
    local json_data="$2"
    
    if [ -n "$json_data" ]; then
        local temp_file
        temp_file=$(mktemp)
        printf '%s' "$json_data" > "$temp_file"
        
        local response
        response=$(curl -s -w "\n---HTTP_CODE---\n%{http_code}" \
            -X POST \
            -H "Content-Type: application/json" \
            --data-binary "@$temp_file" \
            "$url" 2>/dev/null)
        
        rm -f "$temp_file"
        echo "$response"
    else
        echo "ERROR: No JSON data provided"
        return 1
    fi
}

extract_body() {
    echo "$1" | awk '/---HTTP_CODE---/{exit} {print}'
}

extract_code() {
    echo "$1" | awk '/---HTTP_CODE---/{getline; print}'
}

# ----------------------------------------------------------------------------
# FUNCIONES AUXILIARES
# ----------------------------------------------------------------------------

log_file() {
    echo "$1" >> "$REPORT_FILE"
}

print_header() {
    echo ""
    echo -e "${CYAN}============================================================================${NC}"
    echo -e "${WHITE}$1${NC}"
    echo -e "${CYAN}============================================================================${NC}"
    echo ""
    log_file ""
    log_file "============================================================================"
    log_file "$1"
    log_file "============================================================================"
    log_file ""
}

print_section() {
    echo ""
    echo -e "${MAGENTA}>>> $1${NC}"
    echo ""
    log_file ""
    log_file ">>> $1"
    log_file ""
}

print_success() {
    echo -e "${GREEN}[OK] $1${NC}"
    log_file "[OK] $1"
}

print_error() {
    echo -e "${RED}[ERROR] $1${NC}"
    log_file "[ERROR] $1"
}

print_info() {
    echo -e "${BLUE}[INFO] $1${NC}"
    log_file "[INFO] $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN] $1${NC}"
    log_file "[WARN] $1"
}

format_json() {
    if command -v jq &> /dev/null; then
        echo "$1" | jq '.' 2>/dev/null || echo "$1"
    else
        echo "$1"
    fi
}

run_test() {
    local test_name="$1"
    local expected_status="$2"
    local actual_status="$3"
    
    TESTS_TOTAL=$((TESTS_TOTAL + 1))
    
    if [ "$expected_status" = "$actual_status" ]; then
        print_success "TEST #${TESTS_TOTAL}: ${test_name}"
        TESTS_PASSED=$((TESTS_PASSED + 1))
    else
        print_error "TEST #${TESTS_TOTAL}: ${test_name} (esperado: ${expected_status}, obtenido: ${actual_status})"
        TESTS_FAILED=$((TESTS_FAILED + 1))
    fi
}

pause_between_tests() {
    echo ""
    echo -e "${YELLOW}------------------------------------------------------------------------${NC}"
    read -p "Presiona ENTER para continuar con la siguiente prueba..."
    echo -e "${YELLOW}------------------------------------------------------------------------${NC}"
    echo ""
}

# ----------------------------------------------------------------------------
# FUNCIONES DE INFRAESTRUCTURA
# ----------------------------------------------------------------------------

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

wait_for_quarkus() {
    local max_attempts=90
    local attempt=1
    print_info "Esperando que Quarkus este listo..."
    while [ $attempt -le $max_attempts ]; do
        if curl -s -f "$HEALTH_URL" > /dev/null 2>&1; then
            return 0
        fi
        sleep 1
        attempt=$((attempt + 1))
    done
    return 1
}

start_postgres() {
    print_section "Iniciando PostgreSQL (docker-compose)"
    
    if [ ! -f "docker-compose.yml" ]; then
        print_error "No se encuentra docker-compose.yml"
        exit 1
    fi
    
    run_docker_compose up -d > /dev/null 2>&1
    
    if wait_for_postgres; then
        print_success "PostgreSQL listo (BD: ${DB_NAME})"
    else
        print_error "PostgreSQL no responde"
        exit 1
    fi
}

start_quarkus_local() {
    print_section "Compilando y ejecutando Quarkus (JVM local)"
    
    if [ -z "$MVN_CMD" ]; then
        print_error "Maven no encontrado. Instala Maven o usa --docker"
        exit 1
    fi
    
    # Compilar
    print_info "Compilando proyecto..."
    if ! $MVN_CMD package -DskipTests -q > /dev/null 2>&1; then
        print_error "Error compilando. Ejecuta: $MVN_CMD package -DskipTests"
        exit 1
    fi
    print_success "Compilacion exitosa"
    
    # Ejecutar en background
    print_info "Iniciando Quarkus..."
    java -jar target/quarkus-app/quarkus-run.jar > /dev/null 2>&1 &
    QUARKUS_PID=$!
    
    if wait_for_quarkus; then
        print_success "Quarkus listo (PID: ${QUARKUS_PID})"
    else
        print_error "Quarkus no arranco correctamente"
        cleanup_all
        exit 1
    fi
}

start_quarkus_docker() {
    print_section "Construyendo y ejecutando Quarkus (Docker)"
    
    if [ ! -f "src/main/docker/Dockerfile.jvm" ]; then
        print_error "No se encuentra src/main/docker/Dockerfile.jvm"
        exit 1
    fi
    
    # Build
    print_info "Construyendo imagen Docker..."
    if ! docker build -f src/main/docker/Dockerfile.jvm -t "$DOCKER_IMAGE_JVM" . > /dev/null 2>&1; then
        print_error "Error construyendo imagen"
        exit 1
    fi
    print_success "Imagen construida"
    
    # Run
    print_info "Iniciando contenedor..."
    docker rm -f "$DOCKER_CONTAINER_APP" > /dev/null 2>&1 || true
    
    docker run -d \
        --name "$DOCKER_CONTAINER_APP" \
        -p 8080:8080 \
        -e QUARKUS_DATASOURCE_JDBC_URL="jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}" \
        -e QUARKUS_DATASOURCE_USERNAME="${DB_USER}" \
        -e QUARKUS_DATASOURCE_PASSWORD="${DB_PASS}" \
        "$DOCKER_IMAGE_JVM" > /dev/null 2>&1
    
    if wait_for_quarkus; then
        print_success "Quarkus listo (contenedor: ${DOCKER_CONTAINER_APP})"
    else
        print_error "Quarkus no arranco correctamente"
        docker logs "$DOCKER_CONTAINER_APP" 2>&1 | tail -20
        cleanup_all
        exit 1
    fi
}

stop_quarkus() {
    print_section "Deteniendo Quarkus"
    
    if [ "$USE_DOCKER_QUARKUS" = true ]; then
        docker stop "$DOCKER_CONTAINER_APP" > /dev/null 2>&1 || true
        docker rm "$DOCKER_CONTAINER_APP" > /dev/null 2>&1 || true
        docker rmi "$DOCKER_IMAGE_JVM" > /dev/null 2>&1 || true
        print_success "Contenedor detenido"
    else
        if [ -n "$QUARKUS_PID" ]; then
            kill -9 "$QUARKUS_PID" 2>/dev/null || true
            print_success "Proceso detenido (PID: ${QUARKUS_PID})"
        fi
        # Tambien intentar matar cualquier proceso en 8080
        case "$OS_TYPE" in
            "Windows")
                local pid
                pid=$(netstat -ano 2>/dev/null | grep ":8080 " | grep "LISTENING" | awk '{print $5}' | head -1)
                if [ -n "$pid" ]; then
                    taskkill //F //PID "$pid" > /dev/null 2>&1 || true
                fi
                ;;
            *)
                if command -v lsof &> /dev/null; then
                    local pid
                    pid=$(lsof -ti:8080 2>/dev/null || echo "")
                    if [ -n "$pid" ]; then
                        kill -9 $pid 2>/dev/null || true
                    fi
                fi
                ;;
        esac
    fi
}

cleanup_all() {
    echo ""
    print_warning "Limpiando recursos..."
    stop_quarkus
    
    if [ "$KEEP_DB" = false ]; then
        print_section "Deteniendo PostgreSQL"
        run_docker_compose down > /dev/null 2>&1 || true
        print_success "PostgreSQL detenido"
    else
        print_info "PostgreSQL sigue corriendo (--keep-db)"
    fi
}

# Trap para cleanup en caso de Ctrl+C o error
trap cleanup_all EXIT

# ----------------------------------------------------------------------------
# INICIO DEL SCRIPT
# ----------------------------------------------------------------------------

# Crear archivo de reporte
cat > "$REPORT_FILE" << HEADER
================================================================================
REPORTE DE PRUEBAS FUNCIONALES
Sistema de Pre-Aprobacion Crediticia Express
================================================================================

Fecha de ejecucion: $(date +"%Y-%m-%d %H:%M:%S")
Sistema operativo: ${OS_TYPE}
Modo Quarkus: $([ "$USE_DOCKER_QUARKUS" = true ] && echo "Docker" || echo "JVM Local")
Microservicio: ${BASE_URL}
Version: test-aprobacion.sh v2.2

================================================================================

HEADER

print_header "PRUEBAS FUNCIONALES - SISTEMA DE PRE-APROBACION CREDITICIA"

echo -e "${WHITE}Configuracion:${NC}"
echo -e "  - Sistema: ${CYAN}${OS_TYPE}${NC}"
echo -e "  - Modo Quarkus: ${CYAN}$([ "$USE_DOCKER_QUARKUS" = true ] && echo "Docker" || echo "JVM Local")${NC}"
echo -e "  - Mantener BD: ${CYAN}$([ "$KEEP_DB" = true ] && echo "Si" || echo "No")${NC}"
echo ""

# ----------------------------------------------------------------------------
# VERIFICACIONES Y ARRANQUE
# ----------------------------------------------------------------------------

print_header "FASE 0: ARRANQUE DE INFRAESTRUCTURA"

# Verificar Docker
print_section "Verificando requisitos"

if ! command -v docker &> /dev/null; then
    print_error "Docker no esta instalado"
    exit 1
fi

if ! docker info > /dev/null 2>&1; then
    print_error "Docker no esta corriendo. Inicia Docker Desktop."
    exit 1
fi
print_success "Docker funcionando"

if ! run_docker_compose version > /dev/null 2>&1; then
    print_error "docker-compose no disponible"
    exit 1
fi
print_success "Docker Compose disponible"

# Arrancar PostgreSQL
start_postgres

# Arrancar Quarkus
if [ "$USE_DOCKER_QUARKUS" = true ]; then
    start_quarkus_docker
else
    start_quarkus_local
fi

echo ""
print_success "Infraestructura lista!"
echo ""
read -p "Presiona ENTER para comenzar las pruebas..."

# ----------------------------------------------------------------------------
# PRUEBA 1: HEALTH CHECKS
# ----------------------------------------------------------------------------

print_header "PRUEBA 1: HEALTH CHECKS"

print_section "1.1 - Liveness Probe"
print_info "Verificando: ${BASE_URL}/q/health/live"

response=$(do_curl_get "${BASE_URL}/q/health/live")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

echo "Respuesta:" | tee -a "$REPORT_FILE"
format_json "$body" | tee -a "$REPORT_FILE"

run_test "Health check - Liveness" "200" "$http_code"

print_section "1.2 - Readiness Probe"
print_info "Verificando: ${BASE_URL}/q/health/ready"

response=$(do_curl_get "${BASE_URL}/q/health/ready")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

echo "Respuesta:" | tee -a "$REPORT_FILE"
format_json "$body" | tee -a "$REPORT_FILE"

run_test "Health check - Readiness" "200" "$http_code"

pause_between_tests

# ----------------------------------------------------------------------------
# PRUEBA 2: ESTADISTICAS
# ----------------------------------------------------------------------------

print_header "PRUEBA 2: ESTADISTICAS DEL SISTEMA"

print_section "2.1 - Consultar estadisticas"
print_info "Obteniendo: ${API_URL}/estadisticas"

response=$(do_curl_get "${API_URL}/estadisticas")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

echo "Estadisticas:" | tee -a "$REPORT_FILE"
format_json "$body" | tee -a "$REPORT_FILE"

run_test "Consultar estadisticas" "200" "$http_code"

pause_between_tests

# ----------------------------------------------------------------------------
# PRUEBA 3: CLIENTE PERFIL EXCELENTE - APROBADO
# ----------------------------------------------------------------------------

print_header "PRUEBA 3: SOLICITUD APROBADA - CLIENTE PERFIL EXCELENTE"

print_section "3.1 - Crear solicitud"

solicitud_aprobada='{"numeroDocumento":"70001234","tipoDocumento":"DNI","nombreCompleto":"Andrea Valeria Rojas Mendoza","ingresoMensual":10000.00,"montoSolicitado":50000.00,"deudaActual":2000.00,"antiguedadLaboralAnios":15,"edad":42,"tieneGarantia":true,"tipoGarantia":"HIPOTECARIA"}'

print_info "Enviando solicitud (DNI: 70001234)..."
response=$(do_curl_post "${API_URL}/evaluar" "$solicitud_aprobada")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

log_file ""
print_info "Resultado:"
format_json "$body" | tee -a "$REPORT_FILE"

solicitud_id=$(echo "$body" | grep -o '"id":[0-9]*' | grep -o '[0-9]*' | head -1)
if [ -n "$solicitud_id" ]; then
    print_info "ID de solicitud: ${solicitud_id}"
fi

if echo "$body" | grep -q '"aprobado":true'; then
    print_success "Solicitud APROBADA como se esperaba"
else
    print_error "Solicitud RECHAZADA (se esperaba aprobacion)"
fi

run_test "Cliente perfil excelente aprobado" "200" "$http_code"

pause_between_tests

# ----------------------------------------------------------------------------
# PRUEBA 4: CLIENTE CON GARANTIA - APROBADO
# ----------------------------------------------------------------------------

print_header "PRUEBA 4: SOLICITUD APROBADA - CLIENTE CON GARANTIA"

print_section "4.1 - Crear solicitud"

solicitud_garantia='{"numeroDocumento":"70005678","tipoDocumento":"DNI","nombreCompleto":"Roberto Carlos Medina Torres","ingresoMensual":6000.00,"montoSolicitado":30000.00,"deudaActual":2000.00,"antiguedadLaboralAnios":7,"edad":38,"tieneGarantia":true,"tipoGarantia":"VEHICULAR"}'

print_info "Enviando solicitud (DNI: 70005678)..."
response=$(do_curl_post "${API_URL}/evaluar" "$solicitud_garantia")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

log_file ""
print_info "Resultado:"
format_json "$body" | tee -a "$REPORT_FILE"

if echo "$body" | grep -q '"aprobado":true'; then
    print_success "Solicitud APROBADA como se esperaba"
else
    print_error "Solicitud RECHAZADA (se esperaba aprobacion)"
fi

run_test "Cliente con garantia aprobado" "200" "$http_code"

pause_between_tests

# ----------------------------------------------------------------------------
# PRUEBA 5: CLIENTE LISTA NEGRA - RECHAZADO
# ----------------------------------------------------------------------------

print_header "PRUEBA 5: SOLICITUD RECHAZADA - CLIENTE EN LISTA NEGRA"

print_section "5.1 - Crear solicitud"

solicitud_lista_negra='{"numeroDocumento":"12345678","tipoDocumento":"DNI","nombreCompleto":"Juan Carlos Perez Lopez","ingresoMensual":5000.00,"montoSolicitado":25000.00,"deudaActual":3000.00,"antiguedadLaboralAnios":5,"edad":35,"tieneGarantia":false}'

print_info "Enviando solicitud (DNI: 12345678 - lista negra)..."
response=$(do_curl_post "${API_URL}/evaluar" "$solicitud_lista_negra")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

log_file ""
print_info "Resultado:"
format_json "$body" | tee -a "$REPORT_FILE"

if echo "$body" | grep -q '"aprobado":false'; then
    print_success "Solicitud RECHAZADA como se esperaba"
else
    print_error "Solicitud APROBADA (se esperaba rechazo)"
fi

run_test "Cliente lista negra rechazado" "200" "$http_code"

pause_between_tests

# ----------------------------------------------------------------------------
# PRUEBA 6: CLIENTE DEUDA ALTA - RECHAZADO
# ----------------------------------------------------------------------------

print_header "PRUEBA 6: SOLICITUD RECHAZADA - DEUDA MUY ALTA"

print_section "6.1 - Crear solicitud"

solicitud_deuda_alta='{"numeroDocumento":"70009876","tipoDocumento":"DNI","nombreCompleto":"Maria Elena Castro Ruiz","ingresoMensual":3000.00,"montoSolicitado":15000.00,"deudaActual":15000.00,"antiguedadLaboralAnios":4,"edad":32,"tieneGarantia":false}'

print_info "Enviando solicitud (deuda 5x mayor que ingreso)..."
response=$(do_curl_post "${API_URL}/evaluar" "$solicitud_deuda_alta")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

log_file ""
print_info "Resultado:"
format_json "$body" | tee -a "$REPORT_FILE"

if echo "$body" | grep -q '"aprobado":false'; then
    print_success "Solicitud RECHAZADA como se esperaba"
else
    print_error "Solicitud APROBADA (se esperaba rechazo)"
fi

run_test "Cliente deuda alta rechazado" "200" "$http_code"

pause_between_tests

# ----------------------------------------------------------------------------
# PRUEBA 7: VALIDACIONES
# ----------------------------------------------------------------------------

print_header "PRUEBA 7: VALIDACIONES DE ENTRADA"

print_section "7.1 - Ingreso negativo"

solicitud_invalida='{"numeroDocumento":"70011111","tipoDocumento":"DNI","nombreCompleto":"Test Validacion","ingresoMensual":-1000.00,"montoSolicitado":10000.00,"deudaActual":0.00,"antiguedadLaboralAnios":2,"edad":25,"tieneGarantia":false}'

print_info "Enviando solicitud con ingreso negativo..."
response=$(do_curl_post "${API_URL}/evaluar" "$solicitud_invalida")
http_code=$(extract_code "$response")

if [ "$http_code" = "400" ]; then
    print_success "Validacion correcta (HTTP 400)"
else
    print_error "Validacion fallo (se esperaba HTTP 400, obtuvo $http_code)"
fi

run_test "Validacion ingreso negativo" "400" "$http_code"

print_section "7.2 - Edad menor a 18"

solicitud_menor='{"numeroDocumento":"70012222","tipoDocumento":"DNI","nombreCompleto":"Test Menor Edad","ingresoMensual":3000.00,"montoSolicitado":10000.00,"deudaActual":0.00,"antiguedadLaboralAnios":1,"edad":17,"tieneGarantia":false}'

print_info "Enviando solicitud con edad = 17..."
response=$(do_curl_post "${API_URL}/evaluar" "$solicitud_menor")
http_code=$(extract_code "$response")

if [ "$http_code" = "400" ]; then
    print_success "Validacion correcta (HTTP 400)"
else
    print_error "Validacion fallo (se esperaba HTTP 400, obtuvo $http_code)"
fi

run_test "Validacion edad minima" "400" "$http_code"

pause_between_tests

# ----------------------------------------------------------------------------
# PRUEBA 8: CONSULTAS
# ----------------------------------------------------------------------------

print_header "PRUEBA 8: CONSULTA DE SOLICITUDES"

print_section "8.1 - Consultar solicitud existente"

if [ -n "$solicitud_id" ]; then
    print_info "Consultando ID: ${solicitud_id}..."
    response=$(do_curl_get "${API_URL}/${solicitud_id}")
    http_code=$(extract_code "$response")
    body=$(extract_body "$response")
    
    format_json "$body" | tee -a "$REPORT_FILE"
    run_test "Consultar solicitud existente" "200" "$http_code"
else
    print_warning "No hay ID disponible, omitiendo"
fi

print_section "8.2 - Consultar solicitud inexistente"

print_info "Consultando ID: 999999..."
response=$(do_curl_get "${API_URL}/999999")
http_code=$(extract_code "$response")

if [ "$http_code" = "404" ]; then
    print_success "Respuesta correcta (HTTP 404)"
else
    print_error "Respuesta incorrecta (se esperaba HTTP 404)"
fi

run_test "Consultar solicitud inexistente" "404" "$http_code"

pause_between_tests

# ----------------------------------------------------------------------------
# PRUEBA 9: LISTADO
# ----------------------------------------------------------------------------

print_header "PRUEBA 9: LISTADO DE SOLICITUDES"

print_section "9.1 - Listar con paginacion"

print_info "Obteniendo lista (page=0, size=5)..."
response=$(do_curl_get "${API_URL}/listar?page=0&size=5")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

format_json "$body" | tee -a "$REPORT_FILE"

total=$(echo "$body" | grep -o '"total":[0-9]*' | grep -o '[0-9]*' | head -1)
print_info "Total solicitudes: ${total:-0}"

run_test "Listar solicitudes" "200" "$http_code"

# ----------------------------------------------------------------------------
# RESUMEN FINAL
# ----------------------------------------------------------------------------

print_header "RESUMEN DE PRUEBAS"

printf "\n"
printf "+--------------------------------------------------------------+\n"
printf "|                    RESULTADOS FINALES                        |\n"
printf "+--------------------------------------------------------------+\n"
printf "| Total de pruebas:    %-38s |\n" "$TESTS_TOTAL"
printf "| Pruebas exitosas:    %-38s |\n" "$TESTS_PASSED"
printf "| Pruebas fallidas:    %-38s |\n" "$TESTS_FAILED"
printf "+--------------------------------------------------------------+\n"

cat >> "$REPORT_FILE" << SUMMARY

RESULTADOS FINALES
==================
Total de pruebas: $TESTS_TOTAL
Pruebas exitosas: $TESTS_PASSED
Pruebas fallidas: $TESTS_FAILED

SUMMARY

if [ "$TESTS_FAILED" -eq 0 ]; then
    echo ""
    print_success "TODAS LAS PRUEBAS PASARON EXITOSAMENTE!"
    log_file "CONCLUSION: Sistema funcionando correctamente."
    exit_code=0
else
    echo ""
    print_error "Algunas pruebas fallaron."
    log_file "CONCLUSION: Sistema con fallas. Revisar pruebas fallidas."
    exit_code=1
fi

cat >> "$REPORT_FILE" << FOOTER

================================================================================
FIN DEL REPORTE - $(date +"%Y-%m-%d %H:%M:%S")
================================================================================
FOOTER

echo ""
print_success "Reporte guardado en: ${REPORT_FILE}"
echo ""

exit $exit_code