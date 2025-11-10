package pe.banco.evaluacion.servicios;

import jakarta.enterprise.context.ApplicationScoped;  // CDI scope para singleton a nivel de aplicación

/**
 * Servicio de validaciones auxiliares para datos de clientes.
 * <p>
 * Este servicio encapsula lógica de validación de formatos y reglas de negocio
 * específicas que no están cubiertas por Bean Validation estándar. Proporciona
 * validadores personalizados para documentos de identidad, rangos de edad y
 * formatos de correo electrónico según normativas y convenciones locales.
 * </p>
 * <p>
 * <b>Analogía:</b> Piensa en este servicio como el "verificador de documentos" en la
 * ventanilla de un banco. Así como un empleado bancario revisa que tu RUT chileno
 * tenga el dígito verificador correcto, o que tu email esté bien escrito antes de
 * procesarlo, este servicio hace esas verificaciones automáticas para garantizar
 * calidad de datos desde el ingreso.
 * </p>
 * 
 * <h3>Propósito y diseño:</h3>
 * <ul>
 *   <li><b>Separación de responsabilidades:</b> Aísla lógica de validación compleja
 *       que no pertenece en entidades o DTOs</li>
 *   <li><b>Reutilización:</b> Validaciones disponibles para múltiples capas (recursos, servicios)</li>
 *   <li><b>Testing:</b> Lógica de validación fácilmente testeable de forma unitaria</li>
 *   <li><b>Extensibilidad:</b> Lugar centralizado para agregar nuevas validaciones
 *       (ej: validar pasaportes, licencias de conducir, etc.)</li>
 * </ul>
 * 
 * <h3>Relación con Bean Validation:</h3>
 * <p>
 * Este servicio complementa (no reemplaza) Bean Validation. Comparación:
 * </p>
 * <pre>
 * Bean Validation (@NotNull, @Email, etc.):
 * - Validaciones declarativas en DTOs/entidades
 * - Automáticas en capa REST
 * - Ideales para reglas simples y estándar
 * 
 * ValidacionService:
 * - Validaciones programáticas con lógica compleja
 * - Invocación explícita donde se necesite
 * - Ideal para algoritmos específicos (ej: RUT, DNI)
 * </pre>
 * 
 * <h3>Nota sobre validación de DNI peruano:</h3>
 * <p>
 * Aunque este servicio no contiene validador de DNI peruano (existe un validador
 * dedicado {@link pe.banco.evaluacion.validadores.ValidadorDni}), mantiene
 * validador de RUT chileno para demostrar el patrón. En un contexto real de
 * banco peruano, considera remover validarRutChileno() si no se opera en Chile,
 * o mantenerlo si el banco tiene operaciones transfronterizas.
 * </p>
 * 
 * @see pe.banco.evaluacion.validadores.ValidadorDni
 * @see pe.banco.evaluacion.validadores.DniValido
 */
@ApplicationScoped
public class ValidacionService {

    /**
     * Valida formato y dígito verificador de un RUT chileno.
     * <p>
     * El RUT (Rol Único Tributario) es el documento de identificación usado en Chile,
     * similar al DNI peruano. Incluye un dígito verificador calculado mediante algoritmo
     * módulo 11 que permite detectar errores de digitación o RUTs falsos.
     * </p>
     * <p>
     * <b>Formato esperado:</b> XXXXXXXX-Y o XXXXXXX-Y donde:
     * <ul>
     *   <li>X = dígitos del número base (7 u 8 dígitos)</li>
     *   <li>Y = dígito verificador (0-9 o K)</li>
     *   <li>Separador obligatorio: guion (-)</li>
     * </ul>
     * </p>
     * 
     * <h4>Ejemplos de RUTs válidos:</h4>
     * <pre>
     * 12345678-5   → Válido (8 dígitos + verificador)
     * 1234567-K    → Válido (7 dígitos + verificador K)
     * 11111111-1   → Válido (si verificador coincide)
     * </pre>
     * 
     * <h4>Ejemplos de RUTs inválidos:</h4>
     * <pre>
     * 12345678     → Inválido (falta guion y verificador)
     * 12345678-A   → Inválido (verificador debe ser 0-9 o K)
     * 123456789-5  → Inválido (más de 8 dígitos)
     * 12345678-4   → Inválido (verificador incorrecto, debería ser otro)
     * </pre>
     * 
     * <h4>Algoritmo de validación:</h4>
     * <ol>
     *   <li>Verificar formato con regex: 7-8 dígitos, guion, dígito verificador</li>
     *   <li>Separar número base y dígito verificador</li>
     *   <li>Calcular dígito verificador esperado con algoritmo módulo 11</li>
     *   <li>Comparar verificador calculado vs. verificador provisto</li>
     * </ol>
     * 
     * <p>
     * <b>Contexto de uso:</b> Si el banco opera en Chile o atiende clientes chilenos,
     * este validador es crítico para:
     * <ul>
     *   <li>Prevenir fraude de identidad</li>
     *   <li>Cumplir con regulaciones KYC (Know Your Customer)</li>
     *   <li>Integrar con servicios gubernamentales chilenos (Registro Civil)</li>
     *   <li>Validar datos antes de enviarlos a burós de crédito chilenos</li>
     * </ul>
     * </p>
     *
     * @param rut RUT chileno en formato "XXXXXXXX-Y" (con guion separador)
     * @return true si el RUT tiene formato válido y dígito verificador correcto, false en caso contrario
     * @see #calcularDigitoVerificador(String)
     */
    public boolean validarRutChileno(String rut) {
        if (rut == null || rut.trim().isEmpty()) {
            return false;
        }

        // Validar formato: 7-8 dígitos, guion, dígito verificador (0-9 o K)
        if (!rut.matches("^\\d{7,8}-[0-9Kk]$")) {
            return false;
        }

        String[] partes = rut.split("-");
        String numero = partes[0];
        String digitoVerificador = partes[1].toUpperCase();

        return calcularDigitoVerificador(numero).equals(digitoVerificador);
    }

