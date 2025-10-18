#!/bin/bash

##############################################################################
# Script de Pruebas - Parte 3: Autenticación con OIDC (OpenID Connect)
# 
# Este script prueba los endpoints de clientes externos que utilizan
# Keycloak como proveedor de identidad federado mediante OIDC.
#
# Conceptos que se prueban:
# - Autenticación federada con Identity Provider externo (Keycloak)
# - OpenID Connect (OIDC) flow
# - Tokens emitidos por Keycloak (no por nuestra app)
# - Roles gestionados en Keycloak
# - Autorización basada en roles externos
# - Diferenciación de niveles de acceso (customer vs premium-customer)
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

# URL de Keycloak
KEYCLOAK_URL="http://localhost:8180"
REALM="vaultcorp"
CLIENT_ID="vault-api"

# ⚠️ IMPORTANTE: Configura tu CLIENT_SECRET aquí
CLIENT_SECRET="pnQqtvHgHHLWS1wAlaGsdDwBjKk3AgvO"

# Verificar que se configuró el CLIENT_SECRET
if [ "$CLIENT_SECRET" == "TU-CLIENT-SECRET-AQUI" ]; then
    echo -e "${RED}❌ ERROR: Debes configurar CLIENT_SECRET en el script${NC}"
    echo -e "${YELLOW}Edita el archivo y reemplaza 'TU-CLIENT-SECRET-AQUI' con tu client secret de Keycloak${NC}"
    exit 1
fi

# Variables globales para tokens
TOKEN_CUSTOMER=""
TOKEN_PREMIUM=""

echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║     🔐 PRUEBAS DE SEGURIDAD - PARTE 3: OIDC + KEYCLOAK       ║${NC}"
echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo ""

