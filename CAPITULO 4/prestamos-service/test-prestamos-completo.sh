 #!/bin/bash

#═══════════════════════════════════════════════════════════════════════════════
# 🏦 PRUEBAS INTERACTIVAS - SISTEMA DE PRÉSTAMOS BANCARIOS
#═══════════════════════════════════════════════════════════════════════════════

API_URL="http://localhost:8080"
OUTPUT_FILE="resultados-tests-$(date '+%Y-%m-%d_%H-%M-%S').txt"

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

# IDs capturados
CLIENTE1_ID=""
CLIENTE2_ID=""
CLIENTE3_ID=""
PRESTAMO1_ID=""
PRESTAMO2_ID=""

# Limpiar archivo de salida
> "$OUTPUT_FILE"

# Función de logging mejorada
log() {
    local message="$*"
    # Mostrar en terminal con colores
    printf "%b\n" "$message"
    # Guardar en archivo sin códigos ANSI
    printf "%b\n" "$message" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

# Función para mostrar JSON (resumiendo cuotas si son muchas)
show_json() {
    local json="$1"
    
    # Verificar si jq está disponible
    if ! command -v jq &> /dev/null; then
        # Si no hay jq, mostrar JSON tal cual
        printf "%s\n" "$json" | tee -a "$OUTPUT_FILE"
        return
    fi
    
    local cuota_count=$(echo "$json" | jq -r '.cuotas // [] | length' 2>/dev/null)
    
    # Validar que cuota_count sea un número
    if ! [[ "$cuota_count" =~ ^[0-9]+$ ]]; then
        cuota_count=0
    fi
    
    # Si tiene más de 6 cuotas, resumir
    if [ "$cuota_count" -gt 6 ]; then
        # Mostrar JSON modificado con resumen de cuotas
        local json_resumido=$(echo "$json" | jq --arg total "$cuota_count" '
            if .cuotas then
                .cuotas = [
                    .cuotas[0],
                    .cuotas[1],
                    .cuotas[2],
                    {"_resumen": "... (\($total) cuotas en total) ..."},
                    .cuotas[-3],
                    .cuotas[-2],
                    .cuotas[-1]
                ]
            else . end
        ' 2>/dev/null)
        printf "%s\n" "$json_resumido" | tee -a "$OUTPUT_FILE"
    elif [ -n "$json" ]; then
        # JSON normal
        echo "$json" | jq '.' 2>/dev/null | tee -a "$OUTPUT_FILE" || echo "$json" | tee -a "$OUTPUT_FILE"
    fi
}

# Banner
clear
log "${CYAN}╔════════════════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║${RESET}  ${WHITE}🏦  PRUEBAS INTERACTIVAS - SISTEMA DE PRÉSTAMOS BANCARIOS${RESET}              ${CYAN}║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "${CYAN}📅 Fecha:${RESET} $(date '+%d/%m/%Y %H:%M:%S')"
log "${CYAN}🌐 Servidor:${RESET} $API_URL"
log "${CYAN}📄 Resultados:${RESET} $OUTPUT_FILE"
log ""

# Verificar servidor
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$API_URL/clientes" 2>/dev/null)
if [ "$HTTP_CODE" == "200" ]; then
    log "${YELLOW}🔍 Verificando servidor...${RESET} ${GREEN}✓ Online${RESET}"
else
    log "${YELLOW}🔍 Verificando servidor...${RESET} ${RED}✗ Offline (HTTP: $HTTP_CODE)${RESET}"
    log ""
    log "${RED}ERROR: No se puede conectar al servidor${RESET}"
    log "${YELLOW}Solución: ./mvnw quarkus:dev${RESET}"
    exit 1
fi

log ""
log "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}Presiona ENTER para continuar entre tests${RESET}"
log "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
read -r

# Función para ejecutar test
run_test() {
    local test_num="$1"
    local test_name="$2"
    local method="$3"
    local endpoint="$4"
    local data="$5"
    local expected_status="$6"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    log ""
    log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
    log "${WHITE}Test #$test_num: $test_name${RESET}"
    log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
    log ""
    log "${YELLOW}Method:${RESET}   $method"
    log "${YELLOW}Endpoint:${RESET} $endpoint"
    if [ -n "$data" ]; then
        log "${YELLOW}Data:${RESET}"
        show_json "$data"
    fi
    log ""
    
    # Ejecutar request
    if [ -z "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$endpoint" -H "Content-Type: application/json" 2>/dev/null)
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$endpoint" -H "Content-Type: application/json" -d "$data" 2>/dev/null)
    fi
    
    # Separar body y status
    body=$(echo "$response" | sed '$d')
    status=$(echo "$response" | tail -n 1)
    
    # Mostrar response
    log "${YELLOW}Response (HTTP $status):${RESET}"
    show_json "$body"
    log ""
    
    # Validar status
    if [ "$status" == "$expected_status" ]; then
        log "${GREEN}✓ PASS${RESET} (Expected $expected_status, got $status)"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        log "${RED}✗ FAIL${RESET} (Expected $expected_status, got $status)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    
    # CAPTURAR ID si el response tiene uno
    local captured_id=$(echo "$body" | jq -r '.id' 2>/dev/null)
    if [ -n "$captured_id" ] && [ "$captured_id" != "null" ]; then
        log "${YELLOW}→ ID capturado: $captured_id${RESET}"
        echo "$captured_id" > /tmp/last_id.txt
    fi
    
    log ""
    log "${CYAN}Presiona ENTER para continuar...${RESET}"
    read -r
}

#═══════════════════════════════════════════════════════════════════════════════
# MÓDULO 1: CRUD DE CLIENTES
#═══════════════════════════════════════════════════════════════════════════════

clear
log ""
log "${CYAN}▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓${RESET}"
log "${WHITE}  📋 MÓDULO 1: GESTIÓN DE CLIENTES (CRUD)${RESET}"
log "${CYAN}▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓${RESET}"

# Test 1: Crear cliente 1
run_test 1 \
    "Crear cliente María González" \
    "POST" \
    "$API_URL/clientes" \
    '{
        "nombre": "María Elena González Quispe",
        "dni": "47836291",
        "email": "maria.gonzalez@banco.pe",
        "telefono": "987654321"
    }' \
    "201"
CLIENTE1_ID=$(cat /tmp/last_id.txt 2>/dev/null || echo "")

# Test 2: Crear cliente 2
run_test 2 \
    "Crear cliente Carlos Huamán" \
    "POST" \
    "$API_URL/clientes" \
    '{
        "nombre": "Carlos Alberto Huamán Rojas",
        "dni": "72149803",
        "email": "carlos.huaman@banco.pe",
        "telefono": "956782341"
    }' \
    "201"