    /**
     * Calcula el dígito verificador de un RUT chileno usando algoritmo módulo 11.
     * <p>
     * Este algoritmo es el estándar oficial usado por el Registro Civil de Chile
     * para generar y validar RUTs. Utiliza multiplicadores cíclicos 2-7 y módulo 11
     * para generar un código de verificación que detecta errores comunes de digitación.
     * </p>
     * 
     * <h4>Algoritmo paso a paso:</h4>
     * <ol>
     *   <li><b>Multiplicación:</b> Recorrer dígitos de derecha a izquierda, multiplicando
     *       cada uno por secuencia 2,3,4,5,6,7,2,3,4... (ciclo infinito)</li>
     *   <li><b>Suma:</b> Sumar todos los productos</li>
     *   <li><b>Módulo 11:</b> Calcular resto de dividir suma entre 11</li>
     *   <li><b>Dígito:</b> Verificador = 11 - resto, con casos especiales:
     *       <ul>
     *         <li>Si resultado = 11 → "0"</li>
     *         <li>Si resultado = 10 → "K"</li>
     *         <li>Cualquier otro → resultado como String</li>
     *       </ul>
     *   </li>
     * </ol>
     * 
     * <h4>Ejemplo de cálculo para RUT 12345678:</h4>
     * <pre>
     * Dígitos:        1   2   3   4   5   6   7   8
     * Multiplicador:  3   2   7   6   5   4   3   2  (de derecha a izquierda, ciclo 2-7)
     * Productos:      3 + 4 +21 +24 +25 +24 +21 +16 = 138
     * 
     * 138 % 11 = 6 (resto)
     * 11 - 6 = 5 → Verificador = "5"
     * 
     * RUT completo: 12345678-5
     * </pre>
     * 
     * <h4>Ejemplo con resultado especial (K):</h4>
     * <pre>
     * Si suma % 11 = 1
     * 11 - 1 = 10 → Verificador = "K"
     * </pre>
     * 
     * <p>
     * <b>¿Por qué módulo 11?</b> El uso de módulo 11 con multiplicadores cíclicos
     * crea un código de verificación robusto que detecta:
     * <ul>
     *   <li>Todos los errores de un solo dígito</li>
     *   <li>~90% de errores de transposición (ej: 12 → 21)</li>
     *   <li>Muchos errores de doble dígito</li>
     * </ul>
     * Es el mismo principio usado en códigos ISBN, IBAN, y otros estándares internacionales.
     * </p>
     *
     * @param rut Número base del RUT (sin guion ni verificador), 7-8 dígitos
     * @return Dígito verificador calculado como String ("0"-"9" o "K")
     */
    private String calcularDigitoVerificador(String rut) {
        int suma = 0;
        int multiplicador = 2;

        // Recorrer de derecha a izquierda
        for (int i = rut.length() - 1; i >= 0; i--) {
            suma += Character.getNumericValue(rut.charAt(i)) * multiplicador;
            
            // Ciclar multiplicador: 2,3,4,5,6,7,2,3,4,5,6,7,...
            multiplicador = (multiplicador == 7) ? 2 : multiplicador + 1;
        }

        int resto = suma % 11;
        int digitoCalculado = 11 - resto;

        // Casos especiales
        if (digitoCalculado == 11) {
            return "0";
        } else if (digitoCalculado == 10) {
            return "K";
        } else {
            return String.valueOf(digitoCalculado);
        }
    }

