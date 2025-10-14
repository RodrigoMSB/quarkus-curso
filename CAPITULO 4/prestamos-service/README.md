# 🏦 Capítulo 4: Sistema de Préstamos Bancarios con Persistencia

Desarrollo completo de un microservicio con **Hibernate ORM + Panache**, persistencia en base de datos, y patrones Active Record y Repository.

---

## 📋 Prerequisitos

- **Java 21** (OpenJDK recomendado)
- **Maven 3.9+** (o Maven Wrapper incluido)
- **PostgreSQL** (o H2 como alternativa)
- **Quarkus CLI** (opcional pero recomendado)
- **IDE** (IntelliJ IDEA Community, VSCode, Eclipse)
- **Postman o Swagger UI** (para probar endpoints)

---

## 🎯 Objetivos del Capítulo

Al completar este ejercicio, dominarás:

✅ **Hibernate ORM con Panache** - Simplificación de JPA  
✅ **Active Record Pattern** - Entidades con lógica de persistencia  
✅ **Repository Pattern** - Separación de acceso a datos  
✅ **Relaciones JPA** - @OneToMany, @ManyToOne  
✅ **Generación automática de datos** - Cuotas de préstamos  
✅ **Transacciones** con `@Transactional`  
✅ **Lazy Loading** y `@JsonIgnore` para evitar loops  
✅ **PostgreSQL** - Base de datos relacional real  

---

## 🗂️ Arquitectura del Proyecto

```
pe.banco.prestamos
│
├── model/                       # Entidades JPA
│   ├── Cliente.java            (PanacheEntity - Active Record)
│   ├── Prestamo.java           (PanacheEntity - Active Record)
│   └── Cuota.java              (PanacheEntity - Active Record)
│
├── repository/                  # Repositorios
│   └── ClienteRepository.java  (PanacheRepository - Repository Pattern)
│
└── resource/                    # REST Endpoints
    ├── ClienteResource.java
    └── PrestamoResource.java
```

### Modelo de Datos

```
┌──────────────┐
│   CLIENTE    │
│──────────────│
│ id (PK)      │
│ nombre       │
│ dni (UNIQUE) │
│ email (UK)   │
│ telefono     │
└──────────────┘
       │
       │ 1:N
       ▼
┌──────────────┐
│   PRESTAMO   │
│──────────────│
│ id (PK)      │
│ cliente_id   │
│ monto        │
│ plazoMeses   │
│ tasaInteres  │
│ fechaDesbols │
│ estado       │
└──────────────┘
       │
       │ 1:N
       ▼
┌──────────────┐
│    CUOTA     │
│──────────────│
│ id (PK)      │
│ prestamo_id  │
│ numeroCuota  │
│ monto        │
│ fechaVencim. │
│ fechaPago    │
│ pagada       │
└──────────────┘
```

---

## 🚀 Creación del Proyecto Paso a Paso

### **PASO 1: Crear proyecto Quarkus**

**macOS/Linux/Git Bash:**
```bash
quarkus create app pe.banco:prestamos-service \
  --java=21 \
  --extension=hibernate-orm-panache,jdbc-postgresql,rest-jackson,smallrye-openapi \
  --no-code

cd prestamos-service
```

**Windows (PowerShell/CMD):**
```cmd
quarkus create app pe.banco:prestamos-service --java=21 --extension=hibernate-orm-panache,jdbc-postgresql,rest-jackson,smallrye-openapi --no-code

cd prestamos-service
```

**Extensiones incluidas:**
- ✅ `hibernate-orm-panache` → JPA simplificado
- ✅ `jdbc-postgresql` → Driver PostgreSQL
- ✅ `rest-jackson` → REST + JSON
- ✅ `smallrye-openapi` → Swagger UI automático

---

### **PASO 2: Configurar Base de Datos**

Edita `src/main/resources/application.properties`:

