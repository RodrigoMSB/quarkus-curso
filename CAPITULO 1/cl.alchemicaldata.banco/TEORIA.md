# 📚 TEORIA.md - Fundamentos de Quarkus

Teoría completa para entender el desarrollo de microservicios con Quarkus desde cero.

---

## 📖 Índice

1. [¿Qué es Quarkus?](#1-qué-es-quarkus)
2. [¿Por qué Quarkus y no Spring Boot?](#2-por-qué-quarkus-y-no-spring-boot)
3. [Arquitectura de Quarkus](#3-arquitectura-de-quarkus)
4. [Conceptos Fundamentales](#4-conceptos-fundamentales)
5. [JAX-RS y REST en Quarkus](#5-jax-rs-y-rest-en-quarkus)
6. [Inyección de Dependencias (CDI)](#6-inyección-de-dependencias-cdi)
7. [Extensiones de Quarkus](#7-extensiones-de-quarkus)
8. [Dev Mode y Hot Reload](#8-dev-mode-y-hot-reload)
9. [Maven y el ciclo de vida](#9-maven-y-el-ciclo-de-vida)
10. [Compilación Nativa con GraalVM](#10-compilación-nativa-con-graalvm)

---

## 1. ¿Qué es Quarkus?

### Definición

**Quarkus** es un framework Java moderno diseñado específicamente para:
- **Kubernetes y entornos Cloud Native**
- **Microservicios de alta performance**
- **Aplicaciones serverless**
- **Arquitecturas reactivas**

### Características principales

- ⚡ **Supersonic Subatomic Java**: Arranque ultra rápido (~0.015s)
- 🪶 **Footprint mínimo**: Consume muy poca memoria RAM
- 🔥 **Developer Joy**: Experiencia de desarrollo excepcional
- 🚀 **Native Compilation**: Compila a binario nativo con GraalVM
- 🔄 **Hot Reload**: Cambios en vivo sin reiniciar

### Analogía

Imagina que los frameworks tradicionales (como Spring Boot) son como un **motor diésel de camión**: potente pero pesado y tarda en arrancar. 

Quarkus es como un **motor de Fórmula 1**: arranca instantáneamente, consume menos combustible (memoria), y está optimizado para máximo rendimiento en condiciones específicas (cloud/containers).

---

## 2. ¿Por qué Quarkus y no Spring Boot?

### Comparación técnica

| Aspecto | Spring Boot | Quarkus |
|---------|-------------|---------|
| **Tiempo de arranque (JVM)** | ~2-3 segundos | ~0.5-1 segundo |
| **Tiempo de arranque (Native)** | No disponible | ~0.015 segundos |
| **Memoria consumida (JVM)** | ~200-300 MB | ~70-120 MB |
| **Memoria consumida (Native)** | N/A | ~15-30 MB |
| **Optimizado para** | Aplicaciones monolíticas | Microservicios/Cloud |
| **Hot Reload** | DevTools (limitado) | Completo y automático |
| **Curva de aprendizaje** | Moderada | Baja (si conoces Java EE) |
| **Ecosistema** | Muy maduro | En crecimiento rápido |

### Casos de uso ideales para Quarkus

✅ **Usa Quarkus cuando:**
- Desarrollas microservicios pequeños y enfocados
- Despliegas en Kubernetes/OpenShift
- Necesitas serverless/FaaS (AWS Lambda, Azure Functions)
- El tiempo de arranque y consumo de memoria son críticos
- Trabajas con arquitecturas reactivas
- Quieres compilación nativa

❌ **Considera Spring Boot cuando:**
- Tienes un monolito grande ya existente
- Necesitas absolutamente todas las librerías del ecosistema Spring
- Tu equipo ya domina Spring a fondo
- No te importa el consumo de recursos

---

## 3. Arquitectura de Quarkus

### Stack tecnológico

```
┌─────────────────────────────────────┐
│     Tu Aplicación (Resources)      │
├─────────────────────────────────────┤
│  JAX-RS / REST / JSON / Validation  │
├─────────────────────────────────────┤
│      CDI (Inyección Dependencias)   │
├─────────────────────────────────────┤
│    Eclipse MicroProfile / Jakarta   │
├─────────────────────────────────────┤
│         Vert.x (Reactive Core)      │
├─────────────────────────────────────┤
│       Netty (Network Layer)         │
├─────────────────────────────────────┤
│          JVM / GraalVM Native       │
└─────────────────────────────────────┘
```

### Componentes clave

1. **Vert.x**: Motor reactivo no bloqueante (event loop)
2. **Eclipse MicroProfile**: Especificaciones para microservicios
3. **Jakarta EE**: Estándares Java empresariales modernos
4. **GraalVM**: Compilador ahead-of-time (AOT) para binarios nativos
5. **Netty**: Framework de red asíncrono

### Filosofía: Build-Time vs Runtime

**Quarkus hace en BUILD-TIME lo que otros hacen en RUNTIME:**

```
Framework Tradicional (Spring Boot):
┌──────────┐     ┌──────────────────────────────┐
│  Build   │ --> │  Runtime                     │
│  (Maven) │     │  - Escaneo classpath         │
│          │     │  - Reflexión                 │
│          │     │  - Proxy dinámicos           │
│          │     │  - Configuración             │
└──────────┘     └──────────────────────────────┘
                          ⏱️ TIEMPO PERDIDO

Quarkus:
┌────────────────────────────────┐     ┌──────────┐
│  Build Time                    │ --> │  Runtime │
│  - Escaneo classpath           │     │          │
│  - Análisis de dependencias    │     │  ⚡ SOLO │
│  - Generación de metadata      │     │  LÓGICA  │
│  - Bytecode enhancement        │     │  NEGOCIO │
│  - Dead code elimination       │     │          │
└────────────────────────────────┘     └──────────┘
```

**Resultado:** Aplicación optimizada que arranca instantáneamente.

---

## 4. Conceptos Fundamentales

### 4.1 Microservicios

**Definición:** Arquitectura donde una aplicación se divide en servicios pequeños, independientes y desplegables por separado.

**Características:**
- ✅ Cada servicio hace UNA cosa bien
- ✅ Comunicación vía APIs (REST, gRPC, mensajería)
- ✅ Base de datos por servicio (database per service)
- ✅ Despliegue independiente
- ✅ Tecnologías heterogéneas

**Analogía:** 
Un monolito es como una **fábrica gigante** donde todo se produce bajo un mismo techo. Los microservicios son como **talleres especializados**: uno hace zapatos, otro hace cordones, otro las cajas. Cada uno es experto en su área y pueden trabajar independientemente.

### 4.2 Cloud Native

**Definición:** Aplicaciones diseñadas para aprovechar al máximo los entornos cloud.

**Principios (12-Factor App):**
1. **Codebase**: Un repo por servicio
2. **Dependencies**: Declaradas explícitamente
3. **Config**: En variables de entorno, no hardcoded
4. **Backing Services**: Tratados como recursos adjuntos
5. **Build/Release/Run**: Separación estricta
6. **Stateless**: Sin estado en el proceso
7. **Port Binding**: Autocontenido, expone puerto
8. **Concurrency**: Escala horizontalmente
9. **Disposability**: Arranque rápido, apagado graceful
10. **Dev/Prod Parity**: Ambientes similares
11. **Logs**: Como streams de eventos
12. **Admin Processes**: Como procesos one-off

### 4.3 Reactive Programming

**Definición:** Paradigma de programación asíncrono orientado a flujos de datos y propagación de cambios.

**Características:**
- **Responsive**: Responde rápido
- **Resilient**: Maneja fallos con gracia
- **Elastic**: Escala bajo demanda
- **Message Driven**: Comunicación asíncrona

**En Quarkus:**
```java
// Imperativo (bloqueante)
@GET
public String getUser() {
    String data = database.query(); // ⏸️ ESPERA
    return data;
}

// Reactivo (no bloqueante)
@GET
public Uni<String> getUser() {
    return database.queryAsync() // 🔄 NO ESPERA
        .onItem().transform(data -> data);
}
```

---

## 5. JAX-RS y REST en Quarkus

### 5.1 ¿Qué es JAX-RS?

**JAX-RS** (Jakarta RESTful Web Services) es la especificación estándar de Java para crear APIs REST.

**Características:**
- Usa anotaciones para definir endpoints
- Mapea HTTP a métodos Java
- Manejo automático de serialización JSON/XML
- Parte de Jakarta EE

### 5.2 Anatomía de un Resource

```java
package cl.alchemicaldata.banco;

import jakarta.ws.rs.GET;           // HTTP GET
import jakarta.ws.rs.POST;          // HTTP POST
import jakarta.ws.rs.Path;          // URL path
import jakarta.ws.rs.Produces;      // Content-Type respuesta
import jakarta.ws.rs.Consumes;      // Content-Type entrada
import jakarta.ws.rs.PathParam;     // Parámetro de ruta
import jakarta.ws.rs.QueryParam;    // Parámetro query string
import jakarta.ws.rs.core.MediaType;

@Path("/hello")                      // Ruta base: /hello
public class HelloResource {

    @GET                             // HTTP GET
    @Produces(MediaType.TEXT_PLAIN)  // Devuelve texto plano
    public String hello() {
        return "Hola mundo desde Quarkus 🧉";
    }
    
    @GET
    @Path("/{nombre}")               // /hello/Juan
    @Produces(MediaType.TEXT_PLAIN)
    public String saludar(@PathParam("nombre") String nombre) {
        return "Hola " + nombre;
    }
    
    @POST                            // HTTP POST
    @Consumes(MediaType.APPLICATION_JSON)  // Recibe JSON
    @Produces(MediaType.APPLICATION_JSON)  // Devuelve JSON
    public Usuario crear(Usuario usuario) {
        // Lógica de negocio
        return usuario;
    }
}
```

### 5.3 Mapeo HTTP → Java

| HTTP Method | Anotación | Uso típico |
|-------------|-----------|------------|
| **GET** | `@GET` | Obtener recursos |
| **POST** | `@POST` | Crear recursos |
| **PUT** | `@PUT` | Actualizar (completo) |
| **PATCH** | `@PATCH` | Actualizar (parcial) |
| **DELETE** | `@DELETE` | Eliminar recursos |

### 5.4 Content Types comunes

```java
// Texto plano
@Produces(MediaType.TEXT_PLAIN)
public String texto() { return "Hola"; }

// JSON (más común en APIs)
@Produces(MediaType.APPLICATION_JSON)
public Usuario json() { return new Usuario(); }

// XML
@Produces(MediaType.APPLICATION_XML)
public Usuario xml() { return new Usuario(); }

// HTML
@Produces(MediaType.TEXT_HTML)
public String html() { return "<h1>Hola</h1>"; }
```

---

## 6. Inyección de Dependencias (CDI)

### 6.1 ¿Qué es CDI?

**CDI** (Contexts and Dependency Injection) es el sistema de inyección de dependencias de Jakarta EE.

**Propósito:** Evitar el acoplamiento fuerte entre componentes.

### 6.2 Sin CDI vs Con CDI

**❌ Sin CDI (Acoplamiento fuerte):**
```java
public class PedidoResource {
    // Creación manual = acoplamiento
    private PedidoService service = new PedidoService();
    
    @GET
    public List<Pedido> listar() {
        return service.listarTodos();
    }
}
```

**✅ Con CDI (Inyección):**
```java
@Path("/pedidos")
public class PedidoResource {
    
    @Inject  // Quarkus inyecta automáticamente
    PedidoService service;
    
    @GET
    public List<Pedido> listar() {
        return service.listarTodos();
    }
}
```

### 6.3 Scopes (Alcances)

```java
// Sin scope = @Dependent (nueva instancia cada vez)
public class MiServicio { }

// Una instancia por aplicación (Singleton)
@ApplicationScoped
public class ConfigService { }

// Una instancia por request HTTP
@RequestScoped
public class UsuarioActual { }

// Una instancia por sesión (si hay estado)
@SessionScoped
public class CarritoCompra { }
```

### 6.4 Analogía CDI

Imagina un **restaurante**:

- **Sin CDI**: El mesero (Resource) tiene que ir a la cocina, cocinar la comida, traer los ingredientes, lavar platos. Todo él mismo.

- **Con CDI**: El mesero (Resource) solo pide al chef (Service inyectado) que cocine. Cada uno hace su trabajo. Si necesitas cambiar al chef, el mesero no se entera.

---

## 7. Extensiones de Quarkus

### 7.1 ¿Qué son las extensiones?

Las **extensiones** son módulos que agregan funcionalidad a Quarkus. Son el equivalente a "starters" de Spring Boot, pero optimizadas para Quarkus.

### 7.2 Extensiones principales

| Extensión | Propósito | Comando |
|-----------|-----------|---------|
| `rest` | APIs REST (JAX-RS) | `quarkus ext add rest` |
| `rest-jackson` | REST + JSON con Jackson | `quarkus ext add rest-jackson` |
| `hibernate-orm-panache` | ORM simplificado | `quarkus ext add hibernate-orm-panache` |
| `jdbc-postgresql` | Driver PostgreSQL | `quarkus ext add jdbc-postgresql` |
| `smallrye-openapi` | Documentación OpenAPI | `quarkus ext add smallrye-openapi` |
| `rest-client` | Cliente REST | `quarkus ext add rest-client` |
| `security-jdbc` | Seguridad con JDBC | `quarkus ext add security-jdbc` |
| `kafka` | Mensajería Kafka | `quarkus ext add kafka` |

### 7.3 Cómo funcionan

```
Usuario agrega extensión
         ↓
Maven/Gradle descarga dependencia
         ↓
Quarkus detecta extensión en BUILD TIME
         ↓
Genera código optimizado
         ↓
Configura componentes necesarios
         ↓
Listo para usar en runtime
```

### 7.4 Agregar extensiones

**Método 1: CLI**
```bash
quarkus ext add rest-jackson
```

**Método 2: Maven**
```bash
./mvnw quarkus:add-extension -Dextensions="rest-jackson"
```

**Método 3: Manual en pom.xml**
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-jackson</artifactId>
</dependency>
```

---

## 8. Dev Mode y Hot Reload

### 8.1 ¿Qué es Dev Mode?

El **modo desarrollo** de Quarkus permite:
- ✅ Cambios en vivo sin reiniciar
- ✅ Tests continuos en background
- ✅ Dev UI integrada
- ✅ Debug remoto automático

### 8.2 Cómo funciona Hot Reload

```
1. Modificas código Java
         ↓
2. Guardas archivo (Ctrl+S)
         ↓
3. Quarkus detecta cambio
         ↓
4. Recompila solo lo necesario
         ↓
5. Recarga clases en caliente
         ↓
6. Refrescas navegador
         ↓
7. ¡Ves cambios inmediatamente!
```

**Tiempo total:** ~1-2 segundos

### 8.3 Dev UI

Accede a `http://localhost:8080/q/dev` para ver:

- 📊 **Dashboard**: Estado de la aplicación
- 🔧 **Config Editor**: Editar application.properties en vivo
- 🗄️ **Database**: Explorador de base de datos
- 📝 **OpenAPI**: Documentación interactiva
- 🧪 **Continuous Testing**: Tests en tiempo real
- 📈 **Metrics**: Métricas de la app

### 8.4 Comandos interactivos

Mientras está en dev mode, presiona:

```
w - Abrir Dev UI en navegador
d - Abrir documentación
r - Ejecutar todos los tests
s - Ver métricas de la app
h - Ayuda (ver todos los comandos)
q - Salir
```

---

## 9. Maven y el ciclo de vida

### 9.1 ¿Qué es Maven?

**Maven** es una herramienta de:
- 📦 Gestión de dependencias
- 🏗️ Construcción de proyectos (build)
- 📋 Estandarización de estructura

### 9.2 Estructura de proyecto Maven

```
proyecto/
├── pom.xml              ← Configuración Maven
├── src/
│   ├── main/
│   │   ├── java/        ← Código fuente
│   │   └── resources/   ← Archivos de configuración
│   └── test/
│       ├── java/        ← Tests
│       └── resources/   ← Recursos para tests
└── target/              ← Compilado (generado)
```

### 9.3 El archivo pom.xml

```xml
<project>
    <!-- Coordenadas del proyecto -->
    <groupId>cl.alchemicaldata</groupId>
    <artifactId>banco</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    
    <!-- Propiedades -->
    <properties>
        <quarkus.version>3.15.1</quarkus.version>
        <java.version>21</java.version>
    </properties>
    
    <!-- Dependencias -->
    <dependencies>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-rest</artifactId>
        </dependency>
    </dependencies>
    
    <!-- Plugin de Quarkus -->
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

### 9.4 Comandos Maven esenciales

```bash
# Limpiar compilados anteriores
./mvnw clean

# Compilar código fuente
./mvnw compile

# Ejecutar tests
./mvnw test

# Empaquetar (crear JAR)
./mvnw package

# Limpiar + Compilar + Empaquetar
./mvnw clean package

# Modo desarrollo Quarkus
./mvnw quarkus:dev

# Instalar en repositorio local
./mvnw install
```

### 9.5 Maven Wrapper (mvnw)

**¿Por qué mvnw y no mvn?**

```
mvn          → Usa Maven instalado globalmente (puede variar versión)
./mvnw       → Usa Maven específico del proyecto (garantiza versión correcta)
```

**Ventajas:**
- ✅ No requiere Maven instalado
- ✅ Garantiza misma versión en todos los entornos
- ✅ Portable entre desarrolladores

---

## 10. Compilación Nativa con GraalVM

### 10.1 ¿Qué es GraalVM?

**GraalVM** es una JVM avanzada que puede compilar Java a **código nativo** (binario ejecutable).

### 10.2 JVM vs Native

```
┌─────────────────────────────────────────────┐
│           MODO JVM (tradicional)            │
├─────────────────────────────────────────────┤
│  - Requiere JVM instalada                   │
│  - Arranque: ~1 segundo                     │
│  - Memoria: ~120 MB                         │
│  - Portabilidad: JAR funciona en cualquier  │
│    SO con JVM                               │
│  - Warm-up: JIT optimiza en runtime         │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│         MODO NATIVE (GraalVM)               │
├─────────────────────────────────────────────┤
│  - NO requiere JVM                          │
│  - Arranque: ~0.015 segundos (⚡ 60x más)   │
│  - Memoria: ~20 MB (📉 6x menos)            │
│  - Binario específico del SO                │
│  - Optimizado desde inicio (AOT)            │
└─────────────────────────────────────────────┘
```

### 10.3 Cuándo usar Native

**✅ Ideal para:**
- Serverless (AWS Lambda, Azure Functions)
- Microservicios que escalan de 0 a N instancias
- Edge computing / IoT
- CLI tools
- Contenedores con arranque frecuente

**❌ No recomendado para:**
- Apps con mucha reflexión dinámica
- Si usas librerías no compatibles
- Desarrollo local (compilación lenta: ~5 minutos)

### 10.4 Compilar a Native

```bash
# Opción 1: Local (requiere GraalVM instalado)
./mvnw package -Pnative

# Opción 2: En contenedor (sin GraalVM local)
./mvnw package -Pnative -Dquarkus.native.container-build=true

# Ejecutar binario nativo
./target/banco-1.0.0-SNAPSHOT-runner
```

### 10.5 Limitaciones Native

⚠️ **Restricciones:**
- No hay reflexión dinámica en runtime
- No hay carga dinámica de clases
- Configuración adicional para recursos
- Tiempo de build largo (~5-10 min)
- Binario específico del OS (Linux ≠ Windows ≠ macOS)

**Quarkus mitiga esto** haciendo análisis en build-time.

---

## 📊 Resumen Comparativo: Conceptos Clave

| Concepto | Analogía | En Quarkus |
|----------|----------|------------|
| **Microservicio** | Tienda especializada en una cosa | Aplicación Quarkus enfocada |
| **REST** | Menú de restaurante | @Path, @GET, @POST |
| **JAX-RS** | Receta estándar para cocinar | Especificación Java para REST |
| **CDI** | Sistema de contratación de empleados | @Inject automático |
| **Extension** | App de smartphone | Módulo que agrega funcionalidad |
| **Dev Mode** | Cocina con degustación en vivo | Cambios sin reiniciar |
| **Maven** | Gerente de construcción | Orquesta el build |
| **GraalVM Native** | Comida congelada lista | Binario pre-compilado |

---

## 🎯 Flujo de Trabajo Completo

```
1. DESARROLLO
   │
   ├─→ Crear proyecto (quarkus create / code.quarkus.io)
   ├─→ Agregar extensiones necesarias
   ├─→ Escribir código (Resources, Services, Entities)
   ├─→ Configurar (application.properties)
   ├─→ Ejecutar en dev mode (mvnw quarkus:dev)
   └─→ Probar en Dev UI (http://localhost:8080/q/dev)
   
2. TESTING
   │
   ├─→ Tests unitarios (@QuarkusTest)
   ├─→ Tests de integración
   └─→ Tests continuos en dev mode
   
3. BUILD
   │
   ├─→ JVM: mvnw package → JAR
   └─→ Native: mvnw package -Pnative → binario

4. DEPLOYMENT
   │
   ├─→ Container (Docker/Podman)
   ├─→ Kubernetes/OpenShift
   ├─→ Serverless (Lambda, Cloud Run)
   └─→ VM tradicional
```

---

## 🔗 Recursos para Profundizar

### Documentación Oficial
- [Quarkus Guides](https://quarkus.io/guides/)
- [Quarkus Blog](https://quarkus.io/blog/)
- [Jakarta EE Specs](https://jakarta.ee/specifications/)

### Tutoriales
- [Quarkus Insights (Videos)](https://www.youtube.com/c/Quarkusio)
- [Red Hat Developers](https://developers.redhat.com/products/quarkus)

### Libros Recomendados
- "Quarkus for Spring Developers" - Red Hat
- "Understanding Quarkus" - Antonio Goncalves
- "Kubernetes Native Microservices with Quarkus" - Manning

---

## ✅ Checklist de Conocimientos

Después de estudiar esta teoría, deberías poder:

- [ ] Explicar qué es Quarkus y cuándo usarlo
- [ ] Entender la diferencia entre JVM mode y Native mode
- [ ] Crear un endpoint REST con JAX-RS
- [ ] Usar inyección de dependencias con CDI
- [ ] Agregar y configurar extensiones
- [ ] Aprovechar el Dev Mode y Hot Reload
- [ ] Entender la estructura de un proyecto Maven
- [ ] Explicar los beneficios de la compilación Ahead-of-Time
- [ ] Conocer las mejores prácticas de microservicios
