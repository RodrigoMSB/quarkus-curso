# 🔐 VaultCorp API - Sistema de Gestión de Secretos con Quarkus

## 📖 Descripción del Proyecto

**VaultCorp API** es un microservicio educativo desarrollado con **Quarkus** que implementa un sistema completo de gestión de secretos corporativos con **tres niveles de seguridad**:

1. **Autenticación Básica** (Basic Auth) para administradores
2. **JWT (JSON Web Token)** para empleados internos
3. **OIDC (OpenID Connect)** para clientes externos (Parte 3 - próximamente)

### 🎯 Objetivos Pedagógicos

Este proyecto está diseñado para enseñar de forma práctica:

- ✅ Diferentes métodos de autenticación en microservicios
- ✅ Autorización basada en roles (`@RolesAllowed`)
- ✅ Generación y validación de tokens JWT con RSA
- ✅ Aislamiento de datos por usuario (multi-tenancy)
- ✅ Buenas prácticas de seguridad en APIs REST
- ✅ Testing automatizado de seguridad

---

## 🏗️ Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────────┐
│                  VaultCorp API (Quarkus)                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   Admin      │  │   Internal   │  │   External   │     │
│  │   Endpoints  │  │   Endpoints  │  │   Endpoints  │     │
│  │              │  │              │  │              │     │
│  │ Basic Auth   │  │     JWT      │  │     OIDC     │     │
│  │ @RolesAllowed│  │   Bearer     │  │  (Keycloak)  │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              SecretService (In-Memory)               │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
         │                  │                  │
         ▼                  ▼                  ▼
    [Admin Users]      [Employees]       [Customers]
    (curl/Postman)     (JWT tokens)      (Keycloak)
```

---

## 📋 Requisitos Previos

### Software Necesario

| Herramienta | Versión Mínima | Verificación |
|-------------|----------------|--------------|
| **Java JDK** | 17+ | `java -version` |
| **Maven** | 3.8+ | `mvn -version` |
| **curl** | Cualquiera | `curl --version` |
| **Python 3** | 3.6+ (opcional, para formatear JSON) | `python3 --version` |
| **OpenSSL** | 1.1+ (para generar llaves RSA) | `openssl version` |

### Instalación de Java y Maven (si no los tienes)

**macOS (con Homebrew):**
```bash
brew install openjdk@17
brew install maven
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install openjdk-17-jdk maven
```

**Windows:**
- Descargar Java: https://adoptium.net/
- Descargar Maven: https://maven.apache.org/download.cgi

---

## 🚀 Creación del Proyecto desde Cero

### Paso 1: Crear el Proyecto Quarkus

```bash
mvn io.quarkus:quarkus-maven-plugin:3.15.1:create \
    -DprojectGroupId=com.vaultcorp \
    -DprojectArtifactId=vault-api \
    -DprojectVersion=1.0.0-SNAPSHOT \
    -DclassName="com.vaultcorp.resource.HealthResource" \
    -Dpath="/health" \
    -Dextensions="resteasy-reactive-jackson,hibernate-validator,smallrye-jwt,oidc,security"
```

**¿Qué hace este comando?**
- Crea un proyecto Maven con groupId `com.vaultcorp`
- Añade las extensiones necesarias:
  - `resteasy-reactive-jackson` - REST APIs con JSON
  - `hibernate-validator` - Validación de datos
  - `smallrye-jwt` - Soporte para JWT
  - `oidc` - OpenID Connect (Parte 3)
  - `security` - Framework de seguridad base

### Paso 2: Navegar al Proyecto

```bash
cd vault-api
```

### Paso 3: Agregar Extensión Adicional

```bash
./mvnw quarkus:add-extension -Dextensions="elytron-security-properties-file"
```

Esta extensión permite configurar usuarios embebidos para Basic Auth.

---

## 📁 Estructura del Proyecto

```
vault-api/
├── src/
│   ├── main/
│   │   ├── java/com/vaultcorp/
│   │   │   ├── dto/                    # Data Transfer Objects
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── SecretRequest.java
│   │   │   │   └── TokenResponse.java
│   │   │   ├── model/                  # Entidades del dominio
│   │   │   │   ├── Secret.java
│   │   │   │   └── SecretLevel.java
│   │   │   ├── resource/               # REST Controllers
│   │   │   │   ├── AdminSecretResource.java
│   │   │   │   ├── AuthResource.java
│   │   │   │   └── InternalSecretResource.java
│   │   │   ├── security/               # Configuración de seguridad
│   │   │   │   └── Roles.java
│   │   │   └── service/                # Lógica de negocio
│   │   │       ├── JwtService.java
│   │   │       └── SecretService.java
│   │   └── resources/
│   │       ├── application.properties  # Configuración
│   │       ├── privateKey.pem          # Llave privada RSA (JWT)
│   │       └── publicKey.pem           # Llave pública RSA (JWT)
│   └── test/                           # Tests
├── pom.xml                             # Dependencias Maven
├── test-part1-security.sh              # Script de pruebas Parte 1
├── test-part2-jwt.sh                   # Script de pruebas Parte 2
├── README.md                           # Este archivo
├── README-PARTE2.md                    # Guía detallada Parte 2
└── TEORIA-PARTE2.md                    # Teoría JWT
```

---

## ⚙️ Configuración Inicial

### 1. Configurar `application.properties`

Edita `src/main/resources/application.properties`:

```properties
# Configuración básica
quarkus.http.port=8080

