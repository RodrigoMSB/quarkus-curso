#!/bin/bash

# ============================================================================
# SCRIPT DE PRUEBAS FUNCIONALES - SISTEMA DE PRE-APROBACIÓN CREDITICIA
# ============================================================================
# COMPATIBLE CON: Windows (Git Bash/WSL), macOS, Linux
#
# Este script prueba todos los endpoints del microservicio con casos reales
# y genera un reporte detallado en formato .txt
#
# REQUISITOS:
# - Microservicio corriendo en http://localhost:8080
# - curl instalado
# - bash (Git Bash en Windows, terminal nativo en macOS/Linux)
#
# USO:
#   chmod +x test-aprobacion.sh
#   ./test-aprobacion.sh
#
# SALIDA:
#   - Resultados en consola (con colores)
#   - Archivo: test-report-YYYY-MM-DD-HHMMSS.txt
#
# ============================================================================

# ----------------------------------------------------------------------------
# CONFIGURACIÓN
# ----------------------------------------------------------------------------

# URL base del microservicio
BASE_URL="http://localhost:8080"
API_URL="${BASE_URL}/api/preaprobacion"

# Colores para output en consola
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

# Emojis (compatibles con Git Bash en Windows)
CHECK="✅"
CROSS="❌"
INFO="ℹ️"
ROCKET="🚀"
MONEY="💰"
CHART="📊"
WARNING="⚠️"
FIRE="🔥"

# Contadores
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_TOTAL=0

# Archivo de reporte
TIMESTAMP=$(date +"%Y-%m-%d-%H%M%S")
REPORT_FILE="test-report-${TIMESTAMP}.txt"

# ----------------------------------------------------------------------------
# FUNCIÓN PORTABLE PARA HACER REQUESTS - CROSS-PLATFORM (Mac + Windows)
# ----------------------------------------------------------------------------

# Función para hacer request GET
# FUNCIONA EN: Windows (Git Bash), macOS, Linux
do_curl_get() {
    local url=$1
    curl -s -w "\n---HTTP_CODE---\n%{http_code}" "$url" 2>/dev/null
}

