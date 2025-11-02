package pe.banco.evaluacion.recursos;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pe.banco.evaluacion.dtos.SolicitudCreditoDTO;
import pe.banco.evaluacion.entidades.SolicitudCredito;
import pe.banco.evaluacion.entidades.SolicitudCredito.EstadoSolicitud;
import pe.banco.evaluacion.repositorios.SolicitudCreditoRepository;
import pe.banco.evaluacion.servicios.ScoringService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/api/v1/creditos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CreditoRecurso {

    @Inject
    SolicitudCreditoRepository repository;

    @Inject
    ScoringService scoringService;

    @POST
    @Path("/evaluar")
    @Transactional
    public Response evaluar(@Valid SolicitudCreditoDTO dto) {
        SolicitudCredito solicitud = new SolicitudCredito();
        solicitud.setDni(dto.getDni());
        solicitud.setNombreCompleto(dto.getNombreCompleto());
        solicitud.setEmail(dto.getEmail());
        solicitud.setEdad(dto.getEdad());
        solicitud.setIngresosMensuales(dto.getIngresosMensuales());
        solicitud.setDeudasActuales(dto.getDeudasActuales());
        solicitud.setMontoSolicitado(dto.getMontoSolicitado());
        solicitud.setMesesEnEmpleoActual(dto.getMesesEnEmpleoActual());
        solicitud.setEstado(EstadoSolicitud.EN_PROCESO);

        Integer score = scoringService.calcularScore(solicitud);
        boolean aprobada = scoringService.esAprobadaConValidaciones(solicitud, score);
        String razon = scoringService.generarRazonEvaluacion(solicitud, score);

        solicitud.setScoreCrediticio(score);
        solicitud.setAprobada(aprobada);
        solicitud.setRazonEvaluacion(razon);
        solicitud.setEstado(aprobada ? EstadoSolicitud.APROBADA : EstadoSolicitud.RECHAZADA);

        repository.persist(solicitud);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("solicitudId", solicitud.id);
        respuesta.put("dni", solicitud.getDni());
        respuesta.put("nombreCompleto", solicitud.getNombreCompleto());
        respuesta.put("scoreCrediticio", solicitud.getScoreCrediticio());
        respuesta.put("aprobada", solicitud.getAprobada());
        respuesta.put("razonEvaluacion", solicitud.getRazonEvaluacion());
        respuesta.put("estado", solicitud.getEstado());

        return Response.status(Response.Status.CREATED).entity(respuesta).build();
    }

    @GET
    @Path("/{id}")
    public Response obtenerPorId(@PathParam("id") Long id) {
        SolicitudCredito solicitud = repository.findById(id);
        if (solicitud == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(solicitud).build();
    }

    @GET
    public List<SolicitudCredito> listarTodas() {
        return repository.listAll();
    }
}