# Habilitar seguridad en modo desarrollo
quarkus.security.auth.enabled-in-dev-mode=true

# Usuarios embebidos para autenticación básica (Parte 1)
quarkus.security.users.embedded.enabled=true
quarkus.security.users.embedded.plain-text=true
quarkus.security.users.embedded.users.admin=admin123
quarkus.security.users.embedded.roles.admin=vault-admin
quarkus.security.users.embedded.users.auditor=auditor123
quarkus.security.users.embedded.roles.auditor=vault-auditor
quarkus.security.users.embedded.users.employee=employee123
quarkus.security.users.embedded.roles.employee=employee

# Habilitar autenticación básica
quarkus.http.auth.basic=true

# Configuración JWT (Parte 2)
mp.jwt.verify.publickey.location=publicKey.pem
mp.jwt.verify.issuer=https://vaultcorp.com
smallrye.jwt.sign.key.location=privateKey.pem
mp.jwt.verify.clock.skew=60

# Deshabilitar OIDC temporalmente (hasta Parte 3)
quarkus.oidc.enabled=false
```

### 2. Generar Llaves RSA para JWT

```bash
# Generar llave privada (2048 bits)
openssl genrsa -out src/main/resources/privateKey.pem 2048

# Generar llave pública desde la privada
openssl rsa -pubout -in src/main/resources/privateKey.pem -out src/main/resources/publicKey.pem
```

**¿Por qué RSA?** Permite que diferentes microservicios validen tokens sin compartir la clave privada.

---

## 🎓 Parte 1: Autenticación y Autorización Básica

### Conceptos Clave

- **Basic Auth**: Usuario y contraseña en cada request
- **@PermitAll**: Endpoint público sin autenticación
- **@RolesAllowed**: Endpoint protegido por roles
- **HTTP 401**: No autenticado
- **HTTP 403**: Autenticado pero sin permiso

### Usuarios y Roles

| Username | Password | Rol | Permisos |
|----------|----------|-----|----------|
| `admin` | `admin123` | `vault-admin` | ✅ Leer, crear, eliminar secretos |
| `auditor` | `auditor123` | `vault-auditor` | ✅ Solo lectura (stats) |
| `employee` | `employee123` | `employee` | ✅ Login JWT (Parte 2) |

### Endpoints Administrativos

```bash
# ✅ Público - Health Check
curl http://localhost:8080/api/admin/secrets/health

# 🔒 Admin - Listar todos los secretos
curl -u admin:admin123 http://localhost:8080/api/admin/secrets/all

# 🔒 Admin - Eliminar un secreto
curl -X DELETE -u admin:admin123 http://localhost:8080/api/admin/secrets/{id}

# 🔒 Admin/Auditor - Ver estadísticas
curl -u auditor:auditor123 http://localhost:8080/api/admin/secrets/stats
```

### Ejecutar Pruebas Automatizadas

```bash
# Asegúrate de que Quarkus esté corriendo
./mvnw quarkus:dev

# En otra terminal
./test-part1-security.sh
```

**📖 Documentación completa:** Revisar código fuente de `AdminSecretResource.java`

---

## 🔐 Parte 2: Autenticación con JWT

### Conceptos Clave

- **JWT**: Token autocontenido con información del usuario
- **Stateless**: El servidor no guarda sesiones
- **Bearer Token**: Se envía en header `Authorization: Bearer <token>`
- **Claims**: Información dentro del JWT (sub, email, groups, exp)
- **RSA Signature**: Firma criptográfica que garantiza integridad

### Flujo de Autenticación

```
1. Login           2. Token          3. Request         4. Validación
┌────────┐        ┌────────┐        ┌────────┐        ┌────────┐
│ POST   │───────>│ Server │───────>│ GET    │───────>│ Server │
│ /login │ user/  │ genera │  JWT   │ /api   │ Bearer │ valida │
│        │ pass   │  JWT   │ token  │        │ token  │  firma │
└────────┘        └────────┘        └────────┘        └────────┘
```

### Usuarios para JWT

| Username | Password | Email | Rol |
|----------|----------|-------|-----|
| `emp001` | `pass001` | juan.perez@vaultcorp.com | `employee` |
| `emp002` | `pass002` | maria.gonzalez@vaultcorp.com | `employee` |
| `emp003` | `pass003` | carlos.rodriguez@vaultcorp.com | `employee` |

### Flujo Completo Paso a Paso

#### 1. Hacer Login y Obtener Token

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

#### 2. Guardar el Token

```bash
# Método 1: Manual
TOKEN="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."

