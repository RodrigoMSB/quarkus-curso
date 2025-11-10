package pe.banco.evaluacion.entidades;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import pe.banco.evaluacion.validadores.DniValido;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que representa una solicitud de crédito en el sistema bancario.
 * <p>
 * Esta clase modela el ciclo de vida completo de una solicitud crediticia, desde su creación
 * hasta su evaluación y resolución. Utiliza el patrón Active Record de Quarkus a través de
 * PanacheEntity, lo que simplifica las operaciones CRUD y consultas a la base de datos.
 * </p>
 * <p>
 * <b>Analogía:</b> Piensa en esta clase como una "ficha de solicitud" en una entidad bancaria física.
 * Así como un formulario en papel contiene todos los datos del cliente y va acumulando anotaciones
 * (score, aprobación, razones), esta entidad captura digitalmente ese mismo proceso, agregando
 * además auditoría automática de cuándo se creó y modificó.
 * </p>
 * 
 * <h3>Características principales:</h3>
 * <ul>
 *   <li>Validación automática de campos mediante Bean Validation (Jakarta Validation)</li>
 *   <li>Auditoría temporal automática con timestamps de creación y actualización</li>
 *   <li>Validador personalizado para DNI peruano (8 dígitos)</li>
 *   <li>Máquina de estados a través del enum EstadoSolicitud</li>
 *   <li>Precisión decimal para valores monetarios (BigDecimal con escala 2)</li>
 * </ul>
 * 
 * <h3>Ciclo de vida de una solicitud:</h3>
 * <ol>
 *   <li>PENDIENTE: Solicitud recién creada, esperando procesamiento</li>
 *   <li>EN_PROCESO: Sistema está evaluando la solicitud</li>
 *   <li>APROBADA: Solicitud cumple con criterios crediticios</li>
 *   <li>RECHAZADA: Solicitud no cumple criterios o presenta riesgos</li>
 *   <li>REQUIERE_ANALISIS: Casos límite que necesitan revisión manual</li>
 * </ol>
 * 
 * <h3>Validaciones implementadas:</h3>
 * <ul>
 *   <li>DNI: 8 dígitos numéricos (formato peruano)</li>
 *   <li>Edad: Entre 18 y 120 años</li>
 *   <li>Email: Formato válido y único en el sistema</li>
 *   <li>Montos: Siempre positivos, con precisión de 2 decimales</li>
 *   <li>Estabilidad laboral: Meses en empleo actual no negativos</li>
 * </ul>
 * 
 * @see PanacheEntity
 * @see EstadoSolicitud
 * @see pe.banco.evaluacion.servicios.ScoringService
 */
@Entity
@Table(name = "solicitudes_credito")
public class SolicitudCredito extends PanacheEntity {

    /**
     * Documento Nacional de Identidad del solicitante.
     * <p>
     * Validado mediante anotación personalizada {@link DniValido} que verifica el formato
     * peruano de 8 dígitos numéricos exactos.
     * </p>
     * <p>
     * <b>Ejemplo:</b> "12345678" (válido), "1234567" (inválido - faltan dígitos)
     * </p>
     */
    @DniValido
    @NotBlank(message = "El DNI es obligatorio")
    @Column(nullable = false, length = 8)
    private String dni;

    /**
     * Nombre completo del solicitante.
     * <p>
     * Debe incluir nombres y apellidos completos para identificación inequívoca
     * y cumplimiento de requisitos legales y regulatorios.
     * </p>
     * <p>
     * <b>Ejemplo:</b> "Juan Carlos Rodríguez Pérez"
     * </p>
     */
    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
    @Column(nullable = false, length = 150)
    private String nombreCompleto;

    /**
     * Correo electrónico del solicitante.
     * <p>
     * Debe ser único en el sistema para evitar duplicación de solicitudes y facilitar
     * la comunicación con el cliente sobre el estado de su solicitud.
     * </p>
     * <p>
     * <b>Nota:</b> Se utiliza como identificador secundario para notificaciones automatizadas.
     * </p>
     */
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email debe ser válido")
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Edad del solicitante en años.
     * <p>
     * Factor crítico en la evaluación crediticia que afecta:
     * <ul>
     *   <li>Capacidad de pago futura (edad laboral vs. edad de jubilación)</li>
     *   <li>Estabilidad financiera esperada</li>
     *   <li>Riesgo actuarial para el banco</li>
     * </ul>
     * </p>
     * <p>
     * <b>Rango válido:</b> 18-120 años (18 es mayoría de edad legal en Perú)
     * </p>
     */
    @NotNull(message = "La edad es obligatoria")
    @Min(value = 18, message = "Debe ser mayor de 18 años")
    @Max(value = 120, message = "Edad no puede exceder 120 años")
    @Column(nullable = false)
    private Integer edad;

