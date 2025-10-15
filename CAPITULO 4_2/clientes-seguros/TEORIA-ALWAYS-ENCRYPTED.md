# Teoría: Always Encrypted en SQL Server

**Capítulo 4.2: Cifrado Transparente de Columnas con Always Encrypted**

---

## 📚 Tabla de Contenidos

1. [¿Qué es Always Encrypted?](#1-qué-es-always-encrypted)
2. [Arquitectura y Componentes](#2-arquitectura-y-componentes)
3. [Tipos de Cifrado](#3-tipos-de-cifrado)
4. [Column Master Key (CMK)](#4-column-master-key-cmk)
5. [Column Encryption Key (CEK)](#5-column-encryption-key-cek)
6. [Flujo de Cifrado/Descifrado](#6-flujo-de-cifradodescifrado)
7. [Always Encrypted vs Otros Métodos](#7-always-encrypted-vs-otros-métodos)
8. [Casos de Uso](#8-casos-de-uso)
9. [Ventajas y Limitaciones](#9-ventajas-y-limitaciones)
10. [Requisitos Técnicos](#10-requisitos-técnicos)

---

## 1. ¿Qué es Always Encrypted?

**Always Encrypted** es una característica de seguridad de SQL Server (desde 2016) que permite **cifrar datos sensibles de forma transparente** tanto en reposo como en tránsito.

### Concepto Clave:

> Los datos se cifran en el **cliente** (aplicación) y se descifran en el **cliente**.  
> El servidor **nunca** ve los datos en texto plano.

---

### Ejemplo Visual:

```
┌─────────────┐                    ┌─────────────┐
│  Aplicación │                    │  SQL Server │
│   (Cliente) │                    │  (Servidor) │
└─────────────┘                    └─────────────┘
      │                                   │
      │ 1. Datos: "4532-1234-5678-9012"  │
      │────────────────────────────────>  │
      │                                   │
      │ 2. Cifra localmente               │
      │    "A7B3C9D2E4F1..."             │
      │────────────────────────────────>  │
      │                                   │
      │                            3. Guarda cifrado
      │                               en disco
      │                                   │
      │ 4. Lee datos cifrados             │
      │ <────────────────────────────────│
      │    "A7B3C9D2E4F1..."             │
      │                                   │
      │ 5. Descifra localmente            │
      │    "4532-1234-5678-9012"         │
      └───────────────────────────────────┘
```

**El servidor NUNCA ve "4532-1234-5678-9012" en texto plano.**

---

## 2. Arquitectura y Componentes

### Componentes Principales:

```
┌──────────────────────────────────────────────────────┐
│                 APLICACIÓN QUARKUS                   │
│  ┌────────────────────────────────────────────────┐ │
│  │         Driver JDBC Always Encrypted           │ │
│  │  • Cifra datos antes de enviar                 │ │
│  │  • Descifra datos al recibir                   │ │
│  │  • Maneja CEK (Column Encryption Key)          │ │
│  └────────────────────────────────────────────────┘ │
└────────────────────┬─────────────────────────────────┘
                     │
                     │ Datos cifrados
                     ▼
┌──────────────────────────────────────────────────────┐
│                   SQL SERVER                         │
│  ┌────────────────────────────────────────────────┐ │
│  │  Tabla: Cliente                                │ │
│  │  ┌──────────┬─────────────────────────────┐   │ │
│  │  │ nombre   │ "Juan Pérez" (sin cifrar)   │   │ │
│  │  │ tarjeta  │ "A7B3C9..." (CIFRADA)       │   │ │
│  │  │ email    │ "X2Y8Z1..." (CIFRADA)       │   │ │
│  │  └──────────┴─────────────────────────────┘   │ │
│  └────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────┘
                     │
                     │ Metadata de cifrado
                     ▼
┌──────────────────────────────────────────────────────┐
│              KEY STORE PROVIDER                      │
│  • Windows Certificate Store                         │
│  • Azure Key Vault                                   │
│  • Custom Key Store                                  │
│                                                       │
│  ┌────────────────────────────────────────────────┐ │
│  │  Column Master Key (CMK)                       │ │
│  │  • Nunca sale del Key Store                    │ │
│  │  • Cifra/descifra el CEK                       │ │
│  └────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────┘
```

---

## 3. Tipos de Cifrado

Always Encrypted soporta dos tipos de cifrado:

### 3.1 Deterministic Encryption (Determinístico)

**Características:**
- Mismo valor genera **mismo cifrado**
- Permite búsquedas por igualdad (`WHERE tarjeta = '4532-...'`)
- Permite JOINs, GROUP BY, DISTINCT

**Ejemplo:**
```
Texto plano:  "4532-1234-5678-9012"
Cifrado:      "A7B3C9D2E4F1G8H2..."

Texto plano:  "4532-1234-5678-9012"  (mismo valor)
Cifrado:      "A7B3C9D2E4F1G8H2..."  (mismo cifrado)
```

**Cuándo usar:**
- Números de tarjeta (búsqueda exacta)
- RUT/DNI (búsqueda por identificador)
- Códigos únicos

**⚠️ Menos seguro:** Patrones repetidos pueden revelar información.

---

### 3.2 Randomized Encryption (Aleatorio)

**Características:**
- Mismo valor genera **cifrado diferente** cada vez
- NO permite búsquedas
- Mayor seguridad

**Ejemplo:**
```
Texto plano:  "juan@banco.com"
Cifrado 1:    "X2Y8Z1K4M7..."

Texto plano:  "juan@banco.com"  (mismo valor)
Cifrado 2:    "P9Q3R5T6W8..."   (cifrado diferente)
```

**Cuándo usar:**
- Emails (solo lectura)
- Direcciones
- Notas médicas
- Cualquier dato que NO necesite búsquedas

**✅ Más seguro:** Imposible detectar patrones.

---

### Comparación:

| Aspecto | Deterministic | Randomized |
|---------|---------------|------------|
| **Búsquedas** | ✅ Sí (igualdad) | ❌ No |
| **JOIN/GROUP BY** | ✅ Sí | ❌ No |
| **Seguridad** | Media | Alta |
| **Uso típico** | IDs, códigos | Datos sensibles generales |

---

## 4. Column Master Key (CMK)

### ¿Qué es?

El **Column Master Key** es la **clave maestra** que protege las Column Encryption Keys (CEK).

**Analogía:** Es como la llave de una caja fuerte que contiene otras llaves.

---

### Ubicaciones Soportadas:

#### 4.1 Windows Certificate Store ⭐ Más común

**Dónde:** Almacén de certificados de Windows  
**Ventajas:**
- ✅ Gratis
- ✅ Integrado en Windows
- ✅ Fácil de configurar

**Limitaciones:**
- ❌ Solo Windows
- ❌ No funciona en Docker/Linux

**Proceso:**
1. Crear certificado auto-firmado en Windows
2. Almacenar en "Current User" o "Local Machine"
3. Referenciar desde SQL Server

---

#### 4.2 Azure Key Vault

**Dónde:** Servicio cloud de Azure  
**Ventajas:**
- ✅ Funciona desde cualquier plataforma
- ✅ Gestión centralizada
- ✅ Auditoría completa
- ✅ Alta disponibilidad

**Limitaciones:**
- ❌ Requiere cuenta Azure
- ❌ Tiene costos (pequeños)

**Proceso:**
1. Crear Key Vault en Azure
2. Crear clave en Key Vault
3. Configurar acceso desde aplicación
4. Referenciar desde SQL Server

---

#### 4.3 Custom Key Store Provider

**Dónde:** Implementación personalizada (HSM, otro servicio)  
**Ventajas:**
- ✅ Control total
- ✅ Integración con infraestructura existente

**Limitaciones:**
- ❌ Complejo de implementar
- ❌ Requiere código custom

---

### Propiedades del CMK:

```sql
CREATE COLUMN MASTER KEY [MyCMK]
WITH (
    KEY_STORE_PROVIDER_NAME = 'MSSQL_CERTIFICATE_STORE',
    KEY_PATH = 'CurrentUser/My/thumbprint'
);
```

**Importante:** 
- El CMK **nunca sale** del Key Store
- SQL Server solo guarda **metadata** (dónde está el CMK)
- La aplicación accede al Key Store para usarlo

---

## 5. Column Encryption Key (CEK)

### ¿Qué es?

El **Column Encryption Key** es la clave que **realmente cifra los datos** de las columnas.

**Analogía:** Es la llave específica que abre un cajón particular dentro de la caja fuerte.

---

### Relación CMK ↔ CEK:

```
┌─────────────────────────────────────────────┐
│          Column Master Key (CMK)            │
│  "La llave maestra"                         │
│  • Protege los CEK                          │
│  • Nunca sale del Key Store                 │
└────────────┬────────────────────────────────┘
             │
             │ Cifra/Descifra
             ▼
┌─────────────────────────────────────────────┐
│       Column Encryption Key (CEK)           │
│  "La llave específica"                      │
│  • Cifra los datos de las columnas         │
│  • Guardada cifrada en SQL Server          │
│  • Descifrada por el driver usando CMK     │
└────────────┬────────────────────────────────┘
             │
             │ Cifra
             ▼
┌─────────────────────────────────────────────┐
│              Datos en Columna               │
│  • numero_tarjeta (cifrada con CEK)        │
│  • email (cifrada con CEK)                 │
└─────────────────────────────────────────────┘
```

---

### Creación del CEK:

```sql
CREATE COLUMN ENCRYPTION KEY [MyCEK]
WITH VALUES (
    COLUMN_MASTER_KEY = [MyCMK],
    ALGORITHM = 'RSA_OAEP',
    ENCRYPTED_VALUE = 0x01FA... -- Valor cifrado por CMK
);
```

**Flujo:**
1. Se genera un CEK aleatorio
2. Se cifra con el CMK
3. Se guarda cifrado en SQL Server
4. La aplicación:
   - Lee el CEK cifrado
   - Lo descifra usando el CMK (del Key Store)
   - Usa el CEK para cifrar/descifrar datos

---

## 6. Flujo de Cifrado/Descifrado

### 6.1 Flujo de Inserción (Cifrado)

```
APLICACIÓN QUARKUS
    │
    │ 1. Usuario ingresa datos
    │    nombre: "Juan Pérez"
    │    tarjeta: "4532-1234-5678-9012"
    │
    ▼
DRIVER JDBC ALWAYS ENCRYPTED
    │
    │ 2. Consulta metadata de SQL Server
    │    "¿Qué columnas están cifradas?"
    │    Respuesta: "tarjeta usa CEK_Clientes"
    │
    │ 3. Obtiene CEK_Clientes cifrado desde SQL Server
    │
    │ 4. Descifra CEK_Clientes usando CMK
    │    (accede a Windows Certificate Store / Azure Key Vault)
    │
    │ 5. Cifra "4532-1234-5678-9012" con CEK_Clientes
    │    Resultado: "A7B3C9D2E4F1..."
    │
    │ 6. Envía a SQL Server:
    │    INSERT INTO Cliente (nombre, tarjeta)
    │    VALUES ('Juan Pérez', 0xA7B3C9D2E4F1...)
    │
    ▼
SQL SERVER
    │
    │ 7. Guarda datos:
    │    nombre: "Juan Pérez" (texto plano)
    │    tarjeta: 0xA7B3C9D2... (cifrado)
    │
    └─> Datos en disco (cifrados)
```

---

### 6.2 Flujo de Lectura (Descifrado)

```
APLICACIÓN QUARKUS
    │
    │ 1. Ejecuta query
    │    SELECT * FROM Cliente WHERE id = 1
    │
    ▼
SQL SERVER
    │
    │ 2. Lee datos del disco:
    │    nombre: "Juan Pérez" (texto plano)
    │    tarjeta: 0xA7B3C9D2... (cifrado)
    │
    │ 3. Envía datos cifrados a la aplicación
    │
    ▼
DRIVER JDBC ALWAYS ENCRYPTED
    │
    │ 4. Detecta que "tarjeta" está cifrada
    │
    │ 5. Obtiene CEK_Clientes cifrado
    │
    │ 6. Descifra CEK_Clientes usando CMK
    │    (accede a Key Store)
    │
    │ 7. Descifra 0xA7B3C9D2... con CEK_Clientes
    │    Resultado: "4532-1234-5678-9012"
    │
    │ 8. Retorna a la aplicación:
    │    nombre: "Juan Pérez"
    │    tarjeta: "4532-1234-5678-9012"
    │
    ▼
APLICACIÓN QUARKUS
    │
    └─> Usuario ve datos en texto plano
```

---

## 7. Always Encrypted vs Otros Métodos

### Comparación:

| Método | Dónde se cifra | Servidor ve datos | Búsquedas | Transparencia |
|--------|----------------|-------------------|-----------|---------------|
| **Always Encrypted** | Cliente | ❌ No | ✅ Limitadas | ✅ Total |
| **TDE (Transparent Data Encryption)** | Servidor | ✅ Sí | ✅ Todas | ✅ Total |
| **Cifrado a nivel aplicación** | Cliente | ❌ No | ❌ No | ❌ Manual |
| **Azure SQL Column Encryption** | Cliente | ❌ No | ✅ Limitadas | ✅ Total |

---

### Always Encrypted vs TDE:

**TDE (Transparent Data Encryption):**
- Cifra TODO el archivo de base de datos
- El servidor descifra al leer
- Protege contra robo de discos
- NO protege contra accesos no autorizados al servidor

**Always Encrypted:**
- Cifra columnas específicas
- El servidor NUNCA descifra
- Protege contra administradores maliciosos
- Protege datos en tránsito y en reposo

---

## 8. Casos de Uso

### ✅ Cuándo Usar Always Encrypted:

1. **Datos Financieros**
   - Números de tarjeta
   - Números de cuenta
   - CVV, PIN

2. **Datos Personales Sensibles**
   - RUT/DNI
   - Pasaportes
   - Números de seguridad social

3. **Datos de Salud (HIPAA)**
   - Historias clínicas
   - Resultados de exámenes
   - Diagnósticos

4. **Cumplimiento Normativo**
   - PCI-DSS (pagos)
   - GDPR (Europa)
   - HIPAA (salud en USA)
   - SOX (financiero)

5. **Protección contra Insiders**
   - DBAs no deben ver datos sensibles
   - Auditores con acceso limitado

---

### ❌ Cuándo NO Usar Always Encrypted:

1. **Columnas que requieren:**
   - Búsquedas con LIKE `%texto%`
   - Comparaciones (>, <, BETWEEN)
   - Funciones (UPPER, LOWER, etc.)
   - Agregaciones complejas

2. **Datos no sensibles**
   - Nombres
   - Direcciones públicas
   - Descripciones

3. **Alto volumen de escrituras**
   - El cifrado tiene overhead
   - Puede impactar performance

4. **Compatibilidad con herramientas**
   - Algunas herramientas no soportan Always Encrypted
   - Reporting puede ser complejo

---

## 9. Ventajas y Limitaciones

### ✅ Ventajas:

1. **Seguridad Extrema**
   - Servidor nunca ve datos en texto plano
   - Protege contra DBAs maliciosos
   - Protege datos en tránsito y reposo

2. **Transparencia**
   - La aplicación no necesita lógica de cifrado
   - El driver maneja todo
   - Código limpio

3. **Cumplimiento**
   - Facilita cumplir regulaciones
   - Auditoría clara
   - Separación de roles

4. **Granularidad**
   - Se cifran solo columnas específicas
   - No impacta columnas no sensibles

---

### ❌ Limitaciones:

1. **Operaciones Restringidas**
   ```sql
   -- ❌ NO funciona (columna cifrada)
   WHERE tarjeta LIKE '4532%'
   WHERE YEAR(fecha_nacimiento) > 1990
   WHERE salario > 50000
   
   -- ✅ SÍ funciona (deterministic)
   WHERE tarjeta = '4532-1234-5678-9012'
   WHERE rut = '12345678-9'
   ```

2. **Performance**
   - Overhead de cifrado/descifrado
   - Más tráfico de red (metadata)
   - No puede usar índices eficientemente

3. **Compatibilidad**
   - Requiere driver compatible
   - Algunas herramientas no lo soportan
   - Versión mínima: SQL Server 2016

4. **Complejidad de Gestión de Claves**
   - CMK debe estar disponible
   - Rotación de claves es compleja
   - Backup de claves crítico

5. **Limitaciones de Tipo de Dato**
   - Solo ciertos tipos soportados
   - varchar, nvarchar, int, bigint, etc.
   - No soporta: XML, geography, geometry

---

## 10. Requisitos Técnicos

### 10.1 SQL Server

- **Versión:** SQL Server 2016 o superior
- **Edición:** Todas (incluida Express)
- **Azure SQL:** Soportado

---

### 10.2 Driver JDBC

**Para Quarkus/Java:**

```xml
<dependency>
    <groupId>com.microsoft.sqlserver</groupId>
    <artifactId>mssql-jdbc</artifactId>
    <version>12.4.0.jre11</version> <!-- O superior -->
</dependency>
```

**Connection String:**
```
jdbc:sqlserver://localhost:1433;
  databaseName=BancoDB;
  columnEncryptionSetting=Enabled;
  keyStoreAuthentication=KeyVaultClientSecret;
  keyStoreLocation=<location>;
  ...
```

---

### 10.3 Key Store Provider

**Opciones:**

#### Windows Certificate Store:
- **SO:** Windows
- **Configuración:** Certificado auto-firmado
- **Autenticación:** Integrada con Windows

#### Azure Key Vault:
- **SO:** Cualquiera
- **Configuración:** Key Vault + Service Principal
- **Autenticación:** Client ID + Secret

#### Java Key Store:
- **SO:** Cualquiera
- **Configuración:** Custom provider
- **Autenticación:** Según implementación

---

### 10.4 Herramientas

**Para configurar columnas:**
- SQL Server Management Studio (SSMS)
- Azure Data Studio
- Scripts T-SQL

**Para generar CMK/CEK:**
- SSMS (wizard)
- PowerShell
- T-SQL manual

---

## 📊 Resumen Ejecutivo

**Always Encrypted:**
- ✅ Cifrado transparente a nivel cliente
- ✅ Servidor NUNCA ve datos sensibles
- ✅ Protección en reposo y tránsito
- ✅ Cumplimiento normativo

**Componentes clave:**
- Column Master Key (CMK) → Llave maestra
- Column Encryption Key (CEK) → Cifra datos
- Driver compatible → Hace el trabajo

**Tipos:**
- Deterministic → Permite búsquedas (=)
- Randomized → Mayor seguridad, sin búsquedas

**Cuándo usar:**
- Datos financieros sensibles
- PCI-DSS, GDPR, HIPAA
- Protección contra insiders

**Limitaciones:**
- Operaciones SQL restringidas
- Overhead de performance
- Gestión de claves compleja

---

**Siguiente paso:** Configuración práctica en Windows con Certificate Store.

---

**Autor:** Material didáctico - Curso Quarkus  
**Capítulo:** 4.2 - Always Encrypted  
**Fecha:** Octubre 2025
**Propietario:** NETEC