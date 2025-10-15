# Setup Always Encrypted con Azure Key Vault

**Guía paso a paso para configurar Always Encrypted con Azure Key Vault (funciona en cualquier plataforma)**

---

## 📋 Requisitos Previos

- ✅ Cuenta de Azure (gratis: $200 USD de crédito por 30 días)
- ✅ Azure CLI instalado
- ✅ SQL Server (Docker o local)
- ✅ Proyecto Quarkus ya creado

---

## 🎯 Ventajas de Azure Key Vault

- ✅ Funciona en **cualquier plataforma** (Windows/Mac/Linux)
- ✅ No requiere certificados locales
- ✅ Gestión centralizada de claves
- ✅ Auditoría completa
- ✅ Alta disponibilidad
- ✅ Rotación de claves automatizada

---

## PARTE 1: Configurar Azure

### Paso 1.1: Crear Cuenta Azure (si no tienes)

1. Ir a: https://azure.microsoft.com/free/
2. Clic en "Start free"
3. Crear cuenta con email
4. **$200 USD gratis** por 30 días

---

### Paso 1.2: Instalar Azure CLI

**macOS:**
```bash
brew install azure-cli
```

**Windows:**
```powershell
# Descargar desde:
# https://aka.ms/installazurecliwindows
```

**Linux:**
```bash
curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash
```

**Verificar:**
```bash
az --version
```

---

### Paso 1.3: Iniciar Sesión en Azure

```bash
az login
```

Se abrirá el navegador. Inicia sesión con tu cuenta Azure.

---

### Paso 1.4: Crear Resource Group

```bash
az group create \
  --name rg-always-encrypted \
  --location eastus
```

---

### Paso 1.5: Crear Key Vault

```bash
az keyvault create \
  --name kv-clientes-seguros \
  --resource-group rg-always-encrypted \
  --location eastus \
  --enable-purge-protection \
  --retention-days 90
```

**Nota:** El nombre del Key Vault debe ser **único globalmente**. Si `kv-clientes-seguros` está tomado, usa otro nombre (ej: `kv-clientes-seguros-123`).

---

### Paso 1.6: Crear Service Principal (Identidad de la App)

```bash
az ad sp create-for-rbac \
  --name sp-clientes-seguros \
  --role "Key Vault Crypto User" \
  --scopes /subscriptions/$(az account show --query id -o tsv)/resourceGroups/rg-always-encrypted/providers/Microsoft.KeyVault/vaults/kv-clientes-seguros
```

**Guarda el output:**
```json
{
  "appId": "12345678-1234-1234-1234-123456789012",
  "displayName": "sp-clientes-seguros",
  "password": "abcdefgh-1234-5678-90ab-cdefghijklmn",
  "tenant": "87654321-4321-4321-4321-210987654321"
}
```

**Importante:** Guarda estos valores:
- `appId` → **Client ID**
- `password` → **Client Secret**
- `tenant` → **Tenant ID**

---

### Paso 1.7: Dar Permisos al Service Principal

```bash
az keyvault set-policy \
  --name kv-clientes-seguros \
  --spn 12345678-1234-1234-1234-123456789012 \
  --key-permissions get list create encrypt decrypt unwrapKey wrapKey
```

**Reemplaza el SPID con tu `appId` del paso anterior.**

---

### Paso 1.8: Crear Key en Azure Key Vault

```bash
az keyvault key create \
  --vault-name kv-clientes-seguros \
  --name CMK-AlwaysEncrypted \
  --protection software \
  --kty RSA \
  --size 2048
```

**Output:**
```json
{
  "key": {
    "kid": "https://kv-clientes-seguros.vault.azure.net/keys/CMK-AlwaysEncrypted/abc123...",
    ...
  }
}
```

**Guarda el `kid` (Key ID).** Lo necesitarás después.

---

## PARTE 2: Configurar SQL Server

### Paso 2.1: Conectarse a SQL Server

```bash
# Docker
docker exec -it sqlserver /opt/mssql-tools18/bin/sqlcmd \
  -S localhost -U sa -P 'Pass123!Admin' -C

# O local
sqlcmd -S localhost -E -C
```

