package pe.banco.cuentas.resource;

import pe.banco.cuentas.model.Cuenta;
import pe.banco.cuentas.service.CuentaService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * REST Resource (Controlador) para gestionar cuentas bancarias.
 * 
 * CAPA DE PRESENTACIÓN en arquitectura de 3 capas:
 * ┌─────────────────────────────────┐
 * │ Resource (esta clase)           │ ← Maneja HTTP/REST
 * ├─────────────────────────────────┤
 * │ Service (CuentaService)         │ ← Lógica de negocio
 * ├─────────────────────────────────┤
 * │ Data (Map/DB)                   │ ← Persistencia
 * └─────────────────────────────────┘
 * 
 * Responsabilidades:
 * ✅ Exponer endpoints HTTP
 * ✅ Manejar request/response
 * ✅ Serializar JSON ↔ Java
 * ✅ Códigos de estado HTTP
 * ✅ Delegar lógica al Service
 * 
 * ❌ NO hace validaciones de negocio (eso es del Service)
 * ❌ NO accede directamente a datos (eso es del Repository/Service)
 * 
 * Analogía: Como un cajero en el banco que:
 * - Atiende clientes (requests HTTP)
 * - Verifica formularios (valida formato)
 * - Delega operaciones al gerente (Service)
 * - Entrega resultados (response JSON)
 */

// ============================================
// ANOTACIONES DE CLASE
// ============================================

/**
 * @Path("/cuentas")
 * Define la ruta base para todos los endpoints de esta clase.
 * URL completa: http://localhost:8080/cuentas
 * 
 * Convención REST:
 * - Plural para colecciones: /cuentas (✅)
 * - Minúsculas: /cuentas (✅)
 * - No verbos en URL: /crear-cuenta (❌)
 */
@Path("/cuentas")

/**
 * @Produces(MediaType.APPLICATION_JSON)
 * Indica que TODOS los métodos de esta clase retornan JSON por defecto.
 * 
 * Cuando el cliente pide:
 * GET /cuentas
 * Accept: application/json
 * 
 * JAX-RS convierte automáticamente:
 * List<Cuenta> → JSON array
 * 
 * Proceso (marshalling):
 * Java Object → Jackson → JSON String → HTTP Response
 */
@Produces(MediaType.APPLICATION_JSON)

/**
 * @Consumes(MediaType.APPLICATION_JSON)
 * Indica que TODOS los métodos de esta clase aceptan JSON en el body.
 * 
 * Cuando el cliente envía:
 * POST /cuentas
 * Content-Type: application/json
 * {"numero":"123", "titular":"Ana", ...}
 * 
 * JAX-RS convierte automáticamente:
 * JSON String → Objeto Cuenta
 * 
 * Proceso (unmarshalling):
 * HTTP Request Body → Jackson → Java Object (Cuenta)
 */
@Consumes(MediaType.APPLICATION_JSON)

public class CuentaResource {
    
    // ============================================
    // INYECCIÓN DE DEPENDENCIAS (CDI)
    // ============================================
    
    /**
     * Inyección automática del servicio de cuentas.
     * 
     * @Inject le dice a Quarkus/CDI:
     * "Cuando crees esta clase, dame automáticamente una instancia de CuentaService"
     * 
     * VENTAJAS vs new CuentaService():
     * ✅ Desacoplamiento: No dependemos de la implementación concreta
     * ✅ Testing: Podemos inyectar mocks en tests
     * ✅ Lifecycle: CDI gestiona creación/destrucción
     * ✅ Singleton: Reutiliza la misma instancia (@ApplicationScoped)
     * 
     * Analogía: Como pedir un taxi por app en vez de comprar un auto.
     * No necesitas saber cómo funciona el auto, solo usarlo.
     * 
     * Flujo CDI:
     * 1. Quarkus detecta @Inject
     * 2. Busca bean compatible (CuentaService @ApplicationScoped)
     * 3. Lo inyecta automáticamente
     * 4. Si no existe, falla en tiempo de compilación (type-safe)
     */
    @Inject
    CuentaService cuentaService;
    
    // ============================================
    // ENDPOINTS REST - CRUD COMPLETO
    // ============================================
    
