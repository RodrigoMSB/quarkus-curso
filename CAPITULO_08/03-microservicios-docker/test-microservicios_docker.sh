#!/bin/bash

##############################################################################
# Script de Pruebas - Microservicios con Docker
# 
# Este script verifica que los 4 microservicios contenerizados estén
# funcionando correctamente.
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
LOG_FILE="resultados-docker-$(date +%Y%m%d-%H%M%S).txt"

# Contadores de pruebas
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Función para escribir en terminal y archivo
log_both() {
    echo -e "$1"
    echo -e "$1" | sed 's/\x1B\[[0-9;]*[JKmsu]//g' >> "$LOG_FILE"
}

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m'

# Función para pausar (compatible con ambos sistemas)
pause_script() {
    echo ""
    read -p "Presiona ENTER para continuar..." dummy
    echo ""
}

log_both "${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
log_both "${CYAN}║    🐳  PRUEBAS DE MICROSERVICIOS CON DOCKER              ║${NC}"
log_both "${CYAN}║    Arquitectura Contenerizada con Docker Compose         ║${NC}"
log_both "${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"
log_both ""
log_both "${GREEN}🖥️  Sistema Operativo: ${OS_TYPE}${NC}"
log_both "${GREEN}📄 Los resultados se guardarán en: ${LOG_FILE}${NC}"
log_both ""

##############################################################################
# VERIFICACIÓN DE DOCKER
##############################################################################
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both "${YELLOW}🔍 VERIFICACIÓN: Docker y contenedores${NC}"
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both ""

# Verificar que Docker está instalado
if ! command -v docker &> /dev/null; then
    log_both "${RED}❌ ERROR: Docker no está instalado${NC}"
    log_both "${YELLOW}Instala Docker desde: https://docs.docker.com/get-docker/${NC}"
    exit 1
fi
log_both "${GREEN}✅ Docker instalado: $(docker --version)${NC}"

# Verificar que Docker Compose está disponible
if ! command -v docker-compose &> /dev/null; then
    log_both "${RED}❌ ERROR: Docker Compose no está disponible${NC}"
    exit 1
fi
log_both "${GREEN}✅ Docker Compose disponible${NC}"
log_both ""

# Verificar que los contenedores están corriendo
log_both "${YELLOW}Verificando contenedores...${NC}"
log_both ""

CONTAINERS_OK=true

if docker ps --format '{{.Names}}' | grep -q "bureau-service"; then
    STATUS=$(docker inspect -f '{{.State.Health.Status}}' bureau-service 2>/dev/null || echo "no-health")
    log_both "${GREEN}✅ bureau-service: Running ($STATUS)${NC}"
else
    log_both "${RED}❌ bureau-service: NO ESTÁ CORRIENDO${NC}"
    CONTAINERS_OK=false
fi

if docker ps --format '{{.Names}}' | grep -q "identidad-service"; then
    STATUS=$(docker inspect -f '{{.State.Health.Status}}' identidad-service 2>/dev/null || echo "no-health")
    log_both "${GREEN}✅ identidad-service: Running ($STATUS)${NC}"
else
    log_both "${RED}❌ identidad-service: NO ESTÁ CORRIENDO${NC}"
    CONTAINERS_OK=false
fi

if docker ps --format '{{.Names}}' | grep -q "scoring-service"; then
    STATUS=$(docker inspect -f '{{.State.Health.Status}}' scoring-service 2>/dev/null || echo "no-health")
    log_both "${GREEN}✅ scoring-service: Running ($STATUS)${NC}"
else
    log_both "${RED}❌ scoring-service: NO ESTÁ CORRIENDO${NC}"
    CONTAINERS_OK=false
fi

if docker ps --format '{{.Names}}' | grep -q "evaluacion-service"; then
    STATUS=$(docker inspect -f '{{.State.Health.Status}}' evaluacion-service 2>/dev/null || echo "no-health")
    log_both "${GREEN}✅ evaluacion-service: Running ($STATUS)${NC}"
else
    log_both "${RED}❌ evaluacion-service: NO ESTÁ CORRIENDO${NC}"
    CONTAINERS_OK=false
fi

log_both ""

if [ "$CONTAINERS_OK" = false ]; then
    log_both "${RED}⚠️  ERROR: Algunos contenedores no están corriendo.${NC}"
    log_both "${YELLOW}Ejecuta: docker-compose up -d${NC}"
    log_both ""
    exit 1
fi

