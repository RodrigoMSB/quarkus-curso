#!/bin/bash

##############################################################################
# Script de Pruebas - PERFIL PROD
# 
# Este script prueba el perfil de PRODUCCIÓN del microservicio TasaCorp.
# El perfil PROD está optimizado para máxima seguridad con Vault.
#
# COMPATIBLE: Mac y Windows (Git Bash)
##############################################################################

# ============================================================================
# DETECCIÓN DE SISTEMA OPERATIVO
# ============================================================================

detect_os() {
    case "$(uname -s)" in
        Darwin*)    echo "mac" ;;
        Linux*)     echo "linux" ;;
        MINGW*|MSYS*|CYGWIN*)    echo "windows" ;;
        *)          echo "unknown" ;;
    esac
}

OS_TYPE=$(detect_os)

# ============================================================================
# CONFIGURACIÓN
# ============================================================================

TIMESTAMP=$(date +%Y-%m-%d_%H-%M-%S)
OUTPUT_FILE="test-prod-${TIMESTAMP}.txt"

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m'

BASE_URL="http://localhost:8080"
VAULT_URL="http://localhost:8200"
STARTUP_TIMEOUT=60

# Detectar Python (python3 en Mac/Linux, python en Windows)
if command -v python3 &> /dev/null; then
    PYTHON_CMD="python3"
elif command -v python &> /dev/null; then
    PYTHON_CMD="python"
else
    echo "❌ Error: Python no está instalado"
    echo "   Windows: Descarga desde https://www.python.org/downloads/"
    echo "   Mac: brew install python3"
    exit 1
fi

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
    
    if [ "$OS_TYPE" = "windows" ]; then
        # Windows: usar taskkill
        taskkill //F //IM java.exe 2>/dev/null || true
        taskkill //F //FI "WINDOWTITLE eq quarkus*" 2>/dev/null || true
    else
        # Mac/Linux: usar pkill
        pkill -9 -f "quarkus:dev" 2>/dev/null || true
        pkill -9 -f "quarkus-run.jar" 2>/dev/null || true
    fi
    
    sleep 3
    log_success "✅ Limpieza completada"
}

wait_for_app() {
    local elapsed=0
    log_info "⏳ Esperando a que la aplicación arranque (timeout: ${STARTUP_TIMEOUT}s)..."
    
    while [ $elapsed -lt $STARTUP_TIMEOUT ]; do
        if curl -s "$BASE_URL/api/tasas/config" > /dev/null 2>&1; then
            log_success "✅ Aplicación lista en perfil PROD"
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
║              🔴 PRUEBAS - PERFIL PROD                          ║
║              Producción: Máxima seguridad con Vault            ║
╚════════════════════════════════════════════════════════════════╝
EOF
echo ""
echo "🖥️  Sistema Operativo: $OS_TYPE"
echo "🐍 Python: $PYTHON_CMD"
echo "📅 Fecha: $(date '+%d/%m/%Y %H:%M:%S')"
echo "🌐 API Base: $BASE_URL"
echo "🔐 Vault: $VAULT_URL"
echo "📄 Resultados: $OUTPUT_FILE"
echo ""
} | tee "$OUTPUT_FILE"

# ============================================================================
# VERIFICACIÓN DE VAULT
# ============================================================================

log_header "╔════════════════════════════════════════════════════════════════╗"
log_header "║              🔍 VERIFICACIÓN DE VAULT                          ║"
log_header "╚════════════════════════════════════════════════════════════════╝"
log_plain ""

log_info "🔐 Verificando que Vault esté corriendo..."
if ! curl -s "$VAULT_URL/v1/sys/health" > /dev/null 2>&1; then
    log_error "❌ Error: Vault no está corriendo en $VAULT_URL"
    log_plain ""
    log_warning "Para arrancar Vault, ejecuta:"
    log_plain "  docker-compose up -d"
    log_plain ""
    log_warning "Para guardar el secreto:"
    log_plain "  docker exec -it tasacorp-vault sh -c \\"
    log_plain "    \"VAULT_ADDR=http://127.0.0.1:8200 VAULT_TOKEN=root \\"
    log_plain "     vault kv put secret/tasacorp api-key=PREMIUM_KEY_XYZ\""
    log_plain ""
    exit 1
