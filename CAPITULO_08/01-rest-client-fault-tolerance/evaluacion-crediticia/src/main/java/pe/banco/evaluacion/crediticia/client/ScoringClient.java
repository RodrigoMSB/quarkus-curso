package pe.banco.evaluacion.crediticia.client;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import pe.banco.evaluacion.crediticia.model.RespuestaScoring;

@Path("/api/scoring")
@RegisterRestClient(configKey = "scoring-avanzado")
public interface ScoringClient {

    @POST
    @Path("/calcular")
    RespuestaScoring calcularScore(
        @QueryParam("dni") String dni,
        @QueryParam("monto") Double monto,
        @QueryParam("plazo") Integer plazo
    );
}
