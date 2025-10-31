#!/bin/bash

# Script de pruebas para API de Gestión de Cuentas Bancarias
# Capítulo 3 - Quarkus Microservices Course

API_URL="http://localhost:8080/cuentas"
OUTPUT_FILE="resultados-pruebas-$(date +%Y%m%d-%H%M%S).txt"

echo "================================================"
echo "🏦 PRUEBAS DE API - GESTIÓN DE CUENTAS BANCARIAS"
echo "================================================"
echo ""
echo "💾 Los resultados se guardarán en: $OUTPUT_FILE"
echo ""

# Función para logging (muestra en pantalla Y guarda en archivo)
log_and_display() {
    echo "$1" | tee -a "$OUTPUT_FILE"
}

# Función para hacer requests y formatear JSON
make_request() {
    local method=$1
    local endpoint=$2
    local data=$3
    
    # Crear archivo temporal para capturar response y headers
    local temp_file=$(mktemp)
    local http_code
    
    if [ -z "$data" ]; then
        # GET o DELETE sin body
        http_code=$(curl -s -w "%{http_code}" -o "$temp_file" -X "$method" "$endpoint")
    else
        # POST o PUT con body
        http_code=$(curl -s -w "%{http_code}" -o "$temp_file" -X "$method" "$endpoint" \
          -H "Content-Type: application/json" \
          -d "$data")
    fi
    
    # Mostrar código HTTP
    echo "HTTP Status: $http_code" | tee -a "$OUTPUT_FILE"
    echo "" | tee -a "$OUTPUT_FILE"
    
    # Mostrar body si existe
    if [ -s "$temp_file" ]; then
        cat "$temp_file" | tee -a "$OUTPUT_FILE" | jq '.' 2>/dev/null || cat "$temp_file"
    else
        echo "(Sin contenido en el body)" | tee -a "$OUTPUT_FILE"
    fi
    
    # Limpiar archivo temporal
    rm -f "$temp_file"
}

# Función para pausa interactiva
pause() {
    echo ""
    read -n 1 -s -r -p "▶️  Presiona cualquier tecla para continuar..."
    echo ""
    echo ""
}

log_and_display "================================================"
log_and_display "🏦 PRUEBAS DE API - GESTIÓN DE CUENTAS BANCARIAS"
log_and_display "Fecha: $(date)"
log_and_display "API URL: $API_URL"
log_and_display "================================================"
log_and_display ""

# Verificar que el servidor está corriendo
log_and_display "🔍 Verificando conectividad con el servidor..."
if curl -s --max-time 3 "$API_URL" > /dev/null 2>&1; then
    log_and_display "✅ Servidor respondiendo correctamente"
else
    log_and_display "❌ ERROR: No se puede conectar al servidor"
    log_and_display "   Asegúrate de que el servidor está corriendo en $API_URL"
    log_and_display "   Ejecuta: ./mvnw quarkus:dev"
    exit 1
fi
log_and_display ""
pause

# ================================================
# SECCIÓN 1: LISTAR TODAS LAS CUENTAS (GET)
# ================================================

log_and_display "================================================"
log_and_display "📋 TEST 1: LISTAR TODAS LAS CUENTAS (GET)"
log_and_display "================================================"
log_and_display "Endpoint: GET $API_URL"
log_and_display ""

make_request "GET" "$API_URL"

log_and_display ""
log_and_display "✅ Esperado: Array con cuentas pre-cargadas"
log_and_display "   - Juan Pérez (1000000001)"
log_and_display "   - María López (1000000002)"
log_and_display "   - Carlos Ruiz (1000000003)"
log_and_display ""
pause

# ================================================
# SECCIÓN 2: OBTENER UNA CUENTA ESPECÍFICA (GET)
# ================================================

log_and_display "================================================"
log_and_display "📋 TEST 2: OBTENER CUENTA ESPECÍFICA (GET)"
log_and_display "================================================"
log_and_display "Endpoint: GET $API_URL/1000000001"
log_and_display ""

make_request "GET" "$API_URL/1000000001"

log_and_display ""
log_and_display "✅ Esperado: Cuenta de Juan Pérez con saldo 5000.00"
log_and_display ""
pause

# ================================================
# SECCIÓN 3: CREAR NUEVA CUENTA (POST)
# ================================================

