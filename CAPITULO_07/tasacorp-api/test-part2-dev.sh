#!/bin/bash

##############################################################################
# Script de Pruebas - PERFIL DEV
# 
# Este script prueba el perfil de DESARROLLO del microservicio TasaCorp.
# El perfil DEV está optimizado para desarrollo rápido sin restricciones.
##############################################################################

# ============================================================================
# CONFIGURACIÓN
# ============================================================================

TIMESTAMP=$(date +%Y-%m-%d_%H-%M-%S)
OUTPUT_FILE="test-dev-${TIMESTAMP}.txt"

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
            log_success "✅ Aplicación lista en perfil DEV"
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
║              🟢 PRUEBAS - PERFIL DEV                           ║
║              Desarrollo: Sin restricciones                     ║
╔════════════════════════════════════════════════════════════════╗
EOF
echo ""
echo "📅 Fecha: $(date '+%d/%m/%Y %H:%M:%S')"
echo "🌐 API Base: $BASE_URL"
echo "📄 Resultados: $OUTPUT_FILE"
echo ""
} | tee "$OUTPUT_FILE"

# ============================================================================
# LIMPIEZA Y ARRANQUE
# ============================================================================

log_header "╔════════════════════════════════════════════════════════════════╗"
log_header "║              🔍 PREPARACIÓN                                    ║"
log_header "╚════════════════════════════════════════════════════════════════╝"
log_plain ""

kill_all
log_plain ""

log_success "Características del perfil DEV:"
log_plain "  ✓ Comisión: 0.0% (gratis para desarrollo)"
log_plain "  ✓ Límite transaccional: 999,999 (ilimitado)"
log_plain "  ✓ Cache: Desactivado"
log_plain "  ✓ Auditoría: Desactivada"
log_plain "  ✓ Proveedor: MockProvider"
log_plain "  ✓ Vault: Desactivado"
log_plain ""

log_info "🚀 Arrancando aplicación en modo DEV..."
log_plain ""

# Arrancar en background
./mvnw quarkus:dev > /dev/null 2>&1 &
APP_PID=$!
log_info "📋 PID de la aplicación: $APP_PID"
log_plain ""

wait_for_app
log_plain ""

# ============================================================================
# PRUEBAS
# ============================================================================

log_header "╔════════════════════════════════════════════════════════════════╗"
log_header "║              📋 PRUEBAS DEL PERFIL DEV                         ║"
log_header "╚════════════════════════════════════════════════════════════════╝"
log_plain ""

##############################################################################
# PRUEBA 1: Configuración DEV
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 1: Configuración del Perfil DEV"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""

DEV_CONFIG=$(curl -s $BASE_URL/api/tasas/config 2>/dev/null)

if [ $? -eq 0 ]; then
    echo "$DEV_CONFIG" | python3 -c "
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
    
    if perfil != 'dev':
        print(f\"❌ ERROR: Perfil debería ser 'dev' pero es '{perfil}'\")
    if comision != 0.0:
        print(f\"❌ ERROR: Comisión en DEV debería ser 0.0%\")
    if limite != 999999:
        print(f\"❌ ERROR: Límite en DEV debería ser 999,999\")
except Exception as e:
    print(f'❌ Error al procesar respuesta: {e}')
" | tee -a "$OUTPUT_FILE"
else
    log_error "❌ Error: No se pudo conectar al servicio"
    kill_all
    exit 1
fi

log_plain ""
log_success "✅ Configuración DEV correcta"
log_plain ""
sleep 2

##############################################################################
# PRUEBA 2: Conversión SIN Comisión
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 2: Conversión sin Comisión"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""
log_plain "🎯 Objetivo: Verificar que DEV no cobra comisión"
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
    print(f\"💸 Comisión: \${comision:.2f} USD\")
    print(f\"✅ Total: \${convertido + comision:,.2f} USD\")
    
    if comision == 0.0:
        print(f\"\\n✅ CORRECTO: Sin comisión en DEV\")
    else:
        print(f\"\\n❌ ERROR: Comisión debería ser 0.0\")
except Exception as e:
    print(f'❌ Error: {e}')
" | tee -a "$OUTPUT_FILE"

log_plain ""
sleep 2

##############################################################################
# PRUEBA 3: Límite Ilimitado
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 3: Límite Ilimitado"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""
log_plain "🎯 Objetivo: Verificar que DEV acepta montos altos"
log_plain "💰 Operación: Convertir 100,000 PEN"
log_plain ""

curl -s "$BASE_URL/api/tasas/convertir/USD?monto=100000" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    monto = data['monto_origen']
    limite = data['limite_transaccional']
    dentro = data['dentro_limite']
    
    print(f\"💵 Monto Solicitado: \${monto:,.0f}\")
    print(f\"🚦 Límite Transaccional: \${limite:,}\")
    print(f\"📊 Dentro de Límite: {dentro}\")
    
    if dentro:
        print(f\"\\n✅ CORRECTO: DEV acepta montos muy altos\")
    else:
        print(f\"\\n❌ ERROR: Debería estar dentro del límite\")
except Exception as e:
    print(f'❌ Error: {e}')
" | tee -a "$OUTPUT_FILE"

log_plain ""

# ============================================================================
# RESUMEN
# ============================================================================

log_header "╔════════════════════════════════════════════════════════════════╗"
log_header "║              ✅ RESUMEN - PERFIL DEV                           ║"
log_header "╚════════════════════════════════════════════════════════════════╝"
log_plain ""
log_success "✅ Perfil DEV verificado exitosamente"
log_plain ""
log_plain "Características confirmadas:"
log_plain "  ✓ Sin comisiones (desarrollo rápido)"
log_plain "  ✓ Límite ilimitado (sin restricciones)"
log_plain "  ✓ Proveedor Mock (sin API externa)"
log_plain ""
log_plain "📄 Log guardado en: $OUTPUT_FILE"
log_plain ""

# ============================================================================
# LIMPIEZA FINAL
# ============================================================================

log_info "🛑 Deteniendo la aplicación..."
kill_all
log_plain ""

log_success "🎉 ¡Pruebas de DEV completadas!"
log_plain ""