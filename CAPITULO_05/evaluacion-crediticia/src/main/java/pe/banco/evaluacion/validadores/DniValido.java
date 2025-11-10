package pe.banco.evaluacion.validadores;

import jakarta.validation.Constraint;  // Meta-anotación que marca esta anotación como constraint de validación
import jakarta.validation.Payload;  // Interfaz marcadora para metadata adicional en validaciones
import java.lang.annotation.*;  // Clases para definir anotaciones personalizadas

/**
 * Anotación de validación personalizada para DNI peruano.
 * <p>
 * Esta anotación implementa un constraint de Bean Validation (JSR 380) específico
 * para validar el formato del Documento Nacional de Identidad usado en Perú.
 * Permite aplicar validación de DNI de forma declarativa en DTOs y entidades,
 * integrándose perfectamente con el framework de validación estándar de Jakarta EE.
 * </p>
 * <p>
 * <b>Analogía:</b> Piensa en esta anotación como un "sello de calidad" que pones sobre
 * un campo. Así como un inspector de calidad marca productos que cumplen estándares,
 * esta anotación marca campos que deben cumplir el estándar de DNI peruano (8 dígitos).
 * El framework de validación es el "inspector" que verifica automáticamente ese estándar.
 * </p>
 * 
 * <h3>Características del DNI peruano:</h3>
 * <ul>
 *   <li><b>Longitud fija:</b> Exactamente 8 dígitos numéricos</li>
 *   <li><b>Sin letras:</b> Solo caracteres 0-9 permitidos</li>
 *   <li><b>Sin espacios ni separadores:</b> Formato compacto (ej: 12345678)</li>
 *   <li><b>Único por persona:</b> Identificador único asignado por RENIEC</li>
 *   <li><b>Vitalicio:</b> No cambia durante la vida de la persona</li>
 * </ul>
 * 
 * <h3>Comparación con otros documentos latinoamericanos:</h3>
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
 *     <td>8 dígitos</td>
 *     <td>Solo formato (sin dígito verificador)</td>
 *   </tr>
 *   <tr>
 *     <td>Chile</td>
 *     <td>RUT</td>
 *     <td>7-8 dígitos + verificador</td>
 *     <td>Algoritmo módulo 11</td>
 *   </tr>
 *   <tr>
 *     <td>Colombia</td>
 *     <td>Cédula</td>
 *     <td>8-10 dígitos</td>
 *     <td>Solo formato</td>
 *   </tr>
 *   <tr>
 *     <td>Argentina</td>
 *     <td>DNI</td>
 *     <td>7-8 dígitos</td>
 *     <td>Solo formato</td>
 *   </tr>
 * </table>
 * 
 * <h3>Uso de la anotación:</h3>
 * <pre>
 * // En un DTO
 * public class SolicitudCreditoDTO {
 *     @DniValido  // ← Aplicación de la anotación
 *     @NotBlank
 *     private String dni;
 * }
 * 
 * // En una entidad
 * @Entity
 * public class Cliente {
 *     @DniValido  // ← Validación automática antes de persistir
 *     @Column(length = 8)
 *     private String dni;
 * }
 * </pre>
 * 
 * <h3>Validación automática en endpoints REST:</h3>
 * <pre>
 * @POST
 * @Path("/solicitudes")
 * public Response crear(@Valid SolicitudCreditoDTO dto) {
 *     // Si dto.dni no cumple @DniValido, se lanza ConstraintViolationException
 *     // automáticamente ANTES de entrar a este método
 *     // El ValidationExceptionMapper la captura y retorna 400 Bad Request
 * }
 * </pre>
 * 
 * <h3>Componentes de un constraint de Bean Validation:</h3>
 * <ul>
 *   <li><b>Anotación (esta clase):</b> Define dónde y cómo aplicar la validación</li>
 *   <li><b>Validator (ValidadorDni):</b> Implementa la lógica de validación real</li>
 *   <li><b>Mensaje:</b> Texto descriptivo cuando falla la validación</li>
 *   <li><b>Grupos:</b> Permite validaciones condicionales por contexto</li>
 *   <li><b>Payload:</b> Metadata adicional para casos avanzados</li>
 * </ul>
 * 
 * <h3>¿Por qué crear constraint personalizado vs. @Pattern?</h3>
 * <p>
 * Podrías usar <code>@Pattern(regexp = "^\\d{8}$")</code>, pero crear @DniValido es mejor porque:
 * <ul>
 *   <li><b>Semántica:</b> @DniValido es más expresivo que regex críptico</li>
 *   <li><b>Reutilización:</b> Validación centralizada, cambias en un lugar</li>
 *   <li><b>Extensibilidad:</b> Fácil agregar lógica adicional (ej: consultar RENIEC API)</li>
 *   <li><b>Mensajes:</b> Mensaje de error específico y contextualizado</li>
 *   <li><b>Testing:</b> Validador testeable independientemente</li>
 * </ul>
 * </p>
 * 
 * <h3>Meta-anotaciones explicadas:</h3>
 * <ul>
 *   <li><b>@Target:</b> Define dónde puede usarse (campos, parámetros)</li>
 *   <li><b>@Retention:</b> Define cuándo está disponible (runtime para reflection)</li>
 *   <li><b>@Constraint:</b> Vincula esta anotación con su validador</li>
 *   <li><b>@Documented:</b> Incluye anotación en JavaDoc generado</li>
 * </ul>
 * 
 * <h3>Limitaciones del validador actual:</h3>
 * <p>
 * El validador actual solo verifica formato (8 dígitos), NO verifica:
 * <ul>
 *   <li>Que el DNI realmente exista en RENIEC</li>
 *   <li>Que pertenezca a la persona que dice ser</li>
 *   <li>Que no esté en listas de fraude</li>
 * </ul>
 * Para validación exhaustiva en producción, considera integrar con servicios como:
 * <ul>
 *   <li>API de RENIEC (consulta oficial, requiere convenio)</li>
 *   <li>Servicios de verificación de identidad (INFOCORP, Equifax)</li>
 *   <li>Validación biométrica (huella, reconocimiento facial)</li>
 * </ul>
 * </p>
 * 
 * <h3>Evolución futura del validador:</h3>
 * <pre>
 * // Fase 1: Solo formato (actual)
 * @DniValido
 * private String dni;
 * 
 * // Fase 2: Consulta a RENIEC (futuro)
 * @DniValido(verificarConReniec = true)
 * private String dni;
 * 
 * // Fase 3: Validación con datos adicionales (futuro)
 * @DniValido(nombres = "Juan Pérez", fechaNacimiento = "1990-01-01")
 * private String dni;
 * </pre>
 * 
 * @see ValidadorDni
 * @see jakarta.validation.Constraint
 * @see pe.banco.evaluacion.dtos.SolicitudCreditoDTO
 * @see pe.banco.evaluacion.entidades.SolicitudCredito
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})  // Aplicable a campos de clase y parámetros de método
@Retention(RetentionPolicy.RUNTIME)  // Disponible en runtime para inspection vía reflection
@Constraint(validatedBy = ValidadorDni.class)  // Vincula esta anotación con su validador
@Documented  // Incluye en JavaDoc generado para documentación API
public @interface DniValido {
    
    /**
     * Mensaje de error mostrado cuando la validación falla.
     * <p>
     * Este mensaje se retorna al cliente en la respuesta 400 Bad Request cuando
     * el DNI no cumple con el formato esperado. Puede ser:
     * <ul>
     *   <li><b>Literal:</b> "DNI inválido. Debe contener 8 dígitos" (por defecto)</li>
     *   <li><b>Clave de mensaje:</b> "{pe.banco.validacion.dni.invalido}" (internacionalización)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Ejemplo de override del mensaje:</b>
     * <pre>
     * @DniValido(message = "El DNI proporcionado no tiene formato válido")
     * private String dni;
     * </pre>
     * </p>
     * <p>
     * <b>Internacionalización (i18n):</b> Para soportar múltiples idiomas, crea
     * archivo ValidationMessages.properties:
     * <pre>
     * # ValidationMessages_es.properties
     * pe.banco.validacion.dni.invalido=DNI inválido. Debe contener 8 dígitos
     * 
     * # ValidationMessages_en.properties
     * pe.banco.validacion.dni.invalido=Invalid DNI. Must contain 8 digits
     * </pre>
     * Luego usa:
     * <pre>
     * @DniValido(message = "{pe.banco.validacion.dni.invalido}")
     * </pre>
     * </p>
     * <p>
     * <b>Buenas prácticas para mensajes:</b>
     * <ul>
     *   <li>Ser específico: no solo "inválido", sino por qué (longitud, formato)</li>
     *   <li>Ser constructivo: indicar cómo corregir el error</li>
     *   <li>Evitar jerga técnica: el usuario final no sabe qué es "regex" o "constraint"</li>
     *   <li>Mantener tono profesional pero amigable</li>
     * </ul>
     * </p>
     *
     * @return Mensaje de error de validación
     */
    String message() default "DNI inválido. Debe contener 8 dígitos";
    
    /**
     * Grupos de validación para validación condicional.
     * <p>
     * Los grupos permiten aplicar diferentes conjuntos de validaciones según el contexto.
     * Por ejemplo, validaciones diferentes para "crear usuario" vs. "actualizar usuario".
     * </p>
     * <p>
     * <b>Concepto:</b> Imagina que tienes un formulario con 20 campos. En algunos flujos
     * necesitas validar los 20, en otros solo 5. Los grupos te permiten organizar
     * validaciones en "sets" que activas según necesidad.
     * </p>
     * 
     * <h4>Ejemplo de uso de grupos:</h4>
     * <pre>
     * // Definir grupos
     * public interface ValidacionBasica {}
     * public interface ValidacionCompleta extends ValidacionBasica {}
     * 
     * // Aplicar grupos en DTO
     * public class ClienteDTO {
     *     @DniValido(groups = ValidacionBasica.class)  // Siempre validar
     *     private String dni;
     *     
     *     @NotNull(groups = ValidacionCompleta.class)  // Solo en flujo completo
     *     private String direccion;
     * }
     * 
     * // Activar grupos en endpoint
     * @POST
     * @Path("/registro-rapido")
     * public Response registroRapido(@Valid(ValidacionBasica.class) ClienteDTO dto) {
     *     // Solo valida DNI, no direccion
     * }
     * 
     * @POST
     * @Path("/registro-completo")
     * public Response registroCompleto(@Valid(ValidacionCompleta.class) ClienteDTO dto) {
     *     // Valida DNI Y direccion (porque ValidacionCompleta extiende ValidacionBasica)
     * }
     * </pre>
     * 
     * <p>
     * <b>Grupos por defecto:</b> Al no especificar grupos (como en este caso),
     * la validación se aplica al grupo Default.class, que es el grupo usado cuando
     * no se especifica ninguno explícitamente.
     * </p>
     * 
     * <p>
     * <b>Cuándo usar grupos:</b>
     * <ul>
     *   <li>Flujos de registro multi-paso (wizard)</li>
     *   <li>APIs con diferentes niveles de validación (básica, estándar, estricta)</li>
     *   <li>Misma entidad usada en contextos diferentes (crear, actualizar, validar)</li>
     *   <li>Validaciones que solo aplican a ciertos roles (admin vs usuario)</li>
     * </ul>
     * </p>
     * 
     * <p>
     * <b>Orden de validación con grupos:</b> Bean Validation procesa grupos en el orden
     * especificado. Si un grupo falla, los siguientes no se ejecutan (fail-fast).
     * </p>
     *
     * @return Array de clases de grupo (vacío = grupo Default)
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload para metadata adicional sobre el constraint.
     * <p>
     * Payload es un mecanismo de extensión de Bean Validation que permite asociar
     * metadata arbitraria con un constraint. Se usa en casos avanzados para:
     * <ul>
     *   <li>Severidad de error (WARNING, ERROR, CRITICAL)</li>
     *   <li>Categorización de validaciones (SEGURIDAD, NEGOCIO, FORMATO)</li>
     *   <li>Acciones correctivas automáticas</li>
     *   <li>Integración con sistemas de logging/monitoreo</li>
     * </ul>
     * </p>
     * 
     * <h4>Ejemplo de uso con payload:</h4>
     * <pre>
     * // Definir payloads personalizados
     * public interface Critico extends Payload {}
     * public interface RegistroAuditoria extends Payload {}
     * 
     * // Aplicar en validación
     * @DniValido(
     *     payload = {Critico.class, RegistroAuditoria.class}
     * )
     * private String dni;
     * 
     * // Procesar en exception handler
     * @Provider
     * public class ValidationExceptionMapper 
     *         implements ExceptionMapper&lt;ConstraintViolationException&gt; {
     *     
     *     public Response toResponse(ConstraintViolationException ex) {
     *         for (ConstraintViolation violation : ex.getConstraintViolations()) {
     *             Set&lt;Class&lt;? extends Payload&gt;&gt; payloads = 
     *                 violation.getConstraintDescriptor().getPayload();
     *             
     *             if (payloads.contains(Critico.class)) {
     *                 alertService.notificarErrorCritico(violation);
     *             }
     *             
     *             if (payloads.contains(RegistroAuditoria.class)) {
     *                 auditService.registrar(violation);
     *             }
     *         }
     *         // ... construir respuesta
     *     }
     * }
     * </pre>
     * 
     * <p>
     * <b>Payload por defecto:</b> Array vacío indica que no hay metadata adicional.
     * Esto es lo típico para validaciones simples.
     * </p>
     * 
     * <p>
     * <b>Cuándo usar payload:</b>
     * <ul>
     *   <li>Sistemas con múltiples niveles de severidad de errores</li>
     *   <li>Aplicaciones que requieren auditoría detallada de validaciones</li>
     *   <li>Frameworks personalizados que extienden Bean Validation</li>
     *   <li>Integración con sistemas de monitoreo/alertas</li>
     * </ul>
     * En el 95% de casos, el valor por defecto (array vacío) es suficiente.
     * </p>
     *
     * @return Array de clases Payload (vacío = sin metadata adicional)
     */
    Class<? extends Payload>[] payload() default {};
}