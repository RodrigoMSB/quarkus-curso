package pe.banco.prestamos.resource;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pe.banco.prestamos.model.Cliente;
import pe.banco.prestamos.repository.ClienteRepository;

import java.util.List;

/**
 * REST Resource (Controlador) para gestión de clientes bancarios.
 * 
 * CAPA DE PRESENTACIÓN en arquitectura de 3 capas:
 * ┌─────────────────────────────────────────┐
 * │ Resource (esta clase)                   │ ← HTTP/REST/JSON
 * ├─────────────────────────────────────────┤
 * │ Repository (ClienteRepository)          │ ← Acceso a datos
 * ├─────────────────────────────────────────┤
 * │ Model (Cliente)                         │ ← Entidad JPA
 * ├─────────────────────────────────────────┤
 * │ Database (PostgreSQL/H2)                │ ← Persistencia
 * └─────────────────────────────────────────┘
 * 
 * Responsabilidades:
 * ✅ Exponer endpoints HTTP RESTful
 * ✅ Manejar request/response HTTP
 * ✅ Serializar/deserializar JSON ↔ Java
 * ✅ Validar formato de entrada
 * ✅ Retornar códigos de estado HTTP correctos
 * ✅ Delegar lógica de negocio al Repository
 * 
 * ❌ NO contiene lógica de negocio compleja
 * ❌ NO accede directamente a base de datos
 * ❌ NO sabe de SQL o JPA
 * 
 * Endpoints expuestos:
 * GET    /clientes           → Listar todos
 * GET    /clientes/{id}      → Obtener uno
 * POST   /clientes           → Crear nuevo
 * PUT    /clientes/{id}      → Actualizar
 * DELETE /clientes/{id}      → Eliminar
 * 
 * Analogía: Como la ventanilla de atención al cliente en un banco.
 * - Recibe solicitudes (requests HTTP)
 * - Valida formularios (validación de entrada)
 * - Consulta al gerente (repository)
 * - Entrega resultados (response JSON)
 */

// ============================================
// ANOTACIONES DE RECURSO JAX-RS
// ============================================

/**
 * @Path("/clientes")
 * Define la ruta base para todos los endpoints de esta clase.
 * 
 * URL completa: http://localhost:8080/clientes
 * 
 * CONVENCIONES REST:
 * ✅ Plural para colecciones: /clientes (no /cliente)
 * ✅ Minúsculas: /clientes (no /Clientes)
 * ✅ Sin verbos: /clientes (no /obtener-clientes)
 * ✅ Sustantivos: recursos, no acciones
 * 
 * Ejemplos de rutas:
 * /clientes              → Colección
 * /clientes/1            → Recurso específico
 * /clientes/1/prestamos  → Sub-recurso (futuro)
 * 
 * Anti-patrones (evitar):
 * ❌ /getClientes
 * ❌ /cliente/crear
 * ❌ /clientes/delete/1
 */
@Path("/clientes")

/**
 * @Produces(MediaType.APPLICATION_JSON)
 * Define que TODOS los métodos de esta clase producen JSON.
 * 
 * Content-Type en response: application/json
 * 
 * JAX-RS + Jackson (automático):
 * 1. Método retorna objeto Java (Cliente, List<Cliente>)
 * 2. Jackson serializa a JSON
 * 3. Response incluye JSON en body
 * 
 * Ejemplo:
 * Cliente c = new Cliente();
 * c.nombre = "Juan";
 * return c;
 * 
 * HTTP Response:
 * Content-Type: application/json
 * 
 * {
 *   "id": 1,
 *   "nombre": "Juan",
 *   "dni": "12345678",
 *   ...
 * }
 * 
 * Otros formatos posibles:
 * @Produces(MediaType.APPLICATION_XML)
 * @Produces(MediaType.TEXT_PLAIN)
 * @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
 * 
 * Content Negotiation:
 * Si cliente pide Accept: application/json → retorna JSON
 * Si cliente pide Accept: application/xml → retorna XML
 */
@Produces(MediaType.APPLICATION_JSON)

