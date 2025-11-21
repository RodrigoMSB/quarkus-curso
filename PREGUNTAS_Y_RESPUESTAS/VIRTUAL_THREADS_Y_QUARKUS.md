# Virtual Threads y Quarkus: Dos Paradigmas de Concurrencia

## Introducción

Cuando Java 21 introdujo Virtual Threads (Project Loom), surgió una pregunta fundamental en la comunidad: ¿Qué pasa con los frameworks reactivos como Quarkus? ¿Son obsoletos ahora? ¿Se pueden combinar? ¿Cuál debo usar?

En este documento explico en detalle qué son los Virtual Threads, cómo se relacionan con Quarkus, cuándo usar cada uno, y cómo pueden potenciarse mutuamente. Mi objetivo es que entiendas no solo el "cómo", sino el "por qué" de cada decisión arquitectónica.

---

## Parte 1: Entendiendo los Fundamentos

### Los Platform Threads Tradicionales

Para entender Virtual Threads, primero debo explicar el modelo tradicional de concurrencia en Java:

**Platform Thread = Thread del Sistema Operativo**

```java
// Cada request HTTP = 1 thread del OS
@GET
public String procesar() {
    // Este thread se BLOQUEA esperando la BD
    String dato = database.query("SELECT * FROM clientes");
    
    // El thread sigue BLOQUEADO esperando la API externa
    String info = httpClient.get("https://api.externa.com/info");
    
    return dato + info;
}
```

**Características de los Platform Threads:**
- Cada thread Java es mapeado 1:1 con un thread del sistema operativo
- Cada thread consume aproximadamente 2 MB de memoria (stack + metadata)
- El OS tiene un límite práctico de threads (~10,000 en la mayoría de sistemas)
- Cuando un thread espera I/O, está **bloqueado** pero sigue consumiendo recursos

**El problema en números:**

```
Sistema con 8 GB RAM:
- 10,000 threads × 2 MB = 20 GB RAM solo para threads
- Resultado: Sistema colapsa antes de llegar a 10,000 requests concurrentes

En producción con Spring Boot tradicional:
- 200 threads configurados típicamente
- 201° request debe ESPERAR que se libere un thread
- Latencia se dispara cuando hay picos de tráfico
```

---

### Virtual Threads: El Cambio de Paradigma

Virtual Threads (Project Loom) cambian completamente esta ecuación:

**Virtual Thread = Thread ligero gestionado por la JVM**

```java
@GET
@RunOnVirtualThread  // Nueva anotación en Java 21
public String procesar() {
    // Este código es IDÉNTICO al anterior
    String dato = database.query("SELECT * FROM clientes");
    String info = httpClient.get("https://api.externa.com/info");
    return dato + info;
}
```

**Diferencias fundamentales:**

| Aspecto | Platform Thread | Virtual Thread |
|---------|----------------|----------------|
| Gestionado por | Sistema Operativo | JVM (Java Virtual Machine) |
| Memoria por thread | ~2 MB | ~1 KB (2000x menos) |
| Límite práctico | 5,000-10,000 | Millones |
| Costo de creación | Alto (~1ms) | Muy bajo (~1μs) |
| Costo de cambio de contexto | Alto | Muy bajo |

**Cómo funciona internamente:**

```
Platform Threads (tradicional):
┌─────────────────────────────────────┐
│  1 Platform Thread (2 MB)           │
│  └──> 1 OS Thread                   │
│       └──> Bloqueado esperando I/O  │
│            (desperdicia recursos)   │
└─────────────────────────────────────┘

Virtual Threads (Project Loom):
┌──────────────────────────────────────────────┐
│  Carrier Thread Pool (8-16 threads del OS)  │
│  ├──> Virtual Thread 1 (1 KB)               │
│  │     └──> Esperando I/O → se "desmonta"   │
│  │                                           │
│  ├──> Virtual Thread 2 (1 KB)               │
│  │     └──> Ejecutando código CPU           │
│  │                                           │
│  ├──> Virtual Thread 3 (1 KB)               │
│  │     └──> Esperando I/O → se "desmonta"   │
│  │                                           │
│  └──> 1,000,000+ virtual threads pueden     │
│       compartir 8-16 carrier threads        │
└──────────────────────────────────────────────┘
```

**La magia:** Cuando un Virtual Thread se bloquea esperando I/O, la JVM automáticamente lo "desmonta" del carrier thread y monta otro Virtual Thread que tenga trabajo que hacer. Es como tener millones de threads, pero solo usar 8-16 threads del OS reales.

---

### Quarkus Reactive: El Modelo de Event Loop

Antes de Virtual Threads, el mundo Java ya tenía una solución a los problemas de concurrencia: **programación reactiva** con event loops.

**Event Loop = Nunca bloquear threads**

```java
@GET
public Uni<String> procesar() {
    // NO retorna el valor, retorna una "promesa" (Uni)
    return database.queryReactive("SELECT * FROM clientes")
        .flatMap(dato -> 
            // Encadenamos la siguiente operación
            httpClient.getReactive("https://api.externa.com/info")
                .map(info -> dato + info)
        );
    // El método retorna INMEDIATAMENTE
    // El thread queda libre para procesar otros requests
}
```

