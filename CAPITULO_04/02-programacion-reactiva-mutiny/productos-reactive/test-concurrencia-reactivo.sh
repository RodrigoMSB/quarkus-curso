#!/bin/bash

# ============================================================================
# TEST DE CONCURRENCIA CON HEY - ENFOQUE REACTIVO
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
ENDPOINT="/api/v1/productos/reactivo/1"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
OUTPUT_FILE="resultados-reactivo-${TIMESTAMP}.txt"

# Función para logging dual (consola + archivo)
log_dual() {
    echo -e "$1"
    echo -e "$1" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║  TEST DE CONCURRENCIA - ENFOQUE REACTIVO (Mutiny)             ║${NC}"
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
    echo "TEST DE CONCURRENCIA - ENFOQUE REACTIVO (Mutiny)"
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
    echo "TESTS COMPLETADOS - ENFOQUE REACTIVO"
    echo "============================================================================"
    echo "Fecha finalización: $(date '+%Y-%m-%d %H:%M:%S')"
    echo ""
    echo "MÉTRICAS CLAVE A COMPARAR CON EL ENFOQUE CLÁSICO:"
    echo "  • Requests/sec (throughput) - ¿Cuántas peticiones por segundo?"
    echo "  • Latencia promedio - ¿Qué tan rápido responde en promedio?"
    echo "  • Percentil 50 (p50) - ¿Mitad de requests más rápidos que...?"
    echo "  • Percentil 95 (p95) - ¿95% de requests más rápidos que...?"
    echo "  • Percentil 99 (p99) - ¿Experiencia del peor 1%?"
    echo ""
    echo "SWEET SPOT: Busca el test donde la diferencia es MÁS DRAMÁTICA"
    echo "============================================================================"
} >> "$OUTPUT_FILE"

echo ""
echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║  TESTS COMPLETADOS - ENFOQUE REACTIVO                         ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}✅ Todos los tests finalizados${NC}"
echo -e "${GREEN}📄 Resultados guardados en: ${OUTPUT_FILE}${NC}"
echo ""
echo -e "${YELLOW}💡 SIGUIENTE PASO:${NC}"
echo -e "   Ejecuta el test en el proyecto clásico (capitulo_04_1_1)"
echo -e "   y compara especialmente el test de 5K requests (sweet spot esperado)"
echo ""