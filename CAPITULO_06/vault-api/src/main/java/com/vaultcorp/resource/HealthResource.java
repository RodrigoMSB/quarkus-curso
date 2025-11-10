package com.vaultcorp.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Endpoint de verificación de estado del servicio (health check).
 * <p>
 * Proporciona un endpoint público simple para verificar que la aplicación
 * está activa y respondiendo correctamente. Este tipo de endpoint es fundamental
 * para monitoreo, balanceadores de carga y sistemas de orquestación.
 * </p>
 * 
 * <p>Es análogo a una luz indicadora en un servidor: permite verificar rápidamente
 * si el sistema está encendido y funcionando sin necesidad de autenticación.</p>
 * 
 * <p><b>Uso típico:</b></p>
 * <ul>
 *   <li>Kubernetes liveness/readiness probes</li>
 *   <li>Load balancer health checks</li>
 *   <li>Monitoreo de disponibilidad</li>
 * </ul>
 * 
 * @author VaultCorp Development Team
 * @since 1.0
 */
@Path("/health")
public class HealthResource {

    /**
     * Endpoint simple de verificación de estado.
     * <p>
     * Retorna un mensaje de texto plano indicando que el servicio está activo.
     * Este endpoint NO requiere autenticación y está disponible públicamente.
     * </p>
     * 
     * @return mensaje de confirmación de que el servicio está operativo
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Quarkus REST";
    }
}
