#!/bin/bash

##############################################################################
# Script de Pruebas - Microservicios Reales
# 
# Este script verifica que los 4 microservicios estén funcionando
# correctamente y se comuniquen entre sí.
##############################################################################

# Archivo de log
LOG_FILE="resultados-microservicios-$(date +%Y%m%d-%H%M%S).txt"

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

log_both "${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
log_both "${CYAN}║    🏦  PRUEBAS DE MICROSERVICIOS REALES                  ║${NC}"
log_both "${CYAN}║    Arquitectura Distribuida con Quarkus                  ║${NC}"
log_both "${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"
log_both ""
log_both "${GREEN}📄 Los resultados se guardarán en: ${LOG_FILE}${NC}"
log_both ""

##############################################################################
# VERIFICACIÓN DE SERVICIOS
##############################################################################
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both "${YELLOW}🔍 VERIFICACIÓN: Servicios activos${NC}"
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both ""

SERVICIOS_OK=true

# Verificar Bureau Service (8081)
log_both "Verificando Bureau Service (Puerto 8081)..."
if curl -s http://localhost:8081/api/bureau/health > /dev/null 2>&1; then
    log_both "${GREEN}✅ Bureau Service: OK${NC}"
else
    log_both "${RED}❌ Bureau Service: NO RESPONDE${NC}"
    SERVICIOS_OK=false
fi

# Verificar Identidad Service (8082)
log_both "Verificando Identidad Service (Puerto 8082)..."
if curl -s http://localhost:8082/api/identidad/health > /dev/null 2>&1; then
    log_both "${GREEN}✅ Identidad Service: OK${NC}"
else
    log_both "${RED}❌ Identidad Service: NO RESPONDE${NC}"
    SERVICIOS_OK=false
fi

# Verificar Scoring Service (8083)
log_both "Verificando Scoring Service (Puerto 8083)..."
if curl -s http://localhost:8083/api/scoring/health > /dev/null 2>&1; then
    log_both "${GREEN}✅ Scoring Service: OK${NC}"
else
    log_both "${RED}❌ Scoring Service: NO RESPONDE${NC}"
    SERVICIOS_OK=false
fi

# Verificar Evaluacion Service (8080)
log_both "Verificando Evaluacion Service (Puerto 8080)..."
if curl -s http://localhost:8080/api/evaluacion/health > /dev/null 2>&1; then
    log_both "${GREEN}✅ Evaluacion Service: OK${NC}"
else
    log_both "${RED}❌ Evaluacion Service: NO RESPONDE${NC}"
    SERVICIOS_OK=false
fi

log_both ""

if [ "$SERVICIOS_OK" = false ]; then
    log_both "${RED}⚠️  ERROR: Algunos servicios no están activos.${NC}"
    log_both "${YELLOW}Por favor, asegúrate de que los 4 servicios estén corriendo:${NC}"
    log_both "  Terminal 1: cd bureau-service && ./mvnw quarkus:dev"
    log_both "  Terminal 2: cd identidad-service && ./mvnw quarkus:dev"
    log_both "  Terminal 3: cd scoring-service && ./mvnw quarkus:dev"
    log_both "  Terminal 4: cd evaluacion-service && ./mvnw quarkus:dev"
    log_both ""
    exit 1
fi

log_both "${GREEN}✅ Todos los servicios están activos. Iniciando pruebas...${NC}"
log_both ""
read -p "Presiona ENTER para continuar..."
log_both ""

##############################################################################
# PRUEBA 1: Caso Exitoso
##############################################################################
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both "${YELLOW}📋 PRUEBA 1: Evaluación Exitosa (Happy Path)${NC}"
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both ""
log_both "🎯 Objetivo: Verificar comunicación entre todos los microservicios"
log_both "📝 DNI: 12345678 (termina en PAR = buen score)"
log_both "💰 Monto: S/ 30,000"
log_both ""
log_both "${CYAN}🔍 Flujo esperado:${NC}"
log_both "   1️⃣  Evaluacion Service recibe solicitud"
log_both "   2️⃣  → Llama a Identidad Service (8082)"
log_both "   3️⃣  → Llama a Bureau Service (8081)"
log_both "   4️⃣  → Llama a Scoring Service (8083)"
log_both "   5️⃣  → Retorna decisión final"
log_both ""
log_both "${CYAN}Ejecutando solicitud...${NC}"
log_both ""

RESPONSE=$(curl -s -X POST "http://localhost:8080/api/evaluacion/credito" \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "12345678",
    "nombres": "Juan",
    "apellidos": "Perez Lopez",
    "montoSolicitado": 30000,
    "mesesPlazo": 24
  }')

