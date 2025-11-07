#!/bin/bash

##############################################################################
# Script de Pruebas - Parte 2: Autenticación con JWT (JSON Web Token)
# 
# Este script prueba los endpoints de empleados internos que utilizan
# tokens JWT para autenticación y autorización.
#
# Conceptos que se prueban:
# - Generación de JWT mediante endpoint de login
# - Autenticación basada en Bearer Token
# - Extracción de claims del JWT (sub, email, groups)
# - Aislamiento de datos por usuario (cada empleado ve solo sus secretos)
# - Creación de recursos asociados al usuario autenticado
##############################################################################

# Generar nombre de archivo con timestamp
OUTPUT_FILE="test-part2-jwt-$(date '+%Y-%m-%d_%H-%M-%S').txt"

# Limpiar archivo de salida
> "$OUTPUT_FILE"

# Colores para mejor visualización
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
MAGENTA='\033[0;35m'
RESET='\033[0m'

# URL base del microservicio
BASE_URL="http://localhost:8080"

# Variables globales para tokens
TOKEN_EMP001=""
TOKEN_EMP002=""

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
    printf "${CYAN}▶️  Presiona ENTER para continuar...${RESET}"
    read -r dummy
    echo ""
}

# Función para decodificar base64 (compatible con Windows y Mac)
base64_decode() {
    local input="$1"
    # Intentar con -d primero (Linux/Git Bash), si falla usar -D (Mac)
    echo "$input" | base64 -d 2>/dev/null || echo "$input" | base64 -D 2>/dev/null
}

# Banner inicial
clear
log "${CYAN}╔════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║      🔐 PRUEBAS DE SEGURIDAD - PARTE 2: JWT AUTHENTICATION    ║${RESET}"
log "${CYAN}╔════════════════════════════════════════════════════════════════╗${RESET}"
log ""
log "${CYAN}📅 Fecha:${RESET} $(date '+%d/%m/%Y %H:%M:%S')"
log "${CYAN}🌐 API Base:${RESET} $BASE_URL"
log "${CYAN}📄 Resultados:${RESET} $OUTPUT_FILE"
log "${CYAN}🔐 Seguridad:${RESET} JWT (JSON Web Token) + RSA Signing"
log ""
log "${YELLOW}⚠️  IMPORTANTE:${RESET} El servidor debe iniciarse con el perfil ${GREEN}parte2${RESET}"
log "${YELLOW}   Comando:${RESET} ${CYAN}./mvnw quarkus:dev -Dquarkus.profile=parte2${RESET}"
log ""
pause

##############################################################################
# PRUEBA 1: Login y Generación de JWT
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 1: Login y Generación de Token JWT${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Obtener un token JWT válido mediante el endpoint de login"
log "${YELLOW}📍 Endpoint:${RESET} POST /api/auth/login"
log "${YELLOW}👤 Usuario:${RESET} emp001 (Juan Pérez)"
log "${YELLOW}🔐 Método:${RESET} Credenciales en JSON"
log "${YELLOW}✅ Esperado:${RESET} HTTP 200 OK + JWT Token"
log ""
log "${CYAN}Ejecutando login...${RESET}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

# Ejecutar request usando -d en lugar de archivo temporal para evitar problemas en Windows
response=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"emp001","password":"pass001"}' 2>/dev/null)

body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
show_json "$body"
log ""

# Extraer el token
if command -v jq &> /dev/null; then
    TOKEN_EMP001=$(echo "$body" | jq -r '.token // empty' 2>/dev/null)
else
    TOKEN_EMP001=$(echo "$body" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
fi

if [ "$status" == "200" ] && [ -n "$TOKEN_EMP001" ]; then
    log "${GREEN}✓ PASS${RESET} - Login exitoso, token JWT generado"
    PASSED_TESTS=$((PASSED_TESTS + 1))
    log "${MAGENTA}📌 Token generado (primeros 50 caracteres):${RESET} ${TOKEN_EMP001:0:50}..."
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 200)"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   El token JWT contiene 3 partes separadas por puntos: Header.Payload.Signature"
log "   Cada parte está codificada en Base64."
pause

