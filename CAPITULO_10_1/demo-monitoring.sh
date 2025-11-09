#!/bin/bash

# ============================================================================
# DEMO DE MONITOREO - Capítulo 10_1
# ============================================================================
# Script para demostrar:
# - SAGA con compensación
# - Redis Cache (HIT/MISS)
# - Métricas de Prometheus
# - Generación de tráfico para Grafana
#
# Autor: Rodrigo Silva
# Versión: 1.0
# ============================================================================

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color
BOLD='\033[1m'

# URLs de los servicios
ORDER_SERVICE="http://localhost:8080"
INVENTORY_SERVICE="http://localhost:8081"
PAYMENT_SERVICE="http://localhost:8082"
PROMETHEUS="http://localhost:9090"

# ============================================================================
# FUNCIONES AUXILIARES
# ============================================================================

print_header() {
    echo -e "\n${CYAN}${BOLD}============================================${NC}"
    echo -e "${CYAN}${BOLD}  $1${NC}"
    echo -e "${CYAN}${BOLD}============================================${NC}\n"
}

print_step() {
    echo -e "${BLUE}▶ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_metric() {
    echo -e "${MAGENTA}📊 $1${NC}"
}

# Verificar que un servicio esté corriendo
check_service() {
    local service_name=$1
    local service_url=$2
    
    if curl -s -f "${service_url}/health" > /dev/null 2>&1; then
        print_success "${service_name} está corriendo en ${service_url}"
        return 0
    else
        print_error "${service_name} NO está corriendo en ${service_url}"
        return 1
    fi
}

# Pausa con mensaje
pause() {
    echo -e "\n${YELLOW}Presiona ENTER para continuar...${NC}"
    read -r
}

# ============================================================================
# VERIFICACIÓN DE PREREQUISITOS
# ============================================================================

verificar_servicios() {
    print_header "1. VERIFICANDO SERVICIOS"
    
    local all_ok=true
    
    check_service "Order Service" "$ORDER_SERVICE" || all_ok=false
    check_service "Inventory Service" "$INVENTORY_SERVICE" || all_ok=false
    check_service "Payment Service" "$PAYMENT_SERVICE" || all_ok=false
    
    # Verificar Docker containers
    print_step "Verificando contenedores Docker..."
    
    if docker ps | grep -q "prometheus"; then
        print_success "Prometheus está corriendo"
    else
        print_warning "Prometheus NO está corriendo (opcional para esta demo)"
    fi
    
    if docker ps | grep -q "redis-cache"; then
        print_success "Redis está corriendo"
    else
        print_error "Redis NO está corriendo"
        all_ok=false
    fi
    
    if [ "$all_ok" = false ]; then
        print_error "\nAlgunos servicios no están corriendo."
        echo -e "${YELLOW}Ejecuta primero:${NC}"
        echo "  1. docker-compose -f docker-compose-monitoring.yml up -d"
        echo "  2. mvn quarkus:dev en cada servicio (3 terminales)"
        exit 1
    fi
    
    print_success "\n¡Todos los servicios están OK!"
    pause
}

# ============================================================================
# DEMOSTRACIÓN DE REDIS CACHE
# ============================================================================

demo_redis_cache() {
    print_header "2. DEMOSTRACIÓN: REDIS CACHE"
    
    print_step "Limpiando cache de Redis..."
    docker exec redis-cache redis-cli FLUSHALL > /dev/null
    print_success "Cache limpiado"
    
    echo -e "\n${CYAN}Vamos a consultar el mismo producto 3 veces:${NC}"
    echo "  - 1ra vez: CACHE MISS (consulta BD)"
    echo "  - 2da vez: CACHE HIT (desde Redis)"
    echo "  - 3ra vez: CACHE HIT (desde Redis)"
    pause
    
    print_step "Consulta 1 - LAPTOP-001 (esperamos CACHE MISS)"
    time curl -s "$INVENTORY_SERVICE/api/inventory/products/LAPTOP-001" | jq -r '.name'
    sleep 1
    
    print_step "Consulta 2 - LAPTOP-001 (esperamos CACHE HIT)"
    time curl -s "$INVENTORY_SERVICE/api/inventory/products/LAPTOP-001" | jq -r '.name'
    sleep 1
    
    print_step "Consulta 3 - LAPTOP-001 (esperamos CACHE HIT)"
    time curl -s "$INVENTORY_SERVICE/api/inventory/products/LAPTOP-001" | jq -r '.name'
    
    echo -e "\n${MAGENTA}Estadísticas de Redis:${NC}"
    docker exec redis-cache redis-cli INFO stats | grep -E "keyspace_hits|keyspace_misses"
    
    # Calcular hit rate
    hits=$(docker exec redis-cache redis-cli INFO stats | grep "keyspace_hits" | cut -d: -f2 | tr -d '\r')
    misses=$(docker exec redis-cache redis-cli INFO stats | grep "keyspace_misses" | cut -d: -f2 | tr -d '\r')
    total=$((hits + misses))
    
    if [ $total -gt 0 ]; then
        hit_rate=$(awk "BEGIN {printf \"%.2f\", ($hits/$total)*100}")
        print_metric "Cache Hit Rate: ${hit_rate}%"
    fi
    
    pause
}

