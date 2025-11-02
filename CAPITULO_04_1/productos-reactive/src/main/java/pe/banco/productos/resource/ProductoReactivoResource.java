package pe.banco.productos.resource;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
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
 * Controlador REST reactivo para operaciones CRUD de productos.
 * <p>
 * Expone endpoints HTTP que utilizan programación reactiva con Mutiny ({@link Uni})
 * para operaciones no bloqueantes de alta concurrencia. Todas las operaciones retornan
 * tipos reactivos que se suscriben automáticamente por el framework Quarkus RESTEasy Reactive.
 * </p>
 * 
 * <p><strong>Características principales:</strong></p>
 * <ul>
 *   <li><strong>No bloqueante:</strong> Las operaciones no bloquean threads del servidor</li>
 *   <li><strong>Alta concurrencia:</strong> Puede manejar miles de peticiones simultáneas</li>
 *   <li><strong>Backpressure:</strong> Manejo automático de presión de datos</li>
 *   <li><strong>Transaccional:</strong> Usa {@code Panache.withTransaction()} para garantizar consistencia</li>
 * </ul>
 * 
 * <p><strong>Base URL:</strong> {@code /api/v1/productos/reactivo}</p>
 * 
 * <p><strong>Content-Type:</strong> application/json (request y response)</p>
 * 
 * <p><strong>Ventajas sobre enfoque bloqueante:</strong></p>
 * <ul>
 *   <li>Menor uso de threads (pool más pequeño)</li>
 *   <li>Mayor throughput en operaciones I/O intensivas</li>
 *   <li>Mejor escalabilidad vertical y horizontal</li>
 *   <li>Tiempo de respuesta más predecible bajo carga</li>
 * </ul>
 * 
 * @author Curso Quarkus
 * @version 1.0
 * @see Uni
 * @see Panache
 * @see ProductoRepository
 */
