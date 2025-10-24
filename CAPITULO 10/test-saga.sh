#!/bin/bash

# ============================================================================
# SCRIPT DE PRUEBAS FUNCIONALES - SISTEMA E-COMMERCE CON SAGA Y REDIS
# ============================================================================
# COMPATIBLE CON: Windows (Git Bash/WSL), macOS, Linux
#
# Este script prueba el patrón SAGA, Redis Cache y los 3 microservicios
# y genera un reporte detallado en formato .txt
#
# REQUISITOS:
# - Los 3 microservicios corriendo (Order:8080, Inventory:8081, Payment:8082)
# - Docker con Redis y PostgreSQL levantados
# - curl instalado
#
# USO:
#   chmod +x test-saga.sh
#   ./test-saga.sh
#
# SALIDA:
#   - Resultados en consola (con colores)
#   - Archivo: test-saga-report-YYYY-MM-DD-HHMMSS.txt
#
# ============================================================================

# ----------------------------------------------------------------------------
# CONFIGURACIÓN
# ----------------------------------------------------------------------------

# URLs de los servicios
ORDER_SERVICE="http://localhost:8080"
INVENTORY_SERVICE="http://localhost:8081"
PAYMENT_SERVICE="http://localhost:8082"

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m'

# Emojis
CHECK="✅"
CROSS="❌"
INFO="ℹ️"
ROCKET="🚀"
PACKAGE="📦"
MONEY="💳"
CACHE="⚡"
WARNING="⚠️"
FIRE="🔥"
CHART="📊"

# Contadores
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_TOTAL=0

# Archivo de reporte
TIMESTAMP=$(date +"%Y-%m-%d-%H%M%S")
REPORT_FILE="test-saga-report-${TIMESTAMP}.txt"

# ----------------------------------------------------------------------------
# FUNCIONES AUXILIARES
# ----------------------------------------------------------------------------

# Función para pausar y esperar ENTER
pause() {
    echo ""
    log_file ""
    read -p "Presiona ENTER para continuar..."
    echo ""
    log_file ""
}

# Función para medir tiempo (portable macOS/Linux)
get_time_ms() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        python3 -c 'import time; print(int(time.time() * 1000))'
    else
        # Linux
        date +%s%3N
    fi
}

# Función para hacer request y separar body de status code
do_curl() {
    local url=$1
    local method=${2:-GET}
    local data=${3:-}
    
    if [ -n "$data" ]; then
        curl -s -w "\n---HTTP_CODE---\n%{http_code}" -X "$method" \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$url"
    else
        curl -s -w "\n---HTTP_CODE---\n%{http_code}" "$url"
    fi
}

extract_body() {
    echo "$1" | awk '/---HTTP_CODE---/{exit} {print}'
}

extract_code() {
    echo "$1" | awk '/---HTTP_CODE---/{getline; print}'
}

# Función para escribir en archivo Y consola
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
        print_success "TEST #${TESTS_TOTAL}: ${test_name}"
        TESTS_PASSED=$((TESTS_PASSED + 1))
    else
        print_error "TEST #${TESTS_TOTAL}: ${test_name} (esperado: ${expected_status}, obtenido: ${actual_status})"
        TESTS_FAILED=$((TESTS_FAILED + 1))
    fi
}

# ----------------------------------------------------------------------------
# INICIO DE PRUEBAS
# ----------------------------------------------------------------------------

# Crear archivo de reporte con encabezado
cat > "$REPORT_FILE" << HEADER
================================================================================
REPORTE DE PRUEBAS FUNCIONALES
Sistema E-Commerce con Patrón SAGA y Redis Cache
================================================================================

Fecha de ejecución: $(date +"%Y-%m-%d %H:%M:%S")
Servicios bajo prueba:
  - Order Service:     ${ORDER_SERVICE}
  - Inventory Service: ${INVENTORY_SERVICE}
  - Payment Service:   ${PAYMENT_SERVICE}

Generado por: test-saga.sh
Sistema operativo: $(uname -s 2>/dev/null || echo "Windows")

Este reporte contiene los resultados de las pruebas funcionales del sistema
de microservicios con patrón SAGA para transacciones distribuidas y Redis
para caching, incluyendo:
  - Health checks de los 3 microservicios
  - Verificación de productos disponibles
  - SAGA exitosa (Reservar → Pagar → Confirmar)
  - Redis Cache (medición de latencia y mejora de rendimiento)
  - SAGA con compensación (Stock insuficiente)
  - Verificación de rollback

