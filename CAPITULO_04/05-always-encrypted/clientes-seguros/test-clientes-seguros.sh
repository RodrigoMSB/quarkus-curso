#!/bin/bash

#═══════════════════════════════════════════════════════════════════════════════
# 🔐 PRUEBAS INTERACTIVAS - API DE CLIENTES SEGUROS (ALWAYS ENCRYPTED)
#═══════════════════════════════════════════════════════════════════════════════

API_URL="http://localhost:8080"
OUTPUT_FILE="resultados-clientes-seguros-$(date '+%Y-%m-%d_%H-%M-%S').txt"

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
CLIENTE_NUEVO_ID=""

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

# Función para mostrar JSON
show_json() {
    local json="$1"
    
    # Verificar si jq está disponible
    if ! command -v jq &> /dev/null; then
        # Si no hay jq, mostrar JSON tal cual
        printf "%s\n" "$json" | tee -a "$OUTPUT_FILE"
        return
    fi
    
    # Mostrar JSON formateado
    if [ -n "$json" ]; then
        echo "$json" | jq '.' 2>/dev/null | tee -a "$OUTPUT_FILE" || echo "$json" | tee -a "$OUTPUT_FILE"
    fi
}

# Banner
clear
log "${CYAN}╔════════════════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║${RESET}  ${WHITE}🔐 PRUEBAS INTERACTIVAS - API CLIENTES SEGUROS (QUARKUS)${RESET}          ${CYAN}║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "${CYAN}📅 Fecha:${RESET} $(date '+%d/%m/%Y %H:%M:%S')"
log "${CYAN}🌐 Servidor:${RESET} $API_URL"
log "${CYAN}📄 Resultados:${RESET} $OUTPUT_FILE"
log "${CYAN}💾 Base de Datos:${RESET} SQL Server (BancoDB)"
log "${CYAN}🔒 Seguridad:${RESET} Always Encrypted (sin activar en esta demo)"
log ""

# Verificar servidor
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$API_URL/api/v1/clientes" 2>/dev/null)
if [ "$HTTP_CODE" == "200" ]; then
    log "${YELLOW}🔍 Verificando servidor...${RESET} ${GREEN}✓ Online${RESET}"
else
    log "${YELLOW}🔍 Verificando servidor...${RESET} ${RED}✗ Offline (HTTP: $HTTP_CODE)${RESET}"
    log ""
    log "${RED}ERROR: No se puede conectar al servidor${RESET}"
    log "${YELLOW}Solución:${RESET}"
    log "  1. Iniciar SQL Server: ${CYAN}docker start sqlserver${RESET}"
    log "  2. Iniciar aplicación: ${CYAN}./mvnw quarkus:dev${RESET}"
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
        # Usar archivo temporal para evitar problemas con comillas en Windows
        TEMP_JSON=$(mktemp)
        printf '%s' "$data" > "$TEMP_JSON"
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$endpoint" -H "Content-Type: application/json" --data-binary "@$TEMP_JSON" 2>/dev/null)
        rm -f "$TEMP_JSON"
    fi
    
    # Separar body y status (AHORA SON VARIABLES GLOBALES)
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
    local captured_id=""
    if command -v jq &> /dev/null; then
        captured_id=$(echo "$body" | jq -r '.id // empty' 2>/dev/null)
    else
        captured_id=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
    fi
    
    if [ -n "$captured_id" ] && [ "$captured_id" != "null" ]; then
        log "${YELLOW}→ ID capturado: $captured_id${RESET}"
        echo "$captured_id" > /tmp/last_cliente_id.txt
    fi
    
    log ""
    log "${CYAN}Presiona ENTER para continuar...${RESET}"
    read -r
}

#═══════════════════════════════════════════════════════════════════════════════
# MÓDULO 1: OPERACIONES CRUD BÁSICAS
#═══════════════════════════════════════════════════════════════════════════════

clear
log ""
log "${CYAN}▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓${RESET}"
log "${WHITE}  📦 MÓDULO 1: OPERACIONES CRUD BÁSICAS${RESET}"
log "${CYAN}▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓${RESET}"

# Test 1: Listar todos los clientes
run_test 1 \
    "Listar todos los clientes (GET all)" \
    "GET" \
    "$API_URL/api/v1/clientes" \
    "" \
    "200"

# Capturar IDs de clientes existentes (AHORA FUNCIONA porque $body es global)
if command -v jq &> /dev/null; then
    CLIENTE1_ID=$(echo "$body" | jq -r '.[0].id // empty' 2>/dev/null)
    CLIENTE2_ID=$(echo "$body" | jq -r '.[1].id // empty' 2>/dev/null)
