package pe.banco;

import pe.banco.api.DefaultApi;
import pe.banco.model.ValidacionResponse;

public class ValidadorResource implements DefaultApi {

    @Override
    public ValidacionResponse validarNumeroCuentaGet(String numeroCuenta) {
        
        ValidacionResponse response = new ValidacionResponse();
        response.setNumeroCuenta(numeroCuenta);
        
        boolean esValido = validarFormato(numeroCuenta);
        response.setValido(esValido);
        response.setMensaje(esValido 
            ? "Cuenta válida: formato correcto" 
            : "Cuenta inválida: debe tener 10 dígitos numéricos");
        
        return response;
    }
    
    private boolean validarFormato(String numero) {
        return numero != null 
            && numero.length() == 10 
            && numero.matches("\\d+");
    }
}