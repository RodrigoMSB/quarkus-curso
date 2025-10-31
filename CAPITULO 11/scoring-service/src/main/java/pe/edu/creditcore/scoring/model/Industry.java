package pe.edu.creditcore.scoring.model;

/**
 * Industrias con sus factores de riesgo asociados.
 * 
 * El factor de riesgo va de 0.0 (más riesgoso) a 1.0 (menos riesgoso).
 * Este factor afecta directamente el cálculo del score crediticio.
 * 
 * Basado en análisis de morosidad por sector en el sistema financiero peruano.
 */
public enum Industry {
    TECHNOLOGY(0.95, "Tecnología - Alta estabilidad"),
    HEALTHCARE(0.90, "Salud - Demanda constante"),
    EDUCATION(0.85, "Educación - Sector estable"),
    FINANCE(0.80, "Finanzas - Regulado y sólido"),
    MANUFACTURING(0.75, "Manufactura - Ciclos económicos"),
    RETAIL(0.70, "Retail - Competencia intensa"),
    CONSTRUCTION(0.65, "Construcción - Alta volatilidad"),
    HOSPITALITY(0.60, "Hospitalidad - Estacional"),
    AGRICULTURE(0.55, "Agricultura - Clima dependiente"),
    MINING(0.50, "Minería - Commodity dependiente"),
    OTHER(0.70, "Otros - Riesgo promedio");
    
    private final double riskFactor;
    private final String description;
    
    Industry(double riskFactor, String description) {
        this.riskFactor = riskFactor;
        this.description = description;
    }
    
    /**
     * Factor de riesgo de la industria (0.0-1.0).
     * Valores más altos indican menor riesgo.
     */
    public double getRiskFactor() {
        return riskFactor;
    }
    
    public String getDescription() {
        return description;
    }
}
