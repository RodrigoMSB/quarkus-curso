package pe.banco.order.resource;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import pe.banco.order.dto.CreateOrderRequest;
import pe.banco.order.dto.OrderResponse;
import pe.banco.order.service.OrderService;

import java.util.List;

@Path("/api/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {

    private static final Logger LOG = Logger.getLogger(OrderResource.class);

    @Inject
    OrderService orderService;

    @POST
    public Response createOrder(@Valid CreateOrderRequest request) {
        LOG.info("🛒 Creando nueva orden para usuario: " + request.userId);
        try {
            OrderResponse response = orderService.createOrder(request);
            
            if (response.status.equals("COMPLETED")) {
                return Response.status(Response.Status.CREATED).entity(response).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity(response).build();
            }
        } catch (Exception e) {
            LOG.error("Error creando orden", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{orderId}")
    public OrderResponse getOrder(@PathParam("orderId") String orderId) {
        LOG.info("📄 Consultando orden: " + orderId);
        return orderService.getOrderById(orderId);
    }

    @GET
    @Path("/user/{userId}")
    public List<OrderResponse> getUserOrders(@PathParam("userId") String userId) {
        LOG.info("📋 Consultando órdenes del usuario: " + userId);
        return orderService.getOrdersByUser(userId);
    }

    @DELETE
    @Path("/cache/product/{productCode}")
    public Response invalidateCache(@PathParam("productCode") String productCode) {
        LOG.info("🗑️  Invalidando cache de producto: " + productCode);
        orderService.invalidateProductCache(productCode);
        return Response.ok().build();
    }

    public static class ErrorResponse {
        public String error;
        
        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}
