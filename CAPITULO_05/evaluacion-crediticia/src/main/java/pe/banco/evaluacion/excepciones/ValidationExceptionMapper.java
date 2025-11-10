package pe.banco.evaluacion.excepciones;

import jakarta.validation.ConstraintViolation;  // Representa una violación individual de constraint de validación
import jakarta.validation.ConstraintViolationException;  // Excepción lanzada cuando Bean Validation detecta violaciones
import jakarta.ws.rs.core.Response;  // Construcción de respuestas HTTP con códigos de estado y entidades
import jakarta.ws.rs.ext.ExceptionMapper;  // Interfaz JAX-RS para mapear excepciones a respuestas HTTP
import jakarta.ws.rs.ext.Provider;  // Marca la clase como proveedor JAX-RS registrable automáticamente

import java.util.HashMap;  // Estructura de datos mutable para construir respuestas JSON
import java.util.Map;  // Interfaz de HashMap
import java.util.stream.Collectors;  // Operaciones de reducción sobre streams (conversión a mapa)

/**
 * Mapper de excepciones para violaciones de validación de Bean Validation.
 * <p>
 * Este mapper intercepta automáticamente todas las excepciones de tipo
 * {@link ConstraintViolationException} lanzadas en la aplicación y las transforma
 * en respuestas HTTP 400 Bad Request con formato JSON estructurado y legible,
 * proporcionando al cliente información detallada sobre qué campos fallaron
 * la validación y por qué.
 * </p>
 * <p>
 * <b>Analogía:</b> Piensa en este mapper como el "traductor de errores técnicos" del banco.
 * Cuando el sistema detecta un problema de validación (como un DNI inválido), en lugar de
 * mostrarle al cliente un error críptico tipo "jakarta.validation.ConstraintViolationException:
 * validation failed for classes [pe.banco...]", este traductor convierte eso en un mensaje
 * claro: "DNI inválido. Debe contener 8 dígitos". Es el puente entre el lenguaje técnico
 * interno y la comunicación amigable con el cliente.
 * </p>
 * 
 * <h3>¿Qué es un ExceptionMapper?</h3>
 * <p>
 * ExceptionMapper es un componente de JAX-RS (Jakarta RESTful Web Services) que implementa
 * el patrón "Exception Handler". Permite centralizar el manejo de excepciones en lugar de
 * tener try-catch repetidos en cada endpoint. Cuando una excepción no se captura en el
 * código del recurso, JAX-RS busca un mapper registrado para ese tipo de excepción y
 * delega el manejo a él.
 * </p>
 * 
 * <h3>Flujo de manejo de excepciones:</h3>
 * <ol>
 *   <li>Cliente envía request: POST /api/v1/creditos/evaluar con {"dni": "1234567X"}</li>
 *   <li>Quarkus deserializa JSON a SolicitudCreditoDTO</li>
 *   <li>@Valid activa Bean Validation sobre el DTO</li>
 *   <li>ValidadorDni detecta que "1234567X" no cumple regex ^\\d{8}$</li>
 *   <li>Bean Validation crea ConstraintViolation con mensaje "DNI inválido..."</li>
 *   <li>Bean Validation lanza ConstraintViolationException con todas las violaciones</li>
 *   <li>Excepción no se captura en CreditoRecurso (by design, no hay try-catch)</li>
 *   <li>JAX-RS runtime detecta excepción no manejada</li>
 *   <li>JAX-RS busca ExceptionMapper&lt;ConstraintViolationException&gt;</li>
 *   <li>JAX-RS encuentra este mapper y llama toResponse(exception)</li>
 *   <li>Mapper construye respuesta 400 con JSON estructurado</li>
 *   <li>Cliente recibe respuesta limpia y legible</li>
 * </ol>
 * 
 * <h3>Ventajas de usar ExceptionMapper:</h3>
 * <ul>
 *   <li><b>Centralización:</b> Un solo lugar para formatear errores de validación</li>
 *   <li><b>Separación de responsabilidades:</b> Recursos se enfocan en lógica de negocio</li>
 *   <li><b>Consistencia:</b> Todas las respuestas de error tienen el mismo formato</li>
 *   <li><b>Mantenibilidad:</b> Cambiar formato de error solo requiere modificar el mapper</li>
 *   <li><b>Testing:</b> Fácil testear formateo de errores independientemente</li>
 *   <li><b>Documentación:</b> Clientes de API saben qué esperar en errores 400</li>
 * </ul>
 * 
 * <h3>Formato de respuesta producido:</h3>
 * <pre>
 * HTTP/1.1 400 Bad Request
 * Content-Type: application/json
 * 
 * {
 *   "error": "Errores de validación",
 *   "status": 400,
 *   "violaciones": {
 *     "dni": "DNI inválido. Debe contener 8 dígitos",
 *     "edad": "Debe ser mayor de 18 años",
 *     "email": "Email inválido"
 *   }
 * }
 * </pre>
 * 
 * <h3>Estructura de respuesta explicada:</h3>
 * <ul>
 *   <li><b>error:</b> Mensaje general descriptivo del tipo de problema</li>
 *   <li><b>status:</b> Código HTTP (redundante pero útil para debugging)</li>
 *   <li><b>violaciones:</b> Map campo→mensaje con detalle específico por campo</li>
 * </ul>
 * 
 * <h3>Comparación con respuesta por defecto (sin mapper):</h3>
 * <pre>
 * // SIN mapper (respuesta cruda de Quarkus):
 * {
 *   "exception": "jakarta.validation.ConstraintViolationException",
 *   "message": "Validation failed for object='solicitudCreditoDTO'. Error count: 2"
 * }
 * 
 * // CON mapper (respuesta estructurada):
 * {
 *   "error": "Errores de validación",
 *   "status": 400,
 *   "violaciones": {
 *     "dni": "DNI inválido. Debe contener 8 dígitos",
 *     "edad": "Debe ser mayor de 18 años"
 *   }
 * }
 * </pre>
 * La diferencia es clara: con mapper, el cliente recibe información accionable.
 * 
 * <h3>Registro automático:</h3>
 * <p>
 * La anotación @Provider indica a Quarkus/JAX-RS que esta clase debe ser
 * registrada automáticamente como proveedor de servicios. No requiere configuración
 * adicional en application.properties ni CDI beans.xml. El runtime escanea el
 * classpath buscando clases anotadas con @Provider e instancia una copia al startup.
 * </p>
 * 
 * <h3>Orden de mappers (si hay múltiples):</h3>
 * <p>
 * Si existen múltiples ExceptionMappers que pueden manejar una excepción
 * (por herencia de excepciones), JAX-RS elige el más específico:
 * <pre>
 * Exception
 *   └─ RuntimeException
 *        └─ ValidationException
 *             └─ ConstraintViolationException  ← Más específico, será elegido
 * 
 * ExceptionMapper&lt;Exception&gt;                  → Genérico (prioridad baja)
 * ExceptionMapper&lt;ValidationException&gt;        → Menos específico
 * ExceptionMapper&lt;ConstraintViolationException&gt; → Más específico (este mapper, prioridad alta)
 * </pre>
 * </p>
 * 
 * <h3>Mejoras pendientes para producción:</h3>
 * <ul>
 *   <li><b>Logging:</b> Registrar errores de validación para análisis
 *       <pre>LOG.debug("Errores de validación en request: {}", violaciones);</pre>
 *   </li>
 *   <li><b>Request ID:</b> Incluir ID de correlación para tracking
 *       <pre>error.put("requestId", UUID.randomUUID().toString());</pre>
 *   </li>
 *   <li><b>Timestamp:</b> Agregar timestamp del error
 *       <pre>error.put("timestamp", Instant.now());</pre>
 *   </li>
 *   <li><b>Internacionalización:</b> Mensajes en idioma del cliente (Accept-Language)</li>
 *   <li><b>Documentación de errores:</b> Links a documentación sobre cómo resolver
 *       <pre>error.put("helpUrl", "https://api.banco.pe/docs/errors/validation");</pre>
 *   </li>
 *   <li><b>Métricas:</b> Contar errores de validación por tipo para análisis</li>
 * </ul>
 * 
 * @see ConstraintViolationException
 * @see ConstraintViolation
 * @see ExceptionMapper
 * @see pe.banco.evaluacion.validadores.ValidadorDni
 * @see pe.banco.evaluacion.dtos.SolicitudCreditoDTO
 */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    /**
     * Transforma una excepción de validación en respuesta HTTP 400 estructurada.
     * <p>
     * Este método es invocado automáticamente por JAX-RS cuando una
     * ConstraintViolationException no es capturada en el código de negocio.
     * Procesa el conjunto de violaciones, extrae información relevante de cada una,
     * y construye una respuesta JSON amigable para el cliente.
     * </p>
     * 
     * <h4>Estructura de ConstraintViolationException:</h4>
     * <pre>
     * ConstraintViolationException
     *   └─ Set&lt;ConstraintViolation&lt;?&gt;&gt; violaciones
     *        ├─ ConstraintViolation #1
     *        │    ├─ propertyPath: "dni"
     *        │    ├─ message: "DNI inválido. Debe contener 8 dígitos"
     *        │    ├─ invalidValue: "1234567X"
     *        │    └─ constraint: @DniValido
     *        │
     *        └─ ConstraintViolation #2
     *             ├─ propertyPath: "edad"
     *             ├─ message: "Debe ser mayor de 18 años"
     *             ├─ invalidValue: 15
     *             └─ constraint: @Min(18)
     * </pre>
     * 
     * <h4>Procesamiento paso a paso:</h4>
     * <ol>
     *   <li><b>Crear estructura base:</b> Map con "error" y "status"</li>
     *   <li><b>Obtener violaciones:</b> exception.getConstraintViolations()</li>
     *   <li><b>Transformar a Map:</b> Stream API convierte Set a Map</li>
     *   <li><b>Extraer nombre de campo:</b> obtenerNombreCampo() parsea propertyPath</li>
     *   <li><b>Extraer mensaje:</b> violation.getMessage() obtiene mensaje configurado</li>
     *   <li><b>Manejar duplicados:</b> (v1, v2) → v1 mantiene primer mensaje si hay colisiones</li>
     *   <li><b>Agregar a respuesta:</b> Insertar mapa de violaciones en estructura base</li>
     *   <li><b>Construir Response:</b> Status 400 + entity JSON</li>
     * </ol>
     * 
     * <h4>Código detallado con explicaciones:</h4>
     * <pre>
     * Map&lt;String, String&gt; violaciones = exception.getConstraintViolations()
     *     .stream()  // Convierte Set a Stream para procesamiento funcional
     *     .collect(Collectors.toMap(
     *         violation -&gt; obtenerNombreCampo(violation),  // Key: nombre del campo
     *         ConstraintViolation::getMessage,             // Value: mensaje de error
     *         (v1, v2) -&gt; v1  // Merge function: si hay dos errores en mismo campo, usar primero
     *     ));
     * </pre>
     * 
     * <h4>Manejo de duplicados:</h4>
     * <p>
     * Un mismo campo puede tener múltiples violaciones (ej: dni con @NotBlank Y @DniValido).
     * La función de merge (v1, v2) → v1 resuelve conflictos manteniendo el primer mensaje.
     * Alternativas:
     * <ul>
     *   <li><b>Concatenar:</b> (v1, v2) → v1 + "; " + v2 (múltiples mensajes)</li>
     *   <li><b>Lista:</b> Cambiar Map&lt;String, String&gt; a Map&lt;String, List&lt;String&gt;&gt;</li>
     *   <li><b>Priorizar:</b> Ordenar por severidad y tomar más grave</li>
     * </ul>
     * </p>
     * 
     * <h4>Ejemplo de ejecución completa:</h4>
     * <pre>
     * // Input: Exception con 2 violaciones
     * exception.getConstraintViolations() = [
     *   ConstraintViolation(propertyPath="evaluar.dto.dni", message="DNI inválido..."),
     *   ConstraintViolation(propertyPath="evaluar.dto.edad", message="Debe ser mayor...")
     * ]
     * 
     * // Procesamiento:
     * Stream processing:
     *   1. obtenerNombreCampo("evaluar.dto.dni") → "dni"
     *   2. getMessage() → "DNI inválido. Debe contener 8 dígitos"
     *   3. Map.entry("dni", "DNI inválido...")
     *   
     *   1. obtenerNombreCampo("evaluar.dto.edad") → "edad"
     *   2. getMessage() → "Debe ser mayor de 18 años"
     *   3. Map.entry("edad", "Debe ser mayor...")
     * 
     * // Output: Map
     * violaciones = {
     *   "dni": "DNI inválido. Debe contener 8 dígitos",
     *   "edad": "Debe ser mayor de 18 años"
     * }
     * 
     * // Response final:
     * {
     *   "error": "Errores de validación",
     *   "status": 400,
     *   "violaciones": {
     *     "dni": "DNI inválido. Debe contener 8 dígitos",
     *     "edad": "Debe ser mayor de 18 años"
     *   }
     * }
     * </pre>
     * 
     * <h4>Ventajas del formato Map:</h4>
     * <ul>
     *   <li><b>Acceso directo:</b> Cliente puede acceder response.violaciones.dni</li>
     *   <li><b>Binding fácil:</b> Frontend puede vincular errores a campos del formulario</li>
     *   <li><b>Compacto:</b> Menos verboso que array de objetos {field, message}</li>
     *   <li><b>JSON natural:</b> Se serializa perfectamente a JSON sin configuración</li>
     * </ul>
     * 
     * <h4>Uso en frontend (ejemplo JavaScript):</h4>
     * <pre>
     * fetch('/api/v1/creditos/evaluar', {
     *   method: 'POST',
     *   body: JSON.stringify(solicitud)
     * })
     * .then(response =&gt; {
     *   if (response.status === 400) {
     *     return response.json().then(error =&gt; {
     *       // Mostrar errores en formulario
     *       Object.entries(error.violaciones).forEach(([campo, mensaje]) =&gt; {
     *         document.getElementById(`error-${campo}`).textContent = mensaje;
     *         document.getElementById(campo).classList.add('invalid');
     *       });
     *     });
     *   }
     * });
     * </pre>
     * 
     * <p>
     * <b>Performance:</b> Stream API tiene overhead mínimo para sets pequeños (&lt;100 violaciones).
     * En casos extremos con cientos de violaciones, considerar procesamiento imperativo,
     * aunque en práctica, tener &gt;20 violaciones simultáneas indica problema de diseño.
     * </p>
     * 
     * <p>
     * <b>Thread-safety:</b> Este método ES thread-safe porque:
     * <ul>
     *   <li>No mantiene estado entre invocaciones</li>
     *   <li>Todas las variables son locales (stack)</li>
     *   <li>Los objetos creados (Maps, Response) son locales</li>
     * </ul>
     * JAX-RS puede llamar este método concurrentemente desde múltiples threads sin problemas.
     * </p>
     *
     * @param exception Excepción de validación conteniendo todas las violaciones detectadas
     * @return Response HTTP 400 Bad Request con JSON estructurado de errores
     */
    @Override
    public Response toResponse(ConstraintViolationException exception) {
        // Estructura base de la respuesta de error
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Errores de validación");
        error.put("status", 400);
        
        // Transformar violaciones a Map campo→mensaje
        Map<String, String> violaciones = exception.getConstraintViolations()
            .stream()
            .collect(Collectors.toMap(
                violation -> obtenerNombreCampo(violation),  // Extraer nombre de campo del path
                ConstraintViolation::getMessage,              // Obtener mensaje de error configurado
                (v1, v2) -> v1  // Si un campo tiene múltiples errores, mantener el primero
            ));
        
        error.put("violaciones", violaciones);

        return Response.status(Response.Status.BAD_REQUEST)
            .entity(error)
            .build();
    }

    /**
     * Extrae el nombre del campo desde el propertyPath de una violación.
     * <p>
     * Bean Validation genera propertyPaths con estructura jerárquica que incluye
     * información del método, parámetro y campo. Este método parsea esa estructura
     * para extraer solo el nombre del campo, que es lo que el cliente necesita
     * para identificar qué input corregir.
     * </p>
     * 
     * <h4>Estructura de propertyPath:</h4>
     * <p>
     * Cuando Bean Validation valida un parámetro de método anotado con @Valid,
     * el propertyPath tiene esta estructura:
     * <pre>
     * nombreMetodo.nombreParametro.nombreCampo
     *        ↓              ↓              ↓
     *    evaluar         dto            dni
     * 
     * Ejemplo completo: "evaluar.dto.dni"
     * </pre>
     * </p>
     * <p>
     * En validación de entidades JPA (persist/update), el formato puede ser diferente:
     * <pre>
     * nombreEntidad.nombreCampo
     *        ↓           ↓
     * solicitudCredito  dni
     * </pre>
     * </p>
     * 
     * <h4>Algoritmo de extracción:</h4>
     * <ol>
     *   <li>Obtener propertyPath como String: "evaluar.dto.dni"</li>
     *   <li>Dividir por punto separador: ["evaluar", "dto", "dni"]</li>
     *   <li>Tomar último elemento del array: "dni"</li>
     *   <li>Ese es el nombre del campo que el cliente debe conocer</li>
     * </ol>
     * 
     * <h4>Casos especiales:</h4>
     * <pre>
     * // Caso normal (validación de DTO)
     * propertyPath: "evaluar.dto.dni"
     * resultado: "dni"
     * 
     * // Caso de validación directa de entidad
     * propertyPath: "solicitudCredito.dni"
     * resultado: "dni"
     * 
     * // Caso de objeto anidado
     * propertyPath: "evaluar.dto.direccion.ciudad"
     * resultado: "ciudad"  // ⚠️ Pierde contexto de "direccion"
     * 
     * // Caso de colección
     * propertyPath: "evaluar.dto.telefonos[0].numero"
     * resultado: "numero"  // ⚠️ Pierde índice [0]
     * </pre>
     * 
     * <h4>Limitaciones del enfoque actual:</h4>
     * <p>
     * Este método simple funciona bien para validaciones flat (un solo nivel),
     * pero tiene limitaciones con estructuras anidadas:
     * <ul>
     *   <li>Objetos anidados: "direccion.ciudad" se convierte solo en "ciudad",
     *       perdiendo contexto de que es la ciudad de dirección</li>
     *   <li>Colecciones: "telefonos[0].numero" se convierte en "numero",
     *       perdiendo el índice del elemento con error</li>
     * </ul>
     * </p>
     * 
     * <h4>Mejora para objetos anidados:</h4>
     * <pre>
     * private String obtenerNombreCampo(ConstraintViolation&lt;?&gt; violation) {
     *     String path = violation.getPropertyPath().toString();
     *     String[] partes = path.split("\\.");
     *     
     *     // Si path tiene más de 3 partes, hay anidamiento
     *     if (partes.length &gt; 3) {
     *         // Retornar últimas 2 partes: "direccion.ciudad"
     *         return partes[partes.length - 2] + "." + partes[partes.length - 1];
     *     }
     *     
     *     // Path simple, retornar última parte
     *     return partes[partes.length - 1];
     * }
     * </pre>
     * 
     * <h4>Mejora para colecciones:</h4>
     * <pre>
     * private String obtenerNombreCampo(ConstraintViolation&lt;?&gt; violation) {
     *     String path = violation.getPropertyPath().toString();
     *     
     *     // Remover prefijo de método y parámetro: "evaluar.dto."
     *     int ultimoPunto = path.lastIndexOf('.');
     *     int penultimoPunto = path.lastIndexOf('.', ultimoPunto - 1);
     *     
     *     if (penultimoPunto != -1) {
     *         // Retornar desde penúltimo punto: "telefonos[0].numero"
     *         return path.substring(penultimoPunto + 1);
     *     }
     *     
     *     // Fallback: última parte
     *     return path.substring(ultimoPunto + 1);
     * }
     * </pre>
     * 
     * <h4>Testing del método:</h4>
     * <pre>
     * @Test
     * void extraerNombreCampoSimple() {
     *     ConstraintViolation violation = mock(ConstraintViolation.class);
     *     Path path = mock(Path.class);
     *     when(violation.getPropertyPath()).thenReturn(path);
     *     when(path.toString()).thenReturn("evaluar.dto.dni");
     *     
     *     String campo = mapper.obtenerNombreCampo(violation);
     *     
     *     assertEquals("dni", campo);
     * }
     * </pre>
     * 
     * <p>
     * <b>Consideración de diseño:</b> Para aplicaciones simples (DTOs flat), esta
     * implementación es suficiente y preferible por su simplicidad. Para aplicaciones
     * complejas con DTOs anidados, evaluar si la pérdida de contexto es problemática
     * antes de agregar lógica más compleja.
     * </p>
     *
     * @param violation Violación de constraint que contiene el propertyPath completo
     * @return Nombre del campo que violó la validación (última parte del path)
     */
    private String obtenerNombreCampo(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        String[] partes = path.split("\\.");
        return partes[partes.length - 1];
    }
}