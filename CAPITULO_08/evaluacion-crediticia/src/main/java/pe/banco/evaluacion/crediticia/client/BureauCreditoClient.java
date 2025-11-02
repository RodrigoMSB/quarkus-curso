package pe.banco.evaluacion.crediticia.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import pe.banco.evaluacion.crediticia.model.RespuestaBureau;

@Path("/api/bureau")
@RegisterRestClient(configKey = "bureau-credito")
public interface BureauCreditoClient {

    @GET
    @Path("/consulta/{dni}")
    RespuestaBureau consultarHistorial(
        @PathParam("dni") String dni,
        @HeaderParam("X-API-Key") String apiKey
    );
}
