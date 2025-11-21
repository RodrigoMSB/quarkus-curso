# Teoría: Programación Reactiva con Multi y Uni

## 📖 Introducción

En programación reactiva con Quarkus y Mutiny, existen dos tipos fundamentales de flujos de datos:

- **Uni<T>**: Representa **un solo valor** asíncrono
- **Multi<T>**: Representa **múltiples valores** emitidos en el tiempo

Esta distinción es clave para diseñar aplicaciones reactivas eficientes y escalables.

## 🎯 ¿Qué es Uni?

### Definición

`Uni<T>` es un tipo reactivo que representa una computación asíncrona que eventualmente producirá:
- **Exactamente UN valor** (éxito)
- **O un error** (fallo)

### Analogía

Piensa en Uni como un **cupón de regalo**:
- Te dan el cupón (Uni)
- En el futuro lo canjeas y recibes UN regalo (item)
- O el cupón es inválido y no recibes nada (error)

### Equivalentes en otros lenguajes

| Lenguaje/Framework | Equivalente a Uni |
|-------------------|-------------------|
| Java 8+ | `CompletableFuture<T>` |
| JavaScript | `Promise<T>` |
| C# | `Task<T>` |
| Kotlin | `Deferred<T>` |
| Scala | `Future[T]` |
| RxJava | `Single<T>` |

### Ciclo de vida de Uni

```
    Uni<T>
       │
       ├─────── onItem ──────> Success (valor T)
       │
       └────── onFailure ────> Error (Throwable)
```

### Ejemplo práctico

```java
// Buscar un producto por ID - retorna 0 o 1 resultado
Uni<Producto> producto = repository.findById(1L);

// Transformar el resultado
Uni<String> nombre = producto.onItem()
    .transform(p -> p.nombre);

// Manejar ausencia de valor
Uni<Response> response = producto
    .onItem().ifNotNull()
        .transform(p -> Response.ok(p).build())
    .onItem().ifNull()
        .continueWith(Response.status(404).build());
```

### Casos de uso típicos de Uni

✅ Operaciones CRUD estándar
```java
Uni<Producto> crear(ProductoRequest request)
Uni<Producto> buscarPorId(Long id)
Uni<Void> actualizar(Long id, ProductoRequest request)
Uni<Boolean> eliminar(Long id)
```

✅ Consultas que retornan un resultado único
```java
Uni<Cliente> buscarPorDNI(String dni)
Uni<Long> contarProductos()
Uni<Double> calcularPromedioPrecio()
```

✅ Operaciones que producen una colección completa
```java
Uni<List<Producto>> listarTodos()
Uni<List<Cliente>> buscarPorCiudad(String ciudad)
```

## 🌊 ¿Qué es Multi?

### Definición

`Multi<T>` es un tipo reactivo que representa un flujo de datos que puede emitir:
- **Cero, uno, o muchos valores** en el tiempo
- Opcionalmente seguido de **completación** (éxito)
- **O un error** (fallo)

### Analogía

Piensa en Multi como un **canal de TV por streaming**:
- Te conectas al canal (Multi)
- Recibes múltiples frames/eventos en el tiempo
- El canal puede terminar (completación) o fallar (error)
- Puedes desconectarte en cualquier momento (cancelación)

### Equivalentes en otros lenguajes

| Lenguaje/Framework | Equivalente a Multi |
|-------------------|---------------------|
| Java 9+ | `Flow.Publisher<T>` |
| RxJava | `Observable<T>` / `Flowable<T>` |
| Project Reactor | `Flux<T>` |
| JavaScript | `Observable<T>` (RxJS) |
| C# | `IAsyncEnumerable<T>` |
| Kotlin | `Flow<T>` |

### Ciclo de vida de Multi

```
    Multi<T>
       │
       ├─────── onItem ──────> item1, item2, item3, ...
       │
       ├────── onCompletion ─> Success (stream terminó)
       │
       └────── onFailure ────> Error (Throwable)
```

### Ejemplo práctico

```java
// Listar productos como stream
Multi<Producto> productos = repository.listAll()
    .onItem().transformToMulti(lista -> 
        Multi.createFrom().iterable(lista)
    );

// Filtrar items
Multi<Producto> stockBajo = productos
    .select().where(p -> p.stock < 10);

// Transformar items
Multi<String> nombres = productos
    .onItem().transform(p -> p.nombre);

// Limitar cantidad
Multi<Producto> primerosDiez = productos
    .select().first(10);
```

