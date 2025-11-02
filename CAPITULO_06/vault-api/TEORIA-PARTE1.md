# 📚 Teoría: Autenticación y Autorización Básica

## 🎯 Introducción: Los Guardianes de tu Aplicación

Imagina que tu aplicación es un **edificio de oficinas corporativo**. En este edificio:

- La **puerta principal** tiene un guardia que verifica tu identidad (¿Eres quien dices ser?)
- Una vez dentro, hay **puertas con tarjetas de acceso** que verifican tus permisos (¿Tienes autorización para entrar aquí?)
- Algunas áreas son **públicas** (lobby, cafetería) - cualquiera puede entrar
- Otras son **restringidas** (sala de servidores, oficina del CEO) - solo personal autorizado

En seguridad de aplicaciones, estos conceptos se llaman:
- 🔐 **Autenticación** (Authentication) - Verificar tu identidad
- 🔒 **Autorización** (Authorization) - Verificar tus permisos

---

## 🔐 Autenticación vs Autorización

### Definiciones Formales

**Autenticación (Authentication)**
> Proceso de verificar que una entidad (usuario, sistema, dispositivo) es quien dice ser.

**Analogía:** Mostrar tu DNI en la entrada del edificio. El guardia confirma que TÚ eres la persona de la foto.

**Autorización (Authorization)**
> Proceso de verificar que una entidad autenticada tiene permiso para realizar una acción específica o acceder a un recurso.

**Analogía:** Tu tarjeta de acceso te permite entrar al piso 3, pero no al piso 5 donde están las finanzas.

---

### Comparación Visual

```
┌─────────────────────────────────────────────────────────┐
│                   AUTENTICACIÓN                         │
│              "¿Quién eres tú?"                          │
└─────────────────────────────────────────────────────────┘

Usuario: "Soy Juan Pérez"
Sistema: "Demuéstralo"
Usuario: [Proporciona credenciales]
Sistema: ✅ "OK, confirmado que eres Juan Pérez"

         │
         │ Usuario ahora está AUTENTICADO
         ▼

┌─────────────────────────────────────────────────────────┐
│                   AUTORIZACIÓN                          │
│           "¿Qué puedes hacer?"                          │
└─────────────────────────────────────────────────────────┘

Usuario: [Intenta eliminar un secreto]
Sistema: "Verificando tus permisos..."
Sistema: "Tu rol es 'auditor'"
Sistema: "Los auditores NO pueden eliminar"
Sistema: ❌ "Acceso denegado"
```

---

### Tabla Comparativa

| Aspecto | Autenticación | Autorización |
|---------|---------------|--------------|
| **Pregunta** | ¿Quién eres? | ¿Qué puedes hacer? |
| **Verifica** | Identidad | Permisos |
| **Ocurre** | Primero | Después |
| **Método** | Credenciales, tokens, biometría | Roles, permisos, políticas |
| **Error** | HTTP 401 Unauthorized | HTTP 403 Forbidden |
| **Analogía** | Mostrar DNI | Usar tarjeta de acceso |
| **Ejemplo** | Login con usuario/password | Admin puede eliminar, auditor no |

---

## 🔑 HTTP Basic Authentication

### ¿Qué es Basic Auth?

**HTTP Basic Authentication** es el método de autenticación más simple en HTTP. El cliente envía las credenciales (usuario:contraseña) codificadas en Base64 en **cada request**.

### Anatomía de una Petición con Basic Auth

```http
GET /api/admin/secrets/all HTTP/1.1
Host: localhost:8080
Authorization: Basic YWRtaW46YWRtaW4xMjM=
```

**Decodificando el header:**
```
YWRtaW46YWRtaW4xMjM= 
    │
    └──> Base64 decode
         │
         └──> "admin:admin123"
```

### Cómo Funciona (Paso a Paso)