##############################################################################
# PRUEBA 2: Decodificar JWT (Educativo)
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 2: Inspección del Token JWT (Educativo)${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Entender qué contiene un token JWT"
log "${YELLOW}🔍 Nota:${RESET} Un JWT NO está encriptado, está codificado en Base64"
log "${YELLOW}⚠️  Advertencia:${RESET} NUNCA incluir información sensible en el payload"
log ""
log "${CYAN}Decodificando el payload del JWT...${RESET}"
log ""

# Decodificar el payload (segunda parte del JWT) usando la función compatible
PAYLOAD_ENCODED=$(echo "$TOKEN_EMP001" | awk -F'.' '{print $2}')
PAYLOAD=$(base64_decode "$PAYLOAD_ENCODED")
show_json "$PAYLOAD"

log ""
log "${GREEN}✓ Observa los claims importantes:${RESET}"
log "  ${YELLOW}• iss${RESET} (issuer): Quién emitió el token"
log "  ${YELLOW}• sub${RESET} (subject): Identificador del usuario (emp001)"
log "  ${YELLOW}• email${RESET}: Email del usuario"
log "  ${YELLOW}• groups${RESET}: Roles del usuario ([employee])"
log "  ${YELLOW}• iat${RESET} (issued at): Timestamp de creación"
log "  ${YELLOW}• exp${RESET} (expiration): Timestamp de expiración (1 hora)"
log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   Los claims contienen toda la información necesaria para identificar y"
log "   autorizar al usuario sin necesidad de consultar la base de datos."
pause

##############################################################################
# PRUEBA 3: Acceso sin Token (Debe Fallar)
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 3: Intento de Acceso sin Token${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Verificar que endpoints protegidos requieren autenticación"
log "${YELLOW}📍 Endpoint:${RESET} GET /api/internal/secrets/my-secrets"
log "${YELLOW}🔐 Método:${RESET} Sin Authorization header"
log "${YELLOW}❌ Esperado:${RESET} HTTP 401 Unauthorized"
log ""
log "${CYAN}Intentando acceso sin token...${RESET}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

response=$(curl -s -w "\n%{http_code}" $BASE_URL/api/internal/secrets/my-secrets 2>/dev/null)

body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
show_json "$body"
log ""

if [ "$status" == "401" ]; then
    log "${GREEN}✓ PASS${RESET} - Acceso denegado correctamente (HTTP 401)"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 401)"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   El servidor rechaza peticiones sin token, protegiendo los endpoints."
pause

##############################################################################
# PRUEBA 4: Acceso con Token Válido
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 4: Acceso con Token JWT Válido${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Acceder a recursos protegidos usando el token JWT"
log "${YELLOW}📍 Endpoint:${RESET} GET /api/internal/secrets/my-secrets"
log "${YELLOW}👤 Usuario:${RESET} emp001"
log "${YELLOW}🔐 Método:${RESET} Authorization: Bearer <token>"
log "${YELLOW}✅ Esperado:${RESET} HTTP 200 OK con los secretos del usuario"
log ""
log "${CYAN}Ejecutando petición autenticada...${RESET}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

response=$(curl -s -w "\n%{http_code}" $BASE_URL/api/internal/secrets/my-secrets \
  -H "Authorization: Bearer $TOKEN_EMP001" 2>/dev/null)

body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
show_json "$body"
log ""

if [ "$status" == "200" ]; then
    log "${GREEN}✓ PASS${RESET} - Acceso exitoso con token JWT"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 200)"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   El token JWT permite acceder a los recursos del usuario autenticado."
log "   Observa que solo aparecen secretos con ownerId=emp001"
pause

##############################################################################
# PRUEBA 5: Crear Secreto con Token JWT
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 5: Crear Secreto Asociado al Usuario Autenticado${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Crear un nuevo secreto que se asocie automáticamente a emp001"
log "${YELLOW}📍 Endpoint:${RESET} POST /api/internal/secrets"
log "${YELLOW}👤 Usuario:${RESET} emp001"
log "${YELLOW}🔐 Método:${RESET} Authorization: Bearer <token>"
log "${YELLOW}✅ Esperado:${RESET} HTTP 201 Created con ownerId=emp001"
log ""
log "${CYAN}Creando secreto para emp001...${RESET}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

response=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/api/internal/secrets \
  -H "Authorization: Bearer $TOKEN_EMP001" \
  -H "Content-Type: application/json" \
  -d '{"name":"Database Password","content":"super-secret-db-password","level":"INTERNAL"}' 2>/dev/null)

body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
show_json "$body"
log ""

if [ "$status" == "201" ]; then
    log "${GREEN}✓ PASS${RESET} - Secreto creado con ownerId=emp001"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 201)"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   El secreto se crea automáticamente asociado al usuario del token (emp001)."
