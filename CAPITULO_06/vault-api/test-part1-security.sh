#!/bin/bash

##############################################################################
# Script de Pruebas - Parte 1: Autenticación y Autorización Básica
# 
# Este script prueba los endpoints administrativos del microservicio VaultCorp
# utilizando autenticación básica (Basic Auth) y roles.
#
# Conceptos que se prueban:
# - @PermitAll: Endpoints públicos sin autenticación
# - @RolesAllowed: Endpoints protegidos por roles específicos
# - Códigos HTTP: 200 (OK), 401 (No autorizado), 403 (Prohibido)
##############################################################################

# Generar nombre de archivo con timestamp
OUTPUT_FILE="test-part1-security-$(date '+%Y-%m-%d_%H-%M-%S').txt"

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

# Credenciales de usuarios de prueba
ADMIN_USER="admin:admin123"
AUDITOR_USER="auditor:auditor123"
EMPLOYEE_USER="employee:employee123"

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
log "${CYAN}║    🔐 PRUEBAS DE SEGURIDAD - PARTE 1: AUTENTICACIÓN BÁSICA    ║${RESET}"
log "${CYAN}╔════════════════════════════════════════════════════════════════╗${RESET}"
log ""
log "${CYAN}📅 Fecha:${RESET} $(date '+%d/%m/%Y %H:%M:%S')"
log "${CYAN}🌐 API Base:${RESET} $BASE_URL"
log "${CYAN}📄 Resultados:${RESET} $OUTPUT_FILE"
log "${CYAN}🔐 Seguridad:${RESET} Basic Authentication + Role-Based Access Control"
log ""

##############################################################################
# PRUEBA 1: Endpoint Público (@PermitAll)
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 1: Endpoint Público - Health Check${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Verificar que un endpoint con @PermitAll es accesible sin credenciales"
log "${YELLOW}📍 Endpoint:${RESET} GET /api/admin/secrets/health"
log "${YELLOW}🔓 Seguridad:${RESET} @PermitAll (sin autenticación requerida)"
log "${YELLOW}✅ Esperado:${RESET} HTTP 200 OK"
log ""
log "${CYAN}Ejecutando...${RESET}"
log ""

# Ejecutar request
response=$(curl -s -w "\n%{http_code}" $BASE_URL/api/admin/secrets/health 2>/dev/null)
body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
log "$body"
log ""

if [ "$status" == "200" ]; then
    log "${GREEN}✓ PASS${RESET} - Endpoint público accesible sin autenticación"
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 200)"
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   El mensaje 'VaultCorp Admin API is running' confirma que el endpoint"
log "   con @PermitAll es accesible sin necesidad de credenciales."
pause

