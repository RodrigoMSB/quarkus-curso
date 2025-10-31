# 📊 Scoring Service - Sistema de Score Crediticio

**Capítulo 11: Microservicio de Análisis Crediticio**  
**CreditCore Banking System**

## 🎯 Descripción

Microservicio reactivo para cálculo de score crediticio empresarial. Analiza múltiples factores para determinar la elegibilidad crediticia de clientes corporativos.

### ✨ Características Principales

- ✅ **Algoritmo de Scoring Original**: Multi-factor (ingresos, industria, ratio deuda, antigüedad)
- ✅ **Programación Reactiva**: Quarkus Reactive con Mutiny (Uni/Multi)
- ✅ **Integración Reactiva**: REST Client con customer-service
- ✅ **Estrategias de Análisis**: Conservative, Balanced, Aggressive
- ✅ **Cache Redis**: Optimización de consultas frecuentes
- ✅ **Fault Tolerance**: Circuit Breaker, Retry, Timeout
- ✅ **Persistencia**: Histórico completo de scores calculados
- ✅ **Observabilidad**: Métricas, Health Checks, OpenAPI

## 🏗️ Arquitectura

```
┌─────────────────┐
│  API Gateway    │
│  (Keycloak JWT) │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│       SCORING SERVICE (8082)            │
│  ┌───────────────────────────────────┐  │
│  │  REST Resource (JAX-RS Reactive)  │  │
│  └──────────────┬────────────────────┘  │
│                 │                        │
│  ┌──────────────▼──────────────┐        │
│  │   ScoringService            │        │
│  │   - calculateScore()        │        │
│  │   - getScoreHistory()       │        │
│  │   - Algoritmo multi-factor  │        │
│  └──────────────┬──────────────┘        │
│                 │                        │
│        ┌────────┴─────────┐             │
│        │                  │             │
│        ▼                  ▼             │
│  ┌──────────┐      ┌──────────┐        │
│  │  Redis   │      │ Customer │        │
│  │  Cache   │      │ Service  │        │
│  └──────────┘      │ Client   │        │
│                    └──────────┘        │
│                          │              │
│  ┌───────────────────────▼───────────┐ │
│  │   ScoreHistory (Panache Reactive) │ │
│  └───────────────────────────────────┘ │
│                    │                    │
│                    ▼                    │
│             ┌──────────────┐            │
│             │  PostgreSQL  │            │
│             └──────────────┘            │
└─────────────────────────────────────────┘
         │
         ▼
┌─────────────────────┐
│ CUSTOMER SERVICE    │
│     (8081)          │
│ - Datos del cliente │
└─────────────────────┘
```

## 🧮 Algoritmo de Scoring

### Fórmula

```
Score Final = (Score Base × Estrategia) [0-1000]

donde:

Score Base = 
    (Income Score × 0.30) +
    (Industry Score × 0.25) +
    (Debt Ratio Score × 0.25) +
    (Company Age Score × 0.20)
```

### Factores del Score

#### 1️⃣ **Income Score** (30%) - Máx: 300 puntos
Evalúa la capacidad de pago basada en ingresos anuales.

```java
Escala logarítmica:
- S/ 10,000    → ~120 puntos
- S/ 100,000   → ~150 puntos
- S/ 1,000,000 → ~180 puntos
- S/ 10,000,000→ ~210 puntos
```

#### 2️⃣ **Industry Score** (25%) - Máx: 250 puntos
Riesgo por sector industrial.

| Industria      | Factor | Puntos |
|----------------|--------|--------|
| Technology     | 0.95   | 237    |
| Healthcare     | 0.90   | 225    |
| Finance        | 0.80   | 200    |
| Retail         | 0.70   | 175    |
| Construction   | 0.65   | 162    |
| Mining         | 0.50   | 125    |

#### 3️⃣ **Debt Ratio Score** (25%) - Máx: 250 puntos
Relación monto solicitado / ingresos anuales.

```
Ratio < 10%  → 250 puntos (excelente)
Ratio 10-20% → 200-250 puntos (bueno)
Ratio 20-30% → 150-200 puntos (aceptable)
Ratio 30-40% → 50-150 puntos (alto)
Ratio > 40%  → 0-50 puntos (muy alto)
```