```properties
# ===================================
# CONFIGURACIÓN BASE DE LA APLICACIÓN
# ===================================
quarkus.application.name=prestamos-service
quarkus.http.port=8080

# ===================================
# CONFIGURACIÓN DE BASE DE DATOS
# ===================================
# Datasource PostgreSQL
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=postgres
quarkus.datasource.password=postgres
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/prestamos_db

# Hibernate ORM
quarkus.hibernate-orm.database.generation=update
quarkus.hibernate-orm.log.sql=true
quarkus.hibernate-orm.sql-load-script=no-file

# ===================================
# CONFIGURACIÓN DE DESARROLLO
# ===================================
quarkus.log.console.format=%d{HH:mm:ss} %-5p [%c{2.}] (%t) %s%e%n
quarkus.log.level=INFO
quarkus.log.category."pe.banco.prestamos".level=DEBUG
```

**Importante:**
- `update` → Mantiene datos entre reinicios (vs `drop-and-create`)
- `log.sql=true` → Muestra queries SQL en consola
- Asegúrate que PostgreSQL esté corriendo en `localhost:5432`

---

### **PASO 3: Crear estructura de packages**

**macOS/Linux/Git Bash:**
```bash
mkdir -p src/main/java/pe/banco/prestamos/model
mkdir -p src/main/java/pe/banco/prestamos/repository
mkdir -p src/main/java/pe/banco/prestamos/resource
```

**Windows (PowerShell):**
```powershell
New-Item -Path "src\main\java\pe\banco\prestamos\model" -ItemType Directory -Force
New-Item -Path "src\main\java\pe\banco\prestamos\repository" -ItemType Directory -Force
New-Item -Path "src\main\java\pe\banco\prestamos\resource" -ItemType Directory -Force
```

**Windows (CMD):**
```cmd
mkdir src\main\java\pe\banco\prestamos\model
mkdir src\main\java\pe\banco\prestamos\repository
mkdir src\main\java\pe\banco\prestamos\resource
```

---

### **PASO 4: Crear entidad Cliente (Active Record)**

**Archivo:** `src/main/java/pe/banco/prestamos/model/Cliente.java`

```java
package pe.banco.prestamos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "clientes")
public class Cliente extends PanacheEntity {
    
    @Column(nullable = false)
    public String nombre;
    
    @Column(nullable = false, unique = true, length = 8)
    public String dni;
    
    @Column(nullable = false, unique = true)
    public String email;
    
    @Column(nullable = false)
    public String telefono;
    
    @JsonIgnore  // Evita loops infinitos en JSON
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<Prestamo> prestamos;
    
    public Cliente() {
    }
    
    public Cliente(String nombre, String dni, String email, String telefono) {
        this.nombre = nombre;
        this.dni = dni;
        this.email = email;
        this.telefono = telefono;
    }
}
```

**Conceptos clave:**
- `extends PanacheEntity` → Active Record (incluye `id` auto-generado)
- Campos públicos (estilo Panache)
- `@JsonIgnore` → Evita serializar `prestamos` (previene referencias circulares)
- `@OneToMany` → Un cliente puede tener muchos préstamos

---

### **PASO 5: Crear entidad Prestamo**

**Archivo:** `src/main/java/pe/banco/prestamos/model/Prestamo.java`

```java
package pe.banco.prestamos.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "prestamos")
public class Prestamo extends PanacheEntity {
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id")
    public Cliente cliente;
    
    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal monto;
    
    @Column(nullable = false)
    public Integer plazoMeses;
    
    @Column(nullable = false, precision = 5, scale = 2)
    public BigDecimal tasaInteres;
    
    @Column(nullable = false)
    public LocalDate fechaDesembolso;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public EstadoPrestamo estado;
    
    @OneToMany(mappedBy = "prestamo", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<Cuota> cuotas;
    
    public Prestamo() {
    }
    
    public Prestamo(Cliente cliente, BigDecimal monto, Integer plazoMeses, 
                    BigDecimal tasaInteres, LocalDate fechaDesembolso) {
        this.cliente = cliente;
        this.monto = monto;
        this.plazoMeses = plazoMeses;
        this.tasaInteres = tasaInteres;
        this.fechaDesembolso = fechaDesembolso;
        this.estado = EstadoPrestamo.ACTIVO;
    }
    
    public enum EstadoPrestamo {
        ACTIVO,
        PAGADO,
        VENCIDO,
        CANCELADO
    }
}
```

