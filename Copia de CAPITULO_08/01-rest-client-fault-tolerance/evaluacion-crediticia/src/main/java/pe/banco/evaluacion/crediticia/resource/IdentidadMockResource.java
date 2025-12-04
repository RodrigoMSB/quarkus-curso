package pe.banco.evaluacion.crediticia.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import pe.banco.evaluacion.crediticia.model.RespuestaIdentidad;

@Path("/api/identidad")
@Produces(MediaType.APPLICATION_JSON)
public class IdentidadMockResource {

    @GET
    @Path("/validar")
    public RespuestaIdentidad validarIdentidad(
        @QueryParam("dni") String dni,
        @HeaderParam("Authorization") String token
    ) {
        // DNI 000XXXXX: Identidad INVÁLIDA (para rechazo inmediato)
        if (dni.startsWith("000")) {
            System.out.println("🔴 Identidad: DNI " + dni + " - INVÁLIDO (suspendido)");
            return new RespuestaIdentidad(
                dni,
                "Persona Suspendida",
                false,
                "SUSPENDIDO"
            );
        }
        
        // Cualquier otro DNI: VÁLIDO
        System.out.println("🟢 Identidad: DNI " + dni + " - VÁLIDO");
        return new RespuestaIdentidad(
            dni,
            "Juan Perez Lopez",
            true,
            "ACTIVO"
        );
    }
}
