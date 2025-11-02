#!/bin/bash

# ============================================================================
# BENCHMARK JVM vs NATIVE - SISTEMA DE PRE-APROBACIÓN CREDITICIA
# ============================================================================
# COMPATIBLE CON: Windows (Git Bash), macOS, Linux
#
# Este script compara el rendimiento de la aplicación compilada en:
# 1. Modo JVM tradicional (java -jar)
# 2. Modo Native (GraalVM Native Image)
#
# MÉTRICAS COMPARADAS:
# - Tiempo de compilación
# - Tiempo de arranque (startup time)
# - Uso de memoria (RSS)
# - Throughput (requests/segundo)
# - Tamaño del artefacto
#
# OBJETIVO PEDAGÓGICO:
# Demostrar las ventajas y trade-offs de compilación nativa:
# - Native: arranque ultra-rápido, menor memoria, pero compilación lenta
# - JVM: arranque lento, más memoria, pero compilación rápida
#
# REQUISITOS:
# - Maven instalado (./mvnw debe existir)
# - Docker instalado (para compilación native)
# - curl instalado
# - (Opcional) ab o wrk para pruebas de carga
#
# USO:
#   chmod +x benchmark.sh
#   ./benchmark.sh
#
# SALIDA:
#   - Resultados en consola (con colores)
#   - Archivo: benchmark-report-YYYY-MM-DD-HHMMSS.txt
#   - Logs: /tmp/build-jvm.log y /tmp/build-native.log
#
# TIEMPO ESTIMADO: 10-15 minutos
# - Compilación JVM: ~1-2 minutos
# - Compilación Native: ~7-10 minutos
# - Pruebas: ~2-3 minutos
#
# AUTOR: Curso Quarkus Avanzado - Capítulo 9: Despliegue y Contenedores
# ============================================================================

set -e  # Salir si hay errores

# ----------------------------------------------------------------------------
# CONFIGURACIÓN
# ----------------------------------------------------------------------------

# URLs y puertos
BASE_URL="http://localhost:8080"
API_URL="${BASE_URL}/api/preaprobacion"
HEALTH_URL="${BASE_URL}/q/health/ready"

# Archivos de salida
TIMESTAMP=$(date +"%Y-%m-%d-%H%M%S")
REPORT_FILE="benchmark-report-${TIMESTAMP}.txt"
BUILD_JVM_LOG="/tmp/build-jvm-${TIMESTAMP}.log"
BUILD_NATIVE_LOG="/tmp/build-native-${TIMESTAMP}.log"

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m'

# Emojis
CHECK="✅"
CROSS="❌"
ROCKET="🚀"
CLOCK="⏱️"
MEMORY="💾"
FIRE="🔥"
CHART="📊"
PACKAGE="📦"
BUILDING="🔨"

# Variables para resultados
BUILD_TIME_JVM=""
BUILD_TIME_NATIVE=""
STARTUP_TIME_JVM=""
STARTUP_TIME_NATIVE=""
MEMORY_JVM=""
MEMORY_NATIVE=""
THROUGHPUT_JVM=""
THROUGHPUT_NATIVE=""
SIZE_JVM=""
SIZE_NATIVE=""

# ----------------------------------------------------------------------------
# FUNCIONES AUXILIARES
# ----------------------------------------------------------------------------

print_header() {
    echo ""
    echo -e "${CYAN}============================================================================${NC}"
    echo -e "${WHITE}$1${NC}"
    echo -e "${CYAN}============================================================================${NC}"
    echo ""
    
    # Al archivo sin colores
    echo "" >> "$REPORT_FILE"
    echo "============================================================================" >> "$REPORT_FILE"
    echo "$1" >> "$REPORT_FILE"
    echo "============================================================================" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
}

print_section() {
    echo ""
    echo -e "${MAGENTA}>>> $1${NC}"
    echo ""
    
    echo "" >> "$REPORT_FILE"
    echo ">>> $1" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
}

print_success() {
    echo -e "${GREEN}${CHECK} $1${NC}"
    echo "✓ $1" >> "$REPORT_FILE"
}

print_error() {
    echo -e "${RED}${CROSS} $1${NC}"
    echo "✗ $1" >> "$REPORT_FILE"
}

print_info() {
    echo -e "${BLUE}$1${NC}"
    echo "$1" >> "$REPORT_FILE"
}