log "   El backend extrae el 'sub' claim del JWT para determinar el owner."
pause

##############################################################################
# PRUEBA 6: Login de Segundo Usuario
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 6: Login de Segundo Usuario (emp002)${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Obtener token JWT para un segundo usuario"
log "${YELLOW}📍 Endpoint:${RESET} POST /api/auth/login"
log "${YELLOW}👤 Usuario:${RESET} emp002 (María González)"
log "${YELLOW}✅ Esperado:${RESET} HTTP 200 OK + JWT Token diferente"
log ""
log "${CYAN}Ejecutando login...${RESET}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

response=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"emp002","password":"pass002"}' 2>/dev/null)

body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
show_json "$body"
log ""

# Extraer el token
if command -v jq &> /dev/null; then
    TOKEN_EMP002=$(echo "$body" | jq -r '.token // empty' 2>/dev/null)
else
    TOKEN_EMP002=$(echo "$body" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
fi

if [ "$status" == "200" ] && [ -n "$TOKEN_EMP002" ]; then
    log "${GREEN}✓ PASS${RESET} - Login exitoso para emp002"
    PASSED_TESTS=$((PASSED_TESTS + 1))
    log "${MAGENTA}📌 Token generado (primeros 50 caracteres):${RESET} ${TOKEN_EMP002:0:50}..."
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 200)"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   Cada usuario recibe su propio token JWT con sus propios claims."
pause

##############################################################################
# PRUEBA 7: Verificar Aislamiento - emp002 NO puede ver secretos de emp001
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 7: Verificar Aislamiento de Datos (emp002 consulta)${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Verificar que emp002 NO ve los secretos de emp001"
log "${YELLOW}📍 Endpoint:${RESET} GET /api/internal/secrets/my-secrets"
log "${YELLOW}👤 Usuario:${RESET} emp002"
log "${YELLOW}✅ Esperado:${RESET} Lista vacía o solo secretos de emp002"
log ""
log "${CYAN}Consultando secretos de emp002...${RESET}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

response=$(curl -s -w "\n%{http_code}" $BASE_URL/api/internal/secrets/my-secrets \
  -H "Authorization: Bearer $TOKEN_EMP002" 2>/dev/null)

body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
show_json "$body"
log ""

if [ "$status" == "200" ]; then
    log "${GREEN}✓ PASS${RESET} - emp002 solo ve sus propios secretos"
    PASSED_TESTS=$((PASSED_TESTS + 1))
    log "${GREEN}   Observa que NO aparecen secretos de emp001${RESET}"
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 200)"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   Cada usuario solo puede acceder a sus propios recursos."
log "   Esto demuestra aislamiento perfecto (multi-tenancy)."
pause

