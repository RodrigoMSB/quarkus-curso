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

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    JwtService jwtService;

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        // Validación simple de credenciales (mock)
        // En producción, esto debería validar contra una base de datos
        if (validateCredentials(request.getUsername(), request.getPassword())) {
            
            // Determinar el userId y email según el usuario
            String userId = getUserId(request.getUsername());
            String email = getEmail(request.getUsername());
            Set<String> roles = getRoles(request.getUsername());

            // Generar el JWT
            String token = jwtService.generateToken(userId, email, roles);
            
            TokenResponse response = new TokenResponse(token, jwtService.getExpirationTime());
            return Response.ok(response).build();
        }

        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Map.of("error", "Credenciales inválidas"))
                .build();
    }

    private boolean validateCredentials(String username, String password) {
        // Mock de validación - en producción usar base de datos
        return ("emp001".equals(username) && "pass001".equals(password)) ||
               ("emp002".equals(username) && "pass002".equals(password)) ||
               ("emp003".equals(username) && "pass003".equals(password));
    }

    private String getUserId(String username) {
        return username; // En producción, obtener del BD
    }

    private String getEmail(String username) {
        // Mock de emails
        Map<String, String> emails = Map.of(
            "emp001", "juan.perez@vaultcorp.com",
            "emp002", "maria.gonzalez@vaultcorp.com",
            "emp003", "carlos.rodriguez@vaultcorp.com"
        );
        return emails.getOrDefault(username, username + "@vaultcorp.com");
    }

    private Set<String> getRoles(String username) {
        // Todos los empleados tienen el rol "employee"
        return Set.of("employee");
    }
}
