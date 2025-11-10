package pe.banco.evaluacion.validadores;

import io.quarkus.test.junit.QuarkusTest;  // Anotación que habilita testing con contexto de Quarkus completo
import org.junit.jupiter.api.Test;  // Anotación para métodos de test unitarios

import static org.junit.jupiter.api.Assertions.*;  // Métodos de asserción: assertTrue, assertFalse

/**
 * Suite de tests unitarios para el validador de DNI peruano.
 * <p>
 * Esta clase de test valida exhaustivamente el {@link ValidadorDni}, que es responsable
 * de verificar que los DNIs ingresados cumplan con el formato estándar peruano de 8 dígitos
 * numéricos. Dado que el DNI es un campo crítico para identificación única del cliente
 * y cumplimiento regulatorio (KYC - Know Your Customer), es fundamental garantizar que
 * la validación funcione correctamente en todos los casos.
 * </p>
 * <p>
 * <b>Analogía:</b> Piensa en estos tests como el "control de calidad del escáner de documentos"
 * del banco. Así como un escáner de DNI debe detectar correctamente documentos válidos vs.
 * inválidos (con manchas, incompletos, falsificados), este validador debe distinguir entre
 * DNIs con formato correcto vs. incorrecto. Los tests automatizan la verificación de que
 * el "escáner" funciona perfectamente en todas las situaciones posibles.
 * </p>
 * 
 * <h3>Estrategia de testing exhaustivo:</h3>
 * <p>
 * La suite implementa testing de "caja negra" basado en equivalence partitioning y
 * boundary value analysis:
 * <ul>
 *   <li><b>Partición válida:</b> DNIs de exactamente 8 dígitos numéricos</li>
 *   <li><b>Particiones inválidas:</b> Longitud incorrecta, caracteres no numéricos, null, vacío</li>
 *   <li><b>Valores límite:</b> 7 dígitos (justo bajo límite), 9 dígitos (justo sobre límite)</li>
 *   <li><b>Casos especiales:</b> Espacios, caracteres especiales, letras mezcladas</li>
 * </ul>
 * </p>
 * 
 * <h3>Cobertura de testing:</h3>
 * <table border="1">
 *   <tr>
 *     <th>Categoría</th>
 *     <th>Tests</th>
 *     <th>Casos cubiertos</th>
 *   </tr>
 *   <tr>
 *     <td>DNIs válidos</td>
 *     <td>1 test</td>
 *     <td>3 ejemplos diferentes</td>
 *   </tr>
 *   <tr>
 *     <td>Longitud incorrecta</td>
 *     <td>2 tests</td>
 *     <td>Menos de 8 y más de 8 dígitos</td>
 *   </tr>
 *   <tr>
 *     <td>Caracteres inválidos</td>
 *     <td>3 tests</td>
 *     <td>Letras, espacios, caracteres especiales</td>
 *   </tr>
 *   <tr>
 *     <td>Valores especiales</td>
 *     <td>2 tests</td>
 *     <td>null y string vacío</td>
 *   </tr>
 * </table>
 * 
 * <h3>¿Por qué @QuarkusTest para validador simple?</h3>
 * <p>
 * Aunque ValidadorDni no tiene dependencias y podría testearse como POJO puro,
 * usar @QuarkusTest tiene ventajas:
 * <ul>
 *   <li>Consistency: Mismo approach de testing que resto de la aplicación</li>
 *   <li>Future-proof: Si el validador necesita CDI en el futuro, tests no cambian</li>
 *   <li>Integration ready: Simula cómo Bean Validation invocará el validador</li>
 * </ul>
 * Sin embargo, podría simplificarse removiendo @QuarkusTest si performance es crítica.
 * </p>
 * 
 * <h3>Formato del DNI peruano:</h3>
 * <p>
 * El DNI (Documento Nacional de Identidad) peruano tiene estas características:
 * <ul>
 *   <li><b>Longitud:</b> Exactamente 8 caracteres</li>
 *   <li><b>Composición:</b> Solo dígitos (0-9)</li>
 *   <li><b>Sin separadores:</b> No guiones, puntos ni espacios</li>
 *   <li><b>Sin verificador:</b> A diferencia del RUT chileno, no tiene dígito verificador</li>
 * </ul>
 * Emitido por RENIEC (Registro Nacional de Identificación y Estado Civil).
 * </p>
 * 
 * <h3>Comparación con otros documentos:</h3>
 * <table border="1">
 *   <tr>
 *     <th>País</th>
 *     <th>Documento</th>
 *     <th>Formato</th>
 *     <th>Validación</th>
 *   </tr>
 *   <tr>
 *     <td>Perú</td>
 *     <td>DNI</td>
 *     <td>12345678</td>
 *     <td>Solo formato (8 dígitos)</td>
 *   </tr>
 *   <tr>
 *     <td>Chile</td>
 *     <td>RUT</td>
 *     <td>12345678-5</td>
 *     <td>Formato + dígito verificador módulo 11</td>
 *   </tr>
 *   <tr>
 *     <td>Argentina</td>
 *     <td>DNI</td>
 *     <td>12345678</td>
 *     <td>Solo formato (7-8 dígitos)</td>
 *   </tr>
 * </table>
 * 
 * <h3>Limitaciones del validador actual:</h3>
 * <p>
 * El validador implementa solo validación de formato (sintáctica), NO verifica:
 * <ul>
 *   <li>Que el DNI exista realmente en RENIEC</li>
 *   <li>Que el nombre coincida con el DNI</li>
 *   <li>Que el DNI no esté en listas negras (fraude, fallecidos)</li>
 * </ul>
 * Para validación completa en producción, considerar integración con servicios de RENIEC.
 * </p>
 * 
 * <h3>Ejecución de los tests:</h3>
 * <pre>
 * # Ejecutar todos los tests del validador
 * ./mvnw test -Dtest=ValidadorDniTest
 * 
 * # Ejecutar un test específico
 * ./mvnw test -Dtest=ValidadorDniTest#deberiaValidarDniCorrecto
 * 
 * # Ver cobertura
 * ./mvnw test jacoco:report
 * </pre>
 * 
 * <h3>Mejoras pendientes:</h3>
 * <ul>
 *   <li>Agregar test para DNI con leading zeros: "00012345"</li>
 *   <li>Test para DNI con Unicode characters: "1234567８" (8 full-width)</li>
 *   <li>Property-based testing: generar 1000 DNIs aleatorios válidos/inválidos</li>
 *   <li>Performance test: validar 10,000 DNIs en menos de 100ms</li>
 *   <li>Integration test: validar que @DniValido activa este validador correctamente</li>
 * </ul>
 * 
 * @see ValidadorDni
 * @see DniValido
 */
