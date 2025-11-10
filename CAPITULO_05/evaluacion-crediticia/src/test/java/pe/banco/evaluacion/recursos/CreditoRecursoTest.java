package pe.banco.evaluacion.recursos;

import io.quarkus.test.junit.QuarkusTest;  // Anotación que habilita testing con contexto de Quarkus completo y servidor HTTP
import io.restassured.http.ContentType;  // Enumeración de tipos MIME para Content-Type header
import org.junit.jupiter.api.Test;  // Anotación para métodos de test unitarios
import pe.banco.evaluacion.dtos.SolicitudCreditoDTO;  // DTO de entrada para solicitudes

import java.math.BigDecimal;  // Tipo numérico de precisión para montos financieros

import static io.restassured.RestAssured.given;  // DSL fluido de REST Assured para hacer requests HTTP
import static org.hamcrest.Matchers.*;  // Matchers de Hamcrest para aserciones expresivas

/**
 * Suite de tests de integración para el recurso REST de créditos.
 * <p>
 * Esta clase de test valida exhaustivamente el {@link CreditoRecurso}, que expone la API REST
 * del sistema de evaluación crediticia. A diferencia de tests unitarios que mockean dependencias,
 * estos son "end-to-end tests" que validan el stack completo: HTTP → JAX-RS → Validación →
 * Servicio → Repositorio → Base de datos → Respuesta HTTP.
 * </p>
 * <p>
 * <b>Analogía:</b> Piensa en estos tests como un "cliente mystery shopper" del banco. Así como
 * un mystery shopper visita una sucursal bancaria pretendiendo ser un cliente real para verificar
 * que todo el proceso (recepción, evaluación, respuesta) funciona correctamente de principio a fin,
 * estos tests automatizan esa verificación enviando requests HTTP reales y validando respuestas
 * completas, simulando exactamente lo que haría un cliente real de la API.
 * </p>
 * 
 * <h3>Alcance de testing: End-to-End Integration Tests</h3>
 * <table border="1">
 *   <tr>
 *     <th>Capa</th>
 *     <th>¿Se testea?</th>
 *     <th>Componente</th>
 *   </tr>
 *   <tr>
 *     <td>HTTP</td>
 *     <td>✅ Sí</td>
 *     <td>Servidor HTTP embebido, serialización JSON</td>
 *   </tr>
 *   <tr>
 *     <td>REST</td>
 *     <td>✅ Sí</td>
 *     <td>JAX-RS endpoints, routing, status codes</td>
 *   </tr>
 *   <tr>
 *     <td>Validación</td>
 *     <td>✅ Sí</td>
 *     <td>Bean Validation, ValidationExceptionMapper</td>
 *   </tr>
 *   <tr>
 *     <td>Servicio</td>
 *     <td>✅ Sí</td>
 *     <td>ScoringService con lógica de negocio real</td>
 *   </tr>
 *   <tr>
 *     <td>Persistencia</td>
 *     <td>✅ Sí</td>
 *     <td>Repository, JPA, Hibernate, H2 database</td>
 *   </tr>
 * </table>
 * 
 * <h3>Herramienta de testing: REST Assured</h3>
 * <p>
 * REST Assured es una librería Java que proporciona un DSL (Domain Specific Language)
 * fluido y expresivo para testing de APIs REST. Ventajas sobre hacer HTTP requests manualmente:
 * <ul>
 *   <li><b>Sintaxis fluida:</b> given().when().then() es autodocumentado</li>
 *   <li><b>Validación integrada:</b> Hamcrest matchers directamente en assertions</li>
 *   <li><b>Manejo automático de JSON:</b> Serialización/deserialización transparente</li>
 *   <li><b>Extracción de datos:</b> JsonPath para navegar response bodies complejos</li>
 *   <li><b>Logging integrado:</b> Ver requests/responses completos para debugging</li>
 * </ul>
 * </p>
 * 
 * <h3>Anatomía de un test REST Assured:</h3>
 * <pre>
 * given()                              // GIVEN: Preparar request
 *     .contentType(ContentType.JSON)   //   - Content-Type header
 *     .body(dto)                       //   - Request body (auto-serializado a JSON)
 * .when()                              // WHEN: Ejecutar request
 *     .post("/api/v1/creditos/evaluar") //   - Método HTTP y URL
 * .then()                              // THEN: Validar response
 *     .statusCode(201)                 //   - Status HTTP
 *     .body("aprobada", is(true))      //   - Campo en JSON response
 *     .body("score", greaterThan(650)); //   - Validación con matcher
 * </pre>
 * 
 * <h3>Estrategia de testing exhaustivo:</h3>
 * <p>
 * La suite implementa testing completo de la API REST:
 * <ul>
 *   <li><b>Happy paths:</b> Solicitudes válidas que aprueban/rechazan correctamente</li>
 *   <li><b>Validación de entrada:</b> Campos requeridos, formatos, rangos</li>
 *   <li><b>Edge cases:</b> Valores límite, casos especiales</li>
 *   <li><b>Error handling:</b> Respuestas 400, 404 con mensajes apropiados</li>
 *   <li><b>CRUD completo:</b> POST (crear), GET (leer individual y lista)</li>
 * </ul>
 * </p>
 * 
 * <h3>Cobertura de endpoints:</h3>
 * <table border="1">
 *   <tr>
 *     <th>Endpoint</th>
 *     <th>Método</th>
 *     <th>Tests</th>
 *     <th>Escenarios</th>
 *   </tr>
 *   <tr>
 *     <td>/api/v1/creditos/evaluar</td>
 *     <td>POST</td>
 *     <td>10</td>
 *     <td>Aprobación, rechazo, validaciones diversas</td>
 *   </tr>
 *   <tr>
 *     <td>/api/v1/creditos/{id}</td>
 *     <td>GET</td>
 *     <td>2</td>
 *     <td>Encontrado (200), no encontrado (404)</td>
 *   </tr>
 *   <tr>
 *     <td>/api/v1/creditos</td>
 *     <td>GET</td>
 *     <td>1</td>
 *     <td>Listar todas</td>
 *   </tr>
 * </table>
 * 
 * <h3>Servidor HTTP en tests:</h3>
 * <p>
 * @QuarkusTest inicia automáticamente un servidor HTTP embebido en puerto aleatorio.
 * REST Assured se configura automáticamente para apuntar a ese servidor. El ciclo de vida es:
 * <ol>
 *   <li>JUnit detecta @QuarkusTest</li>
 *   <li>Quarkus inicia aplicación completa (CDI, BD, HTTP server)</li>
 *   <li>Tests ejecutan requests HTTP reales contra servidor local</li>
 *   <li>Servidor procesa requests como en producción</li>
 *   <li>Al finalizar tests, Quarkus detiene servidor y limpia recursos</li>
 * </ol>
 * </p>
 * 
 * <h3>Base de datos en tests:</h3>
 * <p>
 * Similar a SolicitudCreditoRepositoryTest, estos tests usan H2 en memoria:
 * <ul>
 *   <li>Cada test ve datos persistidos por tests anteriores (acumulativo)</li>
 *   <li>No hay @BeforeEach que limpie BD entre tests</li>
 *   <li>Tests deben ser idempotentes o usar datos únicos (DNI/email diferentes)</li>
 * </ul>
 * Esto es diferente a RepositoryTest que limpia BD en cada test.
 * </p>
 * 
 * <h3>Orden de ejecución de tests:</h3>
 * <p>
 * JUnit 5 NO garantiza orden de ejecución a menos que uses @TestMethodOrder.
 * Estos tests están diseñados para ejecutarse en cualquier orden (orden-independientes)
 * usando DNIs/emails únicos en cada test para evitar conflictos.
 * </p>
 * 
 * <h3>Ejecución de los tests:</h3>
 * <pre>
 * # Ejecutar todos los tests del recurso REST
 * ./mvnw test -Dtest=CreditoRecursoTest
 * 
 * # Ejecutar con logs de requests HTTP (debugging)
 * ./mvnw test -Dtest=CreditoRecursoTest -Drestassured.logging=ALL
 * 
 * # Ejecutar contra servidor externo (no embebido)
 * ./mvnw test -Dtest=CreditoRecursoTest -Dquarkus.http.test-port=8080
 * 
 * # Ver SQL queries generadas
 * ./mvnw test -Dtest=CreditoRecursoTest -Dquarkus.hibernate-orm.log.sql=true
 * </pre>
 * 
 * <h3>Mejoras pendientes:</h3>
 * <ul>
 *   <li>Tests de concurrencia: múltiples requests simultáneos</li>
 *   <li>Tests de performance: medir tiempos de response</li>
 *   <li>Tests de seguridad: inyección SQL, XSS, CSRF</li>
 *   <li>Tests de rate limiting: detectar abuse de API</li>
 *   <li>Tests de CORS: headers de cross-origin</li>
 *   <li>Tests de compresión: Accept-Encoding gzip</li>
 *   <li>Tests de content negotiation: Accept application/xml</li>
 *   <li>Contract testing: validar contra OpenAPI spec</li>
 * </ul>
 * 
 * @see CreditoRecurso
 * @see SolicitudCreditoDTO
 */
