package pe.banco.aprobacion.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que representa una solicitud de crédito bancario.
 * Extiende PanacheEntity para aprovechar el patrón Active Record.
 */
@Entity
@Table(name = "solicitudes_credito")
public class SolicitudCredito extends PanacheEntity {

    @NotNull(message = "El número de documento es obligatorio")
    @Size(min = 8, max = 12, message = "El documento debe tener entre 8 y 12 caracteres")
    @Column(name = "numero_documento", nullable = false, length = 12)
    public String numeroDocumento;

    @NotNull(message = "El tipo de documento es obligatorio")
    @Column(name = "tipo_documento", nullable = false, length = 10)
    public String tipoDocumento; // DNI, CE, RUC

    @NotNull(message = "El nombre completo es obligatorio")
    @Size(min = 3, max = 200, message = "El nombre debe tener entre 3 y 200 caracteres")
    @Column(name = "nombre_completo", nullable = false, length = 200)
    public String nombreCompleto;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El ingreso debe ser mayor a cero")
    @Column(name = "ingreso_mensual", nullable = false, precision = 12, scale = 2)
    public BigDecimal ingresoMensual;

    @NotNull(message = "El monto solicitado es obligatorio")
    @DecimalMin(value = "1000.00", message = "El monto mínimo es 1000")
    @DecimalMax(value = "500000.00", message = "El monto máximo es 500000")
    @Column(name = "monto_solicitado", nullable = false, precision = 12, scale = 2)
    public BigDecimal montoSolicitado;

    @NotNull(message = "La deuda actual es obligatoria")
    @DecimalMin(value = "0.00", message = "La deuda no puede ser negativa")
    @Column(name = "deuda_actual", nullable = false, precision = 12, scale = 2)
    public BigDecimal deudaActual;

    @NotNull(message = "La antigüedad laboral es obligatoria")
    @Min(value = 0, message = "La antigüedad no puede ser negativa")
    @Max(value = 50, message = "La antigüedad máxima es 50 años")
    @Column(name = "antiguedad_laboral_anios", nullable = false)
    public Integer antiguedadLaboralAnios;

    @NotNull(message = "La edad es obligatoria")
    @Min(value = 18, message = "Debe ser mayor de 18 años")
    @Max(value = 75, message = "La edad máxima es 75 años")
    @Column(name = "edad", nullable = false)
    public Integer edad;

    @Column(name = "tiene_garantia", nullable = false)
    public Boolean tieneGarantia = false;

    @Column(name = "tipo_garantia", length = 50)
    public String tipoGarantia; // HIPOTECARIA, VEHICULAR, PRENDARIA, null

    // Campos de auditoría y resultado
    @Column(name = "fecha_solicitud", nullable = false)
    public LocalDateTime fechaSolicitud;

    @Column(name = "estado", nullable = false, length = 20)
    public String estado; // PENDIENTE, EVALUANDO, APROBADO, RECHAZADO

    @Column(name = "score_calculado")
    public Integer scoreCalculado;

    @Column(name = "monto_aprobado", precision = 12, scale = 2)
    public BigDecimal montoAprobado;

    @Column(name = "tasa_interes", precision = 5, scale = 2)
    public BigDecimal tasaInteres;

    @Column(name = "plazo_maximo_meses")
    public Integer plazoMaximoMeses;

    @Column(name = "tiempo_evaluacion_ms")
    public Long tiempoEvaluacionMs;

    @Column(name = "fecha_evaluacion")
    public LocalDateTime fechaEvaluacion;

    // Constructor
    public SolicitudCredito() {
        this.fechaSolicitud = LocalDateTime.now();
        this.estado = "PENDIENTE";
    }

    // Métodos de negocio
    public BigDecimal calcularRatioDeuda() {
        if (ingresoMensual == null || ingresoMensual.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return deudaActual.divide(ingresoMensual, 4, BigDecimal.ROUND_HALF_UP);
    }

    public boolean esDeudaAlta() {
        return calcularRatioDeuda().compareTo(BigDecimal.valueOf(0.4)) > 0;
    }

    // Métodos de búsqueda Panache
    public static SolicitudCredito findByDocumento(String numeroDocumento) {
        return find("numeroDocumento", numeroDocumento).firstResult();
    }

    public static long countByEstado(String estado) {
        return count("estado", estado);
    }
}