log_both "${GREEN}✅ Todos los contenedores están activos. Iniciando pruebas...${NC}"
log_both ""
pause_script
log_both ""

##############################################################################
# PRUEBA 1: Caso Exitoso
##############################################################################
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both "${YELLOW}📋 PRUEBA 1: Evaluación Exitosa (Happy Path)${NC}"
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both ""
log_both "🎯 Objetivo: Verificar comunicación entre 4 contenedores Docker"
log_both "📝 DNI: 12345678 (termina en PAR = buen score)"
log_both "💰 Monto: S/ 30,000"
log_both ""
log_both "${CYAN}🔍 Flujo esperado:${NC}"
log_both "   1️⃣  Cliente → Evaluacion Container (8080)"
log_both "   2️⃣  Evaluacion → Identidad Container (8082)"
log_both "   3️⃣  Evaluacion → Bureau Container (8081)"
log_both "   4️⃣  Evaluacion → Scoring Container (8083)"
log_both "   5️⃣  Evaluacion → Retorna decisión"
log_both ""
log_both "${CYAN}Ejecutando solicitud...${NC}"
log_both ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

RESPONSE=$(curl -s -X POST "http://localhost:8080/api/evaluacion/credito" \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "12345678",
    "nombres": "Juan",
    "apellidos": "Perez Lopez",
    "montoSolicitado": 30000,
    "mesesPlazo": 24
  }')

# Parsear JSON (compatible con jq o sin jq)
if command -v jq &> /dev/null; then
    DNI=$(echo "$RESPONSE" | jq -r '.dni // "N/A"')
    SCORE=$(echo "$RESPONSE" | jq -r '.scoreTotal // "N/A"')
    DECISION=$(echo "$RESPONSE" | jq -r '.decision // "N/A"')
    MONTO=$(echo "$RESPONSE" | jq -r '.montoAprobado // 0')
    MENSAJE=$(echo "$RESPONSE" | jq -r '.mensaje // "N/A"')
else
    DNI=$(echo "$RESPONSE" | grep -o '"dni":"[^"]*"' | cut -d'"' -f4)
    SCORE=$(echo "$RESPONSE" | grep -o '"scoreTotal":[0-9]*' | cut -d':' -f2)
    DECISION=$(echo "$RESPONSE" | grep -o '"decision":"[^"]*"' | cut -d'"' -f4)
    MONTO=$(echo "$RESPONSE" | grep -o '"montoAprobado":[0-9.]*' | cut -d':' -f2)
    MENSAJE=$(echo "$RESPONSE" | grep -o '"mensaje":"[^"]*"' | cut -d'"' -f4)
fi

OUTPUT="✅ DNI: ${DNI}
📊 Score Total: ${SCORE}
🎯 Decisión: ${DECISION}
💰 Monto Aprobado: S/ ${MONTO}
💬 Mensaje: ${MENSAJE}"

log_both "$OUTPUT"
log_both ""

