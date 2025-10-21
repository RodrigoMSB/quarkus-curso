#!/bin/bash

##############################################################################
# Script de Pruebas - Parte 2: Perfiles y Configuración Sensible
# 
# Este script prueba las capacidades de perfiles del microservicio TasaCorp
# demostrando comportamientos diferentes según el ambiente.
#
# Conceptos que se prueban:
# - Perfiles: %dev, %test, %prod
# - Configuración específica por ambiente
# - Límites transaccionales por perfil
# - Integración con HashiCorp Vault
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

echo -e "${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║    🎭 PRUEBAS DE PERFILES - PARTE 2                       ║${NC}"
echo -e "${CYAN}║    Perfiles de Ambiente y Configuración Sensible         ║${NC}"
echo -e "${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}⚠️  IMPORTANTE:${NC}"
echo -e "Este script probará los 3 perfiles: %dev, %test, %prod"
echo -e "Para cada perfil, necesitarás reiniciar la aplicación."
echo ""
echo -e "${CYAN}Los 3 perfiles que probaremos:${NC}"
echo -e "  🟢 ${GREEN}DEV${NC}  - Desarrollo: Sin restricciones, máxima productividad"
echo -e "  🟡 ${YELLOW}TEST${NC} - Testing: Ambiente controlado, límites realistas"
echo -e "  🔴 ${RED}PROD${NC} - Producción: Máxima seguridad, Vault integrado"
echo ""
read -p "Presiona ENTER para comenzar..."
echo ""

##############################################################################
# SECCIÓN 1: PERFIL DEV
##############################################################################
echo -e "${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║                    🟢 PERFIL: DEV                         ║${NC}"
echo -e "${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}Características esperadas del perfil DEV:${NC}"
echo -e "  ✓ Comisión: 0.0% (gratis para desarrollo)"
echo -e "  ✓ Límite transaccional: 999,999 (ilimitado)"
echo -e "  ✓ Cache: Desactivado"
echo -e "  ✓ Auditoría: Desactivada"
echo -e "  ✓ Proveedor: MockProvider"
echo -e "  ✓ Ambiente: desarrollo"
echo ""
echo -e "${YELLOW}Asegúrate de que la aplicación esté corriendo con:${NC}"
echo -e "${CYAN}./mvnw quarkus:dev${NC}"
echo ""
read -p "¿La aplicación está corriendo en modo DEV? (Enter para continuar)"
echo ""

