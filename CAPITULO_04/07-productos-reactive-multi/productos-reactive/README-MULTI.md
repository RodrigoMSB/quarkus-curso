# Programación Reactiva con Mutiny: Multi vs Uni

## 📋 Descripción

Este ejercicio demuestra la diferencia fundamental entre **Uni** y **Multi** en Quarkus con Mutiny, los dos tipos principales de flujos reactivos para programación no bloqueante.

### ¿Qué es Uni?

`Uni<T>` representa **un solo valor asíncrono**. Es similar a:
- `CompletableFuture<T>` en Java
- `Promise<T>` en JavaScript
- `Task<T>` en C#

**Casos de uso típicos de Uni:**
- Operaciones CRUD REST (crear, actualizar, eliminar un recurso)
- Consultas que retornan un solo resultado o una lista completa
- Cualquier operación que produce exactamente UN resultado

### ¿Qué es Multi?

`Multi<T>` representa **un stream de múltiples valores** emitidos en el tiempo. Es similar a:
- `Publisher<T>` en Reactive Streams
- `Observable<T>` en RxJava
- `IAsyncEnumerable<T>` en C#

**Casos de uso típicos de Multi:**
- Server-Sent Events (SSE) para actualizaciones en tiempo real
- Streaming de grandes datasets procesados por lotes
- Eventos continuos (logs, métricas, notificaciones)
- Monitoreo de sistemas en tiempo real

## 🎯 Objetivos de Aprendizaje

Al completar este ejercicio, aprenderás:

1. ✅ La diferencia conceptual y práctica entre Uni y Multi
2. ✅ Cómo implementar endpoints con Server-Sent Events (SSE)
3. ✅ Operadores de transformación: `transformToMulti`, `onItem().call()`
4. ✅ Cómo crear delays no bloqueantes con Mutiny
5. ✅ Cuándo usar cada tipo de flujo reactivo

## 🏗️ Arquitectura del Ejercicio

### Endpoints implementados

#### 1. Uni - Enfoque tradicional
```
GET /api/v1/productos/reactivo
Retorna: Uni<List<Producto>>
```
- Retorna TODA la lista de una vez
- Respuesta JSON única y completa
- Cliente espera hasta tener todos los datos

#### 2. Multi - Streaming con SSE
```
GET /api/v1/productos/reactivo/stream
Retorna: Multi<Producto>
Content-Type: text/event-stream
```
- Emite productos UNO POR UNO progresivamente
- Delay de 500ms entre cada producto (solo para demostración)
- Cliente recibe datos apenas están listos

#### 3. Multi - Monitor en tiempo real (Bonus)
```
GET /api/v1/productos/reactivo/monitor-stock/{id}
Retorna: Multi<String>
Content-Type: text/event-stream
```
- Stream INFINITO de actualizaciones cada 1 segundo
- Muestra el estado del stock en tiempo real
- Útil para dashboards y monitoreo continuo

## 🚀 Ejecución

### Paso 1: Iniciar la aplicación

```bash
./mvnw quarkus:dev
```

### Paso 2: Ejecutar el script de prueba

**En macOS/Linux:**
```bash
chmod +x test-multi-streaming.sh
./test-multi-streaming.sh
```

**En Windows Git Bash:**
```bash
bash test-multi-streaming.sh
```

## 📊 Comparación Visual

### Uni<List<Producto>>

```
Cliente                          Servidor
   │                                 │
   │──── GET /productos ────────────>│
   │                                 │
   │                        [Procesa TODOS]
   │                                 │
   │<────────── Lista completa ──────│
   │ [producto1, producto2, ...]    │
   │                                 │
```

### Multi<Producto>

```
Cliente                          Servidor
   │                                 │
   │──── GET /stream ───────────────>│
   │                                 │
   │<────────── producto1 ───────────│ (500ms)
   │                                 │
   │<────────── producto2 ───────────│ (500ms)
   │                                 │
   │<────────── producto3 ───────────│ (500ms)
   │                                 │
   │<────────── producto4 ───────────│ (500ms)
```

## 🔧 Conceptos Técnicos Clave

### Operadores de Multi utilizados

#### 1. `Multi.createFrom().iterable()`
Crea un Multi desde una colección existente:
```java
Multi.createFrom().iterable(productos)
```

#### 2. `onItem().transformToMulti()`
Transforma un Uni en Multi:
```java
repository.listAll()  // Uni<List<Producto>>
    .onItem().transformToMulti(productos -> 
        Multi.createFrom().iterable(productos)
    )
```

#### 3. `onItem().call()`
Ejecuta una operación asíncrona por cada item sin modificarlo:
```java
.onItem().call(producto -> 
    Uni.createFrom().item(producto)
        .onItem().delayIt().by(Duration.ofMillis(500))
)
```

#### 4. `Multi.createFrom().ticks()`
Genera un stream infinito con emisiones periódicas:
```java
Multi.createFrom().ticks().every(Duration.ofSeconds(1))
```

### Server-Sent Events (SSE)

SSE es un protocolo HTTP para streaming unidireccional servidor → cliente:

- **Content-Type:** `text/event-stream`
- **Formato:** Líneas con prefijo `data:`
- **Conexión:** Persistente (long-polling)
- **Uso:** Actualizaciones en tiempo real sin WebSockets

**Ejemplo de respuesta SSE:**
```
data: {"id":1,"nombre":"Laptop","precio":1299.99}

data: {"id":2,"nombre":"Mouse","precio":29.99}

data: {"id":3,"nombre":"Teclado","precio":89.99}
```