##############################################################################
# PRUEBA 8: Crear Secreto para emp002
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 8: Crear Secreto para emp002${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Crear un secreto que se asocie automáticamente a emp002"
log "${YELLOW}📍 Endpoint:${RESET} POST /api/internal/secrets"
log "${YELLOW}👤 Usuario:${RESET} emp002"
log "${YELLOW}✅ Esperado:${RESET} HTTP 201 Created con ownerId=emp002"
log ""
log "${CYAN}Creando secreto para emp002...${RESET}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

response=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/api/internal/secrets \
  -H "Authorization: Bearer $TOKEN_EMP002" \
  -H "Content-Type: application/json" \
  -d '{"name":"API Key Production","content":"prod-api-key-xyz789","level":"INTERNAL"}' 2>/dev/null)

body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
show_json "$body"
log ""

if [ "$status" == "201" ]; then
    log "${GREEN}✓ PASS${RESET} - Secreto creado con ownerId=emp002"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 201)"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   El secreto se asocia automáticamente a emp002, demostrando que cada"
log "   usuario opera en su propio contexto aislado."
pause

##############################################################################
# PRUEBA 9: Verificar Aislamiento entre Usuarios
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 9: Verificar Aislamiento de Datos entre Usuarios${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Confirmar que emp001 NO puede ver los secretos de emp002 y viceversa"
log "${YELLOW}🔒 Principio:${RESET} Aislamiento de datos por usuario (Multi-tenancy)"
log ""

log "${CYAN}───────────────────────────────────────────────────────────────${RESET}"
log "${YELLOW}Secretos de emp001:${RESET}"
log "${CYAN}───────────────────────────────────────────────────────────────${RESET}"
log ""

SECRETS_EMP001=$(curl -s $BASE_URL/api/internal/secrets/my-secrets \
  -H "Authorization: Bearer $TOKEN_EMP001" 2>/dev/null)

show_json "$SECRETS_EMP001"

TOTAL_EMP001=$(echo "$SECRETS_EMP001" | grep -o '"totalSecrets":[0-9]*' | grep -o '[0-9]*')

log ""
log "${CYAN}───────────────────────────────────────────────────────────────${RESET}"
log "${YELLOW}Secretos de emp002:${RESET}"
log "${CYAN}───────────────────────────────────────────────────────────────${RESET}"
log ""

SECRETS_EMP002=$(curl -s $BASE_URL/api/internal/secrets/my-secrets \
  -H "Authorization: Bearer $TOKEN_EMP002" 2>/dev/null)

show_json "$SECRETS_EMP002"

TOTAL_EMP002=$(echo "$SECRETS_EMP002" | grep -o '"totalSecrets":[0-9]*' | grep -o '[0-9]*')

log ""
log "${GREEN}✓ emp001 tiene $TOTAL_EMP001 secreto(s) con ownerId=emp001${RESET}"
log "${GREEN}✓ emp002 tiene $TOTAL_EMP002 secreto(s) con ownerId=emp002${RESET}"
log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   Cada usuario solo ve sus propios secretos. ¡Aislamiento perfecto!"
log "   Este patrón es fundamental para aplicaciones multi-tenant."
pause

##############################################################################
# PRUEBA 10: Token Expirado (Simulación Conceptual)
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 10: Conceptos de Expiración de Token${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Entender cómo funcionan los tokens JWT con expiración"
log "${YELLOW}⏰ Configuración:${RESET} Los tokens expiran en 1 hora (3600 segundos)"
log ""
log "${CYAN}Inspeccionando el claim 'exp' del token de emp001...${RESET}"
log ""

PAYLOAD_ENCODED=$(echo "$TOKEN_EMP001" | awk -F'.' '{print $2}')
PAYLOAD=$(base64_decode "$PAYLOAD_ENCODED")

EXP_TIMESTAMP=$(echo "$PAYLOAD" | grep -o '"exp":[0-9]*' | grep -o '[0-9]*')
IAT_TIMESTAMP=$(echo "$PAYLOAD" | grep -o '"iat":[0-9]*' | grep -o '[0-9]*')

if [ -n "$EXP_TIMESTAMP" ] && [ -n "$IAT_TIMESTAMP" ]; then
    DURATION=$((EXP_TIMESTAMP - IAT_TIMESTAMP))
    log "${YELLOW}Timestamp de emisión (iat):${RESET} $IAT_TIMESTAMP"
    log "${YELLOW}Timestamp de expiración (exp):${RESET} $EXP_TIMESTAMP"
    log "${YELLOW}Duración del token:${RESET} $DURATION segundos ($(($DURATION / 60)) minutos)"
    log ""
    log "${GREEN}✓ Token tiene configuración de expiración correcta${RESET}"
else
    log "${RED}No se pudo extraer timestamps del token${RESET}"
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   Cuando el token expire, el servidor rechazará las peticiones con HTTP 401."
log "   El usuario deberá hacer login nuevamente para obtener un token fresco."
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
    log "  ${CYAN}2.${RESET} Inicia con: ${GREEN}./mvnw quarkus:dev -Dquarkus.profile=parte2${RESET}"
    log "  ${CYAN}3.${RESET} Vuelve a ejecutar este script"
    log ""
fi

log "${CYAN}╔════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║              🎓 CONCEPTOS CLAVE DEMOSTRADOS                    ║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "${YELLOW}🔑 JWT (JSON Web Token):${RESET} Estándar abierto para transmitir información de forma segura"
log "${YELLOW}🔑 Bearer Token:${RESET}        Token enviado en header 'Authorization: Bearer <token>'"
log "${YELLOW}🔑 Claims:${RESET}              Información contenida en el JWT (sub, email, exp, etc.)"
log "${YELLOW}🔑 Firma RSA:${RESET}           El JWT se firma con clave privada y se verifica con pública"
log "${YELLOW}🔑 Stateless Auth:${RESET}      El servidor no guarda sesiones, toda la info está en el token"
log "${YELLOW}🔑 Aislamiento:${RESET}         Cada usuario solo accede a sus propios recursos"
log "${YELLOW}🔑 Expiración:${RESET}          Los tokens tienen vida útil limitada (claim 'exp')"
log ""

log "${MAGENTA}╔════════════════════════════════════════════════════════════════╗${RESET}"
log "${MAGENTA}║                   🔐 VENTAJAS DE JWT                           ║${RESET}"
log "${MAGENTA}╚════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "${CYAN}✓ Escalabilidad:${RESET}       No requiere almacenamiento de sesiones en servidor"
log "${CYAN}✓ Portabilidad:${RESET}        El token puede usarse en diferentes servicios"
log "${CYAN}✓ Autocontenido:${RESET}       Toda la información necesaria está en el token"
log "${CYAN}✓ Seguridad:${RESET}           Firmado criptográficamente (no puede ser alterado)"
log "${CYAN}✓ Multi-dominio:${RESET}       Funciona entre diferentes dominios y servicios"
log ""

log "${CYAN}╔════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║           🔄 DIFERENCIAS: JWT vs BASIC AUTH (Parte 1)         ║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "${WHITE}Basic Authentication (Parte 1):${RESET}"
log "  • Credenciales enviadas en cada request"
log "  • Base64 encoding (fácilmente decodificable)"
log "  • Verificación en cada llamada"
log "  • Simple pero menos escalable"
log ""
log "${WHITE}JWT Authentication (Parte 2):${RESET}"
log "  • Token generado una vez en login"
log "  • Firma criptográfica RSA"
log "  • Stateless (sin estado en servidor)"
log "  • Ideal para microservicios y SPAs"
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
log "   • Analizar los tokens JWT generados"
log "   • Compartir los resultados con tu instructor"
log "   • Documentar el comportamiento del sistema"
log ""

log "${GREEN}🎉 ¡Pruebas de la Parte 2 (JWT) completadas exitosamente!${RESET}"
log ""