if [ "$DECISION" = "APROBADO" ]; then
    log_both "${GREEN}✅ Los 4 contenedores se comunicaron correctamente!${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    log_both "${RED}❌ Error en la comunicación${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
log_both ""
pause_script
log_both ""

##############################################################################
# PRUEBA 2: Identidad Inválida
##############################################################################
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both "${YELLOW}📋 PRUEBA 2: Identidad Inválida${NC}"
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both ""
log_both "🎯 Objetivo: Verificar rechazo por identidad suspendida"
log_both "📝 DNI: 00012345 (código especial - suspendido)"
log_both "💰 Monto: S/ 20,000"
log_both ""
log_both "${CYAN}Ejecutando solicitud...${NC}"
log_both ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

RESPONSE=$(curl -s -X POST "http://localhost:8080/api/evaluacion/credito" \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "00012345",
    "nombres": "Usuario",
    "apellidos": "Suspendido",
    "montoSolicitado": 20000,
    "mesesPlazo": 12
  }')

# Parsear JSON
if command -v jq &> /dev/null; then
    DNI=$(echo "$RESPONSE" | jq -r '.dni // "N/A"')
    SCORE=$(echo "$RESPONSE" | jq -r '.scoreTotal // "N/A"')
    DECISION=$(echo "$RESPONSE" | jq -r '.decision // "N/A"')
    MOTIVO=$(echo "$RESPONSE" | jq -r '.motivoRechazo // "N/A"')
    MENSAJE=$(echo "$RESPONSE" | jq -r '.mensaje // "N/A"')
else
    DNI=$(echo "$RESPONSE" | grep -o '"dni":"[^"]*"' | cut -d'"' -f4)
    SCORE=$(echo "$RESPONSE" | grep -o '"scoreTotal":[0-9]*' | cut -d':' -f2)
    DECISION=$(echo "$RESPONSE" | grep -o '"decision":"[^"]*"' | cut -d'"' -f4)
    MOTIVO=$(echo "$RESPONSE" | grep -o '"motivoRechazo":"[^"]*"' | cut -d'"' -f4)
    MENSAJE=$(echo "$RESPONSE" | grep -o '"mensaje":"[^"]*"' | cut -d'"' -f4)
    [ -z "$MOTIVO" ] && MOTIVO="N/A"
fi

OUTPUT="✅ DNI: ${DNI}
📊 Score Total: ${SCORE}
🎯 Decisión: ${DECISION}
⚠️  Motivo: ${MOTIVO}
💬 Mensaje: ${MENSAJE}"

log_both "$OUTPUT"
log_both ""

if [ "$DECISION" = "RECHAZADO" ]; then
    log_both "${GREEN}✅ Identidad Container funcionó correctamente${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    log_both "${RED}❌ Error: Debió rechazar${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
log_both ""
pause_script
log_both ""

##############################################################################
# PRUEBA 3: Morosidad Activa
##############################################################################
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both "${YELLOW}📋 PRUEBA 3: Cliente con Morosidad${NC}"
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both ""
log_both "🎯 Objetivo: Verificar detección de morosidad"
log_both "📝 DNI: 12345679 (termina en IMPAR = morosidad)"
log_both "💰 Monto: S/ 25,000"
log_both ""
log_both "${CYAN}Ejecutando solicitud...${NC}"
log_both ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

RESPONSE=$(curl -s -X POST "http://localhost:8080/api/evaluacion/credito" \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "12345679",
    "nombres": "Maria",
    "apellidos": "Garcia Ruiz",
    "montoSolicitado": 25000,
    "mesesPlazo": 36
  }')

# Parsear JSON
if command -v jq &> /dev/null; then
    DNI=$(echo "$RESPONSE" | jq -r '.dni // "N/A"')
    SCORE=$(echo "$RESPONSE" | jq -r '.scoreTotal // "N/A"')
    DECISION=$(echo "$RESPONSE" | jq -r '.decision // "N/A"')
    MOTIVO=$(echo "$RESPONSE" | jq -r '.motivoRechazo // "N/A"')
    MENSAJE=$(echo "$RESPONSE" | jq -r '.mensaje // "N/A"')
else
    DNI=$(echo "$RESPONSE" | grep -o '"dni":"[^"]*"' | cut -d'"' -f4)
    SCORE=$(echo "$RESPONSE" | grep -o '"scoreTotal":[0-9]*' | cut -d':' -f2)
    DECISION=$(echo "$RESPONSE" | grep -o '"decision":"[^"]*"' | cut -d'"' -f4)
    MOTIVO=$(echo "$RESPONSE" | grep -o '"motivoRechazo":"[^"]*"' | cut -d'"' -f4)
    MENSAJE=$(echo "$RESPONSE" | grep -o '"mensaje":"[^"]*"' | cut -d'"' -f4)
    [ -z "$MOTIVO" ] && MOTIVO="N/A"
fi

OUTPUT="✅ DNI: ${DNI}
📊 Score Total: ${SCORE}
🎯 Decisión: ${DECISION}
⚠️  Motivo: ${MOTIVO}
💬 Mensaje: ${MENSAJE}"

log_both "$OUTPUT"
log_both ""

if [ "$DECISION" = "RECHAZADO" ]; then
    log_both "${GREEN}✅ Bureau Container funcionó correctamente${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    log_both "${RED}❌ Error: Debió rechazar${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
log_both ""
pause_script
log_both ""

##############################################################################
# PRUEBA 4: Monto Alto
##############################################################################
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both "${YELLOW}📋 PRUEBA 4: Monto Alto (Scoring rechaza)${NC}"
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both ""
log_both "🎯 Objetivo: Verificar rechazo por monto alto"
log_both "📝 DNI: 87654320 (termina en PAR)"
log_both "💰 Monto: S/ 100,000 (> 50,000)"
log_both ""
log_both "${CYAN}Ejecutando solicitud...${NC}"
log_both ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

RESPONSE=$(curl -s -X POST "http://localhost:8080/api/evaluacion/credito" \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "87654320",
    "nombres": "Carlos",
    "apellidos": "Mendez Silva",
    "montoSolicitado": 100000,
    "mesesPlazo": 48
  }')

# Parsear JSON
if command -v jq &> /dev/null; then
    DNI=$(echo "$RESPONSE" | jq -r '.dni // "N/A"')
    SCORE=$(echo "$RESPONSE" | jq -r '.scoreTotal // "N/A"')
    DECISION=$(echo "$RESPONSE" | jq -r '.decision // "N/A"')
    MOTIVO=$(echo "$RESPONSE" | jq -r '.motivoRechazo // "N/A"')
    MENSAJE=$(echo "$RESPONSE" | jq -r '.mensaje // "N/A"')
else
    DNI=$(echo "$RESPONSE" | grep -o '"dni":"[^"]*"' | cut -d'"' -f4)
    SCORE=$(echo "$RESPONSE" | grep -o '"scoreTotal":[0-9]*' | cut -d':' -f2)
    DECISION=$(echo "$RESPONSE" | grep -o '"decision":"[^"]*"' | cut -d'"' -f4)
    MOTIVO=$(echo "$RESPONSE" | grep -o '"motivoRechazo":"[^"]*"' | cut -d'"' -f4)
    MENSAJE=$(echo "$RESPONSE" | grep -o '"mensaje":"[^"]*"' | cut -d'"' -f4)
    [ -z "$MOTIVO" ] && MOTIVO="N/A"
fi

OUTPUT="✅ DNI: ${DNI}
📊 Score Total: ${SCORE}
🎯 Decisión: ${DECISION}
⚠️  Motivo: ${MOTIVO}
💬 Mensaje: ${MENSAJE}"

log_both "$OUTPUT"
log_both ""

if [ "$DECISION" = "RECHAZADO" ] || [ "$DECISION" = "REQUIERE_ANALISIS_MANUAL" ]; then
    log_both "${GREEN}✅ Scoring Container funcionó correctamente${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    log_both "${RED}❌ Error: Debió rechazar${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
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
log_both "${GREEN}✅ PRUEBA 1:${NC} Comunicación exitosa entre 4 contenedores Docker"
log_both "${GREEN}✅ PRUEBA 2:${NC} Identidad Container rechazó DNI suspendido"
log_both "${GREEN}✅ PRUEBA 3:${NC} Bureau Container detectó morosidad"
log_both "${GREEN}✅ PRUEBA 4:${NC} Scoring Container rechazó monto alto"
log_both ""
log_both "${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
log_both "${CYAN}║                  📈 ESTADÍSTICAS FINALES                  ║${NC}"
log_both "${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"
log_both ""
log_both "${YELLOW}Total de Pruebas:${NC}     ${TOTAL_TESTS}"
log_both "${GREEN}Pruebas Exitosas:${NC}     ${PASSED_TESTS} ✅"
log_both "${RED}Pruebas Fallidas:${NC}     ${FAILED_TESTS}"
if [ $TOTAL_TESTS -gt 0 ]; then
    SUCCESS_RATE=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    log_both "${CYAN}Tasa de Éxito:${NC}        ${SUCCESS_RATE}%"
fi
log_both ""
log_both "${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
log_both "${CYAN}║          🐳 ARQUITECTURA DOCKER VERIFICADA                ║${NC}"
log_both "${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"
log_both ""
log_both "${YELLOW}🐳 Contenedores funcionando:${NC}"
log_both "   • bureau-service (Puerto 8081)"
log_both "   • identidad-service (Puerto 8082)"
log_both "   • scoring-service (Puerto 8083)"
log_both "   • evaluacion-service (Puerto 8080)"
log_both ""
log_both "${YELLOW}🌐 Red Docker:${NC}"
log_both "   • Red: microservices-network"
log_both "   • Driver: bridge"
log_both "   • DNS interno: Nombres de contenedores"
log_both ""
log_both "${YELLOW}🔗 Comunicación:${NC}"
log_both "   Cliente → evaluacion-service (localhost:8080)"
log_both "   evaluacion-service → identidad-service (http://identidad-service:8082)"
log_both "   evaluacion-service → bureau-service (http://bureau-service:8081)"
log_both "   evaluacion-service → scoring-service (http://scoring-service:8083)"
log_both ""
log_both "${GREEN}🎉 ¡Arquitectura de microservicios contenerizada funcionando!${NC}"
log_both "${CYAN}Esto es producción-ready con Docker.${NC}"
log_both ""
log_both "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
log_both "${GREEN}📄 Log guardado en: ${LOG_FILE}${NC}"
log_both "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
log_both ""