fi

# Test 2: Buscar cliente por ID
if [ -n "$CLIENTE1_ID" ]; then
    run_test 2 \
        "Buscar cliente por ID ($CLIENTE1_ID)" \
        "GET" \
        "$API_URL/api/v1/clientes/$CLIENTE1_ID" \
        "" \
        "200"
else
    run_test 2 \
        "Buscar cliente por ID (1)" \
        "GET" \
        "$API_URL/api/v1/clientes/1" \
        "" \
        "200"
    CLIENTE1_ID=1
fi

# Test 3: Crear nuevo cliente - Carlos Ramírez
run_test 3 \
    "Crear nuevo cliente - Carlos Ramírez" \
    "POST" \
    "$API_URL/api/v1/clientes" \
    '{
        "nombre": "Carlos Ramírez",
        "numeroTarjeta": "5412-9876-5432-1098",
        "email": "carlos.ramirez@banco.com",
        "telefono": "+56987654321"
    }' \
    "201"
CLIENTE_NUEVO_ID=$(cat /tmp/last_cliente_id.txt 2>/dev/null || echo "")

# Test 4: Crear otro cliente - Ana Torres
run_test 4 \
    "Crear nuevo cliente - Ana Torres" \
    "POST" \
    "$API_URL/api/v1/clientes" \
    '{
        "nombre": "Ana Torres",
        "numeroTarjeta": "4532-8888-9999-0000",
        "email": "ana.torres@banco.com",
        "telefono": "+56911223344"
    }' \
    "201"

# Test 5: Verificar que los clientes se crearon (listar nuevamente)
run_test 5 \
    "Verificar creación - Listar todos los clientes" \
    "GET" \
    "$API_URL/api/v1/clientes" \
    "" \
    "200"

#═══════════════════════════════════════════════════════════════════════════════
# MÓDULO 2: BÚSQUEDAS ESPECÍFICAS
#═══════════════════════════════════════════════════════════════════════════════

clear
log ""
log "${CYAN}▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓${RESET}"
log "${WHITE}  🔍 MÓDULO 2: BÚSQUEDAS ESPECÍFICAS${RESET}"
log "${CYAN}▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓${RESET}"

# Test 6: Buscar por número de tarjeta (Carlos Ramírez)
run_test 6 \
    "Buscar cliente por número de tarjeta (5412-9876-5432-1098)" \
    "GET" \
    "$API_URL/api/v1/clientes/tarjeta/5412-9876-5432-1098" \
    "" \
    "200"

# Test 7: Buscar por número de tarjeta que no existe
run_test 7 \
    "Buscar por tarjeta inexistente (debe retornar lista vacía)" \
    "GET" \
    "$API_URL/api/v1/clientes/tarjeta/9999-9999-9999-9999" \
    "" \
    "200"

# Test 8: Crear cliente con tarjeta específica para buscar
run_test 8 \
    "Crear cliente para búsqueda - Pedro González" \
    "POST" \
    "$API_URL/api/v1/clientes" \
    '{
        "nombre": "Pedro González",
        "numeroTarjeta": "4111-1111-1111-1111",
        "email": "pedro.gonzalez@banco.com",
        "telefono": "+56944556677"
    }' \
    "201"

# Test 9: Buscar la tarjeta recién creada
run_test 9 \
    "Buscar cliente recién creado por tarjeta (4111-1111-1111-1111)" \
    "GET" \
    "$API_URL/api/v1/clientes/tarjeta/4111-1111-1111-1111" \
    "" \
    "200"

#═══════════════════════════════════════════════════════════════════════════════
# MÓDULO 3: VALIDACIONES Y CASOS EDGE
#═══════════════════════════════════════════════════════════════════════════════

clear
log ""
log "${CYAN}▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓${RESET}"
log "${WHITE}  🔍 MÓDULO 3: VALIDACIONES Y CASOS EDGE${RESET}"
log "${CYAN}▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓${RESET}"

# Test 10: Buscar cliente que no existe (404)
run_test 10 \
    "Buscar cliente inexistente por ID (404)" \
    "GET" \
    "$API_URL/api/v1/clientes/99999" \
    "" \
    "404"

# Test 11: Crear cliente con datos mínimos (solo obligatorios)
run_test 11 \
    "Crear cliente con datos mínimos" \
    "POST" \
    "$API_URL/api/v1/clientes" \
    '{
        "nombre": "Laura Díaz",
        "numeroTarjeta": "3782-8224-6310-005",
        "email": "laura.diaz@banco.com",
        "telefono": "+56933445566"
    }' \
    "201"

