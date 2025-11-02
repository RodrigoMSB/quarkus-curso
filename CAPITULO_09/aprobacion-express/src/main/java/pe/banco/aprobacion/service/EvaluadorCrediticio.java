package pe.banco.aprobacion.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import pe.banco.aprobacion.model.FactoresRiesgo;
import pe.banco.aprobacion.model.ResultadoEvaluacion;
import pe.banco.aprobacion.model.SolicitudCredito;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio principal que orquesta toda la evaluación crediticia.
 * 
 * Analogía: Es como el director de una orquesta que coordina a todos
 * los músicos (servicios) para crear una sinfonía perfecta (evaluación completa).
 */
@ApplicationScoped
public class EvaluadorCrediticio {

    private static final Logger LOG = Logger.getLogger(EvaluadorCrediticio.class);

    @Inject
    CalculadorScoring calculadorScoring;

    @Inject
    BureauService bureauService;

    // Constantes de negocio
    private static final int SCORE_MINIMO_APROBACION = 600;
    private static final BigDecimal TASA_BASE = BigDecimal.valueOf(12.0); // 12% anual
    private static final int PLAZO_BASE_MESES = 60; // 5 años

    /**
     * Evalúa una solicitud de crédito de forma completa.
     * Este es el método principal que orquesta todo el proceso.
     */
    @Transactional
    public ResultadoEvaluacion evaluar(SolicitudCredito solicitud) {
        long startTime = System.currentTimeMillis();
        
        LOG.infof("🚀 Iniciando evaluación de solicitud para cliente: %s (Documento: %s)", 
                  solicitud.nombreCompleto, solicitud.numeroDocumento);

        // Marcar solicitud como en evaluación
        solicitud.estado = "EVALUANDO";
        solicitud.persistAndFlush();

        try {
            // PASO 1: Consultar Bureau de Crédito
            LOG.info("📋 PASO 1/4: Consultando bureau de crédito...");
            boolean enListaNegra = bureauService.estaEnListaNegra(solicitud.numeroDocumento);
            int scoreHistorico = bureauService.consultarScoreHistorico(solicitud.numeroDocumento);
            boolean tieneMorosidad = bureauService.tieneMorosidadReciente(solicitud.numeroDocumento);
            int creditosActivos = bureauService.consultarCreditosActivos(solicitud.numeroDocumento);

            // PASO 2: Calcular factores de riesgo
            LOG.info("🧮 PASO 2/4: Calculando factores de riesgo...");
            FactoresRiesgo factores = calculadorScoring.calcularFactores(solicitud);
            factores.setEnlistaNegraSimulada(enListaNegra);

            // PASO 3: Calcular score final
            LOG.info("📊 PASO 3/4: Calculando score crediticio final...");
            int scoreFinal = calcularScoreFinal(factores, scoreHistorico);
            String nivelRiesgo = factores.determinarNivelRiesgo();

            LOG.infof("Score calculado: %d | Nivel de riesgo: %s", scoreFinal, nivelRiesgo);

            // PASO 4: Aplicar reglas de negocio y decidir
            LOG.info("⚖️ PASO 4/4: Aplicando reglas de negocio...");
            ResultadoEvaluacion resultado = aplicarReglasNegocio(
                solicitud, scoreFinal, nivelRiesgo, enListaNegra, 
                tieneMorosidad, creditosActivos, factores
            );

            // Actualizar solicitud con el resultado
            actualizarSolicitud(solicitud, resultado);

            // Calcular tiempo de evaluación
            long tiempoEvaluacion = System.currentTimeMillis() - startTime;
            resultado.setTiempoEvaluacionMs(tiempoEvaluacion);
            solicitud.tiempoEvaluacionMs = tiempoEvaluacion;
            solicitud.persistAndFlush();

            LOG.infof("✅ Evaluación completada en %d ms | Resultado: %s", 
                      tiempoEvaluacion, resultado.isAprobado() ? "APROBADO" : "RECHAZADO");

            return resultado;

        } catch (Exception e) {
            LOG.error("❌ Error durante la evaluación", e);
            solicitud.estado = "ERROR";
            solicitud.persistAndFlush();
            throw new RuntimeException("Error al evaluar solicitud: " + e.getMessage(), e);
        }
    }

