package com.vaultcorp.dto;

import com.vaultcorp.model.SecretLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SecretRequest {
    
    @NotBlank(message = "El nombre es obligatorio")
    private String name;
    
    @NotBlank(message = "El contenido es obligatorio")
    private String content;
    
    @NotNull(message = "El nivel de seguridad es obligatorio")
    private SecretLevel level;

    // Constructors
    public SecretRequest() {}

    public SecretRequest(String name, String content, SecretLevel level) {
        this.name = name;
        this.content = content;
        this.level = level;
    }

    // Getters y Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public SecretLevel getLevel() { return level; }
    public void setLevel(SecretLevel level) { this.level = level; }
}
