## 🚀 **CÓMO EJECUTAR LA APLICACIÓN - CAPÍTULO 7 (TasaCorp API)**

### **📋 PREREQUISITOS**
```bash
# Verificar Java 17+
java -version

# Verificar Maven
mvn -version

# Verificar Docker (solo para PROD con Vault)
docker --version
```

---

### **⚡ INICIO RÁPIDO - PERFIL DEV (Por Defecto)**

```bash
# Ejecutar en perfil DEV
./mvnw quarkus:dev
```

**Endpoints disponibles:**
```bash
# Ver configuración actual
curl http://localhost:8080/api/tasas/config

# Consultar tasa USD
curl http://localhost:8080/api/tasas/USD

# Convertir 1000 PEN a USD
curl http://localhost:8080/api/tasas/convertir/USD?monto=1000

# Health check
curl http://localhost:8080/api/tasas/health
```

---

### **🎯 EJECUTAR CON DIFERENTES PERFILES**

#### **PERFIL DEV (Desarrollo)**
```bash
./mvnw quarkus:dev
# o explícitamente:
./mvnw quarkus:dev -Dquarkus.profile=dev
```

**Características DEV:**
- ✅ Comisión: 0.0% (gratis)
- ✅ Límite transaccional: 999,999 (ilimitado)
- ✅ Cache: Deshabilitado
- ✅ Proveedor: MockProvider

---

#### **PERFIL TEST (Testing)**
```bash
./mvnw quarkus:dev -Dquarkus.profile=test
```

**Características TEST:**
- ✅ Comisión: 1.5%
- ✅ Límite transaccional: $1,000
- ✅ Cache: Habilitado (30 min)
- ✅ Proveedor: FreeCurrencyAPI

**Ejemplo de conversión:**
```bash
# Dentro del límite
curl "http://localhost:8080/api/tasas/convertir/USD?monto=500"

# Excede el límite
curl "http://localhost:8080/api/tasas/convertir/USD?monto=2000"
```

---

#### **PERFIL PROD (Producción con Vault)**

**1. Levantar HashiCorp Vault:**
```bash
# Levantar Vault con Docker
docker-compose up -d

# Verificar que está corriendo
docker ps
```

**2. Ejecutar la aplicación:**
```bash
./mvnw quarkus:dev -Dquarkus.profile=prod
```

**Características PROD:**
- ✅ Comisión: 2.5%
- ✅ Límite transaccional: $50,000
- ✅ Cache: Habilitado (15 min)
- ✅ Proveedor: PremiumProvider
- 🔐 API Key: Desde Vault (seguro)

**Ejemplo de conversión:**
```bash
# Dentro del límite
curl "http://localhost:8080/api/tasas/convertir/USD?monto=10000"

# Excede el límite
curl "http://localhost:8080/api/tasas/convertir/USD?monto=60000"
```

---

### **🧪 SCRIPTS DE PRUEBA AUTOMATIZADOS**

#### **Parte 1: Configuración Base**
```bash
# Terminal 1: Levantar app
./mvnw quarkus:dev

# Terminal 2: Ejecutar pruebas
./test-part1-config.sh
```

**Prueba (7 tests):**
- Configuración desde Properties
- Inyección con @ConfigProperty
- Variables de entorno
- System Properties
- Properties vs YAML

📄 **Genera log:** `test-part1-config-YYYY-MM-DD_HH-MM-SS.txt`

---

#### **Parte 2: Perfiles de Entorno**
```bash
# El script te guía para probar los 3 perfiles
./test-part2-profiles.sh
```

**Prueba (10 tests):**
- 3 tests en DEV
- 3 tests en TEST
- 4 tests en PROD (con Vault)

📄 **Genera log:** `test-part2-profiles-YYYY-MM-DD_HH-MM-SS.txt`

---

### **📊 COMPARATIVA DE PERFILES**

| Característica | DEV | TEST | PROD |
|----------------|-----|------|------|
| **Comisión** | 0.0% | 1.5% | 2.5% |
| **Límite Trans.** | 999,999 | $1,000 | $50,000 |
| **Cache** | ❌ | ✅ (30 min) | ✅ (15 min) |
| **Proveedor** | MockProvider | FreeCurrencyAPI | PremiumProvider |
| **API Key** | Hardcoded | Hardcoded | 🔐 Vault |
| **Ambiente** | desarrollo | testing | producción |

---

### **🌐 ENDPOINTS DISPONIBLES**

```bash
# Ver configuración actual del ambiente
GET http://localhost:8080/api/tasas/config

# Consultar tasa de una moneda (USD, EUR, MXN)
GET http://localhost:8080/api/tasas/{moneda}

# Convertir monto
GET http://localhost:8080/api/tasas/convertir/{moneda}?monto={monto}

# Health check
GET http://localhost:8080/api/tasas/health
```

---

### **💡 EJEMPLOS RÁPIDOS**

```bash
# Ver configuración
curl http://localhost:8080/api/tasas/config | jq

# Consultar tasa EUR
curl http://localhost:8080/api/tasas/EUR | jq

# Convertir 5000 PEN a MXN
curl "http://localhost:8080/api/tasas/convertir/MXN?monto=5000" | jq

# Health
curl http://localhost:8080/api/tasas/health
```

---

### **🐳 DOCKER COMPOSE (Para PROD)**

```bash
# Levantar Vault
docker-compose up -d

# Ver logs de Vault
docker-compose logs vault

# Detener Vault
docker-compose down

# Detener y limpiar volúmenes
docker-compose down -v
```

---

### **⚠️ NOTAS IMPORTANTES**

1. **DEV es el perfil por defecto** - no necesitas especificar nada
2. **TEST y PROD requieren `-Dquarkus.profile=xxx`**
3. **PROD requiere Docker** para Vault
4. Los scripts de prueba generan archivos `.txt` con timestamp
5. Puedes probar con `jq` para formatear JSON: `curl ... | jq`