## 💡 Casos de Uso Reales - Contexto Bancario

### Escenario 1: Cotización de Dólar en Tiempo Real
```java
@GET
@Path("/cotizacion-usd")
@Produces(MediaType.SERVER_SENT_EVENTS)
public Multi<CotizacionDTO> streamCotizaciones() {
    return Multi.createFrom().ticks().every(Duration.ofSeconds(5))
        .onItem().transformToUniAndMerge(tick -> 
            obtenerCotizacionActual()
        );
}
```

### Escenario 2: Stream de Transacciones
```java
@GET
@Path("/transacciones/stream")
@Produces(MediaType.SERVER_SENT_EVENTS)
public Multi<Transaccion> streamTransacciones() {
    return transaccionRepository.findAll()
        .onItem().transformToMulti(transacciones ->
            Multi.createFrom().iterable(transacciones)
                .onItem().call(t -> logTransaccion(t))
        );
}
```

### Escenario 3: Monitor de Fraude
```java
@GET
@Path("/monitor-fraude")
@Produces(MediaType.SERVER_SENT_EVENTS)
public Multi<AlertaFraude> monitorearFraude() {
    return Multi.createFrom().ticks().every(Duration.ofSeconds(10))
        .onItem().transformToUniAndMerge(tick ->
            fraudeService.detectarPatronesSospechosos()
        );
}
```

## 📈 Ventajas de Multi sobre Uni<List<T>>

| Aspecto | Uni<List<T>> | Multi<T> |
|---------|--------------|----------|
| **Memoria** | Carga toda la lista en RAM | Procesa item por item (streaming) |
| **Latencia inicial** | Espera tener todos los datos | Primera respuesta inmediata |
| **Escalabilidad** | Limitada por tamaño de lista | Soporta datasets grandes/infinitos |
| **Backpressure** | No aplica | Manejo automático |
| **Cancelación** | No durante procesamiento | Cliente puede cancelar en cualquier momento |
| **Uso de CPU** | Picos al procesar lista completa | Distribuido en el tiempo |

## 🎓 Preguntas Frecuentes de Estudiantes

### ¿Cuándo debo usar Multi en lugar de Uni<List<T>>?

**Usa Multi cuando:**
- El dataset es muy grande (>1000 items)
- Necesitas mostrar resultados progresivamente
- Implementas actualizaciones en tiempo real
- El procesamiento de cada item es costoso
- Quieres dar feedback inmediato al usuario

**Usa Uni<List<T>> cuando:**
- El dataset es pequeño/mediano (<100 items)
- Necesitas todos los datos para procesarlos juntos
- Implementas APIs REST estándar
- La simplicidad es más importante que el streaming

### ¿Multi bloquea threads?

**No.** Multi es completamente no bloqueante:
- Los delays usan timers, no `Thread.sleep()`
- Las consultas a BD son asíncronas
- El servidor puede manejar miles de streams concurrentes

### ¿Cómo manejo errores en Multi?

```java
Multi.createFrom().iterable(productos)
    .onFailure().recoverWithItem(productoDefault)
    .onFailure().invoke(error -> log.error("Error", error))
```

### ¿Puedo transformar Multi a Uni?

Sí, con operadores de agregación:
```java
Multi<Producto> multi = ...;

// Colectar en lista
Uni<List<Producto>> uni = multi.collect().asList();

// Contar items
Uni<Long> count = multi.collect().count();

// Primer item
Uni<Producto> first = multi.toUni();
```

## 🔍 Para Profundizar

### Documentación oficial
- [Mutiny Reference Guide](https://smallrye.io/smallrye-mutiny)
- [Quarkus Reactive Guide](https://quarkus.io/guides/getting-started-reactive)
- [Server-Sent Events Spec](https://html.spec.whatwg.org/multipage/server-sent-events.html)

### Operadores avanzados de Multi
```java
// Filtrado
multi.select().where(p -> p.stock > 0)

// Transformación
multi.onItem().transform(p -> toDTO(p))

// Limitación
multi.select().first(10)

// Agrupación
multi.group().by(p -> p.categoria)

// Combinación
Multi.createBy().merging().streams(multi1, multi2)
```

## 🎯 Ejercicios Propuestos

1. **Modificar el delay**: Cambia el delay de 500ms a 1 segundo y observa el comportamiento

2. **Agregar filtrado**: Implementa un endpoint que haga streaming solo de productos con stock bajo

3. **Implementar paginación reactiva**: Crea un endpoint que emita productos en lotes de 10

4. **Monitor de múltiples productos**: Extiende el monitor para seguir varios productos simultáneamente

5. **Implementar cancelación**: Agrega lógica para detener el stream basado en una condición

## ✅ Checklist de Aprendizaje

- [ ] Entiendo la diferencia entre Uni y Multi
- [ ] Sé cuándo usar cada tipo de flujo
- [ ] Puedo implementar endpoints SSE
- [ ] Comprendo los operadores básicos de Multi
- [ ] Sé crear delays no bloqueantes
- [ ] Puedo transformar entre Uni y Multi
- [ ] Entiendo el concepto de backpressure

## 🏆 Resultado Esperado

Al finalizar este ejercicio, deberías ser capaz de:

1. ✅ Implementar streaming reactivo con Multi
2. ✅ Elegir correctamente entre Uni y Multi según el caso de uso
3. ✅ Usar Server-Sent Events para actualizaciones en tiempo real
4. ✅ Aplicar operadores de transformación de Multi
5. ✅ Diseñar APIs reactivas escalables y eficientes

---

**¡Éxito en tu aprendizaje de programación reactiva! 🚀**
