package pe.banco.evaluacion.excepciones;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GenericExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        LOG.error("Error no controlado", exception);

        Map<String, Object> error = new HashMap<>();
        error.put("error", "Error interno del servidor");
        error.put("mensaje", exception.getMessage());
        error.put("status", 500);

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(error)
            .build();
    }
}