    /**
     * Ingresos mensuales declarados por el solicitante.
     * <p>
     * Valor fundamental para calcular:
     * <ul>
     *   <li>Ratio deuda/ingreso (DTI - Debt-to-Income)</li>
     *   <li>Capacidad de pago de cuotas mensuales</li>
     *   <li>Monto máximo de crédito otorgable</li>
     * </ul>
     * </p>
     * <p>
     * <b>Tipo de dato:</b> BigDecimal para precisión en cálculos financieros.
     * <b>Escala:</b> 2 decimales (centavos).
     * <b>Ejemplo:</b> 3500.50 (tres mil quinientos soles con cincuenta céntimos)
     * </p>
     */
    @NotNull(message = "Los ingresos mensuales son obligatorios")
    @DecimalMin(value = "0.0", message = "Los ingresos deben ser positivos")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal ingresosMensuales;

    /**
     * Total de deudas actuales del solicitante en el sistema financiero.
     * <p>
     * Incluye:
     * <ul>
     *   <li>Préstamos personales activos</li>
     *   <li>Deudas de tarjetas de crédito</li>
     *   <li>Créditos hipotecarios o vehiculares</li>
     *   <li>Cualquier obligación financiera mensual</li>
     * </ul>
     * </p>
     * <p>
     * <b>Uso crítico:</b> Se utiliza para calcular el DTI (Debt-to-Income ratio), 
     * indicador clave que determina si el solicitante puede asumir nueva deuda sin riesgo de sobreendeudamiento.
     * Un DTI superior al 50% generalmente resulta en rechazo automático.
     * </p>
     */
    @NotNull(message = "Las deudas actuales son obligatorias")
    @DecimalMin(value = "0.0", message = "Las deudas no pueden ser negativas")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal deudasActuales;

    /**
     * Monto total solicitado en el crédito.
     * <p>
     * Debe ser al menos 1 sol para considerarse solicitud válida.
     * El sistema evalúa este monto contra los ingresos mensuales para determinar
     * si el ratio monto/ingresos está dentro de parámetros aceptables.
     * </p>
     * <p>
     * <b>Nota de diseño:</b> No se establece un máximo en la entidad; las reglas
     * de negocio para montos máximos se manejan en la capa de servicio y DTO.
     * </p>
     */
    @NotNull(message = "El monto solicitado es obligatorio")
    @DecimalMin(value = "1.0", message = "El monto debe ser mayor a 0")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montoSolicitado;

    /**
     * Cantidad de meses continuos en el empleo actual del solicitante.
     * <p>
     * Indicador de estabilidad laboral que afecta significativamente el scoring:
     * <ul>
     *   <li>0-5 meses: Inestabilidad, penaliza score</li>
     *   <li>6-11 meses: Estabilidad moderada</li>
     *   <li>12-23 meses: Buena estabilidad</li>
     *   <li>24+ meses: Excelente estabilidad, bonifica score</li>
     * </ul>
     * </p>
     * <p>
     * <b>Política crítica:</b> Solicitudes con menos de 3 meses son rechazadas automáticamente
     * sin importar otros factores, por alto riesgo de pérdida de ingreso.
     * </p>
     */
    @NotNull(message = "Los meses en empleo actual son obligatorios")
    @Min(value = 0, message = "Los meses no pueden ser negativos")
    @Column(nullable = false)
    private Integer mesesEnEmpleoActual;

    /**
     * Score o puntaje crediticio calculado por el sistema.
     * <p>
     * Valor numérico entre 0 y 1000 que representa el riesgo crediticio del solicitante.
     * Es calculado por {@link pe.banco.evaluacion.servicios.ScoringService} considerando:
     * <ul>
     *   <li>Ratio deuda/ingreso (DTI)</li>
     *   <li>Edad del solicitante</li>
     *   <li>Estabilidad laboral</li>
     *   <li>Capacidad de pago de cuotas</li>
     *   <li>Proporción monto solicitado vs ingresos</li>
     * </ul>
     * </p>
     * <p>
     * <b>Escala de interpretación:</b>
     * <ul>
     *   <li>800-1000: Excelente perfil crediticio</li>
     *   <li>650-799: Perfil aprobable</li>
     *   <li>0-649: Perfil de alto riesgo, usualmente rechazado</li>
     * </ul>
     * </p>
     * <p>
     * <b>Nota:</b> Puede ser null inicialmente; se calcula durante el proceso de evaluación.
     * </p>
     */
    @Min(value = 0, message = "El score no puede ser negativo")
    @Max(value = 1000, message = "El score no puede exceder 1000")
    private Integer scoreCrediticio;

