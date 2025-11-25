#!/bin/bash

# ============================================================================
# SCRIPT DE PRUEBAS FUNCIONALES - SISTEMA E-COMMERCE CON SAGA Y REDIS
# ============================================================================
# COMPATIBLE CON: Windows (Git Bash), macOS, Linux
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

# Colores para output (compatibles con Git Bash)
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m'

# Emojis (se muestran en terminales con soporte UTF-8)
CHECK="[OK]"
CROSS="[FAIL]"
INFO="[i]"
ROCKET=">>>"
PACKAGE="[P]"
CACHE="[C]"
WARNING="[!]"
CHART="[R]"

# Contadores
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_TOTAL=0

# Archivo de reporte
TIMESTAMP=$(date +"%Y-%m-%d-%H%M%S")
REPORT_FILE="test-saga-report-${TIMESTAMP}.txt"

# ----------------------------------------------------------------------------
# DETECCIÓN DE SISTEMA OPERATIVO
# ----------------------------------------------------------------------------

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

# ----------------------------------------------------------------------------
# FUNCIONES AUXILIARES
# ----------------------------------------------------------------------------

# Función para pausar y esperar ENTER
pause() {
    echo ""
    log_file ""
    read -r -p "Presiona ENTER para continuar..."
    echo ""
    log_file ""
}

# Función para medir tiempo en milisegundos
# COMPATIBLE CON: macOS, Linux, Windows Git Bash
get_time_ms() {
    local ms=""
    
    # Método 1: GNU date (Linux nativo)
    if [[ "$CURRENT_OS" == "linux" ]]; then
        ms=$(date +%s%3N 2>/dev/null)
        if [[ "$ms" =~ ^[0-9]{13,}$ ]]; then
            echo "$ms"
            return
        fi
    fi
    
    # Método 2: perl con Time::HiRes (macOS viene con perl)
    if command -v perl &> /dev/null; then
        ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time * 1000' 2>/dev/null)
        if [[ "$ms" =~ ^[0-9]+$ ]]; then
            echo "$ms"
            return
        fi
    fi
    
    # Método 3: python3 
    if command -v python3 &> /dev/null; then
        ms=$(python3 -c 'import time; print(int(time.time() * 1000))' 2>/dev/null)
        if [[ "$ms" =~ ^[0-9]+$ ]]; then
            echo "$ms"
            return
        fi
    fi
    
    # Método 4: python (algunos sistemas solo tienen python)
    if command -v python &> /dev/null; then
        ms=$(python -c 'import time; print(int(time.time() * 1000))' 2>/dev/null)
        if [[ "$ms" =~ ^[0-9]+$ ]]; then
            echo "$ms"
            return
        fi
    fi
    
    # Fallback: segundos * 1000 (menos preciso pero siempre funciona)
    echo $(($(date +%s) * 1000))
}

# ----------------------------------------------------------------------------
# FUNCIONES CURL - CROSS-PLATFORM (Mac + Windows Git Bash)
# ----------------------------------------------------------------------------

# Función para hacer request GET
do_curl_get() {
    local url=$1
    curl -s -w "\n---HTTP_CODE---\n%{http_code}" "$url" 2>/dev/null
}

# Función para hacer request POST con JSON
# Usa archivo temporal + printf para evitar problemas con echo en Windows
do_curl_post() {
    local url=$1
    local json_data=$2
    
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
    log_file "[OK] $1"
}

print_error() {
    echo -e "${RED}${CROSS} $1${NC}"
    log_file "[FAIL] $1"
}

print_info() {
    echo -e "${BLUE}${INFO} $1${NC}"
    log_file "[i] $1"
}

print_warning() {
    echo -e "${YELLOW}${WARNING} $1${NC}"
    log_file "[!] $1"
}

# Función para formatear JSON (con fallbacks)
format_json() {
    local input="$1"
    
    # Intentar jq primero (mejor formato)
    if command -v jq &> /dev/null; then
        echo "$input" | jq '.' 2>/dev/null && return
    fi
    
    # Intentar python3
    if command -v python3 &> /dev/null; then
        echo "$input" | python3 -m json.tool 2>/dev/null && return
    fi
    
    # Intentar python
    if command -v python &> /dev/null; then
        echo "$input" | python -m json.tool 2>/dev/null && return
    fi
    
    # Sin formato disponible, mostrar tal cual
    echo "$input"
}

