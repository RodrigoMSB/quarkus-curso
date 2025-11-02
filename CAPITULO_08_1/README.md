# 🏦 Capítulo 8.1 - Microservicios Reales con Quarkus

Sistema de evaluación crediticia implementado con **4 microservicios independientes** que se comunican entre sí mediante REST API.

---

## 🎯 ¿Qué es esto?

Este ejercicio demuestra una **arquitectura de microservicios REAL**, donde cada servicio:
- ✅ Es un proyecto Quarkus independiente
- ✅ Tiene su propio puerto
- ✅ Se comunica con otros servicios vía HTTP
- ✅ Puede desplegarse, escalarse y actualizarse de forma independiente

**Diferencia con el Capítulo 8:**
- **Capítulo 8:** Todos los endpoints en el mismo proyecto (monolito con mocks)
- **Capítulo 8.1:** 4 proyectos separados, 4 puertos, comunicación HTTP real

---

## 🏗️ Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────┐
│                    Cliente (curl)                       │
└────────────────────┬────────────────────────────────────┘
                     │ HTTP POST
                     ↓
          ┌──────────────────────┐
          │ Evaluacion Service   │  Puerto 8080
          │    (Orquestador)     │
          └──────┬───────┬───────┘
                 │       │       
        ┌────────┘       │       └────────┐
        │                │                │
        ↓                ↓                ↓
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  Identidad   │  │    Bureau    │  │   Scoring    │
│   Service    │  │   Service    │  │   Service    │
│  Puerto 8082 │  │  Puerto 8081 │  │  Puerto 8083 │
└──────────────┘  └──────────────┘  └──────────────┘
     Valida           Consulta          Calcula
    identidad        historial          scoring
                     crediticio            ML
```

---

## 📦 Estructura del Proyecto

```
capitulo-8.1-microservicios-reales/
│
├── bureau-service/              # Puerto 8081
│   ├── pom.xml
│   └── src/main/java/pe/banco/bureau/
│       ├── model/
│       │   └── RespuestaBureau.java
│       └── resource/
│           └── BureauResource.java
│
├── identidad-service/           # Puerto 8082
│   ├── pom.xml
│   └── src/main/java/pe/banco/identidad/
│       ├── model/
│       │   └── RespuestaIdentidad.java
│       └── resource/
│           └── IdentidadResource.java
│
├── scoring-service/             # Puerto 8083
│   ├── pom.xml
│   └── src/main/java/pe/banco/scoring/
│       ├── model/
│       │   └── RespuestaScoring.java
│       └── resource/
│           └── ScoringResource.java
│
├── evaluacion-service/          # Puerto 8080 (Orquestador)
│   ├── pom.xml
│   └── src/main/java/pe/banco/evaluacion/
│       ├── client/              # REST Clients
│       │   ├── BureauClient.java
│       │   ├── IdentidadClient.java
│       │   └── ScoringClient.java
│       ├── model/               # DTOs
│       │   ├── SolicitudCredito.java
│       │   ├── ResultadoEvaluacion.java
│       │   ├── RespuestaBureau.java
│       │   ├── RespuestaIdentidad.java
│       │   └── RespuestaScoring.java
│       ├── service/
│       │   └── EvaluacionService.java
│       └── resource/
│           └── EvaluacionResource.java
│
├── test-microservicios.sh       # Script de pruebas
└── README.md                    # Este archivo
```

---

## 🚀 Requisitos Previos

- **Java 21** (o superior)
- **Maven 3.8+**
- **4 terminales** (una para cada microservicio)
- **cURL** o **Postman** para pruebas

---

## ⚙️ Instalación y Ejecución

### Paso 1: Levantar los Microservicios

**IMPORTANTE:** Debes levantar los 4 servicios en orden. Cada uno en su propia terminal.

#### Terminal 1 - Bureau Service
```bash
cd bureau-service
./mvnw clean quarkus:dev
```
✅ Espera a ver: `Listening on: http://localhost:8081`

---

#### Terminal 2 - Identidad Service
```bash
cd identidad-service
./mvnw clean quarkus:dev
```
✅ Espera a ver: `Listening on: http://localhost:8082`

---

#### Terminal 3 - Scoring Service
```bash
cd scoring-service
./mvnw clean quarkus:dev
```
✅ Espera a ver: `Listening on: http://localhost:8083`

---

#### Terminal 4 - Evaluacion Service (Orquestador)
```bash
cd evaluacion-service
./mvnw clean quarkus:dev
```
✅ Espera a ver: `Listening on: http://localhost:8080`

---

### Paso 2: Verificar que todos los servicios están activos

En una **quinta terminal**, ejecuta:

