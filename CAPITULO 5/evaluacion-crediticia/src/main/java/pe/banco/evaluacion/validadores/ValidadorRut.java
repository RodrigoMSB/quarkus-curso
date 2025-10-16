package pe.banco.evaluacion.validadores;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidadorRut implements ConstraintValidator<ValidRut, String> {

    @Override
    public void initialize(ValidRut constraintAnnotation) {
    }

    @Override
    public boolean isValid(String rut, ConstraintValidatorContext context) {
        if (rut == null || rut.trim().isEmpty()) {
            return false;
        }

        if (!rut.matches("^\\d{7,8}-[0-9Kk]$")) {
            return false;
        }

        String[] partes = rut.split("-");
        String numero = partes[0];
        String digitoVerificador = partes[1].toUpperCase();

        return calcularDigitoVerificador(numero).equals(digitoVerificador);
    }

    private String calcularDigitoVerificador(String rut) {
        int suma = 0;
        int multiplicador = 2;

        for (int i = rut.length() - 1; i >= 0; i--) {
            suma += Character.getNumericValue(rut.charAt(i)) * multiplicador;
            multiplicador = (multiplicador == 7) ? 2 : multiplicador + 1;
        }

        int resto = suma % 11;
        int digitoCalculado = 11 - resto;

        if (digitoCalculado == 11) {
            return "0";
        } else if (digitoCalculado == 10) {
            return "K";
        } else {
            return String.valueOf(digitoCalculado);
        }
    }
}
