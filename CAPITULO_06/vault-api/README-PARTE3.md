# 🔐 Parte 3: Autenticación con OIDC y Keycloak

## 📖 Descripción

En esta parte integramos **OpenID Connect (OIDC)** con **Keycloak** como proveedor de identidad externo para autenticar **clientes externos**. A diferencia de las partes anteriores donde nuestra aplicación gestionaba la autenticación, aquí delegamos esta responsabilidad a Keycloak.

### 🎯 Objetivos de Aprendizaje

- ✅ Entender qué es OIDC y cómo difiere de JWT propio
- ✅ Configurar Keycloak como Identity Provider
- ✅ Implementar autenticación federada
- ✅ Gestionar usuarios y roles en Keycloak
- ✅ Validar tokens emitidos por Keycloak en Quarkus
- ✅ Implementar autorización basada en roles externos

---

## 🆚 Comparativa: Parte 2 (JWT) vs Parte 3 (OIDC)

| Aspecto | Parte 2: JWT Propio | Parte 3: OIDC + Keycloak |
|---------|---------------------|--------------------------|
| **Quién emite tokens** | Nuestra aplicación | Keycloak |
| **Quién gestiona usuarios** | Nuestra aplicación | Keycloak |
| **Quién gestiona roles** | Nuestra aplicación | Keycloak |
| **Dónde se almacenan usuarios** | application.properties (mock) | Keycloak (BD propia) |
| **Firma del token** | Nuestra clave RSA privada | Clave RSA de Keycloak |
| **Validación del token** | Nuestra clave RSA pública | Clave pública de Keycloak |
| **SSO entre apps** | No | Sí ✅ |
| **Federación con otros IdP** | No | Sí ✅ (Google, GitHub, etc.) |
| **Complejidad inicial** | Baja | Media-Alta |
| **Uso típico** | APIs internas, empleados | Clientes externos, SaaS |

---

## 🏗️ Arquitectura de la Solución

```
┌────────────────────────────────────────────────────────────┐
│                    Cliente Externo                         │
│              (Navegador / Aplicación)                      │
└──────────────────────┬─────────────────────────────────────┘
                       │
                       │ 1. Solicita acceso
                       ▼
           ┌───────────────────────┐
           │   Quarkus App         │
           │   (vault-api)         │
           └───────────┬───────────┘
                       │
                       │ 2. Redirige a Keycloak
                       ▼
           ┌───────────────────────┐
           │      Keycloak         │
           │  (Identity Provider)  │
           │                       │
           │  - Realm: vaultcorp   │
           │  - Usuarios           │
           │  - Roles              │
           └───────────┬───────────┘
                       │
                       │ 3. Usuario se autentica
                       │ 4. Keycloak emite Access Token
                       ▼
           ┌───────────────────────┐
           │   Cliente Externo     │
           │   (guarda token)      │
           └───────────┬───────────┘
                       │
                       │ 5. Request + Bearer Token
                       ▼
           ┌───────────────────────┐
           │   Quarkus App         │
           │   - Valida token      │
           │   - Extrae roles      │
           │   - Autoriza          │
           └───────────────────────┘
```

---

## 📋 Requisitos Previos

### Software Necesario

| Herramienta | Versión | Para qué |
|-------------|---------|----------|
| **Docker** | 20.10+ | Ejecutar Keycloak |
| **Java** | 17+ | Quarkus |
| **Maven** | 3.8+ | Build del proyecto |
| **curl** | Cualquiera | Pruebas |
| **Python 3** | 3.6+ | Formatear JSON (opcional) |

### Verificar Docker

```bash
docker --version
# Docker version 24.0.0 o superior
```

Si no tienes Docker instalado:
- **macOS**: `brew install docker` o descargar Docker Desktop
- **Linux**: Seguir guía oficial de Docker
- **Windows**: Docker Desktop for Windows

---

## 🚀 Paso 1: Levantar Keycloak con Docker

### 1.1 Opción Recomendada: Docker Compose (CON PERSISTENCIA)

