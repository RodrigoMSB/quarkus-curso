package pe.banco.customer.entity;

/**
 * Estados posibles de un cliente empresarial
 */
public enum CustomerStatus {
    /**
     * Cliente activo y operativo
     */
    ACTIVE,
    
    /**
     * Cliente inactivo (sin operaciones)
     */
    INACTIVE,
    
    /**
     * Cliente suspendido por incumplimiento
     */
    SUSPENDED,
    
    /**
     * Cliente bloqueado por el banco
     */
    BLOCKED
}
