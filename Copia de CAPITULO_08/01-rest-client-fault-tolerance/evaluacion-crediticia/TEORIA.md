# 📖 Teoría: Integración y Consumo de Servicios con Quarkus

## 📑 Tabla de Contenidos

1. [Introducción a Arquitecturas Distribuidas](#1-introducción-a-arquitecturas-distribuidas)
2. [REST Client en Quarkus](#2-rest-client-en-quarkus)
3. [Patrones de Tolerancia a Fallos](#3-patrones-de-tolerancia-a-fallos)
4. [Configuración Externalizada](#4-configuración-externalizada)
5. [Validación de Datos](#5-validación-de-datos)
6. [Mejores Prácticas](#6-mejores-prácticas)

---

## 1. Introducción a Arquitecturas Distribuidas

### 1.1 ¿Qué son los Microservicios?

Un **microservicio** es un componente de software pequeño, autónomo e independiente que realiza una función específica del negocio. En lugar de construir una aplicación monolítica gigante, dividimos la funcionalidad en servicios más pequeños que se comunican entre sí.

**Ejemplo bancario:**
```
APLICACIÓN MONOLÍTICA (TODO EN UNO):
┌─────────────────────────────────────┐
│  Banco Tradicional S.A.             │
│  - Cuentas                          │
│  - Créditos                         │
│  - Transferencias                   │
│  - Tarjetas                         │
│  - Reportes                         │
└─────────────────────────────────────┘

ARQUITECTURA DE MICROSERVICIOS (DIVIDIDA):
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  Servicio    │  │  Servicio    │  │  Servicio    │
│  Cuentas     │  │  Créditos    │  │Transferencias│
└──────────────┘  └──────────────┘  └──────────────┘
       ↓                 ↓                  ↓
   [Base Datos]     [Base Datos]       [Base Datos]
```

**Ventajas:**
- ✅ Escalabilidad independiente (escalar solo Transferencias si es necesario)
- ✅ Despliegue independiente (actualizar Créditos sin afectar Cuentas)
- ✅ Tecnologías heterogéneas (Créditos en Quarkus, Cuentas en Spring)
- ✅ Equipos autónomos (un equipo por servicio)

**Desventajas:**
- ❌ Complejidad de comunicación (servicios deben hablarse por red)
- ❌ Transacciones distribuidas (más difíciles de manejar)
- ❌ Debugging complejo (error puede estar en cualquier servicio)
- ❌ **Fallos en cascada** ← Aquí entran los patrones de resiliencia

---

### 1.2 La Falacia de la Red Perfecta

En un monolito, cuando un método llama a otro:
```java
public void crearCuenta() {
    validarDocumentos();  // ← Llamada en memoria, instantánea
}
```

En microservicios, cuando un servicio llama a otro:
```java
public void crearCuenta() {
    documentosClient.validar();  // ← Llamada HTTP por red
}
```

**¿Qué puede salir mal?**
- 🔥 La red puede fallar
- 🐌 La red puede ser lenta
- 💥 El servicio destino puede estar caído
- ⏱️ El servicio puede tardar mucho
- 🔄 La respuesta puede perderse

**Las 8 Falacias de la Computación Distribuida** (Peter Deutsch):
1. La red es confiable ← **FALSO**
2. La latencia es cero ← **FALSO**
3. El ancho de banda es infinito ← **FALSO**
4. La red es segura ← **FALSO**
5. La topología no cambia ← **FALSO**
6. Hay un solo administrador ← **FALSO**
7. El costo de transporte es cero ← **FALSO**
8. La red es homogénea ← **FALSO**

**Conclusión:** En arquitecturas distribuidas, **debemos diseñar para el fallo**.

---

## 2. REST Client en Quarkus

### 2.1 ¿Qué es un REST Client?

Un **REST Client** es un componente que consume (llama) APIs REST externas. Quarkus implementa el estándar **MicroProfile REST Client**, que permite crear clientes de forma **declarativa** usando interfaces.

**Enfoque tradicional (imperativo):**
```java
// Código manual con HttpClient
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("http://api.example.com/usuarios/123"))
    .header("Authorization", "Bearer token")
    .GET()
    .build();

HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
String json = response.body();
Usuario usuario = objectMapper.readValue(json, Usuario.class);
```

**Enfoque declarativo con REST Client:**
```java
// Solo defines la interfaz
@Path("/usuarios")
@RegisterRestClient(configKey = "usuario-api")
public interface UsuarioClient {
    
    @GET
    @Path("/{id}")
    Usuario obtenerUsuario(@PathParam("id") Long id);
}

// Y la usas así:
@Inject
@RestClient
UsuarioClient usuarioClient;

Usuario usuario = usuarioClient.obtenerUsuario(123L);
```

**Ventajas del enfoque declarativo:**
- ✅ Menos código (no manejas HTTP manualmente)
- ✅ Más legible (parece un método local)
- ✅ Serialización automática JSON ↔ Java
- ✅ Integración con CDI (inyección de dependencias)
- ✅ Fácil de testear (puedes mockear la interfaz)

---

### 2.2 Anatomía de un REST Client

```java
@Path("/api/bureau")                              // ← Ruta base
@RegisterRestClient(configKey = "bureau-credito") // ← Identificador de config
public interface BureauCreditoClient {

    @GET                                    // ← Método HTTP
    @Path("/consulta/{dni}")               // ← Ruta específica
    RespuestaBureau consultarHistorial(    // ← Tipo de retorno
        @PathParam("dni") String dni,      // ← Path parameter
        @HeaderParam("X-API-Key") String apiKey  // ← Header personalizado
    );
}
```

**Tipos de parámetros:**

| Anotación | Dónde va | Ejemplo |
|-----------|----------|---------|
| `@PathParam` | En la URL | `/usuarios/{id}` → `123` |
| `@QueryParam` | Query string | `/buscar?nombre=Juan` |
| `@HeaderParam` | HTTP Headers | `Authorization: Bearer token` |
| `@FormParam` | Form data | En formularios POST |
| Sin anotación | Request Body | JSON/XML en el cuerpo |

---

### 2.3 Configuración del REST Client

Cada REST Client necesita configuración en `application.properties`:

```properties
# Configuración básica
quarkus.rest-client.bureau-credito.url=https://api-bureau.pe
quarkus.rest-client.bureau-credito.scope=jakarta.inject.Singleton

# Timeouts (opcional)
quarkus.rest-client.bureau-credito.connect-timeout=5000  # 5 segundos
quarkus.rest-client.bureau-credito.read-timeout=10000    # 10 segundos

# Headers globales (opcional)
quarkus.rest-client.bureau-credito.headers.User-Agent=MiBanco/1.0
```

**Nota:** El `configKey` conecta la interfaz con la configuración.

---

### 2.4 Inyección y Uso

```java
@ApplicationScoped
public class MiServicio {

    @Inject
    @RestClient  // ← Indica que es un REST Client
    BureauCreditoClient bureauClient;

    public void evaluar(String dni) {
        RespuestaBureau respuesta = bureauClient.consultarHistorial(dni, "API_KEY");
        // Usar respuesta...
    }
}
```

---

## 3. Patrones de Tolerancia a Fallos

### 3.1 ¿Por qué son necesarios?

En arquitecturas distribuidas, los servicios **FALLARÁN**. No es cuestión de "si", sino de "cuándo". Los patrones de tolerancia a fallos (Fault Tolerance) nos permiten:

1. **Recuperarnos** de fallos temporales
2. **Proteger** al sistema de fallos en cascada
3. **Degradar** el servicio elegantemente
4. **Monitorear** el estado de dependencias externas

Quarkus implementa **MicroProfile Fault Tolerance**, que proporciona 5 anotaciones principales:

---

### 3.2 @Retry - Reintentos Automáticos

**Concepto:** Si una operación falla, vuélvelo a intentar.

**¿Cuándo usar?**
- Fallos de red transitorios
- Servicios temporalmente sobrecargados
- Timeouts esporádicos

**¿Cuándo NO usar?**
- Errores de validación (400 Bad Request)
- Errores de autenticación (401 Unauthorized)
- Errores lógicos de negocio

**Sintaxis:**
```java
@Retry(
    maxRetries = 3,              // Máximo 3 reintentos
    delay = 1000,                // Espera 1 segundo entre reintentos
    delayUnit = ChronoUnit.MILLIS,
    maxDuration = 10000,         // Abandona después de 10 segundos total
    durationUnit = ChronoUnit.MILLIS,
    retryOn = {IOException.class},      // Solo reintenta en estos errores
    abortOn = {IllegalArgumentException.class}  // No reintentar en estos
)
public RespuestaBureau consultarBureau(String dni) {
    return bureauClient.consultarHistorial(dni, apiKey);
}
```

**Estrategias de espera:**
- **Fija:** Siempre espera el mismo tiempo (1s, 1s, 1s)
- **Exponencial:** Duplica el tiempo (1s, 2s, 4s, 8s) ← No disponible por defecto en MP FT
- **Jitter:** Agrega aleatoriedad para evitar "thundering herd"

**Ejemplo de ejecución:**
```
Intento 1: ❌ Fallo (IOException: Connection timeout)
Espera 1 segundo...
Intento 2: ❌ Fallo (IOException: Connection refused)
Espera 1 segundo...
Intento 3: ✅ Éxito (respuesta OK)
→ Retorna respuesta al llamador
```

**Métricas importantes:**
- Número de reintentos totales
- Tiempo total de ejecución
- Tasa de éxito después de reintentos

---

### 3.3 @Timeout - Límite de Tiempo

**Concepto:** Si una operación tarda más de X tiempo, cancélala.

**¿Por qué es crítico?**
- Previene "thread starvation" (hilos bloqueados esperando)
- Mejora la experiencia del usuario
- Evita acumulación de requests (back pressure)

**Sintaxis:**
```java
@Timeout(
    value = 3,                   // Máximo 3 segundos
    unit = ChronoUnit.SECONDS
)
public RespuestaScoring calcularScoring(SolicitudCredito solicitud) {
    return scoringClient.calcularScore(solicitud.getDni(), ...);
}
```

**¿Qué sucede cuando expira?**
1. Se lanza `org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException`
2. La operación se cancela (si es cancelable)
3. Si hay @Fallback, se ejecuta

**Valores recomendados:**
- **APIs rápidas (CRUD):** 1-3 segundos
- **APIs de cálculo:** 5-10 segundos
- **APIs de procesamiento:** 10-30 segundos
- **Nunca más de 60 segundos** en microservicios sincrónicos

**Consideración:** El timeout se aplica a **cada intento** de @Retry, no al total.

```java
@Retry(maxRetries = 3)
@Timeout(value = 2, unit = ChronoUnit.SECONDS)
public String llamar() { ... }

// Tiempo máximo total: 3 intentos × 2 segundos = 6 segundos
```

---

### 3.4 @Fallback - Plan B

**Concepto:** Si una operación falla, ejecuta un método alternativo.

**¿Cuándo usar?**
- Necesitas responder SÍ o SÍ (no puedes retornar error)
- Tienes una alternativa razonable
- Puedes degradar el servicio temporalmente

**Sintaxis:**
```java
@Fallback(fallbackMethod = "scoringBasicoFallback")
public RespuestaScoring calcularScoring(SolicitudCredito solicitud) {
    // Intenta calcular con ML avanzado
    return scoringClient.calcularScore(...);
}

// Método fallback: MISMA firma que el método original
public RespuestaScoring scoringBasicoFallback(SolicitudCredito solicitud) {
    // Cálculo básico sin ML
    RespuestaScoring fallback = new RespuestaScoring();
    fallback.setScoreInterno(500);  // Score neutral
    fallback.setRecomendacion("REVISAR_MANUAL");
    return fallback;
}
```

**Reglas del método fallback:**
1. ✅ Debe tener la **misma firma** (parámetros y tipo de retorno)
2. ✅ Debe estar en la **misma clase**
3. ✅ Puede ser `public`, `protected` o `private`
4. ✅ Debe ser **súper confiable** (sin llamadas HTTP, sin DB si es posible)
5. ❌ NO debe lanzar excepciones (o todo falla)

**Estrategias de fallback:**

| Estrategia | Descripción | Ejemplo |
|------------|-------------|---------|
| **Valor por defecto** | Retornar constante | Score neutral 500 |
| **Caché** | Usar valor guardado | Último precio conocido |
| **Degradación** | Algoritmo simple | Scoring básico vs ML |
| **Servicio alternativo** | Llamar a otro endpoint | API backup |
| **Stub/Mock** | Datos ficticios | Usuario demo |

**Orden de ejecución:**
```java
@Timeout(3s)      // 1. Aplica timeout
@Retry(3 veces)   // 2. Reintenta si falla
@Fallback(...)    // 3. Si todo falla, usa fallback
public String metodo() { ... }
```

---

### 3.5 @CircuitBreaker - Disyuntor

**Concepto:** Monitorea fallos y "abre el circuito" si detecta que un servicio está caído, evitando llamadas innecesarias.

**Analogía eléctrica:**
```
┌─────────────┐
│  Disyuntor  │  ← Si hay cortocircuito, CORTA la electricidad
│   [CLOSED]  │     para proteger la casa
└─────────────┘
```

**Estados del Circuit Breaker:**

```
     Llamadas exitosas
            │
   ┌────────▼────────┐
   │  CLOSED         │  ← Estado normal, todo funciona
   │  (Circuito      │
   │   cerrado)      │
   └────────┬────────┘
            │
            │ Detecta fallos > threshold
            │
   ┌────────▼────────┐
   │  OPEN           │  ← Circuito abierto, NO llama al servicio
   │  (Circuito      │     Falla INMEDIATAMENTE
   │   abierto)      │
   └────────┬────────┘
            │
            │ Después de delay
            │
   ┌────────▼────────┐
   │  HALF_OPEN      │  ← Permite 1 llamada de prueba
   │  (Medio         │
   │   abierto)      │
   └────┬────────┬───┘
        │        │
    Falla       Éxito
        │        │
   ┌────▼───┐ ┌─▼──────┐
   │  OPEN  │ │ CLOSED │
   └────────┘ └────────┘
```

**Sintaxis:**
```java
@CircuitBreaker(
    requestVolumeThreshold = 4,    // Necesita mínimo 4 requests para evaluar
    failureRatio = 0.5,            // Si 50% fallan, abre el circuito
    delay = 10,                     // Espera 10 segundos antes de probar
    delayUnit = ChronoUnit.SECONDS,
    successThreshold = 2            // Necesita 2 éxitos para cerrar
)
public RespuestaScoring calcularScoring(SolicitudCredito solicitud) {
    return scoringClient.calcularScore(...);
}
```

**Parámetros explicados:**

| Parámetro | Descripción | Valor recomendado |
|-----------|-------------|-------------------|
| `requestVolumeThreshold` | Mínimo de requests para decidir | 4-10 |
| `failureRatio` | % de fallos para abrir | 0.5 (50%) |
| `delay` | Tiempo en OPEN antes de probar | 5-60 segundos |
| `successThreshold` | Éxitos para cerrar de nuevo | 1-3 |

**Ejemplo de ejecución:**

```
Estado: CLOSED (normal)
Request 1: ✅ OK
Request 2: ❌ FALLO
Request 3: ❌ FALLO
Request 4: ✅ OK
→ 2 de 4 fallaron (50%) → ABRE CIRCUITO

Estado: OPEN (circuito abierto)
Request 5: ⚡ Falla INSTANTÁNEAMENTE sin llamar (CircuitBreakerOpenException)
Request 6: ⚡ Falla INSTANTÁNEAMENTE
... (espera 10 segundos) ...

Estado: HALF_OPEN (probando)
Request 7: ✅ OK (1 éxito)
Request 8: ✅ OK (2 éxitos) → CIERRA CIRCUITO

Estado: CLOSED (vuelve a normal)
```

**Beneficios:**
- ✅ Evita sobrecargar servicios caídos
- ✅ Respuestas más rápidas (fail-fast)
- ✅ Permite que servicios se recuperen
- ✅ Reduce costos (menos llamadas HTTP)

**Combinación potente:**
```java
@Retry(maxRetries = 3)              // Reintenta fallos temporales
@Timeout(value = 3, unit = SECONDS) // No espera más de 3s
@Fallback(fallbackMethod = "plan_b") // Usa plan B si falla
@CircuitBreaker(...)                // Aprende y protege
public String metodoRobusto() {
    // Tu código
}
```

---

### 3.6 @Bulkhead - Aislamiento de Recursos

**Concepto:** Limita cuántas llamadas concurrentes puede haber a un servicio, previniendo que un servicio lento consuma todos los threads.

**Analogía naval:**
Un barco tiene compartimentos separados (bulkheads). Si uno se inunda, los otros permanecen secos.

**Sintaxis:**
```java
@Bulkhead(
    value = 5,  // Máximo 5 llamadas concurrentes
    waitingTaskQueue = 10  // Cola de espera de 10
)
public String llamarServicioLento() {
    return servicioLento.procesar();
}
```

**¿Qué pasa si se excede?**
- Se lanza `BulkheadException`
- La request espera en la cola (si hay espacio)
- Si la cola está llena, falla inmediatamente

**Tipos:**
- **Semaphore-based:** Limita threads (por defecto)
- **Thread pool-based:** Pool dedicado de threads

---

### 3.7 @Asynchronous - Ejecución Asíncrona

**Concepto:** Ejecuta el método en otro thread, retornando un `CompletionStage`.

**Sintaxis:**
```java
@Asynchronous
@Timeout(value = 5, unit = ChronoUnit.SECONDS)
public CompletionStage<String> llamarAsync() {
    return CompletableFuture.supplyAsync(() -> {
        return servicioExterno.llamar();
    });
}
```

**Uso:**
```java
CompletionStage<String> future = servicio.llamarAsync();
future.thenAccept(resultado -> {
    System.out.println("Resultado: " + resultado);
});
```

---

## 4. Configuración Externalizada

### 4.1 ¿Por qué externalizar configuración?

**Principio:** El mismo binario (JAR/container) debe poder ejecutarse en diferentes ambientes sin recompilarse.

```
MISMO CÓDIGO:
┌──────────────┐
│  app.jar     │
└──────────────┘
       │
       ├─────> DEV    (usa config dev)
       ├─────> TEST   (usa config test)
       └─────> PROD   (usa config prod)
```

---

### 4.2 Fuentes de Configuración en Quarkus

**Orden de prioridad** (de mayor a menor):

1. **System Properties** (`-Dproperty=value`)
2. **Environment Variables** (`export PROPERTY=value`)
3. **application.properties** (o `.yaml`)
4. **Valores por defecto en código**

```properties
# application.properties
api.bureau.url=http://localhost:8080

# Variable de entorno (sobrescribe)
export API_BUREAU_URL=https://api-prod.bureau.pe

# System property (sobrescribe todo)
java -Dapi.bureau.url=https://api-test.bureau.pe -jar app.jar
```

---

### 4.3 Inyección de Configuración

**Opción 1: @ConfigProperty (valores individuales)**
```java
@ConfigProperty(name = "api.bureau.key")
String bureauApiKey;

@ConfigProperty(name = "api.timeout", defaultValue = "5000")
Integer timeout;
```

**Opción 2: @ConfigMapping (objetos completos)**
```java
@ConfigMapping(prefix = "api.bureau")
public interface BureauConfig {
    String url();
    String apiKey();
    Optional<Integer> timeout();
}

// Uso:
@Inject
BureauConfig config;

String url = config.url();
```

---

### 4.4 Perfiles de Quarkus

**Perfiles predefinidos:**
- `%dev` - Desarrollo (quarkus:dev)
- `%test` - Testing (JUnit)
- `%prod` - Producción

```properties
# Aplica a todos
api.bureau.url=http://localhost:8080

# Solo en dev
%dev.api.bureau.url=http://localhost:8080

# Solo en prod
%prod.api.bureau.url=https://api.bureau.pe
%prod.api.bureau.timeout=3000
```

---

## 5. Validación de Datos

### 5.1 Bean Validation (JSR 380)

**Concepto:** Validar automáticamente objetos usando anotaciones.

**Anotaciones comunes:**

| Anotación | Validación | Ejemplo |
|-----------|------------|---------|
| `@NotNull` | No puede ser null | `@NotNull String nombre` |
| `@NotBlank` | No puede ser null, vacío o solo espacios | `@NotBlank String dni` |
| `@NotEmpty` | No puede ser null o vacío | `@NotEmpty List<String> items` |
| `@Size` | Tamaño entre min y max | `@Size(min=8, max=11) String dni` |
| `@Min` | Valor mínimo | `@Min(0) Integer edad` |
| `@Max` | Valor máximo | `@Max(150) Integer edad` |
| `@Positive` | Número positivo | `@Positive Double monto` |
| `@Email` | Email válido | `@Email String correo` |
| `@Pattern` | Regex | `@Pattern(regexp="[0-9]{8}") String dni` |

---

### 5.2 Validación en REST Endpoints

```java
@POST
@Path("/credito")
public Response evaluarCredito(@Valid SolicitudCredito solicitud) {
    // Si la validación falla, nunca entra aquí
    // Quarkus retorna automáticamente 400 Bad Request
}
```

**Clase con validaciones:**
```java
public class SolicitudCredito {

    @NotBlank(message = "El DNI es obligatorio")
    @Size(min = 8, max = 8, message = "DNI debe tener 8 dígitos")
    private String dni;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a cero")
    @Max(value = 1000000, message = "Monto máximo: S/ 1,000,000")
    private Double montoSolicitado;

    @Min(value = 1, message = "Plazo mínimo: 1 mes")
    @Max(value = 60, message = "Plazo máximo: 60 meses")
    private Integer mesesPlazo;
}
```

**Respuesta de error automática:**
```json
{
  "title": "Constraint Violation",
  "status": 400,
  "violations": [
    {
      "field": "dni",
      "message": "El DNI es obligatorio"
    },
    {
      "field": "montoSolicitado",
      "message": "El monto debe ser mayor a cero"
    }
  ]
}
```

---

### 5.3 Validación Personalizada

```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DniValidator.class)
public @interface DniValido {
    String message() default "DNI inválido";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class DniValidator implements ConstraintValidator<DniValido, String> {
    @Override
    public boolean isValid(String dni, ConstraintValidatorContext context) {
        if (dni == null) return false;
        // Lógica de validación (módulo 11, etc.)
        return dni.matches("[0-9]{8}") && calcularDigitoVerificador(dni);
    }
}
```

---

## 6. Mejores Prácticas

### 6.1 Diseño de APIs REST

**Principios RESTful:**

| Recurso | GET (Leer) | POST (Crear) | PUT (Actualizar) | DELETE (Eliminar) |
|---------|------------|--------------|------------------|-------------------|
| `/usuarios` | Listar todos | Crear nuevo | - | - |
| `/usuarios/123` | Ver detalle | - | Actualizar | Eliminar |

**Códigos HTTP:**
- `200 OK` - Éxito general
- `201 Created` - Recurso creado
- `204 No Content` - Éxito sin contenido (DELETE)
- `400 Bad Request` - Error de validación
- `401 Unauthorized` - No autenticado
- `403 Forbidden` - No autorizado
- `404 Not Found` - Recurso no existe
- `500 Internal Server Error` - Error del servidor
- `503 Service Unavailable` - Servicio caído

---

### 6.2 Manejo de Errores

**Nunca exponer stack traces al cliente:**
```java
// ❌ MAL
@POST
public Response crear(Solicitud s) {
    try {
        servicio.procesar(s);
        return Response.ok().build();
    } catch (Exception e) {
        return Response.serverError()
            .entity(e.getMessage())  // ← Expone detalles internos
            .build();
    }
}

// ✅ BIEN
@POST
public Response crear(Solicitud s) {
    try {
        servicio.procesar(s);
        return Response.ok().build();
    } catch (Exception e) {
        logger.error("Error procesando solicitud", e);  // ← Log completo
        return Response.serverError()
            .entity("Error interno del servidor")  // ← Mensaje genérico
            .build();
    }
}
```

---

### 6.3 Timeouts Razonables

**Regla general:**
```
Timeout del cliente > Timeout del servidor
```

Si el servidor tiene timeout de 30s, el cliente debe tener 35s para recibir la respuesta de error.

**Recomendaciones:**
- API Gateway: 60s
- Servicio REST: 30s
- Llamadas a BD: 5s
- Llamadas a caché: 1s

---

### 6.4 Logging y Observabilidad

**Logs estructurados:**
```java
// ❌ MAL
logger.info("Llamando a bureau para dni " + dni);

// ✅ BIEN
logger.info("Llamando a bureau", 
    kv("dni", dni), 
    kv("servicio", "bureau-credito"));
```

**Métricas importantes:**
- Número de llamadas (total, por endpoint)
- Latencia (percentiles p50, p95, p99)
- Tasa de errores (4xx, 5xx)
- Activaciones de circuit breaker
- Reintentos ejecutados

---

### 6.5 Testing de REST Clients

**Con WireMock:**
```java
@QuarkusTest
public class BureauClientTest {

    @InjectMock
    @RestClient
    BureauCreditoClient bureauClient;

    @Test
    public void testConsultaExitosa() {
        // Mock la respuesta
        RespuestaBureau mockRespuesta = new RespuestaBureau(...);
        when(bureauClient.consultarHistorial(anyString(), anyString()))
            .thenReturn(mockRespuesta);

        // Ejecutar test
        RespuestaBureau respuesta = servicio.consultarBureau("12345678");
        
        // Verificar
        assertThat(respuesta.getScoreBureau()).isEqualTo(750);
    }

    @Test
    public void testTimeout() {
        // Simular timeout
        when(bureauClient.consultarHistorial(anyString(), anyString()))
            .thenThrow(new TimeoutException());

        // Verificar que @Fallback se activa
        RespuestaBureau respuesta = servicio.consultarBureau("12345678");
        assertThat(respuesta).isNotNull();  // Fallback retorna algo
    }
}
```

---

## 📚 Resumen de Conceptos Clave

| Concepto | ¿Qué resuelve? | Cuándo usar |
|----------|----------------|-------------|
| **REST Client** | Consumir APIs externas | Siempre en microservicios |
| **@Retry** | Fallos temporales | Red intermitente |
| **@Timeout** | Servicios lentos | Siempre (protección básica) |
| **@Fallback** | Necesidad de responder | Cuando hay plan B |
| **@CircuitBreaker** | Servicios caídos | Siempre (protección avanzada) |
| **@Bulkhead** | Aislamiento de recursos | Servicios muy lentos |
| **Config externalizada** | Diferentes ambientes | Siempre |
| **Validación** | Datos incorrectos | Siempre en endpoints |

---

## 🎓 Conclusión

La construcción de microservicios resilientes no es opcional, es **obligatoria** en sistemas modernos distribuidos. Los patrones de tolerancia a fallos (Retry, Timeout, Fallback, Circuit Breaker) son las herramientas fundamentales que permiten que nuestros sistemas:

1. **Sobrevivan** a fallos de red y servicios externos
2. **Se recuperen** automáticamente de errores temporales
3. **Degraden** elegantemente cuando algo falla
4. **Protejan** al sistema completo de fallos en cascada

Quarkus, con su implementación de MicroProfile Fault Tolerance, nos proporciona estas herramientas de forma declarativa y simple. Lo que antes requería cientos de líneas de código imperativo, ahora son simples anotaciones.

**Recuerda:** En sistemas distribuidos, **diseñar para el fallo es diseñar para el éxito**.

---

## 📖 Referencias y Lecturas Adicionales

1. **Quarkus REST Client Guide**  
   https://quarkus.io/guides/rest-client

2. **MicroProfile Fault Tolerance Specification**  
   https://microprofile.io/project/eclipse/microprofile-fault-tolerance

3. **Libro: "Release It!" by Michael Nygard**  
   El libro definitivo sobre resiliencia en sistemas de producción

4. **Artículo: "Fallacies of Distributed Computing"**  
   https://en.wikipedia.org/wiki/Fallacies_of_distributed_computing

5. **Netflix Tech Blog - Circuit Breaker**  
   https://netflixtechblog.com/

6. **Martin Fowler - Circuit Breaker Pattern**  
   https://martinfowler.com/bliki/CircuitBreaker.html

---

**¡Fin del documento teórico!**  
Este material cubre todos los conceptos necesarios para entender el ejercicio de Evaluación Crediticia.
EOF
```

¡Listo mi estimado! Ya tienes tu **TEORIA.md** completo. 📚

Este documento cubre:
- ✅ Arquitecturas distribuidas y microservicios
- ✅ REST Client en Quarkus (completo)
- ✅ Los 5 patrones de Fault Tolerance (detallados)
- ✅ Configuración externalizada
- ✅ Validación de datos
- ✅ Mejores prácticas
- ✅ Referencias adicionales
