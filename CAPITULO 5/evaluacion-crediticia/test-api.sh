#!/bin/bash

# Script de pruebas para API de Evaluación Crediticia
# Usa DNI peruano (8 dígitos)

API_URL="http://localhost:8080/api/v1/creditos"
OUTPUT_FILE="resultados-pruebas-$(date +%Y%m%d-%H%M%S).txt"

echo "================================================"
echo "🇵🇪 PRUEBAS DE API - EVALUACIÓN CREDITICIA"
echo "================================================"
echo ""
echo "💾 Los resultados se guardarán en: $OUTPUT_FILE"
echo ""

# Función para logging
log_and_display() {
    echo "$1" | tee -a "$OUTPUT_FILE"
}

log_and_display "================================================"
log_and_display "🇵🇪 PRUEBAS DE API - EVALUACIÓN CREDITICIA"
log_and_display "Fecha: $(date)"
log_and_display "================================================"
log_and_display ""

# Test 1: Solicitud APROBADA
log_and_display "📋 Test 1: Evaluando solicitud con perfil EXCELENTE"
curl -s -X POST "$API_URL/evaluar" \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "12345678",
    "nombreCompleto": "Juan Pérez García",
    "email": "juan.perez@banco.pe",
    "edad": 35,
    "ingresosMensuales": 2500000,
    "deudasActuales": 300000,
    "montoSolicitado": 5000000,
    "mesesEnEmpleoActual": 48
  }' | tee -a "$OUTPUT_FILE" | jq '.'
log_and_display "✅ Esperado: APROBADA con score >= 800"
log_and_display ""

# Test 2: Solicitud APROBADA (score medio)
log_and_display "📋 Test 2: Evaluando solicitud con perfil BUENO"
curl -s -X POST "$API_URL/evaluar" \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "23456789",
    "nombreCompleto": "María Silva Torres",
    "email": "maria.silva@banco.pe",
    "edad": 28,
    "ingresosMensuales": 1800000,
    "deudasActuales": 400000,
    "montoSolicitado": 3000000,
    "mesesEnEmpleoActual": 24
  }' | tee -a "$OUTPUT_FILE" | jq '.'
log_and_display "✅ Esperado: APROBADA con score >= 650"
log_and_display ""

# Test 3: Solicitud RECHAZADA por DTI alto
log_and_display "📋 Test 3: Evaluando solicitud con DTI alto (>50%)"
curl -s -X POST "$API_URL/evaluar" \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "34567890",
    "nombreCompleto": "Carlos Rojas Vega",
    "email": "carlos.rojas@banco.pe",
    "edad": 42,
    "ingresosMensuales": 1500000,
    "deudasActuales": 900000,
    "montoSolicitado": 4000000,
    "mesesEnEmpleoActual": 12
  }' | tee -a "$OUTPUT_FILE" | jq '.'
log_and_display "❌ Esperado: RECHAZADA por ratio deuda/ingreso"
log_and_display ""

# Test 4: Solicitud RECHAZADA por inestabilidad laboral
log_and_display "📋 Test 4: Evaluando solicitud con inestabilidad laboral"
curl -s -X POST "$API_URL/evaluar" \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "45678901",
    "nombreCompleto": "Ana López Muñoz",
    "email": "ana.lopez@banco.pe",
    "edad": 23,
    "ingresosMensuales": 1200000,
    "deudasActuales": 150000,
    "montoSolicitado": 2000000,
    "mesesEnEmpleoActual": 2
  }' | tee -a "$OUTPUT_FILE" | jq '.'
log_and_display "❌ Esperado: RECHAZADA por menos de 3 meses en empleo"
log_and_display ""

# Test 5: DNI inválido
log_and_display "📋 Test 5: Probando validación de DNI inválido"
curl -s -X POST "$API_URL/evaluar" \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "12345",
    "nombreCompleto": "Pedro Inválido",
    "email": "pedro@banco.pe",
    "edad": 30,
    "ingresosMensuales": 2000000,
    "deudasActuales": 200000,
    "montoSolicitado": 3000000,
    "mesesEnEmpleoActual": 12
  }' | tee -a "$OUTPUT_FILE" | jq '.'
log_and_display "❌ Esperado: Error 400 - DNI inválido"
log_and_display ""

# Test 6: Listar solicitudes
log_and_display "📋 Test 6: Listando todas las solicitudes"
TOTAL=$(curl -s -X GET "$API_URL" | tee -a "$OUTPUT_FILE" | jq '. | length')
log_and_display "Total de solicitudes: $TOTAL"
log_and_display "✅ Esperado: Array con múltiples solicitudes"
log_and_display ""

log_and_display "================================================"
log_and_display "✅ PRUEBAS COMPLETADAS"
log_and_display "================================================"
log_and_display ""
log_and_display "💡 Notas importantes:"
log_and_display "   - DNI peruano: 8 dígitos numéricos"
log_and_display "   - Score mínimo aprobación: 650"
log_and_display "   - DTI máximo permitido: 50%"
log_and_display "   - Meses mínimos en empleo: 3"
log_and_display ""
log_and_display "📄 Resultados guardados en: $OUTPUT_FILE"

echo ""
echo "✅ Archivo generado: $OUTPUT_FILE"
