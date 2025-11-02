# 🛠️ QUARKUS-CLI.md - Guía Completa del CLI y Herramientas

Guía definitiva para dominar el Quarkus CLI, buscar extensiones y crear proyectos como un profesional.

---

## 📖 Índice

1. [¿Qué es Quarkus CLI?](#1-qué-es-quarkus-cli)
2. [Instalación del CLI](#2-instalación-del-cli)
3. [Comandos Esenciales](#3-comandos-esenciales)
4. [Crear Proyectos](#4-crear-proyectos)
5. [Trabajar con Extensiones](#5-trabajar-con-extensiones)
6. [Buscar Extensiones](#6-buscar-extensiones)
7. [Alternativas al CLI](#7-alternativas-al-cli)
8. [Comparación de Herramientas](#8-comparación-de-herramientas)
9. [Mejores Prácticas](#9-mejores-prácticas)

---

## 1. ¿Qué es Quarkus CLI?

El **Quarkus CLI** es una herramienta de línea de comandos oficial que simplifica el desarrollo con Quarkus.

### Características principales:

✅ **Crear proyectos** rápidamente  
✅ **Gestionar extensiones** (agregar, quitar, listar)  
✅ **Ejecutar en dev mode** con un comando  
✅ **Build y deploy** simplificados  
✅ **Multiplataforma** (Mac, Windows, Linux)  

### Analogía:

El CLI es como un **asistente personal de desarrollo**:
- **Sin CLI**: Tienes que hacer todo manual (Maven largo, buscar dependencias)
- **Con CLI**: El asistente hace el trabajo pesado por ti

---

## 2. Instalación del CLI

### 🍎 macOS

**Opción 1: Homebrew (Recomendado)**

```bash
brew install quarkusio/tap/quarkus
```

**Opción 2: SDKMAN**

```bash
sdk install quarkus
```

**Verificar instalación:**

```bash
quarkus --version
```

---

### 🪟 Windows

**Opción 1: Chocolatey (Recomendado)**

```powershell
# PowerShell como Administrador (solo para instalar)
choco install quarkus
```

**Opción 2: Descarga Manual**

1. Ir a [Quarkus Releases](https://github.com/quarkusio/quarkus/releases)
2. Descargar el CLI para Windows
3. Agregar al PATH

**Verificar instalación (en Git Bash):**

```bash
quarkus --version
```

---

### 🐧 Linux

**Con SDKMAN:**

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install quarkus
```

**Verificar:**

```bash
quarkus --version
```

---

## 3. Comandos Esenciales

### Ayuda General

```bash
quarkus --help
quarkus -h
```

### Ver Versión

```bash
quarkus --version
quarkus -v
```

### Listar Todos los Comandos

```bash
quarkus
```

### Principales Comandos:

| Comando | Propósito |
|---------|-----------|
| `create` | Crear nuevo proyecto |
| `build` | Compilar proyecto |
| `dev` | Ejecutar en modo desarrollo |
| `ext` | Gestionar extensiones |
| `info` | Información del proyecto |
| `version` | Ver versión de Quarkus |

---

## 4. Crear Proyectos

### 4.1 Sintaxis Básica

```bash
quarkus create app [groupId:]artifactId [OPTIONS]
```

### 4.2 Crear Proyecto Simple

```bash
quarkus create app mi-proyecto
```

Esto crea:
- **groupId:** `org.acme` (por defecto)
- **artifactId:** `mi-proyecto`
- **Sin extensiones adicionales**

### 4.3 Crear con GroupId Personalizado

```bash
quarkus create app com.miempresa:mi-proyecto
                    ↑             ↑
                 groupId      artifactId
```

**Ejemplo real:**

```bash
quarkus create app pe.banco:validador-cuentas
```

### 4.4 Crear con Extensiones

```bash
quarkus create app pe.banco:validador \
  --extension=rest-jackson,smallrye-openapi,hibernate-orm-panache
```

**Forma corta:**

```bash
quarkus create app pe.banco:validador \
  -x rest-jackson,smallrye-openapi
```

### 4.5 Crear sin Código de Ejemplo

```bash
quarkus create app pe.banco:validador --no-code
```

**Útil cuando:**
- Quieres empezar completamente limpio
- Vas a usar Contract-First
- No necesitas el `GreetingResource` de ejemplo

### 4.6 Especificar Versión de Java

```bash
quarkus create app pe.banco:validador \
  --java=21
```

### 4.7 Elegir Build Tool (Maven o Gradle)

```bash
# Maven (por defecto)
quarkus create app pe.banco:validador

# Gradle
quarkus create app pe.banco:validador \
  --gradle
```

### 4.8 Ejemplo Completo

```bash
quarkus create app pe.banco.capitulo3:validador-completo \
  --java=21 \
  --extension=rest-jackson,smallrye-openapi,hibernate-orm-panache,jdbc-postgresql \
  --no-code
```

Esto crea un proyecto con:
- ✅ GroupId: `pe.banco.capitulo3`
- ✅ ArtifactId: `validador-completo`
- ✅ Java 21
- ✅ 4 extensiones incluidas
- ✅ Sin código de ejemplo

---

## 5. Trabajar con Extensiones

### 5.1 Ver Extensiones del Proyecto

Desde el directorio del proyecto:

```bash
quarkus ext list
```

O con Maven:

```bash
./mvnw quarkus:list-extensions
```

### 5.2 Agregar Extensiones

**Con CLI:**

```bash
quarkus ext add rest-jackson smallrye-openapi
```

**Con Maven:**

```bash
./mvnw quarkus:add-extension -Dextensions="rest-jackson,smallrye-openapi"
```

**Forma corta:**

```bash
quarkus ext add rest-jackson
```

### 5.3 Remover Extensiones

```bash
quarkus ext remove rest-jackson
```

**Con Maven:**

```bash
./mvnw quarkus:remove-extension -Dextensions="rest-jackson"
```

### 5.4 Buscar Extensiones Disponibles

```bash
quarkus ext list --installable
```

O buscar por palabra clave:

```bash
quarkus ext list --installable | grep security
quarkus ext list --installable | grep database
```

### 5.5 Ver Info de una Extensión

```bash
quarkus ext list --installable --search=rest-jackson
```

---

## 6. Buscar Extensiones

### 6.1 Desde el CLI (Terminal)

**Listar TODAS las extensiones disponibles:**

```bash
quarkus ext list --installable
```

**Buscar por categoría:**

```bash
# Seguridad
quarkus ext list --installable | grep -i security

# Bases de datos
quarkus ext list --installable | grep -i database

# Mensajería
quarkus ext list --installable | grep -i kafka

# Reactive
quarkus ext list --installable | grep -i reactive
```

**macOS/Linux:**
```bash
quarkus ext list --installable | grep openapi
```

**Windows (PowerShell):**
```powershell
quarkus ext list --installable | Select-String openapi
```

### 6.2 Desde el Sitio Web Oficial ⭐

**URL:** https://quarkus.io/extensions/

**Características:**
- 📚 **Catálogo completo** de extensiones
- 🔍 **Buscador** por nombre o palabra clave
- 📂 **Categorías**: Web, Data, Messaging, Security, etc.
- 📝 **Descripción detallada** de cada extensión
- 🔗 **Links a guías** relacionadas
- 📦 **Snippets** para Maven/Gradle

**Estructura de URLs:**

```
https://quarkus.io/extensions/[groupId]/[artifactId]/
```

**Ejemplos:**

```
https://quarkus.io/extensions/io.quarkus/quarkus-rest-jackson/
https://quarkus.io/extensions/io.quarkus/quarkus-smallrye-openapi/
https://quarkus.io/extensions/io.quarkus/quarkus-hibernate-orm-panache/
```

**Cada página de extensión muestra:**

1. **Descripción:** Para qué sirve
2. **Snippet Maven:**
   ```xml
   <dependency>
       <groupId>io.quarkus</groupId>
       <artifactId>quarkus-rest-jackson</artifactId>
   </dependency>
   ```
3. **Comando CLI:**
   ```bash
   quarkus ext add rest-jackson
   ```
4. **Guías relacionadas:** Links a documentación
5. **Dependencias:** Qué otras extensiones necesita

### 6.3 Desde code.quarkus.io (Web Wizard)

**URL:** https://code.quarkus.io

**Características:**
- 🎨 Interfaz visual elegante
- 🔍 Buscador en tiempo real
- ✅ Checkboxes para seleccionar
- 👁️ Preview del `pom.xml`
- 📦 Descarga proyecto completo

**Flujo:**
1. Configurar groupId, artifactId
2. Buscar extensiones en el buscador
3. Click para agregar/quitar
4. Generate → Descargar ZIP

### 6.4 Desde Maven Central (Alternativa)

**URL:** https://mvnrepository.com/

Buscar: `quarkus-rest-jackson`

**Menos útil** porque:
- No muestra descripciones Quarkus-específicas
- No incluye guías
- Más genérico

---

## 7. Alternativas al CLI

### 7.1 Comparación de Herramientas

| Herramienta | Tipo | Ventajas | Desventajas |
|-------------|------|----------|-------------|
| **Quarkus CLI** | Terminal | ⚡ Rápido, preciso, scriptable | Requiere instalación |
| **code.quarkus.io** | Web | 🎨 Visual, sin instalación | Requiere internet |
| **Maven Archetype** | Terminal | 📦 Estándar Maven | Más verbose |
| **VSCode Plugin** | IDE | 🖱️ Integrado | Básico, conflictos |
| **IntelliJ Plugin** | IDE | 🧠 Completo | Requiere IntelliJ Ultimate |

### 7.2 Quarkus CLI (Terminal) ⭐

**Pros:**
- ✅ Más rápido
- ✅ Control total
- ✅ Scriptable (CI/CD)
- ✅ Sin navegador

**Contras:**
- ❌ Requiere instalación
- ❌ Memorizar sintaxis

**Cuándo usar:**
- Desarrollo diario
- Scripts automatizados
- CI/CD pipelines

### 7.3 code.quarkus.io (Web) ⭐

**Pros:**
- ✅ Visual e intuitivo
- ✅ Sin instalación
- ✅ Buscar extensiones fácil
- ✅ Preview de configuración

**Contras:**
- ❌ Requiere internet
- ❌ Más lento que CLI

**Cuándo usar:**
- Primera vez con Quarkus
- Explorar extensiones
- No tienes CLI instalado
- Prefieres interfaz gráfica

### 7.4 Maven Archetype (Terminal)

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.28.3:create \
  -DprojectGroupId=pe.banco \
  -DprojectArtifactId=validador \
  -Dextensions=rest-jackson,smallrye-openapi
```

**Pros:**
- ✅ No requiere Quarkus CLI
- ✅ Maven puro

**Contras:**
- ❌ Comando muy largo
- ❌ Menos intuitivo

**Cuándo usar:**
- No puedes instalar Quarkus CLI
- Scripts que solo usan Maven

### 7.5 VSCode Plugin (IDE)

**Extensión:** Quarkus (Red Hat)

**Pros:**
- ✅ Integrado en VSCode
- ✅ Wizard gráfico

**Contras:**
- ❌ Básico (pocas opciones)
- ❌ Puede generar conflictos (RESTEasy Classic vs Reactive)
- ❌ No tan actualizado

**Cuándo usar:**
- Ya estás en VSCode
- Proyecto muy simple

**⚠️ No recomendado para proyectos serios**

### 7.6 IntelliJ IDEA Plugin (IDE)

**Plugin:** Quarkus Tools

**Pros:**
- ✅ Muy completo
- ✅ Bien integrado
- ✅ Autocompletado excelente

**Contras:**
- ❌ Requiere IntelliJ Ultimate (pago)

**Cuándo usar:**
- Usas IntelliJ Ultimate
- Prefieres IDE completo

---

## 8. Comparación de Herramientas

### Tabla Resumen

| Característica | CLI | code.quarkus.io | Maven | VSCode | IntelliJ |
|----------------|-----|-----------------|-------|--------|----------|
| **Velocidad** | ⚡⚡⚡ | ⚡ | ⚡⚡ | ⚡⚡ | ⚡⚡ |
| **Facilidad** | ⭐⭐ | ⭐⭐⭐ | ⭐ | ⭐⭐⭐ | ⭐⭐⭐ |
| **Control** | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ | ⭐ | ⭐⭐ |
| **Sin instalar** | ❌ | ✅ | ✅* | ❌ | ❌ |
| **Offline** | ✅ | ❌ | ✅ | ✅ | ✅ |
| **Scripting** | ✅ | ❌ | ✅ | ❌ | ❌ |

*Maven viene con el proyecto (wrapper)

### Mi Recomendación por Escenario

**🏆 Para uso diario:**
1. **Quarkus CLI** (terminal)
2. code.quarkus.io (exploración)

**📚 Para aprender:**
1. **code.quarkus.io** (visual)
2. Quarkus CLI (luego)

**🏢 Para equipos:**
1. **Quarkus CLI** (consistencia)
2. Documentar comandos en README

**🚀 Para CI/CD:**
1. **Quarkus CLI** o **Maven** (scriptable)

---

## 9. Mejores Prácticas

### 9.1 Naming Conventions

```bash
# ✅ BIEN: groupId con dominio inverso
quarkus create app cl.empresa.modulo:servicio

# ❌ MAL: sin dominio
quarkus create app miapp

# ✅ BIEN: artifactId descriptivo con guiones
quarkus create app pe.banco:validador-cuentas

# ❌ MAL: sin guiones, confuso
quarkus create app pe.banco:validadorcuentas
```

### 9.2 Organización de GroupId

```
cl.empresa.proyecto.modulo
│         │        │
│         │        └─ Módulo específico
│         └─ Nombre del proyecto
└─ Dominio de la empresa
```

**Ejemplos:**
```
pe.banco.digital.cuentas
pe.banco.digital.prestamos
pe.banco.digital.usuarios
```

### 9.3 Extensiones Desde el Inicio

**✅ Mejor agregar al crear:**
```bash
quarkus create app pe.banco:validador \
  --extension=rest-jackson,smallrye-openapi
```

**⚠️ Agregar después requiere más pasos:**
```bash
quarkus create app pe.banco:validador
cd validador
quarkus ext add rest-jackson smallrye-openapi
```

### 9.4 Usar --no-code para Contract-First

```bash
quarkus create app pe.banco:api-clientes \
  --extension=rest-jackson,smallrye-openapi,openapi-generator \
  --no-code
```

Así empiezas limpio para diseñar el contrato OpenAPI primero.

### 9.5 Documentar en README

En el README del proyecto, incluir:

```markdown
## Recrear este proyecto

```bash
quarkus create app pe.banco.digital:validador-cuentas \
  --java=21 \
  --extension=rest-jackson,smallrye-openapi,hibernate-orm-panache,jdbc-postgresql
```
```

Así otros desarrolladores pueden recrear el proyecto exactamente.

### 9.6 Verificar Extensiones Necesarias

Antes de empezar, lista las extensiones que necesitarás:

```bash
# Ver extensiones disponibles relacionadas con "database"
quarkus ext list --installable | grep database

# Agregar todas a la vez
quarkus ext add hibernate-orm-panache jdbc-postgresql
```

### 9.7 Mantener CLI Actualizado

```bash
# Homebrew (macOS)
brew upgrade quarkus

# Chocolatey (Windows)
choco upgrade quarkus

# SDKMAN
sdk upgrade quarkus
```

---

## 10. Ejemplos Prácticos Completos

### Ejemplo 1: API REST Simple

```bash
quarkus create app pe.banco:api-simple \
  --java=21 \
  --extension=rest-jackson,smallrye-openapi

cd api-simple
quarkus dev
```

### Ejemplo 2: Microservicio con Base de Datos

```bash
quarkus create app pe.banco:servicio-usuarios \
  --java=21 \
  --extension=rest-jackson,hibernate-orm-panache,jdbc-postgresql,smallrye-openapi \
  --no-code

cd servicio-usuarios
quarkus dev
```

### Ejemplo 3: Contract-First con OpenAPI

```bash
quarkus create app pe.banco:validador-cuentas \
  --java=21 \
  --extension=rest-jackson,smallrye-openapi,openapi-generator,rest-client-jackson \
  --no-code

cd validador-cuentas

# Crear estructura OpenAPI
mkdir -p src/main/openapi

# (Aquí creas tu openapi.yaml)

quarkus dev
```

### Ejemplo 4: Microservicio Reactivo

```bash
quarkus create app pe.banco:notificaciones-reactive \
  --java=21 \
  --extension=resteasy-reactive-jackson,smallrye-reactive-messaging-kafka

cd notificaciones-reactive
quarkus dev
```

---

## 11. Comandos de Desarrollo

### Ejecutar en Dev Mode

```bash
# Desde CLI
quarkus dev

# Desde Maven
./mvnw quarkus:dev
```

### Build del Proyecto

```bash
# Desde CLI
quarkus build

# Desde Maven
./mvnw package
```

### Build Nativo (GraalVM)

```bash
# Desde CLI
quarkus build --native

# Desde Maven
./mvnw package -Pnative
```

### Ver Info del Proyecto

```bash
quarkus info
```

Muestra:
- Versión de Quarkus
- Extensiones instaladas
- Configuración del proyecto

---

## 12. Troubleshooting

### Problema: "command not found: quarkus"

**Causa:** CLI no instalado o no está en PATH

**Solución:**
```bash
# Verificar instalación
which quarkus  # macOS/Linux
where quarkus  # Windows

# Reinstalar
brew reinstall quarkus  # macOS
choco install quarkus --force  # Windows
```

### Problema: Versión antigua del CLI

**Solución:**
```bash
# macOS
brew upgrade quarkus

# Windows
choco upgrade quarkus

# SDKMAN
sdk upgrade quarkus
```

### Problema: Extensión no encontrada

**Causa:** Nombre incorrecto

**Solución:**
```bash
# Buscar el nombre correcto
quarkus ext list --installable | grep nombre
```

### Problema: Crear proyecto falla

**Causa:** Permisos o conectividad

**Solución:**
```bash
# Verificar conexión a internet
# Verificar permisos en carpeta actual
# Intentar con Maven como alternativa
mvn io.quarkus.platform:quarkus-maven-plugin:3.28.3:create \
  -DprojectGroupId=pe.banco \
  -DprojectArtifactId=test
```

---

## 13. Recursos Adicionales

### Documentación Oficial

- [Quarkus CLI Reference](https://quarkus.io/guides/cli-tooling)
- [Quarkus Extensions](https://quarkus.io/extensions/)
- [Quarkus Guides](https://quarkus.io/guides/)

### Herramientas Web

- [code.quarkus.io](https://code.quarkus.io) - Generador visual
- [Quarkus Extensions](https://quarkus.io/extensions/) - Catálogo completo

### Comunidad

- [Quarkus GitHub](https://github.com/quarkusio/quarkus)
- [Quarkus Google Group](https://groups.google.com/g/quarkus-dev)
- [Stack Overflow - Tag: quarkus](https://stackoverflow.com/questions/tagged/quarkus)

---

## ✅ Checklist de Dominio del CLI

Después de leer esta guía, deberías poder:

- [ ] Instalar Quarkus CLI en tu sistema
- [ ] Crear proyecto con groupId y artifactId personalizados
- [ ] Agregar extensiones al crear el proyecto
- [ ] Buscar extensiones disponibles desde terminal
- [ ] Agregar/remover extensiones en proyecto existente
- [ ] Usar code.quarkus.io para explorar extensiones
- [ ] Encontrar documentación de extensiones en quarkus.io
- [ ] Elegir la herramienta correcta según el contexto
- [ ] Ejecutar proyecto en dev mode
- [ ] Documentar comandos en README del proyecto

---

**🎉 ¡Ahora eres un maestro del Quarkus CLI!**

*Con esta guía puedes crear proyectos profesionales de forma rápida y eficiente.* 🚀🧉