**Conceptos clave:**
- `@ManyToOne` → Muchos préstamos pertenecen a un cliente
- `BigDecimal` → Para dinero (precisión exacta)
- `LocalDate` → Fechas modernas Java 8+
- `@Enumerated(STRING)` → Guarda texto del enum, no ordinal
- `cascade = ALL` → Operaciones en cascada a cuotas

---

### **PASO 6: Crear entidad Cuota**

**Archivo:** `src/main/java/pe/banco/prestamos/model/Cuota.java`

```java
package pe.banco.prestamos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cuotas")
public class Cuota extends PanacheEntity {
    
    @JsonIgnore  // Evita loops infinitos
    @ManyToOne(optional = false)
    @JoinColumn(name = "prestamo_id")
    public Prestamo prestamo;
    
    @Column(nullable = false)
    public Integer numeroCuota;
    
    @Column(nullable = false, precision = 10, scale = 2)
    public BigDecimal monto;
    
    @Column(nullable = false)
    public LocalDate fechaVencimiento;
    
    @Column
    public LocalDate fechaPago;
    
    @Column(nullable = false)
    public Boolean pagada;
    
    public Cuota() {
    }
    
    public Cuota(Prestamo prestamo, Integer numeroCuota, BigDecimal monto, 
                 LocalDate fechaVencimiento) {
        this.prestamo = prestamo;
        this.numeroCuota = numeroCuota;
        this.monto = monto;
        this.fechaVencimiento = fechaVencimiento;
        this.pagada = false;
        this.fechaPago = null;
    }
}
```

**Conceptos clave:**
- `@JsonIgnore` en `prestamo` → Evita Prestamo → Cuota → Prestamo loop
- `fechaPago` nullable → `null` si aún no se pagó
- `pagada` Boolean → Estado de pago

---

### **PASO 7: Crear ClienteRepository (Repository Pattern)**

**Archivo:** `src/main/java/pe/banco/prestamos/repository/ClienteRepository.java`

```java
package pe.banco.prestamos.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import pe.banco.prestamos.model.Cliente;

import java.util.Optional;

@ApplicationScoped
public class ClienteRepository implements PanacheRepository<Cliente> {
    
    public Optional<Cliente> findByDni(String dni) {
        return find("dni", dni).firstResultOptional();
    }
    
    public Optional<Cliente> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }
    
    public boolean existsByDni(String dni) {
        return count("dni", dni) > 0;
    }
    
    public boolean existsByEmail(String email) {
        return count("email", email) > 0;
    }
}
```

**Conceptos clave:**
- `implements PanacheRepository<Cliente>` → Repository Pattern
- `@ApplicationScoped` → Singleton CDI
- Métodos custom de búsqueda
- `Optional<T>` → Manejo moderno de null

---

### **PASO 8: Crear ClienteResource (REST)**

**Archivo:** `src/main/java/pe/banco/prestamos/resource/ClienteResource.java`

```java
package pe.banco.prestamos.resource;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pe.banco.prestamos.model.Cliente;
import pe.banco.prestamos.repository.ClienteRepository;

import java.util.List;

@Path("/clientes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClienteResource {
    
    @Inject
    ClienteRepository clienteRepository;
    
    @GET
    public List<Cliente> listar() {
        return clienteRepository.listAll();
    }
    
    @GET
    @Path("/{id}")
    public Response obtener(@PathParam("id") Long id) {
        return clienteRepository.findByIdOptional(id)
                .map(cliente -> Response.ok(cliente).build())
                .orElse(Response.status(404).entity("Cliente no encontrado").build());
    }
    
    @POST
    @Transactional
    public Response crear(Cliente cliente) {
        if (clienteRepository.existsByDni(cliente.dni)) {
            return Response.status(409).entity("DNI ya registrado").build();
        }
        
        if (clienteRepository.existsByEmail(cliente.email)) {
            return Response.status(409).entity("Email ya registrado").build();
        }
        
        clienteRepository.persist(cliente);
        return Response.status(201).entity(cliente).build();
    }
    
    @PUT
    @Path("/{id}")
    @Transactional
    public Response actualizar(@PathParam("id") Long id, Cliente clienteActualizado) {
        return clienteRepository.findByIdOptional(id)
                .map(cliente -> {
                    cliente.nombre = clienteActualizado.nombre;
                    cliente.telefono = clienteActualizado.telefono;
                    return Response.ok(cliente).build();
                })
                .orElse(Response.status(404).entity("Cliente no encontrado").build());
    }
    
    @DELETE
    @Path("/{id}")
    @Transactional
    public Response eliminar(@PathParam("id") Long id) {
        boolean eliminado = clienteRepository.deleteById(id);
        if (!eliminado) {
            return Response.status(404).entity("Cliente no encontrado").build();
        }
        return Response.status(204).build();
    }
}
```