##############################################################################
# PRUEBA 0: Verificar que Keycloak está corriendo
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 0: Verificar Conectividad con Keycloak${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Confirmar que Keycloak está accesible"
echo -e "📍 URL: $KEYCLOAK_URL"
echo ""
echo -e "${CYAN}Verificando conexión...${NC}"
echo ""

if curl -s -o /dev/null -w "%{http_code}" $KEYCLOAK_URL | grep -q "200"; then
    echo -e "${GREEN}✓ Keycloak está corriendo correctamente${NC}"
else
    echo -e "${RED}❌ ERROR: No se puede conectar a Keycloak en $KEYCLOAK_URL${NC}"
    echo -e "${YELLOW}Asegúrate de que Docker con Keycloak esté corriendo${NC}"
    exit 1
fi

echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 1: Obtener Token desde Keycloak (Cliente Básico)
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 1: Obtener Access Token desde Keycloak (Cliente Básico)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Autenticarse con Keycloak y obtener un Access Token OIDC"
echo -e "📍 Endpoint: POST $KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token"
echo -e "👤 Usuario: client001 (rol: customer)"
echo -e "🔑 Grant Type: password (Resource Owner Password Credentials)"
echo -e "✅ Resultado Esperado: Access Token válido emitido por Keycloak"
echo ""
echo -e "${CYAN}Ejecutando login en Keycloak...${NC}"
echo ""

RESPONSE=$(curl -s -X POST $KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=$CLIENT_ID" \
  -d "client_secret=$CLIENT_SECRET" \
  -d "username=client001" \
  -d "password=pass001")

echo "$RESPONSE" | python3 -m json.tool

# Extraer el access_token
TOKEN_CUSTOMER=$(echo "$RESPONSE" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

echo ""
echo -e "${GREEN}✓ Si ves un 'access_token', ¡Keycloak emitió el token correctamente!${NC}"
echo -e "${CYAN}ℹ️  Este token está firmado por Keycloak, no por nuestra aplicación${NC}"
echo -e "${MAGENTA}📌 Token obtenido (primeros 50 caracteres): ${TOKEN_CUSTOMER:0:50}...${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 2: Acceso sin Token (Debe Fallar)
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 2: Intento de Acceso sin Token OIDC${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Verificar que los endpoints OIDC rechazan peticiones sin token"
echo -e "📍 Endpoint: GET /api/external/secrets/profile"
echo -e "🔒 Seguridad: @RolesAllowed + OIDC"
echo -e "❌ Resultado Esperado: HTTP 401 Unauthorized"
echo ""
echo -e "${CYAN}Ejecutando sin Authorization header...${NC}"
echo ""

curl -i $BASE_URL/api/external/secrets/profile

echo ""
echo -e "${GREEN}✓ Si ves 'HTTP/1.1 401 Unauthorized', ¡el endpoint está protegido!${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 3: Ver Perfil con Token OIDC
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 3: Acceso al Perfil con Token OIDC de Keycloak${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Acceder a un endpoint usando token emitido por Keycloak"
echo -e "📍 Endpoint: GET /api/external/secrets/profile"
echo -e "👤 Usuario: client001 (autenticado vía Keycloak)"
echo -e "🔑 Token: Access Token OIDC"
echo -e "✅ Resultado Esperado: HTTP 200 OK + información del usuario desde Keycloak"
echo ""
echo -e "${CYAN}Ejecutando con Bearer Token de Keycloak...${NC}"
echo ""

curl -i $BASE_URL/api/external/secrets/profile \
  -H "Authorization: Bearer $TOKEN_CUSTOMER"

echo ""
echo -e "${GREEN}✓ Si ves 'HTTP/1.1 200 OK' y authMethod: 'OIDC (Keycloak)', ¡funcionó!${NC}"
echo -e "${CYAN}ℹ️  Quarkus validó el token contra la clave pública de Keycloak${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 4: Listar Secretos Públicos (Cliente Básico)
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 4: Acceso a Secretos Públicos (Cliente Básico)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Verificar que clientes básicos pueden ver secretos PUBLIC"
echo -e "📍 Endpoint: GET /api/external/secrets/public"
echo -e "👤 Usuario: client001 (rol: customer)"
echo -e "🔐 Nivel: PUBLIC"
echo -e "✅ Resultado Esperado: HTTP 200 OK + secretos de nivel PUBLIC"
echo ""
echo -e "${CYAN}Listando secretos públicos...${NC}"
echo ""

curl -s $BASE_URL/api/external/secrets/public \
  -H "Authorization: Bearer $TOKEN_CUSTOMER" | python3 -m json.tool

echo ""
echo -e "${GREEN}✓ El cliente básico puede ver secretos PUBLIC${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 5: Intento de Acceso a Secretos Confidenciales (Debe Fallar)
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 5: Cliente Básico NO puede ver Secretos Confidenciales${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Verificar que clientes básicos NO pueden ver secretos CONFIDENTIAL"
echo -e "📍 Endpoint: GET /api/external/secrets/confidential"
echo -e "👤 Usuario: client001 (rol: customer)"
echo -e "🔒 Seguridad: @RolesAllowed(\"premium-customer\")"
echo -e "❌ Resultado Esperado: HTTP 403 Forbidden"
echo ""
echo -e "${CYAN}Intentando acceder a secretos confidenciales con cliente básico...${NC}"
echo ""

curl -i $BASE_URL/api/external/secrets/confidential \
  -H "Authorization: Bearer $TOKEN_CUSTOMER"

echo ""
echo -e "${GREEN}✓ Si ves 'HTTP/1.1 403 Forbidden', ¡la autorización funciona!${NC}"
echo -e "${CYAN}ℹ️  Los clientes básicos NO tienen acceso a secretos CONFIDENTIAL${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 6: Obtener Token para Cliente Premium
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 6: Obtener Token para Cliente Premium${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Autenticar un cliente con rol premium-customer"
echo -e "👤 Usuario: client002 (rol: premium-customer)"
echo -e "✅ Resultado Esperado: Access Token con rol premium"
echo ""
echo -e "${CYAN}Ejecutando login para cliente premium...${NC}"
echo ""

RESPONSE_PREMIUM=$(curl -s -X POST $KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=$CLIENT_ID" \
  -d "client_secret=$CLIENT_SECRET" \
  -d "username=client002" \
  -d "password=pass002")

echo "$RESPONSE_PREMIUM" | python3 -m json.tool

TOKEN_PREMIUM=$(echo "$RESPONSE_PREMIUM" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

echo ""
echo -e "${GREEN}✓ Token premium obtenido correctamente${NC}"
echo -e "${MAGENTA}📌 Token premium (primeros 50 caracteres): ${TOKEN_PREMIUM:0:50}...${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 7: Acceso a Secretos Confidenciales (Cliente Premium)
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 7: Cliente Premium SÍ puede ver Secretos Confidenciales${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Verificar que clientes premium SÍ pueden ver secretos CONFIDENTIAL"
echo -e "📍 Endpoint: GET /api/external/secrets/confidential"
echo -e "👤 Usuario: client002 (rol: premium-customer)"
echo -e "✅ Resultado Esperado: HTTP 200 OK + secretos CONFIDENTIAL"
echo ""
echo -e "${CYAN}Accediendo a secretos confidenciales con cliente premium...${NC}"
echo ""

curl -s $BASE_URL/api/external/secrets/confidential \
  -H "Authorization: Bearer $TOKEN_PREMIUM" | python3 -m json.tool

echo ""
echo -e "${GREEN}✓ El cliente premium SÍ puede ver secretos CONFIDENTIAL${NC}"
echo -e "${CYAN}ℹ️  El nivel de acceso depende del rol asignado en Keycloak${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 8: Comparación de Roles (Educativa)
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 8: Comparación de Roles entre Cliente Básico y Premium${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Visualizar las diferencias de autorización según el rol"
echo ""
echo -e "${CYAN}───────────────────────────────────────────────────────────────${NC}"
echo -e "${YELLOW}Perfil de client001 (customer):${NC}"
echo -e "${CYAN}───────────────────────────────────────────────────────────────${NC}"
echo ""

curl -s $BASE_URL/api/external/secrets/profile \
  -H "Authorization: Bearer $TOKEN_CUSTOMER" | python3 -m json.tool

echo ""
echo -e "${CYAN}───────────────────────────────────────────────────────────────${NC}"
echo -e "${YELLOW}Perfil de client002 (premium-customer):${NC}"
echo -e "${CYAN}───────────────────────────────────────────────────────────────${NC}"
echo ""

curl -s $BASE_URL/api/external/secrets/profile \
  -H "Authorization: Bearer $TOKEN_PREMIUM" | python3 -m json.tool

echo ""
echo -e "${GREEN}✓ Observa la diferencia en los roles: 'customer' vs 'premium-customer'${NC}"
echo -e "${CYAN}ℹ️  Los roles vienen directamente de Keycloak, no de nuestra aplicación${NC}"
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
echo -e "${GREEN}✅ PRUEBA 0:${NC} Keycloak está accesible y corriendo"
echo -e "${GREEN}✅ PRUEBA 1:${NC} Keycloak emite tokens OIDC válidos"
echo -e "${GREEN}✅ PRUEBA 2:${NC} Peticiones sin token son rechazadas (401)"
echo -e "${GREEN}✅ PRUEBA 3:${NC} Token OIDC permite acceso a endpoints protegidos"
echo -e "${GREEN}✅ PRUEBA 4:${NC} Clientes básicos pueden ver secretos PUBLIC"
echo -e "${GREEN}✅ PRUEBA 5:${NC} Clientes básicos NO pueden ver secretos CONFIDENTIAL (403)"
echo -e "${GREEN}✅ PRUEBA 6:${NC} Clientes premium obtienen tokens con rol premium"
echo -e "${GREEN}✅ PRUEBA 7:${NC} Clientes premium SÍ pueden ver secretos CONFIDENTIAL"
echo -e "${GREEN}✅ PRUEBA 8:${NC} Autorización diferenciada según roles de Keycloak"
echo ""
echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║              🎓 CONCEPTOS CLAVE DEMOSTRADOS                    ║${NC}"
echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo ""
echo -e "${YELLOW}🔑 OIDC (OpenID Connect):${NC} Protocolo de autenticación sobre OAuth2"
echo -e "${YELLOW}🔑 Identity Provider:${NC}    Keycloak gestiona usuarios y roles externamente"
echo -e "${YELLOW}🔑 Federación:${NC}           Autenticación delegada a sistema externo"
echo -e "${YELLOW}🔑 Access Token:${NC}         Token emitido por Keycloak, validado por Quarkus"
echo -e "${YELLOW}🔑 Realm:${NC}                Espacio aislado en Keycloak (vaultcorp)"
echo -e "${YELLOW}🔑 Client:${NC}               Nuestra app registrada en Keycloak (vault-api)"
echo -e "${YELLOW}🔑 Roles externos:${NC}       Roles gestionados en Keycloak, no en nuestra app"
echo ""
echo -e "${MAGENTA}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${MAGENTA}║              🆚 OIDC vs JWT Propio (Parte 2)                   ║${NC}"
echo -e "${MAGENTA}╔════════════════════════════════════════════════════════════════╗${NC}"
echo ""
echo -e "${CYAN}JWT Propio (Parte 2):${NC}"
echo -e "  ✓ Nosotros generamos y firmamos los tokens"
echo -e "  ✓ Nosotros gestionamos usuarios y roles"
echo -e "  ✓ Control total del proceso"
echo -e "  ✗ Debemos mantener base de datos de usuarios"
echo ""
echo -e "${CYAN}OIDC con Keycloak (Parte 3):${NC}"
echo -e "  ✓ Keycloak genera y firma los tokens"
echo -e "  ✓ Keycloak gestiona usuarios y roles"
echo -e "  ✓ SSO (Single Sign-On) entre múltiples apps"
echo -e "  ✓ Federación con otros Identity Providers"
echo -e "  ✗ Dependencia de servicio externo (Keycloak)"
echo ""
echo -e "${GREEN}🎉 ¡Pruebas de la Parte 3 (OIDC) completadas exitosamente!${NC}"
echo ""
echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║                  🎓 COMPARATIVA FINAL                          ║${NC}"
echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo ""
echo -e "${YELLOW}Parte 1 (Basic Auth):${NC}     Admins/Auditores  → /api/admin/*"
echo -e "${YELLOW}Parte 2 (JWT Propio):${NC}     Empleados         → /api/internal/*"
echo -e "${YELLOW}Parte 3 (OIDC):${NC}           Clientes Externos → /api/external/*"
echo ""
echo -e "${GREEN}✨ Has completado las 3 partes del ejercicio de seguridad en Quarkus ✨${NC}"
echo ""
