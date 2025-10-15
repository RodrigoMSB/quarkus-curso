# 🔐 Cifrado de Datos Sensibles con Google Tink en Quarkus

Ejercicio práctico de cifrado a nivel de aplicación usando Google Tink con AES-256-GCM para proteger datos sensibles antes de persistirlos en PostgreSQL.

> 📚 **Documentación complementaria:**  
> • [Ver fundamentos teóricos de criptografía](./TEORIA.md)  
> • [Ver conceptos técnicos del cifrado con Tink](./CIFRADO.md)

---

## 📋 Requisitos Previos

- **Java 21** (JDK)
- **Maven 3.8+**
- **PostgreSQL 12+** (instalado y corriendo)
- Cliente SQL (pgAdmin, DBeaver, psql, etc.)
- **curl** o Postman para probar la API

---

## 🚀 Instalación y Configuración

### 1. Crear el proyecto

```bash
mvn io.quarkus:quarkus-maven-plugin:3.28.3:create \
    -DprojectGroupId=pe.banco \
    -DprojectArtifactId=documentos-cifrados \
    -DprojectVersion=1.0.0-SNAPSHOT \
    -Dextensions="hibernate-orm-panache,jdbc-postgresql,rest-jackson,rest"

cd documentos-cifrados
```

### 2. Agregar Google Tink al pom.xml

Agrega esta dependencia en la sección `<dependencies>`:

```xml
<dependency>
    <groupId>com.google.crypto.tink</groupId>
    <artifactId>tink</artifactId>
    <version>1.15.0</version>
</dependency>
```

### 3. Configurar PostgreSQL

Edita `src/main/resources/application.properties`:

```properties
# PostgreSQL Configuration
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=postgres
quarkus.datasource.password=postgres
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/postgres

# Hibernate
quarkus.hibernate-orm.database.generation=drop-and-create
quarkus.hibernate-orm.log.sql=true

# HTTP
quarkus.http.port=8080
```

**Ajusta** `username`, `password` y el nombre de la base de datos según tu instalación local.

---

## 📁 Estructura del Proyecto

```
documentos-cifrados/
├── src/main/java/pe/banco/documentos/
│   ├── entity/
│   │   └── Documento.java              # Entidad JPA
│   ├── repository/
│   │   └── DocumentoRepository.java    # Repositorio Panache
│   ├── service/
│   │   └── CryptoService.java          # Servicio de cifrado Tink
│   ├── dto/
│   │   └── DocumentoRequest.java       # DTO para requests
│   └── resource/
│       └── DocumentoResource.java      # REST Controller
└── src/main/resources/
    └── application.properties           # Configuración
```

---

## 🏗️ Componentes Principales

### CryptoService.java

Servicio encargado de cifrar y descifrar usando Google Tink con AES-256-GCM:

```java
@ApplicationScoped
public class CryptoService {
    private Aead aead;

    @PostConstruct
    public void init() throws Exception {
        AeadConfig.register();
        KeysetHandle keysetHandle = KeysetHandle.generateNew(
            KeyTemplates.get("AES256_GCM")
        );
        this.aead = keysetHandle.getPrimitive(Aead.class);
    }

    public String cifrar(String textoPlano) throws Exception { ... }
    public String descifrar(String textoCifrado) throws Exception { ... }
}
```

**⚠️ IMPORTANTE:** La clave se genera en cada inicio. En producción debe persistirse en un KMS o archivo seguro.

### Documento.java

Entidad que almacena documentos con contenido cifrado:

```java
@Entity
public class Documento extends PanacheEntity {
    public String titulo;
    
    @Column(name = "contenido_cifrado", columnDefinition = "TEXT")
    public String contenidoCifrado;  // Almacenado CIFRADO
    
    @Column(name = "fecha_creacion")
    public LocalDateTime fechaCreacion;
}
```

### DocumentoResource.java

