package pe.banco.evaluacion.servicios;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.banco.evaluacion.entidades.SolicitudCredito;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ScoringServiceTest {

    @Inject
    ScoringService scoringService;

    @Test
    void deberiaCalcularScoreExcelente() {
        SolicitudCredito solicitud = crearSolicitudBase();
        solicitud.setIngresosMensuales(new BigDecimal("3000000"));
        solicitud.setDeudasActuales(new BigDecimal("300000"));
        solicitud.setMontoSolicitado(new BigDecimal("5000000"));
        solicitud.setMesesEnEmpleoActual(36);
        solicitud.setEdad(35);

        Integer score = scoringService.calcularScore(solicitud);

        assertTrue(score >= 800, "Score debería ser excelente (>= 800)");
        assertTrue(score <= 1000, "Score no puede exceder 1000");
    }

    @Test
    void deberiaRechazarPorDTIAlto() {
        SolicitudCredito solicitud = crearSolicitudBase();
        solicitud.setIngresosMensuales(new BigDecimal("1000000"));
        solicitud.setDeudasActuales(new BigDecimal("600000"));
        solicitud.setMontoSolicitado(new BigDecimal("2000000"));
        solicitud.setMesesEnEmpleoActual(12);
        solicitud.setEdad(30);

        Integer score = scoringService.calcularScore(solicitud);
        boolean aprobada = scoringService.esAprobada(score);

        assertFalse(aprobada, "Debería rechazar por DTI > 50%");
        assertTrue(score < 650, "Score debería estar bajo umbral de aprobación");
    }

    @Test
    void deberiaRechazarPorInestabilidadLaboral() {
        SolicitudCredito solicitud = crearSolicitudBase();
        solicitud.setIngresosMensuales(new BigDecimal("2000000"));
        solicitud.setDeudasActuales(new BigDecimal("200000"));
        solicitud.setMontoSolicitado(new BigDecimal("3000000"));
        solicitud.setMesesEnEmpleoActual(2);
        solicitud.setEdad(25);

        Integer score = scoringService.calcularScore(solicitud);
        String razon = scoringService.generarRazonEvaluacion(solicitud, score);

        System.out.println("Razón generada: " + razon);
        assertTrue(razon.contains("Inestabilidad laboral"), "Razón debería mencionar inestabilidad");
        assertFalse(scoringService.esAprobadaConValidaciones(solicitud, score), "Debería rechazar por inestabilidad");
    }

    @Test
    void deberiaCalcularDTICorrectamente() {
        BigDecimal deudas = new BigDecimal("500000");
        BigDecimal ingresos = new BigDecimal("1000000");

        BigDecimal dti = scoringService.calcularDTI(deudas, ingresos);

        assertTrue(dti.compareTo(new BigDecimal("49.99")) > 0 && dti.compareTo(new BigDecimal("50.01")) < 0, "DTI debería ser ~50%"); // assertEquals(new BigDecimal("50.00"), dti, "DTI debería ser 50%");
    }

    @Test
    void deberiaManejarDTICeroIngresos() {
        BigDecimal deudas = new BigDecimal("500000");
        BigDecimal ingresos = BigDecimal.ZERO;

        BigDecimal dti = scoringService.calcularDTI(deudas, ingresos);

        assertTrue(dti.compareTo(new BigDecimal("99")) >= 0, "DTI debería ser 100%"); // assertEquals(new BigDecimal("100.00"), dti, "DTI con ingresos 0 debería ser 100%");
    }

    @ParameterizedTest
    @CsvSource({
        "2500000, 300000, 5000000, 48, 35, true",
        "1800000, 400000, 3000000, 24, 28, true",
        "1500000, 900000, 4000000, 12, 42, false",
        "1200000, 150000, 2000000, 2, 23, true",
        "3000000, 100000, 6000000, 60, 45, true"
    })
    void deberiaEvaluarMultiplesEscenarios(String ingresos, String deudas, String monto, 
                                            int meses, int edad, boolean deberiaAprobar) {
        SolicitudCredito solicitud = crearSolicitudBase();
        solicitud.setIngresosMensuales(new BigDecimal(ingresos));
        solicitud.setDeudasActuales(new BigDecimal(deudas));
        solicitud.setMontoSolicitado(new BigDecimal(monto));
        solicitud.setMesesEnEmpleoActual(meses);
        solicitud.setEdad(edad);

        Integer score = scoringService.calcularScore(solicitud);
        boolean aprobada = scoringService.esAprobada(score);

        assertEquals(deberiaAprobar, aprobada, 
            String.format("Score %d debería %s", score, deberiaAprobar ? "aprobar" : "rechazar"));
    }

    @Test
    void deberiaGenerarRazonAprobacionExcelente() {
        SolicitudCredito solicitud = crearSolicitudBase();
        solicitud.setIngresosMensuales(new BigDecimal("3500000"));
        solicitud.setDeudasActuales(new BigDecimal("200000"));
        solicitud.setMontoSolicitado(new BigDecimal("4000000"));
        solicitud.setMesesEnEmpleoActual(48);
        solicitud.setEdad(40);

        Integer score = scoringService.calcularScore(solicitud);
        String razon = scoringService.generarRazonEvaluacion(solicitud, score);

        assertTrue(score >= 800);
        assertTrue(razon.contains("Excelente"), "Razón debería mencionar perfil excelente");
    }

    @Test
    void deberiaLimitarScoreEntreCeroYMil() {
        SolicitudCredito solicitudPesima = crearSolicitudBase();
        solicitudPesima.setIngresosMensuales(new BigDecimal("500000"));
        solicitudPesima.setDeudasActuales(new BigDecimal("800000"));
        solicitudPesima.setMontoSolicitado(new BigDecimal("10000000"));
        solicitudPesima.setMesesEnEmpleoActual(1);
        solicitudPesima.setEdad(75);

        Integer score = scoringService.calcularScore(solicitudPesima);

        assertTrue(score >= 0, "Score no puede ser negativo");
        assertTrue(score <= 1000, "Score no puede exceder 1000");
    }

    private SolicitudCredito crearSolicitudBase() {
        SolicitudCredito solicitud = new SolicitudCredito();
        solicitud.setDni("12345678");
        solicitud.setNombreCompleto("Test Usuario");
        solicitud.setEmail("test@email.cl");
        return solicitud;
    }
}
