package pe.banco.tasacorp.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import pe.banco.tasacorp.model.ConversionResponse;
import pe.banco.tasacorp.model.TasaResponse;
import pe.banco.tasacorp.service.TasaService;

import java.util.Map;

/**
 * REST Resource para operaciones de tasas de cambio.
 * 
 * 📋 PROPÓSITO:
 * Este es el controlador REST (capa de presentación) que expone
 * los endpoints HTTP para que clientes externos consulten tasas
 * y realicen conversiones de moneda.
 * 
 * 🏗️ ARQUITECTURA - CAPA DE PRESENTACIÓN:
 * 
 * Esta clase es la ENTRADA a la aplicación:
 * 
 * Cliente HTTP (Postman, cURL, browser, app móvil)
 *     ↓  HTTP Request
 * TasaResource (REST) ← Estamos aquí
 *     ↓  Java method call
 * TasaService (Lógica de negocio)
 *     ↓  Config access
 * TasaCorpConfig (Configuración)
 *     ↓  JSON serialization
 * DTOs (ConversionResponse / TasaResponse)
 *     ↓  HTTP Response
 * Cliente recibe JSON
 * 
 * 💡 JAX-RS (Jakarta RESTful Web Services):
 * 
 * Quarkus usa JAX-RS para crear APIs REST.
 * Las anotaciones (@Path, @GET, etc.) definen el contrato REST.
 * 
 * VENTAJAS:
 * ✅ Estándar Jakarta EE (no vendor lock-in)
 * ✅ Declarativo (anotaciones, poco código)
 * ✅ Serialización JSON automática
 * ✅ Manejo de errores integrado
 * 
 * 🎯 RESPONSABILIDADES DE UN RESOURCE:
 * 
 * 1. RECIBIR requests HTTP
 *    → Parsear parámetros de URL y query strings
 * 
 * 2. VALIDAR inputs básicos
 *    → Tipos correctos, formatos válidos
 * 
 * 3. DELEGAR al servicio
 *    → El Resource NO tiene lógica de negocio
 * 
 * 4. MANEJAR errores
 *    → Convertir excepciones en HTTP status codes
 * 
 * 5. DEVOLVER respuestas HTTP
 *    → Convertir objetos Java a JSON automáticamente
 * 
 * ❌ LO QUE NO DEBE HACER UN RESOURCE:
 * 
 * - NO debe tener lógica de negocio (eso es del Service)
 * - NO debe acceder directamente a configuración (usa el Service)
 * - NO debe hacer cálculos (eso es del Service)
 * - NO debe conectarse a bases de datos (eso es del Service/Repository)
 * 
 * 🔗 ANOTACIONES A NIVEL DE CLASE:
 * 
 * @Path("/api/tasas"):
 *   - Define la ruta BASE de todos los endpoints
 *   - Todos los métodos heredan este prefijo
 * 
 * @Produces(MediaType.APPLICATION_JSON):
 *   - Todos los métodos devuelven JSON por defecto
 *   - Header: Content-Type: application/json
 * 
 * @Consumes(MediaType.APPLICATION_JSON):
 *   - Todos los métodos aceptan JSON por defecto
 *   - Header: Accept: application/json
 * 
 * 📊 ENDPOINTS DISPONIBLES:
 * 
 * 1. GET  /api/tasas/config          → Ver configuración actual
 * 2. GET  /api/tasas/{moneda}        → Consultar tasa de una moneda
 * 3. GET  /api/tasas/convertir/{moneda}?monto=X → Convertir monto
 * 4. GET  /api/tasas/health          → Health check
 * 
 * 🎭 COMPORTAMIENTO POR PERFIL:
 * 
 * Aunque este Resource es el mismo en todos los perfiles,
 * las RESPUESTAS varían porque el Service usa configuración
 * diferente según el perfil activo.
 * 
 * EJEMPLO - Endpoint: GET /api/tasas/convertir/USD?monto=1000
 * 
 * DEV:
 *   → comision: 0.0, limite: 999999, proveedor: MockProvider
 * 
 * TEST:
 *   → comision: 56.25, limite: 1000, proveedor: FreeCurrencyAPI
 * 
 * PROD:
 *   → comision: 93.75, limite: 50000, proveedor: PremiumProvider
 * 
 * @author Arquitectura TasaCorp
 * @version 1.0.0
 * @see TasaService Para la lógica de negocio
 * @see ConversionResponse Para el formato de respuesta de conversiones
 * @see TasaResponse Para el formato de respuesta de consultas de tasa
 */
