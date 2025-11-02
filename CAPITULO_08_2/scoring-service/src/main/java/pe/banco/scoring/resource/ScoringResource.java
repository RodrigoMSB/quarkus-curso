package pe.banco.scoring.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import pe.banco.scoring.model.RespuestaScoring;

@Path("/api/scoring")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ScoringResource {

    @POST
    @Path("/calcular")
    public RespuestaScoring calcularScore(
        @QueryParam("dni") String dni,
        @QueryParam("monto") Double monto,
        @QueryParam("plazo") Integer plazo
    ) {
        System.out.println("🧮 Scoring Service: Calculando para DNI " + dni + ", Monto: " + monto);
        
        // Lógica simple: monto <= 50000 = aprobado
        boolean aprobado = monto <= 50000;
        
        return new RespuestaScoring(
            dni,
            aprobado ? 800 : 400,
            aprobado ? 0.15 : 0.75,
            aprobado ? "APROBAR" : "RECHAZAR"
        );
    }

    @GET
    @Path("/health")
    public String health() {
        return "Scoring Service OK - Puerto 8083";
    }
}
