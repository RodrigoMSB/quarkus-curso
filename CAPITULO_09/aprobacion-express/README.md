# 🏦 Sistema de Pre-Aprobación Crediticia Express

## 📋 Tabla de Contenidos

1. [Descripción del Proyecto](#descripción-del-proyecto)
2. [Pre-requisitos CRÍTICOS](#pre-requisitos-críticos)
3. [Setup Inicial - PASO A PASO](#setup-inicial---paso-a-paso)
4. [Ejecución de Tests Funcionales](#ejecución-de-tests-funcionales)
5. [Ejecución del Benchmark JVM vs Native](#ejecución-del-benchmark-jvm-vs-native)
6. [Troubleshooting - PROBLEMAS COMUNES](#troubleshooting---problemas-comunes)
7. [Conceptos Clave para la Clase](#conceptos-clave-para-la-clase)

---

## 📌 Descripción del Proyecto

Sistema bancario de pre-aprobación crediticia que evalúa solicitudes en menos de 200ms.

**Tecnologías:**
- Quarkus 3.28.5
- PostgreSQL 16
- Hibernate ORM + Panache
- REST + Jackson
- Micrometer (métricas)
- SmallRye Health

**Scripts incluidos:**
- `test-aprobacion.sh` - 13 pruebas funcionales (2-3 min)
- `benchmark.sh` - Comparación JVM vs Native (15-20 min)

---

## ⚠️ Pre-requisitos CRÍTICOS

### 1. Software Necesario

```bash
# Verificar versiones
java --version    # Java 17 o superior
mvn --version     # Maven 3.8+
docker --version  # Docker Desktop
curl --version    # Para pruebas HTTP
```

### 2. PostgreSQL - IMPORTANTE ⚠️

**PROBLEMA COMÚN:** Si tienes PostgreSQL instalado localmente en tu Mac (con Homebrew), puede causar conflictos de puerto.

**Verificar si tienes PostgreSQL local:**
```bash
brew services list | grep postgresql
ps aux | grep postgres | grep -v grep
```

**Si está corriendo, DETENLO antes de continuar:**
```bash
# Detener PostgreSQL local temporalmente
brew services stop postgresql@16
# O cualquier versión que tengas
brew services stop postgresql
```

**¿Por qué?** Porque tanto PostgreSQL local como Docker intentan usar el puerto 5432, causando conflictos.

### 3. Puertos Requeridos

- **5432** - PostgreSQL (Docker)
- **8080** - Aplicación Quarkus

**Verificar que estén libres:**
```bash
lsof -i :5432  # Debe estar vacío
lsof -i :8080  # Debe estar vacío
```

---

## 🚀 Setup Inicial - PASO A PASO

### PASO 1: Clonar y configurar el proyecto

```bash
# Navegar al proyecto
cd ~/QUARKUS/"CAPITULO 9"/aprobacion-express

# Verificar estructura
ls -la
# Debes ver: src/, pom.xml, test-aprobacion.sh, benchmark.sh
```

### PASO 2: Configurar PostgreSQL con Docker

**Crear docker-compose.yml:**

```bash
cat > docker-compose.yml << 'DOCKER'
services:
  postgres:
    image: postgres:16-alpine
    container_name: banco-postgres
    environment:
      POSTGRES_DB: banco_credito
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
DOCKER
```

**Levantar PostgreSQL:**

```bash
# Iniciar PostgreSQL
docker-compose up -d

# Esperar a que esté listo (IMPORTANTE)
sleep 10

# Verificar que está corriendo
docker ps | grep postgres
# Debes ver: Up X seconds (healthy)

# Verificar que el usuario existe
docker exec banco-postgres psql -U postgres -c "SELECT version();"
# Debe mostrar: PostgreSQL 16.x
```

**⚠️ SI FALLA con "role postgres does not exist":**

```bash
# Borrar volumen y recrear
docker-compose down -v
sleep 2
docker-compose up -d
sleep 10
docker exec banco-postgres psql -U postgres -c "SELECT version();"
```

### PASO 3: Verificar application.properties

**CRÍTICO:** El archivo `src/main/resources/application.properties` debe tener estas líneas:

```properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=postgres
quarkus.datasource.password=postgres123
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/banco_credito
```

**Verificar rápidamente:**
```bash
grep "quarkus.datasource.username" src/main/resources/application.properties
# Debe mostrar: quarkus.datasource.username=postgres
```

---

## 🧪 Ejecución de Tests Funcionales

### Opción A: Con el script automatizado (RECOMENDADO para clase)

```bash
# Terminal 1: Iniciar aplicación
./mvnw quarkus:dev

# Espera hasta ver:
# Listening on: http://0.0.0.0:8080

# Terminal 2: Ejecutar tests
chmod +x test-aprobacion.sh
./test-aprobacion.sh
```

**Tiempo estimado:** 2-3 minutos

**Qué hace:**
1. ✅ Verifica health checks (liveness, readiness)
2. ✅ Prueba métricas Prometheus
3. ✅ Crea solicitudes aprobadas y rechazadas
4. ✅ Valida reglas de negocio
5. ✅ Genera reporte en .txt

**Resultado esperado:**
```
╔════════════════════════════════════════════════════════════════╗
║                    RESULTADOS FINALES                          ║
╠════════════════════════════════════════════════════════════════╣
║ Total de pruebas:    13                                        ║
║ Pruebas exitosas:    13                                        ║
║ Pruebas fallidas:    0                                         ║
╚════════════════════════════════════════════════════════════════╝

✓ ¡TODAS LAS PRUEBAS PASARON EXITOSAMENTE! 🚀
```

### Opción B: Manual (para debugging)

```bash
# Terminal 1: Iniciar aplicación
./mvnw quarkus:dev

# Terminal 2: Probar endpoints manualmente
curl http://localhost:8080/q/health/ready
curl http://localhost:8080/api/preaprobacion/estadisticas
```

**⚠️ NOTA IMPORTANTE:** Observa que las URLs usan el prefijo `/q/` - esto es específico de Quarkus.

---

## 📊 Ejecución del Benchmark JVM vs Native

### Pre-requisitos para el Benchmark

**IMPORTANTE:** El benchmark requiere GraalVM para compilar Native.

#### Instalar GraalVM con SDKMAN (macOS/Linux)

```bash
# 1. Instalar SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# 2. Verificar instalación
sdk version

# 3. Instalar GraalVM 21
sdk install java 21.0.1-graalce

# Cuando pregunte "set as default": responde "n" (no)

# 4. Activar GraalVM en la terminal actual
sdk use java 21.0.1-graalce

# 5. Verificar instalación
java -version
# Debe decir: "GraalVM CE 21.0.1"

native-image --version
# Debe mostrar: "native-image 21.0.1"
```

### Ejecutar el Benchmark

```bash
# 1. Asegurarse que PostgreSQL esté corriendo
docker ps | grep postgres

# 2. Si tienes PostgreSQL local, detenerlo
brew services stop postgresql@16

# 3. Dar permisos al script
chmod +x benchmark.sh

# 4. Ejecutar benchmark completo
./benchmark.sh
```

**Tiempo estimado:** 15-20 minutos

**Fases del benchmark:**
1. **Fase 1:** Compilación JVM (~10 segundos)
2. **Fase 2:** Pruebas JVM (~2 minutos)
3. **Fase 3:** Compilación Native (~8-10 minutos) ⏳ LA MÁS LENTA
4. **Fase 4:** Pruebas Native (~2 minutos)
5. **Fase 5:** Comparativa final (instantáneo)

**Durante la compilación Native es NORMAL que:**
- CPU llegue al 100%
- Ventilador suene fuerte
- Parezca "pegado" en algunos pasos
- **NO INTERRUMPIR**

**Resultado esperado:**

```
╔══════════════════════════════════════════════════════════════════════════════╗
║                     RESULTADOS DEL BENCHMARK                                 ║
╠══════════════════════════════════════════════════════════════════════════════╣
║ MÉTRICA                     │      JVM MODE       │    NATIVE MODE            ║
╠═════════════════════════════╪═════════════════════╪═══════════════════════════╣
║ Tiempo de compilación       │  7-10s              │  87-96s                   ║
║ Tiempo de arranque          │  2-3s               │  2-3s                     ║
║ Uso de memoria (RSS)        │  245-275 MB         │  68 MB                    ║
║ Throughput                  │  33 req/s           │  50-100 req/s             ║
║ Tamaño del artefacto        │  JAR + JVM          │  91 MB                    ║
╚══════════════════════════════════════════════════════════════════════════════╝
```

**Interpretación:**
- ✅ Native usa **60-75% menos memoria**
- ✅ Native tiene **50-200% más throughput**
- ✅ JVM compila **10-13x más rápido**
- ✅ Arranque similar en apps pequeñas (Native gana en apps grandes)

---

## 🐛 Troubleshooting - PROBLEMAS COMUNES

### ⚠️ PROBLEMA CRÍTICO: URLs de Quarkus

**Quarkus expone los health checks y métricas con prefijo `/q/`:**

| ❌ URL INCORRECTA | ✅ URL CORRECTA |
|-------------------|-----------------|
| `/health/ready` | `/q/health/ready` |
| `/health/live` | `/q/health/live` |
| `/metrics` | `/q/metrics` |
| `/health` | `/q/health` |

**Si tus scripts fallan con 404, verifica las URLs:**

```bash
# Verificar URLs en test-aprobacion.sh
grep "health\|metrics" test-aprobacion.sh

# Deben mostrar /q/ en las rutas:
# ${BASE_URL}/q/health/ready
# ${BASE_URL}/q/metrics
```

---

### Problema 1: "role postgres does not exist"

**Síntoma:**
```
FATAL: role "postgres" does not exist
```

**Causa:** El volumen de Docker tiene datos viejos o el contenedor no se inicializó correctamente.

**Solución:**
```bash
# Borrar volumen y recrear desde cero
docker-compose down -v
sleep 2
docker-compose up -d
sleep 10
docker exec banco-postgres psql -U postgres -c "SELECT version();"
```

---

### Problema 2: Conflicto de puerto 5432

**Síntoma:**
```
Error: port is already allocated
```

**Causa:** PostgreSQL local está corriendo.

**Solución:**
```bash
# Detener PostgreSQL local
brew services stop postgresql@16
brew services stop postgresql

# Verificar que el puerto esté libre
lsof -i :5432
# No debe mostrar nada

# Reiniciar Docker
docker-compose down
docker-compose up -d
```

---

### Problema 3: "Failed to connect to localhost port 8080" o "404 Not Found"

**Síntoma:**
```
curl: (7) Failed to connect to localhost port 8080
# O
404 - Resource Not Found
```

**Causas posibles:**
1. La aplicación no está corriendo
2. Estás usando URLs sin el prefijo `/q/`

**Solución:**
```bash
# 1. Verificar que la app esté corriendo
lsof -i :8080
# Debe mostrar el proceso Java

# 2. Usar la URL CORRECTA con /q/
curl http://localhost:8080/q/health/ready

# 3. Si los scripts fallan, verificar que usen /q/ en las URLs
grep "/health\|/metrics" test-aprobacion.sh
# Debe mostrar: /q/health/ready, /q/metrics
```

---

### Problema 4: "exec format error" en Native

**Síntoma:**
```
zsh: exec format error: ./target/aprobacion-express-1.0.0-runner
```

**Causa:** El ejecutable Native se compiló para Linux (con Docker) pero estás en macOS.

**Solución:**
```bash
# Instalar GraalVM localmente (ver sección anterior)
sdk install java 21.0.1-graalce
sdk use java 21.0.1-graalce

# Recompilar sin Docker
./mvnw clean package -Pnative -DskipTests
```

---

### Problema 5: Test script no encuentra el servicio

**Síntoma:**
```
El servicio no está disponible en http://localhost:8080
```

**Causa:** Olvidaste iniciar la aplicación antes de ejecutar el test.

**Solución:**
```bash
# Terminal 1: Iniciar aplicación
./mvnw quarkus:dev

# Esperar a que arranque (ver mensaje "Listening on")

# Terminal 2: Ejecutar tests (DESPUÉS de que arranque)
./test-aprobacion.sh
```

---

### Problema 6: Benchmark falla en JVM

**Síntoma:**
Benchmark se queda esperando en "Esperando que el servicio esté listo..."

**Causa:** El JAR tiene configuraciones viejas empaquetadas o las URLs son incorrectas.

**Solución:**
```bash
# Limpiar completamente
rm -rf target/
rm -rf ~/.m2/repository/pe/banco/

# Recompilar desde cero
./mvnw clean package -DskipTests

# Probar que funciona
java -jar target/quarkus-app/quarkus-run.jar

# Si arranca OK, detenerlo (Ctrl+C) y ejecutar benchmark
./benchmark.sh
```

---

## 🎓 Conceptos Clave para la Clase

### 1. Dev Services vs JAR Empaquetado

**⚠️ CONCEPTO CRÍTICO para entender por qué a veces funciona y a veces no:**

#### Con `./mvnw quarkus:dev` (Modo Desarrollo)

```
┌─────────────────────────────────────────┐
│  ./mvnw quarkus:dev                     │
│                                         │
│  Dev Services (AUTOMÁTICO):             │
│  - Detecta que necesitas PostgreSQL    │
│  - IGNORA application.properties        │
│  - Levanta PostgreSQL en Docker         │
│  - Configura todo automáticamente       │
│  - Lo destruye al terminar              │
│                                         │
│  ✅ SIEMPRE FUNCIONA                    │
│  ✅ NO necesitas docker-compose         │
│  ✅ NO importa qué usuario/password     │
└─────────────────────────────────────────┘
```

#### Con `java -jar` (JAR Empaquetado)

```
┌─────────────────────────────────────────┐
│  java -jar app.jar                      │
│                                         │
│  Sin Dev Services:                      │
│  - Lee application.properties           │
│  - Se conecta a la URL configurada      │
│  - NECESITA que PostgreSQL exista       │
│  - FALLA si no hay BD                   │
│                                         │
│  ❌ Requiere PostgreSQL externo         │
│  ✅ Usar docker-compose                 │
│  ⚠️  Credenciales deben coincidir       │
└─────────────────────────────────────────┘
```

**Por eso:**
- `test-aprobacion.sh` funciona siempre (usa `quarkus:dev`)
- `benchmark.sh` requiere docker-compose (usa `java -jar`)

### 2. ¿Por qué usar docker-compose?

**Analogía:** Imagina que necesitas una impresora:

- **Dev Services** = Impresora que aparece mágicamente cuando la necesitas y desaparece al terminar
- **docker-compose** = Impresora que instalas una vez y la usas cuando quieras

**Casos de uso:**
- Desarrollo rápido → Dev Services ✅
- Benchmarks, CI/CD, producción → docker-compose ✅

### 3. JVM vs Native - ¿Cuándo usar cada uno?

| Criterio | JVM | Native |
|----------|-----|--------|
| **Desarrollo local** | ✅ Recomendado | ❌ Compilación lenta |
| **Arranque rápido** | ❌ 2-3 segundos | ✅ <1 segundo |
| **Memoria** | ❌ 200-300 MB | ✅ 50-80 MB |
| **Cloud/Contenedores** | ⚠️ Costoso | ✅ Ahorro significativo |
| **Serverless (Lambda)** | ❌ No viable | ✅ Ideal |
| **Debugging** | ✅ Completo | ⚠️ Limitado |

### 4. Endpoints de Quarkus

**IMPORTANTE:** Quarkus usa el prefijo `/q/` para endpoints de framework:

```bash
# Health checks
curl http://localhost:8080/q/health          # Estado general
curl http://localhost:8080/q/health/live     # Liveness probe
curl http://localhost:8080/q/health/ready    # Readiness probe

# Métricas
curl http://localhost:8080/q/metrics         # Prometheus metrics

# Dev UI (solo en modo dev)
http://localhost:8080/q/dev                  # Dev UI
```

**Tus endpoints de negocio NO usan `/q/`:**
```bash
curl http://localhost:8080/api/preaprobacion/estadisticas
```

---

## 📚 Para Después de la Clase

### Reactivar PostgreSQL Local

Después del benchmark, si necesitas tu PostgreSQL local de nuevo:

```bash
# Detener Docker
docker-compose down

# Reiniciar PostgreSQL local
brew services start postgresql@16
```

### Desinstalar GraalVM (si quieres)

```bash
# Ver versiones instaladas
sdk list java

# Desinstalar GraalVM
sdk uninstall java 21.0.1-graalce

# Volver a tu Java normal
sdk default java
```

### Limpiar Todo

```bash
# Detener y eliminar contenedores + volúmenes
docker-compose down -v

# Limpiar builds de Maven
./mvnw clean

# Eliminar logs temporales
rm /tmp/build-*.log
rm /tmp/*-run.log
```

---

## 🎯 Checklist Pre-Clase

Antes de tu clase, verifica:

- [ ] PostgreSQL local detenido: `brew services stop postgresql@16`
- [ ] Docker Desktop corriendo
- [ ] Puerto 5432 libre: `lsof -i :5432` (vacío)
- [ ] Puerto 8080 libre: `lsof -i :8080` (vacío)
- [ ] PostgreSQL Docker levantado: `docker-compose up -d`
- [ ] Usuario postgres existe: `docker exec banco-postgres psql -U postgres -c "SELECT 1;"`
- [ ] Scripts con permisos: `chmod +x test-aprobacion.sh benchmark.sh`
- [ ] GraalVM activado (si harás benchmark): `sdk use java 21.0.1-graalce`
- [ ] Test funciona: `./test-aprobacion.sh` debe dar 13/13 ✅
- [ ] URLs correctas en scripts: `grep "/q/health" test-aprobacion.sh`

---

## 🆘 Si Algo Sale Mal en Clase

**PLAN B - Alternativa Segura:**

Si el benchmark da problemas:

1. **Ejecuta solo test-aprobacion.sh** (99% confiable)
2. **Muestra resultados pre-generados del benchmark** (los que tienes guardados)
3. **Explica teoría con slides** en lugar de demo en vivo

**Comandos de Emergencia:**

```bash
# Resetear TODO
docker-compose down -v
docker-compose up -d
sleep 10
./mvnw clean
pkill -f quarkus

# Verificar estado
docker ps
lsof -i :5432
lsof -i :8080
```

---

## Recursos

- **Documentación Quarkus:** https://quarkus.io/guides/
- **GraalVM:** https://www.graalvm.org


---

## 📄 Licencia

[Tu licencia aquí]

---

**Última actualización:** 2025-10-23  
**Versión:** 1.0.1  
**Autor:** NETEC
