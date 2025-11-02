# 🧪 Testing del Sistema de Préstamos

Script de pruebas automatizadas para validar todos los endpoints del microservicio de préstamos bancarios.

---

## 📋 Prerequisitos
```bash
# Verificar que tengas instalado:
bash --version
curl --version
jq --version

# Si no tienes jq:
# macOS:   brew install jq
# Ubuntu:  sudo apt-get install jq
# Windows: choco install jq
```

---

## 🚀 Uso

### 1. Arrancar el servidor
```bash
cd prestamos-service
./mvnw quarkus:dev
```

Espera a que veas: `Listening on: http://localhost:8080`

### 2. Ejecutar el script
```bash
chmod +x test-prestamos-completo.sh
./test-prestamos-completo.sh
```

---

## 🎯 Qué Prueba el Script

El script ejecuta **16 tests** organizados en 3 módulos:

### Módulo 1: CRUD de Clientes (8 tests)
- ✅ Crear 3 clientes peruanos (María, Carlos, Rosa)
- ✅ Validar rechazo DNI duplicado → HTTP 409
- ✅ Validar rechazo email duplicado → HTTP 409
- ✅ Listar todos los clientes
- ✅ Obtener cliente por ID
- ✅ Actualizar teléfono de cliente

### Módulo 2: Gestión de Préstamos (5 tests)
- ✅ Crear préstamo vehicular S/ 15,000 (24 meses)
- ✅ Crear préstamo personal S/ 8,000 (12 meses)
- ✅ Validar generación automática de cuotas
- ✅ Listar todos los préstamos
- ✅ Obtener préstamo específico con cuotas
- ✅ Listar préstamos por cliente

### Módulo 3: Sistema de Pagos (3 tests)
- ✅ Pagar cuota 1 del préstamo
- ✅ Pagar cuota 2 del préstamo
- ✅ Rechazar pago de cuota ya pagada → HTTP 409

---

## 📊 Salida Esperada
```
╔════════════════════════════════════════════════════════════════════════════╗
║  🏦  PRUEBAS INTERACTIVAS - SISTEMA DE PRÉSTAMOS BANCARIOS              ║
╚════════════════════════════════════════════════════════════════════════════╝

🔍 Verificando servidor... ✓ Online

▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓
  📋 MÓDULO 1: GESTIÓN DE CLIENTES (CRUD)
▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓

Test #1: Crear cliente María González ✓ PASS (HTTP 201)
Test #2: Crear cliente Carlos Huamán ✓ PASS (HTTP 201)
...

╔════════════════════════════════════════════════════════════════════════════╗
║  📊 RESUMEN DE EJECUCIÓN                                                ║
╚════════════════════════════════════════════════════════════════════════════╝

  🎉 ✓ TODOS LOS TESTS PASARON

  ✓ Tests Exitosos:  16 / 16
  ✗ Tests Fallidos:  0 / 16

  📄 Resultados guardados en: resultados-tests-2025-10-30_20-55-23.txt
```

---

## 📁 Archivos Generados

Después de ejecutar, se genera un archivo de texto con timestamp:
```
resultados-tests-2025-10-30_20-55-23.txt
```

**Características del archivo:**
- ✅ Sin códigos ANSI (texto limpio y legible)
- ✅ Cuotas resumidas (primeras 3 + últimas 3)
- ✅ Timestamp único en el nombre
- ✅ ~400 líneas (vs 1462 líneas sin resumen)
- ✅ Listo para compartir y documentar

---

## 🛠️ Troubleshooting

### Error: "servidor no disponible"
```bash
# Verificar que Quarkus esté corriendo
curl http://localhost:8080/q/health

# Si no responde, arrancar:
cd prestamos-service
./mvnw quarkus:dev
```

### Error: "jq: command not found"
```bash
# macOS
brew install jq

# Ubuntu/Debian
sudo apt-get install jq
```

### Error: "Permission denied"
```bash
chmod +x test-prestamos-completo.sh
```

### Tests fallando con 409 (Conflict)

**Causa:** Datos duplicados de ejecuciones anteriores

**Solución (H2):**
```bash
# Reiniciar Quarkus (presionar 'q', luego):
./mvnw quarkus:dev
```

**Solución (PostgreSQL):**
```sql
-- Conectar a psql
psql -U postgres -d postgres

-- Limpiar datos
DELETE FROM cuotas;
DELETE FROM prestamos;
DELETE FROM clientes;
```

---

## 🔗 URLs Útiles

Después de los tests:
```
Swagger UI:    http://localhost:8080/q/swagger-ui
Dev UI:        http://localhost:8080/q/dev
Health:        http://localhost:8080/q/health
Clientes:      http://localhost:8080/clientes
Préstamos:     http://localhost:8080/prestamos
```

---

## 📝 Endpoints Cubiertos
```
✓ GET    /clientes
✓ GET    /clientes/{id}
✓ POST   /clientes
✓ PUT    /clientes/{id}
✓ DELETE /clientes/{id}

✓ GET    /prestamos
✓ GET    /prestamos/{id}
✓ POST   /prestamos
✓ GET    /prestamos/cliente/{id}
✓ PUT    /prestamos/{id}/pagar-cuota/{n}
```

---

## ✅ Checklist

Antes de ejecutar:

- [ ] Quarkus arrancado (`./mvnw quarkus:dev`)
- [ ] PostgreSQL activo (o H2 configurado)
- [ ] `jq` instalado
- [ ] Script con permisos (`chmod +x`)