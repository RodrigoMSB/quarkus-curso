package com.vaultcorp.dto;

import com.vaultcorp.model.SecretLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para la creación de secretos en el sistema VaultCorp.
 * <p>
 * Un secreto puede ser cualquier información sensible que necesite protección:
 * contraseñas, tokens de API, claves de cifrado, etc. Este DTO valida que
 * toda la información requerida esté presente antes de persistir el secreto.
 * </p>
 * 
 * <p>Es análogo a un formulario de depósito bancario donde debes especificar
 * qué guardas, su contenido y el nivel de seguridad requerido.</p>
 * 
 * <p><b>Ejemplo de uso:</b></p>
 * <pre>
 * SecretRequest request = new SecretRequest(
 *     "API Key Production", 
 *     "sk_prod_ABC123XYZ", 
 *     SecretLevel.TOP_SECRET
 * );
 * </pre>
 * 
 * @author VaultCorp Development Team
 * @since 1.0
 */
public class SecretRequest {
    
    /** 
     * Nombre descriptivo del secreto (ej: "Contraseña BD Producción").
     * Este campo es obligatorio y no puede estar vacío.
     */
    @NotBlank(message = "El nombre es obligatorio")
    private String name;
    
    /** 
     * Contenido del secreto (el valor sensible que se desea proteger).
     * Este campo es obligatorio y no puede estar vacío.
     */
    @NotBlank(message = "El contenido es obligatorio")
    private String content;
    
    /** 
     * Nivel de clasificación de seguridad del secreto.
     * Determina quién puede acceder a este secreto según su rol.
     * Este campo es obligatorio.
     */
    @NotNull(message = "El nivel de seguridad es obligatorio")
    private SecretLevel level;

    /**
     * Constructor por defecto requerido para deserialización JSON.
     */
    public SecretRequest() {}

    /**
     * Constructor completo para crear una solicitud de secreto.
     * 
     * @param name nombre descriptivo del secreto
     * @param content el valor sensible a proteger
     * @param level nivel de clasificación de seguridad
     */
    public SecretRequest(String name, String content, SecretLevel level) {
        this.name = name;
        this.content = content;
        this.level = level;
    }

    // Getters y Setters
    
    public String getName() { 
        return name; 
    }
    
    public void setName(String name) { 
        this.name = name; 
    }

    public String getContent() { 
        return content; 
    }
    
    public void setContent(String content) { 
        this.content = content; 
    }

    public SecretLevel getLevel() { 
        return level; 
    }
    
    public void setLevel(SecretLevel level) { 
        this.level = level; 
    }
}