#### 4️⃣ **Company Age Score** (20%) - Máx: 200 puntos
Antigüedad y estabilidad de la empresa.

```
< 1 año     → 50 puntos (startup)
1-3 años    → 100-120 puntos (crecimiento)
3-10 años   → 120-180 puntos (establecida)
> 10 años   → 180-200 puntos (consolidada)
```

### Estrategias

| Estrategia    | Multiplicador | Mín. Aprobación | Uso                    |
|---------------|---------------|-----------------|------------------------|
| CONSERVATIVE  | 0.85          | 700             | Banca tradicional      |
| BALANCED      | 1.00          | 550             | Enfoque equilibrado    |
| AGGRESSIVE    | 1.15          | 400             | Fintech, microcrédito  |

### Niveles de Riesgo

| Score      | Nivel      | Tasa Sugerida | Descripción        |
|------------|------------|---------------|---------------------|
| 800-1000   | EXCELLENT  | 8.5%          | Riesgo muy bajo     |
| 650-799    | GOOD       | 12.0%         | Riesgo bajo         |
| 500-649    | FAIR       | 18.0%         | Riesgo moderado     |
| 350-499    | POOR       | 25.0%         | Riesgo alto         |
| 0-349      | VERY_POOR  | 35.0%         | Riesgo muy alto     |

## 🚀 Inicio Rápido

### Prerrequisitos

1. **Java 21** instalado
2. **Docker** y **Docker Compose**
3. **Customer Service** corriendo en puerto 8081
4. **Keycloak** configurado en puerto 8080

### Paso 1: Levantar Infraestructura

```bash
# Levantar PostgreSQL y Redis
docker compose up -d

# Verificar que estén corriendo
docker compose ps
```

### Paso 2: Ejecutar Scoring Service

```bash
# Modo desarrollo (hot reload)
./mvnw quarkus:dev

# El servicio estará en: http://localhost:8082
```

### Paso 3: Ejecutar Tests

```bash
# Dar permisos de ejecución
chmod +x test-scoring-service.sh

# Ejecutar tests
./test-scoring-service.sh
```

## 📡 API Endpoints

### Autenticación

Todos los endpoints requieren JWT Bearer token:

```bash
# Obtener token
curl -X POST http://localhost:8080/realms/creditcore/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=scoring-service" \
  -d "client_secret=fgwIG77MYz0hLQyImFYyPskmL0nM3Dgi" \
  -d "username=admin-user" \
  -d "password=admin123"
```

### 1. Calcular Score

**POST** `/api/scoring/calculate`

```bash
curl -X POST http://localhost:8082/api/scoring/calculate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "requestedAmount": 150000.00,
    "loanTermMonths": 24,
    "strategy": "BALANCED",
    "notes": "Solicitud para expansión"
  }'
```

**Respuesta:**
```json
{
  "historyId": 8,
  "customerId": 1,
  "customerRuc": "XXXXXXX3456",
  "customerName": "TechPeru S.A.C.",
  "score": 820,
  "riskLevel": "EXCELLENT",
  "strategy": "BALANCED",
  "requestedAmount": 150000.00,
  "loanTermMonths": 24,
  "scoringFactors": {
    "incomeScore": 255,
    "industryScore": 237,
    "debtRatioScore": 225,
    "companyAgeScore": 103,
    "baseScore": 820,
    "strategyMultiplier": 1.0
  },
  "approved": true,
  "recommendation": "✅ APROBACIÓN RECOMENDADA. Score: 820 (Excelente). Perfil excelente, ofrecer mejores condiciones.",
  "maxRecommendedAmount": 1500000.00,
  "suggestedInterestRate": 8.5,
  "calculatedAt": "2024-10-25T16:30:00",
  "fromCache": false
}
```

### 2. Obtener Histórico

**GET** `/api/scoring/history/{customerId}`

```bash
curl http://localhost:8082/api/scoring/history/1 \
  -H "Authorization: Bearer $TOKEN"
```

### 3. Último Score (con Cache)

**GET** `/api/scoring/latest/{customerId}`

```bash
curl http://localhost:8082/api/scoring/latest/1 \
  -H "Authorization: Bearer $TOKEN"
```