# Método 2: Automático
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"emp001","password":"pass001"}' \
  | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
```

#### 3. Ver Perfil

```bash
curl http://localhost:8080/api/internal/secrets/profile \
  -H "Authorization: Bearer $TOKEN"
```

#### 4. Crear un Secreto

```bash
curl -X POST http://localhost:8080/api/internal/secrets \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Mi Secreto",
    "content": "información confidencial",
    "level": "INTERNAL"
  }'
```

#### 5. Listar Mis Secretos

```bash
curl http://localhost:8080/api/internal/secrets/my-secrets \
  -H "Authorization: Bearer $TOKEN"
```

### Ejecutar Pruebas Automatizadas

```bash
# Asegúrate de que Quarkus esté corriendo
./mvnw quarkus:dev

# En otra terminal
./test-part2-jwt.sh
```

**📖 Documentación detallada:** Ver `README-PARTE2.md` y `TEORIA-PARTE2.md`

---

## 🚀 Cómo Ejecutar el Proyecto

### Opción 1: Modo Desarrollo (Recomendado)

```bash
# Iniciar en modo dev con hot reload
./mvnw quarkus:dev
```

**Ventajas:**
- ✅ Hot reload automático al cambiar código
- ✅ Dev UI disponible en http://localhost:8080/q/dev
- ✅ Logs en tiempo real

### Opción 2: Compilar y Ejecutar JAR

```bash
# Compilar
./mvnw clean package

# Ejecutar
java -jar target/quarkus-app/quarkus-run.jar
```

### Opción 3: Compilación Nativa con GraalVM (Avanzado)

```bash
# Compilar binario nativo
./mvnw package -Dnative

# Ejecutar (arranque < 0.05s)
./target/vault-api-1.0.0-SNAPSHOT-runner
```

---

## 🧪 Testing Completo

### Pruebas Automatizadas

```bash
# Parte 1: Autenticación Básica
./test-part1-security.sh

# Parte 2: JWT
./test-part2-jwt.sh
```

### Pruebas Manuales - Checklist

#### ✅ Parte 1: Basic Auth

- [ ] Health check funciona sin autenticación
- [ ] Admin puede listar todos los secretos
- [ ] Admin puede eliminar secretos
- [ ] Auditor puede ver estadísticas
- [ ] Auditor NO puede eliminar secretos
- [ ] Requests sin credenciales son rechazadas (401)

#### ✅ Parte 2: JWT

- [ ] Login genera token válido
- [ ] Token contiene claims correctos (sub, email, groups)
- [ ] Perfil muestra información del usuario
- [ ] Empleado puede crear secretos propios
- [ ] Empleado solo ve sus propios secretos
- [ ] Segundo empleado no ve secretos del primero
- [ ] Token expirado es rechazado (401)

---

## 📊 API Reference - Resumen de Endpoints

### 🔓 Endpoints Públicos

| Método | Ruta | Descripción |
|--------|------|-------------|
| `GET` | `/api/admin/secrets/health` | Health check |
| `POST` | `/api/auth/login` | Generar JWT |

### 🔒 Endpoints con Basic Auth

| Método | Ruta | Rol Requerido | Descripción |
|--------|------|---------------|-------------|
| `GET` | `/api/admin/secrets/all` | `vault-admin` | Listar todos los secretos |
| `DELETE` | `/api/admin/secrets/{id}` | `vault-admin` | Eliminar secreto |
| `GET` | `/api/admin/secrets/stats` | `vault-admin`, `vault-auditor` | Ver estadísticas |

### 🔐 Endpoints con JWT

| Método | Ruta | Rol Requerido | Descripción |
|--------|------|---------------|-------------|
| `GET` | `/api/internal/secrets/profile` | `employee` | Ver perfil |
| `GET` | `/api/internal/secrets/my-secrets` | `employee` | Listar secretos propios |
| `POST` | `/api/internal/secrets` | `employee` | Crear secreto |

---

## 🛠️ Troubleshooting

### Problema: "Port 8080 already in use"

**Solución:**
```bash
# Encontrar proceso usando el puerto
lsof -i :8080

# Matar el proceso
kill -9 <PID>