```bash
# Verificar Bureau Service
curl http://localhost:8081/api/bureau/health

# Verificar Identidad Service
curl http://localhost:8082/api/identidad/health

# Verificar Scoring Service
curl http://localhost:8083/api/scoring/health

# Verificar Evaluacion Service
curl http://localhost:8080/api/evaluacion/health
```

Si todos responden, ¡estás listo! 🎉

---

## 🧪 Ejecutar Pruebas

### Opción 1: Script Automatizado (Recomendado)

```bash
chmod +x test-microservicios.sh
./test-microservicios.sh
```

Este script ejecuta 4 pruebas y genera un archivo de resultados.

---

### Opción 2: Pruebas Manuales

#### Prueba 1: Solicitud Exitosa
```bash
curl -X POST "http://localhost:8080/api/evaluacion/credito" \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "12345678",
    "nombres": "Juan",
    "apellidos": "Perez Lopez",
    "montoSolicitado": 30000,
    "mesesPlazo": 24
  }'
```

**Respuesta esperada:**
```json
{
  "dni": "12345678",
  "decision": "APROBADO",
  "scoreTotal": 775,
  "montoAprobado": 30000.0,
  "mensaje": "Crédito aprobado exitosamente"
}
```

---

#### Prueba 2: Identidad Inválida
```bash
curl -X POST "http://localhost:8080/api/evaluacion/credito" \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "00012345",
    "nombres": "Usuario",
    "apellidos": "Suspendido",
    "montoSolicitado": 20000,
    "mesesPlazo": 12
  }'
```

**Respuesta esperada:**
```json
{
  "dni": "00012345",
  "decision": "RECHAZADO",
  "scoreTotal": 0,
  "motivoRechazo": "Identidad no válida o inactiva",
  "mensaje": "Crédito rechazado: Identidad no válida o inactiva"
}
```

---

#### Prueba 3: Cliente con Morosidad
```bash
curl -X POST "http://localhost:8080/api/evaluacion/credito" \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "12345679",
    "nombres": "Maria",
    "apellidos": "Garcia Ruiz",
    "montoSolicitado": 25000,
    "mesesPlazo": 36
  }'
```

**Respuesta esperada:**
```json
{
  "dni": "12345679",
  "decision": "RECHAZADO",
  "scoreTotal": 0,
  "motivoRechazo": "Presenta morosidad activa",
  "mensaje": "Crédito rechazado: Presenta morosidad activa"
}
```

---

#### Prueba 4: Monto Alto
```bash
curl -X POST "http://localhost:8080/api/evaluacion/credito" \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "87654320",
    "nombres": "Carlos",
    "apellidos": "Mendez Silva",
    "montoSolicitado": 100000,
    "mesesPlazo": 48
  }'
```

**Respuesta esperada:**
```json
{
  "dni": "87654320",
  "decision": "RECHAZADO",
  "scoreTotal": 575,
  "motivoRechazo": "Score insuficiente o recomendación negativa",
  "mensaje": "Crédito rechazado"
}
```

---

## 🔍 Observar la Comunicación entre Microservicios

Cuando ejecutas una solicitud, verás **logs en las 4 terminales** mostrando la comunicación:

**Terminal 4 (Evaluacion Service):**
```
🎯 ORQUESTADOR: Iniciando evaluación para DNI 12345678
   → Llamando a Identidad Service...
   → Llamando a Bureau Service...
   → Llamando a Scoring Service...
   ✅ DECISIÓN: APROBADO
```

**Terminal 2 (Identidad Service):**
```
🪪 Identidad Service: Validando DNI 12345678
```

**Terminal 1 (Bureau Service):**
```
🏦 Bureau Service: Consultando DNI 12345678
```

**Terminal 3 (Scoring Service):**
```
🧮 Scoring Service: Calculando para DNI 12345678, Monto: 30000.0
```

---

## 📊 Endpoints Disponibles

### Bureau Service (Puerto 8081)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/bureau/consulta/{dni}` | Consultar historial crediticio |
| GET | `/api/bureau/health` | Health check |

**Ejemplo directo:**
```bash
curl "http://localhost:8081/api/bureau/consulta/12345678" \
  -H "X-API-Key: BUREAU_API_KEY_12345"
```

---

### Identidad Service (Puerto 8082)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/identidad/validar?dni={dni}` | Validar identidad |
| GET | `/api/identidad/health` | Health check |

**Ejemplo directo:**
```bash
curl "http://localhost:8082/api/identidad/validar?dni=12345678" \
  -H "Authorization: Bearer TOKEN_RENIEC_67890"
```

---

### Scoring Service (Puerto 8083)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/scoring/calcular` | Calcular scoring |
| GET | `/api/scoring/health` | Health check |

