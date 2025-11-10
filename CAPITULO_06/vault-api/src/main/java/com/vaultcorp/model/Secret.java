package com.vaultcorp.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modelo de dominio que representa un secreto almacenado en VaultCorp.
 * <p>
 * Un secreto es cualquier información confidencial que requiere protección y control
 * de acceso. Este modelo encapsula no solo el contenido sensible, sino también metadatos
 * importantes como su clasificación, propietario y ciclo de vida.
 * </p>
 * 
 * <p>Es análogo a un documento clasificado en una bóveda: tiene un contenido protegido,
 * un nivel de clasificación, un responsable, y fechas de creación y expiración.</p>
 * 
 * <p><b>Ejemplo de uso:</b></p>
 * <pre>
 * Secret secret = new Secret();
 * secret.setName("Contraseña Base de Datos");
 * secret.setContent("mySecurePass123");
 * secret.setLevel(SecretLevel.CONFIDENTIAL);
 * secret.setOwnerId("emp001");
 * </pre>
 * 
 * @author VaultCorp Development Team
 * @since 1.0
 */
public class Secret {
    
    /** Identificador único del secreto (UUID autogenerado) */
    private String id;
    
    /** Nombre descriptivo del secreto */
    private String name;
    
    /** Contenido del secreto (información sensible) */
    private String content;
    
    /** Nivel de clasificación de seguridad que determina el control de acceso */
    private SecretLevel level;
    
    /** ID del usuario propietario que creó este secreto */
    private String ownerId;
    
    /** Timestamp de creación del secreto (autogenerado) */
    private LocalDateTime createdAt;
    
    /** Fecha y hora de expiración del secreto (opcional) */
    private LocalDateTime expiresAt;

    /**
     * Constructor que inicializa automáticamente el ID único y la fecha de creación.
     * <p>
     * El ID se genera usando UUID v4 para garantizar unicidad global, y createdAt
     * se establece al momento exacto de la instanciación.
     * </p>
     */
    public Secret() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    // Getters y Setters
    
    public String getId() { 
        return id; 
    }
    
    public void setId(String id) { 
        this.id = id; 
    }

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

    public String getOwnerId() { 
        return ownerId; 
    }
    
    public void setOwnerId(String ownerId) { 
        this.ownerId = ownerId; 
    }

    public LocalDateTime getCreatedAt() { 
        return createdAt; 
    }
    
    public void setCreatedAt(LocalDateTime createdAt) { 
        this.createdAt = createdAt; 
    }

    public LocalDateTime getExpiresAt() { 
        return expiresAt; 
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) { 
        this.expiresAt = expiresAt; 
    }
}
