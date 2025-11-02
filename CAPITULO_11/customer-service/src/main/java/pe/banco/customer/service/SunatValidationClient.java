package pe.banco.customer.service;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST Client para validación de RUC con SUNAT
 * 
 * Demuestra:
 * - Consumo de APIs REST con REST Client (Capítulo 8)
 * - Integración con servicios externos
 * 
 * NOTA: En producción, esto se conectaría a la API real de SUNAT
 * Para el ejercicio, usaremos un mock local
 */
@Path("/api/sunat")
@RegisterRestClient(configKey = "sunat-api")
public interface SunatValidationClient {

    /**
     * Valida si un RUC existe y está activo en SUNAT
     * 
     * @param ruc RUC a validar (11 dígitos)
     * @return Información del contribuyente
     */
    @GET
    @Path("/validate/{ruc}")
    @Produces(MediaType.APPLICATION_JSON)
    SunatResponse validateRuc(@PathParam("ruc") String ruc);

    /**
     * DTO de respuesta de SUNAT (simplificado)
     */
    record SunatResponse(
        String ruc,
        String legalName,
        String status,  // ACTIVE, INACTIVE, SUSPENDED
        String address,
        String district,
        String department,
        boolean valid
    ) {}
}
