package pe.banco.evaluacion.recursos;

import jakarta.inject.Inject;  // CDI para inyección de dependencias
import jakarta.transaction.Transactional;  // Gestión declarativa de transacciones JTA
import jakarta.validation.Valid;  // Trigger de validación automática de Bean Validation
import jakarta.ws.rs.*;  // Anotaciones JAX-RS para definir endpoints REST
import jakarta.ws.rs.core.MediaType;  // Constantes de tipos MIME (application/json, etc.)
import jakarta.ws.rs.core.Response;  // Construcción de respuestas HTTP con códigos de estado
import pe.banco.evaluacion.dtos.SolicitudCreditoDTO;  // DTO de entrada para solicitudes
import pe.banco.evaluacion.entidades.SolicitudCredito;  // Entidad JPA de solicitud
import pe.banco.evaluacion.entidades.SolicitudCredito.EstadoSolicitud;  // Enum de estados
import pe.banco.evaluacion.repositorios.SolicitudCreditoRepository;  // Acceso a datos
import pe.banco.evaluacion.servicios.ScoringService;  // Lógica de scoring crediticio

import java.util.HashMap;  // Estructura de datos para respuestas JSON flexibles
import java.util.List;  // Colección estándar para múltiples resultados
import java.util.Map;  // Interfaz de HashMap

/**
 * Recurso REST para gestión de solicitudes de crédito.
 * <p>
 * Este recurso (controlador en terminología MVC/Spring) expone la API REST que permite
 * a los clientes interactuar con el sistema de evaluación crediticia. Implementa endpoints
 * para crear solicitudes, consultarlas individualmente y listar todas las solicitudes.
 * </p>
 * <p>
 * <b>Analogía:</b> Piensa en este recurso como la "ventanilla de atención al cliente" del banco.
 * Así como un cliente se acerca a una ventanilla para presentar su solicitud de crédito,
 * consultar el estado de una solicitud existente, o ver su historial, los clientes de esta API
 * "se acercan" a estos endpoints HTTP para realizar las mismas operaciones de forma digital.
 * </p>
 * 
 * <h3>Arquitectura REST implementada:</h3>
 * <ul>
 *   <li><b>Diseño orientado a recursos:</b> URLs representan entidades (créditos)</li>
 *   <li><b>Verbos HTTP semánticos:</b> POST para crear, GET para consultar</li>
 *   <li><b>Códigos de estado apropiados:</b> 201 Created, 200 OK, 404 Not Found</li>
 *   <li><b>Content negotiation:</b> Consume y produce JSON exclusivamente</li>
 *   <li><b>Stateless:</b> Cada request es independiente, sin sesión en servidor</li>
 * </ul>
 * 
 * <h3>Endpoints expuestos:</h3>
 * <table border="1">
 *   <tr>
 *     <th>Método</th>
 *     <th>Path</th>
 *     <th>Propósito</th>
 *     <th>Código éxito</th>
 *   </tr>
 *   <tr>
 *     <td>POST</td>
 *     <td>/api/v1/creditos/evaluar</td>
 *     <td>Crear y evaluar nueva solicitud</td>
 *     <td>201 Created</td>
 *   </tr>
 *   <tr>
 *     <td>GET</td>
 *     <td>/api/v1/creditos/{id}</td>
 *     <td>Consultar solicitud por ID</td>
 *     <td>200 OK</td>
 *   </tr>
 *   <tr>
 *     <td>GET</td>
 *     <td>/api/v1/creditos</td>
 *     <td>Listar todas las solicitudes</td>
 *     <td>200 OK</td>
 *   </tr>
 * </table>
 * 
 * <h3>Flujo de una solicitud POST /evaluar:</h3>
 * <ol>
 *   <li>Cliente envía JSON con datos del solicitante</li>
 *   <li>Quarkus deserializa JSON a SolicitudCreditoDTO</li>
 *   <li>Bean Validation valida automáticamente (@Valid)</li>
 *   <li>Si validación falla → 400 Bad Request con detalle de errores</li>
 *   <li>Si validación OK → mapeo manual de DTO a entidad</li>
 *   <li>ScoringService calcula score y determina aprobación</li>
 *   <li>Entidad se persiste en base de datos (@Transactional)</li>
 *   <li>Respuesta JSON con resultado se retorna al cliente (201 Created)</li>
 * </ol>
 * 
 * <h3>Características de diseño:</h3>
 * <ul>
 *   <li><b>Inyección de dependencias:</b> Repository y Service inyectados vía CDI</li>
 *   <li><b>Transaccionalidad:</b> @Transactional garantiza ACID en operaciones de escritura</li>
 *   <li><b>Validación automática:</b> @Valid activa Bean Validation sin código adicional</li>
 *   <li><b>Separación de DTOs:</b> Entrada (SolicitudCreditoDTO) vs Salida (Map/Entidad)</li>
 *   <li><b>Mapeo explícito:</b> Control total sobre qué campos exponer en respuesta</li>
 * </ul>
 * 
 * <h3>Mejoras pendientes (TODOs para producción):</h3>
 * <ul>
 *   <li>Agregar paginación en listarTodas() para manejar grandes volúmenes</li>
 *   <li>Implementar DTO de respuesta tipado en lugar de Map genérico</li>
 *   <li>Agregar endpoints para actualizar y eliminar solicitudes</li>
 *   <li>Implementar filtros de búsqueda (por estado, rango de fechas, DNI)</li>
 *   <li>Agregar documentación OpenAPI/Swagger con @OpenAPIDefinition</li>
 *   <li>Implementar versionado de API (v2, v3) para evolución sin breaking changes</li>
 *   <li>Agregar rate limiting para prevenir abuso de API</li>
 *   <li>Implementar HATEOAS con links a recursos relacionados</li>
 * </ul>
 * 
 * @see SolicitudCreditoDTO
 * @see SolicitudCredito
 * @see ScoringService
 * @see SolicitudCreditoRepository
 */
