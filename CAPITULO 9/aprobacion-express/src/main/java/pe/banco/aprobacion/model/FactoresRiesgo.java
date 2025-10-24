package pe.banco.aprobacion.model;

import java.math.BigDecimal;

/**
 * Clase que encapsula los factores de riesgo analizados
 * durante la evaluación crediticia.
 * No se persiste, es solo para lógica de negocio.
 */
public class FactoresRiesgo {

    private BigDecimal ratioDeudaIngreso;
    private BigDecimal capacidadPago;
    private int puntajeAntiguedadLaboral;
    private int puntajeEdad;
    private int puntajeGarantia;
    private int puntajeMontoSolicitado;
    private boolean enlistaNegraSimulada;

    // Pesos para el cálculo del score (suma 100%)
    public static final double PESO_DEUDA = 0.30;           // 30%
    public static final double PESO_CAPACIDAD_PAGO = 0.25;  // 25%
    public static final double PESO_ANTIGUEDAD = 0.15;      // 15%
    public static final double PESO_EDAD = 0.10;            // 10%
    public static final double PESO_GARANTIA = 0.10;        // 10%
    public static final double PESO_MONTO = 0.10;           // 10%

    // Umbrales de riesgo
    public static final BigDecimal RATIO_DEUDA_BAJO = BigDecimal.valueOf(0.30);
    public static final BigDecimal RATIO_DEUDA_MEDIO = BigDecimal.valueOf(0.40);
    public static final BigDecimal RATIO_DEUDA_ALTO = BigDecimal.valueOf(0.50);

    // Rangos de score
    public static final int SCORE_MINIMO_APROBACION = 600;
    public static final int SCORE_EXCELENTE = 750;
    public static final int SCORE_BUENO = 650;
    public static final int SCORE_REGULAR = 600;

    // Constructor vacío
    public FactoresRiesgo() {
    }

    // Método para calcular el score total ponderado
    public int calcularScoreTotal() {
        double scoreBase = 0.0;
        
        // Cada factor individual está en escala 0-100
        // Los multiplicamos por su peso para obtener su contribución al score final
        
        // Factores de ratio deuda y capacidad pago (ya vienen en escala 0-100)
        double puntajeDeuda = ratioDeudaIngreso != null ? ratioDeudaIngreso.doubleValue() : 0;
        double puntajeCapacidad = capacidadPago != null ? capacidadPago.doubleValue() : 0;
        
        // Aplicar pesos a cada factor (resultado: 0-100)
        scoreBase += (puntajeDeuda * PESO_DEUDA);
        scoreBase += (puntajeCapacidad * PESO_CAPACIDAD_PAGO);
        scoreBase += (puntajeAntiguedadLaboral * PESO_ANTIGUEDAD);
        scoreBase += (puntajeEdad * PESO_EDAD);
        scoreBase += (puntajeGarantia * PESO_GARANTIA);
        scoreBase += (puntajeMontoSolicitado * PESO_MONTO);
        
        // Escalar de 0-100 a 300-850 (escala FICO bancaria)
        // Formula: 300 + (scoreBase * 5.5)
        // Donde: 0 → 300, 50 → 575, 100 → 850
        int scoreFinal = (int) Math.round(300 + (scoreBase * 5.5));
        
        // Asegurar que esté en el rango válido
        return Math.max(300, Math.min(850, scoreFinal));
    }

    // Método para determinar nivel de riesgo basado en score
    public String determinarNivelRiesgo() {
        int score = calcularScoreTotal();
        
        if (enlistaNegraSimulada) {
            return "CRITICO";
        }
        
        if (score >= SCORE_EXCELENTE) {
            return "BAJO";
        } else if (score >= SCORE_BUENO) {
            return "MEDIO";
        } else if (score >= SCORE_REGULAR) {
            return "ALTO";
        } else {
            return "CRITICO";
        }
    }

    // Getters y Setters
    public BigDecimal getRatioDeudaIngreso() {
        return ratioDeudaIngreso;
    }

    public void setRatioDeudaIngreso(BigDecimal ratioDeudaIngreso) {
        this.ratioDeudaIngreso = ratioDeudaIngreso;
    }

    public BigDecimal getCapacidadPago() {
        return capacidadPago;
    }

    public void setCapacidadPago(BigDecimal capacidadPago) {
        this.capacidadPago = capacidadPago;
    }

    public int getPuntajeAntiguedadLaboral() {
        return puntajeAntiguedadLaboral;
    }

    public void setPuntajeAntiguedadLaboral(int puntajeAntiguedadLaboral) {
        this.puntajeAntiguedadLaboral = puntajeAntiguedadLaboral;
    }

    public int getPuntajeEdad() {
        return puntajeEdad;
    }

    public void setPuntajeEdad(int puntajeEdad) {
        this.puntajeEdad = puntajeEdad;
    }

    public int getPuntajeGarantia() {
        return puntajeGarantia;
    }

    public void setPuntajeGarantia(int puntajeGarantia) {
        this.puntajeGarantia = puntajeGarantia;
    }

    public int getPuntajeMontoSolicitado() {
        return puntajeMontoSolicitado;
    }

    public void setPuntajeMontoSolicitado(int puntajeMontoSolicitado) {
        this.puntajeMontoSolicitado = puntajeMontoSolicitado;
    }

    public boolean isEnlistaNegraSimulada() {
        return enlistaNegraSimulada;
    }

    public void setEnlistaNegraSimulada(boolean enlistaNegraSimulada) {
        this.enlistaNegraSimulada = enlistaNegraSimulada;
    }
}
