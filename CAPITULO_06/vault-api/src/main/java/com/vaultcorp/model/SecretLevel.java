package com.vaultcorp.model;

/**
 * Enumeración que define los niveles de clasificación de seguridad para secretos.
 * <p>
 * Cada nivel representa un grado diferente de sensibilidad y determina qué roles
 * de usuario pueden acceder al secreto. Este sistema es análogo a las clasificaciones
 * de documentos gubernamentales o militares.
 * </p>
 * 
 * <p><b>Jerarquía de acceso (de menor a mayor restricción):</b></p>
 * <ul>
 *   <li>PUBLIC: Información general accesible a cualquier usuario autenticado</li>
 *   <li>INTERNAL: Datos corporativos solo para empleados de la organización</li>
 *   <li>CONFIDENTIAL: Información sensible limitada a usuarios premium y administradores</li>
 *   <li>TOP_SECRET: Datos críticos con acceso exclusivo para administradores</li>
 * </ul>
 * 
 * <p><b>Ejemplo de uso:</b></p>
 * <pre>
 * Secret apiKey = new Secret();
 * apiKey.setLevel(SecretLevel.TOP_SECRET); // Solo admins pueden ver
 * 
 * Secret manual = new Secret();
 * manual.setLevel(SecretLevel.PUBLIC); // Todos pueden ver
 * </pre>
 * 
 * @author VaultCorp Development Team
 * @since 1.0
 */
public enum SecretLevel {
    
    /** 
     * Nivel público: accesible por cualquier usuario autenticado.
     * Usado para documentación, enlaces públicos, información general.
     */
    PUBLIC,
    
    /** 
     * Nivel interno: solo empleados de la organización.
     * Usado para contraseñas de desarrollo, recursos internos, datos corporativos.
     */
    INTERNAL,
    
    /** 
     * Nivel confidencial: solo clientes premium y administradores.
     * Usado para credenciales de servicios, tokens de analytics, datos sensibles.
     */
    CONFIDENTIAL,
    
    /** 
     * Nivel máximo secreto: acceso exclusivo para administradores.
     * Usado para claves de producción, secretos críticos, configuraciones maestras.
     */
    TOP_SECRET
}