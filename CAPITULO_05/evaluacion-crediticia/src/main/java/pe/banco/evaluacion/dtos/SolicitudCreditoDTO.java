package pe.banco.evaluacion.dtos;

import jakarta.validation.constraints.*;
import pe.banco.evaluacion.validadores.DniValido;
import java.math.BigDecimal;

public class SolicitudCreditoDTO {

    @DniValido
    @NotBlank(message = "El DNI es obligatorio")
    private String dni;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
    private String nombreCompleto;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    private String email;

    @NotNull(message = "La edad es obligatoria")
    @Min(value = 18, message = "Debe ser mayor de 18 años")
    @Max(value = 120, message = "Edad no válida")
    private Integer edad;

    @NotNull(message = "Los ingresos mensuales son obligatorios")
    @DecimalMin(value = "0.0", inclusive = false, message = "Los ingresos deben ser mayores a 0")
    private BigDecimal ingresosMensuales;

    @NotNull(message = "Las deudas actuales son obligatorias")
    @DecimalMin(value = "0.0", message = "Las deudas no pueden ser negativas")
    private BigDecimal deudasActuales;

    @NotNull(message = "El monto solicitado es obligatorio")
    @DecimalMin(value = "100000.00", message = "El monto mínimo es $100.000")
    @DecimalMax(value = "50000000.00", message = "El monto máximo es $50.000.000")
    private BigDecimal montoSolicitado;

    @NotNull(message = "Los meses en empleo actual son obligatorios")
    @Min(value = 0, message = "Los meses no pueden ser negativos")
    private Integer mesesEnEmpleoActual;

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
