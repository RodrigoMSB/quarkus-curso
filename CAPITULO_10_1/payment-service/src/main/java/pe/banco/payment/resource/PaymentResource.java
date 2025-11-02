package pe.banco.payment.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import pe.banco.payment.dto.PaymentRequest;
import pe.banco.payment.dto.PaymentResponse;
import pe.banco.payment.service.PaymentService;

@Path("/api/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaymentResource {

    private static final Logger LOG = Logger.getLogger(PaymentResource.class);

    @Inject
    PaymentService paymentService;

    @POST
    @Path("/process")
    public Response processPayment(PaymentRequest request) {
        LOG.info("💳 Procesando pago para orden: " + request.orderId);
        PaymentResponse response = paymentService.processPayment(request);
        
        if (response.success) {
            return Response.ok(response).build();
        } else {
            return Response.status(Response.Status.PAYMENT_REQUIRED).entity(response).build();
        }
    }

    @POST
    @Path("/refund/{orderId}")
    public Response refundPayment(@PathParam("orderId") String orderId) {
        LOG.info("↩️  Reembolsando pago para orden: " + orderId);
        paymentService.refundPayment(orderId);
        return Response.ok().build();
    }
}