log_and_display "================================================"
log_and_display "📋 TEST 3: CREAR NUEVA CUENTA (POST)"
log_and_display "================================================"
log_and_display "Endpoint: POST $API_URL"
log_and_display ""
log_and_display "Request Body:"
log_and_display '{
  "numero": "1000000004",
  "titular": "Ana Torres Mendoza",
  "saldo": 3500.00,
  "tipoCuenta": "AHORRO"
}'
log_and_display ""

make_request "POST" "$API_URL" '{
  "numero": "1000000004",
  "titular": "Ana Torres Mendoza",
  "saldo": 3500.00,
  "tipoCuenta": "AHORRO"
}'

log_and_display ""
log_and_display "✅ Esperado: HTTP 201 Created con la cuenta creada"
log_and_display ""
pause

# ================================================
# SECCIÓN 4: VERIFICAR CUENTA CREADA (GET)
# ================================================

log_and_display "================================================"
log_and_display "📋 TEST 4: VERIFICAR CUENTA CREADA (GET)"
log_and_display "================================================"
log_and_display "Endpoint: GET $API_URL/1000000004"
log_and_display ""

make_request "GET" "$API_URL/1000000004"

log_and_display ""
log_and_display "✅ Esperado: Cuenta de Ana Torres con saldo 3500.00"
log_and_display ""
pause

# ================================================
# SECCIÓN 5: ACTUALIZAR CUENTA (PUT)
# ================================================

log_and_display "================================================"
log_and_display "📋 TEST 5: ACTUALIZAR CUENTA (PUT)"
log_and_display "================================================"
log_and_display "Endpoint: PUT $API_URL/1000000004"
log_and_display ""
log_and_display "Request Body (cambio de saldo y tipo):"
log_and_display '{
  "numero": "1000000004",
  "titular": "Ana Torres Mendoza",
  "saldo": 7500.00,
  "tipoCuenta": "CORRIENTE"
}'
log_and_display ""

make_request "PUT" "$API_URL/1000000004" '{
  "numero": "1000000004",
  "titular": "Ana Torres Mendoza",
  "saldo": 7500.00,
  "tipoCuenta": "CORRIENTE"
}'

log_and_display ""
log_and_display "✅ Esperado: HTTP 200 OK con cuenta actualizada"
log_and_display "   - Saldo: 7500.00 (antes: 3500.00)"
log_and_display "   - Tipo: CORRIENTE (antes: AHORRO)"
log_and_display ""
pause

# ================================================
# SECCIÓN 6: VERIFICAR ACTUALIZACIÓN (GET)
# ================================================

log_and_display "================================================"
log_and_display "📋 TEST 6: VERIFICAR ACTUALIZACIÓN (GET)"
log_and_display "================================================"
log_and_display "Endpoint: GET $API_URL/1000000004"
log_and_display ""

make_request "GET" "$API_URL/1000000004"

log_and_display ""
log_and_display "✅ Esperado: Saldo 7500.00 y tipo CORRIENTE"
log_and_display ""
pause

# ================================================
# SECCIÓN 7: ELIMINAR CUENTA (DELETE)
# ================================================

log_and_display "================================================"
log_and_display "📋 TEST 7: ELIMINAR CUENTA (DELETE)"
log_and_display "================================================"
log_and_display "Endpoint: DELETE $API_URL/1000000003"
log_and_display "          (Eliminando cuenta de Carlos Ruiz)"
log_and_display ""

make_request "DELETE" "$API_URL/1000000003"

log_and_display ""
log_and_display "✅ Esperado: HTTP 204 No Content (sin body)"
log_and_display ""
pause

# ================================================
# SECCIÓN 8: VERIFICAR ELIMINACIÓN (GET)
# ================================================

log_and_display "================================================"
log_and_display "📋 TEST 8: VERIFICAR ELIMINACIÓN (GET)"
log_and_display "================================================"
log_and_display "Endpoint: GET $API_URL/1000000003"
log_and_display ""

make_request "GET" "$API_URL/1000000003"

log_and_display ""
log_and_display "✅ Esperado: HTTP 404 Not Found - 'Cuenta no encontrada'"
log_and_display ""
pause

# ================================================
# SECCIÓN 9: CUENTA INEXISTENTE (GET)
# ================================================

log_and_display "================================================"
log_and_display "📋 TEST 9: OBTENER CUENTA INEXISTENTE (GET)"
log_and_display "================================================"
log_and_display "Endpoint: GET $API_URL/9999999999"
log_and_display ""

make_request "GET" "$API_URL/9999999999"

log_and_display ""
log_and_display "✅ Esperado: HTTP 404 Not Found"
log_and_display ""
pause

