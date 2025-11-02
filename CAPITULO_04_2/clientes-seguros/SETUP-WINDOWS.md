# Setup Always Encrypted en Windows

**Guía paso a paso para configurar Always Encrypted con Windows Certificate Store**

---

## 📋 Requisitos Previos

- ✅ Windows 10/11 o Windows Server
- ✅ SQL Server 2016+ o Docker con SQL Server
- ✅ Proyecto Quarkus ya creado (ver INSTALACION.md)
- ✅ PowerShell (incluido en Windows)

---

## 🎯 Resumen del Proceso

```
1. Crear Certificado Auto-firmado (CMK)
2. Exportar Certificado
3. Configurar SQL Server (CMK + CEK)
4. Cifrar Columnas
5. Configurar Driver JDBC
6. Probar
```

---

## PARTE 1: Crear Column Master Key (CMK)

### Paso 1.1: Abrir PowerShell como Administrador

**Windows 10/11:**
1. Buscar "PowerShell" en el menú inicio
2. Clic derecho → "Ejecutar como administrador"

---

### Paso 1.2: Crear Certificado Auto-firmado

```powershell
# Crear certificado para Always Encrypted
$cert = New-SelfSignedCertificate `
    -Subject "CN=AlwaysEncrypted Certificate" `
    -CertStoreLocation "Cert:\CurrentUser\My" `
    -KeyExportPolicy Exportable `
    -Type DocumentEncryptionCert `
    -KeyUsage KeyEncipherment, DataEncipherment `
    -KeySpec KeyExchange `
    -KeyLength 2048
```

**Salida esperada:**
```
Thumbprint                                Subject
----------                                -------
A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q7R8S9T0  CN=AlwaysEncrypted Certificate
```

**Guarda el Thumbprint**, lo necesitarás después.

---

### Paso 1.3: Verificar que el Certificado Existe

```powershell
# Ver el certificado recién creado
Get-ChildItem -Path Cert:\CurrentUser\My | Where-Object {$_.Subject -like "*AlwaysEncrypted*"}
```

**Deberías ver:**
```
Thumbprint                                Subject
----------                                -------
A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q7R8S9T0  CN=AlwaysEncrypted Certificate
```

---

### Paso 1.4: (Opcional) Abrir Administrador de Certificados

Para ver el certificado gráficamente:

1. Presiona `Win + R`
2. Escribe: `certmgr.msc`
3. Navega a: **Personal → Certificates**
4. Busca "AlwaysEncrypted Certificate"

---

## PARTE 2: Configurar SQL Server

### Paso 2.1: Conectarse a SQL Server

**Si usas Docker:**
```bash
docker exec -it sqlserver /opt/mssql-tools18/bin/sqlcmd \
  -S localhost -U sa -P 'Pass123!Admin' -C
```

**Si usas SQL Server local:**
```bash
sqlcmd -S localhost -E -C
```

**O usa SQL Server Management Studio (SSMS).**

---

### Paso 2.2: Usar la Base de Datos

```sql
USE BancoDB;
GO
```

---

### Paso 2.3: Crear Column Master Key (CMK)

```sql
CREATE COLUMN MASTER KEY [CMK_AlwaysEncrypted]
WITH (
    KEY_STORE_PROVIDER_NAME = 'MSSQL_CERTIFICATE_STORE',
    KEY_PATH = 'CurrentUser/My/A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6Q7R8S9T0'
);
GO
```

**⚠️ Importante:** Reemplaza el thumbprint con el tuyo del Paso 1.2.

**Verificar:**
```sql
SELECT * FROM sys.column_master_keys;
GO
```

Deberías ver una fila con `name = 'CMK_AlwaysEncrypted'`.

---

### Paso 2.4: Crear Column Encryption Key (CEK)

```sql
CREATE COLUMN ENCRYPTION KEY [CEK_Clientes]
WITH VALUES (
    COLUMN_MASTER_KEY = [CMK_AlwaysEncrypted],
    ALGORITHM = 'RSA_OAEP',
    ENCRYPTED_VALUE = 0x016E000001630075007200720065006E00740075007300650072002F006D0079002F006100310062003200630033006400340065003500660036006700370068003800690039006A0030006B0031006C0032006D0033006E0034006F0035007000360071003700720038007300390074003000B90B4F3BCE65C6AB5D6F24D8F3B4E8A73910F67B5C8D9E2A1F4B6C7D8E9F0A1B2C3D4E5F6A7B8C9D0E1F2A3B4C5D6E7F8A9B0C1D2E3F4A5B6C7D8E9F0A1B2
);
GO
```

**Nota:** Este `ENCRYPTED_VALUE` es generado automáticamente por SQL Server usando el CMK.

**Verificar:**
```sql
SELECT * FROM sys.column_encryption_keys;
GO
```

---

## PARTE 3: Cifrar las Columnas

### Paso 3.1: Opción A - Cifrar Columnas Existentes (Wizard en SSMS)

**Si tienes SQL Server Management Studio:**

1. Conectar a SQL Server en SSMS
2. Expandir: **Databases → BancoDB → Tables**
3. Clic derecho en tabla `Cliente` → **Encrypt Columns...**
4. Wizard:
   - **Column Selection:**
     - ✅ `numero_tarjeta` → Encryption Type: **Deterministic** → CEK: **CEK_Clientes**
     - ✅ `email` → Encryption Type: **Randomized** → CEK: **CEK_Clientes**
   - **Master Key Configuration:**
     - Seleccionar: **CMK_AlwaysEncrypted**
   - **Validation:** Verificar
   - **Summary:** Revisar
   - **Finish:** Ejecutar

**Esto puede demorar varios minutos** dependiendo del tamaño de la tabla.

---

### Paso 3.2: Opción B - Recrear Tabla con Columnas Cifradas

**Si NO tienes SSMS o prefieres script:**

```sql
-- Paso 1: Renombrar tabla existente
EXEC sp_rename 'Cliente', 'Cliente_Old';
GO

