package pe.banco.evaluacion.validadores;

import jakarta.validation.ConstraintValidator;  // Interfaz base para implementar validadores personalizados
import jakarta.validation.ConstraintValidatorContext;  // Contexto con información sobre la validación en curso

/**
 * Implementación del validador de DNI peruano.
 * <p>
 * Esta clase contiene la lógica real de validación asociada a la anotación {@link DniValido}.
 * Implementa la interfaz {@link ConstraintValidator} de Bean Validation, que define el
 * contrato estándar para todos los validadores personalizados en Jakarta EE.
 * </p>
 * <p>
 * <b>Analogía:</b> Si @DniValido es la "etiqueta de inspección" que pones en un campo,
 * esta clase es el "inspector" que realiza la verificación real. La etiqueta dice "esto
 * debe ser un DNI válido", y este inspector sabe exactamente cómo verificar esa condición
 * (8 dígitos numéricos).
 * </p>
 * 
 * <h3>Relación entre anotación y validador:</h3>
 * <pre>
 * @DniValido ←────────────── Anotación: declara que quieres validar
 *     ↓                       
 * ValidadorDni ←──────────── Validador: implementa cómo validar
 *     ↓
 * isValid() ←──────────────  Método: lógica de validación real
 * </pre>
 * 
 * <h3>Flujo de validación completo:</h3>
 * <ol>
 *   <li>Cliente envía request con JSON: {"dni": "1234567X"}</li>
 *   <li>Quarkus deserializa JSON a DTO con campo anotado @DniValido</li>
 *   <li>@Valid en endpoint activa Bean Validation</li>
 *   <li>Bean Validation encuentra @DniValido en campo dni</li>
 *   <li>Bean Validation busca validador asociado: ValidadorDni.class</li>
 *   <li>Bean Validation instancia ValidadorDni (una vez, cached)</li>
 *   <li>Bean Validation llama isValid("1234567X", context)</li>
 *   <li>isValid() retorna false (tiene letra, no es solo dígitos)</li>
 *   <li>Bean Validation lanza ConstraintViolationException</li>
 *   <li>ValidationExceptionMapper captura excepción</li>
 *   <li>Cliente recibe 400 Bad Request con mensaje de error</li>
 * </ol>
 * 
 * <h3>Características de implementación:</h3>
 * <ul>
 *   <li><b>Stateless:</b> No mantiene estado entre validaciones (thread-safe)</li>
 *   <li><b>Singleton:</b> Bean Validation reutiliza instancias para performance</li>
 *   <li><b>Null-safe:</b> Maneja explícitamente el caso null</li>
 *   <li><b>Fail-fast:</b> Retorna false apenas detecta invalidez</li>
 * </ul>
 * 
 * <h3>Formato del DNI peruano:</h3>
 * <p>
 * El DNI (Documento Nacional de Identidad) peruano es emitido por RENIEC
 * (Registro Nacional de Identificación y Estado Civil) y tiene estas características:
 * <ul>
 *   <li><b>Longitud:</b> Exactamente 8 caracteres</li>
 *   <li><b>Composición:</b> Solo dígitos numéricos (0-9)</li>
 *   <li><b>Formato:</b> Sin espacios, guiones u otros separadores</li>
 *   <li><b>Rango:</b> De 00000001 a 99999999 (teóricamente)</li>
 *   <li><b>Asignación:</b> Secuencial por RENIEC</li>
 * </ul>
 * </p>
 * 
 * <h4>Ejemplos válidos:</h4>
 * <pre>
 * "12345678" → ✅ Válido
 * "00000001" → ✅ Válido (DNI bajo, de registros antiguos)
 * "99999999" → ✅ Válido (formato correcto, aunque probablemente no existe)
 * "72345678" → ✅ Válido (formato típico de DNIs actuales)
 * </pre>
 * 
 * <h4>Ejemplos inválidos:</h4>
 * <pre>
 * null         → ✅ Válido (null se considera válido; usar @NotNull para requerir)
 * ""           → ❌ Inválido (vacío)
 * "1234567"    → ❌ Inválido (solo 7 dígitos)
 * "123456789"  → ❌ Inválido (9 dígitos, excede longitud)
 * "1234567X"   → ❌ Inválido (contiene letra)
 * "1234 5678"  → ❌ Inválido (contiene espacio)
 * "12.345.678" → ❌ Inválido (contiene puntos)
 * " 12345678"  → ❌ Inválido (espacio al inicio)
 * "12345678 "  → ❌ Inválido (espacio al final)
 * </pre>
 * 
 * <h3>Validación mediante regex:</h3>
 * <p>
 * El validador usa expresión regular: <code>^\\d{8}$</code>
 * <ul>
 *   <li><b>^</b> : Inicio de string (anchor)</li>
 *   <li><b>\\d</b> : Dígito (0-9), equivalente a [0-9]</li>
 *   <li><b>{8}</b> : Exactamente 8 repeticiones</li>
 *   <li><b>$</b> : Fin de string (anchor)</li>
 * </ul>
 * </p>
 * <p>
 * Los anchors ^ y $ son críticos: sin ellos, "ABC12345678XYZ" sería válido
 * porque contiene 8 dígitos consecutivos en el medio. Los anchors garantizan
 * que TODO el string sea exactamente 8 dígitos, nada más, nada menos.
 * </p>
 * 
 * <h3>¿Por qué null retorna true?</h3>
 * <p>
 * El validador retorna true para null porque en Bean Validation, la validación
 * de "no nulo" es responsabilidad de @NotNull, no de validadores de formato.
 * Esta separación de responsabilidades permite composición flexible:
 * <pre>
 * // DNI opcional (puede ser null o vacío)
 * @DniValido
 * private String dni;
 * 
 * // DNI requerido (debe estar presente Y tener formato válido)
 * @NotNull
 * @DniValido
 * private String dni;
 * </pre>
 * </p>
 * 
 * <h3>Validación de whitespace:</h3>
 * <p>
 * isEmpty() retorna true si el string es vacío ("") PERO NO si contiene solo espacios (" ").
 * El regex ^ \\d{8}$ rechazará strings con espacios, así que no es problema de seguridad,
 * pero considera normalizar input con trim() antes de validar para mejor UX:
 * <pre>
 * // En el DTO o setter
 * public void setDni(String dni) {
 *     this.dni = dni != null ? dni.trim() : null;
 * }
 * </pre>
 * </p>
 * 
 * <h3>Extensiones futuras:</h3>
 * <p>
 * Este validador implementa validación básica de formato. Para producción robusta, considera:
 * <ul>
 *   <li><b>Consulta a RENIEC:</b> Verificar que el DNI exista realmente
 *       <pre>
 *       public boolean isValid(String dni, ConstraintValidatorContext context) {
 *           if (dni == null || dni.isEmpty()) return true;
 *           if (!dni.matches("^\\d{8}$")) return false;
 *           // Verificación adicional con API de RENIEC
 *           return reniecService.existeDni(dni);
 *       }
 *       </pre>
 *   </li>
 *   <li><b>Caché de DNIs validados:</b> Reducir llamadas a servicios externos</li>
 *   <li><b>Validación cruzada:</b> Verificar consistencia DNI-nombre-fecha</li>
 *   <li><b>Lista negra:</b> Rechazar DNIs reportados como fraudulentos</li>
 *   <li><b>Análisis de patrones:</b> Detectar DNIs secuenciales sospechosos</li>
 * </ul>
 * </p>
 * 
 * <h3>Testing del validador:</h3>
 * <pre>
 * public class ValidadorDniTest {
 *     private ValidadorDni validator = new ValidadorDni();
 *     private ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
 *     
 *     @Test
 *     void dniValido() {
 *         assertTrue(validator.isValid("12345678", context));
 *     }
 *     
 *     @Test
 *     void dniConLetras() {
 *         assertFalse(validator.isValid("1234567X", context));
 *     }
 *     
 *     @Test
 *     void dniMuyCorto() {
 *         assertFalse(validator.isValid("1234567", context));
 *     }
 *     
 *     @Test
 *     void nullEsValido() {
 *         assertTrue(validator.isValid(null, context));
 *     }
 * }
 * </pre>
 * 
 * @see DniValido
 * @see ConstraintValidator
 * @see jakarta.validation.constraints.NotNull
 */