Controlador REST que:
- **Cifra** el contenido antes de persistir
- **Descifra** el contenido al leer de BD
- Expone endpoints REST para CRUD

---

## ▶️ Ejecutar la Aplicación

### Modo desarrollo (con hot reload)

```bash
./mvnw quarkus:dev
```

La aplicación estará disponible en: **http://localhost:8080**

Deberías ver en los logs:
```
Listening on: http://localhost:8080
```

---

## 🧪 Probar la API

### 1. Crear un documento (cifrado automático)

```bash
curl -X POST http://localhost:8080/api/v1/documentos \
  -H "Content-Type: application/json" \
  -d '{
    "titulo": "Documento Secreto",
    "contenido": "Este es un contenido super confidencial que será cifrado"
  }'
```

**Respuesta esperada:**
```json
{
  "id": 1,
  "titulo": "Documento Secreto",
  "contenido": "Este es un contenido super confidencial que será cifrado",
  "fechaCreacion": "2025-10-15T18:42:37.055908"
}
```

✅ El contenido se devuelve **descifrado** en la respuesta.

### 2. Listar todos los documentos

```bash
curl http://localhost:8080/api/v1/documentos
```

### 3. Buscar documento por ID

```bash
curl http://localhost:8080/api/v1/documentos/1
```

---

## 🔍 Verificar el Cifrado en la Base de Datos

Abre tu cliente SQL y ejecuta:

```sql
SELECT * FROM documento;
```

**Resultado esperado:**

| id | titulo | contenido_cifrado | fecha_creacion |
|----|--------|-------------------|----------------|
| 1 | Documento Secreto | `AebqJ3oc/tkB8ryE+6YZ4i3oWlS/SBhcyPul` | 2025-10-15... |

🔐 **Observa que `contenido_cifrado` contiene texto ilegible (Base64)**, mientras que el API devuelve el contenido descifrado.

---

## 🎯 Flujo de Cifrado/Descifrado

```
POST /documentos
    ↓
Cliente envía: "Contenido secreto"
    ↓
CryptoService.cifrar()
    ↓
PostgreSQL guarda: "AebqJ3oc/tkB8ryE..."  ← CIFRADO
    ↓
GET /documentos/1
    ↓
PostgreSQL lee: "AebqJ3oc/tkB8ryE..."
    ↓
CryptoService.descifrar()
    ↓
API devuelve: "Contenido secreto"  ← DESCIFRADO
```

**Concepto clave:** La base de datos nunca almacena el contenido en texto plano.

---

## 🔐 Conceptos de Seguridad

### ¿Qué es AEAD?

**Authenticated Encryption with Associated Data**

- **Cifra** el contenido (confidencialidad)
- **Autentica** que no fue modificado (integridad)
- **AES-256-GCM**: Algoritmo simétrico moderno y eficiente

### ¿Por qué Google Tink?

- API simple y segura por diseño
- Previene errores comunes de criptografía
- Soporte para rotación de claves
- Mantenido por Google
- Integración con KMS (AWS, GCP, Azure)

### Cifrado Simétrico vs Asimétrico

| Tipo | Clave | Velocidad | Uso |
|------|-------|-----------|-----|
| **Simétrico** (AES) | Misma clave para cifrar/descifrar | ⚡ Rápido | Datos en reposo |
| **Asimétrico** (RSA) | Par de claves (pública/privada) | 🐌 Lento | Intercambio de claves |

En este ejercicio usamos **simétrico** porque es ideal para cifrar grandes volúmenes de datos.

---

## ⚠️ Limitaciones de la Implementación Actual

### 🚨 Clave efímera (solo para DEMO)

La clave se **regenera cada vez** que la aplicación arranca:

```java
KeysetHandle keysetHandle = KeysetHandle.generateNew(...);  // ❌ Nueva cada vez
```

