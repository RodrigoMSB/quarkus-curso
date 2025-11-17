# 📊 Capítulo 10_1: Monitoreo con Grafana y Kibana en Quarkus

## Guía de Instalación y Ejecución

---

## 📋 Contenido

1. [Requisitos Previos](#requisitos-previos)
2. [Instalación de Software](#instalación-de-software)
3. [Descargar el Proyecto](#descargar-el-proyecto)
4. [Levantar la Infraestructura](#levantar-la-infraestructura)
5. [Compilar y Ejecutar los Microservicios](#compilar-y-ejecutar-los-microservicios)
6. [Verificar el Sistema](#verificar-el-sistema)
7. [Acceder a las Herramientas de Monitoreo](#acceder-a-las-herramientas-de-monitoreo)
8. [Probar el Sistema](#probar-el-sistema)
9. [Detener el Sistema](#detener-el-sistema)
10. [Troubleshooting](#troubleshooting)

---

## 💻 Requisitos Previos

### Software Necesario

| Software | Versión Requerida | Propósito |
|----------|-------------------|-----------|
| **Java JDK** | 21 | Ejecutar Quarkus |
| **Maven** | 3.8+ | Compilar proyectos |
| **Docker Desktop** | 20.10+ | Contenedores (BD, Redis, Monitoreo) |
| **Git** | Cualquiera | Clonar repositorio |
| **GitBash** (Windows) | Última versión | Terminal compatible con comandos Unix |

### Recursos del Sistema

- **RAM**: 8 GB mínimo (12 GB recomendado)
- **Disco**: 5 GB libres
- **Puertos libres**: 5433, 6379, 8080, 8081, 8082, 9090, 3000, 9200, 5044, 5601

---

## 🔧 Instalación de Software

### ⚠️ IMPORTANTE para Estudiantes de Windows

**Todos los comandos en este README deben ejecutarse en GitBash, NO en CMD o PowerShell.**

GitBash viene incluido con Git for Windows y permite ejecutar comandos Unix en Windows.

---

### Para Windows

#### 1. Verificar Java JDK 21

**Java 21 ya debe estar instalado en tu sistema.**

```bash
# Verificar instalación (en GitBash):
java -version

# Debe mostrar: java version "21.x.x"
```

**⚠️ Si no muestra Java 21:**
- Contacta al instructor
- Verifica que JAVA_HOME apunte a Java 21

#### 2. Instalar Maven

```bash
# Descargar Maven desde:
https://maven.apache.org/download.cgi

# Descargar el archivo: apache-maven-3.9.x-bin.zip
# Extraer en: C:\Program Files\Apache\maven

# Agregar al PATH (GitBash):
export PATH="/c/Program Files/Apache/maven/bin:$PATH"

# Para hacerlo permanente, agregar al archivo ~/.bashrc:
echo 'export PATH="/c/Program Files/Apache/maven/bin:$PATH"' >> ~/.bashrc

# Verificar
mvn -version
```

#### 3. Instalar Docker Desktop

```bash
# 1. Descargar Docker Desktop desde:
https://www.docker.com/products/docker-desktop

# 2. Ejecutar el instalador y seguir el wizard
# 3. Reiniciar el equipo si es necesario
# 4. Abrir Docker Desktop y esperar a que inicie

# 5. Verificar en GitBash:
docker --version
docker ps

# Debe mostrar la versión y no mostrar errores
```

#### 4. Instalar Git (si no lo tienes)

```bash
# Descargar Git for Windows (incluye GitBash) desde:
https://git-scm.com/download/win

# Ejecutar el instalador con opciones por defecto
```

---

### Para macOS

#### 1. Verificar Java JDK 21

**Java 21 ya debe estar instalado en tu sistema.**

```bash
# Verificar instalación:
java -version

# Debe mostrar: java version "21.x.x"
```

**⚠️ Si no muestra Java 21:**
- Contacta al instructor
- Verifica que JAVA_HOME apunte a Java 21

#### 2. Instalar Maven

```bash
# Con Homebrew
brew install maven

# Verificar
mvn -version
```

#### 3. Instalar Docker Desktop

```bash
# 1. Descargar Docker Desktop para Mac desde:
https://www.docker.com/products/docker-desktop

# 2. Abrir el archivo .dmg descargado
# 3. Arrastrar Docker a la carpeta Applications
# 4. Abrir Docker Desktop desde Applications
# 5. Esperar a que inicie

# Verificar en terminal:
docker --version
docker ps
```

#### 4. Instalar Git

```bash
# Git viene preinstalado en macOS, pero puedes actualizarlo:
brew install git

# Verificar
git --version
```

---

## 📥 Descargar el Proyecto

### Windows (GitBash) y macOS

Los comandos son **idénticos** en ambos sistemas:

```bash
# Opción A: Clonar desde repositorio Git
git clone <url-del-repositorio>
cd CAPITULO_10_MONITORING

# Opción B: Desde ZIP descargado
# 1. Descargar y descomprimir el ZIP
# 2. Abrir GitBash (Windows) o Terminal (macOS)
# 3. Navegar a la carpeta:
cd ruta/a/CAPITULO_10_MONITORING
```

### Verificar Estructura del Proyecto

```bash
# Ver contenido del directorio
ls -la

# Deberías ver:
# - docker-compose-monitoring.yml
# - order-service/
# - inventory-service/
# - payment-service/
# - prometheus/
# - grafana/
# - logstash/
# - demo-monitoring.sh
# - README.md
```

---

## 🐳 Levantar la Infraestructura

### Paso 1: Iniciar Docker Desktop

#### Windows
1. Abrir Docker Desktop desde el menú de inicio
2. Esperar a que el ícono de Docker en la barra de tareas muestre "Docker Desktop is running"

#### macOS
1. Abrir Docker Desktop desde Applications
2. Esperar a que muestre "Docker Desktop is running" en la barra superior

### Paso 2: Levantar Contenedores

**Windows (GitBash) y macOS - Mismo comando:**

```bash
# Desde la raíz del proyecto
docker-compose -f docker-compose-monitoring.yml up -d

# El parámetro -d significa "detached" (en segundo plano)
```

**Salida esperada:**
```
Creating network "capitulo_10_monitoring_default" with the default driver
Creating postgres-db ... done
Creating redis-cache ... done
Creating prometheus ... done
Creating grafana ... done
Creating elasticsearch ... done
Creating logstash ... done
Creating kibana ... done
Creating filebeat ... done
```

### Paso 3: Verificar que los Contenedores Estén Corriendo

**Windows (GitBash) y macOS - Mismo comando:**

```bash
docker ps

# Deberías ver 8 contenedores con STATUS "Up"
```

### Paso 4: Esperar a que Elasticsearch y Kibana Inicien

```bash
# Elasticsearch tarda ~1 minuto
# Kibana tarda ~2 minutos

# Verificar logs de Elasticsearch:
docker logs elasticsearch

# Cuando esté listo, verás:
# "started"

# Verificar logs de Kibana:
docker logs kibana

# Cuando esté listo, verás:
# "Server running at http://0:5601"
```

**⏱️ Recomendación:** Espera 2-3 minutos antes de continuar al siguiente paso.

---

## ⚙️ Compilar y Ejecutar los Microservicios

### Paso 1: Compilar los Proyectos

**Windows (GitBash) y macOS - Mismo comando:**

```bash
# Desde la raíz del proyecto
mvn clean package -DskipTests

# Esto compilará los 3 microservicios
# Puede tomar 2-3 minutos la primera vez
```

**Salida esperada:**
```
[INFO] BUILD SUCCESS
```

### Paso 2: Ejecutar los 3 Microservicios

Necesitas abrir **3 terminales diferentes** (GitBash en Windows, Terminal en macOS).

#### Terminal 1 - Inventory Service

```bash
# Navegar a la carpeta del servicio
cd inventory-service

# Iniciar en modo desarrollo
mvn quarkus:dev
```

**Espera a ver:**
```
Listening on: http://localhost:8081
```

#### Terminal 2 - Payment Service

```bash
# Navegar a la carpeta del servicio
cd payment-service

# Iniciar en modo desarrollo
mvn quarkus:dev
```

**Espera a ver:**
```
Listening on: http://localhost:8082
```

#### Terminal 3 - Order Service

```bash
# Navegar a la carpeta del servicio
cd order-service

# Iniciar en modo desarrollo
mvn quarkus:dev
```

**Espera a ver:**
```
Listening on: http://localhost:8080
```

---

## ✅ Verificar el Sistema

### Verificar que los Microservicios Estén Activos

**Windows (GitBash) y macOS - Mismos comandos:**

```bash
# Verificar cada microservicio:
curl http://localhost:8080/q/health
curl http://localhost:8081/q/health
curl http://localhost:8082/q/health

# Cada uno debe responder:
# {"status":"UP"}
```

### Verificar que las Métricas Estén Disponibles

```bash
# Verificar endpoints de métricas:
curl http://localhost:8080/q/metrics | head -20
curl http://localhost:8081/q/metrics | head -20
curl http://localhost:8082/q/metrics | head -20

# Deberías ver métricas en formato Prometheus
```

---

## 🖥️ Acceder a las Herramientas de Monitoreo

Abre tu navegador web (Chrome, Firefox, Safari, Edge) y accede a:

### 1. Prometheus (Métricas)

```
URL: http://localhost:9090

Verificar:
1. Ir a: Status > Targets
2. Los 3 microservicios deben estar en estado "UP"
```

### 2. Grafana (Visualización)

```
URL: http://localhost:3000

Credenciales por defecto:
- Usuario: admin
- Password: admin

Primera vez:
- Te pedirá cambiar la contraseña
- Puedes omitir este paso (Skip)
```

### 3. Kibana (Logs)

```
URL: http://localhost:5601

Configuración inicial:
1. Espera a que cargue completamente (~1 minuto)
2. Verás el home de Kibana
```

---

## 🧪 Probar el Sistema

### Opción 1: Usar el Script de Demo (Recomendado)

#### Windows (GitBash)

```bash
# Dar permisos de ejecución al script
chmod +x demo-monitoring.sh

# Ejecutar el script
./demo-monitoring.sh
```

#### macOS

```bash
# Dar permisos de ejecución al script
chmod +x demo-monitoring.sh

# Ejecutar el script
./demo-monitoring.sh
```

El script creará automáticamente órdenes de prueba y generará tráfico para que puedas ver las métricas y logs.

### Opción 2: Pruebas Manuales con cURL

**Windows (GitBash) y macOS - Mismos comandos:**

#### 1. Consultar Productos Disponibles

```bash
curl http://localhost:8081/api/products

# Debería retornar lista de productos en JSON
```

#### 2. Crear una Orden (Happy Path)

```bash
# JSON en una sola línea (compatible Mac + Windows GitBash)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":"test-user-001","paymentMethod":"credit_card","items":[{"productCode":"LAPTOP-001","quantity":2}]}'

# Respuesta esperada: Orden creada con status: COMPLETED
```

**💡 Alternativa con archivo temporal (más legible):**
```bash
# Crear archivo temporal con el JSON
cat > /tmp/order-success.json << 'EOF'
{"userId":"test-user-001","paymentMethod":"credit_card","items":[{"productCode":"LAPTOP-001","quantity":2}]}
EOF

# Enviar request
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  --data-binary "@/tmp/order-success.json"
```

#### 3. Simular Fallo en Pago (SAGA Compensation)

```bash
# JSON en una sola línea (compatible Mac + Windows GitBash)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":"test-user-002","paymentMethod":"credit_card","items":[{"productCode":"LAPTOP-001","quantity":10000}]}'

# Respuesta esperada: Orden fallida con status: FAILED
# SAGA ejecutará compensación liberando el inventario reservado
```

**💡 Alternativa con archivo temporal:**
```bash
# Cantidad imposible para forzar fallo
cat > /tmp/order-fail.json << 'EOF'
{"userId":"test-user-002","paymentMethod":"credit_card","items":[{"productCode":"LAPTOP-001","quantity":10000}]}
EOF

curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  --data-binary "@/tmp/order-fail.json"
```

#### 4. Verificar Cache de Redis

```bash
# Primera consulta (CACHE MISS)
curl http://localhost:8081/api/products/LAPTOP-001

# Segunda consulta (CACHE HIT - más rápida)
curl http://localhost:8081/api/products/LAPTOP-001
```

---

## 📊 Ver Métricas en Grafana

### Crear tu Primer Dashboard

1. Abrir Grafana: http://localhost:3000
2. Click en "+" (plus) en el menú izquierdo
3. Seleccionar "Dashboard"
4. Click en "Add new panel"
5. En el campo Query, escribir:

```promql
rate(http_server_requests_seconds_count{job="order-service"}[1m])
```

6. Click en "Apply"
7. Ya tienes tu primera gráfica mostrando requests por segundo

### Queries Útiles para Copiar y Pegar

#### Latencia P95 por Servicio

```promql
histogram_quantile(0.95, 
  sum by (job) (rate(http_server_requests_seconds_bucket[5m]))
)
```

#### Memoria JVM Usada

```promql
jvm_memory_used_bytes{area="heap"}
```

#### Error Rate

```promql
rate(http_server_requests_seconds_count{status=~"5.."}[5m]) 
/ 
rate(http_server_requests_seconds_count[5m])
```

---

## 🔍 Ver Logs en Kibana

### Configurar Index Pattern (Primera Vez)

1. Abrir Kibana: http://localhost:5601
2. Ir a: Menu (☰) > Management > Stack Management
3. Click en "Index Patterns" (bajo Kibana)
4. Click en "Create index pattern"
5. En "Index pattern name" escribir: `quarkus-logs-*`
6. Click en "Next step"
7. En "Time field" seleccionar: `@timestamp`
8. Click en "Create index pattern"

### Ver Logs en Tiempo Real

1. Ir a: Menu (☰) > Discover
2. Seleccionar el index pattern: `quarkus-logs-*`
3. Verás los logs de los 3 microservicios en tiempo real

### Filtros Útiles

Escribe estos filtros en la barra de búsqueda de Kibana:

```
# Ver solo errores
level: ERROR

# Ver logs de un servicio específico
service_name: "order-service"

# Ver logs de SAGA
tags: saga

# Ver compensaciones
tags: saga-compensation
```

---

## 🛑 Detener el Sistema

### Detener los Microservicios

En cada una de las 3 terminales donde corren los microservicios:

**Windows (GitBash) y macOS:**
```
Presiona: Ctrl + C
```

### Detener los Contenedores Docker

**Windows (GitBash) y macOS - Mismo comando:**

```bash
# Desde la raíz del proyecto
docker-compose -f docker-compose-monitoring.yml down

# Esto detendrá y eliminará todos los contenedores
```

**Para conservar los datos (no eliminar volúmenes):**
```bash
docker-compose -f docker-compose-monitoring.yml stop
```

**Para reiniciar después:**
```bash
docker-compose -f docker-compose-monitoring.yml start
```

---

## 🔧 Troubleshooting

### Problema 1: "Port already in use"

**Síntoma:**
```
Error: Port 8080 already in use
```

**Solución Windows (GitBash):**
```bash
# Ver qué proceso usa el puerto
netstat -ano | findstr :8080

# Matar el proceso (reemplazar <PID> con el número que aparece)
taskkill /PID <PID> /F
```

**Solución macOS:**
```bash
# Ver qué proceso usa el puerto
lsof -i :8080

# Matar el proceso
kill -9 <PID>
```

---

### Problema 2: Docker no inicia contenedores

**Síntoma:**
```
Error: Cannot connect to the Docker daemon
```

**Solución Windows:**
1. Abrir Docker Desktop
2. Esperar a que el ícono muestre "Docker Desktop is running"
3. Reintentar el comando

**Solución macOS:**
1. Abrir Docker Desktop desde Applications
2. Esperar a que inicie completamente
3. Reintentar el comando

---

### Problema 3: Maven no se encuentra (Windows)

**Síntoma:**
```bash
mvn: command not found
```

**Solución:**
```bash
# Verificar que Maven esté en el PATH
echo $PATH

# Si no aparece Maven, agregarlo:
export PATH="/c/Program Files/Apache/maven/bin:$PATH"

# Hacerlo permanente:
echo 'export PATH="/c/Program Files/Apache/maven/bin:$PATH"' >> ~/.bashrc

# Recargar configuración:
source ~/.bashrc
```

---

### Problema 4: Java no se encuentra

**Síntoma:**
```bash
java: command not found
```

**Solución Windows:**
```bash
# Verificar instalación
"C:\Program Files\Java\jdk-21\bin\java.exe" -version

# Si funciona, agregar al PATH en ~/.bashrc:
export JAVA_HOME="/c/Program Files/Java/jdk-21"
export PATH="$JAVA_HOME/bin:$PATH"

source ~/.bashrc
```

**Solución macOS:**
```bash
# Verificar dónde está instalado Java
/usr/libexec/java_home -V

# Configurar JAVA_HOME en ~/.zshrc o ~/.bash_profile:
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH=$JAVA_HOME/bin:$PATH

source ~/.zshrc
```

---

### Problema 5: Elasticsearch sin memoria

**Síntoma:**
```bash
docker logs elasticsearch
# ERROR: OutOfMemoryError
```

**Solución (Windows y macOS):**

Editar el archivo `docker-compose-monitoring.yml`:

```yaml
elasticsearch:
  environment:
    - "ES_JAVA_OPTS=-Xms1g -Xmx1g"  # Aumentar heap
```

Luego reiniciar:
```bash
docker-compose -f docker-compose-monitoring.yml down
docker-compose -f docker-compose-monitoring.yml up -d
```

---

### Problema 6: Kibana no carga

**Síntoma:**
Kibana muestra "Kibana server is not ready yet"

**Solución:**
```bash
# Verificar que Elasticsearch esté corriendo
docker logs elasticsearch

# Debe mostrar: "started"

# Reiniciar Kibana
docker restart kibana

# Esperar 2 minutos y recargar http://localhost:5601
```

---

### Problema 7: GitBash no ejecuta scripts .sh (Windows)

**Síntoma:**
```bash
./demo-monitoring.sh
bash: ./demo-monitoring.sh: Permission denied
```

**Solución:**
```bash
# Dar permisos de ejecución
chmod +x demo-monitoring.sh

# Ejecutar
./demo-monitoring.sh

# Si aún falla, ejecutar con bash explícito:
bash demo-monitoring.sh
```

---

## 📞 Soporte

Si tienes problemas no resueltos en este documento:

1. Revisa los logs de los contenedores:
   ```bash
   docker logs <nombre-contenedor>
   ```

2. Verifica que todos los puertos estén libres:
   ```bash
   # Windows (GitBash)
   netstat -ano | findstr "8080 8081 8082 9090 3000 5601"
   
   # macOS
   lsof -i :8080,8081,8082,9090,3000,5601
   ```

3. Reinicia todo desde cero:
   ```bash
   # Detener servicios (Ctrl+C en cada terminal)
   
   # Detener y limpiar Docker
   docker-compose -f docker-compose-monitoring.yml down -v
   
   # Reiniciar Docker Desktop
   
   # Volver a empezar desde "Levantar la Infraestructura"
   ```

---

## ✅ Checklist de Verificación

Antes de empezar el ejercicio, verifica que tengas TODO instalado:

- [ ] Java 21 instalado (`java -version`)
- [ ] Maven instalado (`mvn -version`)
- [ ] Docker Desktop corriendo (`docker ps`)
- [ ] GitBash funcionando (solo Windows)
- [ ] Todos los puertos libres
- [ ] 8 GB RAM disponibles
- [ ] Proyecto descargado y descomprimido

---

**¡Listo para empezar el ejercicio! 🚀**

Consulta el archivo `TEORIA.md` para entender los conceptos detrás de este ejercicio.