    /**
     * GET /cuentas
     * Lista todas las cuentas existentes.
     * 
     * Request:
     *   GET http://localhost:8080/cuentas
     *   Accept: application/json
     * 
     * Response 200 OK:
     *   [
     *     {"numero":"1000000001", "titular":"Juan Pérez", ...},
     *     {"numero":"1000000002", "titular":"María López", ...}
     *   ]
     * 
     * CARACTERÍSTICAS HTTP:
     * - Método: GET (idempotente y safe)
     * - Sin body en request
     * - Retorna colección (array JSON)
     * - Siempre 200 OK (incluso si lista vacía [])
     * 
     * @return Lista de todas las cuentas (nunca null, puede ser vacía)
     */
    @GET
    public List<Cuenta> listar() {
        // Delegación directa al Service
        // Resource NO tiene lógica, solo orquesta
        return cuentaService.listarTodas();
    }
    
    /**
     * GET /cuentas/{numero}
     * Obtiene una cuenta específica por su número.
     * 
     * Request:
     *   GET http://localhost:8080/cuentas/1000000001
     * 
     * Response 200 OK:
     *   {"numero":"1000000001", "titular":"Juan Pérez", "saldo":5000, "tipoCuenta":"AHORRO"}
     * 
     * Response 404 Not Found:
     *   "Cuenta no encontrada"
     * 
     * @PathParam("numero")
     * Extrae el valor de la URL y lo pasa como parámetro.
     * 
     * Ejemplo: /cuentas/1000000001
     *                   ↓
     *          @PathParam("numero") = "1000000001"
     * 
     * PATH PARAMETER vs QUERY PARAMETER:
     * - Path: /cuentas/{numero}          ← Identifica recurso específico
     * - Query: /cuentas?tipo=AHORRO      ← Filtra/pagina colección
     * 
     * @param numero Identificador único de la cuenta (extraído de la URL)
     * @return Response con cuenta encontrada (200) o error (404)
     */
    @GET
    @Path("/{numero}")  // {numero} es un placeholder que se captura con @PathParam
    public Response obtener(@PathParam("numero") String numero) {
        // Delegar búsqueda al Service
        Cuenta cuenta = cuentaService.obtenerPorNumero(numero);
        
        // Manejo de caso: cuenta no encontrada
        if (cuenta == null) {
            // HTTP 404 Not Found
            // Entity: mensaje de error simple (String)
            // En producción: usar ErrorResponse DTO estructurado
            return Response
                .status(404)
                .entity("Cuenta no encontrada")
                .build();
        }
        
        // HTTP 200 OK
        // Entity: objeto Cuenta serializado a JSON
        return Response
            .ok(cuenta)  // Shorthand para .status(200).entity(cuenta)
            .build();
    }
    
    /**
     * POST /cuentas
     * Crea una nueva cuenta bancaria.
     * 
     * Request:
     *   POST http://localhost:8080/cuentas
     *   Content-Type: application/json
     *   
     *   {
     *     "numero": "1000000004",
     *     "titular": "Ana Torres",
     *     "saldo": 3500.00,
     *     "tipoCuenta": "AHORRO"
     *   }
     * 
     * Response 201 Created:
     *   {
     *     "numero": "1000000004",
     *     "titular": "Ana Torres",
     *     "saldo": 3500.00,
     *     "tipoCuenta": "AHORRO"
     *   }
     * 
     * PROCESO AUTOMÁTICO (JAX-RS + Jackson):
     * 1. Request body (JSON String) →
     * 2. Jackson deserializa →
     * 3. Objeto Cuenta (parámetro método) →
     * 4. Lógica del método →
     * 5. Objeto Cuenta retornado →
     * 6. Jackson serializa →
     * 7. Response body (JSON String)
     * 
     * CÓDIGOS HTTP:
     * - 201 Created: Recurso creado exitosamente
     * - 400 Bad Request: JSON inválido o datos faltantes (validación futura)
     * - 409 Conflict: Cuenta duplicada (lógica futura en Service)
     * 
     * @param cuenta Objeto deserializado automáticamente desde JSON
     * @return Response 201 con la cuenta creada
     */
    @POST
    public Response crear(Cuenta cuenta) {
        // El parámetro 'cuenta' ya es un objeto Java completo
        // Jackson lo construyó desde el JSON del request body
        
        // Delegar creación al Service
        Cuenta nueva = cuentaService.crear(cuenta);
        
        // HTTP 201 Created
        // Indica que se creó un nuevo recurso
        // Best practice: incluir header Location con URL del recurso
        // Location: http://localhost:8080/cuentas/1000000004
        return Response
            .status(201)
            .entity(nueva)
            // .header("Location", "/cuentas/" + nueva.getNumero())  // Opcional
            .build();
    }
    
