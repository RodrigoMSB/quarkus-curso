# Customer Service - Conceptos Teóricos

## 📚 Índice

1. [Arquitectura General](#arquitectura-general)
2. [Patrón Active Record con Panache](#patrón-active-record-con-panache)
3. [Cifrado a Nivel de Aplicación](#cifrado-a-nivel-de-aplicación)
4. [Caché Distribuido con Redis](#caché-distribuido-con-redis)
5. [Fault Tolerance Patterns](#fault-tolerance-patterns)
6. [Seguridad JWT y OIDC](#seguridad-jwt-y-oidc)
7. [Arquitectura Cloud-Native](#arquitectura-cloud-native)

---

## 🏗️ Arquitectura General

### Capas del Microservicio

```
┌──────────────────────────────────────────┐
│         CustomerResource (REST)          │  ← Capa de presentación
│  - @Path, @GET, @POST                    │  ← RESTEasy Reactive
│  - @RolesAllowed, @Valid                 │  ← Seguridad y validación
└──────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────┐
│         CustomerService (Lógica)         │  ← Capa de negocio
│  - @Transactional                        │  ← Manejo transaccional
│  - @CacheResult, @CacheInvalidate        │  ← Gestión de caché
│  - @Retry, @CircuitBreaker               │  ← Tolerancia a fallos
└──────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────┐
│         Customer (Entidad)               │  ← Capa de persistencia
│  - extends PanacheEntity                 │  ← Active Record
│  - @Entity, @Column                      │  ← JPA/Hibernate
│  - static findByXxx()                    │  ← Métodos de consulta
└──────────────────────────────────────────┘
                    ↓
               PostgreSQL
```

**Principios aplicados:**
- **Separation of Concerns** - Cada capa tiene responsabilidad única
- **Dependency Injection** - CDI maneja las dependencias
- **Transaction Management** - Declarativo con @Transactional
- **API First** - Contrato definido (OpenAPI)

---

## 📦 Patrón Active Record con Panache

### ¿Qué es Active Record?

Un patrón donde **la entidad misma contiene métodos de persistencia**, simplificando el código al eliminar la necesidad de repositorios explícitos.

### Comparación: JPA Tradicional vs Panache

**JPA Tradicional:**
```java
@Repository
public class CustomerRepository {
    @PersistenceContext
    EntityManager em;
    
    public Customer findById(Long id) {
        return em.find(Customer.class, id);
    }
    
    public List<Customer> findByIndustry(String industry) {
        return em.createQuery(
            "SELECT c FROM Customer c WHERE c.industry = :industry",
            Customer.class)
            .setParameter("industry", industry)
            .getResultList();
    }
}
```

**Con Panache (Active Record):**
```java
@Entity
public class Customer extends PanacheEntity {
    // Campos...
    
    public static Customer findById(Long id) {
        return find("id", id).firstResult();
    }
    
    public static List<Customer> findByIndustry(String industry) {
        return list("industry", industry);
    }
}
```

### Ventajas de Panache

1. **Menos boilerplate** - ~70% menos código
2. **Type-safe** - Métodos estáticos tipados
3. **Sintaxis fluida** - Queries legibles
4. **Repository pattern opcional** - Puedes mezclar ambos enfoques

### Cuándo usar Active vs Repository

| Active Record | Repository Pattern |
|---------------|-------------------|
| ✅ Aplicaciones pequeñas/medianas | ✅ Aplicaciones empresariales grandes |
| ✅ Lógica de negocio simple | ✅ Lógica compleja de persistencia |
| ✅ Desarrollo rápido | ✅ Testing más fácil (mockear repos) |

---

## 🔐 Cifrado a Nivel de Aplicación

### Google Tink: Cifrado de Caja Negra

**Tink** es una librería de Google que **abstrae la complejidad criptográfica**, evitando errores comunes:

❌ **Errores comunes sin Tink:**
- Usar AES sin IV (Initialization Vector)
- No autenticar el cifrado (vulnerable a tampering)
- Manejar claves en código fuente
- No rotar claves

✅ **Con Tink:**
```java
// Inicialización (una sola vez)
AeadConfig.register();
KeysetHandle keysetHandle = KeysetHandle.generateNew(
    AeadKeyTemplates.AES256_GCM  // ← Cifrado autenticado
);
Aead aead = keysetHandle.getPrimitive(Aead.class);

// Cifrar
byte[] ciphertext = aead.encrypt(plaintext, associatedData);

// Descifrar
byte[] plaintext = aead.decrypt(ciphertext, associatedData);
```

### AEAD: Authenticated Encryption with Associated Data

**AES-GCM** proporciona:
1. **Confidencialidad** - Nadie puede leer sin la clave
2. **Integridad** - Detecta modificaciones
3. **Autenticación** - Verifica origen del mensaje

### Arquitectura de Cifrado en Customer Service

```
┌─────────────┐     plaintext     ┌──────────────┐
│   Service   │ ───────────────→  │ TinkEncryption│
│             │                   │   .encrypt()  │
└─────────────┘                   └──────────────┘
                                         ↓
                                  ┌──────────────┐
                                  │  Google Tink │
                                  │  AES-256-GCM │
                                  └──────────────┘
                                         ↓
                                  ciphertext (Base64)
                                         ↓
                                  ┌──────────────┐
                                  │  PostgreSQL  │
                                  │ (almacenado) │
                                  └──────────────┘
```

### Always Encrypted vs Tink

| Always Encrypted (SQL Server) | Tink (Aplicación) |
|-------------------------------|-------------------|
| Cifrado transparente en DB | Cifrado en código |
| No requiere cambios en app | Requiere lógica explícita |
| Dificulta búsquedas | Permite búsquedas si se cifra la query |
| Protege contra DBA malicioso | Protege datos en tránsito y reposo |

**En producción:** Combinar ambos para defensa en profundidad.

---

## 💾 Caché Distribuido con Redis

### ¿Por qué Redis?

1. **In-memory** - Latencia ~1ms (vs ~50ms en DB)
2. **Distribuido** - Compartido entre instancias
3. **Tipos de datos ricos** - Strings, Hashes, Sets, etc.
4. **Persistencia opcional** - RDB snapshots + AOF logs

### Anatomía del Cache en Customer Service

```java
@CacheResult(cacheName = "customers")
public CustomerResponse getCustomer(Long id) {
    // Este código solo se ejecuta si NO está en caché
    Customer customer = Customer.findById(id);
    return mapToResponse(customer);
}

@CacheInvalidate(cacheName = "customers")
public CustomerResponse updateCustomer(Long id, ...) {
    // Invalida la entrada del caché automáticamente
}
```

### Flujo de Ejecución

```
1. GET /api/customers/123
         ↓
2. Buscar en Redis: "customers:123"
         ↓
   ┌─────────────────────────┐
   │  ¿Existe en caché?      │
   └─────────────────────────┘
         │
    ┌────┴────┐
    │         │
  SÍ         NO
    │         │
    ↓         ↓
 Redis    PostgreSQL
    │         │
    └────┬────┘
         ↓
   Retornar al cliente
```

### Estrategias de Cache

| Estrategia | Uso | Ejemplo |
|-----------|-----|---------|
| **Cache-Aside** | Lectura frecuente | `@CacheResult` |
| **Write-Through** | Consistencia crítica | Actualizar caché en escritura |
| **Write-Behind** | Alto throughput | Buffer de escrituras |

### TTL (Time To Live)

```properties
# Configuración de expiración
quarkus.cache.redis.expire-after-write=1h
quarkus.cache.redis.customers.expire-after-write=30m
```

### Cache Warming

Pre-cargar datos críticos al inicio:

```java
@Startup
@ApplicationScoped
public class CacheWarmer {
    @Inject CustomerService service;
    
    void warmCache(@Observes StartupEvent event) {
        // Cargar top 100 clientes más consultados
    }
}
```

---

## 🛡️ Fault Tolerance Patterns

### Circuit Breaker: Proteger Servicios Externos

**Analogía:** Interruptor eléctrico que se abre cuando hay cortocircuito.

```
Estados del Circuit Breaker:

    CLOSED ──[5 errores]──→ OPEN
       ↑                      ↓
       │                   [10 seg]
       │                      ↓
       └──[éxito]──── HALF_OPEN
```

**Implementación:**
```java
@CircuitBreaker(
    requestVolumeThreshold = 5,  // Mínimo 5 peticiones
    failureRatio = 0.5,           // 50% de fallos
    delay = 10000                 // 10 seg antes de reintentar
)
@CircuitBreakerName("sunat-validation")
private boolean validateWithSunat(String ruc) {
    return sunatClient.validateRuc(ruc).valid();
}
```

### Retry: Reintentos Inteligentes

```java
@Retry(
    maxRetries = 2,    // Máximo 2 reintentos
    delay = 500        // 500ms entre intentos
)
```

**Backoff exponencial:** 500ms → 1s → 2s

### Timeout: Evitar Bloqueos

```java
@Timeout(3000)  // Máximo 3 segundos
```

### Fallback: Plan B

```java
@Fallback(fallbackMethod = "fallbackSunatValidation")
private boolean validateWithSunat(String ruc) {
    // Método principal
}

private boolean fallbackSunatValidation(String ruc) {
    LOG.warn("SUNAT no disponible");
    return false; // Valor por defecto
}
```

### Combinación de Patrones

```java
@Timeout(3000)
@Retry(maxRetries = 2)
@CircuitBreaker(...)
@Fallback(...)
public Response callExternalService() {
    // Orden de ejecución:
    // 1. Timeout
    // 2. Retry
    // 3. Circuit Breaker
    // 4. Fallback (si todo falla)
}
```

---

## 🔑 Seguridad JWT y OIDC

### OAuth 2.0 + OpenID Connect

**OAuth 2.0:** Protocolo de autorización ("¿Qué puede hacer?")
**OpenID Connect:** Capa de autenticación sobre OAuth 2.0 ("¿Quién eres?")

### Flujo de Autenticación

```
1. Usuario → Keycloak
            GET /realms/creditcore/protocol/openid-connect/auth
            
2. Keycloak → Usuario
            Login form
            
3. Usuario → Keycloak
            Credenciales
            
4. Keycloak → Usuario
            JWT Access Token + ID Token
            
5. Usuario → Customer Service
            GET /api/customers/123
            Authorization: Bearer <JWT>
            
6. Customer Service → Keycloak
            Validar firma del JWT
            
7. Customer Service → Usuario
            200 OK + Datos
```

### Anatomía de un JWT

```
Header.Payload.Signature

{
  "alg": "RS256",
  "typ": "JWT",
  "kid": "key-id"
}
.
{
  "sub": "user-123",
  "name": "Juan Pérez",
  "roles": ["ANALYST", "USER"],
  "exp": 1704067200,
  "iss": "https://keycloak:8080/realms/creditcore"
}
.
[Firma RSA]
```

### Verificación en Quarkus

```java
@Inject
JsonWebToken jwt;

@GET
@RolesAllowed("ANALYST")
public Response getCustomer(@PathParam("id") Long id) {
    String username = jwt.getName();  // Del claim "name"
    Set<String> roles = jwt.getGroups();  // Del claim "groups"
    
    // Verificación automática por Quarkus:
    // 1. Firma válida
    // 2. No expirado
    // 3. Issuer correcto
    // 4. Rol requerido presente
}
```

---

## ☁️ Arquitectura Cloud-Native

### 12-Factor App Aplicado

| Factor | Implementación |
|--------|---------------|
| **I. Codebase** | Un repo, múltiples deploys |
| **II. Dependencies** | Maven pom.xml explícito |
| **III. Config** | application.properties por perfil |
| **IV. Backing Services** | PostgreSQL, Redis como recursos adjuntos |
| **V. Build/Run** | Separación con Docker multi-stage |
| **VI. Stateless** | Sin sesiones, JWT stateless |
| **VII. Port Binding** | HTTP:8081 exportado |
| **VIII. Concurrency** | Escalar horizontalmente con múltiples pods |
| **IX. Disposability** | Startup rápido (~1s), graceful shutdown |
| **X. Dev/Prod Parity** | Docker garantiza entornos idénticos |
| **XI. Logs** | Stdout/stderr, agregados externamente |
| **XII. Admin Tasks** | Scripts en `/scripts` |

### Compilación Nativa con GraalVM

**Beneficios:**
- **Startup:** 0.016s (vs 2.5s en JVM)
- **Memoria:** ~30MB RSS (vs 200MB en JVM)
- **Throughput:** Similar al JVM después del warmup

**Trade-offs:**
- Build time: 3-5 minutos
- Reflexión limitada (requiere hints)
- Debugging más complejo

```bash
# Compilar nativo
./mvnw package -Pnative

# Tamaño del binario
ls -lh target/*-runner
# ~80MB (incluye todo el runtime)
```

---

## 🎓 Conclusiones Pedagógicas

### Conceptos Clave Aprendidos

1. **Active Record** simplifica persistencia sin sacrificar poder
2. **Cifrado** debe ser transparente pero robusto (Tink)
3. **Caché** reduce latencia pero requiere invalidación correcta
4. **Fault Tolerance** no es opcional en microservicios
5. **Seguridad** debe ser declarativa y basada en estándares (JWT/OIDC)
6. **Cloud-Native** no es solo "en la nube", es arquitectura

### Patrones Arquitectónicos Aplicados

- ✅ **Active Record** - Panache
- ✅ **Cache-Aside** - Redis
- ✅ **Circuit Breaker** - SmallRye Fault Tolerance
- ✅ **Retry Pattern** - Con backoff
- ✅ **Fallback Pattern** - Degradación elegante
- ✅ **Encryption at Rest** - Tink + Always Encrypted
- ✅ **Token-Based Auth** - JWT
- ✅ **API Gateway Pattern** - Keycloak como IdP

---

## 📚 Referencias

- [Quarkus Guides](https://quarkus.io/guides/)
- [Google Tink Documentation](https://github.com/google/tink)
- [Redis Best Practices](https://redis.io/docs/manual/patterns/)
- [SmallRye Fault Tolerance](https://smallrye.io/docs/smallrye-fault-tolerance/)
- [OAuth 2.0 RFC 6749](https://datatracker.ietf.org/doc/html/rfc6749)
- [OpenID Connect Spec](https://openid.net/specs/openid-connect-core-1_0.html)

---

_Material educativo - Curso de Quarkus para Banca Peruana_
