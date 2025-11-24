# 📚 TEORIA.md - Conceptos Profundos

# 📚 TEORÍA: Conceptos Profundos del Sistema

## 📋 Tabla de Contenidos

1. [Arquitectura de Quarkus](#arquitectura-de-quarkus)
2. [JVM vs Native: Análisis Profundo](#jvm-vs-native-análisis-profundo)
3. [GraalVM y Compilación Native](#graalvm-y-compilación-native)
4. [Dev Services: Magia de Desarrollo](#dev-services-magia-de-desarrollo)
5. [Hibernate ORM y Panache](#hibernate-orm-y-panache)
6. [Métricas y Observabilidad](#métricas-y-observabilidad)
7. [REST y Serialización JSON](#rest-y-serialización-json)
8. [Transacciones y Gestión de Base de Datos](#transacciones-y-gestión-de-base-de-datos)
9. [Perfiles de Configuración](#perfiles-de-configuración)
10. [Conceptos Avanzados](#conceptos-avanzados)

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
│  SISTEMA OPERATIVO (específico: macOS ARM64)            │
└─────────────────────────────────────────────────────────┘

Características:
✅ Arranque instantáneo (código ya compilado)
✅ Memoria mínima (solo lo usado)
✅ Rendimiento predecible
❌ Compilación lenta (análisis profundo)
❌ Menos portable (binario por SO/arquitectura)
❌ Debugging limitado
```

### Comparación Detallada

| Aspecto | JVM | Native | Ganador |
|---------|-----|--------|---------|
| **ARRANQUE** |
| Tiempo de inicio | 2-5 segundos | 0.05-0.2 segundos | 🏆 Native (20-40x) |
| ¿Por qué? | Inicializa JVM, carga clases, warmup | Todo precompilado | - |
| **MEMORIA** |
| RSS en arranque | 150-300 MB | 30-80 MB | 🏆 Native (60-70% menos) |
| Heap size | Configurable (Xmx) | Fijo, optimizado | - |
| Metaspace | 50-100 MB | No existe | 🏆 Native |
| **RENDIMIENTO** |
| Throughput inicial | Bajo (warming up) | Alto desde inicio | 🏆 Native |
| Throughput pico | Muy alto (JIT optimiza) | Alto estable | 🏆 JVM |
| Latencia | Variable (GC pauses) | Predecible | 🏆 Native |
| **DESARROLLO** |
| Tiempo compilación | 5-10 segundos | 1-2 minutos | 🏆 JVM (12-24x) |
| Hot reload | Sí (quarkus:dev) | No | 🏆 JVM |
| Debugging | Completo | Limitado | 🏆 JVM |
| **DESPLIEGUE** |
| Tamaño artefacto | JAR: 10-50 MB + JVM | 60-100 MB standalone | 🏆 Native* |
| Portabilidad | Total (cualquier OS) | Por SO/arquitectura | 🏆 JVM |
| Dependencias | Requiere JVM instalado | Ninguna | 🏆 Native |

*Native es "más grande" pero incluye TODO. JVM parece pequeño pero requiere JRE adicional.

### ¿Cuándo usar cada modo?

#### Usar JVM cuando:

```
✅ Desarrollo local (iteración rápida)
✅ Aplicaciones long-running (servidores 24/7)
✅ Necesitas debugging avanzado
✅ El equipo no conoce limitaciones de Native
✅ Usas reflection/serialización dinámica intensiva
✅ No hay restricciones de memoria
✅ Despliegue en servidores tradicionales
```

**Ejemplo:** Sistema bancario core que corre 24/7 en data center con recursos abundantes.

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

**Ejemplo:** API de pre-aprobación crediticia que escala según demanda.

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
EJECUTABLE NATIVO (aprobacion-express-runner)
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

## 🪄 Dev Services: Magia de Desarrollo

### ¿Qué son Dev Services?

**Dev Services** es una característica de Quarkus que levanta automáticamente dependencias externas (bases de datos, message brokers, etc.) durante el desarrollo.

### Cómo Funciona

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
│  [3] Ejecuta migraciones (Flyway/Liquibase si existe)   │
│  [4] Arranca aplicación                                  │
│  [5] ¡Listo para desarrollar! ✅                        │
└──────────────────────────────────────────────────────────┘
```

### Dev Services Soportados

| Servicio | Extensión | Imagen Docker |
|----------|-----------|---------------|
| PostgreSQL | `quarkus-jdbc-postgresql` | `postgres:16` |
| MySQL | `quarkus-jdbc-mysql` | `mysql:8` |
| MongoDB | `quarkus-mongodb-client` | `mongo:7` |
| Redis | `quarkus-redis-client` | `redis:7` |
| Kafka | `quarkus-kafka-client` | `redpanda` |
| Keycloak | `quarkus-oidc` | `keycloak` |
| Vault | `quarkus-vault` | `hashicorp/vault` |

### Ventajas de Dev Services

```
✅ Sin configuración manual
✅ Ambiente aislado (no contamina tu sistema)
✅ Versiones consistentes (toda el equipo usa mismo PostgreSQL)
✅ Setup en segundos
✅ Se destruye al terminar (no deja basura)
✅ Ideal para CI/CD
```

### Cuándo NO usar Dev Services

```
❌ En producción (no existe en producción)
❌ Para benchmarks (necesitas control preciso)
❌ Tests de integración específicos
❌ Cuando necesitas datos persistentes entre ejecuciones
```

**Para estos casos:** usa `docker-compose.yml` o servicios reales.

### Configuración de Dev Services (Opcional)

```properties
# Desactivar Dev Services
%dev.quarkus.datasource.devservices.enabled=false

# Personalizar imagen
%dev.quarkus.datasource.devservices.image-name=postgres:15-alpine

# Puerto fijo (en lugar de aleatorio)
%dev.quarkus.datasource.devservices.port=5432

# Inicializar con script
%dev.quarkus.datasource.devservices.init-script-path=init-db.sql
```

### Dev Services vs docker-compose

| Característica | Dev Services | docker-compose |
|----------------|--------------|----------------|
| **Setup** | Automático | Manual (crear YAML) |
| **Activación** | Solo en modo dev | Explícito (`docker-compose up`) |
| **Persistencia** | Efímero | Persistente (volúmenes) |
| **Control** | Limitado | Total |
| **Uso típico** | Desarrollo rápido | Benchmarks, tests, local "production-like" |

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

// Exists
boolean existe = SolicitudCredito.count("numeroDocumento", "12345678") > 0;

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

### Métricas Expuestas

#### 1. Métricas JVM

```
# Memoria
jvm_memory_used_bytes{area="heap"} 245234688
jvm_memory_max_bytes{area="heap"} 536870912
jvm_memory_committed_bytes{area="heap"} 268435456

# Garbage Collection
jvm_gc_memory_promoted_bytes_total 1048576
jvm_gc_pause_seconds_count{action="end of minor GC"} 42
jvm_gc_pause_seconds_sum{action="end of minor GC"} 0.156

# Threads
jvm_threads_live_threads 25
jvm_threads_daemon_threads 20
jvm_threads_peak_threads 28

# Classes
jvm_classes_loaded_classes 8745
jvm_classes_unloaded_classes_total 12
```

#### 2. Métricas HTTP

```
# Requests totales
http_server_requests_total{method="GET",uri="/api/preaprobacion/estadisticas",status="200"} 1547

# Duración de requests
http_server_requests_seconds_count{method="POST",uri="/api/preaprobacion/evaluar"} 234
http_server_requests_seconds_sum{method="POST",uri="/api/preaprobacion/evaluar"} 45.67

# Requests activos
http_server_active_requests{method="POST",uri="/api/preaprobacion/evaluar"} 3
```

#### 3. Métricas de Base de Datos

```
# Conexiones del pool
hikaricp_connections_active{pool="default"} 5
hikaricp_connections_idle{pool="default"} 5
hikaricp_connections_max{pool="default"} 10
hikaricp_connections_min{pool="default"} 2

# Tiempos de espera
hikaricp_connections_acquire_seconds_count 1234
hikaricp_connections_acquire_seconds_sum 12.34
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

### Configuración de Jackson

```java
// Personalizar serialización
@JsonInclude(JsonInclude.Include.NON_NULL) // Omitir nulls
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class) // snake_case
public class ResultadoDTO {
    
    @JsonProperty("solicitud_id") // Nombre custom
    private Long solicitudId;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaEvaluacion;
    
    @JsonIgnore // No serializar
    private String infoInterna;
}
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

### Niveles de Aislamiento

```java
@Transactional(
    isolation = TransactionIsolation.READ_COMMITTED,
    timeout = 30,
    rollbackOn = {BusinessException.class}
)
public void metodoTransaccional() {
    // ...
}
```

| Nivel | Descripción | Uso |
|-------|-------------|-----|
| `READ_UNCOMMITTED` | Lee cambios no commiteados | Raramente usado |
| `READ_COMMITTED` | Solo lee commiteado (default) | Mayoría de casos |
| `REPEATABLE_READ` | Lecturas consistentes | Reportes |
| `SERIALIZABLE` | Máximo aislamiento | Operaciones críticas |

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

# Producción (JAR)
java -jar app.jar
# o forzar:
java -Dquarkus.profile=prod -jar app.jar

# Native
./app-runner
# o forzar:
./app-runner -Dquarkus.profile=prod
```

### Variables de Entorno

```properties
# Sintaxis: ${ENV_VAR:valor_por_defecto}
%prod.quarkus.datasource.username=${DB_USERNAME:postgres}
%prod.quarkus.datasource.password=${DB_PASSWORD:postgres123}
%prod.quarkus.datasource.jdbc.url=${DB_URL:jdbc:postgresql://localhost:5432/banco_credito}
```

**En producción:**
```bash
# Kubernetes ConfigMap/Secret
apiVersion: v1
kind: Secret
metadata:
  name: db-credentials
data:
  DB_USERNAME: cG9zdGdyZXM=
  DB_PASSWORD: c3VwZXJzZWNyZXQ=
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
│  RUNTIME (java -jar)                   │
│  1. Carga artifact pre-procesado       │  ⏱️ 0.1 seg
│  2. ¡Listo!                            │
└────────────────────────────────────────┘
```

**Resultado:** Arranque 20-30x más rápido.

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
| **Garbage Collector** | G1GC, ZGC, Shenandah | Serial GC (simple) |
| **JIT Compiler** | C1 + C2 (tiered) | No (AOT) |
| **Class Loading** | Dinámico | Estático |
| **Reflection** | Runtime completo | Build-time limitado |
| **Memory Layout** | Heap complejo | Heap simple |
| **Optimizaciones** | Runtime (adaptativo) | Build-time (estático) |

### 4. Reactive vs Imperative

```java
// IMPERATIVO (Blocking I/O)
@GET
@Path("/{id}")
public Cliente buscar(@PathParam("id") Long id) {
    Cliente cliente = repository.findById(id); // BLOQUEA thread
    Cliente detalles = externalAPI.getDetalles(cliente); // BLOQUEA thread
    return detalles;
}

// REACTIVO (Non-blocking I/O)
@GET
@Path("/{id}")
public Uni<Cliente> buscar(@PathParam("id") Long id) {
    return repository.findById(id) // No bloquea
        .flatMap(cliente -> externalAPI.getDetalles(cliente)) // No bloquea
        .onFailure().recoverWithItem(ClienteDefault.instance());
}
```

**Cuándo usar cada uno:**
- **Imperativo:** CRUD simple, bajo concurrencia
- **Reactivo:** Alto throughput, muchos requests concurrentes, I/O intensivo

### 5. Continuous Testing

```bash
# Modo dev con tests continuos
./mvnw quarkus:dev

# En la consola interactiva:
# Presiona 'r' → Re-ejecuta tests
# Presiona 't' → Ejecuta test específico
# Cambias código → Tests se ejecutan automáticamente
```

**Ventaja:** Feedback inmediato mientras desarrollas.

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


**Última actualización:** 2025-10-23  
**Versión:** 1.0.0