## 🎓 Conceptos Pedagógicos

### 1. Programación Reactiva con Mutiny

```java
// ❌ Enfoque bloqueante tradicional
public ScoreResult calculateScore(ScoreRequest request) {
    CustomerData customer = customerClient.getById(request.getCustomerId());
    ScoreResult result = performCalculation(customer, request);
    scoreRepository.save(result);
    return result;
}

// ✅ Enfoque reactivo con Mutiny
public Uni<ScoreResult> calculateScore(ScoreRequest request) {
    return customerServiceClient.getCustomerById(request.getCustomerId())
        .onItem().transformToUni(customer -> 
            performScoringCalculation(customer, request))
        .onItem().transformToUni(result -> 
            saveToHistory(result, request));
}
```

**Ventajas:**
- ✅ No bloquea threads
- ✅ Mejor escalabilidad
- ✅ Composición de operaciones asíncronas
- ✅ Manejo elegante de errores

### 2. REST Client Reactive

```java
@RegisterRestClient
public interface CustomerServiceClient {
    
    @GET
    @Path("/{id}")
    @Timeout(10000)
    @Retry(maxRetries = 3, delay = 1000)
    @CircuitBreaker(requestVolumeThreshold = 5, failureRatio = 0.5)
    @Fallback(fallbackMethod = "getCustomerFallback")
    Uni<CustomerData> getCustomerById(@PathParam("id") Long customerId);
    
    default Uni<CustomerData> getCustomerFallback(Long customerId) {
        return Uni.createFrom().failure(
            new CustomerServiceException("Customer Service no disponible")
        );
    }
}
```

**Características:**
- ✅ Integración reactiva entre microservicios
- ✅ Propagación automática de JWT
- ✅ Fault tolerance integrado
- ✅ Fallback en caso de fallo

### 3. Cache con Redis

```java
@CacheResult(cacheName = "latest-scores")
public Uni<ScoreHistory> getLatestScore(Long customerId) {
    return ScoreHistory.findLatestByCustomerId(customerId);
}
```

**Beneficios:**
- ✅ Reduce llamadas a BD
- ✅ Mejora tiempo de respuesta
- ✅ TTL configurable (30 minutos)
- ✅ Invalidación automática

### 4. Panache Reactive

```java
@Entity
public class ScoreHistory extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Campos...
    
    public static Uni<List<ScoreHistory>> findByCustomerId(Long customerId) {
        return list("customerId = ?1 order by calculatedAt desc", customerId);
    }
    
    public static Uni<ScoreHistory> findLatestByCustomerId(Long customerId) {
        return find("customerId = ?1 order by calculatedAt desc", customerId)
            .firstResult();
    }
}
```

**Ventajas:**
- ✅ Menos boilerplate
- ✅ Active Record pattern
- ✅ Queries reactivas
- ✅ Tipado fuerte

## 🧪 Testing

El proyecto incluye un script completo de testing:

```bash
./test-scoring-service.sh
```

**Pruebas incluidas:**
1. ✅ Verificación de servicios
2. ✅ Autenticación JWT
3. ✅ Cálculo de score con estrategia BALANCED
4. ✅ Comparación de estrategias (CONSERVATIVE, BALANCED, AGGRESSIVE)
5. ✅ Detección de alto ratio deuda/ingreso
6. ✅ Consulta de histórico
7. ✅ Cache de últimos scores
8. ✅ Validaciones (Bean Validation)
9. ✅ Manejo de errores
10. ✅ OpenAPI y métricas

## 📊 Monitoreo

### Health Checks

```bash
# Health general
curl http://localhost:8082/q/health

# Liveness
curl http://localhost:8082/q/health/live

# Readiness
curl http://localhost:8082/q/health/ready
```

### Métricas (Prometheus)

```bash
# Todas las métricas
curl http://localhost:8082/q/metrics

# Métricas de aplicación
curl http://localhost:8082/q/metrics/application
```

### Swagger UI

Abrir en navegador:
```
http://localhost:8082/q/swagger-ui
```

## 🛠️ Tecnologías

