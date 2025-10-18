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

# Colores para mejor visualización
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # Sin color

# URL base del microservicio
BASE_URL="http://localhost:8080"

# Credenciales de usuarios de prueba
ADMIN_USER="admin:admin123"
AUDITOR_USER="auditor:auditor123"
EMPLOYEE_USER="employee:employee123"

echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║    🔐 PRUEBAS DE SEGURIDAD - PARTE 1: AUTENTICACIÓN BÁSICA    ║${NC}"
echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo ""

##############################################################################
# PRUEBA 1: Endpoint Público (@PermitAll)
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 1: Endpoint Público - Health Check${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Verificar que un endpoint con @PermitAll es accesible sin credenciales"
echo -e "📍 Endpoint: GET /api/admin/secrets/health"
echo -e "🔓 Seguridad: @PermitAll (sin autenticación requerida)"
echo -e "✅ Resultado Esperado: HTTP 200 OK"
echo ""
echo -e "${CYAN}Ejecutando...${NC}"
echo ""

curl -i $BASE_URL/api/admin/secrets/health

echo ""
echo -e "${GREEN}✓ Si ves 'HTTP/1.1 200 OK' y el mensaje 'VaultCorp Admin API is running', ¡la prueba fue exitosa!${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 2: Acceso sin Autenticación a Endpoint Protegido
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 2: Acceso NO Autorizado (sin credenciales)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Verificar que un endpoint protegido rechaza peticiones sin credenciales"
echo -e "📍 Endpoint: GET /api/admin/secrets/all"
echo -e "🔒 Seguridad: @RolesAllowed(\"vault-admin\")"
echo -e "❌ Resultado Esperado: HTTP 401 Unauthorized"
echo ""
echo -e "${CYAN}Ejecutando...${NC}"
echo ""

curl -i $BASE_URL/api/admin/secrets/all

echo ""
echo -e "${GREEN}✓ Si ves 'HTTP/1.1 401 Unauthorized', ¡el endpoint está correctamente protegido!${NC}"
echo -e "${CYAN}ℹ️  401 significa: 'No estás autenticado, necesito saber quién eres'${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 3: Acceso con Usuario ADMIN (Autorizado)
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 3: Acceso Autorizado con rol ADMIN${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Verificar que un usuario con rol 'vault-admin' puede listar todos los secretos"
echo -e "📍 Endpoint: GET /api/admin/secrets/all"
echo -e "👤 Usuario: admin (rol: vault-admin)"
echo -e "🔒 Seguridad: @RolesAllowed(\"vault-admin\")"
echo -e "✅ Resultado Esperado: HTTP 200 OK + JSON con lista de secretos"
echo ""
echo -e "${CYAN}Ejecutando...${NC}"
echo ""

curl -i -u $ADMIN_USER $BASE_URL/api/admin/secrets/all

echo ""
echo -e "${GREEN}✓ Si ves 'HTTP/1.1 200 OK' y un array JSON con secretos, ¡el acceso fue autorizado!${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 4: Acceso con Usuario AUDITOR a Listar Todos (Prohibido)
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 4: Acceso Prohibido por Rol Insuficiente${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Verificar que un usuario autenticado pero SIN el rol requerido es rechazado"
echo -e "📍 Endpoint: GET /api/admin/secrets/all"
echo -e "👤 Usuario: auditor (rol: vault-auditor)"
echo -e "🔒 Seguridad: @RolesAllowed(\"vault-admin\") - El auditor NO tiene este rol"
echo -e "❌ Resultado Esperado: HTTP 403 Forbidden"
echo ""
echo -e "${CYAN}Ejecutando...${NC}"
echo ""

curl -i -u $AUDITOR_USER $BASE_URL/api/admin/secrets/all

echo ""
echo -e "${GREEN}✓ Si ves 'HTTP/1.1 403 Forbidden', ¡la autorización por roles funciona correctamente!${NC}"
echo -e "${CYAN}ℹ️  403 significa: 'Sé quién eres, pero no tienes permiso para hacer esto'${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 5: Acceso con Usuario AUDITOR a Estadísticas (Autorizado)
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 5: Acceso con Múltiples Roles Permitidos${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Verificar que un endpoint puede permitir múltiples roles"
echo -e "📍 Endpoint: GET /api/admin/secrets/stats"
echo -e "👤 Usuario: auditor (rol: vault-auditor)"
echo -e "🔒 Seguridad: @RolesAllowed({\"vault-admin\", \"vault-auditor\"})"
echo -e "✅ Resultado Esperado: HTTP 200 OK + JSON con estadísticas"
echo ""
echo -e "${CYAN}Ejecutando...${NC}"
echo ""

curl -i -u $AUDITOR_USER $BASE_URL/api/admin/secrets/stats

echo ""
echo -e "${GREEN}✓ Si ves 'HTTP/1.1 200 OK' y JSON con 'totalSecrets', ¡el auditor tiene acceso!${NC}"
echo -e "${CYAN}ℹ️  Este endpoint permite TANTO a admins COMO a auditores${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 6: Eliminar Secreto con Usuario AUDITOR (Prohibido)
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 6: Operación Destructiva Prohibida para Auditor${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Verificar que un auditor NO puede eliminar secretos (solo lectura)"
echo -e "📍 Endpoint: DELETE /api/admin/secrets/{id}"
echo -e "👤 Usuario: auditor (rol: vault-auditor)"
echo -e "🔒 Seguridad: @RolesAllowed(\"vault-admin\")"
echo -e "❌ Resultado Esperado: HTTP 403 Forbidden"
echo ""

# Primero obtenemos un ID de secreto existente
echo -e "${CYAN}Obteniendo un ID de secreto para probar...${NC}"
SECRET_ID=$(curl -s -u $ADMIN_USER $BASE_URL/api/admin/secrets/all | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo -e "📌 ID del secreto a intentar eliminar: ${YELLOW}$SECRET_ID${NC}"
echo ""

echo -e "${CYAN}Ejecutando DELETE con usuario auditor...${NC}"
echo ""

curl -i -X DELETE -u $AUDITOR_USER $BASE_URL/api/admin/secrets/$SECRET_ID

echo ""
echo -e "${GREEN}✓ Si ves 'HTTP/1.1 403 Forbidden', ¡el auditor NO puede eliminar secretos!${NC}"
echo -e "${CYAN}ℹ️  Los auditores solo tienen permisos de lectura, no de escritura/eliminación${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 7: Eliminar Secreto con Usuario ADMIN (Autorizado)
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 7: Operación Destructiva Autorizada para Admin${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Verificar que un admin SÍ puede eliminar secretos"
echo -e "📍 Endpoint: DELETE /api/admin/secrets/{id}"
echo -e "👤 Usuario: admin (rol: vault-admin)"
echo -e "🔒 Seguridad: @RolesAllowed(\"vault-admin\")"
echo -e "✅ Resultado Esperado: HTTP 200 OK + mensaje de éxito"
echo ""
echo -e "${CYAN}Ejecutando DELETE con usuario admin...${NC}"
echo ""

curl -i -X DELETE -u $ADMIN_USER $BASE_URL/api/admin/secrets/$SECRET_ID

echo ""
echo -e "${GREEN}✓ Si ves 'HTTP/1.1 200 OK' y mensaje 'Secret deleted successfully', ¡funcionó!${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 8: Verificar que el Secreto fue Eliminado
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 8: Verificación de Eliminación${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Confirmar que el secreto eliminado ya no existe en el sistema"
echo -e "📍 Endpoint: GET /api/admin/secrets/all"
echo -e "✅ Resultado Esperado: La lista debe tener un secreto menos"
echo ""
echo -e "${CYAN}Listando todos los secretos...${NC}"
echo ""

curl -s -u $ADMIN_USER $BASE_URL/api/admin/secrets/all | python3 -m json.tool

echo ""
echo -e "${GREEN}✓ Compara el número de secretos actual con el inicial (debería ser uno menos)${NC}"
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
echo -e "${GREEN}✅ PRUEBA 1:${NC} Endpoint público accesible sin autenticación"
echo -e "${GREEN}✅ PRUEBA 2:${NC} Peticiones sin credenciales son rechazadas (401)"
echo -e "${GREEN}✅ PRUEBA 3:${NC} Usuario con rol correcto accede exitosamente (200)"
echo -e "${GREEN}✅ PRUEBA 4:${NC} Usuario sin rol requerido es rechazado (403)"
echo -e "${GREEN}✅ PRUEBA 5:${NC} Múltiples roles pueden acceder al mismo endpoint"
echo -e "${GREEN}✅ PRUEBA 6:${NC} Auditor no puede realizar operaciones destructivas"
echo -e "${GREEN}✅ PRUEBA 7:${NC} Admin puede realizar operaciones destructivas"
echo -e "${GREEN}✅ PRUEBA 8:${NC} Los cambios persisten correctamente"
echo ""
echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║              🎓 CONCEPTOS CLAVE DEMOSTRADOS                    ║${NC}"
echo -e "${CYAN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo ""
echo -e "${YELLOW}🔑 @PermitAll:${NC}      Permite acceso público sin autenticación"
echo -e "${YELLOW}🔑 @RolesAllowed:${NC}  Restringe acceso solo a usuarios con roles específicos"
echo -e "${YELLOW}🔑 HTTP 401:${NC}        No autenticado (falta identificación)"
echo -e "${YELLOW}🔑 HTTP 403:${NC}        No autorizado (identificado pero sin permiso)"
echo -e "${YELLOW}🔑 HTTP 200:${NC}        Operación exitosa"
echo ""
echo -e "${GREEN}🎉 ¡Pruebas de la Parte 1 completadas!${NC}"
echo ""