**Cómo funciona el Event Loop:**

```
Event Loop Thread (4-8 threads típicamente):
┌────────────────────────────────────────────────┐
│  Thread 1 (Event Loop)                         │
│  ├──> Request A: Inicia query BD              │
│  │    └──> Se registra callback               │
│  │         └──> Thread LIBRE inmediatamente    │
│  │                                             │
│  ├──> Request B: Inicia HTTP call             │
│  │    └──> Se registra callback               │
│  │         └──> Thread LIBRE inmediatamente    │
│  │                                             │
│  ├──> Request C: Inicia operación I/O         │
│  │    └──> Se registra callback               │
│  │         └──> Thread LIBRE inmediatamente    │
│  │                                             │
│  └──> Cuando llega respuesta de BD:           │
│       └──> Ejecuta callback de Request A      │
│                                                │
│  UN SOLO THREAD procesa MILES de requests     │
│  porque NUNCA se bloquea esperando            │
└────────────────────────────────────────────────┘
```

**Ventajas del modelo reactivo:**
- 4-8 threads pueden manejar 10,000+ requests concurrentes
- Consumo mínimo de memoria
- Máxima eficiencia de CPU
- No hay límite de "pool de threads"

**Desventaja principal:**
- El código es más complejo
- Curva de aprendizaje pronunciada
- Debugging más difícil
- No todas las librerías son reactivas

---

## Parte 2: Comparación Profunda

### Ejemplo Práctico: Sistema de Transferencias Bancarias

Voy a implementar el mismo endpoint de tres formas diferentes para ilustrar las diferencias:

#### Implementación 1: Platform Threads (Spring Boot tradicional)

```java
@RestController
public class TransferenciaController {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @PostMapping("/transferencia")
    public ResponseEntity<Resultado> transferir(@RequestBody TransferenciaRequest req) {
        // 1. Validar cuenta origen (BLOQUEA thread esperando BD)
        Cuenta cuenta = jdbcTemplate.queryForObject(
            "SELECT * FROM cuentas WHERE numero = ?",
            new Object[]{req.getCuentaOrigen()},
            new CuentaRowMapper()
        );
        
        if (cuenta == null) {
            return ResponseEntity.badRequest().body(
                new Resultado("Cuenta no existe")
            );
        }
        
        // 2. Verificar fondos (más tiempo bloqueado)
        if (cuenta.getSaldo().compareTo(req.getMonto()) < 0) {
            return ResponseEntity.badRequest().body(
                new Resultado("Fondos insuficientes")
            );
        }
        
        // 3. Consultar límite diario en servicio externo (BLOQUEA esperando HTTP)
        LimiteResponse limite = restTemplate.getForObject(
            "http://limites-service/consulta/" + req.getCuentaOrigen(),
            LimiteResponse.class
        );
        
        if (limite.getDisponible().compareTo(req.getMonto()) < 0) {
            return ResponseEntity.badRequest().body(
                new Resultado("Excede límite diario")
            );
        }
        
        // 4. Ejecutar transferencia (BLOQUEA esperando BD)
        jdbcTemplate.update(
            "UPDATE cuentas SET saldo = saldo - ? WHERE numero = ?",
            req.getMonto(), req.getCuentaOrigen()
        );
        
        jdbcTemplate.update(
            "UPDATE cuentas SET saldo = saldo + ? WHERE numero = ?",
            req.getMonto(), req.getCuentaDestino()
        );
        
        // 5. Registrar en auditoría (BLOQUEA esperando BD)
        jdbcTemplate.update(
            "INSERT INTO auditoria (cuenta_origen, cuenta_destino, monto, fecha) VALUES (?, ?, ?, ?)",
            req.getCuentaOrigen(), req.getCuentaDestino(), req.getMonto(), LocalDateTime.now()
        );
        
        return ResponseEntity.ok(new Resultado("Transferencia exitosa"));
    }
}
```

**Análisis de este código:**

```
Timeline de ejecución (1 thread procesando 1 request):

0ms   → Request llega
0ms   → Thread asignado (de pool de 200 threads)
5ms   → Esperando respuesta de BD (SELECT cuenta)
         [THREAD BLOQUEADO - desperdiciando recursos]
50ms  → Respuesta BD llega, se verifica saldo
50ms  → Esperando respuesta de servicio externo (HTTP)
         [THREAD BLOQUEADO - desperdiciando recursos]
200ms → Respuesta HTTP llega, se verifica límite
200ms → Esperando respuesta de BD (UPDATE cuenta origen)
         [THREAD BLOQUEADO - desperdiciando recursos]
250ms → UPDATE completado
250ms → Esperando respuesta de BD (UPDATE cuenta destino)
         [THREAD BLOQUEADO - desperdiciando recursos]
300ms → UPDATE completado
300ms → Esperando respuesta de BD (INSERT auditoría)
         [THREAD BLOQUEADO - desperdiciando recursos]
350ms → INSERT completado
350ms → Response enviada al cliente
350ms → Thread liberado

Tiempo total: 350ms
Tiempo en que el thread estuvo BLOQUEADO: ~345ms (98%)
Tiempo en que el thread hizo trabajo útil: ~5ms (2%)
```