    /**
     * Calcula el score final combinando el score calculado y el histórico del bureau.
     */
    private int calcularScoreFinal(FactoresRiesgo factores, int scoreHistorico) {
        int scoreCalculado = factores.calcularScoreTotal();
        
        // Ponderación: 60% score actual + 40% score histórico
        double scoreFinal = (scoreCalculado * 0.6) + (scoreHistorico * 0.4);
        
        return (int) Math.round(scoreFinal);
    }

    /**
     * Aplica todas las reglas de negocio para decidir aprobación o rechazo.
     */
    private ResultadoEvaluacion aplicarReglasNegocio(
            SolicitudCredito solicitud,
            int scoreFinal,
            String nivelRiesgo,
            boolean enListaNegra,
            boolean tieneMorosidad,
            int creditosActivos,
            FactoresRiesgo factores) {

        List<String> motivosRechazo = new ArrayList<>();
        List<String> recomendaciones = new ArrayList<>();

        // REGLA 1: Lista negra = Rechazo automático
        if (enListaNegra) {
            motivosRechazo.add("Cliente figura en lista negra del bureau crediticio");
            return crearResultadoRechazo(solicitud.id, scoreFinal, motivosRechazo, "CRITICO");
        }

        // REGLA 2: Score mínimo
        if (scoreFinal < SCORE_MINIMO_APROBACION) {
            motivosRechazo.add("Score crediticio insuficiente (mínimo requerido: " + SCORE_MINIMO_APROBACION + ")");
        }

        // REGLA 3: Ratio de deuda muy alto
        if (solicitud.esDeudaAlta()) {
            motivosRechazo.add("Ratio deuda/ingreso demasiado alto (máximo aceptable: 40%)");
            recomendaciones.add("Reduzca su nivel de endeudamiento actual antes de solicitar nuevo crédito");
        }

        // REGLA 4: Morosidad reciente
        if (tieneMorosidad) {
            motivosRechazo.add("Presenta morosidad en los últimos 12 meses");
            recomendaciones.add("Regularice sus pagos pendientes para mejorar su historial");
        }

        // REGLA 5: Demasiados créditos activos
        if (creditosActivos > 5) {
            motivosRechazo.add("Exceso de productos crediticios activos (" + creditosActivos + " créditos)");
            recomendaciones.add("Consolide sus deudas actuales antes de solicitar nuevo crédito");
        }

        // REGLA 6: Antigüedad laboral muy baja
        if (solicitud.antiguedadLaboralAnios < 1) {
            motivosRechazo.add("Antigüedad laboral insuficiente (mínimo 1 año requerido)");
            recomendaciones.add("Alcance al menos 1 año de antigüedad laboral en su empleo actual");
        }

        // Si hay motivos de rechazo → RECHAZAR
        if (!motivosRechazo.isEmpty()) {
            ResultadoEvaluacion rechazo = crearResultadoRechazo(solicitud.id, scoreFinal, motivosRechazo, nivelRiesgo);
            rechazo.setRecomendaciones(recomendaciones);
            return rechazo;
        }

        // Si no hay motivos de rechazo → APROBAR
        return crearResultadoAprobacion(solicitud, scoreFinal, nivelRiesgo);
    }

    /**
     * Crea un resultado de APROBACIÓN con condiciones calculadas.
     */
    private ResultadoEvaluacion crearResultadoAprobacion(
            SolicitudCredito solicitud, 
            int scoreFinal, 
            String nivelRiesgo) {

        // Calcular monto máximo aprobado según el score y nivel de riesgo
        BigDecimal montoMaximo = calcularMontoMaximoAprobado(solicitud, scoreFinal, nivelRiesgo);

        // Calcular tasa de interés según el nivel de riesgo
        BigDecimal tasaInteres = calcularTasaInteres(nivelRiesgo, scoreFinal);

        // Calcular plazo máximo según el nivel de riesgo
        int plazoMaximo = calcularPlazoMaximo(nivelRiesgo);

        ResultadoEvaluacion resultado = ResultadoEvaluacion.aprobado(
            solicitud.id,
            scoreFinal,
            montoMaximo,
            tasaInteres,
            plazoMaximo,
            nivelRiesgo
        );

        // Agregar recomendaciones
        if ("MEDIO".equals(nivelRiesgo)) {
            resultado.agregarRecomendacion("Considere reducir el plazo para obtener mejor tasa de interés");
        } else if ("ALTO".equals(nivelRiesgo)) {
            resultado.agregarRecomendacion("Mejore su historial crediticio para acceder a mejores condiciones");
            resultado.agregarRecomendacion("Considere aportar una garantía para reducir la tasa de interés");
        }

        return resultado;
    }

