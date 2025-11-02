package pe.banco.evaluacion.crediticia.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pe.banco.evaluacion.crediticia.model.RespuestaScoring;

@Path("/api/scoring")
@Produces(MediaType.APPLICATION_JSON)
public class ScoringMockResource {

    @POST
    @Path("/calcular")
    public Response calcularScore(
        @QueryParam("dni") String dni,
        @QueryParam("monto") Double monto,
        @QueryParam("plazo") Integer plazo
    ) {
        // DNI 333XXXXX: Simula DEMORA (5 segundos - excede timeout de 3s)
        if (dni.startsWith("333")) {
            System.out.println("⏱️  Scoring: DNI " + dni + " - DEMORANDO 5 segundos (timeout en 3s)");
            try {
                Thread.sleep(5000); // 5 segundos
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // DNI 444XXXXX: Simula FALLO permanente (para Circuit Breaker)
        if (dni.startsWith("444")) {
            System.out.println("🔴 Scoring: DNI " + dni + " - FALLO permanente (activará fallback)");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Motor de scoring no disponible")
                .build();
        }
        
        // Lógica normal: basado en el monto
        boolean aprobado = monto <= 50000;
        
        System.out.println("🟢 Scoring: DNI " + dni + " - Calculado exitosamente");
        
        RespuestaScoring respuesta = new RespuestaScoring(
            dni,
            aprobado ? 800 : 400,
            aprobado ? 0.15 : 0.75,
            aprobado ? "APROBAR" : "RECHAZAR"
        );
        
        return Response.ok(respuesta).build();
    }
}