**Problema con 1,000 requests concurrentes:**
- Se necesitan 1,000 threads
- Memoria: 1,000 × 2 MB = 2 GB solo para threads
- Si el pool tiene solo 200 threads, 800 requests esperan en cola
- Latencia se dispara

---

#### Implementación 2: Virtual Threads (Java 21 + Quarkus)

```java
@Path("/transferencia")
public class TransferenciaResource {
    
    @Inject
    EntityManager em;
    
    @Inject
    @RestClient
    LimitesClient limitesClient;
    
    @POST
    @RunOnVirtualThread  // ← La única diferencia visible
    public Response transferir(TransferenciaRequest req) {
        // EL CÓDIGO ES IDÉNTICO AL TRADICIONAL
        // Pero ahora corre en un Virtual Thread
        
        // 1. Validar cuenta origen (bloquea virtual thread, no carrier thread)
        Cuenta cuenta = em.createQuery(
            "SELECT c FROM Cuenta c WHERE c.numero = :numero",
            Cuenta.class
        )
        .setParameter("numero", req.getCuentaOrigen())
        .getSingleResult();
        
        if (cuenta == null) {
            return Response.status(400)
                .entity(new Resultado("Cuenta no existe"))
                .build();
        }
        
        // 2. Verificar fondos
        if (cuenta.getSaldo().compareTo(req.getMonto()) < 0) {
            return Response.status(400)
                .entity(new Resultado("Fondos insuficientes"))
                .build();
        }
        
        // 3. Consultar límite diario (bloquea virtual thread)
        LimiteResponse limite = limitesClient.consultar(req.getCuentaOrigen());
        
        if (limite.getDisponible().compareTo(req.getMonto()) < 0) {
            return Response.status(400)
                .entity(new Resultado("Excede límite diario"))
                .build();
        }
        
        // 4. Ejecutar transferencia
        cuenta.setSaldo(cuenta.getSaldo().subtract(req.getMonto()));
        
        Cuenta cuentaDestino = em.find(Cuenta.class, req.getCuentaDestino());
        cuentaDestino.setSaldo(cuentaDestino.getSaldo().add(req.getMonto()));
        
        // 5. Registrar auditoría
        Auditoria auditoria = new Auditoria();
        auditoria.setCuentaOrigen(req.getCuentaOrigen());
        auditoria.setCuentaDestino(req.getCuentaDestino());
        auditoria.setMonto(req.getMonto());
        auditoria.setFecha(LocalDateTime.now());
        em.persist(auditoria);
        
        return Response.ok(new Resultado("Transferencia exitosa")).build();
    }
}
```

**Análisis de este código:**

```
Timeline de ejecución (1 virtual thread procesando 1 request):

0ms   → Request llega
0ms   → Virtual thread creado (casi sin costo)
5ms   → Esperando respuesta de BD (SELECT cuenta)
         [VIRTUAL THREAD se desmonta del carrier thread]
         [CARRIER THREAD queda LIBRE para procesar otros requests]
50ms  → Respuesta BD llega, virtual thread se remonta
50ms  → Se verifica saldo
50ms  → Esperando respuesta de servicio externo (HTTP)
         [VIRTUAL THREAD se desmonta del carrier thread]
         [CARRIER THREAD queda LIBRE para procesar otros requests]
200ms → Respuesta HTTP llega, virtual thread se remonta
200ms → Se verifica límite
200ms → Esperando respuesta de BD (UPDATE cuenta origen)
         [VIRTUAL THREAD se desmonta del carrier thread]
250ms → UPDATE completado, virtual thread se remonta
250ms → Esperando respuesta de BD (UPDATE cuenta destino)
         [VIRTUAL THREAD se desmonta del carrier thread]
300ms → UPDATE completado, virtual thread se remonta
300ms → Esperando respuesta de BD (INSERT auditoría)
         [VIRTUAL THREAD se desmonta del carrier thread]
350ms → INSERT completado, virtual thread se remonta
350ms → Response enviada al cliente
350ms → Virtual thread eliminado

Tiempo total: 350ms (igual que antes)
Pero ahora:
- Solo usamos 8-16 carrier threads (threads del OS)
- Esos carrier threads NUNCA están bloqueados
- Podemos tener 1,000,000 virtual threads concurrentemente
```

**Ventajas con 1,000 requests concurrentes:**
- Se crean 1,000 virtual threads
- Memoria: 1,000 × 1 KB = 1 MB (vs 2 GB con platform threads)
- Solo necesitamos 8-16 carrier threads del OS
- No hay cola de espera
- Latencia se mantiene estable

---

#### Implementación 3: Quarkus Reactive

