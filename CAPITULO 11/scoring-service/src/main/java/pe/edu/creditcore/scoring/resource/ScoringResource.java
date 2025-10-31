package pe.edu.creditcore.scoring.resource;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import pe.edu.creditcore.scoring.dto.ScoreRequest;
import pe.edu.creditcore.scoring.dto.ScoreResult;
import pe.edu.creditcore.scoring.entity.ScoreHistory;
import pe.edu.creditcore.scoring.service.ScoringService;

import java.util.List;
import java.util.Map;

/**
 * REST Resource para operaciones de scoring crediticio.
 * 
 * Endpoints:
 * - POST /api/scoring/calculate → Calcular score
 * - GET /api/scoring/history/{customerId} → Histórico de scores
 * - GET /api/scoring/latest/{customerId} → Último score (con cache)
 * 
 * Todos los endpoints requieren autenticación JWT.
 */
@Path("/api/scoring")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Scoring", description = "Operaciones de cálculo de score crediticio")
public class ScoringResource {
    
    @Inject
    ScoringService scoringService;
    
    /**
     * Calcula el score crediticio de un cliente.
     * 
     * Proceso:
     * 1. Valida el request
     * 2. Obtiene datos del cliente desde customer-service
     * 3. Aplica algoritmo de scoring multi-factor
     * 4. Guarda en histórico
     * 5. Retorna resultado
     * 
     * @param request Datos de la solicitud de score
     * @return Score calculado con recomendación
     */
    @POST
    @Path("/calculate")
    @RolesAllowed({"user", "admin"})
    @Operation(
        summary = "Calcular score crediticio",
        description = "Calcula el score crediticio de un cliente basado en múltiples factores: " +
                     "ingresos, industria, ratio deuda/ingreso, y antigüedad de la empresa"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Score calculado exitosamente",
            content = @Content(schema = @Schema(implementation = ScoreResult.class))
        ),
        @APIResponse(
            responseCode = "400",
            description = "Datos de entrada inválidos"
        ),
        @APIResponse(
            responseCode = "404",
            description = "Cliente no encontrado"
        ),
        @APIResponse(
            responseCode = "500",
            description = "Error interno del servidor"
        ),
        @APIResponse(
            responseCode = "503",
            description = "Customer Service no disponible"
        )
    })
    public Uni<Response> calculateScore(@Valid ScoreRequest request) {
        Log.infof("🎯 Solicitud de scoring recibida para cliente: %d", request.getCustomerId());
        
        return scoringService.calculateScore(request)
            .onItem().transform(result -> {
                Log.infof("✅ Score calculado: %d (%s) - %s", 
                         result.getScore(), 
                         result.getRiskLevel(),
                         result.getApproved() ? "APROBADO" : "RECHAZADO");
                return Response.ok(result).build();
            })
            .onFailure().recoverWithItem(err -> {
                Log.errorf(err, "❌ Error calculando score");
                
                if (err.getMessage().contains("Customer Service no disponible")) {
                    return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity(Map.of(
                            "error", "Customer Service no disponible",
                            "message", err.getMessage(),
                            "timestamp", java.time.LocalDateTime.now()
                        ))
                        .build();
                }
                
                if (err.getMessage().contains("not found") || err.getMessage().contains("404")) {
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of(
                            "error", "Cliente no encontrado",
                            "customerId", request.getCustomerId(),
                            "timestamp", java.time.LocalDateTime.now()
                        ))
                        .build();
                }
                
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                        "error", "Error calculando score",
                        "message", err.getMessage(),
                        "timestamp", java.time.LocalDateTime.now()
                    ))
                    .build();
            });
    }
    
    /**
     * Obtiene el histórico completo de scores de un cliente.
     * 
     * @param customerId ID del cliente
     * @return Lista de scores históricos ordenados por fecha descendente
     */
    @GET
    @Path("/history/{customerId}")
    @RolesAllowed({"user", "admin"})
    @Operation(
        summary = "Obtener histórico de scores",
        description = "Retorna todos los scores calculados para un cliente, ordenados por fecha descendente"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Histórico obtenido exitosamente",
            content = @Content(schema = @Schema(implementation = ScoreHistory.class))
        ),
        @APIResponse(
            responseCode = "404",
            description = "Cliente no tiene scores calculados"
        )
    })
    public Uni<Response> getScoreHistory(@PathParam("customerId") Long customerId) {
        Log.infof("📜 Consultando histórico de scores para cliente: %d", customerId);
        
        return scoringService.getScoreHistory(customerId)
            .onItem().transform(history -> {
                if (history.isEmpty()) {
                    Log.infof("ℹ️ No hay histórico para cliente: %d", customerId);
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of(
                            "message", "No hay scores calculados para este cliente",
                            "customerId", customerId
                        ))
                        .build();
                }
                
                Log.infof("✅ Encontrados %d scores en histórico", history.size());
                return Response.ok(history).build();
            })
            .onFailure().recoverWithItem(err -> {
                Log.errorf(err, "❌ Error obteniendo histórico");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                        "error", "Error obteniendo histórico",
                        "message", err.getMessage()
                    ))
                    .build();
            });
    }
    
    /**
     * Obtiene el score más reciente de un cliente.
     * 
     * Este endpoint utiliza cache para mejorar performance.
     * 
     * @param customerId ID del cliente
     * @return Último score calculado
     */
    @GET
    @Path("/latest/{customerId}")
    @RolesAllowed({"user", "admin"})
    @Operation(
        summary = "Obtener último score",
        description = "Retorna el score más reciente del cliente (con cache de 30 minutos)"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Score obtenido exitosamente",
            content = @Content(schema = @Schema(implementation = ScoreHistory.class))
        ),
        @APIResponse(
            responseCode = "404",
            description = "Cliente no tiene scores calculados"
        )
    })
    public Uni<Response> getLatestScore(@PathParam("customerId") Long customerId) {
        Log.infof("🔍 Buscando último score para cliente: %d", customerId);
        
        return scoringService.getLatestScore(customerId)
            .onItem().ifNotNull().transform(score -> {
                Log.infof("✅ Score encontrado: %d (%s)", score.getScore(), score.getRiskLevel());
                return Response.ok(score)
                    .header("X-Cache-Status", "HIT")
                    .build();
            })
            .onItem().ifNull().continueWith(() -> {
                Log.infof("ℹ️ No hay score para cliente: %d", customerId);
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                        "message", "No hay scores calculados para este cliente",
                        "customerId", customerId
                    ))
                    .build();
            })
            .onFailure().recoverWithItem(err -> {
                Log.errorf(err, "❌ Error obteniendo último score");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                        "error", "Error obteniendo score",
                        "message", err.getMessage()
                    ))
                    .build();
            });
    }
    
    /**
     * Health check del servicio de scoring.
     */
    @GET
    @Path("/health")
    @PermitAll
    @Operation(summary = "Health check", description = "Verifica que el servicio esté funcionando")
    public Uni<Response> health() {
        return Uni.createFrom().item(
            Response.ok(Map.of(
                "service", "scoring-service",
                "status", "UP",
                "timestamp", java.time.LocalDateTime.now()
            )).build()
        );
    }
}