### Casos de uso típicos de Multi

✅ Streaming de datos en tiempo real
```java
Multi<Cotizacion> streamCotizaciones()
Multi<Transaccion> streamTransacciones()
Multi<Notificacion> streamNotificaciones()
```

✅ Procesamiento de grandes volúmenes
```java
Multi<Producto> procesarCatalogo()  // 10,000+ items
Multi<Cliente> exportarClientes()   // Base de datos completa
```

✅ Eventos continuos o infinitos
```java
Multi<LogEntry> streamLogs()        // Stream infinito
Multi<Metrica> monitorearSistema()  // Polling continuo
Multi<Evento> escucharEventos()     // Message broker
```

✅ Server-Sent Events (SSE)
```java
@GET
@Path("/stream")
@Produces(MediaType.SERVER_SENT_EVENTS)
Multi<Producto> streamProductos()
```

## ⚖️ Multi vs Uni: Comparación Detallada

### Tabla comparativa

| Aspecto | Uni<T> | Multi<T> |
|---------|--------|----------|
| **Valores emitidos** | Exactamente 1 | 0, 1, o muchos |
| **Semántica** | Promise/Future | Stream/Observable |
| **Memoria** | Carga resultado completo | Procesa item por item |
| **Latencia inicial** | Espera completar toda la operación | Primera respuesta inmediata |
| **Cancelación** | Antes de completar | En cualquier momento |
| **Backpressure** | No aplica | Sí (automático) |
| **Protocolo típico** | REST Request/Response | Server-Sent Events, WebSocket |
| **Ejemplo visual** | Una caja completa | Una cinta transportadora |

### Cuándo usar cada uno

#### Usa Uni<T> cuando:

1. **Necesitas exactamente UN resultado**
   - Buscar un cliente por ID
   - Crear un nuevo pedido
   - Actualizar un registro
   - Eliminar una entidad

2. **Trabajas con colecciones pequeñas** (<100-1000 items)
   - Listar categorías (10-50 items)
   - Obtener productos de una categoría (20-100 items)
   - Buscar transacciones del día (variable, pero limitado)

3. **Implementas APIs REST tradicionales**
   - GET /productos/{id}
   - POST /clientes
   - PUT /ordenes/{id}
   - DELETE /productos/{id}

4. **Necesitas todos los datos juntos**
   - Generar un reporte (necesitas todos los datos)
   - Calcular un total (suma de todos los valores)
   - Mostrar un gráfico (requiere dataset completo)

#### Usa Multi<T> cuando:

1. **El dataset es muy grande**
   - Exportar catálogo completo (10,000+ productos)
   - Procesar todos los clientes (100,000+ registros)
   - Streaming de logs históricos (millones de entradas)

2. **Necesitas actualizaciones en tiempo real**
   - Cotizaciones de bolsa/divisas
   - Notificaciones push
   - Dashboard con métricas en vivo
   - Chat de soporte

3. **Procesas datos conforme llegan**
   - Consumir mensajes de Kafka
   - Escuchar eventos de un WebSocket
   - Procesar archivos línea por línea

4. **Quieres respuesta progresiva**
   - Mostrar resultados de búsqueda incrementalmente
   - Cargar feed de noticias conforme se scrollea
   - Exportar datos en chunks

## 🔄 Transformaciones entre Uni y Multi

### De Uni a Multi

```java
// Caso 1: Lista a Stream
Uni<List<Producto>> uniLista = repository.listAll();
Multi<Producto> multiItems = uniLista
    .onItem().transformToMulti(lista -> 
        Multi.createFrom().iterable(lista)
    );

// Caso 2: Valor único a Stream de 1 elemento
Uni<Producto> uniProducto = repository.findById(1L);
Multi<Producto> multiProducto = uniProducto
    .onItem().transformToMulti(Multi.createFrom()::item);

// Caso 3: Stream vacío si Uni es null
Uni<Producto> uniMaybeNull = repository.findById(999L);
Multi<Producto> multiSafe = uniMaybeNull
    .onItem().transformToMulti(p -> 
        p == null ? Multi.createFrom().empty() 
                  : Multi.createFrom().item(p)
    );
```

### De Multi a Uni

