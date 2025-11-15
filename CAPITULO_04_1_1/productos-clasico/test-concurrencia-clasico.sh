#!/bin/bash

# ============================================================================
# TEST DE CONCURRENCIA CON HEY - ENFOQUE CLÁSICO
# ============================================================================

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Configuración
HOST="http://localhost:8080"
ENDPOINT="/api/v1/productos/clasico/1"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
OUTPUT_FILE="resultados-clasico-${TIMESTAMP}.txt"

# Función para logging dual (consola + archivo)
log_dual() {
    echo -e "$1"
    echo -e "$1" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║  TEST DE CONCURRENCIA - ENFOQUE CLÁSICO (Blocking)            ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Verificar que hey está instalado
if ! command -v hey &> /dev/null; then
    echo -e "${RED}❌ ERROR: 'hey' no está instalado${NC}"
    exit 1
fi

# Verificar que el servicio está corriendo
echo -e "${YELLOW}🔍 Verificando que el servicio esté activo...${NC}"
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -H "Accept: application/json" "${HOST}${ENDPOINT}")
if [ "$RESPONSE" != "200" ]; then
    echo -e "${RED}❌ ERROR: El servicio no está corriendo o el endpoint no responde correctamente${NC}"
    echo -e "${RED}   HTTP Status: ${RESPONSE}${NC}"
    echo -e "${YELLOW}   Endpoint: ${HOST}${ENDPOINT}${NC}"
    echo ""
    echo "Asegúrate de:"
    echo "  1. Haber iniciado el proyecto: ./mvnw quarkus:dev"
    echo "  2. Que exista el producto con ID=1 en la BD"
    exit 1
fi
echo -e "${GREEN}✅ Servicio activo (HTTP 200)${NC}"
echo ""

# Iniciar archivo de resultados
{
    echo "============================================================================"
    echo "TEST DE CONCURRENCIA - ENFOQUE CLÁSICO (Blocking)"
    echo "============================================================================"
    echo "Fecha: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "Endpoint: ${HOST}${ENDPOINT}"
    echo "============================================================================"
    echo ""
} > "$OUTPUT_FILE"

# Función para ejecutar test
run_test() {
    local requests=$1
    local concurrency=$2
    local description=$3
    
    log_dual "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    log_dual "${CYAN}📊 TEST: ${description}${NC}"
    log_dual "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    log_dual "${YELLOW}Requests totales: ${requests}${NC}"
    log_dual "${YELLOW}Concurrencia: ${concurrency} workers${NC}"
    log_dual ""
    
    # Ejecutar hey con header Accept correcto
    hey -n $requests -c $concurrency \
        -H "Accept: application/json" \
        "${HOST}${ENDPOINT}" 2>&1 | tee -a "$OUTPUT_FILE"
    
    log_dual ""
    log_dual "${GREEN}✅ Test completado${NC}"
    log_dual ""
}

echo -e "${CYAN}Ejecutaremos 3 tests con diferentes niveles de carga:${NC}"
echo ""
echo -e "  1️⃣  1,000 requests con 50 workers"
echo -e "  2️⃣  5,000 requests con 100 workers  ${YELLOW}← SWEET SPOT esperado${NC}"
echo -e "  3️⃣  10,000 requests con 200 workers"
echo ""
echo -e "${YELLOW}📄 Los resultados se guardarán en: ${OUTPUT_FILE}${NC}"
echo ""
read -p "Presiona ENTER para comenzar..."
echo ""

# Ejecutar tests
run_test 1000 50 "Carga Ligera (1K requests)"
read -p "Presiona ENTER para continuar con el siguiente test..."
echo ""

run_test 5000 100 "Carga Media (5K requests) - SWEET SPOT"
read -p "Presiona ENTER para continuar con el siguiente test..."
echo ""

run_test 10000 200 "Carga Alta (10K requests)"

# Resumen final
{
    echo ""
    echo "============================================================================"
    echo "TESTS COMPLETADOS - ENFOQUE CLÁSICO"
    echo "============================================================================"
    echo "Fecha finalización: $(date '+%Y-%m-%d %H:%M:%S')"
    echo ""
    echo "ANÁLISIS COMPARATIVO:"
    echo "  Compara estos resultados con el enfoque reactivo (capitulo_04_1)"
    echo ""
    echo "MÉTRICAS CLAVE:"
    echo "  • Requests/sec (throughput) - Menor que reactivo bajo alta concurrencia"
    echo "  • Latencia promedio - Puede ser mayor bajo carga"
    echo "  • Percentil 95 (p95) - Usualmente peor que reactivo en sweet spot"
    echo ""
    echo "ESPERADO:"
    echo "  El enfoque clásico mostrará:"
    echo "  ✗ Menor throughput en el sweet spot (5K requests)"
    echo "  ✗ Mayor latencia p95 bajo alta concurrencia"
    echo "  ✗ Peor escalabilidad que el enfoque reactivo"
    echo ""
    echo "PERO:"
    echo "  ✓ Código más simple y fácil de debuggear"
    echo "  ✓ Suficiente para aplicaciones con baja/media concurrencia"
    echo "  ✓ Menos curva de aprendizaje para el equipo"
    echo "============================================================================"
} >> "$OUTPUT_FILE"

echo ""
echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║  TESTS COMPLETADOS - ENFOQUE CLÁSICO                          ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}✅ Todos los tests finalizados${NC}"
echo -e "${GREEN}📄 Resultados guardados en: ${OUTPUT_FILE}${NC}"
echo ""
echo -e "${YELLOW}💡 ANÁLISIS:${NC}"
echo -e "   Abre ambos archivos (reactivo y clásico) y compara lado a lado"
echo -e "   Enfócate especialmente en el test de 5K requests (sweet spot)"
echo ""
echo -e "${CYAN}¿Qué buscar en la comparación?${NC}"
echo -e "   1. Throughput (req/s) - ¿Cuánto más rápido es el reactivo?"
echo -e "   2. Latencia p95 - ¿Experiencia consistente bajo carga?"
echo -e "   3. Distribución de tiempos - ¿Más predecible el reactivo?"
echo ""