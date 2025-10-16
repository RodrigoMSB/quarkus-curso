# 🧪 Guía Completa de Testing - Sistema de Evaluación Crediticia

## 📋 Índice
1. [Requisitos Previos](#requisitos-previos)
2. [Arquitectura de Testing](#arquitectura-de-testing)
3. [Ejecutar la Aplicación](#ejecutar-la-aplicación)
4. [Tests Unitarios (JUnit)](#tests-unitarios-junit)
5. [Tests de API (Script Bash)](#tests-de-api-script-bash)
6. [Interpretación de Resultados](#interpretación-de-resultados)
7. [Troubleshooting](#troubleshooting)

---

## 📦 Requisitos Previos

Antes de ejecutar cualquier test, asegúrate de tener instalado:
```bash
# Verificar Java 21+
java -version

# Verificar Maven
./mvnw --version

# Verificar jq (para formatear JSON en tests de API)
jq --version

# Si jq no está instalado:
# macOS: brew install jq
# Ubuntu: sudo apt-get install jq
# Windows: descargar de https://stedolan.github.io/jq/
```

**Opcional pero recomendado:**
- Docker Desktop (para PostgreSQL)
- Git (para control de versiones)
- IDE: IntelliJ IDEA, VS Code o Eclipse

---

## 🏗️ Arquitectura de Testing

Este proyecto implementa **3 niveles de testing**:
```
┌─────────────────────────────────────────────┐
│     NIVEL 3: Tests de API (E2E)            │
│     - Script Bash (test-api.sh)             │
│     - Pruebas con curl                      │
│     - Validación de respuestas reales       │
└─────────────────────────────────────────────┘
                    ▲
                    │
┌─────────────────────────────────────────────┐
│     NIVEL 2: Tests de Integración          │
│     - CreditoRecursoTest                    │
│     - SolicitudCreditoRepositoryTest        │
│     - Pruebas con BD en memoria             │
└─────────────────────────────────────────────┘
                    ▲
                    │
┌─────────────────────────────────────────────┐
│     NIVEL 1: Tests Unitarios               │
│     - ValidadorDniTest                      │
│     - ScoringServiceTest                    │
│     - Lógica de negocio aislada            │
└─────────────────────────────────────────────┘
```

**Cobertura actual: 44 tests (100% passing)**

---

## 🚀 Ejecutar la Aplicación

### Paso 1: Iniciar PostgreSQL (Docker)
```bash
# Levantar PostgreSQL con Docker Compose
docker-compose up -d

# Verificar que el contenedor esté corriendo
docker ps

# Deberías ver algo como:
# CONTAINER ID   IMAGE         PORTS                    NAMES
# abc123...      postgres:13   0.0.0.0:5432->5432/tcp   evaluacion-postgres
```

**Alternativa sin Docker:**
Si tienes PostgreSQL instalado localmente, edita `application.properties`:
```properties
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/tu_base_datos
quarkus.datasource.username=tu_usuario
quarkus.datasource.password=tu_password
```

### Paso 2: Compilar el Proyecto
```bash
# Limpiar y compilar
./mvnw clean package

# Esto generará:
# - target/evaluacion-crediticia-1.0.0-SNAPSHOT.jar
# - Ejecutará todos los tests (44 tests)
```

**Salida esperada:**
```
[INFO] Tests run: 44, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Paso 3: Iniciar la Aplicación en Modo Desarrollo
```bash
# Modo desarrollo con hot-reload
./mvnw quarkus:dev

# Espera a ver este mensaje:
# Listening on: http://localhost:8080
# 
# La aplicación está lista cuando veas:
# __  ____  __  _____   ___  __ ____  ______ 
#  --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
#  -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
# --\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
```

**Características del modo `quarkus:dev`:**
- ✅ Hot reload: cambios en código se aplican automáticamente
- ✅ Dev UI: http://localhost:8080/q/dev/
- ✅ Swagger UI: http://localhost:8080/q/swagger-ui/
- ✅ Health Check: http://localhost:8080/q/health/

### Paso 4: Verificar que la API Responde
```bash
# En otra terminal, prueba el health check
curl http://localhost:8080/q/health

# Respuesta esperada:
# {
#     "status": "UP",
#     "checks": [...]
# }
```

---

## 🧪 Tests Unitarios (JUnit)

### ¿Qué son los Tests Unitarios?

Los tests unitarios verifican **componentes individuales** de forma aislada:
- Validadores (DNI)
- Servicios (Scoring)
- Repositorios (Base de datos)
- Endpoints REST

### Ejecutar TODOS los Tests
```bash
# Ejecutar todos los tests del proyecto
./mvnw test

# Esto ejecutará:
# ✅ ValidadorDniTest (8 tests)
# ✅ ScoringServiceTest (12 tests)
# ✅ SolicitudCreditoRepositoryTest (12 tests)
# ✅ CreditoRecursoTest (12 tests)
# TOTAL: 44 tests
```

**Salida esperada:**
```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running pe.banco.evaluacion.validadores.ValidadorDniTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running pe.banco.evaluacion.servicios.ScoringServiceTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running pe.banco.evaluacion.repositorios.SolicitudCreditoRepositoryTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running pe.banco.evaluacion.recursos.CreditoRecursoTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 44, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] BUILD SUCCESS
```

### Ejecutar Tests Específicos
```bash
# Solo tests de validación de DNI
./mvnw test -Dtest=ValidadorDniTest

# Solo tests de scoring
./mvnw test -Dtest=ScoringServiceTest

# Solo tests de repositorio
./mvnw test -Dtest=SolicitudCreditoRepositoryTest

# Solo tests de endpoints REST
./mvnw test -Dtest=CreditoRecursoTest
```

### Ejecutar UN Test Específico
```bash
# Ejecutar un método de test específico
./mvnw test -Dtest=ValidadorDniTest#deberiaValidarDniCorrecto

# Sintaxis: -Dtest=NombreClase#nombreMetodo
```

### Ver Reportes de Tests
```bash
# Los reportes se generan en:
cat target/surefire-reports/TEST-*.xml

# O ver el resumen en HTML (si tienes navegador):
open target/surefire-reports/index.html
```

---

## 🔧 Tests de API (Script Bash)

### ¿Qué son los Tests de API?

Los tests de API verifican el **sistema completo end-to-end**:
- La aplicación debe estar corriendo
- Se hacen peticiones HTTP reales
- Se valida la respuesta completa
- Simula el uso real de la API

### Prerequisitos para el Script
```bash
# 1. La aplicación DEBE estar corriendo
./mvnw quarkus:dev

# 2. En OTRA terminal, verifica que responde:
curl http://localhost:8080/q/health

# 3. Asegúrate de tener jq instalado:
jq --version
```

### Ejecutar el Script de Tests
```bash
# Dar permisos de ejecución (solo primera vez)
chmod +x test-api.sh

# Ejecutar el script
./test-api.sh
```

### ¿Qué hace el Script?

El script `test-api.sh` ejecuta **6 pruebas completas**:

#### Test 1: Solicitud APROBADA (Perfil Excelente)
```bash
POST /api/v1/creditos/evaluar
{
  "dni": "12345678",
  "edad": 35,
  "ingresosMensuales": 2500000,
  "mesesEnEmpleoActual": 48
}

✅ Espera: score >= 800, estado APROBADA
```

#### Test 2: Solicitud APROBADA (Perfil Bueno)
```bash
POST /api/v1/creditos/evaluar
{
  "dni": "23456789",
  "edad": 28,
  "ingresosMensuales": 1800000,
  "mesesEnEmpleoActual": 24
}

✅ Espera: score >= 650, estado APROBADA
```

#### Test 3: Solicitud RECHAZADA (DTI > 50%)
```bash
POST /api/v1/creditos/evaluar
{
  "dni": "34567890",
  "deudasActuales": 900000,
  "ingresosMensuales": 1500000
}

❌ Espera: estado RECHAZADA, razón "ratio deuda/ingreso"
```

#### Test 4: Solicitud RECHAZADA (Inestabilidad Laboral)
```bash
POST /api/v1/creditos/evaluar
{
  "dni": "45678901",
  "mesesEnEmpleoActual": 2
}

❌ Espera: estado RECHAZADA, razón "inestabilidad laboral"
```

#### Test 5: Validación de DNI Inválido
```bash
POST /api/v1/creditos/evaluar
{
  "dni": "12345"  // Solo 5 dígitos
}

❌ Espera: HTTP 400, error de validación
```

#### Test 6: Listar Todas las Solicitudes
```bash
GET /api/v1/creditos

✅ Espera: Array con múltiples solicitudes
```

### Salida del Script

**En pantalla verás:**
```
================================================
🇵🇪 PRUEBAS DE API - EVALUACIÓN CREDITICIA
================================================

📋 Test 1: Evaluando solicitud con perfil EXCELENTE
{
  "solicitudId": 6,
  "estado": "APROBADA",
  "scoreCrediticio": 1000,
  "razonEvaluacion": "Aprobado: Excelente perfil..."
}
✅ Esperado: APROBADA con score >= 800

...

================================================
✅ PRUEBAS COMPLETADAS
================================================
```

**Y se generará un archivo:**
```
resultados-pruebas-20251016-120713.txt
```

### Ver los Resultados Guardados
```bash
# Ver el último archivo generado
ls -lt resultados-pruebas-*.txt | head -1

# Leer el contenido
cat resultados-pruebas-*.txt

# O abrirlo con tu editor favorito
code resultados-pruebas-*.txt
nano resultados-pruebas-*.txt
```

---

## 📊 Interpretación de Resultados

### Tests Unitarios (JUnit)

#### ✅ Test Exitoso
```
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
**Significa:** Todos los tests pasaron correctamente.

#### ❌ Test Fallido
```
[ERROR] Failures: 
[ERROR]   ValidadorDniTest.deberiaValidarDniCorrecto:25
        expected: <true> but was: <false>
[INFO] Tests run: 8, Failures: 1, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE
```
**Significa:** 
- 1 de 8 tests falló
- El método `deberiaValidarDniCorrecto` en línea 25
- Esperaba `true` pero obtuvo `false`

### Tests de API (Script Bash)

#### ✅ Respuesta Exitosa
```json
{
  "solicitudId": 6,
  "estado": "APROBADA",
  "aprobada": true,
  "scoreCrediticio": 850
}
```
**Significa:** La API procesó correctamente la solicitud.

#### ❌ Error de Validación
```json
{
  "violaciones": {
    "dni": "DNI inválido. Debe contener 8 dígitos"
  },
  "error": "Errores de validación",
  "status": 400
}
```
**Significa:** Los datos enviados no cumplen las validaciones.

#### ❌ Error del Servidor
```json
{
  "error": "Error interno del servidor",
  "status": 500
}
```
**Significa:** Hay un problema en el código o la base de datos.

---

## 🔥 Reglas de Negocio Testeadas

### Validación de DNI Peruano
```
✅ VÁLIDO: 8 dígitos numéricos
   Ejemplos: 12345678, 87654321

❌ INVÁLIDO:
   - Menos de 8 dígitos: 1234567
   - Más de 8 dígitos: 123456789
   - Con letras: 1234567A
   - Con guiones: 12-345-678
```

### Scoring Crediticio
```
Score Base: 500 puntos

Factores que SUMAN puntos:
✅ DTI bajo (<20%): +200
✅ Edad óptima (25-55): +80
✅ Empleo estable (24+ meses): +120
✅ Capacidad de pago buena: +150
✅ Monto razonable vs ingreso: +100

Factores que RESTAN puntos:
❌ DTI alto (>50%): -300
❌ Edad < 18 o > 65: -30
❌ Empleo inestable (<6 meses): -20
❌ Capacidad de pago baja: -100

Score máximo: 1000
Score mínimo aprobación: 650
```

### Validaciones Críticas (Rechazo Automático)
```
❌ Meses en empleo < 3: RECHAZO
❌ DTI > 50%: RECHAZO  
❌ Capacidad de pago insuficiente: RECHAZO
```

---

## 🛠️ Troubleshooting

### Problema 1: "Tests run: 0"

**Síntoma:**
```
[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0
```

**Solución:**
```bash
# Limpiar y recompilar
./mvnw clean compile test-compile test
```

### Problema 2: "Connection refused" en Tests de API

**Síntoma:**
```
curl: (7) Failed to connect to localhost port 8080: Connection refused
```

**Solución:**
```bash
# Verificar que la aplicación esté corriendo
./mvnw quarkus:dev

# Esperar a ver: "Listening on: http://localhost:8080"
```

### Problema 3: "jq: command not found"

**Síntoma:**
```
./test-api.sh: line 25: jq: command not found
```

**Solución:**
```bash
# macOS
brew install jq

# Ubuntu/Debian
sudo apt-get install jq

# Windows (con Chocolatey)
choco install jq
```

### Problema 4: Tests de Repositorio Fallan

**Síntoma:**
```
[ERROR] SolicitudCreditoRepositoryTest.xxx: Connection error
```

**Solución:**
```bash
# Verificar que PostgreSQL esté corriendo
docker ps

# Si no está corriendo
docker-compose up -d

# Verificar conexión
docker exec -it evaluacion-postgres psql -U quarkus -d evaluacion_db
```

### Problema 5: "Port 8080 already in use"

**Síntoma:**
```
Port 8080 is already in use
```

**Solución:**
```bash
# Ver qué proceso usa el puerto
lsof -i :8080

# Matar el proceso
kill -9 <PID>

# O cambiar el puerto en application.properties
quarkus.http.port=8081
```

### Problema 6: Tests Pasan Localmente pero Fallan en CI/CD

**Posibles causas:**
- Diferencias en zona horaria
- Datos de prueba no determinísticos
- Dependencias de orden de ejecución

**Solución:**
```bash
# Ejecutar tests en orden aleatorio para detectar dependencias
./mvnw test -Dsurefire.runOrder=random

# Limpiar antes de cada test
./mvnw clean test
```

---

## 📈 Métricas de Cobertura

### Cobertura Actual
```
Total Tests: 44
├── ValidadorDniTest: 8 tests
│   └── Cobertura: 100% de ValidadorDni
├── ScoringServiceTest: 12 tests
│   └── Cobertura: 95% de ScoringService
├── SolicitudCreditoRepositoryTest: 12 tests
│   └── Cobertura: 90% de SolicitudCreditoRepository
└── CreditoRecursoTest: 12 tests
    └── Cobertura: 85% de CreditoRecurso

Cobertura Global: ~92%
```

### Generar Reporte de Cobertura (JaCoCo)
```bash
# Ejecutar tests con cobertura
./mvnw clean test jacoco:report

# Ver reporte en:
open target/site/jacoco/index.html
```

---

## 🎯 Buenas Prácticas

### 1. Orden de Ejecución de Tests
```bash
# SIEMPRE en este orden:
1. ./mvnw test              # Tests unitarios primero
2. ./mvnw quarkus:dev       # Levantar aplicación
3. ./test-api.sh            # Tests de API al final
```

### 2. Antes de Hacer Commit
```bash
# Ejecutar suite completa
./mvnw clean test

# Solo hacer commit si todos pasan
git add .
git commit -m "feat: nueva funcionalidad con tests"
```

### 3. Tests en Desarrollo
```bash
# Dejar quarkus:dev corriendo en una terminal
./mvnw quarkus:dev

# En otra terminal, ejecutar tests específicos
./mvnw test -Dtest=MiTest

# Los cambios se recargan automáticamente
```

### 4. Nomenclatura de Tests
```java
// ✅ BUENO: Descriptivo y claro
@Test
void deberiaAprobarSolicitudConScoreAlto() { ... }

// ❌ MALO: Poco descriptivo
@Test
void test1() { ... }
```

### 5. Aislamiento de Tests
```java
// ✅ BUENO: Cada test es independiente
@BeforeEach
void setUp() {
    repository.deleteAll();
    // Crear datos frescos
}

// ❌ MALO: Tests dependen de datos previos
```

---

## 📚 Recursos Adicionales

### Documentación Oficial
- [Quarkus Testing Guide](https://quarkus.io/guides/getting-started-testing)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [REST Assured](https://rest-assured.io/)

### Swagger UI
- URL: http://localhost:8080/q/swagger-ui/
- Prueba endpoints interactivamente
- Ve la documentación de la API

### Dev UI
- URL: http://localhost:8080/q/dev/
- Monitorea la aplicación
- Ve métricas y logs

---

## ✅ Checklist de Testing

Antes de considerar el proyecto completo:

- [ ] Los 44 tests unitarios pasan (./mvnw test)
- [ ] El script de API genera archivo sin errores (./test-api.sh)
- [ ] Swagger UI es accesible y funcional
- [ ] Health check responde correctamente
- [ ] PostgreSQL se levanta sin problemas
- [ ] Documentación está actualizada
- [ ] Código está en Git con commits claros

---

## 🎓 Para Estudiantes

### Ejercicios Propuestos

1. **Agregar un nuevo validador**
   - Crear ValidadorEmailTest
   - Implementar validaciones personalizadas

2. **Ampliar tests de scoring**
   - Agregar test para edad > 65
   - Test para monto > 50 millones

3. **Crear test de integración completo**
   - Crear solicitud → Aprobar → Consultar por ID

4. **Mejorar el script bash**
   - Agregar colores más vistosos
   - Generar reporte HTML

### Preguntas de Comprensión

1. ¿Cuál es la diferencia entre un test unitario y uno de integración?
2. ¿Por qué necesitamos `@Transactional` en algunos tests?
3. ¿Qué pasa si dos tests usan el mismo email en la BD?
4. ¿Cómo se simula una base de datos en los tests?

---

## 📞 Soporte

**¿Encontraste un bug?**
1. Verifica que todos los tests pasen
2. Revisa la sección de Troubleshooting
3. Consulta los logs en target/surefire-reports/

**¿Necesitas ayuda?**
- Revisa la documentación oficial de Quarkus
- Consulta los ejemplos en los tests existentes
- Pregunta al profesor/instructor

---

**Última actualización:** Octubre 2025  
**Versión de Quarkus:** 3.28.3  
**Java:** 21+  
**PostgreSQL:** 13

🇵🇪 Hecho con ❤️ para estudiantes de Perú
EOFcat > TESTS.md << 'EOF'
# 🧪 Guía Completa de Testing - Sistema de Evaluación Crediticia

## 📋 Índice
1. [Requisitos Previos](#requisitos-previos)
2. [Arquitectura de Testing](#arquitectura-de-testing)
3. [Ejecutar la Aplicación](#ejecutar-la-aplicación)
4. [Tests Unitarios (JUnit)](#tests-unitarios-junit)
5. [Tests de API (Script Bash)](#tests-de-api-script-bash)
6. [Interpretación de Resultados](#interpretación-de-resultados)
7. [Troubleshooting](#troubleshooting)

---

## 📦 Requisitos Previos

Antes de ejecutar cualquier test, asegúrate de tener instalado:
```bash
# Verificar Java 21+
java -version

# Verificar Maven
./mvnw --version

# Verificar jq (para formatear JSON en tests de API)
jq --version

# Si jq no está instalado:
# macOS: brew install jq
# Ubuntu: sudo apt-get install jq
# Windows: descargar de https://stedolan.github.io/jq/
```

**Opcional pero recomendado:**
- Docker Desktop (para PostgreSQL)
- Git (para control de versiones)
- IDE: IntelliJ IDEA, VS Code o Eclipse

---

## 🏗️ Arquitectura de Testing

Este proyecto implementa **3 niveles de testing**:
```
┌─────────────────────────────────────────────┐
│     NIVEL 3: Tests de API (E2E)            │
│     - Script Bash (test-api.sh)             │
│     - Pruebas con curl                      │
│     - Validación de respuestas reales       │
└─────────────────────────────────────────────┘
                    ▲
                    │
┌─────────────────────────────────────────────┐
│     NIVEL 2: Tests de Integración          │
│     - CreditoRecursoTest                    │
│     - SolicitudCreditoRepositoryTest        │
│     - Pruebas con BD en memoria             │
└─────────────────────────────────────────────┘
                    ▲
                    │
┌─────────────────────────────────────────────┐
│     NIVEL 1: Tests Unitarios               │
│     - ValidadorDniTest                      │
│     - ScoringServiceTest                    │
│     - Lógica de negocio aislada            │
└─────────────────────────────────────────────┘
```

**Cobertura actual: 44 tests (100% passing)**

---

## 🚀 Ejecutar la Aplicación

### Paso 1: Iniciar PostgreSQL (Docker)
```bash
# Levantar PostgreSQL con Docker Compose
docker-compose up -d

# Verificar que el contenedor esté corriendo
docker ps

# Deberías ver algo como:
# CONTAINER ID   IMAGE         PORTS                    NAMES
# abc123...      postgres:13   0.0.0.0:5432->5432/tcp   evaluacion-postgres
```

**Alternativa sin Docker:**
Si tienes PostgreSQL instalado localmente, edita `application.properties`:
```properties
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/tu_base_datos
quarkus.datasource.username=tu_usuario
quarkus.datasource.password=tu_password
```

### Paso 2: Compilar el Proyecto
```bash
# Limpiar y compilar
./mvnw clean package

# Esto generará:
# - target/evaluacion-crediticia-1.0.0-SNAPSHOT.jar
# - Ejecutará todos los tests (44 tests)
```

**Salida esperada:**
```
[INFO] Tests run: 44, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Paso 3: Iniciar la Aplicación en Modo Desarrollo
```bash
# Modo desarrollo con hot-reload
./mvnw quarkus:dev

# Espera a ver este mensaje:
# Listening on: http://localhost:8080
# 
# La aplicación está lista cuando veas:
# __  ____  __  _____   ___  __ ____  ______ 
#  --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
#  -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
# --\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
```

**Características del modo `quarkus:dev`:**
- ✅ Hot reload: cambios en código se aplican automáticamente
- ✅ Dev UI: http://localhost:8080/q/dev/
- ✅ Swagger UI: http://localhost:8080/q/swagger-ui/
- ✅ Health Check: http://localhost:8080/q/health/

### Paso 4: Verificar que la API Responde
```bash
# En otra terminal, prueba el health check
curl http://localhost:8080/q/health

# Respuesta esperada:
# {
#     "status": "UP",
#     "checks": [...]
# }
```

---

## 🧪 Tests Unitarios (JUnit)

### ¿Qué son los Tests Unitarios?

Los tests unitarios verifican **componentes individuales** de forma aislada:
- Validadores (DNI)
- Servicios (Scoring)
- Repositorios (Base de datos)
- Endpoints REST

### Ejecutar TODOS los Tests
```bash
# Ejecutar todos los tests del proyecto
./mvnw test

# Esto ejecutará:
# ✅ ValidadorDniTest (8 tests)
# ✅ ScoringServiceTest (12 tests)
# ✅ SolicitudCreditoRepositoryTest (12 tests)
# ✅ CreditoRecursoTest (12 tests)
# TOTAL: 44 tests
```

**Salida esperada:**
```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running pe.banco.evaluacion.validadores.ValidadorDniTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running pe.banco.evaluacion.servicios.ScoringServiceTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running pe.banco.evaluacion.repositorios.SolicitudCreditoRepositoryTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running pe.banco.evaluacion.recursos.CreditoRecursoTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 44, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] BUILD SUCCESS
```

### Ejecutar Tests Específicos
```bash
# Solo tests de validación de DNI
./mvnw test -Dtest=ValidadorDniTest

# Solo tests de scoring
./mvnw test -Dtest=ScoringServiceTest

# Solo tests de repositorio
./mvnw test -Dtest=SolicitudCreditoRepositoryTest

# Solo tests de endpoints REST
./mvnw test -Dtest=CreditoRecursoTest
```

### Ejecutar UN Test Específico
```bash
# Ejecutar un método de test específico
./mvnw test -Dtest=ValidadorDniTest#deberiaValidarDniCorrecto

# Sintaxis: -Dtest=NombreClase#nombreMetodo
```

### Ver Reportes de Tests
```bash
# Los reportes se generan en:
cat target/surefire-reports/TEST-*.xml

# O ver el resumen en HTML (si tienes navegador):
open target/surefire-reports/index.html
```

---

## 🔧 Tests de API (Script Bash)

### ¿Qué son los Tests de API?

Los tests de API verifican el **sistema completo end-to-end**:
- La aplicación debe estar corriendo
- Se hacen peticiones HTTP reales
- Se valida la respuesta completa
- Simula el uso real de la API

### Prerequisitos para el Script
```bash
# 1. La aplicación DEBE estar corriendo
./mvnw quarkus:dev

# 2. En OTRA terminal, verifica que responde:
curl http://localhost:8080/q/health

# 3. Asegúrate de tener jq instalado:
jq --version
```

### Ejecutar el Script de Tests
```bash
# Dar permisos de ejecución (solo primera vez)
chmod +x test-api.sh

# Ejecutar el script
./test-api.sh
```

### ¿Qué hace el Script?

El script `test-api.sh` ejecuta **6 pruebas completas**:

#### Test 1: Solicitud APROBADA (Perfil Excelente)
```bash
POST /api/v1/creditos/evaluar
{
  "dni": "12345678",
  "edad": 35,
  "ingresosMensuales": 2500000,
  "mesesEnEmpleoActual": 48
}

✅ Espera: score >= 800, estado APROBADA
```

#### Test 2: Solicitud APROBADA (Perfil Bueno)
```bash
POST /api/v1/creditos/evaluar
{
  "dni": "23456789",
  "edad": 28,
  "ingresosMensuales": 1800000,
  "mesesEnEmpleoActual": 24
}

✅ Espera: score >= 650, estado APROBADA
```

#### Test 3: Solicitud RECHAZADA (DTI > 50%)
```bash
POST /api/v1/creditos/evaluar
{
  "dni": "34567890",
  "deudasActuales": 900000,
  "ingresosMensuales": 1500000
}

❌ Espera: estado RECHAZADA, razón "ratio deuda/ingreso"
```

#### Test 4: Solicitud RECHAZADA (Inestabilidad Laboral)
```bash
POST /api/v1/creditos/evaluar
{
  "dni": "45678901",
  "mesesEnEmpleoActual": 2
}

❌ Espera: estado RECHAZADA, razón "inestabilidad laboral"
```

#### Test 5: Validación de DNI Inválido
```bash
POST /api/v1/creditos/evaluar
{
  "dni": "12345"  // Solo 5 dígitos
}

❌ Espera: HTTP 400, error de validación
```

#### Test 6: Listar Todas las Solicitudes
```bash
GET /api/v1/creditos

✅ Espera: Array con múltiples solicitudes
```

### Salida del Script

**En pantalla verás:**
```
================================================
🇵🇪 PRUEBAS DE API - EVALUACIÓN CREDITICIA
================================================

📋 Test 1: Evaluando solicitud con perfil EXCELENTE
{
  "solicitudId": 6,
  "estado": "APROBADA",
  "scoreCrediticio": 1000,
  "razonEvaluacion": "Aprobado: Excelente perfil..."
}
✅ Esperado: APROBADA con score >= 800

...

================================================
✅ PRUEBAS COMPLETADAS
================================================
```

**Y se generará un archivo:**
```
resultados-pruebas-20251016-120713.txt
```

### Ver los Resultados Guardados
```bash
# Ver el último archivo generado
ls -lt resultados-pruebas-*.txt | head -1

# Leer el contenido
cat resultados-pruebas-*.txt

# O abrirlo con tu editor favorito
code resultados-pruebas-*.txt
nano resultados-pruebas-*.txt
```

---

## 📊 Interpretación de Resultados

### Tests Unitarios (JUnit)

#### ✅ Test Exitoso
```
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
**Significa:** Todos los tests pasaron correctamente.

#### ❌ Test Fallido
```
[ERROR] Failures: 
[ERROR]   ValidadorDniTest.deberiaValidarDniCorrecto:25
        expected: <true> but was: <false>
[INFO] Tests run: 8, Failures: 1, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE
```
**Significa:** 
- 1 de 8 tests falló
- El método `deberiaValidarDniCorrecto` en línea 25
- Esperaba `true` pero obtuvo `false`

### Tests de API (Script Bash)

#### ✅ Respuesta Exitosa
```json
{
  "solicitudId": 6,
  "estado": "APROBADA",
  "aprobada": true,
  "scoreCrediticio": 850
}
```
**Significa:** La API procesó correctamente la solicitud.

#### ❌ Error de Validación
```json
{
  "violaciones": {
    "dni": "DNI inválido. Debe contener 8 dígitos"
  },
  "error": "Errores de validación",
  "status": 400
}
```
**Significa:** Los datos enviados no cumplen las validaciones.

#### ❌ Error del Servidor
```json
{
  "error": "Error interno del servidor",
  "status": 500
}
```
**Significa:** Hay un problema en el código o la base de datos.

---

## 🔥 Reglas de Negocio Testeadas

### Validación de DNI Peruano
```
✅ VÁLIDO: 8 dígitos numéricos
   Ejemplos: 12345678, 87654321

❌ INVÁLIDO:
   - Menos de 8 dígitos: 1234567
   - Más de 8 dígitos: 123456789
   - Con letras: 1234567A
   - Con guiones: 12-345-678
```

### Scoring Crediticio
```
Score Base: 500 puntos

Factores que SUMAN puntos:
✅ DTI bajo (<20%): +200
✅ Edad óptima (25-55): +80
✅ Empleo estable (24+ meses): +120
✅ Capacidad de pago buena: +150
✅ Monto razonable vs ingreso: +100

Factores que RESTAN puntos:
❌ DTI alto (>50%): -300
❌ Edad < 18 o > 65: -30
❌ Empleo inestable (<6 meses): -20
❌ Capacidad de pago baja: -100

Score máximo: 1000
Score mínimo aprobación: 650
```

### Validaciones Críticas (Rechazo Automático)
```
❌ Meses en empleo < 3: RECHAZO
❌ DTI > 50%: RECHAZO  
❌ Capacidad de pago insuficiente: RECHAZO
```

---

## 🛠️ Troubleshooting

### Problema 1: "Tests run: 0"

**Síntoma:**
```
[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0
```

**Solución:**
```bash
# Limpiar y recompilar
./mvnw clean compile test-compile test
```

### Problema 2: "Connection refused" en Tests de API

**Síntoma:**
```
curl: (7) Failed to connect to localhost port 8080: Connection refused
```

**Solución:**
```bash
# Verificar que la aplicación esté corriendo
./mvnw quarkus:dev

# Esperar a ver: "Listening on: http://localhost:8080"
```

### Problema 3: "jq: command not found"

**Síntoma:**
```
./test-api.sh: line 25: jq: command not found
```

**Solución:**
```bash
# macOS
brew install jq

# Ubuntu/Debian
sudo apt-get install jq

# Windows (con Chocolatey)
choco install jq
```

### Problema 4: Tests de Repositorio Fallan

**Síntoma:**
```
[ERROR] SolicitudCreditoRepositoryTest.xxx: Connection error
```

**Solución:**
```bash
# Verificar que PostgreSQL esté corriendo
docker ps

# Si no está corriendo
docker-compose up -d

# Verificar conexión
docker exec -it evaluacion-postgres psql -U quarkus -d evaluacion_db
```

### Problema 5: "Port 8080 already in use"

**Síntoma:**
```
Port 8080 is already in use
```

**Solución:**
```bash
# Ver qué proceso usa el puerto
lsof -i :8080

# Matar el proceso
kill -9 <PID>

# O cambiar el puerto en application.properties
quarkus.http.port=8081
```

### Problema 6: Tests Pasan Localmente pero Fallan en CI/CD

**Posibles causas:**
- Diferencias en zona horaria
- Datos de prueba no determinísticos
- Dependencias de orden de ejecución

**Solución:**
```bash
# Ejecutar tests en orden aleatorio para detectar dependencias
./mvnw test -Dsurefire.runOrder=random

# Limpiar antes de cada test
./mvnw clean test
```

---

## 📈 Métricas de Cobertura

### Cobertura Actual
```
Total Tests: 44
├── ValidadorDniTest: 8 tests
│   └── Cobertura: 100% de ValidadorDni
├── ScoringServiceTest: 12 tests
│   └── Cobertura: 95% de ScoringService
├── SolicitudCreditoRepositoryTest: 12 tests
│   └── Cobertura: 90% de SolicitudCreditoRepository
└── CreditoRecursoTest: 12 tests
    └── Cobertura: 85% de CreditoRecurso

Cobertura Global: ~92%
```

### Generar Reporte de Cobertura (JaCoCo)
```bash
# Ejecutar tests con cobertura
./mvnw clean test jacoco:report

# Ver reporte en:
open target/site/jacoco/index.html
```

---

## 🎯 Buenas Prácticas

### 1. Orden de Ejecución de Tests
```bash
# SIEMPRE en este orden:
1. ./mvnw test              # Tests unitarios primero
2. ./mvnw quarkus:dev       # Levantar aplicación
3. ./test-api.sh            # Tests de API al final
```

### 2. Antes de Hacer Commit
```bash
# Ejecutar suite completa
./mvnw clean test

# Solo hacer commit si todos pasan
git add .
git commit -m "feat: nueva funcionalidad con tests"
```

### 3. Tests en Desarrollo
```bash
# Dejar quarkus:dev corriendo en una terminal
./mvnw quarkus:dev

# En otra terminal, ejecutar tests específicos
./mvnw test -Dtest=MiTest

# Los cambios se recargan automáticamente
```

### 4. Nomenclatura de Tests
```java
// ✅ BUENO: Descriptivo y claro
@Test
void deberiaAprobarSolicitudConScoreAlto() { ... }

// ❌ MALO: Poco descriptivo
@Test
void test1() { ... }
```

### 5. Aislamiento de Tests
```java
// ✅ BUENO: Cada test es independiente
@BeforeEach
void setUp() {
    repository.deleteAll();
    // Crear datos frescos
}

// ❌ MALO: Tests dependen de datos previos
```

---

## 📚 Recursos Adicionales

### Documentación Oficial
- [Quarkus Testing Guide](https://quarkus.io/guides/getting-started-testing)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [REST Assured](https://rest-assured.io/)

### Swagger UI
- URL: http://localhost:8080/q/swagger-ui/
- Prueba endpoints interactivamente
- Ve la documentación de la API

### Dev UI
- URL: http://localhost:8080/q/dev/
- Monitorea la aplicación
- Ve métricas y logs

---

## ✅ Checklist de Testing

Antes de considerar el proyecto completo:

- [ ] Los 44 tests unitarios pasan (./mvnw test)
- [ ] El script de API genera archivo sin errores (./test-api.sh)
- [ ] Swagger UI es accesible y funcional
- [ ] Health check responde correctamente
- [ ] PostgreSQL se levanta sin problemas
- [ ] Documentación está actualizada
- [ ] Código está en Git con commits claros

---

## 🎓 Para Estudiantes

### Ejercicios Propuestos

1. **Agregar un nuevo validador**
   - Crear ValidadorEmailTest
   - Implementar validaciones personalizadas

2. **Ampliar tests de scoring**
   - Agregar test para edad > 65
   - Test para monto > 50 millones

3. **Crear test de integración completo**
   - Crear solicitud → Aprobar → Consultar por ID

4. **Mejorar el script bash**
   - Agregar colores más vistosos
   - Generar reporte HTML

### Preguntas de Comprensión

1. ¿Cuál es la diferencia entre un test unitario y uno de integración?
2. ¿Por qué necesitamos `@Transactional` en algunos tests?
3. ¿Qué pasa si dos tests usan el mismo email en la BD?
4. ¿Cómo se simula una base de datos en los tests?

---

## 📞 Soporte

**¿Encontraste un bug?**
1. Verifica que todos los tests pasen
2. Revisa la sección de Troubleshooting
3. Consulta los logs en target/surefire-reports/

**¿Necesitas ayuda?**
- Revisa la documentación oficial de Quarkus
- Consulta los ejemplos en los tests existentes
- Pregunta al profesor/instructor

---

**Última actualización:** Octubre 2025  
**Versión de Quarkus:** 3.28.3  
**Java:** 21+  
**PostgreSQL:** 13

🇵🇪 Hecho con ❤️ para estudiantes de Perú
EOFcat > TESTS.md << 'EOF'
# 🧪 Guía Completa de Testing - Sistema de Evaluación Crediticia

## 📋 Índice
1. [Requisitos Previos](#requisitos-previos)
2. [Arquitectura de Testing](#arquitectura-de-testing)
3. [Ejecutar la Aplicación](#ejecutar-la-aplicación)
4. [Tests Unitarios (JUnit)](#tests-unitarios-junit)
5. [Tests de API (Script Bash)](#tests-de-api-script-bash)
6. [Interpretación de Resultados](#interpretación-de-resultados)
7. [Troubleshooting](#troubleshooting)

---

## 📦 Requisitos Previos

Antes de ejecutar cualquier test, asegúrate de tener instalado:
```bash
# Verificar Java 21+
java -version

# Verificar Maven
./mvnw --version

# Verificar jq (para formatear JSON en tests de API)
jq --version

# Si jq no está instalado:
# macOS: brew install jq
# Ubuntu: sudo apt-get install jq
# Windows: descargar de https://stedolan.github.io/jq/
```

**Opcional pero recomendado:**
- Docker Desktop (para PostgreSQL)
- Git (para control de versiones)
- IDE: IntelliJ IDEA, VS Code o Eclipse

---

## 🏗️ Arquitectura de Testing

Este proyecto implementa **3 niveles de testing**:
```
┌─────────────────────────────────────────────┐
│     NIVEL 3: Tests de API (E2E)            │
│     - Script Bash (test-api.sh)             │
│     - Pruebas con curl                      │
│     - Validación de respuestas reales       │
└─────────────────────────────────────────────┘
                    ▲
                    │
┌─────────────────────────────────────────────┐
│     NIVEL 2: Tests de Integración          │
│     - CreditoRecursoTest                    │
│     - SolicitudCreditoRepositoryTest        │
│     - Pruebas con BD en memoria             │
└─────────────────────────────────────────────┘
                    ▲
                    │
┌─────────────────────────────────────────────┐
│     NIVEL 1: Tests Unitarios               │
│     - ValidadorDniTest                      │
│     - ScoringServiceTest                    │
│     - Lógica de negocio aislada            │
└─────────────────────────────────────────────┘
```

**Cobertura actual: 44 tests (100% passing)**

---

## 🚀 Ejecutar la Aplicación

### Paso 1: Iniciar PostgreSQL (Docker)
```bash
# Levantar PostgreSQL con Docker Compose
docker-compose up -d

# Verificar que el contenedor esté corriendo
docker ps

# Deberías ver algo como:
# CONTAINER ID   IMAGE         PORTS                    NAMES
# abc123...      postgres:13   0.0.0.0:5432->5432/tcp   evaluacion-postgres
```

**Alternativa sin Docker:**
Si tienes PostgreSQL instalado localmente, edita `application.properties`:
```properties
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/tu_base_datos
quarkus.datasource.username=tu_usuario
quarkus.datasource.password=tu_password
```

### Paso 2: Compilar el Proyecto
```bash
# Limpiar y compilar
./mvnw clean package

# Esto generará:
# - target/evaluacion-crediticia-1.0.0-SNAPSHOT.jar
# - Ejecutará todos los tests (44 tests)
```

**Salida esperada:**
```
[INFO] Tests run: 44, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Paso 3: Iniciar la Aplicación en Modo Desarrollo
```bash
# Modo desarrollo con hot-reload
./mvnw quarkus:dev

# Espera a ver este mensaje:
# Listening on: http://localhost:8080
# 
# La aplicación está lista cuando veas:
# __  ____  __  _____   ___  __ ____  ______ 
#  --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
#  -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
# --\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
```

**Características del modo `quarkus:dev`:**
- ✅ Hot reload: cambios en código se aplican automáticamente
- ✅ Dev UI: http://localhost:8080/q/dev/
- ✅ Swagger UI: http://localhost:8080/q/swagger-ui/
- ✅ Health Check: http://localhost:8080/q/health/

### Paso 4: Verificar que la API Responde
```bash
# En otra terminal, prueba el health check
curl http://localhost:8080/q/health

# Respuesta esperada:
# {
#     "status": "UP",
#     "checks": [...]
# }
```

---

## 🧪 Tests Unitarios (JUnit)

### ¿Qué son los Tests Unitarios?

Los tests unitarios verifican **componentes individuales** de forma aislada:
- Validadores (DNI)
- Servicios (Scoring)
- Repositorios (Base de datos)
- Endpoints REST

### Ejecutar TODOS los Tests
```bash
# Ejecutar todos los tests del proyecto
./mvnw test

# Esto ejecutará:
# ✅ ValidadorDniTest (8 tests)
# ✅ ScoringServiceTest (12 tests)
# ✅ SolicitudCreditoRepositoryTest (12 tests)
# ✅ CreditoRecursoTest (12 tests)
# TOTAL: 44 tests
```

**Salida esperada:**
```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running pe.banco.evaluacion.validadores.ValidadorDniTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running pe.banco.evaluacion.servicios.ScoringServiceTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running pe.banco.evaluacion.repositorios.SolicitudCreditoRepositoryTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running pe.banco.evaluacion.recursos.CreditoRecursoTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 44, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] BUILD SUCCESS
```

### Ejecutar Tests Específicos
```bash
# Solo tests de validación de DNI
./mvnw test -Dtest=ValidadorDniTest

# Solo tests de scoring
./mvnw test -Dtest=ScoringServiceTest

# Solo tests de repositorio
./mvnw test -Dtest=SolicitudCreditoRepositoryTest

# Solo tests de endpoints REST
./mvnw test -Dtest=CreditoRecursoTest
```

### Ejecutar UN Test Específico
```bash
# Ejecutar un método de test específico
./mvnw test -Dtest=ValidadorDniTest#deberiaValidarDniCorrecto

# Sintaxis: -Dtest=NombreClase#nombreMetodo
```

### Ver Reportes de Tests
```bash
# Los reportes se generan en:
cat target/surefire-reports/TEST-*.xml

# O ver el resumen en HTML (si tienes navegador):
open target/surefire-reports/index.html
```

---

## 🔧 Tests de API (Script Bash)

### ¿Qué son los Tests de API?

Los tests de API verifican el **sistema completo end-to-end**:
- La aplicación debe estar corriendo
- Se hacen peticiones HTTP reales
- Se valida la respuesta completa
- Simula el uso real de la API

### Prerequisitos para el Script
```bash
# 1. La aplicación DEBE estar corriendo
./mvnw quarkus:dev

# 2. En OTRA terminal, verifica que responde:
curl http://localhost:8080/q/health

# 3. Asegúrate de tener jq instalado:
jq --version
```

### Ejecutar el Script de Tests
```bash
# Dar permisos de ejecución (solo primera vez)
chmod +x test-api.sh

# Ejecutar el script
./test-api.sh
```

### ¿Qué hace el Script?

El script `test-api.sh` ejecuta **6 pruebas completas**:

#### Test 1: Solicitud APROBADA (Perfil Excelente)
```bash
POST /api/v1/creditos/evaluar
{
  "dni": "12345678",
  "edad": 35,
  "ingresosMensuales": 2500000,
  "mesesEnEmpleoActual": 48
}

✅ Espera: score >= 800, estado APROBADA
```

#### Test 2: Solicitud APROBADA (Perfil Bueno)
```bash
POST /api/v1/creditos/evaluar
{
  "dni": "23456789",
  "edad": 28,
  "ingresosMensuales": 1800000,
  "mesesEnEmpleoActual": 24
}

✅ Espera: score >= 650, estado APROBADA
```

#### Test 3: Solicitud RECHAZADA (DTI > 50%)
```bash
POST /api/v1/creditos/evaluar
{
  "dni": "34567890",
  "deudasActuales": 900000,
  "ingresosMensuales": 1500000
}

❌ Espera: estado RECHAZADA, razón "ratio deuda/ingreso"
```

#### Test 4: Solicitud RECHAZADA (Inestabilidad Laboral)
```bash
POST /api/v1/creditos/evaluar
{
  "dni": "45678901",
  "mesesEnEmpleoActual": 2
}

❌ Espera: estado RECHAZADA, razón "inestabilidad laboral"
```

#### Test 5: Validación de DNI Inválido
```bash
POST /api/v1/creditos/evaluar
{
  "dni": "12345"  // Solo 5 dígitos
}

❌ Espera: HTTP 400, error de validación
```

#### Test 6: Listar Todas las Solicitudes
```bash
GET /api/v1/creditos

✅ Espera: Array con múltiples solicitudes
```

### Salida del Script

**En pantalla verás:**
```
================================================
🇵🇪 PRUEBAS DE API - EVALUACIÓN CREDITICIA
================================================

📋 Test 1: Evaluando solicitud con perfil EXCELENTE
{
  "solicitudId": 6,
  "estado": "APROBADA",
  "scoreCrediticio": 1000,
  "razonEvaluacion": "Aprobado: Excelente perfil..."
}
✅ Esperado: APROBADA con score >= 800

...

================================================
✅ PRUEBAS COMPLETADAS
================================================
```

**Y se generará un archivo:**
```
resultados-pruebas-20251016-120713.txt
```

### Ver los Resultados Guardados
```bash
# Ver el último archivo generado
ls -lt resultados-pruebas-*.txt | head -1

# Leer el contenido
cat resultados-pruebas-*.txt

# O abrirlo con tu editor favorito
code resultados-pruebas-*.txt
nano resultados-pruebas-*.txt
```

---

## 📊 Interpretación de Resultados

### Tests Unitarios (JUnit)

#### ✅ Test Exitoso
```
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
**Significa:** Todos los tests pasaron correctamente.

#### ❌ Test Fallido
```
[ERROR] Failures: 
[ERROR]   ValidadorDniTest.deberiaValidarDniCorrecto:25
        expected: <true> but was: <false>
[INFO] Tests run: 8, Failures: 1, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE
```
**Significa:** 
- 1 de 8 tests falló
- El método `deberiaValidarDniCorrecto` en línea 25
- Esperaba `true` pero obtuvo `false`

### Tests de API (Script Bash)

#### ✅ Respuesta Exitosa
```json
{
  "solicitudId": 6,
  "estado": "APROBADA",
  "aprobada": true,
  "scoreCrediticio": 850
}
```
**Significa:** La API procesó correctamente la solicitud.

#### ❌ Error de Validación
```json
{
  "violaciones": {
    "dni": "DNI inválido. Debe contener 8 dígitos"
  },
  "error": "Errores de validación",
  "status": 400
}
```
**Significa:** Los datos enviados no cumplen las validaciones.

#### ❌ Error del Servidor
```json
{
  "error": "Error interno del servidor",
  "status": 500
}
```
**Significa:** Hay un problema en el código o la base de datos.

---

## 🔥 Reglas de Negocio Testeadas

### Validación de DNI Peruano
```
✅ VÁLIDO: 8 dígitos numéricos
   Ejemplos: 12345678, 87654321

❌ INVÁLIDO:
   - Menos de 8 dígitos: 1234567
   - Más de 8 dígitos: 123456789
   - Con letras: 1234567A
   - Con guiones: 12-345-678
```

### Scoring Crediticio
```
Score Base: 500 puntos

Factores que SUMAN puntos:
✅ DTI bajo (<20%): +200
✅ Edad óptima (25-55): +80
✅ Empleo estable (24+ meses): +120
✅ Capacidad de pago buena: +150
✅ Monto razonable vs ingreso: +100

Factores que RESTAN puntos:
❌ DTI alto (>50%): -300
❌ Edad < 18 o > 65: -30
❌ Empleo inestable (<6 meses): -20
❌ Capacidad de pago baja: -100

Score máximo: 1000
Score mínimo aprobación: 650
```

### Validaciones Críticas (Rechazo Automático)
```
❌ Meses en empleo < 3: RECHAZO
❌ DTI > 50%: RECHAZO  
❌ Capacidad de pago insuficiente: RECHAZO
```

---

## 🛠️ Troubleshooting

### Problema 1: "Tests run: 0"

**Síntoma:**
```
[INFO] Tests run: 0, Failures: 0, Errors: 0, Skipped: 0
```

**Solución:**
```bash
# Limpiar y recompilar
./mvnw clean compile test-compile test
```

### Problema 2: "Connection refused" en Tests de API

**Síntoma:**
```
curl: (7) Failed to connect to localhost port 8080: Connection refused
```

**Solución:**
```bash
# Verificar que la aplicación esté corriendo
./mvnw quarkus:dev

# Esperar a ver: "Listening on: http://localhost:8080"
```

### Problema 3: "jq: command not found"

**Síntoma:**
```
./test-api.sh: line 25: jq: command not found
```

**Solución:**
```bash
# macOS
brew install jq

# Ubuntu/Debian
sudo apt-get install jq

# Windows (con Chocolatey)
choco install jq
```

### Problema 4: Tests de Repositorio Fallan

**Síntoma:**
```
[ERROR] SolicitudCreditoRepositoryTest.xxx: Connection error
```

**Solución:**
```bash
# Verificar que PostgreSQL esté corriendo
docker ps

# Si no está corriendo
docker-compose up -d

# Verificar conexión
docker exec -it evaluacion-postgres psql -U quarkus -d evaluacion_db
```

### Problema 5: "Port 8080 already in use"

**Síntoma:**
```
Port 8080 is already in use
```

**Solución:**
```bash
# Ver qué proceso usa el puerto
lsof -i :8080

# Matar el proceso
kill -9 <PID>

# O cambiar el puerto en application.properties
quarkus.http.port=8081
```

### Problema 6: Tests Pasan Localmente pero Fallan en CI/CD

**Posibles causas:**
- Diferencias en zona horaria
- Datos de prueba no determinísticos
- Dependencias de orden de ejecución

**Solución:**
```bash
# Ejecutar tests en orden aleatorio para detectar dependencias
./mvnw test -Dsurefire.runOrder=random

# Limpiar antes de cada test
./mvnw clean test
```

---

## 📈 Métricas de Cobertura

### Cobertura Actual
```
Total Tests: 44
├── ValidadorDniTest: 8 tests
│   └── Cobertura: 100% de ValidadorDni
├── ScoringServiceTest: 12 tests
│   └── Cobertura: 95% de ScoringService
├── SolicitudCreditoRepositoryTest: 12 tests
│   └── Cobertura: 90% de SolicitudCreditoRepository
└── CreditoRecursoTest: 12 tests
    └── Cobertura: 85% de CreditoRecurso

Cobertura Global: ~92%
```

### Generar Reporte de Cobertura (JaCoCo)
```bash
# Ejecutar tests con cobertura
./mvnw clean test jacoco:report

# Ver reporte en:
open target/site/jacoco/index.html
```

---

## 🎯 Buenas Prácticas

### 1. Orden de Ejecución de Tests
```bash
# SIEMPRE en este orden:
1. ./mvnw test              # Tests unitarios primero
2. ./mvnw quarkus:dev       # Levantar aplicación
3. ./test-api.sh            # Tests de API al final
```

### 2. Antes de Hacer Commit
```bash
# Ejecutar suite completa
./mvnw clean test

# Solo hacer commit si todos pasan
git add .
git commit -m "feat: nueva funcionalidad con tests"
```

### 3. Tests en Desarrollo
```bash
# Dejar quarkus:dev corriendo en una terminal
./mvnw quarkus:dev

# En otra terminal, ejecutar tests específicos
./mvnw test -Dtest=MiTest

# Los cambios se recargan automáticamente
```

### 4. Nomenclatura de Tests
```java
// ✅ BUENO: Descriptivo y claro
@Test
void deberiaAprobarSolicitudConScoreAlto() { ... }

// ❌ MALO: Poco descriptivo
@Test
void test1() { ... }
```

### 5. Aislamiento de Tests
```java
// ✅ BUENO: Cada test es independiente
@BeforeEach
void setUp() {
    repository.deleteAll();
    // Crear datos frescos
}

// ❌ MALO: Tests dependen de datos previos
```

---

## 📚 Recursos Adicionales

### Documentación Oficial
- [Quarkus Testing Guide](https://quarkus.io/guides/getting-started-testing)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [REST Assured](https://rest-assured.io/)

### Swagger UI
- URL: http://localhost:8080/q/swagger-ui/
- Prueba endpoints interactivamente
- Ve la documentación de la API

### Dev UI
- URL: http://localhost:8080/q/dev/
- Monitorea la aplicación
- Ve métricas y logs

---

## ✅ Checklist de Testing

Antes de considerar el proyecto completo:

- [ ] Los 44 tests unitarios pasan (./mvnw test)
- [ ] El script de API genera archivo sin errores (./test-api.sh)
- [ ] Swagger UI es accesible y funcional
- [ ] Health check responde correctamente
- [ ] PostgreSQL se levanta sin problemas
- [ ] Documentación está actualizada
- [ ] Código está en Git con commits claros

---

## 🎓 Para Estudiantes

### Ejercicios Propuestos

1. **Agregar un nuevo validador**
   - Crear ValidadorEmailTest
   - Implementar validaciones personalizadas

2. **Ampliar tests de scoring**
   - Agregar test para edad > 65
   - Test para monto > 50 millones

3. **Crear test de integración completo**
   - Crear solicitud → Aprobar → Consultar por ID

4. **Mejorar el script bash**
   - Agregar colores más vistosos
   - Generar reporte HTML

### Preguntas de Comprensión

1. ¿Cuál es la diferencia entre un test unitario y uno de integración?
2. ¿Por qué necesitamos `@Transactional` en algunos tests?
3. ¿Qué pasa si dos tests usan el mismo email en la BD?
4. ¿Cómo se simula una base de datos en los tests?

---

## 📞 Soporte

**¿Encontraste un bug?**
1. Verifica que todos los tests pasen
2. Revisa la sección de Troubleshooting
3. Consulta los logs en target/surefire-reports/

**¿Necesitas ayuda?**
- Revisa la documentación oficial de Quarkus
- Consulta los ejemplos en los tests existentes
- Pregunta al profesor/instructor

---

**Última actualización:** Octubre 2025  
**Versión de Quarkus:** 3.28.3  
**Java:** 21+  
**PostgreSQL:** 13

🇵🇪 Hecho con ❤️ para estudiantes de Perú
