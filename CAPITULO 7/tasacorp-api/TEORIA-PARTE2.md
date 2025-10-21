# 📚 Teoría: Perfiles y Configuración Sensible en Quarkus

## Capítulo 7 - Parte 2: Ambientes y Seguridad

---

## 📑 Tabla de Contenidos

1. [¿Qué son los Perfiles?](#qué-son-los-perfiles)
2. [Historia de la Gestión de Ambientes](#historia-de-la-gestión-de-ambientes)
3. [Perfiles en Quarkus: Arquitectura Interna](#perfiles-en-quarkus-arquitectura-interna)
4. [Los Tres Perfiles Estándar](#los-tres-perfiles-estándar)
5. [Configuración Sensible: El Problema](#configuración-sensible-el-problema)
6. [Gestión de Secretos: Evolución](#gestión-de-secretos-evolución)
7. [HashiCorp Vault: Teoría Profunda](#hashicorp-vault-teoría-profunda)
8. [Integración Vault + Quarkus](#integración-vault--quarkus)
9. [Patrones de Configuración por Ambiente](#patrones-de-configuración-por-ambiente)
10. [Seguridad en Configuración](#seguridad-en-configuración)
11. [Mejores Prácticas](#mejores-prácticas)
12. [Anti-Patrones](#anti-patrones)
13. [Casos de Uso del Mundo Real](#casos-de-uso-del-mundo-real)

---

## 🎭 ¿Qué son los Perfiles?

### Definición Formal

Un **perfil** (profile) es un conjunto de configuraciones específicas que se activan según el contexto de ejecución de la aplicación, permitiendo que el mismo artefacto binario se comporte diferente en distintos ambientes.

### El Problema que Resuelven

#### Escenario Sin Perfiles

Imagina que desarrollas una aplicación bancaria:

```
Desarrollo Local:
├── Base de datos: localhost:5432
├── API externa: https://sandbox.api.com
├── Logs: DEBUG (muy verbosos)
└── Cache: Desactivado (cambios inmediatos)

Producción:
├── Base de datos: prod-db.internal.bank.com:5432
├── API externa: https://api.bank.com
├── Logs: ERROR (solo errores críticos)
└── Cache: Activado (alto rendimiento)
```

**Pregunta:** ¿Cómo manejar estas diferencias?

#### ❌ Solución Mala: Múltiples Compilaciones

```bash
# Compilar versión de desarrollo
mvn package -Denv=dev

# Compilar versión de producción
mvn package -Denv=prod
```

**Problemas:**
- 2 binarios diferentes = 2 pruebas diferentes
- Lo que funciona en dev-build puede fallar en prod-build
- Pesadilla de mantenimiento
- Violación del principio "Build once, deploy many"

#### ✅ Solución Correcta: Perfiles

```bash
# UN SOLO binario
mvn package

# Diferentes ejecuciones
java -jar app.jar  # Producción (perfil por defecto)
java -jar app.jar -Dquarkus.profile=dev  # Desarrollo
```

**Ventajas:**
- ✅ Un solo artefacto
- ✅ Mismo código probado en todos los ambientes
- ✅ Configuración externalizada
- ✅ Cambio trivial entre ambientes

### Analogía: El Actor de Teatro

Un actor es **una persona** (el código compilado), pero puede representar **diferentes personajes** (perfiles) según la obra:

```
🎭 MISMO ACTOR, DIFERENTES ROLES:

Obra de Comedia (DEV):
├── Vestuario: Informal
├── Actuación: Relajada
├── Público: Pequeño (equipo de desarrollo)
└── Errores: Se perdonan (debugging activo)

Obra Dramática (TEST):
├── Vestuario: Formal
├── Actuación: Ensayada
├── Público: Mediano (QA team)
└── Errores: Se registran (logs de prueba)

Estreno en Broadway (PROD):
├── Vestuario: Impecable
├── Actuación: Perfecta
├── Público: Miles (usuarios reales)
└── Errores: Inaceptables (alta disponibilidad)
```

---

## 📜 Historia de la Gestión de Ambientes

### Era Pre-Java (1970s-1990s)

**Archivos de configuración separados:**
```
/etc/app/
├── config.dev
├── config.test
└── config.prod
```

**Despliegue:**
```bash
# Copiar el archivo correcto
cp config.prod config
./start-app
```

**Problemas:**
- Error humano al copiar archivos
- Sincronización manual
- Sin versionado unificado

### Era Java Clásico (2000s)

**System Properties:**
```bash
java -Denv=prod -Ddb.url=... -Dapi.key=... MyApp
```

**Problemas:**
- Líneas de comando gigantes
- Difícil de mantener
- No estructurado

### Era Application Servers (2000s-2010s)

**JNDI (Java Naming and Directory Interface):**
```java
Context ctx = new InitialContext();
DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/mydb");
```

**Características:**
- Configuración en el servidor de aplicaciones
- Lookup en runtime
- Complejidad adicional

### Era Spring (2010s)

**Spring Profiles:**
```java
@Profile("dev")
@Configuration
public class DevConfig { }

@Profile("prod")
@Configuration
public class ProdConfig { }
```

**Mejoras:**
- Perfiles nativos
- Autowiring condicional
- Más declarativo

### Era Cloud-Native (2017+)

**Quarkus/MicroProfile Config:**
```properties
%dev.database.url=localhost
%prod.database.url=${DATABASE_URL}
```

**Características:**
- Perfiles lightweight
- Cloud-first
- Container-ready
- Estándar (no vendor lock-in)

---

## 🏗️ Perfiles en Quarkus: Arquitectura Interna

### Cómo Funciona Internamente

#### 1. Resolución del Perfil Activo

Quarkus determina el perfil activo en este orden:

```
1. Propiedad del sistema: -Dquarkus.profile=test
   ↓ (si no existe)
2. Variable de entorno: QUARKUS_PROFILE=test
   ↓ (si no existe)
3. Detección automática:
   - ¿Es quarkus:dev? → dev
   - ¿Es test Maven? → test
   - ¿Es jar ejecutable? → prod
```

#### 2. Carga de Configuración

Una vez determinado el perfil, Quarkus carga las propiedades:

```
Para perfil "test":

1. Cargar propiedades BASE (sin prefijo)
   app.name=MyApp
   database.url=default

2. Sobreescribir con propiedades del perfil
   %test.database.url=testdb
   
3. Aplicar ENV vars (si existen)
   DATABASE_URL=override-testdb
   
4. Aplicar System Props (si existen)
   -Ddatabase.url=final-override
```

#### 3. Ejemplo de Resolución Paso a Paso

**Archivos:**
```properties
# application.properties
app.name=TasaCorp
app.timeout=30

%dev.app.timeout=5
%dev.app.debug=true

%prod.app.timeout=60
%prod.app.secure=true
```

**Ejecución en DEV:**
```bash
./mvnw quarkus:dev
```

**Resolución:**
```
Perfil activo: dev

Propiedades resultantes:
├── app.name = "TasaCorp"      (base)
├── app.timeout = 5            (sobrescrito por %dev)
├── app.debug = true           (solo en dev)
└── app.secure = undefined     (solo en prod)
```

**Ejecución en PROD:**
```bash
java -jar app.jar
```

**Resolución:**
```
Perfil activo: prod

Propiedades resultantes:
├── app.name = "TasaCorp"      (base)
├── app.timeout = 60           (sobrescrito por %prod)
├── app.debug = undefined      (solo en dev)
└── app.secure = true          (solo en prod)
```

### ConfigSources con Perfiles

Cada perfil introduce ConfigSources adicionales:

```
Sin perfil activo:
├── System Properties (400)
├── ENV Variables (300)
├── application.yaml (255)
└── application.properties (250)

Con perfil "test" activo:
├── System Properties (400)
├── ENV Variables (300)
├── application-test.yaml (265)      ← Nuevo
├── application.yaml (255)
├── application-test.properties (260) ← Nuevo
└── application.properties (250)
```

---

## 🎯 Los Tres Perfiles Estándar

### %dev - Desarrollo

#### Filosofía

> "Máxima productividad del desarrollador, mínimas restricciones"

#### Características

**Activación automática:**
- `./mvnw quarkus:dev`
- `quarkus dev`

**Comportamiento típico:**
- ✅ Hot reload activado
- ✅ Dev Services (bases de datos automáticas)
- ✅ Logs verbosos (DEBUG)
- ✅ Sin autenticación/autorización estricta
- ✅ CORS permisivo
- ✅ Cache desactivado
- ✅ Validaciones relajadas

**Ejemplo de configuración:**
```properties
%dev.quarkus.log.level=DEBUG
%dev.quarkus.datasource.devservices.enabled=true
%dev.quarkus.http.cors=true
%dev.app.security.enabled=false
%dev.app.cache.enabled=false
```

#### Casos de Uso

- Desarrollo de features
- Debugging
- Exploración de APIs
- Pruebas manuales rápidas

#### Analogía

**DEV es como practicar en un gimnasio:**
- Ambiente controlado
- Puedes cometer errores
- Feedback inmediato
- Sin consecuencias reales

---

### %test - Testing

#### Filosofía

> "Ambiente controlado para pruebas automatizadas y validación de QA"

#### Características

**Activación automática:**
- Tests de Maven/Gradle
- `mvn test`
- `@QuarkusTest`

**Comportamiento típico:**
- ✅ Base de datos en memoria (H2)
- ✅ Mocks de servicios externos
- ✅ Logs moderados (INFO)
- ✅ Validaciones completas
- ✅ Timeouts cortos
- ✅ Transacciones rollback automático

**Ejemplo de configuración:**
```properties
%test.quarkus.datasource.db-kind=h2
%test.quarkus.datasource.jdbc.url=jdbc:h2:mem:test
%test.quarkus.hibernate-orm.database.generation=drop-and-create
%test.app.api.mock=true
%test.app.timeout=5s
```

#### Casos de Uso

- Tests unitarios
- Tests de integración
- Validación de CI/CD
- Tests de regresión

#### Analogía

**TEST es como un simulador de vuelo:**
- Situaciones controladas
- Puede fallar sin peligro
- Métricas detalladas
- Repetible infinitas veces

---

### %prod - Producción

#### Filosofía

> "Máxima seguridad, rendimiento y estabilidad para usuarios reales"

#### Características

**Activación automática:**
- `java -jar app.jar`
- Contenedor Docker
- Kubernetes

**Comportamiento típico:**
- ✅ Base de datos real (cluster)
- ✅ Servicios externos reales
- ✅ Logs mínimos (ERROR/WARN)
- ✅ Autenticación/autorización estricta
- ✅ CORS restrictivo
- ✅ Cache agresivo
- ✅ Validaciones exhaustivas
- ✅ Health checks
- ✅ Métricas

**Ejemplo de configuración:**
```properties
%prod.quarkus.log.level=ERROR
%prod.quarkus.datasource.jdbc.url=${DATABASE_URL}
%prod.quarkus.http.cors=false
%prod.app.security.enabled=true
%prod.app.cache.enabled=true
%prod.app.cache.ttl=3600
```

#### Casos de Uso

- Usuarios finales
- Transacciones reales
- Datos sensibles
- Alta disponibilidad

#### Analogía

**PROD es como un vuelo comercial real:**
- Cero tolerancia a errores
- Vidas (datos) en juego
- Altamente regulado
- Máximo profesionalismo

---

## 🔐 Configuración Sensible: El Problema

### ¿Qué es Información Sensible?

Datos que, si son expuestos, comprometen la seguridad del sistema:

**Ejemplos:**
- 🔑 Contraseñas de bases de datos
- 🔑 API keys de servicios externos
- 🔑 Certificados privados
- 🔑 Tokens de acceso
- 🔑 Claves de cifrado
- 🔑 Credenciales de servicios cloud

### El Problema Clásico

#### ❌ Antipatrón: Secretos en Git

```properties
# application.properties (EN GIT)
database.password=SuperSecret123!
aws.access.key=AKIAIOSFODNN7EXAMPLE
stripe.api.key=sk_live_51H...
```

**Problemas:**

**1. Exposición Permanente:**
```bash
# El secreto queda EN EL HISTORIAL DE GIT PARA SIEMPRE
git log --all -- application.properties
```

Incluso si lo borras después, sigue en el historial.

**2. Acceso No Controlado:**
- Cualquier desarrollador con acceso al repo ve los secretos
- Ex-empleados mantienen acceso histórico
- Forks del repositorio tienen los secretos

**3. Rotación Imposible:**
- Cambiar una contraseña requiere commit + push + deploy
- Downtime durante rotación
- No se puede rotar sin tocar código

**4. Auditoría Inexistente:**
- No sabes quién accedió a qué secreto
- No sabes cuándo se usó
- No hay trazabilidad

### Vectores de Ataque

#### Escenario Real: Travis CI Leak (2016)

```yaml
# .travis.yml (público en GitHub)
env:
  global:
    - DB_PASSWORD=production_password  # ❌ Expuesto
```

**Resultado:** Miles de secretos expuestos públicamente.

#### Escenario Real: Uber 2016

**Problema:** Claves de AWS hardcodeadas en repo privado de GitHub.

**Ataque:** Hacker obtuvo acceso al repo → Robó claves → Accedió a AWS → 57 millones de datos de usuarios comprometidos.

**Costo:** $148 millones USD en multas y compensaciones.

---

## 🔄 Gestión de Secretos: Evolución

### Nivel 1: Hardcoded (❌ Nunca hacer)

```java
String password = "SuperSecret123";
```

**Seguridad:** 0/10

### Nivel 2: Archivo de Configuración Local

```properties
# application.properties (NO en git)
database.password=SuperSecret123
```

**.gitignore:**
```
application.properties
```

**Seguridad:** 2/10  
**Problema:** ¿Cómo comparten el archivo los devs? ¿Email? 😱

### Nivel 3: Variables de Entorno

```bash
export DATABASE_PASSWORD=SuperSecret123
java -jar app.jar
```

**Seguridad:** 4/10  
**Problema:** Aún visible en `ps aux`, scripts de deploy, etc.

### Nivel 4: Encrypted Config Files

```bash
# Encriptar
ansible-vault encrypt secrets.yml

# Usar
ansible-vault decrypt secrets.yml
```

**Seguridad:** 6/10  
**Problema:** La clave maestra sigue siendo un problema.

### Nivel 5: Cloud Provider Secrets

**AWS Secrets Manager:**
```python
secret = boto3.client('secretsmanager').get_secret_value(
    SecretId='prod/db/password'
)
```

**Seguridad:** 8/10  
**Problema:** Vendor lock-in, multi-cloud difícil.

### Nivel 6: HashiCorp Vault (✅ Solución Moderna)

```java
@ConfigProperty(name = "database.password")
String password; // Se lee de Vault automáticamente
```

**Seguridad:** 10/10  
**Ventajas:**
- ✅ Secretos centralizados
- ✅ Encriptación en reposo y tránsito
- ✅ Control de acceso granular
- ✅ Auditoría completa
- ✅ Rotación automática
- ✅ Secretos con TTL
- ✅ Multi-cloud

---

## 🏛️ HashiCorp Vault: Teoría Profunda

### ¿Qué es Vault?

**Definición oficial:**
> "A tool for secrets management, encryption as a service, and privileged access management"

**Traducción simple:**
> "Una caja fuerte digital centralizada para todos tus secretos"

### Arquitectura de Vault

```
┌─────────────────────────────────────────┐
│         APLICACIONES                     │
│  ┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐    │
│  │ API │  │ Web │  │ CLI │  │ Job │    │
│  └──┬──┘  └──┬──┘  └──┬──┘  └──┬──┘    │
│     │        │        │        │        │
│     └────────┴────────┴────────┘        │
│              ↓                           │
├─────────────────────────────────────────┤
│         VAULT SERVER                     │
│  ┌───────────────────────────────────┐  │
│  │   Authentication (AuthN)          │  │
│  │   ├── Token Auth                  │  │
│  │   ├── LDAP/AD                     │  │
│  │   ├── Kubernetes                  │  │
│  │   └── AWS IAM                     │  │
│  └───────────────────────────────────┘  │
│  ┌───────────────────────────────────┐  │
│  │   Authorization (AuthZ)           │  │
│  │   ├── Policies (HCL)              │  │
│  │   └── ACLs                        │  │
│  └───────────────────────────────────┘  │
│  ┌───────────────────────────────────┐  │
│  │   Secrets Engines                 │  │
│  │   ├── KV (Key-Value)              │  │
│  │   ├── Database (dynamic)          │  │
│  │   ├── AWS (dynamic)               │  │
│  │   └── PKI (certificates)          │  │
│  └───────────────────────────────────┘  │
│  ┌───────────────────────────────────┐  │
│  │   Audit Devices                   │  │
│  │   ├── File                        │  │
│  │   ├── Syslog                      │  │
│  │   └── Socket                      │  │
│  └───────────────────────────────────┘  │
├─────────────────────────────────────────┤
│         STORAGE BACKEND                  │
│  ┌───────────────────────────────────┐  │
│  │   Encrypted at rest               │  │
│  │   ├── Consul                      │  │
│  │   ├── etcd                        │  │
│  │   ├── S3                          │  │
│  │   └── PostgreSQL                  │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

### Conceptos Fundamentales

#### 1. Secrets Engines

**KV (Key-Value) v2:**
- Almacenamiento simple clave-valor
- Versionado automático
- Rollback a versiones anteriores

**Dynamic Secrets:**
- Credenciales generadas on-demand
- Con tiempo de vida limitado (TTL)
- Se revocan automáticamente

**Ejemplo de Dynamic Secret (PostgreSQL):**
```bash
# Vault genera credenciales temporales
vault read database/creds/myapp-role

Key                Value
---                -----
lease_id           database/creds/myapp-role/abc123
lease_duration     1h
password           A1a-random-password-xyz
username           v-token-myapp-abc123
```

Después de 1 hora, el usuario se elimina automáticamente.

#### 2. Authentication Methods

**Token Auth (default):**
```bash
vault login token=s.abc123xyz
```

**AppRole (para aplicaciones):**
```bash
vault write auth/approle/login \
    role_id=my-role-id \
    secret_id=my-secret-id
```

**Kubernetes Auth:**
```yaml
# La app en K8s se autentica automáticamente
vault write auth/kubernetes/login \
    role=myapp \
    jwt=<service-account-token>
```

#### 3. Policies (Control de Acceso)

```hcl
# Policy: read-only-secrets
path "secret/data/myapp/*" {
  capabilities = ["read", "list"]
}

path "secret/data/admin/*" {
  capabilities = ["deny"]
}
```

#### 4. Audit Logging

Todos los accesos quedan registrados:

```json
{
  "time": "2025-10-19T12:34:56Z",
  "type": "request",
  "auth": {
    "display_name": "token-app1"
  },
  "request": {
    "operation": "read",
    "path": "secret/data/database/password"
  }
}
```

### Vault en Modo Dev vs Producción

#### Dev Mode (inseguro, para desarrollo)

```bash
vault server -dev
```

**Características:**
- 🔓 Sin SSL/TLS
- 🔓 Root token conocido
- 💾 Almacenamiento en memoria
- 🗑️ Se pierde todo al reiniciar
- 🚀 Arranque instantáneo

**Uso:** SOLO desarrollo local.

#### Production Mode (seguro)

```hcl
# config.hcl
storage "consul" {
  address = "127.0.0.1:8500"
  path    = "vault/"
}

listener "tcp" {
  address     = "0.0.0.0:8200"
  tls_cert_file = "/path/to/cert.pem"
  tls_key_file  = "/path/to/key.pem"
}

seal "awskms" {
  region     = "us-west-2"
  kms_key_id = "alias/vault-key"
}
```

**Características:**
- 🔒 SSL/TLS obligatorio
- 🔒 Unsealing requerido
- 💾 Almacenamiento persistente
- 🔐 Secrets encriptados en reposo
- 📊 Alta disponibilidad

---

## 🔗 Integración Vault + Quarkus

### Cómo Funciona la Integración

#### Flujo Completo

```
1. App arranca
   ↓
2. Quarkus lee config
   ↓
3. Encuentra: database.password=${db-password}
   ↓
4. Detecta que debe resolver desde Vault
   ↓
5. Se conecta a Vault (http://vault:8200)
   ↓
6. Se autentica (token en este caso)
   ↓
7. Lee secret: secret/tasacorp/db-password
   ↓
8. Reemplaza ${db-password} con el valor real
   ↓
9. Inyecta en @ConfigProperty
```

### Configuración Detallada

```properties
# URL de Vault
quarkus.vault.url=https://vault.company.com:8200

# Método de autenticación: Token
quarkus.vault.authentication.client-token=s.abc123xyz

# Path donde están los secretos
quarkus.vault.secret-config-kv-path=tasacorp

# Versión del KV engine (2 es la actual)
quarkus.vault.kv-secret-engine-version=2

# Timeout de conexión
quarkus.vault.connect-timeout=5s

# Timeout de lectura
quarkus.vault.read-timeout=1s
```

### Path Resolution

**Configuración:**
```properties
quarkus.vault.secret-config-kv-path=myapp
database.password=${db-password}
```

**Vault path real:**
```
secret/data/myapp
```

**Nota:** Vault añade `/data/` automáticamente en KV v2.

### Múltiples Secretos

```properties
# Definir paths de secretos
quarkus.vault.secret-config-kv-path=app1,app2,shared

# Usar secretos
database.password=${db-password}      # Busca en app1, app2, shared
api.key=${external-api-key}           # Busca en app1, app2, shared
```

**Búsqueda:** Primer match gana.

### Transit Engine (Encryption as a Service)

```java
@Inject
VaultTransitSecretEngine transit;

// Encriptar
String encrypted = transit.encrypt("credit-cards", "4111111111111111");
// → vault:v1:8SDd3WHDOjf7mq69CyCqYjBXAiQQAVZRkFM13ok481zoCmHnSeDX9vyf7w==

// Desencriptar
String decrypted = transit.decrypt("credit-cards", encrypted);
// → 4111111111111111
```

---

## 🎨 Patrones de Configuración por Ambiente

### Patrón 1: Configuración en Capas

```
Capa 1: BASE (común a todos)
    ├── app.name=TasaCorp
    └── app.version=1.0.0

Capa 2: AMBIENTE (específico)
    DEV:  database.url=localhost
    TEST: database.url=testdb
    PROD: database.url=prod-cluster

Capa 3: SECRETOS (desde Vault)
    PROD: database.password=${vault-secret}
```

### Patrón 2: Feature Flags por Ambiente

```properties
# Features disponibles según ambiente
%dev.features.new-algorithm=true
%dev.features.admin-panel=true

%test.features.new-algorithm=true
%test.features.admin-panel=false

%prod.features.new-algorithm=false
%prod.features.admin-panel=false
```

### Patrón 3: Degradación por Ambiente

```properties
# Timeouts más permisivos en dev
%dev.api.timeout=300s
%dev.api.retries=10

# Timeouts estrictos en prod
%prod.api.timeout=5s
%prod.api.retries=3
```

### Patrón 4: Configuración Multi-Región

```properties
# Base
app.name=GlobalBank

# Por región
%us.database.url=us-east-1.rds.amazonaws.com
%us.currency.default=USD

%eu.database.url=eu-west-1.rds.amazonaws.com
%eu.currency.default=EUR

%asia.database.url=ap-southeast-1.rds.amazonaws.com
%asia.currency.default=JPY
```

---

## 🛡️ Seguridad en Configuración

### Principio de Mínimo Privilegio

**Regla:** Cada aplicación solo debe tener acceso a los secretos que necesita.

```hcl
# Policy para app1
path "secret/data/app1/*" {
  capabilities = ["read"]
}

path "secret/data/shared/*" {
  capabilities = ["read"]
}

# DENEGAR todo lo demás
path "secret/data/*" {
  capabilities = ["deny"]
}
```

### Defense in Depth

**Múltiples capas de seguridad:**

```
1. Network: VPN/Private network
2. Firewall: Solo IPs autorizadas
3. TLS: Comunicación encriptada
4. Authentication: Token/AppRole
5. Authorization: Policies
6. Audit: Logging de todos los accesos
7. Rotation: Cambio periódico de secretos
```

### Secretos en Logs

```java
// ❌ MAL: Secreto en logs
log.info("Connecting with password: " + password);

// ✅ BIEN: Sin secreto
log.info("Connecting to database");

// ✅ BIEN: Redactado
log.info("Connecting with password: ****");
```

### Validación de Secretos

```java
@ApplicationScoped
public class SecretValidator {
    
    @ConfigProperty(name = "api.key")
    String apiKey;
    
    void validate(@Observes StartupEvent event) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("API key is required");
        }
        
        if (!apiKey.matches("^sk_[a-zA-Z0-9]{32}$")) {
            throw new IllegalStateException("API key format invalid");
        }
    }
}
```

---

## ✅ Mejores Prácticas

### 1. Separación de Secretos por Ambiente

```
secret/
├── dev/
│   └── myapp/
│       ├── db-password (fake/test data)
│       └── api-key (sandbox key)
├── test/
│   └── myapp/
│       ├── db-password (test DB)
│       └── api-key (test key)
└── prod/
    └── myapp/
        ├── db-password (real password)
        └── api-key (production key)
```

### 2. Rotación Periódica

```hcl
# Vault policy con TTL
path "database/creds/myapp" {
  capabilities = ["read"]
  
  # Credenciales válidas por 1 hora
  max_lease_ttl = "1h"
}
```

### 3. Versionado de Secretos

```bash
# Actualizar secreto (crea versión 2)
vault kv put secret/app api-key=new-key

# Leer versión específica
vault kv get -version=1 secret/app

# Rollback si algo falla
vault kv rollback -version=1 secret/app
```

### 4. Segregación de Responsabilidades

**Nunca:**
- Desarrolladores con acceso a secretos de producción
- Mismas credenciales para dev y prod
- Secretos compartidos entre aplicaciones

**Siempre:**
- Secretos únicos por app y ambiente
- Acceso granular (policies)
- Auditoría de todos los accesos

### 5. Documentar Secretos Requeridos

```markdown
# Secretos Requeridos

## Desarrollo
- `tasacorp.provider.apikey`: API key de sandbox

## Producción
- `tasacorp.provider.apikey`: API key de producción (desde Vault)
- `tasacorp.database.password`: Password de PostgreSQL (desde Vault)

## Cómo Obtener
1. Solicitar acceso a Vault
2. Autenticarse: `vault login -method=ldap username=tu-usuario`
3. Leer: `vault kv get secret/tasacorp`
```

---

## ❌ Anti-Patrones

### 1. Secretos en Variables de Entorno (Producción)

```bash
# ❌ MAL en producción
docker run -e DATABASE_PASSWORD=secret123 myapp
```

**Problema:**
- Visible en `docker inspect`
- Visible en orquestadores (K8s configmaps)
- Se loguea en múltiples lugares

**Solución:**
```yaml
# ✅ BIEN: Usar Vault
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: app
    env:
    - name: DATABASE_PASSWORD
      valueFrom:
        secretKeyRef:
          name: vault-secret
```

### 2. Secretos en Código Encriptado

```java
// ❌ MAL: Encriptar en código
String encrypted = "AES:abc123...";
String password = decrypt(encrypted, KEY);
```

**Problema:** ¿Dónde guardas KEY?

### 3. Secretos Compartidos

```
# ❌ MAL: Misma contraseña para todo
dev_db_password=shared123
test_db_password=shared123
prod_db_password=shared123
```

**Problema:** Si se compromete uno, se comprometen todos.

### 4. Sin Rotación

```bash
# ❌ MAL: Misma contraseña por años
database.password=Password123!
# Creada: 2020-01-01
# Última rotación: Nunca
```

**Problema:** Mayor ventana de compromiso.

---

## 🏢 Casos de Uso del Mundo Real

### Caso 1: Banco Internacional

**Problema:** Aplicación desplegada en 15 países, cada uno con su regulación.

**Solución con Perfiles:**
```properties
# Base
app.name=GlobalBank

# Por país
%argentina.currency=ARS
%argentina.regulator.url=https://bcra.gob.ar
%argentina.tax.rate=0.21

%chile.currency=CLP
%chile.regulator.url=https://sbif.cl
%chile.tax.rate=0.19
```

**Despliegue:**
```bash
# Argentina
docker run -e QUARKUS_PROFILE=argentina mybank:latest

# Chile
docker run -e QUARKUS_PROFILE=chile mybank:latest
```

### Caso 2: Startup en Crecimiento

**Evolución de Secretos:**

**Fase 1 (MVP):** Secretos en ENV vars
```bash
export DB_PASSWORD=simple123
```

**Fase 2 (10 clientes):** Archivo .env
```bash
# .env (no en git)
DB_PASSWORD=better_password_456
```

**Fase 3 (100 clientes):** Secrets Manager
```python
# AWS Secrets Manager
secret = get_secret("prod/db/password")
```

**Fase 4 (1000+ clientes):** Vault
```properties
database.password=${vault:secret/db#password}
```

### Caso 3: Empresa de Salud (HIPAA Compliance)

**Requerimientos:**
- ✅ Encriptación end-to-end
- ✅ Auditoría completa
- ✅ Rotación automática cada 90 días
- ✅ Access control estricto
- ✅ No secrets en repos

**Solución:**
```hcl
# Vault policy
path "healthcare/prod/*" {
  capabilities = ["read"]
  
  # Solo doctores pueden acceder
  allowed_entities = ["doctor-role"]
  
  # Auditar TODO
  audit = ["file", "syslog"]
}
```

**Rotación automática:**
```hcl
path "database/creds/hipaa-app" {
  # Credenciales válidas 90 días
  max_lease_ttl = "2160h"
}
```

### Caso 4: E-commerce en Black Friday

**Problema:** Necesitan escalar de 10 a 1000 instancias en minutos.

**Solución:**
```yaml
# Kubernetes deployment
apiVersion: apps/v1
kind: Deployment
spec:
  replicas: 1000  # ¡Escalar!
  template:
    spec:
      containers:
      - name: app
        env:
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: vault-db-creds  # Mismo secreto para todos
```

**Ventaja de Vault:**
- 1000 instancias usan el mismo path de Vault
- No necesitas distribuir 1000 secretos diferentes
- Vault maneja la carga

---

## 📊 Comparación de Soluciones

| Solución | Seguridad | Complejidad | Costo | Cloud Lock-in | Auditoría |
|----------|-----------|-------------|-------|---------------|-----------|
| Hardcoded | 0/10 | Muy baja | $0 | No | No |
| ENV Vars | 2/10 | Baja | $0 | No | No |
| Encrypted Files | 4/10 | Media | $0 | No | Limitada |
| AWS Secrets | 7/10 | Media | $$ | Sí | Sí |
| Azure Key Vault | 7/10 | Media | $$ | Sí | Sí |
| GCP Secret Manager | 7/10 | Media | $$ | Sí | Sí |
| **HashiCorp Vault** | **10/10** | **Alta** | **$$$** | **No** | **Completa** |

---

## 🎓 Resumen de Conceptos Clave

### Perfiles
- **Permiten** misma app, diferente comportamiento
- **%dev:** Desarrollo rápido, sin restricciones
- **%test:** Ambiente controlado de pruebas
- **%prod:** Seguridad y rendimiento máximos

### Configuración Sensible
- **Nunca** en código o git
- **Siempre** externalizada
- **Mejor** en sistema dedicado (Vault)

### Vault
- **Centraliza** todos los secretos
- **Encripta** en reposo y tránsito
- **Audita** todos los accesos
- **Rota** secretos automáticamente

### Mejores Prácticas
- Separar secretos por ambiente
- Rotación periódica obligatoria
- Mínimo privilegio siempre
- Documentar secretos requeridos

---

## 🎓 Preguntas para Reflexión

1. ¿Por qué es crítico no tener secretos en git?
2. ¿Qué ventajas tiene Vault sobre variables de entorno?
3. ¿Cuándo usarías cada perfil (dev, test, prod)?
4. ¿Cómo implementarías rotación de secretos en tu app?
5. ¿Qué harías si un secreto se filtra públicamente?

---

## 📚 Referencias

- [HashiCorp Vault Documentation](https://www.vaultproject.io/docs)
- [Quarkus Vault Guide](https://quarkus.io/guides/vault)
- [OWASP Secrets Management](https://owasp.org/www-community/vulnerabilities/Use_of_hard-coded_password)
- [12-Factor App: Config](https://12factor.net/config)
- [CIS Benchmark: Secrets Management](https://www.cisecurity.org/)

---

**Has completado la teoría completa de Perfiles y Seguridad. Ahora tienes el conocimiento para gestionar configuraciones de forma profesional y segura.** 🎉🔐