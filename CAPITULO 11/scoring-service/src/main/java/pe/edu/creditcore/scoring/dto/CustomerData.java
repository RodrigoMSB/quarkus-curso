package pe.edu.creditcore.scoring.dto;

import lombok.*;
import pe.edu.creditcore.scoring.model.Industry;

import java.time.LocalDate;

/**
 * DTO para representar los datos del cliente obtenidos desde customer-service.
 * 
 * Mapea la respuesta del REST Client.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CustomerData {
    
    private Long id;
    private String ruc;
    private String rucMasked;
    private String legalName;
    private String tradeName;
    private Industry industry;
    private LocalDate foundedDate;
    private Double annualRevenue;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private String city;
    private String district;
    private String province;
    private String department;
    private String country;
    private String website;
    private Boolean active;
    
    /**
     * Calcula la antigüedad de la empresa en años.
     */
    public int getCompanyAgeInYears() {
        if (foundedDate == null) {
            return 0;
        }
        return LocalDate.now().getYear() - foundedDate.getYear();
    }
    
    /**
     * Retorna el nombre para mostrar (tradeName o legalName).
     */
    public String getDisplayName() {
        return tradeName != null && !tradeName.isBlank() 
            ? tradeName 
            : legalName;
    }
}
