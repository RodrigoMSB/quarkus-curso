package com.vaultcorp.resource;

import com.vaultcorp.dto.LoginRequest;
import com.vaultcorp.dto.TokenResponse;
import com.vaultcorp.service.JwtService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;
import java.util.Set;

/**
 * Endpoint de autenticación para empleados internos (Parte 2 del curso).
 * <p>
 * Proporciona el servicio de login que valida credenciales de empleados y
 * genera tokens JWT firmados. Este endpoint es público (no requiere autenticación
 * previa) ya que es el punto de entrada para obtener el token.
 * </p>
 * 
 * <p>Es análogo a la recepción de una empresa: validas tu identidad con credenciales
 * y recibes un pase temporal (JWT) que te permite acceder a las áreas autorizadas.</p>
 * 
 * <p><b>Flujo de autenticación:</b></p>
 * <ol>
 *   <li>Cliente envía credenciales (username/password)</li>
 *   <li>Sistema valida credenciales contra usuarios registrados</li>
 *   <li>Si son válidas, genera JWT con roles y datos del usuario</li>
 *   <li>Retorna el token al cliente para solicitudes subsecuentes</li>
 * </ol>
 * 
 * <p><b>Ejemplo de request:</b></p>
 * <pre>
 * POST /api/auth/login
 * Content-Type: application/json
 * 
 * {
 *   "username": "emp001",
 *   "password": "pass001"
 * }
 * </pre>
 * 
 * <p><b>Ejemplo de response exitosa:</b></p>
 * <pre>
 * {
 *   "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
 *   "type": "Bearer",
 *   "expiresIn": 3600
 * }
 * </pre>
 * 
 * @author VaultCorp Development Team
 * @since 1.0
 */
@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    JwtService jwtService;

    /**
     * Endpoint de autenticación que valida credenciales y genera JWT.
     * <p>
     * IMPORTANTE: Esta implementación usa validación mock para fines educativos.
     * En producción, las credenciales deben validarse contra una base de datos
     * con contraseñas hasheadas (bcrypt, argon2, etc.).
     * </p>
     * 
     * @param request objeto con username y password del empleado
     * @return Response con TokenResponse si las credenciales son válidas,
     *         o status 401 UNAUTHORIZED si son inválidas
     */
    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        // Validación de credenciales (mock para demo)
        if (validateCredentials(request.getUsername(), request.getPassword())) {
            
            // Obtener información del usuario autenticado
            String userId = getUserId(request.getUsername());
            String email = getEmail(request.getUsername());
            Set<String> roles = getRoles(request.getUsername());

            // Generar JWT firmado
            String token = jwtService.generateToken(userId, email, roles);
            
            TokenResponse response = new TokenResponse(token, jwtService.getExpirationTime());
            return Response.ok(response).build();
        }

        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Map.of("error", "Credenciales inválidas"))
                .build();
    }

    /**
     * Valida las credenciales del usuario contra el sistema de autenticación.
     * <p>
     * MOCK: En producción, consultar base de datos y verificar password hasheado.
     * </p>
     * 
     * @param username nombre de usuario
     * @param password contraseña en texto plano
     * @return true si las credenciales son válidas, false en caso contrario
     */
    private boolean validateCredentials(String username, String password) {
        // Mock de validación - SOLO PARA DEMOSTRACIÓN
        return ("emp001".equals(username) && "pass001".equals(password)) ||
               ("emp002".equals(username) && "pass002".equals(password)) ||
               ("emp003".equals(username) && "pass003".equals(password));
    }

    /**
     * Obtiene el ID del usuario basado en su username.
     * <p>
     * MOCK: En producción, consultar desde base de datos.
     * </p>
     * 
     * @param username nombre de usuario
     * @return identificador único del usuario
     */
    private String getUserId(String username) {
        return username; // En producción, obtener desde BD
    }

    /**
     * Obtiene el email corporativo del usuario.
     * <p>
     * MOCK: En producción, consultar desde base de datos o directorio LDAP.
     * </p>
     * 
     * @param username nombre de usuario
     * @return email corporativo del usuario
     */
    private String getEmail(String username) {
        Map<String, String> emails = Map.of(
            "emp001", "juan.perez@vaultcorp.com",
            "emp002", "maria.gonzalez@vaultcorp.com",
            "emp003", "carlos.rodriguez@vaultcorp.com"
        );
        return emails.getOrDefault(username, username + "@vaultcorp.com");
    }

    /**
     * Obtiene los roles asignados al usuario.
     * <p>
     * MOCK: Todos los empleados tienen el rol "employee".
     * En producción, consultar roles desde base de datos o LDAP/Active Directory.
     * </p>
     * 
     * @param username nombre de usuario
     * @return conjunto de roles del usuario
     */
    private Set<String> getRoles(String username) {
        // Todos los empleados tienen el rol "employee"
        return Set.of("employee");
    }
}