# ================================================
# SECCIÓN 10: ACTUALIZAR CUENTA INEXISTENTE (PUT)
# ================================================

log_and_display "================================================"
log_and_display "📋 TEST 10: ACTUALIZAR CUENTA INEXISTENTE (PUT)"
log_and_display "================================================"
log_and_display "Endpoint: PUT $API_URL/9999999999"
log_and_display ""

make_request "PUT" "$API_URL/9999999999" '{
  "numero": "9999999999",
  "titular": "Inexistente",
  "saldo": 1000.00,
  "tipoCuenta": "AHORRO"
}'

log_and_display ""
log_and_display "✅ Esperado: HTTP 404 Not Found"
log_and_display ""
pause

# ================================================
# SECCIÓN 11: ELIMINAR CUENTA INEXISTENTE (DELETE)
# ================================================

log_and_display "================================================"
log_and_display "📋 TEST 11: ELIMINAR CUENTA INEXISTENTE (DELETE)"
log_and_display "================================================"
log_and_display "Endpoint: DELETE $API_URL/9999999999"
log_and_display ""

make_request "DELETE" "$API_URL/9999999999"

log_and_display ""
log_and_display "✅ Esperado: HTTP 404 Not Found"
log_and_display ""
pause

# ================================================
# SECCIÓN 12: LISTAR TODAS LAS CUENTAS FINAL (GET)
# ================================================

log_and_display "================================================"
log_and_display "📋 TEST 12: ESTADO FINAL - LISTAR TODAS (GET)"
log_and_display "================================================"
log_and_display "Endpoint: GET $API_URL"
log_and_display ""

RESPONSE=$(curl -s -X GET "$API_URL")
echo "$RESPONSE" | tee -a "$OUTPUT_FILE" | jq '.' 2>/dev/null || echo "$RESPONSE"

TOTAL=$(echo "$RESPONSE" | jq '. | length' 2>/dev/null || echo "?")
log_and_display ""
log_and_display "📊 Total de cuentas: $TOTAL"
log_and_display ""
log_and_display "✅ Esperado: 3 cuentas"
log_and_display "   - Juan Pérez (1000000001) - original"
log_and_display "   - María López (1000000002) - original"
log_and_display "   - Ana Torres (1000000004) - nueva creada"
log_and_display "   ❌ Carlos Ruiz (1000000003) - eliminada"
log_and_display ""

# ================================================
# RESUMEN FINAL
# ================================================

log_and_display "================================================"
log_and_display "✅ PRUEBAS COMPLETADAS"
log_and_display "================================================"
log_and_display ""
log_and_display "📊 RESUMEN DE OPERACIONES CRUD:"
log_and_display ""
log_and_display "   ✅ READ (GET)    - Listar todas las cuentas"
log_and_display "   ✅ READ (GET)    - Obtener cuenta específica"
log_and_display "   ✅ CREATE (POST) - Crear nueva cuenta"
log_and_display "   ✅ UPDATE (PUT)  - Actualizar cuenta existente"
log_and_display "   ✅ DELETE (DELETE) - Eliminar cuenta"
log_and_display ""
log_and_display "📊 CASOS DE ERROR PROBADOS:"
log_and_display ""
log_and_display "   ✅ GET cuenta inexistente (404)"
log_and_display "   ✅ PUT cuenta inexistente (404)"
log_and_display "   ✅ DELETE cuenta inexistente (404)"
log_and_display ""
log_and_display "💡 NOTAS IMPORTANTES:"
log_and_display ""
log_and_display "   - Endpoint base: $API_URL"
log_and_display "   - Números de cuenta: 10 dígitos"
log_and_display "   - Tipos válidos: AHORRO, CORRIENTE"
log_and_display "   - Saldo: BigDecimal (precisión monetaria)"
log_and_display ""
log_and_display "📄 Resultados guardados en: $OUTPUT_FILE"
log_and_display ""
log_and_display "🔗 RECURSOS ÚTILES:"
log_and_display ""
log_and_display "   - Swagger UI: http://localhost:8080/q/swagger-ui"
log_and_display "   - Dev UI: http://localhost:8080/q/dev"
log_and_display "   - OpenAPI Spec: http://localhost:8080/q/openapi"
log_and_display ""

echo ""
echo "✅ Archivo generado: $OUTPUT_FILE"
echo ""
echo "💡 TIP: Para ver el archivo formateado:"
echo "   cat $OUTPUT_FILE"
echo ""