print_warning() {
    echo -e "${YELLOW}$1${NC}"
    echo "$1" >> "$REPORT_FILE"
}

# Función para medir tiempo de ejecución
measure_time() {
    local start=$(date +%s)
    "$@"
    local end=$(date +%s)
    echo $((end - start))
}

# Función para esperar que el servicio esté listo
wait_for_service() {
    local max_attempts=60
    local attempt=1
    
    print_info "Esperando que el servicio esté listo..."
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s -f "$HEALTH_URL" > /dev/null 2>&1; then
            print_success "Servicio listo después de ${attempt} segundos"
            return 0
        fi
        sleep 1
        attempt=$((attempt + 1))
    done
    
    print_error "Servicio no respondió después de ${max_attempts} segundos"
    return 1
}

# Función para medir tiempo de arranque
measure_startup() {
    local mode=$1
    local log_file=$2
    
    local start=$(date +%s.%N 2>/dev/null || date +%s)
    
    if wait_for_service; then
        local end=$(date +%s.%N 2>/dev/null || date +%s)
        # Calcular diferencia (compatible con macOS que no tiene %N)
        if command -v bc &> /dev/null; then
            echo "$end - $start" | bc
        else
            echo $(($(date +%s) - start))
        fi
    else
        echo "ERROR"
    fi
}

# Función para medir memoria (portable)
measure_memory() {
    local pid=$1
    
    # Intentar diferentes métodos según el sistema
    if command -v ps &> /dev/null; then
        # macOS y Linux
        local mem_kb=$(ps -o rss= -p "$pid" 2>/dev/null | awk '{print $1}')
        if [ -n "$mem_kb" ]; then
            echo "$((mem_kb / 1024)) MB"
            return 0
        fi
    fi
    
    echo "N/A"
}

# Función para hacer prueba de carga simple
simple_load_test() {
    local url=$1
    local requests=100
    local start=$(date +%s)
    
    print_info "Ejecutando ${requests} requests..."
    
    local success=0
    for i in $(seq 1 $requests); do
        if curl -s -f "$url" > /dev/null 2>&1; then
            success=$((success + 1))
        fi
    done
    
    local end=$(date +%s)
    local duration=$((end - start))
    
    if [ $duration -gt 0 ]; then
        local rps=$((success / duration))
        echo "${rps} req/s"
    else
        echo "N/A"
    fi
}

# Función para detener proceso en puerto 8080
stop_service() {
    print_info "Deteniendo servicio en puerto 8080..."
    
    # Buscar PID en puerto 8080
    local pid=""
    
    if command -v lsof &> /dev/null; then
        # macOS y algunas distribuciones Linux
        pid=$(lsof -ti:8080 2>/dev/null || echo "")
    elif command -v netstat &> /dev/null; then
        # Windows Git Bash
        pid=$(netstat -ano | grep ":8080" | awk '{print $5}' | head -1)
    fi
    
    if [ -n "$pid" ] && [ "$pid" != "" ]; then
        kill -9 "$pid" 2>/dev/null || true
        sleep 2
        print_success "Servicio detenido (PID: $pid)"
    else
        print_info "No hay servicio corriendo en puerto 8080"
    fi
}

# Función para obtener tamaño de archivo
get_file_size() {
    local file=$1
    
    if [ -f "$file" ]; then
        local size_bytes=$(wc -c < "$file" 2>/dev/null || stat -f%z "$file" 2>/dev/null || echo "0")
        local size_mb=$((size_bytes / 1024 / 1024))
        echo "${size_mb} MB"
    else
        echo "N/A"
    fi
}

# ----------------------------------------------------------------------------
# INICIO DEL BENCHMARK
# ----------------------------------------------------------------------------

# Crear archivo de reporte con encabezado
cat > "$REPORT_FILE" << HEADER
================================================================================
BENCHMARK: JVM vs NATIVE IMAGE
Sistema de Pre-Aprobación Crediticia Express
================================================================================

Fecha de ejecución: $(date +"%Y-%m-%d %H:%M:%S")
Sistema operativo: $(uname -s 2>/dev/null || echo "Windows")
Arquitectura: $(uname -m 2>/dev/null || echo "x86_64")
Generado por: benchmark.sh

Este reporte compara el rendimiento de la aplicación Quarkus compilada en:
- Modo JVM tradicional (java -jar)
- Modo Native (GraalVM Native Image)