El proyecto incluye un archivo `docker-compose.yml` que levanta Keycloak + PostgreSQL con persistencia de datos.

```bash
# Levantar Keycloak + PostgreSQL
docker-compose up -d

# Ver logs
docker-compose logs -f keycloak

# Detener
docker-compose down
```

**Ventajas:**
- ✅ Un solo comando
- ✅ Configuración persiste (no se pierde al reiniciar)
- ✅ PostgreSQL incluido
- ✅ Listo para producción

**Archivo `docker-compose.yml` incluido en el proyecto:**

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: keycloak
      POSTGRES_USER: keycloak
      POSTGRES_PASSWORD: password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  keycloak:
    image: quay.io/keycloak/keycloak:23.0.0
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak
      KC_DB_USERNAME: keycloak
      KC_DB_PASSWORD: password
    ports:
      - "8180:8080"
    depends_on:
      - postgres
    command: start-dev

volumes:
  postgres_data:
```

### 1.2 Opción Alternativa: Docker Run (SIN PERSISTENCIA)

Si no tienes el `docker-compose.yml`, puedes usar este comando (⚠️ la configuración se pierde al detener):

```bash
docker run -p 8180:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  --name keycloak-vaultcorp \
  quay.io/keycloak/keycloak:23.0.0 \
  start-dev
```

**Explicación de los parámetros:**
- `-p 8180:8080`: Mapea puerto 8180 local → 8080 del contenedor
- `-e KEYCLOAK_ADMIN=admin`: Usuario admin de Keycloak
- `-e KEYCLOAK_ADMIN_PASSWORD=admin`: Contraseña del admin
- `--name keycloak-vaultcorp`: Nombre del contenedor
- `start-dev`: Modo desarrollo

### 1.3 Verificar que está Corriendo

Espera 1-2 minutos y verifica:

**Si usaste docker-compose:**
```bash
docker-compose logs keycloak | grep "started in"
```

**Si usaste docker run:**
```bash
docker logs keycloak-vaultcorp | grep "started in"
```

Deberías ver:
```
Keycloak 23.0.0 on JVM started in 7.621s. Listening on: http://0.0.0.0:8080
```

Abre el navegador en: `http://localhost:8180`

Deberías ver la página de bienvenida de Keycloak.

### 1.4 Comandos Útiles

**Con docker-compose:**
```bash
# Ver contenedores corriendo
docker-compose ps

# Ver logs de Keycloak
docker-compose logs -f keycloak

# Detener todo
docker-compose down

# Iniciar nuevamente
docker-compose up -d

# Eliminar TODO incluyendo volúmenes (⚠️ borra la BD)
docker-compose down -v
```

**Con docker run:**
```bash
# Ver contenedores corriendo
docker ps

# Ver logs de Keycloak
docker logs keycloak-vaultcorp

# Detener Keycloak
docker stop keycloak-vaultcorp

# Iniciar Keycloak nuevamente
docker start keycloak-vaultcorp

# Eliminar el contenedor (⚠️ borra toda la configuración)
docker rm keycloak-vaultcorp
```

---

## ⚙️ Paso 2: Configurar Keycloak

### 2.1 Acceder a la Consola de Administración

1. Navega a: `http://localhost:8180`
2. Click en **"Administration Console"**
3. Login:
   - **Usuario**: `admin`
   - **Password**: `admin`

### 2.2 Crear un Realm

Un **Realm** es un espacio aislado para gestionar usuarios, clientes y roles.

1. En la esquina superior izquierda, click en **"master"** (dropdown)
2. Click en **"Create realm"**
3. **Realm name**: `vaultcorp`
4. Click **"Create"**

✅ Ahora estás en el realm `vaultcorp`

### 2.3 Crear un Client (Nuestra App)

Un **Client** representa una aplicación que puede solicitar autenticación.

1. En el menú lateral izquierdo: **Clients**
2. Click **"Create client"**

**Pantalla 1: General Settings**
- **Client type**: `OpenID Connect` ✅
- **Client ID**: `vault-api`
- **Name**: `VaultCorp API`
- **Description**: (opcional) "API de gestión de secretos"
- Click **"Next"**