@QuarkusTest
public class CreditoRecursoTest {

    /**
     * Verifica que solicitud con buen perfil crediticio sea aprobada.
     * <p>
     * Test del "camino feliz" (happy path) donde todo funciona correctamente:
     * cliente envía datos válidos, sistema evalúa, y responde con aprobación.
     * </p>
     * 
     * <h4>Perfil del cliente de test:</h4>
     * <ul>
     *   <li><b>Ingresos:</b> $2,500,000 (buenos ingresos)</li>
     *   <li><b>Deudas:</b> $300,000 (DTI = 12%, excelente)</li>
     *   <li><b>Empleo:</b> 36 meses (3 años, muy estable)</li>
     *   <li><b>Edad:</b> 35 años (rango óptimo)</li>
     *   <li><b>Monto:</b> $5,000,000 (ratio 2x ingresos, razonable)</li>
     * </ul>
     * Este perfil debería obtener score > 750 y ser aprobado.
     * 
     * <h4>Validaciones exhaustivas de la respuesta:</h4>
     * <pre>
     * .statusCode(201)                             // HTTP 201 Created
     * .body("aprobada", is(true))                  // Campo aprobada = true
     * .body("scoreCrediticio", notNullValue())     // Score fue calculado
     * .body("scoreCrediticio", greaterThanOrEqualTo(650))  // Score >= umbral
     * .body("razonEvaluacion", notNullValue())     // Razón está presente
     * .body("estado", is("APROBADA"))              // Estado final correcto
     * .body("solicitudId", notNullValue())         // ID fue generado
     * </pre>
     * 
     * <h4>¿Por qué status 201 y no 200?</h4>
     * <p>
     * HTTP 201 Created es semánticamente correcto para POST que crea un recurso.
     * Indica al cliente que:
     * <ul>
     *   <li>Request fue exitoso</li>
     *   <li>Se creó un nuevo recurso (solicitud en BD)</li>
     *   <li>Response body contiene representación del recurso creado</li>
     * </ul>
     * 200 OK sería técnicamente válido pero menos preciso semánticamente.
     * </p>
     * 
     * <h4>Request HTTP generado (aproximado):</h4>
     * <pre>
     * POST /api/v1/creditos/evaluar HTTP/1.1
     * Content-Type: application/json
     * 
     * {
     *   "dni": "12345678",
     *   "nombreCompleto": "Juan Pérez Test",
     *   "email": "juan.test@email.cl",
     *   "edad": 35,
     *   "ingresosMensuales": 2500000,
     *   "deudasActuales": 300000,
     *   "montoSolicitado": 5000000,
     *   "mesesEnEmpleoActual": 36
     * }
     * </pre>
     * 
     * <h4>Response HTTP esperado:</h4>
     * <pre>
     * HTTP/1.1 201 Created
     * Content-Type: application/json
     * 
     * {
     *   "solicitudId": 1,
     *   "dni": "12345678",
     *   "nombreCompleto": "Juan Pérez Test",
     *   "scoreCrediticio": 780,
     *   "aprobada": true,
     *   "razonEvaluacion": "Aprobado: Perfil crediticio cumple...",
     *   "estado": "APROBADA"
     * }
     * </pre>
     */
    @Test
    void deberiaEvaluarSolicitudYAprobar() {
        SolicitudCreditoDTO dto = new SolicitudCreditoDTO();
        dto.setDni("12345678");
        dto.setNombreCompleto("Juan Pérez Test");
        dto.setEmail("juan.test@email.cl");
        dto.setEdad(35);
        dto.setIngresosMensuales(new BigDecimal("2500000"));
        dto.setDeudasActuales(new BigDecimal("300000"));
        dto.setMontoSolicitado(new BigDecimal("5000000"));
        dto.setMesesEnEmpleoActual(36);

        given()
            .contentType(ContentType.JSON)
            .body(dto)
        .when()
            .post("/api/v1/creditos/evaluar")
        .then()
            .statusCode(201)
            .body("aprobada", is(true))
            .body("scoreCrediticio", notNullValue())
            .body("scoreCrediticio", greaterThanOrEqualTo(650))
            .body("razonEvaluacion", notNullValue())
            .body("estado", is("APROBADA"))
            .body("solicitudId", notNullValue());
    }

