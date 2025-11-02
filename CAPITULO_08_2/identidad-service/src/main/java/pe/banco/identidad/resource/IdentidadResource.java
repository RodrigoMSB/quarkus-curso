package pe.banco.identidad.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import pe.banco.identidad.model.RespuestaIdentidad;

@Path("/api/identidad")
@Produces(MediaType.APPLICATION_JSON)
public class IdentidadResource {

    @GET
    @Path("/validar")
    public RespuestaIdentidad validarIdentidad(
        @QueryParam("dni") String dni,
        @HeaderParam("Authorization") String token
    ) {
        System.out.println("🪪 Identidad Service: Validando DNI " + dni);
        
        // DNI que empieza con "000" = inválido
        boolean valido = !dni.startsWith("000");
        
        return new RespuestaIdentidad(
            dni,
            valido ? "Juan Perez Lopez" : "Persona Suspendida",
            valido,
            valido ? "ACTIVO" : "SUSPENDIDO"
        );
    }

    @GET
    @Path("/health")
    public String health() {
        return "Identidad Service OK - Puerto 8082";
    }
}
