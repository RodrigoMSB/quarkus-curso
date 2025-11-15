package pe.banco.productos.resource;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pe.banco.productos.dto.ProductoRequest;
import pe.banco.productos.entity.Producto;
import pe.banco.productos.repository.ProductoRepository;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Controlador REST clásico (bloqueante) para operaciones CRUD de productos.
 * <p>
 * Expone endpoints HTTP que utilizan programación tradicional bloqueante.
 * Cada operación bloquea un thread del servidor hasta que la operación de BD completa.
 * Este es el enfoque estándar de Java EE y Spring MVC tradicional.
 * </p>
 * 
 * <p><strong>Características principales:</strong></p>
 * <ul>
 *   <li><strong>Bloqueante:</strong> Las operaciones bloquean threads mientras esperan I/O</li>
 *   <li><strong>Modelo 1:1:</strong> Un thread por request (thread-per-request model)</li>
 *   <li><strong>Fácil debugging:</strong> Stack traces lineales y predecibles</li>
 *   <li><strong>Transaccional:</strong> Usa {@code @Transactional} de JTA</li>
 * </ul>
 * 
 * <p><strong>Base URL:</strong> {@code /api/v1/productos/clasico}</p>
 * 
 * <p><strong>Content-Type:</strong> application/json (request y response)</p>
 * 
 * <p><strong>Características del enfoque bloqueante:</strong></p>
 * <ul>
 *   <li>Pool de threads más grande necesario para alta concurrencia</li>
 *   <li>Menor throughput en operaciones I/O intensivas</li>
 *   <li>Código más simple y directo (sin callbacks ni operadores reactivos)</li>
 *   <li>Ideal para CRUD tradicional con concurrencia baja/media</li>
 * </ul>
 * 
 * <p><strong>Comparación con enfoque reactivo:</strong></p>
 * <ul>
 *   <li>✅ Código más fácil de entender y mantener</li>
 *   <li>✅ Debugging más simple (stack traces claros)</li>
 *   <li>✅ Menos curva de aprendizaje para el equipo</li>
 *   <li>❌ Menor throughput bajo alta concurrencia</li>
 *   <li>❌ Mayor uso de threads (más memoria)</li>
 *   <li>❌ Latencias menos predecibles bajo carga</li>
 * </ul>
 * 
 * @author Curso Quarkus
 * @version 1.0
 * @see ProductoRepository
 * @see Transactional
 */
@Path("/api/v1/productos/clasico")
@Produces(MediaType.APPLICATION_JSON)
public class ProductoClasicoResource {

    @Inject
    ProductoRepository repository;

    /**
     * Lista todos los productos disponibles en el sistema.
     * <p>
     * <strong>Operación bloqueante:</strong> El thread se bloquea mientras espera
     * la respuesta de la base de datos. Durante este tiempo, el thread no puede
     * atender otras peticiones.
     * </p>
     * 
     * <p><strong>HTTP:</strong> {@code GET /api/v1/productos/clasico}</p>
     * 
     * <p><strong>Códigos de respuesta:</strong></p>
     * <ul>
     *   <li><strong>200 OK:</strong> Lista de productos (puede estar vacía)</li>
     * </ul>
     * 
     * <p><strong>Ejemplo de uso:</strong></p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/api/v1/productos/clasico
     * }</pre>
     * 
     * <p><strong>Flujo de ejecución:</strong></p>
     * <pre>{@code
     * 1. Thread recibe request HTTP
     * 2. Thread ejecuta query en BD (BLOQUEADO esperando)
     * 3. BD retorna resultados
     * 4. Thread serializa a JSON
     * 5. Thread envía response
     * 
     * Durante los pasos 2-3, el thread NO puede hacer nada más.
     * }</pre>
     * 
     * @return Lista de todos los productos
     */
    @GET
    public List<Producto> listarTodos() {
        return repository.listAll();
    }

