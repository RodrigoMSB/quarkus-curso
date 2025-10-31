package pe.edu.creditcore.scoring.model;

/**
 * Estrategias de scoring crediticio.
 * 
 * Cada estrategia representa un perfil de análisis diferente:
 * 
 * - CONSERVATIVE: Banca tradicional
 *   Requisitos estrictos, aprueba solo los mejores perfiles
 *   
 * - BALANCED: Enfoque equilibrado
 *   Balance entre riesgo y oportunidad
 *   
 * - AGGRESSIVE: Fintech, microcrédito
 *   Mayor tolerancia al riesgo, inclusión financiera
 */
public enum ScoringStrategy {
    CONSERVATIVE(
        "Conservador",
        "Banca tradicional - Requisitos estrictos",
        0.85,  // multiplier para ajustar score final
        700    // score mínimo recomendado para aprobación
    ),
    BALANCED(
        "Equilibrado",
        "Enfoque balanceado - Riesgo moderado",
        1.0,   // sin ajuste
        550    // score mínimo recomendado
    ),
    AGGRESSIVE(
        "Agresivo",
        "Fintech - Mayor tolerancia al riesgo",
        1.15,  // bonus en el score
        400    // score mínimo más bajo
    );
    
    private final String displayName;
    private final String description;
    private final double scoreMultiplier;
    private final int minimumApprovalScore;
    
    ScoringStrategy(String displayName, String description, 
                    double scoreMultiplier, int minimumApprovalScore) {
        this.displayName = displayName;
        this.description = description;
        this.scoreMultiplier = scoreMultiplier;
        this.minimumApprovalScore = minimumApprovalScore;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public double getScoreMultiplier() {
        return scoreMultiplier;
    }
    
    public int getMinimumApprovalScore() {
        return minimumApprovalScore;
    }
}
