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

# Colores para mejor visualización
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # Sin color

# URL base del microservicio
BASE_URL="http://localhost:8080"

echo -e "${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║    ⚙️  PRUEBAS DE CONFIGURACIÓN - PARTE 1                 ║${NC}"
echo -e "${CYAN}║    Externalización y Prioridades de Carga                ║${NC}"
echo -e "${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"
echo ""

##############################################################################
# PRUEBA 1: Configuración Base (application.properties)
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 1: Configuración Base desde Properties${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Verificar que la aplicación lee la configuración base"
echo -e "📄 Fuente: application.properties"
echo -e "🔧 Valores esperados:"
echo -e "   - Moneda base: PEN"
echo -e "   - Comisión: 2.5%"
echo -e "   - Límite transaccional: 1000"
echo ""
echo -e "${CYAN}Ejecutando...${NC}"
echo ""

curl -s $BASE_URL/api/tasas/config | python3 -c "
import sys, json
data = json.load(sys.stdin)
print(f\"✓ Moneda Base: {data['moneda_base']}\")
print(f\"✓ Comisión: {data['comision_porcentaje']}%\")
print(f\"✓ Límite: \${data['limite_transaccional']}\")
"

echo ""
echo -e "${GREEN}✅ Si ves los valores correctos, ¡la configuración base funciona!${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 2: @ConfigProperty vs @ConfigMapping
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 2: Inyección de Configuración${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Ver cómo se inyectan las propiedades en el servicio"
echo -e "💉 Mecanismos:"
echo -e "   - @ConfigProperty: Para valores individuales"
echo -e "   - @ConfigMapping: Para objetos complejos"
echo ""
echo -e "📊 Configuración actual completa:"
echo ""

curl -s $BASE_URL/api/tasas/config | python3 -m json.tool

echo ""
echo -e "${CYAN}ℹ️  Todos estos valores fueron inyectados automáticamente por Quarkus${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 3: Conversión con Configuración Base
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 3: Conversión usando Configuración Base${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Ver cómo la configuración afecta el comportamiento"
echo -e "💰 Operación: Convertir 1000 PEN a USD"
echo -e "🔧 Config: Comisión 2.5% (desde properties)"
echo ""
echo -e "${CYAN}Ejecutando conversión...${NC}"
echo ""

curl -s "$BASE_URL/api/tasas/convertir/USD?monto=1000" | python3 -c "
import sys, json
data = json.load(sys.stdin)
print(f\"💵 Monto Original: {data['monto_origen']} {data['moneda_origen']}\")
print(f\"💱 Convertido: {data['monto_convertido']} {data['moneda_destino']}\")
print(f\"💸 Comisión ({data.get('comision', 0) / data['monto_convertido'] * 100:.1f}%): {data.get('comision', 0)} USD\")
print(f\"💰 Total: {data['monto_total']} USD\")
"

echo ""
echo -e "${GREEN}✅ La comisión aplicada viene de la configuración (2.5%)${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 4: Preparación para Sobrescritura con ENV
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 4: Preparación - Variable de Entorno${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Demostrar que ENV vars tienen MAYOR prioridad"
echo -e "📊 Prioridades de carga:"
echo -e "   ${GREEN}1. System Properties (-D)${NC}    ← Máxima prioridad"
echo -e "   ${YELLOW}2. Variables de Entorno${NC}      ↑"
echo -e "   ${CYAN}3. application.yaml${NC}          ↑"
echo -e "   ${BLUE}4. application.properties${NC}    ← Mínima prioridad"
echo ""
echo -e "${YELLOW}⚠️  IMPORTANTE:${NC}"
echo -e "Para probar ENV vars, necesitas reiniciar la aplicación con:"
echo ""
echo -e "${CYAN}TASACORP_COMMISSION_RATE=9.99 ./mvnw quarkus:dev${NC}"
echo ""
echo -e "Esto sobrescribirá la comisión de 2.5% a 9.99%"
echo ""
echo -e "${RED}⏸️  Por ahora, continuaremos con System Properties...${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 5: System Properties (Máxima Prioridad)
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 5: System Properties (Máxima Prioridad)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Demostrar System Properties como máxima prioridad"
echo -e "⚙️  System Properties (-D): Son argumentos de la JVM al arrancar"
echo ""
echo -e "${YELLOW}Para probar esto, reinicia la aplicación con:${NC}"
echo ""
echo -e "${CYAN}./mvnw quarkus:dev -Dtasacorp.commission.rate=15.0${NC}"
echo ""
echo -e "📊 Jerarquía que se aplicaría:"
echo -e "   ${GREEN}✓ System Property: 15.0%${NC}      ← ¡GANA! (máxima prioridad)"
echo -e "   ${YELLOW}✗ ENV var: 9.99%${NC}              ← Ignorado"
echo -e "   ${BLUE}✗ Properties: 2.5%${NC}             ← Ignorado"
echo ""
echo -e "${CYAN}ℹ️  Demostración Visual:${NC}"
echo ""
echo -e "Si aplicación arrancó con -Dtasacorp.commission.rate=15.0:"
echo ""
curl -s "$BASE_URL/api/tasas/convertir/USD?monto=1000" 2>/dev/null | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    rate = data.get('comision', 0) / data['monto_convertido'] * 100 if data['monto_convertido'] > 0 else 0
    if rate > 10:
        print(f\"${GREEN}✓ Comisión actual: {rate:.1f}% - System Property está activo!${NC}\")
    else:
        print(f\"${YELLOW}ℹ️  Comisión actual: {rate:.1f}% - Usando configuración base${NC}\")
except:
    print(f\"${YELLOW}ℹ️  No se pudo determinar la comisión actual${NC}\")
"

echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 6: Properties vs YAML
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 6: Properties vs YAML${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Ver valores que vienen de YAML"
echo -e "📄 Fuentes:"
echo -e "   - application.properties: Configuración simple"
echo -e "   - application.yaml: Configuración compleja (tasas, metadata)"
echo ""
echo -e "${CYAN}Valores desde YAML:${NC}"
echo ""

curl -s $BASE_URL/api/tasas/config | python3 -c "
import sys, json
data = json.load(sys.stdin)
print(f\"📊 Metadata:")
print(f\"   - Ambiente: {data.get('ambiente', 'N/A')}\")
print(f\"   - Cache: {data.get('cache_habilitado', False)}\")
print(f\"   - Auditoría: {data.get('auditoria_habilitada', False)}\")
print(f\"   - Refresh: {data.get('refresh_minutos', 'N/A')} minutos\")
"

echo ""
echo -e "${GREEN}✅ YAML permite estructuras jerárquicas más complejas${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 7: Consultar Tasa Específica
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 7: Tasas desde Configuración YAML${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Ver tasas de cambio configuradas en YAML"
echo -e "💱 Tasas configuradas:"
echo -e "   - USD: 3.75 (desde YAML)"
echo -e "   - EUR: 4.10 (desde YAML)"
echo -e "   - MXN: 0.22 (desde YAML)"
echo ""
echo -e "${CYAN}Consultando tasa de USD...${NC}"
echo ""

curl -s $BASE_URL/api/tasas/USD | python3 -m json.tool

echo ""
echo -e "${GREEN}✅ Las tasas vienen del application.yaml${NC}"
echo ""
read -p "Presiona ENTER para ver el resumen..."
echo ""

##############################################################################
# RESUMEN FINAL
##############################################################################
echo -e "${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║                    📊 RESUMEN DE PRUEBAS                  ║${NC}"
echo -e "${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}✅ PRUEBA 1:${NC} Configuración base leída correctamente"
echo -e "${GREEN}✅ PRUEBA 2:${NC} Inyección con @ConfigProperty y @ConfigMapping"
echo -e "${GREEN}✅ PRUEBA 3:${NC} Configuración afecta el comportamiento (comisiones)"
echo -e "${GREEN}✅ PRUEBA 4:${NC} Explicación de variables de entorno"
echo -e "${GREEN}✅ PRUEBA 5:${NC} System Properties como máxima prioridad"
echo -e "${GREEN}✅ PRUEBA 6:${NC} Diferencias entre Properties y YAML"
echo -e "${GREEN}✅ PRUEBA 7:${NC} Tasas configuradas en YAML"
echo ""
echo -e "${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║              🎓 CONCEPTOS CLAVE DEMOSTRADOS               ║${NC}"
echo -e "${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}📄 application.properties:${NC}  Configuración simple y directa"
echo -e "${YELLOW}📝 application.yaml:${NC}        Configuración jerárquica compleja"
echo -e "${YELLOW}💉 @ConfigProperty:${NC}         Inyección de valores individuales"
echo -e "${YELLOW}🎯 @ConfigMapping:${NC}          Mapeo de objetos complejos"
echo -e "${YELLOW}🏆 Prioridades:${NC}             System Props > ENV > YAML > Properties"
echo ""
echo -e "${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║                    🧪 PRUEBAS MANUALES                    ║${NC}"
echo -e "${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}Para probar VARIABLES DE ENTORNO:${NC}"
echo -e "1. Detén la aplicación (Ctrl+C)"
echo -e "2. Ejecuta: ${CYAN}TASACORP_COMMISSION_RATE=9.99 ./mvnw quarkus:dev${NC}"
echo -e "3. Prueba: ${CYAN}curl http://localhost:8080/api/tasas/config$\{NC\}"
echo -e "4. Verás comision_porcentaje: 9.99 (sobrescrito)"
echo ""
echo -e "${YELLOW}Para probar SYSTEM PROPERTIES:${NC}"
echo -e "1. Detén la aplicación (Ctrl+C)"
echo -e "2. Ejecuta: ${CYAN}./mvnw quarkus:dev -Dtasacorp.commission.rate=15.0${NC}"
echo -e "3. Prueba: ${CYAN}curl http://localhost:8080/api/tasas/config$\{NC\}"
echo -e "4. Verás comision_porcentaje: 15.0 (máxima prioridad)"
echo ""
echo -e "${GREEN}🎉 ¡Pruebas de la Parte 1 completadas!${NC}"
echo -e "${CYAN}Continúa con: test-part2-profiles.sh${NC}"
echo ""
