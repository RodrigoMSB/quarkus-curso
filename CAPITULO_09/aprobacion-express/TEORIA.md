# 📚 TEORIA.md - Conceptos Profundos

# 📚 TEORÍA: Conceptos Profundos del Sistema

## 📋 Tabla de Contenidos

1. [Arquitectura de Quarkus](#arquitectura-de-quarkus)
2. [JVM vs Native: Análisis Profundo](#jvm-vs-native-análisis-profundo)
3. [GraalVM y Compilación Native](#graalvm-y-compilación-native)
4. [Dockerfiles Multi-Stage](#dockerfiles-multi-stage)
5. [Dev Services vs Docker-Compose](#dev-services-vs-docker-compose)
6. [Hibernate ORM y Panache](#hibernate-orm-y-panache)
7. [Métricas y Observabilidad](#métricas-y-observabilidad)
8. [REST y Serialización JSON](#rest-y-serialización-json)
9. [Transacciones y Gestión de Base de Datos](#transacciones-y-gestión-de-base-de-datos)
10. [Perfiles de Configuración](#perfiles-de-configuración)
11. [Conceptos Avanzados](#conceptos-avanzados)

---

## 🏗️ Arquitectura de Quarkus

### ¿Qué es Quarkus?

**Quarkus** es un framework Java optimizado para contenedores y cloud, diseñado desde cero para aprovechar GraalVM.

**Filosofía:** "Supersonic Subatomic Java"
- **Supersonic:** Arranque ultra-rápido
- **Subatomic:** Huella de memoria mínima

### Arquitectura en Capas

```
┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE PRESENTACIÓN                     │
│  ┌────────────┐  ┌────────────┐  ┌─────────────────────┐  │
│  │ REST API   │  │ Health     │  │ Métricas Prometheus │  │
│  │ (JAX-RS)   │  │ Checks     │  │ (Micrometer)        │  │
│  └────────────┘  └────────────┘  └─────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│                    CAPA DE NEGOCIO                          │
│  ┌────────────────────────────────────────────────────┐    │
│  │  ScoreCalculator + PreAprobacionService            │    │
│  │  - Cálculo de scoring crediticio                   │    │
│  │  - Validación de reglas de negocio                 │    │
│  │  - Lógica de pre-aprobación                        │    │
│  └────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────┤
│                    CAPA DE PERSISTENCIA                     │
│  ┌────────────────┐  ┌──────────────────────────────┐     │
│  │ Panache        │→ │ Hibernate ORM                 │     │
│  │ Repositories   │  │ - Entity Management           │     │
│  └────────────────┘  │ - Query Generation            │     │
│                      │ - Transaction Management       │     │
│                      └──────────────────────────────┘     │
├─────────────────────────────────────────────────────────────┤
│                    CAPA DE DATOS                            │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  PostgreSQL 16                                      │   │
│  │  - Tabla: solicitud_credito                         │   │
│  │  - Índices: estado, fecha_solicitud                 │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Extensiones Clave del Proyecto

| Extensión | Propósito | Importancia |
|-----------|-----------|-------------|
| `quarkus-hibernate-orm-panache` | ORM simplificado | ⭐⭐⭐⭐⭐ |
| `quarkus-jdbc-postgresql` | Driver PostgreSQL | ⭐⭐⭐⭐⭐ |
| `quarkus-rest-jackson` | REST + JSON | ⭐⭐⭐⭐⭐ |
| `quarkus-hibernate-validator` | Validaciones | ⭐⭐⭐⭐ |
| `quarkus-smallrye-health` | Health checks | ⭐⭐⭐⭐ |
| `quarkus-micrometer-registry-prometheus` | Métricas | ⭐⭐⭐⭐ |
| `quarkus-narayana-jta` | Transacciones | ⭐⭐⭐⭐⭐ |

---

## ⚡ JVM vs Native: Análisis Profundo

### Arquitectura JVM (Modo Tradicional)

```
┌─────────────────────────────────────────────────────────┐
│  APLICACIÓN JAVA (JAR)                                  │
├─────────────────────────────────────────────────────────┤
│  JAVA VIRTUAL MACHINE (JVM)                             │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ Class       │  │ JIT Compiler │  │ Garbage      │  │
│  │ Loader      │  │ (HotSpot)    │  │ Collector    │  │
│  └─────────────┘  └──────────────┘  └──────────────┘  │
│  ┌─────────────────────────────────────────────────┐   │
│  │ Heap Memory (Objects, Arrays)                   │   │
│  └─────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────┐   │
│  │ Metaspace (Classes, Methods)                    │   │
│  └─────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────┤
│  SISTEMA OPERATIVO (macOS, Linux, Windows)             │
└─────────────────────────────────────────────────────────┘

Características:
✅ Portabilidad total (Write Once, Run Anywhere)
✅ JIT optimiza código en runtime (calentamiento)
✅ Garbage Collector automático
❌ Arranque lento (inicializa JVM + carga clases)
❌ Mayor uso de memoria
❌ Consumo de CPU inicial alto
```

### Arquitectura Native (GraalVM)

```
┌─────────────────────────────────────────────────────────┐
│  EJECUTABLE NATIVO (Binario Específico del SO)          │
│                                                          │
│  ┌────────────────────────────────────────────────┐    │
│  │ Código Nativo Precompilado (AOT)               │    │
│  │ - Todo el bytecode → código máquina             │    │
│  │ - Optimizaciones aplicadas                      │    │
│  │ - Solo lo necesario incluido                    │    │
│  └────────────────────────────────────────────────┘    │
│  ┌────────────────────────────────────────────────┐    │
│  │ SubstrateVM (GC Minimalista)                    │    │
│  └────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────┤
│  SISTEMA OPERATIVO (específico: Linux x86_64)           │
└─────────────────────────────────────────────────────────┘

Características:
✅ Arranque instantáneo (código ya compilado)
✅ Memoria mínima (solo lo usado)
✅ Rendimiento predecible
❌ Compilación lenta (análisis profundo)
❌ Menos portable (binario por SO/arquitectura)
❌ Debugging limitado
```

### Comparación Detallada (Resultados Reales del Benchmark)

Los siguientes datos provienen del benchmark ejecutado con `./benchmark.sh 500`:

| Aspecto | JVM (Docker) | Native (Docker) | Ganador |
|---------|--------------|-----------------|---------|
| **BUILD** |
| Tiempo de compilación | ~4 segundos | ~199 segundos (3m 19s) | 🏆 JVM (50x más rápido) |
| Tamaño imagen Docker | 705 MB | 430 MB | 🏆 Native (39% menor) |
| **ARRANQUE** |
| Tiempo de inicio | 1811 ms | 127 ms | 🏆 Native (14x más rápido) |
| ¿Por qué? | Inicializa JVM, carga clases | Todo precompilado | - |
| **MEMORIA** |
| RSS en ejecución | 238 MB | 17 MB | 🏆 Native (92% menos) |
| **RENDIMIENTO** |
| Throughput (500 req) | 50 req/s | 71 req/s | 🏆 Native (42% mejor) |

### Análisis del Benchmark

```
+------------------------------------------------------------------------------+
|                        RESULTADOS DEL BENCHMARK                              |
|                        (500 requests)                                        |
+------------------------------------------------------------------------------+
| METRICA                     | JVM (Docker)       | NATIVE (Docker)          |
+------------------------------------------------------------------------------+
| Tiempo de build             |                 4s |                     199s |
| Tiempo de arranque          |            1811 ms |                   127 ms |
| Uso de memoria              |             238 MB |                    17 MB |
| Throughput                  |           50 req/s |                 71 req/s |
| Tamano imagen               |             705 MB |                   430 MB |
+------------------------------------------------------------------------------+

ANALISIS:
1. BUILD: Native 50x mas lento (pero solo una vez en CI/CD)
2. ARRANQUE: Native 14x MAS RAPIDO
3. MEMORIA: Native usa 92% MENOS
4. THROUGHPUT: Native 42% mejor en pruebas cortas

AHORRO EN PRODUCCION (50 microservicios):
   JVM:    50 × 238 MB = 11,900 MB (~12 GB)
   Native: 50 × 17 MB  =    850 MB (~1 GB)
   Ahorro: ~10 GB de RAM
```

### ¿Cuándo usar cada modo?

#### Usar JVM cuando:

```
✅ Desarrollo local (iteración rápida, hot reload)
✅ Necesitas debugging avanzado
✅ El equipo no conoce limitaciones de Native
✅ Usas reflection/serialización dinámica intensiva
✅ No hay restricciones de memoria
✅ Aplicaciones long-running con warmup completo
```

**Ejemplo:** Desarrollo local con `./mvnw quarkus:dev` para iteración rápida.

#### Usar Native cuando:

```
✅ Microservicios en cloud/Kubernetes
✅ Funciones serverless (AWS Lambda, Azure Functions)
✅ CLI tools y utilidades
✅ Aplicaciones donde arranque rápido es crítico
✅ Restricciones de memoria/costo
✅ Necesitas escalar horizontalmente rápido
✅ Contenedores efímeros
```

**Ejemplo:** API de pre-aprobación crediticia desplegada en Kubernetes que escala según demanda.

---

## 🔬 GraalVM y Compilación Native

### ¿Qué es GraalVM?

**GraalVM** es una máquina virtual universal que puede ejecutar aplicaciones escritas en múltiples lenguajes (Java, JavaScript, Python, Ruby, R, etc.) y compilarlas a código nativo.

### Componentes de GraalVM

```
┌─────────────────────────────────────────────────────────┐
│                      GRAALVM                             │
├─────────────────────────────────────────────────────────┤
│  ┌───────────────┐  ┌──────────────────────────────┐   │
│  │ Graal JIT     │  │ Native Image                  │   │
│  │ Compiler      │  │ (AOT Compiler)                │   │
│  │ (Runtime)     │  │ - Análisis estático           │   │
│  └───────────────┘  │ - Compilación ahead-of-time   │   │
│                     │ - Tree shaking                 │   │
│                     │ - Optimizaciones               │   │
│                     └──────────────────────────────┘   │
│  ┌───────────────────────────────────────────────────┐ │
│  │ Truffle Language Implementation Framework         │ │
│  │ (Soporte multi-lenguaje)                          │ │
│  └───────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────┐ │
│  │ SubstrateVM                                        │ │
│  │ - Runtime minimalista                              │ │
│  │ - GC optimizado                                    │ │
│  │ - Sin interpretación                               │ │
│  └───────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

### Proceso de Compilación Native

```
CÓDIGO JAVA (.java)
    ↓
[javac] Compilación a Bytecode
    ↓
BYTECODE (.class)
    ↓
[Quarkus Build] Augmentation (análisis build-time)
    ↓
OPTIMIZED BYTECODE
    ↓
[GraalVM Native Image] 
    ↓
┌──────────────────────────────────────────┐
│ FASE 1: INICIALIZACIÓN                   │
│ - Carga configuración                    │
│ - Setup classpath                        │
│ - Detecta entry points                   │
└──────────────────────────────────────────┘
    ↓
┌──────────────────────────────────────────┐
│ FASE 2: ANÁLISIS (ANALYSIS)              │
│ - Análisis estático de alcance          │
│ - Identifica clases usadas               │
│ - Detecta reflection/JNI                 │
│ - Build call graph                       │
│ TIEMPO: 30-40% del total                 │
└──────────────────────────────────────────┘
    ↓
┌──────────────────────────────────────────┐
│ FASE 3: UNIVERSE (BUILD UNIVERSE)        │
│ - Construye imagen del heap              │
│ - Resuelve dependencias                  │
│ - Prepara datos estáticos                │
│ TIEMPO: 10-15% del total                 │
└──────────────────────────────────────────┘
    ↓
┌──────────────────────────────────────────┐
│ FASE 4: PARSING                          │
│ - Parse métodos alcanzables              │
│ - Optimizaciones tempranas               │
│ TIEMPO: 10-15% del total                 │
└──────────────────────────────────────────┘
    ↓
┌──────────────────────────────────────────┐
│ FASE 5: INLINING                         │
│ - Inline métodos pequeños                │
│ - Elimina indirecciones                  │
│ TIEMPO: 5-10% del total                  │
└──────────────────────────────────────────┘
    ↓
┌──────────────────────────────────────────┐
│ FASE 6: COMPILACIÓN                      │
│ - Genera código máquina nativo           │
│ - Optimizaciones de bajo nivel           │
│ - Register allocation                    │
│ TIEMPO: 25-35% del total (MÁS LENTA)     │
└──────────────────────────────────────────┘
    ↓
┌──────────────────────────────────────────┐
│ FASE 7: LAYOUT                           │
│ - Organiza código en memoria             │
│ - Crea secciones ejecutables             │
│ TIEMPO: 5-10% del total                  │
└──────────────────────────────────────────┘
    ↓
┌──────────────────────────────────────────┐
│ FASE 8: IMAGE CREATION                   │
│ - Genera archivo ejecutable              │
│ - Empaqueta runtime                      │
│ - Strip symbols (opcional)               │
│ TIEMPO: 5-10% del total                  │
└──────────────────────────────────────────┘
    ↓
EJECUTABLE NATIVO (binario Linux x86_64)
```

### Optimizaciones de Native Image

#### 1. Tree Shaking (Dead Code Elimination)

```java
// Código original
public class Utils {
    public static void metodoUsado() { }
    public static void metodoNoUsado() { }
}

// Después de análisis
public class Utils {
    public static void metodoUsado() { }
    // metodoNoUsado() eliminado
}
```

**Resultado:** Solo el código alcanzable se incluye en el binario.

#### 2. Class Initialization at Build Time

```java
// JVM: Cada vez que arranca
static {
    LOGGER = LoggerFactory.getLogger(MyClass.class);
    CONFIG = loadConfiguration(); // Se ejecuta en arranque
}

// Native: Una sola vez durante build
static {
    // Esto se ejecuta DURANTE la compilación
    // Los valores se "congelan" en el binario
}
```

**Resultado:** Arranque más rápido (no re-inicializa).

#### 3. Closed World Assumption

GraalVM asume que **todo el código necesario está disponible en tiempo de compilación**.

```
JVM (Open World):
- Puede cargar clases dinámicamente
- Reflection sin restricciones
- ClassLoaders en runtime

Native (Closed World):
- Todo conocido en build time
- Reflection requiere configuración
- No carga clases dinámicas
```

### Limitaciones de Native Image

| Característica | JVM | Native | Workaround |
|----------------|-----|--------|------------|
| **Reflection** | ✅ Libre | ⚠️ Requiere config | `reflect-config.json` |
| **Dynamic Proxy** | ✅ Libre | ⚠️ Requiere config | `proxy-config.json` |
| **JNI** | ✅ Libre | ⚠️ Requiere config | `jni-config.json` |
| **Resources** | ✅ Automático | ⚠️ Requiere config | `resource-config.json` |
| **Serialization** | ✅ Libre | ⚠️ Limitado | Jackson alternativo |
| **JVMTI** | ✅ Completo | ❌ No soportado | - |
| **InvokeDynamic** | ✅ Sí | ⚠️ Limitado | Evitar |

**⚠️ CRÍTICO:** Quarkus **automatiza** la generación de estas configuraciones, por eso funciona "mágicamente".

---

## 🐳 Dockerfiles Multi-Stage

### ¿Por qué Docker para Compilación Native?

**Problema tradicional:**
- Instalar GraalVM localmente es complejo
- Windows requiere Visual Studio Build Tools (~8 GB)
- macOS ARM64 genera binarios incompatibles con Linux
- Cada desarrollador tiene ambiente diferente

**Solución: Dockerfiles Multi-Stage**
- GraalVM viene incluido en la imagen de build
- Compilación reproducible en cualquier máquina
- Solo necesitas Docker Desktop

### Arquitectura Multi-Stage

```
┌─────────────────────────────────────────────────────────┐
│  DOCKERFILE MULTI-STAGE                                  │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  STAGE 1: BUILD (imagen grande, temporal)               │
│  ┌───────────────────────────────────────────────────┐ │
│  │  FROM maven:3.9.9-eclipse-temurin-21-alpine       │ │
│  │  - Contiene Maven + JDK completo                  │ │
│  │  - Compila el proyecto                            │ │
│  │  - Genera artefactos                              │ │
│  │  - SE DESCARTA al final                           │ │
│  └───────────────────────────────────────────────────┘ │
│                         ↓                                │
│  STAGE 2: RUNTIME (imagen pequeña, final)               │
│  ┌───────────────────────────────────────────────────┐ │
│  │  FROM eclipse-temurin:21-jre-alpine               │ │
│  │  - Solo JRE (no JDK completo)                     │ │
│  │  - Copia artefactos del Stage 1                   │ │
│  │  - Usuario no-root (seguridad)                    │ │
│  │  - Health check configurado                       │ │
│  │  - ESTA ES LA IMAGEN FINAL                        │ │
│  └───────────────────────────────────────────────────┘ │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Dockerfile.jvm (Multi-Stage)

```dockerfile
# ============================================================
# STAGE 1: BUILD
# ============================================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src

# Compilar proyecto
RUN mvn package -DskipTests -Dquarkus.package.jar.type=fast-jar

# ============================================================
# STAGE 2: RUNTIME
# ============================================================
FROM eclipse-temurin:21-jre-alpine

# Usuario no-root (seguridad)
RUN addgroup -S quarkus && adduser -S quarkus -G quarkus
USER quarkus

WORKDIR /app

# Copiar artefactos del stage de build
COPY --from=build /build/target/quarkus-app/ ./

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s \
  CMD wget -q --spider http://localhost:8080/q/health/ready || exit 1

ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
```

**Resultado:** Imagen de ~705 MB con JRE optimizado.

### Dockerfile.native (Multi-Stage con GraalVM)

```dockerfile
# ============================================================
# STAGE 1: BUILD NATIVE (GraalVM incluido)
# ============================================================
FROM quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21 AS build

USER root
WORKDIR /build

COPY pom.xml .
COPY src ./src

# Compilar a binario nativo (5-10 minutos)
RUN mvn package -DskipTests -Pnative \
    -Dquarkus.native.container-build=false

# ============================================================
# STAGE 2: RUNTIME (imagen mínima)
# ============================================================
FROM registry.access.redhat.com/ubi9/ubi-minimal:9.5

# Usuario no-root
RUN microdnf install -y shadow-utils && \
    groupadd -r quarkus && useradd -r -g quarkus quarkus && \
    microdnf clean all

USER quarkus
WORKDIR /app

# Copiar solo el binario nativo
COPY --from=build /build/target/*-runner /app/application

EXPOSE 8080

# Health check optimizado (arranque rápido)
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s \
  CMD wget -q --spider http://localhost:8080/q/health/ready || exit 1

ENTRYPOINT ["./application"]
```

**Resultado:** Imagen de ~430 MB con binario nativo standalone.

### Comparación de Imágenes

| Característica | Dockerfile.jvm | Dockerfile.native |
|----------------|----------------|-------------------|
| **Base build** | Maven + JDK 21 | GraalVM Mandrel |
| **Base runtime** | JRE Alpine | UBI Minimal |
| **Tiempo build** | ~4 segundos | ~199 segundos |
| **Tamaño imagen** | 705 MB | 430 MB |
| **Arranque** | 1811 ms | 127 ms |
| **Memoria** | 238 MB | 17 MB |
| **Requiere GraalVM local** | ❌ No | ❌ No |

### Ventajas del Enfoque Multi-Stage

```
✅ No necesitas instalar GraalVM localmente
✅ No necesitas Visual Studio en Windows
✅ Funciona igual en Mac, Windows y Linux
✅ Compilación reproducible (mismo resultado siempre)
✅ Solo necesitas Docker Desktop
✅ CI/CD simplificado (mismos Dockerfiles)
✅ Imágenes finales pequeñas (no incluyen herramientas de build)
```

### Ejecución del Benchmark con Docker

```bash
# El script benchmark.sh usa ambos Dockerfiles
./benchmark.sh 500

# Internamente ejecuta:
# 1. docker build -f Dockerfile.jvm -t app-jvm .
# 2. docker run app-jvm (pruebas)
# 3. docker build -f Dockerfile.native -t app-native .
# 4. docker run app-native (pruebas)
# 5. Comparación de resultados
```

---

## 🪄 Dev Services vs Docker-Compose

### ¿Qué son Dev Services?

**Dev Services** es una característica de Quarkus que levanta automáticamente dependencias externas (bases de datos, message brokers, etc.) durante el desarrollo.

### Cómo Funciona Dev Services

```
┌──────────────────────────────────────────────────────────┐
│  DESARROLLADOR                                           │
│                                                          │
│  $ ./mvnw quarkus:dev                                   │
└────────────────┬─────────────────────────────────────────┘
                 │
                 ↓
┌──────────────────────────────────────────────────────────┐
│  QUARKUS DEV MODE                                        │
│                                                          │
│  [1] Detecta extensión: quarkus-jdbc-postgresql         │
│  [2] Busca datasource configurado                       │
│  [3] ¿Hay URL configurada? NO                          │
│  [4] Activa Dev Services para PostgreSQL                │
└────────────────┬─────────────────────────────────────────┘
                 │
                 ↓
┌──────────────────────────────────────────────────────────┐
│  DOCKER (Automático)                                     │
│                                                          │
│  [1] Busca imagen: postgres:16                          │
│  [2] ¿Existe local? No → Descarga desde Docker Hub      │
│  [3] Crea contenedor efímero                            │
│      - Puerto: random (ej: 32768)                       │
│      - Usuario: quarkus                                  │
│      - Password: quarkus                                 │
│      - Base de datos: default                           │
│  [4] Espera hasta que PostgreSQL esté ready             │
└────────────────┬─────────────────────────────────────────┘
                 │
                 ↓
┌──────────────────────────────────────────────────────────┐
│  APLICACIÓN QUARKUS                                      │
│                                                          │
│  [1] Recibe URL dinámica: jdbc:postgresql://localhost:32768/default │
│  [2] Configura datasource automáticamente               │
│  [3] Ejecuta import.sql si existe                       │
│  [4] Arranca aplicación                                  │
│  [5] ¡Listo para desarrollar! ✅                        │
└──────────────────────────────────────────────────────────┘
```

### Dev Services vs docker-compose: Cuándo Usar Cada Uno

| Escenario | Dev Services | docker-compose |
|-----------|--------------|----------------|
| **Desarrollo rápido** | ✅ Ideal | ⚠️ Overkill |
| **Prototipado** | ✅ Ideal | ⚠️ Overkill |
| **Benchmarks** | ❌ No control | ✅ Ideal |
| **Tests automatizados** | ⚠️ Posible | ✅ Mejor control |
| **Scripts de prueba** | ❌ Puerto aleatorio | ✅ Puerto fijo |
| **CI/CD** | ⚠️ Posible | ✅ Reproducible |
| **Datos persistentes** | ❌ Efímero | ✅ Volúmenes |
| **Configuración custom** | ⚠️ Limitado | ✅ Total control |

### Nuestro Enfoque en Este Proyecto

```
┌─────────────────────────────────────────────────────────┐
│  DESARROLLO INTERACTIVO                                  │
│  ./mvnw quarkus:dev                                      │
│                                                          │
│  → Usa Dev Services (PostgreSQL automático)             │
│  → Hot reload activado                                   │
│  → Puerto PostgreSQL aleatorio (no importa)             │
│  → Datos efímeros (se pierden al cerrar)                │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  SCRIPTS AUTOMATIZADOS                                   │
│  ./test-aprobacion.sh                                    │
│  ./benchmark.sh                                          │
│                                                          │
│  → Usa docker-compose (PostgreSQL controlado)           │
│  → Puerto fijo: 5432                                     │
│  → Credenciales conocidas: postgres/postgres123         │
│  → Base de datos: banco_credito                         │
│  → Datos persistentes en volumen                        │
└─────────────────────────────────────────────────────────┘
```

### docker-compose.yml del Proyecto

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: banco-postgres
    environment:
      POSTGRES_DB: banco_credito
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```

### Flujo Completo de los Scripts

```
┌─────────────────────────────────────────────────────────┐
│  ./test-aprobacion.sh --docker                          │
├─────────────────────────────────────────────────────────┤
│  1. docker-compose up -d                                │
│     → Levanta PostgreSQL en puerto 5432                 │
│                                                          │
│  2. docker build -f Dockerfile.jvm -t app-jvm .         │
│     → Construye imagen JVM                              │
│                                                          │
│  3. docker run -p 8080:8080 app-jvm                     │
│     → Ejecuta Quarkus en contenedor                     │
│     → Se conecta a PostgreSQL via host.docker.internal │
│                                                          │
│  4. curl http://localhost:8080/api/...                  │
│     → Ejecuta 11 pruebas funcionales                    │
│                                                          │
│  5. docker stop / docker-compose down                   │
│     → Limpia todo al terminar                           │
└─────────────────────────────────────────────────────────┘
```

### Configuración de Dev Services (Referencia)

```properties
# Desactivar Dev Services (si usas docker-compose)
%dev.quarkus.datasource.devservices.enabled=false

# Personalizar imagen
%dev.quarkus.datasource.devservices.image-name=postgres:15-alpine

# Puerto fijo (en lugar de aleatorio)
%dev.quarkus.datasource.devservices.port=5432

# Inicializar con script
%dev.quarkus.datasource.devservices.init-script-path=init-db.sql
```

---

## 🗄️ Hibernate ORM y Panache

### Hibernate ORM Tradicional

```java
// Entity tradicional
@Entity
@Table(name = "solicitud_credito")
public class SolicitudCredito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Getters y setters...
}

// DAO tradicional (mucho código)
@ApplicationScoped
public class SolicitudDAO {
    @Inject
    EntityManager em;
    
    public List<SolicitudCredito> listarTodas() {
        return em.createQuery("SELECT s FROM SolicitudCredito s", SolicitudCredito.class)
                 .getResultList();
    }
    
    public SolicitudCredito buscarPorId(Long id) {
        return em.find(SolicitudCredito.class, id);
    }
    
    @Transactional
    public void guardar(SolicitudCredito solicitud) {
        if (solicitud.getId() == null) {
            em.persist(solicitud);
        } else {
            em.merge(solicitud);
        }
    }
}
```

### Panache: ORM Simplificado

```java
// Entity con Panache (activo record pattern)
@Entity
@Table(name = "solicitud_credito")
public class SolicitudCredito extends PanacheEntity {
    // PanacheEntity ya incluye:
    // - Long id
    // - persist(), delete()
    // - Métodos estáticos: findAll(), findById(), etc.
    
    public String numeroDocumento;
    public EstadoSolicitud estado;
    
    // Queries personalizados
    public static List<SolicitudCredito> porEstado(EstadoSolicitud estado) {
        return find("estado", estado).list();
    }
}

// Uso directo (sin DAO)
@Transactional
public void procesarSolicitud() {
    // Crear
    SolicitudCredito solicitud = new SolicitudCredito();
    solicitud.numeroDocumento = "12345678";
    solicitud.persist(); // ¡Así de simple!
    
    // Buscar
    List<SolicitudCredito> aprobadas = SolicitudCredito.porEstado(APROBADO);
    
    // Actualizar
    solicitud.estado = EstadoSolicitud.RECHAZADO;
    // Auto-persiste al salir del método @Transactional
    
    // Eliminar
    solicitud.delete();
}
```

### Ventajas de Panache

```
✅ 90% menos código boilerplate
✅ Active Record Pattern (entidad = repositorio)
✅ Queries fluidas y expresivas
✅ Paginación built-in
✅ Integración perfecta con Quarkus
✅ Generación automática de queries
```

### Panache Repository Pattern

```java
// Alternativa: Repository pattern (en lugar de Active Record)
@Entity
public class SolicitudCredito {
    @Id @GeneratedValue
    public Long id;
    public String numeroDocumento;
    // Sin métodos de persistencia
}

@ApplicationScoped
public class SolicitudRepository implements PanacheRepository<SolicitudCredito> {
    // Métodos automáticos:
    // - findAll(), findById(), persist(), delete()
    
    // Queries personalizados
    public List<SolicitudCredito> porEstado(EstadoSolicitud estado) {
        return find("estado", estado).list();
    }
    
    public List<SolicitudCredito> porRangoScore(int min, int max) {
        return find("scoreCalculado >= ?1 and scoreCalculado <= ?2", min, max).list();
    }
}
```

### Queries en Panache

```java
// Queries simples
SolicitudCredito.findAll().list();
SolicitudCredito.findById(1L);
SolicitudCredito.find("estado", APROBADO).list();

// Queries con paginación
SolicitudCredito.findAll()
    .page(Page.of(0, 10))  // Página 0, 10 items
    .list();

// Queries con parámetros nombrados
SolicitudCredito.find("estado = :estado and scoreCalculado > :score",
    Parameters.with("estado", APROBADO).and("score", 700))
    .list();

// Queries con ordenamiento
SolicitudCredito.find("estado", APROBADO)
    .sort("fechaSolicitud", Sort.Direction.Descending)
    .list();

// Streams para procesar grandes volúmenes
SolicitudCredito.streamAll()
    .filter(s -> s.scoreCalculado > 700)
    .forEach(s -> procesarSolicitud(s));

// Count
long total = SolicitudCredito.count("estado", PENDIENTE);

// Delete bulk
long deleted = SolicitudCredito.delete("estado = ?1 and fechaSolicitud < ?2", 
    RECHAZADO, LocalDateTime.now().minusYears(1));
```

---

## 📊 Métricas y Observabilidad

### Stack de Observabilidad

```
┌────────────────────────────────────────────────────────┐
│  APLICACIÓN QUARKUS                                    │
│  ┌──────────────────────────────────────────────┐     │
│  │  Micrometer (Abstracción)                    │     │
│  │  - Counters, Gauges, Timers, Histograms      │     │
│  └──────────────┬───────────────────────────────┘     │
│                 │                                      │
│                 ↓                                      │
│  ┌──────────────────────────────────────────────┐     │
│  │  Prometheus Registry                          │     │
│  │  - Formatea en formato Prometheus            │     │
│  │  - Expone en /q/metrics                       │     │
│  └──────────────────────────────────────────────┘     │
└────────────────┬───────────────────────────────────────┘
                 │
                 │ HTTP GET /q/metrics
                 ↓
┌────────────────────────────────────────────────────────┐
│  PROMETHEUS (Time Series Database)                     │
│  - Scrape métricas cada 15s                            │
│  - Almacena historial                                  │
│  - Permite queries (PromQL)                            │
└────────────────┬───────────────────────────────────────┘
                 │
                 │ Queries
                 ↓
┌────────────────────────────────────────────────────────┐
│  GRAFANA (Visualización)                               │
│  - Dashboards                                          │
│  - Alertas                                             │
│  - Gráficas en tiempo real                            │
└────────────────────────────────────────────────────────┘
```

### Health Checks

```
GET /q/health
```

**Respuesta:**
```json
{
  "status": "UP",
  "checks": [
    {
      "name": "Database connections health check",
      "status": "UP",
      "data": {
        "<default>": "UP"
      }
    }
  ]
}
```

**Tipos de Health Checks:**

| Endpoint | Propósito | Uso típico |
|----------|-----------|------------|
| `/q/health` | Estado general | Info general |
| `/q/health/live` | ¿Está vivo? | Kubernetes liveness probe |
| `/q/health/ready` | ¿Listo para requests? | Kubernetes readiness probe |
| `/q/health/started` | ¿Completó arranque? | Post-startup checks |

---

## 🌐 REST y Serialización JSON

### JAX-RS + Jackson

```java
@Path("/api/preaprobacion")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PreAprobacionResource {
    
    @POST
    @Path("/evaluar")
    public Response evaluar(SolicitudDTO solicitud) {
        // Jackson automáticamente:
        // 1. Deserializa JSON → SolicitudDTO
        // 2. Valida con Bean Validation
        // 3. Ejecuta método
        // 4. Serializa resultado → JSON
        
        return Response.ok(resultado).build();
    }
}
```

### Proceso de Serialización

```
CLIENTE → Request HTTP
    ↓
[JSON String]
{
  "numeroDocumento": "12345678",
  "montoSolicitado": 50000.00
}
    ↓
[Jackson ObjectMapper] Deserialización
    ↓
[Objeto Java] SolicitudDTO
    ↓
[Bean Validation] @NotNull, @Min, @Max
    ↓
[Método Resource] Lógica de negocio
    ↓
[Objeto Java] ResultadoDTO
    ↓
[Jackson ObjectMapper] Serialización
    ↓
[JSON String]
{
  "aprobado": true,
  "montoAprobado": 50000.00,
  "tasaInteres": 9.5
}
    ↓
Response HTTP → CLIENTE
```

---

## 💾 Transacciones y Gestión de Base de Datos

### Transacciones con Narayana JTA

```java
@ApplicationScoped
public class PreAprobacionService {
    
    @Transactional // ← Anotación crítica
    public ResultadoEvaluacion evaluar(SolicitudDTO dto) {
        // TODO lo que pasa aquí es parte de UNA transacción
        
        // 1. Crear entidad
        SolicitudCredito solicitud = new SolicitudCredito();
        solicitud.setEstado(EstadoSolicitud.EN_EVALUACION);
        solicitud.persist(); // INSERT
        
        // 2. Calcular score
        int score = calcularScore(solicitud);
        solicitud.setScoreCalculado(score);
        // UPDATE automático
        
        // 3. Actualizar estado
        if (score > 700) {
            solicitud.setEstado(EstadoSolicitud.APROBADO);
        } else {
            solicitud.setEstado(EstadoSolicitud.RECHAZADO);
        }
        // UPDATE automático
        
        // Al salir del método:
        // - Sin excepciones → COMMIT
        // - Con excepción → ROLLBACK
        
        return resultado;
    }
}
```

### Gestión del Schema

```properties
# application.properties

# Opciones de database.generation:
# - none: No hace nada
# - create: Crea tablas al iniciar, NO las borra
# - drop-and-create: Borra y recrea (DESARROLLO)
# - update: Actualiza schema (CUIDADO en producción)
# - validate: Solo valida que coincida
quarkus.hibernate-orm.database.generation=drop-and-create

# Script de inicialización
quarkus.hibernate-orm.sql-load-script=import.sql
```

**⚠️ IMPORTANTE:** 
- `drop-and-create` es para **desarrollo**
- En **producción** usa migrations (Flyway/Liquibase)

---

## 🎭 Perfiles de Configuración

### Tres Perfiles Principales

```properties
# ============================================================
# CONFIGURACIÓN BASE (Aplica a todos)
# ============================================================
quarkus.application.name=aprobacion-express
quarkus.datasource.db-kind=postgresql

# ============================================================
# PERFIL DEV (Desarrollo)
# ============================================================
%dev.quarkus.log.level=DEBUG
%dev.quarkus.hibernate-orm.log.sql=true
%dev.quarkus.hibernate-orm.database.generation=drop-and-create

# ============================================================
# PERFIL TEST (Testing)
# ============================================================
%test.quarkus.datasource.devservices.enabled=true
%test.quarkus.hibernate-orm.database.generation=drop-and-create

# ============================================================
# PERFIL PROD (Producción)
# ============================================================
%prod.quarkus.datasource.username=${DB_USERNAME:postgres}
%prod.quarkus.datasource.password=${DB_PASSWORD}
%prod.quarkus.hibernate-orm.database.generation=validate
%prod.quarkus.log.level=INFO
```

### Activación de Perfiles

```bash
# Desarrollo (automático con quarkus:dev)
./mvnw quarkus:dev

# Test (automático con test)
./mvnw test

# Producción (JAR o Docker)
java -jar app.jar
# o
docker run -e DB_PASSWORD=secret app-native
```

---

## 🚀 Conceptos Avanzados

### 1. Build Time vs Runtime

Quarkus introduce un concepto revolucionario: **hacer en build-time lo que normalmente se hace en runtime**.

```
FRAMEWORK TRADICIONAL (ej: Spring):
┌────────────────────────────────────────┐
│  ARRANQUE                              │
│  1. Escanea classpath                  │  ⏱️ 2-3 seg
│  2. Analiza anotaciones                │
│  3. Construye metadata                 │
│  4. Inicializa beans                   │
│  5. Conecta dependencias               │
│  6. ¡Listo!                            │
└────────────────────────────────────────┘

QUARKUS (Build-time optimizations):
┌────────────────────────────────────────┐
│  BUILD TIME (./mvnw package)           │
│  1. Escanea classpath                  │  ⏱️ 1 vez
│  2. Analiza anotaciones                │
│  3. Construye metadata                 │
│  4. Pre-inicializa beans               │
│  5. Genera código optimizado           │
│  6. Empaqueta todo                     │
└────────────────────────────────────────┘
                ↓
┌────────────────────────────────────────┐
│  RUNTIME (docker run)                  │
│  1. Carga artifact pre-procesado       │  ⏱️ 0.1 seg
│  2. ¡Listo!                            │
└────────────────────────────────────────┘
```

**Resultado:** Arranque 14x más rápido (1811ms → 127ms).

### 2. Augmentation (Procesamiento en Build)

```java
// Tu código
@Path("/api/clientes")
public class ClienteResource {
    
    @Inject
    ClienteService service;
    
    @GET
    public List<Cliente> listar() {
        return service.listar();
    }
}

// Lo que Quarkus genera en build-time
public class ClienteResource$$QuarkusProxy {
    private final ClienteService service;
    
    public ClienteResource$$QuarkusProxy() {
        this.service = Arc.container()
            .instance(ClienteService.class)
            .get();
    }
    
    public List<Cliente> listar() {
        return service.listar();
    }
}
```

Quarkus **genera código** en build-time, eliminando reflection en runtime.

### 3. SubstrateVM vs HotSpot

| Aspecto | HotSpot (JVM) | SubstrateVM (Native) |
|---------|---------------|----------------------|
| **Garbage Collector** | G1GC, ZGC, Shenandoah | Serial GC (simple) |
| **JIT Compiler** | C1 + C2 (tiered) | No (AOT) |
| **Class Loading** | Dinámico | Estático |
| **Reflection** | Runtime completo | Build-time limitado |
| **Memory Layout** | Heap complejo | Heap simple |
| **Optimizaciones** | Runtime (adaptativo) | Build-time (estático) |

---

## 📖 Recursos Adicionales

### Documentación Oficial
- Quarkus: https://quarkus.io/guides/
- GraalVM: https://www.graalvm.org/latest/docs/
- Hibernate: https://hibernate.org/orm/documentation/

### Libros Recomendados
- "Quarkus for Spring Developers" - Red Hat
- "Understanding Quarkus" - Antonio Goncalves
- "GraalVM in Action" - Oleg Šelajev

### Comunidad
- Quarkus GitHub: https://github.com/quarkusio/quarkus
- Zulip Chat: https://quarkusio.zulipchat.com/
- Stack Overflow: Tag `quarkus`

---

## 🎓 Conclusión

Este documento cubre los conceptos teóricos profundos del sistema. Para:
- **Instrucciones de ejecución:** Ver `README.md`
- **Guía del instructor:** Ver `INSTRUCTOR.md`
- **Uso de scripts:** Ver `GUIA-SCRIPTS-DOCKER.md`

**Última actualización:** 2025-11-24  
**Versión:** 2.0.0