# Capítulo 4: Programación Reactiva vs Clásica en Quarkus

> **Objetivo:** Comprender profundamente las diferencias entre programación reactiva y clásica mediante la implementación de la misma API con ambos enfoques y su comparación científica con tests de concurrencia reales.

---

## 📚 Contenido del Capítulo

| # | Módulo | Nivel |
|---|--------|-------|
| **02** | [Programación Reactiva con Mutiny](./02-programacion-reactiva-mutiny/) | Avanzado |
| **03** | [Programación Clásica Blocking](./03-programacion-clasica-blocking/) | Intermedio |
| **04** | [Comparación Reactivo vs Clásico](./04-comparacion-reactivo-vs-clasico/) | Avanzado |

---

## 🎯 Objetivos de Aprendizaje

### Programación Reactiva
- ✅ Entender el modelo de ejecución reactivo vs bloqueante
- ✅ Usar operadores de Mutiny (`onItem()`, `transformToUni()`, etc.)
- ✅ Implementar APIs REST reactivas con `Uni<T>` y `Multi<T>`
- ✅ Manejar transacciones reactivas con `Panache.withTransaction()`

### Programación Clásica
- ✅ Comprender el modelo thread-per-request tradicional
- ✅ Implementar APIs REST bloqueantes con JAX-RS estándar
- ✅ Manejar transacciones con `@Transactional`
- ✅ Identificar cuándo el enfoque clásico es suficiente

### Performance y Concurrencia
- ✅ Ejecutar tests de carga con `k6` (Grafana Labs)
- ✅ Interpretar métricas: throughput, latencia, percentiles (p50, p95, p99)
- ✅ Identificar el "sweet spot" donde reactivo brilla
- ✅ Tomar decisiones arquitectónicas basadas en datos reales

---

## 🔥 El Experimento: Misma API, Dos Enfoques

Implementamos **exactamente la misma API** de dos formas diferentes y medimos cuál es mejor y cuándo.

### API de Productos (CRUD Completo)

```
Enfoque Reactivo (Módulo 02):
├─ Usa Uni<T> y Multi<T> (Mutiny)
├─ Hibernate Reactive Panache
├─ Threads no bloqueantes (event loop)
├─ Pool: 10-20 threads
└─ Endpoint: /api/v1/productos/reactivo

Enfoque Clásico (Módulo 03):
├─ Métodos síncronos tradicionales
├─ Hibernate ORM estándar
├─ Thread-per-request model
├─ Pool: 100-200 threads
└─ Endpoint: /api/v1/productos/clasico
```

### Endpoints Implementados
- ✅ GET `/` - Listar todos
- ✅ GET `/{id}` - Buscar por ID
- ✅ POST `/` - Crear
- ✅ PUT `/{id}` - Actualizar
- ✅ DELETE `/{id}` - Eliminar
- ✅ GET `/stock-bajo/{umbral}` - Filtro
- ✅ POST `/carga-masiva/{cantidad}` - Batch insert

---

## 🛠️ Instalación de k6

### Mac
```bash
brew install k6
```

### Windows
1. Descargar: https://dl.k6.io/msi/k6-latest-amd64.msi
2. Ejecutar el instalador
3. En Git Bash, agregar al PATH:
```bash
export PATH="$PATH:/c/Program Files/k6"
```

### Verificar instalación
```bash
k6 version
```

---

## 🚀 Ejecución Rápida

### Paso 1: Proyecto Reactivo

```bash
cd 02-programacion-reactiva-mutiny/productos-reactive

# Levantar PostgreSQL
docker-compose up -d

# Iniciar aplicación
./mvnw quarkus:dev

# En otra terminal - tests
./test-concurrencia-reactivo.sh
```

**Output:** `resultados-reactivo-TIMESTAMP.txt`

---

### Paso 2: Proyecto Clásico

```bash
cd ../../03-programacion-clasica-blocking/productos-clasico

# Iniciar aplicación (PostgreSQL ya está corriendo)
./mvnw quarkus:dev

# En otra terminal - tests
./test-concurrencia-clasico.sh
```

**Output:** `resultados-clasico-TIMESTAMP.txt`

---

### Paso 3: Generar Comparativa

```bash
cd ../../04-comparacion-reactivo-vs-clasico/COMPARACION

# Copiar resultados
cp ../../02-programacion-reactiva-mutiny/productos-reactive/resultados-reactivo-*.txt .
cp ../../03-programacion-clasica-blocking/productos-clasico/resultados-clasico-*.txt .

# Generar análisis
./generar-comparativa.sh
```

**Output:** `comparativa-TIMESTAMP.md` con análisis completo

---

## 💎 Resultados Esperados: El "Sweet Spot"

En **5,000 requests con 100 workers** observarás la máxima diferencia:

```
THROUGHPUT:
├─ Reactivo: ~8,100 req/s  ⚡⚡⚡
└─ Clásico:  ~1,950 req/s  📦
   Diferencia: 4X MÁS RÁPIDO

LATENCIA p95:
├─ Reactivo: ~17ms   ✅ Consistente
└─ Clásico:  ~188ms  ⚠️ 11X peor
```

### ¿Por qué?

**Clásico (Thread-per-Request):**
```
Request 1 → Thread 1 [BLOQUEADO esperando BD]
Request 2 → Thread 2 [BLOQUEADO esperando BD]
...
Request 100 → Thread 100 [BLOQUEADO]
Request 101 → ⏳ EN COLA (no hay threads)

→ Pool saturado, latencias se disparan
```

**Reactivo (Event Loop):**
```
Request 1 → Thread 1 [envía query] → LIBERA thread
Request 2 → Thread 1 [envía query] → LIBERA thread
...
Request 5000 → Thread 10 [envía query] → LIBERA thread

→ Threads nunca bloqueados, latencias consistentes
```

---

## 🎓 Conceptos Clave

### 1. Programación Reactiva

Paradigma con **flujos asíncronos** y **callbacks** en vez de bloqueo.

**Analogía del Restaurante:**

```
Mesero Bloqueante:
1. Toma orden Mesa 1
2. Va a cocina
3. ⏳ ESPERA hasta que plato esté listo
4. Lleva plato
5. SOLO AHORA atiende Mesa 2

Mesero Reactivo:
1. Toma orden Mesa 1
2. Deja orden en cocina
3. ✅ INMEDIATAMENTE atiende Mesa 2, 3, 4...
4. Cuando cocina termina → NOTIFICA
5. Lleva plato (pausa breve)

→ Mismo mesero, 5X más mesas
```

### 2. Uni vs Multi

| Tipo | Emite | Uso |
|------|-------|-----|
| `Uni<T>` | 0 o 1 item | findById, save, update |
| `Multi<T>` | 0 a N items | listAll, streaming |

```java
// Uni - un solo resultado
Uni<Producto> producto = repository.findById(1L);

// Multi - múltiples resultados
Multi<Producto> productos = repository.listAll();
```

### 3. Thread Pools

```
CLÁSICO:
├─ Tamaño: 200 threads
├─ Modelo: 1 thread = 1 request
├─ Memoria: ~200 MB
└─ Límite: ~200 requests simultáneos

REACTIVO:
├─ Tamaño: 8-16 threads (2 * cores)
├─ Modelo: callbacks no bloqueantes
├─ Memoria: ~16 MB
└─ Límite: Miles de requests
```

### 4. El "Sweet Spot"

Punto donde una tecnología muestra su máxima ventaja:

| Carga | Comportamiento |
|-------|----------------|
| **1K (Baja)** | Ambos funcionan bien. Diferencia moderada. |
| **5K (Media)** 🎯 | **SWEET SPOT**: Clásico satura, reactivo brilla. Diferencia brutal (4X). |
| **10K (Alta)** | Clásico saturado, BD cuello de botella. Diferencia grande pero menor. |

---

## 🛠️ Cuándo Usar Cada Enfoque

### ✅ Reactivo

| Escenario | Ejemplo |
|-----------|---------|
| Alta concurrencia (>1K req/s) | API pública de pagos |
| I/O intensivo | Sistema que consulta 5 microservicios |
| SLAs estrictos (p95 < 100ms) | Plataforma financiera |
| Escalabilidad crítica | Kubernetes, serverless |
| Recursos limitados | Lambda con 512MB RAM |

### ✅ Clásico

| Escenario | Ejemplo |
|-----------|---------|
| CRUD simple (<500 req/s) | Panel admin interno |
| Equipo sin experiencia | Startup con devs Jr |
| Código legado bloqueante | Integración JDBC antigua |
| MVPs rápidos | Prototipo en 2 semanas |
| Debugging frecuente | Sistema en desarrollo |

---

## 📗 Recursos

- [Quarkus Reactive Architecture](https://quarkus.io/guides/quarkus-reactive-architecture)
- [Mutiny Documentation](https://smallrye.io/smallrye-mutiny/)
- [Hibernate Reactive Panache](https://quarkus.io/guides/hibernate-reactive-panache)
- [k6 Documentation](https://k6.io/docs/) - HTTP load testing (Grafana Labs)
- [Reactive Manifesto](https://www.reactivemanifesto.org/)

---

**¡Bienvenido al experimento!** 🔬  

Este capítulo te dará comprensión **científica y práctica** de cuándo usar programación reactiva. No teoría, sino **datos reales** de tu máquina.

---

**Nivel:** Avanzado