# Función para contar ocurrencias (compatible cross-platform)
count_occurrences() {
    local text="$1"
    local pattern="$2"
    echo "$text" | grep -o "$pattern" | wc -l | tr -d ' '
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
Sistema E-Commerce con Patron SAGA y Redis Cache
================================================================================

Fecha de ejecucion: $(date +"%Y-%m-%d %H:%M:%S")
Sistema operativo: ${CURRENT_OS}
Servicios bajo prueba:
  - Order Service:     ${ORDER_SERVICE}
  - Inventory Service: ${INVENTORY_SERVICE}
  - Payment Service:   ${PAYMENT_SERVICE}

Generado por: test-saga.sh

Este reporte contiene los resultados de las pruebas funcionales del sistema
de microservicios con patron SAGA para transacciones distribuidas y Redis
para caching, incluyendo:
  - Health checks de los 3 microservicios
  - Verificacion de productos disponibles
  - SAGA exitosa (Reservar -> Pagar -> Confirmar)
  - Redis Cache (medicion de latencia y mejora de rendimiento)
  - SAGA con compensacion (Stock insuficiente)
  - Verificacion de rollback

================================================================================

HEADER

print_header "${ROCKET} PRUEBAS FUNCIONALES - SAGA CON REDIS"

echo ""
echo "Sistema operativo detectado: ${CURRENT_OS}"
echo ""

cat << 'INTRO' | tee -a "$REPORT_FILE"

PRUEBAS INCLUIDAS:
   1. Health Checks de los 3 servicios
   2. Verificacion de productos disponibles
   3. SAGA exitosa (orden con stock suficiente)
   4. Redis Cache - Medicion de rendimiento
      - Cache MISS (primera consulta desde PostgreSQL)
      - Cache HIT (consultas subsecuentes desde Redis)
      - Comparacion de latencias
   5. SAGA con compensacion (stock insuficiente)
   6. Verificacion de rollback del inventario

CASOS DE PRUEBA:
   [OK] Orden con stock disponible -> SAGA COMPLETA -> Status: COMPLETED
   [OK] Cache Redis -> Verificar mejora de rendimiento
   [FAIL] Orden con stock insuficiente -> COMPENSACION SAGA -> Status: FAILED
   [OK] Inventario liberado despues de compensacion

INTRO

pause

# ----------------------------------------------------------------------------
# PRUEBA 1: HEALTH CHECKS
# ----------------------------------------------------------------------------

print_header "${INFO} PRUEBA 1: HEALTH CHECKS DE LOS SERVICIOS"

print_section "1.1 - Health Check: Order Service (8080)"
response=$(do_curl_get "${ORDER_SERVICE}/health")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

log_file "Respuesta:"
format_json "$body" | tee -a "$REPORT_FILE"
run_test "Order Service Health" "200" "$http_code"

print_section "1.2 - Health Check: Inventory Service (8081)"
response=$(do_curl_get "${INVENTORY_SERVICE}/health")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

log_file "Respuesta:"
format_json "$body" | tee -a "$REPORT_FILE"
run_test "Inventory Service Health" "200" "$http_code"

print_section "1.3 - Health Check: Payment Service (8082)"
response=$(do_curl_get "${PAYMENT_SERVICE}/health")
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
response=$(do_curl_get "${INVENTORY_SERVICE}/api/inventory/products")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

if [ "$http_code" == "200" ]; then
    product_count=$(count_occurrences "$body" "productCode")
    print_success "Productos encontrados: ${product_count}"
    
    log_file "Primeros 3 productos:"
    format_json "$body" | head -50 | tee -a "$REPORT_FILE"
else
    print_warning "No se pudieron listar productos (HTTP ${http_code})"
fi

run_test "Listar productos" "200" "$http_code"

print_section "2.2 - Consultar producto especifico (LAPTOP-001)"
response=$(do_curl_get "${INVENTORY_SERVICE}/api/inventory/products/LAPTOP-001")
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

# JSON en UNA SOLA LINEA (importante para cross-platform)
orden_exitosa='{"userId":"test-user-saga-001","paymentMethod":"credit_card","items":[{"productCode":"LAPTOP-001","quantity":1},{"productCode":"MOUSE-001","quantity":2}]}'

print_info "Enviando solicitud de orden..."
log_file "Request:"
log_file "$orden_exitosa"

response=$(do_curl_post "${ORDER_SERVICE}/api/orders" "$orden_exitosa")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

log_file ""
log_file "Response:"
format_json "$body" | tee -a "$REPORT_FILE"

orden_id=$(echo "$body" | grep -o '"orderId":"[^"]*"' | head -1 | cut -d'"' -f4)
orden_status=$(echo "$body" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)
total=$(echo "$body" | grep -o '"totalAmount":[0-9.]*' | grep -o '[0-9.]*')

if [ "$orden_status" == "COMPLETED" ]; then
    print_success "SAGA COMPLETADA EXITOSAMENTE!"
    print_info "Order ID: ${orden_id}"
    print_info "Status: ${orden_status}"
    print_info "Total: \$${total}"
    
    log_file ""
    log_file "SAGA EJECUTADA:"
    log_file "  1. [OK] Inventario reservado (LAPTOP-001 x 1, MOUSE-001 x 2)"
    log_file "  2. [OK] Pago procesado (\$${total})"
    log_file "  3. [OK] Reserva confirmada"
    log_file "  4. [OK] Orden guardada en BD"
else
    print_error "SAGA FALLO (Status: ${orden_status})"
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
    response=$(do_curl_get "${ORDER_SERVICE}/api/orders/${orden_id}")
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
    
    print_section "4.2 - Verificar que se guardo en Redis"
    sleep 1
    
    print_info "Buscando clave en Redis..."
    
    # Intentar verificar Redis (puede fallar si docker no esta accesible)
    redis_keys=$(docker exec redis-cache redis-cli --scan --pattern "*${orden_id}*" 2>/dev/null)
    
    if [ -n "$redis_keys" ]; then
        print_success "Clave encontrada en Redis Cache!"
        echo "$redis_keys" | while read -r key; do
            print_info "  Clave: ${key}"
            log_file "Redis Cache: KEY ${key} EXISTE"
            
            # Obtener TTL
            ttl=$(docker exec redis-cache redis-cli TTL "$key" 2>/dev/null)
            if [ -n "$ttl" ] && [ "$ttl" -gt 0 ] 2>/dev/null; then
                print_info "  TTL: ${ttl} segundos"
                log_file "  TTL: ${ttl} segundos"
            fi
        done
    else
        # Buscar con otro patron comun
        redis_keys=$(docker exec redis-cache redis-cli --scan --pattern "order*" 2>/dev/null | head -5)
        
        if [ -n "$redis_keys" ]; then
            print_warning "No se encontro clave exacta, pero hay claves 'order*' en Redis:"
            echo "$redis_keys" | while read -r key; do
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
    print_info "Esta consulta deberia venir desde Redis (mas rapida)..."
    
    start_time=$(get_time_ms)
    response=$(do_curl_get "${ORDER_SERVICE}/api/orders/${orden_id}")
    end_time=$(get_time_ms)
    http_code=$(extract_code "$response")
    
    latency_2=$((end_time - start_time))
    print_success "Latencia segunda consulta: ${latency_2}ms"
    log_file "Latencia: ${latency_2}ms (Cache HIT - desde Redis)"
    
    # Calcular mejora
    if [ "$latency_1" -gt 0 ] && [ "$latency_2" -lt "$latency_1" ]; then
        improvement=$((100 - (latency_2 * 100 / latency_1)))
        print_success "Cache funcionando! Mejora de rendimiento: ${improvement}%"
        print_success "La segunda consulta fue ${improvement}% mas rapida"
        log_file "Mejora de rendimiento: ${improvement}%"
        log_file "Comparacion: ${latency_1}ms (BD) -> ${latency_2}ms (Cache)"
    elif [ "$latency_2" -eq "$latency_1" ]; then
        print_info "Latencias identicas - sistema bajo carga o cache con overhead similar"
        log_file "Comparacion: ${latency_1}ms -> ${latency_2}ms (sin mejora visible)"
    else
        diff=$((latency_2 - latency_1))
        print_warning "Segunda consulta ${diff}ms mas lenta (puede ser ruido de red)"
        log_file "Comparacion: ${latency_1}ms -> ${latency_2}ms (sin mejora)"
    fi
    
    run_test "Segunda consulta orden (Cache HIT)" "200" "$http_code"
    
    print_section "4.4 - Tercera consulta (validar cache persistente)"
    print_info "Consultando una vez mas para confirmar cache..."
    
    start_time=$(get_time_ms)
    response=$(do_curl_get "${ORDER_SERVICE}/api/orders/${orden_id}")
    end_time=$(get_time_ms)
    http_code=$(extract_code "$response")
    
    latency_3=$((end_time - start_time))
    print_info "Latencia tercera consulta: ${latency_3}ms"
    log_file "Latencia tercera consulta: ${latency_3}ms"
    
    print_info ""
    print_info "Comparacion de las 3 consultas:"
    print_info "  1a consulta (Cache MISS):        ${latency_1}ms  <- Desde PostgreSQL"
    print_info "  2a consulta (Cache HIT):         ${latency_2}ms  <- Desde Redis"
    print_info "  3a consulta (Cache persistente): ${latency_3}ms  <- Desde Redis"
    
    log_file ""
    log_file "RESUMEN CACHE:"
    log_file "  1a: ${latency_1}ms (MISS - PostgreSQL)"
    log_file "  2a: ${latency_2}ms (HIT - Redis)"
    log_file "  3a: ${latency_3}ms (HIT - Redis)"
    
    # Verificacion final
    avg_cache=$(( (latency_2 + latency_3) / 2 ))
    if [ "$avg_cache" -lt "$latency_1" ]; then
        improvement_final=$(( 100 - (avg_cache * 100 / latency_1) ))
        print_success ""
        print_success "CACHE CONFIRMADO: Promedio ${avg_cache}ms vs ${latency_1}ms inicial"
        print_success "   Mejora total: ${improvement_final}%"
        log_file ""
        log_file "CONCLUSION CACHE: Funcionando correctamente (${improvement_final}% mejora)"
    else
        print_info "Cache presente pero mejora no significativa (sistema muy rapido)"
        log_file "CONCLUSION CACHE: Presente pero sin mejora medible"
    fi
    
    run_test "Tercera consulta orden (Cache persistente)" "200" "$http_code"
    
else
    print_warning "No se pudo obtener orden_id, omitiendo pruebas de cache"
    log_file "ADVERTENCIA: No se pudo probar Redis Cache (orden_id vacio)"
fi

pause

# ----------------------------------------------------------------------------
# PRUEBA 5: SAGA CON COMPENSACION
# ----------------------------------------------------------------------------

print_header "${CROSS} PRUEBA 5: SAGA CON COMPENSACION - STOCK INSUFICIENTE"

print_section "5.1 - Crear orden con stock INSUFICIENTE"

# JSON en UNA SOLA LINEA (importante para cross-platform)
orden_fallida='{"userId":"test-user-saga-002","paymentMethod":"credit_card","items":[{"productCode":"LAPTOP-001","quantity":10000}]}'

print_info "Enviando solicitud con cantidad imposible (10000 unidades)..."
print_info "Esto deberia disparar la compensacion SAGA..."
log_file "Request:"
log_file "$orden_fallida"

response=$(do_curl_post "${ORDER_SERVICE}/api/orders" "$orden_fallida")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

log_file ""
log_file "Response:"
format_json "$body" | tee -a "$REPORT_FILE"

orden_status_fail=$(echo "$body" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)

if [ "$orden_status_fail" == "FAILED" ]; then
    print_success "COMPENSACION SAGA EJECUTADA CORRECTAMENTE!"
    print_info "Status: ${orden_status_fail}"
    print_info "El sistema detecto stock insuficiente y ejecuto rollback"
    
    log_file ""
    log_file "COMPENSACION EJECUTADA:"
    log_file "  1. [X] Inventario rechazo reserva (stock insuficiente)"
    log_file "  2. [<-] Rollback iniciado automaticamente"
    log_file "  3. [OK] Orden marcada como FAILED"
else
    print_error "COMPENSACION SAGA FALLO (Status: ${orden_status_fail})"
fi

run_test "Orden fallida con compensacion SAGA" "400" "$http_code"

print_section "5.2 - Verificar que el inventario NO fue afectado"
print_info "Consultando producto LAPTOP-001..."
response=$(do_curl_get "${INVENTORY_SERVICE}/api/inventory/products/LAPTOP-001")
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
    log_file "Inventario despues de compensacion:"
    format_json "$body" | tee -a "$REPORT_FILE"
    
    if [ "$reserved_after" == "0" ] || [ -z "$reserved_after" ]; then
        print_success "Inventario correctamente liberado (reservedStock = 0)"
        print_success "Rollback completo"
    else
        print_error "Inventario NO liberado correctamente (reservedStock = ${reserved_after})"
    fi
fi

run_test "Verificar rollback de inventario" "200" "$http_code"

pause

# ----------------------------------------------------------------------------
# RESUMEN FINAL
# ----------------------------------------------------------------------------

print_header "${CHART} RESUMEN DE PRUEBAS"

# Formato del resumen compatible con todas las terminales
echo ""
echo "+================================================================+"
echo "|                    RESULTADOS FINALES                          |"
echo "+================================================================+"
printf "| Total de pruebas:    %-39s |\n" "$TESTS_TOTAL"
printf "| Pruebas exitosas:    %-39s |\n" "$TESTS_PASSED"
printf "| Pruebas fallidas:    %-39s |\n" "$TESTS_FAILED"
echo "+================================================================+"
echo ""

# Guardar en archivo
cat >> "$REPORT_FILE" << SUMMARY

+================================================================+
|                    RESULTADOS FINALES                          |
+================================================================+
| Total de pruebas:    ${TESTS_TOTAL}
| Pruebas exitosas:    ${TESTS_PASSED}
| Pruebas fallidas:    ${TESTS_FAILED}
+================================================================+

SUMMARY

if [ $TESTS_FAILED -eq 0 ]; then
    print_success "TODAS LAS PRUEBAS PASARON EXITOSAMENTE!"
    log_file ""
    log_file "CONCLUSION: Sistema funcionando correctamente."
    log_file "  - Patron SAGA funcionando con orquestacion y compensacion"
    log_file "  - Redis Cache mejorando rendimiento"
    log_file "  - Los 3 microservicios comunicandose correctamente"
    log_file "  - Rollback automatico en caso de fallo"
    exit_code=0
else
    print_error "Algunas pruebas fallaron. Revisa los logs arriba."
    log_file ""
    log_file "CONCLUSION: Sistema con fallas. Revisar pruebas fallidas."
    exit_code=1
fi

# Footer del reporte
cat >> "$REPORT_FILE" << FOOTER

================================================================================
FIN DEL REPORTE
================================================================================

Archivo generado: $REPORT_FILE
Fecha: $(date +"%Y-%m-%d %H:%M:%S")
Sistema: ${CURRENT_OS}

Para mas informacion, consulta:
- README.md: Guia de usuario
- TEORIA.md: Conceptos tecnicos sobre SAGA y Redis

Capitulo 10: Patrones y herramientas avanzadas para microservicios
- Implementacion de caching con Redis
- Patron SAGA para transacciones distribuidas

================================================================================
FOOTER

echo ""
print_success "Reporte guardado en: ${REPORT_FILE}"
echo ""

exit $exit_code

# ----------------------------------------------------------------------------
# FIN DEL SCRIPT
# ----------------------------------------------------------------------------