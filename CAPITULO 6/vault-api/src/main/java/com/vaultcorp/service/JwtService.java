package com.vaultcorp.service;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.Set;

@ApplicationScoped
public class JwtService {

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    public String generateToken(String userId, String email, Set<String> roles) {
        long now = Instant.now().getEpochSecond();
        long exp = now + 3600;

        return Jwt.issuer(issuer)
                .subject(userId)
                .claim("email", email)
                .claim("upn", email)
                .claim("iat", now)
                .claim("exp", exp)
                .claim("preferred_username", email)
                .groups(roles)
                .sign();
    }

    public Long getExpirationTime() {
        return 3600L;
    }
}