/**
 * @Consumes(MediaType.APPLICATION_JSON)
 * Define que TODOS los métodos aceptan JSON en el body.
 * 
 * Aplica a: POST, PUT, PATCH (métodos con body)
 * 
 * JAX-RS + Jackson (automático):
 * 1. Cliente envía JSON en request body
 * 2. Jackson deserializa JSON → Objeto Java
 * 3. Objeto se pasa como parámetro al método
 * 
 * Ejemplo:
 * POST /clientes
 * Content-Type: application/json
 * 
 * {
 *   "nombre": "María",
 *   "dni": "87654321",
 *   "email": "maria@example.com",
 *   "telefono": "987654321"
 * }
 * 
 * JAX-RS lo convierte a:
 * Cliente cliente = new Cliente();
 * cliente.nombre = "María";
 * cliente.dni = "87654321";
 * ...
 * 
 * Y lo pasa a:
 * public Response crear(Cliente cliente) { ... }
 * 
 * Si JSON inválido:
 * → 400 Bad Request automático
 */
@Consumes(MediaType.APPLICATION_JSON)

public class ClienteResource {
    
    // ============================================
    // INYECCIÓN DE DEPENDENCIAS
    // ============================================
    
    /**
     * Repositorio inyectado por CDI.
     * 
     * @Inject
     * Le dice a Quarkus/CDI:
     * "Cuando crees ClienteResource, inyecta automáticamente ClienteRepository"
     * 
     * VENTAJAS vs new ClienteRepository():
     * 
     * ❌ Sin CDI (acoplamiento fuerte):
     * private ClienteRepository repo = new ClienteRepository();
     * - Creas instancia manualmente
     * - Difícil de testear (no puedes mockear)
     * - Sin lifecycle management
     * - Duplicas instancias (desperdicio)
     * 
     * ✅ Con CDI (inyección):
     * @Inject
     * ClienteRepository clienteRepository;
     * - CDI crea y gestiona instancia
     * - Singleton (@ApplicationScoped) → reutiliza
     * - Fácil mockear en tests (@InjectMock)
     * - Lifecycle automático
     * 
     * Proceso de inyección:
     * 1. Quarkus arranca
     * 2. Escanea @Path classes (encuentra ClienteResource)
     * 3. Detecta @Inject ClienteRepository
     * 4. Busca bean @ApplicationScoped ClienteRepository
     * 5. Crea instancia (si no existe)
     * 6. Inyecta referencia
     * 
     * Type-safe:
     * Si no existe ClienteRepository:
     * → Error en compile-time (no runtime)
     * 
     * Testing:
     * @QuarkusTest
     * public class ClienteResourceTest {
     *     @InjectMock
     *     ClienteRepository clienteRepository;  // Mock inyectado
     *     
     *     @Test
     *     void test() {
     *         when(clienteRepository.listAll()).thenReturn(List.of(...));
     *         // ...
     *     }
     * }
     * 
     * Analogía: Como tener asistente automático.
     * No contratas (new), CDI te asigna uno ya entrenado.
     */
    @Inject
    ClienteRepository clienteRepository;
    
    // ============================================
    // ENDPOINTS REST - CRUD COMPLETO
    // ============================================
    
    /**
     * GET /clientes
     * Lista todos los clientes.
     * 
     * ENDPOINT DE COLECCIÓN
     * Retorna array JSON con todos los clientes.
     * 
     * Request HTTP:
     * GET http://localhost:8080/clientes
     * Accept: application/json
     * 
     * Response HTTP 200 OK:
     * Content-Type: application/json
     * 
     * [
     *   {
     *     "id": 1,
     *     "nombre": "María González",
     *     "dni": "12345678",
     *     "email": "maria@example.com",
     *     "telefono": "987654321"
     *   },
     *   {
     *     "id": 2,
     *     "nombre": "Carlos Ruiz",
     *     ...
     *   }
     * ]
     * 
     * CARACTERÍSTICAS HTTP:
     * - Método: GET (safe e idempotente)
     * - Sin body en request
     * - Retorna colección (array JSON)
     * - Siempre 200 OK (incluso si lista vacía [])
     * 
     * SERIALIZACIÓN AUTOMÁTICA:
     * 1. clienteRepository.listAll() retorna List<Cliente>
     * 2. @Produces(JSON) → Jackson serializa
     * 3. List<Cliente> → JSON array
     * 4. @JsonIgnore en Cliente.prestamos → no incluye préstamos
     * 
     * MEJORAS FUTURAS:
     * - Paginación: ?page=0&size=10
     * - Ordenamiento: ?sort=nombre,asc
     * - Filtros: ?nombre=Juan
     * 
     * Ejemplo con paginación:
     * @GET
     * public List<Cliente> listar(
     *     @QueryParam("page") @DefaultValue("0") int page,
     *     @QueryParam("size") @DefaultValue("20") int size
     * ) {
     *     return clienteRepository.findAll()
     *         .page(page, size)
     *         .list();
     * }
     * 
     * SQL ejecutado:
     * SELECT c.id, c.nombre, c.dni, c.email, c.telefono
     * FROM clientes c
     * 
     * @return Lista de todos los clientes (nunca null, puede estar vacía)
     */
    @GET
    public List<Cliente> listar() {
        return clienteRepository.listAll();
    }
    
