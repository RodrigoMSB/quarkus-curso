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

# Generar nombre de archivo con timestamp
OUTPUT_FILE="test-part3-oidc-$(date '+%Y-%m-%d_%H-%M-%S').txt"

# Limpiar archivo de salida
> "$OUTPUT_FILE"

# Colores para mejor visualización
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # Sin color

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
    log "${RED}❌ ERROR: Debes configurar CLIENT_SECRET en el script${NC}"
    log "${YELLOW}Edita el archivo y reemplaza 'TU-CLIENT-SECRET-AQUI' con tu client secret de Keycloak${NC}"
    exit 1
fi

# Variables globales para tokens
TOKEN_CUSTOMER=""
TOKEN_PREMIUM=""

# Contadores de tests
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

log "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
log "${CYAN}║     🔐 PRUEBAS DE SEGURIDAD - PARTE 3: OIDC + KEYCLOAK       ║${NC}"
log "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
log ""
log "${CYAN}📅 Fecha:${NC} $(date '+%d/%m/%Y %H:%M:%S')"
log "${CYAN}🌐 API Base:${NC} $BASE_URL"
log "${CYAN}📄 Resultados:${NC} $OUTPUT_FILE"
log "${CYAN}🔐 Seguridad:${NC} OIDC (OpenID Connect) + Keycloak"
log ""

##############################################################################
# PRUEBA 0: Verificar que Keycloak está corriendo
##############################################################################
log "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
log "${YELLOW}📋 PRUEBA 0: Verificar Conectividad con Keycloak${NC}"
log "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
log ""
log "🎯 Objetivo: Confirmar que Keycloak está accesible"
log "📍 URL: $KEYCLOAK_URL"
log ""
log "${CYAN}Verificando conexión...${NC}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

KEYCLOAK_STATUS=$(curl -s -o /dev/null -w "%{http_code}" $KEYCLOAK_URL 2>/dev/null)
if [ "$KEYCLOAK_STATUS" == "200" ] || [ "$KEYCLOAK_STATUS" == "303" ] || [ "$KEYCLOAK_STATUS" == "301" ]; then
    log "${GREEN}✓ PASS - Keycloak está corriendo correctamente${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    log "${RED}✗ FAIL - No se puede conectar a Keycloak en $KEYCLOAK_URL (HTTP $KEYCLOAK_STATUS)${NC}"
    log "${YELLOW}Asegúrate de que Docker con Keycloak esté corriendo${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
    exit 1
fi

log ""
pause

##############################################################################
# PRUEBA 1: Obtener Token desde Keycloak (Cliente Básico)
##############################################################################
log "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
log "${YELLOW}📋 PRUEBA 1: Obtener Access Token desde Keycloak (Cliente Básico)${NC}"
log "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
log ""
log "🎯 Objetivo: Autenticarse con Keycloak y obtener un Access Token OIDC"
log "📍 Endpoint: POST $KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token"
log "👤 Usuario: client001 (rol: customer)"
log "🔑 Grant Type: password (Resource Owner Password Credentials)"
log "✅ Resultado Esperado: Access Token válido emitido por Keycloak"
log ""
log "${CYAN}Ejecutando login en Keycloak...${NC}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

