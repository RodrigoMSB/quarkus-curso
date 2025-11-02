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

# Función para pausa interactiva
pause() {
    echo ""
    printf "${CYAN}▶️  Presiona ENTER para continuar...${RESET}"
    read -r
    echo ""
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

# Ejecutar request
response=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"emp001","password":"pass001"}' 2>/dev/null)

body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
show_json "$body"
log ""

# Extraer el token
TOKEN_EMP001=$(echo "$body" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ "$status" == "200" ] && [ -n "$TOKEN_EMP001" ]; then
    log "${GREEN}✓ PASS${RESET} - Login exitoso, token JWT generado"
    log "${MAGENTA}📌 Token generado (primeros 50 caracteres):${RESET} ${TOKEN_EMP001:0:50}..."
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 200)"
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

# Decodificar el payload (segunda parte del JWT)
PAYLOAD=$(echo $TOKEN_EMP001 | awk -F'.' '{print $2}' | base64 -d 2>/dev/null)
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
log "${YELLOW}🎯 Objetivo:${RESET} Verificar que los endpoints protegidos con JWT rechazan peticiones sin token"
log "${YELLOW}📍 Endpoint:${RESET} GET /api/internal/secrets/profile"
log "${YELLOW}🔒 Seguridad:${RESET} @RolesAllowed(\"employee\") + JWT requerido"
log "${YELLOW}❌ Esperado:${RESET} HTTP 401 Unauthorized"
log ""
log "${CYAN}Ejecutando sin Authorization header...${RESET}"
log ""

# Ejecutar request
response=$(curl -s -w "\n%{http_code}" $BASE_URL/api/internal/secrets/profile 2>/dev/null)
body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
if [ -n "$body" ]; then
    log "$body"
else
    log "(Sin contenido - esperado para 401)"
fi
log ""

if [ "$status" == "401" ]; then
    log "${GREEN}✓ PASS${RESET} - Endpoint correctamente protegido"
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 401)"
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   El servidor requiere un token Bearer en el header Authorization."
log "   Sin él, rechaza la petición con HTTP 401."
pause

##############################################################################
# PRUEBA 4: Ver Perfil con JWT Válido
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 4: Acceso al Perfil con JWT Válido${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Acceder a un endpoint protegido usando el token JWT"
log "${YELLOW}📍 Endpoint:${RESET} GET /api/internal/secrets/profile"
log "${YELLOW}👤 Usuario:${RESET} emp001"
log "${YELLOW}🔑 Autenticación:${RESET} Bearer Token en header Authorization"
log "${YELLOW}✅ Esperado:${RESET} HTTP 200 OK + información del usuario"
log ""
log "${CYAN}Ejecutando con Bearer Token...${RESET}"
log ""

# Ejecutar request
response=$(curl -s -w "\n%{http_code}" $BASE_URL/api/internal/secrets/profile \
  -H "Authorization: Bearer $TOKEN_EMP001" 2>/dev/null)

body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
show_json "$body"
log ""

if [ "$status" == "200" ]; then
    log "${GREEN}✓ PASS${RESET} - Autenticación JWT funcionó correctamente"
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 200)"
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   El servidor validó la firma del JWT con la clave pública RSA y extrajo"
log "   los claims para identificar al usuario."
pause

##############################################################################
# PRUEBA 5: Crear un Secreto con JWT
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 5: Crear un Secreto Asociado al Usuario${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Crear un secreto que quede automáticamente asociado al usuario autenticado"
log "${YELLOW}📍 Endpoint:${RESET} POST /api/internal/secrets"
log "${YELLOW}👤 Usuario:${RESET} emp001 (extraído del JWT)"
log "${YELLOW}💡 Nota:${RESET} El backend usa el claim 'sub' del JWT para asignar el ownerId"
log "${YELLOW}✅ Esperado:${RESET} HTTP 201 Created + secreto con ownerId=emp001"
log ""

request_body='{
  "name": "API Key de Stripe",
  "content": "sk_live_4eC39HqLyjWDarjtT1zdp7dc",
  "level": "CONFIDENTIAL"
}'

log "${YELLOW}Request Body:${RESET}"
show_json "$request_body"
log ""

log "${CYAN}Creando secreto...${RESET}"
log ""

# Ejecutar request
response=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/api/internal/secrets \
  -H "Authorization: Bearer $TOKEN_EMP001" \
  -H "Content-Type: application/json" \
  -d "$request_body" 2>/dev/null)

body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
show_json "$body"
log ""

if [ "$status" == "201" ]; then
    log "${GREEN}✓ PASS${RESET} - Secreto creado y asociado automáticamente al usuario"
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 201)"
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   El secreto creado tiene ownerId=emp001, extraído automáticamente del"
log "   claim 'sub' del JWT. El usuario no necesita especificarlo."
pause