Métricas evaluadas:
- Tiempo de compilación (build time)
- Tiempo de arranque (startup time)
- Uso de memoria RAM (RSS)
- Throughput (requests por segundo)
- Tamaño del artefacto generado

================================================================================

HEADER

print_header "${ROCKET} BENCHMARK JVM vs NATIVE IMAGE"

cat << 'INTRO' | tee -a "$REPORT_FILE"

ESTE BENCHMARK DEMOSTRARÁ:

  JVM MODE:
    ✅ Compilación rápida (~1-2 min)
    ❌ Arranque lento (~2-3 seg)
    ❌ Mayor uso de memoria (~150-200 MB)
    ✅ No requiere Docker

  NATIVE MODE:
    ❌ Compilación lenta (~7-10 min)
    ✅ Arranque ultra-rápido (~0.05 seg)
    ✅ Menor uso de memoria (~50-80 MB)
    ❌ Requiere Docker o GraalVM

⚠️  ADVERTENCIA: Este proceso tomará 10-15 minutos aproximadamente.
    La mayor parte del tiempo es la compilación en modo Native.

INTRO

read -p "Presiona ENTER para comenzar el benchmark..."

# Detener cualquier servicio corriendo
stop_service

# ----------------------------------------------------------------------------
# FASE 1: COMPILACIÓN EN MODO JVM
# ----------------------------------------------------------------------------

print_header "${BUILDING} FASE 1: COMPILACIÓN JVM"

print_section "1.1 - Limpiando proyecto"
./mvnw clean > /dev/null 2>&1 || true
print_success "Proyecto limpio"

print_section "1.2 - Compilando en modo JVM (esto tomará ~1-2 minutos)"
print_info "Ejecutando: ./mvnw package -DskipTests"
print_info "Log guardado en: ${BUILD_JVM_LOG}"

START_BUILD_JVM=$(date +%s)
if ./mvnw package -DskipTests > "$BUILD_JVM_LOG" 2>&1; then
    END_BUILD_JVM=$(date +%s)
    BUILD_TIME_JVM=$((END_BUILD_JVM - START_BUILD_JVM))
    print_success "Compilación JVM completada en ${BUILD_TIME_JVM} segundos"
    
    # Obtener tamaño del JAR
    if [ -f "target/quarkus-app/quarkus-run.jar" ]; then
        SIZE_JVM=$(get_file_size "target/quarkus-app/quarkus-run.jar")
        print_info "Tamaño del JAR: ${SIZE_JVM}"
    fi
else
    print_error "Error en compilación JVM. Ver log: ${BUILD_JVM_LOG}"
    exit 1
fi

# ----------------------------------------------------------------------------
# FASE 2: PRUEBA EN MODO JVM
# ----------------------------------------------------------------------------

print_header "${FIRE} FASE 2: PRUEBAS JVM"

print_section "2.1 - Midiendo tiempo de arranque JVM"
print_info "Iniciando aplicación JVM..."

# Iniciar en background
java -jar target/quarkus-app/quarkus-run.jar > /tmp/jvm-run.log 2>&1 &
JVM_PID=$!

print_info "PID del proceso JVM: ${JVM_PID}"

# Medir tiempo de arranque
STARTUP_START=$(date +%s)
if wait_for_service; then
    STARTUP_END=$(date +%s)
    STARTUP_TIME_JVM=$((STARTUP_END - STARTUP_START))
    print_success "Arranque JVM: ${STARTUP_TIME_JVM} segundos"
else
    print_error "Fallo al arrancar aplicación JVM"
    kill -9 $JVM_PID 2>/dev/null || true
    exit 1
fi

print_section "2.2 - Midiendo uso de memoria JVM"
sleep 3  # Esperar a que se estabilice
MEMORY_JVM=$(measure_memory $JVM_PID)
print_info "Memoria JVM (RSS): ${MEMORY_JVM}"

print_section "2.3 - Midiendo throughput JVM"
THROUGHPUT_JVM=$(simple_load_test "$API_URL/estadisticas")
print_info "Throughput JVM: ${THROUGHPUT_JVM}"

# Detener aplicación JVM
print_section "2.4 - Deteniendo aplicación JVM"
kill -9 $JVM_PID 2>/dev/null || true
sleep 2
print_success "Aplicación JVM detenida"

# ----------------------------------------------------------------------------
# FASE 3: COMPILACIÓN EN MODO NATIVE
# ----------------------------------------------------------------------------

