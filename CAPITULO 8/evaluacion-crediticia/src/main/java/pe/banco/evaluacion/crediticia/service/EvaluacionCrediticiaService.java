package pe.banco.evaluacion.crediticia.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.*;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import pe.banco.evaluacion.crediticia.client.BureauCreditoClient;
import pe.banco.evaluacion.crediticia.client.IdentidadClient;
import pe.banco.evaluacion.crediticia.client.ScoringClient;
import pe.banco.evaluacion.crediticia.model.*;

import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class EvaluacionCrediticiaService {

    @Inject
    @RestClient
    BureauCreditoClient bureauClient;

    @Inject
    @RestClient
    IdentidadClient identidadClient;

    @Inject
    @RestClient
    ScoringClient scoringClient;

    @ConfigProperty(name = "api.bureau.key")
    String bureauApiKey;

    @ConfigProperty(name = "api.identidad.token")
    String identidadToken;

    public ResultadoEvaluacion evaluarSolicitud(SolicitudCredito solicitud) {
        
        // 1. Validar identidad (sin fault tolerance - debe ser rápido y confiable)
        RespuestaIdentidad identidad = validarIdentidad(solicitud.getDni());
        
        if (!identidad.getIdentidadValida() || !"ACTIVO".equals(identidad.getEstado())) {
            return crearRechazo(solicitud.getDni(), "Identidad no válida o inactiva");
        }

        // 2. Consultar Bureau (con retry - puede fallar temporalmente)
        RespuestaBureau bureau = consultarBureau(solicitud.getDni());
        
        if (bureau.getMorosidadActiva()) {
            return crearRechazo(solicitud.getDni(), "Presenta morosidad activa");
        }

        // 3. Calcular scoring avanzado (con timeout y fallback)
        RespuestaScoring scoring = calcularScoring(solicitud);

        // 4. Evaluar y decidir
        return evaluarYDecidir(solicitud, bureau, scoring);
    }

    // RETRY: Si falla, reintenta hasta 3 veces
    // IMPORTANTE: Debe ser public para que CDI pueda interceptar
    @Retry(maxRetries = 3, delay = 1, delayUnit = ChronoUnit.SECONDS)
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    public RespuestaBureau consultarBureau(String dni) {
        return bureauClient.consultarHistorial(dni, bureauApiKey);
    }

    // TIMEOUT + FALLBACK: Máximo 3 segundos, si falla usa scoring básico
    // IMPORTANTE: Debe ser public para que CDI pueda interceptar
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    @Fallback(fallbackMethod = "scoringBasicoFallback")
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 10, delayUnit = ChronoUnit.SECONDS)
    public RespuestaScoring calcularScoring(SolicitudCredito solicitud) {
        return scoringClient.calcularScore(
            solicitud.getDni(), 
            solicitud.getMontoSolicitado(), 
            solicitud.getMesesPlazo()
        );
    }

    // FALLBACK METHOD: Se usa cuando el scoring avanzado falla
    public RespuestaScoring scoringBasicoFallback(SolicitudCredito solicitud) {
        // Scoring básico simplificado
        RespuestaScoring fallback = new RespuestaScoring();
        fallback.setDni(solicitud.getDni());
        fallback.setScoreInterno(500); // Score neutral
        fallback.setProbabilidadIncumplimiento(0.5);
        fallback.setRecomendacion("REVISAR_MANUAL");
        return fallback;
    }

    public RespuestaIdentidad validarIdentidad(String dni) {
        return identidadClient.validarIdentidad(dni, identidadToken);
    }

    private ResultadoEvaluacion evaluarYDecidir(SolicitudCredito solicitud, RespuestaBureau bureau, RespuestaScoring scoring) {
        ResultadoEvaluacion resultado = new ResultadoEvaluacion();
        resultado.setDni(solicitud.getDni());

        // Score total combinado
        int scoreTotal = (bureau.getScoreBureau() + scoring.getScoreInterno()) / 2;
        resultado.setScoreTotal(scoreTotal);

        // Lógica de decisión
        if (scoreTotal >= 700 && "APROBAR".equals(scoring.getRecomendacion())) {
            resultado.setDecision("APROBADO");
            resultado.setMontoAprobado(solicitud.getMontoSolicitado());
            resultado.setMensaje("Crédito aprobado exitosamente");
        } else if (scoreTotal < 500 || "RECHAZAR".equals(scoring.getRecomendacion())) {
            resultado.setDecision("RECHAZADO");
            resultado.setMotivoRechazo("Score insuficiente o recomendación negativa");
            resultado.setMensaje("Crédito rechazado");
        } else {
            resultado.setDecision("REQUIERE_ANALISIS_MANUAL");
            resultado.setMensaje("La solicitud requiere revisión manual por un analista");
        }

        return resultado;
    }

    private ResultadoEvaluacion crearRechazo(String dni, String motivo) {
        ResultadoEvaluacion resultado = new ResultadoEvaluacion();
        resultado.setDni(dni);
        resultado.setDecision("RECHAZADO");
        resultado.setMotivoRechazo(motivo);
        resultado.setMensaje("Crédito rechazado: " + motivo);
        resultado.setScoreTotal(0);
        return resultado;
    }
}
