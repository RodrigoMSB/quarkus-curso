package pe.banco.aprobacion.service;

import jakarta.enterprise.context.ApplicationScoped;
import pe.banco.aprobacion.model.FactoresRiesgo;
import pe.banco.aprobacion.model.SolicitudCredito;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Servicio encargado de calcular el score crediticio
 * basado en múltiples factores ponderados.
 * 
 * Analogía: Es como un chef que combina ingredientes con 
 * proporciones exactas para crear el plato perfecto.
 */
@ApplicationScoped
public class CalculadorScoring {

    // Constantes para normalización de puntajes (escala 0-100)
    private static final int PUNTAJE_MAXIMO = 100;
    private static final int PUNTAJE_MINIMO = 0;

    /**
     * Calcula todos los factores de riesgo de una solicitud.
     */
    public FactoresRiesgo calcularFactores(SolicitudCredito solicitud) {
        FactoresRiesgo factores = new FactoresRiesgo();

        // 1. Ratio Deuda/Ingreso (30% del score)
        factores.setRatioDeudaIngreso(calcularPuntajeRatioDeuda(solicitud));

        // 2. Capacidad de Pago (25% del score)
        factores.setCapacidadPago(calcularPuntajeCapacidadPago(solicitud));

        // 3. Antigüedad Laboral (15% del score)
        factores.setPuntajeAntiguedadLaboral(calcularPuntajeAntiguedad(solicitud.antiguedadLaboralAnios));

        // 4. Edad del Solicitante (10% del score)
        factores.setPuntajeEdad(calcularPuntajeEdad(solicitud.edad));

        // 5. Garantía (10% del score)
        factores.setPuntajeGarantia(calcularPuntajeGarantia(solicitud.tieneGarantia, solicitud.tipoGarantia));

        // 6. Monto Solicitado vs Ingreso (10% del score)
        factores.setPuntajeMontoSolicitado(calcularPuntajeMontoSolicitado(solicitud));

        return factores;
    }

    /**
     * Calcula el puntaje basado en la relación Deuda/Ingreso.
     * Menos deuda = mejor score.
     */
    private BigDecimal calcularPuntajeRatioDeuda(SolicitudCredito solicitud) {
        BigDecimal ratio = solicitud.calcularRatioDeuda();
        
        // Ratio ideal: < 30% → 100 puntos
        // Ratio aceptable: 30-40% → 70-100 puntos
        // Ratio alto: 40-50% → 40-70 puntos
        // Ratio crítico: > 50% → 0-40 puntos
        
        if (ratio.compareTo(BigDecimal.valueOf(0.30)) <= 0) {
            return BigDecimal.valueOf(PUNTAJE_MAXIMO);
        } else if (ratio.compareTo(BigDecimal.valueOf(0.40)) <= 0) {
            // Interpolación lineal entre 70 y 100
            return interpolate(ratio, 0.30, 0.40, 100, 70);
        } else if (ratio.compareTo(BigDecimal.valueOf(0.50)) <= 0) {
            // Interpolación lineal entre 40 y 70
            return interpolate(ratio, 0.40, 0.50, 70, 40);
        } else {
            // Penalización fuerte por ratio > 50%
            double penalizacion = Math.max(0, 40 - ((ratio.doubleValue() - 0.50) * 80));
            return BigDecimal.valueOf(penalizacion);
        }
    }

    /**
     * Calcula la capacidad de pago: ¿Puede pagar la cuota mensual estimada?
     */
    private BigDecimal calcularPuntajeCapacidadPago(SolicitudCredito solicitud) {
        // Estimamos cuota mensual aproximada (simplificado)
        // Fórmula: Monto / (Plazo * Factor ajuste de interés ~1.5)
        BigDecimal cuotaEstimada = solicitud.montoSolicitado
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP) // Asumimos 60 meses
                .multiply(BigDecimal.valueOf(1.5)); // Factor de interés simplificado

        // Ingreso disponible después de deudas actuales
        BigDecimal ingresoDisponible = solicitud.ingresoMensual
                .subtract(solicitud.deudaActual.multiply(BigDecimal.valueOf(0.05))); // Asumimos 5% de la deuda como cuota mensual

