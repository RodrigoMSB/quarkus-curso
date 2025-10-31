# 🚀 Capítulo 2: Validador de Cuentas Bancarias con Contract-First

Proyecto Quarkus que implementa Contract-First con OpenAPI para validar números de cuenta bancaria.

---

## 📋 Prerequisitos

**Asegúrate de tener instalado** (del Capítulo 1):
- ✅ Java 17 o superior (recomendado Java 21 LTS)
- ✅ Quarkus CLI
- ✅ Git Bash (Windows) o Terminal (macOS)

**Verificar:**
```bash
java -version
quarkus --version
```

Si falta algo, revisa el Capítulo 1.

---

## 🏗️ Creación del Proyecto Paso a Paso

### **PASO 1: Crear proyecto Quarkus**

```bash
quarkus create app pe.banco:validador-banco \
  --extension=rest-jackson,smallrye-openapi

cd validador-banco

# Dar permisos al Maven Wrapper (primera vez)
chmod +x mvnw
```

**Alternativa con Maven:**
```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.15.1:create \
  -DprojectGroupId=pe.banco \
  -DprojectArtifactId=validador-banco \
  -Dextensions=rest-jackson,smallrye-openapi
  
cd validador-banco
chmod +x mvnw
```

---

### **PASO 2: Agregar extensiones necesarias**

```bash
./mvnw quarkus:add-extension -Dextensions="quarkus-openapi-generator,rest-client-jackson"
```

---

### **PASO 3: Crear el contrato OpenAPI (Contract-First)**

**Crear directorio:**
```bash
mkdir -p src/main/openapi
```

**Crear archivo:** `src/main/openapi/openapi.yaml`

```yaml
openapi: 3.0.3
info:
  title: API Validador de Cuentas Bancarias
  version: 1.0.0
  description: Microservicio para validar números de cuenta bancaria

paths:
  /validar/{numeroCuenta}:
    get:
      summary: Validar formato de cuenta bancaria
      operationId: validarNumeroCuentaGet
      parameters:
        - name: numeroCuenta
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':
          description: Validación exitosa
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ValidacionResponse'

components:
  schemas:
    ValidacionResponse:
      type: object
      properties:
        valido:
          type: boolean
        numeroCuenta:
          type: string
        mensaje:
          type: string
```

---

### **PASO 4: Configurar OpenAPI Generator**

**Editar:** `src/main/resources/application.properties`

```properties
quarkus.http.port=8080

# Configuración OpenAPI Generator
quarkus.openapi-generator.codegen.spec.openapi_yaml.base-package=pe.banco
```

---

### **PASO 5: Generar código desde el contrato**

```bash
./mvnw clean compile
```

Esto generará automáticamente:
- `DefaultApi.java` (interfaz)
- `ValidacionResponse.java` (DTO)

En: `target/generated-sources/open-api/`

---

### **PASO 6: Implementar el Resource**

**Crear archivo:** `src/main/java/pe/banco/ValidadorResource.java`

```java
package pe.banco;

import pe.banco.api.DefaultApi;
import pe.banco.model.ValidacionResponse;

public class ValidadorResource implements DefaultApi {

    @Override
    public ValidacionResponse validarNumeroCuentaGet(String numeroCuenta) {
        
        ValidacionResponse response = new ValidacionResponse();
        response.setNumeroCuenta(numeroCuenta);
        
        boolean esValido = validarFormato(numeroCuenta);
        response.setValido(esValido);
        response.setMensaje(esValido 
            ? "Cuenta válida: formato correcto" 
            : "Cuenta inválida: debe tener 10 dígitos numéricos");
        
        return response;
    }
    
    private boolean validarFormato(String numero) {
        return numero != null 
            && numero.length() == 10 
            && numero.matches("\\d+");
    }
}
```

---

### **PASO 7: Ejecutar en Dev Mode**

```bash
./mvnw quarkus:dev
```

**Salida esperada:**
```
__  ____  __  _____   ___  __ ____  ______ 
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   

INFO  [io.quarkus] validador-banco 1.0.0-SNAPSHOT on JVM started in 1.234s
INFO  [io.quarkus] Listening on: http://localhost:8080

Tests paused
Press [r] to resume testing, [h] for more options>
```

---

## 🧪 Probar el Microservicio

### **Opción 1: Navegador**

```
http://localhost:8080/validar/1234567890
```

**Respuesta esperada:**
```json
{
  "valido": true,
  "numeroCuenta": "1234567890",
  "mensaje": "Cuenta válida: formato correcto"
}
```

### **Opción 2: curl (macOS y Git Bash)**

```bash
# Cuenta válida
curl http://localhost:8080/validar/1234567890

# Cuenta inválida
curl http://localhost:8080/validar/123
```

### **Opción 3: Swagger UI**

```
http://localhost:8080/q/swagger-ui
```

Aquí puedes:
1. Ver la documentación generada desde el contrato
2. Probar el endpoint interactivamente
3. Ver el esquema del `ValidacionResponse`

---

## 🔥 Experimentar con Hot Reload

1. **Deja corriendo** el Dev Mode (no lo detengas)

2. **Modifica** `ValidadorResource.java`, línea del mensaje:

```java
response.setMensaje(esValido 
    ? "✅ Cuenta APROBADA - Todo correcto" 
    : "❌ Cuenta RECHAZADA - Formato inválido");
```

3. **Guarda** el archivo (Cmd+S / Ctrl+S)

4. **Refresca** el navegador