**Conceptos clave:**
- `@Transactional` → Obligatorio para modificar BD
- Validación de duplicados (DNI, email)
- `Optional.map()` → Programación funcional
- HTTP 409 Conflict → Duplicados

---

### **PASO 9: Crear PrestamoResource**

**Archivo:** `src/main/java/pe/banco/prestamos/resource/PrestamoResource.java`

```java
package pe.banco.prestamos.resource;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pe.banco.prestamos.model.Cliente;
import pe.banco.prestamos.model.Cuota;
import pe.banco.prestamos.model.Prestamo;
import pe.banco.prestamos.repository.ClienteRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Path("/prestamos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PrestamoResource {
    
    @Inject
    ClienteRepository clienteRepository;
    
    @GET
    public List<Prestamo> listar() {
        return Prestamo.listAll();
    }
    
    @GET
    @Path("/{id}")
    public Response obtener(@PathParam("id") Long id) {
        return Prestamo.findByIdOptional(id)
                .map(prestamo -> Response.ok(prestamo).build())
                .orElse(Response.status(404).entity("Préstamo no encontrado").build());
    }
    
    @POST
    @Transactional
    public Response crear(PrestamoRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId);
        if (cliente == null) {
            return Response.status(404).entity("Cliente no encontrado").build();
        }
        
        Prestamo prestamo = new Prestamo(
            cliente,
            request.monto,
            request.plazoMeses,
            request.tasaInteres,
            LocalDate.now()
        );
        
        prestamo.cuotas = generarCuotas(prestamo);
        prestamo.persist();
        
        return Response.status(201).entity(prestamo).build();
    }
    
    @PUT
    @Path("/{id}/pagar-cuota/{numeroCuota}")
    @Transactional
    public Response pagarCuota(@PathParam("id") Long prestamoId, 
                               @PathParam("numeroCuota") Integer numeroCuota) {
        Prestamo prestamo = Prestamo.findById(prestamoId);
        if (prestamo == null) {
            return Response.status(404).entity("Préstamo no encontrado").build();
        }
        
        Cuota cuota = prestamo.cuotas.stream()
                .filter(c -> c.numeroCuota.equals(numeroCuota))
                .findFirst()
                .orElse(null);
        
        if (cuota == null) {
            return Response.status(404).entity("Cuota no encontrada").build();
        }
        
        if (cuota.pagada) {
            return Response.status(409).entity("Cuota ya pagada").build();
        }
        
        cuota.pagada = true;
        cuota.fechaPago = LocalDate.now();
        
        boolean todasPagadas = prestamo.cuotas.stream().allMatch(c -> c.pagada);
        if (todasPagadas) {
            prestamo.estado = Prestamo.EstadoPrestamo.PAGADO;
        }
        
        return Response.ok(cuota).build();
    }
    
    @GET
    @Path("/cliente/{clienteId}")
    public Response listarPorCliente(@PathParam("clienteId") Long clienteId) {
        List<Prestamo> prestamos = Prestamo.find("cliente.id", clienteId).list();
        return Response.ok(prestamos).build();
    }
    
    private List<Cuota> generarCuotas(Prestamo prestamo) {
        List<Cuota> cuotas = new ArrayList<>();
        BigDecimal montoCuota = calcularMontoCuota(prestamo.monto, 
                                                    prestamo.tasaInteres, 
                                                    prestamo.plazoMeses);
        
        for (int i = 1; i <= prestamo.plazoMeses; i++) {
            LocalDate fechaVencimiento = prestamo.fechaDesembolso.plusMonths(i);
            Cuota cuota = new Cuota(prestamo, i, montoCuota, fechaVencimiento);
            cuotas.add(cuota);
        }
        
        return cuotas;
    }
    
    private BigDecimal calcularMontoCuota(BigDecimal monto, BigDecimal tasaInteres, 
                                          Integer plazoMeses) {
        BigDecimal tasaMensual = tasaInteres.divide(
            BigDecimal.valueOf(100 * 12), 6, BigDecimal.ROUND_HALF_UP);
        
        BigDecimal factor = BigDecimal.ONE.add(
            tasaMensual.multiply(BigDecimal.valueOf(plazoMeses)));
        
        return monto.multiply(factor).divide(
            BigDecimal.valueOf(plazoMeses), 2, BigDecimal.ROUND_HALF_UP);
    }
    
    public static class PrestamoRequest {
        public Long clienteId;
        public BigDecimal monto;
        public Integer plazoMeses;
        public BigDecimal tasaInteres;
    }
}
```

