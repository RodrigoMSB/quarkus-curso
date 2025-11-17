# 🐳 Teoría Práctica de Docker - Operaciones y Comandos

Este documento complementa TEORIA.md con aspectos **operacionales** y **prácticos** del día a día con Docker.

---

## 📑 Tabla de Contenidos

1. [Ciclo de Vida: Subir, Detener, Eliminar](#1-ciclo-de-vida-subir-detener-eliminar)
2. [Gestión de Contenedores](#2-gestión-de-contenedores)
3. [Gestión de Imágenes](#3-gestión-de-imágenes)
4. [Gestión de Redes](#4-gestión-de-redes)
5. [Gestión de Volúmenes](#5-gestión-de-volúmenes)
6. [Limpieza y Mantenimiento](#6-limpieza-y-mantenimiento)
7. [Comandos Esenciales Organizados](#7-comandos-esenciales-organizados)
8. [Troubleshooting Práctico](#8-troubleshooting-práctico)
9. [Mejores Prácticas Operacionales](#9-mejores-prácticas-operacionales)

---

## 1. Ciclo de Vida: Subir, Detener, Eliminar

### 1.1 Levantar Servicios

#### **Primera vez (construir + levantar):**
```bash
docker-compose up --build
```

**¿Qué hace?**
1. 🔨 **Build:** Construye las 4 imágenes Docker
2. 🌐 **Network:** Crea la red `microservices-network`
3. 🐳 **Containers:** Crea y arranca los 4 contenedores
4. ⏳ **Health checks:** Espera que todos estén "healthy"
5. 📊 **Logs:** Muestra logs en tiempo real

**Tiempo:** 2-5 minutos la primera vez

---

#### **Veces siguientes (solo levantar):**
```bash
docker-compose up
```

**¿Qué hace?**
- Usa imágenes ya construidas (caché)
- Crea contenedores nuevos
- Muestra logs en tiempo real

**Tiempo:** 30-60 segundos

---

#### **Levantar en background (modo daemon):**
```bash
docker-compose up -d
```

**¿Qué hace?**
- Levanta todo en background
- NO muestra logs en terminal
- Libera la terminal

**Ver logs después:**
```bash
docker-compose logs -f
```

---

### 1.2 Detener Servicios

#### **Opción 1: Ctrl+C (Solo detener, no eliminar)**

```bash
# En la terminal donde está corriendo docker-compose
Ctrl + C
```

**¿Qué hace?**
- ✅ Detiene los contenedores gracefully (SIGTERM)
- ❌ NO elimina contenedores
- ❌ NO elimina red
- ❌ NO elimina imágenes

**Estado después:**
```bash
docker ps
# Output: (vacío) - No hay nada corriendo

docker ps -a
# Output: 4 contenedores con status "Exited"
```

**Para volver a levantar:**
```bash
docker-compose up
# Rápido, solo arranca los contenedores existentes
```

**Cuándo usar:** Pausa temporal, vas a volver pronto

---

#### **Opción 2: docker-compose down (Detener + Limpiar)**

```bash
docker-compose down
```

**¿Qué hace?**
- ✅ Detiene los contenedores
- ✅ Elimina los contenedores
- ✅ Elimina la red
- ❌ NO elimina las imágenes (quedan en caché)

**Estado después:**
```bash
docker ps -a
# Output: (vacío) - Contenedores eliminados

docker images
# Output: Las 4 imágenes siguen ahí
```

**Para volver a levantar:**
```bash
docker-compose up
# Crea nuevos contenedores desde imágenes existentes
# Rápido (~30 segundos)
```

**Cuándo usar:** Terminaste por hoy, quieres limpiar

---

#### **Opción 3: docker-compose down --rmi all (Limpieza Total)**

```bash
docker-compose down --rmi all
```

**¿Qué hace?**
- ✅ Detiene los contenedores
- ✅ Elimina los contenedores
- ✅ Elimina la red
- ✅ Elimina las imágenes de los servicios

**Estado después:**
```bash
docker ps -a
# Output: (vacío)

docker images
# Output: Solo quedan imágenes base (maven, eclipse-temurin)
```

**Para volver a levantar:**
```bash
docker-compose up --build
# Necesita reconstruir todo
# Lento (2-5 minutos)
```

**Cuándo usar:** Quieres liberar espacio en disco

---

#### **Opción 4: Limpieza Total + Volúmenes**

```bash
docker-compose down -v --rmi all
```

**¿Qué hace?**
- Todo lo de Opción 3
- ✅ Elimina volúmenes (datos persistentes)

**⚠️ CUIDADO:** Si tienes datos en volúmenes (bases de datos), se pierden.

---

### 1.3 Tabla Comparativa

| Comando | Detiene | Elimina Contenedores | Elimina Red | Elimina Imágenes | Elimina Volúmenes |
|---------|---------|---------------------|-------------|------------------|-------------------|
| `Ctrl+C` | ✅ | ❌ | ❌ | ❌ | ❌ |
| `docker-compose down` | ✅ | ✅ | ✅ | ❌ | ❌ |
| `docker-compose down --rmi all` | ✅ | ✅ | ✅ | ✅ | ❌ |
| `docker-compose down -v --rmi all` | ✅ | ✅ | ✅ | ✅ | ✅ |

---

### 1.4 Flujo Recomendado Día a Día

```bash
# LUNES (Primera vez en la semana)
docker-compose up --build
# Trabaja...
Ctrl + C
docker-compose down

# MARTES - VIERNES (Días normales)
docker-compose up  # ← Sin --build, usa caché
# Trabaja...
Ctrl + C
docker-compose down

# VIERNES (Al terminar la semana, liberar espacio)
docker-compose down --rmi all
```

---

## 2. Gestión de Contenedores

### 2.1 Ver Contenedores

```bash
# Ver contenedores corriendo
docker ps

# Ver TODOS los contenedores (incluyendo detenidos)
docker ps -a

# Ver solo IDs
docker ps -q

# Ver con formato personalizado
docker ps --format "table {{.ID}}\t{{.Names}}\t{{.Status}}"
```

---

### 2.2 Ver Logs

```bash
# Ver logs de todos los servicios
docker-compose logs

# Ver logs de un servicio específico
docker-compose logs bureau-service

# Ver logs en tiempo real (follow)
docker-compose logs -f evaluacion-service

# Ver últimas 50 líneas
docker-compose logs --tail=50 bureau-service

# Ver logs con timestamps
docker-compose logs -t bureau-service
```

---

### 2.3 Inspeccionar Contenedores

```bash
# Ver detalles completos de un contenedor
docker inspect bureau-service

# Ver solo la IP del contenedor
docker inspect -f '{{.NetworkSettings.Networks.microservices_network.IPAddress}}' bureau-service

# Ver estado de salud
docker inspect -f '{{.State.Health.Status}}' bureau-service
```

---

### 2.4 Ejecutar Comandos en Contenedores

```bash
# Entrar a un contenedor (shell interactivo)
docker exec -it bureau-service sh

# Ejecutar un comando específico
docker exec bureau-service ls -la /app

# Ejecutar comando como root
docker exec -u root bureau-service apk add curl

# Ver procesos corriendo en el contenedor
docker exec bureau-service ps aux
```

---

### 2.5 Copiar Archivos

```bash
# Copiar archivo DEL contenedor AL host
docker cp bureau-service:/app/application.properties ./backup.properties

# Copiar archivo DEL host AL contenedor
docker cp ./config.txt bureau-service:/app/config.txt
```

---

### 2.6 Reiniciar Servicios

```bash
# Reiniciar un servicio específico
docker-compose restart bureau-service

# Reiniciar todos los servicios
docker-compose restart

# Forzar recreación de un contenedor
docker-compose up -d --force-recreate bureau-service
```

---

### 2.7 Escalar Servicios

```bash
# Escalar scoring-service a 3 instancias
docker-compose up --scale scoring-service=3

# Ver las 3 instancias corriendo
docker ps | grep scoring

# Volver a 1 instancia
docker-compose up --scale scoring-service=1
```

---

## 3. Gestión de Imágenes

### 3.1 Ver Imágenes

```bash
# Ver todas las imágenes locales
docker images

# Ver solo nombres e IDs
docker images --format "{{.Repository}}:{{.Tag}} ({{.Size}})"

# Ver imágenes sin etiquetar (dangling)
docker images -f "dangling=true"

# Ver espacio usado por imágenes
docker system df
```

**Ejemplo de output:**
```
REPOSITORY                        TAG       SIZE
capitulo8_2-evaluacion-service   latest    270MB
capitulo8_2-bureau-service       latest    250MB
capitulo8_2-identidad-service    latest    250MB
capitulo8_2-scoring-service      latest    250MB
maven                            3.9-...   650MB
eclipse-temurin                  21-jre... 170MB
```

---

### 3.2 Construir Imágenes

```bash
# Construir todas las imágenes
docker-compose build

# Construir una imagen específica
docker-compose build bureau-service

# Construir sin usar caché (reconstrucción completa)
docker-compose build --no-cache

# Construir en paralelo
docker-compose build --parallel
```

---

### 3.3 Eliminar Imágenes

```bash
# Eliminar una imagen específica
docker rmi capitulo8_2-bureau-service:latest

# Eliminar todas las imágenes de los servicios
docker-compose down --rmi all

# Eliminar todas las imágenes no usadas
docker image prune -a

# Eliminar imágenes dangling (sin tag)
docker image prune
```

---

### 3.4 Inspeccionar Imágenes

```bash
# Ver capas de una imagen
docker history bureau-service:latest

# Ver detalles completos
docker inspect bureau-service:latest

# Ver solo el tamaño
docker inspect -f '{{.Size}}' bureau-service:latest
```

---

### 3.5 Etiquetar y Subir Imágenes

```bash
# Etiquetar para un registry
docker tag bureau-service:latest myregistry.com/bureau-service:1.0

# Subir a registry
docker push myregistry.com/bureau-service:1.0

# Descargar de registry
docker pull myregistry.com/bureau-service:1.0
```

---

## 4. Gestión de Redes

### 4.1 Ver Redes

```bash
# Ver todas las redes
docker network ls

# Inspeccionar una red
docker network inspect microservices-network

# Ver qué contenedores están en una red
docker network inspect microservices-network | grep Name
```

---

### 4.2 Crear y Eliminar Redes

```bash
# Crear una red
docker network create mi-red-custom

# Crear red con subnet específica
docker network create --subnet=172.20.0.0/16 mi-red-custom

# Eliminar una red
docker network rm mi-red-custom

# Eliminar redes no usadas
docker network prune
```

---

### 4.3 Conectar/Desconectar Contenedores

```bash
# Conectar contenedor a una red
docker network connect mi-red-custom bureau-service

# Desconectar
docker network disconnect mi-red-custom bureau-service

# Ver a qué redes está conectado un contenedor
docker inspect bureau-service | grep -A 10 Networks
```

---

## 5. Gestión de Volúmenes

### 5.1 Ver Volúmenes

```bash
# Ver todos los volúmenes
docker volume ls

# Inspeccionar un volumen
docker volume inspect postgres-data

# Ver espacio usado
docker system df -v
```

---

### 5.2 Crear y Eliminar Volúmenes

```bash
# Crear un volumen
docker volume create mi-volumen

# Eliminar un volumen
docker volume rm mi-volumen

# Eliminar volúmenes no usados
docker volume prune

# Eliminar TODO (incluyendo volúmenes con datos)
docker volume prune -a
```

⚠️ **CUIDADO:** `docker volume prune` elimina datos permanentemente.

---

### 5.3 Usar Volúmenes en docker-compose.yml

```yaml
services:
  postgres:
    image: postgres:15
    volumes:
      - postgres-data:/var/lib/postgresql/data  # Volumen nombrado
      - ./backup:/backup  # Bind mount (carpeta local)

volumes:
  postgres-data:  # Define el volumen
```

---

## 6. Limpieza y Mantenimiento

### 6.1 Comandos de Limpieza

```bash
# Limpiar contenedores detenidos
docker container prune

# Limpiar imágenes sin usar
docker image prune

# Limpiar imágenes dangling
docker image prune -f

# Limpiar redes no usadas
docker network prune

# Limpiar volúmenes no usados
docker volume prune

# LIMPIEZA TOTAL (contenedores, imágenes, redes, volúmenes)
docker system prune -a --volumes
```

⚠️ **ADVERTENCIA:** `docker system prune -a --volumes` elimina TODO lo que no esté corriendo.

---

### 6.2 Ver Uso de Espacio

```bash
# Ver resumen de espacio
docker system df

# Ver detalle completo
docker system df -v
```

**Ejemplo de output:**
```
TYPE            TOTAL   ACTIVE   SIZE      RECLAIMABLE
Images          6       4        2.1GB     850MB (40%)
Containers      4       4        50MB      0B (0%)
Local Volumes   2       1        500MB     250MB (50%)
Build Cache     0       0        0B        0B
```

---

### 6.3 Estrategia de Limpieza Recomendada

**Limpieza Semanal:**
```bash
# Eliminar contenedores detenidos
docker container prune -f

# Eliminar imágenes sin usar hace 7+ días
docker image prune -a --filter "until=168h"
```

**Limpieza Mensual:**
```bash
# Limpieza más agresiva
docker system prune -a
```

**Antes de Presentaciones Importantes:**
```bash
# Liberar máximo espacio
docker system prune -a --volumes

# Luego reconstruir lo que necesites
docker-compose up --build
```

---

## 7. Comandos Esenciales Organizados

### 7.1 Comandos Docker Compose

| Comando | Descripción |
|---------|-------------|
| `docker-compose up` | Levantar servicios |
| `docker-compose up -d` | Levantar en background |
| `docker-compose up --build` | Reconstruir y levantar |
| `docker-compose down` | Detener y limpiar |
| `docker-compose ps` | Ver estado de servicios |
| `docker-compose logs` | Ver logs |
| `docker-compose logs -f` | Ver logs en tiempo real |
| `docker-compose restart` | Reiniciar servicios |
| `docker-compose build` | Construir imágenes |
| `docker-compose exec <servicio> sh` | Entrar a contenedor |
| `docker-compose scale <servicio>=3` | Escalar servicio |

---

### 7.2 Comandos Docker

| Comando | Descripción |
|---------|-------------|
| `docker ps` | Ver contenedores corriendo |
| `docker ps -a` | Ver todos los contenedores |
| `docker images` | Ver imágenes |
| `docker logs <contenedor>` | Ver logs |
| `docker exec -it <contenedor> sh` | Entrar a contenedor |
| `docker inspect <contenedor>` | Ver detalles |
| `docker stop <contenedor>` | Detener contenedor |
| `docker rm <contenedor>` | Eliminar contenedor |
| `docker rmi <imagen>` | Eliminar imagen |
| `docker network ls` | Ver redes |
| `docker volume ls` | Ver volúmenes |

---

### 7.3 Comandos de Limpieza

| Comando | Qué elimina |
|---------|-------------|
| `docker container prune` | Contenedores detenidos |
| `docker image prune` | Imágenes dangling |
| `docker image prune -a` | Imágenes no usadas |
| `docker network prune` | Redes no usadas |
| `docker volume prune` | Volúmenes no usados |
| `docker system prune` | Contenedores + imágenes dangling + redes |
| `docker system prune -a` | Contenedores + todas imágenes + redes |
| `docker system prune -a --volumes` | TODO |

---

## 8. Troubleshooting Práctico

### 8.1 Contenedor no arranca

**Síntoma:**
```bash
docker-compose ps
# bureau-service: Exit 1
```

**Diagnóstico:**
```bash
# Ver logs
docker-compose logs bureau-service

# Ver los últimos logs antes de fallar
docker logs bureau-service --tail 50
```

**Causas comunes:**
- Puerto ya en uso
- Error en el código
- Dependencia faltante
- Configuración incorrecta

---

### 8.2 Health check falla

**Síntoma:**
```bash
docker-compose ps
# bureau-service: unhealthy
```

**Diagnóstico:**
```bash
# Ver logs del health check
docker inspect bureau-service | grep -A 10 Health

# Probar el endpoint manualmente
curl http://localhost:8081/api/bureau/health

# Entrar al contenedor y probar desde dentro
docker exec -it bureau-service sh
wget --spider http://localhost:8081/api/bureau/health
```

**Causas comunes:**
- Servicio aún está iniciando (aumentar `start_period`)
- Endpoint health no existe
- Puerto incorrecto en health check

---

### 8.3 Contenedores no se ven entre sí

**Síntoma:**
```
Connection refused to bureau-service
```

**Diagnóstico:**
```bash
# Verificar que están en la misma red
docker network inspect microservices-network

# Probar DNS desde un contenedor
docker exec -it evaluacion-service sh
ping bureau-service

# Ver configuración de red del contenedor
docker inspect bureau-service | grep -A 20 Networks
```

**Causas comunes:**
- No están en la misma red
- Nombre de servicio incorrecto en application.properties
- Firewall bloqueando

---

### 8.4 Build muy lento

**Síntoma:**
Build tarda 10+ minutos

**Soluciones:**
```bash
# Usar caché agresivamente
docker-compose build --parallel

# Verificar .dockerignore (no copiar node_modules, target, etc.)
cat .dockerignore

# Ver qué se está copiando
docker build -t test . --progress=plain
```

**Optimizaciones:**
- Copiar pom.xml antes que src (caché de deps)
- Usar imágenes más pequeñas (Alpine)
- Multi-stage builds

---

### 8.5 Queda sin espacio en disco

**Síntoma:**
```
Error: No space left on device
```

**Diagnóstico:**
```bash
# Ver uso de espacio
docker system df

# Ver qué consume más
docker system df -v
```

**Solución:**
```bash
# Limpieza agresiva
docker system prune -a --volumes

# Eliminar imágenes antiguas
docker image prune -a --filter "until=720h"  # 30 días
```

---

### 8.6 Puerto ya en uso

**Síntoma:**
```
Error: bind: address already in use
```

**Diagnóstico:**
```bash
# Mac/Linux
lsof -i :8080

# Windows
netstat -ano | findstr :8080
```

**Solución:**
```bash
# Matar el proceso
kill -9 <PID>

# O cambiar puerto en docker-compose.yml
ports:
  - "9080:8080"  # Puerto externo 9080
```

---

## 9. Mejores Prácticas Operacionales

### 9.1 Desarrollo Local

✅ **Usa docker-compose up (sin -d)** para ver logs en tiempo real  
✅ **Ctrl+C para detener** durante desarrollo activo  
✅ **docker-compose down** al final del día  
✅ **Mantén imágenes en caché** (no hagas `--rmi all` constantemente)  

---

### 9.2 Testing / CI/CD

✅ **Siempre usa --build** para asegurar última versión  
✅ **Usa -d** para correr en background  
✅ **Espera health checks** antes de ejecutar tests  
✅ **Limpia después:** `docker-compose down --rmi all`  

---

### 9.3 Producción

✅ **Usa tags específicos** (no `latest`)  
✅ **Health checks obligatorios**  
✅ **Limita recursos** (CPU, RAM)  
✅ **Logs centralizados** (no solo `docker logs`)  
✅ **Monitoreo activo** (Prometheus, Datadog)  

---

### 9.4 Seguridad

✅ **No ejecutes como root:**
```dockerfile
USER appuser
```

✅ **Escanea imágenes:**
```bash
trivy image bureau-service:latest
```

✅ **Mantén imágenes actualizadas:**
```bash
docker pull eclipse-temurin:21-jre-alpine
docker-compose build --pull
```

✅ **No almacenes secretos en imágenes:**
```dockerfile
# ❌ Mal
ENV API_KEY=secret123

# ✅ Bien (pasar en runtime)
docker run -e API_KEY=secret123 ...
```

---

### 9.5 Performance

✅ **Usa .dockerignore** para reducir contexto de build  
✅ **Multi-stage builds** siempre que sea posible  
✅ **Imágenes Alpine** cuando aplique  
✅ **Caché agresivo** de layers (COPY pom.xml antes que src)  

---

## 📊 Cheat Sheet Rápida

### Comandos del Día a Día

```bash
# Levantar
docker-compose up

# Levantar en background
docker-compose up -d

# Ver estado
docker-compose ps

# Ver logs
docker-compose logs -f

# Detener
Ctrl + C  (o docker-compose down)

# Entrar a contenedor
docker exec -it bureau-service sh

# Limpiar al final del día
docker-compose down
```

---

### Comandos de Emergencia

```bash
# Ver qué está consumiendo espacio
docker system df

# Limpieza total
docker system prune -a

# Ver logs de error
docker-compose logs bureau-service | grep ERROR

# Reiniciar un servicio que falló
docker-compose restart bureau-service

# Forzar recreación
docker-compose up -d --force-recreate bureau-service
```

---

## 🎓 Conclusión

Docker no es solo "construir y correr". Saber **gestionar el ciclo de vida completo** es crítico:

✅ **Subir:** `docker-compose up` (con/sin --build)  
✅ **Monitorear:** `docker-compose ps`, `docker-compose logs`  
✅ **Debuggear:** `docker exec`, `docker inspect`  
✅ **Detener:** `Ctrl+C` o `docker-compose down`  
✅ **Limpiar:** `docker system prune` según necesidad  

**Regla de oro:** Entiende qué hace cada comando ANTES de ejecutarlo, especialmente los de limpieza.

---

**Fin del documento.**
