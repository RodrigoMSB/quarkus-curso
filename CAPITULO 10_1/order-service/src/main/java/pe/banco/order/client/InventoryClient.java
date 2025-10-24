package pe.banco.order.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import pe.banco.order.dto.ProductInfoDTO;

@Path("/api/inventory")
@RegisterRestClient(configKey = "inventory-api")
public interface InventoryClient {

    @GET
    @Path("/products/{productCode}")
    @Produces(MediaType.APPLICATION_JSON)
    ProductInfoDTO getProduct(@PathParam("productCode") String productCode);

    @POST
    @Path("/reserve")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    ReservationResponse reserveStock(ReservationRequest request);

    @POST
    @Path("/confirm/{orderId}")
    @Consumes(MediaType.APPLICATION_JSON)
    void confirmReservation(@PathParam("orderId") String orderId, ConfirmRequest request);

    @POST
    @Path("/cancel/{orderId}")
    @Consumes(MediaType.APPLICATION_JSON)
    void cancelReservation(@PathParam("orderId") String orderId, CancelRequest request);

    class ReservationRequest {
        public String orderId;
        public String productCode;
        public Integer quantity;
    }

    class ReservationResponse {
        public boolean success;
        public String message;
        public String orderId;
        public String productCode;
        public Integer quantityReserved;
    }

    class ConfirmRequest {
        public String productCode;
        public Integer quantity;
    }

    class CancelRequest {
        public String productCode;
        public Integer quantity;
    }
}
