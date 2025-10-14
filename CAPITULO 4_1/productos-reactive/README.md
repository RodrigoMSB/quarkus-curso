# Proyecto Quarkus - Persistencia Reactiva con Panache

Ejercicio práctico para demostrar persistencia reactiva usando **Quarkus 3.28.3**, **Hibernate Reactive Panache** y **PostgreSQL**.

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
mvn io.quarkus.platform:quarkus-maven-plugin:3.28.3:create \
    -DprojectGroupId=pe.banco \
    -DprojectArtifactId=productos-reactive \
    -DprojectVersion=1.0.0-SNAPSHOT \
    -Dextensions="resteasy-reactive-jackson,hibernate-reactive-panache,reactive-pg-client,smallrye-openapi"
```

### Paso 2: Entrar al proyecto

```bash
cd productos-reactive
```

### Paso 3: Crear la base de datos en PostgreSQL

```bash
psql -U postgres -c "CREATE DATABASE productos_db;"
```

### Paso 4: Configurar `application.properties`

```properties
# PostgreSQL reactivo
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=postgres
quarkus.datasource.password=postgres
quarkus.datasource.reactive.url=postgresql://localhost:5432/productos_db
quarkus.datasource.jdbc=false

# Hibernate
quarkus.hibernate-orm.database.generation=drop-and-create
quarkus.hibernate-orm.log.sql=true
quarkus.hibernate-orm.sql-load-script=import.sql

# HTTP
quarkus.http.port=8080
```

### Paso 5: Crear datos iniciales (`import.sql`)

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
│   └── ProductoRepository.java    # Repository reactivo (PanacheRepositoryBase)
├── dto/
│   └── ProductoRequest.java       # DTO para requests
└── resource/
    └── ProductoReactivoResource.java  # REST endpoints reactivos
```

---

## ▶️ Ejecutar el Proyecto

```bash
./mvnw quarkus:dev
```

**Accesos:**
- API: http://localhost:8080/api/v1/productos/reactivo
- Swagger UI: http://localhost:8080/q/swagger-ui
- Dev UI: http://localhost:8080/q/dev

---

## 🧪 Pruebas con cURL

### 1. Listar todos los productos

```bash
curl http://localhost:8080/api/v1/productos/reactivo
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
curl http://localhost:8080/api/v1/productos/reactivo/1
```

---

### 3. Crear nuevo producto

```bash
curl -X POST http://localhost:8080/api/v1/productos/reactivo \
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
curl -X PUT http://localhost:8080/api/v1/productos/reactivo/1 \
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
curl -X DELETE http://localhost:8080/api/v1/productos/reactivo/2
```

---

### 6. Buscar productos con stock bajo

```bash
curl http://localhost:8080/api/v1/productos/reactivo/stock-bajo/20
```

**Retorna todos los productos con stock menor a 20 unidades.**

---

### 7. Carga masiva (demuestra concurrencia reactiva)

```bash
curl -X POST http://localhost:8080/api/v1/productos/reactivo/carga-masiva/100
```

**Crea 100 productos de forma reactiva.** Este endpoint demuestra las ventajas de la programación reactiva en operaciones masivas.

---

