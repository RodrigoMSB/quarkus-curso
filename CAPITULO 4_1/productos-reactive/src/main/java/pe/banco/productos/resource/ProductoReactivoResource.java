package pe.banco.productos.resource;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
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

@Path("/api/v1/productos/reactivo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductoReactivoResource {

    @Inject
    ProductoRepository repository;

    /**
     * REACTIVO: Listar todos los productos
     */
    @GET
    public Uni<List<Producto>> listarTodos() {
        return repository.listAll();
    }

    /**
     * REACTIVO: Buscar por ID
     */
    @GET
    @Path("/{id}")
    public Uni<Response> buscarPorId(@PathParam("id") Long id) {
        return repository.findById(id)
                .onItem().ifNotNull().transform(producto -> Response.ok(producto).build())
                .onItem().ifNull().continueWith(Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * REACTIVO: Crear producto
     */
    @POST
    public Uni<Response> crear(ProductoRequest request) {
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
     * REACTIVO: Actualizar producto
     */
    @PUT
    @Path("/{id}")
    public Uni<Response> actualizar(@PathParam("id") Long id, ProductoRequest request) {
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
     * REACTIVO: Eliminar producto
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
     * REACTIVO: Buscar productos con stock bajo
     */
    @GET
    @Path("/stock-bajo/{umbral}")
    public Uni<List<Producto>> stockBajo(@PathParam("umbral") int umbral) {
        return repository.findConStockBajo(umbral);
    }

    /**
     * REACTIVO: Carga masiva - DEMUESTRA CONCURRENCIA
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
