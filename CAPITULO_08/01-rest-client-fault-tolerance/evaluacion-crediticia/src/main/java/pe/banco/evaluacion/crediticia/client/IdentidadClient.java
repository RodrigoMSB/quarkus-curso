package pe.banco.evaluacion.crediticia.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import pe.banco.evaluacion.crediticia.model.RespuestaIdentidad;

@Path("/api/identidad")
@RegisterRestClient(configKey = "identidad")
public interface IdentidadClient {

    @GET
    @Path("/validar")
    RespuestaIdentidad validarIdentidad(
        @QueryParam("dni") String dni,
        @HeaderParam("Authorization") String token
    );
}