@Path("/api/tasas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TasaResource {

    // ========================================================================
    // CONSTANTES Y LOGGER
    // ========================================================================

    /**
     * Logger para registrar las peticiones HTTP.
     * 
     * 💡 LOGGING EN RESOURCES:
     * 
     * Es buena práctica loguear:
     * - Cada request que llega (método + ruta)
     * - Parámetros recibidos
     * - Errores que ocurren
     * 
     * BENEFICIOS:
     * - Debugging: Ver qué endpoints se están llamando
     * - Auditoría: Rastrear quién usa qué
     * - Monitoreo: Detectar endpoints con alto tráfico
     * - Troubleshooting: Investigar problemas en producción
     * 
     * EJEMPLO DE LOG:
     * INFO  [pe.ban.tas.res.TasaResource] Endpoint: GET /api/tasas/USD
     * INFO  [pe.ban.tas.res.TasaResource] Endpoint: GET /api/tasas/convertir/EUR?monto=500.00
     */
    private static final Logger LOG = Logger.getLogger(TasaResource.class);

    // ========================================================================
    // DEPENDENCIAS INYECTADAS
    // ========================================================================

    /**
     * Servicio de tasas de cambio.
     * 
     * 💉 @Inject:
     * Quarkus inyecta automáticamente una instancia del servicio.
     * 
     * 🎯 PATRÓN DE DISEÑO:
     * Esto es DEPENDENCY INJECTION (Inyección de Dependencias):
     * 
     * MAL (acoplamiento fuerte):
     * <pre>
     * TasaService tasaService = new TasaService(); // ❌
     * </pre>
     * 
     * BIEN (acoplamiento débil):
     * <pre>
     * @Inject
     * TasaService tasaService; // ✅
     * </pre>
     * 
     * VENTAJAS:
     * ✅ El Resource no necesita saber CÓMO crear el Service
     * ✅ Fácil de testear (puedes inyectar un mock)
     * ✅ El framework maneja el ciclo de vida
     * ✅ Se comparte una sola instancia (eficiente)
     * 
     * 💡 SEPARACIÓN DE RESPONSABILIDADES:
     * 
     * TasaResource: "¿Qué endpoint llamaron?"
     *     ↓
     * TasaService: "¿Qué lógica ejecutar?"
     *     ↓
     * TasaCorpConfig: "¿Qué configuración usar?"
     * 
     * Cada capa tiene una responsabilidad clara y única.
     */
    @Inject
    TasaService tasaService;

    // ========================================================================
    // ENDPOINTS REST
    // ========================================================================

    /**
     * Endpoint para obtener la configuración actual del sistema.
     * 
     * 📋 ENDPOINT:
     * GET /api/tasas/config
     * 
     * 🎯 PROPÓSITO:
     * Exponer la configuración activa para debugging y verificación.
     * Útil para scripts de prueba y monitoreo.
     * 
     * 📊 RESPUESTA:
     * HTTP 200 OK
     * Content-Type: application/json
     * 
     * Body:
     * {
     *   "aplicacion": "TasaCorp API",
     *   "perfil_activo": "prod",
     *   "ambiente": "producción",
     *   "proveedor": "PremiumProvider",
     *   "proveedor_url": "https://api.currencylayer.com/live",
     *   "moneda_base": "PEN",
     *   "monedas_soportadas": ["USD", "EUR", "MXN"],
     *   "limite_transaccional": 50000,
     *   "comision_porcentaje": 2.5,
     *   "cache_habilitado": true,
     *   "auditoria_habilitada": true,
     *   "refresh_minutos": 15
     * }
     * 
     * 💡 ANOTACIONES:
     * 
     * @GET:
     *   - Este método responde a peticiones HTTP GET
     *   - Solo lectura, no modifica nada (idempotente)
     * 
     * @Path("/config"):
     *   - Ruta específica de este método
     *   - Ruta completa: /api/tasas + /config = /api/tasas/config
     * 
     * 📝 EJEMPLO DE USO:
     * 
     * curl http://localhost:8080/api/tasas/config
     * 
     * O en un browser:
     * http://localhost:8080/api/tasas/config
     * 
     * 🔍 USO EN SCRIPTS DE PRUEBA:
     * Los scripts test-part1-config.sh y test-part2-profiles.sh
     * llaman este endpoint para validar que la configuración
     * sea la correcta según el perfil activo.
     * 
     * @return Response HTTP 200 con la configuración en JSON
     */
    @GET
    @Path("/config")
    public Response obtenerConfiguracion() {
        // Loguear la petición recibida
        LOG.info("Endpoint: GET /api/tasas/config");
        
        // Delegar al servicio para obtener la configuración
        Map<String, Object> config = tasaService.obtenerConfiguracion();
        
        // Construir respuesta HTTP 200 OK con el mapa como JSON
        // Quarkus convierte automáticamente el Map a JSON
        return Response.ok(config).build();
    }

    /**
     * Endpoint para consultar la tasa de cambio de una moneda específica.
     * 
     * 📋 ENDPOINT:
     * GET /api/tasas/{moneda}
     * 
     * 🎯 PROPÓSITO:
     * Permite consultar la tasa actual SIN realizar conversión.
     * El cliente pregunta: "¿A cuánto está el dólar?"
     * 
     * 📊 EJEMPLOS DE USO:
     * 
     * GET /api/tasas/USD  → Tasa del dólar
     * GET /api/tasas/EUR  → Tasa del euro
     * GET /api/tasas/MXN  → Tasa del peso mexicano
     * GET /api/tasas/JPY  → Error 400 (no soportado)
     * 
     * 📥 PARÁMETROS:
     * 
     * {moneda} (path parameter):
     *   - Código de la moneda destino
     *   - Ejemplo: "USD", "EUR", "MXN"
     *   - Case insensitive: "usd" → "USD"
     * 
     * 💡 @PathParam:
     * Extrae el valor de {moneda} de la URL y lo pasa al método.
     * 
     * URL:    /api/tasas/USD
     *                     ^^^
     *                     ↓
     * Método: obtenerTasa(moneda="USD")
     * 
     * 📊 RESPUESTA EXITOSA:
     * HTTP 200 OK
     * Content-Type: application/json
     * 
     * Body:
     * {
     *   "moneda_origen": "PEN",
     *   "moneda_destino": "USD",
     *   "tasa_cambio": 3.75,
     *   "comision_porcentaje": 2.5,
     *   "proveedor": "PremiumProvider",
     *   "ambiente": "producción"
     * }
     * 
     * 📊 RESPUESTA DE ERROR:
     * HTTP 400 BAD REQUEST
     * Content-Type: application/json
     * 
     * Body:
     * {
     *   "error": "Moneda no soportada: JPY"
     * }
     * 
     * 🔍 MANEJO DE ERRORES:
     * 
     * Si el servicio lanza IllegalArgumentException (moneda no soportada),
     * el Resource lo captura y devuelve un HTTP 400 con mensaje descriptivo.
     * 
     * FLUJO:
     * 1. Cliente pide /api/tasas/JPY
     * 2. Service valida y lanza IllegalArgumentException
     * 3. Resource captura la excepción
     * 4. Resource devuelve HTTP 400 con {"error": "..."}
     * 5. Cliente recibe error descriptivo
     * 
     * 📝 EJEMPLO DE USO:
     * 
     * curl http://localhost:8080/api/tasas/USD
     * 
     * @param moneda Código de la moneda destino (extraído de la URL)
     * @return Response HTTP 200 con TasaResponse en JSON, o HTTP 400 si hay error
     */
    @GET
    @Path("/{moneda}")
    public Response obtenerTasa(@PathParam("moneda") String moneda) {
        // Loguear la petición con el parámetro recibido
        LOG.infof("Endpoint: GET /api/tasas/%s", moneda);
        
        try {
            // Normalizar a mayúsculas y delegar al servicio
            // toUpperCase(): "usd" → "USD" (aceptar cualquier case)
            TasaResponse response = tasaService.obtenerTasa(moneda.toUpperCase());
            
            // Si todo OK, devolver HTTP 200 con el DTO
            // Quarkus serializa automáticamente TasaResponse a JSON
            return Response.ok(response).build();
            
        } catch (IllegalArgumentException e) {
            // Si el servicio lanza excepción (moneda no soportada),
            // devolver HTTP 400 BAD REQUEST con el mensaje de error
            return Response.status(Response.Status.BAD_REQUEST)
                          .entity(Map.of("error", e.getMessage()))
                          .build();
        }
    }

    /**
     * Endpoint para convertir un monto de PEN a otra moneda.
     * 
     * 📋 ENDPOINT:
     * GET /api/tasas/convertir/{moneda}?monto={cantidad}
     * 
     * 🎯 PROPÓSITO:
     * Realiza una conversión completa incluyendo:
     * - Cálculo de conversión
     * - Aplicación de comisión
     * - Validación de límites
     * 
     * 📊 EJEMPLOS DE USO:
     * 
     * GET /api/tasas/convertir/USD?monto=1000
     *   → Convertir 1000 PEN a USD
     * 
     * GET /api/tasas/convertir/EUR?monto=500.50
     *   → Convertir 500.50 PEN a EUR
     * 
     * GET /api/tasas/convertir/MXN
     *   → Convertir 100 PEN a MXN (monto por defecto)
     * 
     * 📥 PARÁMETROS:
     * 
     * {moneda} (path parameter):
     *   - Código de la moneda destino
     *   - Ejemplo: "USD", "EUR", "MXN"
     *   - Obligatorio
     *   - @PathParam extrae de la URL
     * 
     * monto (query parameter):
     *   - Cantidad en PEN a convertir
     *   - Ejemplo: 1000, 500.50
     *   - Opcional (default: 100)
     *   - @QueryParam extrae del query string
     *   - @DefaultValue("100") si no se proporciona
     * 
     * 💡 QUERY PARAMETER vs PATH PARAMETER:
     * 
     * PATH PARAMETER (parte de la ruta):
     *   /api/tasas/convertir/USD
     *                        ^^^
     *   - Identifica el RECURSO
     *   - Obligatorio (no puede faltar)
     *   - Afecta la semántica de la URL
     * 
     * QUERY PARAMETER (después de ?):
     *   /api/tasas/convertir/USD?monto=1000
     *                             ^^^^^^^^^^
     *   - FILTRA o MODIFICA el recurso
     *   - Opcional (puede tener default)
     *   - No afecta la semántica de la URL
     * 
     * 📊 RESPUESTA EXITOSA:
     * HTTP 200 OK
     * Content-Type: application/json
     * 
     * Body (ejemplo en PROD):
     * {
     *   "monto_origen": 1000.0,
     *   "moneda_origen": "PEN",
     *   "monto_convertido": 3750.0,
     *   "moneda_destino": "USD",
     *   "tasa_aplicada": 3.75,
     *   "comision": 93.75,
     *   "monto_total": 3843.75,
     *   "proveedor": "PremiumProvider",
     *   "limite_transaccional": 50000,
     *   "dentro_limite": true
     * }
     * 
     * 📊 RESPUESTA DE ERROR:
     * HTTP 400 BAD REQUEST
     * Content-Type: application/json
     * 
     * Body:
     * {
     *   "error": "Moneda no soportada: JPY"
     * }
     * 
     * 🎭 COMPORTAMIENTO POR PERFIL:
     * 
     * Mismo endpoint, diferentes resultados según perfil:
     * 
     * GET /api/tasas/convertir/USD?monto=1000
     * 
     * DEV:
     *   - comision: 0.0 (gratis)
     *   - limite_transaccional: 999999
     *   - dentro_limite: true
     *   - proveedor: MockProvider
     * 
     * TEST:
     *   - comision: 56.25 (1.5%)
     *   - limite_transaccional: 1000
     *   - dentro_limite: true
     *   - proveedor: FreeCurrencyAPI
     * 
     * PROD:
     *   - comision: 93.75 (2.5%)
     *   - limite_transaccional: 50000
     *   - dentro_limite: true
     *   - proveedor: PremiumProvider
     * 
     * 📝 EJEMPLO DE USO:
     * 
     * curl "http://localhost:8080/api/tasas/convertir/USD?monto=1000"
     * 
     * Nota: Las comillas son importantes en shells para preservar el ?
     * 
     * @param moneda Código de la moneda destino (extraído de la URL)
     * @param monto Cantidad en PEN a convertir (extraída del query string, default 100)
     * @return Response HTTP 200 con ConversionResponse en JSON, o HTTP 400 si hay error
     */
    @GET
    @Path("/convertir/{moneda}")
    public Response convertirMoneda(
            @PathParam("moneda") String moneda,
            @QueryParam("monto") @DefaultValue("100") Double monto) {
        
        // Loguear la petición con todos los parámetros
        LOG.infof("Endpoint: GET /api/tasas/convertir/%s?monto=%.2f", moneda, monto);
        
        try {
            // Normalizar a mayúsculas y delegar al servicio
            ConversionResponse response = tasaService.convertirMoneda(moneda.toUpperCase(), monto);
            
            // Si todo OK, devolver HTTP 200 con el DTO completo
            // Quarkus serializa automáticamente ConversionResponse a JSON
            return Response.ok(response).build();
            
        } catch (IllegalArgumentException e) {
            // Si el servicio lanza excepción (moneda no soportada),
            // devolver HTTP 400 BAD REQUEST con el mensaje de error
            return Response.status(Response.Status.BAD_REQUEST)
                          .entity(Map.of("error", e.getMessage()))
                          .build();
        }
    }

    /**
     * Endpoint de health check simple.
     * 
     * 📋 ENDPOINT:
     * GET /api/tasas/health
     * 
     * 🎯 PROPÓSITO:
     * Verificar que el servicio está vivo y respondiendo.
     * 
     * 💡 HEALTH CHECKS:
     * 
     * Los health checks son esenciales en producción para:
     * 
     * - KUBERNETES: Saber cuándo reiniciar un pod
     * - LOAD BALANCERS: Saber a qué instancias enviar tráfico
     * - MONITOREO: Detectar servicios caídos
     * - ALERTAS: Notificar cuando algo falla
     * 
     * TIPOS DE HEALTH CHECKS:
     * 
     * 1. Liveness (¿está vivo?):
     *    - Si falla → reiniciar contenedor
     *    - Este endpoint es un liveness simple
     * 
     * 2. Readiness (¿está listo?):
     *    - Si falla → no enviar tráfico
     *    - Verificaría dependencias (DB, Vault, etc.)
     * 
     * 🏗️ HEALTH CHECK PROFESIONAL:
     * 
     * Quarkus tiene health checks integrados en:
     * /q/health/live
     * /q/health/ready
     * 
     * Este endpoint es una versión simplificada para demostración.
     * 
     * 📊 RESPUESTA:
     * HTTP 200 OK
     * Content-Type: application/json
     * 
     * Body:
     * {
     *   "status": "UP",
     *   "servicio": "TasaCorp API"
     * }
     * 
     * 📝 EJEMPLO DE USO:
     * 
     * curl http://localhost:8080/api/tasas/health
     * 
     * En Kubernetes:
     * <pre>
     * livenessProbe:
     *   httpGet:
     *     path: /api/tasas/health
     *     port: 8080
     *   initialDelaySeconds: 10
     *   periodSeconds: 5
     * </pre>
     * 
     * 💡 NOTA:
     * Este es un health check básico que SIEMPRE devuelve UP.
     * En producción real, verificaríamos:
     * - Conexión a base de datos
     * - Conexión a Vault
     * - Conexión al proveedor de tasas
     * - Memoria/CPU disponible
     * 
     * @return Response HTTP 200 con status UP
     */
    @GET
    @Path("/health")
    public Response health() {
        // No es necesario loguear (se llama muy frecuentemente)
        // En producción, evita contaminar logs con health checks
        
        // Devolver respuesta simple indicando que el servicio está UP
        return Response.ok(Map.of(
            "status", "UP",
            "servicio", "TasaCorp API"
        )).build();
    }
}