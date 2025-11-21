#!/bin/bash

##############################################################################
# Script de Pruebas: Multi vs Uni - Streaming Reactivo con Mutiny
##############################################################################

OUTPUT_FILE="test-multi-streaming-$(date '+%Y-%m-%d_%H-%M-%S').txt"
> "$OUTPUT_FILE"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
WHITE='\033[1;37m'
RESET='\033[0m'

BASE_URL="http://localhost:8080"
API_PATH="/api/v1/productos/reactivo"

TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

log() {
    local message="$*"
    printf "%b\n" "$message"
    printf "%b\n" "$message" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

show_json() {
    local json="$1"
    if ! command -v jq &> /dev/null; then
        printf "%s\n" "$json" | sed 's/,/,\n  /g' | tee -a "$OUTPUT_FILE"
        return
    fi
    if [ -n "$json" ]; then
        echo "$json" | jq '.' 2>/dev/null | tee -a "$OUTPUT_FILE" || echo "$json" | tee -a "$OUTPUT_FILE"
    fi
}

pause() {
    echo ""
    read -r -p "Presiona ENTER para continuar..." dummy
    echo ""
}

##############################################################################
# BANNER
##############################################################################
clear
log "${CYAN}╔════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║          🌊 MULTI vs UNI - STREAMING REACTIVO                  ║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "${CYAN}📅 Fecha:${RESET} $(date '+%d/%m/%Y %H:%M:%S')"
log "${CYAN}🌐 API Base:${RESET} $BASE_URL"
log "${CYAN}📄 Resultados:${RESET} $OUTPUT_FILE"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Ver la diferencia REAL entre:"
log "   • ${GREEN}Uni<List<T>>${RESET} = UN valor (lista completa)"
log "   • ${BLUE}Multi<T>${RESET} = MÚLTIPLES valores (streaming PROGRESIVO)"
log ""
pause

##############################################################################
# PASO 1: Verificar servidor
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}🔌 PASO 1: Verificar Conectividad${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}${API_PATH}" 2>/dev/null || echo "000")

if [ "$HTTP_CODE" != "200" ]; then
    log "${RED}✗ FAIL${RESET} - Servidor no disponible (HTTP $HTTP_CODE)"
    log ""
    log "${YELLOW}Inicia Quarkus: ${CYAN}./mvnw quarkus:dev${RESET}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
    exit 1
fi

log "${GREEN}✓ PASS${RESET} - Servidor disponible"
PASSED_TESTS=$((PASSED_TESTS + 1))
pause

##############################################################################
# PASO 2: Crear datos masivos
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📦 PASO 2: Preparar Datos de Prueba (Carga Masiva)${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}Creando 20 productos automáticamente...${RESET}"
log ""

response=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}${API_PATH}/carga-masiva/20" 2>/dev/null)
status=$(echo "$response" | tail -n 1)

if [ "$status" == "200" ]; then
    log "${GREEN}✓ 20 productos creados exitosamente${RESET}"
else
    log "${YELLOW}⚠ Productos ya existen o error (HTTP $status)${RESET}"
fi

log ""
log "${GREEN}✓ Datos preparados${RESET}"
pause

