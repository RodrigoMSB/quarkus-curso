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

@Path("/api/v1/clientes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClienteResource {

    @Inject
    ClienteRepository repository;

    @GET
    public List<Cliente> listarTodos() {
        return repository.listAll();
    }

    @GET
    @Path("/{id}")
    public Response buscarPorId(@PathParam("id") Long id) {
        Cliente cliente = repository.findById(id);
        if (cliente == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(cliente).build();
    }

    @POST
    @Transactional
    public Response crear(ClienteRequest request) {
        Cliente cliente = new Cliente(
            request.nombre,
            request.numeroTarjeta,
            request.email,
            request.telefono
        );
        repository.persist(cliente);
        return Response.created(URI.create("/api/v1/clientes/" + cliente.id))
                .entity(cliente)
                .build();
    }

    @GET
    @Path("/tarjeta/{numero}")
    public List<Cliente> buscarPorTarjeta(@PathParam("numero") String numero) {
        return repository.buscarPorTarjeta(numero);
    }
}