# ============================================================================
# DEMOSTRACIÓN DE SAGA EXITOSO
# ============================================================================

demo_saga_exitoso() {
    print_header "3. DEMOSTRACIÓN: SAGA EXITOSO"
    
    echo -e "${CYAN}Vamos a crear una orden VÁLIDA:${NC}"
    echo "  - 1 Laptop ($899.99)"
    echo "  - 2 Mouse ($99.99 c/u)"
    echo "  - Total esperado: $1099.97"
    pause
    
    print_step "Creando orden..."
    
    # Crear archivo temporal con JSON (cross-platform: Mac + Windows GitBash)
    local temp_file=$(mktemp)
    printf '%s' '{"userId":"user-demo-1","paymentMethod":"credit_card","items":[{"productCode":"LAPTOP-001","quantity":1},{"productCode":"MOUSE-001","quantity":2}]}' > "$temp_file"
    
    response=$(curl -s -X POST "$ORDER_SERVICE/api/orders" \
      -H "Content-Type: application/json" \
      --data-binary "@$temp_file")
    
    rm -f "$temp_file"
    
    echo "$response" | jq '.'
    
    status=$(echo "$response" | jq -r '.status')
    
    if [ "$status" = "COMPLETED" ]; then
        print_success "SAGA completado exitosamente"
        total=$(echo "$response" | jq -r '.totalAmount')
        print_metric "Total de la orden: \$$total"
    else
        print_error "SAGA falló - Status: $status"
    fi
    
    pause
}

# ============================================================================
# DEMOSTRACIÓN DE SAGA CON COMPENSACIÓN
# ============================================================================

demo_saga_compensacion() {
    print_header "4. DEMOSTRACIÓN: SAGA CON COMPENSACIÓN"
    
    echo -e "${CYAN}Vamos a crear una orden INVÁLIDA:${NC}"
    echo "  - 10,000 Laptops (¡no hay stock!)"
    echo "  - SAGA debe COMPENSAR (rollback distribuido)"
    pause
    
    print_step "Intentando crear orden con stock insuficiente..."
    
    # Crear archivo temporal con JSON (cross-platform: Mac + Windows GitBash)
    local temp_file=$(mktemp)
    printf '%s' '{"userId":"user-demo-2","paymentMethod":"credit_card","items":[{"productCode":"LAPTOP-001","quantity":10000}]}' > "$temp_file"
    
    response=$(curl -s -X POST "$ORDER_SERVICE/api/orders" \
      -H "Content-Type: application/json" \
      --data-binary "@$temp_file")
    
    rm -f "$temp_file"
    
    echo "$response" | jq '.'
    
    status=$(echo "$response" | jq -r '.status')
    
    if [ "$status" = "FAILED" ]; then
        print_success "SAGA compensó correctamente (rollback)"
        message=$(echo "$response" | jq -r '.message')
        print_warning "Razón: $message"
    else
        print_error "Esperábamos FAILED pero obtuvimos: $status"
    fi
    
    echo -e "\n${CYAN}Verifica en los logs de order-service:${NC}"
    echo "  Deberías ver mensajes de: '🔄 Compensando...'"
    
    pause
}

# ============================================================================
# GENERACIÓN DE TRÁFICO PARA MÉTRICAS
# ============================================================================