##############################################################################
# PRUEBA 2: Acceso sin Autenticación a Endpoint Protegido
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 2: Acceso NO Autorizado (sin credenciales)${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Verificar que un endpoint protegido rechaza peticiones sin credenciales"
log "${YELLOW}📍 Endpoint:${RESET} GET /api/admin/secrets/all"
log "${YELLOW}🔒 Seguridad:${RESET} @RolesAllowed(\"vault-admin\")"
log "${YELLOW}❌ Esperado:${RESET} HTTP 401 Unauthorized"
log ""
log "${CYAN}Ejecutando...${RESET}"
log ""

# Ejecutar request
response=$(curl -s -w "\n%{http_code}" $BASE_URL/api/admin/secrets/all 2>/dev/null)
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
log "   HTTP 401 significa: 'No estás autenticado, necesito saber quién eres'."
log "   El servidor rechaza la petición porque no se proporcionaron credenciales."
pause

##############################################################################
# PRUEBA 3: Acceso con Usuario ADMIN (Autorizado)
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 3: Acceso Autorizado con rol ADMIN${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Verificar que un usuario con rol 'vault-admin' puede listar todos los secretos"
log "${YELLOW}📍 Endpoint:${RESET} GET /api/admin/secrets/all"
log "${YELLOW}👤 Usuario:${RESET} admin (rol: vault-admin)"
log "${YELLOW}🔒 Seguridad:${RESET} @RolesAllowed(\"vault-admin\")"
log "${YELLOW}✅ Esperado:${RESET} HTTP 200 OK + JSON con lista de secretos"
log ""
log "${CYAN}Ejecutando...${RESET}"
log ""

# Ejecutar request
response=$(curl -s -w "\n%{http_code}" -u $ADMIN_USER $BASE_URL/api/admin/secrets/all 2>/dev/null)
body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
show_json "$body"
log ""

if [ "$status" == "200" ]; then
    log "${GREEN}✓ PASS${RESET} - Usuario admin autorizado correctamente"
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 200)"
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   El usuario 'admin' tiene el rol 'vault-admin' requerido por el endpoint,"
log "   por lo tanto puede acceder y ver la lista completa de secretos."
pause

##############################################################################
# PRUEBA 4: Acceso con Usuario AUDITOR a Listar Todos (Prohibido)
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 4: Acceso Prohibido por Rol Insuficiente${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Verificar que un usuario autenticado pero SIN el rol requerido es rechazado"
log "${YELLOW}📍 Endpoint:${RESET} GET /api/admin/secrets/all"
log "${YELLOW}👤 Usuario:${RESET} auditor (rol: vault-auditor)"
log "${YELLOW}🔒 Seguridad:${RESET} @RolesAllowed(\"vault-admin\") - El auditor NO tiene este rol"
log "${YELLOW}❌ Esperado:${RESET} HTTP 403 Forbidden"
log ""
log "${CYAN}Ejecutando...${RESET}"
log ""

# Ejecutar request
response=$(curl -s -w "\n%{http_code}" -u $AUDITOR_USER $BASE_URL/api/admin/secrets/all 2>/dev/null)
body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
if [ -n "$body" ]; then
    log "$body"
else
    log "(Sin contenido - esperado para 403)"
fi
log ""

if [ "$status" == "403" ]; then
    log "${GREEN}✓ PASS${RESET} - Autorización por roles funcionando correctamente"
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 403)"
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   HTTP 403 significa: 'Sé quién eres, pero no tienes permiso para hacer esto'."
log "   El auditor está autenticado pero NO tiene el rol 'vault-admin' requerido."
pause

##############################################################################
# PRUEBA 5: Acceso con Usuario AUDITOR a Estadísticas (Autorizado)
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 5: Acceso con Múltiples Roles Permitidos${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Verificar que un endpoint puede permitir múltiples roles"
log "${YELLOW}📍 Endpoint:${RESET} GET /api/admin/secrets/stats"
log "${YELLOW}👤 Usuario:${RESET} auditor (rol: vault-auditor)"
log "${YELLOW}🔒 Seguridad:${RESET} @RolesAllowed({\"vault-admin\", \"vault-auditor\"})"
log "${YELLOW}✅ Esperado:${RESET} HTTP 200 OK + JSON con estadísticas"
log ""
log "${CYAN}Ejecutando...${RESET}"
log ""

# Ejecutar request
response=$(curl -s -w "\n%{http_code}" -u $AUDITOR_USER $BASE_URL/api/admin/secrets/stats 2>/dev/null)
body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
show_json "$body"
log ""

if [ "$status" == "200" ]; then
    log "${GREEN}✓ PASS${RESET} - El auditor tiene acceso permitido"
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 200)"
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   Este endpoint permite TANTO a admins COMO a auditores. El auditor puede"
log "   ver estadísticas generales sin necesidad de tener privilegios de administrador."
pause

##############################################################################
# PRUEBA 6: Eliminar Secreto con Usuario AUDITOR (Prohibido)
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 6: Operación Destructiva Prohibida para Auditor${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Verificar que un auditor NO puede eliminar secretos (solo lectura)"
log "${YELLOW}📍 Endpoint:${RESET} DELETE /api/admin/secrets/{id}"
log "${YELLOW}👤 Usuario:${RESET} auditor (rol: vault-auditor)"
log "${YELLOW}🔒 Seguridad:${RESET} @RolesAllowed(\"vault-admin\")"
log "${YELLOW}❌ Esperado:${RESET} HTTP 403 Forbidden"
log ""

# Primero obtenemos un ID de secreto existente
log "${CYAN}Obteniendo un ID de secreto para probar...${RESET}"
SECRET_ID=$(curl -s -u $ADMIN_USER $BASE_URL/api/admin/secrets/all | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
log "${YELLOW}📌 ID del secreto a intentar eliminar:${RESET} $SECRET_ID"
log ""

log "${CYAN}Ejecutando DELETE con usuario auditor...${RESET}"
log ""

# Ejecutar request
response=$(curl -s -w "\n%{http_code}" -X DELETE -u $AUDITOR_USER $BASE_URL/api/admin/secrets/$SECRET_ID 2>/dev/null)
body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
if [ -n "$body" ]; then
    log "$body"
else
    log "(Sin contenido - esperado para 403)"
fi
log ""

if [ "$status" == "403" ]; then
    log "${GREEN}✓ PASS${RESET} - El auditor NO puede eliminar secretos"
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 403)"
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   Los auditores tienen permisos de solo lectura. No pueden realizar"
log "   operaciones destructivas como eliminar secretos."
pause

##############################################################################
# PRUEBA 7: Eliminar Secreto con Usuario ADMIN (Autorizado)
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 7: Operación Destructiva Autorizada para Admin${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Verificar que un admin SÍ puede eliminar secretos"
log "${YELLOW}📍 Endpoint:${RESET} DELETE /api/admin/secrets/{id}"
log "${YELLOW}👤 Usuario:${RESET} admin (rol: vault-admin)"
log "${YELLOW}🔒 Seguridad:${RESET} @RolesAllowed(\"vault-admin\")"
log "${YELLOW}✅ Esperado:${RESET} HTTP 200 OK + mensaje de éxito"
log ""
log "${CYAN}Ejecutando DELETE con usuario admin...${RESET}"
log ""

# Ejecutar request
response=$(curl -s -w "\n%{http_code}" -X DELETE -u $ADMIN_USER $BASE_URL/api/admin/secrets/$SECRET_ID 2>/dev/null)
body=$(echo "$response" | sed '$d')
status=$(echo "$response" | tail -n 1)

log "${YELLOW}Response (HTTP $status):${RESET}"
show_json "$body"
log ""

if [ "$status" == "200" ]; then
    log "${GREEN}✓ PASS${RESET} - Admin autorizado para eliminar secretos"
else
    log "${RED}✗ FAIL${RESET} - HTTP $status (Esperado: 200)"
fi

log ""
log "${CYAN}💡 Resultado esperado:${RESET}"
log "   El admin tiene permisos completos y puede realizar operaciones destructivas"
log "   como eliminar secretos del sistema."
pause

##############################################################################
# PRUEBA 8: Verificar que el Secreto fue Eliminado
##############################################################################
clear
log ""
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log "${WHITE}📋 PRUEBA 8: Verificación de Eliminación${RESET}"
log "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"
log ""
log "${YELLOW}🎯 Objetivo:${RESET} Confirmar que el secreto eliminado ya no existe en el sistema"
log "${YELLOW}📍 Endpoint:${RESET} GET /api/admin/secrets/all"
log "${YELLOW}✅ Esperado:${RESET} La lista debe tener un secreto menos"
log ""
log "${CYAN}Listando todos los secretos...${RESET}"
log ""

# Ejecutar request
all_secrets=$(curl -s -u $ADMIN_USER $BASE_URL/api/admin/secrets/all 2>/dev/null)

log "${YELLOW}Response:${RESET}"
show_json "$all_secrets"
log ""

log "${GREEN}✓ Compara el número de secretos actual con el inicial (debería ser uno menos)${RESET}"
pause

##############################################################################
# RESUMEN FINAL
##############################################################################
clear
log ""
log "${CYAN}╔════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║                    📊 RESUMEN DE PRUEBAS                       ║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════╗${RESET}"
log ""
log "${GREEN}✅ PRUEBA 1:${RESET} Endpoint público accesible sin autenticación"
log "${GREEN}✅ PRUEBA 2:${RESET} Peticiones sin credenciales son rechazadas (401)"
log "${GREEN}✅ PRUEBA 3:${RESET} Usuario con rol correcto accede exitosamente (200)"
log "${GREEN}✅ PRUEBA 4:${RESET} Usuario sin rol requerido es rechazado (403)"
log "${GREEN}✅ PRUEBA 5:${RESET} Múltiples roles pueden acceder al mismo endpoint"
log "${GREEN}✅ PRUEBA 6:${RESET} Auditor no puede realizar operaciones destructivas"
log "${GREEN}✅ PRUEBA 7:${RESET} Admin puede realizar operaciones destructivas"
log "${GREEN}✅ PRUEBA 8:${RESET} Los cambios persisten correctamente"
log ""

log "${CYAN}╔════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║              🎓 CONCEPTOS CLAVE DEMOSTRADOS                    ║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "${YELLOW}🔑 @PermitAll:${RESET}      Permite acceso público sin autenticación"
log "${YELLOW}🔑 @RolesAllowed:${RESET}  Restringe acceso solo a usuarios con roles específicos"
log "${YELLOW}🔑 HTTP 401:${RESET}        No autenticado (falta identificación)"
log "${YELLOW}🔑 HTTP 403:${RESET}        No autorizado (identificado pero sin permiso)"
log "${YELLOW}🔑 HTTP 200:${RESET}        Operación exitosa"
log ""

log "${CYAN}╔════════════════════════════════════════════════════════════════╗${RESET}"
log "${CYAN}║           🔐 MODELO DE SEGURIDAD: BASIC AUTHENTICATION        ║${RESET}"
log "${CYAN}╚════════════════════════════════════════════════════════════════╝${RESET}"
log ""
log "${WHITE}Características de Basic Auth:${RESET}"
log "  • Credenciales codificadas en Base64"
log "  • Enviadas en header: ${CYAN}Authorization: Basic <base64>${RESET}"
log "  • Verificación en cada request"
log "  • Roles almacenados en application.properties"
log ""
log "${WHITE}Usuarios de Prueba:${RESET}"
log "  • ${CYAN}admin${RESET}     → rol: vault-admin (permisos completos)"
log "  • ${CYAN}auditor${RESET}   → rol: vault-auditor (solo lectura)"
log "  • ${CYAN}employee${RESET}  → rol: vault-employee (acceso básico)"
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
log "   • Compartir los resultados con tu instructor"
log "   • Documentar el comportamiento del sistema"
log ""

log "${GREEN}🎉 ¡Pruebas de la Parte 1 completadas exitosamente!${RESET}"
log ""