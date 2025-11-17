package pe.banco.inventory.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import pe.banco.inventory.dto.ProductDTO;
import pe.banco.inventory.dto.ReservationRequest;
import pe.banco.inventory.dto.ReservationResponse;
import pe.banco.inventory.service.InventoryService;

import java.util.List;

@Path("/api/inventory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InventoryResource {

    private static final Logger LOG = Logger.getLogger(InventoryResource.class);

    @Inject
    InventoryService inventoryService;

    @GET
    @Path("/products")
    public List<ProductDTO> getAllProducts() {
        LOG.info("📦 Consultando todos los productos");
        return inventoryService.getAllProducts();
    }

    @GET
    @Path("/products/{productCode}")
    public ProductDTO getProduct(@PathParam("productCode") String productCode) {
        LOG.info("📦 Consultando producto: " + productCode);
        return inventoryService.getProductByCode(productCode);
    }

    @POST
    @Path("/reserve")
    public Response reserveStock(ReservationRequest request) {
        LOG.info("🔒 Reservando stock para orden: " + request.orderId);
        ReservationResponse response = inventoryService.reserveStock(request);
        
        if (response.success) {
            return Response.ok(response).build();
        } else {
            return Response.status(Response.Status.CONFLICT).entity(response).build();
        }
    }

    @POST
    @Path("/confirm/{orderId}")
    public Response confirmReservation(@PathParam("orderId") String orderId, ConfirmRequest request) {
        LOG.info("✅ Confirmando reserva para orden: " + orderId);
        inventoryService.confirmReservation(orderId, request.productCode, request.quantity);
        return Response.ok().build();
    }

    @POST
    @Path("/cancel/{orderId}")
    public Response cancelReservation(@PathParam("orderId") String orderId, CancelRequest request) {
        LOG.info("❌ Cancelando reserva para orden: " + orderId);
        inventoryService.cancelReservation(orderId, request.productCode, request.quantity);
        return Response.ok().build();
    }

    public static class ConfirmRequest {
        public String productCode;
        public Integer quantity;
    }

    public static class CancelRequest {
        public String productCode;
        public Integer quantity;
    }
}