@QuarkusTest
class ValidadorDniTest {
    
    /**
     * Instancia del validador bajo test.
     * <p>
     * Se instancia directamente (new) porque ValidadorDni no tiene dependencias.
     * Alternativamente, podría inyectarse vía CDI si hubiera necesidad de testing
     * en contexto de framework completo.
     * </p>
     * <p>
     * <b>Nota:</b> El validador es thread-safe y stateless, por lo que una sola
     * instancia puede reutilizarse en todos los tests sin problemas.
     * </p>
     */
    private final ValidadorDni validador = new ValidadorDni();

    /**
     * Verifica que DNIs con formato correcto (8 dígitos) sean aceptados.
     * <p>
     * Este test valida el "camino feliz" (happy path) donde el input cumple
     * con todos los requisitos. Usa 3 ejemplos diferentes para aumentar confianza:
     * </p>
     * <ul>
     *   <li><b>"12345678":</b> DNI secuencial simple (caso típico de testing)</li>
     *   <li><b>"87654321":</b> DNI con dígitos en orden descendente</li>
     *   <li><b>"10203040":</b> DNI con ceros intercalados (verifica que ceros no son problema)</li>
     * </ul>
     * 
     * <h4>¿Por qué múltiples ejemplos en un test?</h4>
     * <p>
     * Aunque técnicamente todos los DNIs de 8 dígitos son equivalentes (misma partición),
     * usar múltiples ejemplos aumenta confianza y documenta que diferentes combinaciones
     * de dígitos son aceptables. Alternativamente, esto podría ser un @ParameterizedTest.
     * </p>
     * 
     * <h4>Parámetro null en isValid():</h4>
     * <p>
     * El segundo parámetro es ConstraintValidatorContext que permite customizar
     * mensajes de error. Como nuestro validador no lo usa, pasamos null en tests.
     * En producción, Bean Validation pasa contexto real automáticamente.
     * </p>
     */
    @Test
    void deberiaValidarDniCorrecto() {
        assertTrue(validador.isValid("12345678", null));
        assertTrue(validador.isValid("87654321", null));
        assertTrue(validador.isValid("10203040", null));
    }

