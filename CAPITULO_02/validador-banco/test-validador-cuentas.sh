#!/bin/bash

#═══════════════════════════════════════════════════════════════════════════════
# 🏦 PRUEBAS - VALIDADOR DE CUENTAS BANCARIAS
# Capítulo 2 - Quarkus Microservices Course
#═══════════════════════════════════════════════════════════════════════════════

API_URL="http://localhost:8080"
OUTPUT_FILE="resultados-validador-$(date '+%Y-%m-%d_%H-%M-%S').txt"

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
RESET='\033[0m'

# Contadores
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Limpiar archivo de salida
> "$OUTPUT_FILE"

# Función de logging
log() {
    local message="$*"
    printf "%b\n" "$message"
    printf "%b\n" "$message" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

# Función para mostrar JSON
show_json() {
    local json="$1"
    if ! command -v jq &> /dev/null; then
        printf "%s\n" "$json" | tee -a "$OUTPUT_FILE"
        return
    fi
    if [ -n "$json" ]; then
        echo "$json" | jq '.' 2>/dev/null | tee -a "$OUTPUT_FILE" || echo "$json" | tee -a "$OUTPUT_FILE"
    fi
}

# Banner
clear
log "${CYAN}╔════════════════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║${RESET}  ${WHITE}🏦 PRUEBAS - VALIDADOR DE CUENTAS BANCARIAS${RESET}                         ${CYAN}║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "${CYAN}📅 Fecha:${RESET} $(date '+%d/%m/%Y %H:%M:%S')"
log "${CYAN}🌐 API:${RESET} $API_URL"
log "${CYAN}📄 Resultados:${RESET} $OUTPUT_FILE"
log "${CYAN}📋 Regla:${RESET} 10 dígitos numéricos"
log ""

# Verificar servidor
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$API_URL/validar/1234567890" 2>/dev/null)
if [ "$HTTP_CODE" == "200" ]; then
    log "${YELLOW}🔍 Verificando servidor...${RESET} ${GREEN}✓ Online${RESET}"
else
    log "${YELLOW}🔍 Verificando servidor...${RESET} ${RED}✗ Offline${RESET}"
    log ""
    log "${RED}ERROR: Servidor no responde${RESET}"
    log "${YELLOW}Ejecuta: ./mvnw quarkus:dev${RESET}"
    exit 1
fi

log ""
log "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}Presiona ENTER para iniciar tests${RESET}"
log "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
read -r

# Función para ejecutar test
run_test() {
    local test_num="$1"
    local test_name="$2"
    local cuenta="$3"
    local expected_valid="$4"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    log ""
    log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
    log "${WHITE}Test #$test_num: $test_name${RESET}"
    log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
    log ""
    log "${YELLOW}Cuenta:${RESET} $cuenta"
    log ""
    
    # Ejecutar request
    response=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/validar/$cuenta" 2>/dev/null)
    
    # Separar body y status
    body=$(echo "$response" | sed '$d')
    status=$(echo "$response" | tail -n 1)
    
    # Mostrar response
    log "${YELLOW}Response (HTTP $status):${RESET}"
    show_json "$body"
    log ""
    
    # Extraer campo 'valido'
    if command -v jq &> /dev/null; then
        actual_valid=$(echo "$body" | jq -r '.valido' 2>/dev/null)
    else
        actual_valid=$(echo "$body" | grep -o '"valido":[^,}]*' | grep -o 'true\|false')
    fi
    
    # Validar
    if [ "$status" == "200" ] && [ "$actual_valid" == "$expected_valid" ]; then
        log "${GREEN}✓ PASS${RESET}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        log "${RED}✗ FAIL${RESET} (Esperado: válido=$expected_valid, HTTP=200)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    
    log ""
    log "${CYAN}Presiona ENTER...${RESET}"
    read -r
}

#═══════════════════════════════════════════════════════════════════════════════
# TESTS DE VALIDACIÓN
#═══════════════════════════════════════════════════════════════════════════════

clear
log ""
log "${CYAN}▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓${RESET}"
log "${WHITE}  ✅ CUENTAS VÁLIDAS${RESET}"
log "${CYAN}▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓${RESET}"

run_test 1 "Cuenta válida estándar" "1234567890" "true"
run_test 2 "Cuenta con ceros al inicio" "0000000123" "true"
run_test 3 "Cuenta todo nueves" "9999999999" "true"

clear
log ""
log "${CYAN}▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓${RESET}"
log "${WHITE}  ❌ CUENTAS INVÁLIDAS${RESET}"
log "${CYAN}▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓${RESET}"

run_test 4 "Cuenta con 9 dígitos (muy corta)" "123456789" "false"
run_test 5 "Cuenta con 5 dígitos (muy corta)" "12345" "false"
run_test 6 "Cuenta con 11 dígitos (muy larga)" "12345678901" "false"
run_test 7 "Cuenta con letras" "123ABC7890" "false"
run_test 8 "Cuenta todo letras" "ABCDEFGHIJ" "false"
run_test 9 "Cuenta con guiones" "1234-56789" "false"
run_test 10 "Cuenta con arroba" "1234@67890" "false"

#═══════════════════════════════════════════════════════════════════════════════
# RESUMEN FINAL
#═══════════════════════════════════════════════════════════════════════════════

clear
log ""
log "${CYAN}╔════════════════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║${RESET}  ${WHITE}📊 RESUMEN DE PRUEBAS${RESET}                                                 ${CYAN}║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════════════════╝${RESET}"
log ""

if [ $FAILED_TESTS -eq 0 ]; then
    log "  ${GREEN}🎉 ✓ TODOS LOS TESTS PASARON${RESET}"
else
    log "  ${YELLOW}⚠️  ALGUNOS TESTS FALLARON${RESET}"
fi

log ""
log "  ${GREEN}✓ Exitosos:${RESET}  $PASSED_TESTS / $TOTAL_TESTS"
log "  ${RED}✗ Fallidos:${RESET}  $FAILED_TESTS / $TOTAL_TESTS"
log ""
log "  ${CYAN}📄 Resultados: $OUTPUT_FILE${RESET}"
log ""

log "${CYAN}╔════════════════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║${RESET}  ${WHITE}📋 CASOS PROBADOS${RESET}                                                      ${CYAN}║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "  ${GREEN}✅ VÁLIDAS (3 tests):${RESET}"
log "     • Cuenta estándar (1234567890)"
log "     • Con ceros (0000000123)"
log "     • Todo nueves (9999999999)"
log ""
log "  ${RED}❌ INVÁLIDAS (7 tests):${RESET}"
log "     • Muy corta: 9 dígitos"
log "     • Muy corta: 5 dígitos"
log "     • Muy larga: 11 dígitos"
log "     • Con letras"
log "     • Todo letras"
log "     • Con guiones"
log "     • Con símbolos (@)"
log ""

log "${CYAN}╔════════════════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║${RESET}  ${WHITE}🎓 CONCEPTOS DEMOSTRADOS${RESET}                                               ${CYAN}║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "  ${GREEN}✓${RESET} OpenAPI contract-first"
log "  ${GREEN}✓${RESET} Generación de código automática"
log "  ${GREEN}✓${RESET} Validación de formato (regex)"
log "  ${GREEN}✓${RESET} Validación de longitud"
log "  ${GREEN}✓${RESET} Testing de API REST"
log ""

log "${CYAN}╔════════════════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║${RESET}  ${WHITE}💡 REGLA DE VALIDACIÓN${RESET}                                                 ${CYAN}║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "  ${WHITE}Formato:${RESET} Exactamente 10 dígitos numéricos (0-9)"
log "  ${WHITE}Regex:${RESET} ^\\d{10}$"
log ""

log "${CYAN}╔════════════════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║${RESET}  ${WHITE}🔗 RECURSOS${RESET}                                                            ${CYAN}║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "  ${CYAN}📊 Swagger UI:${RESET} http://localhost:8080/q/swagger-ui"
log "  ${CYAN}🛠️  Dev UI:${RESET} http://localhost:8080/q/dev"
log "  ${CYAN}📖 OpenAPI:${RESET} http://localhost:8080/q/openapi"
log ""

if [ $FAILED_TESTS -eq 0 ]; then
    exit 0
else
    exit 1
fi