**Pantalla 2: Capability config**
- **Client authentication**: `On` ✅
- **Authorization**: `Off`
- **Authentication flow**:
  - ✅ Standard flow
  - ✅ Direct access grants
- Click **"Next"**

**Pantalla 3: Login settings**
- **Root URL**: `http://localhost:8080`
- **Home URL**: `http://localhost:8080`
- **Valid redirect URIs**: `http://localhost:8080/*`
- **Valid post logout redirect URIs**: `http://localhost:8080/*`
- **Web origins**: `http://localhost:8080`
- Click **"Save"**

### 2.4 Obtener el Client Secret

1. En la página del client `vault-api`, click en la pestaña **"Credentials"**
2. Copia el valor de **"Client secret"**
3. **⚠️ IMPORTANTE**: Guárdalo en un lugar seguro, lo necesitarás después

Ejemplo: `3dQoUzQ7Y4TQ7eknNNxeDbWiAmMjPpVn`

### 2.5 Crear Roles

Los **Realm Roles** definen permisos a nivel de realm.

1. En el menú lateral: **Realm roles**
2. Click **"Create role"**

**Rol 1: customer**
- **Role name**: `customer`
- **Description**: "Cliente básico"
- Click **"Save"**

**Rol 2: premium-customer**
- Click **"Create role"** nuevamente
- **Role name**: `premium-customer`
- **Description**: "Cliente premium"
- Click **"Save"**

### 2.6 Crear Usuarios

1. En el menú lateral: **Users**
2. Click **"Create new user"**

**Usuario 1: Cliente Básico**
- **Username**: `client001`
- **Email**: `cliente1@empresa.com`
- **First name**: `Carlos`
- **Last name**: `Gómez`
- **Email verified**: `On` ✅
- **Required user actions**: (dejar vacío)
- Click **"Create"**

Ahora asignar contraseña:
- Click en pestaña **"Credentials"**
- Click **"Set password"**
- **Password**: `pass001`
- **Password confirmation**: `pass001`
- **Temporary**: `Off` ✅
- Click **"Save"** y confirmar

Ahora asignar rol:
- Click en pestaña **"Role mapping"**
- Click **"Assign role"**
- Seleccionar **`customer`**
- Click **"Assign"**

**Usuario 2: Cliente Premium**
- Repetir los pasos anteriores con:
  - **Username**: `client002`
  - **Email**: `cliente2@empresa.com`
  - **First name**: `María`
  - **Last name**: `López`
  - **Password**: `pass002`
  - **Rol**: `premium-customer`

✅ **Resumen de Usuarios Creados:**

| Username | Password | Email | Rol |
|----------|----------|-------|-----|
| `client001` | `pass001` | cliente1@empresa.com | `customer` |
| `client002` | `pass002` | cliente2@empresa.com | `premium-customer` |

---

## 🔧 Paso 3: Configurar Quarkus

### 3.1 Crear Archivo de Configuración Exclusivo para Parte 3

Para evitar conflictos entre las diferentes partes del ejercicio, crearemos un archivo de configuración separado.

Crea el archivo `src/main/resources/application-parte3.properties` con el siguiente contenido:

```properties
# ═══════════════════════════════════════════════════════════════════════════
# PARTE 3: AUTENTICACIÓN CON OIDC (OPENID CONNECT + KEYCLOAK)
# ═══════════════════════════════════════════════════════════════════════════
# Esta configuración habilita OpenID Connect usando Keycloak como proveedor
# de identidad externo. Los tokens son emitidos y firmados por Keycloak.
#
# Para ejecutar esta parte:
#   ./mvnw quarkus:dev -Dquarkus.profile=parte3
#
# O en Windows (Git Bash):
#   ./mvnw quarkus:dev -Dquarkus.profile=parte3
#
# Prerequisitos:
#   - Keycloak corriendo en http://localhost:8180
#   - Realm "vaultcorp" configurado
#   - Client "vault-api" creado con client secret
#   - Usuarios con roles: customer y premium-customer
# ═══════════════════════════════════════════════════════════════════════════

# ───────────────────────────────────────────────────────────────────────────
# CONFIGURACIÓN OIDC (OpenID Connect)
# ───────────────────────────────────────────────────────────────────────────

# Habilitar OIDC
quarkus.oidc.enabled=true

# URL del servidor de autorización (Keycloak realm)
quarkus.oidc.auth-server-url=http://localhost:8180/realms/vaultcorp

# Client ID registrado en Keycloak
quarkus.oidc.client-id=vault-api

# Client Secret (debe coincidir con el configurado en Keycloak)
# ⚠️ IMPORTANTE: Reemplazar con tu client secret real de Keycloak
quarkus.oidc.credentials.secret=TU-CLIENT-SECRET-AQUI

# Tipo de aplicación: service (backend API)
quarkus.oidc.application-type=service

# ⚠️ CRÍTICO: Los roles vienen del token de acceso (no del ID token)
quarkus.oidc.roles.source=accesstoken

# ⚠️ CRÍTICO: Path del claim de roles en el token (estructura Keycloak por defecto)
# Keycloak estructura los roles en: token.realm_access.roles
quarkus.oidc.roles.role-claim-path=realm_access/roles

# Tolerancia para diferencias de timestamp (60 segundos)
quarkus.oidc.token.age=60

# Verificar el issuer del token (debe coincidir exactamente)
quarkus.oidc.token.issuer=http://localhost:8180/realms/vaultcorp

# ⚠️ SOLO DESARROLLO: Deshabilitar verificación TLS
# En producción DEBE ser: quarkus.oidc.tls.verification=required
quarkus.oidc.tls.verification=none

# ───────────────────────────────────────────────────────────────────────────
# DESHABILITAR OTRAS FORMAS DE AUTENTICACIÓN
# ───────────────────────────────────────────────────────────────────────────

# Deshabilitar Basic Auth
quarkus.http.auth.basic=false

# Deshabilitar usuarios embebidos
quarkus.security.users.embedded.enabled=false

# ───────────────────────────────────────────────────────────────────────────
# CONFIGURACIÓN ADICIONAL
# ───────────────────────────────────────────────────────────────────────────

# Logging para debugging (opcional - puedes comentar en producción)
quarkus.log.category."io.quarkus.oidc".level=DEBUG
quarkus.log.category."com.vaultcorp".level=INFO
```

**⚠️ MUY IMPORTANTE**: No olvides reemplazar `TU-CLIENT-SECRET-AQUI` con el client secret que copiaste de Keycloak en el Paso 2.4.

### 3.2 Ejecutar con el Perfil Correcto

Para usar esta configuración, siempre debes ejecutar Quarkus con el perfil `parte3`:

```bash
./mvnw quarkus:dev -Dquarkus.profile=parte3
```

**¿Por qué usar perfiles?**
- ✅ Evita conflictos entre Basic Auth, JWT y OIDC
- ✅ Cada parte funciona de forma aislada
- ✅ Configuración más limpia y mantenible
- ✅ Más fácil de enseñar y probar

### 3.3 Verificar Configuración

Tu estructura de archivos debería verse así:

```
src/main/resources/
├── application.properties              # Configuración común
├── application-parte1.properties       # Solo Basic Auth
├── application-parte2.properties       # Solo JWT
└── application-parte3.properties       # Solo OIDC (el que acabas de crear)
```

**Para verificar que la configuración está correcta:**

```bash
# 1. Levantar Quarkus con el perfil parte3
./mvnw quarkus:dev -Dquarkus.profile=parte3

# 2. En los logs, deberías ver:
# - "OIDC enabled: true"
# - "OIDC server: http://localhost:8180/realms/vaultcorp"

# 3. Verificar que Keycloak está accesible
curl http://localhost:8180/realms/vaultcorp/.well-known/openid-configuration

# Si ves un JSON con "issuer", "authorization_endpoint", etc. → ¡Todo bien! ✅
```

---

## 💻 Paso 4: Crear Endpoints Externos

