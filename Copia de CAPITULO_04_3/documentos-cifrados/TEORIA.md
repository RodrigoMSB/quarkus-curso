# 📚 Teoría: Cifrado de Datos Sensibles a Nivel de Aplicación

Fundamentos teóricos necesarios para comprender la implementación de cifrado con Google Tink en Quarkus.

---

## 📖 Índice

1. [Fundamentos de Criptografía](#1-fundamentos-de-criptografía)
2. [Tipos de Cifrado](#2-tipos-de-cifrado)
3. [Algoritmos Criptográficos Modernos](#3-algoritmos-criptográficos-modernos)
4. [AEAD: Cifrado Autenticado](#4-aead-cifrado-autenticado)
5. [Gestión de Claves Criptográficas](#5-gestión-de-claves-criptográficas)
6. [Google Tink: Criptografía Segura por Diseño](#6-google-tink-criptografía-segura-por-diseño)
7. [Cifrado a Nivel de Aplicación vs Base de Datos](#7-cifrado-a-nivel-de-aplicación-vs-base-de-datos)
8. [Mejores Prácticas y Errores Comunes](#8-mejores-prácticas-y-errores-comunes)
9. [Compliance y Regulaciones](#9-compliance-y-regulaciones)

---

## 1. Fundamentos de Criptografía

### 1.1 ¿Qué es la Criptografía?

La criptografía es la ciencia de proteger información mediante transformaciones matemáticas que hacen el contenido ilegible para cualquiera que no posea la **clave secreta**.

**Analogía del sobre sellado:**
Imagina que envías una carta en un sobre especial:
- **Texto plano** = Carta abierta que cualquiera puede leer
- **Texto cifrado** = Carta dentro de un sobre con candado
- **Clave** = Llave única que abre el candado
- **Algoritmo** = Tipo de candado (combinación, biométrico, etc.)

### 1.2 Pilares de la Seguridad de la Información

#### Confidencialidad
Solo las entidades autorizadas pueden acceder a la información.

```
Texto plano:  "Transferir $10,000 a cuenta 123456"
           ↓  [CIFRADO]
Texto cifrado: "X7k!mP9@qL#zR..."
```

#### Integridad
La información no ha sido alterada durante su transmisión o almacenamiento.

```
Mensaje original: "Transferir $1,000"
Mensaje alterado: "Transferir $10,000"  ← Detectado por hash/MAC
```

#### Autenticación
Verificación de la identidad del emisor/receptor.

```
¿Este mensaje realmente viene de Alice?
→ Firma digital / MAC verifica la autenticidad
```

#### No repudio
El emisor no puede negar haber enviado el mensaje.

```
Alice firma digitalmente el contrato
→ No puede negar haberlo firmado después
```

---

## 2. Tipos de Cifrado

### 2.1 Cifrado Simétrico

**Definición:** Usa la **misma clave** para cifrar y descifrar.

```
┌─────────────┐    Clave: K123    ┌─────────────┐
│   "Hola"    │ ──────────────────>│ Cifrar(AES) │
│ (texto)     │                     │             │
└─────────────┘                     └─────┬───────┘
                                          │
                                          ▼
                                    "e7k!mP9@qL"
                                    (cifrado)
                                          │
                                          ▼
┌─────────────┐    Clave: K123    ┌─────────────┐
│   "Hola"    │ <──────────────────│ Descifrar   │
│ (texto)     │                     │   (AES)     │
└─────────────┘                     └─────────────┘
```

**Ventajas:**
- ⚡ **Muy rápido** (millones de operaciones por segundo)
- 💾 **Eficiente** en CPU y memoria
- 📦 **Ideal para grandes volúmenes** de datos

**Desventajas:**
- 🔑 **Distribución de claves**: ¿Cómo compartir la clave de forma segura?
- 🔢 **Escalabilidad**: Para N usuarios, necesitas N*(N-1)/2 claves únicas

**Algoritmos comunes:**
- **AES** (Advanced Encryption Standard) ← Usado en este ejercicio
- ChaCha20
- 3DES (obsoleto, no usar)

**Casos de uso:**
- Cifrado de datos en reposo (bases de datos, archivos)
- Cifrado de discos completos
- VPNs y túneles seguros

### 2.2 Cifrado Asimétrico

**Definición:** Usa un **par de claves**:
- **Clave pública**: Puede compartirse libremente, solo cifra
- **Clave privada**: Debe mantenerse secreta, solo descifra

```
         Clave Pública (compartida)
                    ↓
┌─────────────┐         ┌─────────────┐
│   "Hola"    │────────>│ Cifrar(RSA) │
└─────────────┘         └──────┬──────┘
                               │
                               ▼
                         "e7k!mP9@qL"
                         (solo puede descifrarlo
                          quien tenga la clave privada)
                               │
         Clave Privada (secreta)
                    ↓
                ┌──────────────┐
                │ Descifrar    │
                │    (RSA)     │
                └──────┬───────┘
                       ▼
                   "Hola"
```

**Ventajas:**
- 🔐 **No requiere canal seguro** para compartir la clave pública
- 📝 **Firmas digitales** (autentica al emisor)
- 🔑 **Gestión de claves** más simple en sistemas grandes

**Desventajas:**
- 🐌 **1000x más lento** que cifrado simétrico
- 💾 **Mayor uso de recursos** (CPU, memoria)
- 📏 **Limitación de tamaño**: RSA-2048 solo cifra ~245 bytes

**Algoritmos comunes:**
- **RSA** (Rivest-Shamir-Adleman)
- **ECC** (Elliptic Curve Cryptography)
- ElGamal

**Casos de uso:**
- Intercambio de claves simétricas (híbrido)
- Firmas digitales
- Certificados SSL/TLS
- Autenticación

### 2.3 Cifrado Híbrido (Mejor de ambos mundos)

Combina simétrico + asimétrico para máxima seguridad y performance.

```
PASO 1: Generar clave simétrica aleatoria (K_session)
        ↓
PASO 2: Cifrar K_session con clave pública RSA del receptor
        ↓
PASO 3: Cifrar el mensaje completo con K_session (AES)
        ↓
PASO 4: Enviar ambos:
        - K_session cifrada con RSA
        - Mensaje cifrado con AES

RECEPTOR:
PASO 1: Descifrar K_session usando su clave privada RSA
        ↓
PASO 2: Descifrar el mensaje usando K_session (AES)
```

**Usado en:** SSL/TLS, PGP, S/MIME, Signal Protocol

---

## 3. Algoritmos Criptográficos Modernos

### 3.1 AES (Advanced Encryption Standard)

**Historia:**
- Estándar desde 2001 (reemplazó a DES)
- Seleccionado por NIST en competencia mundial
- Algoritmo **Rijndael** ganador
- Usado por NSA para información clasificada (AES-256)

**Características técnicas:**

| Parámetro | AES-128 | AES-192 | AES-256 |
|-----------|---------|---------|---------|
| **Tamaño de clave** | 128 bits | 192 bits | 256 bits |
| **Rondas** | 10 | 12 | 14 |
| **Seguridad** | Alta | Muy Alta | Extrema |
| **Velocidad** | Rápida | Media | Más lenta |

**¿Por qué AES-256?**
- 2^256 combinaciones posibles = más átomos en el universo
- Resistente a ataques de fuerza bruta (computación cuántica incluida)
- Balance perfecto: seguridad + performance

**Modos de operación:**

| Modo | Descripción | Usa IV | Paralelizable | Autenticado |
|------|-------------|--------|---------------|-------------|
| **ECB** | Electronic Codebook | ❌ | ✅ | ❌ |
| **CBC** | Cipher Block Chaining | ✅ | ❌ | ❌ |
| **CTR** | Counter | ✅ | ✅ | ❌ |
| **GCM** | Galois/Counter Mode | ✅ | ✅ | ✅ |

**⚠️ NUNCA usar ECB:** Patrones idénticos en texto plano producen patrones idénticos en texto cifrado.

```
Ejemplo visual del problema de ECB:
┌──────────────┐      ┌──────────────┐
│ Logo empresa │──ECB→│ Logo empresa │  ← Se ve el patrón
│   (pixels)   │      │  (cifrado)   │     aunque esté cifrado
└──────────────┘      └──────────────┘
```

### 3.2 GCM (Galois/Counter Mode)

**¿Qué es GCM?**
Modo de operación para cifrado de bloques que proporciona:
1. **Cifrado** (confidencialidad)
2. **Autenticación** (integridad + autenticidad)

```
        Texto plano + Associated Data (opcional)
                    ↓
        ┌───────────────────────┐
        │   AES-GCM (Cifrado)   │
        └───────────┬───────────┘
                    │
        ┌───────────┴───────────┐
        │                       │
    Texto cifrado           MAC/Tag
    (confidencialidad)   (integridad)
```

**Ventajas de GCM:**
- ⚡ **Paralelizable**: Aprovecha múltiples cores
- 🔐 **AEAD**: Cifrado autenticado (detecta manipulación)
- 🚀 **Rápido**: Optimizado en hardware moderno (AES-NI)
- 📦 **Associated Data**: Puede autenticar datos NO cifrados

**Parámetros de GCM:**
- **Nonce/IV**: 96 bits (12 bytes) - debe ser único por mensaje
- **Tag**: 128 bits (16 bytes) - para autenticación
- **Associated Data**: Datos autenticados pero NO cifrados (opcional)

**Ejemplo práctico:**
```java
// Cifrar con GCM
Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
GCMParameterSpec spec = new GCMParameterSpec(128, nonce); // 128-bit tag
cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

// Agregar datos asociados (autenticados pero no cifrados)
cipher.updateAAD("metadata-version-1".getBytes());

byte[] ciphertext = cipher.doFinal(plaintext);
```

---

## 4. AEAD: Cifrado Autenticado

### 4.1 ¿Qué es AEAD?

**Authenticated Encryption with Associated Data**

AEAD es un paradigma criptográfico que garantiza:
1. **Confidencialidad**: Nadie puede leer el contenido
2. **Integridad**: El mensaje no ha sido alterado
3. **Autenticidad**: El mensaje viene de quien dice ser

**Analogía del sobre con sello de lacre:**
```
┌──────────────────────────────┐
│  Carta dentro de sobre       │  ← Confidencialidad (cifrado)
│  cerrado con candado         │
├──────────────────────────────┤
│  Sello de lacre rojo         │  ← Autenticidad (no fue abierto)
│  con el escudo real          │
└──────────────────────────────┘
```

### 4.2 ¿Por qué AEAD?

**Sin autenticación (solo cifrado):**
```
Atacante intercepta: "e7k!mP9@qL..."
Atacante modifica:   "X9z#nQ2$rM..."  ← Mensaje corrupto
Víctima descifra:    "Transfiere $99,999" ← ¡PELIGRO!
```

**Con AEAD (cifrado + autenticación):**
```
Atacante intercepta: "e7k!mP9@qL..." + MAC
Atacante modifica:   "X9z#nQ2$rM..." + MAC  ← MAC inválido
Víctima descifra:    [ERROR] ← Rechaza el mensaje
```

### 4.3 Componentes de AEAD

**Input:**
- Texto plano (plaintext)
- Clave secreta (secret key)
- Nonce único (nonce/IV)
- Datos asociados opcionales (AAD - Additional Authenticated Data)

**Output:**
- Texto cifrado (ciphertext)
- Tag de autenticación (authentication tag / MAC)

**Proceso:**
```
           ┌─────────────────────┐
Plaintext ─┤                     │
Key ───────┤   AEAD Encrypt      ├──→ Ciphertext
Nonce ─────┤   (AES-GCM)         │
AAD ───────┤                     ├──→ Auth Tag
           └─────────────────────┘

           ┌─────────────────────┐
Ciphertext ┤                     │
Key ───────┤   AEAD Decrypt      ├──→ Plaintext (si tag válido)
Nonce ─────┤   (AES-GCM)         │      OR
AAD ───────┤                     │    Error (si tag inválido)
Auth Tag ──┤                     │
           └─────────────────────┘
```

### 4.4 AAD (Additional Authenticated Data)

Datos que **NO se cifran** pero **SÍ se autentican**.

**Caso de uso real:**
```json
{
  "version": "1.0",              ← AAD (autenticado, no cifrado)
  "timestamp": "2025-10-15",     ← AAD (autenticado, no cifrado)
  "encrypted_payload": "e7k!..."  ← Cifrado + autenticado
}
```

**¿Por qué usar AAD?**
- Metadatos que deben ser legibles pero inmutables
- Números de cuenta, IDs de transacción, versiones de protocolo
- Previene ataques de reordenamiento o repetición

---

## 5. Gestión de Claves Criptográficas

### 5.1 Ciclo de Vida de una Clave

```
┌──────────────┐
│  Generación  │  ← Crear clave con entropía suficiente
└──────┬───────┘
       │
┌──────▼───────┐
│Almacenamiento│  ← Guardar de forma segura (KMS, HSM)
└──────┬───────┘
       │
┌──────▼───────┐
│ Distribución │  ← Compartir solo con entidades autorizadas
└──────┬───────┘
       │
┌──────▼───────┐
│     Uso      │  ← Cifrar/descifrar datos
└──────┬───────┘
       │
┌──────▼───────┐
│  Rotación    │  ← Cambiar periódicamente (compliance)
└──────┬───────┘
       │
┌──────▼───────┐
│ Destrucción  │  ← Eliminar de forma segura y permanente
└──────────────┘
```

### 5.2 Generación de Claves

**Requisitos de una buena clave:**
- **Alta entropía**: Aleatoriedad real (no pseudo-aleatoria débil)
- **Longitud adecuada**: AES-256 = 256 bits mínimo
- **No derivable**: No se puede predecir o reconstruir

**Fuentes de entropía:**
- `/dev/urandom` (Linux/Mac)
- `CryptGenRandom` (Windows)
- Hardware RNG (True Random Number Generator)
- Eventos de hardware (movimientos del mouse, timings de disco)

**❌ NUNCA hacer esto:**
```java
// ¡INCORRECTO! Entropía baja, predecible
String key = "mi-password-123";
byte[] keyBytes = key.getBytes();
SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
```

**✅ Forma correcta:**
```java
// CORRECTO: Generador criptográficamente seguro
KeyGenerator keyGen = KeyGenerator.getInstance("AES");
keyGen.init(256, new SecureRandom());
SecretKey secretKey = keyGen.generateKey();
```

**Con Google Tink (aún mejor):**
```java
// Tink genera y maneja claves de forma segura automáticamente
KeysetHandle keysetHandle = KeysetHandle.generateNew(
    KeyTemplates.get("AES256_GCM")
);
```

### 5.3 Almacenamiento Seguro de Claves

**Regla de oro:** ⚠️ **NUNCA almacenar claves en código fuente**

**Opciones de almacenamiento:**

| Método | Seguridad | Complejidad | Costo | Uso |
|--------|-----------|-------------|-------|-----|
| **Código fuente** | ❌ Nula | Baja | Gratis | NUNCA |
| **Variables de entorno** | ⚠️ Baja | Baja | Gratis | Dev/Testing |
| **Archivo cifrado** | ⚠️ Media | Media | Gratis | Small apps |
| **HSM (Hardware)** | ✅ Máxima | Alta | $$$$$ | Bancos, Gov |
| **KMS (Cloud)** | ✅ Alta | Media | $$ | **Recomendado** |

#### Opción 1: Variables de Entorno

```bash
export TINK_KEYSET="CiQAp91NBhz..."
```

```java
String keysetJson = System.getenv("TINK_KEYSET");
KeysetHandle keysetHandle = CleartextKeysetHandle.read(
    JsonKeysetReader.withString(keysetJson)
);
```

**Pros:** Simple, sin archivos  
**Contras:** Visible en process list, logs, crash dumps

#### Opción 2: Archivo JSON (cifrado)

```java
// Guardar keyset
CleartextKeysetHandle.write(
    keysetHandle,
    JsonKeysetWriter.withFile(new File("keyset.json"))
);

// Cargar keyset
KeysetHandle keysetHandle = CleartextKeysetHandle.read(
    JsonKeysetReader.withFile(new File("keyset.json"))
);
```

**⚠️ Problema:** El archivo debe cifrarse con otra clave (¿dónde la guardas?)

#### Opción 3: KMS (Key Management Service) - **RECOMENDADO**

Google Tink soporta integración nativa con:
- **AWS KMS** (Amazon Web Services)
- **GCP KMS** (Google Cloud Platform)
- **Azure Key Vault** (Microsoft Azure)

```java
// Ejemplo con Google Cloud KMS
String keyUri = "gcp-kms://projects/my-project/locations/global/keyRings/my-ring/cryptoKeys/my-key";
Aead kmsAead = new GcpKmsClient().withDefaultCredentials().getAead(keyUri);

// Cifrar el keyset con la clave maestra del KMS
KeysetHandle keysetHandle = KeysetHandle.generateNew(
    KeyTemplates.get("AES256_GCM")
);
keysetHandle.write(
    JsonKeysetWriter.withFile(new File("encrypted-keyset.json")),
    kmsAead  // ← Clave maestra del KMS cifra el keyset
);
```

**Ventajas de KMS:**
- ✅ Claves maestras nunca salen del HSM
- ✅ Auditoría completa de accesos
- ✅ Rotación automática de claves
- ✅ Control de permisos granular (IAM)
- ✅ Compliance (SOC2, PCI-DSS, HIPAA)

### 5.4 Rotación de Claves

**¿Por qué rotar claves?**
- Compliance (PCI-DSS requiere rotación anual)
- Reducir impacto si una clave se compromete
- Limitar la cantidad de datos cifrados con una sola clave

**Estrategias de rotación:**

#### Estrategia 1: Rotación Manual (Downtime)
```
1. Generar nueva clave
2. Descifrar todos los datos con clave vieja
3. Cifrar todos los datos con clave nueva
4. Eliminar clave vieja
```

**Pros:** Simple  
**Contras:** Requiere downtime, riesgo si falla a mitad del proceso

#### Estrategia 2: Rotación Gradual (Sin Downtime)
```
1. Generar nueva clave (K2)
2. Nuevos datos se cifran con K2
3. Datos viejos siguen cifrados con K1
4. Background job descifra con K1 y recifra con K2
5. Cuando todos los datos usan K2, eliminar K1
```

**Pros:** Sin downtime, seguro  
**Contras:** Complejidad, periodo de transición

#### Estrategia 3: Tink Keysets (Built-in)

Tink maneja múltiples claves en un **keyset** con rotación transparente:

```json
{
  "primaryKeyId": 123456,
  "key": [
    {
      "keyId": 123456,
      "status": "ENABLED",  ← Clave actual (primaria)
      "keyData": "..."
    },
    {
      "keyId": 789012,
      "status": "ENABLED",  ← Clave anterior (aún puede descifrar)
      "keyData": "..."
    }
  ]
}
```

**Proceso:**
1. Nueva clave se agrega al keyset como primaria
2. Datos nuevos se cifran con clave primaria
3. Datos viejos se descifran con su clave original
4. Cuando todos los datos se recifraron, clave vieja se marca como `DISABLED`

---

## 6. Google Tink: Criptografía Segura por Diseño

### 6.1 ¿Qué es Google Tink?

Librería criptográfica creada por el equipo de seguridad de Google para:
- **Simplificar** el uso de criptografía
- **Prevenir errores comunes** (misuse-resistant API)
- **Proporcionar primitivas modernas** (AEAD, MACs, Firmas)

**Analogía del auto con control de crucero:**
- **JCA nativo** = Auto manual (control total, pero fácil chocar)
- **Tink** = Auto con asistencias (ABS, control de tracción, frenado automático)

### 6.2 Arquitectura de Tink

```
┌─────────────────────────────────────────────┐
│         KeysetHandle (Gestión de claves)    │
└────────────────┬────────────────────────────┘
                 │
     ┌───────────┼───────────┬──────────┐
     │           │           │          │
┌────▼────┐ ┌───▼────┐ ┌────▼────┐ ┌──▼───┐
│  Aead   │ │  Mac   │ │ Signature│ │ Hybrid│
│(Cifrado)│ │(Autent)│ │ (Firmas) │ │(Híbri)│
└─────────┘ └────────┘ └──────────┘ └──────┘
     │           │           │          │
┌────▼───────────▼───────────▼──────────▼────┐
│      Implementaciones (AES-GCM, HMAC, etc) │
└─────────────────────────────────────────────┘
```

### 6.3 Primitivas de Tink

#### AEAD (Authenticated Encryption with Associated Data)
```java
Aead aead = keysetHandle.getPrimitive(Aead.class);
byte[] ciphertext = aead.encrypt(plaintext, associatedData);
byte[] plaintext = aead.decrypt(ciphertext, associatedData);
```

**Implementaciones:**
- AES-GCM
- AES-EAX
- AES-CTR-HMAC
- ChaCha20-Poly1305
- XChaCha20-Poly1305

#### MAC (Message Authentication Code)
```java
Mac mac = keysetHandle.getPrimitive(Mac.class);
byte[] tag = mac.computeMac(data);
mac.verifyMac(tag, data);  // Lanza excepción si inválido
```

**Implementaciones:**
- HMAC-SHA256
- HMAC-SHA512
- AES-CMAC

#### Digital Signatures
```java
PublicKeySign signer = privateKeysetHandle.getPrimitive(PublicKeySign.class);
byte[] signature = signer.sign(message);

PublicKeyVerify verifier = publicKeysetHandle.getPrimitive(PublicKeyVerify.class);
verifier.verify(signature, message);
```

**Implementaciones:**
- ECDSA (Elliptic Curve)
- RSA-SSA-PKCS1
- RSA-SSA-PSS
- ED25519

#### Hybrid Encryption (Asimétrico + Simétrico)
```java
HybridEncrypt encrypter = publicKeysetHandle.getPrimitive(HybridEncrypt.class);
byte[] ciphertext = encrypter.encrypt(plaintext, contextInfo);

HybridDecrypt decrypter = privateKeysetHandle.getPrimitive(HybridDecrypt.class);
byte[] plaintext = decrypter.decrypt(ciphertext, contextInfo);
```

**Implementaciones:**
- ECIES (Elliptic Curve Integrated Encryption Scheme)

### 6.4 Ventajas de Tink sobre JCA Nativo

| Aspecto | JCA Nativo | Google Tink |
|---------|------------|-------------|
| **Complejidad** | Alta (muchas clases, parámetros) | Baja (API simple) |
| **Seguridad** | Fácil cometer errores | Seguro por defecto |
| **Rotación de claves** | Manual | Built-in |
| **Múltiples claves** | Complejo | Keyset nativo |
| **Integración KMS** | Manual | Built-in (AWS, GCP, Azure) |
| **Versioning** | No | Sí (metadata en keyset) |
| **Testing** | Difícil | Fácil (keysets de prueba) |

**Ejemplo de complejidad JCA:**

```java
// JCA Nativo - Muchos pasos, fácil fallar
KeyGenerator keyGen = KeyGenerator.getInstance("AES");
keyGen.init(256, new SecureRandom());
SecretKey key = keyGen.generateKey();

byte[] iv = new byte[12];
new SecureRandom().nextBytes(iv);

Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
GCMParameterSpec spec = new GCMParameterSpec(128, iv);
cipher.init(Cipher.ENCRYPT_MODE, key, spec);

byte[] ciphertext = cipher.doFinal(plaintext);
// ¿Dónde guardas el IV? ¿Y la clave? ¿Y el tag?
```

**Mismo resultado con Tink - Mucho más simple:**

```java
// Google Tink - Todo manejado automáticamente
KeysetHandle keysetHandle = KeysetHandle.generateNew(
    KeyTemplates.get("AES256_GCM")
);
Aead aead = keysetHandle.getPrimitive(Aead.class);
byte[] ciphertext = aead.encrypt(plaintext, null);
// IV, tag, y metadata incluidos en ciphertext
```

### 6.5 KeyTemplates Predefinidos

Tink incluye templates seguros y testeados:

```java
// AEAD (Cifrado autenticado)
KeyTemplates.get("AES128_GCM")
KeyTemplates.get("AES256_GCM")           // ← Usado en el ejercicio
KeyTemplates.get("AES128_EAX")
KeyTemplates.get("CHACHA20_POLY1305")

// MAC (Autenticación)
KeyTemplates.get("HMAC_SHA256_128BITTAG")
KeyTemplates.get("HMAC_SHA256_256BITTAG")
KeyTemplates.get("HMAC_SHA512_256BITTAG")

// Signatures (Firmas digitales)
KeyTemplates.get("ECDSA_P256")
KeyTemplates.get("ED25519")
KeyTemplates.get("RSA_SSA_PKCS1_3072_SHA256_F4")

// Hybrid (Asimétrico + Simétrico)
KeyTemplates.get("ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM")
```

---

## 7. Cifrado a Nivel de Aplicación vs Base de Datos

### 7.1 Dónde Cifrar

```
┌──────────────────────────────────────────────────────┐
│              Capas de una Aplicación                 │
├──────────────────────────────────────────────────────┤
│  Frontend (Browser/Mobile)                           │  ← Cifrado E2E
├──────────────────────────────────────────────────────┤
│  Red (HTTPS/TLS)                                     │  ← Cifrado en tránsito
├──────────────────────────────────────────────────────┤
│  Backend (Quarkus/Java)                              │  ← Cifrado a nivel app (Tink)
├──────────────────────────────────────────────────────┤
│  Base de Datos (PostgreSQL/SQL Server)               │  ← TDE / Always Encrypted
├──────────────────────────────────────────────────────┤
│  Sistema Operativo / Disco                           │  ← Cifrado de disco (LUKS, BitLocker)
└──────────────────────────────────────────────────────┘
```

### 7.2 Comparación: App-Level vs DB-Level

| Aspecto | Cifrado en App (Tink) | Cifrado en BD (Always Encrypted) |
|---------|------------------------|-----------------------------------|
| **Control** | Total | Limitado |
| **Portabilidad** | Cualquier BD | SQL Server / Azure SQL |
| **Complejidad** | Media | Alta (certs, drivers) |
| **Performance** | Mínimo impacto | Overhead en queries |
| **Búsquedas** | Difícil (hash) | Soportado (limitado) |
| **Logs** | No aparecen en logs de BD | Aparecen en logs de BD |
| **Auditoría** | En app | En BD |
| **Key Management** | App / KMS | Certificate Store / Key Vault |
| **Soporte multi-tenant** | Fácil (1 clave por tenant) | Complejo |

### 7.3 ¿Cuándo usar cada uno?

#### Cifrado a Nivel de Aplicación (Tink)

**Usar cuando:**
- ✅ Necesitas **control total** sobre claves y cifrado
- ✅ Usas **múltiples bases de datos** (PostgreSQL, MySQL, MongoDB)
- ✅ Requieres **lógica de cifrado personalizada**
- ✅ Los datos sensibles también aparecen en **logs, caches, mensajes**
- ✅ Implementas **multi-tenancy** con claves separadas por cliente
- ✅ Necesitas **cifrado selectivo** (solo ciertos campos)

**Ejemplo:** Startup con PostgreSQL en AWS que necesita cifrar SSN y tarjetas de crédito

#### Cifrado a Nivel de Base de Datos (Always Encrypted / TDE)

**Usar cuando:**
- ✅ Usas **SQL Server / Azure SQL** exclusivamente
- ✅ Requieres **transparencia total** (app no ve claves)
- ✅ Necesitas **búsquedas en campos cifrados**
- ✅ Compliance requiere que **BD proteja datos** independientemente de la app
- ✅ Tienes **infraestructura Windows** robusta (Certificate Store)

**Ejemplo:** Banco corporativo con SQL Server en Azure que debe cumplir PCI-DSS

#### Defense in Depth (Ambos)

**Usar cuando:**
- ✅ Máxima seguridad requerida (salud, finanzas, gobierno)
- ✅ Múltiples vectores de ataque a proteger
- ✅ Compliance extremo (HIPAA, PCI-DSS Level 1)

**Flujo:**
```
Datos en app (Tink) → Cifrado 1 → "X7k!mP9@"
                          ↓
           Guardar en SQL Server
                          ↓
    Always Encrypted → Cifrado 2 → "Qm#8zL!vN"
                          ↓
              Disco (BitLocker) → Cifrado 3
```

**Ventaja:** Compromiso de una capa no expone los datos  
**Desventaja:** Complejidad y overhead de performance

### 7.4 Tabla de Decisión Rápida

```
┌────────────────────────┬──────────────┬──────────────┐
│  Requisito             │  App-Level   │   DB-Level   │
├────────────────────────┼──────────────┼──────────────┤
│ Multi-DB               │      ✅      │      ❌      │
│ Control granular       │      ✅      │      ⚠️      │
│ Simplicidad            │      ✅      │      ❌      │
│ Búsquedas cifradas     │      ❌      │      ✅      │
│ Zero-trust BD          │      ✅      │      ✅      │
│ Log sanitization       │      ✅      │      ❌      │
│ Performance            │      ✅      │      ⚠️      │
│ Rotación de claves     │      ✅      │      ⚠️      │
└────────────────────────┴──────────────┴──────────────┘

Leyenda: ✅ Excelente | ⚠️ Limitado | ❌ No soportado
```

---

## 8. Mejores Prácticas y Errores Comunes

### 8.1 Qué Cifrar

#### ✅ SÍ cifrar:

| Dato | Razón | Regulación |
|------|-------|------------|
| **Números de tarjeta** | PCI-DSS | Obligatorio |
| **SSN / RUT / DNI** | Robo de identidad | GDPR, CCPA |
| **Contraseñas** | Credenciales | OWASP Top 10 |
| **Datos médicos** | Privacidad | HIPAA |
| **Información financiera** | Fraude | SOX, PCI-DSS |
| **Datos biométricos** | Irreversible | GDPR Art. 9 |
| **Mensajes privados** | Privacidad | E2EE |

#### ❌ NO cifrar (o considerar alternativas):

| Dato | Razón | Alternativa |
|------|-------|-------------|
| **IDs / Primary Keys** | Necesarios para JOINs | Hash si es necesario |
| **Timestamps** | Queries de rango | Cifrar solo microsegundos |
| **Emails (en algunos casos)** | Login, búsqueda | Hash para búsqueda |
| **Nombres de usuario** | Públicos | Depende del contexto |
| **Logs de auditoría** | Inmutabilidad | Cifrar campos específicos |

### 8.2 Errores Comunes y Cómo Evitarlos

#### ❌ Error 1: Hardcodear claves en el código

```java
// ¡MAL! Clave visible en código fuente
private static final String SECRET_KEY = "MiClaveSecreta123";
```

**Impacto:** Commit en Git → clave expuesta para siempre (incluso si borras)

**Solución:**
```java
// BIEN: Usar variables de entorno o KMS
String keysetJson = System.getenv("TINK_KEYSET");
// O mejor: integración con AWS KMS, Google Cloud KMS
```

#### ❌ Error 2: Usar ECB mode

```java
// ¡MAL! Modo ECB expone patrones
Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
```

**Impacto:** Mismo texto plano → mismo texto cifrado (ataque por patrones)

**Solución:**
```java
// BIEN: Usar GCM (o CBC con IV aleatorio)
Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
```

#### ❌ Error 3: Reusar IV/Nonce

```java
// ¡MAL! IV fijo
byte[] iv = new byte[12];  // Todo ceros
```

**Impacto:** Dos mensajes cifrados con mismo IV + misma clave → ataque

**Solución:**
```java
// BIEN: IV aleatorio único por mensaje
byte[] iv = new byte[12];
new SecureRandom().nextBytes(iv);
// O usar Tink que lo maneja automáticamente
```

#### ❌ Error 4: Cifrar sin autenticar

```java
// ¡MAL! Solo confidencialidad, sin integridad
Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
```

**Impacto:** Atacante puede modificar ciphertext sin detección

**Solución:**
```java
// BIEN: Usar AEAD (GCM, ChaCha20-Poly1305)
Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
// O usar Tink que solo ofrece AEAD
```

#### ❌ Error 5: No tener plan de recuperación

```
Escenario: Perdiste la clave KMS
Resultado: Todos los datos cifrados = PERDIDOS PARA SIEMPRE
```

**Solución:**
- Backup de claves en múltiples KMS (multi-region)
- Proceso documentado de recuperación de desastres
- Key escrow para casos críticos (con aprobación legal)
- Pruebas periódicas de restauración

#### ❌ Error 6: Cifrar todo

```java
// ¡MAL! Cifrar campos que necesitas para buscar
public String email;  // Cifrado → no puedes hacer WHERE email = 'user@example.com'
```

**Impacto:** Performance horrible, queries imposibles

**Solución:**
- Cifrar solo lo estrictamente necesario
- Usar hash para búsquedas (si no necesitas descifrar)
- Implementar búsqueda por tokens (tokenización)

### 8.3 Checklist de Seguridad

```
[ ] Usar algoritmos modernos (AES-256, no DES/3DES)
[ ] Usar AEAD (GCM, no ECB ni CBC solo)
[ ] Generar claves con alta entropía (SecureRandom, no Math.random())
[ ] Nunca reusar IV/Nonce
[ ] Almacenar claves en KMS (no en código ni archivos sin cifrar)
[ ] Implementar rotación de claves
[ ] Cifrar en tránsito (TLS 1.3+)
[ ] Cifrar en reposo (disco, backups)
[ ] Logs sin datos sensibles
[ ] Plan de recuperación de claves documentado
[ ] Auditoría de accesos a datos sensibles
[ ] Testing de cifrado/descifrado en CI/CD
[ ] Compliance verificado (PCI-DSS, GDPR, HIPAA)
```

---

## 9. Compliance y Regulaciones

### 9.1 PCI-DSS (Payment Card Industry Data Security Standard)

**Aplica a:** Cualquier organización que procesa, almacena o transmite datos de tarjetas de crédito.

**Requisitos clave de cifrado:**

| Requisito | Descripción | Implementación |
|-----------|-------------|----------------|
| **3.4** | Cifrar PAN en tránsito (TLS) | HTTPS, TLS 1.2+ |
| **3.5** | Cifrar PAN almacenado | AES-256-GCM |
| **3.6** | Gestión de claves criptográficas | KMS, rotación anual |
| **3.7** | Restringir acceso a claves | IAM, least privilege |

**Datos que DEBES cifrar según PCI-DSS:**
- PAN (Primary Account Number) - Número de tarjeta
- CVV/CVV2/CVC2/CID (NO almacenar post-autorización)
- PIN blocks

**Datos que NO debes almacenar:**
- Track data completa (banda magnética)
- CAV/CVC/CVV/CID (código de seguridad) después de autorización
- PIN en texto plano

### 9.2 GDPR (General Data Protection Regulation)

**Aplica a:** Organizaciones que procesan datos de ciudadanos de la UE.

**Artículos relevantes:**

| Artículo | Descripción | Cifrado Aplicable |
|----------|-------------|-------------------|
| **Art. 5(1)(f)** | Integridad y confidencialidad | ✅ Cifrado obligatorio |
| **Art. 9** | Datos sensibles especiales | ✅ Cifrado reforzado |
| **Art. 32** | Seguridad del procesamiento | ✅ "Pseudonimización y cifrado" |
| **Art. 34** | Notificación de brechas | ✅ No notificar si está cifrado |

**Categorías especiales (Art. 9):**
- Origen racial/étnico
- Opiniones políticas
- Creencias religiosas
- Datos biométricos
- Datos de salud
- Orientación sexual

**Beneficio del cifrado en GDPR:**
Si datos están "efectivamente cifrados y la clave no fue comprometida", NO es necesario notificar a los afectados en caso de brecha.

### 9.3 HIPAA (Health Insurance Portability and Accountability Act)

**Aplica a:** Organizaciones en EE.UU. que manejan PHI (Protected Health Information).

**Estándar de seguridad HIPAA (45 CFR § 164.312):**

| Estándar | Requisito | Implementación |
|----------|-----------|----------------|
| **Encryption** | "Implement mechanism to encrypt ePHI" | AES-256 |
| **Access Control** | Unique user identification | Claves por usuario/rol |
| **Audit Controls** | Log de accesos a ePHI | Auditoría de descifrado |
| **Integrity** | Protección contra alteración | AEAD (GCM) |

**PHI incluye:**
- Nombres + cualquier dato médico
- Números de seguro médico
- Historiales clínicos
- Resultados de laboratorio
- Información de facturación médica

### 9.4 SOX (Sarbanes-Oxley Act)

**Aplica a:** Empresas públicas en EE.UU. (protección de datos financieros).

**Requisitos:**
- Integridad de registros financieros
- Auditoría de cambios
- Retención de datos (7 años)
- Controles de acceso

**Cifrado recomendado:**
- Transacciones financieras
- Emails con información financiera
- Backups de datos financieros
- Logs de auditoría (inmutables)

### 9.5 Tabla Comparativa de Regulaciones

```
┌─────────────┬──────────┬──────┬────────┬─────────┐
│ Aspecto     │ PCI-DSS  │ GDPR │ HIPAA  │  SOX    │
├─────────────┼──────────┼──────┼────────┼─────────┤
│ Cifrado     │ Obligat  │ Recm │ Recm   │ Recm    │
│ Rotación    │ Anual    │ -    │ -      │ -       │
│ Auditoría   │ Anual    │ -    │ Sí     │ Sí      │
│ Breach      │ Notif    │ 72h  │ 60 días│ -       │
│ Multas      │ $5k-100k │ €20M │ $50k   │ $25M    │
└─────────────┴──────────┴──────┴────────┴─────────┘

Leyenda:
Obligat = Obligatorio
Recm = Recomendado (addressable/required)
Notif = Notificación a procesadores
```

### 9.6 Consecuencias de No Cumplir

**Ejemplos reales de multas:**

| Empresa | Regulación | Multa | Razón |
|---------|------------|-------|-------|
| **British Airways (2020)** | GDPR | £20M | Datos de 400k clientes sin cifrar |
| **Marriott (2020)** | GDPR | £18.4M | Brecha de 339M registros |
| **Uber (2018)** | PCI-DSS | $148M | Datos de tarjetas sin cifrar |
| **Anthem (2018)** | HIPAA | $16M | 79M registros médicos expuestos |

**Lección:** El costo del cifrado es **insignificante** comparado con las multas y daño reputacional.

---

## 📖 Resumen Ejecutivo

### Puntos Clave

1. **Cifrado simétrico (AES-256-GCM)** es perfecto para datos en reposo (rápido, seguro)

2. **AEAD = Cifrado + Autenticación** en una sola operación (nunca cifrar sin autenticar)

3. **Google Tink simplifica criptografía** y previene errores comunes (usar siempre que sea posible)

4. **Gestión de claves es CRÍTICA**:
   - Generar con alta entropía (SecureRandom)
   - Almacenar en KMS (AWS, GCP, Azure)
   - Rotar periódicamente (compliance)
   - Backup seguro (disaster recovery)

5. **Cifrado a nivel de aplicación** ofrece máximo control y portabilidad

6. **Compliance requiere cifrado**: PCI-DSS (obligatorio), GDPR (recomendado fuertemente), HIPAA (addressable)

7. **Errores comunes a evitar**:
   - Hardcodear claves
   - Usar ECB mode
   - Reusar IV/Nonce
   - Cifrar sin autenticar
   - No tener plan de recuperación

### Analogía Final: La Cadena de Seguridad

El cifrado es como una **cadena**. La seguridad total es tan fuerte como el eslabón más débil:

```
┌───────────┐   ┌───────────┐   ┌───────────┐   ┌───────────┐
│ Algoritmo │──→│   Clave   │──→│Implementac│──→│  Gestión  │
│  (AES)    │   │  (256bit) │   │  (Tink)   │   │   (KMS)   │
└───────────┘   └───────────┘   └───────────┘   └───────────┘
   ✅ Fuerte       ✅ Fuerte       ✅ Fuerte       ✅ Fuerte

     ❌ Un solo eslabón débil = TODO comprometido
```

**Ejemplo de eslabón débil:**
- ✅ AES-256 (fuerte)
- ✅ Implementación correcta con Tink (fuerte)
- ❌ Clave hardcodeada en Git (DÉBIL) → TODO inseguro

---

## 🔗 Referencias y Recursos Adicionales

### Documentación Oficial
- [Google Tink](https://github.com/google/tink)
- [NIST Cryptographic Standards](https://csrc.nist.gov/projects/cryptographic-standards-and-guidelines)
- [OWASP Cryptographic Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html)

### Estándares y RFCs
- [RFC 5116 - AEAD Specification](https://tools.ietf.org/html/rfc5116)
- [NIST SP 800-38D - GCM Mode](https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-38d.pdf)
- [FIPS 197 - AES Standard](https://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.197.pdf)

### Libros Recomendados
- "Serious Cryptography" - Jean-Philippe Aumasson
- "Cryptography Engineering" - Ferguson, Schneier, Kohno
- "Applied Cryptography" - Bruce Schneier

### Herramientas
- [CyberChef](https://gchq.github.io/CyberChef/) - Analizar/transformar datos cifrados
- [KeyStore Explorer](https://keystore-explorer.org/) - Gestión de keystores Java
- [OpenSSL](https://www.openssl.org/) - Toolkit criptográfico

### Cursos
- [Cryptography I - Stanford (Coursera)](https://www.coursera.org/learn/crypto)
- [Applied Cryptography - Udacity](https://www.udacity.com/course/applied-cryptography--cs387)

---

**¡Fin de la teoría!** 🎓🔐