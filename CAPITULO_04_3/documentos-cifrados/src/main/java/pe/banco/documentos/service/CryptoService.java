package pe.banco.documentos.service;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.KeyTemplates;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.KeyTemplates;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@ApplicationScoped
public class CryptoService {

    private Aead aead;

    @PostConstruct
    public void init() throws Exception {
        // Registrar configuración AEAD (AES-GCM)
        AeadConfig.register();
        
        // Generar una clave (en producción se carga de un KeyStore seguro)
        KeysetHandle keysetHandle = KeysetHandle.generateNew(
            KeyTemplates.get("AES256_GCM")
        );
        
        // Obtener el primitivo AEAD para cifrar/descifrar
        this.aead = keysetHandle.getPrimitive(Aead.class);
    }

    public String cifrar(String textoPlano) throws Exception {
        byte[] textoCifrado = aead.encrypt(
            textoPlano.getBytes(StandardCharsets.UTF_8),
            null  // associated data (opcional)
        );
        return Base64.getEncoder().encodeToString(textoCifrado);
    }

    public String descifrar(String textoCifrado) throws Exception {
        byte[] cifradoBytes = Base64.getDecoder().decode(textoCifrado);
        byte[] textoPlano = aead.decrypt(cifradoBytes, null);
        return new String(textoPlano, StandardCharsets.UTF_8);
    }
}