```
┌────────────────────────────────────────────────────────┐
│           FLUJO DE BASIC AUTHENTICATION                │
└────────────────────────────────────────────────────────┘

1. Cliente hace request SIN credenciales
   ──────────────────────────────────────────>
   GET /api/admin/secrets/all
   
2. Servidor responde con desafío
   <──────────────────────────────────────────
   HTTP/1.1 401 Unauthorized
   WWW-Authenticate: Basic realm="Protected"
   
3. Cliente envía credenciales (Base64)
   ──────────────────────────────────────────>
   GET /api/admin/secrets/all
   Authorization: Basic YWRtaW46YWRtaW4xMjM=
   
4. Servidor valida credenciales
   - Decodifica Base64 → "admin:admin123"
   - Busca usuario en BD/config
   - Verifica contraseña
   
5a. Si es correcto:
    <──────────────────────────────────────────
    HTTP/1.1 200 OK
    {data...}
    
5b. Si es incorrecto:
    <──────────────────────────────────────────
    HTTP/1.1 401 Unauthorized
```

---

### Implementación en Quarkus

**Configuración (application.properties):**
```properties
# Habilitar autenticación básica
quarkus.http.auth.basic=true

# Usuarios embebidos
quarkus.security.users.embedded.enabled=true
quarkus.security.users.embedded.plain-text=true
quarkus.security.users.embedded.users.admin=admin123
quarkus.security.users.embedded.roles.admin=vault-admin
```

**Uso en Endpoint:**
```java
@GET
@Path("/secrets")
@RolesAllowed("vault-admin")  // Solo usuarios con rol vault-admin
public List<Secret> getAllSecrets() {
    return secretService.getAllSecrets();
}
```

**Request con curl:**
```bash
curl -u admin:admin123 http://localhost:8080/api/admin/secrets/all
```

---

### ⚠️ Limitaciones de Basic Auth

| Limitación | Descripción | Impacto |
|------------|-------------|---------|
| **Credenciales en cada request** | Usuario y contraseña viajan en TODAS las peticiones | Alto consumo de ancho de banda |
| **Base64 NO es encriptación** | Fácilmente decodificable | Vulnerable sin HTTPS |
| **No hay logout** | No hay forma de invalidar credenciales | Sesión "permanente" |
| **No hay expiración** | Las credenciales no caducan | Riesgo de seguridad |
| **Stateful en servidor** | Debe verificar contra BD/config cada vez | No escala bien |
| **Un solo set de credenciales** | No hay refresh, no hay tokens múltiples | Limitado |

---

### ✅ Cuándo Usar Basic Auth

**SÍ usar Basic Auth cuando:**
- ✅ Aplicaciones internas simples
- ✅ APIs de desarrollo/testing
- ✅ Microservicios internos detrás de un API Gateway
- ✅ Scripts automatizados con credenciales de servicio
- ✅ Prototipado rápido

**NO usar Basic Auth cuando:**
- ❌ API pública en internet
- ❌ Aplicaciones móviles
- ❌ Necesitas logout funcional
- ❌ Requieres escalabilidad horizontal masiva
- ❌ Múltiples niveles de autenticación

---

## 🎭 Roles y Permisos

### ¿Qué es un Rol?

Un **rol** es una etiqueta que agrupa un conjunto de permisos. En lugar de asignar permisos individualmente a cada usuario, les asignas un rol.

**Analogía:** En un hospital:
- **Rol: Doctor** → Puede diagnosticar, prescribir medicinas, operar
- **Rol: Enfermero** → Puede administrar medicinas, tomar signos vitales
- **Rol: Recepcionista** → Puede agendar citas, ver información básica

### RBAC (Role-Based Access Control)

```
┌─────────────────────────────────────────────────────────┐
│              CONTROL DE ACCESO BASADO EN ROLES          │
└─────────────────────────────────────────────────────────┘

Usuario                Rol                  Permisos
───────────────────────────────────────────────────────────
Juan Pérez      ──►   vault-admin    ──►   - Leer secretos
                                            - Crear secretos
                                            - Eliminar secretos
                                            - Ver estadísticas
                                            
María González  ──►   vault-auditor  ──►   - Leer secretos
                                            - Ver estadísticas
                                            (NO puede eliminar)
                                            
Carlos Rojas    ──►   employee       ──►   - Login JWT
                                            - Ver sus secretos
                                            - Crear sus secretos
```

---

### Jerarquía de Roles (Opcional)

En sistemas más complejos, los roles pueden heredarse:

```
                    ┌──────────────┐
                    │  SUPER_ADMIN │
                    │  (todos los  │
                    │   permisos)  │
                    └──────┬───────┘
                           │
              ┌────────────┴────────────┐
              ▼                         ▼
       ┌──────────────┐         ┌──────────────┐
       │  VAULT_ADMIN │         │    MANAGER   │
       │  (gestiona   │         │  (aprueba    │
       │   secretos)  │         │   cambios)   │
       └──────┬───────┘         └──────────────┘
              │
    ┌─────────┴─────────┐
    ▼                   ▼
┌──────────────┐  ┌──────────────┐
│VAULT_AUDITOR │  │   EMPLOYEE   │
│ (solo lee)   │  │  (operador)  │
└──────────────┘  └──────────────┘
```

**En nuestro ejercicio usamos roles planos (sin jerarquía) para simplicidad.**

---

## 🛡️ Anotaciones de Seguridad en Quarkus

### @PermitAll

**Definición:** Permite acceso público sin autenticación.

```java
@GET
@Path("/health")
@PermitAll  // ✅ Cualquiera puede acceder
public String healthCheck() {
    return "VaultCorp Admin API is running";
}
```

**Uso:** Health checks, páginas de login, documentación pública

---

### @RolesAllowed

**Definición:** Solo usuarios autenticados con el rol especificado pueden acceder.

**Sintaxis:**
```java
// Un solo rol
@RolesAllowed("vault-admin")

// Múltiples roles (OR lógico)
@RolesAllowed({"vault-admin", "vault-auditor"})
```

**Ejemplos:**

```java
// Solo admins
@DELETE
@Path("/{id}")
@RolesAllowed("vault-admin")
public Response deleteSecret(@PathParam("id") String id) {
    secretService.deleteSecret(id);
    return Response.ok().build();
}

// Admins O auditores
@GET
@Path("/stats")
@RolesAllowed({"vault-admin", "vault-auditor"})
public Response getStatistics() {
    return Response.ok(Map.of(
        "totalSecrets", secretService.getTotalCount()
    )).build();
}
```

---

### @Authenticated (No usada en este ejercicio)

**Definición:** Requiere autenticación pero acepta cualquier rol.

```java
@GET
@Path("/profile")
@Authenticated  // Solo requiere estar autenticado
public Response getProfile() {
    // Cualquier usuario autenticado puede acceder
    return Response.ok().build();
}
```

**Diferencia con @RolesAllowed:**
- `@Authenticated`: "Debes estar logueado" (cualquier rol)
- `@RolesAllowed`: "Debes tener este rol específico"

---

### @DenyAll (No usada en este ejercicio)

**Definición:** Niega acceso a todos, incluso admins.

```java
@DELETE
@Path("/destroy-everything")
@DenyAll  // ❌ Nadie puede acceder
public Response dangerousOperation() {
    // Este endpoint está deshabilitado
}
```

**Uso:** Desactivar endpoints temporalmente sin eliminar el código.

---

### Comparativa de Anotaciones

| Anotación | Requiere Auth | Verifica Rol | Uso Común |
|-----------|---------------|--------------|-----------|
| `@PermitAll` | ❌ No | ❌ No | Recursos públicos |
| `@Authenticated` | ✅ Sí | ❌ No | Perfil de usuario |
| `@RolesAllowed("admin")` | ✅ Sí | ✅ Sí | Operaciones privilegiadas |
| `@DenyAll` | N/A | N/A | Desactivar endpoints |

---

## 🚦 Códigos de Estado HTTP de Seguridad

### HTTP 401 Unauthorized

**Significado:** "No sé quién eres. Necesito que te identifiques."

**Cuándo ocurre:**
- No enviaste credenciales
- Credenciales inválidas (usuario/contraseña incorrectos)
- Token expirado o inválido

**Ejemplo:**
```bash
curl http://localhost:8080/api/admin/secrets/all
# HTTP/1.1 401 Unauthorized
# www-authenticate: Basic
```

**Header importante:**
```
WWW-Authenticate: Basic realm="Protected"
```
Le dice al cliente qué método de autenticación usar.

---

### HTTP 403 Forbidden

**Significado:** "Sé quién eres, pero no tienes permiso para hacer esto."

**Cuándo ocurre:**
- Estás autenticado correctamente
- Pero tu rol no tiene los permisos necesarios

**Ejemplo:**
```bash
# Auditor intenta eliminar (solo admins pueden)
curl -X DELETE -u auditor:auditor123 http://localhost:8080/api/admin/secrets/123
# HTTP/1.1 403 Forbidden
```

---

### Comparación 401 vs 403