# Test 12: Crear cliente con tarjeta Visa
run_test 12 \
    "Crear cliente con tarjeta Visa (empieza con 4)" \
    "POST" \
    "$API_URL/api/v1/clientes" \
    '{
        "nombre": "Roberto Silva",
        "numeroTarjeta": "4532-1234-5678-9010",
        "email": "roberto.silva@banco.com",
        "telefono": "+56955667788"
    }' \
    "201"

# Test 13: Crear cliente con tarjeta Mastercard
run_test 13 \
    "Crear cliente con tarjeta Mastercard (empieza con 5)" \
    "POST" \
    "$API_URL/api/v1/clientes" \
    '{
        "nombre": "Sofía Morales",
        "numeroTarjeta": "5425-2334-3010-9903",
        "email": "sofia.morales@banco.com",
        "telefono": "+56966778899"
    }' \
    "201"

# Test 14: Crear cliente con tarjeta American Express
run_test 14 \
    "Crear cliente con tarjeta AMEX (empieza con 37)" \
    "POST" \
    "$API_URL/api/v1/clientes" \
    '{
        "nombre": "Javier López",
        "numeroTarjeta": "3714-4963-5398-431",
        "email": "javier.lopez@banco.com",
        "telefono": "+56977889900"
    }' \
    "201"

# Test 15: Verificar todos los clientes creados
run_test 15 \
    "Listar todos los clientes (verificación final)" \
    "GET" \
    "$API_URL/api/v1/clientes" \
    "" \
    "200"

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
log "${CYAN}║${RESET}  ${WHITE}IDs Capturados (para pruebas manuales)${RESET}                                ${CYAN}║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "  ${YELLOW}Cliente 1:${RESET}       $CLIENTE1_ID"
log "  ${YELLOW}Cliente 2:${RESET}       $CLIENTE2_ID"
log "  ${YELLOW}Cliente Nuevo:${RESET}   $CLIENTE_NUEVO_ID"
log ""
log "${CYAN}╔════════════════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║${RESET}  ${WHITE}🔐 Seguridad - Always Encrypted (No Activado)${RESET}                          ${CYAN}║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "  ${YELLOW}⚠️  Nota importante:${RESET}"
log "  En esta demo, los datos NO están cifrados en la base de datos."
log "  Always Encrypted requiere:"
log ""
log "  ${WHITE}Opción 1 (Windows):${RESET} Windows Certificate Store"
log "  ${WHITE}Opción 2 (Mac/Linux):${RESET} Azure Key Vault (cloud)"
log ""
log "  ${CYAN}Esta demo prueba la estructura de la API sin cifrado activo.${RESET}"
log "  ${CYAN}Para activar cifrado, consultar: SETUP-WINDOWS.md o SETUP-AZURE.md${RESET}"
log ""
log "${CYAN}╔════════════════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║${RESET}  ${WHITE}🔐 Conceptos de Seguridad Demostrados${RESET}                                  ${CYAN}║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "  ${GREEN}✓${RESET} Estructura para datos sensibles (tarjetas, emails)"
log "  ${GREEN}✓${RESET} API REST con endpoints seguros"
log "  ${GREEN}✓${RESET} Búsqueda por tarjeta (determinística con cifrado)"
log "  ${GREEN}✓${RESET} Separación de datos sensibles vs no sensibles"
log "  ${GREEN}✓${RESET} Preparado para Always Encrypted (SQL Server)"
log "  ${GREEN}✓${RESET} Compliance: PCI-DSS (tarjetas) y GDPR (emails)"
log ""
log "${CYAN}╔════════════════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║${RESET}  ${WHITE}📚 Documentación Disponible${RESET}                                            ${CYAN}║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "  ${YELLOW}→${RESET} README.md                  - Visión general del proyecto"
log "  ${YELLOW}→${RESET} INSTALACION.md             - Setup inicial y base de datos"
log "  ${YELLOW}→${RESET} TEORIA-ALWAYS-ENCRYPTED.md - Conceptos y arquitectura"
log "  ${YELLOW}→${RESET} SETUP-WINDOWS.md           - Configurar en Windows"
log "  ${YELLOW}→${RESET} SETUP-AZURE.md             - Configurar con Azure Key Vault"
log ""

if [ $FAILED_TESTS -eq 0 ]; then
    exit 0
else
    exit 1
fi