package com.vaultcorp.service;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.Set;

/**
 * Servicio para generación de tokens JWT (JSON Web Tokens).
 * <p>
 * Este servicio centraliza la lógica de creación de tokens JWT firmados con RSA,
 * incluyendo todos los claims estándar y personalizados necesarios para la
 * autenticación y autorización en el sistema.
 * </p>
 * 
 * <p>Es análogo a una casa de moneda que emite certificados de identidad:
 * toma la información del usuario y crea un documento firmado digitalmente
 * que prueba su identidad y permisos.</p>
 * 
 * <p><b>Estructura del JWT generado:</b></p>
 * <ul>
 *   <li><b>Header:</b> Algoritmo RS256 (RSA con SHA-256)</li>
 *   <li><b>Payload:</b> Claims del usuario (ID, email, roles, timestamps)</li>
 *   <li><b>Signature:</b> Firma digital usando la clave privada RSA</li>
 * </ul>
 * 
 * <p><b>Ejemplo de uso:</b></p>
 * <pre>
 * String token = jwtService.generateToken(
 *     "emp001", 
 *     "juan.perez@vaultcorp.com", 
 *     Set.of("employee")
 * );
 * </pre>
 * 
 * @author VaultCorp Development Team
 * @since 1.0
 */
@ApplicationScoped
public class JwtService {

    /** 
     * Emisor del token (issuer) configurado en application.properties.
     * Identifica la entidad que emitió el JWT (ej: "https://vaultcorp.com")
     */
    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    /**
     * Genera un token JWT firmado para un usuario autenticado.
     * <p>
     * El token generado incluye los siguientes claims estándar:
     * <ul>
     *   <li><b>iss:</b> Emisor del token (issuer)</li>
     *   <li><b>sub:</b> Sujeto (user ID)</li>
     *   <li><b>iat:</b> Issued At (timestamp de emisión)</li>
     *   <li><b>exp:</b> Expiration Time (timestamp de expiración)</li>
     *   <li><b>groups:</b> Roles del usuario</li>
     * </ul>
     * Y claims personalizados:
     * <ul>
     *   <li><b>email:</b> Correo electrónico del usuario</li>
     *   <li><b>upn:</b> User Principal Name (email)</li>
     *   <li><b>preferred_username:</b> Nombre de usuario preferido</li>
     * </ul>
     * </p>
     * 
     * @param userId identificador único del usuario (será el claim "sub")
     * @param email correo electrónico del usuario
     * @param roles conjunto de roles asignados al usuario
     * @return JWT firmado como String codificado en Base64URL
     */
    public String generateToken(String userId, String email, Set<String> roles) {
        long now = Instant.now().getEpochSecond();
        long exp = now + 3600; // Expira en 1 hora

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

    /**
     * Obtiene el tiempo de expiración configurado para los tokens.
     * 
     * @return tiempo de vida del token en segundos (3600 = 1 hora)
     */
    public Long getExpirationTime() {
        return 3600L;
    }
}