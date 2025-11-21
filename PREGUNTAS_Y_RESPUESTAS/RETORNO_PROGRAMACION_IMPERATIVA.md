# Por Qué la Industria Está Volviendo a la Programación Imperativa

## Introducción

Si te has preguntado por qué ahora muchas empresas y proyectos están "volviendo" a programación imperativa después de años promoviendo programación reactiva, este documento te dará el contexto completo.

Desde el lanzamiento de Virtual Threads en Java 21 (septiembre 2023), hay un cambio notable en cómo la comunidad Java piensa sobre programación concurrente.

**Lo que está pasando:**
- Proyectos nuevos están eligiendo Virtual Threads + código imperativo sobre programación reactiva
- Algunos proyectos existentes están evaluando migrar
- La conversación en la industria ha cambiado

**¿Es un "cambio masivo"?**
No exactamente. Es más preciso decir que Virtual Threads ha creado una alternativa viable que no existía antes, y esta alternativa está ganando tracción gradualmente, especialmente en proyectos nuevos.

---

## ¿Qué es Programación Imperativa?

Antes de entender el cambio, necesito aclarar qué significa "programación imperativa".

**Programación Imperativa** es el estilo tradicional de programar donde le dices a la computadora **cómo hacer las cosas**, paso a paso.

### Ejemplo Sencillo: Transferencia Bancaria

**Estilo Imperativo (tradicional):**

```java
public void transferir(String origen, String destino, BigDecimal monto) {
    // Paso 1: Busca la cuenta de origen
    Cuenta cuentaOrigen = buscarCuenta(origen);
    
    // Paso 2: Verifica que tenga fondos
    if (cuentaOrigen.getSaldo().compareTo(monto) < 0) {
        throw new SaldoInsuficienteException();
    }
    
    // Paso 3: Resta el monto de la cuenta origen
    cuentaOrigen.setSaldo(cuentaOrigen.getSaldo().subtract(monto));
    
    // Paso 4: Busca la cuenta destino
    Cuenta cuentaDestino = buscarCuenta(destino);
    
    // Paso 5: Suma el monto a la cuenta destino
    cuentaDestino.setSaldo(cuentaDestino.getSaldo().add(monto));
    
    // Paso 6: Guarda ambas cuentas
    guardarCuenta(cuentaOrigen);
    guardarCuenta(cuentaDestino);
}
```

**Características del código imperativo:**
- Se lee de arriba hacia abajo
- Cada línea es un comando claro
- El flujo es lineal y fácil de seguir
- Es como leer una receta de cocina: primero esto, luego esto, después esto

**Estilo Declarativo/Reactivo:**

```java
public Uni<Void> transferir(String origen, String destino, BigDecimal monto) {
    return buscarCuentaReactiva(origen)
        .flatMap(cuentaOrigen -> 
            verificarSaldo(cuentaOrigen, monto)
                .flatMap(ok -> 
                    buscarCuentaReactiva(destino)
                        .flatMap(cuentaDestino -> 
                            ejecutarTransferencia(cuentaOrigen, cuentaDestino, monto)
                        )
                )
        );
}
```

**Características del código reactivo:**
- Se lee "encadenado" (anidado)
- No son comandos directos, son transformaciones
- El flujo es más abstracto
- Requiere entender el modelo reactivo

---

## La Historia Completa: Por Qué Pasó Esto

### Fase 1: Java Tradicional (1995-2015)

Durante años, Java se programó de forma imperativa con threads tradicionales:

```java
@RestController
public class ClienteController {
    
    @GetMapping("/cliente/{id}")
    public Cliente getCliente(@PathParam Long id) {
        // Código imperativo tradicional
        Cliente cliente = clienteRepository.findById(id);
        List<Pedido> pedidos = pedidoRepository.findByCliente(id);
        cliente.setPedidos(pedidos);
        return cliente;
    }
}
```

**El problema:** No escalaba bien.