    /**
     * Verifica que solicitud con mal perfil crediticio sea rechazada.
     * <p>
     * Test del "camino de rechazo" donde el sistema correctamente identifica
     * un cliente de alto riesgo y rechaza la solicitud.
     * </p>
     * 
     * <h4>Perfil del cliente de test (alto riesgo):</h4>
     * <ul>
     *   <li><b>Ingresos:</b> $1,000,000 (ingresos bajos)</li>
     *   <li><b>Deudas:</b> $700,000 (DTI = 70%, crítico!)</li>
     *   <li><b>Empleo:</b> 3 meses (justo en el límite mínimo)</li>
     *   <li><b>Edad:</b> 25 años (edad joven, menor bonificación)</li>
     *   <li><b>Monto:</b> $3,000,000 (ratio 3x ingresos, alto)</li>
     * </ul>
     * Este perfil debería obtener score < 650 y ser rechazado.
     * 
     * <h4>Validaciones de rechazo:</h4>
     * <pre>
     * .statusCode(201)                      // Aún 201 (solicitud se creó)
     * .body("aprobada", is(false))          // Pero fue rechazada
     * .body("scoreCrediticio", notNullValue())
     * .body("scoreCrediticio", lessThan(650))  // Score bajo umbral
     * .body("estado", is("RECHAZADA"))
     * </pre>
     * 
     * <h4>¿Por qué 201 y no 400 en rechazo?</h4>
     * <p>
     * Es importante distinguir:
     * <ul>
     *   <li><b>201:</b> Solicitud PROCESADA exitosamente (aunque rechazada por negocio)</li>
     *   <li><b>400:</b> Solicitud INVÁLIDA técnicamente (datos mal formados)</li>
     * </ul>
     * Un rechazo de crédito es un resultado de negocio válido, no un error técnico.
     * El sistema funcionó correctamente al evaluar y rechazar.
     * </p>
     * 
     * <h4>Razones probables de rechazo:</h4>
     * <ul>
     *   <li>"Rechazado: Ratio deuda/ingreso (70.00%) supera el límite permitido (50%)."</li>
     *   <li>"Rechazado: Score crediticio insuficiente para aprobación automática."</li>
     * </ul>
     */
    @Test
    void deberiaEvaluarSolicitudYRechazar() {
        SolicitudCreditoDTO dto = new SolicitudCreditoDTO();
        dto.setDni("23456789");
        dto.setNombreCompleto("María Silva Test");
        dto.setEmail("maria.test@email.cl");
        dto.setEdad(25);
        dto.setIngresosMensuales(new BigDecimal("1000000"));
        dto.setDeudasActuales(new BigDecimal("700000"));  // DTI 70%
        dto.setMontoSolicitado(new BigDecimal("3000000"));
        dto.setMesesEnEmpleoActual(3);

        given()
            .contentType(ContentType.JSON)
            .body(dto)
        .when()
            .post("/api/v1/creditos/evaluar")
        .then()
            .statusCode(201)
            .body("aprobada", is(false))
            .body("scoreCrediticio", notNullValue())
            .body("scoreCrediticio", lessThan(650))
            .body("estado", is("RECHAZADA"));
    }