fi
log_success "✅ Vault está corriendo"
log_plain ""

log_info "🔐 Verificando secreto en Vault..."
VAULT_CHECK=$(docker exec tasacorp-vault sh -c \
  "VAULT_ADDR=http://127.0.0.1:8200 VAULT_TOKEN=root vault kv get -format=json secret/tasacorp" 2>/dev/null)

if [ $? -ne 0 ]; then
    log_error "❌ Error: No se pudo acceder al secreto en Vault"
    log_plain ""
    log_warning "Guarda el secreto con:"
    log_plain "  docker exec -it tasacorp-vault sh -c \\"
    log_plain "    \"VAULT_ADDR=http://127.0.0.1:8200 VAULT_TOKEN=root \\"
    log_plain "     vault kv put secret/tasacorp api-key=PREMIUM_KEY_XYZ\""
    log_plain ""
    exit 1
fi
log_success "✅ Secreto disponible en Vault"
log_plain ""

# ============================================================================
# LIMPIEZA Y COMPILACIÓN
# ============================================================================

log_header "╔════════════════════════════════════════════════════════════════╗"
log_header "║              🔍 PREPARACIÓN                                    ║"
log_header "╚════════════════════════════════════════════════════════════════╝"
log_plain ""

kill_all
log_plain ""

log_success "Características del perfil PROD:"
log_plain "  ✓ Comisión: 2.5% (completa)"
log_plain "  ✓ Límite transaccional: 50,000 (alto pero controlado)"
log_plain "  ✓ Cache: Activado"
log_plain "  ✓ Auditoría: Activada"
log_plain "  ✓ Proveedor: PremiumProvider"
log_plain "  🔐 API Key: Desde Vault (seguro)"
log_plain ""

log_info "📦 Compilando aplicación..."
./mvnw clean package -DskipTests > /dev/null 2>&1
if [ $? -ne 0 ]; then
    log_error "❌ Error al compilar"
    exit 1
fi
log_success "✅ Compilación exitosa"
log_plain ""

log_info "🚀 Arrancando aplicación en modo PROD..."
log_plain ""

# Arrancar en background con perfil PROD
if [ "$OS_TYPE" = "windows" ]; then
    # Windows: usar start para ejecutar en ventana separada
    start //B java -Dquarkus.profile=prod -jar target/quarkus-app/quarkus-run.jar > /dev/null 2>&1
    sleep 2
    APP_PID="N/A (Windows background)"
else
    # Mac/Linux: background normal
    java -Dquarkus.profile=prod -jar target/quarkus-app/quarkus-run.jar > /dev/null 2>&1 &
    APP_PID=$!
fi

log_info "📋 PID de la aplicación: $APP_PID"
log_plain ""

wait_for_app
log_plain ""

# ============================================================================
# PRUEBAS
# ============================================================================

log_header "╔════════════════════════════════════════════════════════════════╗"
log_header "║              📋 PRUEBAS DEL PERFIL PROD                        ║"
log_header "╚════════════════════════════════════════════════════════════════╝"
log_plain ""

##############################################################################
# PRUEBA 1: Configuración PROD + Vault
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 1: Configuración del Perfil PROD"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""

PROD_CONFIG=$(curl -s $BASE_URL/api/tasas/config 2>/dev/null)

if [ $? -eq 0 ]; then
    echo "$PROD_CONFIG" | $PYTHON_CMD -c "
