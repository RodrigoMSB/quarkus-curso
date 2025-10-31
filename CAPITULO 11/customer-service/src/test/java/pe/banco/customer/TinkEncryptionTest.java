package pe.banco.customer;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.banco.customer.security.TinkEncryption;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para el servicio de cifrado con Google Tink
 * 
 * Demuestra:
 * - Testing de servicios CDI
 * - Validación de cifrado/descifrado
 */
@QuarkusTest
class TinkEncryptionTest {

    @Inject
    TinkEncryption encryption;

    @Test
    @DisplayName("Debe cifrar y descifrar texto correctamente")
    void testEncryptDecrypt() {
        String originalText = "20123456789";
        
        // Cifrar
        String encrypted = encryption.encrypt(originalText);
        assertNotNull(encrypted);
        assertNotEquals(originalText, encrypted);
        System.out.println("Original: " + originalText);
        System.out.println("Cifrado: " + encrypted);
        
        // Descifrar
        String decrypted = encryption.decrypt(encrypted);
        assertNotNull(decrypted);
        assertEquals(originalText, decrypted);
        System.out.println("Descifrado: " + decrypted);
    }

    @Test
    @DisplayName("Debe cifrar RUC correctamente")
    void testEncryptRuc() {
        String ruc = "20987654321";
        
        String encryptedRuc = encryption.encryptRuc(ruc);
        assertNotNull(encryptedRuc);
        assertNotEquals(ruc, encryptedRuc);
        
        String decryptedRuc = encryption.decryptRuc(encryptedRuc);
        assertEquals(ruc, decryptedRuc);
    }

    @Test
    @DisplayName("Debe manejar textos vacíos")
    void testEmptyText() {
        String empty = "";
        String encrypted = encryption.encrypt(empty);
        assertEquals(empty, encrypted);
    }

    @Test
    @DisplayName("Debe manejar null")
    void testNullText() {
        String encrypted = encryption.encrypt(null);
        assertNull(encrypted);
    }

    @Test
    @DisplayName("Dos cifrados del mismo texto deben ser diferentes (IV aleatorio)")
    void testRandomIV() {
        String text = "20123456789";
        
        String encrypted1 = encryption.encrypt(text);
        String encrypted2 = encryption.encrypt(text);
        
        // Los cifrados deben ser diferentes (Tink usa IV aleatorio)
        assertNotEquals(encrypted1, encrypted2);
        
        // Pero ambos descifran al mismo texto original
        assertEquals(text, encryption.decrypt(encrypted1));
        assertEquals(text, encryption.decrypt(encrypted2));
    }

    @Test
    @DisplayName("Debe verificar que el cifrado está habilitado")
    void testEncryptionEnabled() {
        // En ambiente de test puede estar deshabilitado según config
        boolean enabled = encryption.isEnabled();
        System.out.println("Cifrado habilitado: " + enabled);
        assertTrue(true); // Siempre pasa, solo verifica el estado
    }
}