@Path("/api/v1/creditos")
@Produces(MediaType.APPLICATION_JSON)  // Todas las respuestas serán JSON
@Consumes(MediaType.APPLICATION_JSON)  // Todas las peticiones esperan JSON
public class CreditoRecurso {

    /**
     * Repositorio para operaciones de persistencia de solicitudes.
     * <p>
     * Inyectado automáticamente por CDI de Quarkus. No requiere new ni configuración
     * adicional gracias a @ApplicationScoped en el repositorio.
     * </p>
     */
    @Inject
    SolicitudCreditoRepository repository;

    /**
     * Servicio de cálculo de scoring y evaluación crediticia.
     * <p>
     * Inyectado automáticamente por CDI. Contiene toda la lógica de negocio
     * para determinar si una solicitud debe ser aprobada o rechazada.
     * </p>
     */
    @Inject
    ScoringService scoringService;

    /**
     * Evalúa una nueva solicitud de crédito y la persiste en la base de datos.
     * <p>
     * Este endpoint implementa el flujo completo de evaluación crediticia automatizada:
     * recibe datos del cliente, valida, calcula score, determina aprobación, genera razón,
     * persiste resultado y retorna respuesta estructurada al cliente.
     * </p>
     * <p>
     * <b>HTTP Method:</b> POST (crea nuevo recurso)<br>
     * <b>Path completo:</b> POST /api/v1/creditos/evaluar<br>
     * <b>Content-Type:</b> application/json<br>
     * <b>Success Status:</b> 201 Created<br>
     * <b>Error Status:</b> 400 Bad Request (validación), 500 Internal Server Error (excepción)
     * </p>
     * 
     * <h4>Request Body esperado (JSON):</h4>
     * <pre>
     * {
     *   "dni": "12345678",
     *   "nombreCompleto": "Juan Carlos Pérez López",
     *   "email": "juan.perez@example.com",
     *   "edad": 35,
     *   "ingresosMensuales": 5000.00,
     *   "deudasActuales": 1200.00,
     *   "montoSolicitado": 150000.00,
     *   "mesesEnEmpleoActual": 24
     * }
     * </pre>
     * 
     * <h4>Response Body de éxito (JSON 201):</h4>
     * <pre>
     * {
     *   "solicitudId": 123,
     *   "dni": "12345678",
     *   "nombreCompleto": "Juan Carlos Pérez López",
     *   "scoreCrediticio": 720,
     *   "aprobada": true,
     *   "razonEvaluacion": "Aprobado: Perfil crediticio cumple con los requisitos del banco.",
     *   "estado": "APROBADA"
     * }
     * </pre>
     * 
     * <h4>Response Body de error de validación (JSON 400):</h4>
     * <pre>
     * {
     *   "error": "Errores de validación",
     *   "status": 400,
     *   "violaciones": {
     *     "dni": "DNI inválido. Debe contener 8 dígitos",
     *     "edad": "Debe ser mayor de 18 años"
     *   }
     * }
     * </pre>
     * 
     * <h4>Flujo interno detallado:</h4>
     * <ol>
     *   <li><b>Recepción y validación automática:</b> @Valid activa Bean Validation sobre dto.
     *       Si falla, ConstraintViolationException se lanza automáticamente y es manejada
     *       por {@link pe.banco.evaluacion.excepciones.ValidationExceptionMapper}</li>
     *   <li><b>Mapeo DTO → Entidad:</b> Copia manual campo por campo de dto a nueva entidad.
     *       Esto da control total sobre qué se persiste y evita problemas de mass assignment</li>
     *   <li><b>Estado inicial:</b> Se establece EN_PROCESO para indicar que está siendo evaluada</li>
     *   <li><b>Cálculo de score:</b> scoringService.calcularScore() evalúa múltiples factores
     *       y asigna puntuación 0-1000</li>
     *   <li><b>Validación integral:</b> scoringService.esAprobadaConValidaciones() verifica
     *       tanto validaciones críticas como umbral de score</li>
     *   <li><b>Generación de razón:</b> scoringService.generarRazonEvaluacion() crea explicación
     *       textual legible para el cliente</li>
     *   <li><b>Actualización de entidad:</b> Se setean score, aprobada, razón y estado final</li>
     *   <li><b>Persistencia transaccional:</b> @Transactional garantiza que todo se guarda
     *       o nada (atomicidad). Si hay error, rollback automático</li>
     *   <li><b>Construcción de respuesta:</b> Se crea Map con campos selectivos a exponer</li>
     *   <li><b>Retorno con status 201:</b> Indica creación exitosa del recurso</li>
     * </ol>
     * 
     * <p>
     * <b>¿Por qué @Transactional aquí y no en el servicio?</b> En este diseño, la transacción
     * se maneja en el controlador porque toda la operación (mapeo, scoring, persistencia) debe
     * ser atómica. Alternativamente, podría moverse a un método de servicio de orquestación.
     * Ambos enfoques son válidos; este prioriza simplicidad para caso de uso único.
     * </p>
     * 
     * <p>
     * <b>Nota sobre mapeo manual:</b> En lugar de usar MapStruct o ModelMapper, se hace mapeo
     * manual campo por campo. Ventajas: explícito, sin magia, fácil de debuggear. Desventajas:
     * verboso, propenso a errores si se agregan campos. Para proyectos grandes, considera
     * automatizar con MapStruct.
     * </p>
     * 
     * <p>
     * <b>Seguridad:</b> Este endpoint no tiene autenticación/autorización. En producción, agregar:
     * <ul>
     *   <li>@RolesAllowed("CLIENTE") para control de acceso</li>
     *   <li>Rate limiting para prevenir spam de solicitudes</li>
     *   <li>Logging de auditoría (quién solicitó qué y cuándo)</li>
     *   <li>Encriptación de datos sensibles antes de persistir</li>
     * </ul>
     * </p>
     * 
     * <h4>Ejemplo de llamada con cURL:</h4>
     * <pre>
     * curl -X POST http://localhost:8080/api/v1/creditos/evaluar \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "dni": "12345678",
     *     "nombreCompleto": "Juan Pérez",
     *     "email": "juan@example.com",
     *     "edad": 35,
     *     "ingresosMensuales": 5000.00,
     *     "deudasActuales": 1000.00,
     *     "montoSolicitado": 100000.00,
     *     "mesesEnEmpleoActual": 24
     *   }'
     * </pre>
     *
     * @param dto Datos de la solicitud validados automáticamente por Bean Validation
     * @return Response HTTP 201 con JSON conteniendo resultado de evaluación
     * @throws jakarta.validation.ConstraintViolationException si validación falla (manejada por mapper)
     */
    @POST
    @Path("/evaluar")
    @Transactional
    public Response evaluar(@Valid SolicitudCreditoDTO dto) {
        // Mapeo explícito de DTO a Entidad (control total sobre qué se persiste)
        SolicitudCredito solicitud = new SolicitudCredito();
        solicitud.setDni(dto.getDni());
        solicitud.setNombreCompleto(dto.getNombreCompleto());
        solicitud.setEmail(dto.getEmail());
        solicitud.setEdad(dto.getEdad());
        solicitud.setIngresosMensuales(dto.getIngresosMensuales());
        solicitud.setDeudasActuales(dto.getDeudasActuales());
        solicitud.setMontoSolicitado(dto.getMontoSolicitado());
        solicitud.setMesesEnEmpleoActual(dto.getMesesEnEmpleoActual());
        solicitud.setEstado(EstadoSolicitud.EN_PROCESO);

        // Evaluación crediticia completa
        Integer score = scoringService.calcularScore(solicitud);
        boolean aprobada = scoringService.esAprobadaConValidaciones(solicitud, score);
        String razon = scoringService.generarRazonEvaluacion(solicitud, score);

        // Actualización de resultado en la entidad
        solicitud.setScoreCrediticio(score);
        solicitud.setAprobada(aprobada);
        solicitud.setRazonEvaluacion(razon);
        solicitud.setEstado(aprobada ? EstadoSolicitud.APROBADA : EstadoSolicitud.RECHAZADA);

        // Persistencia transaccional (rollback automático si hay excepción)
        repository.persist(solicitud);

        // Construcción de respuesta con campos selectivos
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("solicitudId", solicitud.id);
        respuesta.put("dni", solicitud.getDni());
        respuesta.put("nombreCompleto", solicitud.getNombreCompleto());
        respuesta.put("scoreCrediticio", solicitud.getScoreCrediticio());
        respuesta.put("aprobada", solicitud.getAprobada());
        respuesta.put("razonEvaluacion", solicitud.getRazonEvaluacion());
        respuesta.put("estado", solicitud.getEstado());

        return Response.status(Response.Status.CREATED).entity(respuesta).build();
    }

