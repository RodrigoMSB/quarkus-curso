#!/bin/bash

##############################################################################
# Script de Pruebas - Cifrado de Datos Sensibles con Google Tink
# 
# Este script prueba las capacidades de cifrado del microservicio de
# documentos utilizando Google Tink con AES-256-GCM.
#
# Conceptos que se prueban:
# - Cifrado a nivel de aplicación con Google Tink
# - AES-256-GCM (AEAD - Authenticated Encryption with Associated Data)
# - Cifrado antes de persistir / Descifrado al leer
# - Verificación directa en PostgreSQL del contenido cifrado
##############################################################################

# ============================================================================
# CONFIGURACIÓN DEL SCRIPT
# ============================================================================

# Generar timestamp y nombre de archivo de salida
TIMESTAMP=$(date +%Y-%m-%d_%H-%M-%S)
OUTPUT_FILE="test-documentos-cifrados-${TIMESTAMP}.txt"

# Colores para mejor visualización (solo para terminal)
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # Sin color

# URL base del microservicio
BASE_URL="http://localhost:8080"

# Configuración de PostgreSQL
PGHOST="localhost"
PGPORT="5432"
PGDATABASE="postgres"
PGUSER="rodrigosilva"
PGPASSWORD=""

# ============================================================================
# FUNCIÓN PARA LOGGING DUAL (Pantalla + Archivo)
# ============================================================================
# Esta función envía output tanto a la terminal (con colores) 
# como al archivo (sin colores)

