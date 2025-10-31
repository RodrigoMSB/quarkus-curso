# 📚 TEORIA.md - Capítulo 2: Configuración y Contract-First

Fundamentos teóricos para entender el desarrollo Contract-First con Quarkus.

---

## 📖 Índice

1. [Entorno de Desarrollo Quarkus](#1-entorno-de-desarrollo-quarkus)
2. [Estructura de un Proyecto Maven](#2-estructura-de-un-proyecto-maven)
3. [Extensiones de Quarkus](#3-extensiones-de-quarkus)
4. [Dev Mode: El Superpoder de Quarkus](#4-dev-mode-el-superpoder-de-quarkus)
5. [Contract-First vs Code-First](#5-contract-first-vs-code-first)
6. [OpenAPI: El Estándar de Contratos](#6-openapi-el-estándar-de-contratos)
7. [Generación de Código desde Contratos](#7-generación-de-código-desde-contratos)
8. [JAX-RS: REST en Java](#8-jax-rs-rest-en-java)

---

## 1. Entorno de Desarrollo Quarkus

### 1.1 ¿Qué necesita un proyecto Quarkus?

Un proyecto Quarkus requiere tres componentes esenciales:

```
┌─────────────────────────────────────┐
│           JAVA 17+                  │
│  (Motor de ejecución)               │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│           MAVEN 3.9+                │
│  (Gestor de dependencias y build)   │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│        QUARKUS CLI                  │
│  (Herramienta de creación)          │
└─────────────────────────────────────┘
```

### 1.2 Java: La Base

**¿Por qué Java 17 o superior?**

- **Java 17 LTS**: Soporte a largo plazo (hasta 2029)
- **Java 21 LTS**: Última versión LTS (recomendada para nuevos proyectos)
- **Records, Pattern Matching, Text Blocks**: Funcionalidades modernas

**Analogía:**
Java es como el **motor de un auto**. Puedes usar un motor más viejo (Java 11), pero con uno moderno (Java 21) tienes mejor rendimiento, menor consumo (memoria) y nuevas funcionalidades.

### 1.3 Maven: El Constructor

**Maven gestiona:**

1. **Dependencias**: Librerías que necesita el proyecto
2. **Build lifecycle**: Compile → Test → Package → Deploy
3. **Plugins**: Herramientas adicionales (Quarkus, OpenAPI Generator)

**Maven Wrapper (mvnw):**

```bash
./mvnw  # Usa Maven específico del proyecto (versión garantizada)
mvn     # Usa Maven instalado globalmente (versión puede variar)
```

**Ventajas del Wrapper:**
- ✅ No requiere Maven instalado
- ✅ Versión consistente entre desarrolladores
- ✅ CI/CD sin configuración adicional

### 1.4 Quarkus CLI: El Asistente

El CLI de Quarkus simplifica:

```bash
# Crear proyecto
quarkus create app groupId:artifactId

# Agregar extensiones
quarkus ext add rest-jackson

# Ejecutar en dev
quarkus dev

# Ver extensiones disponibles
quarkus ext list
```

**Sin CLI:**
Todo se puede hacer con Maven, pero el CLI es más intuitivo y rápido.

---

## 2. Estructura de un Proyecto Maven

### 2.1 Anatomía del Proyecto

```
validador-banco/
│
├── pom.xml                    # ⭐ Corazón del proyecto
├── mvnw                       # Maven Wrapper (macOS/Linux/Git Bash)
├── mvnw.cmd                   # Maven Wrapper (Windows CMD - no usar)
│
├── src/
│   ├── main/
│   │   ├── java/             # 📝 Código fuente
│   │   ├── resources/        # ⚙️ Configuración
│   │   └── openapi/          # 📋 Contratos OpenAPI
│   │
│   └── test/
│       ├── java/             # 🧪 Tests
│       └── resources/        # Datos de prueba
│
└── target/                   # 🏗️ Archivos generados
    ├── classes/              # Compilados
    ├── generated-sources/    # Código generado
    └── validador-banco-1.0.0-SNAPSHOT.jar
```

### 2.2 El archivo pom.xml

**POM = Project Object Model**

```xml
<project>
    <!-- IDENTIDAD DEL PROYECTO -->
    <groupId>cl.alchemicaldata</groupId>      <!-- Organización -->
    <artifactId>validador-banco</artifactId>   <!-- Nombre proyecto -->
    <version>1.0.0-SNAPSHOT</version>          <!-- Versión -->
    
    <!-- PROPIEDADES -->
    <properties>
        <maven.compiler.release>21</maven.compiler.release>
        <quarkus.version>3.28.3</quarkus.version>
    </properties>
    
    <!-- DEPENDENCIAS -->
    <dependencies>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-rest-jackson</artifactId>
        </dependency>
    </dependencies>
    
    <!-- PLUGINS (TAREAS DE BUILD) -->
    <build>
        <plugins>
            <plugin>
                <groupId>io.quarkus.platform</groupId>
                <artifactId>quarkus-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 2.3 Convenciones Maven

| Convención | Significado | Ejemplo |
|------------|-------------|---------|
| **groupId** | Organización/dominio inverso | `cl.alchemicaldata` |
| **artifactId** | Nombre del proyecto | `validador-banco` |
| **version** | Versión del artefacto | `1.0.0-SNAPSHOT` |
| **SNAPSHOT** | Versión en desarrollo | Se actualiza constantemente |
| **RELEASE** | Versión estable | `1.0.0`, `2.1.5` |

**Analogía:**
El `pom.xml` es como la **receta de un pastel**:
- **groupId**: El chef (tu organización)
- **artifactId**: Nombre del pastel (proyecto)
- **dependencies**: Ingredientes necesarios
- **plugins**: Utensilios de cocina (herramientas)

---

## 3. Extensiones de Quarkus

### 3.1 ¿Qué es una Extensión?

Una **extensión** es un módulo que agrega funcionalidad específica a Quarkus.

**Características:**
- 📦 Auto-configuración
- 🔥 Optimización build-time
- 🚀 Arranque ultra rápido
- 📝 Documentación integrada

### 3.2 Extensiones del Proyecto

#### **rest-jackson**
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-jackson</artifactId>
</dependency>
```

**Propósito:**
- REST endpoints (JAX-RS)
- Serialización/deserialización JSON automática
- Content negotiation

#### **smallrye-openapi**
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-openapi</artifactId>
</dependency>
```

**Propósito:**
- Generación automática de especificación OpenAPI
- Swagger UI integrado
- Documentación interactiva

#### **quarkus-openapi-generator**
```xml
<dependency>
    <groupId>io.quarkiverse.openapigenerator</groupId>
    <artifactId>quarkus-openapi-generator</artifactId>
</dependency>
```

**Propósito:**
- Generar código Java desde OpenAPI
- Interfaces JAX-RS automáticas
- DTOs (Data Transfer Objects)
- Clientes REST

#### **rest-client-jackson**
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-client-jackson</artifactId>
</dependency>
```

**Propósito:**
- Cliente REST type-safe
- Consumir APIs externas
- Integración con generador OpenAPI

### 3.3 Agregar Extensiones

```bash
# Método 1: Quarkus CLI
quarkus ext add rest-jackson

# Método 2: Maven
./mvnw quarkus:add-extension -Dextensions="rest-jackson"

# Método 3: Manual en pom.xml
# Agregar <dependency> en sección <dependencies>
```

### 3.4 Analogía

Las extensiones son como **apps en un smartphone**:
- **Sin extensiones**: Quarkus básico (como teléfono sin apps)
- **rest-jackson**: App de mensajería (comunicación REST)
- **smallrye-openapi**: App de documentos (documentación API)
- **openapi-generator**: Asistente de código (genera por ti)

---

## 4. Dev Mode: El Superpoder de Quarkus

### 4.1 ¿Qué es Dev Mode?

El **modo desarrollo** de Quarkus permite desarrollar con **feedback instantáneo**.

**Activar:**
```bash
./mvnw quarkus:dev
```

### 4.2 Características Principales

#### **🔥 Hot Reload (Recarga en Caliente)**

```
┌─────────────────────────────────────┐
│  1. Modificas código Java           │
│  2. Guardas archivo (Ctrl+S)        │
│  3. Quarkus detecta cambio          │
│  4. Recompila solo lo necesario     │
│  5. Recarga clases en memoria       │
│  6. Refrescas navegador             │
│  7. ¡Ves cambios instantáneos!      │
└─────────────────────────────────────┘
   Tiempo total: ~1-2 segundos
```

**Sin Hot Reload (tradicional):**
```
1. Modificas código
2. Detienes servidor
3. mvn clean package (30-60s)
4. Inicias servidor (10-30s)
5. Pruebas cambios
```

⏱️ **Ahorro de tiempo:** 90% más rápido

#### **🧪 Continuous Testing**

Tests se ejecutan automáticamente en background:

```bash
# En dev mode, presiona 'r'
Press [r] to resume testing
```

**Modos:**
- **Paused**: Tests detenidos (presiona `r` para activar)
- **Running**: Tests continuos al detectar cambios
- **Selective**: Solo tests relacionados con cambios

#### **🎛️ Dev UI**

Acceso: `http://localhost:8080/q/dev`

**Funcionalidades:**
- 📊 Dashboard del proyecto
- 🔧 Editar `application.properties` en vivo
- 🗄️ Explorador de base de datos (si hay)
- 📝 Generador de endpoints
- 🧪 Ejecutor de tests
- 📈 Métricas en tiempo real

#### **📚 Swagger UI Integrado**

Acceso: `http://localhost:8080/q/swagger-ui`

**Características:**
- Documentación interactiva
- Probar endpoints desde el navegador
- Ver esquemas de datos
- Exportar especificación OpenAPI

### 4.3 Comandos Interactivos

Mientras está en dev mode:

| Tecla | Acción |
|-------|--------|
| **`w`** | Abrir Dev UI en navegador |
| **`d`** | Abrir documentación |
| **`r`** | Ejecutar/reanudar tests |
| **`o`** | Toggle test output |
| **`s`** | Ver métricas |
| **`i`** | Toggle instrumentation |
| **`h`** | Ver ayuda completa |
| **`q`** | Salir (Ctrl+C también) |

### 4.4 Comparación con otros Frameworks

| Framework | Tiempo Reinicio | Hot Reload | Dev UI |
|-----------|----------------|------------|--------|
| **Quarkus** | ~1s | ✅ Total | ✅ Completo |
| **Spring Boot** | ~10-30s | ⚠️ Limitado | ❌ No |
| **Micronaut** | ~5-15s | ⚠️ Parcial | ❌ No |

### 4.5 Analogía

Dev Mode es como **cocinar con degustación continua**:

**Sin Dev Mode (tradicional):**
1. Cocinas el plato completo
2. Esperas que enfríe
3. Pruebas
4. Si no gusta, empiezas de cero

**Con Dev Mode (Quarkus):**
1. Cocinas un poco
2. Pruebas inmediatamente
3. Ajustas sabor
4. Vuelves a probar
5. Iteras hasta perfección

---

## 5. Contract-First vs Code-First

### 5.1 Definiciones

#### **Code-First (Código Primero)**

```
1. Escribes código Java
2. Agregas anotaciones JAX-RS
3. Framework genera OpenAPI automáticamente
4. Documentación sale del código
```

**Ejemplo:**
```java
@Path("/users")
public class UserResource {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<User> list() {
        return userService.findAll();
    }
}
```
↓ Genera automáticamente ↓
```yaml
paths:
  /users:
    get:
      responses:
        '200':
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/User'
```

#### **Contract-First (Contrato Primero)**

```
1. Diseñas especificación OpenAPI
2. Defines rutas, modelos, validaciones
3. Generas código desde el contrato
4. Implementas interfaces generadas
```

**Ejemplo:**
```yaml
# Primero: openapi.yaml
paths:
  /users:
    get:
      operationId: listUsers
      responses:
        '200':
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/User'
```
↓ Genera automáticamente ↓
```java
public interface UserApi {
    List<User> listUsers();
}
```
↓ Tu implementas ↓
```java
public class UserResource implements UserApi {
    public List<User> listUsers() {
        return userService.findAll();
    }
}
```

### 5.2 Comparación

| Aspecto | Code-First | Contract-First |
|---------|------------|----------------|
| **Inicio** | Código Java | Especificación OpenAPI |
| **Documentación** | Generada desde código | Diseñada primero |
| **Validación** | Manual | Automática (contrato) |
| **Cambios API** | Código → Doc | Contrato → Código |
| **Consistencia** | Puede divergir | Garantizada |
| **Frontend** | Espera backend | Paralelo con contrato |
| **Testing** | Después del código | Desde el contrato |
| **Complejidad** | Baja (más rápido) | Media (más robusto) |

### 5.3 ¿Cuándo usar cada uno?

#### **✅ Usa Code-First cuando:**
- Proyecto pequeño o prototipo
- Equipo único (backend + frontend)
- API interna, no pública
- Iteración rápida prioritaria
- Documentación no es crítica

#### **✅ Usa Contract-First cuando:**
- API pública o crítica
- Equipos distribuidos (backend ≠ frontend)
- Contratos estrictos requeridos
- Generación de clientes necesaria
- Versionado de API importante
- Arquitectura microservicios

### 5.4 Flujo Contract-First en el Proyecto

```
┌────────────────────────────────────────────┐
│  PASO 1: DISEÑAR CONTRATO                  │
│  src/main/openapi/openapi.yaml             │
│  - Definir endpoints                       │
│  - Definir modelos (schemas)               │
│  - Definir validaciones                    │
└────────────────────────────────────────────┘
                    ↓
┌────────────────────────────────────────────┐
│  PASO 2: GENERAR CÓDIGO                    │
│  mvn compile                               │
│  - Interfaces JAX-RS                       │
│  - DTOs (Data Transfer Objects)            │
│  - Validaciones Bean Validation            │
└────────────────────────────────────────────┘
                    ↓
┌────────────────────────────────────────────┐
│  PASO 3: IMPLEMENTAR                       │
│  ValidadorResource implements DefaultApi   │
│  - Lógica de negocio                       │
│  - Uso de servicios                        │
└────────────────────────────────────────────┘
                    ↓
┌────────────────────────────────────────────┐
│  PASO 4: VALIDAR CUMPLIMIENTO              │
│  - Compilación verifica interfaz           │
│  - Swagger UI muestra contrato             │
│  - Tests validan respuestas                │
└────────────────────────────────────────────┘
```

### 5.5 Analogía

**Code-First** es como **improvisar una receta**:
- Cocinas según tu intuición
- Al final escribes la receta basándote en lo que hiciste
- Rápido pero puede tener inconsistencias

**Contract-First** es como **seguir una receta profesional**:
- Primero defines ingredientes y pasos exactos
- Luego cocinas siguiendo la receta al pie de la letra
- El resultado es consistente y replicable

---

## 6. OpenAPI: El Estándar de Contratos

### 6.1 ¿Qué es OpenAPI?

**OpenAPI Specification (OAS)** es un estándar para definir APIs REST de forma agnóstica al lenguaje.

**Historia:**
- **2011**: Swagger nace (empresa Reverb)
- **2015**: Swagger Spec → OpenAPI Initiative
- **2017**: OpenAPI 3.0 (estándar actual)
- **2021**: OpenAPI 3.1 (alineado con JSON Schema)

### 6.2 Estructura de un Documento OpenAPI

```yaml
openapi: 3.0.3                    # Versión de la especificación

info:                             # Metadata
  title: Mi API
  version: 1.0.0
  description: Descripción de la API

servers:                          # Servidores (opcional)
  - url: http://localhost:8080
    description: Desarrollo

paths:                            # ⭐ Endpoints
  /recurso:
    get:
      summary: Descripción
      operationId: nombreOperacion
      parameters: [...]
      responses: [...]

components:                       # ⭐ Definiciones reutilizables
  schemas:                        # Modelos de datos
    MiModelo:
      type: object
      properties: [...]
  
  securitySchemes:                # Seguridad
    bearer:
      type: http
      scheme: bearer
```

### 6.3 Elementos Clave

#### **Paths (Rutas)**

```yaml
paths:
  /validar/{numeroCuenta}:        # Path con parámetro
    get:                          # HTTP Method
      summary: Validar cuenta     # Descripción corta
      operationId: validarGet     # Nombre del método (generación código)
      parameters:
        - name: numeroCuenta      # Parámetro de ruta
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':                    # Status code
          description: Éxito
          content:
            application/json:     # Media type
              schema:
                $ref: '#/components/schemas/ValidacionResponse'
```

#### **Components / Schemas (Modelos)**

```yaml
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
      required:                   # Campos obligatorios
        - valido
        - numeroCuenta
```

**Traduce a Java:**
```java
public class ValidacionResponse {
    private boolean valido;       // required
    private String numeroCuenta;  // required
    private String mensaje;       // optional
    
    // Getters y Setters
}
```

#### **Data Types**

| OpenAPI | Java | Ejemplo |
|---------|------|---------|
| `string` | `String` | `"texto"` |
| `integer` | `Integer` / `int` | `42` |
| `number` | `BigDecimal` / `double` | `3.14` |
| `boolean` | `Boolean` / `boolean` | `true` |
| `array` | `List<T>` | `[1, 2, 3]` |
| `object` | `Class` | `{...}` |

#### **Validaciones**

```yaml
properties:
  email:
    type: string
    format: email              # Formato email
    
  edad:
    type: integer
    minimum: 0                 # Valor mínimo
    maximum: 120               # Valor máximo
    
  nombre:
    type: string
    minLength: 3               # Longitud mínima
    maxLength: 50              # Longitud máxima
    
  estado:
    type: string
    enum: [ACTIVO, INACTIVO]  # Valores permitidos
```

**Se traduce a Bean Validation:**
```java
@Email
private String email;

@Min(0) @Max(120)
private Integer edad;

@Size(min = 3, max = 50)
private String nombre;

private EstadoEnum estado;  // Enum generado
```

### 6.4 Herramientas OpenAPI

#### **Editores**
- **Swagger Editor**: https://editor.swagger.io
- **Stoplight Studio**: Editor visual
- **VSCode OpenAPI Extension**: Autocompletado y validación

#### **Generadores**
- **OpenAPI Generator**: Código para 40+ lenguajes
- **Quarkus OpenAPI Generator**: Específico Quarkus
- **Swagger Codegen**: Predecesor de OpenAPI Generator

#### **Documentación**
- **Swagger UI**: Interfaz interactiva (integrado en Quarkus)
- **ReDoc**: Documentación estática elegante
- **Postman**: Importar y probar

### 6.5 Beneficios de OpenAPI

✅ **Estandarización**: Todos hablan el mismo idioma  
✅ **Documentación viva**: Siempre actualizada  
✅ **Generación automática**: Código, tests, clientes  
✅ **Validación**: Contratos verificables  
✅ **Interoperabilidad**: Agnóstico al lenguaje  
✅ **Tooling**: Ecosistema enorme de herramientas  

### 6.6 Analogía

OpenAPI es como el **plano arquitectónico de una casa**:

- **Contrato OpenAPI**: Plano detallado (dimensiones, materiales, ubicaciones)
- **Código generado**: Estructura prefabricada según plano
- **Implementación**: Decoración y funcionalidad interna
- **Swagger UI**: Recorrido virtual 3D de la casa
- **Validación**: Inspector verifica cumplimiento del plano

---

## 7. Generación de Código desde Contratos

### 7.1 ¿Cómo funciona?

```
openapi.yaml
     ↓
┌─────────────────────────┐
│  OpenAPI Generator      │
│  (Plugin Maven)         │
└─────────────────────────┘
     ↓
┌─────────────────────────────────────┐
│  Código Java Generado               │
│  - Interfaces JAX-RS                │
│  - DTOs (POJOs)                     │
│  - Validaciones                     │
│  - Clientes REST (opcional)         │
└─────────────────────────────────────┘
```

### 7.2 Configuración en Quarkus

**application.properties:**
```properties
# Configurar generador para openapi.yaml
quarkus.openapi-generator.codegen.spec.openapi_yaml.base-package=cl.alchemicaldata
```

**Nomenclatura:**
- `openapi_yaml` = nombre del archivo (`openapi.yaml`)
- Guiones bajos en lugar de puntos/guiones
- `base-package` = paquete raíz para código generado

### 7.3 ¿Qué se genera?

#### **Interfaces JAX-RS**

**Desde:**
```yaml
paths:
  /validar/{numeroCuenta}:
    get:
      operationId: validarNumeroCuentaGet
```

**Genera:**
```java
@Path("/validar/{numeroCuenta}")
public interface DefaultApi {
    
    @GET
    @Produces("application/json")
    ValidacionResponse validarNumeroCuentaGet(
        @PathParam("numeroCuenta") String numeroCuenta
    );
}
```

**Tu implementas:**
```java
public class ValidadorResource implements DefaultApi {
    
    @Override
    public ValidacionResponse validarNumeroCuentaGet(String numeroCuenta) {
        // Tu lógica aquí
        return new ValidacionResponse(...);
    }
}
```

#### **DTOs (Data Transfer Objects)**

**Desde:**
```yaml
components:
  schemas:
    ValidacionResponse:
      type: object
      properties:
        valido:
          type: boolean
        mensaje:
          type: string
```

**Genera:**
```java
public class ValidacionResponse {
    private Boolean valido;
    private String mensaje;
    
    // Constructor vacío
    public ValidacionResponse() {}
    
    // Getters y Setters
    public Boolean getValido() { return valido; }
    public void setValido(Boolean valido) { this.valido = valido; }
    
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}
```

### 7.4 Ubicación del Código Generado

```
target/
└── generated-sources/
    └── open-api/
        └── cl/alchemicaldata/
            ├── api/
            │   └── DefaultApi.java        # Interfaz JAX-RS
            └── model/
                └── ValidacionResponse.java # DTO
```

**⚠️ Importante:**
- **NO editar** código generado (se sobrescribe en cada build)
- **Sí implementar** las interfaces generadas
- **Sí usar** los DTOs generados

### 7.5 Ventajas de la Generación

✅ **Consistencia garantizada**: Código siempre cumple contrato  
✅ **Ahorro de tiempo**: No escribir código repetitivo  
✅ **Menos errores**: Compilador verifica cumplimiento  
✅ **Actualización fácil**: Cambiar contrato → regenerar  
✅ **Type-safety**: Tipos fuertes, no strings mágicos  
✅ **Documentación sincronizada**: Código = Contrato  

### 7.6 Flujo de Trabajo

```
1. Modificar openapi.yaml
   └─→ Cambiar path, agregar campo, etc.

2. Recompilar
   └─→ mvn compile

3. Código se regenera automáticamente
   └─→ Nuevas interfaces/DTOs

4. Compilador detecta cambios
   └─→ Errores si implementación no coincide

5. Actualizar implementación
   └─→ Implementar nuevos métodos/campos

6. Tests verifican
   └─→ Asegurar cumplimiento
```

### 7.7 Analogía

La generación de código es como **usar moldes en una panadería**:

- **Contrato OpenAPI**: El molde (define forma exacta)
- **Generador**: Máquina que usa el molde
- **Código generado**: Pan con forma perfecta
- **Tu implementación**: El relleno del pan
- **Compilador**: Inspector de calidad (verifica forma)

Si cambias el molde (contrato), todos los panes futuros (código) tendrán la nueva forma automáticamente.

---

## 8. JAX-RS: REST en Java

### 8.1 ¿Qué es JAX-RS?

**Jakarta RESTful Web Services** (antes Java EE, ahora Jakarta EE)

Especificación estándar de Java para crear APIs REST.

**Características:**
- Basado en anotaciones
- Mapeo automático HTTP ↔ Java
- Independiente de implementación
- Integrado en Quarkus

### 8.2 Anotaciones Principales

#### **@Path**

```java
@Path("/usuarios")              // Clase: ruta base
public class UsuarioResource {
    
    @Path("/{id}")              // Método: sub-ruta
    public Usuario obtener(...) {
        // GET /usuarios/{id}
    }
}
```

#### **HTTP Methods**

```java
@GET                            // Obtener recursos
@POST                           // Crear recursos
@PUT                            // Actualizar completo
@PATCH                          // Actualizar parcial
@DELETE                         // Eliminar recursos
```

#### **@Produces / @Consumes**

```java
@Produces(MediaType.APPLICATION_JSON)  // Qué devuelve
@Consumes(MediaType.APPLICATION_JSON)  // Qué recibe

// Content-Type y Accept headers
```

#### **Parámetros**

```java
@PathParam("id")                // /usuarios/{id}
@QueryParam("filtro")           // /usuarios?filtro=activos
@HeaderParam("Authorization")   // Headers HTTP
@FormParam("username")          // Form data
```

### 8.3 Ejemplo Completo

```java
@Path("/cuentas")
public class CuentaResource {
    
    // GET /cuentas
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Cuenta> listar() {
        return cuentaService.findAll();
    }
    
    // GET /cuentas/{id}
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Cuenta obtener(@PathParam("id") Long id) {
        return cuentaService.findById(id);
    }
    
    // POST /cuentas
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response crear(Cuenta cuenta) {
        Cuenta nueva = cuentaService.create(cuenta);
        return Response.status(201).entity(nueva).build();
    }
    
    // PUT /cuentas/{id}
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Cuenta actualizar(@PathParam("id") Long id, Cuenta cuenta) {
        return cuentaService.update(id, cuenta);
    }
    
    // DELETE /cuentas/{id}
    @DELETE
    @Path("/{id}")
    public Response eliminar(@PathParam("id") Long id) {
        cuentaService.delete(id);
        return Response.status(204).build();
    }
    
    // GET /cuentas?estado=ACTIVO
    @GET
    @Path("/buscar")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Cuenta> buscar(@QueryParam("estado") String estado) {
        return cuentaService.findByEstado(estado);
    }
}
```

### 8.4 Response (Respuestas HTTP)

#### **Status Codes Comunes**

| Código | Significado | Uso |
|--------|-------------|-----|
| **200** | OK | GET exitoso |
| **201** | Created | POST exitoso |
| **204** | No Content | DELETE exitoso |
| **400** | Bad Request | Datos inválidos |
| **404** | Not Found | Recurso no existe |
| **500** | Internal Server Error | Error del servidor |

#### **Construcción de Respuestas**

```java
// Retorno simple (200 por defecto)
@GET
public String simple() {
    return "Hola";
}

// Respuesta customizada
@POST
public Response customizada(Dato dato) {
    return Response
        .status(201)                    // Status code
        .entity(dato)                   // Body
        .header("Location", "/datos/1") // Headers
        .build();
}

// Respuesta con error
@GET
public Response conError() {
    return Response
        .status(404)
        .entity("No encontrado")
        .build();
}
```

### 8.5 Serialización JSON Automática

**Jackson (rest-jackson) hace la magia:**

```java
// Java Object → JSON (automático)
@GET
public Cuenta obtener() {
    Cuenta c = new Cuenta();
    c.setNumero("1234567890");
    c.setSaldo(1000.0);
    return c;  // Jackson convierte a JSON
}

// Resultado HTTP:
// Content-Type: application/json
// {"numero":"1234567890","saldo":1000.0}


// JSON → Java Object (automático)
@POST
public Response crear(Cuenta cuenta) {
    // cuenta ya es un objeto Java
    // Jackson convirtió el JSON del body
    System.out.println(cuenta.getNumero());
    return Response.ok().build();
}
```

### 8.6 Analogía

JAX-RS es como un **restaurante automatizado**:

- **@Path**: Dirección del restaurante y mesas
- **@GET/@POST**: Tipo de servicio (llevar, comer ahí, delivery)
- **@Produces**: Tipo de comida que sirve (japonesa, italiana)
- **@Consumes**: Tipo de pedido que acepta (verbal, app, papel)
- **@PathParam**: Número de mesa específico
- **@QueryParam**: Pedidos especiales (sin sal, extra picante)
- **Jackson**: Chef que traduce pedidos (JSON) a comida (Java) y viceversa

---

## 📊 Resumen Comparativo

### Contract-First vs Code-First

```
CONTRACT-FIRST                  CODE-FIRST
    ↓                              ↓
openapi.yaml                   @Path("/api")
    ↓                          public class Resource
mvn compile                        ↓
    ↓                          mvn compile
DefaultApi.java                    ↓
    ↓                          openapi.json
implements DefaultApi          (generado automático)
    ↓                              ↓
ValidadorResource.java         Mismo resultado final
```

**¿Cuál usamos en este capítulo?**
✅ **Contract-First** - Más robusto para producción

---

## 🎯 Conceptos Clave del Capítulo 2

### ✅ **Entorno de Desarrollo**
- Java 17/21 + Maven + Quarkus CLI
- Maven Wrapper para consistencia
- Instalación multiplataforma

### ✅ **Estructura Maven**
- pom.xml: corazón del proyecto
- src/main/java: código fuente
- target/: archivos generados
- Convenciones groupId:artifactId:version

### ✅ **Extensiones Quarkus**
- rest-jackson: REST + JSON
- smallrye-openapi: Documentación
- openapi-generator: Generación código
- rest-client-jackson: Cliente REST

### ✅ **Dev Mode**
- Hot reload en ~1 segundo
- Continuous testing
- Dev UI interactiva
- Swagger UI integrado

### ✅ **Contract-First**
- Diseñar contrato OpenAPI primero
- Generar código automáticamente
- Implementar interfaces generadas
- Garantizar cumplimiento

### ✅ **OpenAPI**
- Estándar para definir APIs
- paths: endpoints
- components/schemas: modelos
- Herramientas: Swagger UI, generators

### ✅ **Generación de Código**
- mvn compile genera automáticamente
- Interfaces JAX-RS
- DTOs con getters/setters
- Ubicación: target/generated-sources/

### ✅ **JAX-RS**
- Especificación Java para REST
- Anotaciones: @Path, @GET, @POST
- Serialización JSON automática
- Response building

---

## 🔄 Flujo Completo del Desarrollo

```
1. DISEÑAR CONTRATO
   └─→ src/main/openapi/openapi.yaml
   └─→ Definir paths, schemas, validaciones

2. CONFIGURAR GENERADOR
   └─→ application.properties
   └─→ base-package, opciones

3. GENERAR CÓDIGO
   └─→ mvn compile
   └─→ Crea interfaces y DTOs

4. IMPLEMENTAR
   └─→ ValidadorResource implements DefaultApi
   └─→ Lógica de negocio

5. EJECUTAR DEV MODE
   └─→ mvn quarkus:dev
   └─→ Hot reload activo

6. PROBAR
   └─→ Swagger UI: http://localhost:8080/q/swagger-ui
   └─→ Navegador, curl, Postman

7. ITERAR
   └─→ Modificar código
   └─→ Guardar
   └─→ Ver cambios inmediatamente
```

---

## 💡 Mejores Prácticas

### ✅ **Diseño de Contratos**
- Usar nombres descriptivos en operationId
- Documentar cada endpoint (summary, description)
- Definir validaciones en el schema
- Usar referencias ($ref) para reutilizar

### ✅ **Organización de Código**
- Interfaces generadas: no editar
- Implementaciones: lógica de negocio
- Servicios: separar responsabilidades
- DTOs: solo datos, sin lógica

### ✅ **Desarrollo**
- Usar Dev Mode siempre
- Hot reload para iteración rápida
- Continuous testing activo
- Swagger UI para validación

### ✅ **Versionado**
- Versionar openapi.yaml en Git
- Documentar cambios breaking
- Usar semantic versioning
- Mantener backward compatibility

---

## 🚀 Siguientes Pasos

Después de dominar estos conceptos, estás listo para:

1. **Capítulo 3**: Persistencia con Panache
2. **Capítulo 4**: CDI e Inyección de Dependencias
3. **Capítulo 5**: Validaciones avanzadas
4. **Capítulo 6**: Manejo de errores
5. **Capítulo 7**: Testing completo
6. **Capítulo 8**: Seguridad y autenticación
7. **Capítulo 9**: Reactive programming
8. **Capítulo 10**: Deployment y producción

---

## 📚 Recursos Adicionales

### Documentación Oficial
- [Quarkus Guides](https://quarkus.io/guides/)
- [OpenAPI Specification](https://swagger.io/specification/)
- [JAX-RS Specification](https://jakarta.ee/specifications/restful-ws/)
- [Quarkus OpenAPI Generator](https://docs.quarkiverse.io/quarkus-openapi-generator/dev/)

### Herramientas
- [Swagger Editor](https://editor.swagger.io/)
- [OpenAPI Generator](https://openapi-generator.tech/)
- [Postman](https://www.postman.com/)
- [Insomnia](https://insomnia.rest/)

### Comunidad
- [Quarkus GitHub](https://github.com/quarkusio/quarkus)
- [Stack Overflow - Tag: quarkus](https://stackoverflow.com/questions/tagged/quarkus)
- [Quarkus Zulip Chat](https://quarkusio.zulipchat.com/)

---

## ✅ Checklist de Conocimientos

Después de estudiar este capítulo, deberías poder:

- [ ] Instalar y configurar entorno Quarkus (Java, Maven, CLI)
- [ ] Crear proyecto Quarkus desde cero
- [ ] Entender estructura Maven (pom.xml, src/, target/)
- [ ] Agregar y configurar extensiones
- [ ] Usar Dev Mode con hot reload
- [ ] Diseñar contratos OpenAPI válidos
- [ ] Configurar OpenAPI Generator
- [ ] Generar código desde contratos
- [ ] Implementar interfaces JAX-RS generadas
- [ ] Crear endpoints REST básicos
- [ ] Usar Swagger UI para documentación
- [ ] Probar APIs desde navegador/curl
- [ ] Explicar diferencia Contract-First vs Code-First
- [ ] Trabajar con DTOs generados
- [ ] Aprovechar serialización JSON automática

---

**🎉 ¡Teoría completa del Capítulo 2!**

*Ahora tienes las bases sólidas para desarrollar microservicios profesionales con Quarkus usando Contract-First.*