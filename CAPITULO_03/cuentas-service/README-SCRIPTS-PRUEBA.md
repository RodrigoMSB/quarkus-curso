# 🧪 Scripts de Prueba - Capítulo 3: API de Cuentas Bancarias

Scripts automatizados para probar el microservicio de gestión de cuentas bancarias.

---

## 📂 Archivos Incluidos

### **`test-cuentas-api.sh`** (Script Completo)
- ✅ Pruebas exhaustivas de todos los endpoints CRUD
- ✅ Genera archivo `.txt` con resultados detallados
- ✅ Incluye timestamp en el nombre del archivo
- ✅ Muestra resultados en pantalla Y los guarda
- ✅ Verifica casos de éxito y error (404)
- ✅ Pausas interactivas entre tests (presionar tecla para continuar)
- ✅ Muestra códigos HTTP explícitos en cada operación

---

## 🚀 Prerequisitos

### 1. **Servidor Quarkus corriendo**
```bash
cd cuentas-service
./mvnw quarkus:dev
```

### 2. **Herramientas necesarias**
- ✅ `curl` (HTTP requests)
- ✅ `jq` (formateo JSON - opcional pero recomendado)

**Instalar jq si no lo tienes:**

**macOS:**
```bash
brew install jq
```

**Windows (Git Bash):**
```bash
# Descargar desde: https://stedolan.github.io/jq/download/
# O con Chocolatey:
choco install jq
```

**Linux:**
```bash
sudo apt-get install jq
```

---

## 📖 Uso

```bash
# Dar permisos de ejecución (solo primera vez)
chmod +x test-cuentas-api.sh

# Ejecutar
./test-cuentas-api.sh
```

**Resultado:**
- Ejecuta 12 tests completos de forma interactiva
- Espera que presiones una tecla entre cada test
- Muestra todo en pantalla con colores, emojis y códigos HTTP
- Genera archivo `resultados-pruebas-YYYYMMDD-HHMMSS.txt`

**Ejemplo de archivo generado:**
```
resultados-pruebas-20251030-214530.txt
```

---

## ⚠️ Nota Importante

**Los datos del API están en memoria (ConcurrentHashMap).** Entre ejecuciones del script:

### **Reiniciar Quarkus para restaurar datos iniciales**

Si ejecutas el script múltiples veces sin reiniciar Quarkus:
- ❌ TEST 7 (DELETE) fallará con 404 en vez de 204
- ❌ Carlos Ruiz ya no existirá (fue eliminado en la ejecución anterior)
- ❌ Ana Torres se duplicará o causará inconsistencias

**Solución:**
```bash
# En la terminal donde corre Quarkus:
# 1. Detener con Ctrl+C
# 2. Reiniciar:
./mvnw quarkus:dev

# 3. Ahora ejecutar el script:
./test-cuentas-api.sh
```

Esto garantiza que **siempre** empiezas con:
- ✅ 3 cuentas originales (Juan, María, Carlos)
- ✅ Todos los tests dan los resultados esperados

---

## 📊 Tests Ejecutados

| # | Test | Método | Endpoint | Validación |
|---|------|--------|----------|------------|
| 1 | Listar todas | GET | `/cuentas` | HTTP 200, Array con 3 cuentas |
| 2 | Obtener específica | GET | `/cuentas/1000000001` | HTTP 200, Cuenta de Juan Pérez |
| 3 | Crear nueva | POST | `/cuentas` | HTTP 201 Created |
| 4 | Verificar creada | GET | `/cuentas/1000000004` | HTTP 200, Cuenta de Ana Torres |
| 5 | Actualizar | PUT | `/cuentas/1000000004` | HTTP 200 OK |
| 6 | Verificar actualización | GET | `/cuentas/1000000004` | HTTP 200, Saldo actualizado |
| 7 | Eliminar | DELETE | `/cuentas/1000000003` | HTTP 204 No Content |
| 8 | Verificar eliminación | GET | `/cuentas/1000000003` | HTTP 404 Not Found |
| 9 | GET inexistente | GET | `/cuentas/9999999999` | HTTP 404 |
| 10 | PUT inexistente | PUT | `/cuentas/9999999999` | HTTP 404 |
| 11 | DELETE inexistente | DELETE | `/cuentas/9999999999` | HTTP 404 |
| 12 | Estado final | GET | `/cuentas` | HTTP 200, 3 cuentas restantes |

---

## 🖥️ Ejemplo de Salida

```
================================================
🏦 PRUEBAS DE API - GESTIÓN DE CUENTAS BANCARIAS
================================================

💾 Los resultados se guardarán en: resultados-pruebas-20251030-214530.txt

🔍 Verificando conectividad con el servidor...
✅ Servidor respondiendo correctamente

▶️  Presiona cualquier tecla para continuar...

================================================
📋 TEST 1: LISTAR TODAS LAS CUENTAS (GET)
================================================
Endpoint: GET http://localhost:8080/cuentas

HTTP Status: 200

[
  {
    "numero": "1000000001",
    "titular": "Juan Pérez",
    "saldo": 5000.00,
    "tipoCuenta": "AHORRO"
  },
  ...
]

✅ Esperado: Array con cuentas pre-cargadas
   - Juan Pérez (1000000001)
   - María López (1000000002)
   - Carlos Ruiz (1000000003)

▶️  Presiona cualquier tecla para continuar...

================================================
📋 TEST 7: ELIMINAR CUENTA (DELETE)
================================================
Endpoint: DELETE http://localhost:8080/cuentas/1000000003
          (Eliminando cuenta de Carlos Ruiz)

HTTP Status: 204

(Sin contenido en el body)

✅ Esperado: HTTP 204 No Content (sin body)

▶️  Presiona cualquier tecla para continuar...
```

