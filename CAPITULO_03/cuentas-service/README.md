# 🏦 Capítulo 3: Microservicio de Gestión de Cuentas Bancarias

Desarrollo completo de un microservicio REST con CRUD, inyección de dependencias (CDI) y arquitectura en capas.

---

## 📋 Prerequisitos

**Asegúrate de tener instalado** (del Capítulo 1):
- ✅ Java 21 (OpenJDK recomendado)
- ✅ Quarkus CLI
- ✅ Git Bash (Windows) o Terminal (macOS)

**Verificar:**
```bash
java -version
quarkus --version
```

Si falta algo, revisa el Capítulo 1.

---

## 🎯 Objetivos del Capítulo

Al completar este ejercicio, dominarás:

✅ **Inyección de Dependencias (CDI)** con `@Inject` y `@ApplicationScoped`  
✅ **CRUD completo** con todos los verbos HTTP (GET, POST, PUT, DELETE)  
✅ **Arquitectura en capas** (Model, Service, Resource)  
✅ **Path parameters** y **Request body**  
✅ **Manejo de errores** con códigos HTTP correctos  
✅ **DTOs** para transferencia de datos  
✅ **Code-First** (código → OpenAPI automático)  

---

## 🏗️ Arquitectura del Proyecto

```
pe.banco.cuentas
│
├── model/                    # DTOs (Data Transfer Objects)
│   └── Cuenta.java          
│
├── service/                  # Lógica de negocio
│   └── CuentaService.java   (@ApplicationScoped, CDI)
│
└── resource/                 # REST Endpoints
    └── CuentaResource.java  (@Inject Service)
```

### Flujo de Petición

```
Cliente HTTP
    ↓
CuentaResource (REST)
    ↓ @Inject
CuentaService (Lógica)
    ↓
Map<String, Cuenta> (Datos en memoria)
```

---

## 🚀 Creación del Proyecto Paso a Paso

### **PASO 1: Crear proyecto Quarkus**

```bash
quarkus create app pe.banco:cuentas-service \
  --java=21 \
  --extension=rest-jackson,smallrye-openapi \
  --no-code

cd cuentas-service

# Dar permisos al Maven Wrapper (primera vez)
chmod +x mvnw
```

**Alternativa con Maven:**
```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.15.1:create \
  -DprojectGroupId=pe.banco \
  -DprojectArtifactId=cuentas-service \
  -Dextensions=rest-jackson,smallrye-openapi

cd cuentas-service
chmod +x mvnw
```

---

### **PASO 2: Crear estructura de packages**

```bash
mkdir -p src/main/java/pe/banco/cuentas/model
mkdir -p src/main/java/pe/banco/cuentas/service
mkdir -p src/main/java/pe/banco/cuentas/resource
```

---

### **PASO 3: Crear el DTO (Cuenta)**

**Crear archivo:** `src/main/java/pe/banco/cuentas/model/Cuenta.java`

```java
package pe.banco.cuentas.model;

import java.math.BigDecimal;

public class Cuenta {
    
    private String numero;
    private String titular;
    private BigDecimal saldo;
    private String tipoCuenta; // AHORRO, CORRIENTE
    
    public Cuenta() {
    }
    
    public Cuenta(String numero, String titular, BigDecimal saldo, String tipoCuenta) {
        this.numero = numero;
        this.titular = titular;
        this.saldo = saldo;
        this.tipoCuenta = tipoCuenta;
    }
    
    // Getters y Setters
    public String getNumero() {
        return numero;
    }
    
    public void setNumero(String numero) {
        this.numero = numero;
    }
    
    public String getTitular() {
        return titular;
    }
    
    public void setTitular(String titular) {
        this.titular = titular;
    }
    
    public BigDecimal getSaldo() {
        return saldo;
    }
    
    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }
    
    public String getTipoCuenta() {
        return tipoCuenta;
    }
    
    public void setTipoCuenta(String tipoCuenta) {
        this.tipoCuenta = tipoCuenta;
    }
}
```

---

### **PASO 4: Crear el Service con CDI**

**Crear archivo:** `src/main/java/pe/banco/cuentas/service/CuentaService.java`

