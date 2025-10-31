package pe.banco;

// Importa la interfaz generada por OpenAPI que define los contratos de los endpoints
import pe.banco.api.DefaultApi;

// Importa el modelo de respuesta que será enviado como JSON al cliente
import pe.banco.model.ValidacionResponse;

/**
 * Recurso REST que implementa la validación de números de cuenta bancaria.
 * Esta clase implementa la interfaz generada por OpenAPI (DefaultApi) y 
 * define la lógica del endpoint `/validar/{numeroCuenta}`.
 */
public class ValidadorResource implements DefaultApi {

    /**
     * Implementación del endpoint GET /validar/{numeroCuenta}.
     * Este método es invocado automáticamente por Quarkus cuando se realiza una
     * petición GET al endpoint definido en el contrato OpenAPI.
     *
     * @param numeroCuenta Número de cuenta enviado como parámetro en la URL.
     * @return Un objeto {@link ValidacionResponse} con el resultado de la validación.
     */
    @Override
    public ValidacionResponse validarNumeroCuentaGet(String numeroCuenta) {
        
        // Se crea una nueva instancia del objeto de respuesta
        ValidacionResponse response = new ValidacionResponse();
        
        // Se asigna el número de cuenta recibido como parámetro
        response.setNumeroCuenta(numeroCuenta);
        
        // Se realiza la validación del formato (10 dígitos numéricos)
        boolean esValido = validarFormato(numeroCuenta);
        
        // Se asigna el resultado de la validación (true/false)
        response.setValido(esValido);
        
        // Se asigna un mensaje explicativo según el resultado
        response.setMensaje(esValido 
            ? "Cuenta válida: formato correcto" 
            : "Cuenta inválida: debe tener 10 dígitos numéricos");
        
        // Se retorna el objeto de respuesta, que será serializado como JSON
        return response;
    }
    
    /**
     * Valida si el número de cuenta cumple con el formato requerido:
     * exactamente 10 caracteres numéricos.
     *
     * @param numero Cadena que representa el número de cuenta a validar.
     * @return true si es válido, false en caso contrario.
     */
    private boolean validarFormato(String numero) {
        return numero != null             // No debe ser nulo
            && numero.length() == 10      // Debe tener exactamente 10 caracteres
            && numero.matches("\\d+");    // Todos los caracteres deben ser dígitos
    }
}