    /**
     * Verifica que DNI inválido (formato incorrecto) retorne error 400.
     * <p>
     * Test de validación automática de Bean Validation. Valida que el sistema
     * rechaza solicitudes con DNI mal formado ANTES de procesarlas.
     * </p>
     * 
     * <h4>Flujo de validación:</h4>
     * <ol>
     *   <li>Cliente envía request con dni="123" (solo 3 dígitos)</li>
     *   <li>Quarkus deserializa JSON a SolicitudCreditoDTO</li>
     *   <li>@Valid activa Bean Validation</li>
     *   <li>@DniValido en campo dni ejecuta ValidadorDni</li>
     *   <li>ValidadorDni.isValid("123") retorna false</li>
     *   <li>Bean Validation lanza ConstraintViolationException</li>
     *   <li>ValidationExceptionMapper captura excepción</li>
     *   <li>Mapper construye respuesta 400 con JSON estructurado</li>
     * </ol>
     * 
     * <h4>Response esperado:</h4>
     * <pre>
     * HTTP/1.1 400 Bad Request
     * Content-Type: application/json
     * 
     * {
     *   "error": "Errores de validación",
     *   "status": 400,
     *   "violaciones": {
     *     "dni": "DNI inválido. Debe contener 8 dígitos"
     *   }
     * }
     * </pre>
     * 
     * <h4>Validación del response:</h4>
     * <pre>
     * .statusCode(400)                          // Bad Request
     * .body("violaciones.dni", containsString("DNI"))  // Mensaje contiene "DNI"
     * </pre>
     * 
     * <p>
     * <b>Nota:</b> El test solo verifica que el mensaje contiene "DNI", no el texto exacto.
     * Esto hace el test más robusto ante cambios en redacción del mensaje.
     * </p>
     */
    @Test
    void deberiaRechazarRutInvalido() {
        SolicitudCreditoDTO dto = new SolicitudCreditoDTO();
        dto.setDni("123");  // DNI inválido: solo 3 dígitos
        dto.setNombreCompleto("Carlos Test");
        dto.setEmail("carlos.test@email.cl");
        dto.setEdad(30);
        dto.setIngresosMensuales(new BigDecimal("2000000"));
        dto.setDeudasActuales(new BigDecimal("300000"));
        dto.setMontoSolicitado(new BigDecimal("4000000"));
        dto.setMesesEnEmpleoActual(12);

        given()
            .contentType(ContentType.JSON)
            .body(dto)
        .when()
            .post("/api/v1/creditos/evaluar")
        .then()
            .statusCode(400)
            .body("violaciones.dni", containsString("DNI"));
    }