print_header "${BUILDING} FASE 3: COMPILACIÓN NATIVE"

print_warning "⚠️  ADVERTENCIA: Esta fase tomará 7-10 minutos"
print_info "La compilación Native es lenta pero el resultado es ultra-rápido"

print_section "3.1 - Limpiando proyecto"
./mvnw clean > /dev/null 2>&1 || true
print_success "Proyecto limpio"

print_section "3.2 - Compilando en modo Native (7-10 minutos)"
print_info "Ejecutando: ./mvnw package -Pnative -DskipTests"
print_info "Log guardado en: ${BUILD_NATIVE_LOG}"
print_info ""
print_info "Mientras esperas, esto es lo que está pasando:"
print_info "- GraalVM analiza toda la aplicación"
print_info "- Genera código nativo específico para tu CPU"
print_info "- Optimiza y reduce el tamaño del ejecutable"
print_info "- Elimina código no usado (tree shaking)"
print_info ""
print_warning "${CLOCK} Por favor, espera... (puedes ver el log en otra terminal)"

START_BUILD_NATIVE=$(date +%s)
if ./mvnw package -Pnative -DskipTests > "$BUILD_NATIVE_LOG" 2>&1; then
    END_BUILD_NATIVE=$(date +%s)
    BUILD_TIME_NATIVE=$((END_BUILD_NATIVE - START_BUILD_NATIVE))
    print_success "Compilación Native completada en ${BUILD_TIME_NATIVE} segundos ($(($BUILD_TIME_NATIVE / 60)) minutos)"
    
    # Obtener tamaño del ejecutable nativo
    if [ -f "target/aprobacion-express-1.0.0-runner" ]; then
        SIZE_NATIVE=$(get_file_size "target/aprobacion-express-1.0.0-runner")
        print_info "Tamaño del ejecutable Native: ${SIZE_NATIVE}"
    fi
else
    print_error "Error en compilación Native. Ver log: ${BUILD_NATIVE_LOG}"
    print_info ""
    print_info "Posibles causas:"
    print_info "- Docker no está corriendo"
    print_info "- Falta memoria RAM (se necesitan ~4GB libres)"
    print_info "- Error en dependencias"
    exit 1
fi

# ----------------------------------------------------------------------------
# FASE 4: PRUEBA EN MODO NATIVE
# ----------------------------------------------------------------------------

print_header "${FIRE} FASE 4: PRUEBAS NATIVE"

print_section "4.1 - Midiendo tiempo de arranque Native"
print_info "Iniciando aplicación Native..."

# Iniciar en background
./target/aprobacion-express-1.0.0-runner > /tmp/native-run.log 2>&1 &
NATIVE_PID=$!

print_info "PID del proceso Native: ${NATIVE_PID}"

# Medir tiempo de arranque
STARTUP_START=$(date +%s)
if wait_for_service; then
    STARTUP_END=$(date +%s)
    STARTUP_TIME_NATIVE=$((STARTUP_END - STARTUP_START))
    print_success "Arranque Native: ${STARTUP_TIME_NATIVE} segundos"
    print_info "¡Nota la diferencia! Native arranca MUCHO más rápido"
else
    print_error "Fallo al arrancar aplicación Native"
    kill -9 $NATIVE_PID 2>/dev/null || true
    exit 1
fi

print_section "4.2 - Midiendo uso de memoria Native"
sleep 3  # Esperar a que se estabilice
MEMORY_NATIVE=$(measure_memory $NATIVE_PID)
print_info "Memoria Native (RSS): ${MEMORY_NATIVE}"

print_section "4.3 - Midiendo throughput Native"
THROUGHPUT_NATIVE=$(simple_load_test "$API_URL/estadisticas")
print_info "Throughput Native: ${THROUGHPUT_NATIVE}"

# Detener aplicación Native
print_section "4.4 - Deteniendo aplicación Native"
kill -9 $NATIVE_PID 2>/dev/null || true
sleep 2
print_success "Aplicación Native detenida"

# ----------------------------------------------------------------------------
# FASE 5: COMPARATIVA Y ANÁLISIS
# ----------------------------------------------------------------------------

print_header "${CHART} FASE 5: COMPARATIVA FINAL"

