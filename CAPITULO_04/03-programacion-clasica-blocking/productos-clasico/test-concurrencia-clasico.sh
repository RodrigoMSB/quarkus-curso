#!/bin/bash

# ============================================================================
# TEST DE CONCURRENCIA CON K6 - ENFOQUE CLÁSICO
# ============================================================================
# Compatible con Mac y Windows (Git Bash)
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

echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║  TEST DE CONCURRENCIA - ENFOQUE CLÁSICO (Blocking)            ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Verificar que k6 está instalado
if ! command -v k6 &> /dev/null; then
    if [ -f "/c/Program Files/k6/k6.exe" ]; then
        export PATH="$PATH:/c/Program Files/k6"
        echo -e "${YELLOW}⚠️  k6 encontrado en /c/Program Files/k6${NC}"
    else
        echo -e "${RED}❌ ERROR: 'k6' no está instalado${NC}"
        echo ""
        echo -e "${YELLOW}Instalación:${NC}"
        echo -e "  Mac:     brew install k6"
        echo -e "  Windows: https://dl.k6.io/msi/k6-latest-amd64.msi"
        echo -e "           Luego: export PATH=\"\$PATH:/c/Program Files/k6\""
        exit 1
    fi
fi

echo -e "${GREEN}✅ k6 encontrado: $(k6 version 2>&1 | head -1)${NC}"
echo ""

# Verificar servicio
echo -e "${YELLOW}🔍 Verificando servicio...${NC}"
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -H "Accept: application/json" "${HOST}${ENDPOINT}")
if [ "$RESPONSE" != "200" ]; then
    echo -e "${RED}❌ ERROR: El servicio no responde (HTTP ${RESPONSE})${NC}"
    echo "Asegúrate de ejecutar: ./mvnw quarkus:dev"
    exit 1
fi
echo -e "${GREEN}✅ Servicio OK${NC}"
echo ""

# Header del archivo
{
    echo "============================================================================"
    echo "TEST DE CONCURRENCIA - ENFOQUE CLÁSICO (Blocking)"
    echo "============================================================================"
    echo "Fecha: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "Endpoint: ${HOST}${ENDPOINT}"
    echo "============================================================================"
    echo ""
} > "$OUTPUT_FILE"

# Crear script k6
K6_SCRIPT=$(mktemp 2>/dev/null || echo "/tmp/k6_script_$$.js")
cat > "$K6_SCRIPT" << 'EOFK6'
import http from 'k6/http';
import { check } from 'k6';

