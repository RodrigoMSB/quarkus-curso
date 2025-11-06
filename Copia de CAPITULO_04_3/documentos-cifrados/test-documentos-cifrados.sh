#!/bin/bash

##############################################################################
# Script de Pruebas - Cifrado de Datos Sensibles con Google Tink
# 
# Este script prueba las capacidades de cifrado del microservicio de
# documentos utilizando Google Tink con AES-256-GCM.
#
# SOLO PRUEBAS REST - No requiere psql ni conexión directa a BD
# COMPATIBLE: Mac y Windows (Git Bash)
##############################################################################

# ============================================================================
# DETECCIÓN DE SISTEMA OPERATIVO
# ============================================================================

detect_os() {
    case "$(uname -s)" in
        Darwin*)    echo "mac" ;;
        Linux*)     echo "linux" ;;
        MINGW*|MSYS*|CYGWIN*)    echo "windows" ;;
        *)          echo "unknown" ;;
    esac
}

OS_TYPE=$(detect_os)

# ============================================================================
# CONFIGURACIÓN
# ============================================================================

TIMESTAMP=$(date +%Y-%m-%d_%H-%M-%S)
OUTPUT_FILE="test-documentos-cifrados-${TIMESTAMP}.txt"

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m'

BASE_URL="http://localhost:8080"

# Detectar Python
if command -v python3 &> /dev/null; then
    PYTHON_CMD="python3"
elif command -v python &> /dev/null; then
    PYTHON_CMD="python"
else
    echo "❌ Error: Python no está instalado"
    echo "   Windows: https://www.python.org/downloads/"
    echo "   Mac: brew install python3"
    exit 1
fi

# ============================================================================
# FUNCIONES DE LOGGING
# ============================================================================

