package pe.banco.payment.dto;

public class PaymentResponse {
    public boolean success;
    public String message;
    public String orderId;
    public String transactionId;
    public Double amount;
    public String status;
    
    public static PaymentResponse success(String orderId, String transactionId, Double amount) {
        PaymentResponse response = new PaymentResponse();
        response.success = true;
        response.message = "Pago procesado exitosamente";
        response.orderId = orderId;
        response.transactionId = transactionId;
        response.amount = amount;
        response.status = "COMPLETED";
        return response;
    }
    
    public static PaymentResponse failure(String orderId, String message) {
        PaymentResponse response = new PaymentResponse();
        response.success = false;
        response.message = message;
        response.orderId = orderId;
        response.status = "FAILED";
        return response;
    }
}