---

## 📁 Revisar Resultados

### Ver archivo de resultados

```bash
# Listar archivos generados
ls -lh resultados-pruebas-*.txt

# Ver el último archivo generado
cat resultados-pruebas-*.txt | tail
```

### Ver archivo específico

```bash
cat resultados-pruebas-20251030-214530.txt
```

### Buscar errores

```bash
grep -i "error\|404" resultados-pruebas-*.txt
```

---

## 🛠️ Personalización

### Cambiar URL del API

Edita la variable al inicio del script:

```bash
API_URL="http://localhost:8081/cuentas"  # Puerto diferente
```

### Agregar tus propios tests

Copia y modifica un bloque existente:

```bash
log_and_display "================================================"
log_and_display "📋 TEST XX: TU NUEVO TEST"
log_and_display "================================================"
log_and_display "Endpoint: POST $API_URL"
log_and_display ""

make_request "POST" "$API_URL" '{
  "numero": "1000000099",
  "titular": "Tu Nombre",
  "saldo": 10000.00,
  "tipoCuenta": "AHORRO"
}'

log_and_display ""
log_and_display "✅ Esperado: ..."
log_and_display ""
pause  # Espera interactiva
```

---

## 🛠️ Solución de Problemas

### Error: "curl: command not found"

**Causa:** curl no está instalado

**Solución:**
```bash
# macOS
brew install curl

# Windows (Git Bash ya incluye curl)
# Si no funciona, reinstala Git Bash

# Linux
sudo apt-get install curl
```

### Error: "jq: command not found"

**Causa:** jq no está instalado (opcional)

**Solución:**
- Los scripts funcionan sin jq, pero el JSON no se formatea
- Instálalo con las instrucciones de la sección Prerequisitos

### Error: "No se puede conectar al servidor"

**Causa:** Quarkus no está corriendo

**Solución:**
```bash
cd cuentas-service
./mvnw quarkus:dev
```

### Error: "Permission denied"

**Causa:** Falta permisos de ejecución

**Solución:**
```bash
chmod +x test-cuentas-api.sh
```

### TEST 7 da HTTP 404 en vez de 204

**Causa:** Carlos Ruiz ya fue eliminado en una ejecución anterior

**Solución:**
```bash
# Reiniciar Quarkus (Ctrl+C en su terminal)
./mvnw quarkus:dev

# Ejecutar script nuevamente
./test-cuentas-api.sh
```

---

## 📚 Recursos Adicionales

### Swagger UI (Pruebas Interactivas)
```
http://localhost:8080/q/swagger-ui
```

### Dev UI (Panel de Desarrollo)
```
http://localhost:8080/q/dev
```

### OpenAPI Spec (Especificación)
```
http://localhost:8080/q/openapi
```

---

## 💡 Tips y Mejores Prácticas

1. **Reinicia Quarkus antes de cada ejecución del script** para resultados consistentes
2. **Guarda los archivos de resultados** para comparar comportamiento entre versiones
3. **Usa las pausas interactivas** para explicar cada test en clase
4. **Agrega tus propios tests** según necesites probar casos específicos
5. **Revisa los códigos HTTP** - son parte esencial del aprendizaje REST

---

## 🎓 Uso en Clases

### Para Instructores

1. **Demo en clase:** El script interactivo es perfecto para mostrar paso a paso
2. **Explicación:** Pausa en cada test para explicar qué está pasando
3. **Ejercicio:** Pide a estudiantes que modifiquen el script agregando nuevos tests
4. **Importante:** Siempre reinicia Quarkus antes de la demo para evitar inconsistencias

### Para Estudiantes

1. **Verifica tu trabajo:** Ejecuta el script después de implementar cada endpoint
2. **Debug:** Si un test falla, usa Swagger UI para verificar manualmente
3. **Aprende:** Lee el código del script para entender cómo usar curl
4. **Experimenta:** Modifica el script para agregar tus propias validaciones

---

## 🎯 Flujo Recomendado para Demos

```bash
# 1. Levantar Quarkus
cd cuentas-service
./mvnw quarkus:dev

# 2. En otra terminal, ejecutar script
./test-cuentas-api.sh

# 3. Presionar tecla en cada pausa para explicar

# 4. Para siguiente demo, reiniciar Quarkus (Ctrl+C)
./mvnw quarkus:dev

# 5. Ejecutar script nuevamente
./test-cuentas-api.sh
```

---

**🎉 ¡Listo para probar tu API!**

Recuerda que estos scripts son herramientas educativas. En producción, 
usarías frameworks de testing como JUnit, RestAssured, o Postman Collections.