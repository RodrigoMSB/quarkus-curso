#!/bin/bash

#═══════════════════════════════════════════════════════════════════════════════
# 📦 PRUEBAS INTERACTIVAS - API CLÁSICA DE PRODUCTOS
#═══════════════════════════════════════════════════════════════════════════════

API_URL="http://localhost:8080"
OUTPUT_FILE="resultados-productos-clasico-$(date '+%Y-%m-%d_%H-%M-%S').txt"

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
PRODUCTO1_ID=""
PRODUCTO2_ID=""
PRODUCTO3_ID=""
PRODUCTO_NUEVO_ID=""

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
log "${CYAN}║${RESET}  ${WHITE}📦 PRUEBAS INTERACTIVAS - API CLÁSICA DE PRODUCTOS (QUARKUS)${RESET}       ${CYAN}║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "${CYAN}📅 Fecha:${RESET} $(date '+%d/%m/%Y %H:%M:%S')"
log "${CYAN}🌐 Servidor:${RESET} $API_URL"
log "${CYAN}📄 Resultados:${RESET} $OUTPUT_FILE"
log ""

# Verificar servidor
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$API_URL/api/v1/productos/clasico" 2>/dev/null)
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
    if command -v jq &> /dev/null; then
        local captured_id=$(echo "$body" | jq -r '.id' 2>/dev/null)
        if [ -n "$captured_id" ] && [ "$captured_id" != "null" ]; then
            log "${YELLOW}→ ID capturado: $captured_id${RESET}"
            echo "$captured_id" > /tmp/last_id.txt
        fi
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
log "${WHITE}  📦 MÓDULO 1: OPERACIONES CRUD BÁSICAS (CLÁSICO)${RESET}"
log "${CYAN}▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓${RESET}"

# Test 1: Listar todos los productos
run_test 1 \
    "Listar todos los productos (GET all)" \
    "GET" \
    "$API_URL/api/v1/productos/clasico" \
    "" \
    "200"

# Capturar IDs de productos existentes (AHORA FUNCIONA porque $body es global)
if command -v jq &> /dev/null; then
    PRODUCTO1_ID=$(echo "$body" | jq -r '.[0].id // empty' 2>/dev/null)
    PRODUCTO2_ID=$(echo "$body" | jq -r '.[1].id // empty' 2>/dev/null)
    PRODUCTO3_ID=$(echo "$body" | jq -r '.[2].id // empty' 2>/dev/null)
fi

# Test 2: Buscar producto por ID
if [ -n "$PRODUCTO1_ID" ]; then
    run_test 2 \
        "Buscar producto por ID ($PRODUCTO1_ID)" \
        "GET" \
        "$API_URL/api/v1/productos/clasico/$PRODUCTO1_ID" \
        "" \
        "200"
else
    run_test 2 \
        "Buscar producto por ID (1)" \
        "GET" \
        "$API_URL/api/v1/productos/clasico/1" \
        "" \
        "200"
    PRODUCTO1_ID=1
fi

# Test 3: Crear nuevo producto
run_test 3 \
    "Crear nuevo producto - Auriculares Sony" \
    "POST" \
    "$API_URL/api/v1/productos/clasico" \
    '{
        "nombre": "Auriculares Sony WH-1000XM5",
        "descripcion": "Auriculares inalámbricos con cancelación de ruido",
        "precio": 299.99,
        "stock": 25
    }' \
    "201"
PRODUCTO_NUEVO_ID=$(cat /tmp/last_id.txt 2>/dev/null || echo "")

# Test 4: Actualizar producto existente
run_test 4 \
    "Actualizar producto existente (ID=$PRODUCTO1_ID)" \
    "PUT" \
    "$API_URL/api/v1/productos/clasico/$PRODUCTO1_ID" \
    '{
        "nombre": "Laptop Dell XPS 15 Pro Actualizada",
        "descripcion": "Laptop profesional de última generación",
        "precio": 1800.00,
        "stock": 20
    }' \
    "200"

# Test 5: Buscar producto que no existe (404)
run_test 5 \
    "Buscar producto inexistente (404)" \
    "GET" \
    "$API_URL/api/v1/productos/clasico/99999" \
    "" \
    "404"

# Test 6: Eliminar producto
if [ -n "$PRODUCTO3_ID" ]; then
    run_test 6 \
        "Eliminar producto (ID=$PRODUCTO3_ID)" \
        "DELETE" \
        "$API_URL/api/v1/productos/clasico/$PRODUCTO3_ID" \
        "" \
        "204"