    /**
     * Verifica que DTO completamente vacío retorne error 400.
     * <p>
     * Test de validación masiva donde TODOS los campos requeridos faltan.
     * Valida que Bean Validation detecta múltiples violaciones simultáneas.
     * </p>
     * 
     * <h4>Campos que fallarán validación:</h4>
     * <ul>
     *   <li>dni: @NotBlank → "El DNI es obligatorio"</li>
     *   <li>nombreCompleto: @NotBlank → "El nombre completo es obligatorio"</li>
     *   <li>email: @NotBlank → "El email es obligatorio"</li>
     *   <li>edad: @NotNull → "La edad es obligatoria"</li>
     *   <li>ingresosMensuales: @NotNull → "Los ingresos mensuales son obligatorios"</li>
     *   <li>deudasActuales: @NotNull → "Las deudas actuales son obligatorias"</li>
     *   <li>montoSolicitado: @NotNull → "El monto solicitado es obligatorio"</li>
     *   <li>mesesEnEmpleoActual: @NotNull → "Los meses en empleo actual son obligatorios"</li>
     * </ul>
     * 
     * <h4>Response esperado:</h4>
     * <pre>
     * HTTP/1.1 400 Bad Request
     * {
     *   "error": "Errores de validación",
     *   "status": 400,
     *   "violaciones": {
     *     "dni": "El DNI es obligatorio",
     *     "nombreCompleto": "El nombre completo es obligatorio",
     *     "email": "El email es obligatorio",
     *     "edad": "La edad es obligatoria",
     *     "ingresosMensuales": "Los ingresos mensuales son obligatorios",
     *     "deudasActuales": "Las deudas actuales son obligatorias",
     *     "montoSolicitado": "El monto solicitado es obligatorio",
     *     "mesesEnEmpleoActual": "Los meses en empleo actual son obligatorios"
     *   }
     * }
     * </pre>
     * 
     * <p>
     * <b>Nota:</b> El test solo verifica statusCode 400, no valida el contenido
     * completo del response. Esto podría mejorarse validando que hay múltiples
     * violaciones presentes.
     * </p>
     */
    @Test
    void deberiaRechazarCamposRequeridos() {
        SolicitudCreditoDTO dto = new SolicitudCreditoDTO();  // DTO completamente vacío

        given()
            .contentType(ContentType.JSON)
            .body(dto)
        .when()
            .post("/api/v1/creditos/evaluar")
        .then()
            .statusCode(400);
    }

