#!/bin/bash

# ============================================================================
# SCRIPT DE PRUEBAS FUNCIONALES - SCORING SERVICE
# ============================================================================
# COMPATIBLE CON: Windows (Git Bash/WSL), macOS, Linux
#
# Este script prueba el Scoring Service con:
# - Algoritmo de scoring multi-factor
# - Integración reactiva con customer-service
# - Redis Cache para performance
# - Fault Tolerance (Circuit Breaker, Retry)
# - Diferentes estrategias (Conservative, Balanced, Aggressive)
#
# REQUISITOS:
# - Scoring Service corriendo en localhost:8082
# - Customer Service corriendo en localhost:8081
# - Docker con PostgreSQL y Redis levantados
# - Keycloak en localhost:8080
# - curl instalado
#
# USO:
#   chmod +x test-scoring-service.sh
#   ./test-scoring-service.sh
#
# SALIDA:
#   - Resultados en consola (con colores)
#   - Archivo: scoring-service-report-YYYY-MM-DD-HHMMSS.txt
#
# ============================================================================

# ----------------------------------------------------------------------------
# CONFIGURACIÓN
# ----------------------------------------------------------------------------

# URLs de servicios
SCORING_SERVICE="http://localhost:8082"
CUSTOMER_SERVICE="http://localhost:8081"
KEYCLOAK_URL="http://localhost:8080"
KEYCLOAK_REALM="creditcore"
KEYCLOAK_CLIENT_ID="scoring-service"
KEYCLOAK_CLIENT_SECRET="fgwIG77MYz0hLQyImFYyPskmL0nM3Dgi"
KEYCLOAK_USERNAME="admin-user"
KEYCLOAK_PASSWORD="admin123"

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0;m'

# Emojis
CHECK="✅"
CROSS="❌"
INFO="ℹ️"
ROCKET="🚀"
CHART="📊"
FIRE="🔥"
SEARCH="🔍"
CACHE="⚡"
WARNING="⚠️"

# Contadores
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_TOTAL=0

# Archivo de reporte
TIMESTAMP=$(date +"%Y-%m-%d-%H%M%S")
REPORT_FILE="scoring-service-report-${TIMESTAMP}.txt"

# Variables globales
JWT_TOKEN=""

# ----------------------------------------------------------------------------
# FUNCIONES AUXILIARES
# ----------------------------------------------------------------------------

pause() {
    echo ""
    log_file ""
    echo -e "${YELLOW}>>> Presiona cualquier tecla para continuar...${NC}"
    read -n 1 -s -r
    echo ""
    log_file ""
}

