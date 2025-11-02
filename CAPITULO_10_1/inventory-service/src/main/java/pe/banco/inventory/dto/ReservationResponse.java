package pe.banco.inventory.dto;

public class ReservationResponse {
    public boolean success;
    public String message;
    public String orderId;
    public String productCode;
    public Integer quantityReserved;
    
    public static ReservationResponse success(String orderId, String productCode, Integer quantity) {
        ReservationResponse response = new ReservationResponse();
        response.success = true;
        response.message = "Reserva exitosa";
        response.orderId = orderId;
        response.productCode = productCode;
        response.quantityReserved = quantity;
        return response;
    }
    
    public static ReservationResponse failure(String orderId, String message) {
        ReservationResponse response = new ReservationResponse();
        response.success = false;
        response.message = message;
        response.orderId = orderId;
        return response;
    }
}