**Conceptos clave:**
- `Prestamo.listAll()` → Active Record
- Generación automática de cuotas
- Cambio de estado a PAGADO cuando todas las cuotas están pagadas
- DTO `PrestamoRequest` para input

---

### **PASO 10: Compilar y Ejecutar**

**1. Compilar:**

**macOS/Linux:**
```bash
./mvnw clean compile
```

**Windows:**
```cmd
mvnw.cmd clean compile
```

**2. Ejecutar en modo dev:**

**macOS/Linux:**
```bash
./mvnw quarkus:dev
```

**Windows:**
```cmd
mvnw.cmd quarkus:dev
```

**3. Verificar:**

Deberías ver:
```
INFO  [io.quarkus] prestamos-service 1.0.0-SNAPSHOT on JVM started in X.XXXs
INFO  [io.quarkus] Listening on: http://localhost:8080
```

Y las queries SQL creando tablas:
```sql
CREATE TABLE clientes (...)
CREATE TABLE prestamos (...)
CREATE TABLE cuotas (...)
```

---

## 🧪 Pruebas con Swagger UI

### URLs Importantes

| Recurso | URL |
|---------|-----|
| **Swagger UI** | http://localhost:8080/q/swagger-ui |
| **OpenAPI Spec** | http://localhost:8080/q/openapi |
| **Dev UI** | http://localhost:8080/q/dev |
| **Health Check** | http://localhost:8080/q/health |

---

### **PRUEBA 1: Crear Clientes**

Abre Swagger UI: http://localhost:8080/q/swagger-ui

1. Expande **POST /clientes**
2. Click **"Try it out"**
3. Request body:

**Cliente 1:**
```json
{
  "nombre": "María González",
  "dni": "12345678",
  "email": "maria@example.com",
  "telefono": "987654321"
}
```

4. Click **"Execute"**
5. **Respuesta esperada:** `201 Created`

**Cliente 2:**
```json
{
  "nombre": "Carlos Ruiz",
  "dni": "87654321",
  "email": "carlos@example.com",
  "telefono": "912345678"
}
```

---

### **PRUEBA 2: Listar Clientes**

1. Expande **GET /clientes**
2. Click **"Try it out"** → **"Execute"**

**Respuesta esperada:**
```json
[
  {
    "id": 1,
    "nombre": "María González",
    "dni": "12345678",
    "email": "maria@example.com",
    "telefono": "987654321"
  },
  {
    "id": 2,
    "nombre": "Carlos Ruiz",
    "dni": "87654321",
    "email": "carlos@example.com",
    "telefono": "912345678"
  }
]
```

---

### **PRUEBA 3: Crear Préstamo**

1. Expande **POST /prestamos**
2. Click **"Try it out"**
3. Request body:

```json
{
  "clienteId": 1,
  "monto": 10000.00,
  "plazoMeses": 12,
  "tasaInteres": 15.50
}
```

