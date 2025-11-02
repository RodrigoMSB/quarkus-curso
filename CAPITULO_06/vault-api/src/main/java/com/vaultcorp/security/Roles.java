package com.vaultcorp.security;

public class Roles {
    // Roles administrativos (Parte 1)
    public static final String VAULT_ADMIN = "vault-admin";
    public static final String VAULT_AUDITOR = "vault-auditor";
    
    // Roles de empleados internos (Parte 2)
    public static final String EMPLOYEE = "employee";
    
    // Roles de clientes externos (Parte 3 - OIDC)
    public static final String CUSTOMER = "customer";
    public static final String PREMIUM_CUSTOMER = "premium-customer";

    private Roles() {
        // Clase de constantes, no instanciable
    }
}