##############################################################################
# PASO 3: UNI - Todo de una vez
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PASO 3: UNI - Toda la lista de UNA VEZ${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🔧 Tipo:${RESET} ${GREEN}Uni<List<Producto>>${RESET}"
log "${YELLOW}📍 Endpoint:${RESET} GET ${API_PATH}"
log ""
log "${CYAN}╔═══════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║  ¿QUÉ ES UNI?                                             ║${RESET}"
log "${CYAN}╚═══════════════════════════════════════════════════════════╝${RESET}"
log ""
log "  ${YELLOW}Analogía:${RESET} Un vaso de agua"
log "    • Esperas a que se llene ${YELLOW}COMPLETAMENTE${RESET}"
log "    • Recibes el vaso ${YELLOW}COMPLETO${RESET} de una vez"
log "    • ${GREEN}UNA SOLA entrega${RESET}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

log "${CYAN}Ejecutando...${RESET}"
log ""

response=$(curl -s -w "\n%{http_code}" "${BASE_URL}${API_PATH}" 2>/dev/null)
body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
log ""
show_json "$body"
log ""

if [ "$status" == "200" ]; then
    log "${GREEN}✓ PASS${RESET} - Lista completa recibida"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    log "${RED}✗ FAIL${RESET} - HTTP $status"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

log ""
log "${CYAN}📊 Observación:${RESET}"
log "  • ${YELLOW}TODA la lista llegó de una vez${RESET}"
log "  • Cliente esperó hasta tener ${YELLOW}TODO${RESET}"
pause

##############################################################################
# PASO 4: MULTI - Streaming PROGRESIVO
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}🌊 PASO 4: MULTI - Streaming PROGRESIVO (UNO POR UNO)${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🔧 Tipo:${RESET} ${BLUE}Multi<Producto>${RESET}"
log "${YELLOW}📍 Endpoint:${RESET} GET ${API_PATH}/stream"
log ""
log "${CYAN}╔═══════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║  ¿QUÉ ES MULTI?                                           ║${RESET}"
log "${CYAN}╚═══════════════════════════════════════════════════════════╝${RESET}"
log ""
log "  ${YELLOW}Analogía:${RESET} Una manguera de agua"
log "    • El agua ${BLUE}fluye continuamente${RESET}"
log "    • ${BLUE}MÚLTIPLES entregas${RESET} en el tiempo"
log ""
log "${CYAN}Delay: ${YELLOW}1000ms${RESET} (1 segundo) entre cada producto${RESET}"
log ""
log "${YELLOW}👀 OBSERVA: Cada producto aparecerá UNO POR UNO${RESET}"
log "${YELLOW}   (verás el delay de 1 segundo entre cada llegada)${RESET}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

log "${CYAN}Iniciando streaming...${RESET}"
log ""
log "${MAGENTA}════════════════════════════════════════════════════════${RESET}"
log ""

# Streaming sin límite - mostrará TODOS los productos de la BD
COUNTER=1
if command -v stdbuf &> /dev/null; then
    stdbuf -o0 curl -N -H "Accept: text/event-stream" "${BASE_URL}${API_PATH}/stream" 2>/dev/null | while IFS= read -r line; do
        if [[ $line == data:* ]]; then
            TIMESTAMP=$(date +%H:%M:%S 2>/dev/null)
            
            log "${GREEN}▶ [$TIMESTAMP] Producto #$COUNTER recibido${RESET}"
            log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
            
            # Extraer solo el JSON (quitar "data:")
            json_clean="${line#data:}"
            # Quitar espacios al inicio
            json_clean=$(echo "$json_clean" | sed 's/^ *//')
            
            if command -v jq &> /dev/null; then
                formatted=$(echo "$json_clean" | jq '.' 2>/dev/null)
                if [ $? -eq 0 ]; then
                    echo "$formatted"
                    echo "$formatted" >> "$OUTPUT_FILE"
                else
                    echo "$json_clean"
                    echo "$json_clean" >> "$OUTPUT_FILE"
                fi
            else
                echo "$json_clean" | sed 's/,/,\n  /g'
                echo "$json_clean" | sed 's/,/,\n  /g' >> "$OUTPUT_FILE"
            fi
            
            log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
            log ""
            
            COUNTER=$((COUNTER + 1))
        fi
    done
else
    curl -N -H "Accept: text/event-stream" "${BASE_URL}${API_PATH}/stream" 2>/dev/null | while IFS= read -r line; do
        if [[ $line == data:* ]]; then
            TIMESTAMP=$(date +%H:%M:%S 2>/dev/null)
            
            log "${GREEN}▶ [$TIMESTAMP] Producto #$COUNTER recibido${RESET}"
            log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
            
            # Extraer solo el JSON (quitar "data:")
            json_clean="${line#data:}"
            # Quitar espacios al inicio
            json_clean=$(echo "$json_clean" | sed 's/^ *//')
            
            if command -v jq &> /dev/null; then
                formatted=$(echo "$json_clean" | jq '.' 2>/dev/null)
                if [ $? -eq 0 ]; then
                    echo "$formatted"
                    echo "$formatted" >> "$OUTPUT_FILE"
                else
                    echo "$json_clean"
                    echo "$json_clean" >> "$OUTPUT_FILE"
                fi
            else
                echo "$json_clean" | sed 's/,/,\n  /g'
                echo "$json_clean" | sed 's/,/,\n  /g' >> "$OUTPUT_FILE"
            fi
            
            log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
            log ""
            
            COUNTER=$((COUNTER + 1))
        fi
    done
fi

log ""
log "${MAGENTA}════════════════════════════════════════════════════════${RESET}"
log ""
log "${GREEN}✓ PASS${RESET} - Streaming completado"
PASSED_TESTS=$((PASSED_TESTS + 1))
log ""
log "${CYAN}📊 ¿Lo viste? Cada producto llegó con 1 segundo de espera${RESET}"
pause

##############################################################################
# PASO 5: Comparación
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}⚖️  PASO 5: Comparación${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${CYAN}╔════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║  Aspecto              │ Uni          │ Multi              ║${RESET}"
log "${CYAN}╠════════════════════════════════════════════════════════════╣${RESET}"
log "${CYAN}║  Valores emitidos     │ ${GREEN}1 (lista)${RESET}    │ ${BLUE}N (stream)${RESET}         ║${RESET}"
log "${CYAN}║  Cuándo llega         │ ${YELLOW}Todo junto${RESET}   │ ${BLUE}Progresivo${RESET}         ║${RESET}"
log "${CYAN}║  Memoria              │ ${RED}Alta${RESET}         │ ${GREEN}Baja${RESET}               ║${RESET}"
log "${CYAN}║  Primera respuesta    │ ${RED}Al final${RESET}     │ ${GREEN}Inmediata${RESET}          ║${RESET}"
log "${CYAN}║  Uso típico           │ ${GREEN}CRUD${RESET}         │ ${BLUE}Streaming/RT${RESET}       ║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "${GREEN}✅ USA UNI:${RESET} APIs REST, listas pequeñas, necesitas todo junto"
log "${BLUE}✅ USA MULTI:${RESET} Tiempo real, grandes volúmenes, datos progresivos"
log ""
log "${YELLOW}Ejemplos bancarios:${RESET}"
log "  ${GREEN}Uni:${RESET} Consultar saldo, crear transferencia, buscar cliente"
log "  ${BLUE}Multi:${RESET} Cotizaciones USD en vivo, stream de transacciones, monitor fraude"
pause

##############################################################################
# RESUMEN
##############################################################################
clear
log ""
log "${CYAN}╔════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║                    📊 RESUMEN                                  ║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "  ${CYAN}Total:${RESET}      $TOTAL_TESTS"
log "  ${GREEN}✓ Exitosos:${RESET}  $PASSED_TESTS"
log "  ${RED}✗ Fallidos:${RESET}  $FAILED_TESTS"
log ""
log "${CYAN}╔════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║              🎓 CONCEPTOS APRENDIDOS                           ║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "${GREEN}1️⃣  UNI = UN SOLO valor asíncrono${RESET}"
log "   • Similar a Promise/CompletableFuture"
log "   • Perfecto para APIs REST tradicionales"
log ""
log "${BLUE}2️⃣  MULTI = MÚLTIPLES valores en el tiempo${RESET}"
log "   • Similar a Observable/Stream"
log "   • Perfecto para streaming y tiempo real"
log ""
log "${YELLOW}3️⃣  Server-Sent Events (SSE)${RESET}"
log "   • Protocolo para streaming servidor→cliente"
log "   • Más simple que WebSockets"
log ""
log "${MAGENTA}4️⃣  Programación reactiva NO BLOQUEANTE${RESET}"
log "   • Alta concurrencia con pocos recursos"
log "   • Backpressure automático"
log ""
log "${GREEN}📄 Log guardado en: ${CYAN}$OUTPUT_FILE${RESET}"
log ""

echo ""
echo -e "${MAGENTA}╔════════════════════════════════════════════════════════════════╗${RESET}"
echo -e "${MAGENTA}║                    ✅ PRUEBAS FINALIZADAS                      ║${RESET}"
echo -e "${MAGENTA}╚════════════════════════════════════════════════════════════════╝${RESET}"
echo ""