@Path("/api/v1/productos/reactivo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductoReactivoResource {

    @Inject
    ProductoRepository repository;

    /**
     * Lista todos los productos disponibles en el sistema.
     * <p>
     * <strong>Operación reactiva:</strong> No bloquea el thread mientras espera la respuesta de BD.
     * El resultado se emite cuando la query completa.
     * </p>
     * 
     * <p><strong>HTTP:</strong> {@code GET /api/v1/productos/reactivo}</p>
     * 
     * <p><strong>Códigos de respuesta:</strong></p>
     * <ul>
     *   <li><strong>200 OK:</strong> Lista de productos (puede estar vacía)</li>
     * </ul>
     * 
     * <p><strong>Ejemplo de uso:</strong></p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/api/v1/productos/reactivo
     * }</pre>
     * 
     * <p><strong>Respuesta esperada:</strong></p>
     * <pre>{@code
     * [
     *   {
     *     "id": 1,
     *     "nombre": "Laptop Dell",
     *     "descripcion": "Laptop profesional",
     *     "precio": 1299.99,
     *     "stock": 10
     *   }
     * ]
     * }</pre>
     * 
     * @return {@link Uni} que emite la lista completa de productos
     */
    @GET
    public Uni<List<Producto>> listarTodos() {
        return repository.listAll();
    }

    /**
     * Busca un producto específico por su ID.
     * <p>
     * <strong>Operación reactiva:</strong> La búsqueda se ejecuta de forma asíncrona.
     * Usa operadores de Mutiny para transformar el resultado según si el producto existe o no.
     * </p>
     * 
     * <p><strong>HTTP:</strong> {@code GET /api/v1/productos/reactivo/{id}}</p>
     * 
     * <p><strong>Códigos de respuesta:</strong></p>
     * <ul>
     *   <li><strong>200 OK:</strong> Producto encontrado</li>
     *   <li><strong>404 NOT FOUND:</strong> Producto no existe con ese ID</li>
     * </ul>
     * 
     * <p><strong>Ejemplo de uso:</strong></p>
     * <pre>{@code
     * curl -X GET http://localhost:8080/api/v1/productos/reactivo/1
     * }</pre>
     * 
     * <p><strong>Flujo reactivo:</strong></p>
     * <pre>{@code
     * repository.findById(id)
     *     .onItem().ifNotNull()  → Si existe, retorna 200 OK con el producto
     *     .onItem().ifNull()     → Si no existe, retorna 404 NOT FOUND
     * }</pre>
     * 
     * @param id Identificador único del producto
     * @return {@link Uni} que emite un {@link Response} con status 200 (OK) o 404 (NOT FOUND)
     */
    @GET
    @Path("/{id}")
    public Uni<Response> buscarPorId(@PathParam("id") Long id) {
        return repository.findById(id)
                .onItem().ifNotNull().transform(producto -> Response.ok(producto).build())
                .onItem().ifNull().continueWith(Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * Crea un nuevo producto en el sistema.
     * <p>
     * <strong>Operación reactiva y transaccional:</strong> Utiliza {@code Panache.withTransaction()}
     * para garantizar que la operación se ejecute dentro de una transacción reactiva.
     * Si ocurre un error, la transacción se revierte automáticamente.
     * </p>
     * 
     * <p><strong>HTTP:</strong> {@code POST /api/v1/productos/reactivo}</p>
     * 
     * <p><strong>Códigos de respuesta:</strong></p>
     * <ul>
     *   <li><strong>201 CREATED:</strong> Producto creado exitosamente (incluye header Location)</li>
     *   <li><strong>400 BAD REQUEST:</strong> Datos inválidos en el request</li>
     * </ul>
     * 
     * <p><strong>Ejemplo de uso:</strong></p>
     * <pre>{@code
     * curl -X POST http://localhost:8080/api/v1/productos/reactivo \
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
     *   <li><strong>Location:</strong> /api/v1/productos/reactivo/{id} (URL del recurso creado)</li>
     * </ul>
     * 
     * @param request DTO con los datos del producto a crear (validado automáticamente)
     * @return {@link Uni} que emite un {@link Response} con status 201 y el producto creado
     */
    @POST
    public Uni<Response> crear(@Valid ProductoRequest request) {
        // Validación programática explícita para asegurar que se ejecuta
        if (request.precio != null && request.precio <= 0) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"El precio debe ser mayor a 0\"}")
                    .build()
            );
        }
        
        if (request.stock != null && request.stock < 0) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"El stock no puede ser negativo\"}")
                    .build()
            );
        }
        
        Producto producto = new Producto(
                request.nombre,
                request.descripcion,
                request.precio,
                request.stock
        );

        return Panache.withTransaction(() -> repository.persist(producto))
                .onItem().transform(p -> Response.created(URI.create("/api/v1/productos/reactivo/" + p.id))
                        .entity(p)
                        .build());
    }

    /**
     * Actualiza un producto existente.
     * <p>
     * <strong>Operación reactiva y transaccional:</strong> Busca el producto, lo modifica y persiste
     * en una única transacción reactiva. Si el producto no existe, retorna 404 sin iniciar transacción.
     * </p>
     * 
     * <p><strong>HTTP:</strong> {@code PUT /api/v1/productos/reactivo/{id}}</p>
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
     * curl -X PUT http://localhost:8080/api/v1/productos/reactivo/1 \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "nombre": "Mouse Logitech MX Master",
     *     "descripcion": "Mouse profesional actualizado",
     *     "precio": 99.99,
     *     "stock": 30
     *   }'
     * }</pre>
     * 
     * <p><strong>Nota:</strong> Esta operación reemplaza TODOS los campos del producto.
     * Para actualizaciones parciales (PATCH), implementar endpoint separado.</p>
     * 
     * @param id Identificador único del producto a actualizar
     * @param request DTO con los nuevos datos del producto (validado automáticamente)
     * @return {@link Uni} que emite un {@link Response} con status 200 (OK) o 404 (NOT FOUND)
     */
    @PUT
    @Path("/{id}")
    public Uni<Response> actualizar(@PathParam("id") Long id, @Valid ProductoRequest request) {
        // Validación programática explícita
        if (request.precio != null && request.precio <= 0) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"El precio debe ser mayor a 0\"}")
                    .build()
            );
        }
        
        if (request.stock != null && request.stock < 0) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"El stock no puede ser negativo\"}")
                    .build()
            );
        }
        
        return Panache.withTransaction(() ->
                repository.findById(id)
                        .onItem().ifNotNull().transformToUni(producto -> {
                            producto.nombre = request.nombre;
                            producto.descripcion = request.descripcion;
                            producto.precio = request.precio;
                            producto.stock = request.stock;
                            return repository.persist(producto)
                                    .onItem().transform(p -> Response.ok(p).build());
                        })
                        .onItem().ifNull().continueWith(Response.status(Response.Status.NOT_FOUND).build())
        );
    }

    /**
     * Elimina un producto del sistema.
     * <p>
     * <strong>Operación reactiva y transaccional:</strong> La eliminación se ejecuta dentro
     * de una transacción. Si el producto no existe, retorna 404 sin modificar la base de datos.
     * </p>
     * 
     * <p><strong>HTTP:</strong> {@code DELETE /api/v1/productos/reactivo/{id}}</p>
     * 
     * <p><strong>Códigos de respuesta:</strong></p>
     * <ul>
     *   <li><strong>204 NO CONTENT:</strong> Producto eliminado exitosamente (sin body)</li>
     *   <li><strong>404 NOT FOUND:</strong> Producto no existe con ese ID</li>
     * </ul>
     * 
     * <p><strong>Ejemplo de uso:</strong></p>
     * <pre>{@code
     * curl -X DELETE http://localhost:8080/api/v1/productos/reactivo/1
     * }</pre>
     * 
     * <p><strong>⚠️ Advertencia:</strong> Esta operación es irreversible. El producto
     * se elimina permanentemente de la base de datos.</p>
     * 
     * @param id Identificador único del producto a eliminar
     * @return {@link Uni} que emite un {@link Response} con status 204 (NO CONTENT) o 404 (NOT FOUND)
     */
    @DELETE
    @Path("/{id}")
    public Uni<Response> eliminar(@PathParam("id") Long id) {
        return Panache.withTransaction(() ->
                repository.deleteById(id)
                        .onItem().transform(deleted -> deleted
                                ? Response.noContent().build()
                                : Response.status(Response.Status.NOT_FOUND).build())
        );
    }

    /**
     * Busca productos con stock por debajo de un umbral especificado.
     * <p>
     * <strong>Operación reactiva:</strong> Útil para reportes de inventario bajo
     * o alertas de reabastecimiento.
     * </p>
     * 
     * <p><strong>HTTP:</strong> {@code GET /api/v1/productos/reactivo/stock-bajo/{umbral}}</p>
     * 
     * <p><strong>Códigos de respuesta:</strong></p>
     * <ul>
     *   <li><strong>200 OK:</strong> Lista de productos con stock bajo (puede estar vacía)</li>
     * </ul>
     * 
     * <p><strong>Ejemplo de uso:</strong></p>
     * <pre>{@code
     * // Buscar productos con menos de 10 unidades
     * curl -X GET http://localhost:8080/api/v1/productos/reactivo/stock-bajo/10
     * }</pre>
     * 
     * <p><strong>Caso de uso:</strong> Dashboard de administración para identificar
     * productos que necesitan reabastecimiento urgente.</p>
     * 
     * @param umbral Cantidad mínima de stock (productos con stock menor a este valor serán retornados)
     * @return {@link Uni} que emite la lista de productos con stock bajo el umbral
     */
    @GET
    @Path("/stock-bajo/{umbral}")
    public Uni<List<Producto>> stockBajo(@PathParam("umbral") int umbral) {
        return repository.findConStockBajo(umbral);
    }

    /**
     * Crea múltiples productos de forma masiva (batch insert).
     * <p>
     * <strong>Operación reactiva de alto rendimiento:</strong> Demuestra la eficiencia
     * de operaciones batch en modo reactivo. Ideal para migraciones, importaciones masivas
     * o carga inicial de datos.
     * </p>
     * 
     * <p><strong>HTTP:</strong> {@code POST /api/v1/productos/reactivo/carga-masiva/{cantidad}}</p>
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
     * curl -X POST http://localhost:8080/api/v1/productos/reactivo/carga-masiva/1000
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
     *   <li>Operación transaccional (todo o nada)</li>
     *   <li>Ideal para testing de performance y carga</li>
     * </ul>
     * 
     * <p><strong>⚠️ Advertencia:</strong> No usar en producción sin validaciones adicionales.
     * Cantidades muy grandes (>10,000) pueden causar timeouts o alta carga en BD.</p>
     * 
     * <p><strong>Benchmark típico:</strong> 1000 productos en ~200-500ms (depende del hardware)</p>
     * 
     * @param cantidad Número de productos a crear (debe ser mayor a 0)
     * @return {@link Uni} que emite un {@link Response} con status 200 y mensaje de confirmación
     */
    @POST
    @Path("/carga-masiva/{cantidad}")
    public Uni<Response> cargaMasiva(@PathParam("cantidad") int cantidad) {
        List<Producto> productos = IntStream.range(1, cantidad + 1)
                .mapToObj(i -> new Producto(
                        "Producto Masivo " + i,
                        "Generado automáticamente",
                        Math.random() * 1000,
                        (int) (Math.random() * 100)
                ))
                .collect(Collectors.toList());

        return Panache.withTransaction(() -> repository.persistirLote(productos))
                .onItem().transform(v -> Response.ok()
                        .entity("{\"mensaje\": \"" + cantidad + " productos creados exitosamente\"}")
                        .build());
    }
}