```
┌────────────────────────────────────────────────────────┐
│                  HTTP 401 vs 403                       │
└────────────────────────────────────────────────────────┘

┌─────────────────────┐              ┌─────────────────────┐
│    HTTP 401         │              │    HTTP 403         │
│  "Unauthorized"     │              │   "Forbidden"       │
└─────────────────────┘              └─────────────────────┘
                                     
No sé quién eres                     Sé quién eres, pero...
                                     
┌──────────────┐                     ┌──────────────┐
│ Sin login    │                     │ Con login    │
│   o login    │                     │  pero sin    │
│  incorrecto  │                     │   permiso    │
└──────────────┘                     └──────────────┘
      │                                     │
      ▼                                     ▼
"Necesito que                         "No tienes
te identifiques"                       autorización"

Solución:                             Solución:
- Proporciona                         - Contacta admin
  credenciales                        - Solicita permisos
- Verifica user/pass                  - Cambia de cuenta
```

**Analogía del Edificio:**

**401:** Intentas entrar al edificio sin mostrar tu DNI. El guardia te detiene en la entrada.

**403:** Entraste al edificio con tu DNI, pero intentas acceder al piso ejecutivo y tu tarjeta no abre esa puerta.

---

### Otros Códigos Relacionados

| Código | Nombre | Significado | Ejemplo |
|--------|--------|-------------|---------|
| **200** | OK | Todo bien | Operación exitosa |
| **201** | Created | Recurso creado | POST exitoso |
| **400** | Bad Request | Request malformado | JSON inválido |
| **401** | Unauthorized | No autenticado | Sin credenciales |
| **403** | Forbidden | No autorizado | Sin permisos |
| **404** | Not Found | Recurso no existe | ID inexistente |
| **500** | Internal Server Error | Error en servidor | Bug, excepción |

---

## 🔒 Stateful vs Stateless Authentication

### Autenticación Stateful (Session-Based)

```
┌────────────────────────────────────────────────────────┐
│             AUTENTICACIÓN STATEFUL                     │
│            (Con Sesiones en Servidor)                  │
└────────────────────────────────────────────────────────┘

1. Login
   Usuario ──[user/pass]──> Servidor
   
2. Servidor crea sesión y la guarda
   ┌─────────────────────────────┐
   │  Memoria/Redis/Base Datos   │
   │  ───────────────────────    │
   │  sessionId: abc123          │
   │  userId: 1                  │
   │  roles: [admin]             │
   │  loginTime: 14:00           │
   └─────────────────────────────┘
   
3. Servidor retorna cookie
   Servidor ──[Set-Cookie: sessionId=abc123]──> Usuario
   
4. Requests posteriores
   Usuario ──[Cookie: sessionId=abc123]──> Servidor
   
5. Servidor busca sesión
   Servidor consulta: sessions["abc123"]
   └─> Encuentra: {userId: 1, roles: [admin]}
   
6. Procesa request
   Servidor ──[Response]──> Usuario
```

**Características:**
- ✅ Control total (puedes invalidar sesiones)
- ✅ Fácil implementar logout
- ✅ Información sensible en servidor
- ❌ Requiere almacenamiento compartido
- ❌ Difícil escalar horizontalmente
- ❌ Latencia por consultas

---

### Autenticación Stateless (Token-Based)

```
┌────────────────────────────────────────────────────────┐
│             AUTENTICACIÓN STATELESS                    │
│              (Sin Sesiones en Servidor)                │
└────────────────────────────────────────────────────────┘

1. Login
   Usuario ──[user/pass]──> Servidor
   
2. Servidor genera token (NO guarda nada)
   Token = sign({userId: 1, roles: [admin]}, secretKey)
   
3. Servidor retorna token
   Servidor ──[{token: "eyJhbG..."}]──> Usuario
   
4. Usuario guarda token (localStorage/cookie)
   
5. Requests posteriores
   Usuario ──[Authorization: Bearer eyJhbG...]──> Servidor
   
6. Servidor verifica firma (NO consulta BD)
   claims = verify(token, secretKey)
   └─> Extrae: {userId: 1, roles: [admin]}
   
7. Procesa request
   Servidor ──[Response]──> Usuario
```

