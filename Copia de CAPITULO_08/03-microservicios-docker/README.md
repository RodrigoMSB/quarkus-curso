# 🐳 Capítulo 8.2 - Microservicios Contenerizados con Docker

Sistema de evaluación crediticia implementado con **4 microservicios contenerizados** usando Docker y orquestados con Docker Compose.

---

## 🎯 ¿Qué es esto?

Este ejercicio demuestra una **arquitectura de microservicios production-ready** donde cada servicio:
- ✅ Está contenerizado con Docker
- ✅ Se ejecuta en un contenedor aislado
- ✅ Se comunica con otros contenedores vía red Docker
- ✅ Puede desplegarse, escalarse y actualizarse independientemente
- ✅ NO requiere Java instalado en tu máquina

**Diferencia con el Capítulo 8.1:**
- **Capítulo 8.1:** 4 proyectos, 4 terminales manuales, requiere Java instalado
- **Capítulo 8.2:** 4 contenedores Docker, 1 comando, NO requiere Java

---

## 🐳 ¿Qué es Docker?

**Docker** es una plataforma que permite empaquetar aplicaciones y todas sus dependencias en **contenedores**.

**Analogía:**
```
SIN DOCKER:
"Funciona en mi máquina" → Pero no en la de tu compañero
- Necesitas Java 21
- Necesitas Maven
- Necesitas configurar todo manualmente

CON DOCKER:
"Funciona en cualquier máquina que tenga Docker"
- NO necesitas Java
- NO necesitas Maven
- Todo está empaquetado en el contenedor
```

**Contenedor = Caja con todo lo necesario**
```
┌─────────────────────────┐
│  Contenedor Bureau      │
│                         │
│  ✅ Java 21 JRE        │
│  ✅ Aplicación         │
│  ✅ Dependencias       │
│  ✅ Configuración      │
│                         │
│  Listo para ejecutar    │
└─────────────────────────┘
```

---

## 🏗️ Arquitectura del Sistema

```
┌────────────────────────────────────────────────────────┐
│               Docker Engine                            │
│                                                        │
│  ┌───────────────────────────────────────────────┐   │
│  │     Red: microservices-network                │   │
│  │                                               │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐   │   │
│  │  │ Bureau   │  │Identidad │  │ Scoring  │   │   │
│  │  │Container │  │Container │  │Container │   │   │
│  │  │  :8081   │  │  :8082   │  │  :8083   │   │   │
│  │  └────▲─────┘  └────▲─────┘  └────▲─────┘   │   │
│  │       │             │             │          │   │
│  │       └─────────────┼─────────────┘          │   │
│  │                     │                        │   │
│  │            ┌────────▼────────┐               │   │
│  │            │   Evaluacion    │               │   │
│  │            │   Container     │               │   │
│  │            │     :8080       │               │   │
│  │            └─────────────────┘               │   │
│  └───────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────┘
         ↑
    Cliente (curl/Postman)
```

---

## 📦 Estructura del Proyecto

```
capitulo-8.2-microservicios-docker/
│
├── bureau-service/
│   ├── Dockerfile              ← Instrucciones para crear imagen
│   ├── .dockerignore           ← Archivos a ignorar
│   ├── pom.xml
│   └── src/...
│
├── identidad-service/
│   ├── Dockerfile
│   ├── .dockerignore
│   ├── pom.xml
│   └── src/...
│
├── scoring-service/
│   ├── Dockerfile
│   ├── .dockerignore
│   ├── pom.xml
│   └── src/...
│
├── evaluacion-service/
│   ├── Dockerfile
│   ├── .dockerignore
│   ├── pom.xml
│   └── src/...
│
├── docker-compose.yml          ← Orquestador maestro
├── test-microservicios-docker.sh
├── README.md
├── TEORIA.md
└── instructor.md
```

---

## 🚀 Requisitos Previos

### 1. Instalar Docker

#### **macOS:**
```bash
# Descargar Docker Desktop desde:
https://www.docker.com/products/docker-desktop

# Verificar instalación:
docker --version
docker-compose --version
```

