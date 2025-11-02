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

@Path("/api/admin/secrets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminSecretResource {

    @Inject
    SecretService secretService;

    // TODO 1.1: Solo VAULT_ADMIN puede listar TODOS los secretos
    @GET
    @Path("/all")
    @RolesAllowed(Roles.VAULT_ADMIN)
    public List<Secret> getAllSecrets() {
        return secretService.getAllSecrets();
    }

    // TODO 1.2: Solo VAULT_ADMIN puede eliminar secretos
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

    // TODO 1.3: VAULT_ADMIN y VAULT_AUDITOR pueden ver estadísticas
    @GET
    @Path("/stats")
    @RolesAllowed({Roles.VAULT_ADMIN, Roles.VAULT_AUDITOR})
    public Response getStatistics() {
        return Response.ok(Map.of(
                "totalSecrets", secretService.getTotalCount(),
                "timestamp", java.time.LocalDateTime.now()
        )).build();
    }

    // TODO 1.4: Endpoint público para health check
    @GET
    @Path("/health")
    @PermitAll
    public String healthCheck() {
        return "VaultCorp Admin API is running";
    }
}