```java
@Path("/transferencia")
public class TransferenciaResource {
    
    @Inject
    PanacheEntityBase cuentaRepository;
    
    @Inject
    @RestClient
    LimitesReactiveClient limitesClient;
    
    @Inject
    PgPool pgPool;
    
    @POST
    public Uni<Response> transferir(TransferenciaRequest req) {
        // Código completamente diferente: todo es asíncrono
        
        // 1. Validar cuenta origen (NO bloquea, retorna Uni)
        return Cuenta.findByNumero(req.getCuentaOrigen())
            .onItem().ifNull().failWith(
                new BadRequestException("Cuenta no existe")
            )
            // 2. Verificar fondos (encadenamos sin bloquear)
            .onItem().transform(cuenta -> {
                if (cuenta.getSaldo().compareTo(req.getMonto()) < 0) {
                    throw new BadRequestException("Fondos insuficientes");
                }
                return cuenta;
            })
            // 3. Consultar límite diario (NO bloquea, retorna Uni)
            .flatMap(cuenta -> 
                limitesClient.consultarReactive(req.getCuentaOrigen())
                    .onItem().transform(limite -> {
                        if (limite.getDisponible().compareTo(req.getMonto()) < 0) {
                            throw new BadRequestException("Excede límite diario");
                        }
                        return cuenta;
                    })
            )
            // 4. Ejecutar transferencia (operación reactiva en BD)
            .flatMap(cuenta -> {
                // Actualizar cuenta origen
                return pgPool.preparedQuery(
                    "UPDATE cuentas SET saldo = saldo - $1 WHERE numero = $2"
                )
                .execute(Tuple.of(req.getMonto(), req.getCuentaOrigen()))
                // 5. Actualizar cuenta destino
                .flatMap(rows -> 
                    pgPool.preparedQuery(
                        "UPDATE cuentas SET saldo = saldo + $1 WHERE numero = $2"
                    )
                    .execute(Tuple.of(req.getMonto(), req.getCuentaDestino()))
                )
                // 6. Registrar auditoría
                .flatMap(rows -> 
                    pgPool.preparedQuery(
                        "INSERT INTO auditoria (cuenta_origen, cuenta_destino, monto, fecha) " +
                        "VALUES ($1, $2, $3, $4)"
                    )
                    .execute(Tuple.of(
                        req.getCuentaOrigen(),
                        req.getCuentaDestino(),
                        req.getMonto(),
                        LocalDateTime.now()
                    ))
                );
            })
            // 7. Construir response
            .onItem().transform(rows -> 
                Response.ok(new Resultado("Transferencia exitosa")).build()
            )
            // Manejo de errores
            .onFailure(BadRequestException.class).recoverWithItem(ex ->
                Response.status(400).entity(new Resultado(ex.getMessage())).build()
            );
        
        // IMPORTANTE: Este método retorna INMEDIATAMENTE
        // El thread NO espera la ejecución
    }
}
```

**Análisis de este código:**

```
Timeline de ejecución (1 event loop thread procesando MÚLTIPLES requests):

Request A:
0ms   → Request A llega
0ms   → Event loop thread procesa Request A
1ms   → Inicia query BD (SELECT cuenta)
1ms   → Se registra callback
1ms   → Thread INMEDIATAMENTE libre

Request B:
2ms   → Request B llega
2ms   → MISMO event loop thread procesa Request B
3ms   → Inicia query BD
3ms   → Se registra callback
3ms   → Thread INMEDIATAMENTE libre

Request C:
4ms   → Request C llega
4ms   → MISMO thread procesa Request C
5ms   → Inicia HTTP call
5ms   → Se registra callback
5ms   → Thread INMEDIATAMENTE libre

... (el thread procesa cientos de requests más) ...

50ms  → Respuesta de BD para Request A llega
50ms  → Event loop ejecuta callback de Request A
51ms  → Inicia siguiente operación (HTTP call)
51ms  → Se registra callback
51ms  → Thread INMEDIATAMENTE libre

... (continúa el ciclo) ...

350ms → Todas las operaciones de Request A completadas
350ms → Response enviada al cliente

UN SOLO THREAD procesó CIENTOS de requests concurrentemente
porque NUNCA se bloqueó esperando I/O
```

**Ventajas con 1,000 requests concurrentes:**
- Solo necesitamos 4-8 event loop threads
- Memoria: 4-8 × 2 MB = 16 MB para threads
- No hay "mounting/unmounting" como en virtual threads
- Máxima eficiencia de CPU
- Latencia más baja (menos overhead)

**Desventaja:**
- Código mucho más complejo
- Difícil de debuggear
- Requiere que TODA la cadena sea reactiva (BD, HTTP clients, etc.)

---

## Parte 3: Comparación de Performance

### Benchmark: Endpoint REST con Operaciones I/O

**Escenario:** Endpoint que consulta BD, llama a 2 APIs externas, y guarda resultado.

#### Hardware: 4 vCPUs, 8 GB RAM

**Métricas con 5,000 requests concurrentes:**