```java
package pe.banco.cuentas.service;

import pe.banco.cuentas.model.Cuenta;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class CuentaService {
    
    private final Map<String, Cuenta> cuentas = new ConcurrentHashMap<>();
    
    public CuentaService() {
        // Datos de ejemplo
        cuentas.put("1000000001", new Cuenta("1000000001", "Juan Pérez", new BigDecimal("5000.00"), "AHORRO"));
        cuentas.put("1000000002", new Cuenta("1000000002", "María López", new BigDecimal("12000.50"), "CORRIENTE"));
        cuentas.put("1000000003", new Cuenta("1000000003", "Carlos Ruiz", new BigDecimal("800.00"), "AHORRO"));
    }
    
    public List<Cuenta> listarTodas() {
        return new ArrayList<>(cuentas.values());
    }
    
    public Cuenta obtenerPorNumero(String numero) {
        return cuentas.get(numero);
    }
    
    public Cuenta crear(Cuenta cuenta) {
        cuentas.put(cuenta.getNumero(), cuenta);
        return cuenta;
    }
    
    public Cuenta actualizar(String numero, Cuenta cuentaActualizada) {
        if (cuentas.containsKey(numero)) {
            cuentaActualizada.setNumero(numero);
            cuentas.put(numero, cuentaActualizada);
            return cuentaActualizada;
        }
        return null;
    }
    
    public boolean eliminar(String numero) {
        return cuentas.remove(numero) != null;
    }
}
```

**Conceptos clave:**
- `@ApplicationScoped`: Una sola instancia del servicio en toda la aplicación
- `ConcurrentHashMap`: Thread-safe para entorno concurrente
- Datos en memoria (sin base de datos)

---

### **PASO 5: Crear el Resource (REST Endpoints)**

**Crear archivo:** `src/main/java/pe/banco/cuentas/resource/CuentaResource.java`

```java
package pe.banco.cuentas.resource;

import pe.banco.cuentas.model.Cuenta;
import pe.banco.cuentas.service.CuentaService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/cuentas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CuentaResource {
    
    @Inject
    CuentaService cuentaService;
    
    @GET
    public List<Cuenta> listar() {
        return cuentaService.listarTodas();
    }
    
    @GET
    @Path("/{numero}")
    public Response obtener(@PathParam("numero") String numero) {
        Cuenta cuenta = cuentaService.obtenerPorNumero(numero);
        if (cuenta == null) {
            return Response.status(404).entity("Cuenta no encontrada").build();
        }
        return Response.ok(cuenta).build();
    }
    
    @POST
    public Response crear(Cuenta cuenta) {
        Cuenta nueva = cuentaService.crear(cuenta);
        return Response.status(201).entity(nueva).build();
    }
    
    @PUT
    @Path("/{numero}")
    public Response actualizar(@PathParam("numero") String numero, Cuenta cuenta) {
        Cuenta actualizada = cuentaService.actualizar(numero, cuenta);
        if (actualizada == null) {
            return Response.status(404).entity("Cuenta no encontrada").build();
        }
        return Response.ok(actualizada).build();
    }
    
    @DELETE
    @Path("/{numero}")
    public Response eliminar(@PathParam("numero") String numero) {
        boolean eliminada = cuentaService.eliminar(numero);
        if (!eliminada) {
            return Response.status(404).entity("Cuenta no encontrada").build();
        }
        return Response.status(204).build();
    }
}
```

**Conceptos clave:**
- `@Inject`: Inyección de dependencias (CDI)
- `@Path`: Ruta base del recurso
- `@Produces/@Consumes`: Tipo de contenido JSON
- `Response.status()`: Control de códigos HTTP

---

### **PASO 6: Ejecutar el proyecto**

```bash
./mvnw quarkus:dev
```

**Salida esperada:**
```
__  ____  __  _____   ___  __ ____  ______ 
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   

INFO  [io.quarkus] cuentas-service 1.0.0-SNAPSHOT on JVM started in 1.234s
INFO  [io.quarkus] Listening on: http://localhost:8080
```

---

## 🧪 Probar el Microservicio

### URLs Importantes

| Recurso | URL |
|---------|-----|
| **Endpoints** | http://localhost:8080/cuentas |
| **Swagger UI** | http://localhost:8080/q/swagger-ui |
| **OpenAPI Spec** | http://localhost:8080/q/openapi |
| **Dev UI** | http://localhost:8080/q/dev |
| **Health Check** | http://localhost:8080/q/health |

---

### Pruebas con Navegador

#### **1. Listar todas las cuentas (GET)**

```
http://localhost:8080/cuentas
```