**Características:**
- ✅ No requiere almacenamiento
- ✅ Escala horizontalmente
- ✅ Baja latencia
- ✅ Funciona entre dominios
- ❌ Difícil revocar tokens
- ❌ Token puede ser grande
- ❌ Payload es visible

---

### Comparativa Detallada

| Aspecto | Stateful (Sesiones) | Stateless (Tokens) |
|---------|--------------------|--------------------|
| **Almacenamiento** | Servidor guarda sesiones | Cliente guarda token |
| **Escalabilidad** | Requiere sesiones compartidas | Escala sin problemas |
| **Revocación** | Inmediata (borra sesión) | Difícil (esperar expiración) |
| **Latencia** | Alta (consulta BD/Redis) | Baja (solo verifica firma) |
| **Logout** | Fácil | Complicado |
| **Tamaño** | Cookie pequeña (32 bytes) | Token grande (1-2KB) |
| **CORS** | Complicado | Simple |
| **Ejemplo** | PHP Sessions, Rails | JWT, OAuth2 |

**En nuestro ejercicio:**
- **Parte 1 (Basic Auth)** → Híbrido: credenciales en cada request, validación contra config (casi stateless)
- **Parte 2 (JWT)** → Stateless puro

---

## 🛡️ Buenas Prácticas de Seguridad

### ✅ Qué HACER

| Práctica | Razón | Implementación |
|----------|-------|----------------|
| **Usar HTTPS en producción** | Credenciales sin encriptar son vulnerables | Certificado SSL/TLS |
| **Hashear contraseñas** | Nunca guardar en texto plano | BCrypt, Argon2 |
| **Principio de menor privilegio** | Dar solo permisos necesarios | Roles granulares |
| **Validar entrada** | Prevenir inyección SQL, XSS | `@NotBlank`, sanitización |
| **Auditar accesos** | Rastrear quién hizo qué | Logs de seguridad |
| **Expirar sesiones** | Limitar ventana de ataque | Timeout de inactividad |
| **Rate limiting** | Prevenir brute force | Límite de intentos |
| **Dos factores (2FA)** | Capa adicional de seguridad | TOTP, SMS |

---

### ❌ Qué NO HACER

| Anti-patrón | Por qué es malo | Consecuencia |
|-------------|-----------------|--------------|
| **Usar HTTP en producción** | Credenciales en texto plano | Interceptación |
| **Contraseñas débiles** | Fáciles de adivinar | Acceso no autorizado |
| **Sin rate limiting** | Vulnerable a brute force | Cuentas comprometidas |
| **Roles demasiado amplios** | Principio de menor privilegio violado | Escalada de privilegios |
| **Sin logs de auditoría** | No puedes rastrear ataques | Imposible investigar |
| **Confiar en el cliente** | Cliente puede ser modificado | Validar siempre en servidor |
| **Errores verbosos** | Revelan información del sistema | "User not found" vs "Invalid credentials" |

---

## 🎭 Ataques Comunes y Defensas

### 1. Brute Force Attack

**Ataque:** Probar todas las combinaciones de contraseñas.

```
Atacante intenta:
- admin:123456
- admin:password
- admin:admin
- admin:qwerty
- ... (millones de intentos)
```

**Defensas:**
- ✅ Rate limiting (3 intentos por minuto)
- ✅ Bloqueo temporal tras N intentos fallidos
- ✅ CAPTCHA después de varios fallos
- ✅ Alertas de seguridad
- ✅ Contraseñas fuertes obligatorias

```java
@Inject
@CacheName("failed-logins")
Cache loginAttempts;

public void validateLogin(String username, String password) {
    int attempts = loginAttempts.get(username).orElse(0);
    
    if (attempts >= 5) {
        throw new SecurityException("Cuenta bloqueada temporalmente");
    }
    
    if (!isValidPassword(password)) {
        loginAttempts.put(username, attempts + 1);
        throw new UnauthorizedException("Credenciales inválidas");
    }
    
    // Login exitoso
    loginAttempts.invalidate(username);
}
```

---

### 2. Credential Stuffing

**Ataque:** Usar credenciales filtradas de otros sitios.

```
Datos filtrados de LinkedIn:
- usuario@email.com:Password123

Atacante prueba en VaultCorp:
- usuario@email.com:Password123
```

**Defensas:**
- ✅ Verificar contraseñas contra bases de datos de filtraciones (HaveIBeenPwned API)
- ✅ Requerir 2FA
- ✅ Detectar patrones anómalos de login
- ✅ Notificar logins desde nuevos dispositivos