export const options = {
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

export default function() {
    const res = http.get(__ENV.TARGET_URL, {
        headers: { 'Accept': 'application/json' }
    });
    check(res, { 'status 200': (r) => r.status === 200 });
}
EOFK6

# Función para ejecutar test
run_test() {
    local requests=$1
    local concurrency=$2
    local description=$3
    
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}📊 TEST: ${description}${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo "Requests: ${requests} | Concurrencia: ${concurrency}"
    echo ""
    
    # Header en archivo
    {
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "📊 TEST: ${description}"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "Requests totales: ${requests}"
        echo "Concurrencia: ${concurrency} workers"
        echo ""
    } >> "$OUTPUT_FILE"
    
    # Ejecutar k6 y capturar salida
    local k6_output
    k6_output=$(TARGET_URL="${HOST}${ENDPOINT}" k6 run \
        --vus "$concurrency" \
        --iterations "$requests" \
        "$K6_SCRIPT" 2>&1)
    
    # Extraer métricas de la salida de k6
    local duration_line=$(echo "$k6_output" | grep "http_req_duration")
    local reqs_line=$(echo "$k6_output" | grep "http_reqs")
    
    # Extraer valores
    local avg_ms=$(echo "$duration_line" | grep -oE 'avg=[0-9.]+' | cut -d= -f2)
    local min_ms=$(echo "$duration_line" | grep -oE 'min=[0-9.]+' | cut -d= -f2)
    local max_ms=$(echo "$duration_line" | grep -oE 'max=[0-9.]+' | cut -d= -f2)
    local med_ms=$(echo "$duration_line" | grep -oE 'med=[0-9.]+' | cut -d= -f2)
    local p90_ms=$(echo "$duration_line" | grep -oE 'p\(90\)=[0-9.]+' | cut -d= -f2)
    local p95_ms=$(echo "$duration_line" | grep -oE 'p\(95\)=[0-9.]+' | cut -d= -f2)
    local p99_ms=$(echo "$duration_line" | grep -oE 'p\(99\)=[0-9.]+' | cut -d= -f2)
    local reqs_sec=$(echo "$reqs_line" | grep -oE '[0-9.]+/s' | sed 's|/s||')
    
    # Convertir ms a secs
    local avg_secs=$(awk "BEGIN {printf \"%.4f\", ${avg_ms:-0}/1000}")
    local min_secs=$(awk "BEGIN {printf \"%.4f\", ${min_ms:-0}/1000}")
    local max_secs=$(awk "BEGIN {printf \"%.4f\", ${max_ms:-0}/1000}")
    local med_secs=$(awk "BEGIN {printf \"%.4f\", ${med_ms:-0}/1000}")
    local p90_secs=$(awk "BEGIN {printf \"%.4f\", ${p90_ms:-0}/1000}")
    local p95_secs=$(awk "BEGIN {printf \"%.4f\", ${p95_ms:-0}/1000}")
    local p99_secs=$(awk "BEGIN {printf \"%.4f\", ${p99_ms:-0}/1000}")
    
    # Mostrar en consola
    echo -e "${GREEN}Requests/sec: ${reqs_sec}${NC}"
    echo -e "Average: ${avg_ms}ms | p95: ${p95_ms}ms | p99: ${p99_ms}ms"
    echo ""
    
    # Guardar en formato hey
    {
        echo ""
        echo "Summary:"
        echo "  Slowest:	${max_secs} secs"
        echo "  Fastest:	${min_secs} secs"
        echo "  Average:	${avg_secs} secs"
        echo "  Requests/sec:	${reqs_sec}"
        echo ""
        echo "Latency distribution:"
        echo "  50% in ${med_secs} secs"
        echo "  90% in ${p90_secs} secs"
        echo "  95% in ${p95_secs} secs"
        echo "  99% in ${p99_secs} secs"
        echo ""
        echo "✅ Test completado"
        echo ""
    } >> "$OUTPUT_FILE"
    
    echo -e "${GREEN}✅ Test completado${NC}"
    echo ""
}

echo -e "${CYAN}Tests a ejecutar:${NC}"
echo -e "  1️⃣  1,000 requests / 50 workers"
echo -e "  2️⃣  5,000 requests / 100 workers  ${YELLOW}← SWEET SPOT${NC}"
echo -e "  3️⃣  10,000 requests / 200 workers"
echo ""
echo -e "${YELLOW}📄 Resultados: ${OUTPUT_FILE}${NC}"
echo ""
read -p "ENTER para comenzar..."
echo ""

run_test 1000 50 "Carga Ligera (1K requests)"
read -p "ENTER para siguiente test..."
echo ""

run_test 5000 100 "Carga Media (5K requests) - SWEET SPOT"
read -p "ENTER para siguiente test..."
echo ""

run_test 10000 200 "Carga Alta (10K requests)"

rm -f "$K6_SCRIPT"

# Footer
{
    echo ""
    echo "============================================================================"
    echo "TESTS COMPLETADOS - ENFOQUE CLÁSICO"
    echo "============================================================================"
    echo "Fecha finalización: $(date '+%Y-%m-%d %H:%M:%S')"
    echo ""
    echo "ANÁLISIS COMPARATIVO:"
    echo "  Compara estos resultados con el enfoque reactivo"
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

echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║  TESTS COMPLETADOS                                            ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}📄 Resultados: ${OUTPUT_FILE}${NC}"
echo ""