else
    run_test 6 \
        "Eliminar producto (ID=3)" \
        "DELETE" \
        "$API_URL/api/v1/productos/clasico/3" \
        "" \
        "204"
    PRODUCTO3_ID=3
fi

# Test 7: Verificar que el producto fue eliminado (404)
run_test 7 \
    "Verificar eliminación (debe retornar 404)" \
    "GET" \
    "$API_URL/api/v1/productos/clasico/$PRODUCTO3_ID" \
    "" \
    "404"

#═══════════════════════════════════════════════════════════════════════════════
# MÓDULO 2: OPERACIONES AVANZADAS (CLÁSICO)
#═══════════════════════════════════════════════════════════════════════════════

clear
log ""
log "${CYAN}▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓${RESET}"
log "${WHITE}  📦 MÓDULO 2: OPERACIONES AVANZADAS (CLÁSICO)${RESET}"
log "${CYAN}▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓${RESET}"

# Test 8: Buscar productos con stock bajo
run_test 8 \
    "Buscar productos con stock bajo (umbral < 30)" \
    "GET" \
    "$API_URL/api/v1/productos/clasico/stock-bajo/30" \
    "" \
    "200"

# Test 9: Carga masiva (bloqueante)
log ""
log "${WHITE}⚠️  NOTA: El siguiente test creará 50 productos de forma bloqueante${RESET}"
log "${WHITE}    Esto permite comparar el rendimiento con la versión reactiva${RESET}"
log ""
log "${CYAN}Presiona ENTER para continuar...${RESET}"
read -r

run_test 9 \
    "Carga masiva - Crear 50 productos (bloqueante)" \
    "POST" \
    "$API_URL/api/v1/productos/clasico/carga-masiva/50" \
    "" \
    "200"

# Test 10: Verificar que la carga masiva funcionó (listar todos)
run_test 10 \
    "Verificar carga masiva - Listar todos los productos" \
    "GET" \
    "$API_URL/api/v1/productos/clasico" \
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

# Test 11: Crear producto con datos inválidos (precio negativo)
run_test 11 \
    "Validar rechazo de precio negativo (400)" \
    "POST" \
    "$API_URL/api/v1/productos/clasico" \
    '{
        "nombre": "Producto Inválido",
        "descripcion": "Precio negativo",
        "precio": -100.00,
        "stock": 10
    }' \
    "400"

# Test 12: Crear producto con stock negativo
run_test 12 \
    "Validar rechazo de stock negativo (400)" \
    "POST" \
    "$API_URL/api/v1/productos/clasico" \
    '{
        "nombre": "Producto Inválido 2",
        "descripcion": "Stock negativo",
        "precio": 100.00,
        "stock": -5
    }' \
    "400"

# Test 13: Actualizar producto inexistente (404)
run_test 13 \
    "Actualizar producto inexistente (404)" \
    "PUT" \
    "$API_URL/api/v1/productos/clasico/99999" \
    '{
        "nombre": "Producto que no existe",
        "descripcion": "Test",
        "precio": 100.00,
        "stock": 10
    }' \
    "404"

# Test 14: Eliminar producto ya eliminado (404)
run_test 14 \
    "Eliminar producto ya eliminado (404)" \
    "DELETE" \
    "$API_URL/api/v1/productos/clasico/$PRODUCTO3_ID" \
    "" \
    "404"

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
log "  ${YELLOW}Producto 1:${RESET}       $PRODUCTO1_ID"
log "  ${YELLOW}Producto 2:${RESET}       $PRODUCTO2_ID"
log "  ${YELLOW}Producto Nuevo:${RESET}   $PRODUCTO_NUEVO_ID"
log ""
log "${CYAN}╔════════════════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║${RESET}  ${WHITE}📦 Conceptos Clásicos Demostrados${RESET}                                     ${CYAN}║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "  ${GREEN}✓${RESET} Operaciones bloqueantes (síncronas)"
log "  ${GREEN}✓${RESET} Repository Pattern con PanacheRepositoryBase"
log "  ${GREEN}✓${RESET} Transacciones con @Transactional"
log "  ${GREEN}✓${RESET} Driver PostgreSQL JDBC (jdbc-postgresql)"
log "  ${GREEN}✓${RESET} Thread bloqueado por request"
log ""

if [ $FAILED_TESTS -eq 0 ]; then
    exit 0
else
    exit 1
fi