#### **Linux (Ubuntu/Debian):**
```bash
# Instalar Docker
sudo apt-get update
sudo apt-get install docker.io docker-compose

# Agregar tu usuario al grupo docker
sudo usermod -aG docker $USER
newgrp docker

# Verificar instalación:
docker --version
docker-compose --version
```

#### **Windows:**
```bash
# Descargar Docker Desktop desde:
https://www.docker.com/products/docker-desktop

# Verificar instalación (PowerShell):
docker --version
docker-compose --version
```

### 2. Verificar que Docker está corriendo

```bash
docker ps
```

**Debería mostrar:**
```
CONTAINER ID   IMAGE   COMMAND   CREATED   STATUS   PORTS   NAMES
```

Si da error, asegúrate de que Docker Desktop esté abierto/corriendo.

---

## ⚙️ Instalación y Ejecución

### Paso 1: Clonar/Descargar el proyecto

```bash
cd capitulo-8.2-microservicios-docker
```

### Paso 2: Construir y Levantar los Contenedores

**UN SOLO COMANDO:**

```bash
docker-compose up --build
```

**¿Qué hace este comando?**
1. 🔨 **Build:** Construye las 4 imágenes Docker (tarda 2-5 minutos la primera vez)
2. 🌐 **Network:** Crea la red `microservices-network`
3. 🐳 **Up:** Levanta los 4 contenedores
4. ⏳ **Health checks:** Espera a que todos estén "healthy"
5. ✅ **Ready:** Los servicios están listos

**Salida esperada:**
```
[+] Building 180.3s (54/54) FINISHED
[+] Running 5/5
✔ Network microservices-network   Created
✔ Container bureau-service        Started
✔ Container identidad-service     Started
✔ Container scoring-service       Started
✔ Container evaluacion-service    Started

bureau-service      | Listening on: http://0.0.0.0:8081
identidad-service   | Listening on: http://0.0.0.0:8082
scoring-service     | Listening on: http://0.0.0.0:8083
evaluacion-service  | Listening on: http://0.0.0.0:8080
```

**Cuando veas los 4 "Listening on", ya está listo.** ✅

---

### Paso 3: Verificar que todo está corriendo

**En OTRA TERMINAL:**

```bash
docker-compose ps
```

**Deberías ver:**
```
NAME                 STATUS
bureau-service       Up (healthy)
identidad-service    Up (healthy)
scoring-service      Up (healthy)
evaluacion-service   Up (healthy)
```

---

## 🧪 Ejecutar Pruebas

### Opción 1: Script Automatizado (Recomendado)

```bash
chmod +x test-microservicios-docker.sh
./test-microservicios-docker.sh
```

Este script ejecuta 4 pruebas y genera un archivo de resultados.

---

### Opción 2: Prueba Manual

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

## 🎛️ Comandos Útiles de Docker

### Ver contenedores corriendo
```bash
docker-compose ps
```

### Ver logs de todos los servicios
```bash
docker-compose logs
```

### Ver logs de un servicio específico
```bash
docker-compose logs -f evaluacion-service
```

El `-f` es para "follow" (ver logs en tiempo real).

### Detener todos los contenedores
```bash
docker-compose down
```

### Detener Y eliminar volúmenes/redes
```bash
docker-compose down -v
```

### Levantar en background (modo daemon)
```bash
docker-compose up -d
```

### Reconstruir solo un servicio
```bash
docker-compose up --build bureau-service
```

### Entrar a un contenedor (para debugging)
```bash
docker exec -it bureau-service sh
```

### Ver imágenes Docker creadas
```bash
docker images
```

### Limpiar todo (imágenes, contenedores, redes)
```bash
docker system prune -a
```

⚠️ **Cuidado:** Esto elimina TODAS las imágenes no usadas, no solo las de este proyecto.

---

## 🔄 Escalar Servicios

**Escalar el Scoring Service a 3 instancias:**

```bash
docker-compose up --scale scoring-service=3
```

**Ver las 3 instancias:**
```bash
docker ps | grep scoring
```

**¿Para qué?** Si el Scoring recibe mucha carga, puedes escalarlo horizontalmente.

