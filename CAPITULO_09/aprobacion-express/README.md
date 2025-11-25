# 🏦 Sistema de Pre-Aprobación Crediticia Express

## 📋 Tabla de Contenidos

1. [Descripción del Proyecto](#descripción-del-proyecto)
2. [Requisitos](#requisitos)
3. [Inicio Rápido](#inicio-rápido)
4. [Ejecución de Tests Funcionales](#ejecución-de-tests-funcionales)
5. [Ejecución del Benchmark JVM vs Native](#ejecución-del-benchmark-jvm-vs-native)
6. [Troubleshooting](#troubleshooting)
7. [Conceptos Clave](#conceptos-clave)

---

## 📌 Descripción del Proyecto

Sistema bancario de pre-aprobación crediticia que evalúa solicitudes en menos de 200ms.

**Tecnologías:**
- Quarkus 3.15+
- PostgreSQL 16
- Hibernate ORM + Panache
- REST + Jackson
- Micrometer (métricas)
- SmallRye Health
- GraalVM Native (compilación en Docker)

**Scripts incluidos:**
- `test-aprobacion.sh` - 11 pruebas funcionales (~5 min)
- `benchmark.sh` - Comparación JVM vs Native (~15 min)

---

## ⚠️ Requisitos

### Lo Único que Necesitas: Docker Desktop

```bash
# Verificar Docker
docker --version    # Docker 20+
docker info         # Debe estar corriendo
```

**No necesitas instalar:**
- ❌ Java (Docker lo incluye)
- ❌ Maven (Docker lo incluye)
- ❌ GraalVM (Docker lo incluye)
- ❌ PostgreSQL (Docker lo levanta)

### Verificar Puertos Libres

```bash
# Mac/Linux
lsof -i :5432  # Debe estar vacío
lsof -i :8080  # Debe estar vacío

# Si PostgreSQL local está corriendo, detenerlo:
brew services stop postgresql@16
brew services stop postgresql
```

### Windows (Git Bash)

- Instalar [Docker Desktop para Windows](https://www.docker.com/products/docker-desktop/)
- Usar Git Bash como terminal
- Si hay errores de sintaxis en scripts:
  ```bash
  sed -i 's/\r$//' test-aprobacion.sh
  sed -i 's/\r$//' benchmark.sh
  ```

---

## 🚀 Inicio Rápido

### Opción A: Todo Automático (Recomendado)

```bash
# 1. Ir al proyecto
cd ~/QUARKUS/"CAPITULO 9"/aprobacion-express

# 2. Ejecutar pruebas (levanta PostgreSQL + Quarkus automáticamente)
chmod +x test-aprobacion.sh
./test-aprobacion.sh --docker
```

**Eso es todo.** El script hace todo:
1. ✅ Levanta PostgreSQL (docker-compose)
2. ✅ Construye imagen Docker de Quarkus
3. ✅ Ejecuta 11 pruebas funcionales
4. ✅ Muestra resultados
5. ✅ Limpia todo al terminar

### Opción B: Modo Desarrollo (con Java local)

Si tienes Java 17+ y Maven instalados:

```bash
# 1. Ir al proyecto
cd ~/QUARKUS/"CAPITULO 9"/aprobacion-express

# 2. Ejecutar pruebas (JVM local, más rápido)
chmod +x test-aprobacion.sh
./test-aprobacion.sh
```

---

## 🧪 Ejecución de Tests Funcionales

### Comando Principal

```bash
# Con Docker (recomendado para Windows)
./test-aprobacion.sh --docker

# Con JVM local (más rápido, requiere Java + Maven)
./test-aprobacion.sh

# Mantener PostgreSQL corriendo al terminar
./test-aprobacion.sh --keep-db
./test-aprobacion.sh --docker --keep-db
```

### ⏱️ Tiempo Estimado

| Modo | Tiempo |
|------|--------|
| JVM local | ~3-4 min |
| Docker | ~5-6 min |

### Pruebas Incluidas (11 tests)

| # | Prueba | Esperado |
|---|--------|----------|
| 1 | Health check - Liveness | 200 OK |
| 2 | Health check - Readiness | 200 OK |
| 3 | Estadísticas del sistema | 200 OK |
| 4 | Cliente perfil excelente | APROBADO |
| 5 | Cliente con garantía | APROBADO |
| 6 | Cliente lista negra | RECHAZADO |
| 7 | Cliente deuda alta | RECHAZADO |
| 8 | Validación ingreso negativo | 400 Error |
| 9 | Validación edad mínima | 400 Error |
| 10 | Consultar solicitud inexistente | 404 Not Found |
| 11 | Listar solicitudes | 200 OK |

### Resultado Esperado

```
+--------------------------------------------------------------+
|                    RESULTADOS FINALES                        |
+--------------------------------------------------------------+
| Total de pruebas:    11                                      |
| Pruebas exitosas:    11                                      |
| Pruebas fallidas:    0                                       |
+--------------------------------------------------------------+

[OK] TODAS LAS PRUEBAS PASARON EXITOSAMENTE!
```

### Archivos Generados

```bash
test-report-2025-11-24-HHMMSS.txt    # Reporte detallado
```

---

## 📊 Ejecución del Benchmark JVM vs Native

### ¿Qué Hace el Benchmark?

Compara **JVM vs Native**, ambos en Docker:

1. Levanta PostgreSQL automáticamente
2. Construye imagen JVM (`Dockerfile.jvm`)
3. Mide: arranque, memoria, throughput
4. Construye imagen Native (`Dockerfile.native`) - **GraalVM incluido**
5. Mide: arranque, memoria, throughput
6. Muestra tabla comparativa

**No necesitas instalar GraalVM.** Docker lo incluye en la imagen de build.

### Comando

```bash
# Dar permisos (solo la primera vez)
chmod +x benchmark.sh

# Ejecutar con 500 requests (por defecto)
./benchmark.sh

# Ejecutar con más requests
./benchmark.sh 1000
```

### ⏱️ Tiempo Estimado

| Fase | Tiempo |
|------|--------|
| Build JVM | ~1-2 min |
| Pruebas JVM | ~1 min |
| **Build Native** | **5-10 min** (GraalVM compila dentro de Docker) |
| Pruebas Native | ~1 min |
| **Total** | **~10-15 min** |

### Durante la Compilación Native es NORMAL que:
- El proceso tarde varios minutos
- Parezca "pegado" en algunos pasos
- **NO INTERRUMPIR**

### Resultado Esperado

```
+------------------------------------------------------------------------------+
|                        RESULTADOS DEL BENCHMARK                              |
|                        (500 requests)                                        |
+------------------------------------------------------------------------------+
| METRICA                     | JVM (Docker)       | NATIVE (Docker)    |
+------------------------------------------------------------------------------+
| Tiempo de build             |                37s |               199s |
| Tiempo de arranque          |            1808 ms |             127 ms |
| Uso de memoria              |             238 MB |              17 MB |
| Throughput                  |           50 req/s |           71 req/s |
| Tamano imagen               |             705 MB |             430 MB |
+------------------------------------------------------------------------------+

ANALISIS:
1. BUILD: Native 5x mas lento (pero solo una vez en CI/CD)
2. ARRANQUE: Native 14x MAS RAPIDO
3. MEMORIA: Native usa 92% MENOS
4. THROUGHPUT: Rendimiento similar

   *** NATIVE CLARAMENTE SUPERIOR para produccion ***

AHORRO (50 microservicios):
   JVM: 11900 MB (~11 GB)
   Native: 850 MB (~0 GB)
   Ahorro: ~10 GB
```

### Archivos Generados

```bash
benchmark-report-2025-11-24-HHMMSS.txt    # Reporte completo
```

---

## 🐛 Troubleshooting

### Error: "Docker no esta corriendo"

```bash
# Solución: Iniciar Docker Desktop
# Windows: Buscar "Docker Desktop" en menú inicio
# Mac: Abrir Docker Desktop desde Applications

# Verificar
docker info
```

### Error en Windows: "syntax error near unexpected token"

```bash
# El archivo tiene finales de línea Windows (CRLF)
# Solución: Convertir a Unix (LF)
sed -i 's/\r$//' benchmark.sh
sed -i 's/\r$//' test-aprobacion.sh
```

### Error: "port 5432 already in use"

```bash
# PostgreSQL local está corriendo
# Mac:
brew services stop postgresql@16
brew services stop postgresql

# Verificar
lsof -i :5432  # Debe estar vacío
```

### Error: "port 8080 already in use"

```bash
# Algo está usando el puerto
# Mac/Linux:
lsof -i :8080
kill -9 <PID>

# Windows (PowerShell):
netstat -ano | findstr :8080
taskkill /F /PID <PID>
```

### Error: "PostgreSQL no responde"

```bash
# Verificar contenedor
docker ps | grep postgres

# Ver logs
docker logs banco-postgres

# Reiniciar
docker-compose down -v
docker-compose up -d
sleep 10
```

### Build Native muy lento (>15 min)

```bash
# Normal en primera ejecución (descarga imágenes grandes)
# Verificar recursos de Docker Desktop:
# Settings → Resources → Memory: mínimo 4GB
# Settings → Resources → CPUs: mínimo 2
```

### Error: "Dockerfile.native not found"

```bash
# Verificar que existan los Dockerfiles
ls -la src/main/docker/

# Deben existir:
# - Dockerfile.jvm (multi-stage)
# - Dockerfile.native (multi-stage con GraalVM)
```

---

## 🎓 Conceptos Clave

### 1. ¿Por qué Docker para Todo?

| Antes | Ahora |
|-------|-------|
| Instalar Java 17 | ❌ Docker lo incluye |
| Instalar Maven | ❌ Docker lo incluye |
| Instalar GraalVM + native-image | ❌ Docker lo incluye |
| Instalar PostgreSQL | ❌ Docker lo incluye |
| Configurar JAVA_HOME, PATH | ❌ No necesario |

**Ventaja:** Funciona igual en Mac, Windows y Linux.

### 2. Dockerfiles Multi-Stage

```
Dockerfile.jvm:
┌─────────────────────────────────┐
│ STAGE 1: maven + JDK           │ → Compila con Maven
│ STAGE 2: JRE Alpine            │ → Solo runtime + JAR
└─────────────────────────────────┘
Resultado: ~400 MB, arranque ~2s

Dockerfile.native:
┌─────────────────────────────────┐
│ STAGE 1: GraalVM Mandrel       │ → Compila a binario nativo
│ STAGE 2: UBI Minimal           │ → Solo binario
└─────────────────────────────────┘
Resultado: ~165 MB, arranque ~0.1s
```

### 3. JVM vs Native - ¿Cuándo usar cada uno?

| Criterio | JVM | Native |
|----------|-----|--------|
| **Desarrollo local** | ✅ Hot reload | ❌ Compilación lenta |
| **Arranque** | ❌ 2-3 segundos | ✅ <0.2 segundos |
| **Memoria** | ❌ 200-300 MB | ✅ 15-50 MB |
| **Cloud/K8s** | ⚠️ Costoso | ✅ Ahorro 70-90% |
| **Serverless** | ❌ Cold start malo | ✅ Ideal |
| **Debugging** | ✅ Completo | ⚠️ Limitado |

### 4. Endpoints de Quarkus

```bash
# Health checks (prefijo /q/)
curl http://localhost:8080/q/health/ready
curl http://localhost:8080/q/health/live

# Métricas
curl http://localhost:8080/q/metrics

# Tu API (sin /q/)
curl http://localhost:8080/api/preaprobacion/estadisticas
```

---

## 🎯 Checklist Pre-Clase

```
□ Docker Desktop corriendo: docker info
□ Puertos libres: lsof -i :5432 && lsof -i :8080
□ PostgreSQL local detenido (si aplica)
□ Scripts con permisos: chmod +x *.sh
□ Test funciona: ./test-aprobacion.sh --docker
□ (Opcional) Benchmark probado: ./benchmark.sh 500
```

---

## 📚 Archivos del Proyecto

```
aprobacion-express/
├── docker-compose.yml              # PostgreSQL
├── src/main/docker/
│   ├── Dockerfile.jvm              # Build JVM (multi-stage)
│   └── Dockerfile.native           # Build Native (GraalVM incluido)
├── benchmark.sh                    # Comparativa JVM vs Native
├── test-aprobacion.sh              # Pruebas funcionales
├── README.md                       # Esta guía
├── TEORIA.md                       # Conceptos teóricos
├── INSTRUCTOR.md                   # Guía del profesor
└── GUIA-SCRIPTS-DOCKER.md          # Guía detallada de scripts
```

---

## 🆘 Si Algo Sale Mal

**Plan B - Alternativa Segura:**

```bash
# 1. Resetear TODO
docker-compose down -v
docker system prune -f

# 2. Reintentar
docker-compose up -d
sleep 10
./test-aprobacion.sh --docker
```

**Si el benchmark falla:**
1. Ejecuta solo `test-aprobacion.sh` (más confiable)
2. Muestra resultados pre-generados del benchmark
3. Explica teoría con slides

---

## 📞 Recursos

- **Documentación Quarkus:** https://quarkus.io/guides/
- **GraalVM:** https://www.graalvm.org
- **Docker:** https://docs.docker.com

---

**Última actualización:** 2025-11-24  
**Versión:** 2.0.0  
**Compatibilidad:** macOS, Windows (Git Bash), Linux