        // Capacidad: ingreso disponible debe ser al menos 2x la cuota
        BigDecimal capacidad = ingresoDisponible.divide(cuotaEstimada, 4, RoundingMode.HALF_UP);

        if (capacidad.compareTo(BigDecimal.valueOf(2.5)) >= 0) {
            return BigDecimal.valueOf(PUNTAJE_MAXIMO);
        } else if (capacidad.compareTo(BigDecimal.valueOf(2.0)) >= 0) {
            return BigDecimal.valueOf(85);
        } else if (capacidad.compareTo(BigDecimal.valueOf(1.5)) >= 0) {
            return BigDecimal.valueOf(65);
        } else if (capacidad.compareTo(BigDecimal.valueOf(1.0)) >= 0) {
            return BigDecimal.valueOf(40);
        } else {
            return BigDecimal.valueOf(PUNTAJE_MINIMO);
        }
    }

    /**
     * Puntaje por antigüedad laboral.
     * Mayor antigüedad = mayor estabilidad = mejor score.
     */
    private int calcularPuntajeAntiguedad(Integer anios) {
        if (anios >= 10) {
            return PUNTAJE_MAXIMO;
        } else if (anios >= 5) {
            return 80;
        } else if (anios >= 3) {
            return 60;
        } else if (anios >= 1) {
            return 40;
        } else {
            return 20; // Menos de 1 año
        }
    }

    /**
     * Puntaje por edad del solicitante.
     * Edad óptima: 30-50 años (mayor estabilidad).
     */
    private int calcularPuntajeEdad(Integer edad) {
        if (edad >= 30 && edad <= 50) {
            return PUNTAJE_MAXIMO; // Rango óptimo
        } else if (edad >= 25 && edad <= 55) {
            return 80; // Rango bueno
        } else if (edad >= 21 && edad <= 60) {
            return 60; // Rango aceptable
        } else {
            return 40; // Muy joven o cerca del retiro
        }
    }

    /**
     * Puntaje por tipo de garantía.
     * Mejor garantía = menor riesgo = mejor score.
     */
    private int calcularPuntajeGarantia(Boolean tieneGarantia, String tipoGarantia) {
        if (tieneGarantia == null || !tieneGarantia) {
            return 50; // Sin garantía
        }

        return switch (tipoGarantia != null ? tipoGarantia.toUpperCase() : "") {
            case "HIPOTECARIA" -> PUNTAJE_MAXIMO;  // Mejor garantía
            case "VEHICULAR" -> 85;
            case "PRENDARIA" -> 70;
            default -> 50; // Sin especificar
        };
    }

    /**
     * Puntaje por monto solicitado en relación al ingreso.
     * Solicitar poco en relación al ingreso = menor riesgo.
     */
    private int calcularPuntajeMontoSolicitado(SolicitudCredito solicitud) {
        // Ratio: Monto / (Ingreso anual)
        BigDecimal ingresoAnual = solicitud.ingresoMensual.multiply(BigDecimal.valueOf(12));
        BigDecimal ratioMonto = solicitud.montoSolicitado.divide(ingresoAnual, 4, RoundingMode.HALF_UP);

        if (ratioMonto.compareTo(BigDecimal.valueOf(1.0)) <= 0) {
            return PUNTAJE_MAXIMO; // Solicita menos de 1 año de ingreso
        } else if (ratioMonto.compareTo(BigDecimal.valueOf(2.0)) <= 0) {
            return 75; // Entre 1 y 2 años de ingreso
        } else if (ratioMonto.compareTo(BigDecimal.valueOf(3.0)) <= 0) {
            return 50; // Entre 2 y 3 años de ingreso
        } else {
            return 25; // Más de 3 años de ingreso
        }
    }

    /**
     * Método auxiliar para interpolación lineal.
     */
    private BigDecimal interpolate(BigDecimal value, double x1, double x2, double y1, double y2) {
        double x = value.doubleValue();
        double result = y1 + ((x - x1) * (y2 - y1) / (x2 - x1));
        return BigDecimal.valueOf(result).setScale(2, RoundingMode.HALF_UP);
    }
}