---

## 🆚 Diferencias: Sin Docker vs Con Docker

| Aspecto | Sin Docker (8.1) | Con Docker (8.2) |
|---------|------------------|------------------|
| **Requisitos** | Java 21 + Maven instalados | Solo Docker |
| **Comandos para levantar** | 4 terminales, 4 comandos | 1 terminal, 1 comando |
| **Tiempo de inicio** | ~30 segundos | ~3 minutos primera vez<br>~30 segundos siguientes |
| **Portabilidad** | "Funciona en mi máquina" | Funciona en cualquier máquina con Docker |
| **Aislamiento** | Compartido (mismo OS) | Aislado (cada contenedor) |
| **Escalabilidad** | Manual (levantar más procesos) | `docker-compose scale` |
| **Deploy** | Manual en cada servidor | `docker-compose up` en cualquier servidor |
| **Recursos** | Ligero | Medio (overhead de Docker) |

---

## 📊 Endpoints Disponibles

### Evaluacion Service (Puerto 8080)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/evaluacion/credito` | Evaluar solicitud de crédito |
| GET | `/api/evaluacion/health` | Health check |

### Bureau Service (Puerto 8081)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/bureau/consulta/{dni}` | Consultar historial crediticio |
| GET | `/api/bureau/health` | Health check |

### Identidad Service (Puerto 8082)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/identidad/validar?dni={dni}` | Validar identidad |
| GET | `/api/identidad/health` | Health check |

### Scoring Service (Puerto 8083)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/scoring/calcular` | Calcular scoring |
| GET | `/api/scoring/health` | Health check |

---

## 🐳 Explicación del Dockerfile

Cada servicio tiene un Dockerfile con **Multi-Stage Build**:

```dockerfile
# ETAPA 1: BUILD (imagen pesada con Maven)
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# ETAPA 2: RUNTIME (imagen ligera solo con JRE)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/quarkus-app/ ./
EXPOSE 8081
CMD ["java", "-jar", "quarkus-run.jar"]
```

**¿Por qué 2 etapas?**

| Etapa | Imagen Base | Tamaño | Propósito |
|-------|-------------|--------|-----------|
| **BUILD** | maven:3.9-eclipse-temurin-21 | ~650 MB | Compilar código |
| **RUNTIME** | eclipse-temurin:21-jre-alpine | ~170 MB | Ejecutar aplicación |

**Imagen final:** ~250 MB (en lugar de 650+ MB)

**Ventajas:**
- ✅ Imagen final más pequeña
- ✅ Más rápida de descargar
- ✅ Más segura (menos superficie de ataque)
- ✅ Solo contiene lo necesario para ejecutar

---

## 📡 Comunicación entre Contenedores

### DNS Interno de Docker

En Docker, los contenedores se llaman por **nombre**, no por localhost.

**En evaluacion-service/application.properties:**
```properties
# NO usa localhost porque están en contenedores diferentes
quarkus.rest-client.bureau-service.url=http://bureau-service:8081
quarkus.rest-client.identidad-service.url=http://identidad-service:8082
quarkus.rest-client.scoring-service.url=http://scoring-service:8083
```

**Docker automáticamente resuelve:**
- `bureau-service` → IP del contenedor Bureau
- `identidad-service` → IP del contenedor Identidad
- `scoring-service` → IP del contenedor Scoring

**Analogía:**
```
Sin Docker:
"Llama al 555-1234" (IP fija)

Con Docker:
"Llama a Juan" → Docker encuentra dónde está Juan
```

---

## 🔒 Redes Docker

**docker-compose.yml** crea una red privada:

```yaml
networks:
  microservices-network:
    driver: bridge
```

**¿Qué hace?**
- ✅ Aísla los contenedores del resto del sistema
- ✅ Solo los 4 servicios pueden comunicarse entre sí
- ✅ DNS automático (nombres → IPs)

**Ver la red:**
```bash
docker network ls
docker network inspect microservices-network
```

---

## 🚨 Troubleshooting

### Problema 1: "Cannot connect to Docker daemon"

