package pe.banco.productos.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manejador global de excepciones de validación Bean Validation.
 * <p>
 * Intercepta {@link ConstraintViolationException} lanzadas cuando las validaciones
 * fallan (ej: {@code @Valid}, {@code @Positive}, {@code @NotNull}) y las convierte
 * en respuestas HTTP 400 (Bad Request) con detalles de los errores.
 * </p>
 * 
 * <p><strong>¿Por qué es necesario?</strong></p>
 * <ul>
 *   <li>Proporciona respuestas consistentes y user-friendly para errores de validación</li>
 *   <li>Evita que excepciones internas se filtren al cliente</li>
 *   <li>Facilita debugging con mensajes claros sobre qué campo falló y por qué</li>
 * </ul>
 * 
 * <p><strong>Flujo de ejecución:</strong></p>
 * <ol>
 *   <li>Cliente envía request con datos inválidos</li>
 *   <li>Quarkus ejecuta validaciones Bean Validation ({@code @Valid})</li>
 *   <li>Si fallan, lanza {@code ConstraintViolationException}</li>
 *   <li>Este mapper la intercepta y genera respuesta HTTP 400</li>
 *   <li>Cliente recibe JSON con detalles de errores</li>
 * </ol>
 * 
 * <p><strong>Ejemplo de respuesta generada:</strong></p>
 * <pre>{@code
 * {
 *   "title": "Constraint Violation",
 *   "status": 400,
 *   "violations": [
 *     {
 *       "field": "precio",
 *       "message": "El precio debe ser mayor a 0"
 *     },
 *     {
 *       "field": "stock",
 *       "message": "El stock no puede ser negativo"
 *     }
 *   ]
 * }
 * }</pre>
 * 
 * <p><strong>Anotaciones clave:</strong></p>
 * <ul>
 *   <li><strong>@Provider:</strong> Registra esta clase como un proveedor JAX-RS automáticamente</li>
 *   <li><strong>ExceptionMapper:</strong> Interfaz que convierte excepciones en Response HTTP</li>
 * </ul>
 * 
 * @author Curso Quarkus
 * @version 1.0
 * @see ConstraintViolationException
 * @see ExceptionMapper
 */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    /**
     * Convierte una {@link ConstraintViolationException} en una respuesta HTTP 400.
     * <p>
     * Extrae todas las violaciones de constraints, formatea los mensajes de error
     * y los empaqueta en un JSON estructurado para el cliente.
     * </p>
     * 
     * <p><strong>Procesamiento de violaciones:</strong></p>
     * <ul>
     *   <li>Itera sobre cada {@code ConstraintViolation} en la excepción</li>
     *   <li>Extrae el nombre del campo violado (ej: "precio", "stock")</li>
     *   <li>Obtiene el mensaje de error personalizado del {@code @Positive(message="...")}</li>
     *   <li>Construye un mapa field → message para cada violación</li>
     * </ul>
     * 
     * <p><strong>Estructura de la respuesta:</strong></p>
     * <pre>{@code
     * {
     *   "title": "Constraint Violation",        // Título del error
     *   "status": 400,                          // HTTP Status Code
     *   "violations": [                         // Array de violaciones
     *     {
     *       "field": "nombre_del_campo",       // Campo que falló
     *       "message": "mensaje_de_error"      // Razón de la falla
     *     }
     *   ]
     * }
     * }</pre>
     * 
     * @param exception La excepción capturada con todas las violaciones de constraints
     * @return {@link Response} HTTP 400 con JSON detallando los errores de validación
     */
    @Override
    public Response toResponse(ConstraintViolationException exception) {
        // Extraer todas las violaciones de la excepción
        Set<ConstraintViolation<?>> violations = exception.getConstraintViolations();
        
        // Crear estructura de respuesta
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("title", "Constraint Violation");
        responseBody.put("status", 400);
        
        // Transformar violaciones a formato amigable
        // Para cada violación, extraer: campo + mensaje de error
        responseBody.put("violations", violations.stream()
                .map(violation -> {
                    Map<String, String> error = new HashMap<>();
                    
                    // Extraer el nombre del campo desde el path (ej: "crear.request.precio")
                    // Solo queremos la última parte: "precio"
                    String fieldName = violation.getPropertyPath().toString();
                    String[] parts = fieldName.split("\\.");
                    String field = parts[parts.length - 1];
                    
                    error.put("field", field);
                    error.put("message", violation.getMessage());
                    return error;
                })
                .toList());
        
        // Retornar respuesta HTTP 400 con el JSON de errores
        return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(responseBody)
                .build();
    }
}