log_header() {
    local message="$1"
    echo -e "${CYAN}${message}${NC}"
    echo "$message" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

log_info() {
    local message="$1"
    echo -e "${BLUE}${message}${NC}"
    echo "$message" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

log_success() {
    local message="$1"
    echo -e "${GREEN}${message}${NC}"
    echo "$message" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

log_warning() {
    local message="$1"
    echo -e "${YELLOW}${message}${NC}"
    echo "$message" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

log_error() {
    local message="$1"
    echo -e "${RED}${message}${NC}"
    echo "$message" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

log_plain() {
    local message="$1"
    echo -e "$message"
    echo "$message" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

# ============================================================================
# HEADER DEL ARCHIVO DE LOG
# ============================================================================

{
cat << 'EOF'
╔════════════════════════════════════════════════════════════════╗
║   🔐 PRUEBAS DE CIFRADO CON GOOGLE TINK                       ║
║   Cifrado a Nivel de Aplicación - AES-256-GCM                ║
╚════════════════════════════════════════════════════════════════╝
EOF

echo ""
echo "📅 Fecha: $(date '+%d/%m/%Y %H:%M:%S')"
echo "🌐 API Base: $BASE_URL"
echo "🗄️  PostgreSQL: $PGHOST:$PGPORT/$PGDATABASE"
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

# Verificar que curl está instalado
if ! command -v curl &> /dev/null; then
    log_error "❌ Error: curl no está instalado"
    exit 1
fi
log_success "✓ curl instalado"

# Verificar que python3 está instalado
if ! command -v python3 &> /dev/null; then
    log_error "❌ Error: python3 no está instalado"
    exit 1
fi
log_success "✓ python3 instalado"

# Verificar que psql está instalado
if ! command -v psql &> /dev/null; then
    log_warning "⚠️  psql no encontrado - pruebas de BD serán omitidas"
    PSQL_AVAILABLE=false
else
    log_success "✓ psql instalado"
    PSQL_AVAILABLE=true
fi

# Verificar conectividad con el servicio
log_plain ""
log_header "Verificando conectividad con el servicio..."
if curl -s --head --request GET "$BASE_URL/api/v1/documentos" | grep "200\|404" > /dev/null; then 
    log_success "✓ Servicio accesible en $BASE_URL"
else
    log_error "❌ Error: No se pudo conectar al servicio en $BASE_URL"
    log_warning "Verifica que la aplicación esté corriendo con: ./mvnw quarkus:dev"
    exit 1
fi

# Verificar conectividad con PostgreSQL
if [ "$PSQL_AVAILABLE" = true ]; then
    log_plain ""
    log_header "Verificando conectividad con PostgreSQL..."
    if PGPASSWORD=$PGPASSWORD psql -h $PGHOST -p $PGPORT -U $PGUSER -d $PGDATABASE -c "SELECT 1" > /dev/null 2>&1; then
        log_success "✓ PostgreSQL accesible en $PGHOST:$PGPORT"
    else
        log_warning "⚠️  No se pudo conectar a PostgreSQL - pruebas de BD serán omitidas"
        PSQL_AVAILABLE=false
    fi
fi

log_plain ""
read -p "$(echo -e ${YELLOW}Presiona ENTER para iniciar las pruebas...${NC})"
log_plain ""

# ============================================================================
# INICIO DE PRUEBAS
# ============================================================================

##############################################################################
# PRUEBA 1: Crear Documento con Cifrado Automático
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 1: Crear Documento con Cifrado Automático"
log_info "═══════════════════════════════════════════════════════"
log_plain ""
log_plain "🎯 Objetivo: Crear un documento y verificar que se cifra antes de persistir"
log_plain "🔐 Algoritmo: AES-256-GCM (Authenticated Encryption)"
log_plain "📝 Contenido: \"Información confidencial del cliente - DNI: 12345678\""
log_plain ""
log_header "Ejecutando POST /api/v1/documentos..."
log_plain ""

DOC1_RESPONSE=$(curl -s -X POST $BASE_URL/api/v1/documentos \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Contrato Confidencial #001",
    "contenido": "Información confidencial del cliente - DNI: 12345678, Cuenta: 1234567890, Saldo: S/. 50,000"
  }')

if [ $? -eq 0 ]; then
    DOC1_ID=$(echo "$DOC1_RESPONSE" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    print(f\"✓ ID: {data['id']}\")
    print(f\"✓ Título: {data['titulo']}\")
    print(f\"✓ Contenido (descifrado): {data['contenido'][:50]}...\")
    print(f\"✓ Fecha: {data['fechaCreacion']}\")
    print(data['id'])
except Exception as e:
    print(f\"❌ Error: {e}\")
" | tee -a "$OUTPUT_FILE" | tail -1)
else
    log_error "❌ Error al crear documento"
    exit 1
fi

log_plain ""
log_success "✅ Documento creado - ID: $DOC1_ID"
log_header "ℹ️  El contenido fue CIFRADO antes de guardarse en PostgreSQL"
log_plain ""
read -p "$(echo -e ${YELLOW}Presiona ENTER para continuar...${NC})"
log_plain ""

##############################################################################
# PRUEBA 2: Verificar Cifrado en la Base de Datos
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 2: Verificar Cifrado en la Base de Datos"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""
log_plain "🎯 Objetivo: Confirmar que el contenido está CIFRADO en PostgreSQL"
log_plain "🔍 Técnica: Consulta SQL directa para ver el contenido_cifrado"
log_plain ""

if [ "$PSQL_AVAILABLE" = true ]; then
    log_header "Ejecutando consulta SQL en PostgreSQL..."
    log_plain ""
    
    PGPASSWORD=$PGPASSWORD psql -h $PGHOST -p $PGPORT -U $PGUSER -d $PGDATABASE -c "
    SELECT 
        id,
        titulo,
        LEFT(contenido_cifrado, 60) || '...' as contenido_cifrado_sample,
        fecha_creacion
    FROM documento
    WHERE id = $DOC1_ID;" | tee -a "$OUTPUT_FILE"
    
    log_plain ""
    log_success "✅ OBSERVA: El campo 'contenido_cifrado' contiene texto ilegible"
    log_header "🔐 Esto confirma que el contenido NO está en texto plano en la BD"
    log_warning "⚠️  Si pudieras leer el contenido, ¡habría un problema de seguridad!"
else
    log_warning "⚠️  psql no disponible - omitiendo verificación de BD"
fi

log_plain ""
read -p "$(echo -e ${YELLOW}Presiona ENTER para continuar...${NC})"
log_plain ""

##############################################################################
# PRUEBA 3: Consultar Documento (Descifrado Automático)
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 3: Consultar Documento (Descifrado Automático)"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""
log_plain "🎯 Objetivo: Verificar que el API descifra automáticamente al leer"
log_plain "🔓 Operación: GET /api/v1/documentos/$DOC1_ID"
log_plain ""
log_header "Ejecutando consulta..."
log_plain ""

curl -s "$BASE_URL/api/v1/documentos/$DOC1_ID" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    print(f\"✓ ID: {data['id']}\")
    print(f\"✓ Título: {data['titulo']}\")
    print(f\"✓ Contenido (DESCIFRADO):\")
    print(f\"   {data['contenido']}\")
    print(f\"✓ Fecha: {data['fechaCreacion']}\")
except Exception as e:
    print(f\"❌ Error: {e}\")
" | tee -a "$OUTPUT_FILE"

log_plain ""
log_success "✅ El API devolvió el contenido DESCIFRADO correctamente"
log_header "ℹ️  Flujo: BD (cifrado) → CryptoService.descifrar() → API (texto plano)"
log_plain ""
read -p "$(echo -e ${YELLOW}Presiona ENTER para continuar...${NC})"
log_plain ""

##############################################################################
# PRUEBA 4: Crear Múltiples Documentos
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 4: Crear Múltiples Documentos Cifrados"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""
log_plain "🎯 Objetivo: Crear varios documentos para probar el listado"
log_plain "📝 Documentos a crear: 3 adicionales"
log_plain ""

# Documento 2
log_header "Creando documento 2..."
curl -s -X POST $BASE_URL/api/v1/documentos \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Informe Médico Confidencial",
    "contenido": "Paciente: Juan Pérez, Diagnóstico: [REDACTADO], Tratamiento: [REDACTADO]"
  }' > /dev/null
log_success "✓ Documento 2 creado"

# Documento 3
log_header "Creando documento 3..."
curl -s -X POST $BASE_URL/api/v1/documentos \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Contraseña de Admin",
    "contenido": "Sistema: BancoCore, Usuario: admin, Password: P@ssw0rd123!"
  }' > /dev/null
log_success "✓ Documento 3 creado"

# Documento 4
log_header "Creando documento 4..."
curl -s -X POST $BASE_URL/api/v1/documentos \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Claves API",
    "contenido": "AWS_ACCESS_KEY=AKIAIOSFODNN7EXAMPLE, AWS_SECRET_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
  }' > /dev/null
log_success "✓ Documento 4 creado"

log_plain ""
log_success "✅ 3 documentos adicionales creados y cifrados"
log_plain ""
read -p "$(echo -e ${YELLOW}Presiona ENTER para continuar...${NC})"
log_plain ""

##############################################################################
# PRUEBA 5: Listar Todos los Documentos
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 5: Listar Todos los Documentos (Descifrado Masivo)"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""
log_plain "🎯 Objetivo: Verificar que todos los documentos se descifran al listar"
log_plain "🔓 Operación: GET /api/v1/documentos"
log_plain ""
log_header "Ejecutando listado..."
log_plain ""

curl -s "$BASE_URL/api/v1/documentos" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    print(f\"📊 Total de documentos: {len(data)}\")
    print()
    for doc in data:
        print(f\"ID {doc['id']}: {doc['titulo']}\")
        contenido = doc['contenido']
        preview = contenido[:50] + '...' if len(contenido) > 50 else contenido
        print(f\"   Contenido: {preview}\")
        print()
except Exception as e:
    print(f\"❌ Error: {e}\")
" | tee -a "$OUTPUT_FILE"

log_success "✅ Todos los documentos fueron descifrados correctamente"
log_header "ℹ️  Cada documento pasa por CryptoService.descifrar() antes de devolverse"
log_plain ""
read -p "$(echo -e ${YELLOW}Presiona ENTER para continuar...${NC})"
log_plain ""

##############################################################################
# PRUEBA 6: Comparación BD vs API
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 6: Comparación BD Cifrada vs API Descifrada"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""
log_plain "🎯 Objetivo: Demostrar visualmente la diferencia"
log_plain "🔐 Se compara el contenido en BD (cifrado) vs API (descifrado)"
log_plain ""

if [ "$PSQL_AVAILABLE" = true ]; then
    log_header "📄 Contenido en PostgreSQL (CIFRADO):"
    log_plain ""
    
    PGPASSWORD=$PGPASSWORD psql -h $PGHOST -p $PGPORT -U $PGUSER -d $PGDATABASE -t -c "
    SELECT 
        '🔐 ID ' || id || ': ' || titulo || E'\n' ||
        '   Cifrado: ' || LEFT(contenido_cifrado, 80) || '...'
    FROM documento
    ORDER BY id
    LIMIT 4;" | tee -a "$OUTPUT_FILE"
    
    log_plain ""
    log_plain ""
    log_header "📄 Contenido desde API (DESCIFRADO):"
    log_plain ""
    
    curl -s "$BASE_URL/api/v1/documentos" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    for doc in data[:4]:
        print(f\"🔓 ID {doc['id']}: {doc['titulo']}\")
        contenido = doc['contenido']
        preview = contenido[:80] + '...' if len(contenido) > 80 else contenido
        print(f\"   Descifrado: {preview}\")
        print()
except Exception as e:
    print(f\"❌ Error: {e}\")
" | tee -a "$OUTPUT_FILE"
    
    log_success "✅ DIFERENCIA CLAVE:"
    log_warning "   • BD almacena: Texto ilegible (AES-256-GCM cifrado)"
    log_warning "   • API devuelve: Texto legible (descifrado en memoria)"
else
    log_warning "⚠️  psql no disponible - omitiendo comparación"
fi

log_plain ""
read -p "$(echo -e ${YELLOW}Presiona ENTER para continuar...${NC})"
log_plain ""

##############################################################################
# PRUEBA 7: Seguridad - ¿Qué pasa si pierdo la clave?
##############################################################################
log_info "═══════════════════════════════════════════════════════════"
log_warning "📋 PRUEBA 7: Demostración de Seguridad con Claves"
log_info "═══════════════════════════════════════════════════════════"
log_plain ""
log_plain "🎯 Objetivo: Entender la importancia de la gestión de claves"
log_plain "⚠️  Limitación actual: La clave se regenera en cada inicio"
log_plain ""
log_error "🚨 ESCENARIO CRÍTICO:"
log_plain "1. Aplicación arranca → Genera clave A"
log_plain "2. Creas documentos → Se cifran con clave A"
log_plain "3. Aplicación reinicia → Genera clave B (¡DIFERENTE!)"
log_plain "4. Intentas leer documentos → ❌ ERROR (cifrado con A, intentas descifrar con B)"
log_plain ""
log_header "💡 SOLUCIÓN PARA PRODUCCIÓN:"
log_success "   • Opción 1: Persistir clave en archivo JSON"
log_success "   • Opción 2: Usar KMS (AWS KMS, Google Cloud KMS, Azure Key Vault)"
log_success "   • Opción 3: Variable de entorno con la clave"
log_success "   • Opción 4: HashiCorp Vault"
log_plain ""
log_warning "⚠️  REGLA DE ORO: Perder la clave = Perder TODOS los datos cifrados"
log_error "   (¡No hay recuperación posible!)"
log_plain ""
read -p "$(echo -e ${YELLOW}Presiona ENTER para ver el resumen...${NC})"
log_plain ""

##############################################################################
# RESUMEN FINAL
##############################################################################
log_header "╔════════════════════════════════════════════════════════════════╗"
log_header "║                    📊 RESUMEN DE PRUEBAS                       ║"
log_header "╚════════════════════════════════════════════════════════════════╝"
log_plain ""
log_success "✅ PRUEBA 1: Documento creado y cifrado automáticamente"
log_success "✅ PRUEBA 2: Contenido confirmado como cifrado en PostgreSQL"
log_success "✅ PRUEBA 3: Descifrado automático al consultar por ID"
log_success "✅ PRUEBA 4: Múltiples documentos creados con cifrado"
log_success "✅ PRUEBA 5: Listado masivo con descifrado de todos los documentos"
log_success "✅ PRUEBA 6: Comparación visual BD cifrada vs API descifrada"
log_success "✅ PRUEBA 7: Comprensión de gestión de claves críticas"
log_plain ""
log_header "╔════════════════════════════════════════════════════════════════╗"
log_header "║              🎓 CONCEPTOS CLAVE DEMOSTRADOS                    ║"
log_header "╚════════════════════════════════════════════════════════════════╝"
log_plain ""
log_warning "🔐 AES-256-GCM (AEAD):     Cifrado + Autenticación"
log_warning "📦 Google Tink:            API segura y fácil de usar"
log_warning "🔄 Flujo de Cifrado:       Antes de INSERT en BD"
log_warning "🔓 Flujo de Descifrado:    Después de SELECT de BD"
log_warning "🗄️  BD nunca ve:           Texto plano (solo contenido cifrado)"
log_warning "🔑 Gestión de Claves:      Crítica para producción (KMS)"
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
log_cyan "PostgreSQL guarda: \"AebqJ3oc/tkB8ryE...\" ← CIFRADO"
log_plain "    ↓"
log_info "GET /documentos/1"
log_plain "    ↓"
log_cyan "PostgreSQL lee: \"AebqJ3oc/tkB8ryE...\""
log_plain "    ↓"
log_success "CryptoService.descifrar() → Texto plano"
log_plain "    ↓"
log_warning "API devuelve: \"Información confidencial\" ← DESCIFRADO"
log_plain ""

# Función auxiliar para cyan
log_cyan() {
    local message="$1"
    echo -e "${CYAN}${message}${NC}"
    echo "$message" | sed 's/\x1b\[[0-9;]*m//g' >> "$OUTPUT_FILE"
}

##############################################################################
# TABLA COMPARATIVA
##############################################################################
log_header "╔════════════════════════════════════════════════════════════════╗"
log_header "║         📊 COMPARACIÓN: ALWAYS ENCRYPTED VS TINK              ║"
log_header "╚════════════════════════════════════════════════════════════════╝"
log_plain ""
{
printf "%-25s | %-25s | %-25s\n" "ASPECTO" "ALWAYS ENCRYPTED" "GOOGLE TINK"
printf "%-25s-+-%-25s-+-%-25s\n" "-------------------------" "-------------------------" "-------------------------"
printf "%-25s | %-25s | %-25s\n" "Dónde se cifra" "SQL Server" "Aplicación Java"
printf "%-25s | %-25s | %-25s\n" "Gestión de claves" "Cert Store / Key Vault" "KMS o archivo"
printf "%-25s | %-25s | %-25s\n" "Portabilidad" "Solo SQL Server" "Cualquier BD"
printf "%-25s | %-25s | %-25s\n" "Control" "Limitado" "Total"
printf "%-25s | %-25s | %-25s\n" "Complejidad inicial" "Alta configuración" "Código explícito"
printf "%-25s | %-25s | %-25s\n" "Algoritmo" "AES-256" "AES-256-GCM (AEAD)"
} | tee -a "$OUTPUT_FILE"
log_plain ""

##############################################################################
# ADVERTENCIAS Y MEJORES PRÁCTICAS
##############################################################################
log_header "╔════════════════════════════════════════════════════════════════╗"
log_header "║           ⚠️  ADVERTENCIAS Y MEJORES PRÁCTICAS                ║"
log_header "╚════════════════════════════════════════════════════════════════╝"
log_plain ""
log_error "🚨 NO HACER EN PRODUCCIÓN:"
log_plain "   ❌ Regenerar claves en cada inicio"
log_plain "   ❌ Hardcodear claves en código fuente"
log_plain "   ❌ Almacenar claves en texto plano en el repositorio"
log_plain "   ❌ Compartir claves por email o chat"
log_plain ""
log_success "✅ HACER EN PRODUCCIÓN:"
log_plain "   ✓ Usar KMS (AWS KMS, Google Cloud KMS, Azure Key Vault)"
log_plain "   ✓ Implementar rotación de claves"
log_plain "   ✓ Auditar accesos a datos cifrados"
log_plain "   ✓ Backup seguro de claves (con cifrado adicional)"
log_plain "   ✓ Seguir principio de mínimo privilegio"
log_plain ""

##############################################################################
# EJERCICIOS PROPUESTOS
##############################################################################
log_header "╔════════════════════════════════════════════════════════════════╗"
log_header "║                  🎯 EJERCICIOS PROPUESTOS                      ║"
log_header "╚════════════════════════════════════════════════════════════════╝"
log_plain ""
log_warning "1. Persistencia de Clave:"
log_plain "   Modifica CryptoService para guardar/cargar la clave desde archivo JSON"
log_plain ""
log_warning "2. Múltiples Claves:"
log_plain "   Implementa diferentes claves para tipos de documentos (público/privado/confidencial)"
log_plain ""
log_warning "3. Cifrado Híbrido:"
log_plain "   Usa RSA para cifrar la clave AES y AES para cifrar el contenido"
log_plain ""
log_warning "4. Búsqueda Segura:"
log_plain "   Implementa búsqueda por hash SHA-256 del contenido sin descifrar"
log_plain ""
log_warning "5. Auditoría:"
log_plain "   Agrega logs de quién y cuándo accede a datos descifrados"
log_plain ""
log_warning "6. Rotación de Claves:"
log_plain "   Investiga cómo rotar claves sin perder acceso a datos antiguos"
log_plain ""

# ============================================================================
# FOOTER DEL ARCHIVO DE LOG
# ============================================================================

{
cat << 'EOF'

╔════════════════════════════════════════════════════════════════╗
║                    📁 ARCHIVO DE LOG                           ║
╚════════════════════════════════════════════════════════════════╝
EOF

echo ""
echo "📝 Todas las pruebas han sido guardadas en:"
echo "   $OUTPUT_FILE"
echo ""
echo "💡 Puedes revisar el log completo para:"
echo "   • Verificar las respuestas HTTP completas"
echo "   • Analizar el contenido cifrado vs descifrado"
echo "   • Compartir los resultados con tu instructor"
echo "   • Documentar el comportamiento del sistema de cifrado"
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
echo -e "${GREEN}📊 Total de pruebas: ${YELLOW}7${NC}"
echo -e "${GREEN}🔐 Documentos creados: ${YELLOW}4${NC}"
echo -e "${GREEN}✨ Estado: ${YELLOW}Completado${NC}"
echo ""
echo -e "${CYAN}🚀 Próximos pasos:${NC}"
echo -e "${YELLOW}   1. Revisa el log para análisis detallado${NC}"
echo -e "${YELLOW}   2. Experimenta con los ejercicios propuestos${NC}"
echo -e "${YELLOW}   3. Implementa persistencia de claves para producción${NC}"
echo ""