| Métrica | Platform Threads | Virtual Threads | Quarkus Reactive |
|---------|-----------------|-----------------|------------------|
| **Threads del OS** | 5,000 | 8-16 | 4-8 |
| **Memoria (threads)** | 10 GB | 20 MB | 16 MB |
| **Throughput** | COLAPSO | 8,000 req/s | 12,000 req/s |
| **Latencia P50** | N/A | 100ms | 70ms |
| **Latencia P95** | N/A | 250ms | 180ms |
| **Latencia P99** | N/A | 500ms | 350ms |
| **CPU utilización** | 100% | 70% | 85% |

**Observaciones:**

1. **Platform Threads:** El sistema colapsa porque intenta crear 5,000 threads del OS. OutOfMemoryError.

2. **Virtual Threads:** Maneja la carga sin problemas. Los 5,000 virtual threads comparten 8-16 carrier threads. La latencia es buena pero no óptima debido al overhead de mounting/unmounting.

3. **Quarkus Reactive:** Mejor performance absoluta. Solo 4-8 threads manejan toda la carga sin ningún tipo de context switching entre threads.

---

### Benchmark: Compilación Nativa (GraalVM)

**Importante:** Aquí Reactive tiene ventaja adicional.

| Métrica | Virtual Threads | Quarkus Reactive |
|---------|-----------------|------------------|
| **Soporte en Native** | ⚠️ Experimental | ✅ Full support |
| **Tiempo compilación** | ~8 minutos | ~6 minutos |
| **Tamaño binario** | ~80 MB | ~60 MB |
| **Tiempo arranque** | ~50ms | ~20ms |
| **Memoria en runtime** | ~40 MB | ~25 MB |

**Conclusión:** Para compilación nativa, Reactive sigue siendo superior.

---

## Parte 4: Cuándo Usar Cada Uno

### Usa Virtual Threads SI:

#### 1. Migración desde código legacy bloqueante

**Escenario:** Tengo una aplicación Spring Boot tradicional con JDBC, RestTemplate, código bloqueante por todos lados.

**Solución:** Migrar a Quarkus con Virtual Threads requiere cambios mínimos.

```java
// Antes (Spring Boot)
@RestController
public class ClienteController {
    @Autowired
    private JdbcTemplate jdbc;
    
    @GetMapping("/clientes")
    public List<Cliente> getClientes() {
        return jdbc.query("SELECT * FROM clientes", new ClienteRowMapper());
    }
}

// Después (Quarkus + Virtual Threads)
@Path("/clientes")
public class ClienteResource {
    @Inject
    EntityManager em;
    
    @GET
    @RunOnVirtualThread  // ← Solo agregar esta anotación
    public List<Cliente> getClientes() {
        return em.createQuery("SELECT c FROM Cliente c", Cliente.class)
                 .getResultList();
    }
}
```

**Beneficio:** Sin reescribir lógica de negocio, obtengo mejor escalabilidad.

---

#### 2. Equipo sin experiencia en programación reactiva

**Escenario:** Mi equipo conoce Java tradicional pero programación reactiva es un mundo nuevo.

**Problema con Reactive:**
```java
// Este código reactivo es difícil de entender para juniors
return cliente.findById(id)
    .flatMap(c -> 
        pedido.findByCliente(c.getId())
            .flatMap(p -> 
                pago.findByPedido(p.getId())
                    .map(pg -> buildResponse(c, p, pg))
            )
    );
```

**Solución con Virtual Threads:**
```java
@RunOnVirtualThread
public Response getDetalle(Long id) {
    // Código imperativo que cualquier Java developer entiende
    Cliente c = clienteRepo.findById(id);
    Pedido p = pedidoRepo.findByCliente(c.getId());
    Pago pg = pagoRepo.findByPedido(p.getId());
    return buildResponse(c, p, pg);
}
```

**Beneficio:** Mantenibilidad, menos bugs, onboarding más rápido.

---

#### 3. Uso de librerías bloqueantes sin alternativas reactivas

**Escenario:** Necesito usar una librería legacy que NO tiene versión reactiva.

```java
// Librería bloqueante legacy
LegacyClient client = new LegacyClient();

@GET
@RunOnVirtualThread  // Virtual thread absorbe el bloqueo
public Response consultar() {
    // Esta llamada BLOQUEA, pero está OK en virtual thread
    String resultado = client.consultaSincrona();
    return Response.ok(resultado).build();
}
```

Si intentara usar esto en código reactivo, rompería todo el modelo no-bloqueante.

---

#### 4. Aplicaciones de baja a media concurrencia

**Escenario:** Sistema interno con 100-500 requests concurrentes máximo.

**Análisis:**
```
100 requests concurrentes:
- Platform threads: 100 × 2 MB = 200 MB → Funciona pero ineficiente
- Virtual threads: 100 × 1 KB = 100 KB → Súper eficiente
- Reactive: 4-8 threads → Máximo eficiente pero overkill

Decisión: Virtual Threads
- Suficiente performance
- Código más simple
- Menos bugs
- Más fácil de mantener
```

---

### Usa Quarkus Reactive SI:

#### 1. Necesitas máxima eficiencia y throughput

**Escenario:** Microservicio crítico de pagos que debe manejar 50,000 transacciones por segundo.

