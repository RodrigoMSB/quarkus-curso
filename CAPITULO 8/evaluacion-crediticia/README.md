cat > README.md << 'EOF'
# 💳 Evaluación Crediticia - Microservicio con Quarkus

Sistema de evaluación crediticia distribuida que demuestra patrones de resiliencia y consumo de APIs REST con Quarkus.

## 📋 Descripción

Este microservicio orquesta la evaluación de solicitudes de crédito consultando múltiples servicios externos:
- **Servicio de Identidad** (validación RENIEC)
- **Bureau de Crédito** (historial crediticio)
- **Scoring Avanzado** (motor de Machine Learning)

Implementa patrones de **Fault Tolerance** para garantizar resiliencia ante fallos de servicios externos.

---

## 🎯 Objetivos Pedagógicos

- Consumir APIs REST con `@RegisterRestClient`
- Aplicar patrones de resiliencia: `@Retry`, `@Timeout`, `@Fallback`, `@CircuitBreaker`
- Configurar REST Clients de forma externalizada
- Validar datos con Bean Validation
- Orquestar múltiples servicios en arquitectura de microservicios

---

## 🏗️ Estructura del Proyecto
```
evaluacion-crediticia/
├── src/
│   └── main/
│       ├── java/pe/banco/evaluacion/crediticia/
│       │   ├── client/              # REST Clients (interfaces)
│       │   │   ├── BureauCreditoClient.java
│       │   │   ├── IdentidadClient.java
│       │   │   └── ScoringClient.java
│       │   │
│       │   ├── model/               # DTOs y modelos de datos
│       │   │   ├── SolicitudCredito.java
│       │   │   ├── ResultadoEvaluacion.java
│       │   │   ├── RespuestaBureau.java
│       │   │   ├── RespuestaIdentidad.java
│       │   │   └── RespuestaScoring.java
│       │   │
│       │   ├── resource/            # Endpoints REST
│       │   │   ├── EvaluacionResource.java
│       │   │   ├── BureauMockResource.java         # Mock para testing
│       │   │   ├── IdentidadMockResource.java      # Mock para testing
│       │   │   └── ScoringMockResource.java        # Mock para testing
│       │   │
│       │   └── service/             # Lógica de negocio
│       │       └── EvaluacionCrediticiaService.java
│       │
│       └── resources/
│           └── application.properties  # Configuración
│
├── pom.xml                          # Dependencias Maven
├── README.md                        # Este archivo
├── TEORIA.md                        # Teoría completa
├── instructor.md                    # Guía del instructor
└── test-evaluacion-crediticia.sh   # Script de pruebas
```

---

## 🚀 Requisitos Previos

- **Java 21** (o superior)
- **Maven 3.8+**
- **Terminal** con bash (Linux/Mac) o Git Bash (Windows)
- **Python 3** (para el script de pruebas)

---

## ⚙️ Instalación y Ejecución

### 1. Clonar/Descargar el proyecto
```bash
cd evaluacion-crediticia
```

### 2. Compilar el proyecto
```bash
./mvnw clean compile
```

### 3. Ejecutar en modo desarrollo
```bash
./mvnw quarkus:dev
```

La aplicación estará disponible en: `http://localhost:8080`

**Dev UI de Quarkus:** `http://localhost:8080/q/dev`

---

## 🧪 Ejecutar Pruebas

### Opción 1: Script automatizado (Recomendado)
```bash
# Dar permisos de ejecución
chmod +x test-evaluacion-crediticia.sh

# Ejecutar todas las pruebas
./test-evaluacion-crediticia.sh
```

Este script ejecuta 6 pruebas que demuestran:
1. ✅ Flujo exitoso completo
2. 🔄 Patrón @Retry con fallos temporales
3. ⏱️ Patrón @Timeout con servicios lentos
4. 🛡️ Patrón @Fallback con respuestas alternativas
5. ⚡ Patrón @CircuitBreaker con servicios caídos
6. 🚫 Validación de identidad inválida

### Opción 2: Prueba manual con cURL

**Solicitud exitosa:**
```bash
curl -X POST "http://localhost:8080/api/evaluacion/credito" \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "11122334456",
    "nombres": "Juan",
    "apellidos": "Perez Lopez",
    "montoSolicitado": 30000,
    "mesesPlazo": 24
  }'
```

**Respuesta esperada:**
```json
{
  "dni": "11122334456",
  "decision": "APROBADO",
  "scoreTotal": 775,
  "montoAprobado": 30000.0,
  "mensaje": "Crédito aprobado exitosamente"
}
```

---

## 🔧 Configuración

### application.properties
```properties
# Puerto del servidor
quarkus.http.port=8080

# REST Client - Bureau de Crédito
quarkus.rest-client.bureau-credito.url=http://localhost:8080

# REST Client - Servicio de Identidad
quarkus.rest-client.identidad.url=http://localhost:8080

# REST Client - Scoring Avanzado
quarkus.rest-client.scoring-avanzado.url=http://localhost:8080

# API Keys (en producción usar variables de entorno)
api.bureau.key=BUREAU_API_KEY_12345
api.identidad.token=Bearer TOKEN_RENIEC_67890
```

