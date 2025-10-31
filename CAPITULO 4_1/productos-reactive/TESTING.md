# 🧪 Testing de API Reactiva de Productos

Script de pruebas automatizadas e interactivas para validar todos los endpoints del microservicio reactivo de productos.

---

## 📋 Prerequisitos

```bash
# Verificar que tengas instalado:
bash --version
curl --version
jq --version  # Opcional pero recomendado

# Si no tienes jq:
# macOS:   brew install jq
# Ubuntu:  sudo apt-get install jq
# Windows: choco install jq
```

---

## 🚀 Uso

### 1. Arrancar el servidor
```bash
cd productos-reactive
./mvnw quarkus:dev
```

Espera a que veas: `Listening on: http://localhost:8080`

### 2. Ejecutar el script
```bash
chmod +x test-productos-reactive.sh
./test-productos-reactive.sh
```

---

## 🎯 Qué Prueba el Script

El script ejecuta **14 tests** organizados en 3 módulos:

### Módulo 1: CRUD Básico (7 tests)
- ✅ Listar todos los productos (GET all)
- ✅ Buscar producto por ID (GET by ID)
- ✅ Crear nuevo producto (POST)
- ✅ Actualizar producto existente (PUT)
- ✅ Buscar producto inexistente → HTTP 404
- ✅ Eliminar producto (DELETE)
- ✅ Verificar eliminación → HTTP 404

### Módulo 2: Operaciones Avanzadas Reactivas (3 tests)
- ✅ Buscar productos con stock bajo (filtro)
- ✅ **Carga masiva** - Crear 50 productos reactivamente
- ✅ Verificar carga masiva (listar todos)

### Módulo 3: Validaciones y Casos Edge (4 tests)
- ✅ Rechazar precio negativo → HTTP 400
- ✅ Rechazar stock negativo → HTTP 400
- ✅ Actualizar producto inexistente → HTTP 404
- ✅ Eliminar producto ya eliminado → HTTP 404

---

## 📊 Salida Esperada

```
╔════════════════════════════════════════════════════════════════════════════╗
║  ⚡ PRUEBAS INTERACTIVAS - API REACTIVA DE PRODUCTOS (QUARKUS)       ║
╚════════════════════════════════════════════════════════════════════════════╝

🔍 Verificando servidor... ✓ Online

▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓
  📦 MÓDULO 1: OPERACIONES CRUD BÁSICAS (REACTIVO)
▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓

Test #1: Listar todos los productos (GET all) ✓ PASS (HTTP 200)
Test #2: Buscar producto por ID ✓ PASS (HTTP 200)
...

╔════════════════════════════════════════════════════════════════════════════╗
║  📊 RESUMEN DE EJECUCIÓN                                                ║
╚════════════════════════════════════════════════════════════════════════════╝

  🎉 ✓ TODOS LOS TESTS PASARON

  ✓ Tests Exitosos:  14 / 14
  ✗ Tests Fallidos:  0 / 14

  📄 Resultados guardados en: resultados-productos-reactive-2025-10-30_21-30-15.txt
```

---

## 📁 Archivos Generados

Después de ejecutar, se genera un archivo de texto con timestamp:
```
resultados-productos-reactive-2025-10-30_21-30-15.txt
```

**Características del archivo:**
- ✅ Sin códigos ANSI (texto limpio y legible)
- ✅ Timestamp único en el nombre
- ✅ Formato profesional para documentación
- ✅ Listo para compartir y archivar

---

## 🛠️ Troubleshooting

### Error: "servidor no disponible"
```bash
# Verificar que Quarkus esté corriendo
curl http://localhost:8080/q/health

# Si no responde, arrancar:
cd productos-reactive
./mvnw quarkus:dev
```

### Error: "jq: command not found"
```bash
# El script funciona sin jq, pero se ve mejor con él

# macOS
brew install jq

# Ubuntu/Debian
sudo apt-get install jq

# Windows (con Chocolatey)
choco install jq
```

### Error: "Permission denied"
```bash
chmod +x test-productos-reactive.sh
```

