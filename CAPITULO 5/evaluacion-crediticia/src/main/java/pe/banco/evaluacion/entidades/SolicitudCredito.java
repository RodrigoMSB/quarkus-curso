package pe.banco.evaluacion.entidades;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import pe.banco.evaluacion.validadores.DniValido;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes_credito")
public class SolicitudCredito extends PanacheEntity {

    @DniValido
    @NotBlank(message = "El DNI es obligatorio")
    @Column(nullable = false, length = 8)
    private String dni;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
    @Column(nullable = false, length = 150)
    private String nombreCompleto;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email debe ser válido")
    @Column(nullable = false, unique = true)
    private String email;

    @NotNull(message = "La edad es obligatoria")
    @Min(value = 18, message = "Debe ser mayor de 18 años")
    @Max(value = 120, message = "Edad no puede exceder 120 años")
    @Column(nullable = false)
    private Integer edad;

    @NotNull(message = "Los ingresos mensuales son obligatorios")
    @DecimalMin(value = "0.0", message = "Los ingresos deben ser positivos")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal ingresosMensuales;

    @NotNull(message = "Las deudas actuales son obligatorias")
    @DecimalMin(value = "0.0", message = "Las deudas no pueden ser negativas")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal deudasActuales;

    @NotNull(message = "El monto solicitado es obligatorio")
    @DecimalMin(value = "1.0", message = "El monto debe ser mayor a 0")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montoSolicitado;

    @NotNull(message = "Los meses en empleo actual son obligatorios")
    @Min(value = 0, message = "Los meses no pueden ser negativos")
    @Column(nullable = false)
    private Integer mesesEnEmpleoActual;

    @Min(value = 0, message = "El score no puede ser negativo")
    @Max(value = 1000, message = "El score no puede exceder 1000")
    private Integer scoreCrediticio;

    private Boolean aprobada;

    @Column(length = 500)
    private String razonEvaluacion;

    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoSolicitud estado;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    private LocalDateTime fechaActualizacion;

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
