# Guía del Instructor: Multi vs Uni en Mutiny

## 🎯 Objetivo de la Lección

Los estudiantes deben comprender la diferencia fundamental entre Uni y Multi, y saber cuándo aplicar cada uno en escenarios reales.

## ⏱️ Duración Estimada

- **Teoría:** 20 minutos
- **Demo en vivo:** 15 minutos
- **Ejercicio práctico:** 25 minutos
- **Total:** 60 minutos

## 📚 Conocimientos Previos Requeridos

✅ Los estudiantes deben saber:
- Programación reactiva básica con Uni
- Concepto de operaciones no bloqueantes
- RESTful APIs en Quarkus
- Uso básico de curl

## 🗣️ Puntos Clave para Enfatizar

### 1. La Analogía del Grifo vs la Manguera

**Para explicar la diferencia:**

> **Uni es como llenar un vaso de agua:**
> - Abres el grifo
> - Esperas a que se llene
> - Recibes el vaso completo
> - Una sola "entrega"

> **Multi es como una manguera conectada:**
> - Abres el grifo
> - El agua fluye continuamente
> - Puedes procesar el agua conforme llega
> - Múltiples "entregas" en el tiempo

### 2. Cuándo Usar Cada Uno

**Usa esta tabla en la pizarra/pantalla:**

```
┌─────────────────────────┬───────────────┬──────────────┐
│ Caso de Uso             │ Usar Uni      │ Usar Multi   │
├─────────────────────────┼───────────────┼──────────────┤
│ Obtener un cliente      │ ✅            │ ❌           │
│ Listar 10 productos     │ ✅            │ ⚠️           │
│ Listar 10,000 productos │ ⚠️            │ ✅           │
│ Crear una orden         │ ✅            │ ❌           │
│ Cotizaciones en vivo    │ ❌            │ ✅           │
│ Stream de transacciones │ ❌            │ ✅           │
│ Logs en tiempo real     │ ❌            │ ✅           │
│ Notificaciones push     │ ❌            │ ✅           │
└─────────────────────────┴───────────────┴──────────────┘

Leyenda: ✅ Ideal | ⚠️ Posible pero no óptimo | ❌ No recomendado
```

### 3. Server-Sent Events (SSE)

**Explica el protocolo SSE con esta comparación:**

| Característica | REST tradicional | WebSocket | SSE |
|----------------|------------------|-----------|-----|
| Dirección | Request/Response | Bidireccional | Servidor → Cliente |
| Protocolo | HTTP | WebSocket | HTTP |
| Complejidad | Baja | Alta | Media |
| Reconexión | Manual | Manual | Automática |
| Caso de uso | CRUD | Chat | Eventos/Notificaciones |

**Mensaje clave:**
> "SSE es perfecto cuando solo el servidor necesita enviar actualizaciones al cliente, como cotizaciones, notificaciones, o logs. Es más simple que WebSockets y suficiente para el 80% de casos de streaming."

## 🎬 Secuencia de Enseñanza Recomendada

### Fase 1: Teoría (20 min)

#### 1.1 Introducción (5 min)
```
"En las clases anteriores vimos Uni para operaciones asíncronas.
Hoy aprenderemos Multi, que extiende la reactividad a STREAMS de datos.

Piensen en Uni como una promesa de UN valor,
y Multi como un canal que emite MÚLTIPLES valores en el tiempo."
```

#### 1.2 Conceptos Core (10 min)

**Dibuja este diagrama:**

```
                    MUTINY
                       |
            ┌──────────┴──────────┐
            │                     │
           UNI                  MULTI
            │                     │
    ┌───────┴───────┐    ┌────────┴────────┐
    │               │    │                 │
 Success         Failure  Items         Complete
    │               │    │                 │
  onItem       onFailure  onItem        onCompletion
```

#### 1.3 Operadores Clave (5 min)

**Muestra este código en pantalla:**

```java
// UNI → MULTI
Uni<List<T>> uni = ...;
Multi<T> multi = uni.onItem()
    .transformToMulti(list -> Multi.createFrom().iterable(list));

// MULTI → UNI
Multi<T> multi = ...;
Uni<List<T>> uni = multi.collect().asList();

// Delay no bloqueante
Multi<T> delayed = multi.onItem()
    .call(item -> Uni.createFrom().item(item)
        .onItem().delayIt().by(Duration.ofMillis(500))
    );
```

### Fase 2: Demo en Vivo (15 min)

#### 2.1 Preparación (2 min)
```bash
# Asegurarse que la app está corriendo
./mvnw quarkus:dev
```

#### 2.2 Demo Uni (5 min)

**Ejecuta y explica:**
```bash
# Mostrar endpoint tradicional
curl http://localhost:8080/api/v1/productos/reactivo

# Punto de enseñanza:
"Observen: recibimos TODA la lista de una vez.
El cliente esperó hasta que el servidor procesó TODO.
Esto está bien para listas pequeñas."
```

#### 2.3 Demo Multi (8 min)