    /**
     * GET /clientes/{id}
     * Obtiene un cliente específico por ID.
     * 
     * ENDPOINT DE RECURSO INDIVIDUAL
     * 
     * Request HTTP:
     * GET http://localhost:8080/clientes/1
     * Accept: application/json
     * 
     * Response 200 OK (cliente existe):
     * {
     *   "id": 1,
     *   "nombre": "María González",
     *   "dni": "12345678",
     *   "email": "maria@example.com",
     *   "telefono": "987654321"
     * }
     * 
     * Response 404 Not Found (cliente no existe):
     * "Cliente no encontrado"
     * 
     * @PathParam("id")
     * Extrae el ID de la URL y lo pasa como parámetro.
     * 
     * Mapeo:
     * URL: /clientes/1
     *              ↓
     * @PathParam("id") Long id = 1L
     * 
     * Conversión automática:
     * String "1" → Long 1L
     * 
     * Si no es número válido:
     * /clientes/abc → 404 Not Found (automático)
     * 
     * PROGRAMACIÓN FUNCIONAL con Optional:
     * 
     * clienteRepository.findByIdOptional(id)
     * → Optional<Cliente>
     * 
     * .map(cliente -> Response.ok(cliente).build())
     * → Si Optional tiene valor:
     *   - Ejecuta lambda
     *   - Retorna Response 200 OK con cliente
     * 
     * .orElse(Response.status(404).entity("...").build())
     * → Si Optional está vacío:
     *   - Retorna Response 404 Not Found
     * 
     * Alternativa imperativa (menos elegante):
     * Optional<Cliente> opt = repo.findByIdOptional(id);
     * if (opt.isPresent()) {
     *     return Response.ok(opt.get()).build();
     * } else {
     *     return Response.status(404)
     *         .entity("Cliente no encontrado")
     *         .build();
     * }
     * 
     * CÓDIGOS HTTP:
     * - 200 OK: Cliente encontrado
     * - 404 Not Found: ID no existe
     * 
     * SQL ejecutado:
     * SELECT c.id, c.nombre, c.dni, c.email, c.telefono
     * FROM clientes c
     * WHERE c.id = ?
     * 
     * @param id Identificador único del cliente (extraído de URL)
     * @return Response con cliente (200) o error (404)
     */
    @GET
    @Path("/{id}")
    public Response obtener(@PathParam("id") Long id) {
        return clienteRepository.findByIdOptional(id)
                .map(cliente -> Response.ok(cliente).build())
                .orElse(Response.status(404).entity("Cliente no encontrado").build());
    }
    
