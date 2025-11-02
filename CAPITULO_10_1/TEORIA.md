# 📚 TEORÍA - Monitoreo con Grafana y Kibana en Entornos Quarkus

## Observabilidad en Arquitecturas de Microservicios

---

## 📋 Tabla de Contenidos

1. [Introducción a la Observabilidad](#1-introducción-a-la-observabilidad)
2. [Los Tres Pilares de la Observabilidad](#2-los-tres-pilares-de-la-observabilidad)
3. [Métricas con Micrometer y Prometheus](#3-métricas-con-micrometer-y-prometheus)
4. [Visualización con Grafana](#4-visualización-con-grafana)
5. [Logs Centralizados con Elastic Stack](#5-logs-centralizados-con-elastic-stack)
6. [Identificación de Cuellos de Botella](#6-identificación-de-cuellos-de-botella)
7. [Detección de Patrones de Error](#7-detección-de-patrones-de-error)
8. [PromQL: El Lenguaje de Prometheus](#8-promql-el-lenguaje-de-prometheus)
9. [Kibana Query Language (KQL)](#9-kibana-query-language-kql)
10. [Mejores Prácticas de Observabilidad](#10-mejores-prácticas-de-observabilidad)
11. [Antipatrones Comunes](#11-antipatrones-comunes)
12. [Casos de Uso Reales](#12-casos-de-uso-reales)

---

## 1. Introducción a la Observabilidad

### 1.1 ¿Qué es Observabilidad?

**Definición formal**: La observabilidad es una propiedad de un sistema que determina qué tan bien puedes entender su estado interno basándote únicamente en sus salidas externas.

**Origen del término**: Proviene de la teoría de control en ingeniería, donde un sistema es "observable" si puedes determinar su estado interno completo mediante sus salidas medibles.

### 1.2 Observabilidad vs Monitoreo

Hay una diferencia crucial entre estos conceptos:

```
┌─────────────────────────────────────────────────────────┐
│                    MONITOREO                            │
│  "¿Está funcionando el sistema?"                        │
│                                                         │
│  • Conoces las preguntas de antemano                   │
│  • Dashboards predefinidos                             │
│  • Alertas basadas en umbrales conocidos               │
│  • Enfoque: Detectar problemas conocidos               │
│                                                         │
│  Ejemplo: "¿El CPU está por encima del 80%?"          │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                  OBSERVABILIDAD                         │
│  "¿Por qué no está funcionando?"                       │
│                                                         │
│  • Puedes hacer preguntas arbitrarias                  │
│  • Exploración ad-hoc                                  │
│  • Correlación de eventos                              │
│  • Enfoque: Entender problemas desconocidos            │
│                                                         │
│  Ejemplo: "¿Por qué este request específico tardó      │
│            500ms más que el anterior?"                  │
└─────────────────────────────────────────────────────────┘
```

### 1.3 Analogía: El Sistema de Salud Humano

**Monitoreo** es como ir al médico para un chequeo rutinario:
- Miden tu presión arterial (métrica conocida)
- Verifican tu peso (métrica conocida)
- Si algo está fuera de rango, suena una alarma

**Observabilidad** es como tener un análisis completo de sangre cuando no te sientes bien pero no sabes por qué:
- El médico puede explorar múltiples biomarcadores
- Correlacionar síntomas con resultados
- Descubrir problemas que no sabías que existían

### 1.4 ¿Por qué es Crítico en Microservicios?

En un monolito:
```
┌─────────────────────┐
│     MONOLITO        │
│                     │
│  - 1 proceso        │
│  - 1 base de datos  │
│  - 1 log file       │
│  - Fácil debuggear  │
└─────────────────────┘
```

En microservicios:
```
┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐
│ Svc1 │→│ Svc2 │→│ Svc3 │→│ Svc4 │
└──┬───┘  └──┬───┘  └──┬───┘  └──┬───┘
   │         │         │         │
   ▼         ▼         ▼         ▼
┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐
│ DB1 │  │ DB2 │  │ DB3 │  │ DB4 │
└─────┘  └─────┘  └─────┘  └─────┘

- 4+ procesos
- 4+ bases de datos
- 4+ archivos de log
- ¿Cuál falló? ¿Por qué?
```

**Problemas sin observabilidad:**
1. **Efecto dominó**: Un servicio falla y no sabes cuál fue el primero
2. **Latencia distribuida**: Un request toca 10 servicios, ¿cuál es lento?
3. **Logs dispersos**: Los logs están en 20 máquinas diferentes
4. **Debugging imposible**: No puedes reproducir el problema en local

---

## 2. Los Tres Pilares de la Observabilidad

### 2.1 Pilar 1: Métricas (Metrics)

**¿Qué son?**
Representaciones numéricas agregadas del estado del sistema a lo largo del tiempo.

**Características:**
- **Estructura fija**: Siempre tienen la misma forma (timestamp + valor)
- **Bajo overhead**: Muy eficientes de almacenar y consultar
- **Agregables**: Puedes calcular promedios, percentiles, sumas
- **Time-series**: Evolucionan en el tiempo

**Tipos de métricas:**

#### Counter (Contador)
Un valor que solo puede incrementar o resetearse a cero.

```
Ejemplo: Número total de requests HTTP procesados

Valor en el tiempo:
T0: 0
T1: 100
T2: 250
T3: 500

Uso típico:
- Total de requests
- Total de errores
- Total de bytes transferidos
```

#### Gauge (Medidor)
Un valor que puede subir o bajar libremente.

```
Ejemplo: Memoria RAM en uso

Valor en el tiempo:
T0: 512 MB
T1: 768 MB
T2: 620 MB
T3: 890 MB

Uso típico:
- Memoria en uso
- CPU en uso
- Número de conexiones activas
- Temperatura
```

#### Histogram (Histograma)
Distribución de valores observados en buckets predefinidos.

```
Ejemplo: Latencia de requests HTTP

Buckets y observaciones:
[0-50ms]:   ████████████████████ 200 requests
[50-100ms]: ██████████ 100 requests
[100-200ms]:████ 40 requests
[200-500ms]:██ 20 requests
[500ms+]:   █ 10 requests

Permite calcular:
- Percentil 50 (mediana)
- Percentil 95
- Percentil 99
```

#### Summary (Resumen)
Similar al histograma pero calcula percentiles directamente en el cliente.

```
Ejemplo: Latencia de llamadas a base de datos

Percentiles pre-calculados:
P50: 10ms
P90: 25ms
P95: 50ms
P99: 100ms
```

**Analogía de métricas**: Son como el velocímetro, tacómetro y medidor de combustible de un auto. Te dan información numérica instantánea sobre el estado del sistema.

### 2.2 Pilar 2: Logs (Registros)

**¿Qué son?**
Eventos discretos con timestamp que describen lo que sucedió en el sistema.

**Características:**
- **Estructura variable**: Pueden tener diferentes campos
- **Alto nivel de detalle**: Incluyen contexto completo
- **No agregables**: Cada log es único
- **Alto overhead**: Ocupan mucho espacio

**Niveles de log:**

```
TRACE: Información extremadamente detallada (debugging profundo)
DEBUG: Información de debug (desarrollo)
INFO:  Eventos informativos normales
WARN:  Situaciones potencialmente problemáticas
ERROR: Errores que permiten continuar la ejecución
FATAL: Errores críticos que fuerzan el cierre del sistema
```

**Ejemplo de logs estructurados (JSON):**

```json
{
  "timestamp": "2025-10-23T14:32:15.123Z",
  "level": "ERROR",
  "service": "payment-service",
  "trace_id": "abc123",
  "span_id": "def456",
  "message": "Payment declined",
  "error": {
    "type": "InsufficientFundsException",
    "code": "PAYMENT_001",
    "details": "Customer balance: $50, required: $100"
  },
  "context": {
    "customer_id": "CUST-001",
    "order_id": "ORD-12345",
    "payment_method": "CREDIT_CARD"
  }
}
```

**Analogía de logs**: Son como la caja negra de un avión. Registran cada evento importante que sucede, con todo el contexto necesario para reconstruir qué pasó.

### 2.3 Pilar 3: Trazas (Traces)

**¿Qué son?**
Representación del recorrido completo de una request a través de múltiples servicios.

**Estructura de una traza:**

```
Trace ID: abc-123-def-456

┌─────────────────────────────────────────────────────────┐
│ Span 1: HTTP GET /orders/123                            │
│ Service: order-service                                  │
│ Duration: 450ms                                         │
│ └─┬─────────────────────────────────────────────────────┤
│   │                                                     │
│   │ Span 2: HTTP GET /inventory/check                   │
│   │ Service: inventory-service                          │
│   │ Duration: 80ms                                      │
│   │ Parent: Span 1                                      │
│   └─────────────────────────────────────────────────────┤
│   │                                                     │
│   │ Span 3: HTTP POST /payments/process                 │
│   │ Service: payment-service                            │
│   │ Duration: 320ms                                     │
│   │ Parent: Span 1                                      │
│   └─────────────────────────────────────────────────────┤
│   │                                                     │
│   │ Span 4: SQL SELECT FROM accounts                    │
│   │ Service: payment-service                            │
│   │ Duration: 250ms  ← CUELLO DE BOTELLA               │
│   │ Parent: Span 3                                      │
│   └─────────────────────────────────────────────────────┤
└─────────────────────────────────────────────────────────┘
```

**Analogía de trazas**: Son como el GPS de un delivery. Puedes ver el camino exacto que tomó el paquete, cuánto tardó en cada punto, y dónde se atascó en el tráfico.

### 2.4 Comparación de los Tres Pilares

| Aspecto | Métricas | Logs | Trazas |
|---------|----------|------|--------|
| **Pregunta** | ¿Qué tan rápido/cuánto? | ¿Qué pasó exactamente? | ¿Cómo fluyó la request? |
| **Cardinalidad** | Baja | Alta | Media |
| **Costo de almacenamiento** | Bajo | Alto | Medio |
| **Granularidad temporal** | Segundos/Minutos | Milisegundos | Milisegundos |
| **Agregación** | Sí | No | No |
| **Uso principal** | Alertas, tendencias | Debugging, auditoría | Performance, debugging distribuido |
| **Ejemplo** | "CPU al 80%" | "Error: Division by zero at line 42" | "Request tomó 500ms: DB=300ms, API=200ms" |

---

## 3. Métricas con Micrometer y Prometheus

### 3.1 ¿Qué es Micrometer?

**Micrometer** es una fachada de instrumentación para aplicaciones JVM, similar a SLF4J pero para métricas en lugar de logs.

**Analogía**: Micrometer es como un adaptador universal de enchufes. Tu código habla el "lenguaje de Micrometer", pero las métricas pueden exportarse a Prometheus, Graphite, InfluxDB, etc.

```
┌─────────────────────┐
│   Tu Código Java    │
│                     │
│  counter.increment()│
│  timer.record()     │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│    MICROMETER       │ ← Abstracción
│   (Facade Layer)    │
└──────────┬──────────┘
           │
    ┌──────┴──────┬──────────┬────────┐
    ▼             ▼          ▼        ▼
┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐
│Promethe│  │Graphite│  │InfluxDB│  │Datadog │
│  us    │  │        │  │        │  │        │
└────────┘  └────────┘  └────────┘  └────────┘
```

### 3.2 Métricas en Quarkus con Micrometer

#### Configuración básica en application.properties

```properties
# Habilitar Micrometer
quarkus.micrometer.enabled=true

# Habilitar exportador de Prometheus
quarkus.micrometer.export.prometheus.enabled=true

# Endpoint de métricas
quarkus.micrometer.export.prometheus.path=/q/metrics

# Métricas de JVM
quarkus.micrometer.binder.jvm=true

# Métricas de sistema
quarkus.micrometer.binder.system=true

# Métricas HTTP
quarkus.micrometer.binder.http-server.enabled=true
```

#### Creación de métricas custom

```java
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class InventoryService {

    @Inject
    MeterRegistry registry;

    private Counter cacheHitCounter;
    private Counter cacheMissCounter;
    private Timer queryTimer;

    @PostConstruct
    void init() {
        // Counter para cache hits
        cacheHitCounter = Counter.builder("cache.hits")
            .description("Number of cache hits")
            .tag("cache", "redis")
            .tag("type", "product")
            .register(registry);

        // Counter para cache misses
        cacheMissCounter = Counter.builder("cache.misses")
            .description("Number of cache misses")
            .tag("cache", "redis")
            .tag("type", "product")
            .register(registry);

        // Timer para medir duración de queries
        queryTimer = Timer.builder("database.query.duration")
            .description("Database query execution time")
            .tag("operation", "findById")
            .register(registry);
    }

    public Product findById(String id) {
        // Intentar obtener de cache
        Product product = redis.get(id);
        
        if (product != null) {
            cacheHitCounter.increment();
            return product;
        }
        
        // Cache miss - consultar BD
        cacheMissCounter.increment();
        
        // Medir tiempo de query con Timer
        return queryTimer.record(() -> {
            Product result = database.findById(id);
            redis.set(id, result);
            return result;
        });
    }
}
```

### 3.3 ¿Qué es Prometheus?

**Prometheus** es un sistema de monitoreo y base de datos time-series diseñado para recolectar métricas de manera pull-based.

#### Arquitectura de Prometheus

```
┌──────────────────────────────────────────────────┐
│               PROMETHEUS SERVER                  │
│                                                  │
│  ┌──────────────┐      ┌──────────────┐        │
│  │  Retrieval   │      │   Storage    │        │
│  │  (Scraping)  │─────▶│  (TSDB)      │        │
│  └──────────────┘      └──────┬───────┘        │
│         │                      │                 │
│         │                      ▼                 │
│         │              ┌──────────────┐         │
│         │              │   Query      │         │
│         │              │   Engine     │         │
│         │              └──────┬───────┘         │
│         │                     │                 │
└─────────┼─────────────────────┼─────────────────┘
          │                     │
          │                     │
┌─────────▼──────────┐   ┌──────▼────────┐
│  TARGETS           │   │  CONSUMERS    │
│                    │   │               │
│ - Microservicio 1  │   │ - Grafana     │
│ - Microservicio 2  │   │ - Alertmanager│
│ - Microservicio 3  │   │ - API Clients │
└────────────────────┘   └───────────────┘
```

#### Pull vs Push Model

**Push Model (tradicional)**:
```
┌──────────┐          ┌──────────┐
│ Service  │──push───▶│ Monitor  │
└──────────┘          └──────────┘

Problemas:
- El servicio debe saber dónde está el monitor
- Si el monitor está caído, se pierden métricas
- El servicio consume CPU enviando métricas
```

**Pull Model (Prometheus)**:
```
┌──────────┐          ┌──────────┐
│ Service  │◀──pull───│Prometheus│
└──────────┘          └──────────┘

Ventajas:
- El servicio solo expone endpoint HTTP
- Prometheus controla la frecuencia de scraping
- Si Prometheus está caído, el servicio no se ve afectado
- Fácil de escalar horizontalmente
```

#### Configuración de Prometheus (prometheus.yml)

```yaml
global:
  scrape_interval: 15s      # Cada cuánto scrapeamos
  evaluation_interval: 15s  # Cada cuánto evaluamos reglas

# Targets a scrapear
scrape_configs:
  - job_name: 'order-service'
    static_configs:
      - targets: ['localhost:8080']
        labels:
          service: 'order'
          env: 'dev'

  - job_name: 'inventory-service'
    static_configs:
      - targets: ['localhost:8081']
        labels:
          service: 'inventory'
          env: 'dev'

  - job_name: 'payment-service'
    static_configs:
      - targets: ['localhost:8082']
        labels:
          service: 'payment'
          env: 'dev'
```

### 3.4 Formato de Métricas de Prometheus

Las métricas se exponen en formato texto plano:

```
# HELP http_server_requests_seconds Duration of HTTP server requests
# TYPE http_server_requests_seconds histogram
http_server_requests_seconds_bucket{method="GET",uri="/api/products",status="200",le="0.05"} 120
http_server_requests_seconds_bucket{method="GET",uri="/api/products",status="200",le="0.1"} 180
http_server_requests_seconds_bucket{method="GET",uri="/api/products",status="200",le="0.5"} 195
http_server_requests_seconds_bucket{method="GET",uri="/api/products",status="200",le="1.0"} 200
http_server_requests_seconds_bucket{method="GET",uri="/api/products",status="200",le="+Inf"} 200
http_server_requests_seconds_count{method="GET",uri="/api/products",status="200"} 200
http_server_requests_seconds_sum{method="GET",uri="/api/products",status="200"} 18.5

# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="G1 Eden Space"} 2.4576E7
jvm_memory_used_bytes{area="heap",id="G1 Old Gen"} 1.2345E7
jvm_memory_used_bytes{area="nonheap",id="Metaspace"} 4.567E7
```

**Elementos clave:**
- **HELP**: Descripción de la métrica
- **TYPE**: Tipo (counter, gauge, histogram, summary)
- **Labels**: Dimensiones adicionales (method, uri, status, etc.)
- **Value**: Valor numérico de la métrica

---

## 4. Visualización con Grafana

### 4.1 ¿Qué es Grafana?

**Grafana** es una plataforma de visualización y análisis de datos que permite crear dashboards interactivos conectándose a múltiples fuentes de datos.

**Analogía**: Grafana es como Excel + PowerBI pero especializado en time-series y observabilidad. Puedes crear gráficas, tablas, heatmaps, y todo actualizado en tiempo real.

### 4.2 Arquitectura de Grafana

```
┌────────────────────────────────────────────────────┐
│                   GRAFANA                          │
│                                                    │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────┐ │
│  │  Dashboards  │  │    Panels    │  │ Queries │ │
│  └──────┬───────┘  └──────┬───────┘  └────┬────┘ │
│         │                 │                │      │
│         └─────────────────┴────────────────┘      │
│                           │                       │
└───────────────────────────┼───────────────────────┘
                            │
         ┌──────────────────┴──────────────────┐
         │                                     │
    ┌────▼─────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
    │Prometheus│  │ InfluxDB │  │Elasticsea│  │PostgreSQL│
    │          │  │          │  │   rch    │  │          │
    └──────────┘  └──────────┘  └──────────┘  └──────────┘
```

### 4.3 Componentes de un Dashboard

#### Panel
La unidad básica de visualización. Puede ser:
- **Graph**: Línea de tiempo clásica
- **Stat**: Valor único grande
- **Gauge**: Medidor circular o lineal
- **Bar gauge**: Barras horizontales
- **Table**: Tabla de datos
- **Heatmap**: Mapa de calor
- **Logs**: Panel de logs

#### Variables
Permiten crear dashboards dinámicos:

```
Dashboard Variables:
- $service = [order, inventory, payment]
- $environment = [dev, staging, prod]
- $interval = [1m, 5m, 15m]

Query con variables:
rate(http_server_requests_seconds_count{
  job="$service-service",
  env="$environment"
}[$interval])
```

#### Annotations
Marcadores de eventos importantes:

```
Ejemplos:
- "Deploy v1.2.3 - 14:30"
- "Incidente resuelto - 15:45"
- "Aumento de tráfico - 16:00"
```

### 4.4 Dashboards Efectivos: Principios de Diseño

#### Método RED (Rate, Errors, Duration)

Para servicios, siempre monitorea:

```
┌─────────────────────────────────────────┐
│         DASHBOARD: Order Service        │
├─────────────────────────────────────────┤
│                                         │
│  Panel 1: RATE (Requests per second)   │
│  ▲                                      │
│  │     ╱╲                               │
│  │    ╱  ╲    ╱╲                        │
│  │   ╱    ╲  ╱  ╲                       │
│  │──╱──────╲╱────╲──────────────────▶   │
│                                         │
│  Panel 2: ERRORS (Error rate %)        │
│  ▲                                      │
│  │                  ╱╲                  │
│  │                 ╱  ╲                 │
│  │                ╱    ╲                │
│  │───────────────╱──────╲───────────▶   │
│                                         │
│  Panel 3: DURATION (Latency P95)       │
│  ▲                                      │
│  │         ╱────╲                       │
│  │        ╱      ╲                      │
│  │   ────╱        ╲─────                │
│  │────────────────────────────────▶     │
│                                         │
└─────────────────────────────────────────┘
```

#### Método USE (Utilization, Saturation, Errors)

Para recursos (CPU, memoria, disco):

```
┌─────────────────────────────────────────┐
│      DASHBOARD: System Resources        │
├─────────────────────────────────────────┤
│                                         │
│  Panel 1: CPU Utilization (%)          │
│  Panel 2: Memory Utilization (%)       │
│  Panel 3: Disk I/O Saturation          │
│  Panel 4: Network Errors               │
│                                         │
└─────────────────────────────────────────┘
```

### 4.5 Alertas en Grafana

Grafana puede enviar alertas cuando las métricas cruzan umbrales:

```yaml
Alert: High Error Rate

Condition:
  WHEN avg() OF query(A, 5m, now)
  IS ABOVE 0.05

Evaluate every: 1m
For: 5m

Notifications:
  - Slack: #alerts
  - Email: oncall@company.com
  - PagerDuty: escalation-policy-1

Message:
  "🚨 Error rate is {{ $value }}% on {{ $labels.service }}"
```

---

## 5. Logs Centralizados con Elastic Stack

### 5.1 ¿Qué es Elastic Stack (ELK)?

**Elastic Stack** (anteriormente ELK Stack) es un conjunto de herramientas para la gestión centralizada de logs.

**Componentes:**
- **E**lasticsearch: Motor de búsqueda y almacenamiento
- **L**ogstash: Procesador y transformador de logs
- **K**ibana: Interfaz de visualización
- **B**eats: Agentes ligeros de recolección

### 5.2 Arquitectura de Elastic Stack

```
┌──────────────────────────────────────────────────────┐
│              MICROSERVICIOS                          │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐             │
│  │Order Svc│  │Inventory│  │Payment  │             │
│  │         │  │  Svc    │  │  Svc    │             │
│  └────┬────┘  └────┬────┘  └────┬────┘             │
│       │            │            │                   │
│       │  Escriben logs a archivo                    │
│       │            │            │                   │
│       ▼            ▼            ▼                   │
│  order.log   inventory.log  payment.log            │
└───────┬────────────┬────────────┬───────────────────┘
        │            │            │
        │     ┌──────▼──────┐     │
        │     │  FILEBEAT   │◀────┘
        │     │  (Shipper)  │
        │     └──────┬──────┘
        │            │
        └────────────┼────────────┘
                     │
                     ▼
              ┌──────────────┐
              │   LOGSTASH   │ ← Parsing, filtering, enrichment
              │  (Pipeline)  │
              └──────┬───────┘
                     │
                     ▼
              ┌──────────────┐
              │ELASTICSEARCH │ ← Indexing & Storage
              │   (Store)    │
              └──────┬───────┘
                     │
                     ▼
              ┌──────────────┐
              │    KIBANA    │ ← Visualization & Search
              │    (UI)      │
              └──────────────┘
```

### 5.3 Elasticsearch: El Motor de Búsqueda

#### ¿Qué es Elasticsearch?

Un motor de búsqueda y análisis distribuido basado en **Apache Lucene**.

**Conceptos clave:**

```
┌─────────────────────────────────────────┐
│         ELASTICSEARCH CLUSTER           │
│                                         │
│  ┌────────────────────────────────┐    │
│  │         INDEX: quarkus-logs    │    │
│  │                                │    │
│  │  ┌──────────┐  ┌──────────┐   │    │
│  │  │  Shard 0 │  │  Shard 1 │   │    │
│  │  │ (Primary)│  │ (Primary)│   │    │
│  │  └──────────┘  └──────────┘   │    │
│  │                                │    │
│  │  ┌──────────┐  ┌──────────┐   │    │
│  │  │  Shard 0 │  │  Shard 1 │   │    │
│  │  │ (Replica)│  │ (Replica)│   │    │
│  │  └──────────┘  └──────────┘   │    │
│  └────────────────────────────────┘    │
└─────────────────────────────────────────┘

Index = Base de datos
Shard = Partición de datos
Replica = Copia de respaldo
Document = Registro individual (log)
```

#### Ejemplo de documento indexado

```json
{
  "_index": "quarkus-logs-2025.10.23",
  "_id": "abc123",
  "_score": 1.0,
  "_source": {
    "@timestamp": "2025-10-23T14:30:15.123Z",
    "level": "ERROR",
    "service_name": "payment-service",
    "logger_name": "pe.banco.payment.service.PaymentService",
    "message": "Payment processing failed",
    "exception": {
      "class": "pe.banco.payment.exception.InsufficientFundsException",
      "message": "Insufficient funds",
      "stacktrace": "..."
    },
    "trace_id": "abc-def-ghi",
    "span_id": "123-456",
    "customer_id": "CUST-001",
    "order_id": "ORD-789",
    "amount": 100.00
  }
}
```

### 5.4 Logstash: El Pipeline de Procesamiento

Logstash procesa logs en tres etapas:

```
┌─────────┐    ┌─────────┐    ┌─────────┐
│ INPUT   │───▶│ FILTER  │───▶│ OUTPUT  │
└─────────┘    └─────────┘    └─────────┘
```

#### Configuración de Logstash

```ruby
# logstash.conf

input {
  # Recibir logs de Filebeat
  beats {
    port => 5044
  }
}

filter {
  # Parsear logs JSON
  if [message] =~ /^{.*}$/ {
    json {
      source => "message"
    }
  }

  # Extraer información del logger_name
  if [logger_name] {
    grok {
      match => {
        "logger_name" => "pe\.banco\.(?<service_component>\w+)\..*"
      }
    }
  }

  # Enriquecer con geolocalización (si hay IP)
  if [client_ip] {
    geoip {
      source => "client_ip"
      target => "geoip"
    }
  }

  # Agregar tags basados en nivel de log
  if [level] == "ERROR" or [level] == "FATAL" {
    mutate {
      add_tag => ["alert"]
    }
  }

  # Calcular duración si existe
  if [duration_ms] {
    ruby {
      code => "event.set('duration_seconds', event.get('duration_ms') / 1000.0)"
    }
  }
}

output {
  # Enviar a Elasticsearch
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "quarkus-logs-%{+YYYY.MM.dd}"
  }

  # Debug: imprimir en stdout (opcional)
  # stdout { codec => rubydebug }
}
```

### 5.5 Kibana: Visualización y Búsqueda

#### Discover: Exploración de Logs

Kibana Discover permite:
1. **Buscar logs** con query language
2. **Filtrar** por campos
3. **Ver contexto** alrededor de un log
4. **Crear visualizaciones** desde búsquedas

#### Kibana Query Language (KQL)

```
# Búsquedas básicas
level: ERROR
service_name: "order-service"

# Operadores lógicos
level: ERROR AND service_name: "order-service"
level: ERROR OR level: FATAL

# Rangos numéricos
duration_ms > 1000
status_code >= 500 AND status_code < 600

# Wildcards
message: *timeout*
customer_id: CUST-*

# Negación
NOT level: DEBUG
NOT service_name: "inventory-service"

# Campos anidados
exception.class: "InsufficientFundsException"
geoip.country_name: "United States"

# Búsqueda de texto completo
message: "payment failed"

# Existe campo
_exists_: exception
NOT _exists_: trace_id
```

#### Visualizaciones en Kibana

```
Tipos de visualizaciones:
┌────────────────────────────────────┐
│ 1. Vertical Bar Chart              │
│    - Logs por nivel en el tiempo   │
│                                    │
│ 2. Line Chart                      │
│    - Tendencia de errores          │
│                                    │
│ 3. Pie Chart                       │
│    - Distribución por servicio     │
│                                    │
│ 4. Data Table                      │
│    - Top 10 errores más comunes    │
│                                    │
│ 5. Tag Cloud                       │
│    - Palabras más frecuentes       │
│                                    │
│ 6. Metric                          │
│    - Total de errores (número)     │
└────────────────────────────────────┘
```

---

## 6. Identificación de Cuellos de Botella

### 6.1 ¿Qué es un Cuello de Botella?

**Definición**: Un punto en el sistema que limita el rendimiento general, como el cuello estrecho de una botella limita el flujo de líquido.

**Analogía del supermercado**:
```
Entrada ─────▶ Pasillos ─────▶ Cajas ─────▶ Salida
(rápido)       (rápido)       (LENTO)       (bloqueado)
                                  ▲
                            CUELLO DE BOTELLA
```

Si tienes 10 cajas registradoras pero solo 2 están abiertas, no importa qué tan rápido la gente encuentre sus productos - todos esperarán en las cajas.

### 6.2 Metodología para Identificar Cuellos de Botella

#### Paso 1: Medir la Latencia End-to-End

**En Grafana:**
```promql
# Latencia P95 de cada endpoint
histogram_quantile(0.95,
  sum by (uri) (
    rate(http_server_requests_seconds_bucket[5m])
  )
)
```

**Resultado esperado:**
```
/api/orders/create     → 850ms ← LENTO
/api/orders/list       → 50ms
/api/products/search   → 80ms
/api/payments/process  → 120ms
```

#### Paso 2: Descomponer la Latencia

**En Kibana, buscar logs del endpoint lento:**
```
uri: "/api/orders/create" AND level: DEBUG
```

**Analizar los tiempos:**
```
2025-10-23T14:30:00.000Z DEBUG Iniciando creación de orden
2025-10-23T14:30:00.050Z DEBUG Validando inventario...       [50ms]
2025-10-23T14:30:00.750Z DEBUG Reservando inventario...      [700ms] ← CUELLO DE BOTELLA
2025-10-23T14:30:00.800Z DEBUG Procesando pago...            [50ms]
2025-10-23T14:30:00.850Z DEBUG Orden creada exitosamente     [50ms]
```

#### Paso 3: Analizar la Operación Lenta

**Posibles causas de "Reservando inventario" tarda 700ms:**

1. **Query SQL lento** (falta índice)
2. **N+1 queries** (consulta en loop)
3. **Lock de base de datos** (transacción bloqueada)
4. **Network latency** (BD en otra región)
5. **Cache miss** (debería estar en Redis)

**Verificar con métricas:**
```promql
# Ver queries a la BD
rate(jdbc_connections_active[1m])

# Ver latencia de queries SQL
histogram_quantile(0.95,
  rate(jdbc_query_seconds_bucket{operation="reserve_inventory"}[5m])
)
```

### 6.3 Técnicas de Optimización

#### Técnica 1: Indexación de Base de Datos

**Problema detectado:**
```sql
-- Query lento (full table scan)
SELECT * FROM products WHERE product_code = 'LAPTOP-001';

-- Explain muestra:
-- Seq Scan on products (cost=0.00..100.00 rows=5000 width=200)
```

**Solución:**
```sql
-- Crear índice
CREATE INDEX idx_products_code ON products(product_code);

-- Ahora:
-- Index Scan using idx_products_code (cost=0.00..8.27 rows=1 width=200)
```

**Resultado:**
- Antes: 700ms
- Después: 50ms
- **Mejora: 14x más rápido**

#### Técnica 2: Implementar Cache

**Problema:**
```
Cada request consulta BD:
Request 1 → BD → 50ms
Request 2 → BD → 50ms
Request 3 → BD → 50ms
...
Request 100 → BD → 50ms

Total: 5000ms para 100 requests
```

**Solución con Redis:**
```
Request 1 → BD → Redis → 50ms (cache miss)
Request 2 → Redis → 5ms (cache hit)
Request 3 → Redis → 5ms (cache hit)
...
Request 100 → Redis → 5ms (cache hit)

Total: 545ms para 100 requests
```

**Resultado: 9x más rápido**

#### Técnica 3: Procesamiento Asíncrono

**Problema:**
```
POST /orders/create

[Cliente espera 5 segundos]

1. Validar inventario     (1s)
2. Procesar pago          (2s)
3. Enviar email           (1s) ← No crítico
4. Notificar warehouse    (1s) ← No crítico

Response: 200 OK
```

**Solución:**
```
POST /orders/create

[Cliente espera 3 segundos]

1. Validar inventario     (1s)
2. Procesar pago          (2s)

Response: 202 Accepted

[En background - async]
3. Enviar email           (1s)
4. Notificar warehouse    (1s)
```

**Resultado: 40% más rápido (perceived)**

### 6.4 Ley de Amdahl

**Teorema**: El speed-up máximo está limitado por la porción secuencial del código.

```
Speedup = 1 / ((1 - P) + P/N)

P = Porción paralelizable
N = Número de procesadores
```

**Ejemplo práctico:**

Si el 90% de tu código es paralelizable:
```
1 CPU:   1x speedup
2 CPUs:  1.8x speedup
4 CPUs:  3.1x speedup
8 CPUs:  4.7x speedup
16 CPUs: 6.4x speedup
∞ CPUs:  10x speedup (límite)
```

**Lección**: Optimiza primero la parte secuencial (el 10% en este caso) antes de agregar más CPUs.

---

## 7. Detección de Patrones de Error

### 7.1 Clasificación de Errores

#### Errores Transitorios (Transient Errors)

**Características:**
- Ocurren temporalmente
- Se resuelven solos o con retry
- No requieren cambios en el código

**Ejemplos:**
```
- Network timeout
- Connection pool exhausted
- Deadlock en BD (se resuelve al reintentar)
- Rate limit exceeded (esperar y reintentar)
```

**Patrón en Grafana:**
```
Error Rate
   ▲
   │    ╱╲
   │   ╱  ╲
   │  ╱    ╲
   │─╱──────╲─────────────▶ Tiempo
   │
   └─ Pico breve, luego vuelve a 0%
```

**Solución:**
```java
@Retry(maxRetries = 3, delay = 1000)
@Fallback(fallbackMethod = "paymentFallback")
public PaymentResponse processPayment(PaymentRequest request) {
    // Código que puede fallar temporalmente
}
```

#### Errores Permanentes (Permanent Errors)

**Características:**
- No se resuelven solos
- Requieren intervención humana
- Indican bugs o problemas de configuración

**Ejemplos:**
```
- NullPointerException
- ArrayIndexOutOfBoundsException
- SQL syntax error
- Missing configuration property
```

**Patrón en Grafana:**
```
Error Rate
   ▲
   │         ╱────────────
   │        ╱
   │       ╱
   │──────╱──────────────▶ Tiempo
   │
   └─ Sube y se mantiene alto
```

**Solución:**
- Fix del bug
- Deploy de hotfix
- Rollback a versión anterior

#### Errores en Cascada (Cascading Failures)

**Características:**
- Un servicio falla y provoca falla en otros
- Efecto dominó
- Puede colapsar todo el sistema

**Ejemplo:**
```
T0: Payment Service falla (error rate 100%)
    ↓
T1: Order Service empieza a fallar (llama a Payment)
    ↓
T2: Frontend muestra errores a usuarios
    ↓
T3: Usuarios recargan página (más carga)
    ↓
T4: Sistema colapsa completamente
```

**Patrón en Grafana:**
```
Error Rate por Servicio

Payment:    ████████████████████████ 100%
            T0 →
Order:           ████████████████████ 100%
                 T1 →
Frontend:             ██████████████ 90%
                      T2 →
```

**Solución: Circuit Breaker**
```java
@CircuitBreaker(
    requestVolumeThreshold = 10,
    failureRatio = 0.5,
    delay = 5000
)
public PaymentResponse processPayment() {
    // Si falla 50% de 10 requests
    // → Circuit OPEN (no llamar por 5 segundos)
}
```

### 7.2 Técnicas de Detección en Kibana

#### Pattern 1: Spike de Errores

**Query en Kibana:**
```
level: ERROR
```

**Visualización:**
- Date Histogram (1 minuto)
- Ver picos inusuales

**Análisis:**
```
Normal:    ▁▁▂▁▁▂▁▁▁▂▁
Spike:     ▁▁▁▁█▁▁▁▁▁▁
                ↑
           ¿Qué pasó aquí?
```

**Correlacionar con:**
- Deploys
- Cambios de configuración
- Aumento de tráfico
- Caídas de servicios externos

#### Pattern 2: Error Recurrente

**Query en Kibana:**
```
level: ERROR AND exception.class: "InsufficientFundsException"
```

**Agrupar por:**
- Customer ID
- Order ID
- Payment method

**Descubrimiento:**
```
Customer CUST-001: 50 errores en 1 hora
Customer CUST-002: 2 errores en 1 hora
Customer CUST-003: 1 error en 1 hora

→ CUST-001 está haciendo algo mal (bot? retry loop?)
```

#### Pattern 3: Degradación Gradual

**Query en Kibana:**
```
message: *slow* OR message: *timeout*
```

**Visualización:**
- Trend line de duración promedio

**Análisis:**
```
Duración Promedio
   ▲
   │               ╱
   │              ╱
   │          ╱╱╱
   │    ╱╱╱╱
   │╱╱╱──────────────────▶ Tiempo
   │
   └─ Performance empeorando gradualmente
```

**Posibles causas:**
- Memory leak
- Tabla de BD creciendo sin índices
- Cache inefectivo
- Conexiones de BD no liberadas

### 7.3 Correlación de Eventos

#### Técnica: Buscar Causa Raíz

**Paso 1: Identificar el primer error**
```
Kibana Query:
level: ERROR
Sort by: @timestamp (ascending)
```

**Paso 2: Ver contexto temporal**
```
- ¿Qué pasó 5 minutos antes?
- ¿Hay logs de WARNING?
- ¿Hay cambios en métricas?
```

**Paso 3: Seguir el trace_id**
```
trace_id: "abc-123-def"

Resultado:
2025-10-23T14:30:00.000Z order-service    INFO  Request received
2025-10-23T14:30:00.050Z inventory-service INFO  Checking inventory
2025-10-23T14:30:00.100Z inventory-service ERROR Database timeout ← CAUSA RAÍZ
2025-10-23T14:30:05.000Z order-service    ERROR Inventory check failed
```

### 7.4 Alertas Inteligentes

#### Alerta Básica (Umbral Estático)

```yaml
Alert: High Error Rate

Condition:
  error_rate > 5%

Problema:
  - Puede haber falsos positivos en bajo tráfico
  - No detecta cambios relativos
```

#### Alerta Avanzada (Anomalía)

```yaml
Alert: Anomalous Error Rate

Condition:
  error_rate > (avg_last_7_days + 3 * stddev)

Ventaja:
  - Se adapta al patrón normal
  - Detecta cambios relativos
```

#### Alerta Predictiva (Machine Learning)

```
Usa ML para predecir:
- "La memoria se llenará en 2 horas"
- "El disco alcanzará 90% en 30 minutos"

Permite: Acción proactiva antes del problema
```

---

## 8. PromQL: El Lenguaje de Prometheus

### 8.1 Sintaxis Básica

#### Seleccionar una Métrica

```promql
# Todas las series de tiempo de esta métrica
http_server_requests_seconds_count

# Filtrar por labels
http_server_requests_seconds_count{job="order-service"}

# Múltiples labels
http_server_requests_seconds_count{
  job="order-service",
  method="GET",
  uri="/api/orders"
}

# Regex en labels
http_server_requests_seconds_count{
  status=~"5.."  # 500, 501, 502, etc.
}
```

### 8.2 Funciones de Agregación

#### rate() - Tasa por segundo

```promql
# Calcula requests por segundo en los últimos 5 minutos
rate(http_server_requests_seconds_count[5m])

# ¿Cómo funciona?
# Toma el valor al final - valor al inicio
# Divide por el tiempo transcurrido
# 
# Ejemplo:
# t=0:  counter=100
# t=5m: counter=400
# rate = (400-100) / 300s = 1 request/sec
```

#### sum() - Suma

```promql
# Total de requests en todos los servicios
sum(rate(http_server_requests_seconds_count[5m]))

# Total por servicio
sum by (job) (rate(http_server_requests_seconds_count[5m]))
```

#### avg() - Promedio

```promql
# Latencia promedio
avg(http_server_requests_seconds_sum / http_server_requests_seconds_count)
```

#### max() / min() - Máximo / Mínimo

```promql
# Memoria máxima usada por cualquier servicio
max(jvm_memory_used_bytes{area="heap"})

# Memoria mínima usada
min(jvm_memory_used_bytes{area="heap"})
```

### 8.3 Percentiles con histogram_quantile()

```promql
# Latencia P50 (mediana)
histogram_quantile(0.50,
  rate(http_server_requests_seconds_bucket[5m])
)

# Latencia P95
histogram_quantile(0.95,
  rate(http_server_requests_seconds_bucket[5m])
)

# Latencia P99
histogram_quantile(0.99,
  rate(http_server_requests_seconds_bucket[5m])
)

# ¿Por qué usar P95 en lugar de promedio?
# Promedio: 50ms (parece bien)
# P95: 500ms (¡5% de usuarios tienen mala experiencia!)
```

### 8.4 Operaciones Matemáticas

```promql
# Error rate (porcentaje)
rate(http_server_requests_seconds_count{status=~"5.."}[5m])
/
rate(http_server_requests_seconds_count[5m])
* 100

# Cache hit rate
cache_hits_total
/
(cache_hits_total + cache_misses_total)
* 100

# Memoria disponible (GB)
(jvm_memory_max_bytes - jvm_memory_used_bytes) / 1024^3
```

### 8.5 Queries Útiles para Copiar

```promql
# ===== MÉTRICAS DE SERVICIO =====

# Requests por segundo por servicio
sum by (job) (rate(http_server_requests_seconds_count[1m]))

# Latencia P95 por endpoint
histogram_quantile(0.95,
  sum by (uri, le) (rate(http_server_requests_seconds_bucket[5m]))
)

# Error rate por servicio
sum by (job) (rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
/
sum by (job) (rate(http_server_requests_seconds_count[5m]))

# ===== MÉTRICAS DE JVM =====

# Heap memory usado (%)
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100

# GC pause time (ms)
rate(jvm_gc_pause_seconds_sum[5m]) * 1000

# Threads activos
jvm_threads_live_threads

# ===== MÉTRICAS DE BD =====

# Conexiones activas a PostgreSQL
jdbc_connections_active{pool="HikariPool-1"}

# Tiempo de espera por conexión (ms)
rate(jdbc_connections_wait_seconds_sum[5m]) * 1000

# ===== MÉTRICAS DE CACHE =====

# Redis cache hit rate
redis_cache_hits_total / (redis_cache_hits_total + redis_cache_misses_total)

# ===== MÉTRICAS DE SISTEMA =====

# CPU usage (%)
system_cpu_usage * 100

# Disk usage (%)
(disk_total_bytes - disk_free_bytes) / disk_total_bytes * 100
```

---

## 9. Kibana Query Language (KQL)

### 9.1 Sintaxis Básica

```
# Búsqueda simple
error

# Campo específico
level: ERROR

# Frase exacta
message: "payment failed"

# Wildcard
customer_id: CUST-*

# Rango numérico
duration_ms > 1000
status_code >= 500 AND status_code < 600

# Existe campo
_exists_: exception

# No existe campo
NOT _exists_: trace_id
```

### 9.2 Operadores Lógicos

```
# AND
level: ERROR AND service_name: "order-service"

# OR
level: ERROR OR level: FATAL

# NOT
NOT level: DEBUG

# Agrupación con paréntesis
(level: ERROR OR level: FATAL) AND service_name: "payment-service"
```

### 9.3 Queries Útiles

```
# ===== ERRORES =====

# Todos los errores
level: ERROR

# Errores de un servicio específico
level: ERROR AND service_name: "payment-service"

# Errores con exception
level: ERROR AND _exists_: exception

# Top errores por tipo
exception.class: *

# ===== PERFORMANCE =====

# Requests lentos (>1 segundo)
duration_ms > 1000

# Timeouts
message: *timeout* OR message: *timed out*

# ===== SAGA =====

# Compensaciones SAGA
tags: saga-compensation

# SAGA fallidas
tags: saga AND level: ERROR

# ===== CACHE =====

# Cache hits
message: "Cache HIT"

# Cache misses
message: "Cache MISS"

# ===== BÚSQUEDA POR CONTEXTO =====

# Logs de una orden específica
order_id: "ORD-12345"

# Logs de un cliente específico
customer_id: "CUST-001"

# Logs de una traza distribuida
trace_id: "abc-123-def"
```

---

## 10. Mejores Prácticas de Observabilidad

### 10.1 Structured Logging

❌ **MAL:**
```java
logger.info("Order created with id " + orderId + " for customer " + customerId);
```

✅ **BIEN:**
```java
logger.info("Order created",
    kv("order_id", orderId),
    kv("customer_id", customerId),
    kv("amount", order.getTotalAmount())
);
```

**Resultado en JSON:**
```json
{
  "message": "Order created",
  "order_id": "ORD-123",
  "customer_id": "CUST-001",
  "amount": 99.99
}
```

**Ventajas:**
- Fácil de buscar en Kibana: `order_id: "ORD-123"`
- Fácil de agregar: Count by customer_id
- No depende de parsing de texto

### 10.2 Semantic Logging

Usa **niveles de log** apropiados:

```java
// TRACE: Información muy detallada (solo desarrollo)
logger.trace("Entering method calculateTotal()");

// DEBUG: Información útil para debugging
logger.debug("Cache miss for product {}", productCode);

// INFO: Eventos importantes del negocio
logger.info("Order created successfully", kv("order_id", orderId));

// WARN: Situaciones inusuales pero manejables
logger.warn("Inventory low for product {}", productCode);

// ERROR: Errores que permiten continuar
logger.error("Failed to send email notification", exception);

// FATAL: Errores críticos (sistema no puede continuar)
logger.fatal("Database connection pool exhausted");
```

### 10.3 Correlation IDs

**Siempre propagar trace_id y span_id:**

```
Request 1: Order Service
  trace_id: abc-123
  span_id: span-1
  
  ↓ Llama a Inventory Service
  
  trace_id: abc-123  (mismo!)
  span_id: span-2
  parent_span_id: span-1
  
  ↓ Llama a Payment Service
  
  trace_id: abc-123  (mismo!)
  span_id: span-3
  parent_span_id: span-1
```

**En Kibana:**
```
trace_id: "abc-123"

Resultado: Todos los logs de esta request a través de los 3 servicios
```

### 10.4 Métricas con Contexto

❌ **MAL:**
```java
counter.increment();
```

✅ **BIEN:**
```java
counter.tag("operation", "reserve_inventory")
       .tag("product_type", "electronics")
       .tag("success", "true")
       .increment();
```

**Ventajas en Grafana:**
```promql
# Puedes filtrar por contexto
sum by (operation) (inventory_operations_total)

# Comparar success vs failure
sum(inventory_operations_total{success="true"})
/
sum(inventory_operations_total)
```

### 10.5 SLIs, SLOs y SLAs

#### SLI (Service Level Indicator)

**Definición**: Métrica que mide el nivel de servicio.

**Ejemplos:**
```
- Latencia P95 < 500ms
- Error rate < 0.1%
- Availability > 99.9%
```

#### SLO (Service Level Objective)

**Definición**: Target interno para un SLI.

**Ejemplo:**
```
SLO: "El 95% de las requests deben completarse en menos de 500ms"

Query PromQL:
histogram_quantile(0.95,
  rate(http_server_requests_seconds_bucket[5m])
) < 0.5
```

#### SLA (Service Level Agreement)

**Definición**: Contrato con consecuencias si no se cumple.

**Ejemplo:**
```
SLA: "99.9% uptime mensual"

Si incumplimos: Crédito de 10% a clientes
```

#### Error Budget

```
SLO: 99.9% availability = 0.1% error budget

Cálculo:
- Mes: 30 días = 43,200 minutos
- Error budget: 43.2 minutos de downtime permitidos

Tracking:
- Semana 1: 5 minutos de downtime (quedan 38.2 min)
- Semana 2: 10 minutos de downtime (quedan 28.2 min)
- Semana 3: 30 minutos de downtime (quedan -1.8 min) ← EXCEEDED!

Acción: Freeze deploys, focus en stability
```

---

## 11. Antipatrones Comunes

### 11.1 Alert Fatigue

❌ **Problema:**
```
Alertas cada 5 minutos:
- CPU > 80%
- Memory > 80%
- Disk > 80%
- Error rate > 1%
- Latency > 100ms

Resultado: Ingeniero ignora todas las alertas
```

✅ **Solución:**
```
Alertas solo para:
- SLO violado durante 5 minutos
- Error budget exhausted
- Incidente crítico

Resultado: Cada alerta es importante
```

### 11.2 Vanity Metrics

❌ **Métricas inútiles:**
```
- Total de usuarios registrados (no dice si están activos)
- Total de requests (no dice si son exitosos)
- Latencia promedio (oculta outliers)
```

✅ **Métricas accionables:**
```
- Usuarios activos diarios (DAU)
- Success rate (%)
- Latencia P95/P99 (experiencia real)
```

### 11.3 Logging Everything

❌ **Problema:**
```java
logger.debug("Entering method");
logger.debug("Parameter x = " + x);
logger.debug("Calling database");
logger.debug("Database returned");
logger.debug("Processing result");
logger.debug("Exiting method");
```

**Costo:**
- Disco lleno en horas
- Elasticsearch colapsa
- Imposible encontrar logs importantes

✅ **Solución:**
```java
// Solo log eventos importantes
logger.info("Order processing started", kv("order_id", orderId));

try {
    result = processOrder(order);
    logger.info("Order processed successfully", kv("order_id", orderId));
} catch (Exception e) {
    logger.error("Order processing failed", kv("order_id", orderId), e);
}
```

### 11.4 Dashboards Incomprensibles

❌ **Dashboard malo:**
```
- 50 paneles en un dashboard
- Gráficas sin título
- Ejes sin unidades
- Sin anotaciones de eventos
- Colores aleatorios
```

✅ **Dashboard bueno:**
```
- 5-10 paneles por dashboard
- Títulos descriptivos
- Unidades claras (ms, GB, %)
- Anotaciones de deploys
- Colores con significado (rojo=mal, verde=bien)
```

---

## 12. Casos de Uso Reales

### 12.1 Caso: Netflix

**Problema:**
Sistema distribuido con miles de microservicios. Difícil saber dónde están los problemas.

**Solución:**
- **Atlas**: Sistema de métricas custom basado en time-series
- **Spectator**: Librería para instrumentación
- **Vizceral**: Visualización de tráfico en tiempo real

**Resultado:**
```
┌─────────────────────────────────────┐
│   Vizceral Dashboard                │
│                                     │
│   [Frontend] ───▶ [API Gateway]    │
│        │                │           │
│        │                ▼           │
│        │         [Recommendations] │
│        │                │           │
│        ▼                ▼           │
│   [Content] ◀──── [Personalization]│
│                                     │
│   Color del edge = Health           │
│   - Verde: OK                       │
│   - Amarillo: Warning               │
│   - Rojo: Critical                  │
└─────────────────────────────────────┘
```

**Lección:** Visualización en tiempo real ayuda a detectar problemas instantáneamente.

### 12.2 Caso: Uber

**Problema:**
Miles de eventos por segundo. Debugging de un ride específico es imposible.

**Solución:**
- **Jaeger**: Distributed tracing
- Cada request tiene trace_id único
- Logs estructurados con trace_id

**Resultado:**
```
Uber Engineer busca en Jaeger:
trace_id: "ride-123"

Ve:
1. Request recibido en API
2. Búsqueda de drivers cercanos (100ms)
3. Cálculo de ETA (50ms)
4. Cálculo de precio (200ms) ← LENTO
5. Match driver-rider (30ms)

Identifica: Servicio de pricing es el cuello de botella
```

**Lección:** Distributed tracing es esencial para debugging en sistemas complejos.

### 12.3 Caso: Amazon Prime Day

**Problema:**
Tráfico 10x normal durante Prime Day. Sistema colapsa cada año.

**Solución:**
- **Load testing** previo con tráfico realista
- **Auto-scaling** agresivo
- **Circuit breakers** en todos los servicios
- **Chaos engineering** (simular fallos antes del evento)

**Métricas clave monitoreadas:**
```
- Requests per second
- Latency P99
- Error rate
- Database connections
- Cache hit rate
- Auto-scaling events
```

**Resultado:**
```
Prima Day 2023:
- 375 millones de items vendidos
- Pico de 1 millón de requests/segundo
- 99.99% uptime
- 0 incidents críticos
```

**Lección:** Observabilidad + preparación = resiliencia.

---

## 📚 Conclusión

La observabilidad no es un lujo - es una **necesidad** en arquitecturas de microservicios.

### Puntos Clave

1. **Los 3 pilares** (métricas, logs, trazas) son complementarios
2. **Micrometer + Prometheus** para métricas eficientes
3. **Grafana** para visualización intuitiva
4. **ELK Stack** para logs centralizados y searchables
5. **Cuellos de botella** se identifican con métricas + logs
6. **Patrones de error** requieren correlación de eventos

### Siguiente Nivel

Para llevar tu observabilidad al siguiente nivel:

1. **Implementa distributed tracing** con OpenTelemetry + Jaeger
2. **Define SLOs** para tus servicios críticos
3. **Automatiza alertas** basadas en error budget
4. **Practica chaos engineering** para validar resiliencia
5. **Construye runbooks** para cada alerta

---

**"You can't improve what you don't measure."** - Peter Drucker

La observabilidad te da los ojos para ver, los oídos para escuchar, y el cerebro para entender qué está pasando en tu sistema distribuido.

---

## 📖 Referencias y Recursos

### Libros
- **"Distributed Systems Observability"** - Cindy Sridharan (O'Reilly)
- **"The Art of Monitoring"** - James Turnbull
- **"Site Reliability Engineering"** - Google (SRE Book)

### Documentación Oficial
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Elastic Stack Documentation](https://www.elastic.co/guide/)
- [Quarkus Micrometer Guide](https://quarkus.io/guides/micrometer)

### Cursos y Tutoriales
- [PromQL Cheat Sheet](https://promlabs.com/promql-cheat-sheet/)
- [Grafana Fundamentals](https://grafana.com/tutorials/)
- [Elasticsearch Fundamentals](https://www.elastic.co/training/)

### Papers Académicos
- **"Dapper: A Large-Scale Distributed Systems Tracing Infrastructure"** - Google (2010)
- **"Monarch: Google's Planet-Scale In-Memory Time Series Database"** - Google (2020)

---

**Fin del documento teórico. Practica con el ejercicio hands-on para consolidar estos conceptos.**