    /**
     * Verifica que DNIs con menos de 8 dígitos sean rechazados.
     * <p>
     * Test de boundary value (valor límite): 7 dígitos está justo por debajo
     * del mínimo requerido (8), y 3 dígitos es un caso más obvio de error.
     * </p>
     * <p>
     * <b>Casos de error real que esto previene:</b>
     * <ul>
     *   <li>Usuario tipea solo 7 dígitos por error</li>
     *   <li>Copy-paste incompleto del DNI</li>
     *   <li>Pérdida de leading zero: "01234567" → "1234567"</li>
     * </ul>
     * </p>
     * 
     * <h4>Regex correspondiente:</h4>
     * <pre>
     * "1234567" NO match con ^\\d{8}$
     *           ↓
     * {8} requiere exactamente 8 dígitos, no menos
     * </pre>
     */
    @Test
    void deberiaRechazarDniConMenosDe8Digitos() {
        assertFalse(validador.isValid("1234567", null));  // 7 dígitos - boundary
        assertFalse(validador.isValid("123", null));       // 3 dígitos - caso extremo
    }

    /**
     * Verifica que DNIs con más de 8 dígitos sean rechazados.
     * <p>
     * Test de boundary value (valor límite): 9 dígitos está justo por encima
     * del máximo permitido (8), y 11 dígitos es un caso más obvio de error.
     * </p>
     * <p>
     * <b>Casos de error real que esto previene:</b>
     * <ul>
     *   <li>Usuario añade dígito extra por error</li>
     *   <li>Confusión con RUT chileno que puede tener 9 caracteres (8 + verificador)</li>
     *   <li>Pegado doble: "1234567812345678"</li>
     * </ul>
     * </p>
     * 
     * <h4>Regex correspondiente:</h4>
     * <pre>
     * "123456789" NO match con ^\\d{8}$
     *             ↓
     * {8} requiere exactamente 8 dígitos, no más
     * $ anchor garantiza fin de string después de dígito 8
     * </pre>
     */
    @Test
    void deberiaRechazarDniConMasDe8Digitos() {
        assertFalse(validador.isValid("123456789", null));    // 9 dígitos - boundary
        assertFalse(validador.isValid("12345678901", null));  // 11 dígitos - caso extremo
    }

    /**
     * Verifica que DNIs conteniendo letras sean rechazados.
     * <p>
     * DNI peruano es puramente numérico, cualquier letra lo invalida.
     * </p>
     * <p>
     * <b>Casos probados:</b>
     * <ul>
     *   <li><b>"1234567A":</b> Letra al final (confusión con RUT chileno que usa K)</li>
     *   <li><b>"ABCDEFGH":</b> Completamente alfabético</li>
     * </ul>
     * </p>
     * 
     * <h4>Casos de error real que esto previene:</h4>
     * <ul>
     *   <li>Usuario ingresa RUT chileno pensando que es DNI peruano</li>
     *   <li>Error de tipeo: tecla adyacente (8 → I en teclado)</li>
     *   <li>Input de texto arbitrario en campo de DNI</li>
     * </ul>
     * 
     * <h4>Regex correspondiente:</h4>
     * <pre>
     * "1234567A" NO match con ^\\d{8}$
     *            ↓
     * \\d solo acepta dígitos [0-9], no letras [A-Za-z]
     * </pre>
     */
    @Test
    void deberiaRechazarDniConLetras() {
        assertFalse(validador.isValid("1234567A", null));  // Letra al final
        assertFalse(validador.isValid("ABCDEFGH", null));  // Solo letras
    }

