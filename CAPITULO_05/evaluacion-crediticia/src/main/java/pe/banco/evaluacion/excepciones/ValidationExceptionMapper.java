package pe.banco.evaluacion.excepciones;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Errores de validación");
        error.put("status", 400);
        
        Map<String, String> violaciones = exception.getConstraintViolations()
            .stream()
            .collect(Collectors.toMap(
                violation -> obtenerNombreCampo(violation),
                ConstraintViolation::getMessage,
                (v1, v2) -> v1
            ));
        
        error.put("violaciones", violaciones);

        return Response.status(Response.Status.BAD_REQUEST)
            .entity(error)
            .build();
    }

    private String obtenerNombreCampo(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        String[] partes = path.split("\\.");
        return partes[partes.length - 1];
    }
}