    /**
     * POST /clientes
     * Crea un nuevo cliente.
     * 
     * ENDPOINT DE CREACIÓN
     * 
     * Request HTTP:
     * POST http://localhost:8080/clientes
     * Content-Type: application/json
     * 
     * {
     *   "nombre": "Ana Torres",
     *   "dni": "11223344",
     *   "email": "ana@example.com",
     *   "telefono": "955444333"
     * }
     * 
     * Response 201 Created (éxito):
     * {
     *   "id": 3,  ← ID generado automáticamente
     *   "nombre": "Ana Torres",
     *   "dni": "11223344",
     *   "email": "ana@example.com",
     *   "telefono": "955444333"
     * }
     * 
     * Response 409 Conflict (DNI duplicado):
     * "DNI ya registrado"
     * 
     * Response 409 Conflict (Email duplicado):
     * "Email ya registrado"
     * 
     * @Transactional
     * CRÍTICO para modificar base de datos.
     * 
     * Sin @Transactional:
     * jakarta.persistence.TransactionRequiredException:
     * No transaction is currently active
     * 
     * Con @Transactional:
     * - Inicia transacción antes del método
     * - Ejecuta persist()
     * - Commit automático al terminar
     * - Rollback automático si hay exception
     * 
     * VALIDACIONES:
     * 
     * 1. DNI duplicado:
     * if (clienteRepository.existsByDni(cliente.dni)) {
     *     return 409 Conflict;
     * }
     * 
     * ¿Por qué validar ANTES de persist()?
     * - Evita intentar INSERT duplicado
     * - Más rápido que catch exception
     * - Control explícito del error
     * - Mensaje claro al usuario
     * 
     * Sin validación:
     * try {
     *     repo.persist(cliente);
     * } catch (PersistenceException e) {
     *     // Maneja constraint violation
     * }
     * 
     * 2. Email duplicado:
     * Similar a DNI, verifica unicidad.
     * 
     * DESERIALIZACIÓN JSON → Objeto:
     * Jackson automáticamente:
     * 1. Lee JSON del request body
     * 2. Crea new Cliente()
     * 3. Llama setters (generados por Panache)
     * 4. Pasa objeto completo al método
     * 
     * PERSISTENCIA:
     * clienteRepository.persist(cliente);
     * 
     * Hibernate:
     * 1. Asigna ID (auto-generado)
     * 2. INSERT INTO clientes (...)
     * 3. cliente.id ahora tiene valor
     * 
     * Response 201:
     * - 201 Created: estándar REST para recursos creados
     * - Body: cliente con ID asignado
     * - Header Location (opcional): /clientes/3
     * 
     * SQL ejecutado:
     * -- Validación DNI
     * SELECT COUNT(c.id) FROM clientes c WHERE c.dni = ?
     * 
     * -- Validación Email
     * SELECT COUNT(c.id) FROM clientes c WHERE c.email = ?
     * 
     * -- Inserción
     * INSERT INTO clientes (nombre, dni, email, telefono)
     * VALUES (?, ?, ?, ?)
     * 
     * MEJORAS FUTURAS (Cap 5):
     * @Valid Cliente cliente
     * → Bean Validation automática
     * 
     * @param cliente Objeto deserializado desde JSON del body
     * @return Response 201 con cliente creado, o 409 si duplicado
     */
    @POST
    @Transactional
    public Response crear(Cliente cliente) {
        // Validar DNI duplicado
        if (clienteRepository.existsByDni(cliente.dni)) {
            return Response.status(409).entity("DNI ya registrado").build();
        }
        
        // Validar email duplicado
        if (clienteRepository.existsByEmail(cliente.email)) {
            return Response.status(409).entity("Email ya registrado").build();
        }
        
        // Persistir
        clienteRepository.persist(cliente);
        
        // Retornar 201 Created con cliente (ahora tiene ID)
        return Response.status(201).entity(cliente).build();
    }
    