    /**
     * Verifica que string vacío sea rechazado.
     * <p>
     * String vacío ("") es diferente de null y debe manejarse explícitamente.
     * </p>
     * <p>
     * <b>Casos de error real que esto previene:</b>
     * <ul>
     *   <li>Usuario envía formulario sin llenar campo DNI</li>
     *   <li>Campo se borra después de validación inicial</li>
     *   <li>Deserialización JSON genera string vacío para campo ausente</li>
     * </ul>
     * </p>
     * 
     * <h4>Implementación en ValidadorDni:</h4>
     * <pre>
     * if (dni == null || dni.isEmpty()) {
     *     return false;  // ← Este código se ejecuta para ""
     * }
     * </pre>
     * 
     * <p>
     * <b>Nota de diseño:</b> Alternativamente, string vacío podría considerarse
     * "ausencia de valor" (como null) y retornar true, delegando validación de
     * presencia a @NotBlank. Sin embargo, es más seguro y claro rechazarlo aquí.
     * </p>
     */
    @Test
    void deberiaRechazarDniVacio() {
        assertFalse(validador.isValid("", null));
    }

    /**
     * Verifica que null sea rechazado.
     * <p>
     * Este comportamiento puede parecer contraintuitivo dado que la documentación
     * de la clase dice "null retorna true", pero la implementación actual retorna false.
     * </p>
     * 
     * <h4>Análisis del comportamiento actual:</h4>
     * <pre>
     * // Código en ValidadorDni.isValid():
     * if (dni == null || dni.isEmpty()) {
     *     return false;  // ← Rechaza null
     * }
     * </pre>
     * 
     * <p>
     * <b>⚠️ INCONSISTENCIA DETECTADA:</b> La documentación de ValidadorDni dice
     * que null debería retornar true (delegando validación de presencia a @NotNull),
     * pero la implementación retorna false. Este test documenta el comportamiento
     * ACTUAL, no el esperado según la documentación.
     * </p>
     * 
     * <h4>Recomendación para resolver inconsistencia:</h4>
     * <p>
     * Opción 1 - Cambiar implementación para aceptar null:
     * <pre>
     * public boolean isValid(String dni, ConstraintValidatorContext context) {
     *     if (dni == null) {
     *         return true;  // Delegar validación de presencia a @NotNull
     *     }
     *     if (dni.isEmpty()) {
     *         return false;
     *     }
     *     return dni.matches("^\\d{8}$");
     * }
     * </pre>
     * 
     * Opción 2 - Actualizar documentación para reflejar que null es rechazado.
     * </p>
     * 
     * <p>
     * <b>Ventajas de aceptar null:</b>
     * <ul>
     *   <li>Separación de responsabilidades: @NotNull valida presencia, @DniValido valida formato</li>
     *   <li>Flexibilidad: campos opcionales pueden usar solo @DniValido sin @NotNull</li>
     *   <li>Consistencia con Bean Validation best practices</li>
     * </ul>
     * </p>
     * 
     * <p>
     * <b>Ventajas de rechazar null:</b>
     * <ul>
     *   <li>Fail-safe: siempre rechaza valores problemáticos</li>
     *   <li>Menos confusión para desarrolladores nuevos</li>
     *   <li>No depende de recordar usar @NotNull en combinación</li>
     * </ul>
     * </p>
     */
    @Test
    void deberiaRechazarDniNulo() {
        assertFalse(validador.isValid(null, null));
    }

