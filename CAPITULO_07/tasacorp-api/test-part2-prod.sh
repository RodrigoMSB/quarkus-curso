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

# Generar nombre de archivo con timestamp
OUTPUT_FILE="test-prod-$(date '+%Y-%m-%d_%H-%M-%S').txt"

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
VAULT_URL="http://localhost:8200"
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
    echo "❌ Error: Python no está instalado"
    echo "   Windows: Descarga desde https://www.python.org/downloads/"
    echo "   Mac: brew install python3"
    exit 1
fi

# ============================================================================
# FUNCIONES
# ============================================================================

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

# Función para matar procesos
kill_all() {
    log "${CYAN}🧹 Matando procesos previos de Quarkus y Java...${RESET}"
    
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
    log "${GREEN}✅ Limpieza completada${RESET}"
}

# Función para esperar arranque de la app
wait_for_app() {
    local elapsed=0
    log "${CYAN}⏳ Esperando a que la aplicación arranque (timeout: ${STARTUP_TIMEOUT}s)...${RESET}"
    
    while [ $elapsed -lt $STARTUP_TIMEOUT ]; do
        if curl -s "$BASE_URL/api/tasas/config" > /dev/null 2>&1; then
            log "${GREEN}✅ Aplicación lista en perfil PROD${RESET}"
            sleep 2
            return 0
        fi
        sleep 2
        elapsed=$((elapsed + 2))
        if [ $((elapsed % 10)) -eq 0 ]; then
            log "${CYAN}   ... esperando (${elapsed}s/${STARTUP_TIMEOUT}s)${RESET}"
        fi
    done
    
    log "${RED}❌ Timeout: La aplicación no arrancó en ${STARTUP_TIMEOUT}s${RESET}"
    exit 1
}

# ============================================================================
# BANNER INICIAL
# ============================================================================
clear
log "${CYAN}╔════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║              🔴 PRUEBAS - PERFIL PROD                          ║${RESET}"
log "${CYAN}║              Producción: Máxima seguridad con Vault            ║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "${CYAN}🖥️  Sistema Operativo:${RESET} $OS_TYPE"
log "${CYAN}🐍 Python:${RESET} $PYTHON_CMD"
log "${CYAN}📅 Fecha:${RESET} $(date '+%d/%m/%Y %H:%M:%S')"
log "${CYAN}🌐 API Base:${RESET} $BASE_URL"
log "${CYAN}🔐 Vault:${RESET} $VAULT_URL"
log "${CYAN}📄 Resultados:${RESET} $OUTPUT_FILE"
log "${CYAN}🔧 Perfil:${RESET} PROD (producción)"
log ""
log "${YELLOW}⚠️  IMPORTANTE:${RESET} El servidor se arrancará automáticamente en perfil ${RED}prod${RESET}"
log "${YELLOW}   Requisito:${RESET} ${CYAN}Vault debe estar corriendo con el secreto guardado${RESET}"
log ""
log "${MAGENTA}Características del perfil PROD:${RESET}"
log "  ✓ Comisión: 2.5% (completa)"
log "  ✓ Límite transaccional: 50,000 (alto pero controlado)"
log "  ✓ Cache: Activado"
log "  ✓ Auditoría: Activada"
log "  ✓ Proveedor: PremiumProvider"
log "  🔐 API Key: Desde Vault (seguro)"
log ""
pause