    /**
     * Busca un producto específico por su ID.
     * <p>
     * <strong>Operación bloqueante:</strong> El thread espera sincrónicamente
     * la respuesta de la BD. Usa condicionales tradicionales para manejar
     * el caso de producto no encontrado.
     * </p>
     * 
     * <p><strong>HTTP:</strong> {@code GET /api/v1/productos/clasico/{id}}</p>
     * 
     * <p><strong>Códigos de respuesta:</strong></p>
     * <ul>
     *   <li><strong>200 OK:</strong> Producto encontrado</li>
     *   <li><strong>404 NOT FOUND:</strong> Producto no existe con ese ID</li>
     * </ul>
     * 
     * <p><strong>Ejemplo de uso:</strong></p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/api/v1/productos/clasico/1
     * }</pre>
     * 
     * <p><strong>Flujo tradicional:</strong></p>
     * <pre>{@code
     * Producto p = repository.findById(id);  // BLOQUEA aquí
     * if (p != null) {
     *     return 200 OK
     * } else {
     *     return 404 NOT FOUND
     * }
     * }</pre>
     * 
     * @param id Identificador único del producto
     * @return {@link Response} con status 200 (OK) o 404 (NOT FOUND)
     */
    @GET
    @Path("/{id}")
    public Response buscarPorId(@PathParam("id") Long id) {
        Producto producto = repository.findById(id);
        if (producto != null) {
            return Response.ok(producto).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    /**
     * Crea un nuevo producto en el sistema.
     * <p>
     * <strong>Operación bloqueante y transaccional:</strong> Utiliza {@code @Transactional}
     * tradicional de JTA. El thread se bloquea durante toda la transacción.
     * Si ocurre una excepción, la transacción se revierte automáticamente.
     * </p>
     * 
     * <p><strong>HTTP:</strong> {@code POST /api/v1/productos/clasico}</p>
     * 
     * <p><strong>Códigos de respuesta:</strong></p>
     * <ul>
     *   <li><strong>201 CREATED:</strong> Producto creado exitosamente (incluye header Location)</li>
     *   <li><strong>400 BAD REQUEST:</strong> Datos inválidos en el request</li>
     * </ul>
     * 
     * <p><strong>Ejemplo de uso:</strong></p>
     * <pre>{@code
     * curl -X POST http://localhost:8080/api/v1/productos/clasico \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "nombre": "Mouse Logitech",
     *     "descripcion": "Mouse inalámbrico",
     *     "precio": 29.99,
     *     "stock": 50
     *   }'
     * }</pre>
     * 
     * <p><strong>Headers de respuesta:</strong></p>
     * <ul>
     *   <li><strong>Location:</strong> /api/v1/productos/clasico/{id} (URL del recurso creado)</li>
     * </ul>
     * 
     * <p><strong>Manejo de transacciones:</strong></p>
     * <pre>{@code
     * @Transactional marca el método como transaccional.
     * Si hay excepción → rollback automático
     * Si completa OK → commit automático
     * 
     * Durante toda la transacción, el thread está BLOQUEADO.
     * }</pre>
     * 
     * @param request DTO con los datos del producto a crear (validado automáticamente con @Valid)
     * @return {@link Response} con status 201 y el producto creado
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response crear(@Valid ProductoRequest request) {
        if (request.precio != null && request.precio <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"El precio debe ser mayor a 0\"}")
                    .build();
        }
        
        if (request.stock != null && request.stock < 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"El stock no puede ser negativo\"}")
                    .build();
        }
        
        Producto producto = new Producto(
                request.nombre,
                request.descripcion,
                request.precio,
                request.stock
        );

        repository.persist(producto);
        
        return Response.created(URI.create("/api/v1/productos/clasico/" + producto.id))
                .entity(producto)
                .build();
    }

    /**
     * Actualiza un producto existente.
     * <p>
     * <strong>Operación bloqueante y transaccional:</strong> Busca el producto,
     * lo modifica y persiste en una única transacción. Todo el flujo bloquea
     * el thread hasta completar.
     * </p>
     * 
     * <p><strong>HTTP:</strong> {@code PUT /api/v1/productos/clasico/{id}}</p>
     * 
     * <p><strong>Códigos de respuesta:</strong></p>
     * <ul>
     *   <li><strong>200 OK:</strong> Producto actualizado exitosamente</li>
     *   <li><strong>404 NOT FOUND:</strong> Producto no existe con ese ID</li>
     *   <li><strong>400 BAD REQUEST:</strong> Datos inválidos en el request</li>
     * </ul>
     * 
     * <p><strong>Ejemplo de uso:</strong></p>
     * <pre>{@code
     * curl -X PUT http://localhost:8080/api/v1/productos/clasico/1 \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "nombre": "Mouse Logitech MX Master",
     *     "descripcion": "Mouse profesional actualizado",
     *     "precio": 99.99,
     *     "stock": 30
     *   }'
     * }</pre>
     * 
     * <p><strong>Flujo secuencial bloqueante:</strong></p>
     * <pre>{@code
     * 1. Buscar producto por ID (BLOQUEA)
     * 2. Si no existe → return 404
     * 3. Modificar campos
     * 4. Persistir cambios (BLOQUEA)
     * 5. Return 200 OK
     * 
     * Cada paso espera que el anterior complete.
     * }</pre>
     * 
     * <p><strong>Nota:</strong> Esta operación reemplaza TODOS los campos del producto.
     * Para actualizaciones parciales (PATCH), implementar endpoint separado.</p>
     * 
     * @param id Identificador único del producto a actualizar
     * @param request DTO con los nuevos datos del producto (validado automáticamente)
     * @return {@link Response} con status 200 (OK) o 404 (NOT FOUND)
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response actualizar(@PathParam("id") Long id, @Valid ProductoRequest request) {
        if (request.precio != null && request.precio <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"El precio debe ser mayor a 0\"}")
                    .build();
        }
        
        if (request.stock != null && request.stock < 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"El stock no puede ser negativo\"}")
                    .build();
        }
        
        Producto producto = repository.findById(id);
        
        if (producto == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        
        producto.nombre = request.nombre;
        producto.descripcion = request.descripcion;
        producto.precio = request.precio;
        producto.stock = request.stock;
        
        repository.persist(producto);
        
        return Response.ok(producto).build();
    }

    /**
     * Elimina un producto del sistema.
     * <p>
     * <strong>Operación bloqueante y transaccional:</strong> La eliminación se ejecuta
     * de forma síncrona. El thread espera hasta que la BD confirma la eliminación.
     * </p>
     * 
     * <p><strong>HTTP:</strong> {@code DELETE /api/v1/productos/clasico/{id}}</p>
     * 
     * <p><strong>Códigos de respuesta:</strong></p>
     * <ul>
     *   <li><strong>204 NO CONTENT:</strong> Producto eliminado exitosamente (sin body)</li>
     *   <li><strong>404 NOT FOUND:</strong> Producto no existe con ese ID</li>
     * </ul>
     * 
     * <p><strong>Ejemplo de uso:</strong></p>
     * <pre>{@code
     * curl -X DELETE http://localhost:8080/api/v1/productos/clasico/1
     * }</pre>
     * 
     * <p><strong>⚠️ Advertencia:</strong> Esta operación es irreversible. El producto
     * se elimina permanentemente de la base de datos.</p>
     * 
     * @param id Identificador único del producto a eliminar
     * @return {@link Response} con status 204 (NO CONTENT) o 404 (NOT FOUND)
     */
    @DELETE
    @Path("/{id}")
    @Transactional
    public Response eliminar(@PathParam("id") Long id) {
        boolean deleted = repository.deleteById(id);
        
        if (deleted) {
            return Response.noContent().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    /**
     * Busca productos con stock por debajo de un umbral especificado.
     * <p>
     * <strong>Operación bloqueante:</strong> Ejecuta una query filtrando por stock.
     * El thread espera sincrónicamente los resultados de la BD.
     * </p>
     * 
     * <p><strong>HTTP:</strong> {@code GET /api/v1/productos/clasico/stock-bajo/{umbral}}</p>
     * 
     * <p><strong>Códigos de respuesta:</strong></p>
     * <ul>
     *   <li><strong>200 OK:</strong> Lista de productos con stock bajo (puede estar vacía)</li>
     * </ul>
     * 
     * <p><strong>Ejemplo de uso:</strong></p>
     * <pre>{@code
     * // Buscar productos con menos de 10 unidades
     * curl -X GET http://localhost:8080/api/v1/productos/clasico/stock-bajo/10
     * }</pre>
     * 
     * <p><strong>Caso de uso:</strong> Dashboard de administración para identificar
     * productos que necesitan reabastecimiento urgente.</p>
     * 
     * @param umbral Cantidad mínima de stock (productos con stock menor a este valor serán retornados)
     * @return Lista de productos con stock bajo el umbral
     */
    @GET
    @Path("/stock-bajo/{umbral}")
    public List<Producto> stockBajo(@PathParam("umbral") int umbral) {
        return repository.findConStockBajo(umbral);
    }

    /**
     * Crea múltiples productos de forma masiva (batch insert).
     * <p>
     * <strong>Operación bloqueante de lote:</strong> Genera y persiste múltiples productos
     * en una única transacción. El thread permanece bloqueado durante toda la operación batch.
     * </p>
     * 
     * <p><strong>HTTP:</strong> {@code POST /api/v1/productos/clasico/carga-masiva/{cantidad}}</p>
     * 
     * <p><strong>Códigos de respuesta:</strong></p>
     * <ul>
     *   <li><strong>200 OK:</strong> Productos creados exitosamente</li>
     *   <li><strong>400 BAD REQUEST:</strong> Cantidad inválida (menor a 1)</li>
     * </ul>
     * 
     * <p><strong>Ejemplo de uso:</strong></p>
     * <pre>{@code
     * // Crear 1000 productos de prueba
     * curl -X POST http://localhost:8080/api/v1/productos/clasico/carga-masiva/1000
     * }</pre>
     * 
     * <p><strong>Respuesta esperada:</strong></p>
     * <pre>{@code
     * {
     *   "mensaje": "1000 productos creados exitosamente"
     * }
     * }</pre>
     * 
     * <p><strong>Características técnicas:</strong></p>
     * <ul>
     *   <li>Genera productos con datos aleatorios (nombre, precio, stock)</li>
     *   <li>Usa batch insert para optimizar I/O de base de datos</li>
     *   <li>Operación transaccional bloqueante (thread ocupado todo el tiempo)</li>
     *   <li>Ideal para testing de performance y comparación con enfoque reactivo</li>
     * </ul>
     * 
     * <p><strong>Comparación con enfoque reactivo:</strong></p>
     * <ul>
     *   <li>Enfoque clásico: Thread bloqueado durante toda la inserción batch</li>
     *   <li>Enfoque reactivo: Thread liberado, puede atender otros requests mientras BD procesa</li>
     *   <li>Bajo alta concurrencia, el enfoque reactivo muestra ventajas significativas</li>
     * </ul>
     * 
     * <p><strong>⚠️ Advertencia:</strong> No usar en producción sin validaciones adicionales.
     * Cantidades muy grandes (>10,000) pueden causar timeouts o alta carga en BD.</p>
     * 
     * <p><strong>Benchmark típico:</strong> 1000 productos en ~300-700ms (depende del hardware).
     * Compara con el enfoque reactivo para ver diferencias bajo carga.</p>
     * 
     * @param cantidad Número de productos a crear (debe ser mayor a 0)
     * @return {@link Response} con status 200 y mensaje de confirmación
     */
    @POST
    @Path("/carga-masiva/{cantidad}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public Response cargaMasiva(@PathParam("cantidad") int cantidad) {
        List<Producto> productos = IntStream.range(1, cantidad + 1)
                .mapToObj(i -> new Producto(
                        "Producto Masivo " + i,
                        "Generado automáticamente",
                        Math.random() * 1000,
                        (int) (Math.random() * 100)
                ))
                .collect(Collectors.toList());

        repository.persistirLote(productos);
        
        return Response.ok()
                .entity("{\"mensaje\": \"" + cantidad + " productos creados exitosamente\"}")
                .build();
    }
}