**Síntoma:**
```
Cannot connect to the Docker daemon at unix:///var/run/docker.sock
```

**Causa:** Docker no está corriendo.

**Solución:**
- **macOS/Windows:** Abre Docker Desktop
- **Linux:** `sudo systemctl start docker`

---

### Problema 2: "Port already in use"

**Síntoma:**
```
Error starting userland proxy: listen tcp4 0.0.0.0:8080: bind: address already in use
```

**Causa:** Otro proceso usa el puerto.

**Solución:**
```bash
# Ver qué proceso usa el puerto
lsof -i :8080

# Matar el proceso
kill -9 <PID>

# O cambiar el puerto en docker-compose.yml
ports:
  - "9080:8080"  # Puerto externo 9080
```

---

### Problema 3: "Unhealthy" status

**Síntoma:**
```bash
docker-compose ps
# Muestra "unhealthy"
```

**Causa:** El health check falla.

**Solución:**
```bash
# Ver logs del servicio
docker-compose logs bureau-service

# Verificar manualmente
curl http://localhost:8081/api/bureau/health
```

---

### Problema 4: Build muy lento

**Síntoma:** Tarda 10+ minutos en construir.

**Causa:** Descarga todas las dependencias cada vez.

**Solución:** Las capas de Docker se cachean. La segunda vez será más rápido (~30 segundos).

**Forzar reconstrucción limpia:**
```bash
docker-compose build --no-cache
```

---

### Problema 5: "No space left on device"

**Síntoma:** Error al construir imágenes.

**Causa:** Docker llenó el disco.

**Solución:**
```bash
# Limpiar contenedores detenidos
docker container prune

# Limpiar imágenes no usadas
docker image prune -a

# Limpiar TODO (cuidado!)
docker system prune -a --volumes
```

---

## 🎓 Conceptos Clave Aprendidos

Al completar este ejercicio, habrás aprendido:

✅ **Qué es Docker** y por qué se usa  
✅ **Diferencia entre imagen y contenedor**  
✅ **Multi-stage builds** para optimizar tamaño  
✅ **Docker Compose** para orquestar múltiples servicios  
✅ **Redes Docker** para aislar y comunicar contenedores  
✅ **Health checks** para verificar estado  
✅ **DNS interno** de Docker  
✅ **Escalamiento horizontal** con `--scale`  
✅ **Portabilidad** ("funciona en cualquier máquina")  

---

## 📚 Documentación Adicional

- **TEORIA.md** - Conceptos profundos de Docker y Microservicios
- **instructor.md** - Guía detallada para instructores
- **DIAGRAMAS.md** - Diagramas de arquitectura

---

## 🎯 Comandos de Inicio Rápido

```bash
# Levantar todo
docker-compose up --build

# En otra terminal: Probar
curl -X POST http://localhost:8080/api/evaluacion/credito \
  -H "Content-Type: application/json" \
  -d '{"dni":"12345678","nombres":"Juan","apellidos":"Perez","montoSolicitado":30000,"mesesPlazo":24}'

# Detener todo
docker-compose down
```

---

## 🏆 Ventajas de Esta Arquitectura

| Ventaja | Explicación |
|---------|-------------|
| **Portabilidad** | Funciona en cualquier máquina con Docker |
| **Aislamiento** | Cada servicio en su propio contenedor |
| **Reproducibilidad** | Mismo comportamiento en dev, test y prod |
| **Escalabilidad** | Escalar servicios independientemente |
| **Versionado** | Cada imagen tiene su versión |
| **Deploy rápido** | `docker-compose up` en cualquier servidor |

---

## 📝 Notas Importantes

⚠️ **Primera ejecución:** Tarda 2-5 minutos descargando imágenes base y compilando.  
⚠️ **Ejecuciones siguientes:** ~30 segundos (usa caché de Docker).  
⚠️ **Imágenes Alpine:** Más pequeñas pero pueden tener limitaciones en algunas librerías.  
⚠️ **Recursos:** Docker consume RAM. Asegúrate de tener al menos 4GB libres.  

---

**¡Felicidades! Has dockerizado una arquitectura de microservicios completa.** 🐳🎉
