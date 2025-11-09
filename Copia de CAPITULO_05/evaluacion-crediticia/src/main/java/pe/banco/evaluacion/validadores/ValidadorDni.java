package pe.banco.evaluacion.validadores;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidadorDni implements ConstraintValidator<DniValido, String> {
    
    @Override
    public boolean isValid(String dni, ConstraintValidatorContext context) {
        if (dni == null || dni.isEmpty()) {
            return false;
        }
        
        // DNI peruano: 8 dígitos numéricos
        return dni.matches("^\\d{8}$");
    }
}