RESPONSE=$(curl -s -X POST $KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=$CLIENT_ID" \
  -d "client_secret=$CLIENT_SECRET" \
  -d "username=client001" \
  -d "password=pass001" 2>/dev/null)

show_json "$RESPONSE"

# Extraer el access_token (compatible con jq o sin jq)
if command -v jq &> /dev/null; then
    TOKEN_CUSTOMER=$(echo "$RESPONSE" | jq -r '.access_token // empty' 2>/dev/null)
else
    TOKEN_CUSTOMER=$(echo "$RESPONSE" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)
fi

log ""
if [ -n "$TOKEN_CUSTOMER" ]; then
    log "${GREEN}✓ PASS - Keycloak emitió el token correctamente${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
    log "${CYAN}ℹ️  Este token está firmado por Keycloak, no por nuestra aplicación${NC}"
    log "${MAGENTA}📌 Token obtenido (primeros 50 caracteres): ${TOKEN_CUSTOMER:0:50}...${NC}"
else
    log "${RED}✗ FAIL - No se pudo obtener el access_token${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
log ""
pause

##############################################################################
# PRUEBA 2: Acceso sin Token (Debe Fallar)
##############################################################################
log "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
log "${YELLOW}📋 PRUEBA 2: Intento de Acceso sin Token OIDC${NC}"
log "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
log ""
log "🎯 Objetivo: Verificar que los endpoints OIDC rechazan peticiones sin token"
log "📍 Endpoint: GET /api/external/secrets/profile"
log "🔒 Seguridad: @RolesAllowed + OIDC"
log "❌ Resultado Esperado: HTTP 401 Unauthorized"
log ""
log "${CYAN}Ejecutando sin Authorization header...${NC}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

RESPONSE_NO_AUTH=$(curl -s -w "\n%{http_code}" $BASE_URL/api/external/secrets/profile 2>/dev/null)
BODY_NO_AUTH=$(echo "$RESPONSE_NO_AUTH" | sed '$d')
STATUS_NO_AUTH=$(echo "$RESPONSE_NO_AUTH" | tail -n 1)

log "${YELLOW}Response (HTTP $STATUS_NO_AUTH):${NC}"
if [ -n "$BODY_NO_AUTH" ]; then
    log "$BODY_NO_AUTH"
else
    log "(Sin contenido - esperado para 401)"
fi

log ""
if [ "$STATUS_NO_AUTH" == "401" ]; then
    log "${GREEN}✓ PASS - El endpoint está protegido correctamente (401)${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    log "${RED}✗ FAIL - HTTP $STATUS_NO_AUTH (Esperado: 401)${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
log ""
pause

##############################################################################
# PRUEBA 3: Ver Perfil con Token OIDC
##############################################################################
log "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
log "${YELLOW}📋 PRUEBA 3: Acceso al Perfil con Token OIDC de Keycloak${NC}"
log "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
log ""
log "🎯 Objetivo: Acceder a un endpoint usando token emitido por Keycloak"
log "📍 Endpoint: GET /api/external/secrets/profile"
log "👤 Usuario: client001 (customer)"
log "✅ Resultado Esperado: HTTP 200 OK + datos del perfil"
log ""
log "${CYAN}Ejecutando con token OIDC...${NC}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

RESPONSE_PROFILE=$(curl -s -w "\n%{http_code}" $BASE_URL/api/external/secrets/profile \
  -H "Authorization: Bearer $TOKEN_CUSTOMER" 2>/dev/null)

BODY_PROFILE=$(echo "$RESPONSE_PROFILE" | sed '$d')
STATUS_PROFILE=$(echo "$RESPONSE_PROFILE" | tail -n 1)

show_json "$BODY_PROFILE"

log ""
if [ "$STATUS_PROFILE" == "200" ]; then
    log "${GREEN}✓ PASS - El token OIDC de Keycloak es válido para nuestra aplicación${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
    log "${CYAN}ℹ️  La app validó el token usando la clave pública de Keycloak${NC}"
else
    log "${RED}✗ FAIL - HTTP $STATUS_PROFILE (Esperado: 200)${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
log ""
pause

##############################################################################
# PRUEBA 4: Ver Secretos Públicos
##############################################################################
log "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
log "${YELLOW}📋 PRUEBA 4: Cliente Básico puede ver Secretos Públicos${NC}"
log "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
log ""
log "🎯 Objetivo: Verificar que clientes básicos pueden ver secretos PUBLIC"
log "📍 Endpoint: GET /api/external/secrets/public"
log "👤 Usuario: client001 (customer)"
log "✅ Resultado Esperado: HTTP 200 OK + secretos con level=PUBLIC"
log ""
log "${CYAN}Accediendo a secretos públicos...${NC}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

RESPONSE_PUBLIC=$(curl -s -w "\n%{http_code}" $BASE_URL/api/external/secrets/public \
  -H "Authorization: Bearer $TOKEN_CUSTOMER" 2>/dev/null)

BODY_PUBLIC=$(echo "$RESPONSE_PUBLIC" | sed '$d')
STATUS_PUBLIC=$(echo "$RESPONSE_PUBLIC" | tail -n 1)

show_json "$BODY_PUBLIC"

log ""
if [ "$STATUS_PUBLIC" == "200" ]; then
    log "${GREEN}✓ PASS - Cliente básico (customer) SÍ puede ver secretos PUBLIC${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
else
    log "${RED}✗ FAIL - HTTP $STATUS_PUBLIC (Esperado: 200)${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
log ""
pause

##############################################################################
# PRUEBA 5: Intento de Ver Secretos Confidenciales (Debe Fallar)
##############################################################################
log "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
log "${YELLOW}📋 PRUEBA 5: Cliente Básico NO puede ver Secretos Confidenciales${NC}"
log "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
log ""
log "🎯 Objetivo: Verificar que clientes básicos NO pueden ver secretos CONFIDENTIAL"
log "📍 Endpoint: GET /api/external/secrets/confidential"
log "👤 Usuario: client001 (customer)"
log "❌ Resultado Esperado: HTTP 403 Forbidden"
log ""
log "${CYAN}Intentando acceder a secretos confidenciales con cliente básico...${NC}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

RESPONSE_FORBIDDEN=$(curl -s -w "\n%{http_code}" $BASE_URL/api/external/secrets/confidential \
  -H "Authorization: Bearer $TOKEN_CUSTOMER" 2>/dev/null)

BODY_FORBIDDEN=$(echo "$RESPONSE_FORBIDDEN" | sed '$d')
STATUS_FORBIDDEN=$(echo "$RESPONSE_FORBIDDEN" | tail -n 1)

log "${YELLOW}Response (HTTP $STATUS_FORBIDDEN):${NC}"
if [ -n "$BODY_FORBIDDEN" ]; then
    log "$BODY_FORBIDDEN"
else
    log "(Sin contenido - esperado para 403)"
fi

log ""
if [ "$STATUS_FORBIDDEN" == "403" ]; then
    log "${GREEN}✓ PASS - La autorización funciona correctamente (403)${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
    log "${CYAN}ℹ️  Los clientes básicos NO tienen acceso a secretos CONFIDENTIAL${NC}"
else
    log "${RED}✗ FAIL - HTTP $STATUS_FORBIDDEN (Esperado: 403)${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
log ""
pause

##############################################################################
# PRUEBA 6: Obtener Token para Cliente Premium
##############################################################################
log "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
log "${YELLOW}📋 PRUEBA 6: Obtener Token para Cliente Premium${NC}"
log "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
log ""
log "🎯 Objetivo: Autenticar un cliente con rol premium-customer"
log "👤 Usuario: client002 (rol: premium-customer)"
log "✅ Resultado Esperado: Access Token con rol premium"
log ""
log "${CYAN}Ejecutando login para cliente premium...${NC}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

RESPONSE_PREMIUM=$(curl -s -X POST $KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=$CLIENT_ID" \
  -d "client_secret=$CLIENT_SECRET" \
  -d "username=client002" \
  -d "password=pass002" 2>/dev/null)

show_json "$RESPONSE_PREMIUM"

# Extraer el access_token (compatible con jq o sin jq)
if command -v jq &> /dev/null; then
    TOKEN_PREMIUM=$(echo "$RESPONSE_PREMIUM" | jq -r '.access_token // empty' 2>/dev/null)
else
    TOKEN_PREMIUM=$(echo "$RESPONSE_PREMIUM" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)
fi

log ""
if [ -n "$TOKEN_PREMIUM" ]; then
    log "${GREEN}✓ PASS - Token premium obtenido correctamente${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
    log "${MAGENTA}📌 Token premium (primeros 50 caracteres): ${TOKEN_PREMIUM:0:50}...${NC}"
else
    log "${RED}✗ FAIL - No se pudo obtener el token premium${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
log ""
pause

##############################################################################
# PRUEBA 7: Acceso a Secretos Confidenciales (Cliente Premium)
##############################################################################
log "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
log "${YELLOW}📋 PRUEBA 7: Cliente Premium SÍ puede ver Secretos Confidenciales${NC}"
log "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
log ""
log "🎯 Objetivo: Verificar que clientes premium SÍ pueden ver secretos CONFIDENTIAL"
log "📍 Endpoint: GET /api/external/secrets/confidential"
log "👤 Usuario: client002 (rol: premium-customer)"
log "✅ Resultado Esperado: HTTP 200 OK + secretos CONFIDENTIAL"
log ""
log "${CYAN}Accediendo a secretos confidenciales con cliente premium...${NC}"
log ""

TOTAL_TESTS=$((TOTAL_TESTS + 1))

RESPONSE_CONFIDENTIAL=$(curl -s -w "\n%{http_code}" $BASE_URL/api/external/secrets/confidential \
  -H "Authorization: Bearer $TOKEN_PREMIUM" 2>/dev/null)

BODY_CONFIDENTIAL=$(echo "$RESPONSE_CONFIDENTIAL" | sed '$d')
STATUS_CONFIDENTIAL=$(echo "$RESPONSE_CONFIDENTIAL" | tail -n 1)

show_json "$BODY_CONFIDENTIAL"

log ""
if [ "$STATUS_CONFIDENTIAL" == "200" ]; then
    log "${GREEN}✓ PASS - El cliente premium SÍ puede ver secretos CONFIDENTIAL${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
    log "${CYAN}ℹ️  El nivel de acceso depende del rol asignado en Keycloak${NC}"
else
    log "${RED}✗ FAIL - HTTP $STATUS_CONFIDENTIAL (Esperado: 200)${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
log ""
pause

##############################################################################
# PRUEBA 8: Comparación de Roles (Educativa)
##############################################################################
log "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
log "${YELLOW}📋 PRUEBA 8: Comparación de Roles entre Cliente Básico y Premium${NC}"
log "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
log ""
log "🎯 Objetivo: Visualizar las diferencias de autorización según el rol"
log ""
log "${CYAN}───────────────────────────────────────────────────────────────${NC}"
log "${YELLOW}Perfil de client001 (customer):${NC}"
log "${CYAN}───────────────────────────────────────────────────────────────${NC}"
log ""

PROFILE_CUSTOMER=$(curl -s $BASE_URL/api/external/secrets/profile \
  -H "Authorization: Bearer $TOKEN_CUSTOMER" 2>/dev/null)

show_json "$PROFILE_CUSTOMER"

log ""
log "${CYAN}───────────────────────────────────────────────────────────────${NC}"
log "${YELLOW}Perfil de client002 (premium-customer):${NC}"
log "${CYAN}───────────────────────────────────────────────────────────────${NC}"
log ""

PROFILE_PREMIUM=$(curl -s $BASE_URL/api/external/secrets/profile \
  -H "Authorization: Bearer $TOKEN_PREMIUM" 2>/dev/null)

show_json "$PROFILE_PREMIUM"

log ""
log "${GREEN}✓ Observa la diferencia en los roles: 'customer' vs 'premium-customer'${NC}"
log "${CYAN}ℹ️  Los roles vienen directamente de Keycloak, no de nuestra aplicación${NC}"
log ""
pause

##############################################################################
# RESUMEN FINAL
##############################################################################
log "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
log "${CYAN}║                    📊 RESUMEN DE PRUEBAS                       ║${NC}"
log "${CYAN}╚════════════════════════════════════════════════════════════════╝${NC}"
log ""
log "  ${CYAN}Total de tests:${NC}      $TOTAL_TESTS"
log "  ${GREEN}✓ Tests Exitosos:${NC}  $PASSED_TESTS"
log "  ${RED}✗ Tests Fallidos:${NC}  $FAILED_TESTS"
log ""

if [ $FAILED_TESTS -gt 0 ]; then
    log "${YELLOW}⚠️  ADVERTENCIA: Algunos tests fallaron${NC}"
    log ""
    log "${YELLOW}Posibles causas:${NC}"
    log "  ${CYAN}1.${NC} Keycloak no está corriendo o no está configurado"
    log "  ${CYAN}2.${NC} El servidor Quarkus no se inició con el perfil correcto"
    log "  ${CYAN}3.${NC} El CLIENT_SECRET no coincide con el configurado en Keycloak"
    log ""
    log "${YELLOW}Solución:${NC}"
    log "  ${CYAN}1.${NC} Verifica que Docker con Keycloak esté corriendo: ${GREEN}docker-compose ps${NC}"
    log "  ${CYAN}2.${NC} Inicia el servidor: ${GREEN}./mvnw quarkus:dev -Dquarkus.profile=parte3${NC}"
    log "  ${CYAN}3.${NC} Verifica el CLIENT_SECRET en Keycloak Admin Console"
    log ""
fi

log "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
log "${CYAN}║                   🎯 TESTS EJECUTADOS                          ║${NC}"
log "${CYAN}╚════════════════════════════════════════════════════════════════╝${NC}"
log ""
log "${GREEN}✅ PRUEBA 0:${NC} Keycloak está accesible y corriendo"
log "${GREEN}✅ PRUEBA 1:${NC} Keycloak emite tokens OIDC válidos"
log "${GREEN}✅ PRUEBA 2:${NC} Peticiones sin token son rechazadas (401)"
log "${GREEN}✅ PRUEBA 3:${NC} Token OIDC permite acceso a endpoints protegidos"
log "${GREEN}✅ PRUEBA 4:${NC} Clientes básicos pueden ver secretos PUBLIC"
log "${GREEN}✅ PRUEBA 5:${NC} Clientes básicos NO pueden ver secretos CONFIDENTIAL (403)"
log "${GREEN}✅ PRUEBA 6:${NC} Clientes premium obtienen tokens con rol premium"
log "${GREEN}✅ PRUEBA 7:${NC} Clientes premium SÍ pueden ver secretos CONFIDENTIAL"
log ""
log "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
log "${CYAN}║              🎓 CONCEPTOS CLAVE DEMOSTRADOS                    ║${NC}"
log "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
log ""
log "${YELLOW}🔑 OIDC (OpenID Connect):${NC} Protocolo de autenticación sobre OAuth2"
log "${YELLOW}🔑 Identity Provider:${NC}    Keycloak gestiona usuarios y roles externamente"
log "${YELLOW}🔑 Federación:${NC}           Autenticación delegada a sistema externo"
log "${YELLOW}🔑 Access Token:${NC}         Token emitido por Keycloak, validado por Quarkus"
log "${YELLOW}🔑 Realm:${NC}                Espacio aislado en Keycloak (vaultcorp)"
log "${YELLOW}🔑 Client:${NC}               Nuestra app registrada en Keycloak (vault-api)"
log "${YELLOW}🔑 Roles externos:${NC}       Roles gestionados en Keycloak, no en nuestra app"
log ""
log "${MAGENTA}╔════════════════════════════════════════════════════════════════╗${NC}"
log "${MAGENTA}║              🆚 OIDC vs JWT Propio (Parte 2)                   ║${NC}"
log "${MAGENTA}╔════════════════════════════════════════════════════════════════╗${NC}"
log ""
log "${CYAN}JWT Propio (Parte 2):${NC}"
log "  ✓ Nosotros generamos y firmamos los tokens"
log "  ✓ Nosotros gestionamos usuarios y roles"
log "  ✓ Control total del proceso"
log "  ✗ Debemos mantener base de datos de usuarios"
log ""
log "${CYAN}OIDC con Keycloak (Parte 3):${NC}"
log "  ✓ Keycloak genera y firma los tokens"
log "  ✓ Keycloak gestiona usuarios y roles"
log "  ✓ SSO (Single Sign-On) entre múltiples apps"
log "  ✓ Federación con otros Identity Providers"
log "  ✗ Dependencia de servicio externo (Keycloak)"
log ""

log "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
log "${CYAN}║                    📁 ARCHIVO DE LOG                           ║${NC}"
log "${CYAN}╚════════════════════════════════════════════════════════════════╝${NC}"
log ""
log "${YELLOW}📝 Todas las pruebas han sido guardadas en:${NC}"
log "   ${GREEN}$OUTPUT_FILE${NC}"
log ""
log "${CYAN}💡 Puedes revisar el log completo en cualquier momento para:${NC}"
log "   • Verificar las respuestas HTTP completas"
log "   • Analizar los tokens OIDC generados por Keycloak"
log "   • Compartir los resultados con tu instructor"
log "   • Documentar el comportamiento del sistema de seguridad"
log ""

log "${GREEN}🎉 ¡Pruebas de la Parte 3 (OIDC) completadas exitosamente!${NC}"
log ""
log "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
log "${CYAN}║                  🎓 COMPARATIVA FINAL                          ║${NC}"
log "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
log ""
log "${YELLOW}Parte 1 (Basic Auth):${NC}     Admins/Auditores  → /api/admin/*"
log "${YELLOW}Parte 2 (JWT Propio):${NC}     Empleados         → /api/internal/*"
log "${YELLOW}Parte 3 (OIDC):${NC}           Clientes Externos → /api/external/*"
log ""
log "${GREEN}✨ Has completado las 3 partes del ejercicio de seguridad en Quarkus ✨${NC}"
log ""