---

### Paso 2.2: Crear Column Master Key (CMK)

```sql
USE BancoDB;
GO

CREATE COLUMN MASTER KEY [CMK_Azure]
WITH (
    KEY_STORE_PROVIDER_NAME = 'AZURE_KEY_VAULT',
    KEY_PATH = 'https://kv-clientes-seguros.vault.azure.net/keys/CMK-AlwaysEncrypted/abc123...'
);
GO
```

**⚠️ Importante:** Usa el `kid` completo de tu key del Paso 1.8.

---

### Paso 2.3: Crear Column Encryption Key (CEK)

```sql
CREATE COLUMN ENCRYPTION KEY [CEK_Clientes_Azure]
WITH VALUES (
    COLUMN_MASTER_KEY = [CMK_Azure],
    ALGORITHM = 'RSA_OAEP'
);
GO
```

---

### Paso 2.4: Recrear Tabla con Columnas Cifradas

```sql
-- Renombrar tabla existente
EXEC sp_rename 'Cliente', 'Cliente_Old';
GO

-- Crear nueva tabla con cifrado
CREATE TABLE Cliente (
    id BIGINT NOT NULL PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    numero_tarjeta VARCHAR(255) 
        COLLATE Latin1_General_BIN2
        ENCRYPTED WITH (
            COLUMN_ENCRYPTION_KEY = [CEK_Clientes_Azure],
            ENCRYPTION_TYPE = Deterministic,
            ALGORITHM = 'AEAD_AES_256_CBC_HMAC_SHA_256'
        ) NOT NULL,
    email VARCHAR(255)
        COLLATE Latin1_General_BIN2
        ENCRYPTED WITH (
            COLUMN_ENCRYPTION_KEY = [CEK_Clientes_Azure],
            ENCRYPTION_TYPE = Randomized,
            ALGORITHM = 'AEAD_AES_256_CBC_HMAC_SHA_256'
        ) NOT NULL,
    telefono VARCHAR(255) NOT NULL
);
GO

-- Copiar datos
SET IDENTITY_INSERT Cliente ON;
INSERT INTO Cliente (id, nombre, numero_tarjeta, email, telefono)
SELECT id, nombre, numero_tarjeta, email, telefono
FROM Cliente_Old;
SET IDENTITY_INSERT Cliente OFF;
GO

-- Eliminar tabla vieja
DROP TABLE Cliente_Old;
GO
```

---

## PARTE 3: Configurar Quarkus

### Paso 3.1: Agregar Dependencia Azure

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.microsoft.sqlserver</groupId>
    <artifactId>mssql-jdbc</artifactId>
    <version>12.4.2.jre11</version>
</dependency>

<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-security-keyvault-keys</artifactId>
    <version>4.7.0</version>
</dependency>

<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-identity</artifactId>
    <version>1.11.0</version>
</dependency>
```

---

### Paso 3.2: Configurar application.properties

```properties
# SQL Server con Always Encrypted + Azure Key Vault
quarkus.datasource.db-kind=mssql
quarkus.datasource.username=sa
quarkus.datasource.password=Pass123!Admin
quarkus.datasource.jdbc.url=jdbc:sqlserver://localhost:1433;\
  databaseName=BancoDB;\
  encrypt=false;\
  trustServerCertificate=true;\
  columnEncryptionSetting=Enabled;\
  keyStoreAuthentication=KeyVaultClientSecret;\
  keyStorePrincipalId=12345678-1234-1234-1234-123456789012;\
  keyStoreSecret=abcdefgh-1234-5678-90ab-cdefghijklmn

# Azure Key Vault
azure.keyvault.client-id=12345678-1234-1234-1234-123456789012
azure.keyvault.client-secret=abcdefgh-1234-5678-90ab-cdefghijklmn
azure.keyvault.tenant-id=87654321-4321-4321-4321-210987654321
azure.keyvault.vault-url=https://kv-clientes-seguros.vault.azure.net/

