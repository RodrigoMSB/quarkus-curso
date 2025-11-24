package pe.banco.aprobacion.resource;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import pe.banco.aprobacion.model.ResultadoEvaluacion;
import pe.banco.aprobacion.model.SolicitudCredito;
import pe.banco.aprobacion.service.EvaluadorCrediticio;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Recurso REST para el sistema de pre-aprobación crediticia.
 * Expone endpoints para evaluar solicitudes de crédito en tiempo real.
 * 
 * Analogía: Es como la ventanilla del banco donde los clientes presentan
 * sus solicitudes y reciben respuestas inmediatas.
 */
@Path("/api/preaprobacion")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PreAprobacionResource {

    private static final Logger LOG = Logger.getLogger(PreAprobacionResource.class);

    @Inject
    EvaluadorCrediticio evaluadorCrediticio;

    /**
     * POST /api/preaprobacion/evaluar
     * Evalúa una solicitud de crédito y retorna el resultado inmediatamente.
     * 
     * @param solicitud Datos de la solicitud (validados automáticamente)
     * @return ResultadoEvaluacion con la decisión y condiciones
     */
    @POST
    @Path("/evaluar")
    public Response evaluar(@Valid SolicitudCredito solicitud) {
        LOG.infof("📥 Recibida solicitud de evaluación para: %s (Doc: %s)", 
                  solicitud.nombreCompleto, solicitud.numeroDocumento);

        try {
            // Validación adicional de negocio
            validarSolicitud(solicitud);

            // Evaluar la solicitud
            ResultadoEvaluacion resultado = evaluadorCrediticio.evaluar(solicitud);

            // Retornar respuesta según el resultado
            if (resultado.isAprobado()) {
                LOG.infof("✅ Solicitud APROBADA para %s | Monto: %s | Tasa: %s%%", 
                          solicitud.nombreCompleto, 
                          resultado.getMontoMaximoAprobado(),
                          resultado.getTasaInteres());
                return Response.ok(resultado).build();
            } else {
                LOG.infof("❌ Solicitud RECHAZADA para %s | Motivos: %d", 
                          solicitud.nombreCompleto, 
                          resultado.getMotivosRechazo().size());
                return Response.status(Response.Status.OK).entity(resultado).build();
            }

        } catch (IllegalArgumentException e) {
            LOG.error("❌ Validación fallida: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of(
                        "error", "Validación fallida",
                        "mensaje", e.getMessage()
                    ))
                    .build();
        } catch (Exception e) {
            LOG.error("❌ Error interno al evaluar solicitud", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                        "error", "Error interno del servidor",
                        "mensaje", "No se pudo procesar la solicitud. Intente nuevamente."
                    ))
                    .build();
        }
    }

    /**
     * GET /api/preaprobacion/{id}
     * Consulta el resultado de una solicitud previamente evaluada.
     * 
     * @param id ID de la solicitud
     * @return Datos completos de la solicitud y su resultado
     */
    @GET
    @Path("/{id}")
    public Response consultarSolicitud(@PathParam("id") Long id) {
        LOG.infof("🔍 Consultando solicitud ID: %d", id);

        SolicitudCredito solicitud = SolicitudCredito.findById(id);

        if (solicitud == null) {
            LOG.warnf("⚠️ Solicitud no encontrada: ID %d", id);
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                        "error", "Solicitud no encontrada",
                        "mensaje", "No existe una solicitud con ID: " + id
                    ))
                    .build();
        }

        return Response.ok(solicitud).build();
    }

    /**
     * GET /api/preaprobacion/estadisticas
     * Retorna estadísticas generales del sistema.
     * 
     * @return Mapa con estadísticas (total solicitudes, aprobadas, rechazadas, etc.)
     */
    @GET
    @Path("/estadisticas")
    public Response obtenerEstadisticas() {
        LOG.info("📊 Consultando estadísticas del sistema");

        long totalSolicitudes = SolicitudCredito.count();
        long aprobadas = SolicitudCredito.countByEstado("APROBADO");
        long rechazadas = SolicitudCredito.countByEstado("RECHAZADO");
        long pendientes = SolicitudCredito.countByEstado("PENDIENTE");
        long enEvaluacion = SolicitudCredito.countByEstado("EVALUANDO");

        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("totalSolicitudes", totalSolicitudes);
        estadisticas.put("aprobadas", aprobadas);
        estadisticas.put("rechazadas", rechazadas);
        estadisticas.put("pendientes", pendientes);
        estadisticas.put("enEvaluacion", enEvaluacion);
        estadisticas.put("tasaAprobacion", calcularTasaAprobacion(aprobadas, rechazadas));

        return Response.ok(estadisticas).build();
    }

    /**
     * GET /api/preaprobacion/listar
     * Lista todas las solicitudes (con paginación opcional).
     * 
     * @param page Número de página (default: 0)
     * @param size Tamaño de página (default: 10)
     * @return Lista de solicitudes
     */
    @GET
    @Path("/listar")
    public Response listarSolicitudes(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size) {
        
        LOG.infof("📋 Listando solicitudes (página: %d, tamaño: %d)", page, size);

        List<SolicitudCredito> solicitudes = SolicitudCredito
                .findAll()
                .page(page, size)
                .list();

        long total = SolicitudCredito.count();

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("solicitudes", solicitudes);
        respuesta.put("total", total);
        respuesta.put("page", page);
        respuesta.put("size", size);
        respuesta.put("totalPages", (total + size - 1) / size);

        return Response.ok(respuesta).build();
    }

    /**
     * DELETE /api/preaprobacion/{id}
     * Elimina una solicitud (solo para testing/demos).
     * 
     * @param id ID de la solicitud a eliminar
     * @return Respuesta de éxito o error
     */
    @DELETE
    @Path("/{id}")
    public Response eliminarSolicitud(@PathParam("id") Long id) {
        LOG.infof("🗑️ Eliminando solicitud ID: %d", id);

        SolicitudCredito solicitud = SolicitudCredito.findById(id);

        if (solicitud == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Solicitud no encontrada"))
                    .build();
        }

        solicitud.delete();
        LOG.infof("✅ Solicitud eliminada: ID %d", id);

        return Response.ok(Map.of("mensaje", "Solicitud eliminada exitosamente")).build();
    }

    /**
     * Validaciones adicionales de negocio.
     */
    private void validarSolicitud(SolicitudCredito solicitud) {
        // Validar que el monto solicitado sea razonable para el ingreso
        if (solicitud.montoSolicitado.compareTo(
                solicitud.ingresoMensual.multiply(java.math.BigDecimal.valueOf(5))) > 0) {
            throw new IllegalArgumentException(
                "El monto solicitado no puede ser mayor a 5 veces el ingreso mensual");
        }

        // Validar tipo de garantía si tiene garantía
        if (solicitud.tieneGarantia && (solicitud.tipoGarantia == null || solicitud.tipoGarantia.isBlank())) {
            throw new IllegalArgumentException(
                "Debe especificar el tipo de garantía si indica que tiene garantía");
        }

        // Validar tipo de documento
        if (!List.of("DNI", "CE", "RUC").contains(solicitud.tipoDocumento)) {
            throw new IllegalArgumentException(
                "Tipo de documento inválido. Valores permitidos: DNI, CE, RUC");
        }
    }

    /**
     * Calcula la tasa de aprobación en porcentaje.
     */
    private double calcularTasaAprobacion(long aprobadas, long rechazadas) {
        long total = aprobadas + rechazadas;
        if (total == 0) return 0.0;
        return Math.round((aprobadas * 100.0 / total) * 100.0) / 100.0;
    }
}