4. Click **"Execute"**

**Respuesta esperada:** `201 Created` con:
- Préstamo creado
- Estado: `ACTIVO`
- **12 cuotas generadas automáticamente**
- Fechas de vencimiento mensuales

**Ejemplo de respuesta:**
```json
{
  "id": 1,
  "cliente": {
    "id": 1,
    "nombre": "María González",
    "dni": "12345678",
    "email": "maria@example.com",
    "telefono": "987654321"
  },
  "monto": 10000.00,
  "plazoMeses": 12,
  "tasaInteres": 15.50,
  "fechaDesembolso": "2025-10-12",
  "estado": "ACTIVO",
  "cuotas": [
    {
      "id": 1,
      "numeroCuota": 1,
      "monto": 962.50,
      "fechaVencimiento": "2025-11-12",
      "fechaPago": null,
      "pagada": false
    },
    {
      "id": 2,
      "numeroCuota": 2,
      "monto": 962.50,
      "fechaVencimiento": "2025-12-12",
      "fechaPago": null,
      "pagada": false
    }
    // ... 10 cuotas más
  ]
}
```

---

### **PRUEBA 4: Pagar Cuota**

1. Expande **PUT /prestamos/{id}/pagar-cuota/{numeroCuota}**
2. Click **"Try it out"**
3. Parámetros:
   - `id` = **1** (préstamo)
   - `numeroCuota` = **1** (primera cuota)
4. Click **"Execute"**

**Respuesta esperada:**
```json
{
  "id": 1,
  "numeroCuota": 1,
  "monto": 962.50,
  "fechaVencimiento": "2025-11-12",
  "fechaPago": "2025-10-12",
  "pagada": true
}
```

**Paga todas las cuotas (2-12)** para ver el cambio de estado a `PAGADO`.

---

### **PRUEBA 5: Listar Préstamos de un Cliente**

1. Expande **GET /prestamos/cliente/{clienteId}**
2. Click **"Try it out"**
3. `clienteId` = **1**
4. Click **"Execute"**

**Respuesta esperada:** Array con préstamos del cliente, incluyendo cuotas.

---

### **PRUEBA 6: Validación de Duplicados**

Intenta crear cliente con DNI duplicado:

**POST /clientes:**
```json
{
  "nombre": "Juan Pérez",
  "dni": "12345678",
  "email": "juan@example.com",
  "telefono": "999888777"
}
```

**Respuesta esperada:** `409 Conflict` - "DNI ya registrado"

---

## 📊 Pruebas con curl

### Crear Cliente
```bash
curl -X POST http://localhost:8080/clientes \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Ana Torres",
    "dni": "11223344",
    "email": "ana@example.com",
    "telefono": "955444333"
  }'
```

### Listar Clientes
```bash
curl http://localhost:8080/clientes
```

### Crear Préstamo
```bash
curl -X POST http://localhost:8080/prestamos \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": 1,
    "monto": 5000.00,
    "plazoMeses": 6,
    "tasaInteres": 12.00
  }'
```

### Pagar Cuota
```bash
curl -X PUT http://localhost:8080/prestamos/1/pagar-cuota/1
```

### Listar Préstamos de Cliente
```bash
curl http://localhost:8080/prestamos/cliente/1
```

---

## 🔍 Estructura Final del Proyecto

```
prestamos-service/
├── mvnw
├── mvnw.cmd
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── pe/banco/prestamos/
│   │   │       ├── model/
│   │   │       │   ├── Cliente.java
│   │   │       │   ├── Prestamo.java
│   │   │       │   └── Cuota.java
│   │   │       ├── repository/
│   │   │       │   └── ClienteRepository.java
│   │   │       └── resource/
│   │   │           ├── ClienteResource.java
│   │   │           └── PrestamoResource.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/
└── target/
```

---

## 🎯 Conceptos Cubiertos

### 1. **Hibernate ORM con Panache**

**Active Record Pattern:**
```java
@Entity
public class Prestamo extends PanacheEntity {
    // Métodos de persistencia heredados
    public static List<Prestamo> findAll() { ... }
    public void persist() { ... }
}
```