### 4.1 Actualizar Roles.java

Agrega los nuevos roles de clientes:

```java
package com.vaultcorp.security;

public class Roles {
    // Parte 1: Roles administrativos
    public static final String VAULT_ADMIN = "vault-admin";
    public static final String VAULT_AUDITOR = "vault-auditor";
    
    // Parte 2: Roles de empleados
    public static final String EMPLOYEE = "employee";
    
    // Parte 3: Roles de clientes (OIDC)
    public static final String CUSTOMER = "customer";
    public static final String PREMIUM_CUSTOMER = "premium-customer";

    private Roles() {}
}
```

### 4.2 Crear ExternalSecretResource.java

Crea `src/main/java/com/vaultcorp/resource/ExternalSecretResource.java`:

```java
package com.vaultcorp.resource;

import com.vaultcorp.model.Secret;
import com.vaultcorp.model.SecretLevel;
import com.vaultcorp.security.Roles;
import com.vaultcorp.service.SecretService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

@Path("/api/external/secrets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExternalSecretResource {

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    SecretService secretService;

    // Endpoint para clientes básicos y premium: secretos públicos
    @GET
    @Path("/public")
    @RolesAllowed({Roles.CUSTOMER, Roles.PREMIUM_CUSTOMER})
    public Response getPublicSecrets() {
        List<Secret> publicSecrets = secretService.getSecretsByLevel(SecretLevel.PUBLIC);
        
        return Response.ok(Map.of(
            "user", securityIdentity.getPrincipal().getName(),
            "roles", securityIdentity.getRoles(),
            "totalSecrets", publicSecrets.size(),
            "secrets", publicSecrets
        )).build();
    }

    // Endpoint SOLO para clientes premium: secretos confidenciales
    @GET
    @Path("/confidential")
    @RolesAllowed(Roles.PREMIUM_CUSTOMER)
    public Response getConfidentialSecrets() {
        List<Secret> confidentialSecrets = secretService.getSecretsByLevel(SecretLevel.CONFIDENTIAL);
        
        return Response.ok(Map.of(
            "user", securityIdentity.getPrincipal().getName(),
            "roles", securityIdentity.getRoles(),
            "level", "CONFIDENTIAL",
            "totalSecrets", confidentialSecrets.size(),
            "secrets", confidentialSecrets
        )).build();
    }

    // Endpoint para ver perfil del cliente autenticado
    @GET
    @Path("/profile")
    @RolesAllowed({Roles.CUSTOMER, Roles.PREMIUM_CUSTOMER})
    public Response getProfile() {
        return Response.ok(Map.of(
            "username", securityIdentity.getPrincipal().getName(),
            "roles", securityIdentity.getRoles(),
            "authMethod", "OIDC (Keycloak)"
        )).build();
    }
}
```

---

## 🧪 Paso 5: Probar la Implementación

### 5.1 Pruebas Manuales

#### Paso 1: Obtener Token desde Keycloak

```bash
# Obtener token para cliente básico
curl -X POST http://localhost:8180/realms/vaultcorp/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=vault-api" \
  -d "client_secret=TU-CLIENT-SECRET" \
  -d "username=client001" \
  -d "password=pass001"
```

**Respuesta esperada:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "refresh_token": "eyJhbGci...",
  "token_type": "Bearer"
}
```

#### Paso 2: Guardar el Token

```bash
# Guarda el access_token en una variable
TOKEN="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
```

#### Paso 3: Ver Perfil del Usuario

```bash
curl http://localhost:8080/api/external/secrets/profile \
  -H "Authorization: Bearer $TOKEN"
```

**Respuesta esperada:**
```json
{
  "username": "client001",
  "roles": ["customer", "default-roles-vaultcorp", "offline_access", "uma_authorization"],
  "authMethod": "OIDC (Keycloak)"
}
```

#### Paso 4: Listar Secretos Públicos

```bash
curl http://localhost:8080/api/external/secrets/public \
  -H "Authorization: Bearer $TOKEN"