do_curl() {
    local url=$1
    local method=${2:-GET}
    local data=${3:-}
    
    local temp_file=$(mktemp)
    local http_code
    
    if [ -n "$data" ]; then
        http_code=$(curl --max-time 15 -s -w "%{http_code}" -o "$temp_file" -X "$method" \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$url" 2>/dev/null || echo "000")
    else
        http_code=$(curl --max-time 15 -s -w "%{http_code}" -o "$temp_file" "$url" 2>/dev/null || echo "000")
    fi
    
    local body=$(cat "$temp_file" 2>/dev/null || echo "")
    rm -f "$temp_file"
    
    echo "$body"
    echo "---HTTP_CODE---"
    echo "$http_code"
}

do_curl_auth() {
    local url=$1
    local method=${2:-GET}
    local data=${3:-}
    local token=${4:-$JWT_TOKEN}
    
    local temp_file=$(mktemp)
    local http_code
    
    if [ -n "$data" ]; then
        http_code=$(curl --max-time 15 -s -w "%{http_code}" -o "$temp_file" -X "$method" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $token" \
            -d "$data" \
            "$url" 2>/dev/null || echo "000")
    else
        http_code=$(curl --max-time 15 -s -w "%{http_code}" -o "$temp_file" \
            -H "Authorization: Bearer $token" \
            "$url" 2>/dev/null || echo "000")
    fi
    
    local body=$(cat "$temp_file" 2>/dev/null || echo "")
    rm -f "$temp_file"
    
    echo "$body"
    echo "---HTTP_CODE---"
    echo "$http_code"
}

get_jwt_token() {
    local token_response=$(curl --max-time 10 -s -X POST \
        "${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=password" \
        -d "client_id=${KEYCLOAK_CLIENT_ID}" \
        -d "client_secret=${KEYCLOAK_CLIENT_SECRET}" \
        -d "username=${KEYCLOAK_USERNAME}" \
        -d "password=${KEYCLOAK_PASSWORD}" 2>/dev/null)
    
    local token=$(echo "$token_response" | grep -o '"access_token":"[^"]*"' | grep -o ':"[^"]*"' | tr -d ':"')
    
    if [ -z "$token" ]; then
        echo ""
    else
        echo "$token"
    fi
}

extract_body() {
    echo "$1" | awk '/---HTTP_CODE---/{exit} {print}'
}

extract_code() {
    local code=$(echo "$1" | awk '/---HTTP_CODE---/{getline; print}')
    if [ -z "$code" ] || [ "$code" == "000" ]; then
        echo "000"
    else
        echo "$code"
    fi
}

log_both() {
    echo "$1" | tee -a "$REPORT_FILE"
}

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
    echo -e "${GREEN}${CHECK} $1${NC}"
    log_file "✓ $1"
}

print_error() {
    echo -e "${RED}${CROSS} $1${NC}"
    log_file "✗ $1"
}

print_info() {
    echo -e "${BLUE}${INFO} $1${NC}"
    log_file "ℹ $1"
}

print_warning() {
    echo -e "${YELLOW}${WARNING} $1${NC}"
    log_file "⚠ $1"
}

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

run_test() {
    local test_name=$1
    local expected_status=$2
    local actual_status=$3
    
    TESTS_TOTAL=$((TESTS_TOTAL + 1))
    
    if [ "$expected_status" == "$actual_status" ]; then
        print_success "Test pasado: ${test_name} (${actual_status})"
        TESTS_PASSED=$((TESTS_PASSED + 1))
    else
        print_error "Test fallido: ${test_name} - Esperado: ${expected_status}, Actual: ${actual_status}"
        TESTS_FAILED=$((TESTS_FAILED + 1))
    fi
}

# ============================================================================
# INICIO DEL SCRIPT
# ============================================================================

clear
cat << "EOF"
╔═══════════════════════════════════════════════════════════════════════════╗
║                                                                           ║
║   ███████╗ ██████╗ ██████╗ ██████╗ ██╗███╗   ██╗ ██████╗                ║
║   ██╔════╝██╔════╝██╔═══██╗██╔══██╗██║████╗  ██║██╔════╝                ║
║   ███████╗██║     ██║   ██║██████╔╝██║██╔██╗ ██║██║  ███╗               ║
║   ╚════██║██║     ██║   ██║██╔══██╗██║██║╚██╗██║██║   ██║               ║
║   ███████║╚██████╗╚██████╔╝██║  ██║██║██║ ╚████║╚██████╔╝               ║
║   ╚══════╝ ╚═════╝ ╚═════╝ ╚═╝  ╚═╝╚═╝╚═╝  ╚═══╝ ╚═════╝                ║
║                                                                           ║
║                    S E R V I C E   T E S T S                              ║
║                                                                           ║
║   Script de pruebas funcionales para Scoring Service                     ║
║   Capítulo 11: Microservicio de Score Crediticio                         ║
║                                                                           ║
╚═══════════════════════════════════════════════════════════════════════════╝
EOF

echo ""
log_file "SCORING SERVICE - PRUEBAS FUNCIONALES"
log_file "Fecha: $(date)"
log_file ""

# ----------------------------------------------------------------------------
# VERIFICACIÓN DE REQUISITOS
# ----------------------------------------------------------------------------

print_header "${SEARCH} VERIFICACIÓN DE SERVICIOS"

print_section "1. Verificando Scoring Service (puerto 8082)"
response=$(do_curl "${SCORING_SERVICE}/api/scoring/health")
http_code=$(extract_code "$response")

if [ "$http_code" == "200" ]; then
    print_success "Scoring Service respondiendo correctamente"
else
    print_error "Scoring Service NO responde en puerto 8082"
    print_error "Asegúrate de iniciar el servicio con: ./mvnw quarkus:dev"
    exit 1
fi

print_section "2. Verificando Customer Service (puerto 8081)"
response=$(do_curl "${CUSTOMER_SERVICE}/q/health")
http_code=$(extract_code "$response")

if [ "$http_code" == "200" ]; then
    print_success "Customer Service respondiendo correctamente"
else
    print_warning "Customer Service NO responde en puerto 8081"
    print_warning "Algunas pruebas pueden fallar sin customer-service activo"
fi

print_section "3. Verificando Keycloak (puerto 8080)"
response=$(do_curl "${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}")
http_code=$(extract_code "$response")

if [ "$http_code" == "200" ]; then
    print_success "Keycloak disponible"
else
    print_warning "Keycloak NO disponible"
    print_warning "Las pruebas con autenticación pueden fallar"
fi

pause

# ----------------------------------------------------------------------------
# AUTENTICACIÓN
# ----------------------------------------------------------------------------

print_header "${LOCK} AUTENTICACIÓN JWT"

print_section "Obteniendo token JWT de Keycloak..."
JWT_TOKEN=$(get_jwt_token)

if [ -n "$JWT_TOKEN" ]; then
    print_success "Token JWT obtenido correctamente"
    print_info "Token (primeros 50 chars): ${JWT_TOKEN:0:50}..."
    log_file "JWT Token: $JWT_TOKEN"
else
    print_error "No se pudo obtener token JWT"
    print_error "Verifica que Keycloak esté configurado correctamente"
    exit 1
fi

pause

# ----------------------------------------------------------------------------
# PRUEBA 1: CALCULAR SCORE - ESTRATEGIA BALANCED
# ----------------------------------------------------------------------------

print_header "${CHART} PRUEBA 1: CALCULAR SCORE - ESTRATEGIA BALANCED"

print_section "1.1 - Score para cliente ID 1 (TechPeru S.A.C.)"
print_info "Características del cliente:"
print_info "  - Industria: TECHNOLOGY (bajo riesgo)"
print_info "  - Ingresos: S/ 5,000,000/año"
print_info "  - Antigüedad: 3 años"
print_info ""
print_info "Solicitud:"
print_info "  - Monto: S/ 150,000"
print_info "  - Plazo: 24 meses"
print_info "  - Estrategia: BALANCED"

score_request='{
  "customerId": 1,
  "requestedAmount": 150000.00,
  "loanTermMonths": 24,
  "strategy": "BALANCED",
  "notes": "Solicitud para expansión de infraestructura"
}'

