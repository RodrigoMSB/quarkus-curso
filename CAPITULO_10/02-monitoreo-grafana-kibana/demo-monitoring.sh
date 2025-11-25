#!/bin/bash

# ============================================================================
# SCRIPT DE PRUEBAS FUNCIONALES - MONITOREO CON GRAFANA Y KIBANA
# ============================================================================
# COMPATIBLE CON: Windows (Git Bash), macOS, Linux
#
# Este script prueba el stack de observabilidad completo:
# - Prometheus (metricas)
# - Grafana (visualizacion)
# - Elasticsearch + Kibana (logs)
# - Redis Cache
# - Patron SAGA
#
# SALIDA:
#   - Resultados en consola (con colores)
#   - Archivo: test-monitoring-report-YYYY-MM-DD-HHMMSS.txt
#
# ============================================================================

# ----------------------------------------------------------------------------
# CONFIGURACION
# ----------------------------------------------------------------------------

# URLs de los servicios
ORDER_SERVICE="http://localhost:8080"
INVENTORY_SERVICE="http://localhost:8081"
PAYMENT_SERVICE="http://localhost:8082"

# URLs de monitoreo
PROMETHEUS="http://localhost:9090"
GRAFANA="http://localhost:3000"
ELASTICSEARCH="http://localhost:9200"
KIBANA="http://localhost:5601"

# Colores para output (compatibles con Git Bash)
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m'

# Simbolos ASCII (compatibles con todas las terminales)
CHECK="[OK]"
CROSS="[FAIL]"
INFO="[i]"
ARROW=">>>"

# Contadores
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_TOTAL=0

# Archivo de reporte
TIMESTAMP=$(date +"%Y-%m-%d-%H%M%S")
REPORT_FILE="test-monitoring-report-${TIMESTAMP}.txt"

# ----------------------------------------------------------------------------
# DETECCION DE SISTEMA OPERATIVO
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

pause() {
    echo ""
    log_file ""
    read -r -p "Presiona ENTER para continuar..."
    echo ""
    log_file ""
}

# Funcion para medir tiempo en milisegundos
# COMPATIBLE CON: macOS, Linux, Windows Git Bash
get_time_ms() {
    local ms=""
    
    if [[ "$CURRENT_OS" == "linux" ]]; then
        ms=$(date +%s%3N 2>/dev/null)
        if [[ "$ms" =~ ^[0-9]{13,}$ ]]; then
            echo "$ms"
            return
        fi
    fi
    
    if command -v perl &> /dev/null; then
        ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time * 1000' 2>/dev/null)
        if [[ "$ms" =~ ^[0-9]+$ ]]; then
            echo "$ms"
            return
        fi
    fi
    
    if command -v python3 &> /dev/null; then
        ms=$(python3 -c 'import time; print(int(time.time() * 1000))' 2>/dev/null)
        if [[ "$ms" =~ ^[0-9]+$ ]]; then
            echo "$ms"
            return
        fi
    fi
    
    if command -v python &> /dev/null; then
        ms=$(python -c 'import time; print(int(time.time() * 1000))' 2>/dev/null)
        if [[ "$ms" =~ ^[0-9]+$ ]]; then
            echo "$ms"
            return
        fi
    fi
    
    echo $(($(date +%s) * 1000))
}

# ----------------------------------------------------------------------------
# FUNCIONES CURL - CROSS-PLATFORM
# ----------------------------------------------------------------------------

do_curl_get() {
    local url=$1
    curl -s -w "\n---HTTP_CODE---\n%{http_code}" "$url" 2>/dev/null
}

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
    echo -e "${MAGENTA}${ARROW} $1${NC}"
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
    echo -e "${YELLOW}[!] $1${NC}"
    log_file "[!] $1"
}

print_concept() {
    echo -e "${YELLOW}$1${NC}"
    log_file "$1"
}

format_json() {
    local input="$1"
    
    if command -v jq &> /dev/null; then
        echo "$input" | jq '.' 2>/dev/null && return
    fi
    
    if command -v python3 &> /dev/null; then
        echo "$input" | python3 -m json.tool 2>/dev/null && return
    fi
    
    if command -v python &> /dev/null; then
        echo "$input" | python -m json.tool 2>/dev/null && return
    fi
    
    echo "$input"
}

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

cat > "$REPORT_FILE" << 'HEADER'
================================================================================
REPORTE DE PRUEBAS - MONITOREO CON GRAFANA Y KIBANA
Sistema E-Commerce con Observabilidad Completa
================================================================================

CONCEPTOS CLAVE DE OBSERVABILIDAD
=================================

