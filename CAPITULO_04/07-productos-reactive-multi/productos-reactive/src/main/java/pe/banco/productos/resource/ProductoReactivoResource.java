package pe.banco.productos.resource;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

import java.time.Duration;
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

@Path("/api/v1/productos/reactivo")
@Produces(MediaType.APPLICATION_JSON)
public class ProductoReactivoResource {

    @Inject
    ProductoRepository repository;

    @GET
    public Uni<List<Producto>> listarTodos() {
        return repository.listAll();
    }

    @GET
    @Path("/{id}")
    public Uni<Response> buscarPorId(@PathParam("id") Long id) {
        return repository.findById(id)
                .onItem().ifNotNull().transform(producto -> Response.ok(producto).build())
                .onItem().ifNull().continueWith(Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> crear(@Valid ProductoRequest request) {
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

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> actualizar(@PathParam("id") Long id, @Valid ProductoRequest request) {
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

    @GET
    @Path("/stock-bajo/{umbral}")
    public Uni<List<Producto>> stockBajo(@PathParam("umbral") int umbral) {
        return repository.findConStockBajo(umbral);
    }

    @POST
    @Path("/carga-masiva/{cantidad}")
    @Consumes(MediaType.APPLICATION_JSON)
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

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<Producto> streamProductos() {
        return Panache.withSession(() -> 
            repository.listAll()
        ).onItem().transformToMulti(productos ->
            Multi.createFrom().iterable(productos)
        ).onItem().call(producto ->
            Uni.createFrom().voidItem()
                .onItem().delayIt().by(Duration.ofMillis(1000))
        );
    }

    @GET
    @Path("/monitor-stock/{id}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<String> monitorearStock(@PathParam("id") Long id) {
        return repository.findById(id)
                .onItem().ifNull().failWith(() -> new NotFoundException("Producto no encontrado"))
                .onItem().transformToMulti(producto ->
                    Multi.createFrom().ticks().every(Duration.ofSeconds(1))
                        .onItem().transformToUniAndMerge(tick ->
                            repository.findById(id)
                                .onItem().transform(p -> {
                                    if (p == null) {
                                        return "{\"error\": \"Producto eliminado\"}";
                                    }
                                    return String.format(
                                        "{\"id\": %d, \"nombre\": \"%s\", \"stock\": %d, \"timestamp\": \"%s\"}",
                                        p.id, p.nombre, p.stock, java.time.LocalDateTime.now()
                                    );
                                })
                        )
                );
    }
}