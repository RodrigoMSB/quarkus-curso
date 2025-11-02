#!/bin/bash
echo "╔═══════════════════════════════════════════╗"
echo "║  Verificación de Entorno Quarkus         ║"
echo "╚═══════════════════════════════════════════╝"
echo ""

# Función para verificar comando
verificar() {
    if command -v $1 &> /dev/null; then
        echo "✅ $2: INSTALADO"
        $1 --version 2>&1 | head -1
    else
        echo "❌ $2: NO ENCONTRADO"
        echo "   Instalar con: $3"
    fi
    echo ""
}

verificar "java" "Java" "brew install openjdk@21"
verificar "mvn" "Maven" "brew install maven (opcional, incluido en proyecto)"
verificar "quarkus" "Quarkus CLI" "brew install quarkusio/tap/quarkus"

# Verificar JAVA_HOME
echo "🔍 JAVA_HOME:"
if [ -z "$JAVA_HOME" ]; then
    echo "   ⚠️  No configurado (puede funcionar sin esto)"
else
    echo "   ✅ $JAVA_HOME"
fi
echo ""

# Verificar versión de Java
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -ge 17 ]; then
    echo "✅ Java version compatible (>= 17)"
else
    echo "❌ Java version muy antigua, se requiere Java 17+"
fi
echo ""

echo "╔═══════════════════════════════════════════╗"
echo "║  Verificación Completa                    ║"
echo "╚═══════════════════════════════════════════╝"
