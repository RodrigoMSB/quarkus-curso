package pe.banco.order.dto;

import java.time.LocalDateTime;
import java.util.List;

public class OrderResponse {
    public String orderId;
    public String userId;
    public String status;
    public Double totalAmount;
    public List<OrderItemDTO> items;
    public LocalDateTime createdAt;
    public String message;
    
    public static class OrderItemDTO {
        public String productCode;
        public String productName;
        public Integer quantity;
        public Double price;
        public Double subtotal;
    }
}