CLIENTE2_ID=$(cat /tmp/last_id.txt 2>/dev/null || echo "")

# Test 3: Crear cliente 3
run_test 3 \
    "Crear cliente Rosa Mendoza" \
    "POST" \
    "$API_URL/clientes" \
    '{
        "nombre": "Rosa María Mendoza Vargas",
        "dni": "68234519",
        "email": "rosa.mendoza@banco.pe",
        "telefono": "923456789"
    }' \
    "201"
CLIENTE3_ID=$(cat /tmp/last_id.txt 2>/dev/null || echo "")

# Test 4: DNI duplicado
run_test 4 \
    "Validar rechazo de DNI duplicado" \
    "POST" \
    "$API_URL/clientes" \
    '{
        "nombre": "Duplicado Test",
        "dni": "47836291",
        "email": "duplicado@test.com",
        "telefono": "999999999"
    }' \
    "409"

# Test 5: Email duplicado
run_test 5 \
    "Validar rechazo de email duplicado" \
    "POST" \
    "$API_URL/clientes" \
    '{
        "nombre": "Duplicado Test 2",
        "dni": "99999999",
        "email": "maria.gonzalez@banco.pe",
        "telefono": "999999999"
    }' \
    "409"

# Test 6: Listar clientes
run_test 6 \
    "Listar todos los clientes" \
    "GET" \
    "$API_URL/clientes" \
    "" \
    "200"

# Test 7: Obtener cliente por ID
run_test 7 \
    "Obtener cliente por ID ($CLIENTE1_ID)" \
    "GET" \
    "$API_URL/clientes/$CLIENTE1_ID" \
    "" \
    "200"

# Test 8: Actualizar cliente
run_test 8 \
    "Actualizar teléfono de cliente" \
    "PUT" \
    "$API_URL/clientes/$CLIENTE2_ID" \
    '{
        "nombre": "Carlos Alberto Huamán Rojas",
        "dni": "72149803",
        "email": "carlos.huaman@banco.pe",
        "telefono": "999111222"
    }' \
    "200"

#═══════════════════════════════════════════════════════════════════════════════
# MÓDULO 2: GESTIÓN DE PRÉSTAMOS
#═══════════════════════════════════════════════════════════════════════════════

clear
log ""
log "${CYAN}▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓${RESET}"
log "${WHITE}  💰 MÓDULO 2: GESTIÓN DE PRÉSTAMOS${RESET}"
log "${CYAN}▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓${RESET}"