    /**
     * Verifica que email con formato inválido retorne error 400.
     * <p>
     * Test de validación de @Email de Bean Validation.
     * </p>
     * 
     * <h4>Email de test:</h4>
     * <p>
     * "email-invalido" no tiene @ ni dominio, por lo que falla validación de formato.
     * </p>
     * 
     * <h4>Otros formatos inválidos que también fallarían:</h4>
     * <ul>
     *   <li>"usuario@" (falta dominio)</li>
     *   <li>"@dominio.com" (falta usuario)</li>
     *   <li>"usuario@dominio" (falta TLD .com/.pe/etc)</li>
     *   <li>"usuario dominio.com" (falta @)</li>
     * </ul>
     * 
     * <h4>Response esperado:</h4>
     * <pre>
     * HTTP/1.1 400 Bad Request
     * {
     *   "error": "Errores de validación",
     *   "status": 400,
     *   "violaciones": {
     *     "email": "Email inválido"
     *   }
     * }
     * </pre>
     */
    @Test
    void deberiaRechazarEmailInvalido() {
        SolicitudCreditoDTO dto = new SolicitudCreditoDTO();
        dto.setDni("12345678");
        dto.setNombreCompleto("Test Usuario");
        dto.setEmail("email-invalido");  // Sin @ ni dominio
        dto.setEdad(30);
        dto.setIngresosMensuales(new BigDecimal("2000000"));
        dto.setDeudasActuales(new BigDecimal("300000"));
        dto.setMontoSolicitado(new BigDecimal("4000000"));
        dto.setMesesEnEmpleoActual(12);

        given()
            .contentType(ContentType.JSON)
            .body(dto)
        .when()
            .post("/api/v1/creditos/evaluar")
        .then()
            .statusCode(400);
    }

    /**
     * Verifica que edad menor a 18 años retorne error 400.
     * <p>
     * Test de validación de @Min(18) que implementa requisito legal de mayoría de edad.
     * </p>
     * 
     * <h4>Fundamento legal:</h4>
     * <p>
     * En Perú, la mayoría de edad es 18 años (Código Civil, Art. 42).
     * Menores de edad NO pueden contratar créditos sin representante legal.
     * </p>
     * 
     * <h4>Edge case - edad 17:</h4>
     * <p>
     * 17 está justo por debajo del límite (boundary value), lo que lo hace
     * un excelente caso de test. Si la validación tuviera un bug off-by-one
     * (ej: edad > 18 en vez de edad >= 18), este test lo detectaría.
     * </p>
     * 
     * <h4>Response esperado:</h4>
     * <pre>
     * {
     *   "violaciones": {
     *     "edad": "Debe ser mayor de 18 años"
     *   }
     * }
     * </pre>
     */
    @Test
    void deberiaRechazarEdadMenorA18() {
        SolicitudCreditoDTO dto = new SolicitudCreditoDTO();
        dto.setDni("12345678");
        dto.setNombreCompleto("Menor Edad");
        dto.setEmail("menor@email.cl");
        dto.setEdad(17);  // Menor de edad
        dto.setIngresosMensuales(new BigDecimal("1500000"));
        dto.setDeudasActuales(new BigDecimal("100000"));
        dto.setMontoSolicitado(new BigDecimal("2000000"));
        dto.setMesesEnEmpleoActual(6);

        given()
            .contentType(ContentType.JSON)
            .body(dto)
        .when()
            .post("/api/v1/creditos/evaluar")
        .then()
            .statusCode(400);
    }

    /**
     * Verifica que monto superior al máximo permitido retorne error 400.
     * <p>
     * Test de validación de @DecimalMax en SolicitudCreditoDTO.
     * </p>
     * 
     * <h4>Política de negocio del banco:</h4>
     * <ul>
     *   <li>Monto mínimo: $100,000</li>
     *   <li>Monto máximo: $50,000,000</li>
     * </ul>
     * 
     * <h4>Monto de test:</h4>
     * <p>
     * $60,000,000 excede el límite de $50,000,000, por lo que debe ser rechazado
     * por Bean Validation antes de llegar a lógica de negocio.
     * </p>
     * 
     * <h4>¿Por qué límite máximo?</h4>
     * <ul>
     *   <li><b>Gestión de riesgo:</b> Limitar exposición por cliente</li>
     *   <li><b>Liquidez:</b> Banco no puede prestar infinito a un solo cliente</li>
     *   <li><b>Regulación:</b> SBS limita concentración de cartera</li>
     * </ul>
     * 
     * <h4>Response esperado:</h4>
     * <pre>
     * {
     *   "violaciones": {
     *     "montoSolicitado": "El monto máximo es $50.000.000"
     *   }
     * }
     * </pre>
     */
    @Test
    void deberiaRechazarMontoFueraDeRango() {
        SolicitudCreditoDTO dto = new SolicitudCreditoDTO();
        dto.setDni("12345678");
        dto.setNombreCompleto("Test Usuario");
        dto.setEmail("test@email.cl");
        dto.setEdad(30);
        dto.setIngresosMensuales(new BigDecimal("2000000"));
        dto.setDeudasActuales(new BigDecimal("300000"));
        dto.setMontoSolicitado(new BigDecimal("60000000"));  // Excede máximo
        dto.setMesesEnEmpleoActual(12);

        given()
            .contentType(ContentType.JSON)
            .body(dto)
        .when()
            .post("/api/v1/creditos/evaluar")
        .then()
            .statusCode(400);
    }

