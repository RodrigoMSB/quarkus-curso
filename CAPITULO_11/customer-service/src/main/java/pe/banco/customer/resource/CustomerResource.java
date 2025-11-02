package pe.banco.customer.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import pe.banco.customer.dto.CustomerRequest;
import pe.banco.customer.dto.CustomerResponse;
import pe.banco.customer.service.CustomerService;

import java.util.List;

/**
 * REST Resource para gestión de clientes empresariales
 * 
 * Demuestra:
 * - RESTEasy Reactive (Capítulo 3)
 * - Bean Validation en endpoints (Capítulo 5)
 * - Seguridad con JWT y OIDC (Capítulo 6)
 * - Roles y autorización (Capítulo 6)
 * - OpenAPI documentation (Capítulo 2)
 */
@Path("/api/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Customers", description = "Gestión de clientes empresariales")
public class CustomerResource {

    private static final Logger LOG = Logger.getLogger(CustomerResource.class);

    @Inject
    CustomerService customerService;

    @Inject
    JsonWebToken jwt;

    /**
     * Crear nuevo cliente
     * 
     * Requiere rol: ANALYST o ADMIN
     */
    @POST
    @RolesAllowed({"ANALYST", "ADMIN"})
    @Operation(
        summary = "Crear nuevo cliente",
        description = "Crea un nuevo cliente empresarial con validación de RUC en SUNAT"
    )
    @SecurityRequirement(name = "bearer-jwt")
    @APIResponse(responseCode = "201", description = "Cliente creado exitosamente")
    @APIResponse(responseCode = "400", description = "Datos inválidos")
    @APIResponse(responseCode = "401", description = "No autenticado")
    @APIResponse(responseCode = "403", description = "Sin permisos")
    public Response createCustomer(@Valid CustomerRequest request) {
        LOG.infof("📝 POST /api/customers - Usuario: %s", jwt.getName());
        
        String username = jwt.getName();
        CustomerResponse response = customerService.createCustomer(request, username);
        
        return Response
            .status(Response.Status.CREATED)
            .entity(response)
            .build();
    }

    /**
     * Obtener cliente por ID
     * 
     * Requiere autenticación (cualquier rol)
     */
    @GET
    @Path("/{id}")
    @RolesAllowed({"CUSTOMER", "ANALYST", "APPROVER", "ADMIN"})
    @Operation(summary = "Obtener cliente por ID")
    @SecurityRequirement(name = "bearer-jwt")
    @APIResponse(
        responseCode = "200",
        description = "Cliente encontrado",
        content = @Content(schema = @Schema(implementation = CustomerResponse.class))
    )
    @APIResponse(responseCode = "404", description = "Cliente no encontrado")
    public Response getCustomer(@PathParam("id") Long id) {
        LOG.infof("🔍 GET /api/customers/%d", id);
        
        CustomerResponse response = customerService.getCustomer(id);
        return Response.ok(response).build();
    }

    /**
     * Buscar cliente por RUC
     * 
     * Requiere rol: ANALYST o ADMIN
     */
    @GET
    @Path("/ruc/{ruc}")
    @RolesAllowed({"ANALYST", "APPROVER", "ADMIN"})
    @Operation(
        summary = "Buscar cliente por RUC",
        description = "Busca un cliente usando su RUC (11 dígitos)"
    )
    @SecurityRequirement(name = "bearer-jwt")
    @APIResponse(responseCode = "200", description = "Cliente encontrado")
    @APIResponse(responseCode = "404", description = "Cliente no encontrado")
    public Response getCustomerByRuc(@PathParam("ruc") String ruc) {
        LOG.infof("🔍 GET /api/customers/ruc/%s", ruc);
        
        CustomerResponse response = customerService.getCustomerByRuc(ruc);
        return Response.ok(response).build();
    }

