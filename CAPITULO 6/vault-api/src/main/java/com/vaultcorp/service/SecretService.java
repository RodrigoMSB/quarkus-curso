package com.vaultcorp.service;

import com.vaultcorp.model.Secret;
import com.vaultcorp.model.SecretLevel;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class SecretService {
    
    private final List<Secret> secrets = new ArrayList<>();

    public SecretService() {
        initializeMockData();
    }

    private void initializeMockData() {
        // Secretos TOP_SECRET (solo admin)
        Secret s1 = new Secret();
        s1.setName("API Key Producción");
        s1.setContent("sk_prod_ABC123XYZ");
        s1.setLevel(SecretLevel.TOP_SECRET);
        s1.setOwnerId("admin");
        secrets.add(s1);

        // Secretos INTERNAL (empleados)
        Secret s2 = new Secret();
        s2.setName("Contraseña BD Dev");
        s2.setContent("devPass2024");
        s2.setLevel(SecretLevel.INTERNAL);
        s2.setOwnerId("emp001");
        secrets.add(s2);

        // Secretos PUBLIC (clientes básicos)
        Secret s3 = new Secret();
        s3.setName("Manual de Usuario");
        s3.setContent("https://docs.vaultcorp.com/manual");
        s3.setLevel(SecretLevel.PUBLIC);
        s3.setOwnerId("marketing");
        secrets.add(s3);

        Secret s4 = new Secret();
        s4.setName("API Pública de Consultas");
        s4.setContent("https://api.vaultcorp.com/public/v1");
        s4.setLevel(SecretLevel.PUBLIC);
        s4.setOwnerId("marketing");
        secrets.add(s4);

        // Secretos CONFIDENTIAL (solo premium)
        Secret s5 = new Secret();
        s5.setName("Credenciales AWS S3");
        s5.setContent("AKIAIOSFODNN7EXAMPLE");
        s5.setLevel(SecretLevel.CONFIDENTIAL);
        s5.setOwnerId("ops");
        secrets.add(s5);

        Secret s6 = new Secret();
        s6.setName("Token Analytics Premium");
        s6.setContent("analytics_premium_token_xyz789");
        s6.setLevel(SecretLevel.CONFIDENTIAL);
        s6.setOwnerId("analytics");
        secrets.add(s6);
    }

    public List<Secret> getAllSecrets() {
        return new ArrayList<>(secrets);
    }

    public Optional<Secret> getSecretById(String id) {
        return secrets.stream()
                .filter(s -> s.getId().equals(id))
                .findFirst();
    }

    public List<Secret> getSecretsByLevel(SecretLevel level) {
        return secrets.stream()
                .filter(s -> s.getLevel() == level)
                .collect(Collectors.toList());
    }

    public List<Secret> getSecretsByOwner(String ownerId) {
        return secrets.stream()
                .filter(s -> s.getOwnerId().equals(ownerId))
                .collect(Collectors.toList());
    }

    public Secret createSecret(Secret secret) {
        secrets.add(secret);
        return secret;
    }

    public boolean deleteSecret(String id) {
        return secrets.removeIf(s -> s.getId().equals(id));
    }

    public int getTotalCount() {
        return secrets.size();
    }
}