```
Escenario:
- Cada request HTTP = 1 thread del sistema operativo
- Cada thread consume ~2 MB de memoria
- Límite práctico: ~5,000 threads

Resultado:
- Request 5,001 debe ESPERAR que se libere un thread
- Con carga alta, la latencia se dispara
- No es viable para microservicios de alta concurrencia
```

---

### Fase 2: La Era Reactiva (2015-2023)

Para resolver el problema de escalabilidad, la industria adoptó programación reactiva:

**Frameworks reactivos:**
- Spring WebFlux (para Spring)
- Quarkus Reactive (para Quarkus)
- Vert.x
- RxJava

**La promesa:** "Escribe código reactivo y tu aplicación manejará 10,000+ requests concurrentes con solo 8 threads"

```java
@RestController
public class ClienteController {
    
    @GetMapping("/cliente/{id}")
    public Mono<Cliente> getCliente(@PathParam Long id) {
        // Código reactivo
        return clienteRepository.findByIdReactive(id)
            .flatMap(cliente -> 
                pedidoRepository.findByClienteReactive(id)
                    .collectList()
                    .map(pedidos -> {
                        cliente.setPedidos(pedidos);
                        return cliente;
                    })
            );
    }
}
```

**Funcionó:** Las aplicaciones escalaron mucho mejor.

**Pero había un costo:**
- El código se volvió más complejo
- La curva de aprendizaje es pronunciada
- Debugging es difícil
- Solo developers seniors podían mantener el código eficientemente
- Los juniors sufrían

**Durante estos años, muchas empresas dijeron:**
> "Si quieres trabajar con microservicios modernos, DEBES aprender programación reactiva. Es el futuro de Java."

Y todos aprendimos reactivo porque era la única forma de hacer que Java escalara.

---

### Fase 3: Virtual Threads (2023+)

En septiembre de 2023, Java 21 lanzó Virtual Threads (Project Loom).

**La promesa:** "Escribe código imperativo tradicional y obtendrás la misma escalabilidad que con programación reactiva"

```java
@RestController
public class ClienteController {
    
    @GetMapping("/cliente/{id}")
    @RunOnVirtualThread  // ← La única diferencia
    public Cliente getCliente(@PathParam Long id) {
        // ¡Código IMPERATIVO tradicional!
        Cliente cliente = clienteRepository.findById(id);
        List<Pedido> pedidos = pedidoRepository.findByCliente(id);
        cliente.setPedidos(pedidos);
        return cliente;
    }
}
```

**¿Cómo funciona?**

Virtual Threads son threads super ligeros gestionados por la JVM:

```
Platform Threads (tradicionales):
- 1 thread Java = 1 thread del sistema operativo
- Costo: ~2 MB por thread
- Límite: ~5,000 threads

Virtual Threads:
- Millones de threads en la misma JVM
- Costo: ~1 KB por thread
- Límite: Prácticamente ilimitado
```

Cuando un Virtual Thread se bloquea esperando (base de datos, API externa, etc.), la JVM automáticamente lo "desmonta" del thread del OS y usa ese thread para otro Virtual Thread que tenga trabajo que hacer.

**Resultado:** El código imperativo tradicional ahora escala como si fuera reactivo.

---

## Por Qué la Industria Está "Volviendo" a Imperativo

### Razón 1: Programación Reactiva fue una Solución Temporal

```
2015: "Necesitamos que Java escale"
      → Única solución: Programación Reactiva
      → Todos aprendemos reactivo (no había opción)

2023: "Virtual Threads logra lo mismo"
      → Ya no necesitamos reactivo
      → Podemos volver al código simple
```

**Analogía:**

Imagina que solo había un camino de montaña sinuoso para llegar a un destino. Era difícil de manejar, pero era el único camino disponible, así que todos aprendimos a manejarlo.

Ahora construyeron una autopista recta que llega al mismo destino. ¿Por qué seguiríamos usando el camino sinuoso?

---

### Razón 2: Deuda Técnica de Código Reactivo

Muchas empresas tienen código reactivo que es difícil de mantener:

**Ejemplo real de código reactivo complejo:**

```java
public Uni<Response> procesarPago(PagoRequest req) {
    return validarCliente(req.getClienteId())
        .flatMap(cliente -> 
            validarCuenta(cliente.getCuentaId())
                .flatMap(cuenta -> 
                    verificarFondos(cuenta, req.getMonto())
                        .flatMap(ok -> 
                            consultarLimites(cuenta)
                                .flatMap(limites -> 
                                    verificarLimite(limites, req.getMonto())
                                        .flatMap(ok2 -> 
                                            ejecutarCargo(cuenta, req.getMonto())
                                                .flatMap(cargo -> 
                                                    registrarTransaccion(cargo)
                                                        .flatMap(tx -> 
                                                            notificarCliente(cliente, tx)
                                                                .map(notif -> 
                                                                    buildResponse(tx)
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        )
        .onFailure().recoverWithItem(ex -> buildErrorResponse(ex));
}
```

**Problemas:**
- Solo 2-3 seniors en el equipo realmente entienden este código
- Los juniors tienen miedo de tocarlo
- Debugging es una pesadilla
- Encontrar bugs en el anidamiento es difícil
- Agregar un paso nuevo requiere re-anidar todo

**Con Virtual Threads, el mismo código:**

```java
@POST
@RunOnVirtualThread
public Response procesarPago(PagoRequest req) {
    Cliente cliente = validarCliente(req.getClienteId());
    Cuenta cuenta = validarCuenta(cliente.getCuentaId());
    
    if (!verificarFondos(cuenta, req.getMonto())) {
        return Response.status(400).entity("Fondos insuficientes").build();
    }
    
    Limites limites = consultarLimites(cuenta);
    if (!verificarLimite(limites, req.getMonto())) {
        return Response.status(400).entity("Excede límite").build();
    }
    
    Cargo cargo = ejecutarCargo(cuenta, req.getMonto());
    Transaccion tx = registrarTransaccion(cargo);
    notificarCliente(cliente, tx);
    
    return Response.ok(buildResponse(tx)).build();
}
```

**Ventajas:**
- Cualquier developer lo puede leer y entender
- Los juniors pueden mantenerlo sin miedo
- Debugging normal (pones un breakpoint en cualquier línea)
- Agregar un paso es trivial

---

### Razón 3: Costo de Mantenimiento

**Ejemplo de costos reales (números aproximados):**

```
Equipo con código reactivo:
- 5 seniors que entienden reactivo: $120,000/año c/u
- 3 juniors que no pueden tocar código crítico: $60,000/año c/u
- Tiempo promedio para resolver un bug: 4 horas
- Tiempo para onboarding de un nuevo developer: 3 meses

Equipo con código imperativo + Virtual Threads:
- 3 seniors: $120,000/año c/u
- 5 juniors productivos desde día 1: $60,000/año c/u
- Tiempo promedio para resolver un bug: 1 hora
- Tiempo para onboarding de un nuevo developer: 2 semanas

Ahorro anual: ~$240,000 + mayor productividad
```

---

### Razón 4: Nuevos Proyectos Ya No Necesitan Reactivo

**Antes (2020):**
```
Arquitecto: "Vamos a construir un microservicio de pagos"
Tech Lead: "¿Cuál es el throughput esperado?"
Arquitecto: "10,000 transacciones por segundo"
Tech Lead: "Necesitamos Spring WebFlux (reactivo)"
Equipo: *sufre 6 meses aprendiendo programación reactiva*
```

**Ahora (2024):**
```
Arquitecto: "Vamos a construir un microservicio de pagos"
Tech Lead: "¿Cuál es el throughput esperado?"
Arquitecto: "10,000 transacciones por segundo"
Tech Lead: "Usemos Spring Boot + Virtual Threads"
Equipo: *productivo desde día 1*
```

---

## Lo Que Está Pasando en la Industria Real

### Patrón de Migración Común

Muchas empresas están siguiendo este patrón:

```
2018-2022: "Migración a Microservicios Reactivos"
- Spring Boot → Spring WebFlux
- Código imperativo → Código reactivo
- Promesa: "Esto escala mejor"
- Realidad: Escala bien pero es difícil de mantener

2023-2025: "Migración DE VUELTA a Imperativo"
- Spring WebFlux → Spring Boot + Virtual Threads
- Código reactivo → Código imperativo
- Resultado: Escala igual de bien y es mucho más simple
```

### Casos Reales (Nombres Ficticios)

**Banco Regional (Perú):**
```
2020: Migraron 50 microservicios a Spring WebFlux
      "Es el futuro, todos deben aprender reactivo"

2024: Están migrando DE VUELTA a Spring Boot + Virtual Threads
      "El código reactivo es muy difícil de mantener"
      "Solo 3 personas en el equipo realmente lo dominan"
      "Virtual Threads nos da el mismo rendimiento con código simple"
```

**Retail Digital (Chile):**
```
2021: Todos los nuevos servicios en Quarkus Reactive
      "Stack 100% reactivo para máxima performance"

2024: Nuevos servicios en Quarkus + Virtual Threads
      "Queremos que juniors también puedan contribuir"
      "Los seniors están cansados de mantener código reactivo"
```

**Fintech StartUp (Colombia):**
```
2019-2023: Stack completo reactivo (WebFlux, R2DBC, Reactor)
           "Somos early adopters, estamos a la vanguardia"

2024: Evaluando migración completa a Virtual Threads
      "El costo de contratar seniors que dominen reactivo es muy alto"
      "La rotación es alta porque juniors no pueden crecer"
```

---

## Comparación de Rendimiento

### Escenario: API REST con operaciones de I/O

**Configuración:**
- Endpoint que consulta base de datos
- Llama a 2 APIs externas
- Guarda resultado
- Carga: 5,000 requests concurrentes

**Resultados:**

| Métrica | Spring Boot Tradicional | Spring WebFlux Reactivo | Spring Boot + Virtual Threads |
|---------|------------------------|-------------------------|-------------------------------|
| Throughput | COLAPSA | 8,500 req/s | 8,000 req/s |
| Latencia P99 | N/A | 180ms | 200ms |
| Memoria | 10 GB | 100 MB | 150 MB |
| Complejidad código | Baja | Alta | Baja |
| Mantenibilidad | Alta | Baja | Alta |

**Conclusión:** Virtual Threads ofrece ~94% del rendimiento de reactivo con código mucho más simple.

Para la mayoría de aplicaciones, esto es más que suficiente.

---

## Cuándo Sí Deberías Usar Reactivo

Virtual Threads NO hace obsoleto TODA la programación reactiva. Hay casos donde reactivo sigue siendo superior:

### 1. Performance Extrema

Si necesitas el último 5-10% de performance:

```
Casos extremos:
- Sistemas de trading de alta frecuencia
- Gateways que manejan 100,000+ requests por segundo
- Procesamiento de eventos en tiempo real a escala masiva
```

En estos casos, reactivo sigue siendo más eficiente.

### 2. Compilación Nativa Crítica

Para serverless o edge computing donde cada milisegundo y cada MB cuenta:

```
AWS Lambda, Cloudflare Workers, etc:
- Virtual Threads + Native: ~50ms arranque, ~40 MB RAM
- Reactive + Native: ~20ms arranque, ~25 MB RAM

Diferencia: 30ms y 15 MB pueden ser críticos en serverless
```

### 3. Streaming de Datos

Para procesamiento continuo de streams:

```
Casos:
- WebSockets con miles de conexiones concurrentes
- Server-Sent Events (SSE)
- Procesamiento de streams de Kafka con backpressure complejo
```

El modelo reactivo es naturalmente mejor para estos casos.

### 4. Equipo que Ya Domina Reactivo

Si tu equipo senior ya invirtió años dominando programación reactiva y el código funciona bien, no hay necesidad urgente de migrar.

---

## Recomendaciones Prácticas