```java
// Caso 1: Colectar en lista
Multi<Producto> multi = streamProductos();
Uni<List<Producto>> uniLista = multi.collect().asList();

// Caso 2: Contar elementos
Uni<Long> count = multi.collect().count();

// Caso 3: Obtener primer elemento
Uni<Producto> primero = multi.toUni();

// Caso 4: Último elemento
Uni<Producto> ultimo = multi.collect().last();

// Caso 5: Reducir/Agregar
Uni<Double> suma = multi
    .onItem().transform(p -> p.precio)
    .collect().with(Collectors.summingDouble(d -> d));
```

## 🎭 Server-Sent Events (SSE)

### ¿Qué es SSE?

Server-Sent Events es un estándar HTML5 para streaming unidireccional servidor → cliente sobre HTTP.

### Características de SSE

- ✅ **Unidireccional**: Servidor envía, cliente recibe
- ✅ **HTTP estándar**: No requiere protocolo especial
- ✅ **Reconexión automática**: El navegador reconecta si se pierde la conexión
- ✅ **Simple**: Más fácil que WebSockets para streaming básico
- ✅ **Text-based**: Perfecto para JSON

### Formato del protocolo

```
Content-Type: text/event-stream

data: {"id":1,"nombre":"Laptop"}

data: {"id":2,"nombre":"Mouse"}

data: {"id":3,"nombre":"Teclado"}
```

Cada mensaje va precedido por `data:` y termina con doble salto de línea.

### Implementación en Quarkus

```java
@GET
@Path("/stream")
@Produces(MediaType.SERVER_SENT_EVENTS)  // ← Clave!
public Multi<Producto> streamProductos() {
    return Multi.createFrom().items(
        new Producto("Laptop", 1299.99),
        new Producto("Mouse", 29.99),
        new Producto("Teclado", 89.99)
    );
}
```

### Consumo con curl

```bash
curl -N -H "Accept: text/event-stream" \
  http://localhost:8080/api/productos/stream
```

La flag `-N` desactiva buffering, permitiendo ver datos conforme llegan.

### Comparación: SSE vs WebSocket vs Long Polling

| Característica | SSE | WebSocket | Long Polling |
|----------------|-----|-----------|--------------|
| Dirección | Servidor → Cliente | Bidireccional | Ambas (request/response) |
| Protocolo | HTTP | WebSocket | HTTP |
| Complejidad | Baja | Media | Alta |
| Reconexión | Automática | Manual | Manual |
| Overhead | Bajo | Muy bajo | Alto |
| Firewall friendly | Sí | A veces | Sí |
| Caso de uso | Notificaciones, streams | Chat, gaming | Polling básico |

## 🔬 Operadores Clave de Multi

### Creación

```java
// Desde valores
Multi<Integer> nums = Multi.createFrom().items(1, 2, 3);

// Desde colección
Multi<String> nombres = Multi.createFrom()
    .iterable(Arrays.asList("Juan", "María", "Pedro"));

// Stream infinito con ticks
Multi<Long> ticks = Multi.createFrom().ticks()
    .every(Duration.ofSeconds(1));

// Desde rango
Multi<Integer> rango = Multi.createFrom().range(1, 100);
```

### Transformación

```java
// Map
Multi<String> mayusculas = nombres
    .onItem().transform(String::toUpperCase);

// FlatMap (transformToMulti)
Multi<Character> letras = nombres
    .onItem().transformToMulti(nombre -> 
        Multi.createFrom().items(nombre.chars()
            .mapToObj(c -> (char) c)
            .toArray(Character[]::new))
    );

// Async transformation con call
Multi<Producto> conDelay = productos
    .onItem().call(p -> 
        Uni.createFrom().item(p)
            .onItem().delayIt().by(Duration.ofMillis(500))
    );
```

### Filtrado

```java
// Where
Multi<Producto> stockAlto = productos
    .select().where(p -> p.stock > 10);

// Distinct
Multi<String> categorias = productos
    .onItem().transform(p -> p.categoria)
    .select().distinct();

// First N
Multi<Producto> primeros = productos
    .select().first(10);
```

### Agregación

```java
// Contar
Uni<Long> cantidad = productos.collect().count();

// Lista
Uni<List<Producto>> lista = productos.collect().asList();

// Reducir
Uni<Double> total = productos
    .onItem().transform(p -> p.precio)
    .collect().with(Collectors.summingDouble(d -> d));
```

## 🚀 Patrones Comunes

### Patrón 1: Paginación Reactiva

```java
@GET
@Path("/stream/paginado")
@Produces(MediaType.SERVER_SENT_EVENTS)
public Multi<Producto> streamPaginado(
    @QueryParam("page") int page,
    @QueryParam("size") int size
) {
    return repository.listAll()
        .onItem().transformToMulti(lista -> 
            Multi.createFrom().iterable(lista)
                .select().skip((long) page * size)
                .select().first(size)
        );
}
```