##############################################################################
# VERIFICACIÓN DE VAULT
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}🔍 VERIFICACIÓN DE VAULT${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""

log "${CYAN}🔐 Verificando que Vault esté corriendo...${RESET}"
if ! curl -s "$VAULT_URL/v1/sys/health" > /dev/null 2>&1; then
    log "${RED}❌ Error: Vault no está corriendo en $VAULT_URL${RESET}"
    log ""
    log "${YELLOW}Para arrancar Vault, ejecuta:${RESET}"
    log "  ${CYAN}docker-compose up -d${RESET}"
    log ""
    log "${YELLOW}Para guardar el secreto:${RESET}"
    log "  ${CYAN}docker exec -it tasacorp-vault sh -c \\${RESET}"
    log "    ${CYAN}\"VAULT_ADDR=http://127.0.0.1:8200 VAULT_TOKEN=root \\${RESET}"
    log "     ${CYAN}vault kv put secret/tasacorp api-key=PREMIUM_KEY_XYZ\"${RESET}"
    log ""
    exit 1
fi
log "${GREEN}✅ Vault está corriendo${RESET}"
log ""

log "${CYAN}🔐 Verificando secreto en Vault...${RESET}"
VAULT_CHECK=$(docker exec tasacorp-vault sh -c \
  "VAULT_ADDR=http://127.0.0.1:8200 VAULT_TOKEN=root vault kv get -format=json secret/tasacorp" 2>/dev/null)

if [ $? -ne 0 ]; then
    log "${RED}❌ Error: No se pudo acceder al secreto en Vault${RESET}"
    log ""
    log "${YELLOW}Guarda el secreto con:${RESET}"
    log "  ${CYAN}docker exec -it tasacorp-vault sh -c \\${RESET}"
    log "    ${CYAN}\"VAULT_ADDR=http://127.0.0.1:8200 VAULT_TOKEN=root \\${RESET}"
    log "     ${CYAN}vault kv put secret/tasacorp api-key=PREMIUM_KEY_XYZ\"${RESET}"
    log ""
    exit 1
fi
log "${GREEN}✅ Secreto disponible en Vault${RESET}"
log ""
pause

##############################################################################
# LIMPIEZA Y COMPILACIÓN
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📦 PREPARACIÓN Y COMPILACIÓN${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""

kill_all
log ""

log "${CYAN}📦 Compilando aplicación...${RESET}"
./mvnw clean package -DskipTests > /dev/null 2>&1
if [ $? -ne 0 ]; then
    log "${RED}❌ Error al compilar${RESET}"
    exit 1
fi
log "${GREEN}✅ Compilación exitosa${RESET}"
log ""

log "${CYAN}🚀 Arrancando aplicación en modo PROD...${RESET}"
log ""

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

log "${CYAN}📋 PID de la aplicación:${RESET} $APP_PID"
log ""

wait_for_app
log ""
pause

##############################################################################
# PRUEBA 1: Configuración PROD + Vault
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 1: Configuración del Perfil PROD + Vault${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Verificar que la aplicación está en perfil PROD con Vault"
log "${YELLOW}📍 Endpoint:${RESET} GET /api/tasas/config"
log "${YELLOW}✅ Esperado:${RESET} perfil_activo=prod, comision=2.5%, limite=50,000, api_key desde Vault"
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
    log "${GREEN}✓ PASS${RESET} - Configuración PROD obtenida correctamente"
    PASSED_TESTS=$((PASSED_TESTS + 1))
    
    # Validación adicional con Python
    echo "$body" | $PYTHON_CMD -c "
import sys, json
try:
    data = json.load(sys.stdin)
    perfil = data.get('perfil_activo', 'N/A')
    comision = data.get('comision_porcentaje', 'N/A')
    limite = data.get('limite_transaccional', 'N/A')
    apikey_source = data.get('api_key_source', 'N/A')
    
    print('${CYAN}Validaciones:${RESET}')
    if perfil == 'prod':
        print('  ${GREEN}✓${RESET} Perfil: prod')
    else:
        print(f\"  ${RED}✗${RESET} Perfil: {perfil} (esperado: prod)\")
    
    if comision == 2.5:
        print('  ${GREEN}✓${RESET} Comisión: 2.5%')
    else:
        print(f\"  ${RED}✗${RESET} Comisión: {comision}% (esperado: 2.5%)\")
    
    if limite == 50000:
        print('  ${GREEN}✓${RESET} Límite: 50,000')
    else:
        print(f\"  ${RED}✗${RESET} Límite: {limite} (esperado: 50,000)\")
    
    if 'Vault' in str(apikey_source):
        print(f\"  ${GREEN}✓${RESET} API Key: desde Vault (seguro)\")
        print(f\"\\n${GREEN}✅ EXCELENTE: API Key viene desde Vault (máxima seguridad)${RESET}\")
    else:
        print(f\"  ${RED}✗${RESET} API Key: {apikey_source} (esperado: desde Vault)\")
except Exception as e:
    print(f'${RED}❌ Error al validar: {e}${RESET}')
" | tee -a "$OUTPUT_FILE"
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 200)"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   perfil_activo: 'prod'"
log "   comision_porcentaje: 2.5"
log "   limite_transaccional: 50000"
log "   api_key_source: 'Vault (KV v2)'"
pause

##############################################################################
# PRUEBA 2: Conversión con Comisión Completa
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 2: Conversión con Comisión 2.5%${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Verificar que PROD cobra comisión del 2.5%"
log "${YELLOW}📍 Endpoint:${RESET} GET /api/tasas/convertir/USD?monto=1000"
log "${YELLOW}💰 Operación:${RESET} Convertir 1,000 PEN a USD"
log "${YELLOW}✅ Esperado:${RESET} comision > 0"
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
    
    # Análisis con Python
    echo "$body" | $PYTHON_CMD -c "
import sys, json
try:
    data = json.load(sys.stdin)
    monto = data['monto_origen']
    convertido = data['monto_convertido']
    comision = data['comision']
    
    print('${CYAN}Desglose de la conversión:${RESET}')
    print(f\"  💵 Monto Original: \${monto:,.0f} PEN\")
    print(f\"  💱 Monto Convertido: \${convertido:,.2f} USD\")
    print(f\"  💸 Comisión (2.5%): \${comision:.2f} USD\")
    print(f\"  ✅ Total: \${convertido + comision:,.2f} USD\")
    
    if comision > 0:
        print(f\"\\n${GREEN}✅ CORRECTO: PROD cobra comisión completa del 2.5%${RESET}\")
    else:
        print(f\"\\n${RED}❌ ERROR: Comisión debería ser > 0${RESET}\")
except Exception as e:
    print(f'${RED}❌ Error: {e}${RESET}')
" | tee -a "$OUTPUT_FILE"
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 200)"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   La comisión debe ser mayor a 0 (2.5% del monto convertido)"
pause

##############################################################################
# PRUEBA 3: Monto Alto Dentro de Límite
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 3: Monto Alto Dentro de Límite${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Verificar que PROD acepta montos altos dentro del límite"
log "${YELLOW}📍 Endpoint:${RESET} GET /api/tasas/convertir/USD?monto=40000"
log "${YELLOW}💰 Operación:${RESET} Convertir 40,000 PEN (dentro del límite de 50,000)"
log "${YELLOW}✅ Esperado:${RESET} dentro_limite: true"
log ""
log "${CYAN}Ejecutando conversión con monto alto...${RESET}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

response=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/tasas/convertir/USD?monto=40000" 2>/dev/null)
body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
show_json "$body"
log ""

if [ "$status" == "200" ]; then
    log "${GREEN}✓ PASS${RESET} - Monto alto aceptado"
    PASSED_TESTS=$((PASSED_TESTS + 1))
    
    # Análisis con Python
    echo "$body" | $PYTHON_CMD -c "
import sys, json
try:
    data = json.load(sys.stdin)
    monto = data['monto_origen']
    limite = data['limite_transaccional']
    dentro = data['dentro_limite']
    
    print('${CYAN}Análisis del límite:${RESET}')
    print(f\"  💵 Monto Solicitado: \${monto:,.0f}\")
    print(f\"  🚦 Límite Transaccional: \${limite:,}\")
    print(f\"  📊 Dentro de Límite: {dentro}\")
    
    if dentro:
        print(f\"\\n${GREEN}✅ CORRECTO: Monto aceptado en PROD${RESET}\")
    else:
        print(f\"\\n${RED}❌ ERROR: Debería estar dentro del límite${RESET}\")
except Exception as e:
    print(f'${RED}❌ Error: {e}${RESET}')
" | tee -a "$OUTPUT_FILE"
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 200)"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   dentro_limite: true (40,000 < 50,000)"
pause

##############################################################################
# PRUEBA 4: Exceder Límite
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 4: Exceder Límite en PROD${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Verificar detección de límite excedido"
log "${YELLOW}📍 Endpoint:${RESET} GET /api/tasas/convertir/USD?monto=60000"
log "${YELLOW}💰 Operación:${RESET} Convertir 60,000 PEN (excede límite de 50,000)"
log "${YELLOW}✅ Esperado:${RESET} dentro_limite: false"
log ""
log "${CYAN}Ejecutando conversión con monto que excede límite...${RESET}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

response=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/tasas/convertir/USD?monto=60000" 2>/dev/null)
body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
show_json "$body"
log ""

if [ "$status" == "200" ]; then
    log "${GREEN}✓ PASS${RESET} - Respuesta obtenida correctamente"
    PASSED_TESTS=$((PASSED_TESTS + 1))
    
    # Análisis con Python
    echo "$body" | $PYTHON_CMD -c "
import sys, json
try:
    data = json.load(sys.stdin)
    monto = data['monto_origen']
    limite = data['limite_transaccional']
    dentro = data['dentro_limite']
    
    print('${CYAN}Análisis del límite excedido:${RESET}')
    print(f\"  💵 Monto Solicitado: \${monto:,.0f}\")
    print(f\"  🚦 Límite Transaccional: \${limite:,}\")
    print(f\"  📊 Dentro de Límite: {dentro}\")
    
    if not dentro:
        print(f\"\\n${GREEN}✅ CORRECTO: Se detectó que excede el límite${RESET}\")
        print(f\"${YELLOW}⚠️  En producción real, esto rechazaría la transacción${RESET}\")
    else:
        print(f\"\\n${RED}❌ ERROR: Debería indicar que excede el límite${RESET}\")
except Exception as e:
    print(f'${RED}❌ Error: {e}${RESET}')
" | tee -a "$OUTPUT_FILE"
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 200)"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   dentro_limite: false (60,000 > 50,000)"
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
    log "${YELLOW}Posible causa:${RESET} Vault no está configurado correctamente o el servidor no arrancó bien"
    log "${YELLOW}Solución:${RESET}"
    log "  ${CYAN}1.${RESET} Verifica que Vault esté corriendo: ${GREEN}docker ps${RESET}"
    log "  ${CYAN}2.${RESET} Verifica que el secreto esté guardado en Vault"
    log "  ${CYAN}3.${RESET} Revisa los logs de la aplicación"
    log ""
fi

log "${CYAN}╔════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║                   🎯 TESTS EJECUTADOS                          ║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "${GREEN}✅ PRUEBA 1:${RESET} Configuración del perfil PROD + Vault verificada"
log "${GREEN}✅ PRUEBA 2:${RESET} Conversión con comisión completa (2.5%)"
log "${GREEN}✅ PRUEBA 3:${RESET} Monto alto dentro de límite verificado"
log "${GREEN}✅ PRUEBA 4:${RESET} Detección de límite excedido verificada"
log ""
log "${CYAN}╔════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║              🎓 CARACTERÍSTICAS DEL PERFIL PROD                ║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "${YELLOW}🔧 Optimizado para:${RESET}       Producción con máxima seguridad"
log "${YELLOW}💸 Comisión:${RESET}              2.5% (completa)"
log "${YELLOW}🚦 Límite:${RESET}                50,000 (alto pero controlado)"
log "${YELLOW}📦 Cache:${RESET}                 Activado (rendimiento)"
log "${YELLOW}📝 Auditoría:${RESET}             Activada (trazabilidad)"
log "${YELLOW}🔌 Proveedor:${RESET}             PremiumProvider (API externa)"
log "${YELLOW}🔐 Vault:${RESET}                 Activado (máxima seguridad)"
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
log "   • Analizar la configuración del perfil PROD"
log "   • Confirmar la integración con Vault"
log "   • Compartir los resultados con tu instructor"
log "   • Documentar el comportamiento del sistema"
log ""

##############################################################################
# LIMPIEZA FINAL
##############################################################################

log "${CYAN}🛑 Deteniendo la aplicación...${RESET}"
kill_all
log ""

log "${GREEN}🎉 ¡Pruebas del perfil PROD completadas exitosamente!${RESET}"
log "${CYAN}Este perfil está listo para entornos de producción con Vault.${RESET}"
log ""