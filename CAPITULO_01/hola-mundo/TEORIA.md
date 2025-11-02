# 🚀 Proyecto Quarkus - Hola Mundo

Proyecto básico de Quarkus con endpoint REST simple para aprender desarrollo de microservicios.

---

## 🔍 VERIFICACIÓN INICIAL (OBLIGATORIO)

**⚠️ IMPORTANTE:** Antes de instalar cualquier cosa, verifica qué tienes y qué te falta.

### Para macOS

```bash
# Verificar si tienes Homebrew
brew --version

# Verificar si tienes Java
java -version

# Verificar si tienes Quarkus CLI
quarkus --version

# Ver resumen completo
echo "=== Estado de tu sistema ==="
echo "Homebrew: $(brew --version 2>/dev/null || echo 'NO INSTALADO')"
echo "Java: $(java -version 2>&1 | head -1 || echo 'NO INSTALADO')"
echo "Quarkus: $(quarkus --version 2>/dev/null || echo 'NO INSTALADO')"
```

### Para Windows (con Git Bash)

```bash
# Verificar si tienes Git Bash (si estás leyendo esto aquí, ya lo tienes)
echo "Git Bash: OK"

# Verificar si tienes Java
java -version

# Verificar si tienes Quarkus CLI
quarkus --version

# Ver resumen completo
echo "=== Estado de tu sistema ==="
echo "Git Bash: OK"
echo "Java: $(java -version 2>&1 | head -1 || echo 'NO INSTALADO')"
echo "Quarkus: $(quarkus --version 2>/dev/null || echo 'NO INSTALADO')"
```

**Resultado esperado:**
```
Java: openjdk version "21.0.x" o superior
Quarkus: 3.15.x o superior
```

---

## 📋 Prerequisitos

- **Java 17 o superior** (recomendado Java 21 LTS)
- **Maven 3.9+** (incluido en el proyecto como Maven Wrapper, no requiere instalación)
- **IDE** (VS Code, IntelliJ IDEA, Eclipse)
- **Terminal:**
  - 🍎 **macOS:** Terminal (Zsh por defecto)
  - 🪟 **Windows:** Git Bash (requerido)

