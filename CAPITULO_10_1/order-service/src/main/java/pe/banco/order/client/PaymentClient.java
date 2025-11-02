package pe.banco.order.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/api/payments")
@RegisterRestClient(configKey = "payment-api")
public interface PaymentClient {

    @POST
    @Path("/process")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    PaymentResponse processPayment(PaymentRequest request);

    @POST
    @Path("/refund/{orderId}")
    void refundPayment(@PathParam("orderId") String orderId);

    class PaymentRequest {
        public String orderId;
        public String userId;
        public Double amount;
        public String paymentMethod;
    }

    class PaymentResponse {
        public boolean success;
        public String message;
        public String orderId;
        public String transactionId;
        public Double amount;
        public String status;
    }
}
