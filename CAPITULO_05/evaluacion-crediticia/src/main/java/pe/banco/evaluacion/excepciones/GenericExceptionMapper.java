package pe.banco.evaluacion.excepciones;

import jakarta.ws.rs.core.Response;  // Construcción de respuestas HTTP con códigos de estado
import jakarta.ws.rs.ext.ExceptionMapper;  // Interfaz JAX-RS para mapear excepciones a respuestas HTTP
import jakarta.ws.rs.ext.Provider;  // Marca la clase como proveedor JAX-RS
import org.jboss.logging.Logger;  // Sistema de logging de Quarkus/JBoss

import java.util.HashMap;  // Estructura de datos para construir respuestas JSON
import java.util.Map;  // Interfaz de HashMap

/**
 * Mapper genérico de excepciones para errores no anticipados.
 * <p>
 * Este mapper actúa como "red de seguridad" (safety net) capturando TODAS las excepciones
 * no manejadas explícitamente por otros mappers más específicos. Convierte excepciones
 * inesperadas del sistema en respuestas HTTP 500 Internal Server Error estructuradas,
 * evitando exponer stack traces y detalles técnicos sensibles al cliente.
 * </p>
 * <p>
 * <b>Analogía:</b> Piensa en este mapper como el "plan de contingencia" del banco. Así como
 * un banco tiene procedimientos específicos para cada tipo de problema (robo, incendio, falla
 * eléctrica), pero también tiene un protocolo general de "emergencia no identificada" que
 * activa alarmas y notifica a seguridad sin entrar en pánico, este mapper captura errores
 * inesperados, los registra para investigación (logging), y responde al cliente con un mensaje
 * profesional genérico sin revelar información sensible del sistema.
 * </p>
 * 
 * <h3>Jerarquía de Exception Mappers:</h3>
 * <pre>
 * Exception (base de todas las excepciones)
 *   ├─ RuntimeException
 *   │    ├─ ValidationException
 *   │    │    └─ ConstraintViolationException ← ValidationExceptionMapper (específico)
 *   │    ├─ NullPointerException              ← GenericExceptionMapper
 *   │    ├─ IllegalArgumentException          ← GenericExceptionMapper
 *   │    └─ ...otras runtime exceptions       ← GenericExceptionMapper
 *   └─ SQLException                            ← GenericExceptionMapper
 *   └─ IOException                             ← GenericExceptionMapper
 *   └─ ...cualquier otra exception             ← GenericExceptionMapper (catch-all)
 * </pre>
 * 
 * <h3>Orden de evaluación de mappers:</h3>
 * <p>
 * JAX-RS busca el mapper más específico para cada excepción:
 * <ol>
 *   <li>Se lanza ConstraintViolationException</li>
 *   <li>JAX-RS encuentra ExceptionMapper&lt;ConstraintViolationException&gt; → Lo usa ✅</li>
 *   <li>Se lanza NullPointerException</li>
 *   <li>JAX-RS NO encuentra ExceptionMapper&lt;NullPointerException&gt;</li>
 *   <li>JAX-RS busca en jerarquía superior: ExceptionMapper&lt;RuntimeException&gt; → No existe</li>
 *   <li>JAX-RS busca en raíz: ExceptionMapper&lt;Exception&gt; → Encuentra este mapper ✅</li>
 * </ol>
 * </p>
 * 
 * <h3>Casos de uso típicos:</h3>
 * <ul>
 *   <li><b>NullPointerException:</b> Bug en código (acceso a objeto null)</li>
 *   <li><b>SQLException:</b> Problema de conectividad o query inválido en BD</li>
 *   <li><b>ClassCastException:</b> Error de tipos en casting</li>
 *   <li><b>IllegalStateException:</b> Operación en estado inválido</li>
 *   <li><b>OutOfMemoryError:</b> Agotamiento de memoria heap</li>
 *   <li><b>Cualquier RuntimeException:</b> Errores inesperados del sistema</li>
 * </ul>
 * 
 * <h3>Formato de respuesta producido:</h3>
 * <pre>
 * HTTP/1.1 500 Internal Server Error
 * Content-Type: application/json
 * 
 * {
 *   "error": "Error interno del servidor",
 *   "mensaje": "Cannot invoke \"String.length()\" because \"dni\" is null",
 *   "status": 500
 * }
 * </pre>
 * 
 * <h3>⚠️ CRÍTICO - Seguridad en producción:</h3>
 * <p>
 * La implementación actual expone exception.getMessage() directamente al cliente.
 * Esto es PELIGROSO en producción porque puede revelar:
 * <ul>
 *   <li><b>Estructura de código:</b> Nombres de clases internas, métodos, rutas de archivos</li>
 *   <li><b>Estructura de BD:</b> Nombres de tablas, columnas, constraints en SQLExceptions</li>
 *   <li><b>Configuración:</b> Paths de servidor, versiones de librerías</li>
 *   <li><b>Credenciales:</b> En el peor caso, contraseñas en mensajes de error</li>
 * </ul>
 * </p>
 * 
 * <h4>Ejemplos de información sensible expuesta:</h4>
 * <pre>
 * // SQLException puede exponer estructura de BD:
 * "mensaje": "ERROR: duplicate key value violates unique constraint \"solicitudes_credito_email_key\""
 * → Revela: nombre de tabla (solicitudes_credito), columna (email), tipo de constraint
 * 
 * // NullPointerException puede exponer código:
 * "mensaje": "Cannot invoke \"pe.banco.evaluacion.servicios.ScoringService.calcularScore\" because \"this.scoringService\" is null"
 * → Revela: paquete completo, clase, método, variable
 * 
 * // FileNotFoundException puede exponer paths:
 * "mensaje": "/opt/banco/config/database.properties (No such file or directory)"
 * → Revela: estructura de directorios del servidor
 * </pre>
 * 
 * <h4>Solución recomendada para producción:</h4>
 * <pre>
 * @Override
 * public Response toResponse(Exception exception) {
 *     // Generar ID único para correlación
 *     String errorId = UUID.randomUUID().toString();
 *     
 *     // Logging completo con stack trace (solo en servidor)
 *     LOG.error("Error ID [{}]: Error no controlado", errorId, exception);
 *     
 *     Map&lt;String, Object&gt; error = new HashMap&lt;&gt;();
 *     error.put("error", "Error interno del servidor");
 *     error.put("errorId", errorId);  // Cliente puede reportar este ID
 *     error.put("status", 500);
 *     
 *     // EN PRODUCCIÓN: NO exponer exception.getMessage()
 *     // Usar mensaje genérico seguro
 *     if (isProdEnvironment()) {
 *         error.put("mensaje", "Ha ocurrido un error inesperado. Por favor contacte a soporte con el ID de error.");
 *     } else {
 *         // En desarrollo/testing: mostrar mensaje para debugging
 *         error.put("mensaje", exception.getMessage());
 *         error.put("type", exception.getClass().getSimpleName());
 *     }
 *     
 *     return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
 *         .entity(error)
 *         .build();
 * }
 * </pre>
 * 
 * <h3>Logging estratégico:</h3>
 * <p>
 * El mapper usa LOG.error() con nivel ERROR, lo cual es apropiado porque:
 * <ul>
 *   <li>Errores 500 representan problemas del sistema, no del usuario</li>
 *   <li>Requieren investigación y acción correctiva inmediata</li>
 *   <li>Deben generar alertas en sistemas de monitoreo (Grafana, PagerDuty)</li>
 * </ul>
 * </p>
 * 
 * <h4>Niveles de logging apropiados por status:</h4>
 * <pre>
 * 400 Bad Request       → LOG.debug() (error de usuario, esperado)
 * 404 Not Found         → LOG.debug() (recurso no existe, normal)
 * 500 Internal Error    → LOG.error() (problema del sistema, crítico)
 * 503 Service Unavailable → LOG.warn() (temporal, requiere atención)
 * </pre>
 * 
 * <h4>Información crucial para logging:</h4>
 * <pre>
 * LOG.error("Error no controlado [{}] - Usuario: {} - Endpoint: {} - Request: {}",
 *     errorId,
 *     securityContext.getUserPrincipal().getName(),
 *     uriInfo.getPath(),
 *     requestBody,
 *     exception  // Stack trace completo
 * );
 * </pre>
 * 
 * <h3>Monitoreo y alertas:</h3>
 * <p>
 * Este mapper debe integrarse con sistema de monitoreo para:
 * <ul>
 *   <li><b>Conteo de errores 500:</b> Métrica clave de salud del sistema</li>
 *   <li><b>Alertas automáticas:</b> Si tasa de errores 500 supera umbral (ej: 5 en 5 min)</li>
 *   <li><b>Análisis de tendencias:</b> Detectar degradación gradual del sistema</li>
 *   <li><b>Dashboards:</b> Visualizar tipos de excepciones más frecuentes</li>
 * </ul>
 * </p>
 * 
 * <h4>Integración con Micrometer/Prometheus:</h4>
 * <pre>
 * @Inject
 * MeterRegistry registry;
 * 
 * @Override
 * public Response toResponse(Exception exception) {
 *     // Incrementar contador de errores 500
 *     registry.counter("http.server.errors", 
 *         "status", "500",
 *         "exception", exception.getClass().getSimpleName()
 *     ).increment();
 *     
 *     LOG.error("Error no controlado", exception);
 *     // ...resto del código
 * }
 * </pre>
 * 
 * <h3>Testing del mapper:</h3>
 * <pre>
 * @Test
 * void debeRetornar500ParaNullPointerException() {
 *     GenericExceptionMapper mapper = new GenericExceptionMapper();
 *     NullPointerException ex = new NullPointerException("Objeto null");
 *     
 *     Response response = mapper.toResponse(ex);
 *     
 *     assertEquals(500, response.getStatus());
 *     Map body = (Map) response.getEntity();
 *     assertEquals("Error interno del servidor", body.get("error"));
 *     assertEquals("Objeto null", body.get("mensaje"));
 * }
 * </pre>
 * 
 * <h3>Relación con otros componentes:</h3>
 * <ul>
 *   <li><b>ValidationExceptionMapper:</b> Maneja errores de validación (400)</li>
 *   <li><b>Este mapper:</b> Maneja todo lo demás (500)</li>
 *   <li><b>WebApplicationException:</b> Ya mapean automáticamente (404, 403, etc.)</li>
 * </ul>
 * 
 * @see ValidationExceptionMapper
 * @see ExceptionMapper
 * @see jakarta.ws.rs.WebApplicationException
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    /**
     * Logger para registro de errores no controlados.
     * <p>
     * Utiliza el sistema de logging de Quarkus (basado en JBoss Logging) que proporciona:
     * <ul>
     *   <li>Rendimiento optimizado con lazy evaluation de mensajes</li>
     *   <li>Integración con MDC (Mapped Diagnostic Context) para request tracking</li>
     *   <li>Configuración centralizada en application.properties</li>
     *   <li>Soporte para múltiples backends (console, file, Graylog, etc.)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Configuración típica en application.properties:</b>
     * <pre>
     * # Nivel de log global
     * quarkus.log.level=INFO
     * 
     * # Nivel específico para este paquete
     * quarkus.log.category."pe.banco.evaluacion.excepciones".level=ERROR
     * 
     * # Formato de salida
     * quarkus.log.console.format=%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n
     * 
     * # Archivo de logs
     * quarkus.log.file.enable=true
     * quarkus.log.file.path=/var/log/banco/application.log
     * quarkus.log.file.rotation.max-file-size=10M
     * quarkus.log.file.rotation.max-backup-index=5
     * </pre>
     * </p>
     */
    private static final Logger LOG = Logger.getLogger(GenericExceptionMapper.class);

    /**
     * Transforma una excepción no anticipada en respuesta HTTP 500 estructurada.
     * <p>
     * Este método es el último recurso para manejar excepciones en la aplicación.
     * Si una excepción llega aquí, significa que:
     * <ol>
     *   <li>No fue capturada con try-catch en código de negocio</li>
     *   <li>No existe un ExceptionMapper más específico para ese tipo</li>
     *   <li>Es un error inesperado que requiere investigación</li>
     * </ol>
     * </p>
     * 
     * <h4>Flujo de ejecución:</h4>
     * <ol>
     *   <li><b>Logging:</b> Registra error completo con stack trace en logs del servidor</li>
     *   <li><b>Construcción de respuesta:</b> Crea Map con error genérico + mensaje específico</li>
     *   <li><b>Retorno HTTP 500:</b> Indica al cliente que hubo un problema del servidor</li>
     * </ol>
     * 
     * <h4>Ejemplo de ejecución:</h4>
     * <pre>
     * // Supongamos que ScoringService no fue inyectado correctamente
     * @POST
     * @Path("/evaluar")
     * public Response evaluar(@Valid SolicitudCreditoDTO dto) {
     *     // scoringService es null porque faltó @Inject
     *     Integer score = scoringService.calcularScore(solicitud);  // ← NullPointerException
     * }
     * 
     * // Flujo:
     * 1. NullPointerException lanzada en línea anterior
     * 2. No hay try-catch en evaluar() → Excepción propaga
     * 3. JAX-RS runtime captura excepción
     * 4. Busca ExceptionMapper&lt;NullPointerException&gt; → No existe
     * 5. Busca ExceptionMapper&lt;Exception&gt; → Encuentra este mapper
     * 6. Llama toResponse(nullPointerException)
     * 7. Logger registra: "ERROR [pe.banco.evaluacion.excepciones.GenericExceptionMapper] Error no controlado
     *    java.lang.NullPointerException: Cannot invoke calcularScore...
     *        at pe.banco.evaluacion.recursos.CreditoRecurso.evaluar(CreditoRecurso.java:45)
     *        ..."
     * 8. Cliente recibe:
     *    HTTP/1.1 500 Internal Server Error
     *    {
     *      "error": "Error interno del servidor",
     *      "mensaje": "Cannot invoke \"pe.banco.evaluacion.servicios.ScoringService.calcularScore\"...",
     *      "status": 500
     *    }
     * </pre>
     * 
     * <h4>⚠️ Exposición de información sensible:</h4>
     * <p>
     * Como se mencionó en la documentación de la clase, exponer exception.getMessage()
     * directamente es un riesgo de seguridad. Ejemplos de qué puede filtrarse:
     * </p>
     * <pre>
     * // Database exception expone estructura
     * SQLException: "Duplicate entry '12345678' for key 'solicitudes_credito.dni_unique'"
     * → Cliente ve: nombre de tabla, columna, tipo de índice
     * 
     * // File access expone paths de servidor
     * IOException: "/opt/banco/app/config/secret.key (Permission denied)"
     * → Cliente ve: estructura de directorios, ubicación de archivos de configuración
     * 
     * // Class loading expone librerías
     * ClassNotFoundException: "com.mysql.cj.jdbc.Driver"
     * → Cliente ve: base de datos usada (MySQL), versión implícita
     * </pre>
     * 
     * <h4>Mejora crítica para producción:</h4>
     * <pre>
     * @Override
     * public Response toResponse(Exception exception) {
     *     String errorId = UUID.randomUUID().toString();
     *     
     *     // Log completo solo visible en servidor
     *     LOG.error("Error ID [" + errorId + "]: Error no controlado", exception);
     *     
     *     Map&lt;String, Object&gt; error = new HashMap&lt;&gt;();
     *     error.put("error", "Error interno del servidor");
     *     error.put("errorId", errorId);
     *     error.put("status", 500);
     *     
     *     // Mensaje sanitizado para cliente
     *     error.put("mensaje", "Ha ocurrido un error inesperado. " +
     *         "Por favor contacte a soporte con el código de error: " + errorId);
     *     
     *     // Opcionalmente agregar timestamp
     *     error.put("timestamp", Instant.now().toString());
     *     
     *     return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
     *         .entity(error)
     *         .build();
     * }
     * </pre>
     * 
     * <h4>Ventajas del errorId:</h4>
     * <ul>
     *   <li><b>Correlación:</b> Conecta reporte del usuario con log del servidor</li>
     *   <li><b>Privacidad:</b> Cliente solo ve ID, no detalles técnicos</li>
     *   <li><b>Eficiencia de soporte:</b> "Tengo error XYZ-123" → buscar en logs</li>
     *   <li><b>Auditoría:</b> Rastrear incidencias y tiempo de resolución</li>
     * </ul>
     * 
     * <h4>Integración con monitoreo:</h4>
     * <pre>
     * @Inject
     * MeterRegistry meterRegistry;
     * 
     * @Inject
     * AlertService alertService;
     * 
     * @Override
     * public Response toResponse(Exception exception) {
     *     String errorId = UUID.randomUUID().toString();
     *     String exceptionType = exception.getClass().getSimpleName();
     *     
     *     // Logging
     *     LOG.error("Error ID [" + errorId + "]: " + exceptionType, exception);
     *     
     *     // Métricas
     *     meterRegistry.counter("application.errors",
     *         "type", exceptionType,
     *         "status", "500"
     *     ).increment();
     *     
     *     // Alertas para excepciones críticas
     *     if (exception instanceof OutOfMemoryError || 
     *         exception instanceof SQLException) {
     *         alertService.alertarEquipoOps("Error crítico: " + errorId, exception);
     *     }
     *     
     *     // ...construir y retornar respuesta
     * }
     * </pre>
     * 
     * <p>
     * <b>Thread-safety:</b> Este método ES thread-safe por las mismas razones que
     * ValidationExceptionMapper: no mantiene estado, todas las variables son locales.
     * </p>
     *
     * @param exception Cualquier excepción no manejada explícitamente
     * @return Response HTTP 500 Internal Server Error con JSON estructurado
     */
    @Override
    public Response toResponse(Exception exception) {
        // Logging completo con stack trace (solo visible en logs del servidor)
        LOG.error("Error no controlado", exception);

        // Construcción de respuesta para el cliente
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Error interno del servidor");
        error.put("mensaje", exception.getMessage());  // ⚠️ RIESGO DE SEGURIDAD - ver documentación
        error.put("status", 500);

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(error)
            .build();
    }
}