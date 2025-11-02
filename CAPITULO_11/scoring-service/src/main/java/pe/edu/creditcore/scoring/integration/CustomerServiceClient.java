package pe.edu.creditcore.scoring.integration;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import pe.edu.creditcore.scoring.dto.CustomerData;

/**
 * REST Client Reactivo para integración con customer-service.
 * 
 * Características:
 * - Programación reactiva con Mutiny (Uni)
 * - Circuit Breaker para evitar cascadas de fallos
 * - Retry automático con backoff
 * - Timeout para evitar cuelgues
 * - Fallback cuando customer-service no está disponible
 * 
 * El token JWT se propaga automáticamente mediante la configuración
 * en application.properties.
 */
@Path("/api/customers")
@RegisterRestClient(configKey = "pe.edu.creditcore.scoring.integration.CustomerServiceClient")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface CustomerServiceClient {
    
    /**
     * Obtener datos de un cliente por ID.
     * 
     * Fault Tolerance:
     * - Timeout: 10 segundos máximo
     * - Retry: 3 intentos con delay creciente (1s, 2s, 4s)
     * - Circuit Breaker: se abre tras 5 fallos consecutivos
     * - Fallback: lanza CustomerServiceException con mensaje claro
     * 
     * @param customerId ID del cliente
     * @return Datos del cliente (reactivo)
     */
    @GET
    @Path("/{id}")
    @Timeout(10000) // 10 segundos
    @Retry(
        maxRetries = 3,
        delay = 1000,      // 1 segundo inicial
        delayUnit = java.util.concurrent.TimeUnit.MILLISECONDS,
        jitter = 200,      // variación aleatoria ±200ms
        maxDuration = 15000 // máximo 15s total
    )
    @CircuitBreaker(
        requestVolumeThreshold = 5,     // Mínimo 5 requests para evaluar
        failureRatio = 0.5,              // 50% de fallos abre el circuito
        delay = 5000,                    // 5s antes de intentar cerrar
        successThreshold = 2             // 2 éxitos consecutivos para cerrar
    )
    @Fallback(fallbackMethod = "getCustomerFallback")
    Uni<CustomerData> getCustomerById(@PathParam("id") Long customerId);
    
    /**
     * Fallback cuando customer-service no responde.
     * 
     * Lanza una excepción con contexto claro del problema.
     * En producción, podría:
     * - Consultar un cache local
     * - Retornar datos parciales
     * - Registrar el fallo en monitoring
     */
    default Uni<CustomerData> getCustomerFallback(Long customerId) {
        return Uni.createFrom().failure(
            new CustomerServiceException(
                String.format(
                    "Customer Service no disponible. No se pudo obtener datos del cliente %d. " +
                    "Posibles causas: servicio caído, timeout, o circuito abierto.",
                    customerId
                )
            )
        );
    }
    
    /**
     * Excepción personalizada para problemas de integración.
     */
    class CustomerServiceException extends RuntimeException {
        public CustomerServiceException(String message) {
            super(message);
        }
        
        public CustomerServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