# Hibernate
quarkus.hibernate-orm.database.generation=none
quarkus.hibernate-orm.log.sql=true

# HTTP
quarkus.http.port=8080
```

**⚠️ Importante:** Reemplaza con tus valores del Paso 1.6.

---

### Paso 3.3: Configurar Variables de Entorno (Más Seguro)

En lugar de poner secrets en `application.properties`:

**macOS/Linux:**
```bash
export AZURE_CLIENT_ID=12345678-1234-1234-1234-123456789012
export AZURE_CLIENT_SECRET=abcdefgh-1234-5678-90ab-cdefghijklmn
export AZURE_TENANT_ID=87654321-4321-4321-4321-210987654321
```

**Windows:**
```powershell
$env:AZURE_CLIENT_ID="12345678-1234-1234-1234-123456789012"
$env:AZURE_CLIENT_SECRET="abcdefgh-1234-5678-90ab-cdefghijklmn"
$env:AZURE_TENANT_ID="87654321-4321-4321-4321-210987654321"
```

**application.properties:**
```properties
quarkus.datasource.jdbc.url=jdbc:sqlserver://localhost:1433;\
  databaseName=BancoDB;\
  encrypt=false;\
  trustServerCertificate=true;\
  columnEncryptionSetting=Enabled;\
  keyStoreAuthentication=KeyVaultClientSecret;\
  keyStorePrincipalId=${AZURE_CLIENT_ID};\
  keyStoreSecret=${AZURE_CLIENT_SECRET}

azure.keyvault.client-id=${AZURE_CLIENT_ID}
azure.keyvault.client-secret=${AZURE_CLIENT_SECRET}
azure.keyvault.tenant-id=${AZURE_TENANT_ID}
azure.keyvault.vault-url=https://kv-clientes-seguros.vault.azure.net/
```

---

## PARTE 4: Probar

### Paso 4.1: Ejecutar Quarkus

```bash
./mvnw quarkus:dev
```

---

### Paso 4.2: Insertar Cliente

```bash
curl -X POST http://localhost:8080/api/v1/clientes \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Ana López Azure",
    "numeroTarjeta": "6011-1111-2222-3333",
    "email": "ana.lopez@banco.com",
    "telefono": "+56955667788"
  }'
```

**Resultado:**
```json
{
  "id": 5,
  "nombre": "Ana López Azure",
  "numeroTarjeta": "6011-1111-2222-3333",
  "email": "ana.lopez@banco.com",
  "telefono": "+56955667788"
}
```

---

### Paso 4.3: Verificar Cifrado

```bash
# Conectar a SQL Server SIN Always Encrypted habilitado
sqlcmd -S localhost -U sa -P 'Pass123!Admin' -C

USE BancoDB;
GO

SELECT * FROM Cliente WHERE id = 5;
GO
```

**Verás datos binarios cifrados.** ✅

---

## PARTE 5: Monitoreo y Auditoría

### Paso 5.1: Ver Accesos en Azure Portal

1. Ir a: https://portal.azure.com
2. Buscar tu Key Vault: `kv-clientes-seguros`
3. Ir a: **Monitoring → Logs**
4. Query:
```kusto
AzureDiagnostics
| where ResourceProvider == "MICROSOFT.KEYVAULT"
| where OperationName == "VaultGet" or OperationName == "Decrypt"
| project TimeGenerated, OperationName, CallerIPAddress, ResultType
```

Verás todos los accesos a la key.

---

### Paso 5.2: Configurar Alertas

1. En Key Vault → **Monitoring → Alerts**
2. **New alert rule**
3. Condición: "Cuando haya más de X accesos fallidos en Y minutos"
4. Acción: Enviar email

---

## PARTE 6: Rotación de Claves (Avanzado)

### Paso 6.1: Crear Nueva Versión de la Key

```bash
az keyvault key create \
  --vault-name kv-clientes-seguros \
  --name CMK-AlwaysEncrypted \
  --protection software