**Consecuencia:**
- Reiniciar la app → Pierdes acceso a datos cifrados anteriormente
- Los documentos NO se pueden descifrar con la nueva clave

### ✅ Solución para PRODUCCIÓN

**Opción 1: Persistir en archivo**

```java
// Guardar clave
String keysetFilename = "tink-keyset.json";
CleartextKeysetHandle.write(keysetHandle, 
    JsonKeysetWriter.withFile(new File(keysetFilename)));

// Cargar clave existente
KeysetHandle keysetHandle = CleartextKeysetHandle.read(
    JsonKeysetReader.withFile(new File(keysetFilename)));
```

**Opción 2: Usar KMS (recomendado)**
- AWS KMS
- Google Cloud KMS
- Azure Key Vault
- HashiCorp Vault

**Opción 3: Variable de entorno**

```java
String keysetJson = System.getenv("TINK_KEYSET");
```

---

## 🐛 Troubleshooting

### Error: "Could not find or load main class"

```bash
./mvnw clean install
./mvnw quarkus:dev
```

### Error: "Connection refused to PostgreSQL"

Verifica que PostgreSQL esté corriendo:

```bash
# Linux/Mac
sudo service postgresql status

# O verifica el puerto
netstat -an | grep 5432
```

### Error: "GeneralSecurityException: decryption failed"

La clave cambió (app reiniciada). Borra los datos:

```sql
TRUNCATE TABLE documento RESTART IDENTITY;
```

### Base de datos con caracteres raros

Verifica encoding UTF-8 en PostgreSQL:

```sql
SHOW client_encoding;
```

---

## 📚 Ejercicios Propuestos

1. **Persistencia de clave:** Modifica `CryptoService` para guardar/cargar la clave desde un archivo JSON

2. **Múltiples claves:** Implementa diferentes claves para diferentes tipos de documentos (públicos, privados, confidenciales)

3. **Cifrado híbrido:** Investiga cómo usar RSA para cifrar la clave AES y AES para cifrar el contenido

4. **Búsqueda segura:** Implementa búsqueda por hash del contenido sin descifrar (hint: SHA-256)

5. **Auditoría:** Agrega logs de quién accede a datos descifrados

---

## 🔗 Referencias

- [Google Tink Documentation](https://github.com/google/tink)
- [Tink Java HOW-TO](https://github.com/google/tink/blob/master/docs/JAVA-HOWTO.md)
- [AEAD Specification (RFC 5116)](https://tools.ietf.org/html/rfc5116)
- [Quarkus Hibernate ORM Guide](https://quarkus.io/guides/hibernate-orm)

---

## 📝 Notas Finales

### Diferencias con Always Encrypted

| Aspecto | Always Encrypted | Tink (App-level) |
|---------|------------------|------------------|
| **Dónde se cifra** | SQL Server | Aplicación Java |
| **Gestión de claves** | Windows Cert Store / Azure Key Vault | KMS o archivo |
| **Portabilidad** | Solo SQL Server | Cualquier BD |
| **Control** | Limitado | Total |
| **Complejidad** | Alta configuración | Código explícito |

### Cuándo usar cada uno

- **Always Encrypted:** Cuando SQL Server debe proteger datos sin que la app vea las claves
- **Tink:** Cuando necesitas control total y portabilidad entre BDs
- **Ambos:** Defense in depth (máxima paranoia) 🛡️

---

## 🎓 Puntos Clave

✅ El cifrado se hace **antes de persistir**, el descifrado **después de leer**  
✅ La BD nunca almacena contenido en texto plano  
✅ AEAD garantiza confidencialidad + integridad  
✅ En producción: **NUNCA** regenerar claves, usar KMS  
✅ Perder la clave = perder TODOS los datos cifrados (sin recuperación)  

---

## 👨‍💻 Autor

Ejercicio desarrollado para el curso de Quarkus - Capítulo 4: Persistencia y Seguridad

---

**¡Happy coding!** 🚀🔐