package com.vaultcorp.resource;

import com.vaultcorp.dto.SecretRequest;
import com.vaultcorp.model.Secret;
import com.vaultcorp.security.Roles;
import com.vaultcorp.service.SecretService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.Map;

@Path("/api/internal/secrets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InternalSecretResource {

    @Inject
    JsonWebToken jwt;

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    SecretService secretService;

    @POST
    @RolesAllowed(Roles.EMPLOYEE)
    public Response createSecret(@Valid SecretRequest request) {
        // Obtener información del usuario desde el JWT
        String userId = jwt.getSubject();
        String email = jwt.getClaim("email");

        // Crear el secreto
        Secret secret = new Secret();
        secret.setName(request.getName());
        secret.setContent(request.getContent());
        secret.setLevel(request.getLevel());
        secret.setOwnerId(userId);

        Secret created = secretService.createSecret(secret);

        return Response.status(Response.Status.CREATED)
                .entity(Map.of(
                    "message", "Secreto creado exitosamente",
                    "secret", created,
                    "createdBy", email
                ))
                .build();
    }

    @GET
    @Path("/my-secrets")
    @RolesAllowed(Roles.EMPLOYEE)
    public Response getMySecrets() {
        String userId = jwt.getSubject();
        List<Secret> mySecrets = secretService.getSecretsByOwner(userId);

        return Response.ok(Map.of(
            "userId", userId,
            "email", jwt.getClaim("email"),
            "totalSecrets", mySecrets.size(),
            "secrets", mySecrets
        )).build();
    }

    @GET
    @Path("/profile")
    @RolesAllowed(Roles.EMPLOYEE)
    public Response getProfile() {
        return Response.ok(Map.of(
            "userId", jwt.getSubject(),
            "email", jwt.getClaim("email"),
            "roles", securityIdentity.getRoles(),
            "tokenIssuer", jwt.getIssuer()
        )).build();
    }
}
