package pe.banco.cuentas.model;

import java.math.BigDecimal;

/**
 * DTO (Data Transfer Object) que representa una cuenta bancaria.
 * 
 * Esta clase es un POJO (Plain Old Java Object) que sirve como:
 * - Modelo de datos para transferencia entre capas
 * - Estructura para serialización/deserialización JSON
 * - Representación de la entidad de negocio "Cuenta"
 * 
 * Analogía: Como una ficha de cliente en un archivo físico del banco,
 * contiene todos los datos esenciales sin comportamiento complejo.
 */
public class Cuenta {
    
    // ============================================
    // ATRIBUTOS - Estado de la cuenta
    // ============================================
    
    /**
     * Número único identificador de la cuenta.
     * Ejemplo: "1000000001"
     * 
     * En una arquitectura real, esto sería un ID generado por la base de datos,
     * pero aquí usamos un String para simular números de cuenta bancarios.
     */
    private String numero;
    
    /**
     * Nombre completo del titular de la cuenta.
     * Ejemplo: "Juan Pérez"
     */
    private String titular;
    
    /**
     * Saldo actual de la cuenta.
     * 
     * Usamos BigDecimal (no double/float) porque:
     * - Precisión exacta para dinero (no errores de redondeo)
     * - Estándar en aplicaciones financieras
     * - Evita bugs como: 0.1 + 0.2 = 0.30000000000000004
     * 
     * Analogía: BigDecimal es como una balanza de precisión digital,
     * mientras que double/float es como estimar "a ojo".
     */
    private BigDecimal saldo;
    
    /**
     * Tipo de cuenta bancaria.
     * Valores esperados: "AHORRO" o "CORRIENTE"
     * 
     * En una versión más avanzada, esto sería un Enum:
     * enum TipoCuenta { AHORRO, CORRIENTE, VISTA }
     */
    private String tipoCuenta;
    
    // ============================================
    // CONSTRUCTORES
    // ============================================
    
    /**
     * Constructor vacío (sin parámetros).
     * 
     * REQUERIDO POR:
     * - JAX-RS/Jackson para deserializar JSON → Objeto Java
     * - JPA/Hibernate para crear instancias desde DB
     * 
     * Cuando llega este JSON:
     * {"numero":"123", "titular":"Ana", "saldo":500, "tipoCuenta":"AHORRO"}
     * 
     * Jackson hace:
     * 1. new Cuenta()           ← Usa este constructor
     * 2. setCampo(valor)        ← Llama setters con valores del JSON
     */
    public Cuenta() {
        // Constructor vacío - No hace nada, pero es ESENCIAL para frameworks
    }
    
    /**
     * Constructor completo con todos los parámetros.
     * 
     * ÚTIL PARA:
     * - Crear instancias de prueba en tests
     * - Inicializar datos de ejemplo (como en CuentaService)
     * - Construcción fluida de objetos
     * 
     * @param numero Identificador único de la cuenta
     * @param titular Nombre del propietario
     * @param saldo Monto actual en la cuenta
     * @param tipoCuenta Clasificación: AHORRO o CORRIENTE
     */
    public Cuenta(String numero, String titular, BigDecimal saldo, String tipoCuenta) {
        this.numero = numero;
        this.titular = titular;
        this.saldo = saldo;
        this.tipoCuenta = tipoCuenta;
    }
    
    // ============================================
    // GETTERS Y SETTERS
    // ============================================
    // Permiten el acceso controlado a los atributos privados
    // siguiendo el principio de ENCAPSULACIÓN
    
    /**
     * Obtiene el número de cuenta.
     * @return Identificador único de la cuenta
     */
    public String getNumero() {
        return numero;
    }
    
    /**
     * Establece el número de cuenta.
     * 
     * Nota: En una versión productiva, validaríamos que:
     * - No sea null o vacío
     * - Cumpla formato esperado (ej: 10 dígitos)
     * 
     * @param numero Nuevo identificador de cuenta
     */
    public void setNumero(String numero) {
        this.numero = numero;
    }
    