    /**
     * PUT /cuentas/{numero}
     * Actualiza una cuenta existente (reemplazo completo).
     * 
     * Request:
     *   PUT http://localhost:8080/cuentas/1000000004
     *   Content-Type: application/json
     *   
     *   {
     *     "numero": "1000000004",
     *     "titular": "Ana Torres",
     *     "saldo": 5000.00,
     *     "tipoCuenta": "CORRIENTE"
     *   }
     * 
     * Response 200 OK:
     *   {"numero":"1000000004", "titular":"Ana Torres", "saldo":5000, "tipoCuenta":"CORRIENTE"}
     * 
     * Response 404 Not Found:
     *   "Cuenta no encontrada"
     * 
     * PUT vs PATCH:
     * - PUT: Reemplazo COMPLETO (envía todos los campos)
     * - PATCH: Actualización PARCIAL (solo campos modificados)
     * 
     * IDEMPOTENCIA:
     * Ejecutar PUT múltiples veces = mismo resultado
     * PUT /cuentas/123 {saldo: 1000} → saldo = 1000
     * PUT /cuentas/123 {saldo: 1000} → saldo = 1000 (sin cambios)
     * 
     * @param numero Identificador de la cuenta a actualizar (URL)
     * @param cuenta Nuevos datos de la cuenta (JSON body)
     * @return Response con cuenta actualizada (200) o error (404)
     */
    @PUT
    @Path("/{numero}")
    public Response actualizar(
        @PathParam("numero") String numero,  // De la URL
        Cuenta cuenta                         // Del body JSON
    ) {
        // Delegar actualización al Service
        // Service valida existencia y aplica cambios
        Cuenta actualizada = cuentaService.actualizar(numero, cuenta);
        
        // Manejo de caso: cuenta no encontrada
        if (actualizada == null) {
            // HTTP 404 Not Found
            return Response
                .status(404)
                .entity("Cuenta no encontrada")
                .build();
        }
        
        // HTTP 200 OK
        // Retorna la cuenta actualizada
        return Response
            .ok(actualizada)
            .build();
    }
    
    /**
     * DELETE /cuentas/{numero}
     * Elimina una cuenta bancaria.
     * 
     * Request:
     *   DELETE http://localhost:8080/cuentas/1000000003
     * 
     * Response 204 No Content:
     *   (sin body, solo status code)
     * 
     * Response 404 Not Found:
     *   "Cuenta no encontrada"
     * 
     * CÓDIGOS HTTP:
     * - 204 No Content: Eliminación exitosa (sin cuerpo en respuesta)
     * - 200 OK: Eliminación exitosa (con info de lo eliminado)
     * - 404 Not Found: Recurso no existe
     * 
     * IDEMPOTENCIA:
     * DELETE es idempotente:
     * - 1er DELETE /cuentas/123 → elimina (204)
     * - 2do DELETE /cuentas/123 → ya no existe (404), pero estado final es el mismo
     * 
     * @param numero Identificador de la cuenta a eliminar
     * @return Response 204 (éxito) o 404 (no encontrada)
     */
    @DELETE
    @Path("/{numero}")
    public Response eliminar(@PathParam("numero") String numero) {
        // Delegar eliminación al Service
        // Service retorna true si eliminó, false si no existía
        boolean eliminada = cuentaService.eliminar(numero);
        
        // Manejo de caso: cuenta no encontrada
        if (!eliminada) {
            // HTTP 404 Not Found
            return Response
                .status(404)
                .entity("Cuenta no encontrada")
                .build();
        }
        
        // HTTP 204 No Content
        // Status code indica éxito
        // Sin body (por convención para DELETE)
        return Response
            .status(204)
            .build();
    }
}