**Ejecuta y explica:**
```bash
# Mostrar streaming con SSE
curl -N -H "Accept: text/event-stream" \
  http://localhost:8080/api/v1/productos/reactivo/stream

# Puntos de enseñanza mientras se ejecuta:
"1. Noten el formato 'data:' - es SSE
2. Cada producto llega UNO POR UNO
3. Hay un delay visible entre cada uno (500ms)
4. El cliente puede PROCESAR cada producto apenas llega
5. En producción, no habría delay artificial"
```

#### 2.4 Demo Monitor (Opcional - 3 min)

```bash
# Mostrar stream infinito
curl -N -H "Accept: text/event-stream" \
  http://localhost:8080/api/v1/productos/reactivo/monitor-stock/1

# Dejar corriendo 10 segundos, luego Ctrl+C

"Este es un stream INFINITO. Perfecto para dashboards
que muestran datos en tiempo real. El servidor NO está
bloqueado - puede atender miles de estos streams concurrentemente."
```

### Fase 3: Script de Prueba (5 min)

```bash
# Ejecutar el script completo
./test-multi-streaming.sh

"Este script automatiza lo que acabamos de hacer manualmente.
Estudienlo - es cross-platform (Mac y Windows).
Noten cómo usa 'mktemp' y '--data-binary' para compatibilidad."
```

### Fase 4: Código Fuente (10 min)

**Abre el ProductoReactivoResource.java y explica:**

#### Endpoint de Streaming
```java
@GET
@Path("/stream")
@Produces(MediaType.SERVER_SENT_EVENTS)
public Multi<Producto> streamProductos() {
    return repository.listAll()              // 1. Uni<List<Producto>>
        .onItem().transformToMulti(productos -> // 2. Convierte a Multi
            Multi.createFrom().iterable(productos)  // 3. Crea stream
                .onItem().call(producto ->      // 4. Por cada producto...
                    Uni.createFrom().item(producto)
                        .onItem().delayIt()     // 5. ...espera 500ms
                        .by(Duration.ofMillis(500))
                )
        );
}
```

**Puntos clave:**
1. "`.transformToMulti()` es el puente entre Uni y Multi"
2. "`.onItem().call()` ejecuta una acción asíncrona sin modificar el item"
3. "El delay es NO BLOQUEANTE - no usa Thread.sleep()"
4. "`@Produces(SERVER_SENT_EVENTS)` activa el protocolo SSE automáticamente"

#### Endpoint de Monitor
```java
@GET
@Path("/monitor-stock/{id}")
@Produces(MediaType.SERVER_SENT_EVENTS)
public Multi<String> monitorearStock(@PathParam("id") Long id) {
    return repository.findById(id)
        .onItem().ifNull().failWith(() -> 
            new NotFoundException("Producto no encontrado"))
        .onItem().transformToMulti(producto ->
            Multi.createFrom().ticks()        // Stream infinito
                .every(Duration.ofSeconds(1))  // Cada 1 segundo
                .onItem().transformToUniAndMerge(tick ->
                    repository.findById(id)    // Re-consulta BD
                        .onItem().transform(p -> {
                            // Genera JSON manualmente
                            return String.format(...);
                        })
                )
        );
}
```

**Puntos clave:**
1. "`.ticks()` genera un stream INFINITO de eventos temporales"
2. "`.transformToUniAndMerge()` ejecuta un Uni por cada tick y combina resultados"
3. "Este patrón es ideal para polling reactivo"

### Fase 5: Ejercicio Práctico (25 min)

#### Ejercicio 1: Modificar Delay (5 min)
```
"Tarea: Cambien el delay de 500ms a 2 segundos.
Ejecuten el script y observen la diferencia.

Pista: Busquen 'Duration.ofMillis(500)' en el código."
```

#### Ejercicio 2: Filtrado en Stream (10 min)
```
"Tarea: Creen un nuevo endpoint '/stream/stock-bajo/{umbral}'
que haga streaming SOLO de productos con stock menor al umbral.

Pistas:
- Usen '.select().where(producto -> ...)'
- El endpoint debe retornar Multi<Producto>
- Mantengan el delay de 500ms"
```

**Solución:**
```java
@GET
@Path("/stream/stock-bajo/{umbral}")
@Produces(MediaType.SERVER_SENT_EVENTS)
public Multi<Producto> streamStockBajo(@PathParam("umbral") int umbral) {
    return repository.findConStockBajo(umbral)
        .onItem().transformToMulti(productos ->
            Multi.createFrom().iterable(productos)
                .onItem().call(producto ->
                    Uni.createFrom().item(producto)
                        .onItem().delayIt()
                        .by(Duration.ofMillis(500))
                )
        );
}
```

#### Ejercicio 3: Ticks Variables (10 min - Avanzado)
```
"Tarea: Modifiquen el monitor de stock para que emita cada 5 segundos
en lugar de cada 1 segundo. Pruébenlo.

Bonus: Agreguen un parámetro @QueryParam("intervalo") para
que el intervalo sea configurable."
```