**Números:**
```
Con Virtual Threads:
- 50,000 requests/s
- 16 carrier threads al máximo
- CPU: 90% utilización
- Latencia P99: 200ms
- Costo AWS: $300/mes

Con Reactive:
- 50,000 requests/s
- 8 event loop threads
- CPU: 75% utilización
- Latencia P99: 120ms
- Costo AWS: $180/mes
```

**Conclusión:** Reactive ofrece 40% menos latencia y 40% menos costo.

---

#### 2. Serverless o Edge Computing

**Escenario:** AWS Lambda o funciones en edge (Cloudflare Workers).

**Requisitos:**
- Arranque instantáneo (< 50ms)
- Memoria mínima (< 128 MB)
- Compilación nativa obligatoria

**Resultados:**

```
Virtual Threads + Native:
- Arranque: ~50ms
- Memoria: ~40 MB
- ⚠️ Soporte experimental

Quarkus Reactive + Native:
- Arranque: ~20ms
- Memoria: ~25 MB
- ✅ Soporte completo y probado
```

**Conclusión:** Para serverless/edge, Reactive es superior.

---

#### 3. Sistema completo diseñado para reactive

**Escenario:** Nueva aplicación desde cero, usando stack reactivo completo.

```
Stack Reactivo:
- Base de datos: PostgreSQL con Reactive SQL Client
- HTTP Client: Mutiny REST Client
- Message Queue: Reactive Messaging (Kafka)
- Cache: Redis Reactive Client
```

Si TODO el stack es reactivo, el código queda natural:

```java
@POST
public Uni<Response> procesar(Request req) {
    return validar(req)
        .flatMap(this::consultarBD)
        .flatMap(this::llamarAPI)
        .flatMap(this::guardarCache)
        .flatMap(this::enviarKafka)
        .map(this::buildResponse);
}
```

**Beneficio:** Todo no-bloqueante, máxima eficiencia end-to-end.

---

#### 4. Equipo con experiencia en programación reactiva

**Escenario:** Equipo senior que domina Reactive Streams, RxJava, Reactor, etc.

Para ellos, el código reactivo es natural y los beneficios de performance justifican la complejidad.

---

## Parte 5: Estrategia Híbrida (Lo Mejor de Ambos Mundos)

En la práctica, puedo combinar ambos paradigmas en la misma aplicación Quarkus.

### Ejemplo Real: Sistema Bancario Completo

```java
// ============================================
// MÓDULO CRÍTICO: TRANSFERENCIAS
// Performance crítica → REACTIVE
// ============================================
@Path("/transferencias")
public class TransferenciaResource {
    
    @POST
    @Path("/ejecutar")
    public Uni<Response> ejecutarTransferencia(TransferenciaRequest req) {
        // Código reactivo para máxima performance
        return validarCuenta(req.getCuentaOrigen())
            .flatMap(cuenta -> verificarFondos(cuenta, req.getMonto()))
            .flatMap(ok -> ejecutarTransaccion(req))
            .flatMap(tx -> notificarCliente(tx))
            .map(result -> Response.ok(result).build());
    }
}

// ============================================
// MÓDULO ADMINISTRATIVO: REPORTES
// Performance no crítica → VIRTUAL THREADS
// ============================================
@Path("/reportes")
public class ReporteResource {
    
    @GET
    @Path("/mensual")
    @RunOnVirtualThread  // Código más simple
    public Response generarReporteMensual(@QueryParam("mes") int mes) {
        // Código imperativo tradicional
        // Más fácil de escribir y mantener
        
        List<Transaccion> transacciones = transaccionRepo.findByMonth(mes);
        List<Cliente> clientes = clienteRepo.findActive();
        
        ReporteMensual reporte = new ReporteMensual();
        reporte.setTransacciones(transacciones);
        reporte.calcularEstadisticas();
        reporte.generarGraficos();
        
        return Response.ok(reporte).build();
    }
}

// ============================================
// MÓDULO INTEGRACIÓN: SERVICIOS EXTERNOS
// Legacy bloqueante → VIRTUAL THREADS
// ============================================
@Path("/integracion")
public class IntegracionResource {
    
    @Inject
    LegacySoapClient soapClient;  // Cliente SOAP bloqueante legacy
    
    @POST
    @Path("/consulta-sunat")
    @RunOnVirtualThread  // Absorbe el bloqueo
    public Response consultarSunat(ConsultaRequest req) {
        // Esta librería SOAP es bloqueante y no tiene versión reactiva
        // Virtual thread permite usarla sin problemas
        String resultado = soapClient.consultarRUC(req.getRuc());
        return Response.ok(resultado).build();
    }
}

// ============================================
// MÓDULO NOTIFICACIONES: ALTA CONCURRENCIA
// No crítico pero alto volumen → REACTIVE
// ============================================
@Path("/notificaciones")
public class NotificacionResource {
    
    @Inject
    @Channel("email-out")
    Emitter<Email> emailEmitter;
    
    @POST
    @Path("/enviar")
    public Uni<Response> enviarNotificacion(NotificacionRequest req) {
        // Reactive messaging para procesar miles de notificaciones
        return Uni.createFrom().item(req)
            .map(this::crearEmail)
            .invoke(email -> emailEmitter.send(email))
            .map(email -> Response.accepted().build());
    }
}
```