| Tecnología               | Versión | Propósito                          |
|--------------------------|---------|-------------------------------------|
| Quarkus                  | 3.15.1  | Framework reactivo                  |
| Mutiny                   | -       | Programación reactiva               |
| Hibernate Reactive       | -       | ORM reactivo                        |
| Panache Reactive         | -       | Simplificación de persistencia      |
| PostgreSQL               | 16      | Base de datos                       |
| Redis                    | 7       | Cache distribuido                   |
| REST Client Reactive     | -       | Integración entre servicios         |
| SmallRye Fault Tolerance | -       | Circuit Breaker, Retry, Timeout     |
| Keycloak OIDC            | -       | Autenticación y autorización        |
| Bean Validation          | -       | Validaciones declarativas           |
| Micrometer               | -       | Métricas                            |
| SmallRye OpenAPI         | -       | Documentación API                   |

## 🎯 Casos de Uso

### Caso 1: Startup Tecnológica

```json
{
  "customerId": 4,
  "requestedAmount": 50000.00,
  "loanTermMonths": 18,
  "strategy": "AGGRESSIVE"
}
```

**Resultado:**
- Score: ~420 (POOR)
- Factores: Ingresos bajos, industria buena, poca antigüedad
- Recomendación: Aprobar con tasa alta (25%)

### Caso 2: Empresa Consolidada

```json
{
  "customerId": 1,
  "requestedAmount": 150000.00,
  "loanTermMonths": 24,
  "strategy": "CONSERVATIVE"
}
```

**Resultado:**
- Score: ~697 (GOOD)
- Factores: Buenos ingresos, bajo riesgo industrial, antigüedad media
- Recomendación: Aprobar con tasa competitiva (12%)

### Caso 3: Alto Endeudamiento

```json
{
  "customerId": 2,
  "requestedAmount": 250000.00,
  "loanTermMonths": 36,
  "strategy": "BALANCED"
}
```

**Resultado:**
- Score: ~550 (FAIR)
- Factores: Ratio deuda/ingreso > 40%
- Recomendación: Aprobar con precaución o reducir monto

## 🔍 Troubleshooting

### Customer Service no disponible

```
Error: Customer Service no disponible
```

**Solución:**
1. Verificar que customer-service esté corriendo en puerto 8081
2. Revisar logs del Circuit Breaker
3. Esperar 5 segundos para que el circuito se cierre

### Cache no funciona

```bash
# Verificar Redis
docker exec -it creditcore-redis-scoring redis-cli ping

# Ver claves en cache
docker exec -it creditcore-redis-scoring redis-cli KEYS "*"

# Limpiar cache
docker exec -it creditcore-redis-scoring redis-cli FLUSHALL
```

### BD no conecta

```bash
# Verificar PostgreSQL
docker exec -it creditcore-postgres-scoring pg_isready

# Ver logs
docker compose logs postgres-scoring
```

## 📚 Material Didáctico

### Para Alumnos

1. **Revisar el algoritmo** en `ScoringService.java`
2. **Experimentar con estrategias** modificando los multiplicadores
3. **Analizar flujo reactivo** con Mutiny
4. **Probar fault tolerance** deteniendo customer-service
5. **Observar cache** con Redis CLI

### Ejercicios Propuestos

1. Agregar nuevo factor: historial de pagos
2. Implementar scoring para personas naturales
3. Crear endpoint de simulación de cuota mensual
4. Agregar machine learning para predicción
5. Implementar webhook cuando score < 400

## 🏆 Mejores Prácticas

✅ **Usar Uni/Multi** para todas las operaciones asíncronas  
✅ **Implementar fault tolerance** en integraciones externas  
✅ **Cachear resultados** de operaciones costosas  
✅ **Validar inputs** con Bean Validation  
✅ **Loggear decisiones** de scoring para auditoría  
✅ **Documentar algoritmos** con comentarios claros  
✅ **Versionar cambios** en factores de scoring  
✅ **Monitorear métricas** de aprobación/rechazo

## 📞 Soporte

- Instructor: [email]
- Repositorio: [GitHub URL]
- Documentación Quarkus: https://quarkus.io/guides/

---

**Capítulo 11 - Scoring Service**  
*CreditCore Banking System - Material Educativo*  
*Curso Avanzado de Quarkus - Perú 2024*
