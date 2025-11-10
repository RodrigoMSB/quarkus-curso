package com.vaultcorp.dto;

/**
 * DTO que representa la respuesta exitosa de autenticación con JWT.
 * <p>
 * Después de validar las credenciales del usuario, el sistema genera un token JWT
 * que se retorna en este objeto. Es análogo a recibir un pase temporal que permite
 * acceder a áreas restringidas sin tener que presentar credenciales en cada puerta.
 * </p>
 * 
 * <p><b>Estructura de respuesta típica:</b></p>
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
public class TokenResponse {
    
    /** Token JWT firmado que debe incluirse en las solicitudes subsecuentes */
    private String token;
    
    /** Tipo de token (siempre "Bearer" para JWT) */
    private String type = "Bearer";
    
    /** Tiempo de expiración del token en segundos desde su emisión */
    private Long expiresIn;

    /**
     * Constructor por defecto requerido para serialización JSON.
     */
    public TokenResponse() {}

    /**
     * Constructor para crear una respuesta con token y tiempo de expiración.
     * 
     * @param token el JWT firmado generado para el usuario autenticado
     * @param expiresIn tiempo en segundos hasta la expiración del token
     */
    public TokenResponse(String token, Long expiresIn) {
        this.token = token;
        this.expiresIn = expiresIn;
    }

    // Getters y Setters
    
    public String getToken() { 
        return token; 
    }
    
    public void setToken(String token) { 
        this.token = token; 
    }

    public String getType() { 
        return type; 
    }
    
    public void setType(String type) { 
        this.type = type; 
    }

    public Long getExpiresIn() { 
        return expiresIn; 
    }
    
    public void setExpiresIn(Long expiresIn) { 
        this.expiresIn = expiresIn; 
    }
}