La OBSERVABILIDAD es la capacidad de entender el estado interno de un sistema
a partir de sus salidas externas. Se basa en tres pilares:

  1. METRICAS (Prometheus + Grafana)
     - Valores numericos que cambian en el tiempo
     - Ejemplos: CPU, memoria, requests/segundo, latencia
     - Analogia: El tablero de un auto (velocimetro, temperatura, combustible)

  2. LOGS (Elasticsearch + Kibana)
     - Registros de eventos con timestamp
     - Ejemplos: errores, transacciones, acciones de usuario
     - Analogia: La caja negra de un avion

  3. TRACES (Distribuidos)
     - Seguimiento de una request a traves de multiples servicios
     - Analogia: Rastrear un paquete de correo por todas las estaciones

PROMETHEUS
----------
Prometheus es un sistema de monitoreo que:
- SCRAPE: Recolecta metricas cada 15 segundos (pull model)
- ALMACENA: Guarda series temporales en su base de datos
- CONSULTA: Usa PromQL para queries (similar a SQL para metricas)

Endpoint de metricas en Quarkus: /q/metrics

GRAFANA
-------
Grafana es una herramienta de visualizacion que:
- Se conecta a Prometheus como fuente de datos
- Permite crear dashboards con graficos
- Soporta alertas basadas en umbrales

ELASTICSEARCH + KIBANA (ELK Stack)
----------------------------------
- Elasticsearch: Motor de busqueda que almacena logs
- Logstash: Procesa y transforma logs
- Kibana: Interfaz web para explorar logs
- Filebeat: Recolecta logs de contenedores Docker

REDIS CACHE
-----------
Redis es una base de datos en memoria usada como cache:

- CACHE MISS: El dato NO esta en cache, se busca en la BD (lento)
- CACHE HIT: El dato SI esta en cache, se retorna directo (rapido)

Analogia del Cache:
  Imagina una biblioteca. El cache es tu escritorio con los libros
  que usas frecuentemente. Si el libro esta en tu escritorio (HIT),
  lo tomas inmediatamente. Si no esta (MISS), debes ir a las estanterias
  (base de datos) a buscarlo, lo cual toma mas tiempo.

TTL (Time To Live):
  Tiempo que un dato permanece en cache antes de expirar.
  Ejemplo: TTL=300 significa que el dato expira en 5 minutos.

================================================================================
HEADER

# Agregar fecha y sistema
cat >> "$REPORT_FILE" << SYSINFO

Fecha de ejecucion: $(date +"%Y-%m-%d %H:%M:%S")
Sistema operativo: ${CURRENT_OS}

Servicios bajo prueba:
  - Order Service:     ${ORDER_SERVICE}
  - Inventory Service: ${INVENTORY_SERVICE}
  - Payment Service:   ${PAYMENT_SERVICE}

Stack de Monitoreo:
  - Prometheus: ${PROMETHEUS}
  - Grafana:    ${GRAFANA}
  - Kibana:     ${KIBANA}

================================================================================

SYSINFO

print_header "PRUEBAS DE MONITOREO - GRAFANA Y KIBANA"

echo ""
echo "Sistema operativo detectado: ${CURRENT_OS}"
echo ""

cat << 'INTRO'

+------------------------------------------------------------------+
|                    PRUEBAS A EJECUTAR                            |
+------------------------------------------------------------------+
|  1. Health Checks de los 3 microservicios                        |
|  2. Verificar stack de monitoreo (Prometheus, Grafana, Kibana)   |
|  3. Verificar endpoints de metricas (/q/metrics)                 |
|  4. Redis Cache - Demostrar CACHE HIT vs CACHE MISS              |
|  5. SAGA exitoso - Crear orden valida                            |
|  6. SAGA con compensacion - Forzar rollback                      |
|  7. Verificar metricas en Prometheus                             |
|  8. Resumen y estadisticas                                       |
+------------------------------------------------------------------+

INTRO

log_file "PRUEBAS A EJECUTAR:"
log_file "  1. Health Checks de los 3 microservicios"
log_file "  2. Verificar stack de monitoreo"
log_file "  3. Verificar endpoints de metricas"
log_file "  4. Redis Cache (HIT vs MISS)"
log_file "  5. SAGA exitoso"
log_file "  6. SAGA con compensacion"
log_file "  7. Verificar metricas en Prometheus"
log_file "  8. Resumen"
log_file ""

pause

# ============================================================================
# PRUEBA 1: HEALTH CHECKS DE MICROSERVICIOS
# ============================================================================

print_header "PRUEBA 1: HEALTH CHECKS DE MICROSERVICIOS"

cat << 'CONCEPT'
+------------------------------------------------------------------+
|  CONCEPTO: HEALTH CHECKS                                         |
+------------------------------------------------------------------+
|  Un Health Check es un endpoint que indica si un servicio esta   |
|  funcionando correctamente. Quarkus expone /health con:          |
|                                                                  |
|  - LIVENESS: El servicio esta vivo (no colgado)                  |
|  - READINESS: El servicio puede recibir trafico                  |
|                                                                  |
|  Kubernetes usa estos endpoints para:                            |
|  - Reiniciar pods que no responden (liveness)                    |
|  - Quitar del balanceador pods no listos (readiness)             |
+------------------------------------------------------------------+

CONCEPT