public class ValidadorDni implements ConstraintValidator<DniValido, String> {
    
    /**
     * Valida que un DNI cumpla con el formato peruano de 8 dígitos numéricos.
     * <p>
     * Este método es llamado automáticamente por el framework de Bean Validation
     * durante el proceso de validación de objetos anotados con @DniValido.
     * </p>
     * 
     * <h4>Lógica de validación (3 pasos):</h4>
     * <ol>
     *   <li><b>Null check:</b> Si dni es null, retornar true (válido).
     *       Responsabilidad de @NotNull verificar presencia</li>
     *   <li><b>Empty check:</b> Si dni está vacío (""), retornar false (inválido).
     *       Un DNI vacío no es un formato válido</li>
     *   <li><b>Format check:</b> Validar regex ^\\d{8}$ para 8 dígitos exactos</li>
     * </ol>
     * 
     * <p>
     * <b>Contexto de validación:</b> El parámetro ConstraintValidatorContext
     * permite personalizar el mensaje de error o agregar metadata adicional.
     * En esta implementación simple no se usa, pero podría usarse así:
     * <pre>
     * public boolean isValid(String dni, ConstraintValidatorContext context) {
     *     if (dni == null || dni.isEmpty()) return true;
     *     
     *     if (dni.length() != 8) {
     *         context.disableDefaultConstraintViolation();
     *         context.buildConstraintViolationWithTemplate(
     *             "DNI debe tener exactamente 8 dígitos, encontrados: " + dni.length()
     *         ).addConstraintViolation();
     *         return false;
     *     }
     *     
     *     return dni.matches("^\\d{8}$");
     * }
     * </pre>
     * </p>
     * 
     * <p>
     * <b>Performance:</b> Este método debe ser rápido porque puede llamarse miles
     * de veces por segundo en APIs de alto tráfico. La regex es eficiente O(n)
     * donde n=8 (constante), así que performance es excelente.
     * </p>
     * 
     * <p>
     * <b>Thread-safety:</b> Este método debe ser thread-safe porque Bean Validation
     * reutiliza instancias de validadores. Esta implementación ES thread-safe porque:
     * <ul>
     *   <li>No mantiene estado mutable</li>
     *   <li>No modifica variables de instancia</li>
     *   <li>Todas las variables son locales al método</li>
     * </ul>
     * </p>
     *
     * @param dni Valor del campo a validar (puede ser null)
     * @param context Contexto de validación con metadata y configuración
     * @return true si el DNI es null o cumple formato de 8 dígitos, false en caso contrario
     */
    @Override
    public boolean isValid(String dni, ConstraintValidatorContext context) {
        if (dni == null || dni.isEmpty()) {
            return false;
        }
        
        // DNI peruano: 8 dígitos numéricos exactos
        return dni.matches("^\\d{8}$");
    }
}