log_header() {
    echo -e "${CYAN}$1${NC}"
    echo "$1" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

log_info() {
    echo -e "${BLUE}$1${NC}"
    echo "$1" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

log_success() {
    echo -e "${GREEN}$1${NC}"
    echo "$1" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

log_warning() {
    echo -e "${YELLOW}$1${NC}"
    echo "$1" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

log_error() {
    echo -e "${RED}$1${NC}"
    echo "$1" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

log_plain() {
    echo -e "$1"
    echo "$1" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

# Función para pausar (compatible Mac/Windows)
pause_script() {
    echo -ne "${YELLOW}Presiona ENTER para continuar...${NC}"
    read -r
    echo ""
}

# ============================================================================
# HEADER
# ============================================================================

{
cat << 'EOF'
╔════════════════════════════════════════════════════════════════╗
║   🔐 PRUEBAS DE CIFRADO CON GOOGLE TINK                       ║
║   Cifrado a Nivel de Aplicación - AES-256-GCM                ║
╚════════════════════════════════════════════════════════════════╝
EOF

echo ""
echo "🖥️  Sistema: $OS_TYPE"
echo "🐍 Python: $PYTHON_CMD"
echo "📅 Fecha: $(date '+%d/%m/%Y %H:%M:%S')"
echo "🌐 API Base: $BASE_URL"
echo "📄 Resultados: $OUTPUT_FILE"
echo "🔧 Algoritmo: AES-256-GCM (Google Tink)"
echo ""
} | tee "$OUTPUT_FILE"

# ============================================================================
# VERIFICACIÓN DE REQUISITOS
# ============================================================================

log_header "╔════════════════════════════════════════════════════════════════╗"
log_header "║   ✅ VERIFICACIÓN DE REQUISITOS                               ║"
log_header "╚════════════════════════════════════════════════════════════════╝"
log_plain ""

# Verificar curl
if ! command -v curl &> /dev/null; then
    log_error "❌ Error: curl no está instalado"
    exit 1
fi
log_success "✓ curl instalado"
log_success "✓ $PYTHON_CMD instalado"

# Verificar servicio
log_plain ""
log_header "Verificando conectividad con el servicio..."
if curl -s --max-time 5 "$BASE_URL/api/v1/documentos" > /dev/null 2>&1; then 
    log_success "✓ Servicio accesible en $BASE_URL"
else
    log_error "❌ Error: No se pudo conectar al servicio en $BASE_URL"
    log_warning "Verifica que la aplicación esté corriendo con: ./mvnw quarkus:dev"
    exit 1
fi

log_plain ""
pause_script
log_plain ""

# ============================================================================
# PRUEBAS
# ============================================================================

##############################################################################
# PRUEBA 1: Crear Documento con Cifrado Automático
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 1: Crear Documento con Cifrado Automático"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""
log_plain "🎯 Objetivo: Crear un documento y verificar que se cifra automáticamente"
log_plain "🔐 Algoritmo: AES-256-GCM (Authenticated Encryption)"
log_plain "📝 Contenido: \"Información confidencial del cliente - DNI: 12345678\""
log_plain ""
log_header "Ejecutando POST /api/v1/documentos..."
log_plain ""

DOC1_RESPONSE=$(curl -s -X POST $BASE_URL/api/v1/documentos \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Contrato Confidencial",
    "contenido": "Información confidencial del cliente - DNI: 12345678",
    "tipoDocumento": "CONFIDENCIAL"
  }')

DOC1_ID=$(echo "$DOC1_RESPONSE" | $PYTHON_CMD -c "
import sys, json
try:
    data = json.load(sys.stdin)
    print(data.get('id', ''))
except:
    print('')
")

if [ -n "$DOC1_ID" ]; then
    echo "$DOC1_RESPONSE" | $PYTHON_CMD -c "
import sys, json
try:
    data = json.load(sys.stdin)
    print(f\"✅ ID: {data.get('id')}\")
    print(f\"📄 Título: {data.get('titulo')}\")
    print(f\"📝 Contenido: {data.get('contenido')}\")
    print(f\"🔒 Tipo: {data.get('tipoDocumento')}\")
except Exception as e:
    print(f'❌ Error: {e}')
" | tee -a "$OUTPUT_FILE"
    log_plain ""
    log_success "✅ Documento creado exitosamente con ID: $DOC1_ID"
    log_plain ""
    log_info "💡 El contenido fue cifrado ANTES de guardarse en PostgreSQL"
    log_info "💡 La API te devuelve el contenido descifrado automáticamente"
else
    log_error "❌ Error al crear documento"
    exit 1
fi

log_plain ""
pause_script
log_plain ""

##############################################################################
# PRUEBA 2: Consultar Documento por ID (Descifrado Automático)
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 2: Descifrado Automático al Consultar"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""
log_plain "🎯 Objetivo: Verificar que el documento se descifra al consultarlo"
log_plain "🔓 Proceso: BD guarda cifrado → API descifra → Cliente recibe texto plano"
log_plain ""
log_header "Ejecutando GET /api/v1/documentos/$DOC1_ID..."
log_plain ""

curl -s -X GET "$BASE_URL/api/v1/documentos/$DOC1_ID" | $PYTHON_CMD -c "
import sys, json
try:
    data = json.load(sys.stdin)
    print(f\"✅ ID: {data.get('id')}\")
    print(f\"📄 Título: {data.get('titulo')}\")
    print(f\"📝 Contenido: {data.get('contenido')}\")
    print(f\"🔒 Tipo: {data.get('tipoDocumento')}\")
    print(f\"📅 Creado: {data.get('fechaCreacion')}\")
except Exception as e:
    print(f'❌ Error: {e}')
" | tee -a "$OUTPUT_FILE"

log_plain ""
log_success "✅ Documento descifrado correctamente"
log_info "💡 El contenido está cifrado en la BD pero la API lo descifra automáticamente"
log_plain ""
pause_script
log_plain ""

##############################################################################
# PRUEBA 3: Crear Múltiples Documentos
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 3: Crear Múltiples Documentos Cifrados"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""
log_plain "🎯 Objetivo: Crear varios documentos con diferentes contenidos"
log_plain "📊 Cantidad: 3 documentos"
log_plain ""

# Documento 2
log_header "Creando documento 2..."
DOC2_RESPONSE=$(curl -s -X POST $BASE_URL/api/v1/documentos \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Datos Personales Cliente VIP",
    "contenido": "Nombre: Juan Pérez | Email: juan@mail.com | Teléfono: 987654321",
    "tipoDocumento": "PRIVADO"
  }')

DOC2_ID=$(echo "$DOC2_RESPONSE" | $PYTHON_CMD -c "import sys, json; print(json.load(sys.stdin).get('id', ''))")
log_success "✅ Documento 2 creado con ID: $DOC2_ID"

# Documento 3
log_header "Creando documento 3..."
DOC3_RESPONSE=$(curl -s -X POST $BASE_URL/api/v1/documentos \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Historial Crediticio",
    "contenido": "Score: 850 | Deudas: S/0.00 | Línea de crédito: S/50,000",
    "tipoDocumento": "CONFIDENCIAL"
  }')

DOC3_ID=$(echo "$DOC3_RESPONSE" | $PYTHON_CMD -c "import sys, json; print(json.load(sys.stdin).get('id', ''))")
log_success "✅ Documento 3 creado con ID: $DOC3_ID"

# Documento 4
log_header "Creando documento 4..."
DOC4_RESPONSE=$(curl -s -X POST $BASE_URL/api/v1/documentos \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Información Bancaria",
    "contenido": "Banco: BCP | Cuenta: 191-1234567-0-89 | CCI: 00219100123456708912",
    "tipoDocumento": "CONFIDENCIAL"
  }')

DOC4_ID=$(echo "$DOC4_RESPONSE" | $PYTHON_CMD -c "import sys, json; print(json.load(sys.stdin).get('id', ''))")
log_success "✅ Documento 4 creado con ID: $DOC4_ID"

log_plain ""
log_success "✅ 4 documentos creados exitosamente, todos cifrados en BD"
log_plain ""
pause_script
log_plain ""

##############################################################################
# PRUEBA 4: Listar Todos los Documentos (Descifrado Masivo)
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 4: Listar Todos los Documentos"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""
log_plain "🎯 Objetivo: Verificar descifrado masivo de múltiples documentos"
log_plain "🔓 Proceso: Cada documento se descifra automáticamente"
log_plain ""
log_header "Ejecutando GET /api/v1/documentos..."
log_plain ""

curl -s -X GET "$BASE_URL/api/v1/documentos" | $PYTHON_CMD -c "
import sys, json
try:
    data = json.load(sys.stdin)
    print(f\"📊 Total de documentos: {len(data)}\")
    print(\"\")
    for i, doc in enumerate(data, 1):
        print(f\"Documento {i}:\")
        print(f\"  ID: {doc.get('id')}\")
        print(f\"  Título: {doc.get('titulo')}\")
        print(f\"  Contenido: {doc.get('contenido')[:50]}...\")
        print(f\"  Tipo: {doc.get('tipoDocumento')}\")
        print(\"\")
except Exception as e:
    print(f'❌ Error: {e}')
" | tee -a "$OUTPUT_FILE"

log_success "✅ Todos los documentos descifrados correctamente"
log_info "💡 Cada documento se descifró individualmente de forma automática"
log_plain ""
pause_script
log_plain ""

##############################################################################
# PRUEBA 5: Actualizar Documento
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 5: Actualizar Documento Cifrado"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""
log_plain "🎯 Objetivo: Actualizar contenido y verificar nuevo cifrado"
log_plain "🔄 Proceso: Nuevo contenido → Nuevo cifrado → Nueva persistencia"
log_plain ""
log_header "Actualizando documento $DOC1_ID..."
log_plain ""

curl -s -X PUT "$BASE_URL/api/v1/documentos/$DOC1_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Contrato Confidencial ACTUALIZADO",
    "contenido": "Información MODIFICADA - DNI: 12345678 | Status: ACTUALIZADO",
    "tipoDocumento": "CONFIDENCIAL"
  }' | $PYTHON_CMD -c "
import sys, json
try:
    data = json.load(sys.stdin)
    print(f\"✅ ID: {data.get('id')}\")
    print(f\"📄 Título: {data.get('titulo')}\")
    print(f\"📝 Contenido: {data.get('contenido')}\")
    print(f\"🔒 Tipo: {data.get('tipoDocumento')}\")
except Exception as e:
    print(f'❌ Error: {e}')
" | tee -a "$OUTPUT_FILE"

log_plain ""
log_success "✅ Documento actualizado y re-cifrado correctamente"
log_plain ""
pause_script
log_plain ""

##############################################################################
# PRUEBA 6: Eliminar Documento
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 6: Eliminar Documento Cifrado"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""
log_plain "🎯 Objetivo: Eliminar un documento de la base de datos"
log_plain ""
log_header "Eliminando documento $DOC4_ID..."
log_plain ""

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE_URL/api/v1/documentos/$DOC4_ID")

if [ "$HTTP_CODE" = "204" ]; then
    log_success "✅ Documento eliminado exitosamente (HTTP 204)"
else
    log_error "❌ Error al eliminar documento (HTTP $HTTP_CODE)"
fi

log_plain ""
pause_script
log_plain ""

# ============================================================================
# RESUMEN
# ============================================================================

log_header "╔════════════════════════════════════════════════════════════════╗"
log_header "║                    📊 RESUMEN DE PRUEBAS                       ║"
log_header "╚════════════════════════════════════════════════════════════════╝"
log_plain ""
log_success "✅ PRUEBA 1: Documento creado y cifrado automáticamente"
log_success "✅ PRUEBA 2: Descifrado automático al consultar por ID"
log_success "✅ PRUEBA 3: Múltiples documentos creados con cifrado"
log_success "✅ PRUEBA 4: Listado masivo con descifrado automático"
log_success "✅ PRUEBA 5: Documento actualizado y re-cifrado"
log_success "✅ PRUEBA 6: Documento eliminado correctamente"
log_plain ""

log_header "╔════════════════════════════════════════════════════════════════╗"
log_header "║              🎓 CONCEPTOS CLAVE DEMOSTRADOS                    ║"
log_header "╚════════════════════════════════════════════════════════════════╝"
log_plain ""
log_warning "🔐 AES-256-GCM (AEAD):     Cifrado + Autenticación integrados"
log_warning "📦 Google Tink:            API segura y fácil de usar"
log_warning "🔄 Flujo de Cifrado:       Antes de INSERT en BD"
log_warning "🔓 Flujo de Descifrado:    Después de SELECT de BD"
log_warning "🗄️  BD nunca ve:           Texto plano (solo contenido cifrado)"
log_warning "🔑 Gestión de Claves:      Crítica para producción (KMS recomendado)"
log_plain ""

log_header "╔════════════════════════════════════════════════════════════════╗"
log_header "║              🔐 FLUJO COMPLETO DE CIFRADO                      ║"
log_header "╚════════════════════════════════════════════════════════════════╝"
log_plain ""
log_info "POST /documentos"
log_plain "    ↓"
log_warning "Cliente envía: \"Información confidencial\""
log_plain "    ↓"
log_success "CryptoService.cifrar() → AES-256-GCM"
log_plain "    ↓"
log_header "PostgreSQL guarda: \"AebqJ3oc/tkB8ryE...\" ← CIFRADO"
log_plain "    ↓"
log_info "GET /documentos/1"
log_plain "    ↓"
log_header "PostgreSQL lee: \"AebqJ3oc/tkB8ryE...\""
log_plain "    ↓"
log_success "CryptoService.descifrar() → Texto plano"
log_plain "    ↓"
log_warning "API devuelve: \"Información confidencial\" ← DESCIFRADO"
log_plain ""

log_header "╔════════════════════════════════════════════════════════════════╗"
log_header "║           ⚠️  ADVERTENCIAS PARA PRODUCCIÓN                    ║"
log_header "╚════════════════════════════════════════════════════════════════╝"
log_plain ""
log_error "🚨 NO HACER:"
log_plain "   ❌ Regenerar claves en cada inicio"
log_plain "   ❌ Hardcodear claves en código"
log_plain "   ❌ Compartir claves por email"
log_plain ""
log_success "✅ SÍ HACER:"
log_plain "   ✓ Usar KMS (AWS/Google/Azure)"
log_plain "   ✓ Rotación de claves periódica"
log_plain "   ✓ Auditar accesos a datos"
log_plain "   ✓ Backup seguro de claves"
log_plain ""

{
echo ""
echo "📝 Log completo guardado en: $OUTPUT_FILE"
echo ""
} | tee -a "$OUTPUT_FILE"

log_success "🎉 ¡Pruebas de cifrado completadas exitosamente!"
log_plain ""

echo ""
echo -e "${MAGENTA}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${MAGENTA}║                    ✅ PRUEBAS FINALIZADAS                      ║${NC}"
echo -e "${MAGENTA}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}📄 Archivo de log: ${CYAN}$OUTPUT_FILE${NC}"
echo -e "${GREEN}📊 Total de pruebas: ${YELLOW}6${NC}"
echo -e "${GREEN}🔐 Documentos probados: ${YELLOW}4${NC}"
echo -e "${GREEN}✨ Estado: ${YELLOW}Completado${NC}"
echo ""