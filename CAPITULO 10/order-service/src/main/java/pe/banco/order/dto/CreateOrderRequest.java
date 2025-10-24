package pe.banco.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class CreateOrderRequest {
    
    @NotBlank(message = "El userId es requerido")
    public String userId;
    
    @NotEmpty(message = "Debe incluir al menos un item")
    public List<OrderItemRequest> items;
    
    @NotBlank(message = "El método de pago es requerido")
    public String paymentMethod;
    
    public static class OrderItemRequest {
        @NotBlank(message = "El productCode es requerido")
        public String productCode;
        
        @NotNull(message = "La cantidad es requerida")
        public Integer quantity;
    }
}