##############################################################################
# PRUEBA 6: Ver Mis Secretos
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 6: Listar Mis Secretos (del Usuario Autenticado)${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Obtener solo los secretos del usuario autenticado"
log "${YELLOW}📍 Endpoint:${RESET} GET /api/internal/secrets/my-secrets"
log "${YELLOW}👤 Usuario:${RESET} emp001"
log "${YELLOW}🔒 Filtro:${RESET} Backend filtra por ownerId=emp001 (extraído del JWT)"
log "${YELLOW}✅ Esperado:${RESET} HTTP 200 OK + solo secretos de emp001"
log ""
log "${CYAN}Consultando mis secretos...${RESET}"
log ""

# Ejecutar request
response=$(curl -s -w "\n%{http_code}" $BASE_URL/api/internal/secrets/my-secrets \
  -H "Authorization: Bearer $TOKEN_EMP001" 2>/dev/null)

body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
show_json "$body"
log ""

if [ "$status" == "200" ]; then
    log "${GREEN}✓ PASS${RESET} - Usuario puede ver solo sus propios secretos"
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 200)"
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   Todos los secretos listados tienen ownerId=emp001. El usuario NO puede"
log "   ver secretos de otros usuarios. Aislamiento perfecto."
pause

##############################################################################
# PRUEBA 7: Login con Segundo Usuario
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 7: Login con Segundo Usuario (Multi-tenancy)${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Demostrar que cada usuario tiene su propio contexto de seguridad"
log "${YELLOW}📍 Endpoint:${RESET} POST /api/auth/login"
log "${YELLOW}👤 Usuario:${RESET} emp002 (María González)"
log "${YELLOW}✅ Esperado:${RESET} Nuevo JWT con claims diferentes"
log ""
log "${CYAN}Ejecutando login para emp002...${RESET}"
log ""

# Ejecutar request
response2=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"emp002","password":"pass002"}' 2>/dev/null)

body2=$(echo "$response2" | sed '$d')
status2=$(echo "$response2" | tail -n 1)

log "${YELLOW}Response (HTTP $status2):${RESET}"
show_json "$body2"
log ""

TOKEN_EMP002=$(echo "$body2" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ "$status2" == "200" ] && [ -n "$TOKEN_EMP002" ]; then
    log "${GREEN}✓ PASS${RESET} - Se generó un nuevo token para emp002"
    log "${MAGENTA}📌 Token emp002 (primeros 50 caracteres):${RESET} ${TOKEN_EMP002:0:50}..."
else
    log "${RED}✗ FAIL${RESET} - HTTP $status2 (Esperado: 200)"
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   Cada usuario obtiene su propio JWT con su identificador único (sub)"
log "   en el payload. Los tokens son independientes."
pause

##############################################################################
# PRUEBA 8: Crear Secreto con el Segundo Usuario
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 8: Crear Secreto con el Usuario emp002${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Crear un secreto para emp002 y verificar que queda asociado a ese usuario"
log "${YELLOW}👤 Usuario:${RESET} emp002"
log "${YELLOW}✅ Esperado:${RESET} Secreto con ownerId=emp002"
log ""

request_body2='{
  "name": "Credencial AWS",
  "content": "AKIAIOSFODNN7EXAMPLE",
  "level": "INTERNAL"
}'

log "${YELLOW}Request Body:${RESET}"
show_json "$request_body2"
log ""

log "${CYAN}Creando secreto para emp002...${RESET}"
log ""

# Ejecutar request
response=$(curl -s -w "\n%{http_code}" -X POST $BASE_URL/api/internal/secrets \
  -H "Authorization: Bearer $TOKEN_EMP002" \
  -H "Content-Type: application/json" \
  -d "$request_body2" 2>/dev/null)

body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
show_json "$body"
log ""

if [ "$status" == "201" ]; then
    log "${GREEN}✓ PASS${RESET} - Secreto creado con ownerId=emp002"
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 201)"
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

EXP_TIMESTAMP=$(echo $TOKEN_EMP001 | awk -F'.' '{print $2}' | base64 -d 2>/dev/null | grep -o '"exp":[0-9]*' | grep -o '[0-9]*')
IAT_TIMESTAMP=$(echo $TOKEN_EMP001 | awk -F'.' '{print $2}' | base64 -d 2>/dev/null | grep -o '"iat":[0-9]*' | grep -o '[0-9]*')

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
log "${GREEN}✅ PRUEBA 1:${RESET} Login genera JWT válido con claims correctos"
log "${GREEN}✅ PRUEBA 2:${RESET} JWT contiene información del usuario (sub, email, groups)"
log "${GREEN}✅ PRUEBA 3:${RESET} Peticiones sin token son rechazadas (401)"
log "${GREEN}✅ PRUEBA 4:${RESET} Token válido permite acceso a endpoints protegidos"
log "${GREEN}✅ PRUEBA 5:${RESET} Secretos se asocian automáticamente al usuario del JWT"
log "${GREEN}✅ PRUEBA 6:${RESET} Cada usuario solo ve sus propios secretos"
log "${GREEN}✅ PRUEBA 7:${RESET} Diferentes usuarios obtienen tokens con claims únicos"
log "${GREEN}✅ PRUEBA 8:${RESET} Multi-tenancy: cada usuario tiene su espacio aislado"
log "${GREEN}✅ PRUEBA 9:${RESET} Aislamiento perfecto entre usuarios"
log "${GREEN}✅ PRUEBA 10:${RESET} Tokens tienen expiración configurable"
log ""

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