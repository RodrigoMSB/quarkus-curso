package pe.banco.evaluacion.resource;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pe.banco.evaluacion.model.ResultadoEvaluacion;
import pe.banco.evaluacion.model.SolicitudCredito;
import pe.banco.evaluacion.service.EvaluacionService;

@Path("/api/evaluacion")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EvaluacionResource {

    @Inject
    EvaluacionService evaluacionService;

    @POST
    @Path("/credito")
    public Response evaluarCredito(@Valid SolicitudCredito solicitud) {
        try {
            ResultadoEvaluacion resultado = evaluacionService.evaluarSolicitud(solicitud);
            return Response.ok(resultado).build();
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al evaluar la solicitud: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/health")
    public Response health() {
        return Response.ok("Evaluacion Service OK - Puerto 8080").build();
    }
}