**Respuesta esperada:**
```json
[
  {
    "numero": "1000000001",
    "titular": "Juan Pérez",
    "saldo": 5000.00,
    "tipoCuenta": "AHORRO"
  },
  {
    "numero": "1000000002",
    "titular": "María López",
    "saldo": 12000.50,
    "tipoCuenta": "CORRIENTE"
  },
  {
    "numero": "1000000003",
    "titular": "Carlos Ruiz",
    "saldo": 800.00,
    "tipoCuenta": "AHORRO"
  }
]
```

#### **2. Obtener una cuenta específica (GET)**

```
http://localhost:8080/cuentas/1000000001
```

**Respuesta esperada:**
```json
{
  "numero": "1000000001",
  "titular": "Juan Pérez",
  "saldo": 5000.00,
  "tipoCuenta": "AHORRO"
}
```

---

### Pruebas con Swagger UI

**Abrir:** http://localhost:8080/q/swagger-ui

#### **3. Crear cuenta (POST)**

1. Expandir **POST /cuentas**
2. Click en **"Try it out"**
3. Request body:
```json
{
  "numero": "1000000004",
  "titular": "Ana Torres",
  "saldo": 3500.00,
  "tipoCuenta": "AHORRO"
}
```
4. Click **"Execute"**

**Respuesta esperada:** `201 Created`

#### **4. Actualizar cuenta (PUT)**

1. Expandir **PUT /cuentas/{numero}**
2. Click en **"Try it out"**
3. En **numero**: `1000000004`
4. Request body:
```json
{
  "numero": "1000000004",
  "titular": "Ana Torres",
  "saldo": 5000.00,
  "tipoCuenta": "CORRIENTE"
}
```
5. Click **"Execute"**

**Respuesta esperada:** `200 OK` con cuenta actualizada

#### **5. Eliminar cuenta (DELETE)**

1. Expandir **DELETE /cuentas/{numero}**
2. Click en **"Try it out"**
3. En **numero**: `1000000003`
4. Click **"Execute"**

**Respuesta esperada:** `204 No Content`

---

### Pruebas con curl

#### **GET - Listar todas**
```bash
curl http://localhost:8080/cuentas
```

#### **GET - Obtener una**
```bash
curl http://localhost:8080/cuentas/1000000001
```

#### **POST - Crear**
```bash
curl -X POST http://localhost:8080/cuentas \
  -H "Content-Type: application/json" \
  -d '{
    "numero": "1000000004",
    "titular": "Ana Torres",
    "saldo": 3500.00,
    "tipoCuenta": "AHORRO"
  }'
```

#### **PUT - Actualizar**
```bash
curl -X PUT http://localhost:8080/cuentas/1000000004 \
  -H "Content-Type: application/json" \
  -d '{
    "numero": "1000000004",
    "titular": "Ana Torres",
    "saldo": 5000.00,
    "tipoCuenta": "CORRIENTE"
  }'
```

#### **DELETE - Eliminar**
```bash
curl -X DELETE http://localhost:8080/cuentas/1000000003
```

---

### Pruebas con PowerShell (Windows)

#### **GET - Listar**
```powershell
Invoke-WebRequest -Uri http://localhost:8080/cuentas | Select-Object -Expand Content
```

#### **POST - Crear**
```powershell
$body = @{
    numero = "1000000004"
    titular = "Ana Torres"
    saldo = 3500.00
    tipoCuenta = "AHORRO"
} | ConvertTo-Json

Invoke-WebRequest -Method POST -Uri http://localhost:8080/cuentas `
  -ContentType "application/json" -Body $body
```

---

## 📁 Estructura Final del Proyecto

```
cuentas-service/
├── mvnw
├── mvnw.cmd
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── pe/banco/cuentas/
│   │   │       ├── model/
│   │   │       │   └── Cuenta.java
│   │   │       ├── service/
│   │   │       │   └── CuentaService.java
│   │   │       └── resource/
│   │   │           └── CuentaResource.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/
└── target/
```

---

## 🎯 Conceptos Cubiertos

### 1. **Inyección de Dependencias (CDI)**

```java
@ApplicationScoped              // Scope del bean
public class CuentaService {    // Servicio inyectable
    // ...
}

