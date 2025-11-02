package pe.edu.creditcore.scoring.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.*;
import lombok.*;
import pe.edu.creditcore.scoring.model.RiskLevel;
import pe.edu.creditcore.scoring.model.ScoringStrategy;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entidad para almacenar el histórico de scores crediticios calculados.
 * 
 * Usa Hibernate Reactive con Panache para operaciones asíncronas.
 * Cada cálculo de score se almacena para:
 * - Auditoría y trazabilidad
 * - Análisis de tendencias
 * - Machine Learning futuro
 */
@Entity
@Table(name = "score_history", indexes = {
    @Index(name = "idx_customer_id", columnList = "customer_id"),
    @Index(name = "idx_calculated_at", columnList = "calculated_at"),
    @Index(name = "idx_score", columnList = "score")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreHistory extends PanacheEntityBase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * ID del cliente (referencia al customer-service)
     */
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    
    /**
     * RUC del cliente (para consultas rápidas sin integración)
     */
    @Column(name = "customer_ruc", length = 11)
    private String customerRuc;
    
    /**
     * Nombre del cliente (denormalizado para performance)
     */
    @Column(name = "customer_name", length = 200)
    private String customerName;
    
    /**
     * Score calculado (0-1000)
     */
    @Column(name = "score", nullable = false)
    private Integer score;
    
    /**
     * Nivel de riesgo determinado
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;
    
    /**
     * Estrategia utilizada para el cálculo
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "strategy", nullable = false, length = 20)
    private ScoringStrategy strategy;
    
    /**
     * Monto solicitado en el análisis
     */
    @Column(name = "requested_amount", precision = 15, scale = 2)
    private Double requestedAmount;
    
    /**
     * Plazo solicitado (en meses)
     */
    @Column(name = "loan_term_months")
    private Integer loanTermMonths;
    
    /**
     * Factores que contribuyeron al score (JSON)
     */
    @Column(name = "scoring_factors", columnDefinition = "TEXT")
    private String scoringFactors;
    
    /**
     * Recomendación generada
     */
    @Column(name = "recommendation", columnDefinition = "TEXT")
    private String recommendation;
    
    /**
     * Monto máximo recomendado
     */
    @Column(name = "max_recommended_amount", precision = 15, scale = 2)
    private Double maxRecommendedAmount;
    
    /**
     * Tasa de interés sugerida (anual)
     */
    @Column(name = "suggested_interest_rate", precision = 5, scale = 2)
    private Double suggestedInterestRate;
    
    /**
     * Timestamp del cálculo
     */
    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;
    
    /**
     * Usuario que solicitó el score (del JWT)
     */
    @Column(name = "requested_by", length = 100)
    private String requestedBy;
    
    /**
     * Notas adicionales
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @PrePersist
    protected void onCreate() {
        if (calculatedAt == null) {
            calculatedAt = LocalDateTime.now();
        }
    }
    
    // ========================================================================
    // QUERIES PERSONALIZADAS CON PANACHE REACTIVE
    // ========================================================================
    
    /**
     * Buscar todos los scores de un cliente.
     */
    public static Uni<List<ScoreHistory>> findByCustomerId(Long customerId) {
        return list("customerId = ?1 order by calculatedAt desc", customerId);
    }
    
    /**
     * Buscar el score más reciente de un cliente.
     */
    public static Uni<ScoreHistory> findLatestByCustomerId(Long customerId) {
        return find("customerId = ?1 order by calculatedAt desc", customerId)
            .firstResult();
    }
    
    /**
     * Buscar scores por RUC del cliente.
     */
    public static Uni<List<ScoreHistory>> findByCustomerRuc(String ruc) {
        return list("customerRuc = ?1 order by calculatedAt desc", ruc);
    }
    
    /**
     * Buscar scores en un rango de fechas.
     */
    public static Uni<List<ScoreHistory>> findByDateRange(LocalDateTime start, LocalDateTime end) {
        return list("calculatedAt >= ?1 and calculatedAt <= ?2 order by calculatedAt desc", 
                   start, end);
    }
    
    /**
     * Buscar scores por nivel de riesgo.
     */
    public static Uni<List<ScoreHistory>> findByRiskLevel(RiskLevel riskLevel) {
        return list("riskLevel = ?1 order by calculatedAt desc", riskLevel);
    }
    
    /**
     * Buscar scores por estrategia utilizada.
     */
    public static Uni<List<ScoreHistory>> findByStrategy(ScoringStrategy strategy) {
        return list("strategy = ?1 order by calculatedAt desc", strategy);
    }
    
    /**
     * Contar scores calculados en un período.
     */
    public static Uni<Long> countByDateRange(LocalDateTime start, LocalDateTime end) {
        return count("calculatedAt >= ?1 and calculatedAt <= ?2", start, end);
    }
    
    /**
     * Obtener promedio de scores en un período.
     */
    public static Uni<Double> averageScoreByDateRange(LocalDateTime start, LocalDateTime end) {
        return find("select avg(score) from ScoreHistory where calculatedAt >= ?1 and calculatedAt <= ?2", 
                   start, end)
            .project(Double.class)
            .firstResult();
    }
}