```

Azure Key Vault crea automáticamente una **nueva versión** de la misma key.

---

### Paso 6.2: Actualizar CEK en SQL Server

```sql
ALTER COLUMN ENCRYPTION KEY [CEK_Clientes_Azure]
ADD VALUE (
    COLUMN_MASTER_KEY = [CMK_Azure],
    ALGORITHM = 'RSA_OAEP',
    ENCRYPTED_VALUE = <nuevo_valor>
)
WITH (DROP OLD VALUE);
GO
```

**Nota:** Este es un proceso avanzado. Consulta documentación de Microsoft para detalles.

---

## 💰 Costos de Azure Key Vault

### Pricing (región East US):

| Operación | Costo |
|-----------|-------|
| Key Vault (mensual) | $0.03 / 10,000 operaciones |
| Key operations | $0.03 / 10,000 operaciones |
| Storage | ~$0.10 / mes por key |

**Ejemplo:**
- 1,000,000 operaciones/mes = **$3 USD**
- Para desarrollo: **<$1 USD/mes**

**Free tier:** $200 USD crédito = Gratis por meses.

---

## 🐛 Solución de Problemas

### Error: "Failed to authenticate with Azure"

**Causa:** Credenciales incorrectas.

**Solución:**
```bash
# Verificar que el Service Principal existe
az ad sp list --display-name sp-clientes-seguros

# Regenerar secret si es necesario
az ad sp credential reset --id <appId>
```

---

### Error: "Access denied to Key Vault"

**Causa:** Permisos insuficientes.

**Solución:**
```bash
az keyvault set-policy \
  --name kv-clientes-seguros \
  --spn <appId> \
  --key-permissions get list create encrypt decrypt unwrapKey wrapKey
```

---

### Error: "The specified key was not found"

**Causa:** El KEY_PATH en SQL Server es incorrecto.

**Solución:**
```bash
# Listar keys
az keyvault key list --vault-name kv-clientes-seguros

# Ver detalles de la key
az keyvault key show \
  --vault-name kv-clientes-seguros \
  --name CMK-AlwaysEncrypted
```

Usar el `key.kid` completo en SQL Server.

---

## 🔒 Mejores Prácticas

1. **Usar Variables de Entorno**
   - Nunca commits secrets al código
   - Usar Azure Key Vault también para app secrets

2. **Habilitar Logging**
   - Auditar todos los accesos a keys
   - Configurar alertas para accesos sospechosos

3. **Rotación de Claves**
   - Rotar keys cada 90 días (mínimo)
   - Documentar proceso de rotación

4. **Backup**
   - Azure Key Vault tiene backup automático
   - Soft-delete habilitado por defecto (90 días)

5. **Principio de Menor Privilegio**
   - Dar solo permisos necesarios
   - Usar Service Principals específicos por app

---

## 📊 Comparación: Windows vs Azure

| Aspecto | Windows Certificate | Azure Key Vault |
|---------|---------------------|-----------------|
| **Plataforma** | Solo Windows | Cualquiera |
| **Costo** | Gratis | ~$1-3/mes |
| **Setup** | Más simple | Más pasos |
| **Auditoría** | Limitada | Completa |
| **HA** | Local | Global |
| **Rotación** | Manual | Automatizable |
| **Producción** | No recomendado | Recomendado |

---

## 🎯 Cuándo Usar Cada Uno

### Windows Certificate Store:
- ✅ Desarrollo local (Windows)
- ✅ Prototipo rápido
- ✅ No hay presupuesto
- ❌ Producción

### Azure Key Vault:
- ✅ Producción
- ✅ Equipos multi-plataforma
- ✅ Requisitos de auditoría
- ✅ Alta disponibilidad
- ❌ Prototipo rápido sin Azure

---

## 🧹 Limpieza (Evitar Costos)

Cuando termines de probar:

```bash
# Eliminar todo el resource group (Key Vault + Key)
az group delete --name rg-always-encrypted --yes

# Eliminar Service Principal
az ad sp delete --id <appId>
```

---

**Autor:** Material didáctico - Curso Quarkus  
**Capítulo:** 4.2 - Always Encrypted Azure Setup  
**Fecha:** Octubre 2025
**Propietario:** NETEC