package pe.edu.creditcore.scoring.dto;

import lombok.*;
import pe.edu.creditcore.scoring.model.RiskLevel;
import pe.edu.creditcore.scoring.model.ScoringStrategy;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Resultado del análisis de score crediticio.
 * 
 * Contiene:
 * - Score calculado (0-1000)
 * - Nivel de riesgo
 * - Factores que contribuyeron al score
 * - Recomendación de aprobación/rechazo
 * - Condiciones sugeridas (monto máximo, tasa)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ScoreResult {
    
    /**
     * ID del historial guardado en BD.
     */
    private Long historyId;
    
    /**
     * ID del cliente analizado.
     */
    private Long customerId;
    
    /**
     * RUC del cliente.
     */
    private String customerRuc;
    
    /**
     * Nombre del cliente.
     */
    private String customerName;
    
    /**
     * Score crediticio calculado (0-1000).
     * Valores más altos indican mejor perfil crediticio.
     */
    private Integer score;
    
    /**
     * Nivel de riesgo determinado.
     */
    private RiskLevel riskLevel;
    
    /**
     * Estrategia utilizada en el cálculo.
     */
    private ScoringStrategy strategy;
    
    /**
     * Monto solicitado en el análisis.
     */
    private Double requestedAmount;
    
    /**
     * Plazo solicitado (en meses).
     */
    private Integer loanTermMonths;
    
    /**
     * Factores que contribuyeron al score.
     * 
     * Ejemplo:
     * {
     *   "incomeScore": 250,
     *   "industryScore": 237,
     *   "debtRatioScore": 200,
     *   "companyAgeScore": 180
     * }
     */
    private Map<String, Object> scoringFactors;
    
    /**
     * Recomendación de aprobación.
     * true = recomendar aprobar, false = recomendar rechazar
     */
    private Boolean approved;
    
    /**
     * Mensaje de recomendación detallada.
     */
    private String recommendation;
    
    /**
     * Monto máximo recomendado para aprobar.
     */
    private Double maxRecommendedAmount;
    
    /**
     * Tasa de interés sugerida (anual).
     * Basada en el nivel de riesgo.
     */
    private Double suggestedInterestRate;
    
    /**
     * Timestamp del cálculo.
     */
    private LocalDateTime calculatedAt;
    
    /**
     * Notas adicionales.
     */
    private String notes;
    
    /**
     * Indica si el resultado viene del cache.
     */
    private Boolean fromCache;
}
