# 📚 Teoría: Externalización de Configuraciones en Quarkus

## Capítulo 7 - Parte 1: Fundamentos de Configuración

---

## 📑 Tabla de Contenidos

1. [¿Qué es la Externalización de Configuraciones?](#qué-es-la-externalización-de-configuraciones)
2. [Historia y Evolución](#historia-y-evolución)
3. [¿Por qué Externalizar Configuraciones?](#por-qué-externalizar-configuraciones)
4. [MicroProfile Config: El Estándar](#microprofile-config-el-estándar)
5. [application.properties vs application.yaml](#applicationproperties-vs-applicationyaml)
6. [Tipos de Configuración](#tipos-de-configuración)
7. [Inyección de Configuraciones](#inyección-de-configuraciones)
8. [Prioridades de Carga: Teoría Profunda](#prioridades-de-carga-teoría-profunda)
9. [Variables de Entorno](#variables-de-entorno)
10. [System Properties en la JVM](#system-properties-en-la-jvm)
11. [Patrones de Configuración](#patrones-de-configuración)
12. [Mejores Prácticas](#mejores-prácticas)
13. [Anti-Patrones](#anti-patrones)
14. [Casos de Uso Reales](#casos-de-uso-reales)

---

## 🎯 ¿Qué es la Externalización de Configuraciones?

### Definición

La **externalización de configuraciones** es el patrón de diseño que consiste en separar los valores configurables de una aplicación del código fuente, permitiendo que estos valores sean modificados sin necesidad de recompilar la aplicación.

### Analogía del Termostato

Imagina que tu casa es una aplicación:

**❌ Sin externalización (hardcoded):**
```
Para cambiar la temperatura, tienes que:
1. Abrir la pared
2. Cambiar los cables del termostato
3. Cerrar la pared
4. Reiniciar el sistema eléctrico
```

**✅ Con externalización:**
```
Para cambiar la temperatura, solo:
1. Giras la perilla del termostato
2. Listo
```

### Ejemplo Comparativo

**Código SIN externalización:**
```java
public class BankService {
    private static final String API_URL = "https://api.banco.com"; // ❌
    private static final double COMMISSION = 2.5; // ❌
    private static final int MAX_RETRIES = 3; // ❌
    
    public void process() {
        // Usa valores fijos
    }
}
```

**Problemas:**
- Para cambiar la URL, hay que modificar el código
- Para ajustar la comisión, hay que recompilar
- Diferentes ambientes (dev, test, prod) requieren diferentes compilaciones

**Código CON externalización:**
```java
@ApplicationScoped
public class BankService {
    @ConfigProperty(name = "bank.api.url")
    String apiUrl; // ✅ Configurable
    
    @ConfigProperty(name = "bank.commission.rate")
    Double commission; // ✅ Configurable
    
    @ConfigProperty(name = "bank.max.retries")
    Integer maxRetries; // ✅ Configurable
    
    public void process() {
        // Usa valores configurables
    }
}
```

**Ventajas:**
- Cambiar valores sin recompilar
- Misma aplicación, diferentes configuraciones
- Configuración por ambiente (dev, test, prod)

---

## 📜 Historia y Evolución

### Era Pre-Java (1990s)

**Archivos .ini y .conf**
```ini
[database]
host=localhost
port=3306
```

**Problemas:**
- Sin tipado
- Sin validación
- Parsing manual

### Java Clásico (2000s)

**Properties Files**
```java
Properties props = new Properties();
FileInputStream in = new FileInputStream("config.properties");
props.load(in);
String value = props.getProperty("key");
```

**Problemas:**
- Todo es String
- No hay inyección automática
- Código repetitivo (boilerplate)

### Java EE / Spring (2010s)

**Spring @Value**
```java
@Value("${app.name}")
private String appName;
```

**Mejoras:**
- Inyección automática
- Conversión de tipos
- SpEL (Spring Expression Language)

### MicroProfile / Quarkus (2017+)

**MicroProfile Config**
```java
@ConfigProperty(name = "app.name")
String appName;
```

**Ventajas:**
- Estándar (no vendor-specific)
- Prioridades claras
- Extensible
- Cloud-native

---

## 🤔 ¿Por qué Externalizar Configuraciones?

### 1. **Separación de Responsabilidades**

**Principio:** El código debe enfocarse en la lógica de negocio, no en valores específicos del entorno.

```
┌─────────────────┐
│  CÓDIGO FUENTE  │  ← Lógica de negocio (NUNCA cambia entre ambientes)
└─────────────────┘
        ↓
┌─────────────────┐
│ CONFIGURACIÓN   │  ← Valores específicos (SIEMPRE cambia entre ambientes)
└─────────────────┘
```

### 2. **Portabilidad**

**Mismo binario, múltiples ambientes:**

```
                    ┌─────────────┐
                    │   app.jar   │
                    └─────────────┘
                          │
        ┌─────────────────┼─────────────────┐
        ↓                 ↓                 ↓
    ┌───────┐         ┌───────┐         ┌───────┐
    │  DEV  │         │  TEST │         │  PROD │
    └───────┘         └───────┘         └───────┘
    config.dev        config.test       config.prod
```

### 3. **Seguridad**

**Secretos NO deben estar en el código:**

```java
// ❌ NUNCA HACER ESTO
private String apiKey = "sk_live_ABC123XYZ789";

// ✅ SIEMPRE HACER ESTO
@ConfigProperty(name = "api.key")
String apiKey;
```

**Razones:**
- El código va a git (público o privado)
- Los secretos deben estar en sistemas seguros (Vault, AWS Secrets)
- Rotación de secretos sin desplegar nueva versión

### 4. **Flexibilidad Operacional**

**Ajustar comportamiento sin desplegar:**

```bash
# Aumentar timeout sin recompilar
export APP_TIMEOUT=60000

# Cambiar URL de API
export API_URL=https://backup-api.com

# Activar debug mode
export LOG_LEVEL=DEBUG
```

### 5. **Cumplimiento de Estándares**

**12-Factor App (Factor III: Config)**

> "Store config in the environment"

Aplicaciones cloud-native DEBEN externalizar configuraciones.

---

## 🌐 MicroProfile Config: El Estándar

### ¿Qué es MicroProfile Config?

**MicroProfile Config** es una especificación de Jakarta EE que define cómo las aplicaciones Java deben manejar configuraciones.

### Principios Fundamentales

#### 1. **Múltiples Fuentes de Configuración**

```
ConfigSources (ordenadas por prioridad)
    ├── System Properties
    ├── Environment Variables
    ├── application.properties
    ├── application.yaml
    └── Programmatic Config
```

#### 2. **Conversión Automática de Tipos**

```java
@ConfigProperty(name = "app.port")
Integer port; // "8080" → 8080

@ConfigProperty(name = "app.active")
Boolean active; // "true" → true

@ConfigProperty(name = "app.timeout")
Duration timeout; // "5s" → Duration.ofSeconds(5)
```

#### 3. **Valores por Defecto**

```java
@ConfigProperty(name = "app.name", defaultValue = "MyApp")
String name;
```

#### 4. **Configuración Dinámica**

Las configuraciones pueden cambiar en runtime (según la implementación).

### SmallRye Config: La Implementación de Quarkus

Quarkus usa **SmallRye Config**, una implementación extendida de MicroProfile Config con características adicionales:

- **@ConfigMapping** para objetos complejos
- **Profiles** nativos (%dev, %test, %prod)
- **Validación** integrada
- **Configuración por expresiones** (${other.property})

---

## 📄 application.properties vs application.yaml

### application.properties

#### Sintaxis

```properties
# Comentario
clave=valor
clave.anidada.profunda=valor
```

#### Características

**Ventajas:**
- ✅ Formato simple y directo
- ✅ Ampliamente conocido
- ✅ Parsing rápido
- ✅ Estándar en Java desde siempre

**Desventajas:**
- ❌ No soporta estructuras complejas naturalmente
- ❌ Repetitivo para jerarquías profundas
- ❌ Listas requieren índices

#### Ejemplo de Listas

```properties
# Lista de URLs (incómodo)
app.urls[0]=https://api1.com
app.urls[1]=https://api2.com
app.urls[2]=https://api3.com

# O con comas
app.urls=https://api1.com,https://api2.com,https://api3.com
```

### application.yaml

#### Sintaxis

```yaml
# Comentario
clave: valor
clave:
  anidada:
    profunda: valor
```

#### Características

**Ventajas:**
- ✅ Estructura jerárquica natural
- ✅ Muy legible
- ✅ Listas nativas
- ✅ Ideal para configuraciones complejas

**Desventajas:**
- ❌ Sensible a indentación
- ❌ Parsing más pesado
- ❌ Menos familiar para algunos

#### Ejemplo de Listas

```yaml
app:
  urls:
    - https://api1.com
    - https://api2.com
    - https://api3.com
```

### Comparación Lado a Lado

**Configuración simple:**

```properties
# properties
app.name=TasaCorp
app.version=1.0.0
app.author=Arquitectura
```

```yaml
# yaml
app:
  name: TasaCorp
  version: 1.0.0
  author: Arquitectura
```

**Configuración compleja:**

```properties
# properties (repetitivo)
database.primary.host=localhost
database.primary.port=5432
database.primary.name=maindb
database.backup.host=backup.server.com
database.backup.port=5432
database.backup.name=maindb
```

```yaml
# yaml (más claro)
database:
  primary:
    host: localhost
    port: 5432
    name: maindb
  backup:
    host: backup.server.com
    port: 5432
    name: maindb
```

### ¿Cuándo Usar Cada Uno?

| Caso de Uso | Recomendación |
|-------------|---------------|
| Configuración simple (< 20 propiedades) | properties |
| Configuración compleja (estructuras anidadas) | yaml |
| Múltiples ambientes con muchas diferencias | yaml + profiles |
| Compatibilidad con herramientas legacy | properties |
| Configuración de microservicios | yaml |

### Pueden Coexistir

Quarkus permite usar AMBOS archivos simultáneamente:

```
src/main/resources/
├── application.properties  (prioridad 250)
└── application.yaml        (prioridad 255)
```

**YAML tiene mayor prioridad** - Si una propiedad está en ambos, YAML gana.

---

## 🔢 Tipos de Configuración

### 1. Propiedades Primitivas

```java
@ConfigProperty(name = "app.port")
Integer port; // int, Integer

@ConfigProperty(name = "app.active")
Boolean active; // boolean, Boolean

@ConfigProperty(name = "app.rate")
Double rate; // double, Double, Float, float

@ConfigProperty(name = "app.name")
String name; // String
```

### 2. Colecciones

```java
@ConfigProperty(name = "app.urls")
List<String> urls; // Lista

@ConfigProperty(name = "app.ports")
Set<Integer> ports; // Conjunto (sin duplicados)
```

**En properties:**
```properties
app.urls=url1,url2,url3
app.ports=8080,8081,8082
```

**En yaml:**
```yaml
app:
  urls:
    - url1
    - url2
    - url3
  ports:
    - 8080
    - 8081
    - 8082
```

### 3. Tipos Avanzados

```java
@ConfigProperty(name = "app.timeout")
Duration timeout; // java.time.Duration

@ConfigProperty(name = "app.memory")
MemorySize memory; // io.quarkus.runtime.configuration.MemorySize

@ConfigProperty(name = "app.url")
URL url; // java.net.URL

@ConfigProperty(name = "app.path")
Path path; // java.nio.file.Path
```

**Ejemplos:**
```properties
app.timeout=5s
app.memory=512M
app.url=https://api.com
app.path=/tmp/data
```

### 4. Optional

Para propiedades no obligatorias:

```java
@ConfigProperty(name = "app.optional.feature")
Optional<String> feature;

public void process() {
    feature.ifPresent(f -> {
        // Usa la feature si está configurada
    });
}
```

### 5. Configuración Compleja con @ConfigMapping

```java
@ConfigMapping(prefix = "database")
public interface DatabaseConfig {
    String host();
    int port();
    Credentials credentials();
    
    interface Credentials {
        String username();
        String password();
    }
}
```

**En yaml:**
```yaml
database:
  host: localhost
  port: 5432
  credentials:
    username: admin
    password: secret
```

---

## 💉 Inyección de Configuraciones

### @ConfigProperty: Para Valores Individuales

#### Sintaxis Básica

```java
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class MyService {
    
    @ConfigProperty(name = "app.name")
    String appName;
    
    @ConfigProperty(name = "app.version")
    String version;
}
```

#### Con Valor por Defecto

```java
@ConfigProperty(name = "app.timeout", defaultValue = "30")
Integer timeout;
```

#### Valores Opcionales

```java
@ConfigProperty(name = "app.feature.beta")
Optional<Boolean> betaFeature;
```

### @ConfigMapping: Para Objetos Complejos

#### Ventajas de @ConfigMapping

- ✅ **Type-safe**: Errores en tiempo de compilación
- ✅ **Inmutable**: Interfaces son naturalmente inmutables
- ✅ **Validación**: Errores al arranque si falta configuración
- ✅ **Legibilidad**: Estructura clara del dominio de configuración

#### Ejemplo Completo

```java
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import java.util.List;

@ConfigMapping(prefix = "tasacorp")
public interface TasaCorpConfig {

    // tasacorp.currency.*
    Currency currency();
    
    // tasacorp.transaction.*
    Transaction transaction();
    
    // tasacorp.commission.*
    Commission commission();

    interface Currency {
        // tasacorp.currency.base
        String base();
        
        // tasacorp.currency.supported
        List<String> supported();
    }

    interface Transaction {
        // tasacorp.transaction.limit
        Integer limit();
        
        // tasacorp.transaction.timeout (con default)
        @WithDefault("30s")
        Duration timeout();
    }

    interface Commission {
        // tasacorp.commission.rate
        Double rate();
        
        // tasacorp.commission.minimum
        @WithDefault("1.0")
        Double minimum();
    }
}
```

#### Uso en Servicios

```java
@ApplicationScoped
public class TasaService {

    @Inject
    TasaCorpConfig config;

    public void procesarTasa() {
        String base = config.currency().base();
        List<String> supported = config.currency().supported();
        Integer limit = config.transaction().limit();
        Double rate = config.commission().rate();
        
        // Lógica de negocio
    }
}
```

### Comparación: @ConfigProperty vs @ConfigMapping

| Aspecto | @ConfigProperty | @ConfigMapping |
|---------|----------------|----------------|
| **Uso** | Valores individuales | Grupos de configuración |
| **Type Safety** | Parcial | Total |
| **Inmutabilidad** | No garantizada | Garantizada (interfaces) |
| **Validación** | Runtime | Build time + Runtime |
| **Código** | Más verbose | Más limpio |
| **Recomendado para** | 1-3 propiedades | > 3 propiedades relacionadas |

---

## 🏆 Prioridades de Carga: Teoría Profunda

### El Sistema de ConfigSources

Quarkus (vía SmallRye Config) implementa un sistema de **ConfigSources** donde cada fuente tiene un **ordinal** (prioridad numérica).

```
┌─────────────────────────────────────┐
│  System Properties (ordinal: 400)   │ ← MÁXIMA PRIORIDAD
├─────────────────────────────────────┤
│  Env Variables (ordinal: 300)       │
├─────────────────────────────────────┤
│  .env file (ordinal: 260)           │
├─────────────────────────────────────┤
│  application.yaml (ordinal: 255)    │
├─────────────────────────────────────┤
│  application.properties (250)       │
├─────────────────────────────────────┤
│  Defaults (ordinal: -2147483648)    │ ← MÍNIMA PRIORIDAD
└─────────────────────────────────────┘
```

### ¿Por qué Este Orden?

#### 1. System Properties (Máxima Prioridad)

**Razón:** Permiten **sobrescribir cualquier cosa en runtime** sin modificar archivos.

**Caso de uso:**
```bash
# Emergencia en producción: cambiar URL de API
java -jar app.jar -Dapi.url=https://backup-api.com
```

#### 2. Variables de Entorno

**Razón:** Configuración específica del **entorno de ejecución** (Docker, Kubernetes, CI/CD).

**Caso de uso:**
```yaml
# docker-compose.yml
services:
  app:
    environment:
      - DATABASE_URL=postgres://prod-db:5432/main
```

#### 3. application.properties/yaml

**Razón:** Configuración **por defecto** empaquetada con la aplicación.

**Caso de uso:**
- Valores razonables para desarrollo
- Configuración base compartida

#### 4. Defaults (@WithDefault)

**Razón:** **Fallback final** si no se encuentra la propiedad en ningún lado.

**Caso de uso:**
```java
@ConfigProperty(name = "app.timeout", defaultValue = "30")
Integer timeout; // Si nadie configura, usa 30
```

### Algoritmo de Resolución

Cuando se pide una propiedad, Quarkus:

```
1. Buscar en System Properties
   ↓ (si no existe)
2. Buscar en Variables de Entorno
   ↓ (si no existe)
3. Buscar en .env
   ↓ (si no existe)
4. Buscar en application.yaml
   ↓ (si no existe)
5. Buscar en application.properties
   ↓ (si no existe)
6. Usar valor por defecto (@WithDefault)
   ↓ (si no existe)
7. ERROR: Property not found
```

### Ejemplo Práctico de Resolución

**Configuraciones definidas:**
```properties
# application.properties (ordinal 250)
app.timeout=10

# application.yaml (ordinal 255)
app:
  timeout: 20
```

```bash
# Variable de entorno (ordinal 300)
export APP_TIMEOUT=30
```

```bash
# System Property (ordinal 400)
java -jar app.jar -Dapp.timeout=40
```

**¿Qué valor se usa?**

```
1. System Property (-D): 40 ← GANA (ordinal 400)
2. ENV var: 30
3. YAML: 20
4. Properties: 10
```

**Resultado:** `app.timeout = 40`

### Sobrescritura Parcial

Las prioridades se aplican **propiedad por propiedad**, no archivo por archivo.

```properties
# application.properties
app.name=MyApp
app.version=1.0
app.timeout=10
```

```bash
# Solo sobrescribir timeout
export APP_TIMEOUT=60
```

**Resultado:**
- `app.name` = "MyApp" (de properties)
- `app.version` = "1.0" (de properties)
- `app.timeout` = 60 (de ENV, sobrescribe)

---

## 🌍 Variables de Entorno

### ¿Qué Son las Variables de Entorno?

Las variables de entorno son **pares clave-valor** definidos en el sistema operativo que las aplicaciones pueden leer.

### A Nivel de Sistema Operativo

#### Windows
```powershell
# Establecer variable
$env:APP_NAME="TasaCorp"

# Leer variable
echo $env:APP_NAME

# Variable permanente (requiere admin)
[Environment]::SetEnvironmentVariable("APP_NAME", "TasaCorp", "Machine")
```

#### Linux/macOS
```bash
# Establecer variable (sesión actual)
export APP_NAME="TasaCorp"

# Leer variable
echo $APP_NAME

# Variable permanente
# En ~/.bashrc o ~/.zshrc
export APP_NAME="TasaCorp"
```

### Mapeo en Quarkus

Quarkus convierte automáticamente nombres de propiedades a nombres de variables de entorno:

#### Reglas de Conversión

1. **Puntos (`.`) → Guiones bajos (`_`)**
2. **Todo en MAYÚSCULAS**
3. **Guiones (`-`) → Guiones bajos (`_`)**

**Ejemplos:**

| Propiedad | Variable de Entorno |
|-----------|---------------------|
| `app.name` | `APP_NAME` |
| `quarkus.http.port` | `QUARKUS_HTTP_PORT` |
| `tasacorp.commission.rate` | `TASACORP_COMMISSION_RATE` |
| `my-app.feature-flag` | `MY_APP_FEATURE_FLAG` |

### Ventajas de Variables de Entorno

#### 1. **Desacoplamiento Ambiente-Aplicación**

```bash
# Mismo binario, diferentes configs
# Desarrollo
export DATABASE_URL=localhost:5432

# Producción
export DATABASE_URL=prod-db.internal:5432
```

#### 2. **Seguridad**

```bash
# Secretos NO en archivos
export API_KEY=$(vault read secret/api-key)
```

#### 3. **Compatibilidad con Contenedores**

```yaml
# docker-compose.yml
services:
  app:
    image: myapp:latest
    environment:
      - DATABASE_URL=postgres://db:5432/main
      - API_KEY=${API_KEY}  # Inyecta desde host
```

#### 4. **Estándar Cloud-Native**

Kubernetes, AWS ECS, Azure Container Apps, etc., todos usan ENV vars como mecanismo principal de configuración.

---

## ⚙️ System Properties en la JVM

### ¿Qué Son los System Properties?

Son propiedades específicas de la **JVM** (Java Virtual Machine) que se pueden establecer al iniciar la aplicación.

### Sintaxis

```bash
java -Dpropiedad=valor -jar app.jar
```

**Múltiples propiedades:**
```bash
java -Dapp.name=TasaCorp \
     -Dapp.port=8080 \
     -Dlog.level=DEBUG \
     -jar app.jar
```

### Casos de Uso

#### 1. **Configuración de Emergencia**

```bash
# Producción caída: cambiar a servidor de respaldo
java -Dapi.url=https://backup.api.com -jar app.jar
```

#### 2. **Debug en Producción**

```bash
# Activar logs detallados temporalmente
java -Dquarkus.log.level=DEBUG -jar app.jar
```

#### 3. **Feature Flags**

```bash
# Activar feature experimental
java -Dfeatures.new-algorithm=true -jar app.jar
```

#### 4. **Testing**

```bash
# Cambiar configuración para tests específicos
mvn test -Dtest.database.url=h2:mem:test
```

### Acceso Programático

```java
// Leer System Property directamente
String value = System.getProperty("app.name");

// Con default
String value = System.getProperty("app.name", "DefaultApp");

// Establecer en runtime (poco común)
System.setProperty("app.name", "NewName");
```

### Ventajas

- ✅ **Máxima prioridad** - Sobrescribe todo
- ✅ **Sin modificar archivos** - Ideal para emergencias
- ✅ **Específico de ejecución** - No afecta otras instancias

### Desventajas

- ❌ **Temporal** - Se pierde al reiniciar
- ❌ **No persistente** - No se guarda en ningún lado
- ❌ **Difícil de trackear** - No queda registro de cambios

---

## 🏛️ Patrones de Configuración

### 1. The Twelve-Factor App

**Factor III: Config**

> "Store config in the environment"

Principios:
- Configuración completamente separada del código
- Configuración en variables de entorno
- No distinción entre ambientes en código

**Implementación en Quarkus:**
```java
// ✅ Cumple 12-Factor
@ConfigProperty(name = "database.url")
String databaseUrl; // Lee de ENV var DATABASE_URL

// ❌ NO cumple 12-Factor
private String databaseUrl = "localhost:5432"; // Hardcoded
```

### 2. Configuration as Code

**Filosofía:** La configuración es parte del código (versionada, revisada, testeada).

```
git/
├── src/
│   └── main/
│       ├── java/
│       └── resources/
│           ├── application.properties      ← Versionado
│           ├── application-dev.properties  ← Versionado
│           └── application-prod.properties ← Versionado
```

**Ventajas:**
- Historia de cambios (git log)
- Code reviews de configuración
- Rollback fácil

### 3. Convention over Configuration

**Filosofía:** Valores por defecto sensatos, mínima configuración necesaria.

```java
// Solo configurar lo que difiere del default
@ConfigProperty(name = "server.port", defaultValue = "8080")
Integer port; // 8080 es razonable, no necesita config

@ConfigProperty(name = "api.key")
String apiKey; // Esto SÍ debe configurarse (no hay default sensato)
```

### 4. Immutable Configuration

**Filosofía:** Configuración NO cambia durante la ejecución.

```java
// ✅ Inmutable (cargada al inicio)
@Inject
TasaCorpConfig config;

public void process() {
    String name = config.app().name(); // Siempre el mismo valor
}

// ❌ Mutable (puede cambiar en runtime)
public void process() {
    String name = System.getProperty("app.name"); // Podría cambiar
}
```

**Ventajas:**
- Comportamiento predecible
- Thread-safe por naturaleza
- Más fácil de testear

---

## ✅ Mejores Prácticas

### 1. Nombres de Propiedades

**Convenciones:**
```properties
# ✅ BIEN: kebab-case o dot.notation
app.name=TasaCorp
app.max-connections=100

# ❌ MAL: camelCase
appName=TasaCorp
```

**Jerarquía Clara:**
```properties
# ✅ BIEN: Agrupación lógica
database.primary.url=...
database.primary.username=...
database.backup.url=...
database.backup.username=...

# ❌ MAL: Sin estructura
db1url=...
db1user=...
db2url=...
```

### 2. Valores por Defecto

**Regla:** Proporcionar defaults razonables cuando sea posible.

```java
// ✅ BIEN: Default sensato
@ConfigProperty(name = "app.timeout", defaultValue = "30")
Integer timeout;

// ✅ BIEN: Sin default para secretos
@ConfigProperty(name = "api.key")
String apiKey; // ERROR si no está configurado

// ❌ MAL: Default para secretos
@ConfigProperty(name = "api.key", defaultValue = "test-key")
String apiKey; // Peligroso en prod
```

### 3. Documentación

**Incluir comentarios en archivos de configuración:**

```properties
# ========================================
# Database Configuration
# ========================================

# Primary database connection URL
# Format: jdbc:postgresql://host:port/database
# Example: jdbc:postgresql://localhost:5432/mydb
database.primary.url=jdbc:postgresql://localhost:5432/tasacorp

# Connection pool size
# Range: 1-100
# Default: 10
# Recommendation: (2 * cores) + number of disks
database.pool.size=10
```

### 4. Validación

**Validar configuración al arranque:**

```java
@ConfigMapping(prefix = "app")
public interface AppConfig {
    
    @Min(1)
    @Max(65535)
    Integer port(); // Valida que puerto esté en rango válido
    
    @Email
    String contactEmail(); // Valida formato email
    
    @Pattern(regexp = "^https?://.*")
    String apiUrl(); // Valida que sea URL HTTP(S)
}
```

### 5. No Exponer Secretos

```java
// ❌ MAL: Devolver API key en endpoint
@GET
@Path("/config")
public Map<String, String> getConfig() {
    return Map.of(
        "api.key", apiKey // NUNCA exponer secretos
    );
}

// ✅ BIEN: Ocultar secretos
@GET
@Path("/config")
public Map<String, String> getConfig() {
    return Map.of(
        "api.key.configured", apiKey != null ? "***" : "not set"
    );
}
```

### 6. Perfiles para Ambientes

```properties
# Configuración por ambiente (veremos en PARTE 2)
%dev.database.url=localhost:5432
%test.database.url=testdb:5432
%prod.database.url=prod-db.internal:5432
```

---

## ❌ Anti-Patrones

### 1. Hardcoding

```java
// ❌ ANTI-PATRÓN
public class Service {
    private static final String API_URL = "https://api.prod.com";
    private static final double TAX_RATE = 0.19;
}
```

**Problema:** Imposible cambiar sin recompilar.

### 2. Configuration in Code

```java
// ❌ ANTI-PATRÓN
if (environment.equals("prod")) {
    apiUrl = "https://api.prod.com";
} else if (environment.equals("test")) {
    apiUrl = "https://api.test.com";
} else {
    apiUrl = "http://localhost:8080";
}
```

**Problema:** Lógica de negocio mezclada con configuración.

### 3. Secretos en Git

```properties
# ❌ ANTI-PATRÓN (EN GIT)
database.password=super_secret_123
api.key=sk_live_ABC123XYZ789
```

**Problema:** Secretos expuestos en historial de git.

**Solución:**
```properties
# ✅ CORRECTO
database.password=${DB_PASSWORD}
api.key=${API_KEY}
```

### 4. Configuración Duplicada

```properties
# ❌ ANTI-PATRÓN
app.database.url=localhost:5432
service.db.connection=localhost:5432
worker.database.server=localhost:5432
```

**Problema:** Inconsistencia, difícil mantenimiento.

**Solución:**
```properties
# ✅ CORRECTO
database.url=localhost:5432

# Referenciar
app.database.url=${database.url}
service.db.connection=${database.url}
```

### 5. Magic Numbers en Configuración

```properties
# ❌ ANTI-PATRÓN
app.pool.size=42
app.timeout=13579
```

**Problema:** No se entiende el origen del número.

**Solución:**
```properties
# ✅ CORRECTO (con comentario explicativo)
# Pool size calculated as: (2 * CPU cores) + spindle count
# Server: 20 cores + 2 SSDs = 42
app.pool.size=42

# Timeout based on SLA: 3.5 hours in milliseconds
app.timeout=12600000
```

---

## 🏢 Casos de Uso Reales

### Caso 1: Startup Fintech

**Problema:** Necesitan desplegar la misma app en múltiples países con diferentes regulaciones.

**Solución:**
```properties
# Base
app.name=FinPay

# Por país (usando perfiles)
%ar.currency.default=ARS
%ar.tax.rate=0.21
%ar.regulator.url=https://bcra.gob.ar

%cl.currency.default=CLP
%cl.tax.rate=0.19
%cl.regulator.url=https://sbif.cl

%mx.currency.default=MXN
%mx.tax.rate=0.16
%mx.regulator.url=https://cnbv.gob.mx
```

### Caso 2: E-commerce Escalable

**Problema:** Black Friday - necesitan aumentar capacidad rápidamente.

**Solución:**
```bash
# Día normal
docker run -e CACHE_SIZE=1GB \
           -e MAX_CONNECTIONS=100 \
           myapp:latest

# Black Friday (aumentar capacidad)
docker run -e CACHE_SIZE=10GB \
           -e MAX_CONNECTIONS=1000 \
           -e RATE_LIMIT=10000 \
           myapp:latest
```

### Caso 3: Aplicación Bancaria

**Problema:** Diferentes configuraciones de seguridad entre sucursales.

**Solución:**
```properties
# Configuración base
security.session.timeout=30m
security.mfa.required=true

# Sucursal corporativa (más estricta)
%corporate.security.session.timeout=15m
%corporate.security.mfa.required=true
%corporate.security.biometric.required=true

# Sucursal retail (más flexible)
%retail.security.session.timeout=60m
%retail.security.mfa.required=false
%retail.security.biometric.required=false
```

### Caso 4: SaaS Multi-tenant

**Problema:** Cada cliente necesita configuraciones diferentes.

**Solución:**
```java
@ConfigMapping(prefix = "tenant")
public interface TenantConfig {
    Map<String, TenantSettings> settings();
    
    interface TenantSettings {
        Integer maxUsers();
        Boolean premiumFeatures();
        String customDomain();
    }
}
```

```yaml
tenant:
  settings:
    acme-corp:
      max-users: 1000
      premium-features: true
      custom-domain: acme.myapp.com
    startup-inc:
      max-users: 50
      premium-features: false
      custom-domain: startup.myapp.com
```

---

## 📖 Resumen de Conceptos Clave

### Externalización
- Separar configuración del código
- Mismo binario, múltiples ambientes
- Facilita cambios sin recompilar

### MicroProfile Config
- Estándar Jakarta EE
- Múltiples fuentes de configuración
- Conversión automática de tipos

### Archivos de Configuración
- **properties:** Simple, directo
- **yaml:** Jerárquico, estructurado
- Ambos pueden coexistir

### Inyección
- **@ConfigProperty:** Valores individuales
- **@ConfigMapping:** Objetos complejos
- Type-safe y validable

### Prioridades
1. System Properties (máxima)
2. Variables de Entorno
3. Archivos de configuración
4. Defaults (mínima)

### Mejores Prácticas
- Nombres consistentes
- Defaults razonables
- Validación temprana
- No exponer secretos
- Documentar configuración

---

## 🎓 Preguntas para Reflexión

1. ¿Por qué es importante externalizar configuraciones en aplicaciones cloud-native?
2. ¿En qué casos usarías properties vs yaml?
3. ¿Cuándo es apropiado usar System Properties?
4. ¿Cómo manejarías secretos sensibles en configuración?
5. ¿Qué ventajas tiene @ConfigMapping sobre @ConfigProperty?

---

## 📚 Referencias

- [MicroProfile Config Specification](https://github.com/eclipse/microprofile-config)
- [SmallRye Config Documentation](https://smallrye.io/smallrye-config)
- [Quarkus Configuration Guide](https://quarkus.io/guides/config)
- [The Twelve-Factor App](https://12factor.net)
- [Jakarta EE Configuration](https://jakarta.ee/specifications/config/)

---

**Has completado la teoría de la Parte 1. Ahora tienes el conocimiento profundo necesario para entender la externalización de configuraciones en Quarkus.** 🎉