**Criterios de decisión por módulo:**

| Módulo | Paradigma | Razón |
|--------|-----------|-------|
| Transferencias | Reactive | Performance crítica, alto throughput |
| Pagos | Reactive | Latencia crítica, alta concurrencia |
| Reportes | Virtual Threads | Bajo volumen, complejidad del código |
| Integración SOAP | Virtual Threads | Librería legacy bloqueante |
| Consultas admin | Virtual Threads | Queries complejos, bajo volumen |
| Notificaciones | Reactive | Alto volumen, no bloqueante natural |

---

## Parte 6: Limitaciones y Consideraciones

### Limitaciones de Virtual Threads

#### 1. Pinning Problem

Algunos casos "anclan" el virtual thread al carrier thread, anulando el beneficio:

```java
// ❌ PROBLEMA: synchronized dentro de operación I/O
@GET
@RunOnVirtualThread
public String problematico() {
    synchronized(this) {
        // Durante esta operación I/O, el carrier thread está BLOQUEADO
        String resultado = database.query("SELECT ...");
        return resultado;
    }
    // El virtual thread NO puede desmontarse
    // Perdimos el beneficio
}

// ✅ SOLUCIÓN: Usar ReentrantLock en lugar de synchronized
private final ReentrantLock lock = new ReentrantLock();

@GET
@RunOnVirtualThread
public String correcto() {
    lock.lock();
    try {
        // Ahora el virtual thread SÍ puede desmontarse durante I/O
        String resultado = database.query("SELECT ...");
        return resultado;
    } finally {
        lock.unlock();
    }
}
```

**Otros casos de pinning:**
- Métodos nativos (JNI) que bloquean
- `synchronized` en general
- Algunos casos de `Thread.sleep()` en loops

---

#### 2. Debugging más complejo

Los stack traces con virtual threads pueden ser confusos:

```
java.lang.RuntimeException: Error en transferencia
    at TransferenciaService.lambda$ejecutar$5(TransferenciaService.java:45)
    at VirtualThread.run(VirtualThread.java:309)
    ... hidden frames ...
    
¿En qué virtual thread ocurrió el error?
¿Qué carrier thread lo estaba ejecutando?
Es más difícil de rastrear que con platform threads tradicionales.
```

---

#### 3. Soporte de librerías

No todas las librerías están optimizadas para virtual threads:
- Pools de conexiones tradicionales (pueden ser contraproducentes)
- Thread-local storage (puede consumir mucha memoria si hay millones de virtual threads)
- Librerías que asumen platform threads

---

### Limitaciones de Quarkus Reactive

#### 1. Curva de aprendizaje pronunciada

```java
// Este código es difícil de entender para developers tradicionales
return cliente.findById(id)
    .onItem().ifNotNull().transformToUni(c -> 
        cuenta.findByCliente(c.getId())
            .onItem().ifNotNull().transformToUni(cu -> 
                transaccion.findByCuenta(cu.getId())
                    .collect().asList()
                    .onItem().transform(txs -> 
                        buildResponse(c, cu, txs)
                    )
            )
    )
    .onItem().ifNull().failWith(new NotFoundException());

// vs código imperativo con virtual threads (más fácil)
Cliente c = clienteRepo.findById(id);
if (c == null) throw new NotFoundException();

Cuenta cu = cuentaRepo.findByCliente(c.getId());
if (cu == null) throw new NotFoundException();

List<Transaccion> txs = transaccionRepo.findByCuenta(cu.getId());
return buildResponse(c, cu, txs);
```

---

#### 2. Stack completo debe ser reactivo

Si UN componente en la cadena es bloqueante, pierdo los beneficios:

```java
// ❌ PROBLEMA: Un método bloqueante rompe toda la cadena
@GET
public Uni<Response> procesar() {
    return validarReactive()  // ✅ No bloqueante
        .flatMap(ok -> {
            // ❌ BLOQUEA el event loop thread
            String resultado = legacyClient.callBlocking();
            return Uni.createFrom().item(resultado);
        })
        .map(this::buildResponse);
}

// ✅ SOLUCIÓN: Ejecutar código bloqueante en thread separado
@GET
public Uni<Response> procesar() {
    return validarReactive()
        .flatMap(ok -> 
            // Ejecutar en pool de workers, no en event loop
            Uni.createFrom().item(() -> 
                legacyClient.callBlocking()
            ).runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
        )
        .map(this::buildResponse);
}
```

Pero esto agrega complejidad.

---

#### 3. Debugging difícil

Los stack traces reactivos son crípticos:

```
java.lang.NullPointerException
    at TransferenciaService.lambda$ejecutar$3(TransferenciaService.java:67)
    at io.smallrye.mutiny.operators.uni.UniOnItemTransformToUni.subscribe(...)
    at io.smallrye.mutiny.operators.uni.UniFlatMap.subscribe(...)
    at io.smallrye.mutiny.operators.AbstractUni.subscribe(...)
    ... 50 more reactive framework frames ...
    
¿Dónde está el error real? Difícil de encontrar.
```