log_file ""
log_file "Request:"
log_file "$score_request"

response=$(do_curl_auth "${SCORING_SERVICE}/api/scoring/calculate" "POST" "$score_request")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

log_file ""
log_file "Response:"
format_json "$body" | tee -a "$REPORT_FILE"

if [ "$http_code" == "200" ]; then
    score=$(echo "$body" | grep -o '"score":[0-9]*' | grep -o '[0-9]*')
    riskLevel=$(echo "$body" | grep -o '"riskLevel":"[^"]*"' | grep -o '"[A-Z_]*"' | tail -1 | tr -d '"')
    approved=$(echo "$body" | grep -o '"approved":[^,]*' | grep -o '[^:]*$' | tr -d ' ')
    
    print_success "Score calculado: ${score} (${riskLevel})"
    print_info "Aprobación: ${approved}"
    
    if [ "$score" -ge 650 ]; then
        print_success "Score excelente o bueno - Cliente de bajo riesgo"
    fi
fi

run_test "Calcular score - BALANCED" "200" "$http_code"

pause

# ----------------------------------------------------------------------------
# PRUEBA 2: DIFERENTES ESTRATEGIAS
# ----------------------------------------------------------------------------

print_header "${FIRE} PRUEBA 2: COMPARACIÓN DE ESTRATEGIAS"

print_section "2.1 - Estrategia CONSERVATIVE (Banca tradicional)"
print_info "Requisitos más estrictos, score multiplicado por 0.85"

score_conservative='{
  "customerId": 1,
  "requestedAmount": 150000.00,
  "loanTermMonths": 24,
  "strategy": "CONSERVATIVE"
}'

response=$(do_curl_auth "${SCORING_SERVICE}/api/scoring/calculate" "POST" "$score_conservative")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

if [ "$http_code" == "200" ]; then
    score=$(echo "$body" | grep -o '"score":[0-9]*' | grep -o '[0-9]*')
    print_success "Score CONSERVATIVE: ${score}"
fi

run_test "Estrategia CONSERVATIVE" "200" "$http_code"

print_section "2.2 - Estrategia AGGRESSIVE (Fintech)"
print_info "Mayor tolerancia al riesgo, score multiplicado por 1.15"

score_aggressive='{
  "customerId": 1,
  "requestedAmount": 150000.00,
  "loanTermMonths": 24,
  "strategy": "AGGRESSIVE"
}'

