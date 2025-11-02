package pe.banco.clientes.resource;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pe.banco.clientes.dto.ClienteRequest;
import pe.banco.clientes.entity.Cliente;
import pe.banco.clientes.repository.ClienteRepository;
import java.net.URI;
import java.util.List;

/**
 * Controlador REST para operaciones CRUD de clientes con datos sensibles.
 * <p>
 * Expone endpoints HTTP para gestionar clientes bancarios que contienen
 * información sensible (números de tarjeta, emails) que debe ser cifrada
 * antes de persistir en la base de datos.
 * </p>
 * 
 * <p><strong>⚠️ ADVERTENCIA CRÍTICA DE SEGURIDAD:</strong></p>
 * <p style="color: red; font-weight: bold;">
 * Esta implementación NO incluye el cifrado de datos sensibles. Es una versión
 * simplificada para demostración. En PRODUCCIÓN, esta clase DEBE:
 * </p>
 * <ul>
 *   <li>Inyectar {@code CryptoService} para cifrar/descifrar datos</li>
 *   <li>Cifrar {@code numeroTarjeta} y {@code email} antes de persistir</li>
 *   <li>Descifrar estos campos antes de retornar al cliente</li>
 *   <li>Nunca exponer datos sensibles sin cifrar en respuestas</li>
 * </ul>
 * 
 * <p><strong>Implementación correcta con CryptoService:</strong></p>
 * <pre>{@code
 * @Inject
 * CryptoService cryptoService;
 * 
 * @POST
 * @Transactional
 * public Response crear(ClienteRequest request) throws Exception {
 *     Cliente cliente = new Cliente(
 *         request.nombre,
 *         cryptoService.cifrar(request.numeroTarjeta),  // CIFRAR
 *         cryptoService.cifrar(request.email),           // CIFRAR
 *         request.telefono
 *     );
 *     repository.persist(cliente);
 *     // Descifrar antes de retornar...
 * }
 * }</pre>
 * 
 * <p><strong>Base URL:</strong> {@code /api/v1/clientes}</p>
 * 
 * <p><strong>Content-Type:</strong> application/json (request y response)</p>
 * 
 * <p><strong>Compliance:</strong></p>
 * <ul>
 *   <li><strong>PCI-DSS Req 3.4:</strong> Números de tarjeta deben estar cifrados</li>
 *   <li><strong>GDPR Art. 32:</strong> Datos personales requieren medidas técnicas apropiadas</li>
 *   <li><strong>⚠️ Esta implementación NO cumple con estos requisitos sin CryptoService</strong></li>
 * </ul>
 * 
 * @author Curso Quarkus - Capítulo 4.2
 * @version 1.0 (Demo - NO lista para producción)
 * @see Cliente
 * @see ClienteRepository
 * @see ClienteRequest
 */
