# 📚 TEORIA.md - Capítulo 3: Microservicios y CDI en Quarkus

Fundamentos completos de microservicios, inyección de dependencias y APIs RESTful.

---

## 📖 Índice

1. [Microservicios: Conceptos Fundamentales](#1-microservicios-conceptos-fundamentales)
2. [Arquitectura de Microservicios](#2-arquitectura-de-microservicios)
3. [CDI: Contexts and Dependency Injection](#3-cdi-contexts-and-dependency-injection)
4. [Scopes en CDI](#4-scopes-en-cdi)
5. [RESTful APIs Completas](#5-restful-apis-completas)
6. [Verbos HTTP y CRUD](#6-verbos-http-y-crud)
7. [Request y Response](#7-request-y-response)
8. [Path Parameters vs Query Parameters](#8-path-parameters-vs-query-parameters)
9. [Códigos de Estado HTTP](#9-códigos-de-estado-http)
10. [Arquitectura en Capas](#10-arquitectura-en-capas)

---

## 1. Microservicios: Conceptos Fundamentales

### 1.1 ¿Qué es un Microservicio?

Un **microservicio** es una aplicación pequeña, independiente y autocontenida que realiza una función de negocio específica.

**Características:**
- 🎯 **Responsabilidad única:** Hace una cosa y la hace bien
- 🔄 **Independiente:** Puede desplegarse sin afectar otros servicios
- 📦 **Autocontenido:** Incluye todo lo necesario para funcionar
- 🌐 **Comunicación por red:** APIs REST, mensajería, gRPC
- 🗄️ **Base de datos por servicio:** Cada uno tiene su almacenamiento

### 1.2 Monolito vs Microservicios

#### **Aplicación Monolítica**

```
┌────────────────────────────────────┐
│        APLICACIÓN MONOLÍTICA       │
│                                    │
│  ┌──────────┐  ┌──────────┐       │
│  │ Usuarios │  │ Productos│       │
│  └──────────┘  └──────────┘       │
│  ┌──────────┐  ┌──────────┐       │
│  │  Pagos   │  │ Inventario│      │
│  └──────────┘  └──────────┘       │
│                                    │
│      Base de Datos Compartida      │
└────────────────────────────────────┘
```

**Características:**
- Todo en un solo proceso
- Una sola base de datos
- Despliegue completo cada vez
- Escalado vertical (más recursos a la misma máquina)

#### **Arquitectura de Microservicios**

```
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  Servicio    │  │  Servicio    │  │  Servicio    │
│  Usuarios    │  │  Productos   │  │   Pagos      │
│              │  │              │  │              │
│   DB User    │  │  DB Product  │  │  DB Payment  │
└──────────────┘  └──────────────┘  └──────────────┘
       ↓                 ↓                 ↓
    ┌─────────────────────────────────────────┐
    │         API Gateway / Load Balancer     │
    └─────────────────────────────────────────┘
```

**Características:**
- Múltiples procesos independientes
- Base de datos por servicio
- Despliegue independiente
- Escalado horizontal (más instancias)

### 1.3 Comparación

| Aspecto | Monolito | Microservicios |
|---------|----------|----------------|
| **Complejidad inicial** | Baja | Alta |
| **Escalabilidad** | Vertical | Horizontal |
| **Despliegue** | Todo junto | Independiente |
| **Tecnologías** | Misma stack | Heterogéneas |
| **Fallas** | Cascada total | Aisladas |
| **Desarrollo** | Centralizado | Equipos independientes |
| **Testing** | Más simple | Más complejo |
| **Latencia** | Baja (mismo proceso) | Mayor (red) |

### 1.4 ¿Cuándo usar Microservicios?

#### **✅ Usar Microservicios cuando:**
- Aplicación grande y compleja
- Equipos distribuidos geográficamente
- Necesitas escalabilidad independiente
- Ciclos de release rápidos
- Tecnologías heterogéneas
- Alta disponibilidad crítica

#### **❌ NO usar Microservicios cuando:**
- Aplicación pequeña (< 10,000 líneas)
- Equipo pequeño (< 5 personas)
- Negocio en etapa temprana (MVP)
- No tienes experiencia en sistemas distribuidos
- Infraestructura limitada

### 1.5 Analogía

**Monolito** es como un **restaurante tradicional**:
- Cocina, servicio, caja todo bajo un techo
- Si falla la cocina, todo el restaurante se detiene
- Ampliar significa construir un edificio más grande

**Microservicios** es como un **food court**:
- Cada local es independiente
- Si falla uno, los demás siguen funcionando
- Ampliar significa agregar más locales
- Cada local puede usar su propia tecnología (italiana, china, mexicana)

---

## 2. Arquitectura de Microservicios

### 2.1 Componentes Principales

```
                    [Usuarios/Clientes]
                            ↓
                    ┌───────────────┐
                    │  API Gateway  │
                    └───────────────┘
                            ↓
        ┌──────────────────┼──────────────────┐
        ↓                  ↓                  ↓
   ┌─────────┐        ┌─────────┐       ┌─────────┐
   │Service A│        │Service B│       │Service C│
   └─────────┘        └─────────┘       └─────────┘
        ↓                  ↓                  ↓
   ┌─────────┐        ┌─────────┐       ┌─────────┐
   │  DB A   │        │  DB B   │       │  DB C   │
   └─────────┘        └─────────┘       └─────────┘
```

#### **API Gateway**
- Punto único de entrada
- Routing a servicios
- Autenticación
- Rate limiting
- Logging

#### **Service Registry**
- Registro de servicios disponibles
- Descubrimiento de servicios
- Health checks

#### **Config Server**
- Configuración centralizada
- Variables de entorno
- Secretos

#### **Message Broker**
- Comunicación asíncrona
- Desacoplamiento
- Event sourcing

### 2.2 Patrones de Comunicación

#### **Síncrona (REST, gRPC)**

```
Cliente → [GET /cuentas] → Servicio Cuentas → Respuesta
         (espera bloqueante)
```

**Ventajas:**
- ✅ Simple de implementar
- ✅ Respuesta inmediata
- ✅ Fácil debugging

**Desventajas:**
- ❌ Acoplamiento temporal
- ❌ Cascada de fallos
- ❌ Latencia acumulada

#### **Asíncrona (Mensajería)**

```
Cliente → [Evento: CuentaCreada] → Message Broker
                                         ↓
                                    Servicio A (procesa)
                                    Servicio B (procesa)
```

**Ventajas:**
- ✅ Desacoplamiento
- ✅ Tolerancia a fallos
- ✅ Escalabilidad

**Desventajas:**
- ❌ Complejidad
- ❌ Eventual consistency
- ❌ Debugging difícil

### 2.3 Database per Service

Cada microservicio tiene su propia base de datos:

```
Servicio Usuarios → DB Users
Servicio Cuentas  → DB Accounts
Servicio Pagos    → DB Payments
```

**Ventajas:**
- ✅ Independencia
- ✅ Tecnología adecuada (SQL, NoSQL)
- ✅ Escalado independiente

**Desventajas:**
- ❌ Transacciones distribuidas
- ❌ Joins entre servicios
- ❌ Duplicación de datos

---

## 3. CDI: Contexts and Dependency Injection

### 3.1 ¿Qué es CDI?

**CDI** (Contexts and Dependency Injection) es el estándar de Jakarta EE para inyección de dependencias y gestión del ciclo de vida de objetos.

### 3.2 Problema que Resuelve

#### **Sin CDI (Acoplamiento Fuerte)**

```java
public class CuentaResource {
    // Creación manual = acoplamiento
    private CuentaService service = new CuentaService();
    
    public List<Cuenta> listar() {
        return service.listarTodas();
    }
}
```

**Problemas:**
- ❌ Acoplamiento directo
- ❌ Difícil de testear (mock)
- ❌ Sin control de ciclo de vida
- ❌ Duplicación de instancias

#### **Con CDI (Inyección)**

```java
@Path("/cuentas")
public class CuentaResource {
    
    @Inject  // CDI inyecta automáticamente
    CuentaService service;
    
    @GET
    public List<Cuenta> listar() {
        return service.listarTodas();
    }
}
```

**Ventajas:**
- ✅ Desacoplamiento
- ✅ Fácil testing (inyectar mocks)
- ✅ CDI gestiona ciclo de vida
- ✅ Instancia compartida (según scope)

### 3.3 Cómo Funciona CDI

```
1. Quarkus escanea clases con anotaciones CDI
        ↓
2. Crea un "contenedor CDI" con beans disponibles
        ↓
3. Cuando encuentra @Inject:
   - Busca bean compatible
   - Lo inyecta automáticamente
   - Gestiona su ciclo de vida
```

### 3.4 Beans en CDI

Un **bean** es cualquier clase que CDI puede gestionar:

```java
@ApplicationScoped  // Esta anotación la hace bean
public class CuentaService {
    // CDI puede inyectar esto
}
```

**Requisitos para ser bean:**
- ✅ Tener un scope (`@ApplicationScoped`, etc.)
- ✅ O estar en package escaneado (auto-detección en Quarkus)

### 3.5 Analogía

CDI es como un **servicio de catering empresarial**:

**Sin CDI:**
- Cada empleado va al supermercado por su comida
- Compra, prepara, limpia
- Desperdicio y esfuerzo duplicado

**Con CDI:**
- El catering (CDI) provee comida a todos
- Centralmente preparada
- Cada empleado solo pide (@Inject) y recibe
- Eficiente y coordinado

---

## 4. Scopes en CDI

### 4.1 ¿Qué es un Scope?

El **scope** define el **ciclo de vida** y **alcance** de un bean.

### 4.2 Scopes Principales

#### **@ApplicationScoped** (Singleton)

```java
@ApplicationScoped
public class CuentaService {
    // UNA SOLA instancia para toda la aplicación
}
```

**Características:**
- Una instancia para toda la app
- Se crea al arrancar (lazy o eager)
- Se destruye al cerrar app
- Thread-safe (debe serlo)

**Usar cuando:**
- Servicios sin estado
- Repositorios
- Configuración
- Cualquier lógica compartida

#### **@RequestScoped**

```java
@RequestScoped
public class RequestContext {
    // Nueva instancia por cada request HTTP
}
```

**Características:**
- Nueva instancia por request
- Se destruye al terminar request
- Aislamiento entre requests

**Usar cuando:**
- Datos específicos del request
- Usuario actual
- Contexto de transacción

#### **@Dependent** (Por Defecto)

```java
// Sin anotación de scope = @Dependent
public class Calculadora {
    // Nueva instancia cada vez que se inyecta
}
```

**Características:**
- Nueva instancia cada inyección
- Ciclo de vida atado al objeto que lo inyecta
- No es proxy

**Usar cuando:**
- Objetos livianos
- Sin estado compartido
- Creación barata

#### **@Singleton** (Quarkus)

```java
@Singleton  // Eager initialization
public class StartupService {
    // Se crea al arrancar, antes que ApplicationScoped
}
```

**Diferencia con @ApplicationScoped:**
- `@Singleton`: Inicialización eager (al arrancar)
- `@ApplicationScoped`: Inicialización lazy (al usar)

### 4.3 Comparación de Scopes

| Scope | Instancias | Ciclo de Vida | Thread-Safe? |
|-------|-----------|---------------|--------------|
| **@ApplicationScoped** | 1 | Toda la app | Debe serlo |
| **@RequestScoped** | 1 por request | Durante request | Sí (aislado) |
| **@Dependent** | N (cada inyección) | Del objeto padre | No aplica |
| **@Singleton** | 1 | Toda la app | Debe serlo |

### 4.4 Ejemplo Completo

```java
@ApplicationScoped  // Singleton compartido
public class CuentaService {
    
    @Inject
    CuentaRepository repository;  // Inyecta otro bean
    
    private Map<String, Cuenta> cache = new ConcurrentHashMap<>();
    
    public Cuenta obtener(String numero) {
        return cache.computeIfAbsent(numero, 
            k -> repository.findByNumero(k));
    }
}

@RequestScoped  // Nuevo por request
public class UsuarioActual {
    private String username;
    private String token;
    
    // Datos específicos de este request
}

@Path("/cuentas")
public class CuentaResource {
    
    @Inject
    CuentaService service;  // Inyecta ApplicationScoped
    
    @Inject
    UsuarioActual usuario;  // Inyecta RequestScoped
    
    @GET
    public List<Cuenta> listar() {
        log.info("Usuario: " + usuario.getUsername());
        return service.listarTodas();
    }
}
```

### 4.5 Analogía de Scopes

**@ApplicationScoped** = **Biblioteca pública**
- Una sola en la ciudad
- Todos la comparten
- Abierta todo el día

**@RequestScoped** = **Camarote de evento**
- Uno por grupo
- Privado durante el evento
- Se libera al terminar

**@Dependent** = **Ticket de entrada**
- Uno por persona
- Válido solo para quien lo tiene
- Se desecha al usarlo

---

## 5. RESTful APIs Completas

### 5.1 ¿Qué es REST?

**REST** (Representational State Transfer) es un estilo arquitectónico para APIs basado en HTTP.

**Principios REST:**
1. **Recursos:** Todo es un recurso con URL única
2. **Verbos HTTP:** Operaciones estándar (GET, POST, PUT, DELETE)
3. **Stateless:** Sin estado en servidor
4. **Representaciones:** JSON, XML, etc.
5. **HATEOAS:** Links a recursos relacionados (opcional)

### 5.2 Recursos y URLs

Un **recurso** es cualquier entidad del dominio:

```
Recurso: Cuenta
URL: /cuentas
```

**Convenciones:**
- Plural para colecciones: `/cuentas`
- ID para específico: `/cuentas/123`
- Minúsculas
- Guiones para palabras: `/cuentas-ahorro`

### 5.3 Diseño de URLs RESTful

#### **Colección**
```
GET    /cuentas           # Listar todas
POST   /cuentas           # Crear nueva
```

#### **Elemento Específico**
```
GET    /cuentas/{id}      # Obtener una
PUT    /cuentas/{id}      # Actualizar
DELETE /cuentas/{id}      # Eliminar
```

#### **Sub-recursos**
```
GET    /cuentas/{id}/transacciones
POST   /cuentas/{id}/transacciones
```

#### **❌ Anti-patrones (evitar)**
```
GET /obtenerCuentas          # Verbo en URL
POST /cuentas/crear          # Redundante
GET /cuentas/delete/123      # Verbo equivocado
```

---

## 6. Verbos HTTP y CRUD

### 6.1 Mapeo CRUD → HTTP

| CRUD | HTTP | Idempotente | Safe |
|------|------|-------------|------|
| **Create** | POST | ❌ | ❌ |
| **Read** | GET | ✅ | ✅ |
| **Update** | PUT | ✅ | ❌ |
| **Delete** | DELETE | ✅ | ❌ |

### 6.2 GET (Leer)

```java
@GET
public List<Cuenta> listar() {
    return service.listarTodas();
}

@GET
@Path("/{numero}")
public Cuenta obtener(@PathParam("numero") String numero) {
    return service.obtenerPorNumero(numero);
}
```

**Características:**
- **Safe:** No modifica estado
- **Idempotente:** Mismo resultado siempre
- **Cacheable:** Puede guardarse en caché

### 6.3 POST (Crear)

```java
@POST
public Response crear(Cuenta cuenta) {
    Cuenta nueva = service.crear(cuenta);
    return Response.status(201)
        .entity(nueva)
        .build();
}
```

**Características:**
- **NO idempotente:** Crea recurso cada vez
- **Retorna 201 Created**
- **Body:** Recurso creado
- **Header Location:** URL del nuevo recurso (opcional)

### 6.4 PUT (Actualizar Completo)

```java
@PUT
@Path("/{numero}")
public Response actualizar(@PathParam("numero") String numero, Cuenta cuenta) {
    Cuenta actualizada = service.actualizar(numero, cuenta);
    return Response.ok(actualizada).build();
}
```

**Características:**
- **Idempotente:** Mismo resultado siempre
- **Reemplaza completo:** Todos los campos
- **Retorna 200 OK** o **204 No Content**

### 6.5 PATCH (Actualizar Parcial)

```java
@PATCH
@Path("/{numero}")
public Response actualizarParcial(@PathParam("numero") String numero, Map<String, Object> cambios) {
    Cuenta actualizada = service.actualizarCampos(numero, cambios);
    return Response.ok(actualizada).build();
}
```

**Características:**
- **Idempotente:** Generalmente sí
- **Actualiza parcial:** Solo campos enviados
- **Más eficiente** que PUT

### 6.6 DELETE (Eliminar)

```java
@DELETE
@Path("/{numero}")
public Response eliminar(@PathParam("numero") String numero) {
    service.eliminar(numero);
    return Response.status(204).build();
}
```

**Características:**
- **Idempotente:** Eliminar 2 veces = mismo resultado
- **Retorna 204 No Content**
- **O 200 OK** con info de eliminación

### 6.7 Idempotencia

**Idempotente** = Ejecutar N veces produce el mismo resultado que ejecutar 1 vez.

```
GET /cuentas/123     → Siempre devuelve lo mismo ✅
DELETE /cuentas/123  → 1ra vez elimina, 2da+ ya no existe (mismo estado final) ✅
POST /cuentas        → Crea recurso cada vez ❌
```

---

## 7. Request y Response

### 7.1 Request (Petición)

#### **Componentes**

```http
POST /cuentas HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Authorization: Bearer token123

{
  "numero": "123",
  "titular": "Juan",
  "saldo": 1000
}
```

**Partes:**
1. **Método:** POST
2. **URL:** /cuentas
3. **Headers:** Content-Type, Authorization
4. **Body:** JSON con datos

#### **En JAX-RS**

```java
@POST
@Consumes(MediaType.APPLICATION_JSON)  // Acepta JSON
public Response crear(
    @HeaderParam("Authorization") String token,  // Header
    Cuenta cuenta  // Body automático desde JSON
) {
    // ...
}
```

### 7.2 Response (Respuesta)

#### **Componentes**

```http
HTTP/1.1 201 Created
Content-Type: application/json
Location: /cuentas/123

{
  "numero": "123",
  "titular": "Juan",
  "saldo": 1000
}
```

**Partes:**
1. **Status Code:** 201
2. **Headers:** Content-Type, Location
3. **Body:** Recurso creado

#### **En JAX-RS**

```java
@POST
@Produces(MediaType.APPLICATION_JSON)  // Produce JSON
public Response crear(Cuenta cuenta) {
    Cuenta nueva = service.crear(cuenta);
    
    return Response
        .status(201)                    // Status
        .entity(nueva)                  // Body
        .header("Location", "/cuentas/" + nueva.getNumero())  // Header
        .build();
}
```

### 7.3 Content Negotiation

Cliente indica qué formato quiere:

```http
GET /cuentas/123
Accept: application/json    # Quiero JSON
```

Servidor responde según Accept:

```java
@GET
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public Cuenta obtener(@PathParam("numero") String numero) {
    // JAX-RS elige formato según Accept header
    return service.obtener(numero);
}
```

---

## 8. Path Parameters vs Query Parameters

### 8.1 Path Parameters

**Parte de la ruta**, identifica **recurso específico**:

```
GET /cuentas/1000000001
              ↑
         Path parameter
```

**En JAX-RS:**

```java
@GET
@Path("/{numero}")
public Cuenta obtener(@PathParam("numero") String numero) {
    return service.obtener(numero);
}
```

**Usar para:**
- Identificar recurso específico
- Obligatorios
- Jerarquías: `/usuarios/{id}/cuentas/{numero}`

### 8.2 Query Parameters

**Después de `?`**, para **filtrar/paginar/ordenar**:

```
GET /cuentas?tipo=AHORRO&limit=10
             ↑
       Query parameters
```

**En JAX-RS:**

```java
@GET
public List<Cuenta> listar(
    @QueryParam("tipo") String tipo,
    @QueryParam("limit") @DefaultValue("20") int limit
) {
    return service.filtrar(tipo, limit);
}
```

**Usar para:**
- Filtros opcionales
- Paginación (`?page=2&size=10`)
- Ordenamiento (`?sort=saldo&order=desc`)
- Búsqueda (`?q=Juan`)

### 8.3 Comparación

| Aspecto | Path Parameter | Query Parameter |
|---------|---------------|-----------------|
| **Ubicación** | En la ruta | Después de `?` |
| **Propósito** | Identificar recurso | Filtrar/configurar |
| **Obligatorio** | Generalmente sí | No |
| **Ejemplo** | `/cuentas/123` | `/cuentas?tipo=AHORRO` |
| **SEO** | Mejor | Peor |

### 8.4 Ejemplos Combinados

```java
// Path + Query
@GET
@Path("/{numero}/transacciones")
public List<Transaccion> obtenerTransacciones(
    @PathParam("numero") String numeroCuenta,
    @QueryParam("desde") String fechaDesde,
    @QueryParam("hasta") String fechaHasta,
    @QueryParam("tipo") String tipoTransaccion
) {
    return service.obtenerTransacciones(
        numeroCuenta, fechaDesde, fechaHasta, tipoTransaccion
    );
}

// Llamada:
// GET /cuentas/123/transacciones?desde=2024-01-01&hasta=2024-12-31&tipo=DEPOSITO
```

---

## 9. Códigos de Estado HTTP

### 9.1 Categorías

| Rango | Categoría | Significado |
|-------|-----------|-------------|
| **1xx** | Informacional | Procesando |
| **2xx** | Éxito | Todo OK |
| **3xx** | Redirección | Recurso movido |
| **4xx** | Error Cliente | Error en request |
| **5xx** | Error Servidor | Error interno |

### 9.2 Códigos Comunes

#### **2xx - Éxito**

| Código | Nombre | Uso |
|--------|--------|-----|
| **200** | OK | GET, PUT exitosos |
| **201** | Created | POST exitoso |
| **204** | No Content | DELETE exitoso |

#### **4xx - Error del Cliente**

| Código | Nombre | Uso |
|--------|--------|-----|
| **400** | Bad Request | Datos inválidos |
| **401** | Unauthorized | No autenticado |
| **403** | Forbidden | No autorizado |
| **404** | Not Found | Recurso no existe |
| **409** | Conflict | Conflicto (ej: duplicado) |

#### **5xx - Error del Servidor**

| Código | Nombre | Uso |
|--------|--------|-----|
| **500** | Internal Server Error | Error no manejado |
| **503** | Service Unavailable | Servicio caído |

### 9.3 Uso en JAX-RS

```java
@GET
@Path("/{numero}")
public Response obtener(@PathParam("numero") String numero) {
    Cuenta cuenta = service.obtener(numero);
    
    if (cuenta == null) {
        return Response.status(404)
            .entity("Cuenta no encontrada")
            .build();
    }
    
    return Response.ok(cuenta).build();  // 200
}

@POST
public Response crear(Cuenta cuenta) {
    if (cuenta.getNumero() == null) {
        return Response.status(400)
            .entity("Número de cuenta requerido")
            .build();
    }
    
    Cuenta nueva = service.crear(cuenta);
    return Response.status(201)
        .entity(nueva)
        .build();
}
```

---

## 10. Arquitectura en Capas

### 10.1 Separación de Responsabilidades

```
┌─────────────────────────────────┐
│   RESOURCE (Presentación)       │  ← JAX-RS, HTTP, JSON
├─────────────────────────────────┤
│   SERVICE (Lógica de Negocio)   │  ← Reglas, cálculos
├─────────────────────────────────┤
│   REPOSITORY (Acceso a Datos)   │  ← DB, queries
├─────────────────────────────────┤
│   MODEL (Entidades/DTOs)        │  ← Estructuras de datos
└─────────────────────────────────┘
```

### 10.2 Capas del Proyecto

#### **Model (DTOs/Entidades)**

```java
package pe.banco.cuentas.model;

public class Cuenta {
    private String numero;
    private String titular;
    private BigDecimal saldo;
    // Solo datos, sin lógica
}
```

**Responsabilidad:**
- Estructuras de datos
- Getters/Setters
- Sin lógica de negocio

#### **Service (Lógica de Negocio)**

```java
package pe.banco.cuentas.service;

@ApplicationScoped
public class CuentaService {
    
    public void transferir(String origen, String destino, BigDecimal monto) {
        // Validaciones
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Monto inválido");
        }
        
        // Lógica de negocio
        Cuenta cuentaOrigen = obtener(origen);
        if (cuentaOrigen.getSaldo().compareTo(monto) < 0) {
            throw new SaldoInsuficienteException();
        }
        
        // Operación
        cuentaOrigen.setSaldo(cuentaOrigen.getSaldo().subtract(monto));
        Cuenta cuentaDestino = obtener(destino);
        cuentaDestino.setSaldo(cuentaDestino.getSaldo().add(monto));
    }
}
```

**Responsabilidad:**
- Lógica de negocio
- Validaciones
- Transacciones
- Orquestación

#### **Resource (Presentación REST)**

```java
package pe.banco.cuentas.resource;

@Path("/cuentas")
public class CuentaResource {
    
    @Inject
    CuentaService service;
    
    @POST
    @Path("/{origen}/transferir")
    public Response transferir(
        @PathParam("origen") String origen,
        TransferenciaRequest request
    ) {
        try {
            service.transferir(origen, request.getDestino(), request.getMonto());
            return Response.ok().build();
        } catch (IllegalArgumentException e) {
            return Response.status(400).entity(e.getMessage()).build();
        } catch (SaldoInsuficienteException e) {
            return Response.status(409).entity("Saldo insuficiente").build();
        }
    }
}
```

**Responsabilidad:**
- HTTP/REST
- Manejo de requests/responses
- Códigos de estado
- Delegación a Service

### 10.3 Flujo Completo

```
1. Cliente HTTP
   ↓
2. CuentaResource (@Path, JAX-RS)
   - Recibe request
   - Valida formato HTTP
   ↓
3. @Inject CuentaService
   - Ejecuta lógica de negocio
   - Valida reglas
   ↓
4. Datos en memoria (o Repository)
   - Acceso/modificación de datos
   ↓
5. Response
   - Service retorna resultado
   - Resource construye HTTP Response
   - Cliente recibe JSON + status code
```

### 10.4 Ventajas de la Arquitectura en Capas

✅ **Mantenibilidad:** Cambios aislados por capa  
✅ **Testabilidad:** Cada capa se testea independiente  
✅ **Reutilización:** Service usado por múltiples Resources  
✅ **Escalabilidad:** Capas pueden distribuirse  
✅ **Claridad:** Responsabilidades bien definidas  

### 10.5 Analogía

La arquitectura en capas es como una **fábrica de automóviles**:

- **Model:** Planos y especificaciones (cómo es un auto)
- **Repository:** Almacén de piezas (donde están los datos)
- **Service:** Línea de ensamblaje (lógica de construcción)
- **Resource:** Sala de ventas (interfaz con el cliente)

Cada área tiene su función específica y trabajan coordinadamente.

---

## 📊 Resumen Comparativo

### Microservicios vs Monolito

| | Monolito | Microservicios |
|-|----------|----------------|
| **Deploy** | Todo junto | Independiente |
| **Scale** | Vertical | Horizontal |
| **Tech** | Única | Múltiple |
| **Team** | Centralizado | Distribuido |
| **Fail** | Cascada | Aislado |

### Scopes CDI

| Scope | Vida | Instancias |
|-------|------|-----------|
| @ApplicationScoped | App completa | 1 |
| @RequestScoped | Request HTTP | 1/request |
| @Dependent | Del padre | N |

### Verbos HTTP

| Verbo | CRUD | Idempotente | Safe |
|-------|------|-------------|------|
| GET | Read | ✅ | ✅ |
| POST | Create | ❌ | ❌ |
| PUT | Update | ✅ | ❌ |
| DELETE | Delete | ✅ | ❌ |

---

## ✅ Checklist de Conocimientos

Después de estudiar esta teoría, deberías poder:

- [ ] Explicar qué es un microservicio y cuándo usarlo
- [ ] Diferenciar monolito vs microservicios
- [ ] Entender CDI y sus beneficios
- [ ] Usar @Inject correctamente
- [ ] Elegir el scope apropiado (@ApplicationScoped, @RequestScoped)
- [ ] Diseñar APIs RESTful correctas
- [ ] Mapear CRUD a verbos HTTP
- [ ] Usar Path y Query parameters adecuadamente
- [ ] Retornar códigos HTTP correctos
- [ ] Organizar código en capas (Model, Service, Resource)
- [ ] Explicar el flujo completo de una petición HTTP

---

**🎉 ¡Teoría completa del Capítulo 3!**

*Ahora tienes las bases sólidas para desarrollar microservicios profesionales con arquitectura limpia y buenas prácticas.* 🚀🏦