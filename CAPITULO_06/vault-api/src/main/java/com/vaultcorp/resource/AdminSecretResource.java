package com.vaultcorp.resource;

import com.vaultcorp.model.Secret;
import com.vaultcorp.security.Roles;
import com.vaultcorp.service.SecretService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

/**
 * Endpoints administrativos para gestión de secretos (Parte 1 del curso).
 * <p>
 * Proporciona operaciones privilegiadas de administración y auditoría que requieren
 * autenticación con Basic Auth y roles administrativos específicos. Estos endpoints
 * representan funciones críticas del sistema con acceso altamente restringido.
 * </p>
 * 
 * <p>Es análogo al panel de control maestro de una bóveda bancaria: solo personal
 * autorizado con credenciales administrativas puede acceder a funciones como
 * visualizar todos los depósitos o eliminar registros.</p>
 * 
 * <p><b>Niveles de acceso requeridos:</b></p>
 * <ul>
 *   <li><b>vault-admin:</b> Acceso completo (listar, eliminar, estadísticas)</li>
 *   <li><b>vault-auditor:</b> Acceso de solo lectura (estadísticas)</li>
 *   <li><b>Público:</b> Solo health check</li>
 * </ul>
 * 
 * <p><b>Autenticación requerida:</b></p>
 * <pre>
 * Authorization: Basic base64(username:password)
 * Ejemplo: admin:admin123 → Authorization: Basic YWRtaW46YWRtaW4xMjM=
 * </pre>
 * 
 * @author VaultCorp Development Team
 * @since 1.0
 */
@Path("/api/admin/secrets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminSecretResource {

    @Inject
    SecretService secretService;

    /**
     * Lista todos los secretos del sistema sin restricciones.
     * <p>
     * Endpoint de máximo privilegio que expone TODOS los secretos independientemente
     * de su nivel de clasificación o propietario. Solo accesible por administradores
     * con rol VAULT_ADMIN.
     * </p>
     * 
     * <p><b>Ejemplo de uso:</b></p>
     * <pre>
     * curl -X GET http://localhost:8080/api/admin/secrets/all \
     *   -u admin:admin123
     * </pre>
     * 
     * @return lista completa de todos los secretos en el sistema
     */
    @GET
    @Path("/all")
    @RolesAllowed(Roles.VAULT_ADMIN)
    public List<Secret> getAllSecrets() {
        return secretService.getAllSecrets();
    }

    /**
     * Elimina un secreto del sistema permanentemente.
     * <p>
     * Operación destructiva que requiere máximos privilegios. Solo administradores
     * con rol VAULT_ADMIN pueden ejecutar esta acción. En producción, considerar
     * implementar soft-delete para auditoría.
     * </p>
     * 
     * <p><b>Ejemplo de uso:</b></p>
     * <pre>
     * curl -X DELETE http://localhost:8080/api/admin/secrets/{id} \
     *   -u admin:admin123
     * </pre>
     * 
     * @param id identificador UUID del secreto a eliminar
     * @return Response con status 200 si fue eliminado exitosamente,
     *         o status 404 si el secreto no existe
     */
    @DELETE
    @Path("/{id}")
    @RolesAllowed(Roles.VAULT_ADMIN)
    public Response deleteSecret(@PathParam("id") String id) {
        boolean deleted = secretService.deleteSecret(id);
        if (deleted) {
            return Response.ok(Map.of("message", "Secret deleted successfully")).build();
        }
        return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Secret not found"))
                .build();
    }

    /**
     * Obtiene estadísticas del sistema para reportes y auditoría.
     * <p>
     * Endpoint de solo lectura accesible por administradores (VAULT_ADMIN) y
     * auditores (VAULT_AUDITOR). Proporciona métricas básicas del sistema sin
     * exponer datos sensibles.
     * </p>
     * 
     * <p><b>Ejemplo de uso:</b></p>
     * <pre>
     * curl -X GET http://localhost:8080/api/admin/secrets/stats \
     *   -u auditor:auditor123
     * </pre>
     * 
     * @return Response con estadísticas del sistema (total de secretos, timestamp)
     */
    @GET
    @Path("/stats")
    @RolesAllowed({Roles.VAULT_ADMIN, Roles.VAULT_AUDITOR})
    public Response getStatistics() {
        return Response.ok(Map.of(
                "totalSecrets", secretService.getTotalCount(),
                "timestamp", java.time.LocalDateTime.now()
        )).build();
    }

    /**
     * Health check administrativo sin autenticación requerida.
     * <p>
     * Endpoint público que permite verificar la disponibilidad del subsistema
     * administrativo sin necesidad de credenciales. Útil para monitoreo externo.
     * </p>
     * 
     * <p><b>Ejemplo de uso:</b></p>
     * <pre>
     * curl -X GET http://localhost:8080/api/admin/secrets/health
     * </pre>
     * 
     * @return mensaje simple confirmando que el API administrativa está activa
     */
    @GET
    @Path("/health")
    @PermitAll
    public String healthCheck() {
        return "VaultCorp Admin API is running";
    }
}