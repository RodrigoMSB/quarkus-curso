package com.vaultcorp.dto;

/**
 * DTO para las solicitudes de autenticación de usuarios.
 * <p>
 * Este objeto encapsula las credenciales necesarias para el proceso de login,
 * análogo a un formulario de inicio de sesión donde el usuario proporciona
 * su nombre de usuario y contraseña.
 * </p>
 * 
 * <p><b>Ejemplo de uso:</b></p>
 * <pre>
 * LoginRequest request = new LoginRequest("emp001", "pass001");
 * </pre>
 * 
 * @author VaultCorp Development Team
 * @since 1.0
 */
public class LoginRequest {
    
    /** Nombre de usuario o identificador único del empleado */
    private String username;
    
    /** Contraseña del usuario (será validada contra el sistema de autenticación) */
    private String password;

    /**
     * Constructor por defecto requerido para deserialización JSON.
     */
    public LoginRequest() {}

    /**
     * Constructor completo para crear una solicitud de login con credenciales.
     * 
     * @param username el nombre de usuario
     * @param password la contraseña del usuario
     */
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters y Setters
    
    public String getUsername() { 
        return username; 
    }
    
    public void setUsername(String username) { 
        this.username = username; 
    }

    public String getPassword() { 
        return password; 
    }
    
    public void setPassword(String password) { 
        this.password = password; 
    }
}