# 🐳 Guía Completa de Docker para VaultCorp API

## 📖 Índice

1. [¿Qué es Docker?](#qué-es-docker)
2. [Conceptos Fundamentales](#conceptos-fundamentales)
3. [Docker Compose](#docker-compose)
4. [Nuestro Setup: Keycloak + PostgreSQL](#nuestro-setup-keycloak--postgresql)
5. [Comandos Útiles](#comandos-útiles)
6. [Troubleshooting](#troubleshooting)
7. [Mejores Prácticas](#mejores-prácticas)

---

## 🎯 ¿Qué es Docker?

### Definición Simple

**Docker** es una plataforma que permite ejecutar aplicaciones en **contenedores**, que son entornos aislados y portables.

### Analogía: Contenedores de Envío

Imagina que tienes que enviar mercancía de Chile a Perú:

**Sin contenedores (antigua manera):**
- Cada camión lleva cosas diferentes (cajas, sacos, barriles)
- Difícil de organizar y transferir
- Incompatible entre barcos, trenes, camiones
- Desorden total

**Con contenedores (estandarizado):**
- Todo va en contenedores de 20 o 40 pies
- Fácil de apilar y transferir
- Compatible con barcos, trenes, camiones
- Organizado y eficiente

**Docker hace lo mismo con software:**
- Empaqueta aplicaciones en "contenedores"
- Funciona en cualquier máquina (Mac, Linux, Windows, Cloud)
- Aislado del sistema operativo
- Fácil de distribuir y ejecutar

---

## 🧱 Conceptos Fundamentales

### 1. Imagen (Image)

**¿Qué es?** Una plantilla de solo lectura que contiene todo lo necesario para ejecutar una aplicación.

**Analogía:** Un **plano arquitectónico** de una casa.

**Contiene:**
- Sistema operativo base
- Dependencias instaladas
- Código de la aplicación
- Configuración

**Ejemplo:**
```bash
quay.io/keycloak/keycloak:23.0.0
  ↓
Imagen de Keycloak versión 23.0.0
```

**Características:**
- ✅ Inmutable (no se modifica)
- ✅ Reutilizable
- ✅ Compartible (Docker Hub, registries)
- ✅ Versionada (tags: `latest`, `23.0.0`, etc.)

---

### 2. Contenedor (Container)

**¿Qué es?** Una instancia en ejecución de una imagen.

**Analogía:** Una **casa construida** a partir del plano arquitectónico.

**Características:**
- ✅ Basado en una imagen
- ✅ Ejecutable (proceso corriendo)
- ✅ Aislado (tiene su propio sistema de archivos, red, procesos)
- ✅ Efímero (se puede eliminar y recrear)
- ✅ Estado mutable (mientras corre, puede cambiar)

**Relación Imagen → Contenedor:**
```
Imagen (plano)              Contenedores (casas)
─────────────────────────────────────────────────
postgres:15          →      contenedor-1 (corriendo)
                     →      contenedor-2 (corriendo)
                     →      contenedor-3 (detenido)
```

Puedes crear **múltiples contenedores** de la **misma imagen**.

---

### 3. Volumen (Volume)

**¿Qué es?** Un espacio de almacenamiento persistente gestionado por Docker.

**Analogía:** Un **disco duro externo** que conectas a tu computadora.

**El Problema sin Volúmenes:**
```
Contenedor (efímero)
├── Sistema de archivos interno
│   ├── /app
│   ├── /etc
│   └── /var/lib/postgresql/data  ← Datos aquí
│
Si eliminas el contenedor → ¡Se pierden los datos! ❌
```

**La Solución con Volúmenes:**
```
Contenedor (efímero)              Volumen (persistente)
├── Sistema de archivos interno   ├── postgres_data
│   ├── /app                      │   ├── base/
│   ├── /etc                      │   ├── global/
│   └── /var/lib/postgresql/data ─┼──→├── pg_wal/
│       (apunta al volumen)       │   └── ...
│                                 │
Si eliminas el contenedor         Los datos siguen aquí ✅
```

**Tipos de almacenamiento:**

| Tipo | Descripción | Persistencia | Uso |
|------|-------------|--------------|-----|
| **Volumen** | Gestionado por Docker | ✅ Sí | Datos de BD, archivos críticos |
| **Bind Mount** | Carpeta del host | ✅ Sí | Desarrollo, código fuente |
| **tmpfs** | En memoria RAM | ❌ No | Datos temporales, cache |

**En nuestro proyecto:**
```yaml
volumes:
  postgres_data:  # ← Volumen para PostgreSQL
    driver: local
```

---

### 4. Red (Network)

**¿Qué es?** Una red virtual que conecta contenedores entre sí.

**Analogía:** Una **LAN privada** dentro de Docker.

**Sin red:**
```
Contenedor A          Contenedor B
    🚫  ←────────────→  🚫
    No se pueden comunicar
```

**Con red:**
```
        ┌─────────────────┐
        │ keycloak-network│
        └─────────────────┘
              ↓       ↓
         Postgres   Keycloak
            ✅  ←──→  ✅
       Se comunican por nombre
```

**Ventajas:**
- ✅ Aislamiento (solo contenedores en la red se ven)
- ✅ Resolución DNS automática (usar nombres en vez de IPs)
- ✅ Seguridad

**Ejemplo de comunicación:**
```yaml
keycloak:
  environment:
    KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak
                                  ↑
                         Nombre del contenedor, no IP
```

Docker resuelve `postgres` → IP del contenedor automáticamente.

---

### Comparativa Visual

```
┌──────────────────────────────────────────────────────┐
│                    TU MÁQUINA                        │
│                                                      │
│  ┌────────────────────────────────────────────┐     │
│  │         DOCKER ENGINE                      │     │
│  │                                            │     │
│  │  ┌──────────────┐     ┌──────────────┐    │     │
│  │  │ Contenedor 1 │     │ Contenedor 2 │    │     │
│  │  │ (Keycloak)   │     │ (PostgreSQL) │    │     │
│  │  │              │     │              │    │     │
│  │  │ Puerto 8080 ─┼─────┼→ Puerto 5432│    │     │
│  │  │              │     │      ↓       │    │     │
│  │  └──────────────┘     │    Volumen   │    │     │
│  │                       │  postgres_data    │     │
│  │                       └──────────────┘    │     │
│  │                              ↓             │     │
│  │                    ┌──────────────────┐   │     │
│  │                    │  Disco del Host  │   │     │
│  │                    │ (data persiste)  │   │     │
│  │                    └──────────────────┘   │     │
│  └────────────────────────────────────────────┘     │
│                                                      │
│  Puertos expuestos:                                 │
│  - localhost:8180 → Keycloak                        │
│  - localhost:5432 → PostgreSQL                      │
└──────────────────────────────────────────────────────┘
```

---

## 🎼 Docker Compose

### ¿Qué es Docker Compose?

**Docker Compose** es una herramienta para definir y ejecutar aplicaciones Docker **multi-contenedor** usando un archivo YAML.

### El Problema sin Docker Compose

Si quieres levantar Keycloak + PostgreSQL:

```bash
# Crear red
docker network create keycloak-network

# Crear volumen
docker volume create postgres_data

# Levantar PostgreSQL
docker run -d \
  --name keycloak-postgres \
  --network keycloak-network \
  -v postgres_data:/var/lib/postgresql/data \
  -e POSTGRES_DB=keycloak \
  -e POSTGRES_USER=keycloak \
  -e POSTGRES_PASSWORD=keycloak123 \
  -p 5432:5432 \
  postgres:15

# Esperar a que PostgreSQL esté listo...

# Levantar Keycloak
docker run -d \
  --name keycloak-vaultcorp \
  --network keycloak-network \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  -e KC_DB=postgres \
  -e KC_DB_URL=jdbc:postgresql://keycloak-postgres:5432/keycloak \
  -e KC_DB_USERNAME=keycloak \
  -e KC_DB_PASSWORD=keycloak123 \
  -p 8180:8080 \
  quay.io/keycloak/keycloak:23.0.0 \
  start-dev
```

**Problemas:**
- ❌ Comandos largos y complejos
- ❌ Difícil de recordar
- ❌ Propenso a errores
- ❌ Difícil de compartir con el equipo
- ❌ Hay que ejecutar múltiples comandos en orden

---

### La Solución: docker-compose.yml

Con Docker Compose, todo lo anterior se convierte en:

```bash
docker-compose up -d
```

**Un solo comando** levanta todo. 🎉

---

### Estructura de docker-compose.yml

```yaml
version: '3.8'  # ← Versión del formato (opcional en versiones nuevas)

services:  # ← Lista de contenedores a levantar

  postgres:  # ← Nombre del servicio
    image: postgres:15  # ← Imagen a usar
    container_name: keycloak-postgres  # ← Nombre del contenedor
    environment:  # ← Variables de entorno
      POSTGRES_DB: keycloak
      POSTGRES_USER: keycloak
      POSTGRES_PASSWORD: keycloak123
    volumes:  # ← Montajes de volúmenes
      - postgres_data:/var/lib/postgresql/data
    ports:  # ← Mapeo de puertos
      - "5432:5432"
    networks:  # ← Redes
      - keycloak-network
    healthcheck:  # ← Verificación de salud
      test: ["CMD-SHELL", "pg_isready -U keycloak"]
      interval: 10s
      timeout: 5s
      retries: 5

  keycloak:
    image: quay.io/keycloak/keycloak:23.0.0
    container_name: keycloak-vaultcorp
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak
      KC_DB_USERNAME: keycloak
      KC_DB_PASSWORD: keycloak123
    ports:
      - "8180:8080"
    depends_on:  # ← Dependencias entre servicios
      postgres:
        condition: service_healthy
    networks:
      - keycloak-network
    command: start-dev  # ← Comando a ejecutar

volumes:  # ← Definición de volúmenes
  postgres_data:
    driver: local

networks:  # ← Definición de redes
  keycloak-network:
    driver: bridge
```

---

## 🔧 Nuestro Setup: Keycloak + PostgreSQL

### Arquitectura

```
┌─────────────────────────────────────────────────────────┐
│                  docker-compose.yml                     │
└─────────────────────────────────────────────────────────┘
                          │
        ┌─────────────────┴─────────────────┐
        │                                   │
        ▼                                   ▼
┌──────────────────┐              ┌──────────────────┐
│   PostgreSQL     │              │    Keycloak      │
│   Container      │◄─────────────│   Container      │
│                  │  JDBC        │                  │
│  Puerto: 5432    │  Connection  │  Puerto: 8080    │
│                  │              │  (→ 8180 host)   │
└────────┬─────────┘              └──────────────────┘
         │                                 │
         │ Persiste en                     │ Lee/Escribe
         ▼                                 │
┌──────────────────┐                       │
│ Volumen          │◄──────────────────────┘
│ postgres_data    │
│                  │
│ - Realm config   │
│ - Usuarios       │
│ - Roles          │
│ - Clients        │
└──────────────────┘
```

---

### Análisis Línea por Línea

#### Servicio PostgreSQL

```yaml
postgres:
  image: postgres:15
```
**Explicación:** Usar imagen oficial de PostgreSQL versión 15.

---

```yaml
  container_name: keycloak-postgres
```
**Explicación:** El contenedor se llamará `keycloak-postgres` (en vez de un nombre autogenerado).

**Sin esto:** `vault-api_postgres_1` (nombre autogenerado)  
**Con esto:** `keycloak-postgres` (nombre fijo)

---

```yaml
  environment:
    POSTGRES_DB: keycloak
    POSTGRES_USER: keycloak
    POSTGRES_PASSWORD: keycloak123
```
**Explicación:** Variables de entorno que PostgreSQL lee al iniciar para:
- Crear una base de datos llamada `keycloak`
- Crear un usuario `keycloak` con contraseña `keycloak123`

**⚠️ IMPORTANTE:** En producción, usar contraseñas seguras y secrets.

---

```yaml
  volumes:
    - postgres_data:/var/lib/postgresql/data
```
**Explicación:** 
- `postgres_data`: Nombre del volumen (definido abajo)
- `/var/lib/postgresql/data`: Ruta DENTRO del contenedor donde PostgreSQL guarda datos

**¿Qué logra?** Todos los datos de PostgreSQL se guardan en el volumen. Si el contenedor se elimina, los datos persisten.

---

```yaml
  ports:
    - "5432:5432"
```
**Explicación:**
- Formato: `"PUERTO_HOST:PUERTO_CONTENEDOR"`
- `5432` (izquierda): Puerto en tu máquina
- `5432` (derecha): Puerto dentro del contenedor

**Resultado:** Puedes conectarte a PostgreSQL desde tu máquina en `localhost:5432`.

---

```yaml
  networks:
    - keycloak-network
```
**Explicación:** Conecta este contenedor a la red `keycloak-network` (definida abajo).

**Efecto:** PostgreSQL puede comunicarse con Keycloak usando el nombre `postgres`.

---

```yaml
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U keycloak"]
    interval: 10s
    timeout: 5s
    retries: 5
```
**Explicación:** Docker ejecuta el comando `pg_isready -U keycloak` cada 10 segundos para verificar que PostgreSQL esté listo.

**¿Por qué es importante?**
- Keycloak necesita que PostgreSQL esté 100% listo antes de iniciar
- Sin healthcheck, Keycloak podría fallar si intenta conectarse muy rápido

**Parámetros:**
- `interval: 10s`: Revisar cada 10 segundos
- `timeout: 5s`: Esperar máximo 5 segundos por respuesta
- `retries: 5`: Intentar 5 veces antes de marcar como "unhealthy"

---

#### Servicio Keycloak

```yaml
keycloak:
  image: quay.io/keycloak/keycloak:23.0.0
  container_name: keycloak-vaultcorp
```
**Explicación:** Usar imagen de Keycloak versión 23.0.0 desde el registry de Quay.io.

---

```yaml
  environment:
    KEYCLOAK_ADMIN: admin
    KEYCLOAK_ADMIN_PASSWORD: admin
```
**Explicación:** Credenciales del usuario administrador inicial de Keycloak.

**Usuario:** `admin`  
**Contraseña:** `admin`

**⚠️ IMPORTANTE:** En producción, cambiar estas credenciales.

---

```yaml
    KC_DB: postgres
    KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak
    KC_DB_USERNAME: keycloak
    KC_DB_PASSWORD: keycloak123
```
**Explicación:** Configuración de base de datos para Keycloak.

- `KC_DB: postgres`: Usar PostgreSQL (no H2 en memoria)
- `KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak`: URL de conexión
  - `postgres`: Nombre del servicio (Docker lo resuelve a la IP)
  - `5432`: Puerto de PostgreSQL
  - `keycloak`: Nombre de la base de datos
- `KC_DB_USERNAME: keycloak`: Usuario de PostgreSQL
- `KC_DB_PASSWORD: keycloak123`: Contraseña

**Resultado:** Keycloak guarda TODO en PostgreSQL, no en memoria.

---

```yaml
    KC_HOSTNAME_STRICT: false
    KC_HTTP_ENABLED: true
```
**Explicación:** Configuración para modo desarrollo.

- `KC_HOSTNAME_STRICT: false`: No validar hostname (permite `localhost`)
- `KC_HTTP_ENABLED: true`: Permitir HTTP (sin HTTPS)

**⚠️ IMPORTANTE:** En producción:
- `KC_HOSTNAME_STRICT: true`
- `KC_HTTP_ENABLED: false` (solo HTTPS)

---

```yaml
  ports:
    - "8180:8080"
```
**Explicación:**
- `8180`: Puerto en tu máquina
- `8080`: Puerto dentro del contenedor (Keycloak escucha en 8080)

**¿Por qué 8180 y no 8080?**  
Porque Quarkus ya usa el puerto 8080, para evitar conflictos usamos 8180.

**Acceso:** `http://localhost:8180`

---

```yaml
  depends_on:
    postgres:
      condition: service_healthy
```
**Explicación:** Keycloak depende de PostgreSQL.

**Sin `depends_on`:**
```
Keycloak inicia → PostgreSQL todavía no está listo → Error ❌
```

**Con `depends_on` + `condition: service_healthy`:**
```
PostgreSQL inicia → Healthcheck pasa → Keycloak inicia ✅
```

Docker espera a que PostgreSQL esté "healthy" antes de iniciar Keycloak.

---

```yaml
  networks:
    - keycloak-network
```
**Explicación:** Conecta Keycloak a la misma red que PostgreSQL.

**Resultado:** Ambos contenedores pueden comunicarse.

---

```yaml
  command: start-dev
```
**Explicación:** Comando que se ejecuta al iniciar el contenedor.

- `start-dev`: Inicia Keycloak en modo desarrollo

**Alternativas:**
- `start`: Modo producción (requiere HTTPS, más configuración)
- `start --optimized`: Producción optimizado

---

#### Volúmenes

```yaml
volumes:
  postgres_data:
    driver: local
```
**Explicación:** Define un volumen llamado `postgres_data`.

- `driver: local`: Almacenamiento local en disco

**¿Dónde se guarda físicamente?**
```bash
# En Linux/Mac:
/var/lib/docker/volumes/vault-api_postgres_data/_data

# En Docker Desktop (Mac/Windows):
En la VM de Docker Desktop
```

**Comandos útiles:**
```bash
# Ver volúmenes
docker volume ls

# Inspeccionar volumen
docker volume inspect vault-api_postgres_data

# Ver tamaño
docker system df -v
```

---

#### Redes

```yaml
networks:
  keycloak-network:
    driver: bridge
```
**Explicación:** Define una red llamada `keycloak-network`.

- `driver: bridge`: Red tipo puente (red virtual privada)

**Tipos de redes:**

| Tipo | Descripción | Uso |
|------|-------------|-----|
| **bridge** | Red privada entre contenedores | Desarrollo, apps multi-contenedor |
| **host** | Usa red del host directamente | Alto rendimiento, sin aislamiento |
| **overlay** | Red entre múltiples hosts | Docker Swarm, Kubernetes |
| **none** | Sin red | Contenedores aislados |

---

## 💻 Comandos Útiles

### Comandos Básicos de Docker Compose

#### Levantar Servicios

```bash
# Levantar en background (-d = detached)
docker-compose up -d

# Levantar y ver logs en tiempo real
docker-compose up

# Levantar solo un servicio
docker-compose up postgres
```

---

#### Detener Servicios

```bash
# Detener contenedores (mantiene volúmenes y redes)
docker-compose stop

# Detener y eliminar contenedores (mantiene volúmenes)
docker-compose down

# Detener, eliminar contenedores Y volúmenes (⚠️ borra data)
docker-compose down -v

# Detener, eliminar contenedores, volúmenes E imágenes
docker-compose down -v --rmi all
```

---

#### Ver Estado

```bash
# Ver servicios corriendo
docker-compose ps

# Ver logs de todos los servicios
docker-compose logs

# Ver logs de un servicio específico
docker-compose logs keycloak

# Seguir logs en tiempo real (-f = follow)
docker-compose logs -f

# Ver últimas 100 líneas
docker-compose logs --tail=100
```

---

#### Reiniciar Servicios

```bash
# Reiniciar todos los servicios
docker-compose restart

# Reiniciar un servicio específico
docker-compose restart keycloak
```

---

#### Ejecutar Comandos en Contenedores

```bash
# Abrir shell en contenedor
docker-compose exec postgres bash

# Conectarse a PostgreSQL
docker-compose exec postgres psql -U keycloak -d keycloak

# Ver tablas de Keycloak
docker-compose exec postgres psql -U keycloak -d keycloak -c "\dt"
```

---

### Comandos de Docker (sin Compose)

#### Listar Recursos

```bash
# Listar contenedores corriendo
docker ps

# Listar todos los contenedores (incluso detenidos)
docker ps -a

# Listar imágenes
docker images

# Listar volúmenes
docker volume ls

# Listar redes
docker network ls
```

---

#### Inspeccionar Recursos

```bash
# Ver detalles de un contenedor
docker inspect keycloak-vaultcorp

# Ver logs de un contenedor
docker logs keycloak-vaultcorp

# Seguir logs en tiempo real
docker logs -f keycloak-vaultcorp

# Ver estadísticas de uso (CPU, RAM)
docker stats
```

---

#### Limpiar Recursos

```bash
# Eliminar contenedores detenidos
docker container prune

# Eliminar imágenes sin usar
docker image prune

# Eliminar volúmenes sin usar
docker volume prune

# Eliminar redes sin usar
docker network prune

# Limpiar todo (⚠️ cuidado)
docker system prune -a --volumes
```

---

#### Volúmenes

```bash
# Crear volumen
docker volume create mi_volumen

# Inspeccionar volumen
docker volume inspect vault-api_postgres_data

# Eliminar volumen
docker volume rm vault-api_postgres_data

# Hacer backup de volumen
docker run --rm -v vault-api_postgres_data:/data \
  -v $(pwd):/backup ubuntu \
  tar czf /backup/postgres-backup.tar.gz /data
```

---

## 🛠️ Troubleshooting

### Problema 1: Puerto ya en uso

**Error:**
```
Error starting userland proxy: listen tcp4 0.0.0.0:8180: bind: address already in use
```

**Causa:** Otro proceso está usando el puerto 8180.

**Solución 1:** Cambiar el puerto en `docker-compose.yml`
```yaml
ports:
  - "8280:8080"  # Cambiado de 8180 a 8280
```

**Solución 2:** Encontrar y detener el proceso que usa el puerto
```bash
# En Mac/Linux
lsof -i :8180

# En Windows
netstat -ano | findstr :8180

# Matar proceso
kill -9 <PID>
```

---

### Problema 2: Keycloak no inicia (falla conexión a PostgreSQL)

**Error en logs:**
```
WARN: KC-SERVICES0003: Failed to connect to database.
```

**Causa:** PostgreSQL no está listo cuando Keycloak intenta conectarse.

**Solución:** Verificar healthcheck en `docker-compose.yml`
```yaml
depends_on:
  postgres:
    condition: service_healthy  # ← Debe estar presente
```

**Verificar estado de PostgreSQL:**
```bash
docker-compose ps

# Debe mostrar "healthy"
keycloak-postgres  ... Up (healthy)
```

---

### Problema 3: Volumen con datos corruptos

**Síntoma:** PostgreSQL no inicia, logs muestran errores de corrupción.

**Solución:** Eliminar volumen y recrear (⚠️ pierdes datos)
```bash
# Detener todo
docker-compose down

# Eliminar volumen
docker volume rm vault-api_postgres_data

# Levantar de nuevo (crea volumen limpio)
docker-compose up -d
```

**Para evitar esto:** Hacer backups regulares.

---

### Problema 4: Cambios en docker-compose.yml no se aplican

**Síntoma:** Modificas `docker-compose.yml` pero nada cambia.

**Causa:** Docker Compose cachea configuración.

**Solución:** Forzar recreación
```bash
# Detener y eliminar contenedores
docker-compose down

# Recrear con cambios
docker-compose up -d --force-recreate
```

---

### Problema 5: "Permission denied" en volúmenes

**Error:**
```
chmod: changing permissions of '/var/lib/postgresql/data': Permission denied
```

**Causa:** Problema de permisos entre host y contenedor.

**Solución:** Verificar permisos del volumen
```bash
# Ver permisos
docker volume inspect vault-api_postgres_data

# En casos extremos, eliminar y recrear
docker-compose down -v
docker-compose up -d
```

---

### Problema 6: Docker Compose no encuentra el archivo

**Error:**
```
Can't find a suitable configuration file in this directory or any parent
```

**Causa:** Estás en el directorio equivocado.

**Solución:**
```bash
# Ir al directorio del proyecto
cd /ruta/al/proyecto/vault-api

# Verificar que existe docker-compose.yml
ls docker-compose.yml

# Ejecutar
docker-compose up -d
```

---

## 📚 Mejores Prácticas

### 1. Separar Entornos

```yaml
# docker-compose.dev.yml (desarrollo)
version: '3.8'
services:
  keycloak:
    command: start-dev
    environment:
      KC_HTTP_ENABLED: true

# docker-compose.prod.yml (producción)
version: '3.8'
services:
  keycloak:
    command: start
    environment:
      KC_HOSTNAME: https://keycloak.miempresa.com
      KC_HTTP_ENABLED: false
```

**Uso:**
```bash
# Desarrollo
docker-compose -f docker-compose.dev.yml up -d

# Producción
docker-compose -f docker-compose.prod.yml up -d
```

---

### 2. Variables de Entorno en Archivo

Crear `.env` en la raíz del proyecto:

```env
# .env
POSTGRES_USER=keycloak
POSTGRES_PASSWORD=mi-password-super-seguro
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=otro-password-seguro
```

Usar en `docker-compose.yml`:

```yaml
services:
  postgres:
    environment:
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
```

**⚠️ IMPORTANTE:** Agregar `.env` a `.gitignore` para no subir contraseñas a Git.

---

### 3. Healthchecks Siempre

```yaml
services:
  postgres:
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U keycloak"]
      interval: 10s
      timeout: 5s
      retries: 5
  
  keycloak:
    depends_on:
      postgres:
        condition: service_healthy
```

**Beneficio:** Evita errores de "servicio no listo".

---

### 4. Limitar Recursos

```yaml
services:
  postgres:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

**Beneficio:** Evita que un contenedor consuma todos los recursos del host.

---

### 5. Usar Versiones Específicas

```yaml
# ❌ Mal (versión flotante)
image: postgres:latest

# ✅ Bien (versión fija)
image: postgres:15.4
```

**Beneficio:** Reproducibilidad - siempre usa la misma versión.

---

### 6. Backups Automáticos

Script de backup (`backup.sh`):

```bash
#!/bin/bash

# Backup de volumen PostgreSQL
docker run --rm \
  -v vault-api_postgres_data:/data \
  -v $(pwd)/backups:/backup \
  ubuntu \
  tar czf /backup/postgres-$(date +%Y%m%d-%H%M%S).tar.gz /data

# Mantener solo últimos 7 backups
cd backups
ls -t postgres-*.tar.gz | tail -n +8 | xargs rm -f
```

**Configurar cron:**
```bash
# Ejecutar cada día a las 2 AM
0 2 * * * /ruta/al/proyecto/backup.sh
```

---

### 7. Logs Rotados

```yaml
services:
  postgres:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

**Beneficio:** Evita que logs llenen el disco.

---

## 🎓 Comandos de Referencia Rápida

```bash
# ═══════════════════════════════════════════════
# LEVANTAR Y DETENER
# ═══════════════════════════════════════════════
docker-compose up -d              # Levantar todo
docker-compose down               # Bajar todo (mantiene volúmenes)
docker-compose down -v            # Bajar todo (elimina volúmenes)
docker-compose restart            # Reiniciar todo

# ═══════════════════════════════════════════════
# LOGS Y MONITOREO
# ═══════════════════════════════════════════════
docker-compose logs -f            # Ver logs en tiempo real
docker-compose logs keycloak      # Logs de un servicio
docker-compose ps                 # Estado de servicios
docker stats                      # Uso de CPU/RAM

# ═══════════════════════════════════════════════
# EJECUTAR COMANDOS
# ═══════════════════════════════════════════════
docker-compose exec postgres bash               # Shell en PostgreSQL
docker-compose exec postgres psql -U keycloak   # Cliente psql
docker-compose exec keycloak bash               # Shell en Keycloak

# ═══════════════════════════════════════════════
# VOLÚMENES
# ═══════════════════════════════════════════════
docker volume ls                               # Listar volúmenes
docker volume inspect vault-api_postgres_data  # Inspeccionar
docker volume rm vault-api_postgres_data       # Eliminar

# ═══════════════════════════════════════════════
# LIMPIEZA
# ═══════════════════════════════════════════════
docker system prune               # Limpiar recursos sin usar
docker volume prune               # Limpiar volúmenes sin usar
docker image prune -a             # Limpiar imágenes sin usar
```

---

## 🎯 Resumen Ejecutivo

### ¿Qué logramos con este setup?

1. **✅ Persistencia:** Configuración de Keycloak se guarda en PostgreSQL
2. **✅ Reproducibilidad:** Un comando levanta todo el entorno
3. **✅ Aislamiento:** Contenedores aislados del sistema host
4. **✅ Portabilidad:** Funciona en Mac, Linux, Windows, Cloud
5. **✅ Escalabilidad:** Fácil agregar más servicios
6. **✅ Mantenibilidad:** Configuración centralizada en YAML

### Flujo de Trabajo Típico

```bash
# Día 1: Setup inicial
git clone proyecto
cd proyecto
docker-compose up -d
# Configurar Keycloak via browser

# Día 2+: Desarrollo normal
./mvnw quarkus:dev
# Keycloak ya está corriendo en background

# Al terminar el día
docker-compose stop

# Al siguiente día
docker-compose start
# Todo sigue configurado ✅
```

---

## 📖 Recursos Adicionales

- [Docker Official Docs](https://docs.docker.com/)
- [Docker Compose Reference](https://docs.docker.com/compose/compose-file/)
- [PostgreSQL Docker Hub](https://hub.docker.com/_/postgres)
- [Keycloak in Containers](https://www.keycloak.org/server/containers)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)