### Patrón 2: Stream con Progreso

```java
@GET
@Path("/export")
@Produces(MediaType.SERVER_SENT_EVENTS)
public Multi<ExportProgress> exportarConProgreso() {
    return repository.listAll()
        .onItem().transformToMulti(lista -> {
            int total = lista.size();
            AtomicInteger processed = new AtomicInteger(0);
            
            return Multi.createFrom().iterable(lista)
                .onItem().transform(producto -> 
                    new ExportProgress(
                        producto,
                        processed.incrementAndGet(),
                        total
                    )
                );
        });
}
```

### Patrón 3: Retry con Delay

```java
Multi<Producto> conRetry = productos
    .onFailure().retry()
        .atMost(3)
        .withBackOff(Duration.ofSeconds(1), Duration.ofSeconds(10));
```

### Patrón 4: Timeout por Item

```java
Multi<Producto> conTimeout = productos
    .onItem().call(p -> 
        procesarProducto(p)
            .ifNoItem().after(Duration.ofSeconds(5))
            .failWith(new TimeoutException())
    );
```

## 💡 Mejores Prácticas

### ✅ DO - Haz esto

1. **Usa Multi para datasets grandes**
   ```java
   // ✅ BIEN: Stream para 10,000 productos
   Multi<Producto> stream = repository.streamAll();
   ```

2. **Aplica backpressure cuando procesas streams externos**
   ```java
   // ✅ BIEN: Control de flujo
   multi.onOverflow().buffer(100)
   ```

3. **Libera recursos con onTermination**
   ```java
   // ✅ BIEN: Cleanup
   multi.onTermination().invoke(() -> cerrarConexion())
   ```

4. **Usa operadores no bloqueantes**
   ```java
   // ✅ BIEN: Delay reactivo
   multi.onItem().call(item -> 
       Uni.createFrom().item(item)
           .onItem().delayIt().by(Duration.ofMillis(500))
   )
   ```

### ❌ DON'T - Evita esto

1. **No uses Multi para listas pequeñas**
   ```java
   // ❌ MAL: Overkill para 10 items
   Multi<Categoria> cats = Multi.createFrom().items(...);
   
   // ✅ BIEN: Usa Uni<List>
   Uni<List<Categoria>> cats = Uni.createFrom().item(List.of(...));
   ```

2. **No bloquees dentro de operadores**
   ```java
   // ❌ MAL: Thread.sleep() bloquea
   multi.onItem().transform(item -> {
       Thread.sleep(1000); // ¡NO!
       return item;
   })
   
   // ✅ BIEN: Delay reactivo
   multi.onItem().call(item -> 
       Uni.createFrom().item(item)
           .onItem().delayIt().by(Duration.ofSeconds(1))
   )
   ```

3. **No ignores errores**
   ```java
   // ❌ MAL: Sin manejo de error
   multi.onItem().transform(this::puedefallar)
   
   // ✅ BIEN: Con recuperación
   multi.onItem().transform(this::puedeFallar)
       .onFailure().recoverWithItem(itemDefault)
   ```

## 🎯 Resumen Ejecutivo

### Uni<T>
- **Qué es**: Un solo valor asíncrono
- **Cuándo**: APIs REST, CRUD, datasets pequeños
- **Como**: `CompletableFuture<T>` en Java

### Multi<T>
- **Qué es**: Stream de múltiples valores
- **Cuándo**: Streaming, tiempo real, datasets grandes
- **Como**: `Observable<T>` en RxJava

### Regla de Oro
> "Si necesitas TODO junto, usa Uni<List>.  
> Si puedes procesar de a uno, usa Multi."

### Contexto Bancario Peruano

**Usa Uni para:**
- Consultar saldo de cuenta
- Crear una transferencia
- Validar un DNI
- Buscar datos de un cliente

**Usa Multi para:**
- Stream de cotizaciones USD/PEN
- Dashboard de transacciones en vivo
- Exportar 50,000 clientes
- Monitor de cajeros automáticos

---

**Referencias:**
- [Mutiny Documentation](https://smallrye.io/smallrye-mutiny)
- [Quarkus Reactive Guide](https://quarkus.io/guides/getting-started-reactive)
- [SSE Specification](https://html.spec.whatwg.org/multipage/server-sent-events.html)