    /**
     * PUT /clientes/{id}
     * Actualiza un cliente existente.
     * 
     * ENDPOINT DE ACTUALIZACIÓN
     * 
     * Request HTTP:
     * PUT http://localhost:8080/clientes/1
     * Content-Type: application/json
     * 
     * {
     *   "nombre": "María González Pérez",  ← Cambio
     *   "telefono": "999888777"            ← Cambio
     * }
     * 
     * Response 200 OK (cliente existe):
     * {
     *   "id": 1,
     *   "nombre": "María González Pérez",  ← Actualizado
     *   "dni": "12345678",                 ← Sin cambio
     *   "email": "maria@example.com",      ← Sin cambio
     *   "telefono": "999888777"            ← Actualizado
     * }
     * 
     * Response 404 Not Found (cliente no existe):
     * "Cliente no encontrado"
     * 
     * @Transactional
     * Requerido para modificar entidad.
     * 
     * CAMPOS ACTUALIZABLES:
     * ✅ nombre: puede cambiar
     * ✅ telefono: puede cambiar
     * ❌ dni: INMUTABLE (no se actualiza)
     * ❌ email: INMUTABLE (no se actualiza)
     * 
     * ¿Por qué DNI y Email inmutables?
     * - Identificadores únicos del cliente
     * - Cambiarlos requiere proceso especial
     * - Auditoria y trazabilidad
     * - Evita confusión
     * 
     * Si cliente necesita cambiar DNI/Email:
     * → Endpoint separado con validaciones extra
     * → Logs de auditoría
     * → Posible aprobación manual
     * 
     * PROGRAMACIÓN FUNCIONAL:
     * 
     * findByIdOptional(id)
     * → Optional<Cliente>
     * 
     * .map(cliente -> { ... })
     * → Si existe:
     *   - Actualiza campos
     *   - Retorna Response 200 OK
     * 
     * .orElse(...)
     * → Si no existe:
     *   - Retorna Response 404
     * 
     * ACTUALIZACIÓN AUTOMÁTICA:
     * Dentro de @Transactional:
     * cliente.nombre = "nuevo valor";
     * 
     * Hibernate detecta cambio (dirty checking):
     * - Al commit: ejecuta UPDATE automático
     * - No necesitas llamar update() explícitamente
     * 
     * SQL ejecutado:
     * -- Búsqueda
     * SELECT c.id, c.nombre, c.dni, c.email, c.telefono
     * FROM clientes c
     * WHERE c.id = ?
     * 
     * -- Actualización (solo si existe)
     * UPDATE clientes
     * SET nombre = ?, telefono = ?
     * WHERE id = ?
     * 
     * PUT vs PATCH:
     * - PUT: Reemplazo completo (todos los campos)
     * - PATCH: Actualización parcial (solo campos enviados)
     * 
     * Aquí usamos PUT pero solo actualizamos algunos campos.
     * Podría considerarse PATCH semánticamente.
     * 
     * @param id ID del cliente a actualizar (de URL)
     * @param clienteActualizado Nuevos datos (de JSON body)
     * @return Response con cliente actualizado (200) o error (404)
     */
    @PUT
    @Path("/{id}")
    @Transactional
    public Response actualizar(@PathParam("id") Long id, Cliente clienteActualizado) {
        return clienteRepository.findByIdOptional(id)
                .map(cliente -> {
                    // Actualizar solo campos permitidos
                    cliente.nombre = clienteActualizado.nombre;
                    cliente.telefono = clienteActualizado.telefono;
                    // DNI y email NO se actualizan (inmutables)
                    
                    // Hibernate detecta cambios y hace UPDATE en commit
                    return Response.ok(cliente).build();
                })
                .orElse(Response.status(404).entity("Cliente no encontrado").build());
    }
    