    /**
     * Actualizar cliente
     * 
     * Requiere rol: ANALYST o ADMIN
     */
    @PUT
    @Path("/{id}")
    @RolesAllowed({"ANALYST", "ADMIN"})
    @Operation(summary = "Actualizar datos del cliente")
    @SecurityRequirement(name = "bearer-jwt")
    @APIResponse(responseCode = "200", description = "Cliente actualizado")
    @APIResponse(responseCode = "404", description = "Cliente no encontrado")
    public Response updateCustomer(
        @PathParam("id") Long id,
        @Valid CustomerRequest request
    ) {
        LOG.infof("📝 PUT /api/customers/%d", id);
        
        CustomerResponse response = customerService.updateCustomer(id, request);
        return Response.ok(response).build();
    }

    /**
     * Listar clientes activos
     * 
     * Acceso público para demostración (en producción requeriría autenticación)
     */
    @GET
    @Operation(
        summary = "Listar clientes activos",
        description = "Retorna todos los clientes con estado ACTIVE"
    )
    @APIResponse(responseCode = "200", description = "Lista de clientes")
    public Response listActiveCustomers() {
        LOG.info("📋 GET /api/customers");
        
        List<CustomerResponse> customers = customerService.listActiveCustomers();
        return Response.ok(customers).build();
    }

    /**
     * Listar clientes por industria
     */
    @GET
    @Path("/industry/{industry}")
    @RolesAllowed({"ANALYST", "APPROVER", "ADMIN"})
    @Operation(
        summary = "Listar clientes por industria",
        description = "Filtra clientes por sector industrial (RETAIL, TECHNOLOGY, etc.)"
    )
    @SecurityRequirement(name = "bearer-jwt")
    @APIResponse(responseCode = "200", description = "Lista de clientes")
    public Response listByIndustry(@PathParam("industry") String industry) {
        LOG.infof("📋 GET /api/customers/industry/%s", industry);
        
        List<CustomerResponse> customers = customerService.listByIndustry(industry);
        return Response.ok(customers).build();
    }

    /**
     * Obtener score crediticio histórico
     * 
     * Endpoint interno llamado por otros microservicios
     */
    @GET
    @Path("/{id}/credit-score")
    @RolesAllowed({"ANALYST", "APPROVER", "ADMIN", "SYSTEM"})
    @Operation(
        summary = "Obtener score crediticio del cliente",
        description = "Retorna el score y categoría de riesgo históricos"
    )
    @SecurityRequirement(name = "bearer-jwt")
    public Response getCreditScore(@PathParam("id") Long id) {
        LOG.infof("📊 GET /api/customers/%d/credit-score", id);
        
        CustomerResponse customer = customerService.getCustomer(id);
        
        var scoreData = new CreditScoreResponse(
            customer.getCreditScore(),
            customer.getRiskCategory(),
            customer.getIsHighRisk(),
            customer.getIsPremium()
        );
        
        return Response.ok(scoreData).build();
    }

    /**
     * Actualizar score (endpoint interno)
     * 
     * Llamado por scoring-service después de calcular el riesgo
     */
    @PUT
    @Path("/{id}/credit-score")
    @RolesAllowed({"SYSTEM", "ADMIN"})
    @Operation(
        summary = "Actualizar score crediticio",
        description = "Endpoint interno para actualizar score calculado por scoring-service"
    )
    @SecurityRequirement(name = "bearer-jwt")
    public Response updateCreditScore(
        @PathParam("id") Long id,
        UpdateScoreRequest request
    ) {
        LOG.infof("📊 PUT /api/customers/%d/credit-score", id);
        
        customerService.updateCreditScore(id, request.score, request.riskCategory);
        return Response.noContent().build();
    }

    /**
     * Health check endpoint
     */
    @GET
    @Path("/health")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Health check", description = "Verifica que el servicio esté operativo")
    public String health() {
        return "Customer Service is UP";
    }

    // ==========================================
    // DTOs internos
    // ==========================================

    public record CreditScoreResponse(
        Integer score,
        String riskCategory,
        Boolean isHighRisk,
        Boolean isPremium
    ) {}

    public record UpdateScoreRequest(
        Integer score,
        String riskCategory
    ) {}
}