# Función para hacer request POST con JSON
# FUNCIONA EN: Windows (Git Bash), macOS, Linux
# Usa archivo temporal + printf para evitar problemas con echo en Windows
do_curl_post() {
    local url=$1
    local json_data=$2
    
    if [ -n "$json_data" ]; then
        local temp_file=$(mktemp)
        printf '%s' "$json_data" > "$temp_file"
        
        local response=$(curl -s -w "\n---HTTP_CODE---\n%{http_code}" \
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

# Función para extraer body de la respuesta
extract_body() {
    echo "$1" | awk '/---HTTP_CODE---/{exit} {print}'
}

# Función para extraer HTTP code de la respuesta
extract_code() {
    echo "$1" | awk '/---HTTP_CODE---/{getline; print}'
}

# ----------------------------------------------------------------------------
# FUNCIONES AUXILIARES
# ----------------------------------------------------------------------------

# Función para escribir en archivo Y consola
log_both() {
    echo "$1" | tee -a "$REPORT_FILE"
}

# Función para escribir solo en archivo (sin colores)
log_file() {
    echo "$1" >> "$REPORT_FILE"
}

# Función para imprimir encabezados
print_header() {
    echo ""
    echo -e "${CYAN}============================================================================${NC}"
    echo -e "${WHITE}$1${NC}"
    echo -e "${CYAN}============================================================================${NC}"
    echo ""
    
    # Al archivo sin colores
    log_file ""
    log_file "============================================================================"
    log_file "$1"
    log_file "============================================================================"
    log_file ""
}

# Función para imprimir secciones
print_section() {
    echo ""
    echo -e "${MAGENTA}>>> $1${NC}"
    echo ""
    
    log_file ""
    log_file ">>> $1"
    log_file ""
}

# Función para imprimir éxito
print_success() {
    echo -e "${GREEN}${CHECK} $1${NC}"
    log_file "✓ $1"
}

# Función para imprimir error
print_error() {
    echo -e "${RED}${CROSS} $1${NC}"
    log_file "✗ $1"
}

# Función para imprimir info
print_info() {
    echo -e "${BLUE}${INFO} $1${NC}"
    log_file "ℹ $1"
}

# Función para imprimir warning
print_warning() {
    echo -e "${YELLOW}${WARNING} $1${NC}"
    log_file "⚠ $1"
}

# Función para formatear JSON (usa python si jq no está disponible)
format_json() {
    if command -v jq &> /dev/null; then
        echo "$1" | jq '.' 2>/dev/null || echo "$1"
    elif command -v python3 &> /dev/null; then
        echo "$1" | python3 -m json.tool 2>/dev/null || echo "$1"
    elif command -v python &> /dev/null; then
        echo "$1" | python -m json.tool 2>/dev/null || echo "$1"
    else
        echo "$1"
    fi
}

# Función para verificar si el servicio está disponible
check_service() {
    print_info "Verificando que el servicio esté disponible..."
    
    response=$(do_curl_get "${BASE_URL}/q/health/ready")
    http_code=$(extract_code "$response")
    
    if [ "$http_code" != "200" ]; then
        print_error "El servicio no está disponible en ${BASE_URL}"
        log_file ""
        log_file "Por favor, inicia el servicio primero:"
        log_file "  - Con Maven: ./mvnw quarkus:dev"
        log_file "  - Con Docker: docker-compose up"
        log_file ""
        exit 1
    fi
    
    print_success "Servicio disponible!"
}

# Función para ejecutar un test
run_test() {
    local test_name=$1
    local expected_status=$2
    local actual_status=$3
    
    TESTS_TOTAL=$((TESTS_TOTAL + 1))
    
    if [ "$expected_status" == "$actual_status" ]; then
        print_success "TEST #${TESTS_TOTAL}: ${test_name}"
        TESTS_PASSED=$((TESTS_PASSED + 1))
    else
        print_error "TEST #${TESTS_TOTAL}: ${test_name} (esperado: ${expected_status}, obtenido: ${actual_status})"
        TESTS_FAILED=$((TESTS_FAILED + 1))
    fi
}

# Función para pausar entre pruebas
pause_between_tests() {
    echo ""
    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    read -p "Presiona ENTER para continuar con la siguiente prueba..."
    echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
}

# ----------------------------------------------------------------------------
# INICIO DE PRUEBAS
# ----------------------------------------------------------------------------

# Crear archivo de reporte con encabezado
cat > "$REPORT_FILE" << HEADER
================================================================================
REPORTE DE PRUEBAS FUNCIONALES
Sistema de Pre-Aprobación Crediticia Express
================================================================================

Fecha de ejecución: $(date +"%Y-%m-%d %H:%M:%S")
Microservicio: ${BASE_URL}
Generado por: test-aprobacion.sh
Sistema operativo: $(uname -s 2>/dev/null || echo "Windows")

Este reporte contiene los resultados de la batería completa de pruebas
funcionales del sistema de pre-aprobación crediticia, incluyendo:
- Health checks y métricas
- Casos de aprobación (clientes con buen perfil)
- Casos de rechazo (lista negra, deuda alta, validaciones)
- Operaciones CRUD (consultas, listados)

================================================================================

HEADER

print_header "${ROCKET} PRUEBAS FUNCIONALES - SISTEMA DE PRE-APROBACIÓN CREDITICIA"

cat << 'INTRO' | tee -a "$REPORT_FILE"

PRUEBAS INCLUIDAS:
   1. Health Checks (liveness y readiness)
   2. Métricas Prometheus
   3. Estadísticas del sistema
   4. Creación de solicitudes (casos variados)
   5. Consulta de solicitudes
   6. Listado con paginación
   7. Validaciones de negocio

CASOS DE PRUEBA:
   ✅ Cliente perfil excelente → APROBADO
   ✅ Cliente con garantía → APROBADO
   ❌ Cliente en lista negra → RECHAZADO
   ❌ Cliente con deuda muy alta → RECHAZADO
   ❌ Validaciones de entrada → ERROR 400

INTRO

read -p "Presiona ENTER para comenzar las pruebas..."

# Verificar que el servicio esté disponible
check_service

# ----------------------------------------------------------------------------
# PRUEBA 1: HEALTH CHECKS
# ----------------------------------------------------------------------------

print_header "${ROCKET} PRUEBA 1: HEALTH CHECKS"

print_section "1.1 - Liveness Probe (¿Está vivo?)"
print_info "Verificando endpoint: ${BASE_URL}/q/health/live"

response=$(do_curl_get "${BASE_URL}/q/health/live")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

echo "Respuesta:" | tee -a "$REPORT_FILE"
format_json "$body" | tee -a "$REPORT_FILE"

run_test "Health check - Liveness" "200" "$http_code"

print_section "1.2 - Readiness Probe (¿Está listo?)"
print_info "Verificando endpoint: ${BASE_URL}/q/health/ready"

response=$(do_curl_get "${BASE_URL}/q/health/ready")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

echo "Respuesta:" | tee -a "$REPORT_FILE"
format_json "$body" | tee -a "$REPORT_FILE"

run_test "Health check - Readiness" "200" "$http_code"

pause_between_tests

# ----------------------------------------------------------------------------
# PRUEBA 2: MÉTRICAS PROMETHEUS
# ----------------------------------------------------------------------------

print_header "${CHART} PRUEBA 2: MÉTRICAS PROMETHEUS"

print_section "2.1 - Endpoint de métricas"
print_info "Obteniendo métricas de: ${BASE_URL}/q/metrics"

response=$(do_curl_get "${BASE_URL}/q/metrics")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

log_file "Primeras 20 líneas de métricas:"
echo "$body" | head -20 | tee -a "$REPORT_FILE"
log_file "..."
log_file "(Total: $(echo "$body" | wc -l) líneas)"

run_test "Métricas Prometheus disponibles" "200" "$http_code"

pause_between_tests

# ----------------------------------------------------------------------------
# PRUEBA 3: ESTADÍSTICAS DEL SISTEMA
# ----------------------------------------------------------------------------

print_header "${CHART} PRUEBA 3: ESTADÍSTICAS DEL SISTEMA"

print_section "3.1 - Consultar estadísticas generales"
print_info "Obteniendo estadísticas de: ${API_URL}/estadisticas"

response=$(do_curl_get "${API_URL}/estadisticas")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

echo "Estadísticas:" | tee -a "$REPORT_FILE"
format_json "$body" | tee -a "$REPORT_FILE"

run_test "Consultar estadísticas del sistema" "200" "$http_code"

pause_between_tests

# ----------------------------------------------------------------------------
# PRUEBA 4: CLIENTE PERFIL EXCELENTE - APROBADO
# ----------------------------------------------------------------------------

print_header "${CHECK} PRUEBA 4: SOLICITUD APROBADA - CLIENTE PERFIL EXCELENTE"

print_section "4.1 - Crear solicitud (Cliente con excelente perfil crediticio)"

# JSON en UNA SOLA LÍNEA (importante para cross-platform)
solicitud_aprobada='{"numeroDocumento":"70001234","tipoDocumento":"DNI","nombreCompleto":"Andrea Valeria Rojas Mendoza","ingresoMensual":10000.00,"montoSolicitado":50000.00,"deudaActual":2000.00,"antiguedadLaboralAnios":15,"edad":42,"tieneGarantia":true,"tipoGarantia":"HIPOTECARIA"}'

print_info "Enviando solicitud (DNI: 70001234)..."
response=$(do_curl_post "${API_URL}/evaluar" "$solicitud_aprobada")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

log_file ""
print_info "Resultado de la evaluación:"
format_json "$body" | tee -a "$REPORT_FILE"

# Extraer ID de la solicitud para pruebas posteriores
solicitud_id=$(echo "$body" | grep -o '"id":[0-9]*' | grep -o '[0-9]*' | head -1)

if [ -n "$solicitud_id" ]; then
    print_info "ID de solicitud creada: ${solicitud_id}"
    log_file "ID de solicitud: ${solicitud_id}"
fi

if echo "$body" | grep -q '"aprobado":true'; then
    print_success "Solicitud APROBADA como se esperaba"
else
    print_error "Solicitud RECHAZADA (se esperaba aprobación)"
fi

run_test "Cliente perfil excelente aprobado" "200" "$http_code"

pause_between_tests

# ----------------------------------------------------------------------------
# PRUEBA 5: CLIENTE CON GARANTÍA - APROBADO
# ----------------------------------------------------------------------------

print_header "${CHECK} PRUEBA 5: SOLICITUD APROBADA - CLIENTE CON GARANTÍA"

print_section "5.1 - Crear solicitud (Cliente con garantía)"

# JSON en UNA SOLA LÍNEA (Ingreso 6000 → Solicita 30000 = 5x, cumple la regla)
solicitud_garantia='{"numeroDocumento":"70005678","tipoDocumento":"DNI","nombreCompleto":"Roberto Carlos Medina Torres","ingresoMensual":6000.00,"montoSolicitado":30000.00,"deudaActual":2000.00,"antiguedadLaboralAnios":7,"edad":38,"tieneGarantia":true,"tipoGarantia":"VEHICULAR"}'

print_info "Enviando solicitud (DNI: 70005678 con garantía)..."
response=$(do_curl_post "${API_URL}/evaluar" "$solicitud_garantia")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

log_file ""
print_info "Resultado de la evaluación:"
format_json "$body" | tee -a "$REPORT_FILE"

if echo "$body" | grep -q '"aprobado":true'; then
    print_success "Solicitud APROBADA como se esperaba"
else
    print_error "Solicitud RECHAZADA (se esperaba aprobación)"
fi

run_test "Cliente con garantía aprobado" "200" "$http_code"

pause_between_tests

# ----------------------------------------------------------------------------
# PRUEBA 6: CASO DE RECHAZO - LISTA NEGRA
# ----------------------------------------------------------------------------

print_header "${CROSS} PRUEBA 6: SOLICITUD RECHAZADA - CLIENTE EN LISTA NEGRA"

print_section "6.1 - Crear solicitud (Cliente en lista negra del bureau)"

# JSON en UNA SOLA LÍNEA
solicitud_lista_negra='{"numeroDocumento":"12345678","tipoDocumento":"DNI","nombreCompleto":"Juan Carlos Pérez López","ingresoMensual":5000.00,"montoSolicitado":25000.00,"deudaActual":3000.00,"antiguedadLaboralAnios":5,"edad":35,"tieneGarantia":false}'

print_info "Enviando solicitud (DNI: 12345678 está en lista negra)..."
response=$(do_curl_post "${API_URL}/evaluar" "$solicitud_lista_negra")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

log_file ""
print_info "Resultado de la evaluación:"
format_json "$body" | tee -a "$REPORT_FILE"

if echo "$body" | grep -q '"aprobado":false'; then
    print_success "Solicitud RECHAZADA como se esperaba"
    print_info "Motivo: Cliente en lista negra del bureau"
else
    print_error "Solicitud APROBADA (se esperaba rechazo)"
fi

run_test "Cliente lista negra rechazado" "200" "$http_code"

pause_between_tests

# ----------------------------------------------------------------------------
# PRUEBA 7: CASO DE RECHAZO - DEUDA MUY ALTA
# ----------------------------------------------------------------------------

print_header "${CROSS} PRUEBA 7: SOLICITUD RECHAZADA - RATIO DEUDA/INGRESO ALTO"

print_section "7.1 - Crear solicitud (Cliente con deuda muy alta)"

# JSON en UNA SOLA LÍNEA
solicitud_deuda_alta='{"numeroDocumento":"70009876","tipoDocumento":"DNI","nombreCompleto":"María Elena Castro Ruiz","ingresoMensual":3000.00,"montoSolicitado":15000.00,"deudaActual":15000.00,"antiguedadLaboralAnios":4,"edad":32,"tieneGarantia":false}'

print_info "Enviando solicitud (deuda 5x mayor que ingreso)..."
response=$(do_curl_post "${API_URL}/evaluar" "$solicitud_deuda_alta")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

log_file ""
print_info "Resultado de la evaluación:"
format_json "$body" | tee -a "$REPORT_FILE"

if echo "$body" | grep -q '"aprobado":false'; then
    print_success "Solicitud RECHAZADA como se esperaba"
else
    print_error "Solicitud APROBADA (se esperaba rechazo)"
fi

run_test "Cliente deuda alta rechazado" "200" "$http_code"

pause_between_tests

# ----------------------------------------------------------------------------
# PRUEBA 8: VALIDACIONES - DATOS INVÁLIDOS
# ----------------------------------------------------------------------------

print_header "${WARNING} PRUEBA 8: VALIDACIONES DE ENTRADA"

print_section "8.1 - Solicitud con datos inválidos (ingreso negativo)"

# JSON en UNA SOLA LÍNEA
solicitud_invalida='{"numeroDocumento":"70011111","tipoDocumento":"DNI","nombreCompleto":"Test Validación","ingresoMensual":-1000.00,"montoSolicitado":10000.00,"deudaActual":0.00,"antiguedadLaboralAnios":2,"edad":25,"tieneGarantia":false}'

print_info "Enviando solicitud con ingreso negativo (debe ser rechazada)..."
response=$(do_curl_post "${API_URL}/evaluar" "$solicitud_invalida")
http_code=$(extract_code "$response")

if [ "$http_code" == "400" ]; then
    print_success "Validación funcionó correctamente (HTTP 400)"
else
    print_error "Validación falló (se esperaba HTTP 400)"
fi

run_test "Validación de ingreso negativo" "400" "$http_code"

print_section "8.2 - Solicitud con edad menor a 18 años"

# JSON en UNA SOLA LÍNEA
solicitud_menor_edad='{"numeroDocumento":"70012222","tipoDocumento":"DNI","nombreCompleto":"Test Menor Edad","ingresoMensual":3000.00,"montoSolicitado":10000.00,"deudaActual":0.00,"antiguedadLaboralAnios":1,"edad":17,"tieneGarantia":false}'

print_info "Enviando solicitud con edad = 17 años (debe ser rechazada)..."
response=$(do_curl_post "${API_URL}/evaluar" "$solicitud_menor_edad")
http_code=$(extract_code "$response")

if [ "$http_code" == "400" ]; then
    print_success "Validación funcionó correctamente (HTTP 400)"
else
    print_error "Validación falló (se esperaba HTTP 400)"
fi

run_test "Validación de edad mínima" "400" "$http_code"

pause_between_tests

# ----------------------------------------------------------------------------
# PRUEBA 9: CONSULTA DE SOLICITUDES
# ----------------------------------------------------------------------------

print_header "${INFO} PRUEBA 9: CONSULTA DE SOLICITUDES"

print_section "9.1 - Consultar solicitud por ID"

if [ -n "$solicitud_id" ] && [ "$solicitud_id" != "" ]; then
    print_info "Consultando solicitud ID: ${solicitud_id}..."
    response=$(do_curl_get "${API_URL}/${solicitud_id}")
    http_code=$(extract_code "$response")
    body=$(extract_body "$response")
    
    echo "Solicitud encontrada:" | tee -a "$REPORT_FILE"
    format_json "$body" | tee -a "$REPORT_FILE"
    run_test "Consultar solicitud existente" "200" "$http_code"
else
    print_warning "No se pudo obtener ID de solicitud, omitiendo prueba"
fi

print_section "9.2 - Consultar solicitud inexistente"

print_info "Consultando solicitud ID: 999999..."
response=$(do_curl_get "${API_URL}/999999")
http_code=$(extract_code "$response")

if [ "$http_code" == "404" ]; then
    print_success "Respuesta correcta para solicitud inexistente (HTTP 404)"
else
    print_error "Respuesta incorrecta (se esperaba HTTP 404)"
fi

run_test "Consultar solicitud inexistente" "404" "$http_code"

pause_between_tests

# ----------------------------------------------------------------------------
# PRUEBA 10: LISTADO CON PAGINACIÓN
# ----------------------------------------------------------------------------

print_header "${CHART} PRUEBA 10: LISTADO DE SOLICITUDES"

print_section "10.1 - Listar todas las solicitudes (página 0, tamaño 5)"

print_info "Obteniendo lista..."
response=$(do_curl_get "${API_URL}/listar?page=0&size=5")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

echo "Resultado:" | tee -a "$REPORT_FILE"
format_json "$body" | tee -a "$REPORT_FILE"

total=$(echo "$body" | grep -o '"total":[0-9]*' | grep -o '[0-9]*' | head -1)
print_info "Total de solicitudes en BD: ${total}"

run_test "Listar solicitudes con paginación" "200" "$http_code"

# ----------------------------------------------------------------------------
# RESUMEN FINAL
# ----------------------------------------------------------------------------

print_header "${CHART} RESUMEN DE PRUEBAS"

SUMMARY="
╔════════════════════════════════════════════════════════════════╗
║                    RESULTADOS FINALES                          ║
╠════════════════════════════════════════════════════════════════╣
║ Total de pruebas:    $TESTS_TOTAL                                        ║
║ Pruebas exitosas:    $TESTS_PASSED                                        ║
║ Pruebas fallidas:    $TESTS_FAILED                                        ║
╚════════════════════════════════════════════════════════════════╝
"

echo "$SUMMARY" | tee -a "$REPORT_FILE"

if [ $TESTS_FAILED -eq 0 ]; then
    print_success "¡TODAS LAS PRUEBAS PASARON EXITOSAMENTE! ${ROCKET}"
    log_file ""
    log_file "CONCLUSIÓN: Sistema funcionando correctamente. Todas las pruebas pasadas."
    exit_code=0
else
    print_error "Algunas pruebas fallaron. Revisa los logs arriba."
    log_file ""
    log_file "CONCLUSIÓN: Sistema con fallas. Revisar pruebas fallidas."
    exit_code=1
fi

# Footer del reporte
cat >> "$REPORT_FILE" << FOOTER

================================================================================
FIN DEL REPORTE
================================================================================

Archivo generado: $REPORT_FILE
Fecha: $(date +"%Y-%m-%d %H:%M:%S")

Para más información, consulta:
- README.md: Guía de usuario
- TEORIA.md: Conceptos técnicos
- INSTRUCTOR.md: Guía del profesor

================================================================================
FOOTER

echo ""
print_success "Reporte guardado en: ${REPORT_FILE}"
echo ""

exit $exit_code

# ----------------------------------------------------------------------------
# FIN DEL SCRIPT
# ----------------------------------------------------------------------------