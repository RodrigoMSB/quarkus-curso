# 🏦 TasaCorp API - PARTE 1: Externalización de Configuraciones

## Capítulo 7: Configuración y Perfiles en Quarkus (30 minutos)

---

## 📋 Índice

1. [Objetivo de Aprendizaje](#objetivo-de-aprendizaje)
2. [Requisitos Previos](#requisitos-previos)
3. [Creación del Proyecto](#creación-del-proyecto)
4. [Estructura del Proyecto](#estructura-del-proyecto)
5. [Configuración con application.properties](#configuración-con-applicationproperties)
6. [Configuración con application.yaml](#configuración-con-applicationyaml)
7. [Inyección de Configuraciones](#inyección-de-configuraciones)
8. [Prioridades de Carga](#prioridades-de-carga)
9. [Pruebas Paso a Paso](#pruebas-paso-a-paso)
10. [Verificación](#verificación)

---

## 🎯 Objetivo de Aprendizaje

Al finalizar esta parte, podrás:

✅ Externalizar configuraciones fuera del código fuente  
✅ Usar `application.properties` y `application.yaml`  
✅ Inyectar configuraciones con `@ConfigProperty` y `@ConfigMapping`  
✅ Entender las **prioridades de carga** en Quarkus  
✅ Sobrescribir configuraciones con variables de entorno  
✅ Usar System Properties para configuración dinámica  

---

## 📦 Requisitos Previos

### Windows
```powershell
# Verificar Java (17 o superior)
java -version

# Verificar Maven
mvn -version

# Verificar Quarkus CLI (opcional)
quarkus version
```

### macOS/Linux
```bash
# Verificar Java (17 o superior)
java -version

# Verificar Maven
mvn -version

# Verificar Quarkus CLI (opcional)
quarkus version
```

---

## 🚀 Creación del Proyecto

### Windows
```powershell
# Crear proyecto
mvn io.quarkus.platform:quarkus-maven-plugin:3.17.5:create `
    -DprojectGroupId=pe.banco `
    -DprojectArtifactId=tasacorp-api `
    -Dextensions="resteasy-reactive-jackson,config-yaml"

# Entrar al proyecto
cd tasacorp-api
```

### macOS/Linux
```bash
# Crear proyecto
mvn io.quarkus.platform:quarkus-maven-plugin:3.17.5:create \
    -DprojectGroupId=pe.banco \
    -DprojectArtifactId=tasacorp-api \
    -Dextensions="resteasy-reactive-jackson,config-yaml"

# Entrar al proyecto
cd tasacorp-api
```

---

## 📁 Estructura del Proyecto

### Windows
```powershell
# Crear estructura de carpetas
mkdir src\main\java\pe\banco\tasacorp\config
mkdir src\main\java\pe\banco\tasacorp\model
mkdir src\main\java\pe\banco\tasacorp\service
mkdir src\main\java\pe\banco\tasacorp\resource
```

### macOS/Linux
```bash
# Crear estructura de carpetas
mkdir -p src/main/java/pe/banco/tasacorp/config
mkdir -p src/main/java/pe/banco/tasacorp/model
mkdir -p src/main/java/pe/banco/tasacorp/service
mkdir -p src/main/java/pe/banco/tasacorp/resource
```

**Estructura resultante:**
```
src/main/java/pe/banco/tasacorp/
├── config/          → Clases de configuración
├── model/           → DTOs y modelos
├── service/         → Lógica de negocio
└── resource/        → Endpoints REST
```

---

## ⚙️ Configuración con application.properties

Quarkus utiliza `application.properties` como archivo principal de configuración.

**Ubicación:** `src/main/resources/application.properties`

**Contenido básico:**

```properties
# ========================================
# TasaCorp API - Configuración Base
# ========================================

# Información de la aplicación
app.name=TasaCorp API
app.version=1.0.0
app.banco=Banco TasaCorp Perú

# Configuración de tasas de cambio
tasacorp.currency.base=PEN
tasacorp.currency.supported=USD,EUR,MXN

# Límite transaccional
tasacorp.transaction.limit=1000

# Comisión por operación (%)
tasacorp.commission.rate=2.5

# Provider de tasas
tasacorp.provider.name=mock
tasacorp.provider.url=http://localhost:8080/mock

# Configuración del servidor
quarkus.http.port=8080

# Logging
quarkus.log.level=INFO
```

### 📌 Conceptos Clave

**1. Propiedades Simples:**
```properties
app.name=TasaCorp API
```

**2. Propiedades Jerárquicas:**
```properties
tasacorp.currency.base=PEN
tasacorp.currency.supported=USD,EUR,MXN
```

**3. Propiedades de Quarkus:**
```properties
quarkus.http.port=8080
quarkus.log.level=INFO
```

---

## 📝 Configuración con application.yaml

YAML es una alternativa más legible para configuraciones complejas.

**Ubicación:** `src/main/resources/application.yaml`

**Contenido:**

```yaml
# ========================================
# TasaCorp API - Configuración YAML
# ========================================

tasacorp:
  exchange:
    rates:
      usd: 3.75
      eur: 4.10
      mxn: 0.22
    
  metadata:
    created-by: "Arquitectura TasaCorp"
    environment: "multi-profile"
    supported-profiles:
      - dev
      - test  
      - prod

  features:
    cache-enabled: false
    rate-refresh-minutes: 60
    audit-enabled: true
```

### ⚖️ Properties vs YAML

| Característica | properties | yaml |
|---------------|-----------|------|
| **Sintaxis** | Plana | Jerárquica |
| **Legibilidad** | Básica | Excelente |
| **Listas** | Separadas por coma | Nativas |
| **Comentarios** | `#` | `#` |
| **Recomendado para** | Configs simples | Configs complejas |

---

## 💉 Inyección de Configuraciones

Quarkus ofrece dos formas principales de inyectar configuraciones.

### 1️⃣ @ConfigProperty (Propiedades Individuales)

**Uso simple en clases:**

```java
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TasaService {

    @ConfigProperty(name = "app.name")
    String appName;

    @ConfigProperty(name = "tasacorp.commission.rate")
    Double commissionRate;

    @ConfigProperty(name = "quarkus.profile")
    String activeProfile;
}
```

### 2️⃣ @ConfigMapping (Mapeo de Objetos)

**Para configuraciones complejas:**

```java
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import java.util.List;

@ConfigMapping(prefix = "tasacorp")
public interface TasaCorpConfig {

    Currency currency();
    Transaction transaction();
    Commission commission();

    interface Currency {
        String base();
        List<String> supported();
    }

    interface Transaction {
        Integer limit();
    }

    interface Commission {
        Double rate();
    }
}
```

**Uso en servicios:**

```java
@ApplicationScoped
public class TasaService {

    @Inject
    TasaCorpConfig config;

    public void procesarTasa() {
        String base = config.currency().base();
        Double rate = config.commission().rate();
    }
}
```

---

## 📊 Prioridades de Carga

Quarkus sigue una jerarquía de prioridades para cargar configuraciones:

```
🔼 MAYOR PRIORIDAD
1. System Properties (-D)
2. Variables de entorno (ENV)  
3. .env file (si existe)
4. application.properties (perfil específico)
5. application.properties (base)
6. application.yaml
7. Valores por defecto (@WithDefault)
🔽 MENOR PRIORIDAD
```

### 📌 Regla de Oro

**"El que está más arriba gana"** - Si una propiedad se define en múltiples lugares, prevalece la de mayor prioridad.

---

## 🧪 Pruebas Paso a Paso

### Prueba 1: Valor Base desde Properties

**1. Arrancar en modo DEV:**

#### Windows
```powershell
.\mvnw.cmd quarkus:dev
```

#### macOS/Linux
```bash
./mvnw quarkus:dev
```

**2. En otra terminal, consultar la configuración:**

#### Windows
```powershell
curl http://localhost:8080/api/tasas/config
```

#### macOS/Linux
```bash
curl http://localhost:8080/api/tasas/config | jq
```

**Resultado esperado:**
```json
{
  "comision_porcentaje": 2.5,
  "moneda_base": "PEN",
  "limite_transaccional": 1000
}
```

> 💡 **Estos valores vienen de:** `application.properties`

---

### Prueba 2: Sobrescribir con Variable de Entorno

Las variables de entorno tienen **MAYOR prioridad** que los archivos de configuración.

**1. Para el servidor** (Ctrl+C)

**2. Arrancar con variable de entorno:**

#### Windows
```powershell
$env:TASACORP_COMMISSION_RATE="9.99"
.\mvnw.cmd quarkus:dev
```

#### macOS/Linux
```bash
TASACORP_COMMISSION_RATE=9.99 ./mvnw quarkus:dev
```

**3. Consultar de nuevo:**

#### Windows
```powershell
curl http://localhost:8080/api/tasas/config
```

#### macOS/Linux
```bash
curl http://localhost:8080/api/tasas/config | jq
```

**Resultado esperado:**
```json
{
  "comision_porcentaje": 9.99,
  "moneda_base": "PEN",
  "limite_transaccional": 1000
}
```

> ✅ **La comisión cambió de 2.5% → 9.99%**  
> 💡 **ENV > properties**

### 📌 Mapeo de Propiedades a Variables de Entorno

Quarkus convierte automáticamente las propiedades:

| Propiedad | Variable de Entorno |
|-----------|---------------------|
| `tasacorp.commission.rate` | `TASACORP_COMMISSION_RATE` |
| `app.name` | `APP_NAME` |
| `quarkus.http.port` | `QUARKUS_HTTP_PORT` |

**Reglas:**
- Puntos (`.`) → Guiones bajos (`_`)
- Todo en MAYÚSCULAS

---

### Prueba 3: System Properties (Máxima Prioridad)

Los System Properties (`-D`) tienen la **MÁXIMA prioridad**.

**1. Para el servidor** (Ctrl+C)

**2. Arrancar con System Property Y variable de entorno:**

#### Windows
```powershell
$env:TASACORP_COMMISSION_RATE="9.99"
.\mvnw.cmd quarkus:dev -Dtasacorp.commission.rate=15.0
```

#### macOS/Linux
```bash
TASACORP_COMMISSION_RATE=9.99 ./mvnw quarkus:dev -Dtasacorp.commission.rate=15.0
```

**3. Consultar:**

#### Windows
```powershell
curl http://localhost:8080/api/tasas/config
```

#### macOS/Linux
```bash
curl http://localhost:8080/api/tasas/config | jq
```

**Resultado esperado:**
```json
{
  "comision_porcentaje": 15.0,
  "moneda_base": "PEN",
  "limite_transaccional": 1000
}
```

> ✅ **La comisión ahora es 15.0%**  
> 💡 **System Property (-D) > ENV > properties**

### 🎯 Demostración de Prioridades

| Fuente | Valor | ¿Ganó? |
|--------|-------|--------|
| application.properties | 2.5 | ❌ |
| Variable ENV | 9.99 | ❌ |
| **System Property (-D)** | **15.0** | **✅ GANADOR** |

---

## 🧪 Verificación

### Prueba Completa de Conversión

**Verificar que la comisión se aplica correctamente:**

#### Windows
```powershell
curl "http://localhost:8080/api/tasas/convertir/USD?monto=1000"
```

#### macOS/Linux
```bash
curl "http://localhost:8080/api/tasas/convertir/USD?monto=1000" | jq
```

**Con comisión de 15.0%:**
```json
{
  "monto_origen": 1000.0,
  "monto_convertido": 3750.0,
  "comision": 562.5,
  "monto_total": 4312.5,
  "tasa_aplicada": 3.75
}
```

**Cálculo:**
- Convertido: 1000 × 3.75 = 3750 USD
- Comisión: 3750 × 15% = 562.5 USD
- Total: 3750 + 562.5 = 4312.5 USD

---

## 📚 Conceptos Clave Aprendidos

### ✅ Externalización de Configuraciones

**Antes (hardcoded):**
```java
public class Service {
    private double commission = 2.5; // ❌ Valor fijo en código
}
```

**Después (externalizado):**
```java
@ApplicationScoped
public class Service {
    @ConfigProperty(name = "tasacorp.commission.rate")
    Double commission; // ✅ Configurable externamente
}
```

### ✅ Prioridades de Carga

```
System Properties (-D)    ← MÁXIMA PRIORIDAD
      ↓
Variables de Entorno      
      ↓
application.properties
      ↓
application.yaml
      ↓
Valores por defecto       ← MÍNIMA PRIORIDAD
```

### ✅ Casos de Uso

| Escenario | Mecanismo | Ejemplo |
|-----------|-----------|---------|
| Desarrollo local | properties | `tasacorp.commission.rate=0.0` |
| CI/CD | ENV vars | `TASACORP_COMMISSION_RATE=2.5` |
| Producción crítica | System Props | `-Dtasacorp.commission.rate=1.0` |
| Configuración por defecto | @WithDefault | `@WithDefault("2.5")` |

---

## 🎓 Ejercicios Adicionales

### Ejercicio 1: Agregar Nueva Propiedad

**Objetivo:** Agregar un límite de conversión diaria.

1. Agregar en `application.properties`:
```properties
tasacorp.daily.limit=100000
```

2. Inyectar en el servicio:
```java
@ConfigProperty(name = "tasacorp.daily.limit")
Integer dailyLimit;
```

3. Probar sobrescribiendo con ENV:

#### Windows
```powershell
$env:TASACORP_DAILY_LIMIT="500000"
.\mvnw.cmd quarkus:dev
```

#### macOS/Linux
```bash
TASACORP_DAILY_LIMIT=500000 ./mvnw quarkus:dev
```

### Ejercicio 2: Configuración Compleja con YAML

**Objetivo:** Agregar configuración de múltiples proveedores.

En `application.yaml`:
```yaml
tasacorp:
  providers:
    primary:
      name: PremiumAPI
      url: https://premium.api.com
      timeout: 5000
    backup:
      name: BackupAPI
      url: https://backup.api.com
      timeout: 10000
```

Crear interface de configuración:
```java
@ConfigMapping(prefix = "tasacorp.providers")
public interface ProvidersConfig {
    ProviderInfo primary();
    ProviderInfo backup();

    interface ProviderInfo {
        String name();
        String url();
        Integer timeout();
    }
}
```

---

## 🚨 Problemas Comunes

### ❌ Error: "Property not found"

**Síntoma:**
```
SRCFG00014: The config property tasacorp.xxx is required 
but it could not be found in any config source
```

**Solución:**
1. Verificar que la propiedad existe en `application.properties`
2. Verificar la ortografía exacta
3. Asegurarse de que el archivo está en `src/main/resources/`

### ❌ Error: "Cannot convert value"

**Síntoma:**
```
SRCFG00040: Failed to convert "abc" to Integer
```

**Solución:**
- Verificar que el valor sea del tipo correcto
- Para números: `tasacorp.limit=1000` (sin comillas)
- Para strings: `app.name=TasaCorp API`

### ❌ Variables de entorno no funcionan

**En Windows PowerShell:**
```powershell
# ❌ Incorrecto
set TASACORP_RATE=5.0

# ✅ Correcto
$env:TASACORP_RATE="5.0"
```

**En macOS/Linux:**
```bash
# ❌ Incorrecto (en terminal separada)
export TASACORP_RATE=5.0
./mvnw quarkus:dev

# ✅ Correcto (en la misma línea)
TASACORP_RATE=5.0 ./mvnw quarkus:dev
```

---

## ✅ Checklist de Verificación

Antes de continuar a la PARTE 2, asegúrate de:

- [ ] El proyecto compila sin errores
- [ ] La aplicación arranca en modo dev
- [ ] Puedes consultar `/api/tasas/config`
- [ ] Entiendes la diferencia entre properties y yaml
- [ ] Puedes sobrescribir valores con ENV vars
- [ ] Entiendes las prioridades de carga
- [ ] Sabes usar @ConfigProperty y @ConfigMapping
- [ ] Probaste las 3 pruebas de prioridades

---

## 📖 Recursos Adicionales

- [Quarkus Configuration Guide](https://quarkus.io/guides/config)
- [MicroProfile Config Specification](https://github.com/eclipse/microprofile-config)
- [SmallRye Config Documentation](https://smallrye.io/smallrye-config)

---

## ➡️ Siguiente Paso

Continúa con: **[README-PARTE2.md](README-PARTE2.md)** - Perfiles de Entorno y Configuración Sensible