# O cambiar el puerto
./mvnw quarkus:dev -Dquarkus.http.port=8081
```

### Problema: "Token issued to client quarkus-app is not active"

**Causa:** Conflicto entre extensiones OIDC y JWT

**Solución:** Verificar que `application.properties` contenga:
```properties
quarkus.oidc.enabled=false
```

### Problema: "401 Unauthorized" con Basic Auth

**Verificar:**
1. ¿Estás usando el formato correcto? `curl -u username:password`
2. ¿El usuario existe en `application.properties`?
3. ¿La contraseña es correcta?

### Problema: "401 Unauthorized" con JWT

**Verificar:**
1. ¿El header es correcto? `Authorization: Bearer <token>`
2. ¿El token expiró? Los tokens duran 1 hora
3. ¿Las llaves RSA existen en `src/main/resources/`?

### Problema: No puedo ver secretos de otro usuario

**¡Eso es correcto!** Es una característica de seguridad (aislamiento multi-tenancy).

---

## 📚 Recursos de Aprendizaje

### Documentación Oficial

- [Quarkus Security](https://quarkus.io/guides/security)
- [Quarkus JWT](https://quarkus.io/guides/security-jwt)
- [RFC 7519 - JWT Standard](https://tools.ietf.org/html/rfc7519)

### Herramientas Útiles

- [jwt.io](https://jwt.io) - Debugger de JWT
- [Quarkus Dev UI](http://localhost:8080/q/dev) - Interfaz de desarrollo
- [Postman](https://www.postman.com/) - Testing de APIs

### Archivos del Proyecto

- `README-PARTE2.md` - Guía detallada de JWT
- `TEORIA-PARTE2.md` - Teoría completa de JWT
- `test-part1-security.sh` - Script de pruebas Parte 1
- `test-part2-jwt.sh` - Script de pruebas Parte 2

---

## 🎯 Próximos Pasos

### Parte 3: OIDC con Keycloak (Próximamente)

- Integración con proveedores de identidad externos
- OpenID Connect (OIDC)
- Single Sign-On (SSO)
- Keycloak como Identity Provider
- Federación de identidades

### Mejoras Adicionales

- [ ] Persistencia con PostgreSQL
- [ ] Cifrado de secretos en base de datos
- [ ] Auditoría completa de accesos
- [ ] Rate limiting
- [ ] API versioning
- [ ] OpenAPI/Swagger documentation
- [ ] Docker Compose setup
- [ ] CI/CD con GitHub Actions

---

## 🤝 Contribuciones

Este es un proyecto educativo. Sugerencias y mejoras son bienvenidas.

---

## 📝 Licencia

Este proyecto es material educativo y está disponible libremente para propósitos de aprendizaje.

---

## 👨‍🏫 Notas para Instructores

### Orden de Enseñanza Recomendado

1. **Teoría de Autenticación/Autorización** (30 min)
   - Diferencia entre autenticación y autorización
   - HTTP 401 vs 403
   - Basic Auth vs Token-based

2. **Parte 1: Hands-on con Basic Auth** (60 min)
   - Crear proyecto
   - Implementar endpoints con `@RolesAllowed`
   - Ejecutar script de pruebas
   - Discutir limitaciones

3. **Teoría de JWT** (45 min)
   - Leer `TEORIA-PARTE2.md`
   - Anatomía del JWT
   - Stateless vs Stateful
   - RSA vs HMAC

4. **Parte 2: Hands-on con JWT** (90 min)
   - Generar llaves RSA
   - Implementar login y endpoints
   - Ejecutar script de pruebas
   - Decodificar tokens en jwt.io

5. **Comparación y Discusión** (30 min)
   - ¿Cuándo usar cada método?
   - Trade-offs de seguridad
   - Casos de uso reales

### Puntos Clave a Enfatizar

1. **JWT NO es encriptación**: El payload es visible
2. **Stateless = Escalabilidad**: Pero dificulta revocación
3. **Expiración es crítica**: Limitar ventana de ataque
4. **RSA en microservicios**: Separación de responsabilidades
5. **Aislamiento por diseño**: Filtrar siempre por usuario autenticado

---

## ✅ Checklist de Verificación Final

Antes de dar por completado el ejercicio:

- [ ] El proyecto compila sin errores
- [ ] Quarkus arranca en modo dev
- [ ] Health check responde
- [ ] Basic Auth funciona con admin y auditor
- [ ] Login genera JWT válido
- [ ] JWT permite acceso a endpoints protegidos
- [ ] Aislamiento entre usuarios funciona
- [ ] Scripts de prueba ejecutan sin errores
- [ ] Puedes decodificar un JWT en jwt.io
- [ ] Entiendes la diferencia entre 401 y 403

---

## 📞 Soporte

Para dudas o problemas:
1. Revisar sección de Troubleshooting
2. Verificar logs de Quarkus
3. Consultar documentación oficial de Quarkus
4. Revisar archivos de teoría incluidos