---

### 3. Session Hijacking

**Ataque:** Robar la cookie/token de sesión.

```
Atacante intercepta:
Cookie: sessionId=abc123

Atacante usa la cookie robada:
curl -H "Cookie: sessionId=abc123" http://...
```

**Defensas:**
- ✅ HTTPS obligatorio
- ✅ Cookies con flag `httpOnly` (no accesibles desde JS)
- ✅ Cookies con flag `secure` (solo HTTPS)
- ✅ Regenerar session ID tras login
- ✅ Validar IP/User-Agent

```java
@GET
public Response login(...) {
    return Response.ok()
        .cookie(new NewCookie(
            "sessionId",
            sessionId,
            "/",
            null,
            null,
            3600,
            true,   // secure = true (solo HTTPS)
            true    // httpOnly = true (no JS)
        ))
        .build();
}
```

---

### 4. Privilege Escalation

**Ataque:** Usuario normal obtiene permisos de admin.

**Ejemplo:** Modificar request para cambiar rol

```javascript
// Request original
POST /api/users
{
  "username": "hacker",
  "role": "user"
}

// Request modificado (ataque)
POST /api/users
{
  "username": "hacker",
  "role": "admin"  // 🚨
}
```

**Defensas:**
- ✅ NUNCA confiar en el cliente para roles
- ✅ Validar permisos en CADA operación
- ✅ Roles asignados solo por admins
- ✅ Auditoría de cambios de permisos

```java
@POST
@Path("/users")
@RolesAllowed("admin")  // Solo admin puede crear usuarios
public Response createUser(UserRequest request) {
    // NUNCA confiar en request.role del cliente
    // Asignar rol por defecto o validar estrictamente
    
    if (request.getRole().equals("admin")) {
        // Verificación adicional
        if (!securityIdentity.hasRole("super-admin")) {
            throw new ForbiddenException("No puedes crear admins");
        }
    }
    
    userService.create(request);
    return Response.ok().build();
}
```

---

## 🧪 Testing de Seguridad

### Unit Tests

```java
@Test
public void testPermitAllEndpoint() {
    given()
    .when()
        .get("/api/admin/secrets/health")
    .then()
        .statusCode(200);
}

@Test
public void testProtectedEndpointWithoutAuth() {
    given()
    .when()
        .get("/api/admin/secrets/all")
    .then()
        .statusCode(401);
}

@Test
public void testProtectedEndpointWithValidAuth() {
    given()
        .auth().basic("admin", "admin123")
    .when()
        .get("/api/admin/secrets/all")
    .then()
        .statusCode(200);
}

@Test
public void testForbiddenForWrongRole() {
    given()
        .auth().basic("auditor", "auditor123")
    .when()
        .delete("/api/admin/secrets/123")
    .then()
        .statusCode(403);
}
```

---

### Checklist de Testing de Seguridad

- [ ] Endpoints públicos accesibles sin auth
- [ ] Endpoints protegidos rechazan requests sin auth (401)
- [ ] Usuarios con rol correcto pueden acceder (200)
- [ ] Usuarios sin rol correcto son rechazados (403)
- [ ] Credenciales inválidas son rechazadas (401)
- [ ] No se filtra información sensible en errores
- [ ] Rate limiting funciona
- [ ] Logout invalida sesiones/tokens
- [ ] HTTPS es obligatorio en producción
- [ ] Contraseñas están hasheadas

---

## 📚 Patrones de Diseño de Seguridad

### 1. Defense in Depth (Defensa en Profundidad)

**Principio:** Múltiples capas de seguridad.

```
┌─────────────────────────────────────┐
│     Capa 1: Firewall/WAF            │
├─────────────────────────────────────┤
│     Capa 2: Rate Limiting           │
├─────────────────────────────────────┤
│     Capa 3: Autenticación           │
├─────────────────────────────────────┤
│     Capa 4: Autorización            │
├─────────────────────────────────────┤
│     Capa 5: Validación Input        │
├─────────────────────────────────────┤
│     Capa 6: Auditoría               │
└─────────────────────────────────────┘
```

Si una capa falla, las demás protegen.

---

### 2. Least Privilege (Menor Privilegio)

**Principio:** Dar solo los permisos mínimos necesarios.

