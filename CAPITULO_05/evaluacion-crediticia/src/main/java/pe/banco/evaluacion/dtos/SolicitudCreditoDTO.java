package pe.banco.evaluacion.dtos;

import jakarta.validation.constraints.*;
import pe.banco.evaluacion.validadores.DniValido;
import java.math.BigDecimal;

/**
 * Data Transfer Object para solicitudes de crédito.
 * <p>
 * Este DTO sirve como contrato de entrada en la capa REST, separando la representación
 * externa (API) de la interna (entidad de base de datos). Esta separación proporciona:
 * <ul>
 *   <li>Control granular de qué campos pueden ser provistos por el cliente</li>
 *   <li>Validación específica de API diferente a validaciones de persistencia</li>
 *   <li>Protección contra mass assignment attacks</li>
 *   <li>Evolucionabilidad independiente del modelo de dominio</li>
 * </ul>
 * </p>
 * <p>
 * <b>Analogía:</b> Imagina este DTO como un formulario web que el usuario llena en un banco.
 * No incluye campos como "aprobada" o "scoreCrediticio" porque esos son calculados internamente
 * por el banco, no proporcionados por el cliente. Es la interfaz pública vs. los procesos internos.
 * </p>
 * 
 * <h3>Diferencias clave con SolicitudCredito (entidad):</h3>
 * <ul>
 *   <li>No incluye campos calculados (score, aprobada, razón, estado)</li>
 *   <li>No incluye timestamps de auditoría</li>
 *   <li>Validaciones más estrictas en algunos campos (ej: monto mínimo 100,000)</li>
 *   <li>No extiende PanacheEntity ni tiene anotaciones JPA</li>
 * </ul>
 * 
 * <h3>Flujo de uso:</h3>
 * <ol>
 *   <li>Cliente envía JSON en POST /api/v1/creditos/evaluar</li>
 *   <li>Quarkus deserializa automáticamente a SolicitudCreditoDTO</li>
 *   <li>Bean Validation valida todos los constraints</li>
 *   <li>Si válido, se mapea a entidad SolicitudCredito</li>
 *   <li>Entidad se procesa, calcula score y se persiste</li>
 * </ol>
 * 
 * @see pe.banco.evaluacion.entidades.SolicitudCredito
 * @see pe.banco.evaluacion.recursos.CreditoRecurso#evaluar(SolicitudCreditoDTO)
 */
public class SolicitudCreditoDTO {

    /**
     * DNI del solicitante (formato peruano: 8 dígitos).
     * <p>
     * Campo crítico validado con validador personalizado {@link DniValido}.
     * Debe coincidir exactamente con el documento de identidad oficial.
     * </p>
     * 
     * @see pe.banco.evaluacion.validadores.ValidadorDni
     */
    @DniValido
    @NotBlank(message = "El DNI es obligatorio")
    private String dni;

    /**
     * Nombre completo del solicitante.
     * <p>
     * Debe incluir nombres y apellidos completos. Este campo se usa para:
     * <ul>
     *   <li>Identificación oficial del titular del crédito</li>
     *   <li>Generación de contratos y documentos legales</li>
     *   <li>Cumplimiento regulatorio (KYC - Know Your Customer)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Ejemplo:</b> "María Elena Flores Gutiérrez"
     * </p>
     */
    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
    private String nombreCompleto;

    /**
     * Correo electrónico del solicitante.
     * <p>
     * Usado para comunicaciones oficiales sobre el estado de la solicitud.
     * Debe ser un email válido y funcional.
     * </p>
     */
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    private String email;

    /**
     * Edad del solicitante en años.
     * <p>
     * Restricción legal: debe ser mayor de edad (18 años en Perú) para contratar créditos.
     * La edad afecta el scoring crediticio y el plazo máximo de financiamiento.
     * </p>
     */
    @NotNull(message = "La edad es obligatoria")
    @Min(value = 18, message = "Debe ser mayor de 18 años")
    @Max(value = 120, message = "Edad no válida")
    private Integer edad;

