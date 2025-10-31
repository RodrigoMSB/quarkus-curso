package pe.banco.customer;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import pe.banco.customer.dto.CustomerRequest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

/**
 * Tests de integración para Customer Service
 * 
 * Demuestra:
 * - @QuarkusTest (Capítulo 5)
 * - Dev Services (PostgreSQL automático)
 * - REST Assured para testing de APIs
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CustomerResourceTest {

    private static Long createdCustomerId;

    @Test
    @Order(1)
    @DisplayName("Debe crear un nuevo cliente")
    void testCreateCustomer() {
        CustomerRequest request = new CustomerRequest(
            "20123456789", // RUC válido (11 dígitos)
            "Tech Solutions S.A.C.",
            "TechSol",
            "TECHNOLOGY",
            LocalDate.of(2015, 5, 20),
            new BigDecimal("5000000.00"),
            "contacto@techsol.pe",
            "+51987654321",
            "Av. Tecnología 123",
            "Lima"
        );

        createdCustomerId = given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .post("/api/customers")
            .then()
            .statusCode(201)
            .body("legalName", equalTo("Tech Solutions S.A.C."))
            .body("industry", equalTo("TECHNOLOGY"))
            .body("rucMasked", equalTo("XXXXXXXXX89")) // RUC enmascarado
            .body("sunatValidated", notNullValue())
            .extract()
            .path("id");

        System.out.println("✅ Cliente creado con ID: " + createdCustomerId);
    }

    @Test
    @Order(2)
    @DisplayName("Debe obtener un cliente por ID")
    void testGetCustomer() {
        given()
            .pathParam("id", createdCustomerId)
            .when()
            .get("/api/customers/{id}")
            .then()
            .statusCode(200)
            .body("id", equalTo(createdCustomerId.intValue()))
            .body("legalName", equalTo("Tech Solutions S.A.C."))
            .body("rucMasked", startsWith("XXXXXXXXX"));
    }

    @Test
    @Order(3)
    @DisplayName("Debe actualizar un cliente")
    void testUpdateCustomer() {
        CustomerRequest updateRequest = new CustomerRequest(
            "20123456789",
            "Tech Solutions S.A.C.", // Mismo nombre
            "TechSol Pro", // Nombre comercial actualizado
            "TECHNOLOGY",
            LocalDate.of(2015, 5, 20),
            new BigDecimal("8000000.00"), // Ingresos actualizados
            "nuevo@techsol.pe", // Email actualizado
            "+51987654322",
            "Av. Tecnología 456", // Dirección actualizada
            "Lima"
        );

        given()
            .contentType(ContentType.JSON)
            .pathParam("id", createdCustomerId)
            .body(updateRequest)
            .when()
            .put("/api/customers/{id}")
            .then()
            .statusCode(200)
            .body("tradeName", equalTo("TechSol Pro"))
            .body("contactEmail", equalTo("nuevo@techsol.pe"))
            .body("annualRevenue", equalTo(8000000.00f));
    }

    @Test
    @Order(4)
    @DisplayName("Debe listar clientes activos")
    void testListActiveCustomers() {
        given()
            .when()
            .get("/api/customers")
            .then()
            .statusCode(200)
            .body("$.size()", greaterThan(0));
    }

    @Test
    @Order(5)
    @DisplayName("Debe buscar clientes por industria")
    void testListByIndustry() {
        given()
            .pathParam("industry", "TECHNOLOGY")
            .when()
            .get("/api/customers/industry/{industry}")
            .then()
            .statusCode(200)
            .body("$.size()", greaterThan(0))
            .body("[0].industry", equalTo("TECHNOLOGY"));
    }

    @Test
    @DisplayName("Debe retornar 404 al buscar cliente inexistente")
    void testGetNonExistentCustomer() {
        given()
            .pathParam("id", 99999)
            .when()
            .get("/api/customers/{id}")
            .then()
            .statusCode(500); // IllegalArgumentException capturado
    }

    @Test
    @DisplayName("Debe validar RUC con formato incorrecto")
    void testInvalidRucFormat() {
        CustomerRequest invalidRequest = new CustomerRequest(
            "123", // RUC inválido (menos de 11 dígitos)
            "Test Company",
            null,
            "RETAIL",
            LocalDate.now(),
            new BigDecimal("1000000"),
            "test@test.com",
            "+51999999999",
            "Test Address",
            "Lima"
        );

        given()
            .contentType(ContentType.JSON)
            .body(invalidRequest)
            .when()
            .post("/api/customers")
            .then()
            .statusCode(400); // Validation error
    }

    @Test
    @DisplayName("Debe validar email con formato incorrecto")
    void testInvalidEmail() {
        CustomerRequest invalidRequest = new CustomerRequest(
            "20999999999",
            "Test Company",
            null,
            "RETAIL",
            LocalDate.now(),
            new BigDecimal("1000000"),
            "email-invalido", // Email sin formato correcto
            "+51999999999",
            "Test Address",
            "Lima"
        );

        given()
            .contentType(ContentType.JSON)
            .body(invalidRequest)
            .when()
            .post("/api/customers")
            .then()
            .statusCode(400); // Validation error
    }

    @Test
    @DisplayName("Health check debe responder OK")
    void testHealthCheck() {
        given()
            .when()
            .get("/api/customers/health")
            .then()
            .statusCode(200)
            .body(equalTo("Customer Service is UP"));
    }
}