```

**Respuesta esperada:**
```json
{
  "user": "client001",
  "roles": ["customer", ...],
  "totalSecrets": 2,
  "secrets": [
    {
      "id": "...",
      "name": "Manual de Usuario",
      "content": "https://docs.vaultcorp.com/manual",
      "level": "PUBLIC"
    }
  ]
}
```

#### Paso 5: Intentar Acceder a Secretos Confidenciales (Debe Fallar)

```bash
curl -i http://localhost:8080/api/external/secrets/confidential \
  -H "Authorization: Bearer $TOKEN"
```

**Respuesta esperada:**
```
HTTP/1.1 403 Forbidden
```

✅ ¡Perfecto! El cliente básico NO puede acceder a secretos CONFIDENTIAL.

#### Paso 6: Obtener Token Premium

```bash
# Obtener token para cliente premium
curl -X POST http://localhost:8180/realms/vaultcorp/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=vault-api" \
  -d "client_secret=TU-CLIENT-SECRET" \
  -d "username=client002" \
  -d "password=pass002"

# Guardar el nuevo token
TOKEN_PREMIUM="eyJhbGci..."
```

#### Paso 7: Acceder a Secretos Confidenciales con Cliente Premium

```bash
curl http://localhost:8080/api/external/secrets/confidential \
  -H "Authorization: Bearer $TOKEN_PREMIUM"
```

**Respuesta esperada:**
```json
{
  "user": "client002",
  "roles": ["premium-customer", ...],
  "level": "CONFIDENTIAL",
  "totalSecrets": 2,
  "secrets": [
    {
      "id": "...",
      "name": "Credenciales AWS S3",
      "content": "AKIAIOSFODNN7EXAMPLE",
      "level": "CONFIDENTIAL"
    }
  ]
}
```

✅ ¡Perfecto! El cliente premium SÍ puede acceder a secretos CONFIDENTIAL.

### 5.2 Script Automatizado

```bash
# 1. Asegúrate de que Keycloak esté corriendo
docker-compose ps      # O: docker ps | grep keycloak

# 2. Asegúrate de que Quarkus esté corriendo con perfil parte3
./mvnw quarkus:dev -Dquarkus.profile=parte3

# 3. En otra terminal, ejecuta el script de pruebas
./test-part3-oidc.sh
```

El script te guiará paso a paso por todas las pruebas y generará un archivo de log.

---

## 📊 Endpoints Disponibles

| Método | Ruta | Roles Permitidos | Descripción |
|--------|------|------------------|-------------|
| `GET` | `/api/external/secrets/profile` | `customer`, `premium-customer` | Ver perfil del usuario |
| `GET` | `/api/external/secrets/public` | `customer`, `premium-customer` | Listar secretos PUBLIC |
| `GET` | `/api/external/secrets/confidential` | `premium-customer` | Listar secretos CONFIDENTIAL |

**Headers necesarios:**
```
Authorization: Bearer <access-token-de-keycloak>
```

---

## 💾 Persistir Configuración de Keycloak

**Si usaste docker-compose:** ✅ Tu configuración YA persiste automáticamente en PostgreSQL. No necesitas hacer nada más.

**Si usaste docker run:** La configuración se pierde al detener el contenedor. Opciones:

### Opción 1: Cambiar a docker-compose (Recomendado)

Usa el `docker-compose.yml` del proyecto y reinicia:
```bash
docker stop keycloak-vaultcorp
docker rm keycloak-vaultcorp
docker-compose up -d
```

Vuelve a configurar Keycloak (Realm, Client, Usuarios). Esta vez sí persistirá.

### Opción 2: Exportar/Importar Configuración

**Exportar:**

```bash
docker exec -it keycloak-vaultcorp /opt/keycloak/bin/kc.sh export \
  --dir /tmp/keycloak-export \
  --realm vaultcorp

docker cp keycloak-vaultcorp:/tmp/keycloak-export ./keycloak-backup
```

**Importar:**

```bash
docker run -p 8180:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  -v $(pwd)/keycloak-backup:/opt/keycloak/data/import \
  --name keycloak-vaultcorp \
  quay.io/keycloak/keycloak:23.0.0 \
  start-dev --import-realm