**Repository Pattern:**
```java
@ApplicationScoped
public class ClienteRepository implements PanacheRepository<Cliente> {
    // Separación de lógica de acceso a datos
}
```

**Comparación:**

| Aspecto | Active Record | Repository |
|---------|---------------|------------|
| **Ubicación lógica** | En la entidad | Clase separada |
| **Uso** | `Prestamo.listAll()` | `repository.listAll()` |
| **Testing** | Más difícil (mockear static) | Más fácil (inyectar mock) |
| **Convención** | Simple, directo | Separación de responsabilidades |

### 2. **Relaciones JPA**

**@OneToMany (Cliente → Préstamos):**
```java
@OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
public List<Prestamo> prestamos;
```

**@ManyToOne (Préstamo → Cliente):**
```java
@ManyToOne(optional = false)
@JoinColumn(name = "cliente_id")
public Cliente cliente;
```

**Cascade Types:**
- `ALL` → Propaga todas las operaciones
- `PERSIST` → Solo insert
- `MERGE` → Solo update
- `REMOVE` → Solo delete
- `orphanRemoval = true` → Elimina huérfanos

### 3. **Transacciones**

```java
@Transactional  // Obligatorio para modificar BD
public Response crear(Cliente cliente) {
    clienteRepository.persist(cliente);
    return Response.status(201).entity(cliente).build();
}
```

**Sin @Transactional:**
- Solo lectura (SELECT)
- Modificaciones fallan con error

**Con @Transactional:**
- ACID garantizado
- Rollback automático en excepción

### 4. **Lazy Loading y @JsonIgnore**

**Problema:**
```java
// Cliente → Prestamos (lazy) → Cliente (lazy) → loop infinito
```

**Solución:**
```java
@JsonIgnore
@OneToMany(...)
public List<Prestamo> prestamos;
```

Evita serializar colecciones que causan loops.

### 5. **BigDecimal para Dinero**

```java
@Column(precision = 12, scale = 2)
public BigDecimal monto;  // ✅ Correcto

public double monto;      // ❌ Nunca usar
```

**Por qué BigDecimal:**
- Precisión exacta (no redondeo)
- Estándar en finanzas
- Evita bugs: `0.1 + 0.2 = 0.30000000000000004`

### 6. **Generación Automática de Cuotas**

```java
private List<Cuota> generarCuotas(Prestamo prestamo) {
    List<Cuota> cuotas = new ArrayList<>();
    BigDecimal montoCuota = calcularMontoCuota(...);
    
    for (int i = 1; i <= prestamo.plazoMeses; i++) {
        LocalDate fechaVencimiento = prestamo.fechaDesembolso.plusMonths(i);
        Cuota cuota = new Cuota(prestamo, i, montoCuota, fechaVencimiento);
        cuotas.add(cuota);
    }
    
    return cuotas;
}
```

Lógica de negocio compleja encapsulada.

---

## 🛠️ Solución de Problemas

### Error: "role postgres does not exist"

**Causa:** Usuario de PostgreSQL incorrecto

**Solución 1 (Cambiar usuario):**
```properties
quarkus.datasource.username=TU_USUARIO
quarkus.datasource.password=TU_PASSWORD
```

**Solución 2 (Usar H2 en memoria):**

En `pom.xml`, reemplaza:
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>jdbc-postgresql</artifactId>
</dependency>
```

Por:
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>jdbc-h2</artifactId>
</dependency>
```

En `application.properties`:
```properties
quarkus.datasource.db-kind=h2
quarkus.datasource.username=sa
quarkus.datasource.password=
quarkus.datasource.jdbc.url=jdbc:h2:mem:prestamos_db
```

### Error: "LazyInitializationException"

**Causa:** Intentar acceder a colección lazy fuera de sesión Hibernate

**Solución:** Agregar `@JsonIgnore` en la relación inversa:
```java
@JsonIgnore
@OneToMany(mappedBy = "cliente")
public List<Prestamo> prestamos;
```

### Datos se borran al reiniciar

**Causa:** `database.generation=drop-and-create`