/**
 * ═══════════════════════════════════════════════════════════════
 * FLUJO COMPLETO DE UNA PETICIÓN
 * ═══════════════════════════════════════════════════════════════
 * 
 * Ejemplo: GET /cuentas/1000000001
 * 
 * 1. Cliente HTTP envía request:
 *    GET http://localhost:8080/cuentas/1000000001
 *    Accept: application/json
 * 
 * 2. Servidor Quarkus (Vert.x):
 *    - Recibe request HTTP
 *    - Identifica ruta: /cuentas/1000000001
 *    - Match con @Path("/cuentas") + @Path("/{numero}")
 * 
 * 3. JAX-RS (RESTEasy Reactive):
 *    - Extrae path parameter: numero = "1000000001"
 *    - Verifica Accept header (JSON OK)
 *    - Invoca: obtener("1000000001")
 * 
 * 4. CDI Container:
 *    - Inyecta CuentaService (singleton @ApplicationScoped)
 * 
 * 5. Método obtener():
 *    - Llama: cuentaService.obtenerPorNumero("1000000001")
 *    - Service busca en Map y retorna Cuenta
 *    - Resource valida (null check)
 *    - Construye Response con Cuenta
 * 
 * 6. JAX-RS + Jackson:
 *    - Serializa Cuenta → JSON
 *    - Agrega headers (Content-Type: application/json)
 * 
 * 7. HTTP Response al cliente:
 *    HTTP/1.1 200 OK
 *    Content-Type: application/json
 *    
 *    {"numero":"1000000001", "titular":"Juan Pérez", ...}
 * 
 * ═══════════════════════════════════════════════════════════════
 * MAPEO CRUD → HTTP
 * ═══════════════════════════════════════════════════════════════
 * 
 * | Operación | HTTP   | Endpoint            | Status   |
 * |-----------|--------|---------------------|----------|
 * | Create    | POST   | /cuentas            | 201      |
 * | Read All  | GET    | /cuentas            | 200      |
 * | Read One  | GET    | /cuentas/{numero}   | 200/404  |
 * | Update    | PUT    | /cuentas/{numero}   | 200/404  |
 * | Delete    | DELETE | /cuentas/{numero}   | 204/404  |
 * 
 * ═══════════════════════════════════════════════════════════════
 * BUENAS PRÁCTICAS APLICADAS
 * ═══════════════════════════════════════════════════════════════
 * 
 * ✅ Arquitectura en Capas
 *    Resource → Service → Data
 *    Separación de responsabilidades
 * 
 * ✅ Inyección de Dependencias
 *    @Inject en vez de new
 *    Desacoplamiento y testing
 * 
 * ✅ RESTful Design
 *    Recursos, no acciones
 *    /cuentas (✅) vs /obtenerCuentas (❌)
 * 
 * ✅ Códigos HTTP Correctos
 *    200 OK, 201 Created, 204 No Content, 404 Not Found
 * 
 * ✅ Content Negotiation
 *    @Produces/@Consumes
 *    JSON automático
 * 
 * ✅ Idempotencia
 *    GET, PUT, DELETE son idempotentes
 *    POST NO es idempotente
 * 
 * ═══════════════════════════════════════════════════════════════
 * MEJORAS FUTURAS (Próximos Capítulos)
 * ═══════════════════════════════════════════════════════════════
 * 
 * Capítulo 4 - Validación:
 *   public Response crear(@Valid Cuenta cuenta) {...}
 *   → Validación automática con Bean Validation
 * 
 * Capítulo 5 - Exception Handling:
 *   @Provider
 *   ExceptionMapper<CuentaNoEncontradaException>
 *   → Manejo centralizado de errores
 * 
 * Capítulo 6 - Seguridad:
 *   @RolesAllowed("admin")
 *   → Solo admin puede eliminar cuentas
 * 
 * Capítulo 7 - Async:
 *   public Uni<List<Cuenta>> listar() {...}
 *   → Endpoints reactivos no bloqueantes
 * 
 * ═══════════════════════════════════════════════════════════════
 */