-- Paso 2: Crear nueva tabla con columnas cifradas
CREATE TABLE Cliente (
    id BIGINT NOT NULL PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    numero_tarjeta VARCHAR(255) 
        COLLATE Latin1_General_BIN2
        ENCRYPTED WITH (
            COLUMN_ENCRYPTION_KEY = [CEK_Clientes],
            ENCRYPTION_TYPE = Deterministic,
            ALGORITHM = 'AEAD_AES_256_CBC_HMAC_SHA_256'
        ) NOT NULL,
    email VARCHAR(255)
        COLLATE Latin1_General_BIN2
        ENCRYPTED WITH (
            COLUMN_ENCRYPTION_KEY = [CEK_Clientes],
            ENCRYPTION_TYPE = Randomized,
            ALGORITHM = 'AEAD_AES_256_CBC_HMAC_SHA_256'
        ) NOT NULL,
    telefono VARCHAR(255) NOT NULL
);
GO

-- Paso 3: Copiar datos (SQL Server cifrará automáticamente)
SET IDENTITY_INSERT Cliente ON;
INSERT INTO Cliente (id, nombre, numero_tarjeta, email, telefono)
SELECT id, nombre, numero_tarjeta, email, telefono
FROM Cliente_Old;
SET IDENTITY_INSERT Cliente OFF;
GO

-- Paso 4: Eliminar tabla vieja
DROP TABLE Cliente_Old;
GO
```

**Importante:** El collation `Latin1_General_BIN2` es **requerido** para columnas cifradas.

---

### Paso 3.3: Verificar Configuración

```sql
-- Ver columnas cifradas
SELECT 
    c.name AS ColumnName,
    cek.name AS EncryptionKeyName,
    CASE ce.encryption_type
        WHEN 1 THEN 'Deterministic'
        WHEN 2 THEN 'Randomized'
    END AS EncryptionType
FROM sys.columns c
INNER JOIN sys.column_encryption_keys cek 
    ON c.column_encryption_key_id = cek.column_encryption_key_id
INNER JOIN sys.column_encryption_key_values ce
    ON cek.column_encryption_key_id = ce.column_encryption_key_id
WHERE c.object_id = OBJECT_ID('Cliente');
GO
```

**Salida esperada:**
```
ColumnName        EncryptionKeyName  EncryptionType
--------------    -----------------  --------------
numero_tarjeta    CEK_Clientes      Deterministic
email             CEK_Clientes      Randomized
```

---

## PARTE 4: Configurar Quarkus

### Paso 4.1: Actualizar pom.xml

Verifica que tengas el driver correcto:

```xml
<dependency>
    <groupId>com.microsoft.sqlserver</groupId>
    <artifactId>mssql-jdbc</artifactId>
    <version>12.4.2.jre11</version>
</dependency>
```

---

### Paso 4.2: Actualizar application.properties

```properties
# SQL Server con Always Encrypted
quarkus.datasource.db-kind=mssql
quarkus.datasource.username=sa
quarkus.datasource.password=Pass123!Admin
quarkus.datasource.jdbc.url=jdbc:sqlserver://localhost:1433;\
  databaseName=BancoDB;\
  encrypt=false;\
  trustServerCertificate=true;\
  columnEncryptionSetting=Enabled

# Hibernate (NO usar drop-and-create con columnas cifradas)
quarkus.hibernate-orm.database.generation=none
quarkus.hibernate-orm.log.sql=true

# HTTP
quarkus.http.port=8080
```

**Clave:** `columnEncryptionSetting=Enabled`

---

### Paso 4.3: NO Modificar el Código Java

**El cifrado es transparente.** Tu código sigue igual:

```java
@Entity
public class Cliente extends PanacheEntity {
    public String nombre;
    public String numeroTarjeta;  // Cifrada, pero tu código no cambia
    public String email;          // Cifrada, pero tu código no cambia
    public String telefono;
}
```

---

## PARTE 5: Probar

### Paso 5.1: Ejecutar Quarkus

```bash
./mvnw quarkus:dev
```

---

### Paso 5.2: Insertar Cliente

```bash
curl -X POST http://localhost:8080/api/v1/clientes \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Pedro Sánchez",
    "numeroTarjeta": "5500-0000-0000-0004",
    "email": "pedro.sanchez@banco.com",
    "telefono": "+56944556677"
  }'