```

---

## 🛠️ Troubleshooting

### Problema: "Connection refused" al conectar con Keycloak

**Causa:** Keycloak no está corriendo.

**Solución con docker-compose:**
```bash
docker-compose ps
# Si no aparece "Up":
docker-compose up -d
```

**Solución con docker run:**
```bash
docker ps
# Si no aparece keycloak-vaultcorp:
docker start keycloak-vaultcorp
```

### Problema: "401 Unauthorized" al usar token de Keycloak

**Causas comunes:**

1. **Token expirado** - Los tokens OIDC expiran en 5 minutos (300s)
2. **Client secret incorrecto** - Verifica en `application-parte3.properties`
3. **Falta configuración de roles** - ⚠️ CRÍTICO

**Verificar configuración de roles:**

Abre `application-parte3.properties` y asegúrate de tener:

```properties
quarkus.oidc.roles.source=accesstoken
quarkus.oidc.roles.role-claim-path=realm_access/roles
```

**Sin estas líneas, Quarkus NO puede extraer los roles del token y todos los endpoints retornarán 401.**

**Solución:**
1. Agrega las líneas faltantes
2. Reinicia Quarkus: `./mvnw quarkus:dev -Dquarkus.profile=parte3`
3. Obtén un nuevo token de Keycloak
4. Prueba de nuevo

### Problema: "403 Forbidden" con cliente básico

**¡Eso es correcto!** El cliente básico (`customer`) NO puede acceder a secretos CONFIDENTIAL. Solo el cliente `premium-customer` tiene acceso.

### Problema: Keycloak reinicia y pierdo la configuración

**Solución:** Usa Docker Compose con PostgreSQL (ver sección "Persistir Configuración").

### Problema: Puerto 8180 ya en uso

**Solución:**
```bash
# Cambiar el puerto al levantar Keycloak
docker run -p 8280:8080 ...
```

Y actualizar en `application-parte3.properties`:
```properties
quarkus.oidc.auth-server-url=http://localhost:8280/realms/vaultcorp
quarkus.oidc.token.issuer=http://localhost:8280/realms/vaultcorp
```

---

## 🎓 Conceptos Clave

### ¿Qué es OIDC?

**OpenID Connect (OIDC)** es un protocolo de autenticación construido sobre OAuth 2.0. Permite que las aplicaciones deleguen la autenticación a un **Identity Provider** (IdP) como Keycloak.

### Flujo OIDC Simplificado

```
1. Usuario → App: "Quiero acceder"
2. App → Keycloak: "Autentica a este usuario"
3. Usuario → Keycloak: Ingresa credenciales
4. Keycloak → App: "Aquí está el token, está autenticado"
5. App: Valida token y autoriza acceso
```

### Componentes OIDC

| Componente | Descripción | En este ejercicio |
|------------|-------------|-------------------|
| **Identity Provider (IdP)** | Servicio que autentica usuarios | Keycloak |
| **Relying Party (RP)** | Aplicación que confía en el IdP | Nuestra app Quarkus |
| **Access Token** | Token JWT para acceder a recursos | Emitido por Keycloak |
| **ID Token** | Token con info del usuario | Emitido por Keycloak |
| **Realm** | Espacio aislado de usuarios/roles | `vaultcorp` |
| **Client** | Aplicación registrada en el IdP | `vault-api` |

### Ventajas de OIDC

- ✅ **Single Sign-On (SSO)**: Un login para múltiples apps
- ✅ **Federación**: Integración con Google, GitHub, etc.
- ✅ **Centralización**: Gestión de usuarios en un solo lugar
- ✅ **Seguridad**: Identity Provider especializado
- ✅ **Escalabilidad**: Separación de responsabilidades

### Desventajas de OIDC

- ❌ **Dependencia externa**: Requiere IdP corriendo
- ❌ **Complejidad**: Más componentes que gestionar
- ❌ **Latencia**: Validación de tokens contra Keycloak
- ❌ **Setup inicial**: Configuración más compleja

---

## 📚 Comparativa Final: Parte 1 vs 2 vs 3

| Aspecto | Parte 1 (Basic Auth) | Parte 2 (JWT) | Parte 3 (OIDC) |
|---------|----------------------|---------------|----------------|
| **Usuarios** | Admins/Auditores | Empleados | Clientes externos |
| **Endpoints** | `/api/admin/*` | `/api/internal/*` | `/api/external/*` |
| **Método** | Credenciales en cada request | Token JWT propio | Token OIDC de Keycloak |
| **Gestión usuarios** | application.properties | Endpoint /login | Keycloak |
| **Gestión roles** | application.properties | Código Java | Keycloak |
| **Expiración** | No | Sí (1 hora) | Sí (5 minutos) |
| **SSO** | No | No | Sí ✅ |
| **Complejidad** | Baja | Media | Alta |
| **Uso típico** | APIs internas | Apps móviles/SPAs | SaaS, multi-tenant |

---

## ✅ Checklist de Verificación

Antes de dar por completada la Parte 3:

- [ ] Keycloak está corriendo en Docker
- [ ] Realm `vaultcorp` creado
- [ ] Client `vault-api` configurado con client secret
- [ ] Roles `customer` y `premium-customer` creados
- [ ] Usuarios `client001` y `client002` creados con contraseñas
- [ ] Roles asignados a usuarios correctamente
- [ ] `application-parte3.properties` existe con configuración completa
- [ ] Configuración incluye `quarkus.oidc.roles.source=accesstoken`
- [ ] Configuración incluye `quarkus.oidc.roles.role-claim-path=realm_access/roles`
- [ ] `ExternalSecretResource.java` creado
- [ ] Cliente básico puede ver secretos PUBLIC (200)
- [ ] Cliente básico NO puede ver secretos CONFIDENTIAL (403)
- [ ] Cliente premium SÍ puede ver secretos CONFIDENTIAL (200)
- [ ] Script `test-part3-oidc.sh` ejecuta sin errores
- [ ] Archivo de log generado con todos los tests PASS ✅

---

## 🚀 Próximos Pasos (Opcional)

### Mejoras Avanzadas

1. **Integración con Google/GitHub**
   - Configurar Identity Providers en Keycloak
   - Permitir login social

2. **Multi-tenancy con Organizations**
   - Usar claim `organization` en tokens
   - Filtrar secretos por organización

3. **Refresh Tokens**
   - Implementar renovación automática de tokens
   - Mejor experiencia de usuario

4. **Logout funcional**
   - Endpoint de logout que invalida tokens en Keycloak
   - Blacklist de tokens revocados

5. **Admin UI con React**
   - Interfaz gráfica para gestionar secretos
   - Login con Keycloak desde navegador

---

## 📖 Recursos Adicionales

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Quarkus OIDC Guide](https://quarkus.io/guides/security-oidc-bearer-token-authentication)
- [OpenID Connect Spec](https://openid.net/connect/)
- [OAuth 2.0 RFC 6749](https://tools.ietf.org/html/rfc6749)

---

## 📝 Notas para el Instructor

### Preparación de la Clase

**Antes de la clase:**
1. Levantar Keycloak en tu máquina
2. Configurar ngrok: `ngrok http 8180`
3. Compartir URL de ngrok con alumnos
4. Tener configuración de Keycloak exportada como backup

**Durante la clase:**
- Mostrar demo en vivo del flujo OIDC
- Explicar diferencias con JWT propio
- Enfatizar cuándo usar cada método
- Resolver dudas sobre Keycloak

**Después de la clase:**
- Compartir export de configuración de Keycloak
- Enviar guía de instalación de Docker
- Dar tiempo para que repliquen en casa

### Tiempos Estimados

- Setup Keycloak: 10 min
- Configurar Realm/Client/Roles/Usuarios: 15 min
- Configurar Quarkus: 5 min
- Crear endpoints: 15 min
- Pruebas: 10 min
- Discusión: 10 min

**Total:** ~65 minutos

---