    /**
     * Indicador de aprobación de la solicitud.
     * <p>
     * Resultado final de la evaluación crediticia:
     * <ul>
     *   <li>true: Solicitud aprobada, cliente elegible para crédito</li>
     *   <li>false: Solicitud rechazada</li>
     *   <li>null: Aún no evaluada (estado inicial)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Importante:</b> Este valor es determinado por
     * {@link pe.banco.evaluacion.servicios.ScoringService#esAprobadaConValidaciones}
     * que aplica tanto validaciones de score como validaciones críticas de negocio.
     * </p>
     */
    private Boolean aprobada;

    /**
     * Descripción textual del resultado de la evaluación.
     * <p>
     * Provee contexto humano-legible sobre por qué se aprobó o rechazó la solicitud.
     * Este mensaje se envía al solicitante y debe cumplir con requisitos regulatorios
     * de transparencia en decisiones crediticias.
     * </p>
     * <p>
     * <b>Ejemplos:</b>
     * <ul>
     *   <li>"Aprobado: Excelente perfil crediticio. Felicitaciones."</li>
     *   <li>"Rechazado: Ratio deuda/ingreso (55.32%) supera el límite permitido (50%)."</li>
     *   <li>"Rechazado: Inestabilidad laboral. Se requiere mínimo 3 meses en empleo actual."</li>
     * </ul>
     * </p>
     */
    @Column(length = 500)
    private String razonEvaluacion;

    /**
     * Estado actual de la solicitud en su ciclo de vida.
     * <p>
     * Implementa una máquina de estados simple pero efectiva para tracking del proceso.
     * Este campo es obligatorio y debe tener valor desde la creación de la entidad.
     * </p>
     * 
     * @see EstadoSolicitud
     */
    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoSolicitud estado;

    /**
     * Timestamp automático de creación del registro.
     * <p>
     * Gestionado por Hibernate mediante @CreationTimestamp, se establece automáticamente
     * al persistir la entidad por primera vez y nunca se modifica posteriormente.
     * </p>
     * <p>
     * <b>Uso:</b> Auditoría, análisis de tiempos de procesamiento, cumplimiento regulatorio.
     * </p>
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Timestamp automático de última actualización.
     * <p>
     * Gestionado por Hibernate mediante @UpdateTimestamp, se actualiza automáticamente
     * cada vez que la entidad se modifica y persiste.
     * </p>
     * <p>
     * <b>Uso:</b> Tracking de modificaciones, detección de solicitudes estancadas,
     * análisis de tiempo de resolución.
     * </p>
     */
    @UpdateTimestamp
    private LocalDateTime fechaActualizacion;

    /**
     * Enumeración que define los estados posibles de una solicitud de crédito.
     * <p>
     * Representa la máquina de estados del proceso de evaluación crediticia.
     * Las transiciones típicas son:
     * PENDIENTE → EN_PROCESO → (APROBADA | RECHAZADA | REQUIERE_ANALISIS)
     * </p>
     * 
     * <h3>Estados definidos:</h3>
     * <ul>
     *   <li><b>PENDIENTE:</b> Solicitud creada, esperando inicio de evaluación</li>
     *   <li><b>EN_PROCESO:</b> Sistema evaluando scoring y validaciones</li>
     *   <li><b>APROBADA:</b> Solicitud cumple todos los criterios, crédito otorgable</li>
     *   <li><b>RECHAZADA:</b> Solicitud no cumple criterios o presenta alto riesgo</li>
     *   <li><b>REQUIERE_ANALISIS:</b> Casos borderline que necesitan revisión humana</li>
     * </ul>
     * 
     * <p>
     * <b>Nota de implementación:</b> Se almacena como String en DB para legibilidad
     * en consultas SQL directas y facilita auditorías.
     * </p>
     */
    public enum EstadoSolicitud {
        PENDIENTE,
        EN_PROCESO,
        APROBADA,
        RECHAZADA,
        REQUIERE_ANALISIS
    }

    // Getters y Setters
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

    public Integer getScoreCrediticio() { return scoreCrediticio; }
    public void setScoreCrediticio(Integer scoreCrediticio) { this.scoreCrediticio = scoreCrediticio; }

    public Boolean getAprobada() { return aprobada; }
    public void setAprobada(Boolean aprobada) { this.aprobada = aprobada; }

    public String getRazonEvaluacion() { return razonEvaluacion; }
    public void setRazonEvaluacion(String razonEvaluacion) { this.razonEvaluacion = razonEvaluacion; }

    public EstadoSolicitud getEstado() { return estado; }
    public void setEstado(EstadoSolicitud estado) { this.estado = estado; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}