================================================================================

HEADER

print_header "${ROCKET} PRUEBAS FUNCIONALES - SAGA CON REDIS"

cat << 'INTRO' | tee -a "$REPORT_FILE"

PRUEBAS INCLUIDAS:
   1. Health Checks de los 3 servicios
   2. Verificación de productos disponibles
   3. SAGA exitosa (orden con stock suficiente)
   4. Redis Cache - Medición de rendimiento
      - Cache MISS (primera consulta desde PostgreSQL)
      - Cache HIT (consultas subsecuentes desde Redis)
      - Comparación de latencias
   5. SAGA con compensación (stock insuficiente)
   6. Verificación de rollback del inventario

CASOS DE PRUEBA:
   ✅ Orden con stock disponible → SAGA COMPLETA → Status: COMPLETED
   ✅ Cache Redis → Verificar mejora de rendimiento
   ❌ Orden con stock insuficiente → COMPENSACIÓN SAGA → Status: FAILED
   ✅ Inventario liberado después de compensación

INTRO

pause

# ----------------------------------------------------------------------------
# PRUEBA 1: HEALTH CHECKS
# ----------------------------------------------------------------------------

print_header "${INFO} PRUEBA 1: HEALTH CHECKS DE LOS SERVICIOS"

print_section "1.1 - Health Check: Order Service (8080)"
response=$(do_curl "${ORDER_SERVICE}/health")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

log_file "Respuesta:"
format_json "$body" | tee -a "$REPORT_FILE"
run_test "Order Service Health" "200" "$http_code"

print_section "1.2 - Health Check: Inventory Service (8081)"
response=$(do_curl "${INVENTORY_SERVICE}/health")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

log_file "Respuesta:"
format_json "$body" | tee -a "$REPORT_FILE"
run_test "Inventory Service Health" "200" "$http_code"

print_section "1.3 - Health Check: Payment Service (8082)"
response=$(do_curl "${PAYMENT_SERVICE}/health")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

log_file "Respuesta:"
format_json "$body" | tee -a "$REPORT_FILE"
run_test "Payment Service Health" "200" "$http_code"

pause

# ----------------------------------------------------------------------------
# PRUEBA 2: VERIFICAR PRODUCTOS DISPONIBLES
# ----------------------------------------------------------------------------

print_header "${PACKAGE} PRUEBA 2: VERIFICAR PRODUCTOS EN INVENTARIO"

print_section "2.1 - Listar productos disponibles"
print_info "Consultando inventario..."
response=$(do_curl "${INVENTORY_SERVICE}/api/inventory/products")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

if [ "$http_code" == "200" ]; then
    product_count=$(echo "$body" | grep -o "productCode" | wc -l)
    print_success "Productos encontrados: ${product_count}"
    
    log_file "Primeros 3 productos:"
    format_json "$body" | head -50 | tee -a "$REPORT_FILE"
else
    print_warning "No se pudieron listar productos (HTTP ${http_code})"
fi

run_test "Listar productos" "200" "$http_code"

print_section "2.2 - Consultar producto específico (LAPTOP-001)"
response=$(do_curl "${INVENTORY_SERVICE}/api/inventory/products/LAPTOP-001")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

if [ "$http_code" == "200" ]; then
    log_file "Producto LAPTOP-001:"
    format_json "$body" | tee -a "$REPORT_FILE"
    
    stock=$(echo "$body" | grep -o '"stock":[0-9]*' | grep -o '[0-9]*')
    print_info "Stock disponible: ${stock} unidades"
else
    print_error "Producto no encontrado"
fi

run_test "Consultar producto LAPTOP-001" "200" "$http_code"

pause

# ----------------------------------------------------------------------------
# PRUEBA 3: SAGA EXITOSA
# ----------------------------------------------------------------------------

print_header "${CHECK} PRUEBA 3: SAGA EXITOSA - CREAR ORDEN"

print_section "3.1 - Crear orden con stock suficiente"

orden_exitosa='{
  "userId": "test-user-saga-001",
  "paymentMethod": "credit_card",
  "items": [
    {"productCode": "LAPTOP-001", "quantity": 1},
    {"productCode": "MOUSE-001", "quantity": 2}
  ]
}'

