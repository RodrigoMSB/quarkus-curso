package pe.banco.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PaymentRequest {
    
    @NotBlank(message = "El orderId es requerido")
    public String orderId;
    
    @NotBlank(message = "El userId es requerido")
    public String userId;
    
    @NotNull(message = "El monto es requerido")
    @Min(value = 1, message = "El monto debe ser mayor a 0")
    public Double amount;
    
    @NotBlank(message = "El método de pago es requerido")
    public String paymentMethod;
}
