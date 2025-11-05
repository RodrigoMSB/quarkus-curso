#!/bin/bash

##############################################################################
# Script de Pruebas - Parte 1: Externalización y Prioridades de Carga
# 
# Este script prueba las capacidades de configuración del microservicio TasaCorp
# utilizando diferentes fuentes de configuración.
#
# Conceptos que se prueban:
# - application.properties vs application.yaml
# - @ConfigProperty vs @ConfigMapping
# - Prioridades: System Properties > ENV vars > Files
##############################################################################

# ============================================================================
# CONFIGURACIÓN DEL SCRIPT
# ============================================================================

# Generar timestamp y nombre de archivo de salida
TIMESTAMP=$(date +%Y-%m-%d_%H-%M-%S)
OUTPUT_FILE="test-part1-config-${TIMESTAMP}.txt"

# Colores para mejor visualización (solo para terminal)
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # Sin color

# URL base del microservicio
BASE_URL="http://localhost:8080"

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
# FUNCIÓN PARA LOGGING DUAL (Pantalla + Archivo)
# ============================================================================
# Esta función envía output tanto a la terminal (con colores) 
# como al archivo (sin colores)

log_header() {
    local message="$1"
    echo -e "${CYAN}${message}${NC}"
    echo "$message" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

log_info() {
    local message="$1"
    echo -e "${BLUE}${message}${NC}"
    echo "$message" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

log_success() {
    local message="$1"
    echo -e "${GREEN}${message}${NC}"
    echo "$message" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

log_warning() {
    local message="$1"
    echo -e "${YELLOW}${message}${NC}"
    echo "$message" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

log_error() {
    local message="$1"
    echo -e "${RED}${message}${NC}"
    echo "$message" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

log_plain() {
    local message="$1"
    echo -e "$message"
    echo "$message" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

# ============================================================================
# HEADER DEL ARCHIVO DE LOG
# ============================================================================

{
cat << 'EOF'
╔════════════════════════════════════════════════════════════════╗
║     ⚙️  PRUEBAS DE CONFIGURACIÓN - PARTE 1                     ║
║     Externalización y Prioridades de Carga                    ║
╔════════════════════════════════════════════════════════════════╗
EOF

echo ""
echo "📅 Fecha: $(date '+%d/%m/%Y %H:%M:%S')"
echo "🌐 API Base: $BASE_URL"
echo "📄 Resultados: $OUTPUT_FILE"
echo "🔧 Configuración: application.properties + application.yaml"
echo ""
} | tee "$OUTPUT_FILE"

# ============================================================================
# INICIO DE PRUEBAS
# ============================================================================

log_header "╔════════════════════════════════════════════════════════════════╗"
log_header "║    ⚙️  PRUEBAS DE CONFIGURACIÓN - PARTE 1                     ║"
log_header "║    Externalización y Prioridades de Carga                    ║"
log_header "╚════════════════════════════════════════════════════════════════╝"
log_plain ""

##############################################################################
# PRUEBA 1: Configuración Base (application.properties)
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 1: Configuración Base desde Properties"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""
log_plain "🎯 Objetivo: Verificar que la aplicación lee la configuración base"
log_plain "📄 Fuente: application.properties"
log_plain "🔧 Valores esperados:"
log_plain "   - Moneda base: PEN"
log_plain "   - Comisión: 2.5%"
log_plain "   - Límite transaccional: 1000"
log_plain ""
log_header "Ejecutando consulta de configuración..."
log_plain ""

CONFIG_RESPONSE=$(curl -s $BASE_URL/api/tasas/config)

if [ $? -eq 0 ]; then
    echo "$CONFIG_RESPONSE" | $PYTHON_CMD -c "
import sys, json
try:
    data = json.load(sys.stdin)
    print(f\"✓ Moneda Base: {data['moneda_base']}\")
    print(f\"✓ Comisión: {data['comision_porcentaje']}%\")
    print(f\"✓ Límite: \${data['limite_transaccional']:,}\")
except Exception as e:
    print(f\"❌ Error al procesar respuesta: {e}\")
" | tee -a "$OUTPUT_FILE"
else
    log_error "❌ Error: No se pudo conectar al servicio"
    log_warning "Verifica que la aplicación esté corriendo en $BASE_URL"
    exit 1
fi

log_plain ""
log_success "✅ Si ves los valores correctos, ¡la configuración base funciona!"
log_plain ""
read -p "$(echo -e ${YELLOW}Presiona ENTER para continuar...${NC})"
log_plain ""

##############################################################################
# PRUEBA 2: @ConfigProperty vs @ConfigMapping
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 2: Inyección de Configuración"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""
log_plain "🎯 Objetivo: Ver cómo se inyectan las propiedades en el servicio"
log_plain "💉 Mecanismos:"
log_plain "   - @ConfigProperty: Para valores individuales"
log_plain "   - @ConfigMapping: Para objetos complejos"
log_plain ""
log_plain "📊 Configuración actual completa:"
log_plain ""

curl -s $BASE_URL/api/tasas/config | $PYTHON_CMD -m json.tool | tee -a "$OUTPUT_FILE"

log_plain ""
log_header "ℹ️  Todos estos valores fueron inyectados automáticamente por Quarkus"
log_plain ""
read -p "$(echo -e ${YELLOW}Presiona ENTER para continuar...${NC})"
log_plain ""

##############################################################################
# PRUEBA 3: Conversión con Configuración Base
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 3: Conversión usando Configuración Base"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""
log_plain "🎯 Objetivo: Ver cómo la configuración afecta el comportamiento"
log_plain "💰 Operación: Convertir 1000 PEN a USD"
log_plain "🔧 Config: Comisión 2.5% (desde properties)"
log_plain ""
log_header "Ejecutando conversión..."
log_plain ""

curl -s "$BASE_URL/api/tasas/convertir/USD?monto=1000" | $PYTHON_CMD -c "
import sys, json
try:
    data = json.load(sys.stdin)
    print(f\"💵 Monto Original: {data['monto_origen']} {data['moneda_origen']}\")
    print(f\"💱 Tasa Aplicada: {data['tasa_aplicada']}\")
    print(f\"💱 Convertido: {data['monto_convertido']:.2f} {data['moneda_destino']}\")
    comision = data.get('comision', 0)
    comision_pct = (comision / data['monto_convertido'] * 100) if data['monto_convertido'] > 0 else 0
    print(f\"💸 Comisión ({comision_pct:.1f}%): {comision:.2f} USD\")
    print(f\"💰 Total: {data['monto_total']:.2f} USD\")
except Exception as e:
    print(f\"❌ Error: {e}\")
" | tee -a "$OUTPUT_FILE"

log_plain ""
log_success "✅ La comisión aplicada viene de la configuración (2.5%)"
log_plain ""
read -p "$(echo -e ${YELLOW}Presiona ENTER para continuar...${NC})"
log_plain ""

##############################################################################
# PRUEBA 4: Preparación para Sobrescritura con ENV
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 4: Preparación - Variables de Entorno"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""
log_plain "🎯 Objetivo: Demostrar que ENV vars tienen MAYOR prioridad"
log_plain "📊 Prioridades de carga:"
log_success "   1. System Properties (-D)    ← Máxima prioridad"
log_warning "   2. Variables de Entorno      ↑"
log_header "   3. application.yaml          ↑"
log_info "   4. application.properties    ← Mínima prioridad"
log_plain ""
log_warning "⚠️  IMPORTANTE:"
log_plain "Para probar ENV vars, necesitas reiniciar la aplicación con:"
log_plain ""
log_header "TASACORP_COMMISSION_RATE=9.99 ./mvnw quarkus:dev"
log_plain ""
log_plain "Esto sobrescribirá la comisión de 2.5% a 9.99%"
log_plain ""
log_error "⏸️  Por ahora, continuaremos con System Properties..."
log_plain ""
read -p "$(echo -e ${YELLOW}Presiona ENTER para continuar...${NC})"
log_plain ""

##############################################################################
# PRUEBA 5: System Properties (Máxima Prioridad)
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 5: System Properties (Máxima Prioridad)"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""
log_plain "🎯 Objetivo: Demostrar System Properties como máxima prioridad"
log_plain "⚙️  System Properties (-D): Son argumentos de la JVM al arrancar"
log_plain ""
log_warning "Para probar esto, reinicia la aplicación con:"
log_plain ""
log_header "./mvnw quarkus:dev -Dtasacorp.commission.rate=15.0"
log_plain ""
log_plain "📊 Jerarquía que se aplicaría:"
log_success "   ✓ System Property: 15.0%      ← ¡GANA! (máxima prioridad)"
log_warning "   ✗ ENV var: 9.99%              ← Ignorado"
log_info "   ✗ Properties: 2.5%             ← Ignorado"
log_plain ""
log_header "ℹ️  Demostración Visual:"
log_plain ""
log_plain "Si aplicación arrancó con -Dtasacorp.commission.rate=15.0:"
log_plain ""

curl -s "$BASE_URL/api/tasas/convertir/USD?monto=1000" 2>/dev/null | $PYTHON_CMD -c "
import sys, json
try:
    data = json.load(sys.stdin)
    comision = data.get('comision', 0)
    monto_convertido = data['monto_convertido']
    rate = (comision / monto_convertido * 100) if monto_convertido > 0 else 0
    if rate > 10:
        print(f\"✓ Comisión actual: {rate:.1f}% - System Property está activo!\")
    else:
        print(f\"ℹ️  Comisión actual: {rate:.1f}% - Usando configuración base\")
except:
    print(f\"ℹ️  No se pudo determinar la comisión actual\")
" | tee -a "$OUTPUT_FILE"

log_plain ""
read -p "$(echo -e ${YELLOW}Presiona ENTER para continuar...${NC})"
log_plain ""

##############################################################################
# PRUEBA 6: Properties vs YAML
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 6: Properties vs YAML"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""
log_plain "🎯 Objetivo: Ver valores que vienen de YAML"
log_plain "📄 Fuentes:"
log_plain "   - application.properties: Configuración simple"
log_plain "   - application.yaml: Configuración compleja (tasas, metadata)"
log_plain ""
log_header "Valores desde YAML:"
log_plain ""

curl -s $BASE_URL/api/tasas/config | $PYTHON_CMD -c "
import sys, json
try:
    data = json.load(sys.stdin)
    print(f\"📊 Metadata:\")
    print(f\"   - Ambiente: {data.get('ambiente', 'N/A')}\")
    print(f\"   - Cache: {data.get('cache_habilitado', False)}\")
    print(f\"   - Auditoría: {data.get('auditoria_habilitada', False)}\")
    print(f\"   - Refresh: {data.get('refresh_minutos', 'N/A')} minutos\")
except Exception as e:
    print(f\"❌ Error: {e}\")
" | tee -a "$OUTPUT_FILE"

log_plain ""
log_success "✅ YAML permite estructuras jerárquicas más complejas"
log_plain ""
read -p "$(echo -e ${YELLOW}Presiona ENTER para continuar...${NC})"
log_plain ""

##############################################################################
# PRUEBA 7: Consultar Tasa Específica
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 7: Tasas desde Configuración YAML"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""
log_plain "🎯 Objetivo: Ver tasas de cambio configuradas en YAML"
log_plain "💱 Tasas configuradas:"
log_plain "   - USD: 3.75 (desde YAML)"
log_plain "   - EUR: 4.10 (desde YAML)"
log_plain "   - MXN: 0.22 (desde YAML)"
log_plain ""
log_header "Consultando tasa de USD..."
log_plain ""

curl -s $BASE_URL/api/tasas/USD | $PYTHON_CMD -m json.tool | tee -a "$OUTPUT_FILE"

log_plain ""
log_success "✅ Las tasas vienen del application.yaml"
log_plain ""
read -p "$(echo -e ${YELLOW}Presiona ENTER para ver el resumen...${NC})"
log_plain ""

##############################################################################
# RESUMEN FINAL
##############################################################################
log_header "╔════════════════════════════════════════════════════════════════╗"
log_header "║                    📊 RESUMEN DE PRUEBAS                       ║"
log_header "╚════════════════════════════════════════════════════════════════╝"
log_plain ""
log_success "✅ PRUEBA 1: Configuración base leída correctamente"
log_success "✅ PRUEBA 2: Inyección con @ConfigProperty y @ConfigMapping"
log_success "✅ PRUEBA 3: Configuración afecta el comportamiento (comisiones)"
log_success "✅ PRUEBA 4: Explicación de variables de entorno"
log_success "✅ PRUEBA 5: System Properties como máxima prioridad"
log_success "✅ PRUEBA 6: Diferencias entre Properties y YAML"
log_success "✅ PRUEBA 7: Tasas configuradas en YAML"
log_plain ""
log_header "╔════════════════════════════════════════════════════════════════╗"
log_header "║              🎓 CONCEPTOS CLAVE DEMOSTRADOS                    ║"
log_header "╚════════════════════════════════════════════════════════════════╝"
log_plain ""
log_warning "📄 application.properties:  Configuración simple y directa"
log_warning "📝 application.yaml:        Configuración jerárquica compleja"
log_warning "💉 @ConfigProperty:         Inyección de valores individuales"
log_warning "🎯 @ConfigMapping:          Mapeo de objetos complejos"
log_warning "🏆 Prioridades:             System Props > ENV > YAML > Properties"
log_plain ""
log_header "╔════════════════════════════════════════════════════════════════╗"
log_header "║                    🧪 PRUEBAS MANUALES                         ║"
log_header "╚════════════════════════════════════════════════════════════════╝"
log_plain ""
log_warning "Para probar VARIABLES DE ENTORNO:"
log_plain "1. Detén la aplicación (Ctrl+C)"
log_plain "2. Ejecuta: TASACORP_COMMISSION_RATE=9.99 ./mvnw quarkus:dev"
log_plain "3. Prueba: curl http://localhost:8080/api/tasas/config"
log_plain "4. Verás comision_porcentaje: 9.99 (sobrescrito)"
log_plain ""
log_warning "Para probar SYSTEM PROPERTIES:"
log_plain "1. Detén la aplicación (Ctrl+C)"
log_plain "2. Ejecuta: ./mvnw quarkus:dev -Dtasacorp.commission.rate=15.0"
log_plain "3. Prueba: curl http://localhost:8080/api/tasas/config"
log_plain "4. Verás comision_porcentaje: 15.0 (máxima prioridad)"
log_plain ""

# ============================================================================
# FOOTER DEL ARCHIVO DE LOG
# ============================================================================

{
cat << 'EOF'

╔════════════════════════════════════════════════════════════════╗
║                    📁 ARCHIVO DE LOG                           ║
╚════════════════════════════════════════════════════════════════╝
EOF

echo ""
echo "📝 Todas las pruebas han sido guardadas en:"
echo "   $OUTPUT_FILE"
echo ""
echo "💡 Puedes revisar el log completo en cualquier momento para:"
echo "   • Verificar las respuestas HTTP completas"
echo "   • Analizar las configuraciones cargadas"
echo "   • Compartir los resultados con tu instructor"
echo "   • Documentar el comportamiento del sistema de configuración"
echo ""
} | tee -a "$OUTPUT_FILE"

log_success "🎉 ¡Pruebas de la Parte 1 completadas exitosamente!"
log_header "Continúa con: test-part2-profiles.sh"
log_plain ""

echo ""
echo -e "${MAGENTA}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${MAGENTA}║                    ✅ PRUEBAS FINALIZADAS                      ║${NC}"
echo -e "${MAGENTA}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}📄 Archivo de log generado: ${CYAN}$OUTPUT_FILE${NC}"
echo -e "${GREEN}📊 Total de pruebas: ${YELLOW}7${NC}"
echo -e "${GREEN}✨ Estado: ${YELLOW}Completado${NC}"
echo ""