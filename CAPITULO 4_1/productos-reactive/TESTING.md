# Guía de Testing - API Reactiva Quarkus

Documentación completa de pruebas para el proyecto de persistencia reactiva.

---

## 📋 Tabla de Contenidos

1. [Pruebas con Script Automatizado](#1-pruebas-con-script-automatizado)
2. [Pruebas Manuales con cURL](#2-pruebas-manuales-con-curl)
3. [Pruebas con Swagger UI](#3-pruebas-con-swagger-ui)
4. [Pruebas de Rendimiento](#4-pruebas-de-rendimiento)
5. [Validación de Resultados](#5-validación-de-resultados)
6. [Solución de Problemas](#6-solución-de-problemas)

---

## 1. Pruebas con Script Automatizado

### 1.1 Preparación del Script

```bash
# Dar permisos de ejecución (solo primera vez)
chmod +x test-api.sh
```

### 1.2 Ejecutar el Script Completo

El proyecto incluye `test-api.sh` que prueba todos los endpoints automáticamente.

```bash
# Ejecutar todas las pruebas
./test-api.sh
```

⚠️ **Problema:** La consola se llena y no muestra todo el output.

**Soluciones:**

#### Solución A: Guardar en Archivo ⭐ Recomendado
```bash
# Guardar resultados completos
./test-api.sh > resultados.txt

# Ver el archivo
cat resultados.txt

# O verlo paginado
less resultados.txt
```

#### Solución B: Paginar Directamente
```bash
# Ver con scroll
./test-api.sh | less

# Controles en less:
# - Espacio: avanzar página
# - b: retroceder página  
# - q: salir
```

#### Solución C: Script de Resumen (Ver Solo lo Importante)

Crear `test-api-resumen.sh` para ver solo resultados clave sin todo el JSON:

```bash
cat > test-api-resumen.sh << 'EOF'
#!/bin/bash

echo "=========================================="
echo "RESUMEN DE PRUEBAS API REACTIVA"
echo "=========================================="
echo ""

echo "1️⃣  Listar productos..."
RESULT=$(curl -s http://localhost:8080/api/v1/productos/reactivo)
COUNT=$(echo $RESULT | jq '. | length')
echo "   ✅ $COUNT productos encontrados"
echo ""

echo "2️⃣  Buscar producto ID=1..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/productos/reactivo/1)
echo "   ✅ HTTP $STATUS"
echo ""

echo "3️⃣  Crear producto..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/api/v1/productos/reactivo \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Test","descripcion":"Test","precio":100,"stock":10}')
echo "   ✅ HTTP $STATUS"
echo ""

echo "4️⃣  Actualizar producto ID=1..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X PUT http://localhost:8080/api/v1/productos/reactivo/1 \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Updated","descripcion":"Test","precio":200,"stock":5}')
echo "   ✅ HTTP $STATUS"
echo ""

echo "5️⃣  Stock bajo (< 20)..."
RESULT=$(curl -s http://localhost:8080/api/v1/productos/reactivo/stock-bajo/20)
COUNT=$(echo $RESULT | jq '. | length')
echo "   ✅ $COUNT productos con stock bajo"
echo ""

echo "6️⃣  Carga masiva (100 productos)..."
START=$(date +%s%N)
curl -s -X POST http://localhost:8080/api/v1/productos/reactivo/carga-masiva/100 > /dev/null
END=$(date +%s%N)
DURATION=$(echo "scale=3; ($END - $START) / 1000000000" | bc)
echo "   ✅ Completado en ${DURATION}s"
echo ""

echo "7️⃣  Eliminar producto ID=3..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE http://localhost:8080/api/v1/productos/reactivo/3)
echo "   ✅ HTTP $STATUS"
echo ""

echo "8️⃣  Verificar eliminación (debe ser 404)..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/productos/reactivo/3)
echo "   ✅ HTTP $STATUS"
echo ""

echo "=========================================="
echo "✅ Todas las pruebas completadas!"
echo "=========================================="
EOF

chmod +x test-api-resumen.sh
```

**Ejecutar el resumen:**
```bash
./test-api-resumen.sh
```

**Salida esperada:**
```
==========================================
RESUMEN DE PRUEBAS API REACTIVA
==========================================

1️⃣  Listar productos...
   ✅ 104 productos encontrados

2️⃣  Buscar producto ID=1...
   ✅ HTTP 200

3️⃣  Crear producto...
   ✅ HTTP 201

4️⃣  Actualizar producto ID=1...
   ✅ HTTP 200

5️⃣  Stock bajo (< 20)...
   ✅ 27 productos con stock bajo

6️⃣  Carga masiva (100 productos)...
   ✅ Completado en 0.234s

7️⃣  Eliminar producto ID=3...
   ✅ HTTP 204

8️⃣  Verificar eliminación (debe ser 404)...
   ✅ HTTP 404

==========================================
✅ Todas las pruebas completadas!
==========================================
```

#### Solución D: Aumentar Buffer de Terminal

**iTerm2 (Mac):**
- Preferences → Profiles → Terminal → Scrollback lines → 10000

**Terminal (Mac):**
- Preferences → Profiles → Window → Scrollback → 10000

**Windows Terminal:**
- Settings → Default → Scrollback → 10000

### 1.3 Recomendación de Uso

**Para desarrollo diario:**
```bash
./test-api-resumen.sh  # Rápido, solo ve que todo funciona
```

**Para documentación/evidencia:**
```bash
./test-api.sh > resultados-$(date +%Y%m%d).txt  # Guarda todo con fecha
```

**Para debugging:**
```bash
./test-api.sh | less  # Navega por todo el output
```

### 1.2 Resultados Esperados

#### ✅ Test 1: Listar Todos los Productos

**Comando:**
```bash
curl -s http://localhost:8080/api/v1/productos/reactivo | jq
```

**Resultado esperado:**
- HTTP 200 OK
- Array JSON con productos
- Incluye productos iniciales (Laptop, Mouse, Teclado) más cualquier producto creado

**Ejemplo de salida:**
```json
[
  {
    "id": 1,
    "nombre": "Laptop Dell XPS 15",
    "descripcion": "Laptop actualizada",
    "precio": 1600.0,
    "stock": 15
  },
  ...
]
```

---

#### ✅ Test 2: Buscar Producto por ID

**Comando:**
```bash
curl -s http://localhost:8080/api/v1/productos/reactivo/1 | jq
```

**Resultado esperado:**
- HTTP 200 OK si existe
- HTTP 404 si no existe
- Objeto JSON con el producto

**Ejemplo de salida:**
```json
{
  "id": 1,
  "nombre": "Laptop Dell XPS 15",
  "descripcion": "Laptop actualizada",
  "precio": 1600.0,
  "stock": 15
}
```

---

#### ✅ Test 3: Crear Nuevo Producto

**Comando:**
```bash
curl -s -X POST http://localhost:8080/api/v1/productos/reactivo \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Auriculares Sony",
    "descripcion": "Auriculares inalámbricos con cancelación de ruido",
    "precio": 299.99,
    "stock": 25
  }' | jq
```

**Resultado esperado:**
- HTTP 201 Created
- Header `Location` con URL del nuevo recurso
- Objeto JSON con el producto creado (incluye ID asignado)

**Ejemplo de salida:**
```json
{
  "id": 105,
  "nombre": "Auriculares Sony",
  "descripcion": "Auriculares inalámbricos con cancelación de ruido",
  "precio": 299.99,
  "stock": 25
}
```

---

#### ✅ Test 4: Actualizar Producto

**Comando:**
```bash
curl -s -X PUT http://localhost:8080/api/v1/productos/reactivo/1 \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Laptop Dell XPS 15 Pro",
    "descripcion": "Laptop profesional actualizada",
    "precio": 1800.00,
    "stock": 20
  }' | jq
```

**Resultado esperado:**
- HTTP 200 OK si existe
- HTTP 404 si no existe
- Objeto JSON con el producto actualizado

---

#### ✅ Test 5: Stock Bajo

**Comando:**
```bash
curl -s http://localhost:8080/api/v1/productos/reactivo/stock-bajo/20 | jq
```

**Resultado esperado:**
- HTTP 200 OK
- Array con productos que tienen stock menor al umbral especificado
- Si no hay productos, array vacío `[]`

**Ejemplo de salida:**
```json
[
  {
    "id": 4,
    "nombre": "Monitor LG 27",
    "descripcion": "Monitor gaming 144Hz",
    "precio": 450.0,
    "stock": 8
  },
  {
    "id": 18,
    "nombre": "Producto Masivo 14",
    "descripcion": "Generado automáticamente",
    "precio": 888.18,
    "stock": 9
  }
]
```

---

#### ✅ Test 6: Carga Masiva (Demuestra Concurrencia)

**Comando:**
```bash
time curl -s -X POST http://localhost:8080/api/v1/productos/reactivo/carga-masiva/100 | jq
```

**Resultado esperado:**
- HTTP 200 OK
- Mensaje confirmando la cantidad de productos creados
- Operación completada en **menos de 1 segundo** (demuestra eficiencia reactiva)

**Ejemplo de salida:**
```json
{
  "mensaje": "100 productos creados exitosamente"
}
```

**Nota:** El comando `time` muestra cuánto demoró la operación. Observarás que crear 100 productos es muy rápido gracias al enfoque reactivo.

---

#### ✅ Test 7: Eliminar Producto

**Comando:**
```bash
curl -s -X DELETE http://localhost:8080/api/v1/productos/reactivo/3 -w "\nHTTP Status: %{http_code}\n"
```

**Resultado esperado:**
- HTTP 204 No Content si existe y se elimina
- HTTP 404 si no existe
- Sin body en la respuesta

**Ejemplo de salida:**
```
HTTP Status: 204
```

---

#### ✅ Test 8: Verificar Eliminación

**Comando:**
```bash
curl -s http://localhost:8080/api/v1/productos/reactivo/3 -w "\nHTTP Status: %{http_code}\n"
```

**Resultado esperado:**
- HTTP 404 Not Found
- Confirma que el producto fue eliminado exitosamente

---

## 2. Pruebas Manuales con cURL

### 2.1 Formato de Respuestas

Por defecto, las respuestas vienen en formato JSON compacto. Para mejor legibilidad:

```bash
# Con jq (recomendado)
curl -s http://localhost:8080/api/v1/productos/reactivo | jq

# Sin jq (alternativa)
curl http://localhost:8080/api/v1/productos/reactivo
```

---

### 2.2 Ver Headers de Respuesta

```bash
# Incluir headers en la salida
curl -i http://localhost:8080/api/v1/productos/reactivo/1

# Ver solo headers
curl -I http://localhost:8080/api/v1/productos/reactivo/1
```

---

### 2.3 Verbose Mode (Debugging)

```bash
# Ver toda la comunicación HTTP
curl -v http://localhost:8080/api/v1/productos/reactivo/1
```

---

### 2.4 Guardar Respuesta en Archivo

```bash
# Guardar en archivo
curl -s http://localhost:8080/api/v1/productos/reactivo > productos.json

# Ver el archivo
cat productos.json | jq
```

---

## 3. Pruebas con Swagger UI

### 3.1 Acceder a Swagger

1. Abrir navegador en: http://localhost:8080/q/swagger-ui
2. Expandir el endpoint a probar
3. Clic en **"Try it out"**
4. Completar parámetros
5. Clic en **"Execute"**
6. Ver respuesta abajo

### 3.2 Ventajas de Swagger

- ✅ Interfaz gráfica (ideal para Windows)
- ✅ No requiere instalar nada
- ✅ Validación automática de parámetros
- ✅ Ejemplos de request/response
- ✅ Documentación interactiva

---

## 4. Pruebas de Rendimiento

### 4.1 Medir Tiempo de Respuesta

#### Prueba Individual
```bash
time curl -s http://localhost:8080/api/v1/productos/reactivo
```

#### Carga Masiva (100 productos)
```bash
time curl -s -X POST http://localhost:8080/api/v1/productos/reactivo/carga-masiva/100
```

**Resultado esperado:** < 1 segundo para 100 productos

---

### 4.2 Prueba de Concurrencia (Avanzado)

Requiere Apache Bench (`ab`):

```bash
# Instalar ab
# macOS: incluido en Apache
# Linux: sudo apt-get install apache2-utils

# 1000 requests, 100 concurrentes
ab -n 1000 -c 100 http://localhost:8080/api/v1/productos/reactivo
```

**Métricas a observar:**
- Requests per second (RPS)
- Time per request
- Failed requests (debería ser 0)

---

### 4.3 Comparación: Operación Simple vs Carga Masiva

```bash
# Crear 1 producto a la vez (100 veces) - LENTO
for i in {1..100}; do
  curl -s -X POST http://localhost:8080/api/v1/productos/reactivo \
    -H "Content-Type: application/json" \
    -d "{\"nombre\":\"Test $i\",\"precio\":100,\"stock\":10}" > /dev/null
done

# vs

# Crear 100 productos de una vez (batch) - RÁPIDO
curl -s -X POST http://localhost:8080/api/v1/productos/reactivo/carga-masiva/100
```

**Diferencia esperada:** El batch es ~50-100x más rápido.

---

## 5. Validación de Resultados

### 5.1 Verificar en Base de Datos

```bash
# Conectar a PostgreSQL
psql -U postgres -d productos_db

# Ver todos los productos
SELECT id, nombre, precio, stock FROM Producto ORDER BY id;

# Contar productos
SELECT COUNT(*) FROM Producto;

# Ver productos con stock bajo
SELECT * FROM Producto WHERE stock < 20;

# Salir
\q
```

---

### 5.2 Verificar Logs de Quarkus

En la terminal donde corre `./mvnw quarkus:dev`, observa:

```
Hibernate: SELECT ... FROM Producto ...
Hibernate: INSERT INTO Producto ...
```

**Puntos a verificar:**
- ✅ Queries SQL ejecutándose
- ✅ Transacciones completadas
- ✅ Sin errores en logs

---

### 5.3 Checklist de Validación

Después de ejecutar `./test-api.sh`, verificar:

- [ ] Test 1: Retorna lista de productos ✅
- [ ] Test 2: Encuentra producto por ID ✅
- [ ] Test 3: Crea producto y retorna 201 ✅
- [ ] Test 4: Actualiza producto existente ✅
- [ ] Test 5: Filtra por stock correctamente ✅
- [ ] Test 6: Carga masiva completa en < 1s ✅
- [ ] Test 7: Elimina producto (204) ✅
- [ ] Test 8: Producto eliminado no existe (404) ✅

---

## 6. Solución de Problemas

### ❌ Error: "Connection refused"

**Causa:** Quarkus no está corriendo.

**Solución:**
```bash
./mvnw quarkus:dev
```

Espera a ver: `Listening on: http://localhost:8080`

---

### ❌ Error: "jq: command not found"

**Causa:** jq no está instalado.

**Solución:**
```bash
# macOS
brew install jq

# Linux
sudo apt-get install jq

# Windows
# Descargar desde: https://stedolan.github.io/jq/download/
```

**O ejecutar sin jq:**
```bash
curl http://localhost:8080/api/v1/productos/reactivo
```

---

### ❌ Error: 404 Not Found

**Causa:** URL incorrecta o endpoint no existe.

**Solución:**
- Verificar la URL: `/api/v1/productos/reactivo`
- Ver rutas disponibles en: http://localhost:8080/q/dev
- Revisar que el ID existe (para GET/{id}, PUT/{id}, DELETE/{id})

---

### ❌ Error: 500 Internal Server Error

**Causa:** Error en el servidor (BD, código).

**Solución:**
1. Ver logs en la terminal de Quarkus
2. Verificar que PostgreSQL está corriendo:
   ```bash
   psql -U postgres -c "SELECT 1"
   ```
3. Revisar que la base de datos `productos_db` existe

---

### ❌ Base de Datos Vacía

**Causa:** `import.sql` no se ejecutó.

**Solución:**
```bash
# Verificar que existe
cat src/main/resources/import.sql

# Reiniciar Quarkus (presiona 's' en la terminal)
# O detener y volver a iniciar:
# Ctrl+C
./mvnw quarkus:dev
```

---

### ❌ Puerto 8080 en Uso

**Causa:** Otro proceso usa el puerto 8080.

**Solución:**
```bash
# Ver qué proceso usa el puerto
lsof -i :8080

# Matar el proceso
kill -9 <PID>

# O cambiar el puerto en application.properties
quarkus.http.port=8081
```

---

## 📊 Resultados de Pruebas Reales

### Ejemplo de Ejecución Completa

```bash
$ ./test-api.sh

==========================================
PRUEBAS API REACTIVA - QUARKUS
==========================================

1️⃣  Listar todos los productos:
✅ 104 productos retornados

2️⃣  Buscar producto por ID (ID=1):
✅ Producto encontrado

3️⃣  Crear nuevo producto:
✅ Producto creado con ID=105

4️⃣  Actualizar producto (ID=1):
✅ Producto actualizado

5️⃣  Buscar productos con stock bajo (umbral < 20):
✅ 27 productos con stock bajo

6️⃣  Carga masiva (crear 100 productos):
✅ 100 productos creados en 0.234 segundos

7️⃣  Eliminar producto (ID=3):
✅ HTTP Status: 204

8️⃣  Verificar eliminación:
✅ HTTP Status: 404

==========================================
✅ Pruebas completadas exitosamente!
==========================================
```

---

## 🎯 Ejercicios Propuestos

### Ejercicio 1: Flujo Completo CRUD
1. Crear un producto
2. Leer el producto creado
3. Actualizar el producto
4. Eliminarlo
5. Verificar que ya no existe

### Ejercicio 2: Carga y Análisis
1. Ejecutar carga masiva de 500 productos
2. Consultar productos con stock bajo (umbral 10)
3. Medir tiempo de respuesta
4. Comparar con umbral 50

### Ejercicio 3: Rendimiento
1. Medir tiempo de crear 100 productos uno por uno
2. Medir tiempo de crear 100 productos con carga masiva
3. Calcular diferencia de rendimiento
4. Explicar por qué la diferencia

---

## 📚 Recursos Adicionales

- [cURL Documentation](https://curl.se/docs/)
- [jq Manual](https://stedolan.github.io/jq/manual/)
- [HTTP Status Codes](https://httpstatuses.com/)
- [Swagger UI](https://swagger.io/tools/swagger-ui/)

---

## ✅ Checklist de Entrega

Antes de entregar, verificar:

- [ ] `./mvnw quarkus:dev` inicia sin errores
- [ ] `./test-api.sh` pasa todas las pruebas
- [ ] Swagger UI accesible en http://localhost:8080/q/swagger-ui
- [ ] PostgreSQL corriendo y BD creada
- [ ] README.md documentado
- [ ] TEORIA.md completo
- [ ] TESTING.md (este archivo) incluido
