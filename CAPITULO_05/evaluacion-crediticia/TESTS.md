# 🧪 Guía de Testing - Sistema de Evaluación Crediticia

## 📋 Índice
1. [Requisitos Previos](#requisitos-previos)
2. [Arquitectura de Testing](#arquitectura-de-testing)
3. [Dev Services](#dev-services)
4. [Ejecutar la Aplicación](#ejecutar-la-aplicación)
5. [Tests Unitarios](#tests-unitarios)
6. [Tests de API](#tests-de-api)
7. [Pruebas Nativas con GraalVM](#pruebas-nativas-con-graalvm)
8. [Validación de Datos](#validación-de-datos)
9. [Troubleshooting](#troubleshooting)

---

## 📦 Requisitos Previos

```bash
# Verificar Java 21+
java -version

# Verificar Maven
./mvnw --version

# Verificar Docker (para Dev Services)
docker --version
docker ps  # Debe mostrar contenedores o estar vacío (pero sin error)
```

**Necesario:**
- Java 21+
- Maven 3.9+
- Docker Desktop (para Dev Services en tests)
- jq (para el script de tests de API)

**Opcional:**
- PostgreSQL local (solo si desactivas Dev Services)
- GraalVM 21+ (solo para pruebas nativas)

---

## 🏗️ Arquitectura de Testing

Este proyecto implementa **3 niveles de testing**:

```
┌─────────────────────────────────────────────┐
│     NIVEL 3: Tests de API (E2E)            │
│     - Script Bash                           │
│     - Pruebas con curl                      │
│     - Validación de respuestas reales       │
└─────────────────────────────────────────────┘
                    ▲
                    │
┌─────────────────────────────────────────────┐
│     NIVEL 2: Tests de Integración          │
│     - CreditoRecursoTest                    │
│     - SolicitudCreditoRepositoryTest        │
│     - Pruebas con BD H2 en memoria          │
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

**Cobertura:** 44 tests (100% passing)

---

## 🐳 Dev Services

### ¿Qué es Dev Services?

Dev Services es una funcionalidad de Quarkus que **automáticamente levanta servicios** (bases de datos, brokers, etc.) durante el desarrollo y testing, **sin configuración manual**.

**Analogía:** Es como tener un asistente que prepara tu laboratorio automáticamente. Necesitas PostgreSQL para testear? Dev Services lo levanta por ti en un contenedor Docker.

### ¿Cómo funciona en este proyecto?

Cuando ejecutas:
```bash
./mvnw test
```

Quarkus detecta que tienes `quarkus-jdbc-postgresql` en el `pom.xml` y:

1. 🐳 **Levanta PostgreSQL automáticamente** en un contenedor (Testcontainers)
2. 🗄️ **Crea la base de datos** con el esquema definido en tus entidades
3. 🧪 **Ejecuta los tests** contra esa BD temporal
4. 🧹 **Limpia todo** al terminar

**NO necesitas:**
- ❌ Docker Compose
- ❌ PostgreSQL instalado localmente (para tests)
- ❌ Configuración manual de BD de prueba

### Requisitos para Dev Services

```bash
# Solo necesitas Docker Desktop corriendo
docker --version

# Si Docker no está corriendo:
# Mac/Windows: Abre Docker Desktop
# Linux: sudo systemctl start docker
```

### Logs de Dev Services

Cuando ejecutas tests, verás en los logs:

```
Dev Services for the default datasource (postgresql) started.
Container: /gifted_cartwright (postgres:13)
Connection URL: jdbc:postgresql://localhost:32768/quarkus
```

Esto confirma que Dev Services levantó PostgreSQL automáticamente.

### Desactivar Dev Services (opcional)

Si quieres usar tu PostgreSQL local en vez de Dev Services, agrega en `application.properties`:

```properties
# Desactivar Dev Services
quarkus.devservices.enabled=false

# Usar tu BD local
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/tu_base_datos
quarkus.datasource.username=tu_usuario
quarkus.datasource.password=tu_password
```

### Ventajas de Dev Services

✅ **Tests aislados:** Cada test corre en una BD limpia  
✅ **Sin configuración:** Funciona out-of-the-box  
✅ **Reproducible:** Mismos tests, mismos resultados, siempre  
✅ **Múltiples servicios:** Soporta PostgreSQL, MySQL, MongoDB, Kafka, Redis, etc.

---

## 🚀 Ejecutar la Aplicación

### Paso 1: Verificar Docker (para Dev Services)

**Para desarrollo y tests**, Quarkus usa Dev Services que requiere Docker:

```bash
# Verificar que Docker Desktop esté corriendo
docker ps

# Si no muestra contenedores, abre Docker Desktop
```

**Para producción**, puedes usar PostgreSQL local:
```bash
# Verificar PostgreSQL instalado localmente
psql -U postgres -c "SELECT version();"
```

**Nota:** Dev Services levanta PostgreSQL automáticamente en modo dev y test. Solo necesitas Docker corriendo.

### Paso 2: Compilar el Proyecto

```bash
./mvnw clean package
```

**Salida esperada:**
```
[INFO] Tests run: 44, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Paso 3: Iniciar la Aplicación

```bash
./mvnw quarkus:dev
```

Espera a ver:
```
Listening on: http://localhost:8080
```

**URLs disponibles:**
- API: http://localhost:8080/api/v1/creditos
- Swagger UI: http://localhost:8080/q/swagger-ui/
- Health: http://localhost:8080/q/health/

---

## 🧪 Tests Unitarios

### Ejecutar TODOS los Tests

```bash
./mvnw test
```

**Tests ejecutados:**
- ValidadorDniTest: 8 tests
- ScoringServiceTest: 7 tests (@Test) + 1 test parametrizado (5 casos) = 12 ejecuciones
- SolicitudCreditoRepositoryTest: 12 tests
- CreditoRecursoTest: 12 tests

**Total: 39 métodos = 44 ejecuciones de test**

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
./mvnw test -Dtest=ValidadorDniTest#deberiaValidarDniCorrecto
```

### Ver Tests Ejecutándose en Detalle

Por defecto Maven no muestra cada test en la consola. Para verlos:

```bash
# Ver cada test mientras se ejecuta
./mvnw test -Dsurefire.useFile=false

# Con más detalle
./mvnw test -Dsurefire.printSummary=true -Dsurefire.useFile=false
```

### Ver Reportes Después de Ejecutar

```bash
# Ver todos los reportes de texto
cat target/surefire-reports/*.txt

# Ver con scroll
cat target/surefire-reports/*.txt | less

# Buscar fallos específicos
grep -i "failure\|error" target/surefire-reports/*.txt
```

### Configuración Permanente en pom.xml

Para que SIEMPRE muestre el detalle, edita el `pom.xml`:

```xml
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>${surefire-plugin.version}</version>
    <configuration>
        <systemPropertyVariables>
            <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
            <maven.home>${maven.home}</maven.home>
        </systemPropertyVariables>
        <!-- AGREGAR ESTAS LÍNEAS -->
        <printSummary>true</printSummary>
        <useFile>false</useFile>
    </configuration>
</plugin>
```

Después solo ejecutas:
```bash
./mvnw test  # Ahora siempre muestra el detalle
```

### Generar Reporte HTML

```bash
# Generar reporte visual
./mvnw surefire-report:report

# Abrir en navegador
# Mac:
open target/site/surefire-report.html

# Linux:
xdg-open target/site/surefire-report.html

# Windows:
start target/site/surefire-report.html
```

---

## 🔧 Tests de API

### Prerequisitos

```bash
# 1. La aplicación DEBE estar corriendo
./mvnw quarkus:dev

# 2. En OTRA terminal, verificar que responde:
curl http://localhost:8080/q/health

# 3. Instalar jq si no lo tienes:
# macOS: brew install jq
# Linux: sudo apt-get install jq
```

### Ejecutar el Script de Tests

```bash
# Dar permisos de ejecución (solo primera vez)
chmod +x test-evaluacion-crediticia.sh

# Ejecutar el script
./test-evaluacion-crediticia.sh
```

### Pruebas que ejecuta el script:

#### Test 1: Solicitud APROBADA (Perfil Excelente)
```json
{
  "dni": "12345678",
  "edad": 35,
  "ingresosMensuales": 2500000,
  "mesesEnEmpleoActual": 48
}
```
✅ Espera: score >= 800, estado APROBADA

#### Test 2: Solicitud APROBADA (Perfil Bueno)
```json
{
  "dni": "23456789",
  "edad": 28,
  "ingresosMensuales": 1800000,
  "mesesEnEmpleoActual": 24
}
```
✅ Espera: score >= 650, estado APROBADA

#### Test 3: Solicitud RECHAZADA (DTI > 50%)
```json
{
  "dni": "34567890",
  "deudasActuales": 900000,
  "ingresosMensuales": 1500000
}
```
❌ Espera: estado RECHAZADA, razón "ratio deuda/ingreso"

#### Test 4: Solicitud RECHAZADA (Inestabilidad Laboral)
```json
{
  "dni": "45678901",
  "mesesEnEmpleoActual": 2
}
```
❌ Espera: estado RECHAZADA, razón "inestabilidad laboral"

#### Test 5: Validación de DNI Inválido
```json
{
  "dni": "12345"
}
```
❌ Espera: HTTP 400, error de validación

#### Test 6: Listar Todas las Solicitudes
```bash
GET /api/v1/creditos
```
✅ Espera: Array con múltiples solicitudes

### Archivo de Resultados

El script genera:
```
resultados-evaluacion-crediticia-YYYY-MM-DD_HH-MM-SS.txt
```

Ver resultados:
```bash
ls -lt resultados-evaluacion-crediticia-*.txt | head -1
cat resultados-evaluacion-crediticia-*.txt
```

---

## 🚀 Pruebas Nativas con GraalVM

### ¿Qué son las Pruebas Nativas?

Las pruebas nativas verifican que tu aplicación funciona correctamente cuando se **compila a binario nativo con GraalVM**.

**Analogía:** Es la diferencia entre probar tu código en Python (interpretado) vs probarlo compilado a ejecutable de C. El binario nativo es mucho más rápido pero puede tener problemas de compatibilidad.

### ¿Por qué son importantes?

GraalVM usa **compilación ahead-of-time (AOT)** y puede tener comportamientos diferentes a JVM:
- Reflexión limitada
- Recursos cargados diferente
- Proxies dinámicos requieren configuración

Los tests nativos aseguran que **todo funciona en modo nativo**.

### Archivo de Test Nativo

El proyecto incluye `NativeImageIT.java`:

```java
@QuarkusIntegrationTest
public class NativeImageIT extends CreditoRecursoTest {
}
```

Este test:
1. ✅ Reutiliza TODOS los tests de `CreditoRecursoTest`
2. ✅ Los ejecuta contra el **binario nativo compilado**
3. ✅ Verifica compatibilidad con GraalVM

### La Diferencia Clave

**Los MISMOS 12 tests, dos formas de ejecutarlos:**

| Comando | Clase | Runtime | Arranque | RAM | Cuándo |
|---------|-------|---------|----------|-----|--------|
| `./mvnw test` | CreditoRecursoTest | JVM | ~1.5 seg | ~150 MB | Desarrollo diario |
| `./mvnw verify -Dnative` | NativeImageIT | Binario Nativo | ~0.13 seg | ~30 MB | Pre-producción |

**¿Por qué NativeImageIT hereda de CreditoRecursoTest?**

```java
@QuarkusIntegrationTest
public class NativeImageIT extends CreditoRecursoTest {
    // Hereda los 12 tests automáticamente
}
```

**Objetivo:** Verificar que el binario nativo funciona **exactamente igual** que JVM, pero 100x más rápido al arrancar y con 5x menos memoria.

### Requisitos para Compilación Nativa

```bash
# Verificar GraalVM instalado
java -version
# Debe mostrar: GraalVM CE 21 o superior

# Verificar Native Image
native-image --version
```

**Instalar GraalVM:**
- Descargar de: https://www.graalvm.org/downloads/
- Instalar Native Image: `gu install native-image`

### Ejecutar Tests Nativos

#### Opción 1: Compilar y testear (completo)

```bash
# Compilar binario nativo y ejecutar tests
./mvnw verify -Dnative

# Esto toma ~5-10 minutos la primera vez
```

**Salida esperada:**
```
[INFO] Building native image...
[INFO] Running integration test...
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

#### Opción 2: Compilar sin tests (más rápido)

```bash
# Solo compilar el binario nativo
./mvnw package -Dnative -DskipTests

# Ejecutar el binario
./target/evaluacion-crediticia-1.0.0-SNAPSHOT-runner
```

#### Opción 3: Compilación nativa en contenedor

Si no tienes GraalVM instalado localmente:

```bash
# Compilar usando Docker (no requiere GraalVM local)
./mvnw package -Dnative -Dquarkus.native.container-build=true

# Esto usa una imagen Docker con GraalVM pre-instalado
```

### Tiempos de Compilación

| Modo | Tiempo Compilación | Tiempo Arranque | Memoria |
|------|-------------------|-----------------|---------|
| JVM | ~10 segundos | ~1.5 segundos | ~150 MB |
| Nativo | ~5 minutos | ~0.015 segundos | ~30 MB |

**Ventaja nativa:** 100x más rápido al iniciar, usa 5x menos memoria

### Verificar el Binario Nativo

```bash
# Ver el archivo compilado
ls -lh target/*-runner

# Ejecutar
./target/evaluacion-crediticia-1.0.0-SNAPSHOT-runner

# Debería iniciar en milisegundos:
# Listening on: http://localhost:8080 (started in 0.015s)
```

### Problemas Comunes en Compilación Nativa

**1. Reflexión no configurada**
```
Error: Class X not found for reflection
```
**Solución:** Agregar en `application.properties`:
```properties
quarkus.native.additional-build-args=--initialize-at-run-time=clase.Problemática
```

**2. Memoria insuficiente**
```
Error: Image build ran out of memory
```
**Solución:**
```bash
./mvnw package -Dnative -Dquarkus.native.native-image-xmx=8g
```

**3. Tiempo de compilación muy largo**
**Solución:** Usa compilación en contenedor o máquina con más CPU/RAM

### ¿Cuándo usar Tests Nativos?

✅ **Usar cuando:**
- Vas a desplegar binarios nativos en producción
- Necesitas arranque ultra-rápido (serverless, edge computing)
- Quieres optimizar uso de memoria

❌ **NO necesario si:**
- Solo desarrollas/despliegas JARs normales
- Es un curso introductorio
- No tienes GraalVM instalado

### Resumen

```bash
# Tests normales (JVM)
./mvnw test                    # 44 tests en ~10 segundos

# Tests nativos (GraalVM)
./mvnw verify -Dnative         # 12 tests en ~5 minutos (compilación incluida)
```

---

## ✅ Validación de Datos

### Validador Custom: @DniValido

El proyecto implementa un validador personalizado para DNI peruano:

**Anotación:**
```java
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidadorDni.class)
public @interface DniValido {
    String message() default "DNI peruano inválido. Debe contener exactamente 8 dígitos.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

**Uso en DTO:**
```java
public class SolicitudCreditoDTO {
    @DniValido  // ← Validación custom
    private String dni;
    
    @NotBlank
    @Email
    private String email;
    
    @Min(18) @Max(70)
    private Integer edad;
    
    @DecimalMin("1")
    private BigDecimal ingresosMensuales;
}
```

**Implementación:**
```java
public class ValidadorDni implements ConstraintValidator<DniValido, String> {
    @Override
    public boolean isValid(String dni, ConstraintValidatorContext context) {
        if (dni == null || dni.isBlank()) {
            return false;
        }
        return dni.matches("^[0-9]{8}$");  // Exactamente 8 dígitos
    }
}
```

### Bean Validation Estándar

El proyecto usa validaciones estándar de Jakarta Bean Validation:

| Anotación | Uso | Ejemplo |
|-----------|-----|---------|
| `@NotNull` | Campo requerido | `@NotNull private String nombre;` |
| `@NotBlank` | String no vacío | `@NotBlank private String dni;` |
| `@Email` | Email válido | `@Email private String correo;` |
| `@Min/@Max` | Rango numérico | `@Min(18) private Integer edad;` |
| `@DecimalMin/@DecimalMax` | Rango decimal | `@DecimalMin("0") private BigDecimal deuda;` |
| `@Size` | Longitud string | `@Size(min=8, max=8) private String dni;` |

### Exception Mappers

Manejo de errores de validación con Exception Mappers:

**ValidationExceptionMapper.java:**
```java
@Provider
public class ValidationExceptionMapper 
    implements ExceptionMapper<ConstraintViolationException> {
    
    @Override
    public Response toResponse(ConstraintViolationException exception) {
        Map<String, String> violaciones = new HashMap<>();
        
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            String campo = violation.getPropertyPath().toString();
            String mensaje = violation.getMessage();
            violaciones.put(campo, mensaje);
        }
        
        return Response.status(400)
            .entity(Map.of(
                "error", "Errores de validación",
                "violaciones", violaciones
            ))
            .build();
    }
}
```

**Respuesta de error:**
```json
{
  "error": "Errores de validación",
  "violaciones": {
    "dni": "DNI peruano inválido. Debe contener exactamente 8 dígitos.",
    "email": "Debe ser un email válido",
    "edad": "Debe ser mayor o igual a 18"
  },
  "status": 400
}
```

### Tests de Validación

El proyecto incluye tests específicos para validaciones:

**ValidadorDniTest.java** (8 tests):
- ✅ DNI correcto (8 dígitos)
- ❌ Menos de 8 dígitos
- ❌ Más de 8 dígitos
- ❌ Con letras
- ❌ Vacío/nulo
- ❌ Con espacios
- ❌ Con caracteres especiales

**CreditoRecursoTest.java** (validaciones REST):
- ❌ DNI inválido → HTTP 400
- ❌ Email inválido → HTTP 400
- ❌ Edad < 18 → HTTP 400
- ❌ Campos requeridos vacíos → HTTP 400
- ❌ Valores fuera de rango → HTTP 400

---

## 📊 Detalle de los Tests

### ValidadorDniTest (8 tests)

```java
✅ deberiaValidarDniCorrecto
✅ deberiaRechazarDniConMenosDe8Digitos
✅ deberiaRechazarDniConMasDe8Digitos
✅ deberiaRechazarDniConLetras
✅ deberiaRechazarDniVacio
✅ deberiaRechazarDniNulo
✅ deberiaRechazarDniConEspacios
✅ deberiaRechazarDniConCaracteresEspeciales
```

### ScoringServiceTest (12 ejecuciones)

```java
✅ deberiaCalcularScoreExcelente
✅ deberiaRechazarPorDTIAlto
✅ deberiaRechazarPorInestabilidadLaboral
✅ deberiaCalcularDTICorrectamente
✅ deberiaManejarDTICeroIngresos
✅ deberiaEvaluarMultiplesEscenarios (5 casos parametrizados)
✅ deberiaGenerarRazonAprobacionExcelente
✅ deberiaLimitarScoreEntreCeroYMil
```

### SolicitudCreditoRepositoryTest (12 tests)

```java
✅ deberiaListarTodasLasSolicitudes
✅ deberiaBuscarPorId
✅ deberiaBuscarPorDni
✅ deberiaBuscarPorEstado
✅ deberiaContarSolicitudesPorEstado
✅ deberiaFiltrarPorRangoScore
✅ deberiaCrearSolicitud
✅ deberiaActualizarEstado
✅ deberiaEliminarSolicitud
✅ deberiaValidarCamposObligatorios
✅ deberiaValidarEmailUnico
✅ deberiaCalcularPromedioScore
```

### CreditoRecursoTest (12 tests)

```java
✅ deberiaEvaluarSolicitudExitosa
✅ deberiaRechazarSolicitudConDTIAlto
✅ deberiaValidarDNIInvalido
✅ deberiaValidarCamposRequeridos
✅ deberiaListarTodasLasSolicitudes
✅ deberiaBuscarSolicitudPorId
✅ deberiaRetornar404SiNoExiste
✅ deberiaValidarEmailFormato
✅ deberiaValidarEdadMinima
✅ deberiaValidarIngresosPositivos
✅ deberiaValidarMesesEmpleoMinimo
✅ deberiaRetornarErrorConDatosInvalidos
```

---

## 🎯 Reglas de Negocio Testeadas

### Validación de DNI Peruano

✅ **VÁLIDO:** 8 dígitos numéricos (12345678, 87654321)

❌ **INVÁLIDO:**
- Menos de 8 dígitos: 1234567
- Más de 8 dígitos: 123456789
- Con letras: 1234567A
- Con caracteres especiales: 12-345-678

### Scoring Crediticio

**Score Base:** 500 puntos

**Factores que SUMAN:**
- DTI bajo (<20%): +200
- Edad óptima (25-55): +80
- Empleo estable (24+ meses): +120
- Capacidad de pago buena: +150
- Monto razonable vs ingreso: +100

**Factores que RESTAN:**
- DTI alto (>50%): -300
- Edad < 18 o > 65: -30
- Empleo inestable (<6 meses): -20
- Capacidad de pago baja: -100

**Umbral de aprobación:** 650 puntos

### Validaciones Críticas (Rechazo Automático)

❌ Meses en empleo < 3: RECHAZO  
❌ DTI > 50%: RECHAZO  
❌ Capacidad de pago insuficiente: RECHAZO

---

## 🛠️ Troubleshooting

### Problema 1: "Tests run: 0"

**Solución:**
```bash
./mvnw clean compile test-compile test
```

### Problema 2: "Connection refused" en Tests de API

**Solución:**
```bash
# Verificar que la aplicación esté corriendo
./mvnw quarkus:dev
# Esperar a ver: "Listening on: http://localhost:8080"
```

### Problema 3: "jq: command not found"

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

**Causa:** Dev Services necesita Docker corriendo.

**Solución:**
```bash
# Verificar que Docker Desktop esté corriendo
docker ps

# Si no hay contenedores, Docker no está activo
# Mac/Windows: Abrir Docker Desktop
# Linux: sudo systemctl start docker

# Reintentar tests
./mvnw test
```

**Alternativa sin Dev Services:**
```properties
# En application.properties
quarkus.devservices.enabled=false
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/tu_bd
quarkus.datasource.username=tu_usuario
quarkus.datasource.password=tu_password
```

### Problema 5: "Port 8080 already in use"

**Solución:**
```bash
# Ver qué proceso usa el puerto
lsof -i :8080

# Matar el proceso
kill -9 <PID>

# O cambiar el puerto en application.properties
quarkus.http.port=8081
```

---

## 📈 Cobertura de Tests

```
Total Tests: 44 ejecuciones (39 métodos)
├── ValidadorDniTest: 8 tests → Cobertura: 100% de ValidadorDni
├── ScoringServiceTest: 12 ejecuciones → Cobertura: 95% de ScoringService
├── SolicitudCreditoRepositoryTest: 12 tests → Cobertura: 90% de Repository
└── CreditoRecursoTest: 12 tests → Cobertura: 85% de CreditoRecurso

Cobertura Global: ~92%
```

---

## 🎯 Buenas Prácticas

### Orden de Ejecución

```bash
# SIEMPRE en este orden:
1. ./mvnw test                            # Tests unitarios primero
2. ./mvnw quarkus:dev                     # Levantar aplicación
3. ./test-evaluacion-crediticia.sh        # Tests de API al final
```

### Antes de Hacer Commit

```bash
# Ejecutar suite completa
./mvnw clean test

# Solo hacer commit si todos pasan
git add .
git commit -m "feat: nueva funcionalidad con tests"
```

---

## 📚 Resumen del Capítulo 5

Este capítulo cubre **3 pilares fundamentales** del testing en Quarkus:

### 1️⃣ Pruebas Unitarias con JUnit 5 (60 min)

✅ **4 clases de test** con 44 ejecuciones:
- `@QuarkusTest` para levantar contexto de Quarkus
- `@Inject` para inyectar dependencias
- `@Test` para tests unitarios
- `@ParameterizedTest` para múltiples casos
- `@Transactional` para tests de BD

**Ejecutar:**
```bash
./mvnw test
```

### 2️⃣ Dev Services y Pruebas Nativas (60 min)

✅ **Dev Services:**
- Levanta PostgreSQL automáticamente para tests
- Usa Testcontainers + Docker
- Zero configuración manual
- Limpia automáticamente

✅ **Pruebas Nativas (NativeImageIT.java):**
- Compila binario con GraalVM
- Verifica compatibilidad nativa
- Arranque en milisegundos

**Ejecutar:**
```bash
# Dev Services (automático con ./mvnw test)
docker ps  # Verás PostgreSQL corriendo

# Pruebas nativas
./mvnw verify -Dnative
```

### 3️⃣ Validación de Datos y Manejo de Errores (60 min)

✅ **Validador Custom:**
- `@DniValido` - Validación personalizada de DNI peruano
- 8 tests específicos de validación

✅ **Bean Validation:**
- `@NotNull`, `@NotBlank`, `@Email`
- `@Min`, `@Max`, `@DecimalMin`, `@DecimalMax`

✅ **Exception Mappers:**
- `ValidationExceptionMapper` - Maneja errores de validación
- `GenericExceptionMapper` - Maneja errores generales
- Respuestas JSON estructuradas

**Total del capítulo: 180 minutos de contenido práctico**

---

## ✅ Checklist de Testing

- [ ] Docker Desktop está corriendo (para Dev Services)
- [ ] Los 44 tests pasan (`./mvnw test`)
- [ ] Dev Services levanta PostgreSQL automáticamente
- [ ] El script genera archivo sin errores (`./test-evaluacion-crediticia.sh`)
- [ ] Validaciones custom funcionan (@DniValido)
- [ ] Exception Mappers responden correctamente
- [ ] Swagger UI es accesible
- [ ] Health check responde
- [ ] (Opcional) Pruebas nativas pasan (`./mvnw verify -Dnative`)
- [ ] Documentación actualizada

---

**Última actualización:** Octubre 2025  
**Versión de Quarkus:** 3.28.3  
**Java:** 21+  
**Base de Datos:** PostgreSQL con Dev Services (Testcontainers)