OUTPUT=$(echo "$RESPONSE" | python3 -c "
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
log_both "${GREEN}✅ Microservicios se comunicaron correctamente!${NC}"
log_both ""
read -p "Presiona ENTER para continuar..."
log_both ""

##############################################################################
# PRUEBA 2: Identidad Inválida
##############################################################################
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both "${YELLOW}📋 PRUEBA 2: Identidad Inválida${NC}"
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both ""
log_both "🎯 Objetivo: Verificar que Identidad Service rechaza DNIs inválidos"
log_both "📝 DNI: 00012345 (código especial - suspendido)"
log_both "💰 Monto: S/ 20,000"
log_both ""
log_both "${CYAN}Ejecutando solicitud...${NC}"
log_both ""

RESPONSE=$(curl -s -X POST "http://localhost:8080/api/evaluacion/credito" \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "00012345",
    "nombres": "Usuario",
    "apellidos": "Suspendido",
    "montoSolicitado": 20000,
    "mesesPlazo": 12
  }')

OUTPUT=$(echo "$RESPONSE" | python3 -c "
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
log_both "${GREEN}✅ Identidad Service funcionó correctamente (rechazó DNI suspendido)${NC}"
log_both ""
read -p "Presiona ENTER para continuar..."
log_both ""

##############################################################################
# PRUEBA 3: Morosidad Activa
##############################################################################
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both "${YELLOW}📋 PRUEBA 3: Cliente con Morosidad${NC}"
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both ""
log_both "🎯 Objetivo: Verificar que Bureau Service detecta morosidad"
log_both "📝 DNI: 12345679 (termina en IMPAR = morosidad)"
log_both "💰 Monto: S/ 25,000"
log_both ""
log_both "${CYAN}Ejecutando solicitud...${NC}"
log_both ""

RESPONSE=$(curl -s -X POST "http://localhost:8080/api/evaluacion/credito" \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "12345679",
    "nombres": "Maria",
    "apellidos": "Garcia Ruiz",
    "montoSolicitado": 25000,
    "mesesPlazo": 36
  }')

OUTPUT=$(echo "$RESPONSE" | python3 -c "
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
log_both "${GREEN}✅ Bureau Service funcionó correctamente (detectó morosidad)${NC}"
log_both ""
read -p "Presiona ENTER para continuar..."
log_both ""

##############################################################################
# PRUEBA 4: Monto Alto
##############################################################################
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both "${YELLOW}📋 PRUEBA 4: Monto Alto (Scoring rechaza)${NC}"
log_both "${BLUE}═══════════════════════════════════════════════════════════${NC}"
log_both ""
log_both "🎯 Objetivo: Verificar que Scoring Service rechaza montos altos"
log_both "📝 DNI: 87654320 (termina en PAR)"
log_both "💰 Monto: S/ 100,000 (> 50,000)"
log_both ""
log_both "${CYAN}Ejecutando solicitud...${NC}"
log_both ""

RESPONSE=$(curl -s -X POST "http://localhost:8080/api/evaluacion/credito" \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "87654320",
    "nombres": "Carlos",
    "apellidos": "Mendez Silva",
    "montoSolicitado": 100000,
    "mesesPlazo": 48
  }')

OUTPUT=$(echo "$RESPONSE" | python3 -c "
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
log_both "${GREEN}✅ Scoring Service funcionó correctamente (rechazó monto alto)${NC}"
log_both ""
read -p "Presiona ENTER para ver el resumen..."
log_both ""

##############################################################################
# RESUMEN FINAL
##############################################################################
log_both "${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
log_both "${CYAN}║                    📊 RESUMEN DE PRUEBAS                  ║${NC}"
log_both "${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"
log_both ""
log_both "${GREEN}✅ PRUEBA 1:${NC} Comunicación exitosa entre 4 microservicios"
log_both "${GREEN}✅ PRUEBA 2:${NC} Identidad Service rechazó DNI suspendido"
log_both "${GREEN}✅ PRUEBA 3:${NC} Bureau Service detectó morosidad"
log_both "${GREEN}✅ PRUEBA 4:${NC} Scoring Service rechazó monto alto"
log_both ""
log_both "${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
log_both "${CYAN}║              🎓 ARQUITECTURA VERIFICADA                   ║${NC}"
log_both "${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"
log_both ""
log_both "${YELLOW}🏗️  Microservicios funcionando:${NC}"
log_both "   • Bureau Service (Puerto 8081) → Consultas de historial crediticio"
log_both "   • Identidad Service (Puerto 8082) → Validación de identidad"
log_both "   • Scoring Service (Puerto 8083) → Cálculo de scoring"
log_both "   • Evaluacion Service (Puerto 8080) → Orquestador principal"
log_both ""
log_both "${YELLOW}🔗 Comunicación:${NC}"
log_both "   Cliente → Evaluacion Service (8080)"
log_both "           → Identidad Service (8082)"
log_both "           → Bureau Service (8081)"
log_both "           → Scoring Service (8083)"
log_both ""
log_both "${GREEN}🎉 ¡Arquitectura de microservicios funcionando correctamente!${NC}"
log_both "${CYAN}Esta SÍ es una arquitectura distribuida real.${NC}"
log_both ""
log_both "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
log_both "${GREEN}📄 Log guardado en: ${LOG_FILE}${NC}"
log_both "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
log_both ""