## 🎯 Endpoints Disponibles

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/api/v1/productos/reactivo` | Listar todos |
| `GET` | `/api/v1/productos/reactivo/{id}` | Buscar por ID |
| `POST` | `/api/v1/productos/reactivo` | Crear producto |
| `PUT` | `/api/v1/productos/reactivo/{id}` | Actualizar producto |
| `DELETE` | `/api/v1/productos/reactivo/{id}` | Eliminar producto |
| `GET` | `/api/v1/productos/reactivo/stock-bajo/{umbral}` | Stock bajo |
| `POST` | `/api/v1/productos/reactivo/carga-masiva/{cantidad}` | Carga masiva |

---

## 🔍 Alternativas a cURL

### Postman (Windows/Mac/Linux)
1. Descargar: https://www.postman.com/downloads/
2. Importar endpoints desde Swagger: http://localhost:8080/q/swagger-ui

### Thunder Client (VS Code)
1. Instalar extensión "Thunder Client" en VS Code
2. Crear requests con interfaz gráfica

### Swagger UI (incluido) ⭐ Recomendado para Windows

**URL:** http://localhost:8080/q/swagger-ui

#### Cómo usar Swagger UI:

1. **Abrir en el navegador:** http://localhost:8080/q/swagger-ui
2. **Expandir** el endpoint que quieres probar (clic en la fila)
3. **Clic en "Try it out"**
4. **Completar los parámetros** según la tabla abajo
5. **Clic en "Execute"**
6. **Ver la respuesta** en la sección "Response body"

---

#### 📝 Datos para cada endpoint en Swagger:

##### 1️⃣ GET - Listar todos los productos
- **Endpoint:** `/api/v1/productos/reactivo`
- **Parámetros:** Ninguno
- **Action:** Solo clic en "Execute"

---

##### 2️⃣ GET - Buscar por ID
- **Endpoint:** `/api/v1/productos/reactivo/{id}`
- **Parámetro `id`:** `1`
- **Action:** Clic en "Execute"

---

##### 3️⃣ POST - Crear producto
- **Endpoint:** `/api/v1/productos/reactivo`
- **Request body:**
```json
{
  "nombre": "Monitor LG 27",
  "descripcion": "Monitor gaming 144Hz",
  "precio": 450.00,
  "stock": 8
}
```
- **Action:** Pegar el JSON y clic en "Execute"

---

##### 4️⃣ PUT - Actualizar producto
- **Endpoint:** `/api/v1/productos/reactivo/{id}`
- **Parámetro `id`:** `1`
- **Request body:**
```json
{
  "nombre": "Laptop Dell XPS 15 Actualizada",
  "descripcion": "Laptop de última generación",
  "precio": 1600.00,
  "stock": 15
}
```
- **Action:** Pegar el JSON y clic en "Execute"

---

##### 5️⃣ DELETE - Eliminar producto
- **Endpoint:** `/api/v1/productos/reactivo/{id}`
- **Parámetro `id`:** `2`
- **Action:** Clic en "Execute"

---

##### 6️⃣ GET - Stock bajo
- **Endpoint:** `/api/v1/productos/reactivo/stock-bajo/{umbral}`
- **Parámetro `umbral`:** `20`
- **Action:** Clic en "Execute"
- **Resultado:** Muestra productos con stock menor a 20

---

##### 7️⃣ POST - Carga masiva (demuestra concurrencia)
- **Endpoint:** `/api/v1/productos/reactivo/carga-masiva/{cantidad}`
- **Parámetro `cantidad`:** `100`
- **Action:** Clic en "Execute"
- **Resultado:** Crea 100 productos reactivamente

---

**💡 Tip:** Swagger UI es la forma más fácil de probar la API en Windows sin instalar nada adicional.

---

## 📊 Conceptos Reactivos Demostrados

### ✅ `Uni<T>` - Operación asíncrona que retorna un solo valor
```java
public Uni<List<Producto>> listarTodos() {
    return repository.listAll();
}
```

### ✅ Composición Reactiva
```java
return repository.findById(id)
    .onItem().ifNotNull().transform(producto -> Response.ok(producto).build())
    .onItem().ifNull().continueWith(Response.status(404).build());
```

### ✅ Transacciones Reactivas
```java
return Panache.withTransaction(() -> repository.persist(producto));
```

### ✅ Operaciones en Lote
```java
return repository.persistirLote(productos);
```

---

## 🛠️ Tecnologías Utilizadas

- **Quarkus 3.28.3** - Framework Java supersónico
- **Hibernate Reactive Panache** - ORM reactivo simplificado
- **PostgreSQL** - Base de datos relacional
- **SmallRye Mutiny** - Librería reactiva (Uni/Multi)
- **RESTEasy Reactive** - REST endpoints reactivos
- **SmallRye OpenAPI** - Documentación automática (Swagger)

---

## 📚 Recursos Adicionales

- [Quarkus - Hibernate Reactive Panache](https://quarkus.io/guides/hibernate-reactive-panache)
- [SmallRye Mutiny](https://smallrye.io/smallrye-mutiny/)
- [Reactive Programming](https://www.reactivemanifesto.org/)

---

## 🎓 Ejercicio Propuesto

1. Ejecutar carga masiva de 500 productos
2. Observar los logs SQL
3. Comparar tiempos con diferentes cantidades
4. Analizar: ¿Por qué el enfoque reactivo es más eficiente en alta concurrencia?

---

## 🐛 Solución de Problemas

### Error: "Unable to find JDBC driver"
**Solución:** Verificar que `quarkus.datasource.jdbc=false` esté en `application.properties`

### Error: "Connection refused"
**Solución:** Asegurarse de que PostgreSQL esté corriendo:
```bash
psql -U postgres -c "SELECT version();"
```

### Tabla vacía
**Solución:** Verificar que `import.sql` esté en `src/main/resources/` y que use IDs explícitos.