    /**
     * Valida que la edad esté en rango legal para contratar servicios financieros.
     * <p>
     * Verifica que la persona sea mayor de edad (18 años en Perú) y que la edad
     * declarada sea realista (máximo 120 años). Esta validación es requisito legal
     * previo a cualquier operación crediticia.
     * </p>
     * <p>
     * <b>Marco legal peruano:</b>
     * <ul>
     *   <li>Código Civil peruano (Art. 42): Mayoría de edad a los 18 años</li>
     *   <li>Menores de 18 no pueden contratar créditos sin representante legal</li>
     *   <li>Ley General del Sistema Financiero requiere verificación de capacidad jurídica</li>
     * </ul>
     * </p>
     * <p>
     * <b>¿Por qué máximo 120 años?</b> Es un límite técnico razonable que:
     * <ul>
     *   <li>Permite detectar errores de digitación (ej: 1200 en vez de 21)</li>
     *   <li>Cubre casos extremos reales (récord mundial: 122 años)</li>
     *   <li>Previene valores absurdos en la base de datos</li>
     * </ul>
     * </p>
     * 
     * <h4>Casos de validación:</h4>
     * <pre>
     * validarEdadLegal(null)  → false (edad requerida)
     * validarEdadLegal(17)    → false (menor de edad)
     * validarEdadLegal(18)    → true  (recién mayor de edad)
     * validarEdadLegal(45)    → true  (edad típica)
     * validarEdadLegal(120)   → true  (límite superior inclusive)
     * validarEdadLegal(121)   → false (excede límite)
     * validarEdadLegal(-5)    → false (edad negativa)
     * </pre>
     * 
     * <p>
     * <b>Consideración de diseño:</b> Esta validación es redundante con las anotaciones
     * Bean Validation en la entidad/DTO (@Min(18), @Max(120)). Se mantiene aquí para:
     * <ul>
     *   <li>Validación programática donde Bean Validation no esté disponible</li>
     *   <li>Testing unitario independiente de framework</li>
     *   <li>Lógica de negocio que necesite validar edad sin DTO</li>
     * </ul>
     * En refactoring futuro, considera si es necesario mantener ambas o consolidar.
     * </p>
     *
     * @param edad Edad en años del solicitante
     * @return true si edad está entre 18 y 120 (inclusive), false en caso contrario o si es null
     */
    public boolean validarEdadLegal(Integer edad) {
        return edad != null && edad >= 18 && edad <= 120;
    }

    /**
     * Valida formato de dirección de correo electrónico.
     * <p>
     * Verifica que el email cumpla con estructura básica de formato estándar:
     * usuario@dominio.extension. Esta validación es esencial para:
     * <ul>
     *   <li>Prevenir errores de captura de datos</li>
     *   <li>Garantizar comunicaciones exitosas con el cliente</li>
     *   <li>Evitar intentos de envío a direcciones malformadas (ahorro de recursos)</li>
     * </ul>
     * </p>
     * 
     * <h4>Formato validado por la regex:</h4>
     * <pre>
     * ^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$
     * 
     * Desglose:
     * - ^                      : Inicio de string
     * - [A-Za-z0-9+_.-]+       : Usuario (letras, números, +_.- permitidos)
     * - @                      : Arroba obligatoria
     * - [A-Za-z0-9.-]+         : Dominio (letras, números, puntos, guiones)
     * - \.                     : Punto literal antes de extensión
     * - [A-Za-z]{2,}           : Extensión (mínimo 2 letras: .pe, .com, etc.)
     * - $                      : Fin de string
     * </pre>
     * 
     * <h4>Emails válidos:</h4>
     * <pre>
     * juan.perez@banco.com.pe       → Válido
     * cliente_123@gmail.com         → Válido
     * soporte+tickets@empresa.io    → Válido (+ permitido para aliases Gmail)
     * info@sub.dominio.org          → Válido (subdominios)
     * </pre>
     * 
     * <h4>Emails inválidos:</h4>
     * <pre>
     * juan.perez                    → Inválido (falta @dominio)
     * @banco.com                    → Inválido (falta usuario)
     * juan@banco                    → Inválido (falta extensión .com/.pe/etc)
     * juan perez@banco.com          → Inválido (espacios no permitidos)
     * juan@banco..com               → Inválido (puntos consecutivos)
     * </pre>
     * 
     * <p>
     * <b>Limitaciones de esta validación (simple):</b>
     * Esta regex es una validación básica, no cubre todos los casos del RFC 5322 completo:
     * <ul>
     *   <li>No valida IPs como dominio (ej: usuario@[192.168.1.1])</li>
     *   <li>No valida caracteres Unicode/internacionales</li>
     *   <li>No verifica que el dominio realmente exista (DNS lookup)</li>
     *   <li>No valida que el buzón esté activo</li>
     * </ul>
     * Para validación robusta en producción, considera usar librerías especializadas
     * como Apache Commons Validator o servicios de verificación de email.
     * </p>
     * 
     * <p>
     * <b>Redundancia con Bean Validation:</b> Similar a validarEdadLegal(), esta validación
     * es redundante con @Email de Bean Validation. Se mantiene por las mismas razones:
     * validación programática, testing unitario, y uso fuera de contexto de DTO.
     * </p>
     * 
     * <h4>Alternativa recomendada para producción:</h4>
     * <pre>
     * // Usar Bean Validation @Email en DTO
     * @Email(message = "Email inválido")
     * private String email;
     * 
     * // O usar librería especializada
     * import org.apache.commons.validator.routines.EmailValidator;
     * 
     * public boolean validarEmail(String email) {
     *     return EmailValidator.getInstance().isValid(email);
     * }
     * </pre>
     *
     * @param email Dirección de correo electrónico a validar
     * @return true si el email tiene formato básico válido, false en caso contrario o si es null/vacío
     */
    public boolean validarEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}