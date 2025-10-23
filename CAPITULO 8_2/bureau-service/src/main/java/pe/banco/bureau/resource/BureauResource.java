package pe.banco.bureau.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import pe.banco.bureau.model.RespuestaBureau;

@Path("/api/bureau")
@Produces(MediaType.APPLICATION_JSON)
public class BureauResource {

    @GET
    @Path("/consulta/{dni}")
    public RespuestaBureau consultarHistorial(
        @PathParam("dni") String dni,
        @HeaderParam("X-API-Key") String apiKey
    ) {
        System.out.println("🏦 Bureau Service: Consultando DNI " + dni);
        
        // Lógica simple: DNI terminado en par = buen score
        boolean buenScore = Integer.parseInt(dni.substring(dni.length()-1)) % 2 == 0;
        
        return new RespuestaBureau(
            dni,
            buenScore ? 750 : 450,
            buenScore ? 5000 : 25000,
            !buenScore,
            buenScore ? "NORMAL" : "DEFICIENTE"
        );
    }

    @GET
    @Path("/health")
    public String health() {
        return "Bureau Service OK - Puerto 8081";
    }
}
