package pe.banco.evaluacion.servicios;

import jakarta.enterprise.context.ApplicationScoped;
import pe.banco.evaluacion.entidades.SolicitudCredito;

import java.math.BigDecimal;
import java.math.RoundingMode;

@ApplicationScoped
public class ScoringService {
    private static final Integer UMBRAL_APROBACION = 650;
    private static final BigDecimal DTI_LIMITE = new BigDecimal("50.00");
    private static final BigDecimal CAPACIDAD_PAGO_FACTOR = new BigDecimal("0.40");

    public Integer calcularScore(SolicitudCredito solicitud) {
        int score = 500;
        score += evaluarDTI(solicitud);
        score += evaluarEdad(solicitud);
        score += evaluarEstabilidadLaboral(solicitud);
        score += evaluarCapacidadPago(solicitud);
        score += evaluarMontoSolicitado(solicitud);
        
        score = Math.min(score, 1000);
        score = Math.min(score, 1000);
        solicitud.setScoreCrediticio(score);
        return score;
    }

    public BigDecimal calcularDTI(BigDecimal deudas, BigDecimal ingresosMensuales) {
        if (ingresosMensuales.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("100");
        }
        return deudas.divide(ingresosMensuales, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }

    private int evaluarDTI(SolicitudCredito solicitud) {
        BigDecimal dti = calcularDTI(solicitud.getDeudasActuales(), solicitud.getIngresosMensuales());
        if (dti.compareTo(new BigDecimal("20")) <= 0) {
            return 200;
        } else if (dti.compareTo(new BigDecimal("35")) <= 0) {
            return 100;
        } else if (dti.compareTo(DTI_LIMITE) <= 0) {
            return 0;
        } else {
            return -300;
        }
    }

    private int evaluarEstabilidadLaboral(SolicitudCredito solicitud) {
        int meses = solicitud.getMesesEnEmpleoActual();
        if (meses >= 24) {
            return 120;
        } else if (meses >= 12) {
            return 80;
        } else if (meses >= 6) {
            return 40;
        } else {
            return -20;
        }
    }

    private int evaluarCapacidadPago(SolicitudCredito solicitud) {
        BigDecimal capacidadPago = solicitud.getIngresosMensuales()
            .multiply(CAPACIDAD_PAGO_FACTOR);
        BigDecimal cuotaEstimada = solicitud.getMontoSolicitado()
            .divide(new BigDecimal("36"), 2, RoundingMode.HALF_UP);
        
        if (cuotaEstimada.compareTo(capacidadPago) <= 0) {
            return 150;
        } else if (cuotaEstimada.compareTo(capacidadPago.multiply(new BigDecimal("1.2"))) <= 0) {
            return 50;
        } else {
            return -100;
        }
    }

    private int evaluarEdad(SolicitudCredito solicitud) {
        int edad = solicitud.getEdad();
        if (edad >= 25 && edad <= 55) {
            return 80;
        } else if (edad >= 18 && edad < 25) {
            return 30;
        } else if (edad > 55 && edad <= 65) {
            return 50;
        } else {
            return -30;
        }
    }

    private int evaluarMontoSolicitado(SolicitudCredito solicitud) {
        BigDecimal monto = solicitud.getMontoSolicitado();
        BigDecimal ingresos = solicitud.getIngresosMensuales();
        BigDecimal ratio = monto.divide(ingresos, 2, RoundingMode.HALF_UP);
        
        if (ratio.compareTo(new BigDecimal("10")) <= 0) {
            return 100;
        } else if (ratio.compareTo(new BigDecimal("20")) <= 0) {
            return 50;
        } else if (ratio.compareTo(new BigDecimal("30")) <= 0) {
            return 0;
        } else {
            return -50;
        }
    }

    public boolean esAprobada(Integer score) {
        return score >= UMBRAL_APROBACION;
    }

    public boolean esAprobadaConValidaciones(SolicitudCredito solicitud, Integer score) {
        // Primero: validaciones críticas
        if (solicitud.getMesesEnEmpleoActual() < 3) {
            return false;
        }
        
        BigDecimal dti = calcularDTI(solicitud.getDeudasActuales(), solicitud.getIngresosMensuales());
        if (dti.compareTo(DTI_LIMITE) > 0) {
            return false;
        }
        
        BigDecimal capacidadPago = solicitud.getIngresosMensuales().multiply(CAPACIDAD_PAGO_FACTOR);
        BigDecimal cuotaEstimada = solicitud.getMontoSolicitado()
            .divide(new BigDecimal("36"), 2, RoundingMode.HALF_UP);
        if (cuotaEstimada.compareTo(capacidadPago.multiply(new BigDecimal("1.5"))) > 0) {
            return false;
        }
        
        // Después: evaluar score
        return esAprobada(score);
    }

    public String generarRazonEvaluacion(SolicitudCredito solicitud, Integer score) {
        // PRIMERO: Validaciones críticas independientes del score
        if (solicitud.getMesesEnEmpleoActual() < 3) {
            return "Rechazado: Inestabilidad laboral. Se requiere mínimo 3 meses en empleo actual.";
        }
        
        BigDecimal dti = calcularDTI(solicitud.getDeudasActuales(), solicitud.getIngresosMensuales());
        if (dti.compareTo(DTI_LIMITE) > 0) {
            return String.format("Rechazado: Ratio deuda/ingreso (%.2f%%) supera el límite permitido (50%%).", dti);
        }
        
        BigDecimal capacidadPago = solicitud.getIngresosMensuales().multiply(CAPACIDAD_PAGO_FACTOR);
        BigDecimal cuotaEstimada = solicitud.getMontoSolicitado()
            .divide(new BigDecimal("36"), 2, RoundingMode.HALF_UP);
        if (cuotaEstimada.compareTo(capacidadPago.multiply(new BigDecimal("1.5"))) > 0) {
            return "Rechazado: Monto solicitado excede capacidad de pago mensual.";
        }
        
        // DESPUÉS: Evaluación por score
        if (score >= 800) {
            return "Aprobado: Excelente perfil crediticio. Felicitaciones.";
        }
        if (score >= UMBRAL_APROBACION) {
            return "Aprobado: Perfil crediticio cumple con los requisitos del banco.";
        }
        
        return "Rechazado: Score crediticio insuficiente para aprobación automática.";
    }
}
