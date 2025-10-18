package com.vaultcorp.resource;

import com.vaultcorp.model.Secret;
import com.vaultcorp.model.SecretLevel;
import com.vaultcorp.security.Roles;
import com.vaultcorp.service.SecretService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/api/external/secrets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExternalSecretResource {

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    SecretService secretService;

    @GET
    @Path("/public")
    @RolesAllowed({Roles.CUSTOMER, Roles.PREMIUM_CUSTOMER})
    public Response getPublicSecrets() {
        List<Secret> publicSecrets = secretService.getSecretsByLevel(SecretLevel.PUBLIC);
        
        return Response.ok(Map.of(
            "user", securityIdentity.getPrincipal().getName(),
            "roles", securityIdentity.getRoles(),
            "totalSecrets", publicSecrets.size(),
            "secrets", publicSecrets
        )).build();
    }

    @GET
    @Path("/confidential")
    @RolesAllowed(Roles.PREMIUM_CUSTOMER)
    public Response getConfidentialSecrets() {
        List<Secret> confidentialSecrets = secretService.getSecretsByLevel(SecretLevel.CONFIDENTIAL);
        
        return Response.ok(Map.of(
            "user", securityIdentity.getPrincipal().getName(),
            "roles", securityIdentity.getRoles(),
            "level", "CONFIDENTIAL",
            "totalSecrets", confidentialSecrets.size(),
            "secrets", confidentialSecrets
        )).build();
    }

    @GET
    @Path("/profile")
    @RolesAllowed({Roles.CUSTOMER, Roles.PREMIUM_CUSTOMER})
    public Response getProfile() {
        return Response.ok(Map.of(
            "username", securityIdentity.getPrincipal().getName(),
            "roles", securityIdentity.getRoles(),
            "authMethod", "OIDC (Keycloak)"
        )).build();
    }
}