@Path("/api/v1/clientes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClienteResource {

    @Inject
    ClienteRepository repository;

    /**
     * Lista todos los clientes del sistema.
     * <p>
     * <strong>HTTP:</strong> {@code GET /api/v1/clientes}
     * </p>
     * 
     * <p><strong>⚠️ PROBLEMA DE SEGURIDAD:</strong></p>
     * <p>
     * Este método retorna los clientes directamente desde la base de datos,
     * exponiendo los campos {@code numeroTarjeta} y {@code email} CIFRADOS
     * (texto ilegible en Base64). El cliente no puede usar estos datos.
     * </p>
     * 
     * <p><strong>Implementación correcta:</strong></p>
     * <pre>{@code
     * @GET
     * public Response listarTodos() throws Exception {
     *     List<Cliente> clientes = repository.listAll();
     *     
     *     // Descifrar campos sensibles antes de retornar
     *     List<ClienteDTO> clientesDTO = clientes.stream()
     *         .map(c -> new ClienteDTO(
     *             c.id,
     *             c.nombre,
     *             cryptoService.descifrar(c.numeroTarjeta),  // Descifrar
     *             cryptoService.descifrar(c.email),          // Descifrar
     *             c.telefono
     *         ))
     *         .collect(Collectors.toList());
     *     
     *     return Response.ok(clientesDTO).build();
     * }
     * }</pre>
     * 
     * <p><strong>Códigos de respuesta:</strong></p>
     * <ul>
     *   <li><strong>200 OK:</strong> Lista de clientes (puede estar vacía)</li>
     * </ul>
     * 
     * <p><strong>Ejemplo de uso:</strong></p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/api/v1/clientes
     * }</pre>
     * 
     * <p><strong>Respuesta actual (INCORRECTA):</strong></p>
     * <pre>{@code
     * [
     *   {
     *     "id": 1,
     *     "nombre": "Juan Pérez",
     *     "numeroTarjeta": "AebqJ3oc/tkB8ryE...",  // ← Cifrado ilegible
     *     "email": "Xm8kL!pQ3@zR7vN...",           // ← Cifrado ilegible
     *     "telefono": "+56912345678"
     *   }
     * ]
     * }</pre>
     * 
     * <p><strong>Respuesta esperada (CORRECTA con descifrado):</strong></p>
     * <pre>{@code
     * [
     *   {
     *     "id": 1,
     *     "nombre": "Juan Pérez",
     *     "numeroTarjeta": "****-****-****-9012",  // ← Enmascarado
     *     "email": "juan.perez@banco.com",         // ← Descifrado
     *     "telefono": "+56912345678"
     *   }
     * ]
     * }</pre>
     * 
     * <p><strong>Nota de seguridad:</strong> El número de tarjeta debería mostrarse
     * enmascarado (solo últimos 4 dígitos) incluso después de descifrar, según PCI-DSS Req 3.3.</p>
     * 
     * @return Lista completa de clientes con datos cifrados (INCORRECTO en producción)
     */
    @GET
    public List<Cliente> listarTodos() {
        return repository.listAll();
    }

    /**
     * Busca un cliente específico por su ID.
     * <p>
     * <strong>HTTP:</strong> {@code GET /api/v1/clientes/{id}}
     * </p>
     * 
     * <p><strong>⚠️ PROBLEMA DE SEGURIDAD:</strong></p>
     * <p>
     * Este método retorna el cliente directamente desde la BD con campos cifrados
     * sin descifrar, haciendo los datos inutilizables para el consumidor de la API.
     * </p>
     * 
     * <p><strong>Códigos de respuesta:</strong></p>
     * <ul>
     *   <li><strong>200 OK:</strong> Cliente encontrado (con datos cifrados)</li>
     *   <li><strong>404 NOT FOUND:</strong> Cliente no existe con ese ID</li>
     * </ul>
     * 
     * <p><strong>Ejemplo de uso:</strong></p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/api/v1/clientes/1
     * }</pre>
     * 
     * <p><strong>Implementación correcta:</strong> Ver documentación de {@link #listarTodos()}
     * para ejemplo de cómo descifrar antes de retornar.</p>
     * 
     * @param id Identificador único del cliente
     * @return {@link Response} con status 200 (OK) o 404 (NOT FOUND)
     */
    @GET
    @Path("/{id}")
    public Response buscarPorId(@PathParam("id") Long id) {
        Cliente cliente = repository.findById(id);
        if (cliente == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(cliente).build();
    }

    /**
     * Crea un nuevo cliente en el sistema.
     * <p>
     * <strong>HTTP:</strong> {@code POST /api/v1/clientes}
     * </p>
     * 
     * <p><strong>🔴 ERROR CRÍTICO DE SEGURIDAD:</strong></p>
     * <p style="color: red; font-weight: bold;">
     * Este método NO cifra los datos sensibles antes de guardarlos en la base de datos.
     * Los campos {@code numeroTarjeta} y {@code email} se guardan EN TEXTO PLANO,
     * violando PCI-DSS y GDPR.
     * </p>
     * 
     * <p><strong>Implementación correcta:</strong></p>
     * <pre>{@code
     * @Inject
     * CryptoService cryptoService;
     * 
     * @POST
     * @Transactional
     * public Response crear(ClienteRequest request) throws Exception {
     *     // 1. CIFRAR datos sensibles antes de crear la entidad
     *     String numeroTarjetaCifrado = cryptoService.cifrar(request.numeroTarjeta);
     *     String emailCifrado = cryptoService.cifrar(request.email);
     *     
     *     // 2. Crear cliente con datos cifrados
     *     Cliente cliente = new Cliente(
     *         request.nombre,
     *         numeroTarjetaCifrado,  // ← YA cifrado
     *         emailCifrado,          // ← YA cifrado
     *         request.telefono
     *     );
     *     
     *     // 3. Persistir
     *     repository.persist(cliente);
     *     
     *     // 4. Retornar respuesta con datos enmascarados/descifrados según corresponda
     *     ClienteDTO dto = new ClienteDTO(
     *         cliente.id,
     *         cliente.nombre,
     *         enmascarar(request.numeroTarjeta),  // ****-****-****-9012
     *         request.email,
     *         cliente.telefono
     *     );
     *     
     *     return Response.created(URI.create("/api/v1/clientes/" + cliente.id))
     *         .entity(dto)
     *         .build();
     * }
     * }</pre>
     * 
     * <p><strong>Códigos de respuesta:</strong></p>
     * <ul>
     *   <li><strong>201 CREATED:</strong> Cliente creado exitosamente</li>
     *   <li><strong>400 BAD REQUEST:</strong> Datos inválidos (si hay validaciones)</li>
     * </ul>
     * 
     * <p><strong>Ejemplo de uso:</strong></p>
     * <pre>{@code
     * curl -X POST http://localhost:8080/api/v1/clientes \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "nombre": "María González",
     *     "numeroTarjeta": "5412-9876-5432-1098",
     *     "email": "maria.gonzalez@banco.com",
     *     "telefono": "+56987654321"
     *   }'
     * }</pre>
     * 
     * <p><strong>⚠️ Consecuencias de no cifrar:</strong></p>
     * <ul>
     *   <li>Violación de PCI-DSS → Multas de $5,000 a $100,000/mes</li>
     *   <li>Violación de GDPR → Multas de hasta €20M o 4% del revenue anual</li>
     *   <li>Responsabilidad legal en caso de brecha de datos</li>
     *   <li>Daño reputacional irreparable</li>
     * </ul>
     * 
     * @param request DTO con los datos del cliente en texto plano
     * @return {@link Response} con status 201 y el cliente creado
     */
    @POST
    @Transactional
    public Response crear(ClienteRequest request) {
        Cliente cliente = new Cliente(
            request.nombre,
            request.numeroTarjeta,  // 🔴 NO CIFRADO - GRAVE ERROR
            request.email,          // 🔴 NO CIFRADO - GRAVE ERROR
            request.telefono
        );
        repository.persist(cliente);
        return Response.created(URI.create("/api/v1/clientes/" + cliente.id))
                .entity(cliente)
                .build();
    }

    /**
     * Busca clientes por número de tarjeta.
     * <p>
     * <strong>HTTP:</strong> {@code GET /api/v1/clientes/tarjeta/{numero}}
     * </p>
     * 
     * <p><strong>⚠️ PROBLEMA DE SEGURIDAD:</strong></p>
     * <p>
     * Este método tiene múltiples problemas de seguridad:
     * </p>
     * <ol>
     *   <li>Recibe el número de tarjeta en la URL (¡visible en logs, proxies, historial!)</li>
     *   <li>No cifra el número antes de buscar en BD</li>
     *   <li>Retorna clientes con datos cifrados sin descifrar</li>
     *   <li>Expone endpoint inseguro que podría usarse para enumerar tarjetas</li>
     * </ol>
     * 
     * <p><strong>🚨 CRÍTICO - Nunca poner datos sensibles en URLs:</strong></p>
     * <pre>{@code
     * ❌ MAL:  GET /api/v1/clientes/tarjeta/4532-1234-5678-9012
     *          ↑ Número de tarjeta visible en:
     *            - Logs del servidor
     *            - Logs de proxies/load balancers
     *            - Historial del navegador
     *            - Caché de CDN
     * 
     * ✅ BIEN: POST /api/v1/clientes/buscar-por-tarjeta
     *          Body: { "numeroTarjeta": "4532-1234-5678-9012" }
     *          ↑ Datos en body sobre HTTPS, no quedan en logs
     * }</pre>
     * 
     * <p><strong>Implementación correcta (cambiar a POST):</strong></p>
     * <pre>{@code
     * @POST
     * @Path("/buscar-por-tarjeta")
     * public Response buscarPorTarjeta(BuscarTarjetaRequest request) throws Exception {
     *     // 1. Validar autenticación y autorización
     *     if (!tienePermisoParaBuscarTarjetas(securityContext)) {
     *         return Response.status(Response.Status.FORBIDDEN).build();
     *     }
     *     
     *     // 2. Cifrar el número recibido antes de buscar
     *     String numeroTarjetaCifrado = cryptoService.cifrar(request.numeroTarjeta);
     *     
     *     // 3. Buscar con valor cifrado
     *     List<Cliente> clientes = repository.buscarPorTarjeta(numeroTarjetaCifrado);
     *     
     *     // 4. Descifrar y enmascarar antes de retornar
     *     // ...
     *     
     *     // 5. Auditar el acceso (quién, cuándo, qué tarjeta buscó)
     *     auditService.log("BUSCAR_TARJETA", usuario, enmascarar(request.numeroTarjeta));
     *     
     *     return Response.ok(clientesDTO).build();
     * }
     * }</pre>
     * 
     * <p><strong>Códigos de respuesta:</strong></p>
     * <ul>
     *   <li><strong>200 OK:</strong> Lista de clientes (normalmente 0 o 1)</li>
     * </ul>
     * 
     * <p><strong>Ejemplo de uso ACTUAL (INSEGURO):</strong></p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/api/v1/clientes/tarjeta/4532-1234-5678-9012
     *                                                          ↑
     *                              Número de tarjeta EXPUESTO en URL
     * }</pre>
     * 
     * <p><strong>Mejores prácticas de seguridad:</strong></p>
     * <ul>
     *   <li>Usar POST con body en lugar de GET con path param</li>
     *   <li>Implementar rate limiting (máximo 5 búsquedas/minuto)</li>
     *   <li>Requerir autenticación y autorización específica</li>
     *   <li>Auditar TODAS las búsquedas por tarjeta</li>
     *   <li>Retornar solo últimos 4 dígitos enmascarados</li>
     *   <li>Implementar alertas por búsquedas sospechosas</li>
     * </ul>
     * 
     * @param numero Número de tarjeta EN TEXTO PLANO (🔴 INSEGURO - visible en URL)
     * @return Lista de clientes con datos cifrados sin descifrar (🔴 INÚTIL)
     */
    @GET
    @Path("/tarjeta/{numero}")
    public List<Cliente> buscarPorTarjeta(@PathParam("numero") String numero) {
        return repository.buscarPorTarjeta(numero);  // 🔴 No cifra antes de buscar
    }
}