```

**Resultado esperado:**
```json
{
  "id": 4,
  "nombre": "Pedro Sánchez",
  "numeroTarjeta": "5500-0000-0000-0004",
  "email": "pedro.sanchez@banco.com",
  "telefono": "+56944556677"
}
```

**Para ti, se ve normal.** Pero los datos están cifrados en la BD.

---

### Paso 5.3: Verificar Cifrado en SQL Server

```sql
-- Conectar SIN habilitar Always Encrypted
sqlcmd -S localhost -U sa -P 'Pass123!Admin' -C

USE BancoDB;
GO

SELECT id, nombre, numero_tarjeta, email, telefono
FROM Cliente
WHERE id = 4;
GO
```

**Verás:**
```
id  nombre          numero_tarjeta                          email                                   telefono
--- --------------- --------------------------------------- --------------------------------------- -------------
4   Pedro Sánchez   0x01A7B3C9D2E4F1G8H2I5J9K3L7M1N6...   0x02X5Y9Z3A7B1C5D9E3F7G1H5I9J3K7...   +56944556677
```

**Los datos cifrados son binarios (0x...).** ✅

---

### Paso 5.4: Buscar por Tarjeta (Deterministic)

```bash
curl "http://localhost:8080/api/v1/clientes/tarjeta/5500-0000-0000-0004"
```

**Funciona porque `numero_tarjeta` usa cifrado Deterministic.** ✅

---

### Paso 5.5: Intentar Buscar por Email (Randomized)

Si intentas:
```java
repository.list("email", "pedro.sanchez@banco.com")
```

**NO funcionará** porque `email` usa cifrado Randomized (no permite búsquedas).

---

## PARTE 6: Verificación Completa

### ✅ Checklist:

- [ ] Certificado creado en Windows Certificate Store
- [ ] CMK creado en SQL Server
- [ ] CEK creado en SQL Server
- [ ] Columnas `numero_tarjeta` y `email` cifradas
- [ ] `columnEncryptionSetting=Enabled` en connection string
- [ ] Aplicación puede insertar datos
- [ ] Aplicación puede leer datos (descifrados)
- [ ] SQL Server guarda datos cifrados (verificado con sqlcmd sin AE)
- [ ] Búsqueda por tarjeta funciona (deterministic)

---

## 🔐 Seguridad Adicional

### Exportar Certificado (Backup)

```powershell
# Exportar certificado con clave privada
$cert = Get-ChildItem -Path Cert:\CurrentUser\My | Where-Object {$_.Subject -like "*AlwaysEncrypted*"}

$password = ConvertTo-SecureString -String "MiPasswordSeguro123!" -Force -AsPlainText

Export-PfxCertificate `
    -Cert $cert `
    -FilePath "C:\Backup\AlwaysEncrypted.pfx" `
    -Password $password
```

**⚠️ Guarda este archivo en un lugar seguro.** Sin él, no podrás descifrar los datos.

---

### Importar Certificado (Restaurar)

```powershell
# En otra máquina o después de reinstalar
$password = ConvertTo-SecureString -String "MiPasswordSeguro123!" -Force -AsPlainText

Import-PfxCertificate `
    -FilePath "C:\Backup\AlwaysEncrypted.pfx" `
    -CertStoreLocation "Cert:\CurrentUser\My" `
    -Password $password
```

---

## 🐛 Solución de Problemas

### Error: "Cannot find the certificate"

**Causa:** El thumbprint es incorrecto o el certificado no existe.

**Solución:**
```powershell
# Listar todos los certificados
Get-ChildItem -Path Cert:\CurrentUser\My

# Buscar el correcto
Get-ChildItem -Path Cert:\CurrentUser\My | Where-Object {$_.Subject -like "*AlwaysEncrypted*"}
```

---

### Error: "Column encryption setting is not enabled"

**Causa:** Falta `columnEncryptionSetting=Enabled` en connection string.

**Solución:** Agregar en `application.properties`:
```properties
quarkus.datasource.jdbc.url=jdbc:sqlserver://...;columnEncryptionSetting=Enabled
```

---

### Error: "Operand type clash: varchar is incompatible with varchar(255) encrypted with"

**Causa:** Intentando usar columna cifrada sin el driver correcto.

**Solución:** 
1. Verificar que el driver sea `mssql-jdbc 12.4.0+`
2. Verificar `columnEncryptionSetting=Enabled`

---

### Error al buscar por email (randomized)

**Es esperado.** Randomized NO permite búsquedas.

**Solución:** Si necesitas buscar, usa Deterministic (menos seguro pero permite `WHERE =`).

---

## 📚 Próximos Pasos

- [ ] Documentar proceso en README.md
- [ ] Crear script de automatización (PowerShell)
- [ ] Implementar rotación de claves (avanzado)
- [ ] Configurar Azure Key Vault (alternativa cloud)

---

**Autor:** Material didáctico - Curso Quarkus  
**Capítulo:** 4.2 - Always Encrypted Setup Windows  
**Fecha:** Octubre 2025
**Propietario:** NETEC