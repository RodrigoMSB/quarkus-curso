# 📖 Teoría: Microservicios Contenerizados con Docker

## 📑 Tabla de Contenidos

1. [¿Qué es Docker?](#1-qué-es-docker)
2. [Contenedores vs Máquinas Virtuales](#2-contenedores-vs-máquinas-virtuales)
3. [Imágenes vs Contenedores](#3-imágenes-vs-contenedores)
4. [Dockerfile Explicado](#4-dockerfile-explicado)
5. [Multi-Stage Builds](#5-multi-stage-builds)
6. [Docker Compose](#6-docker-compose)
7. [Redes Docker](#7-redes-docker)
8. [Por Qué Docker para Microservicios](#8-por-qué-docker-para-microservicios)
9. [Mejores Prácticas](#9-mejores-prácticas)
10. [Docker en Producción](#10-docker-en-producción)

---

## 1. ¿Qué es Docker?

### 1.1 Definición

**Docker** es una plataforma que permite empaquetar, distribuir y ejecutar aplicaciones en **contenedores**.

Un **contenedor** es un paquete ligero, portable y autónomo que contiene:
- ✅ Tu aplicación
- ✅ Todas sus dependencias (librerías, frameworks)
- ✅ Runtime (Java, Python, Node.js)
- ✅ Variables de entorno
- ✅ Configuración

---

### 1.2 Analogía: Contenedores de Carga

**Imagina el transporte marítimo antes de los contenedores estandarizados:**

```
ANTES (Sin contenedores):
┌────────────────────────────────────┐
│   Barco                            │
│                                    │
│  🍎🍎  📦📦  🏺🏺  ⚙️⚙️          │
│  Frutas Cajas Vasijas Maquinaria  │
│                                    │
│  Cada carga es diferente           │
│  Difícil de apilar                 │
│  Fácil de romper                   │
│  Lento de cargar/descargar         │
└────────────────────────────────────┘

DESPUÉS (Con contenedores):
┌────────────────────────────────────┐
│   Barco                            │
│                                    │
│  📦 📦 📦 📦                       │
│  📦 📦 📦 📦                       │
│                                    │
│  Todos tienen el mismo tamaño      │
│  Fácil de apilar                   │
│  Protegidos                        │
│  Rápido de cargar/descargar        │
└────────────────────────────────────┘
```

**Docker hace lo mismo con software:**

```
SIN DOCKER:
"En mi máquina funciona" ❌
- Necesitas instalar Java 21
- Necesitas Maven 3.9
- Configuración manual
- Diferentes entre dev/test/prod

CON DOCKER:
"Funciona en cualquier máquina" ✅
- Todo empaquetado en el contenedor
- Mismo comportamiento en dev/test/prod
- Una sola vez: docker-compose up
```

---

### 1.3 El Problema que Resuelve Docker

**Escenario típico SIN Docker:**

```
Desarrollador: "El código funciona perfectamente en mi laptop"
    ↓
QA: "No puedo ejecutarlo, me da error"
    ↓
Desarrollador: "¿Tienes Java 21? ¿Maven? ¿Las variables de entorno?"
    ↓
QA: "Tengo Java 17..."
    ↓
Desarrollador: "Ah, necesitas Java 21"
    ↓ (3 horas después instalando Java 21)
QA: "Ahora otro error..."
    ↓
Desarrollador: "¿Configuraste application.properties?"
    ↓
😫 FRUSTRACIÓN
```

**CON Docker:**

```
Desarrollador: "Aquí está la imagen Docker"
    ↓
QA: "docker-compose up"
    ↓
QA: "Ya está funcionando" ✅
    ↓
😊 FELICIDAD
```

---

## 2. Contenedores vs Máquinas Virtuales

### 2.1 Arquitectura

**MÁQUINAS VIRTUALES (VMs):**
```
┌─────────────────────────────────────┐
│         Servidor Físico             │
│  ┌───────────────────────────────┐  │
│  │  Sistema Operativo Host       │  │
│  │  (Ubuntu, Windows, macOS)     │  │
│  │  ┌─────────────────────────┐  │  │
│  │  │   Hypervisor (VMware)   │  │  │
│  │  │  ┌────────┐  ┌────────┐ │  │  │
│  │  │  │  VM 1  │  │  VM 2  │ │  │  │
│  │  │  │        │  │        │ │  │  │
│  │  │  │ Guest  │  │ Guest  │ │  │  │
│  │  │  │   OS   │  │   OS   │ │  │  │
│  │  │  │ (5 GB) │  │ (5 GB) │ │  │  │
│  │  │  │        │  │        │ │  │  │
│  │  │  │  App   │  │  App   │ │  │  │
│  │  │  └────────┘  └────────┘ │  │  │
│  │  └─────────────────────────┘  │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

**CONTENEDORES DOCKER:**
```
┌─────────────────────────────────────┐
│         Servidor Físico             │
│  ┌───────────────────────────────┐  │
│  │  Sistema Operativo Host       │  │
│  │  (Ubuntu, Windows, macOS)     │  │
│  │  ┌─────────────────────────┐  │  │
│  │  │    Docker Engine        │  │  │
│  │  │  ┌────────┐  ┌────────┐ │  │  │
│  │  │  │Cont. 1 │  │Cont. 2 │ │  │  │
│  │  │  │        │  │        │ │  │  │
│  │  │  │  App   │  │  App   │ │  │  │
│  │  │  │(250 MB)│  │(250 MB)│ │  │  │
│  │  │  └────────┘  └────────┘ │  │  │
│  │  └─────────────────────────┘  │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
     Comparten el kernel del Host
```

---

### 2.2 Comparación Detallada

| Característica | Máquinas Virtuales | Contenedores Docker |
|----------------|-------------------|---------------------|
| **Tamaño** | GBs (incluye OS completo) | MBs (solo app y deps) |
| **Tiempo de inicio** | Minutos | Segundos |
| **Aislamiento** | Completo (hardware virtual) | Proceso (namespaces) |
| **Rendimiento** | Overhead del hypervisor | Casi nativo |
| **Portabilidad** | Media (imagen grande) | Alta (imagen pequeña) |
| **Uso de recursos** | Alto (cada VM tiene su OS) | Bajo (comparten kernel) |
| **Mejor para** | Apps que requieren OS diferente | Microservicios, apps cloud-native |

---

### 2.3 Analogía de Edificios

**MÁQUINAS VIRTUALES = Casas individuales**
```
🏠 Casa 1         🏠 Casa 2         🏠 Casa 3
├─ Cimientos      ├─ Cimientos      ├─ Cimientos
├─ Plomería       ├─ Plomería       ├─ Plomería
├─ Electricidad   ├─ Electricidad   ├─ Electricidad
├─ Cocina         ├─ Cocina         ├─ Cocina
└─ Familia A      └─ Familia B      └─ Familia C

Cada casa tiene TODO duplicado
```

**CONTENEDORES = Departamentos**
```
🏢 Edificio
├─ Cimientos (compartidos)
├─ Plomería (compartida)
├─ Electricidad (compartida)
│
├─ 🚪 Depto 1 (Familia A)
├─ 🚪 Depto 2 (Familia B)
└─ 🚪 Depto 3 (Familia C)

Comparten infraestructura, pero están aislados
```

---

## 3. Imágenes vs Contenedores

### 3.1 ¿Qué es una Imagen Docker?

Una **imagen** es una plantilla de solo lectura que contiene:
- Sistema operativo base (Alpine, Ubuntu)
- Runtime (Java JRE)
- Tu aplicación compilada
- Dependencias

**Analogía: Imagen = Molde de galletas** 🍪

```
┌─────────────────┐
│  IMAGEN DOCKER  │  ← Plantilla (molde)
│   (Read-only)   │
└─────────────────┘
        │
        │ docker run
        ↓
┌─────────────────┐
│   CONTENEDOR    │  ← Instancia en ejecución (galleta)
│   (Read-write)  │
└─────────────────┘
```

**Una imagen puede crear MÚLTIPLES contenedores:**

```
Imagen: bureau-service
    ↓
    ├─ Contenedor 1 (bureau-service-1)
    ├─ Contenedor 2 (bureau-service-2)
    └─ Contenedor 3 (bureau-service-3)
```

---

### 3.2 Capas de una Imagen

Las imágenes Docker se construyen en **capas**:

```
┌─────────────────────────────────┐
│ Capa 4: CMD ["java", "-jar"...] │  ← Comando
├─────────────────────────────────┤
│ Capa 3: COPY quarkus-app ./     │  ← Tu app (250 MB)
├─────────────────────────────────┤
│ Capa 2: JRE 21 Alpine            │  ← Runtime (170 MB)
├─────────────────────────────────┤
│ Capa 1: Alpine Linux             │  ← Base OS (5 MB)
└─────────────────────────────────┘
```

**Ventaja del sistema de capas:**
- ✅ Si cambias tu app (Capa 3), solo esa capa se reconstruye
- ✅ Capas 1 y 2 se reutilizan (caché)
- ✅ Múltiples imágenes comparten capas base

---

### 3.3 Comandos Clave

```bash
# Ver imágenes locales
docker images

# Construir una imagen
docker build -t mi-app:1.0 .

# Descargar una imagen
docker pull eclipse-temurin:21-jre-alpine

# Eliminar una imagen
docker rmi mi-app:1.0

# Inspeccionar capas de una imagen
docker history mi-app:1.0
```

---

## 4. Dockerfile Explicado

### 4.1 ¿Qué es un Dockerfile?

Un **Dockerfile** es un archivo de texto con instrucciones para construir una imagen Docker.

**Analogía: Dockerfile = Receta de cocina** 🧑‍🍳

```
Receta de Pastel:
1. Toma un molde
2. Agrega harina
3. Agrega huevos
4. Hornea a 180°C

Dockerfile:
1. FROM ubuntu (base)
2. COPY app.jar (código)
3. RUN install deps (dependencias)
4. CMD run app (ejecutar)
```

---

### 4.2 Dockerfile del Bureau Service (Línea por Línea)

```dockerfile
# ═══════════════════════════════════════════
# ETAPA 1: BUILD (Construcción)
# ═══════════════════════════════════════════

# FROM: Define la imagen base
# maven:3.9-eclipse-temurin-21 incluye Maven + JDK 21
FROM maven:3.9-eclipse-temurin-21 AS builder
# ↑ AS builder: Da nombre a esta etapa para usarla después

# WORKDIR: Establece el directorio de trabajo
# Todos los comandos siguientes se ejecutan aquí
WORKDIR /build

# COPY: Copia archivos del host al contenedor
# Primero copia solo pom.xml
COPY pom.xml .

# RUN: Ejecuta comandos durante la construcción
# Descarga dependencias (esta capa se cachea)
RUN mvn dependency:go-offline

# Ahora copia el código fuente
COPY src ./src

# Compila la aplicación
# -DskipTests: No ejecuta tests (más rápido)
RUN mvn clean package -DskipTests

# ═══════════════════════════════════════════
# ETAPA 2: RUNTIME (Ejecución)
# ═══════════════════════════════════════════

# Imagen base más pequeña (solo JRE, no JDK)
# Alpine: Distribución Linux ultra ligera (5 MB)
FROM eclipse-temurin:21-jre-alpine

# Directorio de trabajo en el contenedor final
WORKDIR /app

# COPY --from=builder: Copia desde la etapa anterior
# Solo copia el JAR compilado, no el código fuente ni Maven
COPY --from=builder /build/target/quarkus-app/ ./

# EXPOSE: Documenta qué puerto usa (no lo abre)
EXPOSE 8081

# HEALTHCHECK: Verifica si el contenedor está saludable
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/api/bureau/health || exit 1

# CMD: Comando por defecto al iniciar el contenedor
# ["java", "-jar", "quarkus-run.jar"] formato exec (preferido)
CMD ["java", "-jar", "quarkus-run.jar"]
```

---

### 4.3 Instrucciones Principales

| Instrucción | Propósito | Ejemplo |
|-------------|-----------|---------|
| `FROM` | Imagen base | `FROM eclipse-temurin:21-jre-alpine` |
| `WORKDIR` | Directorio de trabajo | `WORKDIR /app` |
| `COPY` | Copiar archivos | `COPY target/app.jar .` |
| `RUN` | Ejecutar comando (build time) | `RUN mvn clean package` |
| `CMD` | Comando por defecto (runtime) | `CMD ["java", "-jar", "app.jar"]` |
| `EXPOSE` | Documentar puerto | `EXPOSE 8080` |
| `ENV` | Variable de entorno | `ENV JAVA_OPTS="-Xmx512m"` |
| `ARG` | Argumento de build | `ARG VERSION=1.0` |
| `HEALTHCHECK` | Verificación de salud | `HEALTHCHECK CMD curl localhost:8080/health` |

---

## 5. Multi-Stage Builds

### 5.1 ¿Qué es Multi-Stage Build?

**Multi-Stage Build** permite usar múltiples imágenes base en un solo Dockerfile, copiando solo lo necesario entre etapas.

**Problema que resuelve:**

```
SINGLE-STAGE (Malo):
┌─────────────────────────────┐
│  Imagen Final               │
│  ✅ Java JDK 21 (300 MB)   │
│  ✅ Maven (50 MB)           │
│  ✅ Código fuente           │
│  ✅ App compilada           │
│  ❌ Total: ~700 MB          │
└─────────────────────────────┘

MULTI-STAGE (Bueno):
Etapa 1 (se descarta):
┌─────────────────────────────┐
│  Maven + JDK                │
│  Compila la app             │
│  (Se usa y se tira)         │
└─────────────────────────────┘
        ↓ (copia solo JAR)
Etapa 2 (imagen final):
┌─────────────────────────────┐
│  Imagen Final               │
│  ✅ Java JRE 21 (170 MB)   │
│  ✅ App compilada (80 MB)  │
│  ✅ Total: ~250 MB          │
└─────────────────────────────┘
```

---

### 5.2 Ventajas

✅ **Imágenes más pequeñas:** 700 MB → 250 MB (64% reducción)  
✅ **Más rápidas de descargar:** Menos tiempo de deploy  
✅ **Más seguras:** Menos componentes = menos vulnerabilidades  
✅ **Mejor práctica:** Solo producción en imagen final  

---

### 5.3 Comparación

**Sin Multi-Stage:**
```dockerfile
FROM maven:3.9-eclipse-temurin-21
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package
CMD ["java", "-jar", "target/app.jar"]

# Imagen final incluye Maven, JDK, código fuente ❌
# Tamaño: 700+ MB
```

**Con Multi-Stage:**
```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/*.jar ./
CMD ["java", "-jar", "app.jar"]

# Imagen final: solo JRE + JAR ✅
# Tamaño: 250 MB
```

---

## 6. Docker Compose

### 6.1 ¿Qué es Docker Compose?

**Docker Compose** es una herramienta para definir y ejecutar aplicaciones multi-contenedor.

**Problema sin Docker Compose:**

```bash
# Crear red
docker network create mi-red

# Levantar servicio 1
docker run -d --name bureau --network mi-red -p 8081:8081 bureau-service

# Levantar servicio 2
docker run -d --name identidad --network mi-red -p 8082:8082 identidad-service

# Levantar servicio 3
docker run -d --name scoring --network mi-red -p 8083:8083 scoring-service

# Levantar servicio 4
docker run -d --name evaluacion --network mi-red -p 8080:8080 \
  -e BUREAU_URL=http://bureau:8081 \
  -e IDENTIDAD_URL=http://identidad:8082 \
  -e SCORING_URL=http://scoring:8083 \
  evaluacion-service

# 😫 Muchos comandos, difícil de mantener
```

**Con Docker Compose:**

```bash
docker-compose up
# 😊 Un solo comando
```

---

### 6.2 Anatomía del docker-compose.yml

```yaml
version: '3.8'  # Versión del formato (obsoleto en nuevas versiones)

services:  # Define los contenedores

  bureau-service:  # Nombre del servicio
    build:  # Construir desde Dockerfile
      context: ./bureau-service  # Dónde está el Dockerfile
      dockerfile: Dockerfile     # Nombre del Dockerfile
    
    container_name: bureau-service  # Nombre del contenedor
    
    ports:  # Mapeo de puertos: HOST:CONTENEDOR
      - "8081:8081"
    
    networks:  # Redes a las que se conecta
      - microservices-network
    
    healthcheck:  # Verificación de salud
      test: wget --spider http://localhost:8081/health || exit 1
      interval: 10s  # Cada 10 segundos
      timeout: 5s    # Timeout de 5 segundos
      retries: 5     # 5 intentos antes de marcar unhealthy
      start_period: 30s  # Espera 30s antes de empezar checks

  evaluacion-service:
    build:
      context: ./evaluacion-service
    
    container_name: evaluacion-service
    
    ports:
      - "8080:8080"
    
    networks:
      - microservices-network
    
    environment:  # Variables de entorno
      - BUREAU_SERVICE_URL=http://bureau-service:8081
      - IDENTIDAD_SERVICE_URL=http://identidad-service:8082
    
    depends_on:  # Dependencias (espera a que estén healthy)
      bureau-service:
        condition: service_healthy
      identidad-service:
        condition: service_healthy
      scoring-service:
        condition: service_healthy

networks:  # Define las redes
  microservices-network:
    driver: bridge  # Tipo de red
    name: microservices-network  # Nombre explícito
```

---

### 6.3 Comandos Esenciales

```bash
# Construir y levantar todos los servicios
docker-compose up --build

# Levantar en background (daemon)
docker-compose up -d

# Detener todos los servicios
docker-compose down

# Ver logs de todos los servicios
docker-compose logs

# Ver logs de un servicio específico (follow)
docker-compose logs -f bureau-service

# Ver estado de los servicios
docker-compose ps

# Escalar un servicio
docker-compose up --scale scoring-service=3

# Reconstruir un servicio específico
docker-compose build bureau-service

# Reiniciar un servicio
docker-compose restart bureau-service

# Ejecutar comando en un servicio
docker-compose exec bureau-service sh
```

---

## 7. Redes Docker

### 7.1 ¿Por Qué Redes en Docker?

**Sin red personalizada:**
- Contenedores no pueden verse por nombre
- Necesitas IPs estáticas (frágil)
- No hay aislamiento

**Con red personalizada:**
- DNS automático (nombres → IPs)
- Aislamiento de otros contenedores
- Comunicación segura

---

### 7.2 Tipos de Redes Docker

| Tipo | Descripción | Uso |
|------|-------------|-----|
| **bridge** | Red privada en el host | Desarrollo local (default) |
| **host** | Comparte red del host | Alto rendimiento |
| **overlay** | Comunicación entre hosts | Docker Swarm, Kubernetes |
| **none** | Sin red | Contenedores aislados |

**Nuestro caso: bridge**

```
Host (tu máquina)
└─ Red: microservices-network (bridge)
    ├─ bureau-service (172.18.0.2)
    ├─ identidad-service (172.18.0.3)
    ├─ scoring-service (172.18.0.4)
    └─ evaluacion-service (172.18.0.5)
```

---

### 7.3 DNS Interno

Docker tiene un **DNS interno** que resuelve nombres de contenedores:

```
evaluacion-service quiere llamar a bureau-service

1. evaluacion-service hace: 
   HTTP GET http://bureau-service:8081/api/bureau/health

2. Docker DNS resuelve:
   bureau-service → 172.18.0.2

3. Llega al contenedor correcto
```

**Ventaja:** No necesitas saber la IP, solo el nombre.

---

### 7.4 Comandos de Red

```bash
# Ver redes
docker network ls

# Inspeccionar una red
docker network inspect microservices-network

# Ver qué contenedores están en una red
docker network inspect microservices-network | grep Name

# Crear una red
docker network create mi-red-custom

# Conectar contenedor a red
docker network connect mi-red-custom bureau-service

# Desconectar
docker network disconnect mi-red-custom bureau-service
```

---

## 8. Por Qué Docker para Microservicios

### 8.1 Los 12 Factores de Aplicaciones Cloud-Native

Docker cumple con **The Twelve-Factor App** (metodología para apps modernas):

| Factor | Cómo Docker lo cumple |
|--------|----------------------|
| **I. Codebase** | Una imagen = un repo |
| **II. Dependencies** | Dependencias en la imagen, no en el host |
| **III. Config** | Variables de entorno |
| **IV. Backing services** | Servicios como contenedores |
| **V. Build, release, run** | `docker build`, `docker tag`, `docker run` |
| **VI. Processes** | Contenedores son stateless |
| **VII. Port binding** | EXPOSE + ports en compose |
| **VIII. Concurrency** | Escalar con `--scale` |
| **IX. Disposability** | Contenedores arrancan/paran rápido |
| **X. Dev/prod parity** | Misma imagen en dev y prod |
| **XI. Logs** | stdout/stderr → `docker logs` |
| **XII. Admin processes** | `docker exec` |

---

### 8.2 Ventajas Específicas

**1. Aislamiento**
```
Sin Docker:
- Todos los servicios compiten por recursos
- Conflict de puertos
- Una falla afecta a todos

Con Docker:
- Cada servicio en su contenedor
- Recursos limitados por contenedor
- Falla aislada
```

**2. Portabilidad**
```
Sin Docker:
Desarrollador → QA → Staging → Producción
Cada ambiente es diferente ❌

Con Docker:
Misma imagen en todos los ambientes ✅
```

**3. Escalabilidad**
```bash
# Tráfico alto en Scoring?
docker-compose up --scale scoring-service=5

# Scaling horizontal instantáneo
```

**4. Deploy rápido**
```
Sin Docker:
1. SSH al servidor
2. Instalar Java
3. Instalar deps
4. Copiar JAR
5. Configurar systemd
6. Restart
⏱️ 30 minutos

Con Docker:
1. docker-compose pull
2. docker-compose up -d
⏱️ 2 minutos
```

---

### 8.3 Casos de Uso Ideales

✅ **Microservicios** (este ejercicio)  
✅ **CI/CD pipelines** (testing automatizado)  
✅ **Ambientes de desarrollo** (onboarding rápido)  
✅ **Multi-tenant SaaS** (aislamiento por cliente)  
✅ **Aplicaciones cloud-native** (AWS ECS, GKE, AKS)  

❌ **NO ideal para:**
- Aplicaciones con GUI pesada
- Sistemas que requieren hardware específico
- Monolitos muy grandes sin refactorizar

---

## 9. Mejores Prácticas

### 9.1 Dockerfile

#### ✅ **Usa imágenes oficiales**
```dockerfile
# ✅ Bien
FROM eclipse-temurin:21-jre-alpine

# ❌ Mal
FROM random-user/java:latest
```

#### ✅ **Minimiza capas**
```dockerfile
# ❌ Mal (3 capas)
RUN apt-get update
RUN apt-get install -y curl
RUN apt-get install -y wget

# ✅ Bien (1 capa)
RUN apt-get update && \
    apt-get install -y curl wget && \
    rm -rf /var/lib/apt/lists/*
```

#### ✅ **Usa .dockerignore**
```
# .dockerignore
target/
.git/
*.log
node_modules/
```

Evita copiar archivos innecesarios → imagen más pequeña.

#### ✅ **Multi-stage builds**
```dockerfile
# Siempre usa multi-stage para apps compiladas
FROM builder AS build
# ...compila...

FROM runtime
COPY --from=build /app.jar .
```

#### ✅ **Usuario no-root**
```dockerfile
# ✅ Mejor práctica de seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
CMD ["java", "-jar", "app.jar"]
```

#### ✅ **Orden de COPY para caché**
```dockerfile
# ✅ Bien (caché de deps)
COPY pom.xml .
RUN mvn dependency:go-offline  # ← Se cachea
COPY src ./src  # ← Cambios aquí no invalidan caché de arriba
RUN mvn package

# ❌ Mal (siempre reconstruye todo)
COPY . .
RUN mvn package
```

---

### 9.2 Docker Compose

#### ✅ **Usa health checks**
```yaml
healthcheck:
  test: curl -f http://localhost:8080/health || exit 1
  interval: 30s
  timeout: 10s
  retries: 3
```

#### ✅ **depends_on con condiciones**
```yaml
depends_on:
  database:
    condition: service_healthy  # Espera a que esté healthy
```

#### ✅ **Variables de entorno en archivo .env**
```bash
# .env
BUREAU_URL=http://bureau-service:8081
API_KEY=secret123
```

```yaml
# docker-compose.yml
environment:
  - BUREAU_URL=${BUREAU_URL}
  - API_KEY=${API_KEY}
```

#### ✅ **Recursos limitados**
```yaml
deploy:
  resources:
    limits:
      cpus: '0.5'
      memory: 512M
```

---

### 9.3 Seguridad

#### 🔒 **Escanea imágenes**
```bash
# Usa herramientas como Trivy
trivy image bureau-service:latest
```

#### 🔒 **No almacenes secretos en imágenes**
```dockerfile
# ❌ Mal
ENV API_KEY=secret123

# ✅ Bien
# Pasa secretos en runtime vía env vars o secrets
```

#### 🔒 **Imágenes Alpine cuando sea posible**
- Menos paquetes = menos vulnerabilidades
- Actualizaciones más frecuentes

#### 🔒 **Ejecuta como usuario no-root**
```dockerfile
USER appuser  # No root
```

---

## 10. Docker en Producción

### 10.1 Orquestadores

Para producción, Docker Compose NO es suficiente. Necesitas un **orquestador**:

| Orquestador | Descripción | Mejor para |
|-------------|-------------|------------|
| **Kubernetes** | Estándar de la industria | Grandes empresas, multi-cloud |
| **Docker Swarm** | Más simple que K8s | SMBs, equipos pequeños |
| **AWS ECS** | Integrado con AWS | Si ya usas AWS |
| **Nomad** | HashiCorp, simple | Alternativa a K8s |

---

### 10.2 De Docker Compose a Kubernetes

**Docker Compose (dev):**
```yaml
services:
  bureau-service:
    image: bureau-service:1.0
    ports:
      - "8081:8081"
```

**Kubernetes (prod):**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: bureau-service
spec:
  replicas: 3  # 3 instancias
  selector:
    matchLabels:
      app: bureau
  template:
    metadata:
      labels:
        app: bureau
    spec:
      containers:
      - name: bureau
        image: bureau-service:1.0
        ports:
        - containerPort: 8081
---
apiVersion: v1
kind: Service
metadata:
  name: bureau-service
spec:
  selector:
    app: bureau
  ports:
  - port: 8081
    targetPort: 8081
```

---

### 10.3 Registros de Imágenes

**En producción, necesitas un registry:**

| Registry | Descripción |
|----------|-------------|
| **Docker Hub** | Público/privado, oficial |
| **AWS ECR** | Integrado con AWS ECS/EKS |
| **GCR** | Google Container Registry |
| **Azure ACR** | Azure Container Registry |
| **Harbor** | Self-hosted, CNCF |

**Workflow típico:**
```bash
# 1. Build
docker build -t bureau-service:1.0 .

# 2. Tag con registry
docker tag bureau-service:1.0 myregistry.com/bureau-service:1.0

# 3. Push
docker push myregistry.com/bureau-service:1.0

# 4. Deploy en servidor
docker pull myregistry.com/bureau-service:1.0
docker run -d myregistry.com/bureau-service:1.0
```

---

### 10.4 Monitoreo y Logs

**Logging:**
```bash
# Docker logs (básico)
docker logs bureau-service

# ELK Stack (producción)
Elasticsearch + Logstash + Kibana

# Fluentd (alternativa)
Recolecta logs de contenedores → Elasticsearch
```

**Monitoreo:**
- **Prometheus + Grafana:** Métricas de contenedores
- **cAdvisor:** Uso de CPU, RAM por contenedor
- **Datadog/NewRelic:** APM completo

---

## 📊 Resumen de Conceptos Clave

| Concepto | Descripción | Importancia |
|----------|-------------|-------------|
| **Docker** | Plataforma de contenedores | ⭐⭐⭐⭐⭐ |
| **Imagen** | Plantilla read-only | ⭐⭐⭐⭐⭐ |
| **Contenedor** | Instancia en ejecución de imagen | ⭐⭐⭐⭐⭐ |
| **Dockerfile** | Receta para construir imagen | ⭐⭐⭐⭐⭐ |
| **Multi-Stage Build** | Optimización de tamaño | ⭐⭐⭐⭐⭐ |
| **Docker Compose** | Orquestador para dev | ⭐⭐⭐⭐⭐ |
| **Redes Docker** | Comunicación entre contenedores | ⭐⭐⭐⭐ |
| **Health Checks** | Verificación de estado | ⭐⭐⭐⭐ |
| **Volúmenes** | Persistencia de datos | ⭐⭐⭐ |

---

## 🎓 Conclusión

Docker transformó la manera de desarrollar, distribuir y ejecutar aplicaciones. Para microservicios, es **esencial** porque provee:

✅ **Aislamiento:** Cada servicio en su contenedor  
✅ **Portabilidad:** Misma imagen en dev/test/prod  
✅ **Escalabilidad:** Horizontal fácil  
✅ **Consistencia:** "Funciona en mi máquina" = "Funciona en producción"  

**En este ejercicio aprendiste:**
- ✅ Qué es Docker y por qué lo necesitas
- ✅ Diferencia entre imágenes y contenedores
- ✅ Cómo escribir Dockerfiles optimizados
- ✅ Multi-stage builds para reducir tamaño
- ✅ Docker Compose para orquestar servicios
- ✅ Redes Docker para comunicación
- ✅ Mejores prácticas de seguridad y rendimiento

**Siguiente paso:** Kubernetes para escalar a cientos de microservicios. 🚀

---

**Fin del documento teórico.**
