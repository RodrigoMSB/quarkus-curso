# 🔐 Cifrado de Datos Sensibles con Google Tink en Quarkus

## 📋 Resumen del Ejercicio

Implementamos **cifrado a nivel de aplicación** para proteger datos sensibles antes de persistirlos en PostgreSQL usando **Google Tink** con **AES-256-GCM**.

### Diferencias clave:
- **Always Encrypted (SQL Server)**: Cifrado transparente manejado por la BD
- **Tink (este ejercicio)**: Cifrado explícito en tu código Java

---

## 🎯 ¿Qué logramos?

✅ **Contenido cifrado en BD**: Texto ilegible (`AebqJ3oc/tkB8ryE+6YZ4i3oWlS/SBhcyPul`)  
✅ **Contenido descifrado en API**: Texto legible (`"Este es un contenido super confidencial..."`)  
✅ **Cifrado simétrico**: AES-256-GCM (rápido, seguro, moderno)  
✅ **Gestión automática**: Cifrar al guardar, descifrar al leer  

---

## 🏗️ Arquitectura del Flujo

```
┌──────────────┐    POST      ┌─────────────────┐    Cifrar    ┌──────────────┐
│   Cliente    │────────────> │ DocumentoResource│────────────> │ CryptoService│
│  (Frontend)  │              │   (Controller)   │              │   (Tink)     │
└──────────────┘              └─────────────────┘              └──────────────┘
                                       │                               │
                        texto plano:   │                               │ texto cifrado:
                        "Contenido     │                               │ "AebqJ3oc..."
                         secreto"      ▼                               ▼
                              ┌─────────────────┐            ┌─────────────────┐
                              │  Repository     │──persist──>│   PostgreSQL    │
                              │  (Panache)      │            │   (Tabla)       │
                              └─────────────────┘            └─────────────────┘
                                       │                               │
                        GET /{id}      │                               │
                              ┌────────┘                               │
                              │                                        │
                              ▼                      SELECT            │
                    ┌─────────────────┐◄──────────────────────────────┘
                    │ Descifrar con   │         texto cifrado
                    │  CryptoService  │         "AebqJ3oc..."
                    └─────────────────┘
                              │
                              │ texto plano:
                              │ "Contenido secreto"
                              ▼
                    Respuesta JSON al cliente
```

---

## 🧩 Componentes Clave

### 1. **CryptoService** - El corazón del cifrado

```java
@ApplicationScoped
public class CryptoService {
    private Aead aead;  // Authenticated Encryption with Associated Data

    @PostConstruct
    public void init() throws Exception {
        AeadConfig.register();
        KeysetHandle keysetHandle = KeysetHandle.generateNew(
            KeyTemplates.get("AES256_GCM")
        );
        this.aead = keysetHandle.getPrimitive(Aead.class);
    }
}
```

**¿Qué hace?**
- **AEAD**: Cifrado autenticado (cifra + verifica integridad)
- **AES-256-GCM**: Algoritmo simétrico moderno y rápido
- **Tink**: Abstrae la complejidad de JCA y previene errores comunes

### 2. **Cifrado antes de persistir**

```java
@POST
@Transactional
public Response crear(DocumentoRequest request) {
    String contenidoCifrado = cryptoService.cifrar(request.contenido);
    Documento documento = new Documento(request.titulo, contenidoCifrado);
    repository.persist(documento);
}
```

**Flujo:**
1. Llega texto plano del cliente
2. Se cifra con Tink
3. Se guarda cifrado en BD
4. Nadie con acceso a la BD puede leer el contenido

### 3. **Descifrado al leer**

```java
private Map<String, Object> buildResponse(Documento doc) {
    response.put("contenido", cryptoService.descifrar(doc.contenidoCifrado));
}
```

**Flujo:**
1. Se lee de BD (texto cifrado)
2. Se descifra con Tink
3. Se devuelve texto plano al cliente autorizado

---

## 🔑 ¿Por qué Google Tink?

| Característica | Tink | JCA Nativo | Jasypt |
|----------------|------|------------|--------|
| **API moderna** | ✅ Simple | ❌ Compleja | ⚠️ Antigua |
| **Seguro por diseño** | ✅ Previene errores | ❌ Fácil meter la pata | ⚠️ Depende |
| **Rotación de claves** | ✅ Built-in | ❌ Manual | ❌ Manual |
| **Performance** | ✅ Optimizado | ✅ Nativo | ⚠️ Overhead |
| **Mantenimiento** | ✅ Google | ✅ Oracle | ⚠️ Comunidad |

**Analogía:** Tink es como TypeScript para JavaScript de criptografía. Te protege de errores comunes y te da mejores abstracciones.

---

## ⚠️ PROBLEMA CRÍTICO ACTUAL