### Tests fallando con 404 en productos

**Causa:** Base de datos vacía o sin datos iniciales

**Solución 1 - Verificar import.sql:**
```bash
# Verificar que existe: src/main/resources/import.sql
cat src/main/resources/import.sql
```

**Solución 2 - Reiniciar con drop-and-create:**
```properties
# En application.properties:
quarkus.hibernate-orm.database.generation=drop-and-create
```

### Base de datos se borra al reiniciar

**Causa:** `database.generation=drop-and-create`

**Solución:** Cambiar a:
```properties
quarkus.hibernate-orm.database.generation=update
```

---

## 🔗 URLs Útiles

Después de los tests:
```
Swagger UI:    http://localhost:8080/q/swagger-ui
Dev UI:        http://localhost:8080/q/dev
Health:        http://localhost:8080/q/health
Productos:     http://localhost:8080/api/v1/productos/reactivo
```

---

## 📋 Endpoints Cubiertos

```
✓ GET    /api/v1/productos/reactivo              # Listar todos
✓ GET    /api/v1/productos/reactivo/{id}         # Por ID
✓ POST   /api/v1/productos/reactivo              # Crear
✓ PUT    /api/v1/productos/reactivo/{id}         # Actualizar
✓ DELETE /api/v1/productos/reactivo/{id}         # Eliminar
✓ GET    /api/v1/productos/reactivo/stock-bajo/{umbral}  # Filtrar stock
✓ POST   /api/v1/productos/reactivo/carga-masiva/{cantidad}  # Carga masiva
```

---

## ⚡ Conceptos Reactivos Demostrados

El script demuestra las siguientes características reactivas:

### 1. **Operaciones No Bloqueantes (Uni\<T\>)**
```java
public Uni<List<Producto>> listarTodos() {
    return repository.listAll(); // ⚡ No bloquea thread
}
```

### 2. **Composición Reactiva**
```java
return repository.findById(id)
    .onItem().ifNotNull().transform(p -> Response.ok(p).build())
    .onItem().ifNull().continueWith(Response.status(404).build());
```

### 3. **Transacciones Reactivas**
```java
return Panache.withTransaction(() -> 
    repository.persist(producto)
);
```

### 4. **Alta Concurrencia**
El endpoint de **carga masiva** demuestra cómo crear múltiples registros sin bloquear threads, permitiendo alta concurrencia.

---

## 📚 Comparación: Clásico vs Reactivo

| Aspecto | Clásico (Préstamos) | Reactivo (Productos) |
|---------|---------------------|----------------------|
| **Tipo retorno** | `List<T>` | `Uni<List<T>>` |
| **Operaciones** | Bloqueantes | No bloqueantes |
| **Threads** | Pool grande (200+) | Event Loop (4-8) |
| **Concurrencia** | ~200 req/seg | ~2000+ req/seg |
| **Transacciones** | `@Transactional` | `Panache.withTransaction()` |
| **Driver BD** | JDBC | Reactive PostgreSQL |

---

## ✅ Checklist

Antes de ejecutar:

- [ ] Quarkus arrancado (`./mvnw quarkus:dev`)
- [ ] PostgreSQL activo
- [ ] Base de datos `productos_db` creada
- [ ] `import.sql` con datos iniciales
- [ ] Script con permisos (`chmod +x`)

---

## 🎓 Aprendizaje

Este script te ayuda a entender:

1. **Programación Reactiva** - Uni\<T\> vs tipos bloqueantes
2. **Event Loop** - Cómo funcionan los threads reactivos
3. **Alta Concurrencia** - Manejo eficiente de múltiples requests
4. **Composición** - Encadenamiento de operaciones asíncronas
5. **Backpressure** - Control automático de flujo

---

## 💡 Ejercicio Propuesto

1. Ejecutar carga masiva con diferentes cantidades (10, 50, 100, 500)
2. Observar los logs SQL reactivos
3. Comparar tiempos de ejecución
4. Analizar: ¿Por qué el enfoque reactivo es más eficiente?
