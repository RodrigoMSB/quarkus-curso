# Customer Service - CreditCore

Microservicio de gestión de clientes empresariales con cifrado, caché y validaciones.

## 📋 Descripción

Servicio RESTful para la gestión de clientes empresariales en el sistema CreditCore de originación de créditos. Implementa:

- ✅ **Panache Active Record** - Persistencia simplificada
- ✅ **Google Tink** - Cifrado de datos sensibles (RUC)
- ✅ **Redis Cache** - Caché de clientes frecuentes
- ✅ **REST Client** - Validación con SUNAT (simulada)
- ✅ **Fault Tolerance** - Circuit Breaker, Retry, Timeout
- ✅ **JWT + OIDC** - Seguridad con Keycloak
- ✅ **Bean Validation** - Validación de datos
- ✅ **OpenAPI** - Documentación automática
- ✅ **Micrometer** - Métricas y observabilidad

---

## 🛠️ Tecnologías

| Componente | Versión |
|------------|---------|
| Quarkus | 3.15.1 |
| Java | 17+ |
| PostgreSQL | 15 |
| Redis | 7 |
| Google Tink | 1.13.0 |
| Maven | 3.8+ |

---

## 🚀 Inicio Rápido

### 1. Pre-requisitos

```bash
# Verificar Java
java --version  # Debe ser 17+

# Verificar Maven
mvn --version

# Verificar Docker
docker --version
docker-compose --version
```

### 2. Levantar infraestructura

```bash
# Levantar PostgreSQL + Redis
docker-compose up -d postgres-customer redis

# Verificar que estén corriendo
docker-compose ps
```

### 3. Modo Desarrollo (Dev Mode)

```bash
# Arrancar con hot reload
./mvnw quarkus:dev

# O especificar perfil explícitamente
./mvnw quarkus:dev -Dquarkus.profile=dev
```

El servicio estará disponible en: `http://localhost:8081`

**Dev Mode incluye:**
- 🔄 Hot reload automático
- 🐘 PostgreSQL auto-iniciado (Dev Services)
- 📊 UI de desarrollo en `http://localhost:8081/q/dev`
- 📝 Swagger UI en `http://localhost:8081/q/swagger-ui`

---

## 📡 Endpoints Principales

### Sin Autenticación

```bash
# Health Check
GET /api/customers/health

# Listar clientes activos
GET /api/customers

# Listar por industria
GET /api/customers/industry/{industry}
```

### Con Autenticación (JWT)

```bash
# Crear cliente (ANALYST, ADMIN)
POST /api/customers

# Obtener por ID (Todos los roles)
GET /api/customers/{id}

# Buscar por RUC (ANALYST, APPROVER, ADMIN)
GET /api/customers/ruc/{ruc}

# Actualizar (ANALYST, ADMIN)
PUT /api/customers/{id}
```

---

## 🧪 Pruebas Automatizadas

### Script de pruebas

```bash
# Ejecutar todas las pruebas
./test-scripts.sh
```

### Pruebas unitarias/integración

```bash
# Ejecutar tests
./mvnw test

# Con cobertura
./mvnw verify

# Solo tests específicos
./mvnw test -Dtest=CustomerResourceTest
```

---

## 🐳 Docker

### Construcción

```bash
# Imagen JVM (rápida)
docker build -f Dockerfile.jvm -t customer-service:jvm .

# Imagen nativa (lenta pero óptima)
docker build -f Dockerfile.native -t customer-service:native .
```

### Ejecución completa

```bash
# Levantar todo el stack
docker-compose up

# Ver logs
docker-compose logs -f customer-service

# Detener
docker-compose down
```

---

## 🔐 Seguridad

### Cifrado con Google Tink

El RUC se cifra automáticamente antes de almacenarse:

```java
// En CustomerService.java
String encryptedRuc = encryption.encryptRuc(request.getRuc());
customer.setRuc(encryptedRuc);
```

**Ubicación de claves:**
- Desarrollo: `./keys/tink-keyset.json`
- Producción: Variable de entorno `ENCRYPTION_KEY_PATH`

### Roles JWT

| Rol | Permisos |
|-----|----------|
| **CUSTOMER** | Ver su propia información |
| **ANALYST** | Crear, actualizar, ver clientes |
| **APPROVER** | Ver clientes, buscar por RUC |
| **ADMIN** | Acceso total |
| **SYSTEM** | Endpoints internos (inter-servicios) |

---

## ⚙️ Configuración por Perfil

### DEV
```properties
# application-dev.properties
quarkus.hibernate-orm.database.generation=drop-and-create
quarkus.log.level=DEBUG
app.encryption.enabled=true
```

### TEST
```properties
# application-test.properties
quarkus.datasource.db-kind=h2  # Base de datos en memoria
app.encryption.enabled=false    # Deshabilitar para tests rápidos
```

### PROD
```properties
# application-prod.properties
quarkus.hibernate-orm.database.generation=validate  # NUNCA drop-and-create
quarkus.log.console.json=true                       # Logs estructurados
```

---

## 📊 Observabilidad

### Health Checks
```bash
curl http://localhost:8081/q/health
```

### Métricas (Prometheus format)
```bash
curl http://localhost:8081/q/metrics
```

### OpenAPI Spec
```bash
curl http://localhost:8081/q/openapi
```

### Swagger UI
Abre en navegador: `http://localhost:8081/q/swagger-ui`

---

## 🔧 Troubleshooting

### Error: "Could not connect to PostgreSQL"

```bash
# Verificar que PostgreSQL está corriendo
docker-compose ps postgres-customer

# Ver logs
docker-compose logs postgres-customer

# Reiniciar
docker-compose restart postgres-customer
```

### Error: "Redis connection refused"

```bash
# Verificar Redis
docker-compose ps redis

# Reiniciar
docker-compose restart redis
```

### Error: "Encryption initialization failed"

```bash
# Crear directorio de claves
mkdir -p keys

# El servicio creará automáticamente la clave en el primer arranque
```

---

## 📚 Documentación Adicional

- **TEORIA.md** - Conceptos profundos y arquitectura
- **instructor.md** - Guía para el profesor
- **Swagger UI** - http://localhost:8081/q/swagger-ui

---

## 🎓 Ejercicios Sugeridos

1. **Agregar nuevo campo cifrado**
   - Añadir campo `taxId` a Customer
   - Cifrarlo con Tink antes de persistir

2. **Implementar paginación**
   - Modificar `listActiveCustomers()` para soportar `?page=1&size=10`

3. **Agregar filtros avanzados**
   - Buscar por rango de ingresos
   - Buscar por categoría de riesgo

4. **Implementar auditoría**
   - Registrar quién y cuándo modificó cada cliente

5. **Cache warming**
   - Pre-cargar clientes más consultados al inicio

---

## 👥 Autor

Curso de Quarkus - Banca Peruana
Desarrollo de Microservicios Modernos

---

## 📄 Licencia

Material educativo - Uso académico
