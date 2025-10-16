package pe.banco.evaluacion.validadores;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidadorDni.class)
@Documented
public @interface DniValido {
    String message() default "DNI inválido. Debe contener 8 dígitos";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
