package pe.edu.creditcore.scoring.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import pe.edu.creditcore.scoring.model.ScoringStrategy;

/**
 * Request para calcular el score crediticio de un cliente.
 * 
 * Incluye validaciones para garantizar datos correctos:
 * - Customer ID válido
 * - Monto solicitado positivo
 * - Plazo razonable (1-360 meses = 30 años máximo)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ScoreRequest {
    
    /**
     * ID del cliente en el sistema.
     * El servicio consultará customer-service para obtener los datos.
     */
    @NotNull(message = "El ID del cliente es obligatorio")
    @Positive(message = "El ID del cliente debe ser positivo")
    private Long customerId;
    
    /**
     * Monto del crédito solicitado (en soles).
     */
    @NotNull(message = "El monto solicitado es obligatorio")
    @DecimalMin(value = "1000.0", message = "El monto mínimo es S/ 1,000")
    @DecimalMax(value = "10000000.0", message = "El monto máximo es S/ 10,000,000")
    private Double requestedAmount;
    
    /**
     * Plazo del préstamo en meses.
     */
    @NotNull(message = "El plazo es obligatorio")
    @Min(value = 1, message = "El plazo mínimo es 1 mes")
    @Max(value = 360, message = "El plazo máximo es 360 meses")
    private Integer loanTermMonths;
    
    /**
     * Estrategia de scoring a utilizar.
     * Si no se especifica, se usa BALANCED por defecto.
     */
    private ScoringStrategy strategy;
    
    /**
     * Notas adicionales sobre la solicitud.
     */
    @Size(max = 500, message = "Las notas no pueden exceder 500 caracteres")
    private String notes;
    
    /**
     * Devuelve la estrategia o BALANCED si es null.
     */
    public ScoringStrategy getStrategyOrDefault() {
        return strategy != null ? strategy : ScoringStrategy.BALANCED;
    }
}