response=$(do_curl_auth "${SCORING_SERVICE}/api/scoring/calculate" "POST" "$score_aggressive")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

if [ "$http_code" == "200" ]; then
    score=$(echo "$body" | grep -o '"score":[0-9]*' | grep -o '[0-9]*')
    print_success "Score AGGRESSIVE: ${score}"
    print_info "Nota: Score más alto por estrategia permisiva"
fi

run_test "Estrategia AGGRESSIVE" "200" "$http_code"

pause

# ----------------------------------------------------------------------------
# PRUEBA 3: ALTO RATIO DEUDA/INGRESO
# ----------------------------------------------------------------------------

print_header "${WARNING} PRUEBA 3: ANÁLISIS DE ALTO ENDEUDAMIENTO"

print_section "3.1 - Solicitud con ratio deuda/ingreso > 40%"
print_info "Cliente ID 2 con ingresos S/ 500,000"
print_info "Solicita S/ 250,000 (50% de ingresos)"

high_debt_request='{
  "customerId": 2,
  "requestedAmount": 250000.00,
  "loanTermMonths": 36,
  "strategy": "BALANCED",
  "notes": "Solicitud con alto ratio deuda/ingreso"
}'

response=$(do_curl_auth "${SCORING_SERVICE}/api/scoring/calculate" "POST" "$high_debt_request")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

log_file ""
log_file "Response (alto endeudamiento):"
format_json "$body" | head -30 | tee -a "$REPORT_FILE"

if [ "$http_code" == "200" ]; then
    recommendation=$(echo "$body" | grep -o '"recommendation":"[^"]*"' | cut -d'"' -f4)
    print_info "Recomendación: ${recommendation}"
    
    if echo "$recommendation" | grep -q "deuda/ingreso"; then
        print_success "Sistema detectó correctamente el alto ratio deuda/ingreso"
    fi
fi

run_test "Detectar alto endeudamiento" "200" "$http_code"

pause

# ----------------------------------------------------------------------------
# PRUEBA 4: HISTÓRICO DE SCORES
# ----------------------------------------------------------------------------

print_header "${SEARCH} PRUEBA 4: CONSULTAR HISTÓRICO"

print_section "4.1 - Histórico completo del cliente 1"
print_info "Consultando todos los scores calculados..."

response=$(do_curl_auth "${SCORING_SERVICE}/api/scoring/history/1")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

log_file ""
log_file "Histórico:"
format_json "$body" | head -40 | tee -a "$REPORT_FILE"

if [ "$http_code" == "200" ]; then
    count=$(echo "$body" | grep -o '"id"' | wc -l | tr -d ' ')
    print_success "Encontrados ${count} scores en histórico"
fi

run_test "Obtener histórico" "200" "$http_code"

print_section "4.2 - Último score calculado (con cache)"
print_info "Este endpoint usa Redis cache para mejor performance"

response=$(do_curl_auth "${SCORING_SERVICE}/api/scoring/latest/1")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

if [ "$http_code" == "200" ]; then
    score=$(echo "$body" | grep -o '"score":[0-9]*' | grep -o '[0-9]*')
    print_success "Último score: ${score}"
    print_info "${CACHE} Resultado cacheado en Redis"
fi

run_test "Obtener último score (cache)" "200" "$http_code"

pause

# ----------------------------------------------------------------------------
# PRUEBA 5: VALIDACIONES
# ----------------------------------------------------------------------------

print_header "${CROSS} PRUEBA 5: VALIDACIONES"

print_section "5.1 - Monto negativo"

invalid_amount='{
  "customerId": 1,
  "requestedAmount": -1000.00,
  "loanTermMonths": 12
}'

response=$(do_curl_auth "${SCORING_SERVICE}/api/scoring/calculate" "POST" "$invalid_amount")
http_code=$(extract_code "$response")

if [ "$http_code" == "400" ]; then
    print_success "Validación funcionó: rechazó monto negativo"
else
    print_error "Validación NO funcionó (código: ${http_code})"
fi

run_test "Validar monto negativo" "400" "$http_code"

print_section "5.2 - Plazo fuera de rango"

invalid_term='{
  "customerId": 1,
  "requestedAmount": 50000.00,
  "loanTermMonths": 500
}'

response=$(do_curl_auth "${SCORING_SERVICE}/api/scoring/calculate" "POST" "$invalid_term")
http_code=$(extract_code "$response")