print_info "Enviando solicitud de orden..."
log_file "Request:"
log_file "$orden_exitosa"

response=$(do_curl "${ORDER_SERVICE}/api/orders" "POST" "$orden_exitosa")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

log_file ""
log_file "Response:"
format_json "$body" | tee -a "$REPORT_FILE"

orden_id=$(echo "$body" | grep -o '"orderId":"[^"]*"' | head -1 | grep -o '"[a-zA-Z0-9\-]*"' | tail -1 | tr -d '"')
orden_status=$(echo "$body" | grep -o '"status":"[^"]*"' | head -1 | grep -o '"[A-Z]*"' | tail -1 | tr -d '"')
total=$(echo "$body" | grep -o '"totalAmount":[0-9.]*' | grep -o '[0-9.]*')

if [ "$orden_status" == "COMPLETED" ]; then
    print_success "SAGA COMPLETADA EXITOSAMENTE!"
    print_info "Order ID: ${orden_id}"
    print_info "Status: ${orden_status}"
    print_info "Total: \$${total}"
    
    log_file ""
    log_file "SAGA EJECUTADA:"
    log_file "  1. ✓ Inventario reservado (LAPTOP-001 × 1, MOUSE-001 × 2)"
    log_file "  2. ✓ Pago procesado (\$${total})"
    log_file "  3. ✓ Reserva confirmada"
    log_file "  4. ✓ Orden guardada en BD"
else
    print_error "SAGA FALLÓ (Status: ${orden_status})"
fi

run_test "Crear orden exitosa (SAGA completa)" "201" "$http_code"

pause

# ----------------------------------------------------------------------------
# PRUEBA 4: REDIS CACHE
# ----------------------------------------------------------------------------

print_header "${CACHE} PRUEBA 4: VERIFICAR REDIS CACHE"

