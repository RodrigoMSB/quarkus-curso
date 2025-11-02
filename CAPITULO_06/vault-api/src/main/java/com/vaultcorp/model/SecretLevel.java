package com.vaultcorp.model;

public enum SecretLevel {
    PUBLIC,        // Cualquiera puede ver
    INTERNAL,      // Solo empleados
    CONFIDENTIAL,  // Solo premium customers y admins
    TOP_SECRET     // Solo admins
}