    /**
     * Obtiene una solicitud de crédito específica por su ID.
     * <p>
     * Endpoint de consulta que permite recuperar los detalles completos de una solicitud
     * existente. Útil para tracking de solicitudes, dashboards de administración, o cuando
     * el cliente quiere consultar el estado de su solicitud previamente enviada.
     * </p>
     * <p>
     * <b>HTTP Method:</b> GET (operación idempotente de lectura)<br>
     * <b>Path completo:</b> GET /api/v1/creditos/{id}<br>
     * <b>Success Status:</b> 200 OK<br>
     * <b>Error Status:</b> 404 Not Found (ID no existe)
     * </p>
     * 
     * <h4>Response Body de éxito (JSON 200):</h4>
     * <pre>
     * {
     *   "id": 123,
     *   "dni": "12345678",
     *   "nombreCompleto": "Juan Pérez",
     *   "email": "juan@example.com",
     *   "edad": 35,
     *   "ingresosMensuales": 5000.00,
     *   "deudasActuales": 1200.00,
     *   "montoSolicitado": 150000.00,
     *   "mesesEnEmpleoActual": 24,
     *   "scoreCrediticio": 720,
     *   "aprobada": true,
     *   "razonEvaluacion": "Aprobado: ...",
     *   "estado": "APROBADA",
     *   "fechaCreacion": "2025-01-15T10:30:00",
     *   "fechaActualizacion": "2025-01-15T10:30:05"
     * }
     * </pre>
     * 
     * <h4>Response de error (204 o 404):</h4>
     * <pre>
     * HTTP/1.1 404 Not Found
     * (sin body)
     * </pre>
     * 
     * <p>
     * <b>Nota sobre exposición de entidad completa:</b> Este endpoint retorna la entidad
     * JPA directamente, exponiendo todos sus campos. Esto puede ser problemático en producción
     * por varias razones:
     * <ul>
     *   <li><b>Acoplamiento:</b> Cambios en la entidad afectan automáticamente al contrato de API</li>
     *   <li><b>Seguridad:</b> Campos internos sensibles podrían exponerse accidentalmente</li>
     *   <li><b>Performance:</b> Lazy loading puede causar N+1 queries o LazyInitializationException</li>
     *   <li><b>Versionado:</b> Dificulta mantener múltiples versiones de API</li>
     * </ul>
     * </p>
     * 
     * <p>
     * <b>Mejora recomendada:</b> Crear SolicitudCreditoResponseDTO y mapear entidad → DTO:
     * <pre>
     * public Response obtenerPorId(@PathParam("id") Long id) {
     *     SolicitudCredito solicitud = repository.findById(id);
     *     if (solicitud == null) {
     *         return Response.status(Response.Status.NOT_FOUND).build();
     *     }
     *     SolicitudCreditoResponseDTO dto = mapper.toDto(solicitud);
     *     return Response.ok(dto).build();
     * }
     * </pre>
     * </p>
     * 
     * <h4>Casos de uso:</h4>
     * <ul>
     *   <li><b>Dashboard de cliente:</b> "Ver estado de mi solicitud #123"</li>
     *   <li><b>Notificaciones:</b> Sistema recupera datos para enviar email con resultado</li>
     *   <li><b>Auditoría:</b> Analistas revisan solicitudes específicas para investigación</li>
     *   <li><b>APIs de terceros:</b> Sistemas externos consultan estado de solicitudes integradas</li>
     * </ul>
     * 
     * <h4>Ejemplo de llamada con cURL:</h4>
     * <pre>
     * curl -X GET http://localhost:8080/api/v1/creditos/123
     * </pre>
     * 
     * <p>
     * <b>Seguridad:</b> Este endpoint permite consultar cualquier solicitud por ID sin
     * verificar ownership. En producción, implementar:
     * <ul>
     *   <li>Autenticación para identificar quién consulta</li>
     *   <li>Autorización para verificar que el usuario puede ver esa solicitud específica</li>
     *   <li>Logging de accesos para auditoría de quién consultó qué</li>
     * </ul>
     * Ejemplo con autenticación:
     * <pre>
     * @GET
     * @Path("/{id}")
     * @RolesAllowed({"CLIENTE", "ADMIN"})
     * public Response obtenerPorId(@PathParam("id") Long id, @Context SecurityContext sec) {
     *     // Verificar que el usuario autenticado puede ver esta solicitud
     *     if (!puedeVerSolicitud(sec.getUserPrincipal(), id)) {
     *         return Response.status(Response.Status.FORBIDDEN).build();
     *     }
     *     // ...resto del código
     * }
     * </pre>
     * </p>
     *
     * @param id Identificador único de la solicitud a consultar (path parameter)
     * @return Response 200 OK con entidad completa si existe, 404 Not Found si no existe
     */
    @GET
    @Path("/{id}")
    public Response obtenerPorId(@PathParam("id") Long id) {
        SolicitudCredito solicitud = repository.findById(id);
        if (solicitud == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(solicitud).build();
    }

    /**
     * Lista todas las solicitudes de crédito en el sistema.
     * <p>
     * Endpoint de consulta masiva que retorna todas las solicitudes sin filtros ni paginación.
     * Útil para dashboards administrativos, reportes o exportaciones de datos.
     * </p>
     * <p>
     * <b>HTTP Method:</b> GET<br>
     * <b>Path completo:</b> GET /api/v1/creditos<br>
     * <b>Success Status:</b> 200 OK<br>
     * <b>Response:</b> Array JSON con todas las solicitudes
     * </p>
     * 
     * <h4>Response Body de éxito (JSON 200):</h4>
     * <pre>
     * [
     *   {
     *     "id": 1,
     *     "dni": "12345678",
     *     "nombreCompleto": "Juan Pérez",
     *     "scoreCrediticio": 720,
     *     "aprobada": true,
     *     "estado": "APROBADA",
     *     ...
     *   },
     *   {
     *     "id": 2,
     *     "dni": "87654321",
     *     "nombreCompleto": "María García",
     *     "scoreCrediticio": 590,
     *     "aprobada": false,
     *     "estado": "RECHAZADA",
     *     ...
     *   },
     *   ...
     * ]
     * </pre>
     * 
     * <p>
     * <b>⚠️ ADVERTENCIA - Problema crítico de escalabilidad:</b> Este endpoint NO implementa
     * paginación, lo que lo hace inadecuado para producción. Problemas que causará:
     * <ul>
     *   <li><b>Out of Memory:</b> Con 100,000+ solicitudes, la aplicación puede quedarse sin heap</li>
     *   <li><b>Timeouts:</b> Query y serialización de toda la tabla puede tomar minutos</li>
     *   <li><b>Performance de BD:</b> SELECT * sin LIMIT sobrecarga la base de datos</li>
     *   <li><b>Experiencia de usuario:</b> Respuesta de 50MB+ es inutilizable en frontend</li>
     * </ul>
     * </p>
     * 
     * <p>
     * <b>Solución requerida - Implementar paginación:</b>
     * <pre>
     * @GET
     * public Response listarTodas(
     *     @QueryParam("page") @DefaultValue("0") int page,
     *     @QueryParam("size") @DefaultValue("20") int size,
     *     @QueryParam("estado") EstadoSolicitud estado) {
     *     
     *     PanacheQuery&lt;SolicitudCredito&gt; query;
     *     if (estado != null) {
     *         query = repository.find("estado", estado);
     *     } else {
     *         query = repository.findAll();
     *     }
     *     
     *     List&lt;SolicitudCredito&gt; solicitudes = query
     *         .page(page, size)
     *         .list();
     *     
     *     long total = query.count();
     *     int totalPages = (int) Math.ceil((double) total / size);
     *     
     *     Map&lt;String, Object&gt; response = Map.of(
     *         "content", solicitudes,
     *         "page", page,
     *         "size", size,
     *         "totalElements", total,
     *         "totalPages", totalPages
     *     );
     *     
     *     return Response.ok(response).build();
     * }
     * </pre>
     * </p>
     * 
     * <p>
     * <b>Otras mejoras necesarias:</b>
     * <ul>
     *   <li><b>Filtros:</b> Permitir filtrar por estado, rango de fechas, aprobada/rechazada</li>
     *   <li><b>Ordenamiento:</b> Permitir ordenar por fecha, score, estado</li>
     *   <li><b>Proyección:</b> Permitir solicitar solo campos específicos (GraphQL-style)</li>
     *   <li><b>Búsqueda:</b> Full-text search por nombre, email, DNI</li>
     * </ul>
     * Ejemplo de llamada con filtros:
     * <pre>
     * GET /api/v1/creditos?page=0&size=20&estado=APROBADA&sort=fechaCreacion,desc
     * </pre>
     * </p>
     * 
     * <h4>Casos de uso actuales (limitados):</h4>
     * <ul>
     *   <li>Desarrollo/testing con dataset pequeño (&lt;100 registros)</li>
     *   <li>Dashboard simple sin filtros en ambiente de pruebas</li>
     *   <li>Exportación one-time de toda la data (mejor usar batch job)</li>
     * </ul>
     * 
     * <h4>Ejemplo de llamada con cURL:</h4>
     * <pre>
     * curl -X GET http://localhost:8080/api/v1/creditos
     * </pre>
     * 
     * <p>
     * <b>Seguridad:</b> Similar a obtenerPorId(), este endpoint debe protegerse:
     * <ul>
     *   <li>Solo usuarios ADMIN deben poder listar todas las solicitudes</li>
     *   <li>Clientes regulares solo deberían ver sus propias solicitudes</li>
     *   <li>Implementar @RolesAllowed("ADMIN") o filtrado por usuario autenticado</li>
     * </ul>
     * </p>
     * 
     * <p>
     * <b>Performance actual en producción (estimado):</b>
     * <ul>
     *   <li>1,000 solicitudes: ~200ms response time, ~500KB payload</li>
     *   <li>10,000 solicitudes: ~2-5s response time, ~5MB payload</li>
     *   <li>100,000 solicitudes: Timeout probable, OutOfMemoryError posible</li>
     * </ul>
     * </p>
     *
     * @return Lista completa de todas las solicitudes en la base de datos (sin paginación)
     */
    @GET
    public List<SolicitudCredito> listarTodas() {
        return repository.listAll();
    }
}