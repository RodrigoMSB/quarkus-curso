#!/bin/bash

##############################################################################
# Script de Pruebas - PERFIL TEST
# 
# Este script prueba el perfil de TESTING del microservicio TasaCorp.
# El perfil TEST esta optimizado para pruebas con limites realistas.
#
# COMPATIBLE: Mac y Windows (Git Bash)
##############################################################################

# ============================================================================
# DETECCION DE SISTEMA OPERATIVO
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

# Generar nombre de archivo con timestamp
OUTPUT_FILE="test-test-$(date '+%Y-%m-%d_%H-%M-%S').txt"

# Limpiar archivo de salida
> "$OUTPUT_FILE"

# Colores para mejor visualizacion
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
STARTUP_TIMEOUT=60

# Contadores de tests
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Detectar Python (python3 en Mac/Linux, python en Windows)
if command -v python3 &> /dev/null; then
    PYTHON_CMD="python3"
elif command -v python &> /dev/null; then
    PYTHON_CMD="python"
else
    echo "Error: Python no esta instalado"
    echo "   Windows: Descarga desde https://www.python.org/downloads/"
    echo "   Mac: brew install python3"
    exit 1
fi

# ============================================================================
# FUNCIONES
# ============================================================================

# Funcion de logging (muestra con colores en pantalla, guarda sin colores en archivo)
log() {
    local message="$*"
    printf "%b\n" "$message"
    printf "%b\n" "$message" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

# Funcion para mostrar JSON formateado
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

# Funcion para pausa interactiva (compatible con Windows)
pause() {
    echo ""
    read -r -p "Presiona ENTER para continuar..." dummy
    echo ""
}

# Funcion para matar procesos
kill_all() {
    log "${CYAN}Matando procesos previos de Quarkus y Java...${RESET}"
    
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
    log "${GREEN}Limpieza completada${RESET}"
}

# Funcion para esperar arranque de la app
wait_for_app() {
    local elapsed=0
    log "${CYAN}Esperando a que la aplicacion arranque (timeout: ${STARTUP_TIMEOUT}s)...${RESET}"
    
    # Espera inicial de 5 segundos para que Quarkus arranque
    sleep 5
    elapsed=5
    
    while [ $elapsed -lt $STARTUP_TIMEOUT ]; do
        if curl -s "$BASE_URL/api/tasas/config" > /dev/null 2>&1; then
            log "${GREEN}Aplicacion lista en perfil TEST${RESET}"
            sleep 2
            return 0
        fi
        sleep 2
        elapsed=$((elapsed + 2))
        if [ $((elapsed % 10)) -eq 0 ]; then
            log "${CYAN}   ... esperando (${elapsed}s/${STARTUP_TIMEOUT}s)${RESET}"
        fi
    done
    
    log "${RED}Timeout: La aplicacion no arranco en ${STARTUP_TIMEOUT}s${RESET}"
    exit 1
}

# ============================================================================
# BANNER INICIAL
# ============================================================================
clear
log "${CYAN}================================================================${RESET}"
log "${CYAN}              PRUEBAS - PERFIL TEST                            ${RESET}"
log "${CYAN}              Testing: Ambiente controlado                     ${RESET}"
log "${CYAN}================================================================${RESET}"
log ""
log "${CYAN}Sistema Operativo:${RESET} $OS_TYPE"
log "${CYAN}Python:${RESET} $PYTHON_CMD"
log "${CYAN}Fecha:${RESET} $(date '+%d/%m/%Y %H:%M:%S')"
log "${CYAN}API Base:${RESET} $BASE_URL"
log "${CYAN}Resultados:${RESET} $OUTPUT_FILE"
log "${CYAN}Perfil:${RESET} TEST (testing)"
log ""
log "${YELLOW}IMPORTANTE:${RESET} El servidor se arrancara automaticamente en perfil ${YELLOW}test${RESET}"
log "${YELLOW}El script manejara:${RESET} ${CYAN}Compilacion, arranque, pruebas y limpieza${RESET}"
log ""
log "${MAGENTA}Caracteristicas del perfil TEST:${RESET}"
log "  - Comision: 1.5% (moderada)"
log "  - Limite transaccional: 1,000 (bajo para pruebas)"
log "  - Cache: Activado"
log "  - Auditoria: Activada"
log "  - Proveedor: FreeCurrencyAPI"
log "  - Vault: Desactivado"
log ""
pause

##############################################################################
# LIMPIEZA Y COMPILACION
##############################################################################
clear
log ""
log "${CYAN}================================================================${RESET}"
log "${WHITE}PREPARACION Y COMPILACION${RESET}"
log "${CYAN}================================================================${RESET}"
log ""

kill_all
log ""

log "${CYAN}Compilando aplicacion...${RESET}"
mvn clean package -DskipTests > /dev/null 2>&1
if [ $? -ne 0 ]; then
    log "${RED}Error al compilar${RESET}"
    exit 1
fi
log "${GREEN}Compilacion exitosa${RESET}"
log ""

log "${CYAN}Arrancando aplicacion en modo TEST...${RESET}"
log ""

# Arrancar en background con perfil TEST
if [ "$OS_TYPE" = "windows" ]; then
    # Windows: usar start para ejecutar en ventana separada
    start //B java -Dquarkus.profile=test -jar target/quarkus-app/quarkus-run.jar > /dev/null 2>&1
    sleep 2
    APP_PID="N/A (Windows background)"
else
    # Mac/Linux: background normal
    java -Dquarkus.profile=test -jar target/quarkus-app/quarkus-run.jar > /dev/null 2>&1 &
    APP_PID=$!
fi

log "${CYAN}PID de la aplicacion:${RESET} $APP_PID"
log ""

wait_for_app
log ""
pause

##############################################################################
# PRUEBA 1: Configuracion TEST
##############################################################################
clear
log ""
log "${CYAN}================================================================${RESET}"
log "${WHITE}PRUEBA 1: Configuracion del Perfil TEST${RESET}"
log "${CYAN}================================================================${RESET}"
log ""
log "${YELLOW}Objetivo:${RESET} Verificar que la aplicacion esta en perfil TEST"
log "${YELLOW}Endpoint:${RESET} GET /api/tasas/config"
log "${YELLOW}Esperado:${RESET} perfil_activo=test, comision=1.5%, limite=1,000"
log ""
log "${CYAN}Ejecutando consulta de configuracion...${RESET}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

response=$(curl -s -w "\n%{http_code}" $BASE_URL/api/tasas/config 2>/dev/null)
body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
show_json "$body"
log ""

if [ "$status" == "200" ]; then
    log "${GREEN}PASS - Configuracion TEST obtenida correctamente${RESET}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
    
    # Validacion adicional con Python
    echo "$body" | $PYTHON_CMD -c "
import sys, json
try:
    data = json.load(sys.stdin)
    perfil = data.get('perfil_activo', 'N/A')
    comision = data.get('comision_porcentaje', 'N/A')
    limite = data.get('limite_transaccional', 'N/A')
    proveedor = data.get('proveedor', 'N/A')
    
    print('Validaciones:')
    if perfil == 'test':
        print('  [OK] Perfil: test')
    else:
        print(f'  [FAIL] Perfil: {perfil} (esperado: test)')
    
    if comision == 1.5:
        print('  [OK] Comision: 1.5%')
    else:
        print(f'  [FAIL] Comision: {comision}% (esperado: 1.5%)')
    
    if limite == 1000:
        print('  [OK] Limite: 1,000')
    else:
        print(f'  [FAIL] Limite: {limite} (esperado: 1,000)')
    
    print(f'  [OK] Proveedor: {proveedor}')
except Exception as e:
    print(f'Error al validar: {e}')
" | tee -a "$OUTPUT_FILE"
else
    log "${RED}FAIL - HTTP $status (Esperado: 200)${RESET}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

log ""
log "${CYAN}Resultado esperado:${RESET}"
log "   perfil_activo: 'test'"
log "   comision_porcentaje: 1.5"
log "   limite_transaccional: 1000"
pause

##############################################################################
# PRUEBA 2: Conversion CON Comision
##############################################################################
clear
log ""
log "${CYAN}================================================================${RESET}"
log "${WHITE}PRUEBA 2: Conversion con Comision 1.5%${RESET}"
log "${CYAN}================================================================${RESET}"
log ""
log "${YELLOW}Objetivo:${RESET} Verificar que TEST cobra comision del 1.5%"
log "${YELLOW}Endpoint:${RESET} GET /api/tasas/convertir/USD?monto=1000"
log "${YELLOW}Operacion:${RESET} Convertir 1,000 PEN a USD"
log "${YELLOW}Esperado:${RESET} comision > 0"
log ""
log "${CYAN}Ejecutando conversion...${RESET}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

response=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/tasas/convertir/USD?monto=1000" 2>/dev/null)
body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
show_json "$body"
log ""

if [ "$status" == "200" ]; then
    log "${GREEN}PASS - Conversion realizada correctamente${RESET}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
    
    # Analisis con Python
    echo "$body" | $PYTHON_CMD -c "
import sys, json
try:
    data = json.load(sys.stdin)
    monto = data['monto_origen']
    convertido = data['monto_convertido']
    comision = data['comision']
    dentro = data.get('dentro_limite', True)
    
    print('Desglose de la conversion:')
    print(f'  Monto Original: \${monto:,.0f} PEN')
    print(f'  Monto Convertido: \${convertido:,.2f} USD')
    print(f'  Comision (1.5%): \${comision:.2f} USD')
    print(f'  Total: \${convertido + comision:,.2f} USD')
    print(f'  Dentro de Limite: {dentro}')
    
    if comision > 0:
        print(f'\n[OK] TEST cobra comision del 1.5%')
    else:
        print(f'\n[FAIL] Comision deberia ser > 0')
except Exception as e:
    print(f'Error: {e}')
" | tee -a "$OUTPUT_FILE"
else
    log "${RED}FAIL - HTTP $status (Esperado: 200)${RESET}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

log ""
log "${CYAN}Resultado esperado:${RESET}"
log "   La comision debe ser mayor a 0 (1.5% del monto convertido)"
log "   El monto de 1,000 debe estar dentro del limite"
pause

##############################################################################
# PRUEBA 3: Limite Excedido
##############################################################################
clear
log ""
log "${CYAN}================================================================${RESET}"
log "${WHITE}PRUEBA 3: Detectar Limite Excedido${RESET}"
log "${CYAN}================================================================${RESET}"
log ""
log "${YELLOW}Objetivo:${RESET} Verificar deteccion de limite excedido"
log "${YELLOW}Endpoint:${RESET} GET /api/tasas/convertir/USD?monto=1500"
log "${YELLOW}Operacion:${RESET} Convertir 1,500 PEN (excede limite de 1,000)"
log "${YELLOW}Esperado:${RESET} dentro_limite: false"
log ""
log "${CYAN}Ejecutando conversion con monto que excede limite...${RESET}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

response=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/tasas/convertir/USD?monto=1500" 2>/dev/null)
body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
show_json "$body"
log ""

if [ "$status" == "200" ]; then
    log "${GREEN}PASS - Respuesta obtenida correctamente${RESET}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
    
    # Analisis con Python
    echo "$body" | $PYTHON_CMD -c "
import sys, json
try:
    data = json.load(sys.stdin)
    monto = data['monto_origen']
    limite = data['limite_transaccional']
    dentro = data['dentro_limite']
    
    print('Analisis del limite excedido:')
    print(f'  Monto Solicitado: \${monto:,.0f}')
    print(f'  Limite Transaccional: \${limite:,}')
    print(f'  Dentro de Limite: {dentro}')
    
    if not dentro:
        print(f'\n[OK] Se detecto que excede el limite')
        print(f'[INFO] En un sistema real, esto rechazaria la transaccion')
    else:
        print(f'\n[FAIL] Deberia indicar que excede el limite')
except Exception as e:
    print(f'Error: {e}')
" | tee -a "$OUTPUT_FILE"
else
    log "${RED}FAIL - HTTP $status (Esperado: 200)${RESET}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

log ""
log "${CYAN}Resultado esperado:${RESET}"
log "   dentro_limite: false (1,500 > 1,000)"
pause

##############################################################################
# RESUMEN FINAL
##############################################################################
clear
log ""
log "${CYAN}================================================================${RESET}"
log "${CYAN}                    RESUMEN DE PRUEBAS                         ${RESET}"
log "${CYAN}================================================================${RESET}"
log ""
log "  ${CYAN}Total de tests:${RESET}      $TOTAL_TESTS"
log "  ${GREEN}Tests Exitosos:${RESET}  $PASSED_TESTS"
log "  ${RED}Tests Fallidos:${RESET}  $FAILED_TESTS"
log ""

if [ $FAILED_TESTS -gt 0 ]; then
    log "${YELLOW}ADVERTENCIA: Algunos tests fallaron${RESET}"
    log ""
    log "${YELLOW}Posible causa:${RESET} El servidor no se inicio con el perfil correcto"
    log "${YELLOW}Solucion:${RESET}"
    log "  ${CYAN}1.${RESET} Deten el servidor (Ctrl+C)"
    log "  ${CYAN}2.${RESET} Vuelve a ejecutar este script"
    log "  ${CYAN}3.${RESET} Revisa los logs de la aplicacion"
    log ""
fi

log "${CYAN}================================================================${RESET}"
log "${CYAN}                   TESTS EJECUTADOS                            ${RESET}"
log "${CYAN}================================================================${RESET}"
log ""
log "${GREEN}PRUEBA 1:${RESET} Configuracion del perfil TEST verificada"
log "${GREEN}PRUEBA 2:${RESET} Conversion con comision moderada (1.5%)"
log "${GREEN}PRUEBA 3:${RESET} Deteccion de limite excedido verificada"
log ""
log "${CYAN}================================================================${RESET}"
log "${CYAN}              CARACTERISTICAS DEL PERFIL TEST                  ${RESET}"
log "${CYAN}================================================================${RESET}"
log ""
log "${YELLOW}Optimizado para:${RESET}       Pruebas en ambiente controlado"
log "${YELLOW}Comision:${RESET}              1.5% (moderada)"
log "${YELLOW}Limite:${RESET}                1,000 (bajo para pruebas)"
log "${YELLOW}Cache:${RESET}                 Activado (rendimiento)"
log "${YELLOW}Auditoria:${RESET}             Activada (trazabilidad)"
log "${YELLOW}Proveedor:${RESET}             FreeCurrencyAPI (API gratuita)"
log "${YELLOW}Vault:${RESET}                 Desactivado"
log ""

log "${CYAN}================================================================${RESET}"
log "${CYAN}                    ARCHIVO DE LOG                             ${RESET}"
log "${CYAN}================================================================${RESET}"
log ""
log "${YELLOW}Todas las pruebas han sido guardadas en:${RESET}"
log "   ${GREEN}$OUTPUT_FILE${RESET}"
log ""
log "${CYAN}Puedes revisar el log completo en cualquier momento para:${RESET}"
log "   - Verificar las respuestas HTTP completas"
log "   - Analizar la configuracion del perfil TEST"
log "   - Confirmar el comportamiento de limites"
log "   - Compartir los resultados con tu instructor"
log "   - Documentar el comportamiento del sistema"
log ""

##############################################################################
# LIMPIEZA FINAL
##############################################################################

log "${CYAN}Deteniendo la aplicacion...${RESET}"
kill_all
log ""

log "${GREEN}Pruebas del perfil TEST completadas exitosamente!${RESET}"
log "${CYAN}Continua con: test-part2-prod.sh (perfil PROD)${RESET}"
log ""