    /**
     * Ingresos mensuales del solicitante en soles peruanos.
     * <p>
     * <b>Validación estricta:</b> Debe ser mayor a 0 (inclusive = false).
     * Esto evita casos borde donde ingresos = 0 causarían división por cero
     * al calcular ratios financieros.
     * </p>
     * <p>
     * <b>Nota de diseño:</b> Se usa BigDecimal para evitar errores de precisión
     * en cálculos monetarios. Float/Double son inadecuados para dinero.
     * </p>
     */
    @NotNull(message = "Los ingresos mensuales son obligatorios")
    @DecimalMin(value = "0.0", inclusive = false, message = "Los ingresos deben ser mayores a 0")
    private BigDecimal ingresosMensuales;

    /**
     * Total de deudas mensuales actuales del solicitante.
     * <p>
     * Incluye cuotas de todos los créditos vigentes. Puede ser cero si no tiene deudas previas.
     * Se usa para calcular el DTI (Debt-to-Income ratio).
     * </p>
     * <p>
     * <b>Importante:</b> A diferencia de ingresosMensuales, aquí SÍ se permite 0 (inclusive = true por defecto),
     * ya que un solicitante sin deudas es un caso válido y deseable.
     * </p>
     */
    @NotNull(message = "Las deudas actuales son obligatorias")
    @DecimalMin(value = "0.0", message = "Las deudas no pueden ser negativas")
    private BigDecimal deudasActuales;

    /**
     * Monto total solicitado por el cliente.
     * <p>
     * <b>Validaciones de negocio en DTO:</b>
     * <ul>
     *   <li>Mínimo: S/. 100,000.00 (política del banco para créditos corporativos/hipotecarios)</li>
     *   <li>Máximo: S/. 50,000,000.00 (límite de exposición por cliente)</li>
     * </ul>
     * </p>
     * <p>
     * Estas restricciones son más estrictas que en la entidad porque representan
     * políticas de negocio de la API, no limitaciones técnicas de la base de datos.
     * </p>
     * <p>
     * <b>Ejemplo:</b> Un cliente con ingresos de S/. 5,000 no podría solicitar S/. 50,000
     * porque el ratio monto/ingresos sería 10 (considerado alto riesgo).
     * </p>
     */
    @NotNull(message = "El monto solicitado es obligatorio")
    @DecimalMin(value = "100000.00", message = "El monto mínimo es $100.000")
    @DecimalMax(value = "50000000.00", message = "El monto máximo es $50.000.000")
    private BigDecimal montoSolicitado;

    /**
     * Meses de antigüedad en el empleo actual.
     * <p>
     * Indicador clave de estabilidad laboral. Permite validar:
     * <ul>
     *   <li>Permanencia en empleo (trabajadores con más tiempo son menor riesgo)</li>
     *   <li>Ingresos sostenibles vs. ingresos esporádicos</li>
     *   <li>Probabilidad de continuidad en el pago de cuotas</li>
     * </ul>
     * </p>
     * <p>
     * <b>Regla de negocio:</b> Menos de 3 meses resulta en rechazo automático.
     * Esta validación se realiza en el servicio, no en el DTO.
     * </p>
     */
    @NotNull(message = "Los meses en empleo actual son obligatorios")
    @Min(value = 0, message = "Los meses no pueden ser negativos")
    private Integer mesesEnEmpleoActual;

    /**
     * Constructor sin argumentos requerido para deserialización JSON.
     * <p>
     * Frameworks como Jackson, JSON-B (usado por Quarkus) necesitan un constructor
     * sin argumentos para crear instancias y luego poblarlas vía setters.
     * </p>
     */
    public SolicitudCreditoDTO() {}

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Integer getEdad() { return edad; }
    public void setEdad(Integer edad) { this.edad = edad; }

    public BigDecimal getIngresosMensuales() { return ingresosMensuales; }
    public void setIngresosMensuales(BigDecimal ingresosMensuales) { this.ingresosMensuales = ingresosMensuales; }

    public BigDecimal getDeudasActuales() { return deudasActuales; }
    public void setDeudasActuales(BigDecimal deudasActuales) { this.deudasActuales = deudasActuales; }

    public BigDecimal getMontoSolicitado() { return montoSolicitado; }
    public void setMontoSolicitado(BigDecimal montoSolicitado) { this.montoSolicitado = montoSolicitado; }

    public Integer getMesesEnEmpleoActual() { return mesesEnEmpleoActual; }
    public void setMesesEnEmpleoActual(Integer mesesEnEmpleoActual) { this.mesesEnEmpleoActual = mesesEnEmpleoActual; }
}