    /**
     * Verifica que GET /creditos/{id} retorne solicitud existente con status 200.
     * <p>
     * Test de endpoint de consulta individual. Asume que existe solicitud con ID 1
     * (creada por tests anteriores o datos de prueba).
     * </p>
     * 
     * <h4>⚠️ Fragilidad del test:</h4>
     * <p>
     * Este test asume que:
     * <ul>
     *   <li>Existe solicitud con ID 1 en base de datos</li>
     *   <li>Fue creada por test anterior (deberiaEvaluarSolicitudYAprobar)</li>
     * </ul>
     * Si tests se ejecutan en orden diferente o BD se limpia, este test fallará.
     * </p>
     * 
     * <h4>Mejora recomendada:</h4>
     * <pre>
     * // 1. Crear solicitud en este test
     * Response createResponse = given()
     *     .contentType(ContentType.JSON)
     *     .body(dto)
     * .when()
     *     .post("/api/v1/creditos/evaluar")
     * .then()
     *     .statusCode(201)
     *     .extract().response();
     * 
     * Long solicitudId = createResponse.jsonPath().getLong("solicitudId");
     * 
     * // 2. Consultar la solicitud creada
     * given()
     * .when()
     *     .get("/api/v1/creditos/" + solicitudId)
     * .then()
     *     .statusCode(200)
     *     .body("id", is(solicitudId.intValue()));
     * </pre>
     * 
     * <h4>Response esperado:</h4>
     * <pre>
     * HTTP/1.1 200 OK
     * {
     *   "id": 1,
     *   "dni": "12345678",
     *   "nombreCompleto": "Juan Pérez Test",
     *   "scoreCrediticio": 780,
     *   "aprobada": true,
     *   ...
     * }
     * </pre>
     */
    @Test
    void deberiaObtenerSolicitudPorId() {
        given()
        .when()
            .get("/api/v1/creditos/1")
        .then()
            .statusCode(200)
            .body("id", is(1))
            .body("dni", notNullValue())
            .body("nombreCompleto", notNullValue())
            .body("scoreCrediticio", notNullValue());
    }

    /**
     * Verifica que GET /creditos/{id} retorne 404 cuando ID no existe.
     * <p>
     * Test del manejo de "recurso no encontrado". Valida que el endpoint
     * retorna status HTTP apropiado cuando se solicita ID inexistente.
     * </p>
     * 
     * <h4>ID de test:</h4>
     * <p>
     * 99999 es ID suficientemente alto que es improbable exista en BD de test.
     * </p>
     * 
     * <h4>Response esperado:</h4>
     * <pre>
     * HTTP/1.1 404 Not Found
     * (sin body, o body vacío)
     * </pre>
     * 
     * <h4>Semántica REST:</h4>
     * <ul>
     *   <li><b>200:</b> Recurso encontrado y retornado</li>
     *   <li><b>404:</b> Recurso no existe en el servidor</li>
     *   <li><b>400:</b> Request malformado (ej: ID con letras)</li>
     * </ul>
     */
    @Test
    void deberiaRetornar404SiSolicitudNoExiste() {
        given()
        .when()
            .get("/api/v1/creditos/99999")
        .then()
            .statusCode(404);
    }