generar_trafico() {
    print_header "5. GENERANDO TRÁFICO PARA MÉTRICAS"
    
    echo -e "${CYAN}Vamos a generar tráfico variado:${NC}"
    echo "  - 10 órdenes exitosas"
    echo "  - 3 órdenes fallidas"
    echo "  - Consultas de productos (cache)"
    echo -e "\n${YELLOW}Esto tomará ~30 segundos...${NC}"
    pause
    
    print_step "Generando órdenes exitosas..."
    for i in {1..10}; do
        # Crear archivo temporal con JSON (cross-platform: Mac + Windows GitBash)
        local temp_file=$(mktemp)
        printf '%s' "{\"userId\":\"user-load-$i\",\"paymentMethod\":\"credit_card\",\"items\":[{\"productCode\":\"MOUSE-001\",\"quantity\":1}]}" > "$temp_file"
        
        curl -s -X POST "$ORDER_SERVICE/api/orders" \
          -H "Content-Type: application/json" \
          --data-binary "@$temp_file" > /dev/null
        
        rm -f "$temp_file"
        echo -n "."
        sleep 0.5
    done
    print_success "\n10 órdenes exitosas creadas"
    
    print_step "Generando órdenes fallidas (para métricas de error)..."
    for i in {1..3}; do
        # Crear archivo temporal con JSON (cross-platform: Mac + Windows GitBash)
        local temp_file=$(mktemp)
        printf '%s' "{\"userId\":\"user-fail-$i\",\"paymentMethod\":\"credit_card\",\"items\":[{\"productCode\":\"LAPTOP-001\",\"quantity\":9999}]}" > "$temp_file"
        
        curl -s -X POST "$ORDER_SERVICE/api/orders" \
          -H "Content-Type: application/json" \
          --data-binary "@$temp_file" > /dev/null
        
        rm -f "$temp_file"
        echo -n "."
        sleep 0.5
    done
    print_success "\n3 órdenes fallidas generadas"
    
    print_step "Consultando productos (generando cache hits)..."
    products=("LAPTOP-001" "MOUSE-001" "KEYBOARD-001" "MONITOR-001" "MOUSE-PAD-001")
    for i in {1..15}; do
        product=${products[$RANDOM % ${#products[@]}]}
        curl -s "$INVENTORY_SERVICE/api/inventory/products/$product" > /dev/null
        echo -n "."
        sleep 0.3
    done
    print_success "\n15 consultas de productos realizadas"
    
    pause
}

# ============================================================================
# MOSTRAR MÉTRICAS
# ============================================================================

mostrar_metricas() {
    print_header "6. MÉTRICAS Y ESTADÍSTICAS"
    
    # Redis Stats
    echo -e "${MAGENTA}📊 REDIS CACHE:${NC}"
    docker exec redis-cache redis-cli INFO stats | grep -E "keyspace_hits|keyspace_misses"
    hits=$(docker exec redis-cache redis-cli INFO stats | grep "keyspace_hits" | cut -d: -f2 | tr -d '\r')
    misses=$(docker exec redis-cache redis-cli INFO stats | grep "keyspace_misses" | cut -d: -f2 | tr -d '\r')
    total=$((hits + misses))
    if [ $total -gt 0 ]; then
        hit_rate=$(awk "BEGIN {printf \"%.2f\", ($hits/$total)*100}")
        echo -e "${GREEN}Hit Rate: ${hit_rate}%${NC}"
    fi
    
    # Prometheus metrics (si está disponible)
    echo -e "\n${MAGENTA}📊 MÉTRICAS DE PROMETHEUS:${NC}"
    if curl -s -f "$PROMETHEUS/api/v1/query?query=http_server_requests_seconds_count" > /dev/null 2>&1; then
        print_success "Prometheus está recolectando métricas"
        echo -e "\n${CYAN}Abre estos URLs para ver métricas:${NC}"
        echo "  • Prometheus: $PROMETHEUS"
        echo "  • Grafana: http://localhost:3000 (admin/admin)"
        echo "  • Kibana: http://localhost:5601"
    else
        print_warning "Prometheus no está disponible o aún no tiene datos"
    fi
    
    # Verificar endpoints de métricas
    echo -e "\n${MAGENTA}📊 ENDPOINTS DE MÉTRICAS:${NC}"
    echo "  • Order Service: $ORDER_SERVICE/q/metrics"
    echo "  • Inventory Service: $INVENTORY_SERVICE/q/metrics"
    echo "  • Payment Service: $PAYMENT_SERVICE/q/metrics"
    
    pause
}

# ============================================================================
# QUERIES ÚTILES DE PROMETHEUS
# ============================================================================

mostrar_queries_prometheus() {
    print_header "7. QUERIES ÚTILES PARA PROMETHEUS"
    
    echo -e "${CYAN}Copia estas queries en Prometheus ($PROMETHEUS):${NC}\n"
    
    echo -e "${YELLOW}📈 Requests por segundo:${NC}"
    echo "rate(http_server_requests_seconds_count[1m])"
    echo ""
    
    echo -e "${YELLOW}📈 Latencia P95:${NC}"
    echo "histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))"
    echo ""
    
    echo -e "${YELLOW}📈 Error Rate:${NC}"
    echo "rate(http_server_requests_seconds_count{status=~\"5..\"}[5m]) / rate(http_server_requests_seconds_count[5m])"
    echo ""
    
    echo -e "${YELLOW}📈 Memoria JVM:${NC}"
    echo "jvm_memory_used_bytes{area=\"heap\"}"
    echo ""
    
    echo -e "${YELLOW}📈 Threads activos:${NC}"
    echo "jvm_threads_live_threads"
    echo ""
    
    pause
}

# ============================================================================
# RESUMEN FINAL
# ============================================================================

mostrar_resumen() {
    print_header "8. RESUMEN DE LA DEMO"
    
    echo -e "${GREEN}✅ Demostración completada exitosamente${NC}\n"
    
    echo -e "${CYAN}Lo que vimos:${NC}"
    echo "  ✓ Redis Cache funcionando (HIT/MISS)"
    echo "  ✓ SAGA con transacción exitosa"
    echo "  ✓ SAGA con compensación (rollback)"
    echo "  ✓ Generación de tráfico para métricas"
    echo "  ✓ Métricas expuestas en /q/metrics"
    
    echo -e "\n${CYAN}Herramientas de monitoreo disponibles:${NC}"
    echo "  📊 Prometheus: http://localhost:9090"
    echo "  📈 Grafana: http://localhost:3000 (admin/admin)"
    echo "  📋 Kibana: http://localhost:5601"
    
    echo -e "\n${CYAN}Próximos pasos:${NC}"
    echo "  1. Abre Grafana y crea dashboards"
    echo "  2. Explora las queries de Prometheus"
    echo "  3. Revisa los logs en Kibana"
    echo "  4. Genera más tráfico con: ./demo-monitoring.sh --trafico"
    
    echo -e "\n${YELLOW}Para volver a ejecutar la demo completa:${NC}"
    echo "  ./demo-monitoring.sh"
    
    echo -e "\n${GREEN}¡Demo completada! 🎉${NC}\n"
}

# ============================================================================
# MENÚ PRINCIPAL
# ============================================================================

mostrar_menu() {
    clear
    print_header "🚀 DEMO DE MONITOREO - CAPÍTULO 10_1"
    
    echo -e "${CYAN}Selecciona una opción:${NC}\n"
    echo "  1) Demo completa (recomendado)"
    echo "  2) Solo verificar servicios"
    echo "  3) Solo demo de Redis Cache"
    echo "  4) Solo demo de SAGA"
    echo "  5) Solo generar tráfico"
    echo "  6) Mostrar métricas"
    echo "  7) Queries de Prometheus"
    echo "  8) Salir"
    echo ""
    echo -ne "${YELLOW}Opción: ${NC}"
    read -r opcion
    
    case $opcion in
        1)
            verificar_servicios
            demo_redis_cache
            demo_saga_exitoso
            demo_saga_compensacion
            generar_trafico
            mostrar_metricas
            mostrar_queries_prometheus
            mostrar_resumen
            ;;
        2)
            verificar_servicios
            ;;
        3)
            demo_redis_cache
            ;;
        4)
            demo_saga_exitoso
            demo_saga_compensacion
            ;;
        5)
            generar_trafico
            mostrar_metricas
            ;;
        6)
            mostrar_metricas
            ;;
        7)
            mostrar_queries_prometheus
            ;;
        8)
            echo -e "\n${GREEN}¡Hasta luego!${NC}\n"
            exit 0
            ;;
        *)
            print_error "Opción inválida"
            sleep 2
            mostrar_menu
            ;;
    esac
}

# ============================================================================
# MAIN
# ============================================================================

# Verificar si jq está instalado
if ! command -v jq &> /dev/null; then
    print_warning "jq no está instalado (opcional pero recomendado)"
    echo "Para instalar: brew install jq"
    echo ""
fi

# Si se pasa --trafico como argumento, solo generar tráfico
if [ "$1" = "--trafico" ]; then
    generar_trafico
    mostrar_metricas
    exit 0
fi

# Si se pasa --auto, ejecutar demo completa sin pausas
if [ "$1" = "--auto" ]; then
    verificar_servicios() { print_step "Verificando servicios..."; sleep 1; }
    pause() { sleep 2; }
    verificar_servicios
    demo_redis_cache
    demo_saga_exitoso
    demo_saga_compensacion
    generar_trafico
    mostrar_metricas
    mostrar_resumen
    exit 0
fi

# Mostrar menú interactivo
mostrar_menu