    /**
     * Crea un resultado de RECHAZO.
     */
    private ResultadoEvaluacion crearResultadoRechazo(
            Long solicitudId, 
            int scoreFinal, 
            List<String> motivos, 
            String nivelRiesgo) {
        
        return ResultadoEvaluacion.rechazado(solicitudId, scoreFinal, motivos, nivelRiesgo);
    }

    /**
     * Calcula el monto máximo que se puede aprobar.
     */
    private BigDecimal calcularMontoMaximoAprobado(
            SolicitudCredito solicitud, 
            int score, 
            String nivelRiesgo) {

        BigDecimal montoSolicitado = solicitud.montoSolicitado;
        BigDecimal ingresoAnual = solicitud.ingresoMensual.multiply(BigDecimal.valueOf(12));

        // Factor según nivel de riesgo
        BigDecimal factor = switch (nivelRiesgo) {
            case "BAJO" -> BigDecimal.valueOf(3.0);      // Hasta 3x ingreso anual
            case "MEDIO" -> BigDecimal.valueOf(2.0);     // Hasta 2x ingreso anual
            case "ALTO" -> BigDecimal.valueOf(1.5);      // Hasta 1.5x ingreso anual
            default -> BigDecimal.valueOf(1.0);
        };

        BigDecimal montoMaximoPorIngreso = ingresoAnual.multiply(factor);

        // Retornar el menor entre lo solicitado y el máximo por ingreso
        return montoSolicitado.min(montoMaximoPorIngreso)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula la tasa de interés según el nivel de riesgo y score.
     */
    private BigDecimal calcularTasaInteres(String nivelRiesgo, int score) {
        BigDecimal tasa = TASA_BASE;

        // Ajuste por nivel de riesgo
        tasa = switch (nivelRiesgo) {
            case "BAJO" -> tasa.subtract(BigDecimal.valueOf(2.0));     // 10%
            case "MEDIO" -> tasa;                                        // 12%
            case "ALTO" -> tasa.add(BigDecimal.valueOf(3.0));           // 15%
            case "CRITICO" -> tasa.add(BigDecimal.valueOf(6.0));        // 18%
            default -> tasa;
        };

        // Ajuste fino por score
        if (score >= 750) {
            tasa = tasa.subtract(BigDecimal.valueOf(0.5));
        } else if (score < 650) {
            tasa = tasa.add(BigDecimal.valueOf(0.5));
        }

        return tasa.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula el plazo máximo en meses según el nivel de riesgo.
     */
    private int calcularPlazoMaximo(String nivelRiesgo) {
        return switch (nivelRiesgo) {
            case "BAJO" -> 84;      // 7 años
            case "MEDIO" -> 60;     // 5 años
            case "ALTO" -> 36;      // 3 años
            default -> 24;          // 2 años
        };
    }

    /**
     * Actualiza la solicitud con el resultado de la evaluación.
     */
    private void actualizarSolicitud(SolicitudCredito solicitud, ResultadoEvaluacion resultado) {
        solicitud.estado = resultado.isAprobado() ? "APROBADO" : "RECHAZADO";
        solicitud.scoreCalculado = resultado.getScoreCalculado();
        solicitud.montoAprobado = resultado.getMontoMaximoAprobado();
        solicitud.tasaInteres = resultado.getTasaInteres();
        solicitud.plazoMaximoMeses = resultado.getPlazoMaximoMeses();
        solicitud.fechaEvaluacion = LocalDateTime.now();
    }
}
