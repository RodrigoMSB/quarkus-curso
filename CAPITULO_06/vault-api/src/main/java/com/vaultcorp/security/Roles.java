package com.vaultcorp.security;

/**
 * Catálogo centralizado de roles de seguridad en VaultCorp.
 * <p>
 * Define las constantes de roles utilizadas para control de acceso basado en roles (RBAC).
 * Cada constante representa un rol específico con permisos definidos en el sistema.
 * </p>
 * 
 * <p>Esta clase es análoga a una lista de credenciales de acceso en una empresa:
 * cada tarjeta (rol) abre ciertas puertas (endpoints) pero no otras.</p>
 * 
 * <p><b>Organización de roles por contexto:</b></p>
 * <ul>
 *   <li><b>Administrativos (Parte 1):</b> Gestión y auditoría del sistema</li>
 *   <li><b>Empleados (Parte 2):</b> Personal interno con autenticación JWT</li>
 *   <li><b>Clientes externos (Parte 3):</b> Usuarios externos autenticados vía OIDC</li>
 * </ul>
 * 
 * <p><b>Ejemplo de uso:</b></p>
 * <pre>
 * {@literal @}RolesAllowed(Roles.VAULT_ADMIN)
 * public Response deleteSecret(String id) { ... }
 * </pre>
 * 
 * @author VaultCorp Development Team
 * @since 1.0
 */
public class Roles {
    
    // ═══════════════════════════════════════════════════════════════
    // ROLES ADMINISTRATIVOS (Parte 1 - Basic Auth)
    // ═══════════════════════════════════════════════════════════════
    
    /** 
     * Rol de administrador con acceso completo al sistema.
     * Puede crear, leer, modificar y eliminar todos los secretos.
     */
    public static final String VAULT_ADMIN = "vault-admin";
    
    /** 
     * Rol de auditor con acceso de solo lectura para supervisión.
     * Puede ver estadísticas y realizar auditorías sin modificar datos.
     */
    public static final String VAULT_AUDITOR = "vault-auditor";
    
    // ═══════════════════════════════════════════════════════════════
    // ROLES DE EMPLEADOS INTERNOS (Parte 2 - JWT)
    // ═══════════════════════════════════════════════════════════════
    
    /** 
     * Rol de empleado estándar de la organización.
     * Puede crear y gestionar sus propios secretos de nivel INTERNAL.
     */
    public static final String EMPLOYEE = "employee";
    
    // ═══════════════════════════════════════════════════════════════
    // ROLES DE CLIENTES EXTERNOS (Parte 3 - OIDC + Keycloak)
    // ═══════════════════════════════════════════════════════════════
    
    /** 
     * Rol de cliente básico externo.
     * Puede acceder únicamente a secretos de nivel PUBLIC.
     */
    public static final String CUSTOMER = "customer";
    
    /** 
     * Rol de cliente premium externo con acceso ampliado.
     * Puede acceder a secretos PUBLIC y CONFIDENTIAL.
     */
    public static final String PREMIUM_CUSTOMER = "premium-customer";

    /**
     * Constructor privado para prevenir instanciación.
     * Esta es una clase de constantes (utility class) y no debe ser instanciada.
     */
    private Roles() {
        throw new AssertionError("La clase Roles no debe ser instanciada");
    }
}