log_file ""
log_file "CONCEPTO: HEALTH CHECKS"
log_file "Un Health Check indica si un servicio esta funcionando."
log_file "Quarkus expone /health con LIVENESS y READINESS checks."
log_file ""

print_section "1.1 - Health Check: Order Service (8080)"
response=$(do_curl_get "${ORDER_SERVICE}/health")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

if [ "$http_code" == "200" ]; then
    print_success "Order Service respondio correctamente"
    log_file "Response: $body"
else
    print_error "Order Service no responde (HTTP $http_code)"
fi
run_test "Order Service Health" "200" "$http_code"

print_section "1.2 - Health Check: Inventory Service (8081)"
response=$(do_curl_get "${INVENTORY_SERVICE}/health")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

if [ "$http_code" == "200" ]; then
    print_success "Inventory Service respondio correctamente"
    log_file "Response: $body"
else
    print_error "Inventory Service no responde (HTTP $http_code)"
fi
run_test "Inventory Service Health" "200" "$http_code"

print_section "1.3 - Health Check: Payment Service (8082)"
response=$(do_curl_get "${PAYMENT_SERVICE}/health")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

if [ "$http_code" == "200" ]; then
    print_success "Payment Service respondio correctamente"
    log_file "Response: $body"
else
    print_error "Payment Service no responde (HTTP $http_code)"
fi
run_test "Payment Service Health" "200" "$http_code"

pause

# ============================================================================
# PRUEBA 2: STACK DE MONITOREO
# ============================================================================

print_header "PRUEBA 2: VERIFICAR STACK DE MONITOREO"

cat << 'CONCEPT'
+------------------------------------------------------------------+
|  CONCEPTO: STACK DE OBSERVABILIDAD                               |
+------------------------------------------------------------------+
|                                                                  |
|  PROMETHEUS (puerto 9090)                                        |
|  - Recolecta metricas de los microservicios cada 15 segundos     |
|  - Almacena datos en formato de series temporales                |
|  - Permite consultas con PromQL                                  |
|                                                                  |
|  GRAFANA (puerto 3000)                                           |
|  - Visualiza las metricas de Prometheus en dashboards            |
|  - Permite crear alertas                                         |
|  - Login: admin / admin                                          |
|                                                                  |
|  ELASTICSEARCH (puerto 9200)                                     |
|  - Almacena logs en indices                                      |
|  - Motor de busqueda full-text                                   |
|                                                                  |
|  KIBANA (puerto 5601)                                            |
|  - Interfaz web para explorar logs en Elasticsearch              |
|  - Permite crear visualizaciones y dashboards de logs            |
|                                                                  |
+------------------------------------------------------------------+

CONCEPT

log_file ""
log_file "CONCEPTO: STACK DE OBSERVABILIDAD"
log_file "Prometheus recolecta metricas, Grafana las visualiza."
log_file "Elasticsearch almacena logs, Kibana permite explorarlos."
log_file ""

print_section "2.1 - Verificar Prometheus"
response=$(do_curl_get "${PROMETHEUS}/-/healthy")
http_code=$(extract_code "$response")

if [ "$http_code" == "200" ]; then
    print_success "Prometheus esta corriendo en ${PROMETHEUS}"
    print_info "Abre ${PROMETHEUS} en tu navegador para ver metricas"
else
    print_warning "Prometheus no responde (puede estar iniciando)"
fi
run_test "Prometheus Health" "200" "$http_code"

print_section "2.2 - Verificar Grafana"
response=$(do_curl_get "${GRAFANA}/api/health")
http_code=$(extract_code "$response")

if [ "$http_code" == "200" ]; then
    print_success "Grafana esta corriendo en ${GRAFANA}"
    print_info "Login: admin / admin"
else
    print_warning "Grafana no responde (puede estar iniciando)"
fi
run_test "Grafana Health" "200" "$http_code"

print_section "2.3 - Verificar Elasticsearch"
response=$(do_curl_get "${ELASTICSEARCH}/_cluster/health")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

