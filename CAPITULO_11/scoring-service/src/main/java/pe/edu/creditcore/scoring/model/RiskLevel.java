package pe.edu.creditcore.scoring.model;

/**
 * Niveles de riesgo crediticio basados en el score calculado.
 * 
 * Clasificación:
 * - EXCELLENT: 800-1000 → Riesgo muy bajo, mejores tasas
 * - GOOD: 650-799 → Riesgo bajo, tasas competitivas
 * - FAIR: 500-649 → Riesgo moderado, tasas estándar
 * - POOR: 350-499 → Riesgo alto, tasas elevadas
 * - VERY_POOR: 0-349 → Riesgo muy alto, posible rechazo
 */
public enum RiskLevel {
    EXCELLENT("Excelente", "Riesgo muy bajo"),
    GOOD("Bueno", "Riesgo bajo"),
    FAIR("Regular", "Riesgo moderado"),
    POOR("Malo", "Riesgo alto"),
    VERY_POOR("Muy Malo", "Riesgo muy alto");
    
    private final String displayName;
    private final String description;
    
    RiskLevel(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Determina el nivel de riesgo basado en el score.
     * 
     * @param score Puntuación crediticia (0-1000)
     * @return Nivel de riesgo correspondiente
     */
    public static RiskLevel fromScore(int score) {
        if (score >= 800) return EXCELLENT;
        if (score >= 650) return GOOD;
        if (score >= 500) return FAIR;
        if (score >= 350) return POOR;
        return VERY_POOR;
    }
}