    /**
     * Verifica que DNIs con espacios sean rechazados.
     * <p>
     * Espacios pueden aparecer por:
     * <ul>
     *   <li>Copy-paste de PDF con formato visual: "1234 5678"</li>
     *   <li>Usuario ingresa espacio por costumbre (como en números telefónicos)</li>
     *   <li>Leading/trailing whitespace: " 12345678" o "12345678 "</li>
     * </ul>
     * </p>
     * 
     * <h4>Casos probados:</h4>
     * <ul>
     *   <li><b>"1234 5678":</b> Espacio en medio (formato visual común)</li>
     *   <li><b>" 12345678":</b> Espacio al inicio (whitespace no trimmeado)</li>
     * </ul>
     * 
     * <h4>Regex correspondiente:</h4>
     * <pre>
     * "1234 5678" NO match con ^\\d{8}$
     *             ↓
     * \\d no acepta espacios, solo dígitos [0-9]
     * El espacio interrumpe la secuencia de 8 dígitos consecutivos
     * </pre>
     * 
     * <p>
     * <b>Consideración de UX:</b> En una aplicación real, podrías considerar
     * normalizar el input con trim() y replace(" ", "") ANTES de validar,
     * mejorando experiencia de usuario sin comprometer seguridad.
     * </p>
     * 
     * <h4>Ejemplo de pre-procesamiento:</h4>
     * <pre>
     * // En el DTO o setter
     * public void setDni(String dni) {
     *     this.dni = dni != null ? dni.trim().replaceAll("\\s+", "") : null;
     * }
     * </pre>
     */
    @Test
    void deberiaRechazarDniConEspacios() {
        assertFalse(validador.isValid("1234 5678", null));  // Espacio en medio
        assertFalse(validador.isValid(" 12345678", null));  // Leading space
    }

    /**
     * Verifica que DNIs con caracteres especiales sean rechazados.
     * <p>
     * Caracteres especiales pueden aparecer por:
     * <ul>
     *   <li>Copy-paste de documentos con formato visual: "12.345.678"</li>
     *   <li>Confusión con formatos de otros documentos: "12345-678"</li>
     *   <li>Intentos de inyección o exploits</li>
     * </ul>
     * </p>
     * 
     * <h4>Casos probados:</h4>
     * <ul>
     *   <li><b>"12345-678":</b> Guion (común en RUT chileno, no en DNI peruano)</li>
     *   <li><b>"12.345.678":</b> Puntos (formato visual en algunos países)</li>
     * </ul>
     * 
     * <h4>Regex correspondiente:</h4>
     * <pre>
     * "12345-678" NO match con ^\\d{8}$
     *             ↓
     * \\d solo acepta [0-9], no '-' ni '.'
     * Los separadores interrumpen la secuencia de dígitos
     * </pre>
     * 
     * <p>
     * <b>Seguridad:</b> Rechazar caracteres especiales previene:
     * <ul>
     *   <li>SQL injection: "'; DROP TABLE--"</li>
     *   <li>XSS: "&lt;script&gt;alert('xss')&lt;/script&gt;"</li>
     *   <li>Path traversal: "../../../etc/passwd"</li>
     * </ul>
     * Aunque estos ataques serían neutralizados por otras capas (prepared statements,
     * HTML encoding), es buena práctica validar estrictamente en entrada.
     * </p>
     * 
     * <p>
     * <b>Mejora potencial:</b> Agregar tests para más caracteres especiales:
     * <pre>
     * assertFalse(validador.isValid("12345678;", null));  // Punto y coma
     * assertFalse(validador.isValid("12345678'", null));  // Comilla simple
     * assertFalse(validador.isValid("12345678<", null));  // Menor que
     * </pre>
     * </p>
     */
    @Test
    void deberiaRechazarDniConCaracteresEspeciales() {
        assertFalse(validador.isValid("12345-678", null));   // Guion
        assertFalse(validador.isValid("12.345.678", null));  // Puntos
    }
}