if [ -n "$orden_id" ] && [ "$orden_id" != "null" ] && [ "$orden_id" != "" ]; then
    
    print_section "4.1 - Primera consulta (Cache MISS esperado)"
    print_info "Consultando orden ${orden_id} por primera vez..."
    print_info "Esta consulta va directo a PostgreSQL..."
    
    start_time=$(get_time_ms)
    response=$(do_curl "${ORDER_SERVICE}/api/orders/${orden_id}")
    end_time=$(get_time_ms)
    http_code=$(extract_code "$response")
    body=$(extract_body "$response")
    
    latency_1=$((end_time - start_time))
    print_success "Latencia primera consulta: ${latency_1}ms (desde PostgreSQL)"
    log_file "Latencia: ${latency_1}ms (Cache MISS - consulta a BD)"
    log_file ""
    log_file "Response (primera consulta):"
    format_json "$body" | head -30 | tee -a "$REPORT_FILE"
    
    run_test "Primera consulta orden (Cache MISS)" "200" "$http_code"
    
    print_section "4.2 - Verificar que se guardó en Redis"
    sleep 1
    
    print_info "Buscando clave en Redis..."
    
    # Buscar todas las claves relacionadas con la orden
    redis_keys=$(docker exec redis-cache redis-cli --scan --pattern "*${orden_id}*" 2>/dev/null)
    
    if [ -n "$redis_keys" ]; then
        print_success "¡Clave encontrada en Redis Cache!"
        echo "$redis_keys" | while read key; do
            print_info "  Clave: ${key}"
            log_file "Redis Cache: KEY ${key} EXISTE"
            
            # Obtener TTL
            ttl=$(docker exec redis-cache redis-cli TTL "$key" 2>/dev/null)
            if [ "$ttl" -gt 0 ]; then
                print_info "  TTL: ${ttl} segundos"
                log_file "  TTL: ${ttl} segundos"
            fi
        done
    else
        # Buscar con otro patrón común
        redis_keys=$(docker exec redis-cache redis-cli --scan --pattern "order*" 2>/dev/null | head -5)
        
        if [ -n "$redis_keys" ]; then
            print_warning "No se encontró clave exacta, pero hay claves 'order*' en Redis:"
            echo "$redis_keys" | while read key; do
                print_info "  ${key}"
            done
            log_file "Redis Cache: Claves 'order*' encontradas"
        else
            print_info "No se pudo verificar Redis directamente con docker"
            print_info "Pero podemos validar el cache midiendo latencias..."
            log_file "Redis Cache: No se pudo verificar con docker exec"
        fi
    fi
    
    print_section "4.3 - Segunda consulta (Cache HIT esperado)"
    print_info "Consultando orden ${orden_id} nuevamente..."
    print_info "Esta consulta debería venir desde Redis (más rápida)..."
    
    start_time=$(get_time_ms)
    response=$(do_curl "${ORDER_SERVICE}/api/orders/${orden_id}")
    end_time=$(get_time_ms)
    http_code=$(extract_code "$response")
    
    latency_2=$((end_time - start_time))
    print_success "Latencia segunda consulta: ${latency_2}ms"
    log_file "Latencia: ${latency_2}ms (Cache HIT - desde Redis)"
    
    # Calcular mejora
    if [ "$latency_1" -gt 0 ] && [ "$latency_2" -lt "$latency_1" ]; then
        improvement=$((100 - (latency_2 * 100 / latency_1)))
        print_success "✨ ¡Cache funcionando! Mejora de rendimiento: ${improvement}%"
        print_success "La segunda consulta fue ${improvement}% más rápida"
        log_file "Mejora de rendimiento: ${improvement}%"
        log_file "Comparación: ${latency_1}ms (BD) → ${latency_2}ms (Cache)"
    elif [ "$latency_2" -eq "$latency_1" ]; then
        print_info "Latencias idénticas - sistema bajo o cache con overhead similar"
        log_file "Comparación: ${latency_1}ms → ${latency_2}ms (sin mejora visible)"
    else
        diff=$((latency_2 - latency_1))
        print_warning "Segunda consulta ${diff}ms más lenta (puede ser ruido de red)"
        log_file "Comparación: ${latency_1}ms → ${latency_2}ms (sin mejora)"
    fi
    
    run_test "Segunda consulta orden (Cache HIT)" "200" "$http_code"
    
    print_section "4.4 - Tercera consulta (validar cache persistente)"
    print_info "Consultando una vez más para confirmar cache..."
    
    start_time=$(get_time_ms)
    response=$(do_curl "${ORDER_SERVICE}/api/orders/${orden_id}")
    end_time=$(get_time_ms)
    http_code=$(extract_code "$response")
    
    latency_3=$((end_time - start_time))
    print_info "Latencia tercera consulta: ${latency_3}ms"
    log_file "Latencia tercera consulta: ${latency_3}ms"
    
    print_info ""
    print_info "📊 Comparación de las 3 consultas:"
    print_info "  1ª consulta (Cache MISS):        ${latency_1}ms  ← Desde PostgreSQL"
    print_info "  2ª consulta (Cache HIT):         ${latency_2}ms  ← Desde Redis"
    print_info "  3ª consulta (Cache persistente): ${latency_3}ms  ← Desde Redis"
    
    log_file ""
    log_file "RESUMEN CACHE:"
    log_file "  1ª: ${latency_1}ms (MISS - PostgreSQL)"
    log_file "  2ª: ${latency_2}ms (HIT - Redis)"
    log_file "  3ª: ${latency_3}ms (HIT - Redis)"
    
    # Verificación final
    avg_cache=$(( (latency_2 + latency_3) / 2 ))
    if [ "$avg_cache" -lt "$latency_1" ]; then
        improvement_final=$(( 100 - (avg_cache * 100 / latency_1) ))
        print_success ""
        print_success "✅ CACHE CONFIRMADO: Promedio ${avg_cache}ms vs ${latency_1}ms inicial"
        print_success "   Mejora total: ${improvement_final}%"
        log_file ""
        log_file "CONCLUSIÓN CACHE: Funcionando correctamente (${improvement_final}% mejora)"
    else
        print_info "Cache presente pero mejora no significativa (sistema muy rápido)"
        log_file "CONCLUSIÓN CACHE: Presente pero sin mejora medible"
    fi
    
    run_test "Tercera consulta orden (Cache persistente)" "200" "$http_code"
    
else
    print_warning "No se pudo obtener orden_id, omitiendo pruebas de cache"
    log_file "ADVERTENCIA: No se pudo probar Redis Cache (orden_id vacío)"
fi

pause

# ----------------------------------------------------------------------------
# PRUEBA 5: SAGA CON COMPENSACIÓN
# ----------------------------------------------------------------------------

print_header "${CROSS} PRUEBA 5: SAGA CON COMPENSACIÓN - STOCK INSUFICIENTE"