> **Nota para Windows:** Este curso utiliza **Git Bash** como terminal estándar. Si no lo tienes, instálalo desde [git-scm.com](https://git-scm.com/downloads)

---

## 🛠️ Instalación del Entorno

### 🍎 macOS

**Opción 1: Con Homebrew (Recomendado)**

```bash
# 1. Instalar Homebrew si no lo tienes
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 2. Instalar Java 21
brew install openjdk@21

# 3. Configurar Java en el PATH
echo 'export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# 4. Instalar Quarkus CLI
brew install quarkusio/tap/quarkus

# 5. Verificar instalación
java -version
quarkus --version
```

**Opción 2: Con SDKMAN (Para gestión avanzada de versiones)**

```bash
# 1. Instalar SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# 2. Instalar Java 21
sdk install java 21-tem
sdk use java 21-tem

# 3. Instalar Quarkus CLI
sdk install quarkus

# 4. Verificar instalación
java -version
quarkus --version
```

---

### 🪟 Windows (con Git Bash)

**Opción 1: Con Chocolatey (Recomendado)**

> **Nota:** Estos comandos se ejecutan en **PowerShell como Administrador**, luego cambias a Git Bash

```powershell
# 1. Instalar Chocolatey (ejecutar en PowerShell como Administrador)
Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

# 2. Instalar Java 21
choco install openjdk21 -y

# 3. Instalar Quarkus CLI
choco install quarkus -y

# 4. Cerrar PowerShell y abrir Git Bash, luego verificar:
```

Ahora en **Git Bash**:
```bash
java -version
quarkus --version
```

**Opción 2: Con Scoop (Alternativa moderna)**

En **PowerShell normal** (no requiere administrador):

```powershell
# 1. Instalar Scoop
Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
irm get.scoop.sh | iex

# 2. Agregar bucket de Java
scoop bucket add java

# 3. Instalar herramientas
scoop install openjdk21
scoop install maven

# Nota: Quarkus CLI no está disponible en Scoop, instalar manualmente (ver Opción 3)
```

Ahora en **Git Bash**:
```bash
java -version
mvn --version
```

**Opción 3: Instalación Manual**

1. **Descargar Java 21:**
   - Ir a [Adoptium](https://adoptium.net/)
   - Descargar "Eclipse Temurin 21 (LTS)" para Windows
   - Instalar siguiendo el wizard (marcar "Add to PATH")

2. **Configurar JAVA_HOME en Git Bash (Recomendado - MÁS SIMPLE):**

   **Paso 1: Encontrar dónde está instalado Java**
   ```bash
   # Abrir Git Bash y ejecutar:
   which java
   ```
   
   **Resultado esperado:**
   ```
   /c/Program Files/Eclipse Adoptium/jdk-21.0.5+11/bin/java
   ```
   
   **Paso 2: Copiar la ruta SIN el `/bin/java` al final**
   - Del ejemplo anterior, tu JAVA_HOME es: `/c/Program Files/Eclipse Adoptium/jdk-21.0.5+11`
   
   **Paso 3: Configurar JAVA_HOME (reemplazar la ruta con la tuya)**
   ```bash
   echo 'export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.5+11"' >> ~/.bashrc
   echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.bashrc
   ```
   
   **Paso 4: Recargar la configuración**
   ```bash
   source ~/.bashrc
   ```
   
   **Paso 5: Verificar**
   ```bash
   echo $JAVA_HOME
   # Debe mostrar: /c/Program Files/Eclipse Adoptium/jdk-21.0.5+11
   
   java -version
   javac -version
   ```

3. **Alternativa: Configurar JAVA_HOME en Variables de Entorno de Windows**

   Si prefieres configurarlo a nivel de sistema Windows (no solo en Git Bash):

   **Paso 1: Encontrar la ruta exacta**
   - Abrir Explorador de Windows
   - Navegar a: `C:\Program Files\Eclipse Adoptium\`
   - Anotar el nombre completo de la carpeta (ej: `jdk-21.0.5+11`)

   **Paso 2: Abrir Variables de Entorno**
   - Presionar `Windows + R`
   - Escribir: `sysdm.cpl` y presionar Enter
   - Ir a la pestaña "Opciones avanzadas"
   - Clic en "Variables de entorno..."

   **Paso 3: Crear JAVA_HOME**
   - En la sección "Variables del sistema" (abajo), clic en "Nueva..."
   - Nombre de la variable: `JAVA_HOME`
   - Valor de la variable: `C:\Program Files\Eclipse Adoptium\jdk-21.0.5+11`
   - Clic en "Aceptar"

   **Paso 4: Agregar al PATH**
   - En "Variables del sistema", buscar y seleccionar la variable `Path`
   - Clic en "Editar..."
   - Clic en "Nuevo"
   - Agregar: `%JAVA_HOME%\bin`
   - Clic en "Aceptar" en todas las ventanas

   **Paso 5: Reiniciar Git Bash completamente y verificar**
   ```bash
   echo $JAVA_HOME
   java -version
   ```

3. **Descargar Quarkus CLI:**
   - Ir a [Quarkus CLI Releases](https://github.com/quarkusio/quarkus/releases)
   - Buscar la versión más reciente del archivo `quarkus-cli-X.X.X-windows-x86_64.zip`
   - Descomprimir en `C:\quarkus-cli`
   - Agregar `C:\quarkus-cli\bin` al PATH de Windows

4. **Verificar en Git Bash:**
   ```bash
   java -version
   quarkus --version
   ```

**Opción 4: WSL2 + SDKMAN (Para desarrolladores avanzados)**

```bash
# 1. Instalar WSL2 (ejecutar en PowerShell como admin)
wsl --install

# 2. Reiniciar Windows

# 3. Abrir Ubuntu desde el menú de inicio

# 4. Seguir los pasos de instalación de macOS con SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21-tem
sdk install quarkus

# 5. Verificar
java -version
quarkus --version
```

---

## ✅ Script de Verificación Automatizado

El proyecto incluye scripts automatizados para verificar tu entorno:

- **macOS/Linux:** `verificar-mac.sh`
- **Windows (Git Bash):** `verificar-windows.sh`

**Uso:**

```bash
# Dar permisos de ejecución (solo primera vez)
chmod +x verificar-mac.sh verificar-windows.sh

# Ejecutar el script según tu sistema operativo
./verificar-mac.sh       # macOS/Linux
./verificar-windows.sh   # Windows con Git Bash
```

**Qué verifica:**
- ✅ Java instalado y versión correcta (>= 17)
- ✅ Quarkus CLI instalado
- ✅ JAVA_HOME configurado (opcional pero recomendado)

**Salida esperada:**
```
✅ Java: INSTALADO
openjdk version "21.0.x"

✅ Quarkus CLI: INSTALADO
3.15.x

✅ Java version compatible (>= 17)
```

---

## 🏗️ Creación del Proyecto

### Opción 1: Con Quarkus CLI (Recomendado)

```bash
# Crear proyecto con extensión REST
quarkus create app pe.banco:hola-mundo \
    --extension=rest \
    --no-wrapper

# Entrar al directorio
cd hola-mundo
```

### Opción 2: Con Quarkus CLI sin código (Educativo)

```bash
# Crear proyecto limpio
quarkus create app pe.banco:hola-mundo --no-code

# Entrar al directorio
cd hola-mundo

# Agregar extensión REST después
./mvnw quarkus:add-extension -Dextensions="rest"
```

### Opción 3: Desde Maven Archetype

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.15.1:create \
    -DprojectGroupId=pe.banco \
    -DprojectArtifactId=hola-mundo \
    -DprojectVersion=1.0.0-SNAPSHOT \
    -Dextensions=rest
    
cd hola-mundo
```

### Opción 4: Desde Web (Más visual)

1. Ir a [code.quarkus.io](https://code.quarkus.io)
2. Configurar:
   - **Group:** `pe.banco`
   - **Artifact:** `hola-mundo`
   - **Build Tool:** Maven
   - **Java Version:** 21
3. Agregar extensión: **RESTEasy Reactive**
4. Generar y descargar ZIP
5. Descomprimir y abrir el proyecto

---

## 📁 Estructura del Proyecto

```
hola-mundo/
├── mvnw                          # Maven Wrapper (macOS/Linux/Git Bash)
├── mvnw.cmd                      # Maven Wrapper (Windows CMD/PowerShell - no usar)
├── pom.xml                       # Configuración Maven
├── README.md                     # Este archivo
├── TEORIA.md                     # Documentación teórica
├── .dockerignore                 # Exclusiones para Docker
├── .gitignore                    # Exclusiones para Git
├── .mvn/                         # Configuración Maven Wrapper
├── src/
│   ├── main/
│   │   ├── docker/               # Dockerfiles
│   │   │   ├── Dockerfile.jvm            # Imagen Docker modo JVM
│   │   │   ├── Dockerfile.legacy-jar     # Imagen legacy
│   │   │   ├── Dockerfile.native         # Imagen nativa GraalVM
│   │   │   └── Dockerfile.native-micro   # Imagen nativa ultra-compacta
│   │   ├── java/
│   │   │   └── pe/banco/hola/
│   │   │       └── HelloResource.java    # Endpoint REST principal
│   │   └── resources/
│   │       └── application.properties    # Configuración de la app
│   └── test/
│       └── java/
│           └── pe/banco/hola/
│               └── HelloResourceTest.java
└── target/                       # Archivos compilados (generado)
```

---

## 🔧 Configuración Inicial

### 1. Posicionarse en el directorio del proyecto

```bash
cd hola-mundo
```

### 2. Dar permisos al Maven Wrapper

**macOS/Linux/Git Bash:**
```bash
chmod +x mvnw
```

> **Nota para Windows:** En Git Bash este comando funciona perfectamente

### 3. Verificar que la extensión REST está instalada

Revisar el archivo `pom.xml`, debe contener:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest</artifactId>
</dependency>
```

Si no está, agregarla:

```bash
./mvnw quarkus:add-extension -Dextensions="rest"
```

---

## ✍️ Endpoint HelloResource

Archivo: `src/main/java/pe/banco/hola/HelloResource.java`

```java
package pe.banco.hola;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class HelloResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hola mundo desde Quarkus 🚀";
    }
}
```

---

## ▶️ Ejecución del Proyecto

### Modo Desarrollo (Hot Reload Automático)

```bash
./mvnw quarkus:dev
```

**Salida esperada:**

```
__  ____  __  _____   ___  __ ____  ______ 
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   

INFO  [io.quarkus] (Quarkus Main Thread) hola-mundo 1.0.0-SNAPSHOT on JVM started in 1.234s
INFO  [io.quarkus] (Quarkus Main Thread) Listening on: http://localhost:8080

Tests paused
Press [e] to edit command line args, [r] to resume testing, [h] for more options>
```

**Accesos:**
- **Endpoint:** http://localhost:8080/hello
- **Dev UI:** http://localhost:8080/q/dev
- **Health Check:** http://localhost:8080/q/health
- **Metrics:** http://localhost:8080/q/metrics

### Compilar sin ejecutar

```bash
./mvnw clean compile
```

### Empaquetar aplicación (JAR)

```bash
./mvnw package
```

### Ejecutar JAR empaquetado

```bash
# Ambas formas funcionan en Git Bash y macOS
java -jar target/quarkus-app/quarkus-run.jar
```

---

## 🧪 Pruebas

### Probar el endpoint manualmente

**Opción 1: Navegador**
```
http://localhost:8080/hello
```

**Opción 2: curl (macOS y Git Bash)**
```bash
curl http://localhost:8080/hello
```

**Opción 3: Postman/Insomnia**
- Method: GET
- URL: http://localhost:8080/hello

### Ejecutar tests automatizados

```bash
./mvnw test
```

---

## 🐳 Docker (Opcional)

### Construir imagen Docker (JVM Mode)

```bash
docker build -f src/main/docker/Dockerfile.jvm -t hola-mundo:1.0.0-jvm .
```

### Ejecutar contenedor

```bash
docker run -i --rm -p 8080:8080 hola-mundo:1.0.0-jvm
```

### Construir imagen nativa (requiere GraalVM)

```bash
./mvnw package -Pnative -Dquarkus.native.container-build=true
docker build -f src/main/docker/Dockerfile.native -t hola-mundo:1.0.0-native .
```

---

## 🔍 Comandos Útiles en Modo Dev

Cuando la aplicación está corriendo con `quarkus:dev`, puedes usar estas teclas:

| Tecla | Acción |
|-------|--------|
| **`w`** | Abrir Dev UI en navegador |
| **`d`** | Abrir documentación |
| **`r`** | Ejecutar tests |
| **`s`** | Ver métricas |
| **`h`** | Ver todas las opciones |
| **`q`** | Salir de la aplicación |
| **`Ctrl+C`** | Forzar salida |

---

## ⚙️ Configuración (application.properties)

Archivo: `src/main/resources/application.properties`

```properties
# Puerto del servidor (default: 8080)
quarkus.http.port=8080

# Habilitar CORS en desarrollo
quarkus.http.cors=true

# Nivel de log
quarkus.log.level=INFO
quarkus.log.console.level=INFO

# Hot reload (activado por defecto en dev mode)
quarkus.live-reload.instrumentation=true
```

---

## 🚨 Solución de Problemas Comunes

### ❌ Error: "jakarta.ws.rs not found" o imports subrayados en rojo

**Causa:** Falta la extensión REST

**Solución:**
```bash
./mvnw quarkus:add-extension -Dextensions="rest"
```

### ❌ Error: "Permission denied: ./mvnw"

**Causa:** El wrapper no tiene permisos de ejecución

**Solución:**
```bash
chmod +x mvnw
```

### ❌ Error: "Port 8080 already in use"

**Causa:** Otro proceso está usando el puerto 8080

**Solución 1 - Cambiar puerto:**

En `application.properties`:
```properties
quarkus.http.port=8081
```

**Solución 2 - Liberar puerto (macOS/Linux/Git Bash):**
```bash
# Identificar proceso
lsof -ti:8080

# Matar proceso
lsof -ti:8080 | xargs kill -9
```

**Solución 3 - Liberar puerto (Windows - PowerShell como admin):**
```powershell
# Identificar proceso
Get-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess

# Detener proceso
Get-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess | Stop-Process -Force
```

### ❌ Error: "JAVA_HOME is not set"

**Causa:** Java está instalado pero la variable de entorno no está configurada

---

**Solución para macOS (COMPLETA Y CORRECTA):**

**Paso 1: Encontrar dónde está instalado Java**
```bash
which java
```

**Resultado esperado:**
```
/opt/homebrew/opt/openjdk@21/bin/java
# o
/usr/local/opt/openjdk@21/bin/java
```

**Paso 2: Copiar la ruta SIN el `/bin/java` al final**
- Del ejemplo anterior, tu JAVA_HOME es: `/opt/homebrew/opt/openjdk@21`

**Paso 3: Configurar JAVA_HOME (reemplazar la ruta con la tuya)**
```bash
# Si usas zsh (default en macOS moderno)
echo 'export JAVA_HOME="/opt/homebrew/opt/openjdk@21"' >> ~/.zshrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# Si usas bash
echo 'export JAVA_HOME="/opt/homebrew/opt/openjdk@21"' >> ~/.bash_profile
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.bash_profile
source ~/.bash_profile
```

**Paso 4: Verificar**
```bash
echo $JAVA_HOME
# Debe mostrar: /opt/homebrew/opt/openjdk@21

java -version
javac -version
```

---

**Solución para Windows (COMPLETA Y CORRECTA):**

**Opción 1: Configurar en Git Bash (MÁS SIMPLE - RECOMENDADO)**

**Paso 1: Encontrar dónde está instalado Java**
```bash
# En Git Bash, ejecutar:
which java
```

**Resultado esperado:**
```
/c/Program Files/Eclipse Adoptium/jdk-21.0.5+11/bin/java
```

**Paso 2: Copiar la ruta SIN el `/bin/java` al final**
- Del ejemplo anterior, tu JAVA_HOME es: `/c/Program Files/Eclipse Adoptium/jdk-21.0.5+11`

**Paso 3: Configurar JAVA_HOME (reemplazar la ruta con la tuya del Paso 1)**
```bash
echo 'export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.5+11"' >> ~/.bashrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.bashrc
```

**Paso 4: Recargar la configuración**
```bash
source ~/.bashrc
```

**Paso 5: Verificar**
```bash
echo $JAVA_HOME
# Debe mostrar: /c/Program Files/Eclipse Adoptium/jdk-21.0.5+11

java -version
javac -version
```

---

**Opción 2: Configurar en Variables de Entorno de Windows**

Si prefieres configurarlo a nivel de sistema Windows (no solo Git Bash):

**Paso 1: Encontrar la ruta exacta de Java**
```bash
# En Git Bash:
which java
# Resultado ejemplo: /c/Program Files/Eclipse Adoptium/jdk-21.0.5+11/bin/java

# Convertir a formato Windows: C:\Program Files\Eclipse Adoptium\jdk-21.0.5+11
```

**Paso 2: Abrir Variables de Entorno**
- Presionar `Windows + R`
- Escribir: `sysdm.cpl` y presionar Enter
- Ir a la pestaña **"Opciones avanzadas"**
- Clic en **"Variables de entorno..."**

**Paso 3: Crear la variable JAVA_HOME**
- En la sección **"Variables del sistema"** (abajo)
- Clic en **"Nueva..."**
- Nombre de la variable: `JAVA_HOME`
- Valor de la variable: `C:\Program Files\Eclipse Adoptium\jdk-21.0.5+11` (tu ruta del Paso 1)
- Clic en **"Aceptar"**

**Paso 4: Agregar Java al PATH**
- En "Variables del sistema", buscar y seleccionar la variable `Path`
- Clic en **"Editar..."**
- Clic en **"Nuevo"**
- Agregar: `%JAVA_HOME%\bin`
- Mover esta entrada hacia arriba (opcional)
- Clic en **"Aceptar"** en todas las ventanas

**Paso 5: Verificar**
- **Cerrar completamente Git Bash** (todas las ventanas)
- Abrir Git Bash nuevamente
- Ejecutar:
```bash
echo $JAVA_HOME
# Debe mostrar: /c/Program Files/Eclipse Adoptium/jdk-21.0.5+11

java -version
javac -version
```

**Notas importantes:**
- ⚠️ **Opción 1 (Git Bash) es MÁS RÁPIDA** - solo 3 comandos y listo
- ⚠️ **Opción 2 (Windows)** afecta a todo el sistema, no solo Git Bash
- ⚠️ Siempre usar `which java` para encontrar la ruta correcta
- ⚠️ Copiar la ruta SIN el `/bin/java` al final
- ⚠️ Cerrar y reabrir Git Bash después de cambiar configuración

### ❌ Error: "Failed to execute goal... dependencies could not be resolved"

**Causa:** Maven no puede descargar dependencias (problema de red o cache corrupto)

**Solución:**
```bash
# Limpiar cache de Maven y reintentar
./mvnw dependency:purge-local-repository
./mvnw clean install
```

### ❌ Error: "No compiler is provided in this environment"

**Causa:** Maven no encuentra el compilador de Java (JDK no instalado, solo JRE)

**Solución:**
```bash
# Verificar que tienes JDK (no solo JRE)
javac -version

# Si no funciona, reinstala Java JDK:
# macOS: brew reinstall openjdk@21
# Windows: reinstalar desde Adoptium con JDK completo
```

### ❌ Error: Maven muy lento descargando dependencias

**Causa:** Repositorio Maven central puede ser lento desde algunas ubicaciones

**Solución:** Agregar mirror en `~/.m2/settings.xml`:

```xml
<settings>
  <mirrors>
    <mirror>
      <id>central-mirror</id>
      <mirrorOf>central</mirrorOf>
      <url>https://repo1.maven.org/maven2</url>
    </mirror>
  </mirrors>
</settings>
```

### ❌ Error en Git Bash: "mvnw: command not found"

**Causa:** Estás en el directorio incorrecto

**Solución:**
```bash
# Verificar que estás en el directorio del proyecto
pwd
ls -la mvnw

# Si no ves mvnw, navega al directorio correcto
cd hola-mundo  # o donde esté tu proyecto
```

### ❌ Git Bash muestra caracteres extraños o colores incorrectos

**Causa:** Configuración de terminal en Windows

**Solución:**
```bash
# Agregar a ~/.bashrc
export LANG=en_US.UTF-8
export LC_ALL=en_US.UTF-8

# Recargar
source ~/.bashrc
```

---

## 📚 Recursos Adicionales

### Documentación Oficial
- [Guías de Quarkus](https://quarkus.io/guides/)
- [Quarkus Cheat Sheet](https://lordofthejars.github.io/quarkus-cheat-sheet/)
- [REST con Quarkus](https://quarkus.io/guides/rest)
- [Quarkus Dev Services](https://quarkus.io/guides/dev-services)

### Comunidad
- [Quarkus GitHub](https://github.com/quarkusio/quarkus)
- [Stack Overflow - Tag: quarkus](https://stackoverflow.com/questions/tagged/quarkus)
- [Quarkus Zulip Chat](https://quarkusio.zulipchat.com/)

### Extensiones Útiles para VS Code
- **Extension Pack for Java** (Microsoft)
- **Quarkus Tools** (Red Hat)
- **REST Client** (Huachao Mao)
- **Thunder Client** (RangaV Vadhineni)

---

## 📝 Notas Importantes para Estudiantes

### Para Usuarios de Windows
- **SIEMPRE usa Git Bash** como terminal en este curso
- Git Bash simula un entorno Unix/Linux en Windows
- Los comandos son idénticos a macOS/Linux
- Si ves `./mvnw`, úsalo tal cual en Git Bash
- **No uses CMD ni PowerShell** para seguir este curso (evitarás errores)

### Para Usuarios de macOS
- **Homebrew** es la forma más simple de instalar todo
- **SDKMAN** es útil si necesitas cambiar versiones de Java frecuentemente
- El terminal por defecto (zsh) funciona perfecto

### Mejores Prácticas
1. **Siempre** estar en el directorio raíz del proyecto (donde está `pom.xml`)
2. **Verificar** que Java y Maven estén instalados antes de empezar
3. **Usar modo dev** (`quarkus:dev`) durante desarrollo para hot reload
4. **Revisar logs** cuando algo falle, Quarkus da mensajes claros
5. **Explorar Dev UI** (`http://localhost:8080/q/dev`) tiene muchas herramientas útiles

### Comandos Resumidos

```bash
# Verificar entorno
java -version
quarkus --version

# Crear proyecto
quarkus create app pe.banco:hola-mundo --extension=rest

# Entrar al proyecto
cd hola-mundo

# Dar permisos (primera vez)
chmod +x mvnw

# Ejecutar en modo desarrollo
./mvnw quarkus:dev

# Probar
curl http://localhost:8080/hello

# Ejecutar tests
./mvnw test

# Empaquetar
./mvnw package

# Ejecutar JAR
java -jar target/quarkus-app/quarkus-run.jar
```

---

## 📄 Licencia

Este proyecto es material educativo de NETEC