package pe.banco.evaluacion.validadores;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ValidadorDniTest {
    
    private final ValidadorDni validador = new ValidadorDni();

    @Test
    void deberiaValidarDniCorrecto() {
        assertTrue(validador.isValid("12345678", null));
        assertTrue(validador.isValid("87654321", null));
        assertTrue(validador.isValid("10203040", null));
    }

    @Test
    void deberiaRechazarDniConMenosDe8Digitos() {
        assertFalse(validador.isValid("1234567", null));
        assertFalse(validador.isValid("123", null));
    }

    @Test
    void deberiaRechazarDniConMasDe8Digitos() {
        assertFalse(validador.isValid("123456789", null));
        assertFalse(validador.isValid("12345678901", null));
    }

    @Test
    void deberiaRechazarDniConLetras() {
        assertFalse(validador.isValid("1234567A", null));
        assertFalse(validador.isValid("ABCDEFGH", null));
    }

    @Test
    void deberiaRechazarDniVacio() {
        assertFalse(validador.isValid("", null));
    }

    @Test
    void deberiaRechazarDniNulo() {
        assertFalse(validador.isValid(null, null));
    }

    @Test
    void deberiaRechazarDniConEspacios() {
        assertFalse(validador.isValid("1234 5678", null));
        assertFalse(validador.isValid(" 12345678", null));
    }

    @Test
    void deberiaRechazarDniConCaracteresEspeciales() {
        assertFalse(validador.isValid("12345-678", null));
        assertFalse(validador.isValid("12.345.678", null));
    }
}