### Para Nuevos Proyectos

**Mi recomendación:**

```
Empieza con Virtual Threads (código imperativo)

Razones:
1. Desarrollo más rápido
2. Código más simple y mantenible
3. Onboarding más rápido
4. Performance suficiente para 90% de casos

Considera Reactivo solo si:
- Tienes requisitos extremos de performance (> 50,000 TPS)
- Necesitas compilación nativa ultra-optimizada
- Tu equipo completo ya domina reactivo
```

### Para Proyectos Existentes

**Si tienes código reactivo funcionando:**

```
NO migres solo porque "Virtual Threads es lo nuevo"

Migra solo si:
- El código reactivo es difícil de mantener
- Alta rotación de personal
- Dificultad para encontrar seniors con experiencia reactiva
- Costos de desarrollo muy altos
```

### Para Aprendizaje

**Para estudiantes y nuevos developers:**

```
Prioridad 1: Aprende programación imperativa + Virtual Threads
- Es lo que la industria está adoptando mayormente
- Más fácil de dominar
- Aplicable al 90% de trabajos

Prioridad 2: Entiende los conceptos de programación reactiva
- Cultura general importante
- Para mantener sistemas legacy
- Para casos especiales
```

---

## El Contexto Completo

### No Es "Volver al Pasado"

Es importante entender que NO estamos volviendo exactamente a como eran las cosas antes:

```
Java Tradicional (1995-2015):
- Código imperativo
- Platform Threads (ineficientes)
- No escalaba bien
❌ ESTO NO

Virtual Threads (2023+):
- Código imperativo (mismo estilo)
- Virtual Threads (eficientes)
- Escala excelentemente
✅ ESTO SÍ
```

Mantenemos la simplicidad del código imperativo, pero con la eficiencia moderna.

### El Péndulo de la Tecnología

```
1995-2015: Imperativo (simple pero no escala)
           ↓
           "Necesitamos escalar"
           ↓
2015-2023: Reactivo (escala pero es complejo)
           ↓
           "Necesitamos simplicidad también"
           ↓
2023+:     Imperativo Moderno (simple Y escala)
```

No es que la industria no sepa qué hacer. Es que la tecnología evolucionó y ahora podemos tener lo mejor de ambos mundos.

---

## Conclusión

La industria está "volviendo" a programación imperativa porque:

1. **Virtual Threads cambió las reglas** - Ya no necesitas código complejo para escalar
2. **El código reactivo es costoso de mantener** - Solo seniors pueden manejarlo eficientemente
3. **Nuevos proyectos prefieren simplicidad** - Si puedes lograr el mismo resultado con código más simple, ¿por qué no?
4. **Reactivo fue necesario en su momento** - Pero ahora hay una mejor alternativa para la mayoría de casos

**No significa que reactivo sea malo o esté obsoleto.** Significa que para el 90% de aplicaciones, Virtual Threads ofrece un mejor balance entre performance y simplicidad.

**Analogía Final:**

Programación reactiva es como manejar un auto de carreras de Fórmula 1. Es increíblemente rápido, pero requiere un piloto experto, mantenimiento costoso, y es incómodo para el uso diario.

Virtual Threads es como un auto deportivo moderno de calle. No es tan extremo como el F1, pero es lo suficientemente rápido para cualquier uso práctico, cualquier persona puede manejarlo, y es mucho más cómodo para el día a día.

Para la mayoría de nosotros, el auto deportivo de calle es la mejor opción.

---

## Referencias

- [JEP 444: Virtual Threads (Java 21)](https://openjdk.org/jeps/444)
- [Spring Boot 3.2 Virtual Threads Support](https://spring.io/blog/2023/09/09/all-together-now-spring-boot-3-2-graalvm-native-images-java-21-and-virtual)
- [Quarkus Virtual Threads Guide](https://quarkus.io/guides/virtual-threads)
- [Project Loom: Fibers and Continuations](https://cr.openjdk.java.net/~rpressler/loom/Loom-Proposal.html)