**Solución:** Cambiar a:
```properties
quarkus.hibernate-orm.database.generation=update
```

### Error: "No transaction is currently active"

**Causa:** Falta `@Transactional` en método que modifica BD

**Solución:**
```java
@POST
@Transactional  // ← Agregar
public Response crear(...) { ... }
```

---

## 🔐 Plan B: H2 Database (Sin PostgreSQL)

Si tienes problemas con PostgreSQL, usa H2:

**1. Cambiar dependencia en `pom.xml`:**
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>jdbc-h2</artifactId>
</dependency>
```

**2. Actualizar `application.properties`:**
```properties
quarkus.datasource.db-kind=h2
quarkus.datasource.username=sa
quarkus.datasource.password=
quarkus.datasource.jdbc.url=jdbc:h2:mem:prestamos_db

# H2 Console (opcional)
quarkus.datasource.jdbc.url=jdbc:h2:mem:prestamos_db;DB_CLOSE_DELAY=-1
quarkus.h2.console.enabled=true
```

**3. Acceder a H2 Console:**
- URL: http://localhost:8080/q/h2-console
- JDBC URL: `jdbc:h2:mem:prestamos_db`
- Usuario: `sa`
- Password: (vacío)

**Ventajas H2:**
- ✅ Cero configuración
- ✅ En memoria (rápido)
- ✅ Perfecto para desarrollo/demos

**Desventajas H2:**
- ❌ Datos volátiles (se pierden al apagar)
- ❌ No para producción

---

## 📚 Recursos Adicionales

### Documentación

- [Quarkus Hibernate ORM with Panache](https://quarkus.io/guides/hibernate-orm-panache)
- [Quarkus Datasources](https://quarkus.io/guides/datasource)
- [JPA Relationships](https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1.html)
- [Panache Entity vs Repository](https://quarkus.io/guides/hibernate-orm-panache#solution-1-using-the-active-record-pattern)

### Siguientes Pasos

Después de dominar este capítulo:
1. **Capítulo 5:** Bean Validation (`@NotNull`, `@Size`, `@Min`)
2. **Capítulo 6:** Exception Handling centralizado
3. **Capítulo 7:** Testing con RestAssured + TestContainers
4. **Capítulo 8:** Seguridad (JWT, RBAC)
5. **Capítulo 9:** Reactive Programming (Mutiny)

---

## ✅ Checklist de Aprendizaje

Después de completar este ejercicio, deberías poder:

- [ ] Configurar Hibernate ORM con Panache
- [ ] Crear entidades JPA con anotaciones
- [ ] Implementar Active Record Pattern
- [ ] Implementar Repository Pattern
- [ ] Configurar relaciones @OneToMany y @ManyToOne
- [ ] Usar @Transactional correctamente
- [ ] Evitar LazyInitializationException con @JsonIgnore
- [ ] Trabajar con BigDecimal para dinero
- [ ] Generar datos relacionados automáticamente
- [ ] Configurar PostgreSQL (o H2 alternativa)
- [ ] Probar endpoints con Swagger UI
- [ ] Usar Optional para manejo de null

---

## 🎉 Comparación con Capítulo 3

### Capítulo 3 (Memoria)
```java
@ApplicationScoped
public class CuentaService {
    private Map<String, Cuenta> cuentas = new ConcurrentHashMap<>();
    
    public Cuenta crear(Cuenta cuenta) {
        cuentas.put(cuenta.getNumero(), cuenta);
        return cuenta;
    }
}
```

### Capítulo 4 (Persistencia)
```java
@ApplicationScoped
public class ClienteRepository implements PanacheRepository<Cliente> {
    
    @Transactional
    public Cliente crear(Cliente cliente) {
        persist(cliente);  // Se guarda en PostgreSQL
        return cliente;
    }
}
```

**Evolución:**
- ❌ Map en memoria → ✅ Base de datos real
- ❌ Datos volátiles → ✅ Persistencia permanente
- ❌ Sin relaciones → ✅ Foreign keys y joins
- ❌ Queries manuales → ✅ JPA/HQL automático

---

**🎉