### 🚨 La clave se regenera cada vez que la app arranca

```java
@PostConstruct
public void init() {
    KeysetHandle keysetHandle = KeysetHandle.generateNew(...);  // ❌ NUEVA clave cada vez
}
```

**Consecuencia:**
- Reinicia la app → Pierdes acceso a todos los datos cifrados anteriores
- Los documentos cifrados con la clave anterior **NO SE PUEDEN DESCIFRAR**

**Solución en PRODUCCIÓN:**

1. **Guardar la clave en un KeyStore externo:**
   ```java
   // Guardar clave en archivo
   String keysetFilename = "my-keyset.json";
   CleartextKeysetHandle.write(keysetHandle, 
       JsonKeysetWriter.withFile(new File(keysetFilename)));
   
   // Cargar clave existente
   KeysetHandle keysetHandle = CleartextKeysetHandle.read(
       JsonKeysetReader.withFile(new File(keysetFilename)));
   ```

2. **Usar un KMS (Key Management Service):**
   - AWS KMS
   - Google Cloud KMS
   - Azure Key Vault
   - HashiCorp Vault

3. **Variables de entorno cifradas:**
   ```java
   String encryptedKeyset = System.getenv("TINK_KEYSET");
   ```

---

## 🎓 Analogía: La caja fuerte

Imagina que:

- **Contenido del documento** = Joyas valiosas
- **Clave de cifrado** = Llave de la caja fuerte
- **Base de datos** = Banco donde guardas la caja

**Situación actual (DEMO):**
- Cada vez que abres tu app, generas una **nueva llave**
- Las cajas antiguas quedan con llaves perdidas → imposible abrirlas

**Situación en PRODUCCIÓN:**
- Guardas tu llave maestra en un lugar seguro (KMS)
- Siempre usas la misma llave para todas las cajas
- Si pierdes la llave → pierdes TODO (no hay "resetear contraseña")

---

## 🔐 Conceptos de Criptografía

### Cifrado Simétrico (AES)
- **Misma clave** para cifrar y descifrar
- **Rápido**: ideal para grandes volúmenes de datos
- **Problema**: ¿Cómo compartir la clave de forma segura?

### AEAD (Authenticated Encryption with Associated Data)
- **Cifra** el contenido
- **Autentica** que no fue modificado
- **GCM** (Galois/Counter Mode): implementación moderna y eficiente

### Base64
- **NO es cifrado**, es solo codificación
- Convierte bytes binarios en texto ASCII
- Usado aquí para guardar el texto cifrado en la columna TEXT de PostgreSQL

---

## 🛡️ Mejores Prácticas

### ✅ Lo que hicimos bien:
1. Cifrado simétrico fuerte (AES-256-GCM)
2. Separación de responsabilidades (CryptoService)
3. Cifrado transparente para el resto de la app
4. Validación de errores en el Resource

### ⚠️ Lo que falta para PRODUCCIÓN:
1. **Gestión persistente de claves** (KMS)
2. **Rotación de claves** periódica
3. **Auditoría** de accesos a datos sensibles
4. **Control de acceso** (¿quién puede descifrar?)
5. **Backup seguro** de las claves

---

## 📊 Comparación: Dónde aplicar cada tipo de cifrado

| Escenario | Always Encrypted | Tink (App-level) | Ambos |
|-----------|------------------|------------------|-------|
| BD comprometida | ✅ | ✅ | ✅ |
| Logs comprometidos | ❌ | ✅ | ✅ |
| Control granular | ❌ | ✅ | ✅ |
| Performance | ⚠️ | ✅ | ⚠️ |
| Complejidad | Alta | Media | Muy Alta |
| Búsquedas cifradas | ⚠️ Limitado | ❌ | ⚠️ |

**Recomendación para tu curso:**
- **Always Encrypted**: Para datos que SQL Server debe proteger (ideal para Windows + Azure)
- **Tink**: Para control total en la app (multiplataforma, cualquier BD)
- **Ambos**: Defense in depth (paranoia máxima) 🛡️

---

## 🚀 Próximos Pasos para el estudio personal

1. **Ejercicio:** Implementar persistencia de claves en archivo JSON
2. **Ejercicio:** Agregar campo "tipo_documento" y usar claves diferentes por tipo
3. **Investigar:** Cifrado híbrido (asimétrico + simétrico)
4. **Challenge:** Implementar búsqueda en campos cifrados (hint: hash)
5. **Avanzado:** Integración con HashiCorp Vault

---

## 📚 Referencias

- **Google Tink**: https://github.com/google/tink
- **Tink Java Docs**: https://github.com/google/tink/tree/master/java
- **AEAD**: https://tools.ietf.org/html/rfc5116
- **AES-GCM**: https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-38d.pdf

---