    /**
     * Obtiene el titular de la cuenta.
     * @return Nombre completo del propietario
     */
    public String getTitular() {
        return titular;
    }
    
    /**
     * Establece el titular de la cuenta.
     * @param titular Nombre del propietario
     */
    public void setTitular(String titular) {
        this.titular = titular;
    }
    
    /**
     * Obtiene el saldo actual.
     * @return Monto disponible en la cuenta
     */
    public BigDecimal getSaldo() {
        return saldo;
    }
    
    /**
     * Establece el saldo de la cuenta.
     * 
     * IMPORTANTE: Este método NO valida lógica de negocio.
     * No previene saldos negativos, eso es responsabilidad del Service.
     * 
     * Arquitectura en capas:
     * - Model (esta clase): Solo estructura de datos
     * - Service: Validaciones y lógica de negocio
     * 
     * @param saldo Nuevo monto de la cuenta
     */
    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }
    
    /**
     * Obtiene el tipo de cuenta.
     * @return "AHORRO" o "CORRIENTE"
     */
    public String getTipoCuenta() {
        return tipoCuenta;
    }
    
    /**
     * Establece el tipo de cuenta.
     * @param tipoCuenta Clasificación de la cuenta
     */
    public void setTipoCuenta(String tipoCuenta) {
        this.tipoCuenta = tipoCuenta;
    }
    
    // ============================================
    // MÉTODOS ÚTILES (Opcionales pero recomendados)
    // ============================================
    
    /**
     * Representación en texto de la cuenta.
     * Útil para debugging y logs.
     * 
     * @return String legible con datos de la cuenta
     */
    @Override
    public String toString() {
        return "Cuenta{" +
                "numero='" + numero + '\'' +
                ", titular='" + titular + '\'' +
                ", saldo=" + saldo +
                ", tipoCuenta='" + tipoCuenta + '\'' +
                '}';
    }
}

/**
 * ═══════════════════════════════════════════════════════════════
 * CONCEPTOS CLAVE APLICADOS EN ESTA CLASE
 * ═══════════════════════════════════════════════════════════════
 * 
 * 1. DTO (Data Transfer Object)
 *    - Transporta datos entre capas sin lógica de negocio
 *    - Analogía: Como un sobre con documentos, solo contiene info
 * 
 * 2. POJO (Plain Old Java Object)
 *    - Clase simple, sin heredar frameworks
 *    - Solo constructores, getters, setters
 * 
 * 3. JavaBeans Convention
 *    - Constructor vacío obligatorio
 *    - Atributos privados
 *    - Getters/Setters públicos
 *    - Permite frameworks usar reflexión
 * 
 * 4. Inmutabilidad vs Mutabilidad
 *    - Esta clase ES MUTABLE (tiene setters)
 *    - Para inmutabilidad: final fields, sin setters, builder pattern
 * 
 * 5. BigDecimal para Dinero
 *    - SIEMPRE usar para valores monetarios
 *    - Nunca float/double en finanzas
 * 
 * 6. Separación de Responsabilidades
 *    - Esta clase: SOLO datos
 *    - Service: Lógica de negocio
 *    - Resource: Manejo HTTP
 * 
 * ═══════════════════════════════════════════════════════════════
 * EVOLUCIÓN FUTURA
 * ═══════════════════════════════════════════════════════════════
 * 
 * Capítulo 4 - Persistencia:
 *   @Entity
 *   @Id
 *   @GeneratedValue
 *   → Se convierte en entidad JPA
 * 
 * Capítulo 5 - Validación:
 *   @NotNull
 *   @Size(min=10, max=10)
 *   @DecimalMin("0.00")
 *   → Validaciones automáticas
 * 
 * Capítulo 6 - Seguridad:
 *   @JsonIgnore en campos sensibles
 *   → Ocultar datos en respuestas
 * ═══════════════════════════════════════════════════════════════
 */