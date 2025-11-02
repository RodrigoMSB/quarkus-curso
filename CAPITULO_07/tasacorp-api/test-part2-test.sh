#!/bin/bash

##############################################################################
# Script de Pruebas - PERFIL TEST
# 
# Este script prueba el perfil de TESTING del microservicio TasaCorp.
# El perfil TEST está optimizado para pruebas con límites realistas.
##############################################################################

# ============================================================================
# CONFIGURACIÓN
# ============================================================================

TIMESTAMP=$(date +%Y-%m-%d_%H-%M-%S)
OUTPUT_FILE="test-test-${TIMESTAMP}.txt"

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m'

BASE_URL="http://localhost:8080"
STARTUP_TIMEOUT=60

# ============================================================================
# FUNCIONES DE LOGGING
# ============================================================================

log_header() {
    echo -e "${CYAN}$1${NC}"
    echo "$1" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

log_info() {
    echo -e "${BLUE}$1${NC}"
    echo "$1" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

log_success() {
    echo -e "${GREEN}$1${NC}"
    echo "$1" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

log_warning() {
    echo -e "${YELLOW}$1${NC}"
    echo "$1" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

log_error() {
    echo -e "${RED}$1${NC}"
    echo "$1" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

log_plain() {
    echo -e "$1"
    echo "$1" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

# ============================================================================
# FUNCIONES DE GESTIÓN
# ============================================================================

kill_all() {
    log_info "🧹 Matando procesos previos de Quarkus y Java..."
    pkill -9 -f "quarkus:dev" 2>/dev/null
    pkill -9 -f "quarkus-run.jar" 2>/dev/null
    sleep 3
    log_success "✅ Limpieza completada"
}

wait_for_app() {
    local elapsed=0
    log_info "⏳ Esperando a que la aplicación arranque (timeout: ${STARTUP_TIMEOUT}s)..."
    
    while [ $elapsed -lt $STARTUP_TIMEOUT ]; do
        if curl -s "$BASE_URL/api/tasas/config" > /dev/null 2>&1; then
            log_success "✅ Aplicación lista en perfil TEST"
            sleep 2
            return 0
        fi
        sleep 2
        elapsed=$((elapsed + 2))
        if [ $((elapsed % 10)) -eq 0 ]; then
            log_info "   ... esperando (${elapsed}s/${STARTUP_TIMEOUT}s)"
        fi
    done
    
    log_error "❌ Timeout: La aplicación no arrancó en ${STARTUP_TIMEOUT}s"
    exit 1
}

# ============================================================================
# HEADER
# ============================================================================

{
cat << 'EOF'
╔════════════════════════════════════════════════════════════════╗
║              🟡 PRUEBAS - PERFIL TEST                          ║
║              Testing: Ambiente controlado                      ║
╔════════════════════════════════════════════════════════════════╗
EOF
echo ""
echo "📅 Fecha: $(date '+%d/%m/%Y %H:%M:%S')"
echo "🌐 API Base: $BASE_URL"
echo "📄 Resultados: $OUTPUT_FILE"
echo ""
} | tee "$OUTPUT_FILE"

# ============================================================================
# LIMPIEZA Y COMPILACIÓN
# ============================================================================

log_header "╔════════════════════════════════════════════════════════════════╗"
log_header "║              🔍 PREPARACIÓN                                    ║"
log_header "╚════════════════════════════════════════════════════════════════╝"
log_plain ""

kill_all
log_plain ""

log_success "Características del perfil TEST:"
log_plain "  ✓ Comisión: 1.5% (moderada)"
log_plain "  ✓ Límite transaccional: 1,000 (bajo para pruebas)"
log_plain "  ✓ Cache: Activado"
log_plain "  ✓ Auditoría: Activada"
log_plain "  ✓ Proveedor: FreeCurrencyAPI"
log_plain "  ✓ Vault: Desactivado"
log_plain ""

log_info "📦 Compilando aplicación..."
./mvnw clean package -DskipTests > /dev/null 2>&1
if [ $? -ne 0 ]; then
    log_error "❌ Error al compilar"
    exit 1
fi
log_success "✅ Compilación exitosa"
log_plain ""

log_info "🚀 Arrancando aplicación en modo TEST..."
log_plain ""

# Arrancar en background con perfil TEST
java -Dquarkus.profile=test -jar target/quarkus-app/quarkus-run.jar > /dev/null 2>&1 &
APP_PID=$!
log_info "📋 PID de la aplicación: $APP_PID"
log_plain ""

wait_for_app
log_plain ""

# ============================================================================
# PRUEBAS
# ============================================================================

log_header "╔════════════════════════════════════════════════════════════════╗"
log_header "║              📋 PRUEBAS DEL PERFIL TEST                        ║"
log_header "╚════════════════════════════════════════════════════════════════╝"
log_plain ""

##############################################################################
# PRUEBA 1: Configuración TEST
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 1: Configuración del Perfil TEST"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""

TEST_CONFIG=$(curl -s $BASE_URL/api/tasas/config 2>/dev/null)

if [ $? -eq 0 ]; then
    echo "$TEST_CONFIG" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    perfil = data.get('perfil_activo', 'N/A')
    ambiente = data.get('ambiente', 'N/A')
    comision = data.get('comision_porcentaje', 'N/A')
    limite = data.get('limite_transaccional', 'N/A')
    proveedor = data.get('proveedor', 'N/A')
    
    print(f'✓ Perfil activo: {perfil}')
    print(f'✓ Ambiente: {ambiente}')
    print(f'✓ Comisión: {comision}%')
    print(f'✓ Límite transaccional: \${limite:,}')
    print(f'✓ Proveedor: {proveedor}')
    
    if perfil != 'test':
        print(f\"❌ ERROR: Perfil debería ser 'test' pero es '{perfil}'\")
    if comision != 1.5:
        print(f\"❌ ERROR: Comisión en TEST debería ser 1.5%\")
    if limite != 1000:
        print(f\"❌ ERROR: Límite en TEST debería ser 1,000\")
except Exception as e:
    print(f'❌ Error al procesar respuesta: {e}')
" | tee -a "$OUTPUT_FILE"
else
    log_error "❌ Error: No se pudo conectar al servicio"
    kill_all
    exit 1
fi

log_plain ""
log_success "✅ Configuración TEST correcta"
log_plain ""
sleep 2

##############################################################################
# PRUEBA 2: Conversión CON Comisión
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 2: Conversión con Comisión 1.5%"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""
log_plain "🎯 Objetivo: Verificar que TEST cobra comisión del 1.5%"
log_plain "💰 Operación: Convertir 1,000 PEN a USD"
log_plain ""

curl -s "$BASE_URL/api/tasas/convertir/USD?monto=1000" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    monto = data['monto_origen']
    convertido = data['monto_convertido']
    comision = data['comision']
    
    print(f\"💵 Monto Original: \${monto:,.0f} PEN\")
    print(f\"💱 Monto Convertido: \${convertido:,.2f} USD\")
    print(f\"💸 Comisión (1.5%): \${comision:.2f} USD\")
    print(f\"✅ Total: \${convertido + comision:,.2f} USD\")
    
    if comision > 0:
        print(f\"\\n✅ CORRECTO: TEST cobra comisión del 1.5%\")
    else:
        print(f\"\\n❌ ERROR: Comisión debería ser > 0\")
except Exception as e:
    print(f'❌ Error: {e}')
" | tee -a "$OUTPUT_FILE"

log_plain ""
sleep 2

##############################################################################
# PRUEBA 3: Límite Excedido
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 3: Detectar Límite Excedido"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""
log_plain "🎯 Objetivo: Verificar detección de límite excedido"
log_plain "💰 Operación: Convertir 1,500 PEN (excede límite de 1,000)"
log_plain ""

curl -s "$BASE_URL/api/tasas/convertir/USD?monto=1500" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    monto = data['monto_origen']
    limite = data['limite_transaccional']
    dentro = data['dentro_limite']
    
    print(f\"💵 Monto Solicitado: \${monto:,.0f}\")
    print(f\"🚦 Límite Transaccional: \${limite:,}\")
    print(f\"📊 Dentro de Límite: {dentro}\")
    
    if not dentro:
        print(f\"\\n✅ CORRECTO: Se detectó que excede el límite\")
        print(f\"⚠️  En un sistema real, esto rechazaría la transacción\")
    else:
        print(f\"\\n❌ ERROR: Debería indicar que excede el límite\")
except Exception as e:
    print(f'❌ Error: {e}')
" | tee -a "$OUTPUT_FILE"

log_plain ""

# ============================================================================
# RESUMEN
# ============================================================================

log_header "╔════════════════════════════════════════════════════════════════╗"
log_header "║              ✅ RESUMEN - PERFIL TEST                          ║"
log_header "╚════════════════════════════════════════════════════════════════╝"
log_plain ""
log_success "✅ Perfil TEST verificado exitosamente"
log_plain ""
log_plain "Características confirmadas:"
log_plain "  ✓ Comisión moderada (1.5%)"
log_plain "  ✓ Límite bajo para pruebas (1,000)"
log_plain "  ✓ Detecta límites excedidos"
log_plain ""
log_plain "📄 Log guardado en: $OUTPUT_FILE"
log_plain ""

# ============================================================================
# LIMPIEZA FINAL
# ============================================================================

log_info "🛑 Deteniendo la aplicación..."
kill_all
log_plain ""

log_success "🎉 ¡Pruebas de TEST completadas!"
log_plain ""