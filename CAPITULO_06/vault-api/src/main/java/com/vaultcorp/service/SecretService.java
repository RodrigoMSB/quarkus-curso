package com.vaultcorp.service;

import com.vaultcorp.model.Secret;
import com.vaultcorp.model.SecretLevel;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de secretos en VaultCorp.
 * <p>
 * Proporciona operaciones CRUD para secretos con almacenamiento en memoria.
 * En un entorno de producción, este servicio se conectaría a una base de datos
 * o un servicio de gestión de secretos como HashiCorp Vault o AWS Secrets Manager.
 * </p>
 * 
 * <p>Es análogo al sistema de gestión de una bóveda bancaria: permite depositar,
 * consultar y retirar elementos (secretos), manteniendo un registro organizado
 * por clasificación de seguridad y propietario.</p>
 * 
 * <p><b>Datos de prueba incluidos:</b></p>
 * <ul>
 *   <li>2 secretos TOP_SECRET (solo admin)</li>
 *   <li>1 secreto INTERNAL (empleados)</li>
 *   <li>2 secretos PUBLIC (cualquier usuario)</li>
 *   <li>2 secretos CONFIDENTIAL (premium customers)</li>
 * </ul>
 * 
 * @author VaultCorp Development Team
 * @since 1.0
 */
@ApplicationScoped
public class SecretService {
    
    /** 
     * Almacenamiento en memoria de todos los secretos.
     * NOTA: En producción, usar persistencia con base de datos.
     */
    private final List<Secret> secrets = new ArrayList<>();

    /**
     * Constructor que inicializa el servicio con datos de prueba.
     */
    public SecretService() {
        initializeMockData();
    }

    /**
     * Inicializa el sistema con secretos de ejemplo para demostración.
     * <p>
     * Crea secretos de diferentes niveles de clasificación con propietarios
     * variados para facilitar las pruebas de control de acceso.
     * </p>
     */
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

    /**
     * Obtiene todos los secretos almacenados en el sistema.
     * <p>
     * IMPORTANTE: Este método no aplica filtros de seguridad.
     * El control de acceso debe manejarse a nivel de endpoint.
     * </p>
     * 
     * @return lista con todos los secretos (copia defensiva)
     */
    public List<Secret> getAllSecrets() {
        return new ArrayList<>(secrets);
    }

    /**
     * Busca un secreto específico por su ID único.
     * 
     * @param id identificador UUID del secreto
     * @return Optional conteniendo el secreto si existe, Optional.empty() si no
     */
    public Optional<Secret> getSecretById(String id) {
        return secrets.stream()
                .filter(s -> s.getId().equals(id))
                .findFirst();
    }

    /**
     * Filtra secretos por nivel de clasificación de seguridad.
     * <p>
     * Útil para endpoints que deben retornar solo secretos de un nivel específico
     * según los permisos del usuario autenticado.
     * </p>
     * 
     * @param level nivel de clasificación a filtrar (PUBLIC, INTERNAL, etc.)
     * @return lista de secretos que coinciden con el nivel especificado
     */
    public List<Secret> getSecretsByLevel(SecretLevel level) {
        return secrets.stream()
                .filter(s -> s.getLevel() == level)
                .collect(Collectors.toList());
    }

    /**
     * Filtra secretos por propietario.
     * <p>
     * Permite a los usuarios consultar únicamente los secretos que ellos
     * han creado, implementando el principio de propiedad de datos.
     * </p>
     * 
     * @param ownerId identificador del usuario propietario
     * @return lista de secretos pertenecientes al usuario especificado
     */
    public List<Secret> getSecretsByOwner(String ownerId) {
        return secrets.stream()
                .filter(s -> s.getOwnerId().equals(ownerId))
                .collect(Collectors.toList());
    }

    /**
     * Crea y almacena un nuevo secreto en el sistema.
     * 
     * @param secret el secreto a crear (debe tener todos los campos requeridos)
     * @return el secreto creado con su ID y timestamp asignados
     */
    public Secret createSecret(Secret secret) {
        secrets.add(secret);
        return secret;
    }

    /**
     * Elimina un secreto del sistema por su ID.
     * 
     * @param id identificador del secreto a eliminar
     * @return true si el secreto fue eliminado, false si no existía
     */
    public boolean deleteSecret(String id) {
        return secrets.removeIf(s -> s.getId().equals(id));
    }

    /**
     * Obtiene el conteo total de secretos almacenados.
     * <p>
     * Útil para estadísticas y reportes administrativos.
     * </p>
     * 
     * @return número total de secretos en el sistema
     */
    public int getTotalCount() {
        return secrets.size();
    }
}