---

## 🧩 Patrones de Resiliencia Implementados

### @Retry - Reintentos Automáticos
```java
@Retry(maxRetries = 3, delay = 1, delayUnit = ChronoUnit.SECONDS)
public RespuestaBureau consultarBureau(String dni)
```
**Qué hace:** Reintenta hasta 3 veces si el servicio falla temporalmente.

---

### @Timeout - Límite de Tiempo
```java
@Timeout(value = 3, unit = ChronoUnit.SECONDS)
public RespuestaScoring calcularScoring(SolicitudCredito solicitud)
```
**Qué hace:** Cancela la operación si tarda más de 3 segundos.

---

### @Fallback - Respuesta Alternativa
```java
@Fallback(fallbackMethod = "scoringBasicoFallback")
public RespuestaScoring calcularScoring(SolicitudCredito solicitud)
```
**Qué hace:** Si falla, usa un método alternativo (scoring básico).

---

### @CircuitBreaker - Protección contra Servicios Caídos
```java
@CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 10)
public RespuestaScoring calcularScoring(SolicitudCredito solicitud)
```
**Qué hace:** Detecta servicios caídos y deja de intentar llamarlos temporalmente.

---

## 🎭 DNIs Especiales para Testing

El sistema reconoce códigos especiales en los DNIs para simular diferentes escenarios:

| DNI | Comportamiento |
|-----|----------------|
| `111XXXXX` (terminado en PAR) | ✅ Flujo exitoso completo |
| `222XXXXX` (terminado en PAR) | 🔄 Bureau falla 2 veces, activa @Retry |
| `333XXXXX` (terminado en PAR) | ⏱️ Scoring demora 5s, activa @Timeout + @Fallback |
| `444XXXXXXX` | 🛡️ Scoring falla siempre, activa @Fallback + @CircuitBreaker |
| `000XXXXXXX` | 🚫 Identidad inválida, rechaza inmediatamente |

**Nota:** Los DNIs deben terminar en número **PAR** para tener buen score en Bureau (sin morosidad).

---

## 📊 Endpoints Disponibles

### Endpoints Principales

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/evaluacion/credito` | Evaluar solicitud de crédito |
| GET | `/api/evaluacion/health` | Health check del servicio |

### Endpoints Mock (para testing)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/bureau/consulta/{dni}` | Consultar Bureau de Crédito |
| GET | `/api/identidad/validar?dni={dni}` | Validar identidad |
| POST | `/api/scoring/calcular` | Calcular scoring avanzado |

---

## 📚 Documentación Adicional

- **[TEORIA.md](TEORIA.md)** - Teoría completa sobre REST Client y Fault Tolerance
- **[instructor.md](instructor.md)** - Guía detallada para instructores
- **[Quarkus REST Client Guide](https://quarkus.io/guides/rest-client)**
- **[MicroProfile Fault Tolerance](https://microprofile.io/project/eclipse/microprofile-fault-tolerance)**

---

## 🐛 Troubleshooting

### Problema: "Error al serializar JSON"
**Solución:** Verifica que tengas la dependencia `quarkus-rest-jackson` en el `pom.xml`.

### Problema: "Annotations @Retry will have no effect"
**Solución:** Asegúrate de que los métodos con anotaciones de Fault Tolerance sean `public`.

### Problema: "Connection refused"
**Solución:** Verifica que la aplicación esté corriendo en `http://localhost:8080`.

### Problema: "No se ve el @Retry en acción"
**Solución:** Observa los logs en la consola donde corre `./mvnw quarkus:dev`. Verás mensajes como:
```
🔴 Bureau: Intento 1 - FALLA
🔴 Bureau: Intento 2 - FALLA
🟢 Bureau: Intento 3 - ÉXITO
```

---

## 🏆 Conceptos Clave Aprendidos

Tras completar este ejercicio, habrás aprendido:

✅ Consumir APIs REST externas con `@RegisterRestClient`  
✅ Manejar parámetros: `@PathParam`, `@QueryParam`, `@HeaderParam`  
✅ Aplicar `@Retry` para fallos temporales  
✅ Usar `@Timeout` para servicios lentos  
✅ Implementar `@Fallback` para respuestas alternativas  
✅ Configurar `@CircuitBreaker` para protección avanzada  
✅ Validar datos con Bean Validation (`@NotBlank`, `@Positive`)  
✅ Externalizar configuración con `application.properties`  
✅ Orquestar múltiples servicios en una arquitectura de microservicios  

---

## 👨‍💻 Autor

Ejercicio diseñado para el **Curso de Quarkus - Capítulo 8: Integración y Consumo de Servicios**

---

## 📝 Licencia

Material educativo para fines académicos de NETEC.