```
❌ Mal:
Usuario "empleado" tiene rol "admin" 
  "por si acaso necesita hacer algo"

✅ Bien:
Usuario "empleado" tiene rol "employee"
  - Puede: Ver sus secretos, crear secretos
  - NO puede: Eliminar secretos de otros, ver estadísticas globales
```

---

### 3. Fail Secure (Fallar Seguro)

**Principio:** Si algo falla, negar acceso por defecto.

```java
@GET
@Path("/secret/{id}")
public Response getSecret(@PathParam("id") String id) {
    try {
        // Verificar permisos
        if (!hasPermission(id)) {
            return Response.status(403).build();
        }
        
        Secret secret = service.getById(id);
        return Response.ok(secret).build();
        
    } catch (Exception e) {
        // ✅ En caso de error, NEGAR acceso
        log.error("Error checking permissions", e);
        return Response.status(403).build();
        
        // ❌ NO hacer esto:
        // return Response.ok(secret).build();  // Dar acceso por defecto
    }
}
```

---

## 🎓 Conceptos Avanzados

### 1. Separation of Concerns

Separar autenticación de autorización:

```java
// ❌ Mal: Todo mezclado
@GET
@Path("/secrets")
public Response getSecrets() {
    // Autenticación
    User user = authenticateUser(request);
    
    // Autorización
    if (!user.hasRole("admin")) {
        return Response.status(403).build();
    }
    
    // Lógica de negocio
    return Response.ok(service.getAll()).build();
}

// ✅ Bien: Separado con anotaciones
@GET
@Path("/secrets")
@RolesAllowed("admin")  // Autorización declarativa
public Response getSecrets() {
    // Solo lógica de negocio
    return Response.ok(service.getAll()).build();
}
```

---

### 2. Context-Based Authorization

Autorización basada en contexto, no solo en roles:

```java
@DELETE
@Path("/secrets/{id}")
@RolesAllowed("employee")
public Response deleteSecret(@PathParam("id") String id) {
    Secret secret = service.getById(id);
    
    // Verificación adicional: ¿Es el dueño?
    if (!secret.getOwnerId().equals(securityIdentity.getPrincipal().getName())) {
        throw new ForbiddenException("Solo puedes eliminar tus propios secretos");
    }
    
    service.delete(id);
    return Response.ok().build();
}
```

---

## 📊 Resumen Comparativo: Parte 1 vs Parte 2

| Aspecto | Parte 1: Basic Auth | Parte 2: JWT |
|---------|--------------------|--------------| 
| **Método** | Credenciales en cada request | Token una vez |
| **Estado** | Casi stateless | Stateless puro |
| **Formato** | `Authorization: Basic base64(user:pass)` | `Authorization: Bearer <token>` |
| **Expiración** | No (credenciales permanentes) | Sí (claim `exp`) |
| **Escalabilidad** | Media | Alta |
| **Revocación** | N/A | Difícil |
| **Uso común** | APIs internas, admin panels | APIs públicas, microservicios |

---

## 🎯 Resumen Ejecutivo

### Puntos Clave

1. **Autenticación ≠ Autorización**: Son dos procesos distintos
2. **401 vs 403**: No autenticado vs No autorizado
3. **Basic Auth es simple pero limitado**: Bueno para desarrollo, no para producción pública
4. **Roles simplifican permisos**: RBAC es más mantenible que permisos individuales
5. **Stateful vs Stateless**: Trade-off entre control y escalabilidad
6. **Seguridad en capas**: Defense in depth
7. **Validar siempre en servidor**: Nunca confiar en el cliente

---

## 🧠 Preguntas de Autoevaluación

1. ¿Cuál es la diferencia entre autenticación y autorización?
2. ¿Qué significa HTTP 401? ¿Y HTTP 403?
3. ¿Por qué Basic Auth requiere HTTPS en producción?
4. ¿Qué hace la anotación `@RolesAllowed`?
5. ¿Cuál es la diferencia entre `@PermitAll` y `@Authenticated`?
6. ¿Qué es RBAC?
7. ¿Por qué es importante el principio de menor privilegio?
8. ¿Qué es un ataque de brute force y cómo prevenirlo?
9. ¿Qué significa "stateful" en autenticación?
10. ¿Cuándo usarías Basic Auth vs JWT?