# Test 9: Crear préstamo 1
run_test 9 \
    "Crear préstamo vehicular S/ 15,000 (24 meses)" \
    "POST" \
    "$API_URL/prestamos" \
    "{
        \"clienteId\": $CLIENTE1_ID,
        \"monto\": 15000.00,
        \"plazoMeses\": 24,
        \"tasaInteres\": 18.50
    }" \
    "201"
PRESTAMO1_ID=$(cat /tmp/last_id.txt 2>/dev/null || echo "")

# Test 10: Crear préstamo 2
run_test 10 \
    "Crear préstamo personal S/ 8,000 (12 meses)" \
    "POST" \
    "$API_URL/prestamos" \
    "{
        \"clienteId\": $CLIENTE2_ID,
        \"monto\": 8000.00,
        \"plazoMeses\": 12,
        \"tasaInteres\": 22.00
    }" \
    "201"
PRESTAMO2_ID=$(cat /tmp/last_id.txt 2>/dev/null || echo "")

# Test 11: Listar préstamos
run_test 11 \
    "Listar todos los préstamos" \
    "GET" \
    "$API_URL/prestamos" \
    "" \
    "200"

# Test 12: Obtener préstamo específico
run_test 12 \
    "Obtener préstamo por ID ($PRESTAMO1_ID)" \
    "GET" \
    "$API_URL/prestamos/$PRESTAMO1_ID" \
    "" \
    "200"

# Test 13: Préstamos de un cliente
run_test 13 \
    "Listar préstamos de María González" \
    "GET" \
    "$API_URL/prestamos/cliente/$CLIENTE1_ID" \
    "" \
    "200"

#═══════════════════════════════════════════════════════════════════════════════
# MÓDULO 3: SISTEMA DE PAGOS
#═══════════════════════════════════════════════════════════════════════════════

clear
log ""
log "${CYAN}▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓${RESET}"
log "${WHITE}  💳 MÓDULO 3: SISTEMA DE PAGOS DE CUOTAS${RESET}"
log "${CYAN}▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓${RESET}"

# Test 14: Pagar cuota 1
run_test 14 \
    "Pagar cuota 1 del préstamo $PRESTAMO1_ID" \
    "PUT" \
    "$API_URL/prestamos/$PRESTAMO1_ID/pagar-cuota/1" \
    "" \
    "200"

# Test 15: Pagar cuota 2
run_test 15 \
    "Pagar cuota 2 del préstamo $PRESTAMO1_ID" \
    "PUT" \
    "$API_URL/prestamos/$PRESTAMO1_ID/pagar-cuota/2" \
    "" \
    "200"

# Test 16: Intentar pagar cuota ya pagada
run_test 16 \
    "Rechazar pago de cuota ya pagada" \
    "PUT" \
    "$API_URL/prestamos/$PRESTAMO1_ID/pagar-cuota/1" \
    "" \
    "409"

#═══════════════════════════════════════════════════════════════════════════════
# RESUMEN FINAL
#═══════════════════════════════════════════════════════════════════════════════

clear
log ""
log "${CYAN}╔════════════════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║${RESET}  ${WHITE}📊 RESUMEN DE EJECUCIÓN${RESET}                                                ${CYAN}║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════════════════╝${RESET}"
log ""

if [ $FAILED_TESTS -eq 0 ]; then
    log "  ${GREEN}🎉 ✓ TODOS LOS TESTS PASARON${RESET}"
else
    log "  ${YELLOW}⚠️  ALGUNOS TESTS FALLARON${RESET}"
fi

log ""
log "  ${GREEN}✓ Tests Exitosos:${RESET}  $PASSED_TESTS / $TOTAL_TESTS"
log "  ${RED}✗ Tests Fallidos:${RESET}  $FAILED_TESTS / $TOTAL_TESTS"
log ""
log "  ${CYAN}📄 Resultados guardados en: $OUTPUT_FILE${RESET}"
log ""
log "${CYAN}╔════════════════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║${RESET}  ${WHITE}IDs Generados (para pruebas manuales)${RESET}                                 ${CYAN}║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "  ${YELLOW}Cliente 1 (María):${RESET}  $CLIENTE1_ID"
log "  ${YELLOW}Cliente 2 (Carlos):${RESET} $CLIENTE2_ID"
log "  ${YELLOW}Cliente 3 (Rosa):${RESET}   $CLIENTE3_ID"
log "  ${YELLOW}Préstamo 1:${RESET}         $PRESTAMO1_ID"
log "  ${YELLOW}Préstamo 2:${RESET}         $PRESTAMO2_ID"
log ""

if [ $FAILED_TESTS -eq 0 ]; then
    exit 0
else
    exit 1
fi