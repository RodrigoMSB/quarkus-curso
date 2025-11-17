package pe.banco.evaluacion.crediticia.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class SolicitudCredito {

    @NotBlank(message = "El DNI es obligatorio")
    private String dni;

    @NotBlank(message = "Los nombres son obligatorios")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    private String apellidos;

    @NotNull(message = "El monto solicitado es obligatorio")
    @Positive(message = "El monto debe ser mayor a cero")
    private Double montoSolicitado;

    @NotNull(message = "Los meses de plazo son obligatorios")
    @Positive(message = "El plazo debe ser mayor a cero")
    private Integer mesesPlazo;

    // Constructors
    public SolicitudCredito() {}

    public SolicitudCredito(String dni, String nombres, String apellidos, Double montoSolicitado, Integer mesesPlazo) {
        this.dni = dni;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.montoSolicitado = montoSolicitado;
        this.mesesPlazo = mesesPlazo;
    }

    // Getters y Setters
    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public Double getMontoSolicitado() {
        return montoSolicitado;
    }

    public void setMontoSolicitado(Double montoSolicitado) {
        this.montoSolicitado = montoSolicitado;
    }

    public Integer getMesesPlazo() {
        return mesesPlazo;
    }

    public void setMesesPlazo(Integer mesesPlazo) {
        this.mesesPlazo = mesesPlazo;
    }
}
