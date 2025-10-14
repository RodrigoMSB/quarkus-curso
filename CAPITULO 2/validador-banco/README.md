# 🚀 Capítulo 2: Validador de Cuentas Bancarias con Contract-First

Proyecto Quarkus que implementa Contract-First con OpenAPI para validar números de cuenta bancaria.

---

## 📋 Prerequisitos

### Software necesario:

- **Java 17 o superior** (recomendado Java 21 LTS)
- **Maven 3.9+** (o usar Maven Wrapper incluido)
- **Quarkus CLI** (opcional pero recomendado)
- **IDE** (VS Code, IntelliJ IDEA, Eclipse)

---

## 🛠️ Instalación del Entorno

### 🍎 macOS

**Opción 1: Con Homebrew (Recomendado)**

```bash
# Instalar Homebrew si no lo tienes
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Instalar Java 21
brew install openjdk@21

# Configurar Java
echo 'export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# Instalar Quarkus CLI
brew install quarkusio/tap/quarkus

# Verificar instalación
java -version
quarkus --version
```

**Opción 2: Con SDKMAN**

```bash
# Instalar SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Instalar Java 21
sdk install java 21-tem
sdk use java 21-tem

# Instalar Quarkus CLI
sdk install quarkus

# Verificar
java -version
quarkus --version
```

---

### 🪟 Windows

**Opción 1: Con Chocolatey (Recomendado)**

```powershell
# 1. Instalar Chocolatey (PowerShell como Administrador)
Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

# 2. Instalar Java 21
choco install openjdk21 -y

# 3. Instalar Quarkus CLI
choco install quarkus -y

# 4. Reiniciar PowerShell y verificar
java -version
quarkus --version
```

**Opción 2: Con Scoop**

```powershell
# Instalar Scoop
Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
irm get.scoop.sh | iex

# Agregar bucket de Java
scoop bucket add java

# Instalar herramientas
scoop install openjdk21
scoop install maven
scoop install quarkus-cli

# Verificar
java -version
quarkus --version
```

**Opción 3: Instalación Manual**

1. Descargar Java 21 desde [Adoptium](https://adoptium.net/)
2. Instalar siguiendo el wizard
3. Configurar `JAVA_HOME` y agregar al `PATH`
4. Descargar Quarkus CLI desde [GitHub Releases](https://github.com/quarkusio/quarkus/releases)

---

## 🏗️ Creación del Proyecto Paso a Paso

### **PASO 1: Crear proyecto Quarkus**

**macOS/Linux/Git Bash:**
```bash
quarkus create app cl.alchemicaldata:validador-banco \
  --extension=rest-jackson,smallrye-openapi

cd validador-banco
```

**Windows (CMD/PowerShell):**
```cmd
quarkus create app cl.alchemicaldata:validador-banco --extension=rest-jackson,smallrye-openapi

cd validador-banco
```

**Alternativa con Maven (todas las plataformas):**
```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.28.3:create \
  -DprojectGroupId=cl.alchemicaldata \
  -DprojectArtifactId=validador-banco \
  -Dextensions=rest-jackson,smallrye-openapi
  
cd validador-banco
```

---

### **PASO 2: Agregar extensiones necesarias**

**macOS/Linux/Git Bash:**
```bash
./mvnw quarkus:add-extension -Dextensions="quarkus-openapi-generator,rest-client-jackson"
```

**Windows:**
```cmd
mvnw.cmd quarkus:add-extension -Dextensions="quarkus-openapi-generator,rest-client-jackson"
```

---

### **PASO 3: Crear el contrato OpenAPI (Contract-First)**

**Crear directorio:**
```bash
mkdir -p src/main/openapi
```

**Windows (CMD):**
```cmd
mkdir src\main\openapi
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
quarkus.openapi-generator.codegen.spec.openapi_yaml.base-package=cl.alchemicaldata
```

---

### **PASO 5: Generar código desde el contrato**

**macOS/Linux/Git Bash:**
```bash
./mvnw clean compile
```

**Windows:**
```cmd
mvnw.cmd clean compile
```

Esto generará automáticamente:
- `DefaultApi.java` (interfaz)
- `ValidacionResponse.java` (DTO)

En: `target/generated-sources/open-api/`

---

### **PASO 6: Implementar el Resource**

**Crear archivo:** `src/main/java/cl/alchemicaldata/ValidadorResource.java`

```java
package cl.alchemicaldata;

import cl.alchemicaldata.api.DefaultApi;
import cl.alchemicaldata.model.ValidacionResponse;

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

**macOS/Linux/Git Bash:**
```bash
./mvnw quarkus:dev
```

**Windows:**
```cmd
mvnw.cmd quarkus:dev
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

### **Opción 2: curl (macOS/Linux/Git Bash)**

```bash
# Cuenta válida
curl http://localhost:8080/validar/1234567890

# Cuenta inválida
curl http://localhost:8080/validar/123
```

### **Opción 3: PowerShell (Windows)**

```powershell
Invoke-WebRequest -Uri http://localhost:8080/validar/1234567890 | Select-Object -Expand Content
```

### **Opción 4: Swagger UI**

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
├── mvnw                              # Maven Wrapper (macOS/Linux)
├── mvnw.cmd                          # Maven Wrapper (Windows)
├── pom.xml                           # Configuración Maven
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── cl/alchemicaldata/
│   │   │       ├── GreetingResource.java (generado)
│   │   │       └── ValidadorResource.java
│   │   ├── openapi/
│   │   │   └── openapi.yaml          # ⭐ CONTRATO (primero)
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/
├── target/
│   └── generated-sources/
│       └── open-api/                 # ⭐ CÓDIGO GENERADO
│           └── cl/alchemicaldata/
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

### ❌ Error: "Permission denied: ./mvnw" (macOS/Linux)

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

**Opción 2 - Liberar puerto (macOS/Linux):**
```bash
lsof -ti:8080 | xargs kill -9
```

**Opción 2 - Liberar puerto (Windows PowerShell como admin):**
```powershell
Get-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess | Stop-Process
```

### ❌ Error: "package cl.alchemicaldata.api does not exist"

**Causa:** No se generó el código desde OpenAPI

**Solución:**
```bash
# macOS/Linux
./mvnw clean compile

# Windows
mvnw.cmd clean compile
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
# macOS/Linux
./mvnw quarkus:dev          # Modo desarrollo
./mvnw clean compile        # Compilar
./mvnw test                 # Ejecutar tests
./mvnw package              # Empaquetar JAR

# Windows
mvnw.cmd quarkus:dev
mvnw.cmd clean compile
mvnw.cmd test
mvnw.cmd package
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