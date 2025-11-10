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

/**
 * Endpoints para clientes externos autenticados con OIDC/Keycloak (Parte 3 del curso).
 * <p>
 * Proporciona acceso controlado a secretos para usuarios externos que se autentican
 * mediante OpenID Connect usando Keycloak como proveedor de identidad. Los tokens
 * son emitidos por Keycloak y validados automáticamente por Quarkus.
 * </p>
 * 
 * <p>Es análoga a una plataforma de servicios en la nube: los clientes se autentican
 * con su cuenta externa (similar a "Login con Google") y acceden a recursos según
 * su nivel de suscripción (customer básico o premium-customer).</p>
 * 
 * <p><b>Niveles de acceso:</b></p>
 * <ul>
 *   <li><b>customer:</b> Acceso a secretos PUBLIC solamente</li>
 *   <li><b>premium-customer:</b> Acceso a secretos PUBLIC y CONFIDENTIAL</li>
 * </ul>
 * 
 * <p><b>Flujo de autenticación OIDC:</b></p>
 * <ol>
 *   <li>Cliente obtiene token desde Keycloak:
 *       <pre>POST http://localhost:8180/realms/vaultcorp/protocol/openid-connect/token</pre>
 *   </li>
 *   <li>Keycloak valida credenciales y emite access_token con roles</li>
 *   <li>Cliente usa el token en header: Authorization: Bearer {access_token}</li>
 *   <li>Quarkus valida automáticamente el token contra Keycloak</li>
 *   <li>Sistema autoriza acceso según roles en el token</li>
 * </ol>
 * 
 * <p><b>Diferencias con JWT interno:</b></p>
 * <ul>
 *   <li>Token emitido por Keycloak (externo), no por la aplicación</li>
 *   <li>Validación contra servidor de autorización externo</li>
 *   <li>Roles gestionados en Keycloak realm</li>
 *   <li>Soporte para SSO y federación de identidades</li>
 * </ul>
 * 
 * @author VaultCorp Development Team
 * @since 1.0
 */
@Path("/api/external/secrets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExternalSecretResource {

    /** 
     * Identidad de seguridad del usuario autenticado vía OIDC.
     * Contiene información extraída del token validado por Keycloak.
     */
    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    SecretService secretService;

    /**
     * Lista secretos públicos accesibles para todos los clientes.
     * <p>
     * Este endpoint implementa el nivel de acceso más básico para clientes externos.
     * Tanto clientes básicos (customer) como premium (premium-customer) pueden
     * acceder a secretos de nivel PUBLIC, que típicamente contienen información
     * de documentación, enlaces a recursos públicos, etc.
     * </p>
     * 
     * <p><b>Ejemplo de uso:</b></p>
     * <pre>
     * # Primero obtener token de Keycloak
     * TOKEN=$(curl -X POST http://localhost:8180/realms/vaultcorp/protocol/openid-connect/token \
     *   -d "grant_type=password" \
     *   -d "client_id=vault-api" \
     *   -d "client_secret={client_secret}" \
     *   -d "username=client001" \
     *   -d "password=pass001" | jq -r .access_token)
     * 
     * # Luego consultar secretos públicos
     * curl -X GET http://localhost:8080/api/external/secrets/public \
     *   -H "Authorization: Bearer $TOKEN"
     * </pre>
     * 
     * @return Response con lista de secretos PUBLIC y metadatos del usuario
     */
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

    /**
     * Lista secretos confidenciales exclusivos para clientes premium.
     * <p>
     * Implementa un nivel de acceso premium: solo clientes con rol premium-customer
     * pueden acceder a secretos CONFIDENTIAL. Estos secretos típicamente contienen
     * credenciales de servicios, tokens de analytics, o información sensible
     * disponible únicamente para suscriptores de nivel superior.
     * </p>
     * 
     * <p>Si un cliente básico (sin rol premium) intenta acceder, recibirá
     * 403 FORBIDDEN automáticamente.</p>
     * 
     * <p><b>Ejemplo de uso:</b></p>
     * <pre>
     * # Autenticarse como cliente premium
     * TOKEN=$(curl -X POST http://localhost:8180/realms/vaultcorp/protocol/openid-connect/token \
     *   -d "grant_type=password" \
     *   -d "client_id=vault-api" \
     *   -d "client_secret={client_secret}" \
     *   -d "username=client002" \
     *   -d "password=pass002" | jq -r .access_token)
     * 
     * # Consultar secretos confidenciales
     * curl -X GET http://localhost:8080/api/external/secrets/confidential \
     *   -H "Authorization: Bearer $TOKEN"
     * </pre>
     * 
     * @return Response con lista de secretos CONFIDENTIAL y metadatos del usuario
     */
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

    /**
     * Obtiene el perfil del cliente autenticado extraído del token OIDC.
     * <p>
     * Endpoint útil para verificar la información del usuario autenticado vía
     * Keycloak. Muestra el username, roles asignados y el método de autenticación.
     * No ejecuta operaciones de negocio, solo retorna metadatos de seguridad.
     * </p>
     * 
     * <p><b>Ejemplo de uso:</b></p>
     * <pre>
     * curl -X GET http://localhost:8080/api/external/secrets/profile \
     *   -H "Authorization: Bearer {access_token}"
     * </pre>
     * 
     * @return Response con información del perfil: username, roles, método de autenticación
     */
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