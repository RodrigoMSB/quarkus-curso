package pe.banco.customer.security;

import com.google.crypto.tink.*;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.AeadKeyTemplates;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

/**
 * Servicio de cifrado/descifrado con Google Tink
 * 
 * Demuestra:
 * - Cifrado simétrico a nivel de aplicación (Capítulo 4)
 * - CDI y Application Scoped (Capítulo 3)
 * - Configuración externalizada (Capítulo 7)
 * 
 * Google Tink proporciona cifrado seguro de alto nivel sin necesidad de
 * manejar directamente claves AES, IVs, etc.
 */
@ApplicationScoped
public class TinkEncryption {

    private static final Logger LOG = Logger.getLogger(TinkEncryption.class);

    @ConfigProperty(name = "app.encryption.enabled", defaultValue = "true")
    boolean encryptionEnabled;

    @ConfigProperty(name = "app.encryption.key-path", defaultValue = "./keys/tink-keyset.json")
    String keyPath;

    private Aead aead;

    /**
     * Inicializa Tink y carga/crea la clave de cifrado
     */
    @PostConstruct
    public void init() {
        if (!encryptionEnabled) {
            LOG.warn("⚠️  Cifrado DESHABILITADO - Solo para desarrollo/testing");
            return;
        }

        try {
            // Registrar configuración AEAD (Authenticated Encryption with Associated Data)
            AeadConfig.register();

            File keyFile = new File(keyPath);
            KeysetHandle keysetHandle;

            if (keyFile.exists()) {
                // Cargar clave existente
                LOG.info("📂 Cargando clave de cifrado desde: " + keyPath);
                keysetHandle = CleartextKeysetHandle.read(
                    JsonKeysetReader.withFile(keyFile)
                );
            } else {
                // Crear nueva clave
                LOG.info("🔐 Creando nueva clave de cifrado...");
                keysetHandle = KeysetHandle.generateNew(AeadKeyTemplates.AES256_GCM);
                
                // Guardar para persistencia
                File parentDir = keyFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                
                CleartextKeysetHandle.write(
                    keysetHandle,
                    JsonKeysetWriter.withFile(keyFile)
                );
                LOG.info("✅ Clave guardada en: " + keyPath);
            }

            // Obtener primitiva AEAD
            this.aead = keysetHandle.getPrimitive(Aead.class);
            LOG.info("✅ Sistema de cifrado Tink inicializado correctamente");

        } catch (GeneralSecurityException | IOException e) {
            LOG.error("❌ Error al inicializar sistema de cifrado", e);
            throw new RuntimeException("No se pudo inicializar el cifrado", e);
        }
    }

    /**
     * Cifra un texto plano
     * 
     * @param plaintext Texto a cifrar
     * @return Texto cifrado en Base64
     */
    public String encrypt(String plaintext) {
        if (!encryptionEnabled) {
            return plaintext; // Pass-through en modo dev
        }

        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }

        try {
            byte[] ciphertext = aead.encrypt(
                plaintext.getBytes(StandardCharsets.UTF_8),
                null // Associated data (opcional)
            );
            return Base64.getEncoder().encodeToString(ciphertext);
        } catch (GeneralSecurityException e) {
            LOG.error("❌ Error al cifrar datos", e);
            throw new RuntimeException("Error en cifrado", e);
        }
    }

    /**
     * Descifra un texto cifrado
     * 
     * @param ciphertext Texto cifrado en Base64
     * @return Texto plano descifrado
     */
    public String decrypt(String ciphertext) {
        if (!encryptionEnabled) {
            return ciphertext; // Pass-through en modo dev
        }

        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }

        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(ciphertext);
            byte[] decryptedBytes = aead.decrypt(encryptedBytes, null);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            LOG.error("❌ Error al descifrar datos", e);
            throw new RuntimeException("Error en descifrado", e);
        }
    }

    /**
     * Verifica si el cifrado está habilitado
     */
    public boolean isEnabled() {
        return encryptionEnabled;
    }

    /**
     * Cifra RUC para almacenamiento seguro
     */
    public String encryptRuc(String ruc) {
        LOG.debug("🔐 Cifrando RUC...");
        return encrypt(ruc);
    }

    /**
     * Descifra RUC desde almacenamiento
     */
    public String decryptRuc(String encryptedRuc) {
        LOG.debug("🔓 Descifrando RUC...");
        return decrypt(encryptedRuc);
    }
}
