#!/bin/bash
echo "╔═══════════════════════════════════════════╗"
echo "║  Verificación de Entorno Quarkus         ║"
echo "║  Windows + Git Bash                       ║"
echo "╚═══════════════════════════════════════════╝"
echo ""

# Función para verificar comando
verificar() {
    if command -v $1 &> /dev/null; then
        echo "✅ $2: INSTALADO"
        $1 --version 2>&1 | head -1
    else
        echo "❌ $2: NO ENCONTRADO"
        echo "   Instalar con Chocolatey: choco install $3"
    fi
    echo ""
}

# Verificar Git Bash
echo "✅ Git Bash: OK (estás ejecutando este script aquí)"
echo ""

verificar "java" "Java" "openjdk21"
verificar "quarkus" "Quarkus CLI" "quarkus"

# Verificar JAVA_HOME
echo "🔍 JAVA_HOME:"
if [ -z "$JAVA_HOME" ]; then
    echo "   ⚠️  No configurado"
    echo "   Configurar en Variables de Entorno de Windows"
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

# Verificar que estamos en Git Bash
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
    echo "✅ Sistema operativo: Windows con Git Bash"
else
    echo "⚠️  Este script está diseñado para Windows + Git Bash"
fi
echo ""

echo "╔═══════════════════════════════════════════╗"
echo "║  Verificación Completa                    ║"
echo "╚═══════════════════════════════════════════╝"
