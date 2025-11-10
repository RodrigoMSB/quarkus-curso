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

/**
 * Endpoints para empleados internos autenticados con JWT (Parte 2 del curso).
 * <p>
 * Proporciona operaciones de gestión de secretos para empleados de la organización
 * que se autentican mediante tokens JWT. Los endpoints extraen automáticamente
 * información del usuario desde el JWT validado.
 * </p>
 * 
 * <p>Es análogo a un sistema de casilleros personales en una empresa: cada empleado
 * puede depositar y consultar sus propios documentos usando su credencial digital,
 * y el sistema sabe automáticamente quién es por su tarjeta de acceso (JWT).</p>
 * 
 * <p><b>Flujo de uso:</b></p>
 * <ol>
 *   <li>Empleado obtiene JWT mediante /api/auth/login</li>
 *   <li>Incluye JWT en header: Authorization: Bearer {token}</li>
 *   <li>El sistema extrae automáticamente userId, email y roles del token</li>
 *   <li>Ejecuta la operación solicitada con el contexto del usuario</li>
 * </ol>
 * 
 * <p><b>Ejemplo de request:</b></p>
 * <pre>
 * POST /api/internal/secrets
 * Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
 * Content-Type: application/json
 * 
 * {
 *   "name": "Password BD QA",
 *   "content": "qaPass2024",
 *   "level": "INTERNAL"
 * }
 * </pre>
 * 
 * @author VaultCorp Development Team
 * @since 1.0
 */
@Path("/api/internal/secrets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InternalSecretResource {

    /** 
     * Token JWT del usuario autenticado, inyectado automáticamente por Quarkus.
     * Contiene todos los claims del usuario: ID, email, roles, timestamps.
     */
    @Inject
    JsonWebToken jwt;

    /** 
     * Identidad de seguridad del usuario autenticado.
     * Proporciona información adicional como principal name y roles.
     */
    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    SecretService secretService;

    /**
     * Crea un nuevo secreto asociado al empleado autenticado.
     * <p>
     * El sistema extrae automáticamente el userId desde el JWT para establecer
     * la propiedad del secreto. Valida que todos los campos requeridos estén
     * presentes mediante Bean Validation (@Valid).
     * </p>
     * 
     * <p><b>Ejemplo de uso:</b></p>
     * <pre>
     * curl -X POST http://localhost:8080/api/internal/secrets \
     *   -H "Authorization: Bearer {token}" \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "name": "Password BD Dev",
     *     "content": "devPass123",
     *     "level": "INTERNAL"
     *   }'
     * </pre>
     * 
     * @param request datos del secreto a crear (validados automáticamente)
     * @return Response con status 201 CREATED y el secreto creado,
     *         incluyendo metadatos del creador
     */
    @POST
    @RolesAllowed(Roles.EMPLOYEE)
    public Response createSecret(@Valid SecretRequest request) {
        // Extraer información del usuario desde el JWT
        String userId = jwt.getSubject();
        String email = jwt.getClaim("email");

        // Crear el secreto con información del propietario
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

    /**
     * Lista todos los secretos creados por el empleado autenticado.
     * <p>
     * Implementa el principio de propiedad de datos: cada usuario solo puede
     * ver los secretos que él mismo ha creado. El filtrado se realiza
     * automáticamente usando el userId extraído del JWT.
     * </p>
     * 
     * <p><b>Ejemplo de uso:</b></p>
     * <pre>
     * curl -X GET http://localhost:8080/api/internal/secrets/my-secrets \
     *   -H "Authorization: Bearer {token}"
     * </pre>
     * 
     * @return Response con la lista de secretos del usuario y metadatos
     */
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

    /**
     * Obtiene el perfil del empleado autenticado extraído del JWT.
     * <p>
     * Endpoint útil para debugging y verificación de la información contenida
     * en el token. Muestra todos los claims relevantes del usuario sin ejecutar
     * ninguna operación de negocio.
     * </p>
     * 
     * <p><b>Ejemplo de uso:</b></p>
     * <pre>
     * curl -X GET http://localhost:8080/api/internal/secrets/profile \
     *   -H "Authorization: Bearer {token}"
     * </pre>
     * 
     * @return Response con información del perfil: userId, email, roles, issuer
     */
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