**¡Los cambios se aplican INSTANTÁNEAMENTE sin reiniciar!** 🔥

---

## 📁 Estructura del Proyecto

```
validador-banco/
├── mvnw                              # Maven Wrapper (macOS/Linux/Git Bash)
├── mvnw.cmd                          # Maven Wrapper (Windows CMD - no usar)
├── pom.xml                           # Configuración Maven
├── .gitignore                        # Exclusiones Git
├── .dockerignore                     # Exclusiones Docker
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── pe/banco/
│   │   │       ├── GreetingResource.java (generado)
│   │   │       └── ValidadorResource.java
│   │   ├── openapi/
│   │   │   └── openapi.yaml          # ⭐ CONTRATO (primero)
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/
│           └── pe/banco/
├── target/
│   └── generated-sources/
│       └── open-api/                 # ⭐ CÓDIGO GENERADO
│           └── pe/banco/
│               ├── api/
│               │   └── DefaultApi.java
│               └── model/
│                   └── ValidacionResponse.java
```

---

## 🎯 Conceptos Cubiertos

### ✅ **Contract-First con OpenAPI**
- Diseñar especificación OpenAPI **antes** de programar
- Generar código automáticamente desde el contrato
- Garantizar que la implementación cumple el contrato

### ✅ **Estructura de Proyecto Maven**
- `pom.xml`: dependencias y plugins
- `src/main/java`: código fuente
- `src/main/resources`: configuración
- `target/`: archivos compilados y generados

### ✅ **Extensiones de Quarkus**
- `rest-jackson`: REST + JSON
- `smallrye-openapi`: Especificación OpenAPI
- `quarkus-openapi-generator`: Generación de código
- `rest-client-jackson`: Cliente REST

### ✅ **Dev Mode**
- Hot reload automático
- Continuous testing
- Dev UI en `/q/dev`
- Swagger UI en `/q/swagger-ui`

---

## 🚨 Solución de Problemas

### ❌ Error: "Permission denied: ./mvnw"

```bash
chmod +x mvnw
./mvnw quarkus:dev
```

### ❌ Error: "Port 8080 already in use"

**Opción 1 - Cambiar puerto:**

En `application.properties`:
```properties
quarkus.http.port=8081
```

**Opción 2 - Liberar puerto (macOS/Linux/Git Bash):**
```bash
lsof -ti:8080 | xargs kill -9
```

### ❌ Error: "package pe.banco.api does not exist"

**Causa:** No se generó el código desde OpenAPI

**Solución:**
```bash
./mvnw clean compile
```

### ❌ VSCode no reconoce el package

**Solución 1: Recargar ventana**
- Cmd/Ctrl + Shift + P
- Escribir: `Reload Window`
- Enter

**Solución 2: Compilar**
```bash
./mvnw clean compile
```

### ❌ Error: "cannot find symbol: class ValidacionResponse"

**Causa:** Falta generar código o VSCode no sincronizó

**Solución:**
1. Compilar: `./mvnw clean compile`
2. Recargar VSCode: Cmd/Ctrl + Shift + P → `Reload Window`

---

## 📊 Comandos Útiles

### **Desarrollo**

```bash
./mvnw quarkus:dev          # Modo desarrollo
./mvnw clean compile        # Compilar
./mvnw test                 # Ejecutar tests
./mvnw package              # Empaquetar JAR
```

### **En Dev Mode**

| Tecla | Acción |
|-------|--------|
| **`w`** | Abrir Dev UI |
| **`d`** | Abrir documentación |
| **`r`** | Ejecutar tests |
| **`s`** | Ver métricas |
| **`h`** | Ver ayuda |
| **`q`** | Salir |

---

## 🔗 URLs Importantes

| Recurso | URL |
|---------|-----|
| **Endpoint** | http://localhost:8080/validar/{numeroCuenta} |
| **Swagger UI** | http://localhost:8080/q/swagger-ui |
| **OpenAPI Spec** | http://localhost:8080/q/openapi |
| **Dev UI** | http://localhost:8080/q/dev |
| **Health Check** | http://localhost:8080/q/health |
| **Metrics** | http://localhost:8080/q/metrics |

---

## 📚 Flujo Contract-First

```
1. DISEÑAR CONTRATO
   └─→ openapi.yaml (definir API)
   
2. GENERAR CÓDIGO
   └─→ mvn compile (genera interfaces y DTOs)
   
3. IMPLEMENTAR
   └─→ ValidadorResource implements DefaultApi
   
4. EJECUTAR
   └─→ mvnw quarkus:dev
   
5. VALIDAR
   └─→ Swagger UI verifica cumplimiento del contrato
```

---

## 🎓 Ejercicio Completado

**Has aprendido:**

✅ Crear proyecto Quarkus desde CLI  
✅ Configurar extensiones necesarias  
✅ Diseñar contratos OpenAPI primero  
✅ Generar código automáticamente  
✅ Implementar interfaces generadas  
✅ Usar Dev Mode con Hot Reload  
✅ Probar con Swagger UI  
✅ Validar cumplimiento de contratos  

---

## 📖 Recursos Adicionales

- [Documentación Quarkus](https://quarkus.io/guides/)
- [OpenAPI Generator](https://github.com/quarkiverse/quarkus-openapi-generator)
- [OpenAPI Specification](https://swagger.io/specification/)
- [Quarkus Dev Services](https://quarkus.io/guides/dev-services)

---

**🎉 ¡Proyecto completado exitosamente!**

*Ahora estás listo para desarrollar microservicios con Contract-First en Quarkus.*