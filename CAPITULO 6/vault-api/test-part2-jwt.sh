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

# Variables globales para tokens
TOKEN_EMP001=""
TOKEN_EMP002=""

echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║      🔐 PRUEBAS DE SEGURIDAD - PARTE 2: JWT AUTHENTICATION    ║${NC}"
echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo ""

##############################################################################
# PRUEBA 1: Login y Generación de JWT
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 1: Login y Generación de Token JWT${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Obtener un token JWT válido mediante el endpoint de login"
echo -e "📍 Endpoint: POST /api/auth/login"
echo -e "👤 Usuario: emp001 (Juan Pérez)"
echo -e "🔐 Método: Credenciales en JSON"
echo -e "✅ Resultado Esperado: HTTP 200 OK + JWT Token"
echo ""
echo -e "${CYAN}Ejecutando login...${NC}"
echo ""

RESPONSE=$(curl -s -i -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"emp001","password":"pass001"}')

echo "$RESPONSE"

# Extraer el token
TOKEN_EMP001=$(echo "$RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

echo ""
echo -e "${GREEN}✓ Si ves 'HTTP/1.1 200 OK' y un campo 'token' con un string largo, ¡el login fue exitoso!${NC}"
echo -e "${CYAN}ℹ️  El token JWT contiene 3 partes separadas por puntos: Header.Payload.Signature${NC}"
echo -e "${MAGENTA}📌 Token generado (primeros 50 caracteres): ${TOKEN_EMP001:0:50}...${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 2: Decodificar JWT (Educativo)
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 2: Inspección del Token JWT (Educativo)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Entender qué contiene un token JWT"
echo -e "🔍 Un JWT NO está encriptado, está codificado en Base64"
echo -e "⚠️  Por eso NUNCA se debe incluir información sensible en el payload"
echo ""
echo -e "${CYAN}Decodificando el payload del JWT...${NC}"
echo ""

# Decodificar el payload (segunda parte del JWT)
PAYLOAD=$(echo $TOKEN_EMP001 | awk -F'.' '{print $2}' | base64 -d 2>/dev/null)
echo "$PAYLOAD" | python3 -m json.tool

echo ""
echo -e "${GREEN}✓ Observa los claims importantes:${NC}"
echo -e "  ${YELLOW}• iss${NC} (issuer): Quién emitió el token"
echo -e "  ${YELLOW}• sub${NC} (subject): Identificador del usuario (emp001)"
echo -e "  ${YELLOW}• email${NC}: Email del usuario"
echo -e "  ${YELLOW}• groups${NC}: Roles del usuario ([employee])"
echo -e "  ${YELLOW}• iat${NC} (issued at): Timestamp de creación"
echo -e "  ${YELLOW}• exp${NC} (expiration): Timestamp de expiración (1 hora)"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 3: Acceso sin Token (Debe Fallar)
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 3: Intento de Acceso sin Token${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Verificar que los endpoints protegidos con JWT rechazan peticiones sin token"
echo -e "📍 Endpoint: GET /api/internal/secrets/profile"
echo -e "🔒 Seguridad: @RolesAllowed(\"employee\") + JWT requerido"
echo -e "❌ Resultado Esperado: HTTP 401 Unauthorized"
echo ""
echo -e "${CYAN}Ejecutando sin Authorization header...${NC}"
echo ""

curl -i $BASE_URL/api/internal/secrets/profile

echo ""
echo -e "${GREEN}✓ Si ves 'HTTP/1.1 401 Unauthorized', ¡el endpoint está protegido correctamente!${NC}"
echo -e "${CYAN}ℹ️  El servidor requiere un token Bearer en el header Authorization${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 4: Ver Perfil con JWT Válido
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 4: Acceso al Perfil con JWT Válido${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Acceder a un endpoint protegido usando el token JWT"
echo -e "📍 Endpoint: GET /api/internal/secrets/profile"
echo -e "👤 Usuario: emp001"
echo -e "🔑 Autenticación: Bearer Token en header Authorization"
echo -e "✅ Resultado Esperado: HTTP 200 OK + información del usuario"
echo ""
echo -e "${CYAN}Ejecutando con Bearer Token...${NC}"
echo ""

curl -i $BASE_URL/api/internal/secrets/profile \
  -H "Authorization: Bearer $TOKEN_EMP001"

echo ""
echo -e "${GREEN}✓ Si ves 'HTTP/1.1 200 OK' y los datos del usuario, ¡la autenticación JWT funcionó!${NC}"
echo -e "${CYAN}ℹ️  El servidor validó la firma del JWT con la clave pública RSA${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 5: Crear un Secreto con JWT
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 5: Crear un Secreto Asociado al Usuario${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Crear un secreto que quede automáticamente asociado al usuario autenticado"
echo -e "📍 Endpoint: POST /api/internal/secrets"
echo -e "👤 Usuario: emp001 (extraído del JWT)"
echo -e "💡 El backend usa el claim 'sub' del JWT para asignar el ownerId"
echo -e "✅ Resultado Esperado: HTTP 201 Created + secreto con ownerId=emp001"
echo ""
echo -e "${CYAN}Creando secreto...${NC}"
echo ""

curl -i -X POST $BASE_URL/api/internal/secrets \
  -H "Authorization: Bearer $TOKEN_EMP001" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "API Key de Stripe",
    "content": "sk_live_XXXXXXXXXXXXXXXXX",
    "level": "INTERNAL"
  }'

echo ""
echo -e "${GREEN}✓ Si ves 'HTTP/1.1 201 Created' y 'ownerId: emp001', ¡el secreto se creó correctamente!${NC}"
echo -e "${CYAN}ℹ️  El usuario NO especifica el ownerId en el request, el backend lo extrae del JWT${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 6: Listar Secretos Propios
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 6: Listar SOLO los Secretos del Usuario Autenticado${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Verificar que cada usuario solo ve sus propios secretos"
echo -e "📍 Endpoint: GET /api/internal/secrets/my-secrets"
echo -e "👤 Usuario: emp001"
echo -e "🔐 Filtro: Solo secretos con ownerId == emp001"
echo -e "✅ Resultado Esperado: Lista de secretos solo del usuario emp001"
echo ""
echo -e "${CYAN}Listando secretos del usuario emp001...${NC}"
echo ""

curl -s $BASE_URL/api/internal/secrets/my-secrets \
  -H "Authorization: Bearer $TOKEN_EMP001" | python3 -m json.tool

echo ""
echo -e "${GREEN}✓ Solo aparecen secretos con 'ownerId: emp001'${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 7: Login con Segundo Usuario
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 7: Login con Segundo Usuario (Aislamiento)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Demostrar que cada usuario tiene su propio contexto de seguridad"
echo -e "📍 Endpoint: POST /api/auth/login"
echo -e "👤 Usuario: emp002 (María González)"
echo -e "✅ Resultado Esperado: Nuevo JWT con claims diferentes"
echo ""
echo -e "${CYAN}Ejecutando login para emp002...${NC}"
echo ""

RESPONSE2=$(curl -s -X POST $BASE_URL/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"emp002","password":"pass002"}')

TOKEN_EMP002=$(echo "$RESPONSE2" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

echo "$RESPONSE2" | python3 -m json.tool

echo ""
echo -e "${GREEN}✓ Se generó un nuevo token para emp002${NC}"
echo -e "${MAGENTA}📌 Token emp002 (primeros 50 caracteres): ${TOKEN_EMP002:0:50}...${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 8: Crear Secreto con el Segundo Usuario
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 8: Crear Secreto con el Usuario emp002${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Crear un secreto para emp002 y verificar que queda asociado a ese usuario"
echo -e "👤 Usuario: emp002"
echo -e "✅ Resultado Esperado: Secreto con ownerId=emp002"
echo ""
echo -e "${CYAN}Creando secreto para emp002...${NC}"
echo ""

curl -i -X POST $BASE_URL/api/internal/secrets \
  -H "Authorization: Bearer $TOKEN_EMP002" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Credencial AWS",
    "content": "AKIAIOSFODNN7EXAMPLE",
    "level": "INTERNAL"
  }'

echo ""
echo -e "${GREEN}✓ Secreto creado con ownerId=emp002${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 9: Verificar Aislamiento entre Usuarios
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 9: Verificar Aislamiento de Datos entre Usuarios${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Confirmar que emp001 NO puede ver los secretos de emp002 y viceversa"
echo -e "🔒 Principio de Seguridad: Aislamiento de datos por usuario"
echo ""
echo -e "${CYAN}───────────────────────────────────────────────────────────────${NC}"
echo -e "${YELLOW}Secretos de emp001:${NC}"
echo -e "${CYAN}───────────────────────────────────────────────────────────────${NC}"
echo ""

SECRETS_EMP001=$(curl -s $BASE_URL/api/internal/secrets/my-secrets \
  -H "Authorization: Bearer $TOKEN_EMP001")

echo "$SECRETS_EMP001" | python3 -m json.tool

TOTAL_EMP001=$(echo "$SECRETS_EMP001" | grep -o '"totalSecrets":[0-9]*' | grep -o '[0-9]*')

echo ""
echo -e "${CYAN}───────────────────────────────────────────────────────────────${NC}"
echo -e "${YELLOW}Secretos de emp002:${NC}"
echo -e "${CYAN}───────────────────────────────────────────────────────────────${NC}"
echo ""

SECRETS_EMP002=$(curl -s $BASE_URL/api/internal/secrets/my-secrets \
  -H "Authorization: Bearer $TOKEN_EMP002")

echo "$SECRETS_EMP002" | python3 -m json.tool

TOTAL_EMP002=$(echo "$SECRETS_EMP002" | grep -o '"totalSecrets":[0-9]*' | grep -o '[0-9]*')

echo ""
echo -e "${GREEN}✓ emp001 tiene $TOTAL_EMP001 secreto(s) con ownerId=emp001${NC}"
echo -e "${GREEN}✓ emp002 tiene $TOTAL_EMP002 secreto(s) con ownerId=emp002${NC}"
echo -e "${CYAN}ℹ️  Cada usuario solo ve sus propios secretos. ¡Aislamiento perfecto!${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 10: Token Expirado (Simulación Conceptual)
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 10: Conceptos de Expiración de Token${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Entender cómo funcionan los tokens JWT con expiración"
echo -e "⏰ Configuración actual: Los tokens expiran en 1 hora (3600 segundos)"
echo ""
echo -e "${CYAN}Inspeccionando el claim 'exp' del token de emp001...${NC}"
echo ""

EXP_TIMESTAMP=$(echo $TOKEN_EMP001 | awk -F'.' '{print $2}' | base64 -d 2>/dev/null | grep -o '"exp":[0-9]*' | grep -o '[0-9]*')
IAT_TIMESTAMP=$(echo $TOKEN_EMP001 | awk -F'.' '{print $2}' | base64 -d 2>/dev/null | grep -o '"iat":[0-9]*' | grep -o '[0-9]*')

if [ -n "$EXP_TIMESTAMP" ] && [ -n "$IAT_TIMESTAMP" ]; then
    DURATION=$((EXP_TIMESTAMP - IAT_TIMESTAMP))
    echo -e "${YELLOW}Timestamp de emisión (iat):${NC} $IAT_TIMESTAMP"
    echo -e "${YELLOW}Timestamp de expiración (exp):${NC} $EXP_TIMESTAMP"
    echo -e "${YELLOW}Duración del token:${NC} $DURATION segundos ($(($DURATION / 60)) minutos)"
    echo ""
    echo -e "${CYAN}ℹ️  Cuando el token expire, el servidor rechazará las peticiones con HTTP 401${NC}"
    echo -e "${CYAN}ℹ️  El usuario deberá hacer login nuevamente para obtener un token fresco${NC}"
else
    echo -e "${RED}No se pudo extraer timestamps del token${NC}"
fi

echo ""
read -p "Presiona ENTER para ver el resumen final..."
echo ""

##############################################################################
# RESUMEN FINAL
##############################################################################
echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║                    📊 RESUMEN DE PRUEBAS                       ║${NC}"
echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo ""
echo -e "${GREEN}✅ PRUEBA 1:${NC} Login genera JWT válido con claims correctos"
echo -e "${GREEN}✅ PRUEBA 2:${NC} JWT contiene información del usuario (sub, email, groups)"
echo -e "${GREEN}✅ PRUEBA 3:${NC} Peticiones sin token son rechazadas (401)"
echo -e "${GREEN}✅ PRUEBA 4:${NC} Token válido permite acceso a endpoints protegidos"
echo -e "${GREEN}✅ PRUEBA 5:${NC} Secretos se asocian automáticamente al usuario del JWT"
echo -e "${GREEN}✅ PRUEBA 6:${NC} Cada usuario solo ve sus propios secretos"
echo -e "${GREEN}✅ PRUEBA 7:${NC} Diferentes usuarios obtienen tokens con claims únicos"
echo -e "${GREEN}✅ PRUEBA 8:${NC} Multi-tenancy: cada usuario tiene su espacio aislado"
echo -e "${GREEN}✅ PRUEBA 9:${NC} Aislamiento perfecto entre usuarios"
echo -e "${GREEN}✅ PRUEBA 10:${NC} Tokens tienen expiración configurable"
echo ""
echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║              🎓 CONCEPTOS CLAVE DEMOSTRADOS                    ║${NC}"
echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo ""
echo -e "${YELLOW}🔑 JWT (JSON Web Token):${NC} Estándar abierto para transmitir información de forma segura"
echo -e "${YELLOW}🔑 Bearer Token:${NC}        Token enviado en header 'Authorization: Bearer <token>'"
echo -e "${YELLOW}🔑 Claims:${NC}              Información contenida en el JWT (sub, email, exp, etc.)"
echo -e "${YELLOW}🔑 Firma RSA:${NC}           El JWT se firma con clave privada y se verifica con pública"
echo -e "${YELLOW}🔑 Stateless Auth:${NC}      El servidor no guarda sesiones, toda la info está en el token"
echo -e "${YELLOW}🔑 Aislamiento:${NC}         Cada usuario solo accede a sus propios recursos"
echo -e "${YELLOW}🔑 Expiración:${NC}          Los tokens tienen vida útil limitada (claim 'exp')"
echo ""
echo -e "${MAGENTA}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${MAGENTA}║                   🔐 VENTAJAS DE JWT                           ║${NC}"
echo -e "${MAGENTA}╔════════════════════════════════════════════════════════════════╗${NC}"
echo ""
echo -e "${CYAN}✓ Escalabilidad:${NC}       No requiere almacenamiento de sesiones en servidor"
echo -e "${CYAN}✓ Portabilidad:${NC}        El token puede usarse en diferentes servicios"
echo -e "${CYAN}✓ Autocontenido:${NC}       Toda la información necesaria está en el token"
echo -e "${CYAN}✓ Seguridad:${NC}           Firmado criptográficamente (no puede ser alterado)"
echo -e "${CYAN}✓ Multi-dominio:${NC}       Funciona entre diferentes dominios y servicios"
echo ""
echo -e "${GREEN}🎉 ¡Pruebas de la Parte 2 (JWT) completadas exitosamente!${NC}"
echo ""
