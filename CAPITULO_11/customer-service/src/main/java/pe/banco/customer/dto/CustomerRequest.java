package pe.banco.customer.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para crear y actualizar clientes
 * Demuestra Bean Validation (Capítulo 5)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequest {

    @NotNull(message = "El RUC es obligatorio")
    @Pattern(regexp = "^[0-9]{11}$", message = "El RUC debe tener exactamente 11 dígitos")
    private String ruc;

    @NotBlank(message = "La razón social es obligatoria")
    @Size(min = 3, max = 200, message = "La razón social debe tener entre 3 y 200 caracteres")
    private String legalName;

    @Size(max = 200, message = "El nombre comercial no puede exceder 200 caracteres")
    private String tradeName;

    @NotBlank(message = "El sector industrial es obligatorio")
    private String industry; // RETAIL, TECHNOLOGY, MANUFACTURING, SERVICES, etc.

    @PastOrPresent(message = "La fecha de constitución no puede ser futura")
    private LocalDate foundedDate;

    @Positive(message = "Los ingresos anuales deben ser positivos")
    private BigDecimal annualRevenue;

    @NotBlank(message = "El email de contacto es obligatorio")
    @Email(message = "El email no tiene formato válido")
    private String contactEmail;

    @Pattern(regexp = "^[+]?[0-9]{9,15}$", message = "El teléfono debe tener entre 9 y 15 dígitos")
    private String contactPhone;

    @Size(max = 500, message = "La dirección no puede exceder 500 caracteres")
    private String address;

    @Size(max = 100, message = "La ciudad no puede exceder 100 caracteres")
    private String city;
}
