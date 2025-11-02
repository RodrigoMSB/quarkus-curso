package pe.banco.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ReservationRequest {
    
    @NotBlank(message = "El orderId es requerido")
    public String orderId;
    
    @NotBlank(message = "El productCode es requerido")
    public String productCode;
    
    @NotNull(message = "La cantidad es requerida")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    public Integer quantity;
}