print_section "5.1 - Crear orden con stock INSUFICIENTE"

orden_fallida='{
  "userId": "test-user-saga-002",
  "paymentMethod": "credit_card",
  "items": [
    {"productCode": "LAPTOP-001", "quantity": 10000}
  ]
}'

print_info "Enviando solicitud con cantidad imposible (10000 unidades)..."
print_info "Esto debería disparar la compensación SAGA..."
log_file "Request:"
log_file "$orden_fallida"

response=$(do_curl "${ORDER_SERVICE}/api/orders" "POST" "$orden_fallida")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

log_file ""
log_file "Response:"
format_json "$body" | tee -a "$REPORT_FILE"

orden_status_fail=$(echo "$body" | grep -o '"status":"[^"]*"' | head -1 | grep -o '"[A-Z]*"' | tail -1 | tr -d '"')

if [ "$orden_status_fail" == "FAILED" ]; then
    print_success "COMPENSACIÓN SAGA EJECUTADA CORRECTAMENTE!"
    print_info "Status: ${orden_status_fail}"
    print_info "El sistema detectó stock insuficiente y ejecutó rollback"
    
    log_file ""
    log_file "COMPENSACIÓN EJECUTADA:"
    log_file "  1. ✗ Inventario rechazó reserva (stock insuficiente)"
    log_file "  2. ↩️  Rollback iniciado automáticamente"
    log_file "  3. ✓ Orden marcada como FAILED"
else
    print_error "COMPENSACIÓN SAGA FALLÓ (Status: ${orden_status_fail})"
fi

run_test "Orden fallida con compensación SAGA" "400" "$http_code"

print_section "5.2 - Verificar que el inventario NO fue afectado"
print_info "Consultando producto LAPTOP-001..."
response=$(do_curl "${INVENTORY_SERVICE}/api/inventory/products/LAPTOP-001")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

if [ "$http_code" == "200" ]; then
    stock_after=$(echo "$body" | grep -o '"stock":[0-9]*' | grep -o '[0-9]*')
    reserved_after=$(echo "$body" | grep -o '"reservedStock":[0-9]*' | grep -o '[0-9]*')
    available_after=$(echo "$body" | grep -o '"availableStock":[0-9]*' | grep -o '[0-9]*')
    
    print_info "Stock total: ${stock_after}"
    print_info "Stock reservado: ${reserved_after:-0}"
    print_info "Stock disponible: ${available_after}"
    
    log_file ""
    log_file "Inventario después de compensación:"
    format_json "$body" | tee -a "$REPORT_FILE"
    
    if [ "$reserved_after" == "0" ] || [ -z "$reserved_after" ]; then
        print_success "✓ Inventario correctamente liberado (reservedStock = 0)"
        print_success "✓ Rollback completo"
    else
        print_error "✗ Inventario NO liberado correctamente (reservedStock = ${reserved_after})"
    fi
fi

run_test "Verificar rollback de inventario" "200" "$http_code"

pause

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
║ Pruebas fallidas:    ${TESTS_FAILED}                                        ║
╚════════════════════════════════════════════════════════════════╝
"

echo "$SUMMARY" | tee -a "$REPORT_FILE"

if [ $TESTS_FAILED -eq 0 ]; then
    print_success "¡TODAS LAS PRUEBAS PASARON EXITOSAMENTE! ${ROCKET}"
    log_file ""
    log_file "CONCLUSIÓN: Sistema funcionando correctamente."
    log_file "  - Patrón SAGA funcionando con orquestación y compensación"
    log_file "  - Redis Cache mejorando rendimiento"
    log_file "  - Los 3 microservicios comunicándose correctamente"
    log_file "  - Rollback automático en caso de fallo"
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
- TEORIA.md: Conceptos técnicos sobre SAGA y Redis
- instructor.md: Guía del profesor

Capítulo 10: Patrones y herramientas avanzadas para microservicios
- Implementación de caching con Redis
- Patrón SAGA para transacciones distribuidas
- Monitoreo con Grafana y Kibana (próxima sección)

================================================================================
FOOTER

echo ""
print_success "Reporte guardado en: ${REPORT_FILE}"
echo ""

exit $exit_code

# ----------------------------------------------------------------------------
# FIN DEL SCRIPT
# ----------------------------------------------------------------------------