COMPARISON="
╔══════════════════════════════════════════════════════════════════════════════╗
║                     RESULTADOS DEL BENCHMARK                                 ║
╠══════════════════════════════════════════════════════════════════════════════╣
║ MÉTRICA                     │      JVM MODE       │    NATIVE MODE            ║
╠═════════════════════════════╪═════════════════════╪═══════════════════════════╣
║ Tiempo de compilación       │  ${BUILD_TIME_JVM}s ($(($BUILD_TIME_JVM / 60))m)        │  ${BUILD_TIME_NATIVE}s ($(($BUILD_TIME_NATIVE / 60))m)       ║
║ Tiempo de arranque          │  ${STARTUP_TIME_JVM}s                │  ${STARTUP_TIME_NATIVE}s                 ║
║ Uso de memoria (RSS)        │  ${MEMORY_JVM}           │  ${MEMORY_NATIVE}            ║
║ Throughput                  │  ${THROUGHPUT_JVM}         │  ${THROUGHPUT_NATIVE}        ║
║ Tamaño del artefacto        │  ${SIZE_JVM}           │  ${SIZE_NATIVE}            ║
╚══════════════════════════════════════════════════════════════════════════════╝
"

echo "$COMPARISON" | tee -a "$REPORT_FILE"

print_section "ANÁLISIS Y CONCLUSIONES"

cat << 'ANALYSIS' | tee -a "$REPORT_FILE"

📊 INTERPRETACIÓN DE RESULTADOS:

1. TIEMPO DE COMPILACIÓN:
   - JVM es ~5-8x más rápido en compilar
   - Native toma más tiempo pero es proceso único (una vez)
   - En producción, el tiempo de compilación no importa mucho

2. TIEMPO DE ARRANQUE:
   - Native arranca ~20-40x más rápido que JVM
   - Ideal para: serverless, microservicios, contenedores
   - JVM: arranque lento pero aceptable para apps long-running

3. USO DE MEMORIA:
   - Native usa ~50-60% menos memoria que JVM
   - Importante para: contenedores, cloud (ahorro de costos)
   - JVM: mayor uso pero con mejor optimización en runtime

4. THROUGHPUT:
   - Rendimiento similar en ambos modos
   - JVM puede tener mejor rendimiento con JIT optimizations
   - Native: rendimiento predecible y consistente

5. TAMAÑO DEL ARTEFACTO:
   - Native: ejecutable único, sin dependencias
   - JVM: requiere JDK/JRE instalado
   - Native: mejor para distribución

🎯 CUÁNDO USAR CADA MODO:

USE JVM MODE CUANDO:
  ✅ Desarrollo local (compilación rápida)
  ✅ Aplicaciones long-running (servidores tradicionales)
  ✅ No hay restricciones de memoria
  ✅ Necesita debugging avanzado

USE NATIVE MODE CUANDO:
  ✅ Despliegue en cloud/contenedores (ahorro de costos)
  ✅ Microservicios con alta escalabilidad
  ✅ Funciones serverless (AWS Lambda, Azure Functions)
  ✅ Arranque rápido es crítico
  ✅ Restricciones de memoria

💡 RECOMENDACIÓN PARA ESTE PROYECTO:
   Para un sistema bancario de pre-aprobación crediticia que necesita:
   - Respuestas rápidas (< 200ms)
   - Escalabilidad horizontal
   - Optimización de costos en cloud
   
   → NATIVE MODE es la mejor opción para producción
   → JVM MODE para desarrollo

ANALYSIS

# Footer del reporte
cat >> "$REPORT_FILE" << FOOTER

================================================================================
FIN DEL BENCHMARK
================================================================================

Archivo generado: $REPORT_FILE
Fecha: $(date +"%Y-%m-%d %H:%M:%S")

Logs de compilación:
- JVM: ${BUILD_JVM_LOG}
- Native: ${BUILD_NATIVE_LOG}

Para más información, consulta:
- README.md: Guía de usuario
- TEORIA.md: Conceptos profundos sobre GraalVM
- instructor.md: Guía del profesor

================================================================================
FOOTER

print_success "Reporte completo guardado en: ${REPORT_FILE}"
echo ""
print_info "Logs de compilación disponibles en:"
print_info "  - JVM: ${BUILD_JVM_LOG}"
print_info "  - Native: ${BUILD_NATIVE_LOG}"
echo ""

print_success "${ROCKET} ¡Benchmark completado exitosamente!"

# ----------------------------------------------------------------------------
# FIN DEL SCRIPT
# ----------------------------------------------------------------------------

