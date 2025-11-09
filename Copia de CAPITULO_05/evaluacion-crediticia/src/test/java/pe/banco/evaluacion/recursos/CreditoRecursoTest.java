package pe.banco.evaluacion.recursos;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import pe.banco.evaluacion.dtos.SolicitudCreditoDTO;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class CreditoRecursoTest {

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

    @Test
    void deberiaEvaluarSolicitudYRechazar() {
        SolicitudCreditoDTO dto = new SolicitudCreditoDTO();
        dto.setDni("23456789");
        dto.setNombreCompleto("María Silva Test");
        dto.setEmail("maria.test@email.cl");
        dto.setEdad(25);
        dto.setIngresosMensuales(new BigDecimal("1000000"));
        dto.setDeudasActuales(new BigDecimal("700000"));
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

    @Test
    void deberiaRechazarCamposRequeridos() {
        SolicitudCreditoDTO dto = new SolicitudCreditoDTO();

        given()
            .contentType(ContentType.JSON)
            .body(dto)
        .when()
                    .post("/api/v1/creditos/evaluar")
        .then()
            .statusCode(400);
    }

    @Test
    void deberiaRechazarEmailInvalido() {
        SolicitudCreditoDTO dto = new SolicitudCreditoDTO();
        dto.setDni("12345678");
        dto.setNombreCompleto("Test Usuario");
        dto.setEmail("email-invalido");
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

    @Test
    void deberiaRechazarEdadMenorA18() {
        SolicitudCreditoDTO dto = new SolicitudCreditoDTO();
        dto.setDni("12345678");
        dto.setNombreCompleto("Menor Edad");
        dto.setEmail("menor@email.cl");
        dto.setEdad(17);
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

    @Test
    void deberiaRechazarMontoFueraDeRango() {
        SolicitudCreditoDTO dto = new SolicitudCreditoDTO();
        dto.setDni("12345678");
        dto.setNombreCompleto("Test Usuario");
        dto.setEmail("test@email.cl");
        dto.setEdad(30);
        dto.setIngresosMensuales(new BigDecimal("2000000"));
        dto.setDeudasActuales(new BigDecimal("300000"));
        dto.setMontoSolicitado(new BigDecimal("60000000"));
        dto.setMesesEnEmpleoActual(12);

        given()
            .contentType(ContentType.JSON)
            .body(dto)
        .when()
                    .post("/api/v1/creditos/evaluar")
        .then()
            .statusCode(400);
    }

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

    @Test
    void deberiaRetornar404SiSolicitudNoExiste() {
        given()
        .when()
            .get("/api/v1/creditos/99999")
        .then()
            .statusCode(404);
    }

    @Test
    void deberiaListarTodasLasSolicitudes() {
        given()
        .when()
            .get("/api/v1/creditos")
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(5));
    }

    @Test
    void deberiaValidarIngresosPositivos() {
        SolicitudCreditoDTO dto = new SolicitudCreditoDTO();
        dto.setDni("12345678");
        dto.setNombreCompleto("Test Usuario");
        dto.setEmail("test@email.cl");
        dto.setEdad(30);
        dto.setIngresosMensuales(new BigDecimal("0"));
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

    @Test
    void deberiaValidarDeudasNoNegativas() {
        SolicitudCreditoDTO dto = new SolicitudCreditoDTO();
        dto.setDni("12345678");
        dto.setNombreCompleto("Test Usuario");
        dto.setEmail("test@email.cl");
        dto.setEdad(30);
        dto.setIngresosMensuales(new BigDecimal("2000000"));
        dto.setDeudasActuales(new BigDecimal("-100000"));
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