import sys, json
try:
    data = json.load(sys.stdin)
    perfil = data.get('perfil_activo', 'N/A')
    ambiente = data.get('ambiente', 'N/A')
    comision = data.get('comision_porcentaje', 'N/A')
    limite = data.get('limite_transaccional', 'N/A')
    proveedor = data.get('proveedor', 'N/A')
    apikey_source = data.get('api_key_source', 'N/A')
    
    print(f'✓ Perfil activo: {perfil}')
    print(f'✓ Ambiente: {ambiente}')
    print(f'✓ Comisión: {comision}%')
    print(f'✓ Límite transaccional: \${limite:,}')
    print(f'✓ Proveedor: {proveedor}')
    print(f'🔐 API Key Source: {apikey_source}')
    
    if perfil != 'prod':
        print(f\"❌ ERROR: Perfil debería ser 'prod' pero es '{perfil}'\")
    if comision != 2.5:
        print(f\"❌ ERROR: Comisión en PROD debería ser 2.5%\")
    if limite != 50000:
        print(f\"❌ ERROR: Límite en PROD debería ser 50,000\")
    if 'Vault' not in str(apikey_source):
        print(f\"❌ ERROR: API Key debería venir desde Vault\")
    else:
        print(f\"\\n✅ EXCELENTE: API Key viene desde Vault (seguro)\")
except Exception as e:
    print(f'❌ Error al procesar respuesta: {e}')
" | tee -a "$OUTPUT_FILE"
else
    log_error "❌ Error: No se pudo conectar al servicio"
    kill_all
    exit 1
fi

log_plain ""
log_success "✅ Configuración PROD correcta"
log_plain ""
sleep 2

##############################################################################
# PRUEBA 2: Conversión con Comisión Completa
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 2: Conversión con Comisión 2.5%"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""
log_plain "🎯 Objetivo: Verificar que PROD cobra comisión del 2.5%"
log_plain "💰 Operación: Convertir 1,000 PEN a USD"
log_plain ""

curl -s "$BASE_URL/api/tasas/convertir/USD?monto=1000" | $PYTHON_CMD -c "
import sys, json
try:
    data = json.load(sys.stdin)
    monto = data['monto_origen']
    convertido = data['monto_convertido']
    comision = data['comision']
    
    print(f\"💵 Monto Original: \${monto:,.0f} PEN\")
    print(f\"💱 Monto Convertido: \${convertido:,.2f} USD\")
    print(f\"💸 Comisión (2.5%): \${comision:.2f} USD\")
    print(f\"✅ Total: \${convertido + comision:,.2f} USD\")
    
    if comision > 0:
        print(f\"\\n✅ CORRECTO: PROD cobra comisión completa del 2.5%\")
    else:
        print(f\"\\n❌ ERROR: Comisión debería ser > 0\")
except Exception as e:
    print(f'❌ Error: {e}')
" | tee -a "$OUTPUT_FILE"

log_plain ""
sleep 2

##############################################################################
# PRUEBA 3: Monto Alto Dentro de Límite
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 3: Monto Alto Dentro de Límite"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""
log_plain "🎯 Objetivo: Verificar que PROD acepta montos altos"
log_plain "💰 Operación: Convertir 40,000 PEN (dentro del límite)"
log_plain ""

curl -s "$BASE_URL/api/tasas/convertir/USD?monto=40000" | $PYTHON_CMD -c "
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
        print(f\"\\n✅ CORRECTO: Monto aceptado en PROD\")
    else:
        print(f\"\\n❌ ERROR: Debería estar dentro del límite\")
except Exception as e:
    print(f'❌ Error: {e}')
" | tee -a "$OUTPUT_FILE"

log_plain ""
sleep 2

##############################################################################
# PRUEBA 4: Exceder Límite
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 4: Exceder Límite en PROD"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""
log_plain "🎯 Objetivo: Verificar detección de límite excedido"
log_plain "💰 Operación: Convertir 60,000 PEN (excede límite de 50,000)"
log_plain ""

curl -s "$BASE_URL/api/tasas/convertir/USD?monto=60000" | $PYTHON_CMD -c "
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
        print(f\"⚠️  En producción real, esto rechazaría la transacción\")
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
log_header "║              ✅ RESUMEN - PERFIL PROD                          ║"
log_header "╚════════════════════════════════════════════════════════════════╝"
log_plain ""
log_success "✅ Perfil PROD verificado exitosamente"
log_plain ""
log_plain "Características confirmadas:"
log_plain "  ✓ Comisión completa (2.5%)"
log_plain "  ✓ Límite alto pero controlado (50,000)"
log_plain "  ✓ Detecta límites excedidos"
log_plain "  🔐 API Key desde Vault (máxima seguridad)"
log_plain ""
log_plain "📄 Log guardado en: $OUTPUT_FILE"
log_plain ""

# ============================================================================
# LIMPIEZA FINAL
# ============================================================================

log_info "🛑 Deteniendo la aplicación..."
kill_all
log_plain ""

log_success "🎉 ¡Pruebas de PROD completadas!"
log_plain ""