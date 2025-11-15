# Proyecto Quarkus - Persistencia Clásica con Panache

Ejercicio práctico para demostrar persistencia clásica (bloqueante) usando **Quarkus 3.17.4**, **Hibernate ORM Panache** y **PostgreSQL**.

---

## 📋 Requisitos Previos

- Java 21
- Maven 3.8+
- PostgreSQL 12+ (corriendo en `localhost:5432`)
- cURL (incluido en Windows 10+, macOS y Linux)

---

## 🚀 Creación del Proyecto

### Paso 1: Crear el proyecto Quarkus

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.17.4:create \
    -DprojectGroupId=pe.banco \
    -DprojectArtifactId=productos-clasico \
    -DprojectVersion=1.0.0-SNAPSHOT \
    -Dextensions="rest-jackson,hibernate-orm-panache,jdbc-postgresql,hibernate-validator"
```

### Paso 2: Entrar al proyecto

```bash
cd productos-clasico
```

### Paso 3: Configurar PostgreSQL

Asegúrate de que PostgreSQL esté corriendo y accesible. El proyecto está configurado para usar:
- **Host:** localhost
- **Puerto:** 5432
- **Base de datos:** postgres
- **Usuario:** rodrigosilva
- **Password:** (vacío)

Si necesitas cambiar estas credenciales, edita `src/main/resources/application.properties`.

### Paso 4: Verificar configuración

El archivo `application.properties` ya está configurado con:

```properties
# PostgreSQL JDBC (bloqueante)
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=rodrigosilva
quarkus.datasource.password=
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/postgres

# Hibernate
quarkus.hibernate-orm.database.generation=drop-and-create
quarkus.hibernate-orm.log.sql=true
quarkus.hibernate-orm.sql-load-script=import.sql

# HTTP
quarkus.http.port=8080
```

### Paso 5: Datos iniciales

El archivo `import.sql` contiene datos de prueba que se cargan automáticamente:

```sql
INSERT INTO Producto (id, nombre, descripcion, precio, stock) VALUES (1, 'Laptop Dell XPS', 'Laptop de alto rendimiento', 1500.00, 10);
INSERT INTO Producto (id, nombre, descripcion, precio, stock) VALUES (2, 'Mouse Logitech', 'Mouse inalámbrico', 25.50, 50);
INSERT INTO Producto (id, nombre, descripcion, precio, stock) VALUES (3, 'Teclado Mecánico', 'Teclado RGB', 89.99, 30);
ALTER SEQUENCE Producto_SEQ RESTART WITH 4;
```

---

## 📁 Estructura del Proyecto

```
pe.banco.productos
├── entity/
│   └── Producto.java              # Entidad JPA que extiende PanacheEntity
├── repository/
│   └── ProductoRepository.java    # Repository clásico (PanacheRepositoryBase)
├── dto/
│   └── ProductoRequest.java       # DTO para requests
└── resource/
    └── ProductoClasico​Resource.java  # REST endpoints clásicos (bloqueantes)
```

---

## ▶️ Ejecutar el Proyecto

```bash
./mvnw quarkus:dev
```

**Accesos:**
- API: http://localhost:8080/api/v1/productos/clasico
- Dev UI: http://localhost:8080/q/dev

---

## 🧪 Pruebas con cURL

### 1. Listar todos los productos

```bash
curl http://localhost:8080/api/v1/productos/clasico
```

**Respuesta esperada:**
```json
[
  {
    "id": 1,
    "nombre": "Laptop Dell XPS",
    "descripcion": "Laptop de alto rendimiento",
    "precio": 1500.0,
    "stock": 10
  },
  ...
]
```

---

### 2. Buscar producto por ID

```bash
curl http://localhost:8080/api/v1/productos/clasico/1
```

---

### 3. Crear nuevo producto

```bash
curl -X POST http://localhost:8080/api/v1/productos/clasico \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Monitor LG 27",
    "descripcion": "Monitor gaming 144Hz",
    "precio": 450.00,
    "stock": 8
  }'
```

**Respuesta:**
```json
{
  "id": 4,
  "nombre": "Monitor LG 27",
  "descripcion": "Monitor gaming 144Hz",
  "precio": 450.0,
  "stock": 8
}
```

---

### 4. Actualizar producto

```bash
curl -X PUT http://localhost:8080/api/v1/productos/clasico/1 \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Laptop Dell XPS 15",
    "descripcion": "Laptop actualizada",
    "precio": 1600.00,
    "stock": 15
  }'
