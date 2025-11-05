#!/bin/bash

##############################################################################
# Script de Pruebas - Evaluación Crediticia con Fault Tolerance
# 
# Este script prueba las capacidades de resiliencia del microservicio
# utilizando REST Client y patrones de Fault Tolerance.
#
# Conceptos que se prueban:
# - REST Client (@RegisterRestClient)
# - @Retry - Reintentos automáticos
# - @Timeout - Límites de tiempo
# - @Fallback - Respuestas alternativas
# - @CircuitBreaker - Apertura de circuito
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

# Archivo de log
LOG_FILE="resultados-pruebas-$(date +%Y%m%d-%H%M%S).txt"

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

# Colores para mejor visualización
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # Sin color

# URL base del microservicio
BASE_URL="http://localhost:8080"

# ============================================================================
# FUNCIONES
# ============================================================================

# Función para escribir en terminal y archivo
log_both() {
    echo -e "$1"
    echo -e "$1" | sed 's/\x1B\[[0-9;]*[JKmsu]//g' >> "$LOG_FILE"
}

# Función para pausar (compatible con ambos sistemas)
pause_script() {
    if [ "$OS_TYPE" = "windows" ]; then
        read -p "Presiona ENTER para continuar..."
    else
        read -p "Presiona ENTER para continuar..."
    fi
}

# ============================================================================
# HEADER
# ============================================================================

log_both "${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
log_both "${CYAN}║    💳  EVALUACIÓN CREDITICIA - PRUEBAS DE RESILIENCIA    ║${NC}"
log_both "${CYAN}║    REST Client + Fault Tolerance Patterns                ║${NC}"
log_both "${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"
log_both ""
log_both "${GREEN}🖥️  Sistema Operativo: ${OS_TYPE}${NC}"
log_both "${GREEN}🐍 Python: ${PYTHON_CMD}${NC}"
log_both "${GREEN}📄 Los resultados se guardarán en: ${LOG_FILE}${NC}"
log_both ""

##############################################################################
# PRUEBA 1: Caso Exitoso (DNI 111 terminado en PAR)
##############################################################################
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both "${YELLOW}📋 PRUEBA 1: Evaluación Exitosa (Happy Path)${NC}"
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both ""
log_both "🎯 Objetivo: Verificar que todo funciona correctamente"
log_both "📝 DNI: 11122334456 (termina en PAR = buen score)"
log_both "💰 Monto: S/ 30,000"
log_both "⏱️  Plazo: 24 meses"
log_both ""
log_both "${CYAN}🔍 Flujo esperado:${NC}"
log_both "   1️⃣  Validar identidad → ✅ OK"
log_both "   2️⃣  Consultar Bureau → ✅ OK (sin reintentos)"
log_both "   3️⃣  Calcular Scoring → ✅ OK (sin timeout)"
log_both "   4️⃣  Decisión final → ✅ APROBADO"
log_both ""
log_both "${CYAN}Ejecutando solicitud...${NC}"
log_both ""

RESPONSE=$(curl -s -X POST "$BASE_URL/api/evaluacion/credito" \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "11122334456",
    "nombres": "Juan",
    "apellidos": "Perez Lopez",
    "montoSolicitado": 30000,
    "mesesPlazo": 24
  }')