@Path("/cuentas")
public class CuentaResource {
    @Inject                     // Inyección automática
    CuentaService cuentaService;
}
```

**Scopes disponibles:**
- `@ApplicationScoped`: Una instancia por aplicación (Singleton)
- `@RequestScoped`: Una instancia por request HTTP
- `@Dependent`: Nueva instancia cada vez (por defecto)

### 2. **CRUD Completo**

| Operación | Verbo HTTP | Endpoint | Código HTTP |
|-----------|------------|----------|-------------|
| **Create** | POST | `/cuentas` | 201 Created |
| **Read All** | GET | `/cuentas` | 200 OK |
| **Read One** | GET | `/cuentas/{numero}` | 200 OK / 404 Not Found |
| **Update** | PUT | `/cuentas/{numero}` | 200 OK / 404 Not Found |
| **Delete** | DELETE | `/cuentas/{numero}` | 204 No Content / 404 Not Found |

### 3. **Arquitectura en Capas**

```
Resource Layer (REST)
    ↓
Service Layer (Lógica de negocio)
    ↓
Data Layer (En memoria - futuro: DB)
```

**Ventajas:**
- ✅ Separación de responsabilidades
- ✅ Código mantenible
- ✅ Fácil testing
- ✅ Reutilización de lógica

### 4. **Manejo de Errores**

```java
if (cuenta == null) {
    return Response.status(404)
        .entity("Cuenta no encontrada")
        .build();
}
```

**Códigos HTTP usados:**
- `200 OK`: Operación exitosa
- `201 Created`: Recurso creado
- `204 No Content`: Eliminado exitosamente
- `404 Not Found`: Recurso no encontrado

---

## 🔥 Hot Reload en Acción

Con `quarkus:dev` corriendo:

1. **Modificar** `CuentaService.java`:
```java
public List<Cuenta> listarTodas() {
    System.out.println("🔥 Listando cuentas...");  // Agregar log
    return new ArrayList<>(cuentas.values());
}
```

2. **Guardar** el archivo (Cmd+S / Ctrl+S)

3. **Refrescar** `http://localhost:8080/cuentas`

4. **Ver el log** en la consola 🔥

**¡Los cambios se aplican instantáneamente!**

---

## 🚨 Solución de Problemas

### Error: "Port 8080 already in use"

**Causa:** Otro proceso usa el puerto 8080

**Solución 1 - Liberar puerto (macOS/Linux/Git Bash):**
```bash
lsof -ti:8080 | xargs kill -9
./mvnw quarkus:dev
```

**Solución 2 - Liberar puerto (Windows - PowerShell como admin - alternativa):**
```powershell
Get-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess | Stop-Process
```

**Solución 3 - Cambiar puerto:**

En `application.properties`:
```properties
quarkus.http.port=8081
```

### Error: "CuentaService cannot be resolved"

**Causa:** Falta compilar

**Solución:**
```bash
./mvnw clean compile
./mvnw quarkus:dev
```

### Error: 404 en todos los endpoints

**Causa:** `@Path` incorrecto o package mal ubicado

**Solución:**
1. Verificar que `CuentaResource` tenga `@Path("/cuentas")`
2. Verificar packages: `pe.banco.cuentas.resource`
3. Recompilar: `./mvnw clean compile`

---

## 📚 Recursos Adicionales

### Documentación

- [Quarkus CDI Reference](https://quarkus.io/guides/cdi-reference)
- [Quarkus REST Guide](https://quarkus.io/guides/rest)
- [JAX-RS Specification](https://jakarta.ee/specifications/restful-ws/)

### Siguientes Pasos

Después de dominar este capítulo:
1. **Capítulo 4:** Persistencia con Hibernate ORM Panache
2. **Capítulo 5:** Validaciones con Bean Validation
3. **Capítulo 6:** Manejo avanzado de errores
4. **Capítulo 7:** Seguridad y autenticación

---

## ✅ Checklist de Aprendizaje

Después de completar este ejercicio, deberías poder:

- [ ] Crear proyecto Quarkus desde CLI
- [ ] Organizar código en packages (model, service, resource)
- [ ] Usar `@Inject` para inyección de dependencias
- [ ] Entender `@ApplicationScoped` y otros scopes
- [ ] Implementar CRUD completo con JAX-RS
- [ ] Usar `@Path`, `@PathParam`, `@QueryParam`
- [ ] Manejar Request body con JSON
- [ ] Retornar Response con códigos HTTP correctos
- [ ] Probar endpoints con Swagger UI
- [ ] Aprovechar hot reload para desarrollo rápido

---

**🎉 ¡Proyecto completado! Ahora dominas CDI y microservicios REST con Quarkus.**