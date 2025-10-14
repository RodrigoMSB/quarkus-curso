# 📚 TEORIA.md - Capítulo 4: Persistencia con Hibernate ORM y Panache

Fundamentos completos de persistencia de datos, JPA, Hibernate, Panache y patrones de diseño para acceso a datos.

---

## 📖 Índice

1. [¿Qué es la Persistencia?](#1-qué-es-la-persistencia)
2. [JPA: Java Persistence API](#2-jpa-java-persistence-api)
3. [Hibernate ORM](#3-hibernate-orm)
4. [Panache: JPA Simplificado](#4-panache-jpa-simplificado)
5. [Active Record vs Repository Pattern](#5-active-record-vs-repository-pattern)
6. [Entidades y Mapeo ORM](#6-entidades-y-mapeo-orm)
7. [Relaciones entre Entidades](#7-relaciones-entre-entidades)
8. [Transacciones y ACID](#8-transacciones-y-acid)
9. [Lazy Loading vs Eager Loading](#9-lazy-loading-vs-eager-loading)
10. [Queries con Panache](#10-queries-con-panache)

---

## 1. ¿Qué es la Persistencia?

### 1.1 Definición

**Persistencia** es la capacidad de guardar datos de forma permanente, sobreviviendo más allá del ciclo de vida de la aplicación.

### 1.2 Tipos de Persistencia

#### **Volatilidad (Sin persistencia)**

```java
// Datos en memoria - se pierden al cerrar app
Map<String, Cliente> clientes = new HashMap<>();
clientes.put("123", cliente);
// ❌ Al reiniciar: datos perdidos
```

**Características:**
- ❌ Datos se pierden al reiniciar
- ✅ Muy rápido (RAM)
- ✅ Simple de implementar
- 🎯 Uso: Cachés, datos temporales

#### **Persistencia en Archivos**

```java
// Guardar en archivo
FileWriter writer = new FileWriter("clientes.json");
writer.write(toJson(clientes));
// ✅ Al reiniciar: datos recuperables
```

**Características:**
- ✅ Datos sobreviven reinicio
- ⚠️ No hay estructura relacional
- ⚠️ Difícil hacer queries complejas
- 🎯 Uso: Configuraciones, logs

#### **Persistencia en Base de Datos**

```java
// Guardar en PostgreSQL
@Entity
public class Cliente extends PanacheEntity {
    // Hibernate maneja SQL automáticamente
}
```

**Características:**
- ✅ Datos permanentes y estructurados
- ✅ Queries SQL potentes
- ✅ Transacciones ACID
- ✅ Relaciones y restricciones
- 🎯 Uso: Aplicaciones productivas

### 1.3 Analogía

**Sin persistencia** = Pizarra blanca
- Escribes información temporal
- Se borra fácilmente
- No queda registro

**Con persistencia** = Libro de contabilidad
- Registro permanente
- Consultable en cualquier momento
- Trazabilidad completa

---

## 2. JPA: Java Persistence API

### 2.1 ¿Qué es JPA?

**JPA** (Jakarta Persistence API, antes Java Persistence API) es la **especificación estándar** de Java para mapear objetos Java a bases de datos relacionales.

**JPA NO es:**
- ❌ Una librería
- ❌ Una implementación
- ❌ Un framework

**JPA ES:**
- ✅ Una especificación (conjunto de interfaces y reglas)
- ✅ Un estándar de Jakarta EE
- ✅ Un contrato que implementan frameworks

### 2.2 Arquitectura JPA

```
┌─────────────────────────────────┐
│   Aplicación Java               │
│   (Usa anotaciones JPA)         │
└─────────────────┬───────────────┘
                  │
                  ▼
┌─────────────────────────────────┐
│   JPA Specification             │
│   (@Entity, @Id, etc)           │
└─────────────────┬───────────────┘
                  │
                  ▼
┌─────────────────────────────────┐
│   Implementación JPA            │
│   (Hibernate, EclipseLink)      │
└─────────────────┬───────────────┘
                  │
                  ▼
┌─────────────────────────────────┐
│   JDBC Driver                   │
└─────────────────┬───────────────┘
                  │
                  ▼
┌─────────────────────────────────┐
│   Base de Datos                 │
│   (PostgreSQL, MySQL, etc)      │
└─────────────────────────────────┘
```

### 2.3 Implementaciones de JPA

| Implementación | Características | Uso |
|---------------|-----------------|-----|
| **Hibernate** | Más popular, feature-rich | Producción, Quarkus default |
| **EclipseLink** | Implementación de referencia | Jakarta EE servers |
| **OpenJPA** | Apache, menos usado | Legacy systems |

### 2.4 Ventajas de JPA

✅ **Portabilidad:** Cambiar de DB sin cambiar código  
✅ **Productividad:** No escribir SQL manualmente  
✅ **Orientado a Objetos:** Trabajar con POJOs  
✅ **Estándar:** Conocimiento transferible  
✅ **Cachés:** Optimización automática  

### 2.5 Ejemplo JPA Básico

**Sin JPA (JDBC puro):**
```java
// Manual, verboso, propenso a errores
String sql = "INSERT INTO clientes (nombre, dni) VALUES (?, ?)";
PreparedStatement stmt = conn.prepareStatement(sql);
stmt.setString(1, cliente.getNombre());
stmt.setString(2, cliente.getDni());
stmt.executeUpdate();
```

**Con JPA:**
```java
// Simple, automático, seguro
@Entity
public class Cliente {
    @Id
    private Long id;
    private String nombre;
    private String dni;
}

entityManager.persist(cliente); // ✅ Hibernate genera SQL
```

---

## 3. Hibernate ORM

### 3.1 ¿Qué es Hibernate?

**Hibernate ORM** es la **implementación de JPA más popular**, que mapea objetos Java (clases) a tablas relacionales (SQL).

**ORM** = Object-Relational Mapping

### 3.2 Problema que Resuelve

#### **Impedancia Objeto-Relacional**

```
Mundo Java (POO)          Mundo SQL (Relacional)
─────────────────────     ─────────────────────
Objetos                   Tablas
Herencia                  No hay herencia
Referencias               Foreign Keys
Colecciones (List)        JOIN queries
Navegación (obj.getX())   SQL SELECT
```

**Hibernate** es el puente entre ambos mundos.

### 3.3 Cómo Funciona Hibernate

```java
// 1. Defines entidad
@Entity
public class Cliente {
    @Id
    private Long id;
    private String nombre;
}

// 2. Hibernate genera SQL automáticamente
Cliente c = new Cliente();
c.setNombre("Juan");
entityManager.persist(c);

// 3. Hibernate ejecuta:
// INSERT INTO cliente (nombre) VALUES ('Juan');
```

**Flujo interno:**
1. Analiza anotaciones (`@Entity`, `@Id`)
2. Construye metadata del esquema
3. Genera SQL dinámicamente
4. Ejecuta via JDBC
5. Mapea ResultSet → Objetos

### 3.4 SessionFactory y EntityManager

#### **SessionFactory (Hibernate nativo)**
```java
SessionFactory sf = new Configuration().buildSessionFactory();
Session session = sf.openSession();
session.save(cliente);
```

#### **EntityManager (JPA estándar)**
```java
@PersistenceContext
EntityManager em;

em.persist(cliente);  // JPA API
```

**En Quarkus con Panache:**
- Se abstrae completamente
- No necesitas `EntityManager` directo
- Todo es más simple

### 3.5 Estados de una Entidad Hibernate

```
┌─────────────┐
│  TRANSIENT  │  Objeto nuevo, no conocido por Hibernate
└──────┬──────┘
       │ persist()
       ▼
┌─────────────┐
│  PERSISTENT │  Hibernate lo rastrea, sincroniza con DB
└──────┬──────┘
       │ detach()
       ▼
┌─────────────┐
│  DETACHED   │  Ya no rastreado, pero tiene ID de DB
└──────┬──────┘
       │ merge()
       ▼
┌─────────────┐
│  PERSISTENT │
└─────────────┘
       │ remove()
       ▼
┌─────────────┐
│   REMOVED   │  Marcado para eliminación
└─────────────┘
```

**Ejemplo:**
```java
Cliente c = new Cliente();        // TRANSIENT
em.persist(c);                    // PERSISTENT
em.detach(c);                     // DETACHED
Cliente c2 = em.merge(c);         // c2 es PERSISTENT
em.remove(c2);                    // REMOVED
```

---

## 4. Panache: JPA Simplificado

### 4.1 ¿Qué es Panache?

**Panache** es una extensión de Quarkus que **simplifica Hibernate**, eliminando boilerplate y haciendo JPA más intuitivo.

**Creado por:** Red Hat (equipo Quarkus)  
**Inspirado en:** Spring Data JPA, Active Record de Ruby on Rails

### 4.2 JPA Tradicional vs Panache

#### **JPA Tradicional (verbose):**
```java
@Entity
public class Cliente {
    @Id
    @GeneratedValue
    private Long id;
    
    // Getters y setters (30+ líneas)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    // ...
}

@ApplicationScoped
public class ClienteRepository {
    
    @PersistenceContext
    EntityManager em;
    
    @Transactional
    public void persist(Cliente c) {
        em.persist(c);
    }
    
    public Cliente findById(Long id) {
        return em.find(Cliente.class, id);
    }
    
    public List<Cliente> listAll() {
        return em.createQuery("SELECT c FROM Cliente c", Cliente.class)
                 .getResultList();
    }
}
```

#### **Panache (conciso):**
```java
@Entity
public class Cliente extends PanacheEntity {
    public String nombre;  // Campos públicos
    public String dni;
    
    // ID ya incluido automáticamente
    // Métodos de persistencia heredados
}

// Usar directamente:
Cliente.persist(cliente);
Cliente c = Cliente.findById(1L);
List<Cliente> todos = Cliente.listAll();
```

**Diferencia:**
- ❌ JPA: 80+ líneas
- ✅ Panache: 10 líneas

### 4.3 Características de Panache

✅ **Campos públicos** → Panache genera getters/setters en bytecode  
✅ **ID automático** → `PanacheEntity` incluye `public Long id`  
✅ **Métodos estáticos** → `Cliente.persist()`, `Cliente.findById()`  
✅ **Queries simplificadas** → `find("nombre", "Juan")`  
✅ **Sin EntityManager** → Todo abstraído  

### 4.4 PanacheEntity vs PanacheEntityBase

#### **PanacheEntity (con ID Long)**
```java
@Entity
public class Cliente extends PanacheEntity {
    // Hereda: public Long id;
}
```

#### **PanacheEntityBase (ID custom)**
```java
@Entity
public class Cliente extends PanacheEntityBase {
    @Id
    public String dni;  // String como PK
}
```

### 4.5 Panache y Active Record

Panache implementa **Active Record Pattern**:

```java
// Entidad = Datos + Lógica de persistencia
Cliente cliente = new Cliente();
cliente.nombre = "Juan";
cliente.persist();  // Se guarda a sí misma

Cliente c = Cliente.findById(1L);
c.delete();  // Se elimina a sí misma
```

**Inspiración:** Ruby on Rails ActiveRecord

---

## 5. Active Record vs Repository Pattern

### 5.1 Active Record Pattern

**Definición:** La entidad contiene tanto **datos** como **lógica de persistencia**.

#### **Implementación en Panache:**
```java
@Entity
public class Prestamo extends PanacheEntity {
    public BigDecimal monto;
    public Integer plazoMeses;
    
    // Métodos de persistencia EN LA ENTIDAD
    public static List<Prestamo> findActivos() {
        return find("estado", EstadoPrestamo.ACTIVO).list();
    }
    
    public void aprobar() {
        this.estado = EstadoPrestamo.APROBADO;
        this.persist();  // Se guarda a sí misma
    }
}

// Uso directo
Prestamo p = new Prestamo();
p.monto = new BigDecimal("10000");
p.persist();  // ✅ Entidad se persiste sola

List<Prestamo> activos = Prestamo.findActivos();
```

**Ventajas:**
- ✅ Simple e intuitivo
- ✅ Menos clases (no necesitas Repository)
- ✅ Código conciso
- ✅ Ideal para CRUD simple

**Desventajas:**
- ❌ Entidad sabe de persistencia (rompe SRP - Single Responsibility)
- ❌ Difícil de testear (mockear métodos estáticos)
- ❌ Acoplamiento a framework

### 5.2 Repository Pattern

**Definición:** **Clase separada** maneja la lógica de persistencia, la entidad solo tiene datos.

#### **Implementación en Panache:**
```java
// Entidad: solo datos (POJO puro)
@Entity
public class Cliente extends PanacheEntity {
    public String nombre;
    public String dni;
    public String email;
    
    // Sin métodos de persistencia
}

// Repository: lógica de acceso a datos
@ApplicationScoped
public class ClienteRepository implements PanacheRepository<Cliente> {
    
    public Optional<Cliente> findByDni(String dni) {
        return find("dni", dni).firstResultOptional();
    }
    
    public boolean existsByEmail(String email) {
        return count("email", email) > 0;
    }
    
    public List<Cliente> findActivos() {
        return find("activo", true).list();
    }
}

// Uso con inyección
@Inject
ClienteRepository repository;

Cliente c = new Cliente();
repository.persist(c);  // ✅ Repository maneja persistencia

Optional<Cliente> opt = repository.findByDni("12345678");
```

**Ventajas:**
- ✅ Separación de responsabilidades (entidad = datos, repo = persistencia)
- ✅ Fácil de testear (inyectar mock del repository)
- ✅ Queries complejas organizadas
- ✅ Reutilizable

**Desventajas:**
- ❌ Más clases (una por entidad)
- ❌ Ligeramente más verboso

### 5.3 Comparación Completa

| Aspecto | Active Record | Repository |
|---------|---------------|------------|
| **Ubicación lógica** | En la entidad | Clase separada |
| **Ejemplo uso** | `Prestamo.persist()` | `repository.persist(prestamo)` |
| **Métodos** | Estáticos | De instancia |
| **Testing** | Difícil (static mock) | Fácil (DI mock) |
| **SRP** | ❌ Viola | ✅ Cumple |
| **Complejidad** | Simple | Moderada |
| **Queries custom** | En entidad | En repository |
| **DDD** | ❌ No recomendado | ✅ Recomendado |

### 5.4 ¿Cuándo Usar Cada Uno?

#### **Active Record (PanacheEntity):**
```java
@Entity
public class Cuota extends PanacheEntity {
    public Integer numero;
    public BigDecimal monto;
}
```

**Usar cuando:**
- ✅ CRUD simple
- ✅ Aplicación pequeña/demo
- ✅ No hay lógica compleja
- ✅ Prototipado rápido

#### **Repository (PanacheRepository):**
```java
@ApplicationScoped
public class ClienteRepository implements PanacheRepository<Cliente> {
    // Queries complejas aquí
}
```

**Usar cuando:**
- ✅ Lógica de acceso a datos compleja
- ✅ Testing importante
- ✅ DDD (Domain-Driven Design)
- ✅ Aplicación grande/productiva
- ✅ Múltiples desarrolladores

### 5.5 Analogía

**Active Record** = Chef que cocina y sirve su propia comida
- Todo en uno
- Rápido para comidas simples
- Difícil escalar a restaurante grande

**Repository** = Restaurante con chef y meseros separados
- Responsabilidades divididas
- Más organizado
- Escala mejor

---

## 6. Entidades y Mapeo ORM

### 6.1 ¿Qué es una Entidad?

Una **entidad** es una clase Java que se mapea a una tabla de base de datos.

```java
@Entity                          // Marca como entidad JPA
@Table(name = "clientes")        // Nombre de tabla (opcional)
public class Cliente extends PanacheEntity {
    
    @Column(nullable = false)    // Mapeo de columna
    public String nombre;
    
    @Column(unique = true, length = 8)
    public String dni;
}
```

**Mapeo:**
```
Clase Java          →    Tabla SQL
─────────────────        ──────────────────
Cliente             →    clientes
  id                →      id (PK)
  nombre            →      nombre
  dni               →      dni (UNIQUE)
```

### 6.2 Anotaciones de Entidad

#### **@Entity**
```java
@Entity  // JPA reconoce esta clase como entidad
public class Prestamo { }
```

#### **@Table**
```java
@Table(
    name = "prestamos",              // Nombre de tabla
    schema = "finanzas",             // Esquema (opcional)
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"cliente_id", "numero"})
    }
)
public class Prestamo { }
```

#### **@Id**
```java
@Entity
public class Cliente extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
}
```

**PanacheEntity ya incluye:**
```java
public abstract class PanacheEntity {
    @Id
    @GeneratedValue
    public Long id;
}
```

#### **@Column**
```java
@Column(
    name = "email_address",      // Nombre columna diferente
    nullable = false,            // NOT NULL
    unique = true,               // UNIQUE
    length = 100,                // VARCHAR(100)
    precision = 10,              // Para números (total dígitos)
    scale = 2                    // Para números (decimales)
)
public String email;
```

### 6.3 Tipos de Datos

#### **Mapeo Automático:**
```java
public String nombre;           →  VARCHAR(255)
public Integer edad;            →  INTEGER
public Long id;                 →  BIGINT
public Boolean activo;          →  BOOLEAN
public LocalDate fechaNac;      →  DATE
public LocalDateTime creado;    →  TIMESTAMP
public BigDecimal saldo;        →  NUMERIC(precision, scale)
```

#### **Enums:**
```java
public enum Estado { ACTIVO, INACTIVO, BLOQUEADO }

@Enumerated(EnumType.STRING)    // Guarda "ACTIVO"
public Estado estado;

@Enumerated(EnumType.ORDINAL)   // Guarda 0, 1, 2 (no recomendado)
public Estado estado;
```

**⚠️ Siempre usa `EnumType.STRING`:**
- ✅ Legible en DB
- ✅ Robusto a cambios de orden
- ❌ ORDINAL es frágil

### 6.4 Estrategias de Generación de ID

```java
// Identidad (Auto-increment en DB)
@GeneratedValue(strategy = GenerationType.IDENTITY)
public Long id;

// Secuencia (PostgreSQL, Oracle)
@GeneratedValue(strategy = GenerationType.SEQUENCE)
public Long id;

// Tabla (universal pero más lento)
@GeneratedValue(strategy = GenerationType.TABLE)
public Long id;

// UUID
@GeneratedValue(generator = "UUID")
public UUID id;
```

**PanacheEntity usa AUTO:**
```java
@GeneratedValue  // Elige la mejor según DB
```

### 6.5 Campos Públicos en Panache

**JPA Tradicional:**
```java
private String nombre;

public String getNombre() {
    return nombre;
}

public void setNombre(String nombre) {
    this.nombre = nombre;
}
```

**Panache:**
```java
public String nombre;  // ✅ Panache genera getters/setters en bytecode
```

**¿Cómo funciona?**
- Panache intercepta acceso a campos públicos
- Genera getters/setters dinámicamente
- Hibernate los usa normalmente
- **No hay diferencia en performance**

---

## 7. Relaciones entre Entidades

### 7.1 Tipos de Relaciones

```
1:1  (One-to-One)      Cliente → Cuenta
1:N  (One-to-Many)     Cliente → Préstamos
N:1  (Many-to-One)     Préstamo → Cliente
N:M  (Many-to-Many)    Estudiante ↔ Cursos
```

### 7.2 @OneToMany (1:N)

**Cliente tiene muchos Préstamos:**

```java
@Entity
public class Cliente extends PanacheEntity {
    public String nombre;
    
    @OneToMany(
        mappedBy = "cliente",           // Campo en Prestamo que apunta a Cliente
        cascade = CascadeType.ALL,      // Operaciones en cascada
        orphanRemoval = true            // Elimina huérfanos
    )
    public List<Prestamo> prestamos;
}
```

**Características:**
- `mappedBy` → Indica lado inverso (Prestamo es el dueño)
- `cascade` → Propagar operaciones
- `orphanRemoval` → Si `prestamo.cliente = null`, se elimina

### 7.3 @ManyToOne (N:1)

**Muchos Préstamos pertenecen a un Cliente:**

```java
@Entity
public class Prestamo extends PanacheEntity {
    public BigDecimal monto;
    
    @ManyToOne(optional = false)        // Cliente obligatorio
    @JoinColumn(name = "cliente_id")    // Nombre FK en DB
    public Cliente cliente;
}
```

**Genera SQL:**
```sql
CREATE TABLE prestamos (
    id BIGINT PRIMARY KEY,
    monto DECIMAL(12,2),
    cliente_id BIGINT NOT NULL,
    FOREIGN KEY (cliente_id) REFERENCES clientes(id)
);
```

### 7.4 @OneToOne (1:1)

**Usuario tiene un único Perfil:**

```java
@Entity
public class Usuario extends PanacheEntity {
    public String username;
    
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "perfil_id")
    public Perfil perfil;
}

@Entity
public class Perfil extends PanacheEntity {
    public String bio;
    
    @OneToOne(mappedBy = "perfil")
    public Usuario usuario;
}
```

### 7.5 @ManyToMany (N:M)

**Estudiantes ↔ Cursos (muchos a muchos):**

```java
@Entity
public class Estudiante extends PanacheEntity {
    public String nombre;
    
    @ManyToMany
    @JoinTable(
        name = "estudiante_curso",
        joinColumns = @JoinColumn(name = "estudiante_id"),
        inverseJoinColumns = @JoinColumn(name = "curso_id")
    )
    public List<Curso> cursos;
}

@Entity
public class Curso extends PanacheEntity {
    public String nombre;
    
    @ManyToMany(mappedBy = "cursos")
    public List<Estudiante> estudiantes;
}
```

**Genera SQL:**
```sql
CREATE TABLE estudiante_curso (
    estudiante_id BIGINT,
    curso_id BIGINT,
    PRIMARY KEY (estudiante_id, curso_id),
    FOREIGN KEY (estudiante_id) REFERENCES estudiante(id),
    FOREIGN KEY (curso_id) REFERENCES curso(id)
);
```

### 7.6 Cascade Types

```java
@OneToMany(cascade = CascadeType.???)
```

| Cascade Type | Significado | Ejemplo |
|--------------|-------------|---------|
| **ALL** | Todas las operaciones | `persist`, `merge`, `remove`, `refresh` |
| **PERSIST** | Solo insert | Guardar Cliente → guarda Préstamos |
| **MERGE** | Solo update | Actualizar Cliente → actualiza Préstamos |
| **REMOVE** | Solo delete | Eliminar Cliente → elimina Préstamos |
| **REFRESH** | Recargar desde DB | Refrescar Cliente → refresca Préstamos |
| **DETACH** | Desconectar de sesión | Detach Cliente → detach Préstamos |

**Ejemplo práctico:**
```java
@OneToMany(cascade = CascadeType.ALL)
public List<Cuota> cuotas;

// Al hacer:
prestamo.persist();
// Automáticamente persiste todas las cuotas
```

### 7.7 orphanRemoval

```java
@OneToMany(orphanRemoval = true)
public List<Cuota> cuotas;

// Si haces:
prestamo.cuotas.remove(cuota);
// Hibernate ejecuta DELETE de esa cuota
```

**Con `orphanRemoval = false`:**
- Cuota queda huérfana (FK = NULL)

**Con `orphanRemoval = true`:**
- Cuota se elimina de DB

---

## 8. Transacciones y ACID

### 8.1 ¿Qué es una Transacción?

Una **transacción** es un conjunto de operaciones que se ejecutan como **una unidad atómica**.

```java
@Transactional
public void transferir(Long origenId, Long destinoId, BigDecimal monto) {
    Cuenta origen = Cuenta.findById(origenId);
    Cuenta destino = Cuenta.findById(destinoId);
    
    origen.saldo = origen.saldo.subtract(monto);   // Op 1
    destino.saldo = destino.saldo.add(monto);      // Op 2
    
    // Si falla Op 2, Op 1 se revierte (rollback)
}
```

### 8.2 Propiedades ACID

#### **A - Atomicity (Atomicidad)**
Todo o nada. Si una operación falla, todas se revierten.

```java
@Transactional
void transferir() {
    origen.retirar(100);  // ✅
    destino.depositar(100); // ❌ FALLA
    // Rollback automático: origen vuelve a estado original
}
```

#### **C - Consistency (Consistencia)**
La DB siempre está en estado válido.

```java
// Restricción: saldo >= 0
@Transactional
void retirar(BigDecimal monto) {
    if (saldo.compareTo(monto) < 0) {
        throw new SaldoInsuficienteException();
        // Rollback: mantiene consistencia
    }
    saldo = saldo.subtract(monto);
}
```

#### **I - Isolation (Aislamiento)**
Transacciones concurrentes no se interfieren.

```java
// Usuario A y B retiran al mismo tiempo
// Isolation evita condiciones de carrera
@Transactional(isolation = Isolation.SERIALIZABLE)
void retirar() { ... }
```

#### **D - Durability (Durabilidad)**
Cambios confirmados sobreviven a crashes.

```java
@Transactional
void guardar(Cliente c) {
    c.persist();
    // Commit → datos en disco
    // Crash después → datos siguen ahí
}
```

### 8.3 @Transactional en Quarkus

```java
import jakarta.transaction.Transactional;

@POST
@Transactional  // ← OBLIGATORIO para modificar DB
public Response crear(Cliente cliente) {
    cliente.persist();
    return Response.status(201).entity(cliente).build();
}
```

**Sin @Transactional:**
```
jakarta.persistence.TransactionRequiredException: 
No transaction is currently active
```

**Dónde usar @Transactional:**
- ✅ `persist()`
- ✅ `update()`  
- ✅ `delete()`
- ❌ `find()` (solo lectura)
- ❌ `listAll()` (solo lectura)

### 8.4 Propagación de Transacciones

```java
@Transactional(value = TxType.???)
```

| Tipo | Comportamiento |
|------|---------------|
| **REQUIRED** (default) | Usa tx existente o crea nueva |
| **REQUIRES_NEW** | Siempre crea nueva tx (suspende actual) |
| **MANDATORY** | Debe existir tx, sino error |
| **SUPPORTS** | Usa tx si existe, sino ejecuta sin tx |
| **NOT_SUPPORTED** | Suspende tx actual |
| **NEVER** | Error si existe tx |

**Ejemplo:**
```java
@Transactional
void metodoA() {
    // Transacción A
    metodoB();  // Usa misma transacción
}

@Transactional(TxType.REQUIRES_NEW)
void metodoB() {
    // Nueva transacción independiente
    // Commit separado
}
```

---

## 9. Lazy Loading vs Eager Loading

### 9.1 ¿Qué es Lazy Loading?

**Lazy (Perezoso):** Cargar datos **solo cuando se accede**.

```java
@Entity
public class Cliente extends PanacheEntity {
    public String nombre;
    
    @OneToMany(fetch = FetchType.LAZY)  // Default
    public List<Prestamo> prestamos;
}

// Query:
Cliente c = Cliente.findById(1L);
// SQL: SELECT * FROM clientes WHERE id = 1
// NO carga préstamos aún

System.out.println(c.prestamos.size());
// AHORA ejecuta: SELECT * FROM prestamos WHERE cliente_id = 1
```

### 9.2 ¿Qué es Eager Loading?

**Eager (Ansioso):** Cargar datos **inmediatamente** con JOIN.

```java
@OneToMany(fetch = FetchType.EAGER)
public List<Prestamo> prestamos;

// Query:
Cliente c = Cliente.findById(1L);
// SQL: 
// SELECT c.*, p.* 
// FROM clientes c 
// LEFT JOIN prestamos p ON c.id = p.cliente_id 
// WHERE c.id = 1
// Carga TODO de una vez
```

### 9.3 Comparación

| Aspecto | Lazy | Eager |
|---------|------|-------|
| **Carga inicial** | Rápida (solo entidad) | Lenta (JOIN) |
| **Queries SQL** | Múltiples (N+1 problem) | Una sola |
| **Memoria** | Menor | Mayor |
| **Sesión cerrada** | ❌ LazyInitException | ✅ Funciona |
| **Default** | @OneToMany, @ManyToMany | @ManyToOne, @OneToOne |

### 9.4 Problema N+1

**Código:**
```java
List<Cliente> clientes = Cliente.listAll();  // 1 query

for (Cliente c : clientes) {
    System.out.println(c.prestamos.size());  // N queries
}
```

**SQL ejecutado:**
```sql
-- Query 1:
SELECT * FROM clientes;  

-- Query 2 (cliente 1):
SELECT * FROM prestamos WHERE cliente_id = 1;

-- Query 3 (cliente 2):
SELECT * FROM prestamos WHERE cliente_id = 2;

-- ... N queries (uno por cliente)
```

**Total:** 1 + N queries = **Problema N+1**

### 9.5 Soluciones al N+1

#### **Solución 1: JOIN FETCH**
```java
@GET
public List<Cliente> listar() {
    return Cliente.find("SELECT c FROM Cliente c JOIN FETCH c.prestamos").list();
    // 1 sola query con JOIN
}
```

#### **Solución 2: @EntityGraph**
```java
@NamedEntityGraph(
    name = "Cliente.prestamos",
    attributeNodes = @NamedAttributeNode("prestamos")
)
@Entity
public class Cliente { ... }

// Uso:
entityManager.find(Cliente.class, 1L, 
    Map.of("jakarta.persistence.fetchgraph", "Cliente.prestamos"));
```

#### **Solución 3: Eager (cuidado)**
```java
@OneToMany(fetch = FetchType.EAGER)
public List<Prestamo> prestamos;
// Siempre carga, incluso si no necesitas
```

### 9.6 LazyInitializationException

**Error común:**
```java
@GET
public Cliente obtener(@PathParam("id") Long id) {
    Cliente c = Cliente.findById(id);
    // Sesión Hibernate cierra aquí
    return c;  // Jackson serializa
}

// JSON intenta acceder c.prestamos
// ❌ LazyInitializationException: no session
```

**Solución 1: @JsonIgnore**
```java
@JsonIgnore
@OneToMany(...)
public List<Prestamo> prestamos;
// No serializa, evita lazy load
```

**Solución 2: DTO**
```java
public class ClienteDTO {
    public Long id;
    public String nombre;
    // Solo datos necesarios, sin lazy relations
}
```

**Solución 3: EAGER (para este caso)**
```java
@OneToMany(fetch = FetchType.EAGER)
public List<Prestamo> prestamos;
```

---

## 10. Queries con Panache

### 10.1 Métodos Básicos

```java
// Find by ID
Cliente c = Cliente.findById(1L);

// Listar todos
List<Cliente> todos = Cliente.listAll();

// Contar
long total = Cliente.count();

// Existe
boolean existe = Cliente.count("dni", "12345678") > 0;

// Eliminar todos
Cliente.deleteAll();
```

### 10.2 Find con Parámetros

```java
// Por campo
List<Cliente> clientes = Cliente.find("nombre", "Juan").list();

// Con operadores
List<Cliente> clientes = Cliente.find("edad > ?1", 18).list();

// Named parameters
Cliente c = Cliente.find("dni = :dni", Parameters.with("dni", "12345678"))
                   .firstResult();

// Múltiples condiciones
List<Prestamo> prestamos = Prestamo.find(
    "estado = ?1 AND monto > ?2", 
    EstadoPrestamo.ACTIVO, 
    new BigDecimal("5000")
).list();
```

### 10.3 Queries HQL/JPQL

```java
// HQL completo
List<Prestamo> prestamos = Prestamo.find(
    "SELECT p FROM Prestamo p WHERE p.cliente.nombre LIKE ?1",
    "%Juan%"
).list();

// JOIN
List<Prestamo> prestamos = Prestamo.find(
    "SELECT p FROM Prestamo p JOIN p.cliente c WHERE c.dni = ?1",
    "12345678"
).list();

// Agregaciones
Long total = Prestamo.find("SELECT SUM(p.monto) FROM Prestamo p")
                     .project(Long.class)
                     .firstResult();
```

### 10.4 Paginación

```java
// Página 1, 10 elementos
PanacheQuery<Cliente> query = Cliente.findAll();
List<Cliente> pagina1 = query.page(0, 10).list();

// Página 2
List<Cliente> pagina2 = query.page(1, 10).list();

// Total de páginas
int totalPaginas = query.pageCount();
```

### 10.5 Ordenamiento

```java
// Ordenar ascendente
List<Cliente> clientes = Cliente.listAll(Sort.by("nombre"));

// Descendente
List<Cliente> clientes = Cliente.listAll(Sort.by("nombre").descending());

// Múltiples campos
List<Cliente> clientes = Cliente.listAll(
    Sort.by("apellido").and("nombre")
);
```

### 10.6 Queries Nativas (SQL)

```java
// SQL nativo
List<Cliente> clientes = Cliente.find(
    "SELECT * FROM clientes WHERE edad > ?1",
    Parameters.with(1, 18)
).project(Cliente.class).list();
```

### 10.7 Stream API

```java
// Procesar con Stream
try (Stream<Cliente> stream = Cliente.streamAll()) {
    stream
        .filter(c -> c.edad > 18)
        .map(c -> c.nombre)
        .forEach(System.out::println);
}
```

---

## 📊 Resumen Comparativo

### JPA vs Panache

| Aspecto | JPA Tradicional | Panache |
|---------|----------------|---------|
| **EntityManager** | Manual `@PersistenceContext` | Abstraído |
| **Repository** | Crear clase completa | `implements PanacheRepository` |
| **Queries** | `em.createQuery(...)` | `find("campo", valor)` |
| **Getters/Setters** | Obligatorios | Generados automáticamente |
| **Boilerplate** | Alto | Mínimo |
| **Curva aprendizaje** | Alta | Baja |

### Active Record vs Repository

| | Active Record | Repository |
|-|---------------|------------|
| **Lógica persistencia** | En entidad | Clase separada |
| **Testing** | Difícil | Fácil |
| **DDD** | No recomendado | Recomendado |
| **Simplicidad** | ✅ Muy simple | ⚠️ Moderado |

### Lazy vs Eager

| | Lazy | Eager |
|-|------|-------|
| **Performance inicial** | ✅ Rápido | ❌ Lento |
| **N+1 Problem** | ❌ Sí | ✅ No |
| **Memoria** | ✅ Bajo | ❌ Alto |
| **Sesión cerrada** | ❌ Falla | ✅ OK |

---

## ✅ Checklist de Conocimientos

Después de estudiar esta teoría, deberías poder:

- [ ] Explicar qué es persistencia y sus tipos
- [ ] Diferenciar JPA (especificación) vs Hibernate (implementación)
- [ ] Entender cómo funciona ORM
- [ ] Conocer ventajas de Panache sobre JPA tradicional
- [ ] Implementar Active Record Pattern
- [ ] Implementar Repository Pattern
- [ ] Elegir entre Active Record y Repository según caso
- [ ] Mapear entidades con anotaciones JPA
- [ ] Configurar relaciones @OneToMany, @ManyToOne, etc.
- [ ] Usar cascade types correctamente
- [ ] Entender ACID y transacciones
- [ ] Saber cuándo usar @Transactional
- [ ] Diferenciar Lazy vs Eager loading
- [ ] Resolver problema N+1
- [ ] Evitar LazyInitializationException
- [ ] Escribir queries con Panache
- [ ] Implementar paginación y ordenamiento

---

**🎉 ¡Teoría completa del Capítulo 4!**

*Ahora tienes las bases sólidas para trabajar con persistencia en aplicaciones reales.* 🚀🏦