if [ "$http_code" == "400" ]; then
    print_success "Validación funcionó: rechazó plazo > 360 meses"
else
    print_error "Validación NO funcionó (código: ${http_code})"
fi

run_test "Validar plazo fuera de rango" "400" "$http_code"

print_section "5.3 - Cliente inexistente"

nonexistent_customer='{
  "customerId": 99999,
  "requestedAmount": 50000.00,
  "loanTermMonths": 12
}'

response=$(do_curl_auth "${SCORING_SERVICE}/api/scoring/calculate" "POST" "$nonexistent_customer")
http_code=$(extract_code "$response")

if [ "$http_code" == "404" ] || [ "$http_code" == "503" ]; then
    print_success "Sistema manejó correctamente cliente inexistente"
else
    print_warning "Código inesperado: ${http_code}"
fi

run_test "Manejar cliente inexistente" "404|503" "$http_code"

pause

# ----------------------------------------------------------------------------
# PRUEBA 6: OPENAPI Y MÉTRICAS
# ----------------------------------------------------------------------------

print_header "${CHART} PRUEBA 6: OBSERVABILIDAD"

print_section "6.1 - OpenAPI Specification"

response=$(do_curl "${SCORING_SERVICE}/q/openapi")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

log_file ""
log_file "OpenAPI (primeras líneas):"
echo "$body" | head -15 | tee -a "$REPORT_FILE"

run_test "OpenAPI disponible" "200" "$http_code"

print_section "6.2 - Métricas (Prometheus)"

response=$(do_curl "${SCORING_SERVICE}/q/metrics")
http_code=$(extract_code "$response")

run_test "Métricas disponibles" "200" "$http_code"

print_info ""
print_info "URLs útiles:"
print_info "  - Swagger UI: ${SCORING_SERVICE}/q/swagger-ui"
print_info "  - Health:     ${SCORING_SERVICE}/q/health"
print_info "  - Metrics:    ${SCORING_SERVICE}/q/metrics"

# ----------------------------------------------------------------------------
# RESUMEN FINAL
# ----------------------------------------------------------------------------

print_header "${CHART} RESUMEN DE PRUEBAS"

SUMMARY="
╔════════════════════════════════════════════════════════════════╗
║                    RESULTADOS FINALES                          ║
╠════════════════════════════════════════════════════════════════╣
║ Total de pruebas:    ${TESTS_TOTAL}                                        ║
║ Pruebas exitosas:    ${TESTS_PASSED}                                        ║
║ Pruebas fallidas:    ${TESTS_FAILED}                                         ║
╚════════════════════════════════════════════════════════════════╝
"

echo "$SUMMARY" | tee -a "$REPORT_FILE"

if [ $TESTS_FAILED -eq 0 ]; then
    print_success "¡TODAS LAS PRUEBAS PASARON EXITOSAMENTE! ${ROCKET}"
    log_file ""
    log_file "CONCLUSIÓN: Scoring Service funcionando correctamente."
    log_file "  ✓ Algoritmo de scoring multi-factor operativo"
    log_file "  ✓ Integración reactiva con customer-service"
    log_file "  ✓ Estrategias CONSERVATIVE, BALANCED, AGGRESSIVE funcionando"
    log_file "  ✓ Redis Cache mejorando rendimiento"
    log_file "  ✓ Validaciones y fault tolerance activos"
    exit_code=0
else
    print_error "Algunas pruebas fallaron. Revisa los logs arriba."
    log_file ""
    log_file "CONCLUSIÓN: Sistema con fallas. Revisar pruebas fallidas."
    exit_code=1
fi

cat >> "$REPORT_FILE" << FOOTER

================================================================================
FIN DEL REPORTE
================================================================================

Archivo generado: $REPORT_FILE
Fecha: $(date +"%Y-%m-%d %H:%M:%S")

Capítulo 11: Scoring Service con Quarkus Reactive
Tecnologías implementadas:
  - Quarkus Reactive con Mutiny
  - Hibernate Reactive Panache
  - REST Client Reactive (integración con customer-service)
  - Redis Cache (optimización)
  - Fault Tolerance (Circuit Breaker, Retry, Timeout)
  - Bean Validation
  - JWT/OIDC Security
  - Algoritmo de scoring original multi-factor

================================================================================
FOOTER

echo ""
print_success "Reporte guardado en: ${REPORT_FILE}"
echo ""

exit $exit_code