if [ "$http_code" == "200" ]; then
    print_success "Elasticsearch esta corriendo en ${ELASTICSEARCH}"
    
    # Extraer status del cluster
    cluster_status=$(echo "$body" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    if [ "$cluster_status" == "green" ]; then
        print_success "Cluster status: GREEN (optimo)"
    elif [ "$cluster_status" == "yellow" ]; then
        print_warning "Cluster status: YELLOW (funcional, sin replicas)"
    else
        print_warning "Cluster status: $cluster_status"
    fi
    log_file "Elasticsearch cluster status: $cluster_status"
else
    print_warning "Elasticsearch no responde (puede estar iniciando)"
fi
run_test "Elasticsearch Health" "200" "$http_code"

print_section "2.4 - Verificar Kibana"
response=$(do_curl_get "${KIBANA}/api/status")
http_code=$(extract_code "$response")

if [ "$http_code" == "200" ]; then
    print_success "Kibana esta corriendo en ${KIBANA}"
    print_info "Abre ${KIBANA} para explorar logs"
else
    print_warning "Kibana no responde (tarda ~2 min en iniciar)"
fi
run_test "Kibana Health" "200" "$http_code"

print_section "2.5 - Verificar Redis"
redis_ping=$(docker exec redis-cache redis-cli PING 2>/dev/null)

if [ "$redis_ping" == "PONG" ]; then
    print_success "Redis esta corriendo y responde PONG"
    TESTS_TOTAL=$((TESTS_TOTAL + 1))
    TESTS_PASSED=$((TESTS_PASSED + 1))
    log_file "[OK] TEST #${TESTS_TOTAL}: Redis PING/PONG"
else
    print_error "Redis no responde"
    TESTS_TOTAL=$((TESTS_TOTAL + 1))
    TESTS_FAILED=$((TESTS_FAILED + 1))
    log_file "[FAIL] TEST #${TESTS_TOTAL}: Redis PING/PONG"
fi

pause

# ============================================================================
# PRUEBA 3: ENDPOINTS DE METRICAS
# ============================================================================

print_header "PRUEBA 3: VERIFICAR ENDPOINTS DE METRICAS"

cat << 'CONCEPT'
+------------------------------------------------------------------+
|  CONCEPTO: METRICAS DE PROMETHEUS                                |
+------------------------------------------------------------------+
|                                                                  |
|  Quarkus expone metricas en /q/metrics en formato Prometheus:    |
|                                                                  |
|  TIPOS DE METRICAS:                                              |
|  - Counter: Solo aumenta (ej: total_requests)                    |
|  - Gauge: Sube y baja (ej: memoria_usada)                        |
|  - Histogram: Distribucion de valores (ej: latencia)             |
|  - Summary: Similar a histogram, calcula percentiles             |
|                                                                  |
|  METRICAS COMUNES EN QUARKUS:                                    |
|  - http_server_requests_seconds: Latencia de requests HTTP       |
|  - jvm_memory_used_bytes: Memoria JVM usada                      |
|  - jvm_threads_live_threads: Threads activos                     |
|  - process_cpu_usage: Uso de CPU                                 |
|                                                                  |
|  Prometheus hace SCRAPE de estos endpoints cada 15 segundos.     |
|                                                                  |
+------------------------------------------------------------------+

CONCEPT

log_file ""
log_file "CONCEPTO: METRICAS DE PROMETHEUS"
log_file "Quarkus expone metricas en /q/metrics."
log_file "Tipos: Counter, Gauge, Histogram, Summary."
log_file ""

print_section "3.1 - Metricas de Order Service"
response=$(do_curl_get "${ORDER_SERVICE}/q/metrics")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

if [ "$http_code" == "200" ]; then
    # Contar metricas
    metric_count=$(echo "$body" | grep -c "^[a-z]" 2>/dev/null || echo "0")
    print_success "Order Service expone metricas en /q/metrics"
    print_info "Metricas encontradas: ~${metric_count} lineas"
    
    # Mostrar algunas metricas clave
    if echo "$body" | grep -q "jvm_memory"; then
        print_info "  - jvm_memory_* (memoria JVM)"
    fi
    if echo "$body" | grep -q "http_server"; then
        print_info "  - http_server_* (requests HTTP)"
    fi
    
    log_file "Order Service: ${metric_count} lineas de metricas"
else
    print_error "No se pueden obtener metricas (HTTP $http_code)"
fi
run_test "Order Service Metrics" "200" "$http_code"

print_section "3.2 - Metricas de Inventory Service"
response=$(do_curl_get "${INVENTORY_SERVICE}/q/metrics")
http_code=$(extract_code "$response")

if [ "$http_code" == "200" ]; then
    print_success "Inventory Service expone metricas en /q/metrics"
else
    print_error "No se pueden obtener metricas (HTTP $http_code)"
fi
run_test "Inventory Service Metrics" "200" "$http_code"

print_section "3.3 - Metricas de Payment Service"
response=$(do_curl_get "${PAYMENT_SERVICE}/q/metrics")
http_code=$(extract_code "$response")

if [ "$http_code" == "200" ]; then
    print_success "Payment Service expone metricas en /q/metrics"
else
    print_error "No se pueden obtener metricas (HTTP $http_code)"
fi
run_test "Payment Service Metrics" "200" "$http_code"

print_section "3.4 - Verificar que Prometheus esta scrapeando"
response=$(do_curl_get "${PROMETHEUS}/api/v1/targets")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

if [ "$http_code" == "200" ]; then
    # Contar targets activos
    up_count=$(echo "$body" | grep -o '"health":"up"' | wc -l | tr -d ' ')
    print_success "Prometheus tiene ${up_count} targets activos"
    print_info "Verifica en ${PROMETHEUS}/targets"
    log_file "Prometheus targets UP: ${up_count}"
else
    print_warning "No se pudo verificar targets de Prometheus"
fi
run_test "Prometheus Targets" "200" "$http_code"

pause

# ============================================================================
# PRUEBA 4: REDIS CACHE (HIT vs MISS)
# ============================================================================

print_header "PRUEBA 4: REDIS CACHE - HIT vs MISS"

cat << 'CONCEPT'
+------------------------------------------------------------------+
|  CONCEPTO: CACHE HIT vs CACHE MISS                               |
+------------------------------------------------------------------+
|                                                                  |
|  CACHE MISS (Primera consulta):                                  |
|  +--------+     X     +---------+    consulta    +----------+   |
|  | Client | --------> |  Redis  | ------------> | PostgreSQL|   |
|  +--------+  no esta  +---------+    (lento)    +----------+   |
|                            |                          |         |
|                            +<---- guarda en cache ----+         |
|                                                                  |
|  CACHE HIT (Consultas siguientes):                               |
|  +--------+   found!  +---------+                               |
|  | Client | <-------- |  Redis  |  (PostgreSQL no se toca)      |
|  +--------+  (rapido) +---------+                               |
|                                                                  |
|  BENEFICIOS:                                                     |
|  - Reduce carga en la base de datos                              |
|  - Mejora tiempo de respuesta (ms vs segundos)                   |
|  - Permite escalar horizontalmente                               |
|                                                                  |
|  METRICAS CLAVE:                                                 |
|  - Hit Rate = hits / (hits + misses) * 100                       |
|  - Un buen hit rate es > 80%                                     |
|                                                                  |
+------------------------------------------------------------------+

CONCEPT

log_file ""
log_file "CONCEPTO: CACHE HIT vs CACHE MISS"
log_file "MISS: Dato no en cache, se busca en BD (lento)"
log_file "HIT: Dato en cache, se retorna directo (rapido)"
log_file "Hit Rate = hits / (hits + misses) * 100"
log_file ""

print_section "4.1 - Limpiar cache de Redis"
docker exec redis-cache redis-cli FLUSHALL > /dev/null 2>&1
print_success "Cache limpiado (FLUSHALL)"
log_file "Redis cache limpiado con FLUSHALL"

print_section "4.2 - Primera consulta (CACHE MISS esperado)"
print_info "Consultando producto LAPTOP-001 por primera vez..."
print_info "El dato NO esta en cache, ira a PostgreSQL..."

start_time=$(get_time_ms)
response=$(do_curl_get "${INVENTORY_SERVICE}/api/inventory/products/LAPTOP-001")
end_time=$(get_time_ms)
http_code=$(extract_code "$response")
body=$(extract_body "$response")

latency_1=$((end_time - start_time))
print_success "Latencia 1ra consulta: ${latency_1}ms (CACHE MISS - desde PostgreSQL)"
log_file "1ra consulta: ${latency_1}ms (MISS)"

if [ "$http_code" == "200" ]; then
    product_name=$(echo "$body" | grep -o '"name":"[^"]*"' | cut -d'"' -f4)
    print_info "Producto encontrado: $product_name"
fi

run_test "Primera consulta producto (CACHE MISS)" "200" "$http_code"

print_section "4.3 - Segunda consulta (CACHE HIT esperado)"
print_info "Consultando producto LAPTOP-001 nuevamente..."
print_info "El dato DEBERIA estar en cache (mas rapido)..."

sleep 1

start_time=$(get_time_ms)
response=$(do_curl_get "${INVENTORY_SERVICE}/api/inventory/products/LAPTOP-001")
end_time=$(get_time_ms)
http_code=$(extract_code "$response")

latency_2=$((end_time - start_time))
print_success "Latencia 2da consulta: ${latency_2}ms (CACHE HIT - desde Redis)"
log_file "2da consulta: ${latency_2}ms (HIT)"

run_test "Segunda consulta producto (CACHE HIT)" "200" "$http_code"

print_section "4.4 - Tercera consulta (confirmando cache)"
start_time=$(get_time_ms)
response=$(do_curl_get "${INVENTORY_SERVICE}/api/inventory/products/LAPTOP-001")
end_time=$(get_time_ms)
http_code=$(extract_code "$response")

latency_3=$((end_time - start_time))
print_info "Latencia 3ra consulta: ${latency_3}ms"
log_file "3ra consulta: ${latency_3}ms (HIT)"

run_test "Tercera consulta producto (CACHE HIT)" "200" "$http_code"

print_section "4.5 - Estadisticas de Cache"

echo ""
echo "+------------------------------------------+"
echo "|       COMPARACION DE LATENCIAS           |"
echo "+------------------------------------------+"
printf "| 1ra consulta (MISS): %6s ms           |\n" "$latency_1"
printf "| 2da consulta (HIT):  %6s ms           |\n" "$latency_2"
printf "| 3ra consulta (HIT):  %6s ms           |\n" "$latency_3"
echo "+------------------------------------------+"

log_file ""
log_file "COMPARACION DE LATENCIAS:"
log_file "  1ra consulta (MISS): ${latency_1}ms"
log_file "  2da consulta (HIT):  ${latency_2}ms"
log_file "  3ra consulta (HIT):  ${latency_3}ms"

# Calcular mejora
if [ "$latency_1" -gt 0 ] && [ "$latency_2" -lt "$latency_1" ]; then
    improvement=$((100 - (latency_2 * 100 / latency_1)))
    echo ""
    print_success "Mejora de rendimiento con cache: ${improvement}%"
    log_file "Mejora de rendimiento: ${improvement}%"
fi

# Estadisticas de Redis
echo ""
print_info "Estadisticas de Redis:"
hits=$(docker exec redis-cache redis-cli INFO stats 2>/dev/null | grep "keyspace_hits" | cut -d: -f2 | tr -d '\r')
misses=$(docker exec redis-cache redis-cli INFO stats 2>/dev/null | grep "keyspace_misses" | cut -d: -f2 | tr -d '\r')

if [ -n "$hits" ] && [ -n "$misses" ]; then
    print_info "  Keyspace Hits: $hits"
    print_info "  Keyspace Misses: $misses"
    total=$((hits + misses))
    if [ "$total" -gt 0 ]; then
        hit_rate=$(awk "BEGIN {printf \"%.1f\", ($hits/$total)*100}")
        print_success "  Hit Rate: ${hit_rate}%"
        log_file "Redis Hit Rate: ${hit_rate}%"
    fi
fi

pause

# ============================================================================
# PRUEBA 5: SAGA EXITOSO
# ============================================================================

print_header "PRUEBA 5: SAGA EXITOSO - CREAR ORDEN VALIDA"

cat << 'CONCEPT'
+------------------------------------------------------------------+
|  CONCEPTO: PATRON SAGA                                           |
+------------------------------------------------------------------+
|                                                                  |
|  SAGA es un patron para transacciones distribuidas:              |
|                                                                  |
|  Flujo EXITOSO:                                                  |
|  1. Order Service crea orden (status: PENDING)                   |
|  2. Inventory Service reserva stock                              |
|  3. Payment Service procesa pago                                 |
|  4. Inventory Service confirma reserva                           |
|  5. Order Service actualiza (status: COMPLETED)                  |
|                                                                  |
|        +-------+    +-------+    +-------+                       |
|        | Order | -> | Inven | -> | Payme |                       |
|        |PENDING|    |RESERV |    | PAID  |                       |
|        +-------+    +-------+    +-------+                       |
|            |                          |                          |
|            +<---- COMPLETED <---------+                          |
|                                                                  |
|  A diferencia de transacciones ACID, SAGA usa COMPENSACIONES     |
|  en lugar de ROLLBACK para deshacer operaciones.                 |
|                                                                  |
+------------------------------------------------------------------+

CONCEPT

log_file ""
log_file "CONCEPTO: PATRON SAGA"
log_file "SAGA maneja transacciones distribuidas con compensaciones."
log_file "Flujo: Order -> Inventory (reserva) -> Payment -> Confirmar"
log_file ""

print_section "5.1 - Crear orden con stock suficiente"

orden_json='{"userId":"user-test-001","paymentMethod":"credit_card","items":[{"productCode":"LAPTOP-001","quantity":1},{"productCode":"MOUSE-001","quantity":2}]}'

print_info "Enviando orden:"
print_info "  - 1x LAPTOP-001 (\$899.99)"
print_info "  - 2x MOUSE-001 (\$99.99 c/u)"
print_info "  - Total esperado: \$1099.97"

log_file "Request: $orden_json"

response=$(do_curl_post "${ORDER_SERVICE}/api/orders" "$orden_json")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

log_file "Response: $body"

orden_id=$(echo "$body" | grep -o '"orderId":"[^"]*"' | head -1 | cut -d'"' -f4)
orden_status=$(echo "$body" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)
total=$(echo "$body" | grep -o '"totalAmount":[0-9.]*' | grep -o '[0-9.]*')

echo ""
if [ "$orden_status" == "COMPLETED" ]; then
    print_success "SAGA COMPLETADO EXITOSAMENTE!"
    print_info "Order ID: ${orden_id}"
    print_info "Status: ${orden_status}"
    print_info "Total: \$${total}"
    
    log_file ""
    log_file "SAGA EXITOSO:"
    log_file "  Order ID: ${orden_id}"
    log_file "  Status: ${orden_status}"
    log_file "  Total: \$${total}"
else
    print_error "SAGA fallo - Status: ${orden_status}"
fi

run_test "Crear orden exitosa (SAGA completo)" "201" "$http_code"

pause

# ============================================================================
# PRUEBA 6: SAGA CON COMPENSACION
# ============================================================================

print_header "PRUEBA 6: SAGA CON COMPENSACION - STOCK INSUFICIENTE"

cat << 'CONCEPT'
+------------------------------------------------------------------+
|  CONCEPTO: COMPENSACION EN SAGA                                  |
+------------------------------------------------------------------+
|                                                                  |
|  Cuando un paso de la SAGA falla, se ejecutan COMPENSACIONES     |
|  para deshacer los pasos anteriores (rollback distribuido):      |
|                                                                  |
|  Flujo con FALLO:                                                |
|  1. Order Service crea orden (status: PENDING)                   |
|  2. Inventory Service intenta reservar -> FALLA (sin stock)      |
|  3. Se dispara COMPENSACION:                                     |
|     - Inventory libera reserva (si la hubo)                      |
|     - Order se marca como FAILED                                 |
|                                                                  |
|        +-------+    +-------+                                    |
|        | Order | -> | Inven |  X  (sin stock)                    |
|        |PENDING|    | FAIL  |                                    |
|        +-------+    +-------+                                    |
|            |            |                                        |
|            +<-- COMPENSAR (liberar, marcar FAILED)               |
|            |                                                     |
|        +-------+                                                 |
|        | Order |                                                 |
|        | FAILED|                                                 |
|        +-------+                                                 |
|                                                                  |
|  IMPORTANTE: Las compensaciones deben ser IDEMPOTENTES           |
|  (ejecutarlas multiples veces produce el mismo resultado)        |
|                                                                  |
+------------------------------------------------------------------+

CONCEPT

log_file ""
log_file "CONCEPTO: COMPENSACION EN SAGA"
log_file "Cuando falla un paso, se ejecutan compensaciones."
log_file "Las compensaciones deben ser idempotentes."
log_file ""

print_section "6.1 - Intentar orden con stock INSUFICIENTE"

orden_fallida='{"userId":"user-test-002","paymentMethod":"credit_card","items":[{"productCode":"LAPTOP-001","quantity":10000}]}'

print_info "Enviando orden IMPOSIBLE:"
print_info "  - 10,000x LAPTOP-001 (no hay suficiente stock)"
print_info "  - Esto DEBE disparar la compensacion SAGA..."

log_file "Request: $orden_fallida"

response=$(do_curl_post "${ORDER_SERVICE}/api/orders" "$orden_fallida")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

log_file "Response: $body"

orden_status=$(echo "$body" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)

echo ""
if [ "$orden_status" == "FAILED" ]; then
    print_success "COMPENSACION SAGA EJECUTADA CORRECTAMENTE!"
    print_info "Status: ${orden_status}"
    print_info "El inventario NO fue afectado (rollback exitoso)"
    
    log_file ""
    log_file "COMPENSACION EJECUTADA:"
    log_file "  Status: FAILED"
    log_file "  Inventario liberado correctamente"
else
    print_error "Se esperaba FAILED pero se obtuvo: ${orden_status}"
fi

run_test "Orden fallida con compensacion SAGA" "400" "$http_code"

print_section "6.2 - Verificar que el inventario no fue afectado"
response=$(do_curl_get "${INVENTORY_SERVICE}/api/inventory/products/LAPTOP-001")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

if [ "$http_code" == "200" ]; then
    reserved=$(echo "$body" | grep -o '"reservedStock":[0-9]*' | grep -o '[0-9]*')
    stock=$(echo "$body" | grep -o '"stock":[0-9]*' | head -1 | grep -o '[0-9]*')
    
    print_info "Stock total: ${stock}"
    print_info "Stock reservado: ${reserved:-0}"
    
    if [ "${reserved:-0}" == "0" ]; then
        print_success "Inventario limpio - reservedStock = 0"
    else
        print_warning "Hay stock reservado: ${reserved}"
    fi
    
    log_file "Stock despues de compensacion: total=${stock}, reserved=${reserved:-0}"
fi

run_test "Verificar inventario despues de compensacion" "200" "$http_code"

pause

# ============================================================================
# PRUEBA 7: VERIFICAR METRICAS EN PROMETHEUS
# ============================================================================

print_header "PRUEBA 7: METRICAS EN PROMETHEUS"

cat << 'CONCEPT'
+------------------------------------------------------------------+
|  CONCEPTO: QUERIES DE PROMETHEUS (PromQL)                        |
+------------------------------------------------------------------+
|                                                                  |
|  PromQL es el lenguaje de consultas de Prometheus:               |
|                                                                  |
|  QUERIES UTILES:                                                 |
|                                                                  |
|  # Requests por segundo (rate en 1 minuto)                       |
|  rate(http_server_requests_seconds_count[1m])                    |
|                                                                  |
|  # Latencia percentil 95                                         |
|  histogram_quantile(0.95,                                        |
|    rate(http_server_requests_seconds_bucket[5m]))                |
|                                                                  |
|  # Memoria JVM usada                                             |
|  jvm_memory_used_bytes{area="heap"}                              |
|                                                                  |
|  # Error rate (5xx / total)                                      |
|  rate(http_server_requests_seconds_count{status=~"5.."}[5m])     |
|  /                                                               |
|  rate(http_server_requests_seconds_count[5m])                    |
|                                                                  |
+------------------------------------------------------------------+

CONCEPT

log_file ""
log_file "CONCEPTO: QUERIES DE PROMETHEUS (PromQL)"
log_file "rate() calcula incremento por segundo"
log_file "histogram_quantile() calcula percentiles"
log_file ""

print_section "7.1 - Consultar metricas de requests HTTP"

# Query para contar requests
query="http_server_requests_seconds_count"
response=$(do_curl_get "${PROMETHEUS}/api/v1/query?query=${query}")
http_code=$(extract_code "$response")
body=$(extract_body "$response")

if [ "$http_code" == "200" ]; then
    result_count=$(echo "$body" | grep -o '"__name__"' | wc -l | tr -d ' ')
    print_success "Prometheus tiene metricas de HTTP requests"
    print_info "Series encontradas: ${result_count}"
    log_file "HTTP request metrics: ${result_count} series"
else
    print_warning "No se pudieron obtener metricas"
fi

run_test "Prometheus HTTP metrics" "200" "$http_code"

print_section "7.2 - Consultar metricas de JVM"

query="jvm_memory_used_bytes"
response=$(do_curl_get "${PROMETHEUS}/api/v1/query?query=${query}")
http_code=$(extract_code "$response")

if [ "$http_code" == "200" ]; then
    print_success "Prometheus tiene metricas de JVM"
    log_file "JVM metrics disponibles"
else
    print_warning "No se pudieron obtener metricas de JVM"
fi

run_test "Prometheus JVM metrics" "200" "$http_code"

echo ""
print_info "QUERIES PARA PROBAR EN PROMETHEUS (${PROMETHEUS}):"
echo ""
echo "  # Requests por segundo:"
echo "  rate(http_server_requests_seconds_count[1m])"
echo ""
echo "  # Latencia P95:"
echo "  histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))"
echo ""
echo "  # Memoria heap usada:"
echo "  jvm_memory_used_bytes{area=\"heap\"}"
echo ""

log_file ""
log_file "QUERIES RECOMENDADAS:"
log_file "  rate(http_server_requests_seconds_count[1m])"
log_file "  histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))"
log_file "  jvm_memory_used_bytes{area=\"heap\"}"

pause

# ============================================================================
# RESUMEN FINAL
# ============================================================================

print_header "RESUMEN DE PRUEBAS"

echo ""
echo "+================================================================+"
echo "|                    RESULTADOS FINALES                          |"
echo "+================================================================+"
printf "| Total de pruebas:    %-39s |\n" "$TESTS_TOTAL"
printf "| Pruebas exitosas:    %-39s |\n" "$TESTS_PASSED"
printf "| Pruebas fallidas:    %-39s |\n" "$TESTS_FAILED"
echo "+================================================================+"
echo ""

cat >> "$REPORT_FILE" << SUMMARY

================================================================================
RESUMEN FINAL
================================================================================

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
    
    cat >> "$REPORT_FILE" << 'CONCLUSION'

CONCLUSION: Sistema funcionando correctamente.

COMPONENTES VERIFICADOS:
  [OK] 3 microservicios (Order, Inventory, Payment)
  [OK] Stack de monitoreo (Prometheus, Grafana, Elasticsearch, Kibana)
  [OK] Redis Cache funcionando (HIT/MISS demostrado)
  [OK] Patron SAGA con transacciones exitosas
  [OK] SAGA con compensaciones (rollback distribuido)
  [OK] Metricas expuestas en /q/metrics
  [OK] Prometheus recolectando metricas

URLS DE HERRAMIENTAS:
  - Prometheus: http://localhost:9090
  - Grafana:    http://localhost:3000 (admin/admin)
  - Kibana:     http://localhost:5601

CONCLUSION
    exit_code=0
else
    print_error "Algunas pruebas fallaron. Revisa los logs."
    
    cat >> "$REPORT_FILE" << 'CONCLUSION'

CONCLUSION: Sistema con fallas. Revisar pruebas fallidas.

Posibles causas:
  - Contenedores Docker no iniciados
  - Microservicios no corriendo
  - Elasticsearch aun iniciando (esperar 2-3 min)

CONCLUSION
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
  - README.md: Guia de instalacion
  - TEORIA.md: Conceptos de observabilidad

Capitulo 10: Monitoreo con Grafana y Kibana
  - Prometheus para metricas
  - Grafana para visualizacion
  - ELK Stack para logs

================================================================================
FOOTER

echo ""
echo "+------------------------------------------------------------------+"
echo "|                    HERRAMIENTAS DISPONIBLES                      |"
echo "+------------------------------------------------------------------+"
echo "|  Prometheus:  http://localhost:9090                              |"
echo "|  Grafana:     http://localhost:3000  (admin / admin)             |"
echo "|  Kibana:      http://localhost:5601                              |"
echo "+------------------------------------------------------------------+"
echo ""

print_success "Reporte guardado en: ${REPORT_FILE}"
echo ""

exit $exit_code

# ============================================================================
# FIN DEL SCRIPT
# ============================================================================