    /**
     * DELETE /clientes/{id}
     * Elimina un cliente.
     * 
     * ENDPOINT DE ELIMINACIÓN
     * 
     * Request HTTP:
     * DELETE http://localhost:8080/clientes/3
     * 
     * Response 204 No Content (eliminado exitosamente):
     * (sin body, solo status code)
     * 
     * Response 404 Not Found (cliente no existe):
     * "Cliente no encontrado"
     * 
     * @Transactional
     * Requerido para operación DELETE.
     * 
     * ELIMINACIÓN EN CASCADA:
     * Si cliente tiene préstamos:
     * - Cliente.prestamos tiene cascade=ALL, orphanRemoval=true
     * - Al eliminar cliente → elimina sus préstamos automáticamente
     * - Al eliminar préstamos → elimina cuotas (Prestamo.cuotas cascade)
     * 
     * SQL ejecutado:
     * DELETE FROM cuotas WHERE prestamo_id IN (
     *     SELECT id FROM prestamos WHERE cliente_id = ?
     * );
     * 
     * DELETE FROM prestamos WHERE cliente_id = ?;
     * 
     * DELETE FROM clientes WHERE id = ?;
     * 
     * clienteRepository.deleteById(id)
     * - Retorna boolean
     * - true: cliente existía y fue eliminado
     * - false: cliente no existía
     * 
     * CÓDIGOS HTTP:
     * - 204 No Content: Eliminado exitosamente (estándar REST)
     * - 404 Not Found: ID no existe
     * 
     * Alternativa 200 OK:
     * return Response.ok()
     *     .entity("Cliente eliminado exitosamente")
     *     .build();
     * 
     * 204 vs 200:
     * - 204: Sin body (más eficiente)
     * - 200: Con body (info adicional)
     * - Ambos válidos, 204 es más común para DELETE
     * 
     * SOFT DELETE (alternativa):
     * En producción, a veces NO se elimina físicamente:
     * 
     * cliente.activo = false;
     * cliente.fechaEliminacion = LocalDateTime.now();
     * 
     * Ventajas:
     * - Auditoría completa
     * - Recuperación posible
     * - Historial preservado
     * 
     * VALIDACIÓN ADICIONAL (futura):
     * if (cliente.prestamos.stream().anyMatch(p -> p.estado == ACTIVO)) {
     *     return Response.status(409)
     *         .entity("No se puede eliminar cliente con préstamos activos")
     *         .build();
     * }
     * 
     * @param id ID del cliente a eliminar (de URL)
     * @return Response 204 (éxito) o 404 (no encontrado)
     */
    @DELETE
    @Path("/{id}")
    @Transactional
    public Response eliminar(@PathParam("id") Long id) {
        boolean eliminado = clienteRepository.deleteById(id);
        
        if (!eliminado) {
            return Response.status(404).entity("Cliente no encontrado").build();
        }
        
        return Response.status(204).build();
    }
}

