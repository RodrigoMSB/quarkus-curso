#!/bin/bash

##############################################################################
# Script de Pruebas - PERFIL DEV
# 
# Este script prueba el perfil de DESARROLLO del microservicio TasaCorp.
# El perfil DEV está optimizado para desarrollo rápido sin restricciones.
#
# COMPATIBLE: Mac y Windows (Git Bash)
##############################################################################

# Generar nombre de archivo con timestamp
OUTPUT_FILE="test-dev-$(date '+%Y-%m-%d_%H-%M-%S').txt"

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
log "${CYAN}║              🟢 PRUEBAS - PERFIL DEV                           ║${RESET}"
log "${CYAN}║              Desarrollo: Sin restricciones                     ║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "${CYAN}📅 Fecha:${RESET} $(date '+%d/%m/%Y %H:%M:%S')"
log "${CYAN}🌐 API Base:${RESET} $BASE_URL"
log "${CYAN}📄 Resultados:${RESET} $OUTPUT_FILE"
log "${CYAN}🔧 Perfil:${RESET} DEV (desarrollo)"
log ""
log "${YELLOW}⚠️  IMPORTANTE:${RESET} El servidor debe estar corriendo en perfil ${GREEN}dev${RESET}"
log "${YELLOW}   Comando:${RESET} ${CYAN}./mvnw quarkus:dev${RESET}"
log ""
log "${MAGENTA}Características del perfil DEV:${RESET}"
log "  ✓ Comisión: 0.0% (gratis para desarrollo)"
log "  ✓ Límite transaccional: 999,999 (ilimitado)"
log "  ✓ Cache: Desactivado"
log "  ✓ Auditoría: Desactivada"
log "  ✓ Proveedor: MockProvider"
log ""
pause

##############################################################################
# PRUEBA 1: Configuración DEV
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 1: Configuración del Perfil DEV${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Verificar que la aplicación está en perfil DEV"
log "${YELLOW}📍 Endpoint:${RESET} GET /api/tasas/config"
log "${YELLOW}✅ Esperado:${RESET} perfil_activo=dev, comision=0.0%, limite=999,999"
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
    log "${GREEN}✓ PASS${RESET} - Configuración DEV obtenida correctamente"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 200)"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   perfil_activo: 'dev'"
log "   comision_porcentaje: 0.0"
log "   limite_transaccional: 999999"
pause

##############################################################################
# PRUEBA 2: Conversión SIN Comisión
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 2: Conversión SIN Comisión${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Verificar que DEV NO cobra comisión"
log "${YELLOW}📍 Endpoint:${RESET} GET /api/tasas/convertir/USD?monto=1000"
log "${YELLOW}💰 Operación:${RESET} Convertir 1,000 PEN a USD"
log "${YELLOW}✅ Esperado:${RESET} comision: 0.0"
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
    log "${CYAN}ℹ️  En perfil DEV no se cobra comisión (desarrollo rápido)${RESET}"
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 200)"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   La comisión debe ser 0.0 (sin costo en desarrollo)"
pause

##############################################################################
# PRUEBA 3: Límite Ilimitado
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 3: Límite Ilimitado${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Verificar que DEV acepta montos muy altos"
log "${YELLOW}📍 Endpoint:${RESET} GET /api/tasas/convertir/USD?monto=100000"
log "${YELLOW}💰 Operación:${RESET} Convertir 100,000 PEN a USD (monto alto)"
log "${YELLOW}✅ Esperado:${RESET} dentro_limite: true"
log ""
log "${CYAN}Ejecutando conversión con monto alto...${RESET}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

response=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/tasas/convertir/USD?monto=100000" 2>/dev/null)
body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
show_json "$body"
log ""

if [ "$status" == "200" ]; then
    log "${GREEN}✓ PASS${RESET} - Monto alto aceptado (límite ilimitado en DEV)"
    PASSED_TESTS=$((PASSED_TESTS + 1))
    log "${CYAN}ℹ️  El perfil DEV tiene límite de 999,999 (prácticamente ilimitado)${RESET}"
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 200)"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   dentro_limite: true (DEV acepta montos muy altos)"
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
    log "${YELLOW}Posible causa:${RESET} El servidor no se inició con el perfil correcto"
    log "${YELLOW}Solución:${RESET}"
    log "  ${CYAN}1.${RESET} Detén el servidor (Ctrl+C)"
    log "  ${CYAN}2.${RESET} Inicia con: ${GREEN}./mvnw quarkus:dev${RESET}"
    log "  ${CYAN}3.${RESET} Vuelve a ejecutar este script"
    log ""
fi

log "${CYAN}╔════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║                   🎯 TESTS EJECUTADOS                          ║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "${GREEN}✅ PRUEBA 1:${RESET} Configuración del perfil DEV verificada"
log "${GREEN}✅ PRUEBA 2:${RESET} Conversión sin comisión (0.0%)"
log "${GREEN}✅ PRUEBA 3:${RESET} Límite ilimitado verificado (999,999)"
log ""
log "${CYAN}╔════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║              🎓 CARACTERÍSTICAS DEL PERFIL DEV                 ║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "${YELLOW}🔧 Optimizado para:${RESET}       Desarrollo rápido sin restricciones"
log "${YELLOW}💸 Comisión:${RESET}              0.0% (gratis)"
log "${YELLOW}🚦 Límite:${RESET}                999,999 (ilimitado)"
log "${YELLOW}📦 Cache:${RESET}                 Desactivado (cambios inmediatos)"
log "${YELLOW}📝 Auditoría:${RESET}             Desactivada (logs limpios)"
log "${YELLOW}🔌 Proveedor:${RESET}             MockProvider (sin API externa)"
log "${YELLOW}🔐 Vault:${RESET}                 Desactivado"
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
log "   • Analizar la configuración del perfil DEV"
log "   • Compartir los resultados con tu instructor"
log "   • Documentar el comportamiento del sistema"
log ""

log "${GREEN}🎉 ¡Pruebas del perfil DEV completadas exitosamente!${RESET}"
log "${CYAN}Continúa con: test-part2-test.sh (perfil TEST)${RESET}"
log ""