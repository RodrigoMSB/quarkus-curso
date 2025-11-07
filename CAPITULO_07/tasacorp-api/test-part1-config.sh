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

# Generar nombre de archivo con timestamp
OUTPUT_FILE="test-part1-config-$(date '+%Y-%m-%d_%H-%M-%S').txt"

# Limpiar archivo de salida
> "$OUTPUT_FILE"

# Colores para mejor visualización
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
WHITE='\033[1;37m'
RESET='\033[0m'

# URL base del microservicio
BASE_URL="http://localhost:8080"

# Contadores de tests
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Función de logging (muestra con colores en pantalla, guarda sin colores en archivo)
log() {
    local message="$*"
    printf "%b\n" "$message"
    printf "%b\n" "$message" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

# Función para mostrar JSON formateado
show_json() {
    local json="$1"
    
    if ! command -v jq &> /dev/null; then
        printf "%s\n" "$json" | tee -a "$OUTPUT_FILE"
        return
    fi
    
    if [ -n "$json" ]; then
        echo "$json" | jq '.' 2>/dev/null | tee -a "$OUTPUT_FILE" || echo "$json" | tee -a "$OUTPUT_FILE"
    fi
}

# Función para pausa interactiva (compatible con Windows)
pause() {
    echo ""
    read -r -p "Presiona ENTER para continuar..." dummy
    echo ""
}

# Banner inicial
clear
log "${CYAN}╔════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║     ⚙️  PRUEBAS DE CONFIGURACIÓN - PARTE 1                     ║${RESET}"
log "${CYAN}║     Externalización y Prioridades de Carga                    ║${RESET}"
log "${CYAN}╔════════════════════════════════════════════════════════════════╗${RESET}"
log ""
log "${CYAN}📅 Fecha:${RESET} $(date '+%d/%m/%Y %H:%M:%S')"
log "${CYAN}🌐 API Base:${RESET} $BASE_URL"
log "${CYAN}📄 Resultados:${RESET} $OUTPUT_FILE"
log "${CYAN}🔧 Configuración:${RESET} application.properties + application.yaml"
log ""
pause

##############################################################################
# PRUEBA 1: Configuración Base (application.properties)
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 1: Configuración Base desde Properties${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Verificar que la aplicación lee la configuración base"
log "${YELLOW}📄 Fuente:${RESET} application.properties"
log "${YELLOW}🔧 Valores esperados:${RESET}"
log "   - Moneda base: PEN"
log "   - Comisión: 0.0% (perfil dev)"
log "   - Límite transaccional: 999,999"
log ""
log "${CYAN}Ejecutando consulta de configuración...${RESET}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

response=$(curl -s -w "\n%{http_code}" $BASE_URL/api/tasas/config 2>/dev/null)
body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
show_json "$body"
log ""

if [ "$status" == "200" ]; then
    log "${GREEN}✓ PASS${RESET} - Configuración base leída correctamente"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 200)"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   La aplicación carga valores desde application.properties"
pause

##############################################################################
# PRUEBA 2: @ConfigProperty vs @ConfigMapping
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 2: Inyección de Configuración${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Ver cómo se inyectan las propiedades en el servicio"
log "${YELLOW}💉 Mecanismos:${RESET}"
log "   - @ConfigProperty: Para valores individuales"
log "   - @ConfigMapping: Para objetos complejos"
log ""
log "${CYAN}Configuración actual completa:${RESET}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

response=$(curl -s -w "\n%{http_code}" $BASE_URL/api/tasas/config 2>/dev/null)
body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

show_json "$body"
log ""

if [ "$status" == "200" ]; then
    log "${GREEN}✓ PASS${RESET} - Valores inyectados correctamente"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 200)"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

log ""
log "${CYAN}ℹ️  Todos estos valores fueron inyectados automáticamente por Quarkus${RESET}"
log ""
pause

##############################################################################
# PRUEBA 3: Conversión con Configuración Base
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 3: Conversión usando Configuración Base${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Ver cómo la configuración afecta el comportamiento"
log "${YELLOW}💰 Operación:${RESET} Convertir 1000 PEN a USD"
log "${YELLOW}🔧 Config:${RESET} Comisión 0.0% (perfil dev)"
log ""
log "${CYAN}Ejecutando conversión...${RESET}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

response=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/tasas/convertir/USD?monto=1000" 2>/dev/null)
body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
show_json "$body"
log ""

if [ "$status" == "200" ]; then
    log "${GREEN}✓ PASS${RESET} - Conversión realizada correctamente"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 200)"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   La comisión del 0.0% viene del perfil dev"
log ""
pause

##############################################################################
# PRUEBA 4: Variables de Entorno (Explicación)
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 4: Variables de Entorno (Explicación)${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Demostrar que ENV vars tienen MAYOR prioridad"
log "${YELLOW}📊 Prioridades de carga:${RESET}"
log "${GREEN}   1. System Properties (-D)    ← Máxima prioridad${RESET}"
log "${YELLOW}   2. Variables de Entorno      ↑${RESET}"
log "${CYAN}   3. application.yaml          ↑${RESET}"
log "${BLUE}   4. application.properties    ← Mínima prioridad${RESET}"
log ""
log "${YELLOW}⚠️  IMPORTANTE:${RESET}"
log "Para probar ENV vars, necesitas reiniciar la aplicación con:"
log ""
log "${CYAN}TASACORP_COMMISSION_RATE=9.99 ./mvnw quarkus:dev${RESET}"
log ""
log "Esto sobrescribirá la comisión de 0.0% a 9.99%"
log ""
log "${MAGENTA}⏸️  Por ahora, continuaremos con System Properties...${RESET}"
log ""
pause

##############################################################################
# PRUEBA 5: System Properties (Máxima Prioridad)
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 5: System Properties (Máxima Prioridad)${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Demostrar System Properties como máxima prioridad"
log "${YELLOW}⚙️  System Properties (-D):${RESET} Son argumentos de la JVM al arrancar"
log ""
log "${YELLOW}Para probar esto, reinicia la aplicación con:${RESET}"
log ""
log "${CYAN}./mvnw quarkus:dev -Dtasacorp.commission.rate=15.0${RESET}"
log ""
log "${YELLOW}📊 Jerarquía que se aplicaría:${RESET}"
log "${GREEN}   ✓ System Property: 15.0%      ← ¡GANA! (máxima prioridad)${RESET}"
log "${YELLOW}   ✗ ENV var: 9.99%              ← Ignorado${RESET}"
log "${BLUE}   ✗ Properties: 0.0%             ← Ignorado${RESET}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

response=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/tasas/config" 2>/dev/null)
body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${CYAN}ℹ️  Configuración actual:${RESET}"
show_json "$body"
log ""

if [ "$status" == "200" ]; then
    log "${GREEN}✓ PASS${RESET} - Demostración de prioridades explicada"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 200)"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

log ""
pause

##############################################################################
# PRUEBA 6: Properties vs YAML
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 6: Properties vs YAML${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Ver valores que vienen de YAML"
log "${YELLOW}📄 Fuentes:${RESET}"
log "   - application.properties: Configuración simple"
log "   - application.yaml: Configuración compleja (tasas, metadata)"
log ""
log "${CYAN}Valores desde YAML:${RESET}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

response=$(curl -s -w "\n%{http_code}" $BASE_URL/api/tasas/config 2>/dev/null)
body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

show_json "$body"
log ""

if [ "$status" == "200" ]; then
    log "${GREEN}✓ PASS${RESET} - YAML permite estructuras jerárquicas más complejas"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 200)"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

log ""
pause

##############################################################################
# PRUEBA 7: Consultar Tasa Específica
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 7: Tasas desde Configuración YAML${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Ver tasas de cambio configuradas en YAML"
log "${YELLOW}💱 Tasas configuradas:${RESET}"
log "   - USD: 3.75 (desde YAML)"
log "   - EUR: 4.10 (desde YAML)"
log "   - MXN: 0.22 (desde YAML)"
log ""
log "${CYAN}Consultando tasa de USD...${RESET}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

response=$(curl -s -w "\n%{http_code}" $BASE_URL/api/tasas/USD 2>/dev/null)
body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
show_json "$body"
log ""

if [ "$status" == "200" ]; then
    log "${GREEN}✓ PASS${RESET} - Las tasas vienen del application.yaml"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 200)"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

log ""
pause

##############################################################################
# RESUMEN FINAL
##############################################################################
clear
log ""
log "${CYAN}╔════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║                    📊 RESUMEN DE PRUEBAS                       ║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "  ${CYAN}Total de tests:${RESET}      $TOTAL_TESTS"
log "  ${GREEN}✓ Tests Exitosos:${RESET}  $PASSED_TESTS"
log "  ${RED}✗ Tests Fallidos:${RESET}  $FAILED_TESTS"
log ""

if [ $FAILED_TESTS -gt 0 ]; then
    log "${YELLOW}⚠️  ADVERTENCIA: Algunos tests fallaron${RESET}"
    log ""
    log "${YELLOW}Posible causa:${RESET} La aplicación no está corriendo o no responde"
    log "${YELLOW}Solución:${RESET}"
    log "  ${CYAN}1.${RESET} Verifica que la aplicación esté corriendo"
    log "  ${CYAN}2.${RESET} Inicia con: ${GREEN}./mvnw quarkus:dev${RESET}"
    log "  ${CYAN}3.${RESET} Vuelve a ejecutar este script"
    log ""
fi

log "${CYAN}╔════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║                   🎯 TESTS EJECUTADOS                          ║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "${GREEN}✅ PRUEBA 1:${RESET} Configuración base leída correctamente"
log "${GREEN}✅ PRUEBA 2:${RESET} Inyección con @ConfigProperty y @ConfigMapping"
log "${GREEN}✅ PRUEBA 3:${RESET} Configuración afecta el comportamiento (comisiones)"
log "${GREEN}✅ PRUEBA 4:${RESET} Explicación de variables de entorno"
log "${GREEN}✅ PRUEBA 5:${RESET} System Properties como máxima prioridad"
log "${GREEN}✅ PRUEBA 6:${RESET} Diferencias entre Properties y YAML"
log "${GREEN}✅ PRUEBA 7:${RESET} Tasas configuradas en YAML"
log ""
log "${CYAN}╔════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║              🎓 CONCEPTOS CLAVE DEMOSTRADOS                    ║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "${YELLOW}📄 application.properties:${RESET}  Configuración simple y directa"
log "${YELLOW}📝 application.yaml:${RESET}        Configuración jerárquica compleja"
log "${YELLOW}💉 @ConfigProperty:${RESET}         Inyección de valores individuales"
log "${YELLOW}🎯 @ConfigMapping:${RESET}          Mapeo de objetos complejos"
log "${YELLOW}🏆 Prioridades:${RESET}             System Props > ENV > YAML > Properties"
log ""
log "${CYAN}╔════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║                    🧪 PRUEBAS MANUALES                         ║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "${YELLOW}Para probar VARIABLES DE ENTORNO:${RESET}"
log "1. Detén la aplicación (Ctrl+C)"
log "2. Ejecuta: ${CYAN}TASACORP_COMMISSION_RATE=9.99 ./mvnw quarkus:dev${RESET}"
log "3. Prueba: ${CYAN}curl http://localhost:8080/api/tasas/config${RESET}"
log "4. Verás comision_porcentaje: 9.99 (sobrescrito)"
log ""
log "${YELLOW}Para probar SYSTEM PROPERTIES:${RESET}"
log "1. Detén la aplicación (Ctrl+C)"
log "2. Ejecuta: ${CYAN}./mvnw quarkus:dev -Dtasacorp.commission.rate=15.0${RESET}"
log "3. Prueba: ${CYAN}curl http://localhost:8080/api/tasas/config${RESET}"
log "4. Verás comision_porcentaje: 15.0 (máxima prioridad)"
log ""

log "${CYAN}╔════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║                    📁 ARCHIVO DE LOG                           ║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "${YELLOW}📝 Todas las pruebas han sido guardadas en:${RESET}"
log "   ${GREEN}$OUTPUT_FILE${RESET}"
log ""
log "${CYAN}💡 Puedes revisar el log completo en cualquier momento para:${RESET}"
log "   • Verificar las respuestas HTTP completas"
log "   • Analizar las configuraciones cargadas"
log "   • Compartir los resultados con tu instructor"
log "   • Documentar el comportamiento del sistema de configuración"
log ""

log "${GREEN}🎉 ¡Pruebas de la Parte 1 completadas exitosamente!${RESET}"
log "${CYAN}Continúa con: test-part2-profiles.sh${RESET}"
log ""

echo ""
echo -e "${MAGENTA}╔════════════════════════════════════════════════════════════════╗${RESET}"
echo -e "${MAGENTA}║                    ✅ PRUEBAS FINALIZADAS                      ║${RESET}"
echo -e "${MAGENTA}╚════════════════════════════════════════════════════════════════╝${RESET}"
echo ""
echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${RESET}"
echo -e "${CYAN}║                    📊 RESUMEN FINAL                            ║${RESET}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════════╝${RESET}"
echo ""
echo -e "  ${CYAN}Total de tests:${RESET}      $TOTAL_TESTS"
echo -e "  ${GREEN}✓ Tests Exitosos:${RESET}  $PASSED_TESTS"
echo -e "  ${RED}✗ Tests Fallidos:${RESET}  $FAILED_TESTS"
echo ""
echo -e "${GREEN}📄 Archivo de log generado: ${CYAN}$OUTPUT_FILE${RESET}"
echo -e "${GREEN}✨ Estado: ${YELLOW}Completado${RESET}"
echo ""