```

---

### 5. Eliminar producto

```bash
curl -X DELETE http://localhost:8080/api/v1/productos/clasico/2
```

---

### 6. Buscar productos con stock bajo

```bash
curl http://localhost:8080/api/v1/productos/clasico/stock-bajo/20
```

**Retorna todos los productos con stock menor a 20 unidades.**

---

### 7. Carga masiva

```bash
curl -X POST http://localhost:8080/api/v1/productos/clasico/carga-masiva/100
```

**Crea 100 productos de forma bloqueante.** Este endpoint permite comparar el rendimiento con la versión reactiva.

---

## 🎯 Endpoints Disponibles

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/api/v1/productos/clasico` | Listar todos |
| `GET` | `/api/v1/productos/clasico/{id}` | Buscar por ID |
| `POST` | `/api/v1/productos/clasico` | Crear producto |
| `PUT` | `/api/v1/productos/clasico/{id}` | Actualizar producto |
| `DELETE` | `/api/v1/productos/clasico/{id}` | Eliminar producto |
| `GET` | `/api/v1/productos/clasico/stock-bajo/{umbral}` | Stock bajo |
| `POST` | `/api/v1/productos/clasico/carga-masiva/{cantidad}` | Carga masiva |

---

## 🔄 Diferencias con Versión Reactiva

| Aspecto | Clásico (Este proyecto) | Reactivo |
|---------|-------------------------|----------|
| **Thread Model** | Un thread por request (bloqueante) | Event loop (no bloqueante) |
| **Tipos de retorno** | `List<T>`, `Response` | `Uni<T>`, `Multi<T>` |
| **Transacciones** | `@Transactional` | `Panache.withTransaction()` |
| **Driver BD** | JDBC (jdbc-postgresql) | Reactivo (reactive-pg-client) |
| **Complejidad** | ✅ Más simple | ⚠️ Mayor curva de aprendizaje |
| **Throughput** | Limitado por threads | ✅ Muy alto |
| **Latencia bajo carga** | ⚠️ Aumenta con concurrencia | ✅ Más estable |
| **Casos de uso** | Apps tradicionales, CRUD simple | Alta concurrencia, microservicios |

---

## 📊 Conceptos Clásicos Demostrados

### ✅ Repository Pattern con PanacheRepositoryBase
```java
@ApplicationScoped
public class ProductoRepository implements PanacheRepositoryBase<Producto, Long> {
    public List<Producto> findConStockBajo(int umbral) {
        return list("stock < ?1", umbral);
    }
}
```

### ✅ Transacciones con @Transactional
```java
@POST
@Transactional
public Response crear(ProductoRequest request) {
    Producto producto = new Producto(...);
    repository.persist(producto);
    return Response.created(...).build();
}
```

### ✅ Operaciones Bloqueantes
```java
// El thread se bloquea hasta que la BD responde
List<Producto> productos = repository.listAll();
```

---

## 🛠️ Tecnologías Utilizadas

- **Quarkus 3.17.4** - Framework Java supersónico
- **Hibernate ORM Panache** - ORM simplificado
- **PostgreSQL** - Base de datos relacional
- **JDBC Driver** - Driver clásico bloqueante para PostgreSQL
- **RESTEasy Classic** - REST endpoints tradicionales
- **Hibernate Validator** - Validación de datos

---

## 🎓 Ejercicio Propuesto

1. Ejecutar carga masiva de 500 productos
2. Observar los logs SQL
3. Comparar tiempos con la versión reactiva
4. Analizar: ¿Por qué el enfoque reactivo es más eficiente en alta concurrencia?

---

## 🐛 Solución de Problemas

### Error: "Connection refused"
**Solución:** Asegurarse de que PostgreSQL esté corriendo:
```bash
psql -U rodrigosilva -d postgres -c "SELECT version();"
```

### Error: "Unable to find JDBC driver"
**Solución:** Verificar que la dependencia `quarkus-jdbc-postgresql` esté en el `pom.xml`

### Tabla vacía
**Solución:** Verificar que `import.sql` esté en `src/main/resources/` y que use IDs explícitos.

### Puerto 8080 ocupado
**Solución:** Cambiar el puerto en `application.properties`:
```properties
quarkus.http.port=8081
```

---

## 🚀 Siguientes Pasos

1. Implementar validaciones Bean Validation en `ProductoRequest`
2. Agregar paginación a `listarTodos()`
3. Implementar búsqueda con filtros múltiples
4. Agregar tests unitarios y de integración
5. Comparar performance con versión reactiva

---

## 📚 Recursos Adicionales

- [Quarkus - Hibernate ORM Panache](https://quarkus.io/guides/hibernate-orm-panache)
- [Quarkus - Simplified Hibernate ORM with Panache](https://quarkus.io/guides/hibernate-orm-panache)
- [Repository Pattern](https://martinfowler.com/eaaCatalog/repository.html)