    /**
     * Verifica que GET /creditos retorne lista de todas las solicitudes.
     * <p>
     * Test de endpoint de listado completo (sin paginación).
     * </p>
     * 
     * <h4>Validación:</h4>
     * <p>
     * Verifica que response contiene array con al menos 5 elementos.
     * El número exacto depende de cuántos tests se ejecutaron previamente
     * (cada POST /evaluar agrega una solicitud a BD).
     * </p>
     * 
     * <h4>Sintaxis de REST Assured:</h4>
     * <pre>
     * .body("size()", greaterThanOrEqualTo(5))
     * </pre>
     * <p>
     * size() es función especial de JsonPath que retorna tamaño del array JSON.
     * Equivalente a parsear JSON y hacer array.length.
     * </p>
     * 
     * <h4>⚠️ Advertencia:</h4>
     * <p>
     * Este endpoint sin paginación es problemático en producción. Con 100,000
     * solicitudes, retornaría 50MB+ de JSON, causando timeout. Ver mejoras
     * pendientes en documentación de CreditoRecurso.
     * </p>
     */
    @Test
    void deberiaListarTodasLasSolicitudes() {
        given()
        .when()
            .get("/api/v1/creditos")
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(5));
    }

    /**
     * Verifica que ingresos de 0 (cero) sean rechazados.
     * <p>
     * Test de validación de @DecimalMin con inclusive=false en ingresosMensuales.
     * </p>
     * 
     * <h4>Validación en DTO:</h4>
     * <pre>
     * @DecimalMin(value = "0.0", inclusive = false, message = "Los ingresos deben ser mayores a 0")
     * </pre>
     * 
     * <h4>¿Por qué rechazar ingresos=0?</h4>
     * <ul>
     *   <li>División por cero al calcular DTI (deudas / ingresos)</li>
     *   <li>Sin ingresos, no hay capacidad de pago</li>
     *   <li>Indicador de datos incorrectos o fraude</li>
     * </ul>
     * 
     * <h4>Response esperado:</h4>
     * <pre>
     * {
     *   "violaciones": {
     *     "ingresosMensuales": "Los ingresos deben ser mayores a 0"
     *   }
     * }
     * </pre>
     */
    @Test
    void deberiaValidarIngresosPositivos() {
        SolicitudCreditoDTO dto = new SolicitudCreditoDTO();
        dto.setDni("12345678");
        dto.setNombreCompleto("Test Usuario");
        dto.setEmail("test@email.cl");
        dto.setEdad(30);
        dto.setIngresosMensuales(new BigDecimal("0"));  // Ingresos = 0
        dto.setDeudasActuales(new BigDecimal("300000"));
        dto.setMontoSolicitado(new BigDecimal("4000000"));
        dto.setMesesEnEmpleoActual(12);

        given()
            .contentType(ContentType.JSON)
            .body(dto)
        .when()
            .post("/api/v1/creditos/evaluar")
        .then()
            .statusCode(400);
    }

    /**
     * Verifica que deudas negativas sean rechazadas.
     * <p>
     * Test de validación de @DecimalMin(0) en deudasActuales.
     * </p>
     * 
     * <h4>¿Por qué rechazar deudas negativas?</h4>
     * <ul>
     *   <li>No tiene sentido matemático (deuda es siempre ≥ 0)</li>
     *   <li>Indicador de error de captura de datos</li>
     *   <li>Podría distorsionar cálculo de DTI</li>
     * </ul>
     * 
     * <h4>Validación en DTO:</h4>
     * <pre>
     * @DecimalMin(value = "0.0", message = "Las deudas no pueden ser negativas")
     * </pre>
     * 
     * <h4>Response esperado:</h4>
     * <pre>
     * {
     *   "violaciones": {
     *     "deudasActuales": "Las deudas no pueden ser negativas"
     *   }
     * }
     * </pre>
     */
    @Test
    void deberiaValidarDeudasNoNegativas() {
        SolicitudCreditoDTO dto = new SolicitudCreditoDTO();
        dto.setDni("12345678");
        dto.setNombreCompleto("Test Usuario");
        dto.setEmail("test@email.cl");
        dto.setEdad(30);
        dto.setIngresosMensuales(new BigDecimal("2000000"));
        dto.setDeudasActuales(new BigDecimal("-100000"));  // Deudas negativas
        dto.setMontoSolicitado(new BigDecimal("4000000"));
        dto.setMesesEnEmpleoActual(12);

        given()
            .contentType(ContentType.JSON)
            .body(dto)
        .when()
            .post("/api/v1/creditos/evaluar")
        .then()
            .statusCode(400);
    }
}