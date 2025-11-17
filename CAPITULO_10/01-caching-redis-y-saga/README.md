# 📚 Capítulo 10: Patrones y Herramientas Avanzadas para Microservicios

## Sistema E-Commerce con Patrón SAGA y Redis Cache

---

## 📋 Tabla de Contenidos

- [Descripción General](#-descripción-general)
- [Arquitectura del Sistema](#-arquitectura-del-sistema)
- [Tecnologías Utilizadas](#-tecnologías-utilizadas)
- [Requisitos Previos](#-requisitos-previos)
- [Instalación](#-instalación)
- [Ejecución del Sistema](#-ejecución-del-sistema)
- [Pruebas](#-pruebas)
- [Estructura del Proyecto](#-estructura-del-proyecto)
- [Endpoints Disponibles](#-endpoints-disponibles)
- [Troubleshooting](#-troubleshooting)
- [Limpieza](#-limpieza)

---

## 🎯 Descripción General

Este ejercicio implementa un **sistema de e-commerce completo** usando microservicios con dos patrones avanzados:

1. **Patrón SAGA** para transacciones distribuidas
2. **Redis Cache** para optimización de rendimiento

### ¿Qué aprenderás?

- ✅ Implementar el patrón SAGA con orquestación centralizada
- ✅ Manejar transacciones distribuidas entre múltiples servicios
- ✅ Implementar compensaciones automáticas en caso de fallos
- ✅ Usar Redis como cache para mejorar rendimiento
- ✅ Comunicación entre microservicios con REST
- ✅ Manejo de consistencia eventual

---

## 🏗️ Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENTE                                 │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    ORDER SERVICE (8080)                         │
│  ┌──────────────────────────────────────────────────────┐      │
│  │         SAGA ORCHESTRATOR                            │      │
│  │  1. Reservar Inventario → 2. Procesar Pago →        │      │
│  │  3. Confirmar Reserva → 4. Guardar Orden            │      │
│  │                                                      │      │
│  │  En caso de fallo: COMPENSACIÓN automática          │      │
│  └──────────────────────────────────────────────────────┘      │
│                                                                 │
│  Redis Cache (órdenes consultadas)                             │
└─────────────┬───────────────────────────┬───────────────────────┘
              │                           │
              ▼                           ▼
┌─────────────────────────┐   ┌─────────────────────────┐
│  INVENTORY SERVICE      │   │  PAYMENT SERVICE        │
│  (8081)                 │   │  (8082)                 │
│                         │   │                         │
│  • Gestión de stock     │   │  • Procesar pagos       │
│  • Reservas             │   │  • Validar métodos      │
│  • Confirmaciones       │   │  • Generar recibos      │
│                         │   │                         │
│  PostgreSQL             │   │  PostgreSQL             │
│  (inventory_db)         │   │  (payment_db)           │
└─────────────────────────┘   └─────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    INFRAESTRUCTURA                              │
│  • Redis (puerto 6379) - Cache                                  │
│  • PostgreSQL (puerto 5433) - 3 bases de datos                  │
└─────────────────────────────────────────────────────────────────┘
```

### Flujo del Patrón SAGA

#### **Caso Exitoso:**
```
1. Cliente → POST /api/orders
2. Order Service inicia SAGA
3. PASO 1: Reservar inventario (Inventory Service)
   ✓ Stock disponible, reserva exitosa
4. PASO 2: Procesar pago (Payment Service)
   ✓ Pago aprobado
5. PASO 3: Confirmar reserva (Inventory Service)
   ✓ Inventario actualizado
6. PASO 4: Guardar orden en BD
   ✓ Orden con status: COMPLETED
7. Cliente ← HTTP 201 + Orden completa
```

#### **Caso con Compensación:**
```
1. Cliente → POST /api/orders (stock insuficiente)
2. Order Service inicia SAGA
3. PASO 1: Reservar inventario (Inventory Service)
   ✗ Stock insuficiente → HTTP 409
4. COMPENSACIÓN automática iniciada
5. Rollback: Liberar recursos
6. Orden marcada como FAILED
7. Cliente ← HTTP 400 + Mensaje de error
```

---

## 🛠️ Tecnologías Utilizadas

| Tecnología | Versión | Uso |
|------------|---------|-----|
| **Java** | 21 | Lenguaje de programación |
| **Quarkus** | 3.28.5 | Framework de microservicios |
| **PostgreSQL** | 15 | Base de datos relacional |
| **Redis** | 7 | Cache en memoria |
| **Docker** | Latest | Contenedores para infraestructura |
| **Maven** | 3.9+ | Gestión de dependencias |
| **Hibernate ORM** | Incluido | ORM para base de datos |
| **REST Client** | Incluido | Comunicación entre servicios |

---

## ✅ Requisitos Previos

Antes de comenzar, asegúrate de tener instalado:

### Obligatorios:
- ☑️ **Java 21** o superior
  ```bash
  java -version
  # Debe mostrar: java version "21.x.x"
  ```

- ☑️ **Maven 3.9+**
  ```bash
  mvn -version
  # Debe mostrar: Apache Maven 3.9.x
  ```

- ☑️ **Docker Desktop** (corriendo)
  ```bash
  docker --version
  docker ps
  # Debe mostrar la lista de contenedores (puede estar vacía)
  ```

- ☑️ **cURL** (para pruebas)
  ```bash
  curl --version
  ```

### Opcionales:
- 🔹 **VS Code** con extensión "REST Client" (para usar test-api.http)
- 🔹 **jq** (para formatear JSON en terminal)
  ```bash
  # macOS
  brew install jq
  
  # Linux
  sudo apt-get install jq
  ```

---

## 📦 Instalación

### Paso 1: Clonar o descargar el proyecto

```bash
# Si tienes el proyecto en un repositorio:
git clone <URL_DEL_REPOSITORIO>
cd "CAPITULO 10"

# O navega a la carpeta del proyecto
cd /ruta/a/tu/proyecto/CAPITULO\ 10
```

### Paso 2: Verificar la estructura del proyecto

```bash
ls -la
```

Deberías ver:
```
CAPITULO 10/
├── docker-compose.yml
├── init-databases.sh
├── pom.xml
├── test-saga.sh
├── test-api.http
├── inventory-service/
│   ├── pom.xml
│   └── src/
├── payment-service/
│   ├── pom.xml
│   └── src/
└── order-service/
    ├── pom.xml
    └── src/
```

### Paso 3: Compilar todos los servicios

```bash
# Desde la raíz del proyecto (CAPITULO 10)
mvn clean package -DskipTests
```

**Salida esperada:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: 10-15 segundos
```

⚠️ **Si falla:** Verifica que tienes Java 21 y Maven 3.9+

---

## 🚀 Ejecución del Sistema

### Paso 1: Levantar la infraestructura (Docker)

```bash
# Desde la raíz del proyecto
docker-compose up -d
```

Esto levanta:
- **Redis** en puerto `6379`
- **PostgreSQL** en puerto `5433` con 3 bases de datos:
  - `orders_db`
  - `inventory_db`
  - `payment_db`

**Verificar que estén corriendo:**
```bash
docker ps
```

Deberías ver:
```
CONTAINER ID   IMAGE                  STATUS         PORTS
xxxxx          postgres:15-alpine     Up X minutes   0.0.0.0:5433->5432/tcp
xxxxx          redis:7-alpine         Up X minutes   0.0.0.0:6379->6379/tcp
```

⚠️ **Si no aparecen:** Ejecuta `docker-compose logs` para ver errores

---

### Paso 2: Insertar datos de prueba en la base de datos

```bash
# Insertar productos en inventory_db
docker exec -it postgres-db psql -U postgres -d inventory_db -c "
INSERT INTO products (productcode, name, stock, reservedstock, price, created_at, updated_at) 
VALUES 
  ('LAPTOP-001', 'Laptop HP Pavilion 15', 50, 0, 899.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('MOUSE-001', 'Mouse Logitech MX Master', 100, 0, 99.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('KEYBOARD-001', 'Teclado Mecánico', 80, 0, 79.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
"
```

**Salida esperada:**
```
INSERT 0 3
```

---

### Paso 3: Iniciar los 3 microservicios

**Necesitas 3 terminales separadas:**

#### Terminal 1 - Inventory Service (Puerto 8081)
```bash
cd inventory-service
mvn quarkus:dev
```

**Espera a ver:**
```
Listening on: http://localhost:8081
```

#### Terminal 2 - Payment Service (Puerto 8082)
```bash
cd payment-service
mvn quarkus:dev
```

**Espera a ver:**
```
Listening on: http://localhost:8082
```

#### Terminal 3 - Order Service (Puerto 8080)
```bash
cd order-service
mvn quarkus:dev
```

**Espera a ver:**
```
Listening on: http://localhost:8080
```

---

### Paso 4: Verificar que todo esté funcionando

En una **cuarta terminal**, ejecuta:

```bash
# Health check de los 3 servicios
curl http://localhost:8080/health
curl http://localhost:8081/health
curl http://localhost:8082/health
```

**Salida esperada para cada uno:**
```json
{
  "status": "UP",
  "checks": [...]
}
```

✅ **Si todos responden con `"status": "UP"`, el sistema está listo!**

---

## 🧪 Pruebas

### Opción 1: Script Automatizado (RECOMENDADO)

Este script prueba TODO el sistema de forma automática:

```bash
# Desde la raíz del proyecto (CAPITULO 10)
chmod +x test-saga.sh
./test-saga.sh
```

**El script ejecuta 11 pruebas:**
1. Health checks de los 3 servicios
2. Verificación de productos en inventario
3. Creación de orden exitosa (SAGA completa)
4. Medición de Redis Cache (3 consultas)
5. Creación de orden con stock insuficiente (compensación SAGA)
6. Verificación de rollback de inventario

**Salida esperada:**
```
╔════════════════════════════════════════════════════════════════╗
║                    RESULTADOS FINALES                          ║
╠════════════════════════════════════════════════════════════════╣
║ Total de pruebas:    11                                        ║
║ Pruebas exitosas:    11                                        ║
║ Pruebas fallidas:    0                                         ║
╚════════════════════════════════════════════════════════════════╝

✓ ¡TODAS LAS PRUEBAS PASARON EXITOSAMENTE! 🚀

Reporte guardado en: test-saga-report-YYYY-MM-DD-HHMMSS.txt
```

---

### Opción 2: Pruebas Manuales con cURL

#### Prueba 1: Crear una orden EXITOSA

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "paymentMethod": "credit_card",
    "items": [
      {"productCode": "LAPTOP-001", "quantity": 1},
      {"productCode": "MOUSE-001", "quantity": 2}
    ]
  }' | jq
```

**Salida esperada:**
```json
{
  "orderId": "a1b2c3d4-...",
  "userId": "user-123",
  "status": "COMPLETED",
  "totalAmount": 1099.97,
  "items": [...],
  "message": "Orden creada exitosamente"
}
```

#### Prueba 2: Consultar una orden (Cache)

```bash
# Primera consulta (Cache MISS - lenta)
time curl -s http://localhost:8080/api/orders/a1b2c3d4-...

# Segunda consulta (Cache HIT - más rápida)
time curl -s http://localhost:8080/api/orders/a1b2c3d4-...
```

La segunda consulta debería ser más rápida (cache funcionando).

#### Prueba 3: Orden con stock INSUFICIENTE (Compensación)

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-456",
    "paymentMethod": "credit_card",
    "items": [
      {"productCode": "LAPTOP-001", "quantity": 10000}
    ]
  }' | jq
```

**Salida esperada:**
```json
{
  "orderId": "x1y2z3...",
  "userId": "user-456",
  "status": "FAILED",
  "message": "Error al crear orden: ... stock insuficiente ..."
}
```

✅ **La compensación SAGA se ejecutó correctamente!**

---

### Opción 3: Usar test-api.http con VS Code

1. **Instalar extensión:** "REST Client" de Huachao Mao en VS Code
2. **Abrir:** `test-api.http`
3. **Click en "Send Request"** sobre cada petición
4. Ver respuestas en panel lateral

---

## 📁 Estructura del Proyecto

```
CAPITULO 10/
│
├── 📄 README.md                    # Este archivo
├── 📄 TEORIA.md                    # Conceptos teóricos (SAGA, Redis)
├── 📄 instructor.md                # Guía para el profesor
│
├── 🐳 docker-compose.yml           # Redis + PostgreSQL
├── 🔧 init-databases.sh            # Script de inicialización de BDs
├── 📦 pom.xml                      # Parent POM (multi-módulo)
│
├── 🧪 test-saga.sh                 # Script de pruebas automatizadas
├── 📝 test-api.http                # Pruebas manuales (VS Code)
│
├── 📂 order-service/               # Servicio de Órdenes (Orquestador SAGA)
│   ├── pom.xml
│   └── src/main/java/pe/banco/order/
│       ├── entity/
│       │   ├── Order.java
│       │   └── OrderItem.java
│       ├── dto/
│       │   ├── OrderRequestDTO.java
│       │   └── OrderResponseDTO.java
│       ├── saga/
│       │   └── OrderSagaOrchestrator.java    # ⭐ Lógica del SAGA
│       ├── client/
│       │   ├── InventoryClient.java          # REST Client
│       │   └── PaymentClient.java
│       ├── service/
│       │   └── OrderService.java
│       └── resource/
│           └── OrderResource.java
│
├── 📂 inventory-service/           # Servicio de Inventario
│   ├── pom.xml
│   └── src/main/java/pe/banco/inventory/
│       ├── entity/
│       │   └── Product.java
│       ├── dto/
│       │   └── ReservationRequestDTO.java
│       ├── service/
│       │   └── InventoryService.java
│       └── resource/
│           └── InventoryResource.java
│
└── 📂 payment-service/             # Servicio de Pagos
    ├── pom.xml
    └── src/main/java/pe/banco/payment/
        ├── entity/
        │   └── Payment.java
        ├── dto/
        │   └── PaymentRequestDTO.java
        ├── service/
        │   └── PaymentService.java
        └── resource/
            └── PaymentResource.java
```

---

## 🔌 Endpoints Disponibles

### Order Service (Puerto 8080)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/health` | Health check del servicio |
| `POST` | `/api/orders` | Crear nueva orden (inicia SAGA) |
| `GET` | `/api/orders/{id}` | Consultar orden por ID (usa cache) |
| `GET` | `/api/orders` | Listar todas las órdenes |

### Inventory Service (Puerto 8081)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/health` | Health check del servicio |
| `GET` | `/api/inventory/products` | Listar productos |
| `GET` | `/api/inventory/products/{code}` | Consultar producto |
| `POST` | `/api/inventory/reserve` | Reservar stock |
| `POST` | `/api/inventory/confirm` | Confirmar reserva |
| `POST` | `/api/inventory/cancel` | Cancelar reserva (compensación) |

### Payment Service (Puerto 8082)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/health` | Health check del servicio |
| `POST` | `/api/payments/process` | Procesar pago |
| `GET` | `/api/payments/{id}` | Consultar pago |

---

## 🔧 Troubleshooting

### Problema 1: "Port already in use"

**Error:**
```
Port 8080 seems to be in use by another process
```

**Solución:**
```bash
# Ver qué proceso usa el puerto
lsof -i :8080
lsof -i :8081
lsof -i :8082

# Matar el proceso
kill -9 <PID>

# O matar todos los procesos de Quarkus
pkill -f quarkus
```

---

### Problema 2: Docker no inicia

**Error:**
```
Cannot connect to the Docker daemon
```

**Solución:**
1. Abrir Docker Desktop
2. Esperar a que inicie completamente
3. Verificar: `docker ps`
4. Reintentar: `docker-compose up -d`

---

### Problema 3: "Connection refused" a PostgreSQL

**Error:**
```
Connection refused: localhost:5433
```

**Solución:**
```bash
# Ver logs de PostgreSQL
docker-compose logs postgres-db

# Reiniciar contenedor
docker-compose restart postgres-db

# Esperar 10 segundos y reintentar
```

---

### Problema 4: No hay productos en la base de datos

**Error:**
```
Producto no encontrado
```

**Solución:**
```bash
# Volver a insertar productos
docker exec -it postgres-db psql -U postgres -d inventory_db -c "
INSERT INTO products (productcode, name, stock, reservedstock, price, created_at, updated_at) 
VALUES 
  ('LAPTOP-001', 'Laptop HP Pavilion 15', 50, 0, 899.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('MOUSE-001', 'Mouse Logitech MX Master', 100, 0, 99.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('KEYBOARD-001', 'Teclado Mecánico', 80, 0, 79.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
"
```

---

### Problema 5: Redis Cache no funciona

**Síntomas:**
- Todas las consultas tienen la misma latencia
- No hay mejora en la segunda consulta

**Diagnóstico:**
```bash
# Verificar que Redis esté corriendo
docker ps | grep redis

# Conectarse a Redis y ver claves
docker exec -it redis-cache redis-cli
> KEYS *
> PING
> exit
```

**Solución:**
- Verificar que Order Service tenga configuración de Redis en `application.properties`
- Reiniciar Order Service

---

### Problema 6: Compilación falla

**Error:**
```
[ERROR] Failed to execute goal
```

**Solución:**
```bash
# Limpiar todo y recompilar
mvn clean
mvn clean install -U -DskipTests

# Si persiste, verificar versión de Java
java -version  # Debe ser 21+
```

---

## 🧹 Limpieza

### Detener los servicios

```bash
# En cada terminal con un servicio corriendo:
Ctrl + C
```

### Detener Docker

```bash
# Detener contenedores
docker-compose down

# Detener y eliminar volúmenes (borra datos)
docker-compose down -v
```

### Limpiar archivos compilados

```bash
# Desde la raíz del proyecto
mvn clean
```

---

## 📚 Archivos Adicionales

- **TEORIA.md**: Explicación profunda del patrón SAGA, Redis Cache, consistencia eventual, etc.
- **instructor.md**: Guía para el profesor con soluciones, puntos de evaluación, y extensiones del ejercicio.

---

## 🎓 Conceptos Clave Aprendidos

Después de completar este ejercicio, habrás implementado:

✅ **Patrón SAGA** con orquestación centralizada  
✅ **Transacciones distribuidas** sin 2PC (Two-Phase Commit)  
✅ **Compensaciones automáticas** en caso de fallo  
✅ **Redis Cache** para optimizar consultas  
✅ **Comunicación REST** entre microservicios  
✅ **Manejo de consistencia eventual**  
✅ **Circuit Breaker** y tolerancia a fallos  
✅ **Health checks** y monitoreo básico  

---

## 📞 Soporte

Si tienes problemas:

1. Revisa la sección **Troubleshooting** arriba
2. Verifica que cumples todos los **Requisitos Previos**
3. Ejecuta el script de pruebas: `./test-saga.sh`
4. Revisa los logs de los servicios en las terminales
5. Consulta el archivo **TEORIA.md** para entender los conceptos

---

## 🚀 Próximos Pasos

Una vez que domines este ejercicio, puedes:

1. **Agregar Grafana/Kibana** para monitoreo avanzado (Capítulo 10 - Parte 3)
2. **Implementar Event Sourcing** como alternativa a SAGA
3. **Agregar autenticación JWT** entre servicios
4. **Implementar Circuit Breaker** con Resilience4j
5. **Agregar trazabilidad distribuida** con OpenTelemetry