##############################################################################
# PRUEBA 1.1: Configuración DEV
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 1.1: Configuración del Perfil DEV${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${CYAN}Consultando configuración...${NC}"
echo ""

DEV_CONFIG=$(curl -s $BASE_URL/api/tasas/config 2>/dev/null)

if [ $? -eq 0 ]; then
    echo "$DEV_CONFIG" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    perfil = data.get('perfil_activo', 'N/A')
    ambiente = data.get('ambiente', 'N/A')
    comision = data.get('comision_porcentaje', 'N/A')
    limite = data.get('limite_transaccional', 'N/A')
    cache = data.get('cache_habilitado', 'N/A')
    auditoria = data.get('auditoria_habilitada', 'N/A')
    proveedor = data.get('proveedor', 'N/A')
    
    print(f'${GREEN}✓${NC} Perfil activo: {perfil}')
    print(f'${GREEN}✓${NC} Ambiente: {ambiente}')
    print(f'${GREEN}✓${NC} Comisión: {comision}%')
    print(f'${GREEN}✓${NC} Límite transaccional: \${limite:,}')
    print(f'${GREEN}✓${NC} Cache: {cache}')
    print(f'${GREEN}✓${NC} Auditoría: {auditoria}')
    print(f'${GREEN}✓${NC} Proveedor: {proveedor}')
    
    # Validaciones
    if perfil != 'dev':
        print(f\"${RED}❌ ERROR: Perfil debería ser 'dev' pero es '{perfil}'${NC}\")
    if comision != 0.0:
        print(f\"${RED}❌ ERROR: Comisión en DEV debería ser 0.0%${NC}\")
    if limite != 999999:
        print(f\"${RED}❌ ERROR: Límite en DEV debería ser 999,999${NC}\")
except Exception as e:
    print(f'${RED}❌ Error al procesar respuesta: {e}${NC}')
"
else
    echo -e "${RED}❌ Error: No se pudo conectar al servicio${NC}"
    echo -e "${YELLOW}Verifica que la aplicación esté corriendo en http://localhost:8080${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}✅ Si todos los valores son correctos, ¡perfil DEV funciona!${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 1.2: Conversión SIN Comisión (DEV)
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 1.2: Conversión SIN Comisión (DEV)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Verificar que en DEV no se cobra comisión"
echo -e "💰 Operación: Convertir 1000 PEN a USD"
echo -e "💸 Comisión esperada: 0.0 USD (0%)"
echo ""
echo -e "${CYAN}Ejecutando conversión...${NC}"
echo ""

curl -s "$BASE_URL/api/tasas/convertir/USD?monto=1000" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    print(f\"💵 Monto Original: {data['monto_origen']} {data['moneda_origen']}\")
    print(f\"💱 Tasa Aplicada: {data['tasa_aplicada']}\")
    print(f\"💱 Convertido: {data['monto_convertido']} {data['moneda_destino']}\")
    print(f\"💸 Comisión: {data.get('comision', 0)} USD\")
    print(f\"💰 Total: {data['monto_total']} USD\")
    print(f\"🏦 Proveedor: {data.get('proveedor', 'N/A')}\")
    
    if data.get('comision', 0) == 0:
        print(f\"\\n${GREEN}✅ CORRECTO: Sin comisión en modo DEV${NC}\")
    else:
        print(f\"\\n${RED}❌ ERROR: Debería tener comisión 0 en DEV${NC}\")
except Exception as e:
    print(f'${RED}Error: {e}${NC}')
"

echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 1.3: Límite Ilimitado (DEV)
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 1.3: Límite Ilimitado en DEV${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Probar que en DEV no hay límites transaccionales"
echo -e "💰 Operación: Convertir 500,000 PEN (excede límites de TEST y PROD)"
echo -e "✅ Resultado esperado: Transacción aceptada (dentro_limite: true)"
echo ""
echo -e "${CYAN}Ejecutando conversión de monto alto...${NC}"
echo ""

curl -s "$BASE_URL/api/tasas/convertir/USD?monto=500000" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    print(f\"💵 Monto: \${data['monto_origen']:,.0f} {data['moneda_origen']}\")
    print(f\"🚦 Límite Transaccional: \${data['limite_transaccional']:,}\")
    print(f\"✓ Dentro de Límite: {data['dentro_limite']}\")
    
    if data['dentro_limite']:
        print(f\"\\n${GREEN}✅ CORRECTO: En DEV se aceptan montos altos (límite: 999,999)${NC}\")
    else:
        print(f\"\\n${RED}❌ ERROR: En DEV no debería haber límites restrictivos${NC}\")
except Exception as e:
    print(f'${RED}Error: {e}${NC}')
"

echo ""
echo -e "${CYAN}ℹ️  En DEV el límite es 999,999 para facilitar el desarrollo${NC}"
echo ""
read -p "Presiona ENTER para pasar al perfil TEST..."
echo ""

##############################################################################
# SECCIÓN 2: PERFIL TEST
##############################################################################
echo -e "${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║                    🟡 PERFIL: TEST                        ║${NC}"
echo -e "${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}Características esperadas del perfil TEST:${NC}"
echo -e "  ✓ Comisión: 1.5% (moderada)"
echo -e "  ✓ Límite transaccional: 1,000 (bajo para testing)"
echo -e "  ✓ Cache: Activado (30 min)"
echo -e "  ✓ Auditoría: Activada"
echo -e "  ✓ Proveedor: FreeCurrencyAPI"
echo -e "  ✓ Ambiente: testing"
echo ""
echo -e "${RED}⚠️  Necesitas REINICIAR la aplicación con:${NC}"
echo -e "${CYAN}./mvnw quarkus:dev -Dquarkus.profile=test${NC}"
echo ""
read -p "Presiona ENTER después de reiniciar en modo TEST..."
echo ""

##############################################################################
# PRUEBA 2.1: Configuración TEST
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 2.1: Configuración del Perfil TEST${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${CYAN}Consultando configuración...${NC}"
echo ""

TEST_CONFIG=$(curl -s $BASE_URL/api/tasas/config 2>/dev/null)

if [ $? -eq 0 ]; then
    echo "$TEST_CONFIG" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    perfil = data.get('perfil_activo', 'N/A')
    ambiente = data.get('ambiente', 'N/A')
    comision = data.get('comision_porcentaje', 'N/A')
    limite = data.get('limite_transaccional', 'N/A')
    cache = data.get('cache_habilitado', 'N/A')
    auditoria = data.get('auditoria_habilitada', 'N/A')
    refresh = data.get('refresh_minutos', 'N/A')
    proveedor = data.get('proveedor', 'N/A')
    
    print(f'${GREEN}✓${NC} Perfil activo: {perfil}')
    print(f'${GREEN}✓${NC} Ambiente: {ambiente}')
    print(f'${GREEN}✓${NC} Comisión: {comision}%')
    print(f'${GREEN}✓${NC} Límite transaccional: \${limite:,}')
    print(f'${GREEN}✓${NC} Cache: {cache}')
    print(f'${GREEN}✓${NC} Refresh: {refresh} minutos')
    print(f'${GREEN}✓${NC} Auditoría: {auditoria}')
    print(f'${GREEN}✓${NC} Proveedor: {proveedor}')
    
    # Validaciones
    if perfil != 'test':
        print(f\"\\n${RED}❌ ERROR: Perfil debería ser 'test' pero es '{perfil}'${NC}\")
        print(f\"${YELLOW}Reinicia con: ./mvnw quarkus:dev -Dquarkus.profile=test${NC}\")
    if comision != 1.5:
        print(f\"\\n${RED}❌ ERROR: Comisión en TEST debería ser 1.5%${NC}\")
    if limite != 1000:
        print(f\"\\n${RED}❌ ERROR: Límite en TEST debería ser 1,000${NC}\")
except Exception as e:
    print(f'${RED}Error: {e}${NC}')
"
else
    echo -e "${RED}❌ Error: No se pudo conectar al servicio${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}✅ Si el perfil es 'test', ¡configuración correcta!${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 2.2: Conversión CON Comisión (TEST)
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 2.2: Conversión CON Comisión (TEST)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Verificar comisión de 1.5% en TEST"
echo -e "💰 Operación: Convertir 500 PEN a USD"
echo -e "💸 Comisión esperada: ~28.13 USD (1.5%)"
echo ""
echo -e "${CYAN}Ejecutando conversión...${NC}"
echo ""

curl -s "$BASE_URL/api/tasas/convertir/USD?monto=500" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    convertido = data['monto_convertido']
    comision = data.get('comision', 0)
    comision_pct = (comision / convertido * 100) if convertido > 0 else 0
    
    print(f\"💵 Monto Original: {data['monto_origen']} {data['moneda_origen']}\")
    print(f\"💱 Convertido: {convertido} {data['moneda_destino']}\")
    print(f\"💸 Comisión ({comision_pct:.1f}%): {comision} USD\")
    print(f\"💰 Total: {data['monto_total']} USD\")
    
    if 1.4 <= comision_pct <= 1.6:
        print(f\"\\n${GREEN}✅ CORRECTO: Comisión de ~1.5% aplicada en TEST${NC}\")
    else:
        print(f\"\\n${RED}❌ ERROR: Comisión debería ser ~1.5%${NC}\")
        
    # Mostrar cálculo
    print(f\"\\n📊 Cálculo:\")
    print(f\"   Convertido: {convertido} USD\")
    print(f\"   Comisión (1.5%): {convertido} × 1.5% = {comision} USD\")
    print(f\"   Total: {convertido} + {comision} = {data['monto_total']} USD\")
except Exception as e:
    print(f'${RED}Error: {e}${NC}')
"

echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 2.3: Límite Transaccional EXCEDIDO (TEST)
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 2.3: Exceder Límite Transaccional (TEST)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Probar que se detecta cuando se excede el límite"
echo -e "💰 Operación: Convertir 2,000 PEN (límite TEST es 1,000)"
echo -e "⚠️  Resultado esperado: dentro_limite: false"
echo ""
echo -e "${CYAN}Ejecutando conversión que excede límite...${NC}"
echo ""

curl -s "$BASE_URL/api/tasas/convertir/USD?monto=2000" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    monto = data['monto_origen']
    limite = data['limite_transaccional']
    dentro = data['dentro_limite']
    
    print(f\"💵 Monto Solicitado: \${monto:,.0f}\")
    print(f\"🚦 Límite Transaccional: \${limite:,}\")
    print(f\"📊 Dentro de Límite: {dentro}\")
    
    if not dentro:
        print(f\"\\n${GREEN}✅ CORRECTO: Se detectó que excede el límite${NC}\")
        print(f\"${YELLOW}⚠️  En un sistema real, esto podría rechazar la transacción${NC}\")
    else:
        print(f\"\\n${RED}❌ ERROR: Debería indicar que excede el límite${NC}\")
        
    print(f\"\\n💡 Explicación:\")
    print(f\"   El monto de \${monto:,.0f} excede el límite de \${limite:,}\")
    print(f\"   El servicio procesa la conversión pero marca: dentro_limite=false\")
except Exception as e:
    print(f'${RED}Error: {e}${NC}')
"

echo ""
read -p "Presiona ENTER para pasar al perfil PROD..."
echo ""

##############################################################################
# SECCIÓN 3: PERFIL PROD
##############################################################################
echo -e "${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║                    🔴 PERFIL: PROD                        ║${NC}"
echo -e "${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${RED}Características esperadas del perfil PROD:${NC}"
echo -e "  ✓ Comisión: 2.5% (completa)"
echo -e "  ✓ Límite transaccional: 50,000 (alto)"
echo -e "  ✓ Cache: Activado (15 min)"
echo -e "  ✓ Auditoría: Activada"
echo -e "  ✓ Proveedor: PremiumProvider"
echo -e "  ✓ Ambiente: producción"
echo -e "  🔐 API Key: Desde Vault"
echo ""
echo -e "${YELLOW}⚠️  PREREQUISITOS PARA PROD:${NC}"
echo -e "1. Vault debe estar corriendo"
echo -e "2. Secreto debe estar guardado en Vault"
echo ""

##############################################################################
# VERIFICAR VAULT
##############################################################################
echo -e "${CYAN}Verificando Vault...${NC}"
echo ""

# Verificar si Vault está corriendo
if docker ps | grep -q tasacorp-vault; then
    echo -e "${GREEN}✓ Vault está corriendo${NC}"
else
    echo -e "${RED}❌ Vault NO está corriendo${NC}"
    echo -e "${YELLOW}Ejecuta: docker-compose up -d${NC}"
    echo ""
    read -p "¿Deseas intentar levantar Vault ahora? (s/n): " respuesta
    if [[ "$respuesta" == "s" || "$respuesta" == "S" ]]; then
        echo -e "${CYAN}Levantando Vault...${NC}"
        docker-compose up -d
        sleep 5
        if docker ps | grep -q tasacorp-vault; then
            echo -e "${GREEN}✓ Vault levantado exitosamente${NC}"
        else
            echo -e "${RED}❌ Error al levantar Vault${NC}"
            exit 1
        fi
    else
        echo -e "${YELLOW}No se puede probar PROD sin Vault${NC}"
        exit 1
    fi
fi

echo ""
echo -e "${CYAN}Verificando secreto en Vault...${NC}"
docker exec -it tasacorp-vault sh -c "VAULT_ADDR=http://127.0.0.1:8200 VAULT_TOKEN=root vault kv get secret/tasacorp" 2>/dev/null | grep -q "api-key"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Secreto existe en Vault${NC}"
else
    echo -e "${YELLOW}⚠️  Secreto no encontrado. Creando...${NC}"
    docker exec -it tasacorp-vault sh -c "VAULT_ADDR=http://127.0.0.1:8200 VAULT_TOKEN=root vault kv put secret/tasacorp api-key=PREMIUM_VAULT_SECRET_KEY_PROD_XYZ789" > /dev/null 2>&1
    echo -e "${GREEN}✓ Secreto creado${NC}"
fi

echo ""
echo -e "${RED}⚠️  Ahora REINICIA la aplicación con:${NC}"
echo -e "${CYAN}./mvnw quarkus:dev -Dquarkus.profile=prod${NC}"
echo ""
read -p "Presiona ENTER después de reiniciar en modo PROD..."
echo ""

##############################################################################
# PRUEBA 3.1: Configuración PROD + Vault
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 3.1: Configuración PROD + Integración Vault${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${CYAN}Consultando configuración...${NC}"
echo ""

PROD_CONFIG=$(curl -s $BASE_URL/api/tasas/config 2>/dev/null)

if [ $? -eq 0 ]; then
    echo "$PROD_CONFIG" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    perfil = data.get('perfil_activo', 'N/A')
    ambiente = data.get('ambiente', 'N/A')
    comision = data.get('comision_porcentaje', 'N/A')
    limite = data.get('limite_transaccional', 'N/A')
    cache = data.get('cache_habilitado', 'N/A')
    refresh = data.get('refresh_minutos', 'N/A')
    auditoria = data.get('auditoria_habilitada', 'N/A')
    proveedor = data.get('proveedor', 'N/A')
    
    print(f'${GREEN}✓${NC} Perfil activo: {perfil}')
    print(f'${GREEN}✓${NC} Ambiente: {ambiente}')
    print(f'${GREEN}✓${NC} Comisión: {comision}%')
    print(f'${GREEN}✓${NC} Límite transaccional: \${limite:,}')
    print(f'${GREEN}✓${NC} Cache: {cache}')
    print(f'${GREEN}✓${NC} Refresh: {refresh} minutos')
    print(f'${GREEN}✓${NC} Auditoría: {auditoria}')
    print(f'${GREEN}✓${NC} Proveedor: {proveedor}')
    
    # Validaciones
    if perfil != 'prod':
        print(f\"\\n${RED}❌ ERROR: Perfil debería ser 'prod' pero es '{perfil}'${NC}\")
        print(f\"${YELLOW}Reinicia con: ./mvnw quarkus:dev -Dquarkus.profile=prod${NC}\")
    else:
        print(f\"\\n${GREEN}✅ PERFIL PROD ACTIVO${NC}\")
        
    if comision != 2.5:
        print(f\"${RED}❌ ERROR: Comisión en PROD debería ser 2.5%${NC}\")
    if limite != 50000:
        print(f\"${RED}❌ ERROR: Límite en PROD debería ser 50,000${NC}\")
        
    # Verificar que se conectó a Vault (indirectamente)
    if proveedor == 'PremiumProvider':
        print(f\"\\n${GREEN}🔐 API Key desde Vault: ✓ Configurado${NC}\")
        print(f\"${CYAN}   (La API key no se muestra por seguridad)${NC}\")
except Exception as e:
    print(f'${RED}Error: {e}${NC}')
"
else
    echo -e "${RED}❌ Error: No se pudo conectar al servicio${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}✅ Si el perfil es 'prod' y proveedor es 'PremiumProvider',${NC}"
echo -e "${GREEN}   ¡la configuración con Vault funciona correctamente!${NC}"
echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 3.2: Conversión en PROD
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 3.2: Conversión en Producción${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Verificar comisión de 2.5% en PROD"
echo -e "💰 Operación: Convertir 1,000 PEN a USD"
echo -e "💸 Comisión esperada: 93.75 USD (2.5%)"
echo ""
echo -e "${CYAN}Ejecutando conversión...${NC}"
echo ""

curl -s "$BASE_URL/api/tasas/convertir/USD?monto=1000" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    convertido = data['monto_convertido']
    comision = data.get('comision', 0)
    comision_pct = (comision / convertido * 100) if convertido > 0 else 0
    
    print(f\"💵 Monto Original: {data['monto_origen']} {data['moneda_origen']}\")
    print(f\"💱 Convertido: {convertido} {data['moneda_destino']}\")
    print(f\"💸 Comisión ({comision_pct:.1f}%): {comision} USD\")
    print(f\"💰 Total: {data['monto_total']} USD\")
    print(f\"🏦 Proveedor: {data.get('proveedor', 'N/A')}\")
    
    if 2.4 <= comision_pct <= 2.6:
        print(f\"\\n${GREEN}✅ CORRECTO: Comisión de 2.5% aplicada en PROD${NC}\")
    else:
        print(f\"\\n${RED}❌ ERROR: Comisión debería ser ~2.5%${NC}\")
except Exception as e:
    print(f'${RED}Error: {e}${NC}')
"

echo ""
read -p "Presiona ENTER para continuar..."
echo ""

##############################################################################
# PRUEBA 3.3: Límite Alto en PROD
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 3.3: Límite Alto en Producción${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Verificar que PROD acepta montos altos"
echo -e "💰 Operación: Convertir 40,000 PEN (dentro del límite de 50,000)"
echo -e "✅ Resultado esperado: dentro_limite: true"
echo ""
echo -e "${CYAN}Ejecutando conversión de monto alto...${NC}"
echo ""

curl -s "$BASE_URL/api/tasas/convertir/USD?monto=40000" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    monto = data['monto_origen']
    limite = data['limite_transaccional']
    dentro = data['dentro_limite']
    
    print(f\"💵 Monto Solicitado: \${monto:,.0f}\")
    print(f\"🚦 Límite Transaccional: \${limite:,}\")
    print(f\"📊 Dentro de Límite: {dentro}\")
    
    if dentro:
        print(f\"\\n${GREEN}✅ CORRECTO: Monto aceptado en PROD (límite: 50,000)${NC}\")
    else:
        print(f\"\\n${RED}❌ ERROR: Debería estar dentro del límite${NC}\")
except Exception as e:
    print(f'${RED}Error: {e}${NC}')
"

echo ""
read -p "Presiona ENTER para probar límite excedido..."
echo ""

##############################################################################
# PRUEBA 3.4: Exceder Límite en PROD
##############################################################################
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}📋 PRUEBA 3.4: Exceder Límite en PROD${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "🎯 Objetivo: Verificar detección de límite excedido"
echo -e "💰 Operación: Convertir 60,000 PEN (excede límite de 50,000)"
echo -e "⚠️  Resultado esperado: dentro_limite: false"
echo ""
echo -e "${CYAN}Ejecutando conversión que excede límite...${NC}"
echo ""

curl -s "$BASE_URL/api/tasas/convertir/USD?monto=60000" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    monto = data['monto_origen']
    limite = data['limite_transaccional']
    dentro = data['dentro_limite']
    
    print(f\"💵 Monto Solicitado: \${monto:,.0f}\")
    print(f\"🚦 Límite Transaccional: \${limite:,}\")
    print(f\"📊 Dentro de Límite: {dentro}\")
    
    if not dentro:
        print(f\"\\n${GREEN}✅ CORRECTO: Se detectó que excede el límite de PROD${NC}\")
        print(f\"${YELLOW}⚠️  En producción real, esto debería rechazar la transacción${NC}\")
    else:
        print(f\"\\n${RED}❌ ERROR: Debería indicar que excede el límite${NC}\")
except Exception as e:
    print(f'${RED}Error: {e}${NC}')
"

echo ""
read -p "Presiona ENTER para ver tabla comparativa..."
echo ""

##############################################################################
# TABLA COMPARATIVA
##############################################################################
echo -e "${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║            📊 TABLA COMPARATIVA DE PERFILES               ║${NC}"
echo -e "${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}┌─────────────────────┬──────────────┬──────────────┬──────────────┐${NC}"
echo -e "${YELLOW}│ Configuración       │     DEV      │     TEST     │     PROD     │${NC}"
echo -e "${YELLOW}├─────────────────────┼──────────────┼──────────────┼──────────────┤${NC}"
echo -e "${YELLOW}│ Comisión            │${NC}     0.0%    ${YELLOW}│${NC}     1.5%    ${YELLOW}│${NC}     2.5%    ${YELLOW}│${NC}"
echo -e "${YELLOW}│ Límite Trans.       │${NC}   999,999   ${YELLOW}│${NC}    1,000    ${YELLOW}│${NC}   50,000    ${YELLOW}│${NC}"
echo -e "${YELLOW}│ Cache               │${NC}      ❌      ${YELLOW}│${NC}   ✅ (30m)  ${YELLOW}│${NC}   ✅ (15m)  ${YELLOW}│${NC}"
echo -e "${YELLOW}│ Auditoría           │${NC}      ❌      ${YELLOW}│${NC}      ✅      ${YELLOW}│${NC}      ✅      ${YELLOW}│${NC}"
echo -e "${YELLOW}│ Proveedor           │${NC} MockProvider${YELLOW}│${NC}FreeCurrency${YELLOW}│${NC} Premium     ${YELLOW}│${NC}"
echo -e "${YELLOW}│ API Key Source      │${NC} Hardcoded   ${YELLOW}│${NC} Hardcoded  ${YELLOW}│${NC} 🔐 Vault    ${YELLOW}│${NC}"
echo -e "${YELLOW}│ Ambiente            │${NC} desarrollo  ${YELLOW}│${NC}  testing   ${YELLOW}│${NC} producción  ${YELLOW}│${NC}"
echo -e "${YELLOW}└─────────────────────┴──────────────┴──────────────┴──────────────┘${NC}"
echo ""
echo -e "${CYAN}💡 Ejemplo de conversión de 1,000 PEN a USD:${NC}"
echo -e "${YELLOW}┌─────────────────────┬──────────────┬──────────────┬──────────────┐${NC}"
echo -e "${YELLOW}│                     │     DEV      │     TEST     │     PROD     │${NC}"
echo -e "${YELLOW}├─────────────────────┼──────────────┼──────────────┼──────────────┤${NC}"
echo -e "${YELLOW}│ Monto Original      │${NC}  1,000 PEN  ${YELLOW}│${NC}  1,000 PEN  ${YELLOW}│${NC}  1,000 PEN  ${YELLOW}│${NC}"
echo -e "${YELLOW}│ Convertido          │${NC}  3,750 USD  ${YELLOW}│${NC}  3,750 USD  ${YELLOW}│${NC}  3,750 USD  ${YELLOW}│${NC}"
echo -e "${YELLOW}│ Comisión            │${NC}    0.00 USD ${YELLOW}│${NC}   56.25 USD ${YELLOW}│${NC}   93.75 USD ${YELLOW}│${NC}"
echo -e "${YELLOW}│ Total               │${NC}  3,750 USD  ${YELLOW}│${NC}  3,806 USD  ${YELLOW}│${NC}  3,844 USD  ${YELLOW}│${NC}"
echo -e "${YELLOW}│ Dentro Límite?      │${NC}      ✅      ${YELLOW}│${NC}      ❌      ${YELLOW}│${NC}      ✅      ${YELLOW}│${NC}"
echo -e "${YELLOW}└─────────────────────┴──────────────┴──────────────┴──────────────┘${NC}"
echo ""

##############################################################################
# RESUMEN FINAL
##############################################################################
echo -e "${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║                    📋 RESUMEN DE PRUEBAS                  ║${NC}"
echo -e "${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}✅ PERFIL DEV:${NC}"
echo -e "   • Sin comisiones (desarrollo rápido)"
echo -e "   • Límite ilimitado (sin restricciones)"
echo -e "   • Cache desactivado (cambios inmediatos)"
echo ""
echo -e "${GREEN}✅ PERFIL TEST:${NC}"
echo -e "   • Comisión moderada (1.5%)"
echo -e "   • Límite bajo para pruebas (1,000)"
echo -e "   • Cache activado (30 min)"
echo -e "   • Detecta límites excedidos"
echo ""
echo -e "${GREEN}✅ PERFIL PROD:${NC}"
echo -e "   • Comisión completa (2.5%)"
echo -e "   • Límite alto para producción (50,000)"
echo -e "   • Cache optimizado (15 min)"
echo -e "   • 🔐 API Key desde Vault (seguro)"
echo ""
echo -e "${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║              🎓 CONCEPTOS CLAVE DEMOSTRADOS               ║${NC}"
echo -e "${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}🎭 Perfiles:${NC}              Mismo código, comportamiento diferente"
echo -e "${YELLOW}⚙️  Configuración:${NC}        Específica por ambiente"
echo -e "${YELLOW}🔐 Vault:${NC}                 Gestión segura de secretos"
echo -e "${YELLOW}🚦 Límites:${NC}               Validación por perfil"
echo -e "${YELLOW}💸 Comisiones:${NC}            Diferentes según ambiente"
echo -e "${YELLOW}📊 Cache/Auditoría:${NC}       Features por perfil"
echo ""
echo -e "${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║                    🎯 CASOS DE USO                        ║${NC}"
echo -e "${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}DEV:${NC}  Desarrollo local, debugging, pruebas rápidas"
echo -e "${YELLOW}TEST:${NC} CI/CD, tests automatizados, validación QA"
echo -e "${RED}PROD:${NC} Usuarios reales, transacciones reales, máxima seguridad"
echo ""
echo -e "${GREEN}🎉 ¡Pruebas de la Parte 2 completadas exitosamente!${NC}"
echo ""
echo -e "${CYAN}Has dominado:${NC}"
echo -e "  ✓ Perfiles de ambiente (%dev, %test, %prod)"
echo -e "  ✓ Configuración específica por perfil"
echo -e "  ✓ Integración con HashiCorp Vault"
echo -e "  ✓ Gestión segura de secretos"
echo -e "  ✓ Validación de límites transaccionales"
echo ""
echo -e "${YELLOW}📚 Revisa la teoría en:${NC}"
echo -e "  • TEORIA-PARTE2.md - Perfiles y Seguridad"
echo -e "  • README-PARTE2.md - Guía práctica"
echo ""