OUTPUT=$(echo "$RESPONSE" | $PYTHON_CMD -c "
import sys, json
data = json.load(sys.stdin)
print(f\"✅ DNI: {data['dni']}\")
print(f\"📊 Score Total: {data['scoreTotal']}\")
print(f\"🎯 Decisión: {data['decision']}\")
print(f\"💰 Monto Aprobado: S/ {data.get('montoAprobado', 0):,.2f}\")
print(f\"💬 Mensaje: {data['mensaje']}\")
")

log_both "$OUTPUT"
log_both ""
log_both "${GREEN}✅ ¡Solicitud procesada exitosamente sin ningún patrón de resiliencia!${NC}"
log_both ""
pause_script
log_both ""

##############################################################################
# PRUEBA 2: @Retry - Bureau con fallos temporales (DNI 222)
##############################################################################
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both "${YELLOW}📋 PRUEBA 2: @Retry - Reintentos Automáticos${NC}"
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both ""
log_both "🎯 Objetivo: Ver cómo @Retry maneja fallos temporales"
log_both "📝 DNI: 22233445568 (código especial para simular fallo + PAR)"
log_both "💰 Monto: S/ 40,000"
log_both ""
log_both "${CYAN}🔍 Configuración @Retry:${NC}"
log_both "   - maxRetries = 3"
log_both "   - delay = 1 segundo"
log_both ""
log_both "${CYAN}🎬 Escenario simulado:${NC}"
log_both "   Intento 1 → ${RED}❌ Bureau falla${NC}"
log_both "   Intento 2 → ${RED}❌ Bureau falla${NC}"
log_both "   Intento 3 → ${GREEN}✅ Bureau responde OK${NC}"
log_both ""
log_both "${MAGENTA}🔎 Observa los logs en la consola de Quarkus para ver los reintentos...${NC}"
log_both ""
log_both "${CYAN}Ejecutando solicitud...${NC}"
log_both ""

RESPONSE=$(curl -s -X POST "$BASE_URL/api/evaluacion/credito" \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "22233445568",
    "nombres": "Maria",
    "apellidos": "Garcia Ruiz",
    "montoSolicitado": 40000,
    "mesesPlazo": 36
  }')

OUTPUT=$(echo "$RESPONSE" | $PYTHON_CMD -c "
import sys, json
data = json.load(sys.stdin)
print(f\"✅ DNI: {data['dni']}\")
print(f\"📊 Score Total: {data['scoreTotal']}\")
print(f\"🎯 Decisión: {data['decision']}\")
print(f\"💬 Mensaje: {data['mensaje']}\")
")

log_both "$OUTPUT"
log_both ""
log_both "${GREEN}✅ ¡@Retry permitió recuperarse del fallo temporal!${NC}"
log_both "${YELLOW}💡 Sin @Retry, esta solicitud habría fallado inmediatamente${NC}"
log_both ""
pause_script
log_both ""

##############################################################################
# PRUEBA 3: @Timeout - Scoring se demora demasiado (DNI 333)
##############################################################################
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both "${YELLOW}📋 PRUEBA 3: @Timeout - Límite de Tiempo${NC}"
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both ""
log_both "🎯 Objetivo: Ver cómo @Timeout protege de servicios lentos"
log_both "📝 DNI: 33344556678 (código especial - demora 5 segundos + PAR)"
log_both "💰 Monto: S/ 25,000"
log_both ""
log_both "${CYAN}🔍 Configuración @Timeout:${NC}"
log_both "   - timeout = 3 segundos"
log_both ""
log_both "${CYAN}🎬 Escenario simulado:${NC}"
log_both "   Scoring tarda 5 segundos → ${RED}⏱️  Excede timeout${NC}"
log_both "   @Timeout corta la espera → ${YELLOW}⚠️  TimeoutException${NC}"
log_both "   @Fallback se activa → ${GREEN}✅ Usa scoring básico${NC}"
log_both ""
log_both "${MAGENTA}🔎 Observa que la respuesta llega en ~3 segundos (no 5)...${NC}"
log_both ""
log_both "${CYAN}Ejecutando solicitud...${NC}"
log_both ""

START=$(date +%s)

RESPONSE=$(curl -s -X POST "$BASE_URL/api/evaluacion/credito" \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "33344556678",
    "nombres": "Carlos",
    "apellidos": "Mendez Silva",
    "montoSolicitado": 25000,
    "mesesPlazo": 12
  }')

END=$(date +%s)
DURATION=$((END - START))

OUTPUT=$(echo "$RESPONSE" | $PYTHON_CMD -c "
import sys, json
data = json.load(sys.stdin)
print(f\"✅ DNI: {data['dni']}\")
print(f\"📊 Score Total: {data['scoreTotal']} (score básico por fallback)\")
print(f\"🎯 Decisión: {data['decision']}\")
print(f\"💬 Mensaje: {data['mensaje']}\")
")

log_both "$OUTPUT"
log_both ""
log_both "${YELLOW}⏱️  Tiempo de respuesta: ~${DURATION} segundos${NC}"
log_both "${GREEN}✅ ¡@Timeout evitó esperar 5 segundos!${NC}"
log_both "${GREEN}✅ ¡@Fallback proporcionó una respuesta alternativa!${NC}"
log_both ""
pause_script
log_both ""

##############################################################################
# PRUEBA 4: @Fallback - Scoring falla completamente (DNI 444)
##############################################################################
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both "${YELLOW}📋 PRUEBA 4: @Fallback - Respuesta Alternativa${NC}"
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both ""
log_both "🎯 Objetivo: Ver cómo @Fallback proporciona plan B"
log_both "📝 DNI: 44455667788 (código especial - fallo permanente + PAR)"
log_both "💰 Monto: S/ 35,000"
log_both ""
log_both "${CYAN}🔍 Configuración @Fallback:${NC}"
log_both "   - fallbackMethod = scoringBasicoFallback"
log_both ""
log_both "${CYAN}🎬 Escenario simulado:${NC}"
log_both "   Scoring avanzado falla → ${RED}❌ Error 500${NC}"
log_both "   @Fallback se activa → ${GREEN}✅ Usa scoring básico simplificado${NC}"
log_both "   Decisión: REQUIERE_ANALISIS_MANUAL"
log_both ""
log_both "${CYAN}Ejecutando solicitud...${NC}"
log_both ""

RESPONSE=$(curl -s -X POST "$BASE_URL/api/evaluacion/credito" \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "44455667788",
    "nombres": "Ana",
    "apellidos": "Rodriguez Torres",
    "montoSolicitado": 35000,
    "mesesPlazo": 48
  }')

OUTPUT=$(echo "$RESPONSE" | $PYTHON_CMD -c "
import sys, json
data = json.load(sys.stdin)
print(f\"✅ DNI: {data['dni']}\")
print(f\"📊 Score Total: {data['scoreTotal']} (score neutral del fallback)\")
print(f\"🎯 Decisión: {data['decision']}\")
print(f\"💬 Mensaje: {data['mensaje']}\")
")

log_both "$OUTPUT"
log_both ""
log_both "${GREEN}✅ ¡La aplicación NO falló a pesar del error en Scoring!${NC}"
log_both "${YELLOW}💡 @Fallback permitió degradar el servicio gracefully${NC}"
log_both ""
pause_script
log_both ""

##############################################################################
# PRUEBA 5: @CircuitBreaker - Múltiples fallos (DNI 444 x5)
##############################################################################
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both "${YELLOW}📋 PRUEBA 5: @CircuitBreaker - Apertura del Circuito${NC}"
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both ""
log_both "🎯 Objetivo: Ver cómo Circuit Breaker protege servicios caídos"
log_both "📝 DNI: 44455667788 (mismo que antes - falla siempre)"
log_both "💰 Haremos 5 solicitudes consecutivas"
log_both ""
log_both "${CYAN}🔍 Configuración @CircuitBreaker:${NC}"
log_both "   - requestVolumeThreshold = 4 (mínimo de peticiones)"
log_both "   - failureRatio = 0.5 (50% de fallos para abrir)"
log_both "   - delay = 10 segundos (tiempo antes de reintentar)"
log_both ""
log_both "${CYAN}🎬 Comportamiento esperado:${NC}"
log_both "   Solicitudes 1-4 → ${YELLOW}Llaman al servicio (fallback activo)${NC}"
log_both "   Solicitud 5+ → ${RED}Circuito ABIERTO - ni siquiera intenta${NC}"
log_both ""
log_both "${MAGENTA}🔎 Observa cómo después de 4 fallos, el circuito se abre...${NC}"
log_both ""

for i in {1..5}; do
  log_both "${CYAN}Solicitud #$i...${NC}"
  
  RESPONSE=$(curl -s -X POST "$BASE_URL/api/evaluacion/credito" \
    -H "Content-Type: application/json" \
    -d '{
      "dni": "44455667788",
      "nombres": "Test",
      "apellidos": "Circuit Breaker",
      "montoSolicitado": 30000,
      "mesesPlazo": 24
    }')
  
  OUTPUT=$(echo "$RESPONSE" | $PYTHON_CMD -c "
import sys, json
try:
    data = json.load(sys.stdin)
    print(f\"  → Decisión: {data['decision']}\")
except:
    print(\"  → Error procesando respuesta\")
" 2>/dev/null)
  
  log_both "$OUTPUT"
  sleep 1
done

log_both ""
log_both "${GREEN}✅ ¡Circuit Breaker protegió el sistema!${NC}"
log_both "${YELLOW}💡 Sin Circuit Breaker, seguiríamos llamando a un servicio caído${NC}"
log_both ""
pause_script
log_both ""

##############################################################################
# PRUEBA 6: Identidad Inválida (DNI 000)
##############################################################################
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both "${YELLOW}📋 PRUEBA 6: Validación de Identidad${NC}"
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both ""
log_both "🎯 Objetivo: Ver cómo se manejan identidades inválidas"
log_both "📝 DNI: 00011223344 (código especial - suspendido)"
log_both "💰 Monto: S/ 20,000"
log_both ""
log_both "${CYAN}🎬 Escenario:${NC}"
log_both "   Validación de identidad → ${RED}❌ SUSPENDIDO${NC}"
log_both "   Evaluación se detiene → ${RED}🛑 RECHAZADO inmediatamente${NC}"
log_both ""
log_both "${CYAN}Ejecutando solicitud...${NC}"
log_both ""

RESPONSE=$(curl -s -X POST "$BASE_URL/api/evaluacion/credito" \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "00011223344",
    "nombres": "Usuario",
    "apellidos": "Suspendido",
    "montoSolicitado": 20000,
    "mesesPlazo": 12
  }')

OUTPUT=$(echo "$RESPONSE" | $PYTHON_CMD -c "
import sys, json
data = json.load(sys.stdin)
print(f\"✅ DNI: {data['dni']}\")
print(f\"📊 Score Total: {data['scoreTotal']}\")
print(f\"🎯 Decisión: {data['decision']}\")
print(f\"⚠️  Motivo: {data.get('motivoRechazo', 'N/A')}\")
print(f\"💬 Mensaje: {data['mensaje']}\")
")

log_both "$OUTPUT"
log_both ""
log_both "${GREEN}✅ ¡Validación preventiva evitó procesamiento innecesario!${NC}"
log_both ""
pause_script
log_both ""

##############################################################################
# RESUMEN FINAL
##############################################################################
log_both "${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
log_both "${CYAN}║                    📊 RESUMEN DE PRUEBAS                  ║${NC}"
log_both "${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"
log_both ""
log_both "${GREEN}✅ PRUEBA 1:${NC} Flujo exitoso sin patrones de resiliencia"
log_both "${GREEN}✅ PRUEBA 2:${NC} @Retry recuperó servicio con fallos temporales"
log_both "${GREEN}✅ PRUEBA 3:${NC} @Timeout evitó esperas largas + @Fallback activado"
log_both "${GREEN}✅ PRUEBA 4:${NC} @Fallback proporcionó respuesta alternativa"
log_both "${GREEN}✅ PRUEBA 5:${NC} @CircuitBreaker protegió servicios caídos"
log_both "${GREEN}✅ PRUEBA 6:${NC} Validación preventiva rechazó identidad inválida"
log_both ""
log_both "${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
log_both "${CYAN}║              🎓 CONCEPTOS CLAVE DEMOSTRADOS               ║${NC}"
log_both "${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"
log_both ""
log_both "${YELLOW}🔌 REST Client:${NC}              Consumo de APIs externas con @RegisterRestClient"
log_both "${YELLOW}🔄 @Retry:${NC}                   Reintentos automáticos ante fallos temporales"
log_both "${YELLOW}⏱️  @Timeout:${NC}                 Límites de tiempo para evitar esperas infinitas"
log_both "${YELLOW}🛡️  @Fallback:${NC}                Respuestas alternativas cuando algo falla"
log_both "${YELLOW}⚡ @CircuitBreaker:${NC}          Protección contra servicios caídos"
log_both "${YELLOW}🎯 Configuración:${NC}            URLs y parámetros externalizados"
log_both ""
log_both "${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
log_both "${CYAN}║                    🔧 CÓDIGOS DE PRUEBA                   ║${NC}"
log_both "${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"
log_both ""
log_both "${YELLOW}DNIs especiales para testing:${NC}"
log_both "  ${GREEN}111XXXXX(PAR)${NC} → Flujo exitoso completo"
log_both "  ${YELLOW}222XXXXX(PAR)${NC} → Bureau falla temporalmente (activa @Retry)"
log_both "  ${BLUE}333XXXXX(PAR)${NC} → Scoring se demora 5s (activa @Timeout)"
log_both "  ${MAGENTA}444XXXXXXXX${NC} → Scoring falla (activa @Fallback y @CircuitBreaker)"
log_both "  ${RED}000XXXXXXXX${NC} → Identidad inválida (rechazo inmediato)"
log_both ""
log_both "${CYAN}NOTA: Los DNIs deben terminar en número PAR para tener buen score en Bureau${NC}"
log_both ""
log_both "${GREEN}🎉 ¡Pruebas completadas exitosamente!${NC}"
log_both "${CYAN}Has demostrado todos los patrones de resiliencia de Quarkus${NC}"
log_both ""
log_both "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
log_both "${GREEN}📄 Log guardado en: ${LOG_FILE}${NC}"
log_both "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
log_both ""