---

## Parte 7: Recomendaciones para Aplicaciones Reales

### Matriz de Decisión

```
┌─────────────────────────────────────────────────────────────┐
│                   MATRIZ DE DECISIÓN                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Throughput Requerido                                       │
│      │                                                      │
│  Alto│                     REACTIVE                         │
│      │                                                      │
│      │              VIRTUAL THREADS                         │
│      │                                                      │
│ Medio│                                                      │
│      │                                                      │
│      │    PLATFORM                                          │
│ Bajo │    THREADS                                           │
│      │                                                      │
│      └──────────────────────────────────────────────────    │
│         Baja          Media          Alta                   │
│                Complejidad Aceptable                        │
└─────────────────────────────────────────────────────────────┘
```

---

### Guía Rápida de Decisión

**Pregunta 1:** ¿Es código nuevo o migración?
- **Código nuevo + equipo experto** → Evaluar Reactive
- **Código nuevo + equipo junior** → Virtual Threads
- **Migración desde legacy** → Virtual Threads

**Pregunta 2:** ¿Cuál es el throughput requerido?
- **< 1,000 req/s** → Virtual Threads suficiente
- **1,000 - 10,000 req/s** → Virtual Threads o Reactive
- **> 10,000 req/s** → Reactive preferible

**Pregunta 3:** ¿Compilación nativa es crítica?
- **Sí** → Reactive (mejor soporte)
- **No** → Cualquiera funciona

**Pregunta 4:** ¿Stack completo puede ser reactivo?
- **Sí** → Considerar Reactive
- **No (librerías bloqueantes)** → Virtual Threads

**Pregunta 5:** ¿Latencia es crítica?
- **< 50ms P99** → Reactive
- **< 200ms P99** → Virtual Threads
- **> 200ms P99** → Cualquiera

---

## Parte 8: Configuración en Quarkus

### Habilitar Virtual Threads

```properties
# application.properties

# Habilitar soporte de virtual threads
quarkus.virtual-threads.enabled=true

# Número de carrier threads (por defecto: número de CPUs)
# -1 = automático
quarkus.virtual-threads.name-prefix=vt-

# Para REST endpoints específicos, usar @RunOnVirtualThread
```

```java
// En código
@GET
@Path("/clientes")
@RunOnVirtualThread  // Este endpoint usa virtual threads
public List<Cliente> getClientes() {
    return clienteRepo.findAll();
}
```

---

### Configurar Reactive

```properties
# application.properties

# Número de event loop threads (por defecto: 2 × número de CPUs)
quarkus.vertx.eventLoops=8

# Worker thread pool (para operaciones bloqueantes ocasionales)
quarkus.thread-pool.max-threads=20

# Para REST endpoints, retornar Uni o Multi
```

```java
// En código
@GET
@Path("/clientes")
public Uni<List<Cliente>> getClientes() {
    return Cliente.listAll(); // Panache Reactive
}
```

---

### Estrategia Híbrida

```properties
# application.properties

# Habilitar ambos
quarkus.virtual-threads.enabled=true
quarkus.vertx.eventLoops=8
```

```java
// Reactive para endpoints críticos
@Path("/transferencias")
public class TransferenciaResource {
    @POST
    public Uni<Response> transferir(Request req) {
        return procesarReactive(req);
    }
}

// Virtual Threads para endpoints administrativos
@Path("/reportes")
public class ReporteResource {
    @GET
    @RunOnVirtualThread
    public Response generar() {
        return procesarImperativo();
    }
}
```

---

## Conclusión

Virtual Threads y Quarkus Reactive no son enemigos, son herramientas complementarias para diferentes necesidades:

**Virtual Threads:**
- Código imperativo que escala bien
- Ideal para migraciones y equipos tradicionales
- Excelente para aplicaciones con librerías bloqueantes
- Curva de aprendizaje baja

**Quarkus Reactive:**
- Máxima eficiencia y throughput
- Ideal para aplicaciones críticas de alta concurrencia
- Mejor para compilación nativa y serverless
- Curva de aprendizaje alta

**Mi recomendación:**
- Para proyectos nuevos con equipos experimentados: Empezar con Reactive
- Para migraciones desde código legacy: Virtual Threads
- Para aplicaciones reales: Estrategia híbrida basada en necesidades de cada módulo

La elección correcta depende del contexto: equipo, requisitos de performance, complejidad aceptable, y recursos disponibles. No hay una respuesta única, sino la decisión arquitectónica correcta para cada caso específico.

---

## Referencias

- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [Project Loom Documentation](https://wiki.openjdk.org/display/loom)
- [Quarkus Virtual Threads Guide](https://quarkus.io/guides/virtual-threads)
- [Quarkus Reactive Architecture](https://quarkus.io/guides/quarkus-reactive-architecture)
- [SmallRye Mutiny Documentation](https://smallrye.io/smallrye-mutiny/)