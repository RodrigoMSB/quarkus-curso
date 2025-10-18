# 🔐 Parte 2: Autenticación con JWT (JSON Web Token)

## 📚 ¿Qué es JWT?

**JWT (JSON Web Token)** es un estándar abierto ([RFC 7519](https://tools.ietf.org/html/rfc7519)) que define una forma compacta y autocontenida de transmitir información de forma segura entre dos partes como un objeto JSON.

### Anatomía de un JWT

Un JWT consta de **tres partes** separadas por puntos (`.`):

```
eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL3ZhdWx0Y29ycC5jb20iLCJzdWIiOiJlbXAwMDEifQ.signature
│─────────── Header ──────────────│───────────── Payload ────────────│─── Signature ───│
```

1. **Header**: Tipo de token y algoritmo de firma (Base64)
2. **Payload**: Claims o datos del usuario (Base64)
3. **Signature**: Firma criptográfica para verificar integridad (RSA/HMAC)

### 🎯 Ventajas de JWT

| Ventaja | Descripción |
|---------|-------------|
| **Stateless** | El servidor no almacena sesiones, toda la info está en el token |
| **Escalable** | Perfecto para microservicios distribuidos |
| **Portátil** | Funciona entre diferentes dominios y servicios |
| **Autocontenido** | El token incluye toda la información necesaria |
| **Seguro** | Firmado criptográficamente (no puede alterarse) |

---

## 🏗️ Arquitectura de la Solución

```
┌─────────────────────────────────────────────────────────────┐
│                    Cliente (Postman/curl)                   │
└────────────────────────────┬────────────────────────────────┘
                             │
                   ┌─────────▼──────────┐
                   │  1. POST /login    │
                   │  (user/password)   │
                   └─────────┬──────────┘
                             │
                   ┌─────────▼──────────┐
                   │   AuthResource     │
                   │  (valida creds)    │
                   └─────────┬──────────┘
                             │
                   ┌─────────▼──────────┐
                   │    JwtService      │
                   │ (firma con RSA)    │
                   └─────────┬──────────┘
                             │
                   ┌─────────▼──────────┐
                   │  2. Retorna JWT    │
                   │   (token válido)   │
                   └─────────┬──────────┘
                             │
┌────────────────────────────▼─────────────────────────────────┐
│  Cliente guarda el token y lo envía en requests posteriores  │
│        Authorization: Bearer eyJhbGciOiJSUzI1NiI...          │
└────────────────────────────┬─────────────────────────────────┘
                             │
              ┌──────────────▼──────────────┐
              │ 3. GET /internal/secrets/*  │
              │   + Authorization Header    │
              └──────────────┬──────────────┘
                             │
              ┌──────────────▼──────────────┐
              │   Quarkus SmallRye JWT      │
              │  (valida firma con public   │
              │   key y extrae claims)      │
              └──────────────┬──────────────┘
                             │
              ┌──────────────▼──────────────┐
              │  InternalSecretResource     │
              │  (@RolesAllowed("employee"))│
              │  (usa jwt.getSubject())     │
              └──────────────┬──────────────┘
                             │
              ┌──────────────▼──────────────┐
              │   Retorna solo secretos     │
              │   del usuario autenticado   │
              └─────────────────────────────┘
```

---

## 🔧 Requisitos Previos

- ✅ Haber completado la **Parte 1** (Autenticación Básica)
- ✅ Tener Quarkus corriendo: `./mvnw quarkus:dev`
- ✅ Tener `curl` instalado para las pruebas
- ✅ (Opcional) Python 3 para formatear JSON

---

## 📦 Componentes Implementados

### 1. **Generación de Llaves RSA**

```bash
# Llave privada (firma tokens)
src/main/resources/privateKey.pem

# Llave pública (verifica tokens)
src/main/resources/publicKey.pem
```

### 2. **Servicio de JWT** (`JwtService.java`)

```java
@ApplicationScoped
public class JwtService {
    public String generateToken(String userId, String email, Set<String> roles) {
        return Jwt.issuer("https://vaultcorp.com")
                .subject(userId)
                .claim("email", email)
                .groups(roles)
                .expiresIn(3600) // 1 hora
                .sign();
    }
}
```

### 3. **Endpoint de Autenticación** (`AuthResource.java`)

```java
@POST
@Path("/login")
public Response login(LoginRequest request) {
    // Valida credenciales
    // Genera JWT
    // Retorna TokenResponse
}
```

### 4. **Endpoints Protegidos** (`InternalSecretResource.java`)

```java
@Path("/api/internal/secrets")
public class InternalSecretResource {
    
    @Inject JsonWebToken jwt;
    
    @GET
    @Path("/my-secrets")
    @RolesAllowed("employee")
    public Response getMySecrets() {
        String userId = jwt.getSubject(); // Extrae del token
        // Retorna solo secretos del usuario
    }
}
```

---

## 🚀 Cómo Ejecutar el Ejercicio

### Opción 1: Script Automatizado (Recomendado)

```bash
# Asegúrate de que Quarkus esté corriendo
./mvnw quarkus:dev

# En otra terminal, ejecuta el script de pruebas
./test-part2-jwt.sh
```

El script te guiará paso a paso por todas las pruebas con explicaciones educativas.

### Opción 2: Pruebas Manuales con curl

#### Paso 1: Hacer Login y Obtener Token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "emp001",
    "password": "pass001"
  }'
```

**Respuesta:**
```json
{
  "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "expiresIn": 3600
}
```

#### Paso 2: Guardar el Token

```bash
# Guarda el token en una variable
TOKEN="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

#### Paso 3: Ver tu Perfil

```bash
curl http://localhost:8080/api/internal/secrets/profile \
  -H "Authorization: Bearer $TOKEN"
```

**Respuesta:**
```json
{
  "userId": "emp001",
  "email": "juan.perez@vaultcorp.com",
  "roles": ["employee"],
  "tokenIssuer": "https://vaultcorp.com"
}
```

#### Paso 4: Crear un Secreto

```bash
curl -X POST http://localhost:8080/api/internal/secrets \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Mi Clave Secreta",
    "content": "super-secret-123",
    "level": "INTERNAL"
  }'
```

#### Paso 5: Ver tus Secretos

```bash
curl http://localhost:8080/api/internal/secrets/my-secrets \
  -H "Authorization: Bearer $TOKEN"
```

---

## 📋 Endpoints Disponibles

### 🔓 Endpoint Público

| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| `POST` | `/api/auth/login` | Genera un JWT | No |

**Request Body:**
```json
{
  "username": "emp001",
  "password": "pass001"
}
```

**Response:**
```json
{
  "token": "eyJhbGci...",
  "type": "Bearer",
  "expiresIn": 3600
}
```

### 🔒 Endpoints Protegidos con JWT

| Método | Ruta | Descripción | Rol Requerido |
|--------|------|-------------|---------------|
| `GET` | `/api/internal/secrets/profile` | Ver perfil del usuario | `employee` |
| `GET` | `/api/internal/secrets/my-secrets` | Listar secretos propios | `employee` |
| `POST` | `/api/internal/secrets` | Crear un secreto | `employee` |

**Todas las peticiones deben incluir:**
```
Authorization: Bearer <tu-token-jwt>
```

---

## 👥 Usuarios de Prueba

| Username | Password | Email | Rol |
|----------|----------|-------|-----|
| `emp001` | `pass001` | juan.perez@vaultcorp.com | `employee` |
| `emp002` | `pass002` | maria.gonzalez@vaultcorp.com | `employee` |
| `emp003` | `pass003` | carlos.rodriguez@vaultcorp.com | `employee` |

---

## 🔍 Conceptos Clave Aprendidos

### 1. **Stateless Authentication**

A diferencia de las sesiones tradicionales, JWT no requiere que el servidor almacene información de sesión. Todo está en el token.

```
Sesiones Tradicionales              JWT (Stateless)
────────────────────────            ────────────────
1. Login                            1. Login
2. Servidor crea sesión             2. Servidor genera JWT
3. Servidor guarda en memoria       3. Servidor NO guarda nada
4. Cliente recibe session ID        4. Cliente recibe JWT
5. Cliente envía cookie             5. Cliente envía Bearer token
6. Servidor busca en memoria        6. Servidor valida firma
```

### 2. **Claims del JWT**

Los **claims** son declaraciones sobre el usuario. En nuestro caso:

```json
{
  "iss": "https://vaultcorp.com",     // Emisor del token
  "sub": "emp001",                     // Subject (ID del usuario)
  "email": "juan.perez@vaultcorp.com", // Email del usuario
  "upn": "juan.perez@vaultcorp.com",   // User Principal Name
  "groups": ["employee"],              // Roles del usuario
  "iat": 1234567890,                   // Issued At (timestamp)
  "exp": 1234571490,                   // Expiration (timestamp)
  "jti": "abc-123-xyz"                 // JWT ID (único)
}
```

### 3. **Flujo de Autenticación**

```
┌──────────┐                           ┌──────────┐
│ Cliente  │                           │ Servidor │
└────┬─────┘                           └────┬─────┘
     │                                      │
     │  POST /login                         │
     │  {user, pass}                        │
     ├─────────────────────────────────────>│
     │                                      │
     │                  Valida credenciales │
     │                  Genera JWT firmado  │
     │                                      │
     │  200 OK                              │
     │  {token: "eyJ..."}                   │
     │<─────────────────────────────────────┤
     │                                      │
     │  GET /internal/secrets/my-secrets    │
     │  Authorization: Bearer eyJ...        │
     ├─────────────────────────────────────>│
     │                                      │
     │              Verifica firma con RSA  │
     │              Extrae claims (sub)     │
     │              Filtra por ownerId      │
     │                                      │
     │  200 OK                              │
     │  [{secretos del usuario}]            │
     │<─────────────────────────────────────┤
     │                                      │
```

### 4. **Aislamiento Multi-Tenancy**

Cada usuario solo puede acceder a **sus propios recursos**:

```java
@GET
@Path("/my-secrets")
@RolesAllowed("employee")
public Response getMySecrets() {
    String userId = jwt.getSubject(); // "emp001" del token
    
    // Filtra SOLO por el usuario autenticado
    List<Secret> secrets = secretService.getSecretsByOwner(userId);
    
    return Response.ok(secrets).build();
}
```

✅ **emp001** solo ve secretos con `ownerId = "emp001"`  
✅ **emp002** solo ve secretos con `ownerId = "emp002"`  
❌ **Ningún usuario puede ver secretos de otros**

---

## 🔐 Seguridad: RSA vs HMAC

### RSA (Asimétrica) - Lo que usamos

```
┌─────────────┐                    ┌─────────────┐
│  Servidor   │                    │   Cliente   │
│             │                    │             │
│ Firma con   │──── JWT firmado ───>│ Verifica    │
│ PRIVATE KEY │    (no puede       │ con PUBLIC  │
│             │     alterarse)     │ KEY         │
└─────────────┘                    └─────────────┘

✅ Más seguro
✅ Clave pública puede distribuirse
✅ Ideal para microservicios
```

### HMAC (Simétrica) - Alternativa

```
┌─────────────┐                    ┌─────────────┐
│  Servidor   │                    │   Cliente   │
│             │                    │             │
│ Firma con   │──── JWT firmado ───>│ Verifica    │
│ SECRET KEY  │                    │ con MISMO   │
│             │                    │ SECRET KEY  │
└─────────────┘                    └─────────────┘

⚠️  Clave debe ser compartida
⚠️  Menos seguro si se expone
```

---

## 🧪 Decodificar un JWT (Educativo)

Puedes decodificar cualquier JWT en [jwt.io](https://jwt.io) o con este comando:

```bash
# Guardar el token
TOKEN="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."

# Extraer y decodificar el payload (segunda parte)
echo $TOKEN | awk -F'.' '{print $2}' | base64 -d | python3 -m json.tool
```

**⚠️ Importante:** JWT **NO está encriptado**, solo está **codificado en Base64**. Por eso:
- ❌ Nunca incluyas contraseñas o datos sensibles en el payload
- ❌ Nunca confíes en un JWT sin verificar su firma
- ✅ Siempre valida la firma antes de confiar en los claims

---

## ⚙️ Configuración (application.properties)

```properties
# Configuración JWT
mp.jwt.verify.publickey.location=publicKey.pem
mp.jwt.verify.issuer=https://vaultcorp.com
smallrye.jwt.sign.key.location=privateKey.pem

# Tolerancia para clock skew (diferencias de tiempo entre servidores)
mp.jwt.verify.clock.skew=60

# Deshabilitar OIDC temporalmente (para Parte 3)
quarkus.oidc.enabled=false
```

---

## ❓ Troubleshooting

### Problema: "Token issued to client quarkus-app is not active"

**Causa:** Conflicto entre las extensiones `oidc` y `smallrye-jwt`.

**Solución:** Asegúrate de tener en `application.properties`:
```properties
quarkus.oidc.enabled=false
```

### Problema: "401 Unauthorized" al usar el token

**Verificar:**
1. ¿El header está correcto? `Authorization: Bearer <token>`
2. ¿El token no expiró? Los tokens duran 1 hora
3. ¿Las llaves RSA existen en `src/main/resources/`?

### Problema: No puedo ver los secretos de otro usuario

**¡Eso es correcto!** Es una característica de seguridad. Cada usuario solo puede ver sus propios secretos.

---

## 📚 Recursos Adicionales

- [RFC 7519 - JSON Web Token (JWT)](https://tools.ietf.org/html/rfc7519)
- [Quarkus Security JWT Guide](https://quarkus.io/guides/security-jwt)
- [jwt.io - Debugger de JWT](https://jwt.io)
- [SmallRye JWT Documentation](https://github.com/smallrye/smallrye-jwt)

---

## 🎯 Próximos Pasos

Una vez dominada la Parte 2, estás listo para:

### **Parte 3: OIDC con Keycloak** 🔜
- Integración con proveedores de identidad externos
- OpenID Connect (OIDC)
- Single Sign-On (SSO)
- Federación de identidades

---

## ✅ Checklist de Verificación

Antes de pasar a la Parte 3, asegúrate de haber logrado:

- [ ] Login genera un JWT válido
- [ ] Puedes ver tu perfil con el token
- [ ] Puedes crear secretos asociados a tu usuario
- [ ] Solo ves tus propios secretos (no los de otros)
- [ ] Entiendes qué contiene un JWT (claims)
- [ ] Sabes la diferencia entre RSA y HMAC
- [ ] Comprendes el concepto de autenticación stateless

---

## 📝 Notas del Instructor

**Puntos clave para enfatizar con los alumnos:**

1. **JWT no es encriptación, es codificación**: Cualquiera puede decodificar el payload con Base64
2. **La firma es lo que garantiza la integridad**: Sin la clave privada, no se puede crear un token válido
3. **Stateless = Escalabilidad**: Perfecto para microservicios distribuidos
4. **Expiración es crítica**: Los tokens DEBEN tener tiempo de vida limitado
5. **Aislamiento por diseño**: El backend debe SIEMPRE filtrar por el usuario del token