/**
 * ═══════════════════════════════════════════════════════════════
 * MAPEO COMPLETO CRUD → HTTP
 * ═══════════════════════════════════════════════════════════════
 * 
 * ┌──────────┬────────┬─────────────────────┬────────────────────┐
 * │ CRUD     │ HTTP   │ Endpoint            │ Status Codes       │
 * ├──────────┼────────┼─────────────────────┼────────────────────┤
 * │ Create   │ POST   │ /clientes           │ 201, 409           │
 * │ Read All │ GET    │ /clientes           │ 200                │
 * │ Read One │ GET    │ /clientes/{id}      │ 200, 404           │
 * │ Update   │ PUT    │ /clientes/{id}      │ 200, 404           │
 * │ Delete   │ DELETE │ /clientes/{id}      │ 204, 404           │
 * └──────────┴────────┴─────────────────────┴────────────────────┘
 * 
 * ═══════════════════════════════════════════════════════════════
 * CÓDIGOS DE ESTADO HTTP USADOS
 * ═══════════════════════════════════════════════════════════════
 * 
 * 2xx - ÉXITO:
 * - 200 OK: GET exitoso, PUT exitoso
 * - 201 Created: POST exitoso (recurso creado)
 * - 204 No Content: DELETE exitoso (sin body)
 * 
 * 4xx - ERROR DEL CLIENTE:
 * - 404 Not Found: Recurso no existe
 * - 409 Conflict: Duplicado (DNI, Email)
 * 
 * 5xx - ERROR DEL SERVIDOR:
 * - 500 Internal Server Error: Exception no manejada
 * 
 * ═══════════════════════════════════════════════════════════════
 * FLUJO COMPLETO: CREAR CLIENTE
 * ═══════════════════════════════════════════════════════════════
 * 
 * 1. CLIENTE HTTP ENVÍA REQUEST:
 * POST http://localhost:8080/clientes
 * Content-Type: application/json
 * 
 * {
 *   "nombre": "Juan Pérez",
 *   "dni": "12345678",
 *   "email": "juan@example.com",
 *   "telefono": "987654321"
 * }
 * 
 * 2. QUARKUS RECIBE REQUEST:
 * - Vert.x procesa HTTP
 * - Identifica ruta: POST /clientes
 * - Match con ClienteResource.crear()
 * 
 * 3. JAX-RS PROCESA:
 * - Lee Content-Type: application/json
 * - Deserializa JSON → Cliente objeto
 * - Verifica @Consumes(JSON) ✅
 * 
 * 4. CDI INYECTA:
 * - clienteRepository ya inyectado
 * - Reutiliza instancia singleton
 * 
 * 5. MÉTODO crear() EJECUTA:
 * 
 *   a. Validar DNI:
 *      existsByDni("12345678")
 *      → SELECT COUNT(*) FROM clientes WHERE dni = '12345678'
 *      → count = 0 (no existe) ✅
 * 
 *   b. Validar Email:
 *      existsByEmail("juan@example.com")
 *      → SELECT COUNT(*) FROM clientes WHERE email = '...'
 *      → count = 0 (no existe) ✅
 * 
 *   c. Persistir:
 *      persist(cliente)
 *      → INSERT INTO clientes (nombre, dni, email, telefono)
 *        VALUES ('Juan Pérez', '12345678', 'juan@...', '987654321')
 *      → cliente.id = 1 (generado por BD)
 * 
 *   d. @Transactional commit:
 *      → Cambios confirmados en BD
 * 
 *   e. Response:
 *      Response.status(201).entity(cliente)
 * 
 * 6. JAX-RS SERIALIZA RESPONSE:
 * - Cliente objeto → Jackson → JSON
 * - @Produces(JSON) → Content-Type: application/json
 * 
 * 7. HTTP RESPONSE AL CLIENTE:
 * HTTP/1.1 201 Created
 * Content-Type: application/json
 * 
 * {
 *   "id": 1,
 *   "nombre": "Juan Pérez",
 *   "dni": "12345678",
 *   "email": "juan@example.com",
 *   "telefono": "987654321"
 * }
 * 
 * ═══════════════════════════════════════════════════════════════
 * TESTING DEL RESOURCE
 * ═══════════════════════════════════════════════════════════════
 * 
 * @QuarkusTest
 * public class ClienteResourceTest {
 *     
 *     @Test
 *     void testListarClientes() {
 *         given()
 *             .when().get("/clientes")
 *             .then()
 *             .statusCode(200)
 *             .contentType(MediaType.APPLICATION_JSON);
 *     }
 *     
 *     @Test
 *     void testCrearCliente() {
 *         Cliente cliente = new Cliente(
 *             "Test User",
 *             "99999999",
 *             "test@example.com",
 *             "999999999"
 *         );
 *         
 *         given()
 *             .contentType(MediaType.APPLICATION_JSON)
 *             .body(cliente)
 *         .when()
 *             .post("/clientes")
 *         .then()
 *             .statusCode(201)
 *             .body("id", notNullValue())
 *             .body("nombre", equalTo("Test User"));
 *     }
 *     
 *     @Test
 *     void testCrearClienteDniDuplicado() {
 *         // Crear primero
 *         Cliente c1 = new Cliente("User 1", "88888888", "a@e.com", "999");
 *         given().body(c1).post("/clientes").then().statusCode(201);
 *         
 *         // Intentar duplicar DNI
 *         Cliente c2 = new Cliente("User 2", "88888888", "b@e.com", "888");
 *         given().body(c2).post("/clientes")
 *             .then()
 *             .statusCode(409)
 *             .body(containsString("DNI ya registrado"));
 *     }
 * }
 * 
 * ═══════════════════════════════════════════════════════════════
 * MEJORAS FUTURAS
 * ═══════════════════════════════════════════════════════════════
 * 
 * Capítulo 5 - Bean Validation:
 *   @POST
 *   public Response crear(@Valid Cliente cliente) {
 *       // Validación automática antes de ejecutar
 *   }
 * 
 * Capítulo 6 - Exception Handling:
 *   @Provider
 *   public class ClienteNotFoundExceptionMapper 
 *       implements ExceptionMapper<ClienteNotFoundException> {
 *       // Manejo centralizado de errores
 *   }
 * 
 * Capítulo 7 - DTOs:
 *   public Response crear(ClienteCreateDTO dto) {
 *       // DTO separado para input
 *       Cliente cliente = dto.toEntity();
 *       ...
 *   }
 * 
 * Capítulo 8 - Seguridad:
 *   @DELETE
 *   @RolesAllowed("admin")
 *   public Response eliminar(...) {
 *       // Solo admin puede eliminar
 *   }
 * 
 * Capítulo 9 - Async/Reactive:
 *   @GET
 *   public Uni<List<Cliente>> listar() {
 *       // Retorno reactivo no bloqueante
 *   }
 * 
 * ═══════════════════════════════════════════════════════════════
 */