## 🎓 Preguntas Frecuentes de Estudiantes

### P: "¿Multi bloquea threads?"
**R:** "No. Multi es completamente no bloqueante. Usa event loops y callbacks internamente. Un servidor puede manejar miles de streams concurrentes sin problemas."

### P: "¿Por qué usar SSE en lugar de WebSockets?"
**R:** "SSE es más simple cuando solo necesitas servidor → cliente. WebSockets son bidireccionales pero requieren más código. Para notificaciones, actualizaciones de precio, logs: SSE es suficiente y más fácil."

### P: "¿Puedo retornar Multi desde un @POST?"
**R:** "Técnicamente sí, pero es raro. POST generalmente crea UN recurso (usa Uni<Response>). Multi es más común en GET para streaming de consultas."

### P: "¿Cómo cancelo un Multi del lado del cliente?"
**R:** "El cliente simplemente cierra la conexión HTTP (Ctrl+C en curl, `.unsubscribe()` en RxJS, etc.). El servidor detecta la desconexión y libera recursos automáticamente."

### P: "¿Multi sirve para archivos grandes?"
**R:** "Sí, pero para archivos usa `Multi<Buffer>` o chunked encoding. Para datos estructurados como JSON usa `Multi<T>` con SSE como vimos hoy."

## 💡 Consejos de Enseñanza

### 1. Usa Analogías Locales
```
"Piensen en Multi como el streaming de un partido de fútbol:
- Uni sería esperar a que termine y ver el resumen (todo junto)
- Multi es verlo en vivo (eventos conforme ocurren)"
```

### 2. Relaciona con Contextos Bancarios
```
"En un banco, Multi es perfecto para:
- Dashboard que muestra transacciones en tiempo real
- Cotización de dólar que cambia cada minuto
- Alertas de fraude que deben llegar instantáneamente
- Monitoreo de cajeros automáticos en vivo"
```

### 3. Demuestra el Valor de No-Blocking
```
"Ejecuten 'jps -v' mientras corren el monitor.
Verán que Quarkus usa pocos threads (8-10).
Con código bloqueante necesitarían 1 thread por conexión.
Con 1000 clientes monitoreando = 1000 threads = colapso.
Con Multi = 1000 clientes = 8 threads = eficiencia."
```

## 🐛 Problemas Comunes

### 1. El stream no se ve en navegador
**Causa:** Los navegadores procesan SSE de forma especial.
**Solución:** Usa curl con `-N` o herramientas como Postman/Insomnia.

### 2. "Connection reset" en Windows Git Bash
**Causa:** Git Bash puede tener issues con streams largos.
**Solución:** Usar timeout o limitar items: `multi.select().first(10)`

### 3. Delay no funciona
**Causa:** Usar `Thread.sleep()` en lugar de `.delayIt()`
**Solución:** Siempre usar delays reactivos de Mutiny.

## 📝 Evaluación

### Criterios de Éxito
El estudiante debe poder:

1. ✅ Explicar la diferencia entre Uni y Multi
2. ✅ Implementar un endpoint SSE básico
3. ✅ Usar `.transformToMulti()` correctamente
4. ✅ Crear delays no bloqueantes
5. ✅ Decidir cuándo usar cada tipo

### Rúbrica Sugerida

| Criterio | Básico (1pt) | Intermedio (2pts) | Avanzado (3pts) |
|----------|--------------|-------------------|-----------------|
| **Concepto** | Sabe que Multi emite múltiples valores | Explica cuándo usar cada uno | Da ejemplos de casos reales |
| **Implementación** | Copia código sin entender | Modifica delay/filtros | Crea endpoints nuevos desde cero |
| **Debugging** | No puede resolver errores | Usa logs para depurar | Entiende stack traces reactivos |

## 🔗 Material Complementario

### Para Profundizar
- [Mutiny Docs: Multi](https://smallrye.io/smallrye-mutiny/2.5.0/reference/multi/)
- [Quarkus: Reactive in Practice](https://quarkus.io/guides/reactive-routes)
- [SSE Spec](https://html.spec.whatwg.org/multipage/server-sent-events.html)

### Ejercicios Adicionales
1. Implementar paginación reactiva con Multi
2. Combinar múltiples streams con `Multi.merge()`
3. Implementar rate limiting en streams
4. Crear un chat simple con Multi y SSE

## ✅ Checklist Pre-Clase

- [ ] Quarkus dev corriendo
- [ ] Test script probado en Mac y Windows
- [ ] Ejemplos de código preparados
- [ ] Base de datos con datos de prueba
- [ ] Postman/Insomnia configurado (opcional)
- [ ] Diagrams dibujados o en slides

## 🎯 Mensaje Final

> "Multi no es más complicado que Uni - es simplemente una herramienta diferente para problemas diferentes. Uni para valores únicos, Multi para streams. Con estas dos herramientas, pueden construir sistemas reactivos completos y escalables."

---

**¡Buena clase! 🚀**
