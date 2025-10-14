# Teoría: Persistencia Reactiva con Quarkus

**Capítulo 4: Hibernate Reactive Panache y SmallRye Mutiny**

---

## 📚 Tabla de Contenidos

1. [Introducción a la Programación Reactiva](#1-introducción-a-la-programación-reactiva)
2. [El Problema del Enfoque Clásico](#2-el-problema-del-enfoque-clásico)
3. [La Solución Reactiva](#3-la-solución-reactiva)
4. [Uni y Multi: Los Pilares de Mutiny](#4-uni-y-multi-los-pilares-de-mutiny)
5. [Hibernate Reactive Panache](#5-hibernate-reactive-panache)
6. [PanacheRepositoryBase vs Active Record](#6-panacherepositorybase-vs-active-record)
7. [Composición Reactiva](#7-composición-reactiva)
8. [Transacciones Reactivas](#8-transacciones-reactivas)
9. [Backpressure](#9-backpressure)
10. [Comparativa: Clásico vs Reactivo](#10-comparativa-clásico-vs-reactivo)
11. [Casos de Uso](#11-casos-de-uso)
12. [Ventajas y Desventajas](#12-ventajas-y-desventajas)
13. [Patrones Comunes](#13-patrones-comunes)
14. [Buenas Prácticas](#14-buenas-prácticas)
15. [Conclusiones](#15-conclusiones)

---

## 1. Introducción a la Programación Reactiva

### ¿Qué es la Programación Reactiva?

La **programación reactiva** es un paradigma de programación orientado a **flujos de datos asíncronos** y la **propagación de cambios**. En lugar de bloquear un thread esperando un resultado, la programación reactiva permite que el sistema continúe procesando otras tareas mientras espera.

### Principios Fundamentales

1. **No bloqueante (Non-blocking):** Las operaciones I/O no bloquean threads
2. **Asíncrono:** Las operaciones retornan inmediatamente, el resultado llega después
3. **Basado en eventos:** Responde a eventos (datos disponibles, error, completado)
4. **Backpressure:** Control del flujo cuando el consumidor es más lento que el productor

### El Manifiesto Reactivo

Los sistemas reactivos son:
- **Responsivos:** Responden rápidamente
- **Resilientes:** Se mantienen operativos ante fallos
- **Elásticos:** Escalan según demanda
- **Orientados a mensajes:** Comunicación asíncrona

---

## 2. El Problema del Enfoque Clásico

### Modelo Clásico (Bloqueante)

```java
@GET
public List<Producto> listarProductos() {
    return em.createQuery("SELECT p FROM Producto p", Producto.class)
             .getResultList(); // ⏸️ Thread bloqueado esperando BD
}
```

**Flujo de ejecución:**
```
Request → Thread asignado → Espera BD (bloqueado) → Respuesta → Thread liberado
          ↑_____________ Thread ocupado sin hacer nada _______________↑
```

### Problemas del Modelo Clásico

#### 1. **Bloqueo de Threads**
Cada request consume un thread que queda bloqueado esperando I/O (BD, APIs externas, etc.).

**Ejemplo:**
- 100 requests simultáneos = 100 threads bloqueados
- Si cada operación demora 2 segundos, tienes 100 threads sin hacer nada útil

#### 2. **Limitación de Escalabilidad**
Los threads son recursos costosos:
- Memoria: ~1MB por thread
- Context switching: overhead de CPU
- Límite del pool de threads

**Cálculo:**
- Pool de 200 threads
- Request #201 debe esperar hasta que se libere un thread
- Alta latencia en períodos de alta carga

#### 3. **Ineficiencia en Alta Concurrencia**
```
1000 requests concurrentes:
├── Pool de 200 threads
├── 200 procesando (la mayoría bloqueados esperando I/O)
└── 800 esperando en cola
```

#### 4. **Cascada de Bloqueos**
```java
// Cada llamada bloquea
Producto p = buscarProducto(id);        // Bloqueo 1
Usuario u = buscarUsuario(p.userId);    // Bloqueo 2
Orden o = crearOrden(p, u);             // Bloqueo 3
```

Tiempo total = suma de todos los tiempos de espera.

---

## 3. La Solución Reactiva

### Modelo Reactivo (No Bloqueante)

```java
@GET
public Uni<List<Producto>> listarProductos() {
    return repository.listAll(); // ⚡ Retorna inmediatamente
}
```

**Flujo de ejecución:**
```
Request → Thread asignado → Inicia operación → Thread liberado
                                ↓
                           (operación en background)
                                ↓
                           Resultado listo → Callback → Respuesta
```

### Ventajas del Modelo Reactivo

#### 1. **Threads Liberados Inmediatamente**
El thread no espera, puede procesar otros requests mientras la BD responde.

#### 2. **Event Loop**
```
Event Loop (pocos threads, ej: 4-8):
├── Recibe request 1 → inicia operación BD → libera thread
├── Recibe request 2 → inicia operación BD → libera thread
├── Recibe request 3 → inicia operación BD → libera thread
└── BD responde request 1 → callback → envía respuesta
```

Un solo thread puede manejar **miles de requests** concurrentes.

#### 3. **Mayor Throughput**
```
Clásico: 200 threads → 200 requests/segundo
Reactivo: 8 threads → 2000+ requests/segundo
```

#### 4. **Composición Sin Bloqueos**
```java
// Operaciones paralelas sin bloqueos
return buscarProducto(id)
    .chain(p -> buscarUsuario(p.userId))
    .chain(u -> crearOrden(p, u));
```

Cada operación libera el thread mientras espera.

---

## 4. Uni y Multi: Los Pilares de Mutiny

**SmallRye Mutiny** es la librería reactiva usada por Quarkus. Proporciona dos tipos fundamentales:

### 4.1 Uni\<T\>

**Definición:** Una operación asíncrona que producirá **exactamente un resultado** (o un error).

**Analogía:** Una promesa de JavaScript o CompletableFuture de Java, pero más expresiva.

#### Características:
- Retorna **0 o 1 elemento**
- Puede fallar (error)
- Lazy (no ejecuta hasta que alguien se subscribe)

#### Ejemplo:
```java
Uni<Producto> producto = repository.findById(1L);
// ⚠️ Nada ha ocurrido aún, es "lazy"

// Solo cuando se usa:
producto.subscribe().with(
    p -> System.out.println("Producto: " + p),
    error -> System.out.println("Error: " + error)
);
```

#### Estados de un Uni:
```
Uni<T>
  ├── Sin iniciar (lazy)
  ├── En progreso (ejecutándose)
  ├── Completado con valor (item)
  └── Completado con error (failure)
```

#### Operadores Comunes:

##### `onItem()`
Trabaja con el resultado exitoso:
```java
Uni<Producto> producto = repository.findById(1L);

// Transformar el resultado
Uni<String> nombre = producto.onItem().transform(p -> p.nombre);

// Condiciones
Uni<Response> response = producto
    .onItem().ifNotNull().transform(p -> Response.ok(p).build())
    .onItem().ifNull().continueWith(Response.status(404).build());
```

##### `onFailure()`
Manejo de errores:
```java
return repository.findById(id)
    .onFailure().recoverWithItem(new Producto("Default", "", 0.0, 0))
    .onFailure().retry().atMost(3);
```

##### `chain()` o `transformToUni()`
Encadenar operaciones asíncronas:
```java
return repository.findById(id)
    .chain(producto -> {
        producto.stock++;
        return repository.persist(producto);
    });
```

---

### 4.2 Multi\<T\>

**Definición:** Un flujo de **múltiples elementos** emitidos a lo largo del tiempo.

**Analogía:** Un Stream de Java, pero reactivo (con backpressure y asíncrono).

#### Características:
- Retorna **0, 1 o N elementos**
- Puede emitir elementos continuamente
- Soporta backpressure
- Puede ser infinito

#### Ejemplo:
```java
Multi<Producto> productos = Multi.createFrom().items(
    producto1, producto2, producto3
);

productos.subscribe().with(
    p -> System.out.println("Producto: " + p),
    error -> System.out.println("Error: " + error),
    () -> System.out.println("Completado")
);
```

#### Estados de un Multi:
```
Multi<T>
  ├── Sin iniciar (lazy)
  ├── Emitiendo items (0..N)
  ├── Completado
  └── Error
```

#### Operadores Comunes:

##### `select()`
Filtrar y transformar:
```java
Multi<Producto> productosCaros = Multi.createFrom().items(productos)
    .select().where(p -> p.precio > 1000)
    .onItem().transform(p -> p.nombre.toUpperCase());
```

##### `collect()`
Convertir Multi a Uni (agrupar):
```java
Uni<List<Producto>> lista = Multi.createFrom().items(productos)
    .collect().asList();
```

##### `onItem()`
Procesar cada elemento:
```java
Multi.createFrom().items(productos)
    .onItem().invoke(p -> System.out.println("Procesando: " + p.nombre));
```

---

### 4.3 Uni vs Multi vs Tipos Clásicos

| Tipo Clásico | Tipo Reactivo | Descripción |
|--------------|---------------|-------------|
| `T` | `Uni<T>` | Un valor único |
| `Optional<T>` | `Uni<T>` | Un valor que puede no existir |
| `CompletableFuture<T>` | `Uni<T>` | Operación asíncrona |
| `Stream<T>` | `Multi<T>` | Flujo de valores |
| `List<T>` | `Uni<List<T>>` | Colección completa |

---

## 5. Hibernate Reactive Panache

### ¿Qué es Hibernate Reactive?

**Hibernate Reactive** es la versión reactiva de Hibernate ORM. Permite:
- Operaciones de BD **no bloqueantes**
- Usa drivers reactivos (reactive-pg-client, reactive-mysql-client)
- API basada en `Uni` y `Multi`

### ¿Qué es Panache?

**Panache** simplifica Hibernate eliminando boilerplate:
- Sin necesidad de escribir queries simples
- Métodos predefinidos (`findAll`, `findById`, `persist`, etc.)
- Active Record o Repository pattern

### Hibernate Reactive + Panache = ❤️

La combinación perfecta:
- Operaciones reactivas sin complejidad
- Métodos simples que retornan `Uni` o `Multi`
- Código limpio y expresivo

---

## 6. PanacheRepositoryBase vs Active Record

Panache ofrece **dos patrones** para trabajar con entidades:

### 6.1 Active Record Pattern

**Concepto:** La entidad contiene su propia lógica de persistencia.

```java
@Entity
public class Producto extends PanacheEntity {
    public String nombre;
    public Double precio;
    
    // Métodos estáticos
    public static Uni<List<Producto>> listAll() {
        return PanacheEntity.listAll();
    }
    
    public static Uni<Producto> findById(Long id) {
        return PanacheEntity.findById(id);
    }
}

// Uso:
Uni<List<Producto>> productos = Producto.listAll();
```

**Ventajas:**
- Menos código
- Todo en un solo lugar
- Ideal para prototipos y proyectos pequeños

**Desventajas:**
- Mezcla responsabilidades (entidad + persistencia)
- Dificulta testing (métodos estáticos)
- Menos flexible para lógica compleja

---

### 6.2 Repository Pattern (PanacheRepositoryBase) ⭐ USADO EN ESTE EJERCICIO

**Concepto:** Separa la lógica de persistencia en una clase Repository.

```java
// Entidad simple (solo datos)
@Entity
public class Producto extends PanacheEntity {
    public String nombre;
    public Double precio;
}

// Repository separado
@ApplicationScoped
public class ProductoRepository implements PanacheRepositoryBase<Producto, Long> {
    
    public Uni<List<Producto>> findConStockBajo(int umbral) {
        return list("stock < ?1", umbral);
    }
}

// Uso:
@Inject ProductoRepository repository;
Uni<List<Producto>> productos = repository.listAll();
```

**Ventajas:**
- **Separación de responsabilidades** (SRP)
- Fácil de testear (inyección de dependencias)
- Más profesional y escalable
- Permite múltiples repositorios para la misma entidad

**Desventajas:**
- Más clases (pero mejor organizado)

---

### Comparación:

| Aspecto | Active Record | Repository Pattern |
|---------|---------------|-------------------|
| **Código** | Menos líneas | Más estructurado |
| **Testing** | Difícil (métodos estáticos) | Fácil (mocking) |
| **Escalabilidad** | Limitada | Excelente |
| **Casos de uso** | Prototipos, apps pequeñas | Producción, apps grandes |
| **Separación de responsabilidades** | ❌ | ✅ |

**Recomendación:** Usa **Repository Pattern** para proyectos profesionales.

---

## 7. Composición Reactiva

La **composición reactiva** es encadenar operaciones asíncronas de forma fluida.

### 7.1 Transformaciones Simples

#### `transform()` - Transforma el valor
```java
Uni<String> nombre = repository.findById(1L)
    .onItem().transform(producto -> producto.nombre);
```

#### `transformToUni()` - Encadena otra operación asíncrona
```java
Uni<Response> response = repository.findById(id)
    .onItem().transformToUni(producto -> {
        producto.stock++;
        return repository.persist(producto);
    })
    .onItem().transform(p -> Response.ok(p).build());
```

---

### 7.2 Manejo Condicional

```java
Uni<Response> response = repository.findById(id)
    .onItem().ifNotNull().transform(p -> Response.ok(p).build())
    .onItem().ifNull().continueWith(Response.status(404).build());
```

**Explicación:**
- Si existe el producto → Status 200 + producto
- Si no existe → Status 404

---

### 7.3 Manejo de Errores

```java
return repository.findById(id)
    .onFailure().recoverWithItem(productoDefault)
    .onFailure().retry().atMost(3)
    .onFailure().transform(error -> new CustomException(error));
```

---

### 7.4 Combinación de Múltiples Uni

#### `combine()` - Ejecutar en paralelo
```java
Uni<Producto> producto = repository.findById(1L);
Uni<Usuario> usuario = usuarioRepository.findById(10L);

Uni<Orden> orden = Uni.combine().all()
    .unis(producto, usuario)
    .asTuple()
    .onItem().transform(tuple -> {
        Producto p = tuple.getItem1();
        Usuario u = tuple.getItem2();
        return new Orden(p, u);
    });
```

**Ventaja:** Ambas queries se ejecutan **en paralelo**, no secuencialmente.

---

### 7.5 Composición Compleja

```java
return Panache.withTransaction(() ->
    repository.findById(id)
        .onItem().ifNull().failWith(new NotFoundException())
        .chain(producto -> {
            producto.stock -= cantidad;
            if (producto.stock < 0) {
                return Uni.createFrom().failure(new StockInsuficienteException());
            }
            return repository.persist(producto);
        })
        .chain(producto -> notificarAlmacen(producto.id))
        .onItem().transform(v -> Response.ok().build())
        .onFailure(StockInsuficienteException.class)
            .recoverWithItem(Response.status(400).entity("Stock insuficiente").build())
);
```

**Explicación paso a paso:**
1. Inicia transacción
2. Busca producto (falla si no existe)
3. Descuenta stock (falla si es insuficiente)
4. Persiste cambios
5. Notifica al almacén (otra operación asíncrona)
6. Retorna respuesta exitosa o error específico

---

## 8. Transacciones Reactivas

### 8.1 El Problema con @Transactional

`@Transactional` clásico **bloquea el thread** hasta que la transacción completa.

```java
@POST
@Transactional  // ⚠️ Bloquea thread
public Response crear(ProductoRequest request) {
    Producto p = new Producto(...);
    em.persist(p);
    return Response.ok(p).build();
}
```

---

### 8.2 Solución: Panache.withTransaction()

```java
@POST
public Uni<Response> crear(ProductoRequest request) {
    Producto producto = new Producto(...);
    
    return Panache.withTransaction(() -> 
        repository.persist(producto)
    ).onItem().transform(p -> 
        Response.created(URI.create("/productos/" + p.id))
                .entity(p)
                .build()
    );
}
```

**Características:**
- **No bloqueante:** El thread se libera mientras la BD procesa
- **Automático:** Commit si exitoso, rollback si falla
- **Composable:** Se integra con `Uni` y operadores

---

### 8.3 Transacciones Anidadas

```java
return Panache.withTransaction(() ->
    repository.findById(id)
        .chain(producto -> {
            producto.precio *= 0.9; // Descuento 10%
            return repository.persist(producto);
        })
        .chain(producto -> 
            historialRepository.registrar(producto.id, "Descuento aplicado")
        )
);
```

**Importante:** Todo el bloque está en la **misma transacción**.

---

### 8.4 Control Manual de Transacciones

```java
return Panache.getSession()
    .chain(session -> session.beginTransaction()
        .chain(tx -> repository.persist(producto)
            .chain(p -> {
                if (condicion) {
                    return tx.commit().replaceWith(p);
                } else {
                    return tx.rollback().chain(() -> 
                        Uni.createFrom().failure(new ValidationException())
                    );
                }
            })
        )
    );
```

**Raramente necesario**, pero disponible para casos avanzados.

---

## 9. Backpressure

### ¿Qué es Backpressure?

**Backpressure** es el mecanismo para controlar el flujo cuando:
- El **productor** genera datos más rápido que el **consumidor** los procesa

### Problema sin Backpressure:

```
Productor (BD) → [1000 productos/seg] → Consumidor (API) [100 productos/seg]
                         ↓
                   Buffer crece → OutOfMemoryError
```

---

### Solución con Backpressure:

**Multi** soporta backpressure automáticamente:

```java
@GET
@Path("/stream")
@Produces(MediaType.SERVER_SENT_EVENTS)
public Multi<Producto> streamProductos() {
    return Multi.createFrom().items(/* millones de productos */)
        .onOverflow().buffer(100)  // Buffer de 100 elementos
        .onOverflow().drop();       // Descarta si buffer lleno
}
```

**El cliente** controla la velocidad:
- Solicita N elementos
- Procesa
- Solicita más cuando está listo

---

### Estrategias de Backpressure:

1. **Buffer:** Almacena temporalmente
2. **Drop:** Descarta elementos excedentes
3. **Error:** Lanza error si overflow
4. **Latest:** Solo mantiene el más reciente

```java
Multi.createFrom().items(productos)
    .onOverflow().buffer(50)
    .onOverflow().drop()
```

---

## 10. Comparativa: Clásico vs Reactivo

### 10.1 Código Lado a Lado

#### Buscar por ID:

**Clásico:**
```java
@GET
@Path("/{id}")
public Response buscarPorId(@PathParam("id") Long id) {
    Producto p = em.find(Producto.class, id);
    if (p == null) {
        return Response.status(404).build();
    }
    return Response.ok(p).build();
}
```

**Reactivo:**
```java
@GET
@Path("/{id}")
public Uni<Response> buscarPorId(@PathParam("id") Long id) {
    return repository.findById(id)
        .onItem().ifNotNull().transform(p -> Response.ok(p).build())
        .onItem().ifNull().continueWith(Response.status(404).build());
}
```

---

#### Crear Producto:

**Clásico:**
```java
@POST
@Transactional
public Response crear(ProductoRequest req) {
    Producto p = new Producto(req.nombre, req.descripcion, req.precio, req.stock);
    em.persist(p);
    return Response.created(URI.create("/productos/" + p.id)).entity(p).build();
}
```

**Reactivo:**
```java
@POST
public Uni<Response> crear(ProductoRequest req) {
    Producto p = new Producto(req.nombre, req.descripcion, req.precio, req.stock);
    return Panache.withTransaction(() -> repository.persist(p))
        .onItem().transform(producto ->
            Response.created(URI.create("/productos/" + producto.id))
                    .entity(producto)
                    .build()
        );
}
```

---

### 10.2 Tabla Comparativa Completa

| Aspecto | Clásico (JPA) | Reactivo (Hibernate Reactive) |
|---------|---------------|-------------------------------|
| **Thread Model** | 1 thread por request (bloqueante) | Event loop (no bloqueante) |
| **Tipo de retorno** | `T`, `List<T>` | `Uni<T>`, `Multi<T>` |
| **Transacciones** | `@Transactional` | `Panache.withTransaction()` |
| **Driver BD** | JDBC (bloqueante) | Reactive driver (no bloqueante) |
| **Concurrencia** | Pool de threads limitado | Miles de requests con pocos threads |
| **Latencia** | Mayor en alta carga | Menor y más consistente |
| **Throughput** | Limitado por threads | Muy alto |
| **Curva de aprendizaje** | Baja | Media-Alta |
| **Debugging** | Fácil (stack traces simples) | Más complejo (callbacks) |
| **Testing** | Directo | Requiere `UniAsserter` |
| **Composición** | Secuencial (bloqueante) | Fluida (operadores reactivos) |
| **Manejo de errores** | Try-catch | `onFailure()` |
| **Backpressure** | No | Sí (con Multi) |

---

### 10.3 Rendimiento: Números Concretos

#### Escenario: API con operaciones de BD que demoran 50ms cada una

**Configuración Clásica:**
- Pool de 200 threads
- Cada request bloquea un thread por 50ms

**Cálculo:**
```
Capacidad máxima = 200 threads × (1000ms / 50ms) = 4,000 requests/segundo
```

**Configuración Reactiva:**
- Event loop de 8 threads
- Threads no se bloquean

**Cálculo:**
```
Capacidad práctica = 10,000+ requests/segundo con los mismos 8 threads
```

**Resultado:** ~2.5x más throughput con 25x menos threads.

---

## 11. Casos de Uso

### ✅ Cuándo Usar Reactivo:

1. **Alta concurrencia:** Miles de usuarios simultáneos
2. **Microservicios:** Muchas llamadas I/O a otros servicios
3. **APIs públicas:** Necesitas manejar picos de tráfico
4. **Real-time:** Streaming, notificaciones, SSE
5. **Operaciones I/O intensivas:** Muchas queries a BD, APIs externas
6. **Recursos limitados:** Servidor con poca RAM/CPU

**Ejemplo:** Sistema de pagos que consulta múltiples APIs (banco, antifraude, inventario) por cada transacción.

---

### ❌ Cuándo NO Usar Reactivo:

1. **CPU intensivo:** Cálculos pesados, procesamiento de imágenes
2. **Baja concurrencia:** App interna con 10 usuarios
3. **Equipo sin experiencia:** Curva de aprendizaje puede retrasar proyecto
4. **Legacy code:** Migrar sistema grande puede no justificar el esfuerzo
5. **Operaciones bloqueantes inevitables:** Librerías externas bloqueantes

**Ejemplo:** Batch job nocturno que procesa archivos locales secuencialmente.

---

## 12. Ventajas y Desventajas

### ✅ Ventajas de Reactivo

1. **Escalabilidad Superior**
   - Más requests con menos recursos
   - Event loop eficiente

2. **Mejor Uso de Recursos**
   - Threads no bloqueados
   - Menor consumo de memoria

3. **Latencia Consistente**
   - No hay esperas en queue de threads
   - Respuestas más predecibles bajo carga

4. **Composición Elegante**
   - Operadores expresivos
   - Código fluido y funcional

5. **Backpressure**
   - Control automático de flujo
   - Evita OutOfMemory

6. **Resiliencia**
   - Retry, timeout, fallback incorporados
   - Manejo robusto de errores

---

### ❌ Desventajas de Reactivo

1. **Curva de Aprendizaje**
   - Paradigma diferente
   - Requiere cambio de mentalidad

2. **Debugging Complejo**
   - Stack traces no lineales
   - Difícil seguir el flujo

3. **Testing Más Elaborado**
   - Requiere `UniAsserter`, `StepVerifier`
   - Mocking más complejo

4. **No Todo es Reactivo**
   - Librerías bloqueantes rompen la cadena
   - JDBC drivers no son reactivos

5. **Overhead Mental**
   - Pensar en flujos asíncronos
   - Composición puede ser compleja

6. **Overkill para Apps Simples**
   - CRUD básico no necesita reactivo
   - Complejidad innecesaria

---

## 13. Patrones Comunes

### 13.1 Patrón: Búsqueda con Fallback

```java
public Uni<Producto> buscarConFallback(Long id) {
    return repository.findById(id)
        .onItem().ifNull().switchTo(() -> 
            cacheRepository.findById(id)
        )
        .onItem().ifNull().continueWith(productoDefault);
}
```

---

### 13.2 Patrón: Retry con Backoff

```java
public Uni<Producto> buscarConRetry(Long id) {
    return repository.findById(id)
        .onFailure().retry()
            .withBackOff(Duration.ofSeconds(1))
            .atMost(3);
}
```

---

### 13.3 Patrón: Timeout

```java
public Uni<List<Producto>> listarConTimeout() {
    return repository.listAll()
        .ifNoItem().after(Duration.ofSeconds(5))
        .failWith(new TimeoutException());
}
```

---

### 13.4 Patrón: Operaciones en Paralelo

```java
public Uni<ReporteCompleto> generarReporte(Long id) {
    Uni<Producto> producto = repository.findById(id);
    Uni<List<Venta>> ventas = ventaRepository.findByProducto(id);
    Uni<Integer> stock = stockRepository.getStock(id);
    
    return Uni.combine().all()
        .unis(producto, ventas, stock)
        .asTuple()
        .onItem().transform(tuple -> 
            new ReporteCompleto(
                tuple.getItem1(),
                tuple.getItem2(),
                tuple.getItem3()
            )
        );
}
```

**Ventaja:** Las 3 queries se ejecutan **en paralelo**, no secuencialmente.

---

### 13.5 Patrón: Cache Reactivo

```java
private final Map<Long, Uni<Producto>> cache = new ConcurrentHashMap<>();

public Uni<Producto> findByIdConCache(Long id) {
    return cache.computeIfAbsent(id, key -> 
        repository.findById(key)
            .memoize().indefinitely() // Cachea el resultado
    );
}
```

---

## 14. Buenas Prácticas

### ✅ DO (Hacer)

1. **Usa Repository Pattern para Producción**
   ```java
   @ApplicationScoped
   public class ProductoRepository implements PanacheRepositoryBase<Producto, Long> {
       // Lógica de persistencia aquí
   }
   ```

2. **Siempre Maneja Errores**
   ```java
   return repository.findById(id)
       .onFailure().recoverWithItem(defaultValue)
       .onFailure().invoke(e -> logger.error("Error", e));
   ```

3. **Usa `Panache.withTransaction()` para Modificaciones**
   ```java
   return Panache.withTransaction(() -> 
       repository.persist(producto)
   );
   ```

4. **Nombra los Uni Descriptivamente**
   ```java
   Uni<Producto> productoActualizado = repository.persist(producto);
   ```

5. **Compón Operaciones Fluidamente**
   ```java
   return repository.findById(id)
       .chain(this::validar)
       .chain(this::actualizar)
       .chain(this::notificar);
   ```

6. **Usa `ifNoItem()` para Timeouts**
   ```java
   return operation()
       .ifNoItem().after(Duration.ofSeconds(10))
       .failWith(new TimeoutException());
   ```

---

### ❌ DON'T (No Hacer)

1. **No Bloquees en Código Reactivo**
   ```java
   // ❌ MAL
   return repository.findById(id)
       .onItem().transform(p -> {
           Thread.sleep(1000); // ¡Nunca!
           return p;
       });
   ```

2. **No Uses `.await()` en Código Reactivo**
   ```java
   // ❌ MAL
   Producto p = repository.findById(id).await().indefinitely();
   ```
   
   **Solo usar `.await()` en tests:**
   ```java
   // ✅ OK en tests
   @Test
   void test() {
       Producto p = repository.findById(1L).await().indefinitely();
       assertEquals("Laptop", p.nombre);
   }
   ```

3. **No Ignores Errores**
   ```java
   // ❌ MAL
   return repository.findById(id); // ¿Qué pasa si falla?
   
   // ✅ BIEN
   return repository.findById(id)
       .onFailure().recoverWithItem(defaultProduct);
   ```

4. **No Mezcles Bloqueante con Reactivo**
   ```java
   // ❌ MAL
   return repository.findById(id)
       .onItem().transform(p -> {
           jdbcTemplate.query(...); // JDBC bloqueante
           return p;
       });
   ```

5. **No Olvides Transacciones**
   ```java
   // ❌ MAL
   return repository.persist(producto); // Sin transacción
   
   // ✅ BIEN
   return Panache.withTransaction(() -> 
       repository.persist(producto)
   );
   ```

---

## 15. Conclusiones

### Resumen Ejecutivo

La **persistencia reactiva** con Quarkus, Hibernate Reactive Panache y Mutiny ofrece:

1. **Mayor eficiencia:** Más requests con menos recursos
2. **Mejor escalabilidad:** Maneja miles de requests concurrentes
3. **Código expresivo:** Composición fluida con operadores
4. **Resiliencia incorporada:** Retry, timeout, fallback

### Cuándo Adoptarlo

**Reactivo es ideal para:**
- APIs con alta concurrencia
- Microservicios con muchas llamadas I/O
- Sistemas que requieren alta disponibilidad
- Proyectos nuevos sin legacy code

**Clásico sigue siendo válido para:**
- Apps internas con baja concurrencia
- CRUD simple
- Equipos sin experiencia en reactivo
- Operaciones CPU-intensivas

### El Futuro

La programación reactiva es el **estándar de facto** para sistemas modernos:
- Spring WebFlux (reactivo)
- Node.js (naturalmente reactivo)
- Vert.x (reactivo)
- Quarkus (híbrido, con soporte reactivo de primera clase)

### Recomendación Final

Para proyectos profesionales en Quarkus:
1. Usa **Repository Pattern** (`PanacheRepositoryBase`)
2. Aprende los **operadores básicos** de Mutiny (`onItem`, `chain`, `onFailure`)
3. Practica la **composición** de operaciones
4. Implementa **manejo de errores** robusto
5. Mide y compara **rendimiento** vs clásico

**El esfuerzo de aprender reactivo se compensa con sistemas más eficientes, escalables y resilientes.**

---

## 📚 Referencias

- [Quarkus - Hibernate Reactive Panache](https://quarkus.io/guides/hibernate-reactive-panache)
- [SmallRye Mutiny](https://smallrye.io/smallrye-mutiny/)
- [Reactive Manifesto](https://www.reactivemanifesto.org/)
- [Hibernate Reactive](https://hibernate.org/reactive/)

