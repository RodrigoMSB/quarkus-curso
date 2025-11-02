package com.vaultcorp.dto;

public class TokenResponse {
    private String token;
    private String type = "Bearer";
    private Long expiresIn;

    // Constructors
    public TokenResponse() {}

    public TokenResponse(String token, Long expiresIn) {
        this.token = token;
        this.expiresIn = expiresIn;
    }

    // Getters y Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }
}
