package pe.banco.evaluacion.crediticia.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pe.banco.evaluacion.crediticia.model.RespuestaBureau;

import java.util.concurrent.atomic.AtomicInteger;

@Path("/api/bureau")
@Produces(MediaType.APPLICATION_JSON)
public class BureauMockResource {

    // Contador para simular fallos temporales
    private final AtomicInteger intentos = new AtomicInteger(0);

    @GET
    @Path("/consulta/{dni}")
    public Response consultarHistorial(
        @PathParam("dni") String dni,
        @HeaderParam("X-API-Key") String apiKey
    ) {
        // DNI 222XXXXX: Simula fallo temporal (falla 2 veces, luego responde OK)
        if (dni.startsWith("222")) {
            int intento = intentos.incrementAndGet();
            if (intento <= 2) {
                System.out.println("🔴 Bureau: Intento " + intento + " - FALLA (simulando error temporal)");
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("Bureau temporalmente no disponible")
                    .build();
            } else {
                System.out.println("🟢 Bureau: Intento " + intento + " - ÉXITO (después de reintentos)");
                intentos.set(0); // Reset para próxima prueba
            }
        }

        // Lógica normal: DNI terminado en par = buen score
        boolean buenScore = Integer.parseInt(dni.substring(dni.length()-1)) % 2 == 0;
        
        RespuestaBureau respuesta = new RespuestaBureau(
            dni,
            buenScore ? 750 : 450,
            buenScore ? 5000 : 25000,
            !buenScore,
            buenScore ? "NORMAL" : "DEFICIENTE"
        );
        
        return Response.ok(respuesta).build();
    }
}
