# 📊 Guía de Monitoreo con Grafana, Prometheus y ELK Stack

## Capítulo 10_1: SAGA + Redis + Observabilidad

---

## 📋 Tabla de Contenidos

- [Introducción](#introducción)
- [¿Qué es Observabilidad?](#qué-es-observabilidad)
- [Stack de Monitoreo](#stack-de-monitoreo)
- [Instalación y Configuración](#instalación-y-configuración)
- [Uso de Prometheus](#uso-de-prometheus)
- [Uso de Grafana](#uso-de-grafana)
- [Uso de ELK Stack](#uso-de-elk-stack)
- [Métricas Clave](#métricas-clave)
- [Identificar Cuellos de Botella](#identificar-cuellos-de-botella)
- [Patrones de Error](#patrones-de-error)
- [Troubleshooting](#troubleshooting)

---

## 🎯 Introducción

Este capítulo extiende el ejercicio de SAGA + Redis Cache agregando un **stack completo de observabilidad** para monitorear el comportamiento de los microservicios en tiempo real.

### ¿Qué aprenderás?

- ✅ Configurar **Micrometer** para exponer métricas desde Quarkus
- ✅ Usar **Prometheus** para recolectar y almacenar métricas
- ✅ Crear **dashboards en Grafana** para visualización
- ✅ Implementar **ELK Stack** (Elasticsearch + Logstash + Kibana) para logs centralizados
- ✅ Identificar **cuellos de botella** y patrones de rendimiento
- ✅ Detectar y analizar **patrones de error**
- ✅ Monitorear métricas de **SAGA** (compensaciones, circuit breaker)
- ✅ Monitorear métricas de **Redis Cache** (hit rate)

---

## 🔍 ¿Qué es Observabilidad?

**Observabilidad** es la capacidad de entender el estado interno de un sistema basándose únicamente en sus salidas externas.

### Los 3 Pilares de la Observabilidad

```
┌─────────────────────────────────────────────────────────┐
│                   OBSERVABILIDAD                        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  1️⃣  MÉTRICAS (Metrics)                                │
│     • ¿Qué tan rápido? ¿Cuánto?                        │
│     • Prometheus + Grafana                             │
│     • Ejemplo: RPS, latencia, error rate               │
│                                                         │
│  2️⃣  LOGS (Logs)                                        │
│     • ¿Qué pasó? ¿Cuándo?                             │
│     • ELK Stack (Elasticsearch + Logstash + Kibana)    │
│     • Ejemplo: "Error en pago", "SAGA compensando"     │
│                                                         │
│  3️⃣  TRAZAS (Traces)                                   │
│     • ¿Cómo fluye una request?                         │
│     • OpenTelemetry + Jaeger (no en este ejercicio)   │
│     • Ejemplo: Request atraviesa 3 servicios          │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**Analogía**: Observar un carro:
- **Métricas**: Velocímetro, tacómetro (datos numéricos)
- **Logs**: Computadora de diagnóstico (eventos históricos)
- **Trazas**: GPS mostrando la ruta completa (flujo de viaje)

---

## 🛠️ Stack de Monitoreo

### Arquitectura del Sistema con Observabilidad

```
                    ┌──────────────┐
                    │   CLIENTE    │
                    └───────┬──────┘
                            │
                    ┌───────▼──────┐
                    │ Order Service│
                    │   (8080)     │
                    └┬─────────────┬┘
                     │             │
         ┌───────────▼──┐    ┌────▼─────────┐
         │ Inventory    │    │  Payment     │
         │ Service      │    │  Service     │
         │  (8081)      │    │   (8082)     │
         └──────────────┘    └──────────────┘
                  │                  │
                  │    Métricas      │
                  ▼                  ▼
         ┌─────────────────────────────────┐
         │       PROMETHEUS (9090)         │
         │   Recolecta métricas cada 15s   │
         └────────────┬────────────────────┘
                      │
                      ▼
         ┌─────────────────────────────────┐
         │        GRAFANA (3000)           │
         │   Visualiza métricas            │
         └─────────────────────────────────┘

                  │                  │
                  │      Logs        │
                  ▼                  ▼
         ┌─────────────────────────────────┐
         │      LOGSTASH (5000)            │
         │   Procesa y parsea logs         │
         └────────────┬────────────────────┘
                      │
                      ▼
         ┌─────────────────────────────────┐
         │   ELASTICSEARCH (9200)          │
         │   Almacena logs indexados       │
         └────────────┬────────────────────┘
                      │
                      ▼
         ┌─────────────────────────────────┐
         │       KIBANA (5601)             │
         │   Visualiza y busca logs        │
         └─────────────────────────────────┘
```

### Componentes del Stack

| Componente | Puerto | Función |
|-----------|--------|---------|
| **Prometheus** | 9090 | Recolección de métricas |
| **Grafana** | 3000 | Visualización de métricas |
| **Elasticsearch** | 9200 | Almacenamiento de logs |
| **Logstash** | 5000 | Procesamiento de logs |
| **Kibana** | 5601 | Visualización de logs |
| **Filebeat** | - | Recolector de logs (opcional) |

---

## 📦 Instalación y Configuración

### Requisitos Previos

- ✅ Docker Desktop corriendo
- ✅ 8 GB RAM disponibles (mínimo)
- ✅ Los 3 microservicios del Capítulo 10 funcionando

### Paso 1: Levantar el Stack de Monitoreo

```bash
# Desde la raíz del proyecto CAPITULO 10_1
docker-compose -f docker-compose-monitoring.yml up -d
```

**Esto levanta:**
- PostgreSQL (ya existente)
- Redis (ya existente)
- Prometheus
- Grafana
- Elasticsearch
- Logstash
- Kibana
- Filebeat

**Verificar que estén corriendo:**
```bash
docker ps

# Deberías ver 8 contenedores activos
```

**Esperar a que estén listos (~2 minutos):**
```bash
# Verificar salud de cada contenedor
docker ps --format "table {{.Names}}\t{{.Status}}"
```

---

### Paso 2: Compilar los Microservicios con Micrometer

```bash
# Desde la raíz del proyecto
mvn clean package -DskipTests

# Esto compila los 3 servicios con las nuevas dependencias de Micrometer
```

---

### Paso 3: Iniciar los 3 Microservicios

**Terminal 1 - Inventory Service:**
```bash
cd inventory-service
mvn quarkus:dev
```

**Terminal 2 - Payment Service:**
```bash
cd payment-service
mvn quarkus:dev
```

**Terminal 3 - Order Service:**
```bash
cd order-service
mvn quarkus:dev
```

---

### Paso 4: Verificar Endpoints de Métricas

```bash
# Verificar que cada servicio expone métricas
curl http://localhost:8080/q/metrics  # Order Service
curl http://localhost:8081/q/metrics  # Inventory Service
curl http://localhost:8082/q/metrics  # Payment Service

# Deberías ver métricas en formato Prometheus
```

**Salida esperada:**
```
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="G1 Eden Space",} 2.45760E7
...
```

---

## 📊 Uso de Prometheus

### Acceder a Prometheus

1. Abrir navegador: http://localhost:9090
2. Ir a **Status > Targets**
3. Verificar que los 3 microservicios estén **UP**

```
Endpoint                              State    Last Scrape
order-service (localhost:8080)        UP       2s ago
inventory-service (localhost:8081)    UP       3s ago
payment-service (localhost:8082)      UP       1s ago
```

### Consultas Básicas de Prometheus (PromQL)

#### 1. Requests por segundo (RPS)

```promql
# RPS total del Order Service
rate(http_server_requests_seconds_count{job="order-service"}[1m])

# RPS por endpoint
rate(http_server_requests_seconds_count{job="order-service", uri="/api/orders"}[1m])
```

#### 2. Latencia P95 (percentil 95)

```promql
# Latencia P95 del Order Service
histogram_quantile(0.95, 
  rate(http_server_requests_seconds_bucket{job="order-service"}[5m])
)
```

#### 3. Error Rate (tasa de errores)

```promql
# Porcentaje de errores 5xx
rate(http_server_requests_seconds_count{status=~"5..",job="order-service"}[5m]) 
/ 
rate(http_server_requests_seconds_count{job="order-service"}[5m])
```

#### 4. Uso de memoria JVM

```promql
# Memoria heap usada
jvm_memory_used_bytes{area="heap",job="order-service"}

# Memoria heap máxima
jvm_memory_max_bytes{area="heap",job="order-service"}
```

#### 5. Redis Cache Hit Rate

```promql
# Cache Hit Rate (custom metric si la implementamos)
redis_cache_hit_total / (redis_cache_hit_total + redis_cache_miss_total)
```

### Ejercicio Práctico: Crear Alertas en Prometheus

Crear archivo `prometheus/alert_rules.yml`:

```yaml
groups:
  - name: microservices_alerts
    interval: 30s
    rules:
      # Alerta si error rate > 5%
      - alert: HighErrorRate
        expr: |
          rate(http_server_requests_seconds_count{status=~"5.."}[5m]) 
          / 
          rate(http_server_requests_seconds_count[5m]) > 0.05
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value | humanizePercentage }}"

      # Alerta si latencia P95 > 1 segundo
      - alert: HighLatency
        expr: |
          histogram_quantile(0.95, 
            rate(http_server_requests_seconds_bucket[5m])
          ) > 1.0
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High latency detected"
          description: "P95 latency is {{ $value }}s"
```

---

## 📈 Uso de Grafana

### Acceder a Grafana

1. Abrir navegador: http://localhost:3000
2. Login:
   - **Usuario**: `admin`
   - **Contraseña**: `admin`
3. (Opcional) Cambiar contraseña o skip

### Dashboard Pre-configurado

Ya viene un dashboard básico en `grafana/dashboards/microservices-dashboard.json`

Para verlo:
1. Ir a **Dashboards** (icono de 4 cuadrados)
2. Click en **Microservices Monitoring Dashboard**

### Crear Tu Propio Dashboard

#### Panel 1: Requests Per Second (RPS)

1. Click en **+ Create Dashboard**
2. Click en **Add new panel**
3. En la query, escribir:
   ```promql
   rate(http_server_requests_seconds_count{job="order-service"}[1m])
   ```
4. En **Legend**: `{{method}} {{uri}}`
5. Panel type: **Time series**
6. Guardar panel con título: "RPS - Order Service"

#### Panel 2: Latencia P50, P95, P99

1. Agregar nuevo panel
2. Query A (P50):
   ```promql
   histogram_quantile(0.50, rate(http_server_requests_seconds_bucket{job="order-service"}[5m]))
   ```
3. Query B (P95):
   ```promql
   histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{job="order-service"}[5m]))
   ```
4. Query C (P99):
   ```promql
   histogram_quantile(0.99, rate(http_server_requests_seconds_bucket{job="order-service"}[5m]))
   ```
5. Legend: `P50`, `P95`, `P99`
6. Unit: `seconds (s)`

#### Panel 3: Error Rate Gauge

1. Agregar panel tipo **Gauge**
2. Query:
   ```promql
   rate(http_server_requests_seconds_count{status=~"5.."}[5m]) 
   / 
   rate(http_server_requests_seconds_count[5m])
   ```
3. Unit: **Percent (0.0-1.0)**
4. Thresholds:
   - Verde: 0 - 0.01 (1%)
   - Amarillo: 0.01 - 0.05 (5%)
   - Rojo: > 0.05

#### Panel 4: Circuit Breaker State

1. Agregar panel tipo **Stat**
2. Query:
   ```promql
   resilience4j_circuitbreaker_state{name="order-saga"}
   ```
3. Value mappings:
   - 0 = CLOSED (verde)
   - 1 = OPEN (rojo)
   - 2 = HALF_OPEN (amarillo)

#### Panel 5: JVM Memory Usage

1. Agregar panel
2. Query A (Used):
   ```promql
   jvm_memory_used_bytes{area="heap"}
   ```
3. Query B (Max):
   ```promql
   jvm_memory_max_bytes{area="heap"}
   ```
4. Legend: `{{job}} - Used` y `{{job}} - Max`
5. Unit: **bytes (IEC)**

### Guardar Dashboard

1. Click en **Save dashboard** (icono de disquete)
2. Nombre: "Microservices SAGA & Redis Monitoring"
3. Click en **Save**

---

## 📋 Uso de ELK Stack

### Acceder a Kibana

1. Abrir navegador: http://localhost:5601
2. Esperar a que Kibana cargue (puede tomar 1-2 minutos)

### Configurar Index Pattern

1. Ir a **Management > Stack Management**
2. Click en **Index Patterns**
3. Click en **Create index pattern**
4. Index pattern name: `quarkus-logs-*`
5. Time field: `@timestamp`
6. Click en **Create index pattern**

### Buscar Logs

1. Ir a **Discover** (icono de brújula)
2. Seleccionar index pattern: `quarkus-logs-*`
3. Ver logs en tiempo real

### Filtros Útiles

#### Ver solo logs de SAGA

```
tags: saga
```

#### Ver solo errores

```
level: ERROR
```

#### Ver compensaciones de SAGA

```
tags: saga-compensation
```

#### Ver logs de un servicio específico

```
service_name: "order-service"
```

### Crear Visualizaciones

#### Visualización 1: Logs por Nivel

1. Ir a **Visualize Library**
2. Click en **Create visualization**
3. Tipo: **Pie chart**
4. Data source: `quarkus-logs-*`
5. Metrics:
   - Aggregation: **Count**
6. Buckets:
   - Aggregation: **Terms**
   - Field: `level.keyword`
7. Guardar como: "Logs by Level"

#### Visualización 2: Errores en el Tiempo

1. Crear visualización tipo **Area**
2. Metrics: **Count**
3. Buckets:
   - X-axis: **Date Histogram**
   - Field: `@timestamp`
4. Filters: `level: ERROR`
5. Guardar como: "Errors Over Time"

#### Visualización 3: Top Servicios con Errores

1. Tipo: **Horizontal Bar**
2. Metrics: **Count**
3. Buckets:
   - Y-axis: **Terms**
   - Field: `service_name.keyword`
4. Filters: `level: ERROR`
5. Guardar como: "Services with Most Errors"

### Crear Dashboard en Kibana

1. Ir a **Dashboard**
2. Click en **Create dashboard**
3. Agregar las 3 visualizaciones creadas
4. Guardar como: "Microservices Logs Dashboard"

---

## 🔍 Métricas Clave

### 1. Métricas de Aplicación (RED Method)

| Métrica | PromQL | Descripción |
|---------|--------|-------------|
| **Rate** (RPS) | `rate(http_server_requests_seconds_count[1m])` | Requests por segundo |
| **Errors** | `rate(http_server_requests_seconds_count{status=~"5.."}[5m])` | Errores por segundo |
| **Duration** (Latencia) | `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))` | Tiempo de respuesta P95 |

### 2. Métricas de SAGA

| Métrica | Dónde verla | Qué indica |
|---------|-------------|------------|
| **SAGA Success Rate** | Logs con tag `saga` sin `saga-compensation` | % de SAGAs exitosas |
| **SAGA Compensation Rate** | Logs con tag `saga-compensation` | % de compensaciones ejecutadas |
| **SAGA Duration** | Latencia del endpoint `/api/orders` | Tiempo total de la transacción distribuida |

### 3. Métricas de Redis Cache

| Métrica | Dónde verla | Qué indica |
|---------|-------------|------------|
| **Cache Hit Rate** | Logs con "Cache HIT" vs "Cache MISS" | Efectividad del cache |
| **Cache Latency** | Latencia de llamadas con cache | Beneficio de usar cache |

### 4. Métricas de Circuit Breaker

| Métrica | PromQL | Qué indica |
|---------|--------|------------|
| **Circuit State** | `resilience4j_circuitbreaker_state` | Estado actual (CLOSED/OPEN/HALF_OPEN) |
| **Failure Rate** | `resilience4j_circuitbreaker_failure_rate` | % de fallos que causaron la apertura |

### 5. Métricas de JVM

| Métrica | PromQL | Qué indica |
|---------|--------|------------|
| **Heap Memory** | `jvm_memory_used_bytes{area="heap"}` | Memoria usada |
| **GC Pause** | `jvm_gc_pause_seconds` | Tiempo en Garbage Collection |
| **Threads** | `jvm_threads_live` | Número de threads activos |

---

## 🐌 Identificar Cuellos de Botella

### Metodología: Análisis de Latencia

```
┌────────────────────────────────────────────────────────┐
│ REQUEST COMPLETA: 500ms                                │
├────────────────────────────────────────────────────────┤
│ 1. Order Service recibe request         │  10ms       │
│ 2. Buscar producto en cache (MISS)      │  50ms  ⚠️   │
│ 3. SAGA Step 1: Reserve Inventory       │  150ms ⚠️   │
│ 4. SAGA Step 2: Process Payment         │  200ms 🔴   │
│ 5. SAGA Step 3: Confirm Inventory       │  80ms       │
│ 6. Save order to DB                     │  10ms       │
└────────────────────────────────────────────────────────┘

🔍 Análisis:
- 🔴 Payment Service es el cuello de botella (200ms = 40% del total)
- ⚠️ Inventory Service también es lento (150ms)
- ⚠️ Cache MISS agrega 50ms extra
```

### Paso 1: Identificar el Servicio Más Lento

**En Grafana**, crear query para comparar latencias:

```promql
# Latencia P95 de cada servicio
histogram_quantile(0.95, 
  rate(http_server_requests_seconds_bucket[5m])
)
```

**Agrupar por servicio**:
```promql
histogram_quantile(0.95, 
  sum by (job) (rate(http_server_requests_seconds_bucket[5m]))
)
```

### Paso 2: Identificar el Endpoint Más Lento

```promql
# Latencia P95 por endpoint
histogram_quantile(0.95, 
  sum by (uri) (rate(http_server_requests_seconds_bucket{job="order-service"}[5m]))
)
```

### Paso 3: Correlacionar con Logs en Kibana

1. Ir a Kibana > Discover
2. Filtrar por el servicio lento:
   ```
   service_name: "payment-service" AND level: DEBUG
   ```
3. Buscar patrones:
   - ¿Hay muchas consultas a BD?
   - ¿Hay timeouts?
   - ¿Hay reintentos?

### Ejemplo de Análisis Completo

**Escenario**: Payment Service es lento (P95 = 800ms)

**Paso 1 - Métricas en Grafana**:
```promql
# Payment Service latencia por endpoint
histogram_quantile(0.95, 
  sum by (uri) (rate(http_server_requests_seconds_bucket{job="payment-service"}[5m]))
)

# Resultado: /api/payments/process = 800ms
```

**Paso 2 - Logs en Kibana**:
```
service_name: "payment-service" AND uri: "/api/payments/process"

# Encontramos:
2024-10-23 20:30:45 DEBUG PaymentService - Validando tarjeta... (50ms)
2024-10-23 20:30:46 DEBUG PaymentService - Consultando fraude externo... (700ms) 🔴
2024-10-23 20:30:46 DEBUG PaymentService - Guardando pago... (50ms)
```

**Conclusión**: El servicio externo de fraude es el cuello de botella.

**Soluciones**:
1. Cachear resultados de validación de fraude
2. Hacer la consulta asíncrona
3. Implementar circuit breaker con fallback

---

## ❌ Patrones de Error

### 1. Error Intermitente (Transient Errors)

**Síntomas en Grafana**:
- Error rate con picos pero retorna a 0%
- Gráfica de "dientes de sierra"

**Ejemplo en Kibana**:
```
level: ERROR AND message: "Connection timeout"
```

**Causa común**: Servicio externo inestable

**Solución**: Implementar retry con backoff exponencial

---

### 2. Error en Cascada

**Síntomas**:
- Un servicio falla → Todos los servicios muestran errores
- Error rate aumenta en todos los servicios al mismo tiempo

**Ejemplo**:
```
T0: Payment Service falla (error rate 100%)
T1: Order Service falla porque Payment falla (error rate 100%)
T2: Clientes reciben errores (UX degradada)
```

**Solución**: Circuit Breaker (ya implementado)

---

### 3. Memory Leak

**Síntomas en Grafana**:
- Memoria heap crece constantemente
- GC pause time aumenta
- Eventualmente: OutOfMemoryError

```promql
# Ver tendencia de memoria
jvm_memory_used_bytes{area="heap",job="order-service"}
```

**En Kibana**:
```
level: ERROR AND message: "OutOfMemoryError"
```

**Solución**: Heap dump analysis con VisualVM

---

### 4. SAGA Compensation Storm

**Síntomas**:
- Muchas compensaciones al mismo tiempo
- Logs llenos de "🔄 compensando..."

**En Kibana**:
```
tags: saga-compensation

# Contar compensaciones por minuto
```

**Causa**: Servicio downstream fallando consistentemente

**Solución**: Implementar fallback + circuit breaker

---

## 🛠️ Troubleshooting

### Problema 1: Prometheus no scrapea métricas

**Síntoma**:
```
Status > Targets → order-service: DOWN
```

**Diagnóstico**:
```bash
# Verificar que el servicio expone métricas
curl http://localhost:8080/q/metrics

# Si falla, verificar logs del servicio
```

**Soluciones**:
1. Verificar que Micrometer esté habilitado en `application.properties`
2. Verificar que el servicio esté corriendo
3. En Docker, usar `host.docker.internal` en lugar de `localhost`

---

### Problema 2: Grafana no muestra datos

**Síntoma**: Panels vacíos con "No data"

**Diagnóstico**:
1. Verificar datasource: **Configuration > Data sources > Prometheus**
2. Click en **Test**: Debe decir "Data source is working"
3. Verificar que hay métricas en Prometheus: http://localhost:9090

**Solución**:
```bash
# Reiniciar Grafana
docker restart grafana
```

---

### Problema 3: Kibana no muestra logs

**Síntoma**: Index pattern sin datos

**Diagnóstico**:
```bash
# Verificar que Elasticsearch está corriendo
curl http://localhost:9200/_cluster/health

# Verificar que Logstash está procesando
docker logs logstash

# Verificar índices creados
curl http://localhost:9200/_cat/indices
```

**Solución**:
1. Verificar que los microservicios están enviando logs a Logstash
2. Verificar configuración de `logstash.conf`
3. Reiniciar stack ELK:
   ```bash
   docker restart elasticsearch logstash kibana
   ```

---

### Problema 4: Elasticsearch se queda sin memoria

**Síntoma**:
```bash
docker logs elasticsearch
# ERROR: OutOfMemoryError
```

**Solución**:
Aumentar heap size en `docker-compose-monitoring.yml`:
```yaml
elasticsearch:
  environment:
    - "ES_JAVA_OPTS=-Xms1g -Xmx1g"  # Aumentar a 1GB
```

---

## 🎯 Ejercicios Prácticos

### Ejercicio 1: Detectar Cache Miss Ratio Alto

**Objetivo**: Usar Grafana para ver el cache hit rate de Redis

**Pasos**:
1. Generar tráfico:
   ```bash
   # Crear 100 órdenes con productos diferentes
   for i in {1..100}; do
     curl -X POST http://localhost:8080/api/orders \
       -H "Content-Type: application/json" \
       -d "{...}"
   done
   ```

2. En Grafana, buscar en logs:
   - "Cache HIT"
   - "Cache MISS"

3. Calcular ratio: `HIT / (HIT + MISS)`

**Pregunta**: ¿Cómo mejorarías un cache hit rate de 30%?

---

### Ejercicio 2: Simular Fallo en Payment Service

**Objetivo**: Ver cómo se comportan las métricas y logs durante un fallo

**Pasos**:
1. Detener Payment Service:
   ```bash
   # En la terminal donde corre payment-service
   Ctrl + C
   ```

2. Crear órdenes:
   ```bash
   curl -X POST http://localhost:8080/api/orders ...
   ```

3. En Grafana, observar:
   - Error rate sube a 100%
   - Circuit breaker se abre

4. En Kibana, buscar:
   ```
   tags: saga-compensation
   ```

5. Reiniciar Payment Service y observar recuperación

---

### Ejercicio 3: Identificar Cuello de Botella

**Objetivo**: Usar métricas para encontrar el servicio más lento

**Pasos**:
1. Generar tráfico sostenido
2. En Grafana, comparar latencias:
   ```promql
   histogram_quantile(0.95, 
     sum by (job) (rate(http_server_requests_seconds_bucket[5m]))
   )
   ```
3. Identificar el servicio con mayor latencia
4. Ir a Kibana y buscar logs de ese servicio
5. Proponer solución

---

## 📚 Recursos Adicionales

### Documentación

- [Prometheus Query Language (PromQL)](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Grafana Dashboard Best Practices](https://grafana.com/docs/grafana/latest/dashboards/build-dashboards/best-practices/)
- [Kibana Query Language (KQL)](https://www.elastic.co/guide/en/kibana/current/kuery-query.html)
- [Quarkus Micrometer Guide](https://quarkus.io/guides/micrometer)

### Dashboards Públicos

- [JVM Dashboard for Grafana](https://grafana.com/grafana/dashboards/4701)
- [Quarkus Dashboard](https://grafana.com/grafana/dashboards/14370)

---

## 🎓 Conclusión

Has aprendido a:

✅ Configurar un **stack completo de observabilidad** para microservicios  
✅ Usar **Prometheus** para recolectar métricas  
✅ Crear **dashboards en Grafana** para visualización en tiempo real  
✅ Implementar **ELK Stack** para logs centralizados  
✅ **Identificar cuellos de botella** usando métricas de latencia  
✅ **Detectar patrones de error** correlacionando métricas y logs  
✅ Monitorear comportamiento de **SAGA** y **Redis Cache**  

### 🚀 Próximos Pasos

1. Implementar **distributed tracing** con OpenTelemetry + Jaeger
2. Agregar **alerting** con Prometheus Alertmanager
3. Crear **SLOs** (Service Level Objectives) para tus servicios
4. Implementar **synthetic monitoring** con pruebas automatizadas

---

**¡Felicitaciones por completar el Capítulo 10_1! 🎉**
