package pe.banco.evaluacion.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import pe.banco.evaluacion.client.BureauClient;
import pe.banco.evaluacion.client.IdentidadClient;
import pe.banco.evaluacion.client.ScoringClient;
import pe.banco.evaluacion.model.*;

@ApplicationScoped
public class EvaluacionService {

    @Inject
    @RestClient
    BureauClient bureauClient;

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
        
        System.out.println("🎯 ORQUESTADOR: Iniciando evaluación para DNI " + solicitud.getDni());
        
        // 1. Validar identidad
        System.out.println("   → Llamando a Identidad Service...");
        RespuestaIdentidad identidad = identidadClient.validarIdentidad(solicitud.getDni(), identidadToken);
        
        if (!identidad.getIdentidadValida() || !"ACTIVO".equals(identidad.getEstado())) {
            return crearRechazo(solicitud.getDni(), "Identidad no válida o inactiva");
        }

        // 2. Consultar Bureau
        System.out.println("   → Llamando a Bureau Service...");
        RespuestaBureau bureau = bureauClient.consultarHistorial(solicitud.getDni(), bureauApiKey);
        
        if (bureau.getMorosidadActiva()) {
            return crearRechazo(solicitud.getDni(), "Presenta morosidad activa");
        }

        // 3. Calcular Scoring
        System.out.println("   → Llamando a Scoring Service...");
        RespuestaScoring scoring = scoringClient.calcularScore(
            solicitud.getDni(), 
            solicitud.getMontoSolicitado(), 
            solicitud.getMesesPlazo()
        );

        // 4. Evaluar y decidir
        return evaluarYDecidir(solicitud, bureau, scoring);
    }

    private ResultadoEvaluacion evaluarYDecidir(SolicitudCredito solicitud, RespuestaBureau bureau, RespuestaScoring scoring) {
        ResultadoEvaluacion resultado = new ResultadoEvaluacion();
        resultado.setDni(solicitud.getDni());

        int scoreTotal = (bureau.getScoreBureau() + scoring.getScoreInterno()) / 2;
        resultado.setScoreTotal(scoreTotal);

        if (scoreTotal >= 700 && "APROBAR".equals(scoring.getRecomendacion())) {
            resultado.setDecision("APROBADO");
            resultado.setMontoAprobado(solicitud.getMontoSolicitado());
            resultado.setMensaje("Crédito aprobado exitosamente");
            System.out.println("   ✅ DECISIÓN: APROBADO");
        } else if (scoreTotal < 500 || "RECHAZAR".equals(scoring.getRecomendacion())) {
            resultado.setDecision("RECHAZADO");
            resultado.setMotivoRechazo("Score insuficiente o recomendación negativa");
            resultado.setMensaje("Crédito rechazado");
            System.out.println("   ❌ DECISIÓN: RECHAZADO");
        } else {
            resultado.setDecision("REQUIERE_ANALISIS_MANUAL");
            resultado.setMensaje("La solicitud requiere revisión manual por un analista");
            System.out.println("   ⚠️  DECISIÓN: REQUIERE_ANALISIS_MANUAL");
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
        System.out.println("   ❌ DECISIÓN: RECHAZADO - " + motivo);
        return resultado;
    }
}
