package pe.banco.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.banco.customer.entity.CustomerStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO para respuestas de consultas de clientes
 * 
 * IMPORTANTE: No expone campos sensibles sin cifrar (RUC se devuelve parcialmente enmascarado)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {

    private Long id;

    /**
     * RUC enmascarado para seguridad: XXXXXXXXX99 (solo últimos 2 dígitos visibles)
     */
    private String rucMasked;

    private String legalName;
    private String tradeName;
    private String industry;
    private LocalDate foundedDate;
    private BigDecimal annualRevenue;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private String city;
    private CustomerStatus status;
    private Integer creditScore;
    private String riskCategory;
    private Boolean sunatValidated;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;

    /**
     * Indicadores de riesgo calculados
     */
    private Boolean isHighRisk;
    private Boolean isPremium;

    /**
     * Método helper para enmascarar RUC
     * Ejemplo: 20123456789 -> XXXXXXXXX89
     */
    public static String maskRuc(String ruc) {
        if (ruc == null || ruc.length() < 11) {
            return "***********";
        }
        return "XXXXXXXXX" + ruc.substring(9);
    }
}
