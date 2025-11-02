package com.vaultcorp.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Secret {
    private String id;
    private String name;
    private String content;
    private SecretLevel level;
    private String ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public Secret() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public SecretLevel getLevel() { return level; }
    public void setLevel(SecretLevel level) { this.level = level; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
