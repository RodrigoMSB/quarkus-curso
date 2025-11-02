# TEORÍA - Capítulo 5: Testing y Validación en Quarkus

## 📚 Índice

1. [Testing en Quarkus](#1-testing-en-quarkus)
2. [JUnit 5 y @QuarkusTest](#2-junit-5-y-quarkustest)
3. [Inyección de Dependencias en Tests](#3-inyección-de-dependencias-en-tests)
4. [REST Assured](#4-rest-assured)
5. [Dev Services](#5-dev-services)
6. [Pruebas Nativas con GraalVM](#6-pruebas-nativas-con-graalvm)
7. [Bean Validation (javax/jakarta.validation)](#7-bean-validation)
8. [Validadores Custom](#8-validadores-custom)
9. [Exception Mappers](#9-exception-mappers)
10. [Conceptos Bancarios del Ejercicio](#10-conceptos-bancarios-del-ejercicio)
11. [Buenas Prácticas](#11-buenas-prácticas)

---

## 1. Testing en Quarkus

### ¿Por qué testear en Quarkus es diferente?

**Analogía:** Si tu aplicación es un auto de carreras, los tests son la pista de pruebas donde verificas que todo funciona ANTES de la competencia real.

Quarkus tiene un enfoque único para testing:

1. **Tests rápidos:** Gracias a hot reload y arranque ultra-rápido
2. **Tests realistas:** Con Dev Services levantas bases de datos reales automáticamente
3. **Tests nativos:** Verificas que tu aplicación compilada con GraalVM funciona correctamente

### Tipos de tests en este ejercicio:

| Tipo | Qué prueba | Ejemplo |
|------|------------|---------|
| **Unitarios** | Lógica aislada | `ScoringService.calcularScore()` |
| **Integración** | Componentes + BD | `SolicitudCreditoRepository.buscarPorDni()` |
| **REST** | Endpoints HTTP | `POST /api/v1/creditos/evaluar` |
| **Validación** | Reglas de negocio | DNI peruano válido |
| **Nativos** | Binario GraalVM | Todas las pruebas en modo nativo |

---

## 2. JUnit 5 y @QuarkusTest

### @QuarkusTest: La magia comienza aquí

```java
@QuarkusTest  // ← Esta anotación hace TODA la magia
class ScoringServiceTest {
    
    @Inject  // ← Inyecta dependencias REALES
    ScoringService scoringService;
    
    @Test  // ← Método de prueba
    void deberiaCalcularScoreExcelente() {
        // Given (Dado)
        SolicitudCredito solicitud = crearSolicitud();
        
        // When (Cuando)
        Integer score = scoringService.calcularScore(solicitud);
        
        // Then (Entonces)
        assertTrue(score >= 800);
    }
}
```

### ¿Qué hace @QuarkusTest?

1. **Arranca Quarkus** en modo test (con perfiles `%test`)
2. **Levanta Dev Services** (PostgreSQL automático)
3. **Inyecta dependencias** reales (CDI completo)
4. **Ejecuta `import.sql`** para datos de prueba
5. **Limpia todo** después de los tests

**Analogía:** Es como tener un laboratorio completo que se monta solo, ejecutas tus experimentos, y se desmonta automáticamente.

---

## 3. Inyección de Dependencias en Tests

### CDI en tests

Quarkus usa **CDI (Contexts and Dependency Injection)** para inyectar beans en tus tests:

```java
@QuarkusTest
class MiTest {
    
    @Inject
    ScoringService scoringService;  // ← Bean real
    
    @Inject
    SolicitudCreditoRepository repository;  // ← Bean real
    
    @Test
    void miTest() {
        // Usas los beans como si estuvieran en producción
    }
}
```

### ¿Por qué NO usamos Mockito aquí?

En este ejercicio usamos **beans reales** porque queremos probar:
- La interacción real con la base de datos
- El algoritmo de scoring con datos reales
- Los endpoints REST con todo el stack completo

**Cuándo usar mocks:**
- Para aislar lógica compleja
- Para simular servicios externos (APIs, colas, etc.)
- Para acelerar tests muy lentos

**Ejemplo con mock (no usado en este ejercicio):**
```java
@QuarkusTest
class MiTestConMock {
    
    @InjectMock  // ← Mock de Mockito
    ExternalService externalService;
    
    @Test
    void testConMock() {
        when(externalService.llamar()).thenReturn("respuesta simulada");
        // ...
    }
}
```

---

## 4. REST Assured

### Testing de APIs REST

**REST Assured** es una librería para testear APIs REST de forma fluida y legible.

**Analogía:** Es como tener un Postman automatizado que verifica tus respuestas.

### Sintaxis básica:

```java
given()  // ← Preparar la petición
    .contentType(ContentType.JSON)
    .body(dto)
.when()  // ← Ejecutar
    .post("/api/v1/creditos/evaluar")
.then()  // ← Verificar
    .statusCode(201)
    .body("aprobada", is(true));
```

### Ejemplo del ejercicio:

```java
@Test
void deberiaAprobarSolicitudExcelente() {
    SolicitudCreditoDTO dto = new SolicitudCreditoDTO();
    dto.setDni("12345678");
    dto.setNombreCompleto("Juan Pérez García");
    // ... más campos
    
    given()
        .contentType(ContentType.JSON)
        .body(dto)  // ← JSON automático
    .when()
        .post("/api/v1/creditos/evaluar")
    .then()
        .statusCode(201)  // ← Verifica código HTTP
        .body("aprobada", is(true))  // ← Verifica campo del JSON
        .body("scoreCrediticio", greaterThanOrEqualTo(650));  // ← Hamcrest matchers
}
```

### Ventajas de REST Assured:

✅ **Fluent API:** Código legible como lenguaje natural  
✅ **Validación JSON:** Con JSONPath y Hamcrest matchers  
✅ **Serialización automática:** Convierte POJOs a JSON  
✅ **Headers, cookies, auth:** Todo integrado  

---

## 5. Dev Services

### ¿Qué son los Dev Services?

**Dev Services** es una característica de Quarkus que **levanta servicios reales automáticamente** durante desarrollo y testing.

**Analogía:** Es como tener un asistente que instala PostgreSQL, lo configura, lo arranca, carga datos y lo apaga cuando terminas. TODO automáticamente.

### ¿Cómo funciona?

1. Detecta que necesitas PostgreSQL (por la dependencia `jdbc-postgresql`)
2. Usa **Testcontainers** para levantar un contenedor Docker con PostgreSQL
3. Configura automáticamente la conexión
4. Ejecuta `import.sql` para cargar datos iniciales
5. Lo apaga cuando terminas los tests

### Sin Dev Services (antes):

```bash
# Tenías que hacer esto manualmente:
docker run -d --name postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres
# Configurar application.properties
# Crear la base de datos
# Cargar datos de prueba
# ...y acordarte de apagar el contenedor
```

### Con Dev Services (ahora):

```bash
./mvnw quarkus:test
# ¡Y listo! PostgreSQL se levanta solo
```

### Configuración en application.properties:

```properties
# Dev Services está habilitado por defecto
quarkus.devservices.enabled=true

# Script de inicialización
quarkus.datasource.devservices.init-script-path=import.sql

# Puerto específico para tests (opcional)
%test.quarkus.datasource.devservices.port=5433
```

### ¿Qué servicios soporta Dev Services?

- PostgreSQL, MySQL, MariaDB, SQL Server
- MongoDB, Redis
- Kafka, RabbitMQ, Artemis
- Keycloak (para seguridad)
- Y muchos más...

---

## 6. Pruebas Nativas con GraalVM

### ¿Por qué probar en modo nativo?

Cuando compilas con GraalVM, tu aplicación se comporta **diferente**:
- No hay JVM
- No hay reflection dinámica (limitada)
- Clases y métodos se resuelven en tiempo de compilación

**Analogía:** Es como probar tu auto en dos pistas diferentes:
- **JVM:** Autopista normal
- **Nativo:** Autopista congelada (más rápido, pero más restrictivo)

### Test nativo en el ejercicio:

```java
@QuarkusIntegrationTest  // ← Prueba la aplicación compilada
public class NativeImageIT extends CreditoRecursoTest {
    // Hereda TODOS los tests de CreditoRecursoTest
    // Pero los ejecuta sobre el binario nativo
}
```

### ¿Qué verifica?

✅ Tu aplicación arranca en modo nativo  
✅ Todos los endpoints funcionan  
✅ Las validaciones funcionan  
✅ La serialización JSON funciona  
✅ El acceso a base de datos funciona  

### Ejecutar tests nativos:

```bash
./mvnw verify -Dnative
```

**Nota:** Toma más tiempo (compilar el binario nativo es lento), pero garantiza que tu aplicación funciona en producción.

---

## 7. Bean Validation

### ¿Qué es Bean Validation?

Es un estándar de Java (JSR 380) para **validar objetos** usando anotaciones.

**Analogía:** Son como los "requisitos mínimos" en un formulario web, pero a nivel de código.

### Validaciones estándar usadas en el ejercicio:

```java
public class SolicitudCreditoDTO {
    
    @NotBlank(message = "El DNI es obligatorio")
    private String dni;
    
    @Email(message = "Email inválido")
    private String email;
    
    @Min(value = 18, message = "Debe ser mayor de 18 años")
    @Max(value = 120, message = "Edad no válida")
    private Integer edad;
    
    @DecimalMin(value = "100000.00", message = "El monto mínimo es $100.000")
    @DecimalMax(value = "50000000.00", message = "El monto máximo es $50.000.000")
    private BigDecimal montoSolicitado;
}
```

### Anotaciones de validación:

| Anotación | Qué valida | Ejemplo |
|-----------|------------|---------|
| `@NotNull` | No nulo | `@NotNull Integer edad` |
| `@NotBlank` | No nulo, no vacío, no solo espacios | `@NotBlank String nombre` |
| `@Email` | Formato de email válido | `@Email String email` |
| `@Min` / `@Max` | Rango numérico | `@Min(18) Integer edad` |
| `@DecimalMin` / `@DecimalMax` | Rango decimal | `@DecimalMin("0.0") BigDecimal` |
| `@Size` | Longitud string/colección | `@Size(min=3, max=150)` |
| `@Pattern` | Expresión regular | `@Pattern(regexp="...")` |
| `@Digits` | Cantidad de dígitos | `@Digits(integer=10, fraction=2)` |

### ¿Cuándo se ejecutan las validaciones?

Automáticamente cuando:
1. **Endpoint REST** recibe un DTO con `@Valid`
2. **Método** tiene parámetro con `@Valid`
3. **Validator.validate()** se llama manualmente

```java
@POST
@Path("/evaluar")
public Response evaluar(@Valid SolicitudCreditoDTO dto) {
    // Si dto es inválido, se lanza ConstraintViolationException
    // ANTES de entrar a este método
}
```

---

## 8. Validadores Custom

### ¿Cuándo crear un validador custom?

Cuando las validaciones estándar no son suficientes. Ejemplo: **validar DNI peruano**.

### Anatomía de un validador custom:

**1. Crear la anotación:**

```java
@Target({ElementType.FIELD, ElementType.PARAMETER})  // ← Dónde se puede usar
@Retention(RetentionPolicy.RUNTIME)  // ← Disponible en runtime
@Constraint(validatedBy = ValidadorDni.class)  // ← Quién valida
public @interface DniValido {
    
    String message() default "DNI peruano inválido";  // ← Mensaje de error
    
    Class<?>[] groups() default {};  // ← Grupos de validación
    
    Class<? extends Payload>[] payload() default {};  // ← Metadata
}
```

**2. Implementar el validador:**

```java
public class ValidadorDni implements ConstraintValidator<DniValido, String> {
    
    @Override
    public boolean isValid(String dni, ConstraintValidatorContext context) {
        if (dni == null || dni.isEmpty()) {
            return false;
        }
        
        // DNI peruano: exactamente 8 dígitos numéricos
        return dni.matches("^\\d{8}$");
    }
}
```

**3. Usar la anotación:**

```java
public class SolicitudCreditoDTO {
    
    @DniValido  // ← Tu validador custom
    @NotBlank(message = "El DNI es obligatorio")
    private String dni;
}
```

### Validación de DNI peruano:

El DNI peruano es **simple y directo**: exactamente **8 dígitos numéricos**.

```
DNI válido: 12345678
           ^^^^^^^^
           8 dígitos numéricos

Características:
✅ Exactamente 8 dígitos
✅ Solo números (0-9)
✅ Sin guiones, puntos ni espacios
✅ No tiene dígito verificador

Ejemplos válidos:
✅ 12345678
✅ 87654321
✅ 00000001

Ejemplos inválidos:
❌ 1234567    (solo 7 dígitos)
❌ 123456789  (9 dígitos)
❌ 1234567A   (contiene letra)
❌ 12-345-678 (contiene guiones)
```

**Expresión regular:**
```java
^\\d{8}$
  ^      → Inicio de la cadena
  \\d{8} → Exactamente 8 dígitos (0-9)
  $      → Fin de la cadena
```

**Comparación con RUT chileno:**

| Característica | DNI Peruano | RUT Chileno |
|----------------|-------------|-------------|
| **Formato** | 12345678 | 12345678-5 |
| **Longitud** | 8 dígitos | 7-8 dígitos + guión + DV |
| **Dígito verificador** | No | Sí (módulo 11) |
| **Complejidad** | Simple | Algoritmo matemático |
| **Validación** | Regex básica | Cálculo módulo 11 |

---

## 9. Exception Mappers

### ¿Qué son los Exception Mappers?

Son clases que **capturan excepciones** y las **transforman en respuestas HTTP** amigables.

**Analogía:** Son como un traductor que convierte errores técnicos en mensajes que el usuario entiende.

### Sin Exception Mapper:

```json
// Cliente recibe esto (horrible):
{
  "error": "jakarta.validation.ConstraintViolationException",
  "message": "evaluar.arg0.dni: DNI inválido, evaluar.arg0.edad: Debe ser mayor de 18 años",
  "stackTrace": [...]
}
```

### Con Exception Mapper:

```json
// Cliente recibe esto (legible):
{
  "error": "Errores de validación",
  "status": 400,
  "violaciones": {
    "dni": "DNI peruano inválido. Debe contener exactamente 8 dígitos.",
    "edad": "Debe ser mayor de 18 años"
  }
}
```

### Implementación:

```java
@Provider  // ← Se registra automáticamente
public class ValidationExceptionMapper 
    implements ExceptionMapper<ConstraintViolationException> {
    
    @Override
    public Response toResponse(ConstraintViolationException exception) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Errores de validación");
        error.put("status", 400);
        
        // Extraer violaciones y formatearlas
        Map<String, String> violaciones = exception.getConstraintViolations()
            .stream()
            .collect(Collectors.toMap(
                violation -> obtenerNombreCampo(violation),
                ConstraintViolation::getMessage
            ));
        
        error.put("violaciones", violaciones);
        
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(error)
            .build();
    }
}
```

### Exception Mappers en el ejercicio:

1. **ValidationExceptionMapper:** Maneja errores de validación (400)
2. **GenericExceptionMapper:** Maneja errores no controlados (500)

---

## 10. Conceptos Bancarios del Ejercicio

### 10.1 Score Crediticio

**¿Qué es?**  
Un número (0-1000) que representa la **probabilidad de que pagues tu deuda**.

**Analogía:** Es como tu "nota" en el colegio, pero para préstamos.

**Escala:**
- **800-1000:** Excelente (cliente premium)
- **650-799:** Bueno (cliente confiable)
- **500-649:** Regular (requiere análisis)
- **0-499:** Malo (alto riesgo)

### 10.2 DTI (Debt-to-Income Ratio)

**Fórmula:**
```
DTI = (Deudas Mensuales / Ingresos Mensuales) × 100
```

**Ejemplo:**
```
Ingresos: $2.000.000 CLP
Deudas: $800.000 CLP
DTI = (800.000 / 2.000.000) × 100 = 40%
```

**Interpretación:**
- **< 20%:** Excelente capacidad de endeudamiento
- **20-35%:** Buena capacidad
- **35-50%:** Capacidad limitada (pero aceptable)
- **> 50%:** Sobre-endeudamiento (rechazo automático)

**¿Por qué 50%?**  
Estándar internacional Basel III para gestión de riesgo bancario.

### 10.3 Capacidad de Pago

**Regla del 30%:**  
Máximo 30% de tus ingresos debe ir a cuotas de crédito.

```
Capacidad de Pago = Ingresos Mensuales × 0.30

Ejemplo:
Ingresos: $2.500.000
Capacidad: $2.500.000 × 0.30 = $750.000 mensuales
```

**En el algoritmo:**
```java
BigDecimal capacidadPago = solicitud.getIngresosMensuales()
    .multiply(new BigDecimal("0.30"));

BigDecimal cuotaEstimada = solicitud.getMontoSolicitado()
    .divide(new BigDecimal("36"), 2, RoundingMode.HALF_UP);  // 36 meses

if (cuotaEstimada.compareTo(capacidadPago) <= 0) {
    score += 150;  // ✅ Puede pagar
} else {
    score -= 100;  // ❌ No puede pagar
}
```

### 10.4 Estabilidad Laboral

**¿Por qué importa?**  
A mayor tiempo en el mismo empleo, menor riesgo de perder ingresos.

**Scoring:**
```
≥ 24 meses: +120 puntos (muy estable)
≥ 12 meses: +80 puntos (estable)
≥ 6 meses: +40 puntos (aceptable)
< 6 meses: -20 puntos (riesgo)
```

### 10.5 Factores de Edad

**Rango óptimo:** 25-55 años

**¿Por qué?**
- **< 25 años:** Ingresos inestables, poca historia crediticia
- **25-55 años:** Ingresos estables, capacidad de pago óptima
- **> 55 años:** Riesgo de jubilación, ingresos decrecientes

```java
if (edad >= 25 && edad <= 55) {
    score += 80;  // Rango óptimo
} else if (edad > 70) {
    score -= 30;  // Riesgo por edad avanzada
}
```

---

## 11. Buenas Prácticas

### 11.1 Tests

✅ **Nombre descriptivos:** `deberiaRechazarPorDTIAlto()`  
✅ **Estructura Given-When-Then:**
```java
// Given (contexto)
SolicitudCredito solicitud = crearSolicitud();

// When (acción)
Integer score = scoringService.calcularScore(solicitud);

// Then (verificación)
assertTrue(score >= 800);
```
✅ **Tests independientes:** Cada test debe poder ejecutarse solo  
✅ **Tests rápidos:** < 1 segundo idealmente  
✅ **Tests repetibles:** Mismo resultado cada vez  

### 11.2 Validaciones

✅ **Mensajes claros:** "El DNI es obligatorio" (no "campo requerido")  
✅ **Validar en el DTO:** No confiar en el cliente  
✅ **Validaciones de negocio en servicios:** Lógica compleja fuera del DTO  
✅ **Respuestas estructuradas:** JSON con campo "violaciones"  

### 11.3 Exception Handling

✅ **No exponer stack traces** en producción  
✅ **Códigos HTTP correctos:**
  - 400: Bad Request (error del cliente)
  - 422: Unprocessable Entity (validación de negocio)
  - 500: Internal Server Error (error del servidor)
✅ **Loguear errores** para debugging  
✅ **Mensajes traducibles** (i18n)  

### 11.4 Dev Services

✅ **No configurar BD en tests:** Dev Services lo hace  
✅ **Usar `import.sql`** para datos de prueba  
✅ **Limpiar datos entre tests:** `@Transactional` + rollback  
✅ **Puerto específico para tests:** Evitar conflictos  

---

## 📊 Resumen del Capítulo

| Concepto | Qué aprendiste | Dónde se usa |
|----------|----------------|--------------|
| **@QuarkusTest** | Arrancar Quarkus para tests | Todos los tests |
| **@Inject** | Inyectar beans en tests | `ScoringServiceTest` |
| **REST Assured** | Testear APIs REST | `CreditoRecursoTest` |
| **Dev Services** | BD automática en tests | Automático |
| **Bean Validation** | Validar DTOs | `SolicitudCreditoDTO` |
| **Validador Custom** | DNI peruano | `@DniValido` |
| **Exception Mapper** | Transformar errores | `ValidationExceptionMapper` |
| **Tests Nativos** | Probar binario GraalVM | `NativeImageIT` |
| **DTI** | Ratio deuda/ingreso | `ScoringService` |
| **Scoring** | Algoritmo crediticio | `ScoringService` |

---

## 🎯 Objetivos de Aprendizaje Cumplidos

Al completar este capítulo, ahora sabes:

✅ Escribir tests unitarios con `@QuarkusTest`  
✅ Inyectar dependencias en tests con `@Inject`  
✅ Testear APIs REST con REST Assured  
✅ Usar Dev Services para BD automática  
✅ Validar datos con Bean Validation  
✅ Crear validadores custom para DNI peruano  
✅ Manejar errores con Exception Mappers  
✅ Ejecutar tests nativos con GraalVM  
✅ Implementar lógica de negocio bancaria  
✅ Calcular scoring crediticio