# 🏦 TasaCorp API - PARTE 2: Perfiles y Configuración Sensible

## Capítulo 7: Configuración y Perfiles en Quarkus (30 minutos)

---

## 📋 Índice

1. [Objetivo de Aprendizaje](#objetivo-de-aprendizaje)
2. [Prerequisitos](#prerequisitos)
3. [¿Qué son los Perfiles?](#qué-son-los-perfiles)
4. [Configuración por Perfil](#configuración-por-perfil)
5. [Pruebas con Diferentes Perfiles](#pruebas-con-diferentes-perfiles)
6. [Configuración Sensible con Vault](#configuración-sensible-con-vault)
7. [Integración de Vault](#integración-de-vault)
8. [Pruebas con Vault](#pruebas-con-vault)
9. [Comparativa de los 3 Perfiles](#comparativa-de-los-3-perfiles)
10. [Verificación Final](#verificación-final)

---

## 🎯 Objetivo de Aprendizaje

Al finalizar esta parte, podrás:

✅ Crear y usar perfiles de entorno (%dev, %test, %prod)  
✅ Configurar comportamientos diferentes según el perfil activo  
✅ Proteger información sensible con HashiCorp Vault  
✅ Integrar Vault en aplicaciones Quarkus  
✅ Aplicar mejores prácticas de seguridad en configuración  
✅ Entender cuándo usar cada perfil  

---

## 📦 Prerequisitos

### Completar Parte 1

✅ Proyecto `tasacorp-api` creado  
✅ Estructura de carpetas configurada  
✅ Clases Java implementadas  
✅ Configuración básica funcionando  

### Docker Desktop Instalado

#### Windows
```powershell
# Verificar Docker
docker --version
docker-compose --version

# Verificar que Docker esté corriendo
docker ps
```

#### macOS/Linux
```bash
# Verificar Docker
docker --version
docker-compose --version

# Verificar que Docker esté corriendo
docker ps
```

**Si Docker no está instalado:** [Descargar Docker Desktop](https://www.docker.com/products/docker-desktop)

---

## 🎭 ¿Qué son los Perfiles?

### Definición

Los **perfiles** (profiles) permiten tener configuraciones diferentes según el **entorno de ejecución** sin cambiar el código ni crear múltiples archivos JAR.

### Analogía: Modos de un Auto

Imagina un auto con diferentes modos de conducción:

```
🚗 MISMO AUTO, DIFERENTES MODOS:
├── 🟢 ECO (desarrollo)      → Motor suave, sin límites, consumo mínimo
├── 🟡 NORMAL (testing)      → Comportamiento balanceado
└── 🔴 SPORT (producción)    → Máximo rendimiento, límites estrictos
```

### Los 3 Perfiles de Quarkus

| Perfil | Cuándo se Activa | Uso |
|--------|------------------|-----|
| **%dev** | `./mvnw quarkus:dev` | Desarrollo local |
| **%test** | Durante tests | Pruebas automatizadas |
| **%prod** | `java -jar app.jar` | Producción |

### Ventajas

✅ **Mismo código fuente** para todos los ambientes  
✅ **Configuración específica** por ambiente  
✅ **Fácil cambio** entre ambientes  
✅ **Sin condicionales** en el código  

---

## ⚙️ Configuración por Perfil

### Sintaxis en application.properties

```properties
# Configuración BASE (aplica a todos los perfiles)
app.name=TasaCorp API

# Configuración específica de DEV
%dev.app.mode=desarrollo
%dev.app.debug=true

# Configuración específica de TEST
%test.app.mode=testing
%test.app.debug=false

# Configuración específica de PROD
%prod.app.mode=producción
%prod.app.debug=false
```

### Sintaxis en application.yaml

```yaml
# Configuración BASE
app:
  name: TasaCorp API

# Configuración por perfil
"%dev":
  app:
    mode: desarrollo
    debug: true

"%test":
  app:
    mode: testing
    debug: false

"%prod":
  app:
    mode: producción
    debug: false
```

### Configuración Completa para TasaCorp

**Actualizar `src/main/resources/application.properties`:**

```properties
# ========================================
# TasaCorp API - Configuración Base
# ========================================

# Información de la aplicación
app.name=TasaCorp API
app.version=1.0.0
app.banco=Banco TasaCorp Perú

# Configuración de tasas de cambio
tasacorp.currency.base=PEN
tasacorp.currency.supported=USD,EUR,MXN

# Límite transaccional (se sobreescribe por perfil)
tasacorp.transaction.limit=1000

# Comisión por operación (%)
tasacorp.commission.rate=2.5

# Provider de tasas (se sobreescribe por perfil)
tasacorp.provider.name=mock
tasacorp.provider.url=http://localhost:8080/mock

# ========================================
# PERFIL: %dev (Desarrollo)
# ========================================
%dev.tasacorp.provider.name=MockProvider
%dev.tasacorp.provider.url=http://localhost:8080/mock
%dev.tasacorp.transaction.limit=999999
%dev.tasacorp.commission.rate=0.0
%dev.tasacorp.provider.apikey=DEV_NO_API_KEY_NEEDED

# ========================================
# PERFIL: %test (Testing)
# ========================================
%test.tasacorp.provider.name=FreeCurrencyAPI
%test.tasacorp.provider.url=https://api.freecurrencyapi.com/v1
%test.tasacorp.transaction.limit=1000
%test.tasacorp.commission.rate=1.5
%test.tasacorp.provider.apikey=test_free_api_12345

# ========================================
# PERFIL: %prod (Producción)
# ========================================
%prod.tasacorp.provider.name=PremiumProvider
%prod.tasacorp.provider.url=https://api.currencylayer.com/live
%prod.tasacorp.transaction.limit=50000
%prod.tasacorp.commission.rate=2.5

# ========================================
# Configuración del servidor
# ========================================
quarkus.http.port=8080

# Logging
quarkus.log.level=INFO
%dev.quarkus.log.level=DEBUG
```

**Actualizar `src/main/resources/application.yaml`:**

```yaml
# ========================================
# TasaCorp API - Configuración YAML
# ========================================

tasacorp:
  exchange:
    rates:
      usd: 3.75
      eur: 4.10
      mxn: 0.22
    
  metadata:
    created-by: "Arquitectura TasaCorp"
    environment: "multi-profile"
    supported-profiles:
      - dev
      - test  
      - prod

  features:
    cache-enabled: false
    rate-refresh-minutes: 60
    audit-enabled: true

# ========================================
# Configuración por perfiles en YAML
# ========================================

"%dev":
  tasacorp:
    features:
      cache-enabled: false
      audit-enabled: false
    metadata:
      environment: "desarrollo"

"%test":
  tasacorp:
    features:
      cache-enabled: true
      rate-refresh-minutes: 30
      audit-enabled: true
    metadata:
      environment: "testing"

"%prod":
  tasacorp:
    features:
      cache-enabled: true
      rate-refresh-minutes: 15
      audit-enabled: true
    metadata:
      environment: "producción"
```

---

## 🧪 Pruebas con Diferentes Perfiles

### Prueba 1: Perfil DEV (Desarrollo)

**Características del perfil DEV:**
- ✅ Sin comisiones (0%)
- ✅ Límite transaccional ilimitado (999999)
- ✅ Logs en DEBUG
- ✅ Cache desactivado
- ✅ Auditoría desactivada
- ✅ Proveedor: MockProvider

**1. Arrancar en modo DEV:**

#### Windows
```powershell
.\mvnw.cmd quarkus:dev
```

#### macOS/Linux
```bash
./mvnw quarkus:dev
```

**2. Verificar configuración:**

#### Windows
```powershell
curl http://localhost:8080/api/tasas/config
```

#### macOS/Linux
```bash
curl http://localhost:8080/api/tasas/config | jq
```

**Resultado esperado:**
```json
{
  "perfil_activo": "dev",
  "ambiente": "desarrollo",
  "proveedor": "MockProvider",
  "proveedor_url": "http://localhost:8080/mock",
  "comision_porcentaje": 0.0,
  "limite_transaccional": 999999,
  "cache_habilitado": false,
  "auditoria_habilitada": false,
  "moneda_base": "PEN",
  "monedas_soportadas": ["USD", "EUR", "MXN"]
}
```

**3. Probar conversión (SIN comisión):**

#### Windows
```powershell
curl "http://localhost:8080/api/tasas/convertir/USD?monto=1000"
```

#### macOS/Linux
```bash
curl "http://localhost:8080/api/tasas/convertir/USD?monto=1000" | jq
```

**Resultado:**
```json
{
  "monto_origen": 1000.0,
  "monto_convertido": 3750.0,
  "comision": 0.0,
  "monto_total": 3750.0,
  "proveedor": "MockProvider",
  "limite_transaccional": 999999,
  "dentro_limite": true
}
```

> 💡 **Observación:** Sin comisión (perfecto para desarrollo)

---

### Prueba 2: Perfil TEST (Testing)

**Características del perfil TEST:**
- ✅ Comisión moderada (1.5%)
- ✅ Límite transaccional de prueba ($1000)
- ✅ Cache activado
- ✅ Auditoría activada
- ✅ Proveedor: FreeCurrencyAPI

**1. Para el servidor** (Ctrl+C)

**2. Arrancar en modo TEST:**

#### Windows
```powershell
.\mvnw.cmd quarkus:dev -Dquarkus.profile=test
```

#### macOS/Linux
```bash
./mvnw quarkus:dev -Dquarkus.profile=test
```

**3. Verificar configuración:**

#### Windows
```powershell
curl http://localhost:8080/api/tasas/config
```

#### macOS/Linux
```bash
curl http://localhost:8080/api/tasas/config | jq
```

**Resultado esperado:**
```json
{
  "perfil_activo": "test",
  "ambiente": "testing",
  "proveedor": "FreeCurrencyAPI",
  "proveedor_url": "https://api.freecurrencyapi.com/v1",
  "comision_porcentaje": 1.5,
  "limite_transaccional": 1000,
  "cache_habilitado": true,
  "auditoria_habilitada": true,
  "refresh_minutos": 30
}
```

**4. Probar conversión (CON comisión 1.5%):**

#### Windows
```powershell
curl "http://localhost:8080/api/tasas/convertir/USD?monto=500"
```

#### macOS/Linux
```bash
curl "http://localhost:8080/api/tasas/convertir/USD?monto=500" | jq
```

**Resultado:**
```json
{
  "monto_origen": 500.0,
  "monto_convertido": 1875.0,
  "comision": 28.125,
  "monto_total": 1903.125,
  "dentro_limite": true
}
```

> 💡 **Observación:** Comisión = 1875 × 1.5% = 28.125

**5. Probar límite transaccional (EXCEDER límite):**

#### Windows
```powershell
curl "http://localhost:8080/api/tasas/convertir/USD?monto=2000"
```

#### macOS/Linux
```bash
curl "http://localhost:8080/api/tasas/convertir/USD?monto=2000" | jq
```

**Resultado:**
```json
{
  "monto_origen": 2000.0,
  "monto_convertido": 7500.0,
  "comision": 112.5,
  "monto_total": 7612.5,
  "limite_transaccional": 1000,
  "dentro_limite": false
}
```

> ⚠️ **Observación:** `dentro_limite: false` - Excede el límite de $1000

**6. Revisar logs:**

En la consola del servidor deberías ver:
```
WARN  [pe.ban.tas.ser.TasaService] Monto 2000.00 excede el límite transaccional de 1000
```

---

### Prueba 3: Perfil PROD (sin Vault aún)

**Características del perfil PROD:**
- ✅ Comisión productiva (2.5%)
- ✅ Límite transaccional alto ($50,000)
- ✅ Cache activado
- ✅ Auditoría activada
- ✅ Proveedor: PremiumProvider
- ⚠️ API key requerida (la configuraremos con Vault)

**Nota:** Por ahora, no arrancaremos PROD porque requiere Vault. Lo haremos en la siguiente sección.

---

## 🔐 Configuración Sensible con Vault

### ¿Qué es HashiCorp Vault?

**HashiCorp Vault** es una herramienta para gestionar secretos y proteger datos sensibles.

### Analogía: Caja Fuerte Bancaria

```
┌─────────────────────────────────────┐
│     🏦 BANCO (Tu aplicación)        │
│                                      │
│  ❌ Dinero en el escritorio          │ ← Secretos en properties
│     (cualquiera lo ve)               │
│                                      │
│  ✅ Dinero en la caja fuerte         │ ← Secretos en Vault
│     (solo quien tiene la llave)      │
└─────────────────────────────────────┘
```

### ¿Por qué Usar Vault?

#### ❌ Problemas SIN Vault

```properties
# application.properties (EN GIT)
database.password=super_secret_123  # ❌ Expuesto en git
api.key=sk_live_ABC123XYZ789        # ❌ Cualquiera lo ve
```

**Riesgos:**
- Secretos en historial de git (permanentes)
- Difíciles de rotar (requiere redeploy)
- Sin auditoría de accesos
- Expuestos a todo el equipo

#### ✅ Ventajas CON Vault

```properties
# application.properties
database.password=${db-password}  # ✅ Referencia a Vault
api.key=${api-key}                # ✅ Secreto protegido
```

**Beneficios:**
- 🔒 Secretos encriptados
- 🔄 Rotación sin redeploy
- 📊 Auditoría de accesos
- 🎯 Control de acceso granular
- ⏰ Secretos con tiempo de vida

---

## 🐳 Integración de Vault

### Paso 1: Crear docker-compose.yml

**Ubicación:** Raíz del proyecto

#### Windows (PowerShell)
```powershell
@"
version: '3.8'

services:
  vault:
    image: hashicorp/vault:1.15.2
    container_name: tasacorp-vault
    ports:
      - "8200:8200"
    environment:
      VAULT_DEV_ROOT_TOKEN_ID: root
      VAULT_DEV_LISTEN_ADDRESS: 0.0.0.0:8200
    cap_add:
      - IPC_LOCK
    command: server -dev
    healthcheck:
      test: ["CMD", "vault", "status"]
      interval: 10s
      timeout: 5s
      retries: 3
    networks:
      - tasacorp-network

networks:
  tasacorp-network:
    driver: bridge
"@ | Out-File -FilePath docker-compose.yml -Encoding UTF8
```

#### macOS/Linux
```bash
cat > docker-compose.yml << 'EOF'
version: '3.8'

services:
  vault:
    image: hashicorp/vault:1.15.2
    container_name: tasacorp-vault
    ports:
      - "8200:8200"
    environment:
      VAULT_DEV_ROOT_TOKEN_ID: root
      VAULT_DEV_LISTEN_ADDRESS: 0.0.0.0:8200
    cap_add:
      - IPC_LOCK
    command: server -dev
    healthcheck:
      test: ["CMD", "vault", "status"]
      interval: 10s
      timeout: 5s
      retries: 3
    networks:
      - tasacorp-network

networks:
  tasacorp-network:
    driver: bridge
EOF
```

### Paso 2: Levantar Vault

#### Windows
```powershell
docker-compose up -d

# Verificar que esté corriendo
docker ps | Select-String vault
```

#### macOS/Linux
```bash
docker-compose up -d

# Verificar que esté corriendo
docker ps | grep vault
```

**Deberías ver:**
```
tasacorp-vault   hashicorp/vault:1.15.2   ...   Up   0.0.0.0:8200->8200/tcp
```

### Paso 3: Guardar Secreto en Vault

#### Windows
```powershell
docker exec -it tasacorp-vault sh -c "VAULT_ADDR=http://127.0.0.1:8200 VAULT_TOKEN=root vault kv put secret/tasacorp api-key=PREMIUM_VAULT_SECRET_KEY_PROD_XYZ789"
```

#### macOS/Linux
```bash
docker exec -it tasacorp-vault sh -c "VAULT_ADDR=http://127.0.0.1:8200 VAULT_TOKEN=root vault kv put secret/tasacorp api-key=PREMIUM_VAULT_SECRET_KEY_PROD_XYZ789"
```

**Resultado esperado:**
```
==== Secret Path ====
secret/data/tasacorp

======= Metadata =======
Key                Value
---                -----
created_time       2025-10-19T...
version            1
```

### Paso 4: Verificar Secreto

#### Windows
```powershell
docker exec -it tasacorp-vault sh -c "VAULT_ADDR=http://127.0.0.1:8200 VAULT_TOKEN=root vault kv get secret/tasacorp"
```

#### macOS/Linux
```bash
docker exec -it tasacorp-vault sh -c "VAULT_ADDR=http://127.0.0.1:8200 VAULT_TOKEN=root vault kv get secret/tasacorp"
```

**Deberías ver:**
```
===== Data =====
Key        Value
---        -----
api-key    PREMIUM_VAULT_SECRET_KEY_PROD_XYZ789
```

### Paso 5: Configurar Quarkus para Vault

**Agregar al final de `application.properties`:**

```properties
# ========================================
# Configuración de Vault (solo PROD)
# ========================================
%prod.quarkus.vault.url=http://localhost:8200
%prod.quarkus.vault.authentication.client-token=root
%prod.quarkus.vault.secret-config-kv-path=tasacorp
%prod.quarkus.vault.kv-secret-engine-version=2

# API key viene de Vault
%prod.tasacorp.provider.apikey=${api-key}
```

### 📌 Explicación de la Configuración

| Propiedad | Valor | Descripción |
|-----------|-------|-------------|
| `quarkus.vault.url` | `http://localhost:8200` | URL de Vault |
| `quarkus.vault.authentication.client-token` | `root` | Token de acceso (DEV mode) |
| `quarkus.vault.secret-config-kv-path` | `tasacorp` | Path del secreto |
| `quarkus.vault.kv-secret-engine-version` | `2` | Versión del KV engine |
| `tasacorp.provider.apikey` | `${api-key}` | Referencia al secreto |

---

## 🧪 Pruebas con Vault

### Prueba 1: Arrancar PROD con Vault

**1. Asegurarse de que Vault esté corriendo:**

#### Windows
```powershell
docker ps | Select-String vault
```

#### macOS/Linux
```bash
docker ps | grep vault
```

**2. Arrancar en modo PROD:**

#### Windows
```powershell
.\mvnw.cmd quarkus:dev -Dquarkus.profile=prod
```

#### macOS/Linux
```bash
./mvnw quarkus:dev -Dquarkus.profile=prod
```

**3. Verificar logs - Deberías ver:**
```
INFO  [io.quarkus] Profile prod activated
INFO  [io.quarkus] tasacorp-api started in X.XXXs
```

**4. Consultar configuración:**

#### Windows
```powershell
curl http://localhost:8080/api/tasas/config
```

#### macOS/Linux
```bash
curl http://localhost:8080/api/tasas/config | jq
```

**Resultado esperado:**
```json
{
  "perfil_activo": "prod",
  "ambiente": "producción",
  "proveedor": "PremiumProvider",
  "proveedor_url": "https://api.currencylayer.com/live",
  "comision_porcentaje": 2.5,
  "limite_transaccional": 50000,
  "cache_habilitado": true,
  "auditoria_habilitada": true,
  "refresh_minutos": 15
}
```

> ✅ **¡La API key se leyó desde Vault!** (aunque no se muestra por seguridad)

### Prueba 2: Conversión en PROD

#### Windows
```powershell
curl "http://localhost:8080/api/tasas/convertir/USD?monto=1000"
```

#### macOS/Linux
```bash
curl "http://localhost:8080/api/tasas/convertir/USD?monto=1000" | jq
```

**Resultado:**
```json
{
  "monto_origen": 1000.0,
  "monto_convertido": 3750.0,
  "comision": 93.75,
  "monto_total": 3843.75,
  "proveedor": "PremiumProvider",
  "limite_transaccional": 50000,
  "dentro_limite": true
}
```

> 💡 **Observación:** Comisión = 3750 × 2.5% = 93.75

### Prueba 3: Probar Límite de PROD

#### Windows
```powershell
curl "http://localhost:8080/api/tasas/convertir/USD?monto=60000"
```

#### macOS/Linux
```bash
curl "http://localhost:8080/api/tasas/convertir/USD?monto=60000" | jq
```

**Resultado:**
```json
{
  "monto_origen": 60000.0,
  "monto_convertido": 225000.0,
  "comision": 5625.0,
  "monto_total": 230625.0,
  "limite_transaccional": 50000,
  "dentro_limite": false
}
```

> ⚠️ **Observación:** Excede el límite de producción ($50,000)

**En los logs verás:**
```
WARN  Monto 60000.00 excede el límite transaccional de 50000
```

---

## 📊 Comparativa de los 3 Perfiles

### Tabla Comparativa Completa

| Configuración | DEV | TEST | PROD |
|---------------|-----|------|------|
| **Proveedor** | MockProvider | FreeCurrencyAPI | PremiumProvider |
| **URL** | localhost:8080/mock | freecurrencyapi.com/v1 | currencylayer.com/live |
| **Comisión** | 0.0% (gratis) | 1.5% (moderado) | 2.5% (completo) |
| **Límite Trans.** | 999,999 (ilimitado) | 1,000 (bajo) | 50,000 (alto) |
| **Cache** | ❌ Desactivado | ✅ Activado (30min) | ✅ Activado (15min) |
| **Auditoría** | ❌ Desactivada | ✅ Activada | ✅ Activada |
| **Log Level** | DEBUG | INFO | INFO |
| **API Key** | Hardcoded | Hardcoded | 🔐 Vault |

### Cálculo de Comisiones

**Conversión de $1000 PEN a USD (tasa 3.75):**

| Perfil | Convertido | Comisión | Total | Límite OK |
|--------|-----------|----------|-------|-----------|
| **DEV** | 3,750 USD | 0.00 USD | 3,750.00 USD | ✅ Sí |
| **TEST** | 3,750 USD | 56.25 USD | 3,806.25 USD | ❌ No (excede $1K) |
| **PROD** | 3,750 USD | 93.75 USD | 3,843.75 USD | ✅ Sí |

### Casos de Uso por Perfil

#### %dev - Desarrollo Local

**Uso:**
- Desarrollo de nuevas features
- Debugging
- Pruebas manuales rápidas

**Características:**
- 🚀 Máxima velocidad
- 🔍 Logs detallados
- 💵 Sin costos (sin comisiones)
- 🔓 Sin restricciones

#### %test - Testing / QA

**Uso:**
- Tests automatizados
- Tests de integración
- Validación de QA

**Características:**
- ⚖️ Comportamiento balanceado
- 🎯 Límites realistas
- 📊 Auditoría activada
- 🧪 Configuración de prueba

#### %prod - Producción

**Uso:**
- Ambiente productivo
- Usuarios reales
- Transacciones reales

**Características:**
- 🔒 Máxima seguridad
- 💰 Comisiones reales
- 🚨 Límites estrictos
- 📈 Monitoreo completo

---

## ✅ Verificación Final

### Checklist de Funcionalidad

Antes de dar por completada la Parte 2, verifica:

- [ ] **DEV funciona**
  - [ ] Arranca con `./mvnw quarkus:dev`
  - [ ] Comisión es 0.0%
  - [ ] Límite es 999999
  - [ ] Cache desactivado
  
- [ ] **TEST funciona**
  - [ ] Arranca con `-Dquarkus.profile=test`
  - [ ] Comisión es 1.5%
  - [ ] Límite es 1000
  - [ ] Cache activado
  - [ ] Detecta límite excedido

- [ ] **PROD funciona**
  - [ ] Vault está corriendo (`docker ps`)
  - [ ] Secreto guardado en Vault
  - [ ] Arranca con `-Dquarkus.profile=prod`
  - [ ] Comisión es 2.5%
  - [ ] Límite es 50000
  - [ ] Lee API key desde Vault

### Script de Verificación Rápida

#### Windows (PowerShell)
```powershell
# Test DEV
Write-Host "Probando DEV..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "mvn quarkus:dev"
Start-Sleep -Seconds 10
$devResult = Invoke-WebRequest -Uri "http://localhost:8080/api/tasas/config" | ConvertFrom-Json
Write-Host "DEV - Perfil: $($devResult.perfil_activo), Comisión: $($devResult.comision_porcentaje)%"

# Test TEST
Write-Host "Probando TEST..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "mvn quarkus:dev -Dquarkus.profile=test"
Start-Sleep -Seconds 10
$testResult = Invoke-WebRequest -Uri "http://localhost:8080/api/tasas/config" | ConvertFrom-Json
Write-Host "TEST - Perfil: $($testResult.perfil_activo), Comisión: $($testResult.comision_porcentaje)%"

# Test PROD
Write-Host "Probando PROD..." -ForegroundColor Red
docker-compose up -d
Start-Sleep -Seconds 5
Start-Process powershell -ArgumentList "-NoExit", "-Command", "mvn quarkus:dev -Dquarkus.profile=prod"
Start-Sleep -Seconds 10
$prodResult = Invoke-WebRequest -Uri "http://localhost:8080/api/tasas/config" | ConvertFrom-Json
Write-Host "PROD - Perfil: $($prodResult.perfil_activo), Comisión: $($prodResult.comision_porcentaje)%"
```

#### macOS/Linux
```bash
#!/bin/bash

echo "🧪 Verificando los 3 perfiles..."

# DEV
echo -e "\n✅ Probando DEV..."
./mvnw quarkus:dev &
sleep 10
curl -s http://localhost:8080/api/tasas/config | jq '.perfil_activo, .comision_porcentaje'
pkill -f quarkus:dev

# TEST
echo -e "\n✅ Probando TEST..."
./mvnw quarkus:dev -Dquarkus.profile=test &
sleep 10
curl -s http://localhost:8080/api/tasas/config | jq '.perfil_activo, .comision_porcentaje'
pkill -f quarkus:dev

# PROD
echo -e "\n✅ Probando PROD..."
docker-compose up -d
sleep 5
./mvnw quarkus:dev -Dquarkus.profile=prod &
sleep 10
curl -s http://localhost:8080/api/tasas/config | jq '.perfil_activo, .comision_porcentaje'
pkill -f quarkus:dev

echo -e "\n✅ Verificación completa!"
```

---

## 🚨 Troubleshooting

### Error: "Could not expand value api-key"

**Síntoma:**
```
SRCFG00011: Could not expand value api-key in property tasacorp.provider.apikey
```

**Solución:**
1. Verificar que Vault esté corriendo:
   ```bash
   docker ps | grep vault
   ```

2. Verificar que el secreto existe:
   ```bash
   docker exec -it tasacorp-vault sh -c "VAULT_ADDR=http://127.0.0.1:8200 VAULT_TOKEN=root vault kv get secret/tasacorp"
   ```

3. Verificar la configuración en `application.properties`:
   ```properties
   %prod.quarkus.vault.secret-config-kv-path=tasacorp
   ```

### Error: Vault no arranca

**Síntoma:**
```
docker-compose up -d
ERROR: ...
```

**Solución:**
1. Verificar que Docker esté corriendo
2. Verificar puerto 8200 disponible:
   
   **Windows:**
   ```powershell
   Get-NetTCPConnection -LocalPort 8200
   ```
   
   **macOS/Linux:**
   ```bash
   lsof -i :8200
   ```

3. Si está ocupado, cambiar puerto en `docker-compose.yml`:
   ```yaml
   ports:
     - "8201:8200"  # Usar 8201 en lugar de 8200
   ```

### Error: "Profile not found"

**Síntoma:**
```
El perfil test/prod no se activa
```

**Solución:**
Verificar sintaxis del comando:

**Windows:**
```powershell
.\mvnw.cmd quarkus:dev -Dquarkus.profile=test
```

**macOS/Linux:**
```bash
./mvnw quarkus:dev -Dquarkus.profile=test
```

**Nota:** El argumento `-D` debe ir **ANTES** de `quarkus:dev`

---

## 🎓 Ejercicios Adicionales

### Ejercicio 1: Agregar Perfil "staging"

**Objetivo:** Crear un cuarto perfil para ambiente de staging.

1. Agregar en `application.properties`:
```properties
%staging.tasacorp.provider.name=StagingProvider
%staging.tasacorp.transaction.limit=10000
%staging.tasacorp.commission.rate=2.0
```

2. Probar:
```bash
./mvnw quarkus:dev -Dquarkus.profile=staging
```

### Ejercicio 2: Vault con Múltiples Secretos

**Objetivo:** Guardar múltiples secretos en Vault.

1. Guardar secretos adicionales:
```bash
docker exec -it tasacorp-vault sh -c "VAULT_ADDR=http://127.0.0.1:8200 VAULT_TOKEN=root vault kv put secret/tasacorp \
  api-key=PREMIUM_KEY \
  db-password=secret123 \
  smtp-password=email456"
```

2. Configurar en properties:
```properties
%prod.tasacorp.provider.apikey=${api-key}
%prod.database.password=${db-password}
%prod.email.password=${smtp-password}
```

### Ejercicio 3: Variables de Entorno por Perfil

**Objetivo:** Combinar perfiles con variables de entorno.

```bash
# Sobrescribir comisión en TEST
TASACORP_COMMISSION_RATE=3.0 ./mvnw quarkus:dev -Dquarkus.profile=test

# Debería usar 3.0% en lugar de 1.5%
```

---

## 📚 Mejores Prácticas

### 1. Nunca Secretos en Git

```properties
# ❌ NUNCA hacer esto
database.password=my_secret_password

# ✅ SIEMPRE hacer esto
database.password=${DB_PASSWORD}
```

### 2. Documentar Perfiles

```properties
# ========================================
# PERFIL: %dev (Desarrollo Local)
# Propósito: Desarrollo rápido sin restricciones
# Características:
#   - Sin comisiones
#   - Límites ilimitados
#   - Logs DEBUG
# ========================================
```

### 3. Validar Configuración al Arranque

```java
@ApplicationScoped
public class ConfigValidator {
    
    @Inject
    TasaCorpConfig config;
    
    void validateOnStartup(@Observes StartupEvent event) {
        if (config.commission().rate() < 0) {
            throw new IllegalStateException("Commission rate cannot be negative");
        }
    }
}
```

### 4. Usar Perfiles Específicos en CI/CD

```yaml
# .github/workflows/test.yml
- name: Run Tests
  run: mvn test -Dquarkus.profile=test
```

---

## 🎉 ¡Felicitaciones!

Has completado exitosamente la **Parte 2 del Capítulo 7**.

### Lo que Aprendiste

✅ Crear y usar perfiles (%dev, %test, %prod)  
✅ Configurar comportamientos diferentes por ambiente  
✅ Integrar HashiCorp Vault para secretos  
✅ Proteger información sensible  
✅ Aplicar mejores prácticas de configuración  

### Siguiente Paso

Continúa con: **[TEORIA-PARTE2.md](TEORIA-PARTE2.md)** - Teoría Profunda de Perfiles y Seguridad

---

## 📖 Recursos Adicionales

- [Quarkus Profiles](https://quarkus.io/guides/config-reference#profiles)
- [HashiCorp Vault](https://www.vaultproject.io/)
- [Quarkus Vault Extension](https://quarkus.io/guides/vault)
- [12-Factor App: Config](https://12factor.net/config)