**Ejemplo directo:**
```bash
curl -X POST "http://localhost:8083/api/scoring/calcular?dni=12345678&monto=30000&plazo=24"
```

---

### Evaluacion Service (Puerto 8080)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/evaluacion/credito` | Evaluar solicitud de crédito |
| GET | `/api/evaluacion/health` | Health check |

---

## 🎓 Conceptos Clave Aprendidos

### 1. Microservicios Independientes
Cada servicio es un proyecto Maven completo con:
- Su propio `pom.xml`
- Sus propias clases
- Su propio puerto
- Puede desplegarse independientemente

### 2. Comunicación HTTP entre Servicios
Los servicios se comunican mediante:
- **REST Clients** (`@RegisterRestClient`)
- **HTTP GET/POST**
- **JSON** como formato de intercambio

### 3. Orquestación
El `Evaluacion Service` actúa como **orquestador**:
- Recibe solicitudes del cliente
- Llama a los servicios necesarios
- Agrega resultados
- Retorna decisión final

### 4. Configuración Externalizada
Cada servicio tiene su propio `application.properties`:
```properties
# Bureau Service
quarkus.http.port=8081

# Evaluacion Service (configura URLs de servicios externos)
quarkus.rest-client.bureau-service.url=http://localhost:8081
quarkus.rest-client.identidad-service.url=http://localhost:8082
quarkus.rest-client.scoring-service.url=http://localhost:8083
```

### 5. Separación de Responsabilidades
- **Bureau Service:** Solo consulta historial crediticio
- **Identidad Service:** Solo valida identidad
- **Scoring Service:** Solo calcula scoring
- **Evaluacion Service:** Orquesta y decide

---

## 🔄 Comparación: Capítulo 8 vs 8.1

| Aspecto | Capítulo 8 | Capítulo 8.1 |
|---------|------------|--------------|
| **Proyectos** | 1 proyecto | 4 proyectos |
| **Puertos** | 1 puerto (8080) | 4 puertos (8080-8083) |
| **Servicios externos** | Mocks en el mismo proyecto | Proyectos independientes |
| **Comunicación** | Llamadas internas (mismo JVM) | HTTP entre servicios (red real) |
| **Despliegue** | Todo junto | Cada servicio por separado |
| **Escalabilidad** | Escala todo o nada | Escala servicios específicos |
| **Arquitectura** | Monolito con REST Clients | Microservicios distribuidos |

---

## 🚨 Troubleshooting

### Problema: "Connection refused" al hacer solicitud
**Causa:** Algún servicio no está levantado.

**Solución:** Verifica que los 4 servicios estén corriendo:
```bash
# Deben responder todos
curl http://localhost:8080/api/evaluacion/health
curl http://localhost:8081/api/bureau/health
curl http://localhost:8082/api/identidad/health
curl http://localhost:8083/api/scoring/health
```

---

### Problema: "Port already in use"
**Causa:** El puerto ya está ocupado.

**Solución:** Mata el proceso:
```bash
# Ver qué proceso usa el puerto 8081
lsof -i :8081

# Matar el proceso (reemplaza PID)
kill -9 <PID>
```

---

### Problema: No veo los logs de comunicación
**Causa:** Los servicios están en modo silencioso.

**Solución:** Los logs aparecen en la terminal de cada servicio. Asegúrate de tener las 4 terminales visibles.

---

## 💡 Ejercicios Adicionales

### Ejercicio 1: Agregar un 5to Microservicio
Crea un **"Notificaciones Service"** que:
- Reciba notificaciones del Evaluacion Service
- Envíe emails/SMS simulados
- Puerto: 8084

### Ejercicio 2: Implementar Retry en Evaluacion Service
Agrega `@Retry` a las llamadas de los REST Clients para manejar fallos temporales.

### Ejercicio 3: Agregar Base de Datos
Integra PostgreSQL en el Bureau Service para almacenar consultas reales.

### Ejercicio 4: Implementar Circuit Breaker
Usa `@CircuitBreaker` en el Evaluacion Service para protegerse de servicios caídos.

---

## 📚 Documentación Adicional

- **TEORIA.md** - Conceptos de microservicios y arquitectura distribuida
- **instructor.md** - Guía detallada para instructores
- **DIAGRAMAS.md** - Diagramas de arquitectura y secuencia

---

## 🎯 Conclusión

Este ejercicio demuestra una **arquitectura de microservicios REAL**:

✅ **4 servicios independientes**  
✅ **Comunicación HTTP entre servicios**  
✅ **Orquestación distribuida**  
✅ **Configuración externalizada**  
✅ **Separación de responsabilidades**  

**Esto SÍ es una arquitectura distribuida**, no un monolito con mocks.
