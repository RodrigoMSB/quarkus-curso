package pe.banco.evaluacion.crediticia.model;

public class RespuestaIdentidad {

    private String dni;
    private String nombreCompleto;
    private Boolean identidadValida;
    private String estado; // ACTIVO, FALLECIDO, SUSPENDIDO

    // Constructors
    public RespuestaIdentidad() {}

    public RespuestaIdentidad(String dni, String nombreCompleto, Boolean identidadValida, String estado) {
        this.dni = dni;
        this.nombreCompleto = nombreCompleto;
        this.identidadValida = identidadValida;
        this.estado = estado;
    }

    // Getters y Setters
    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public Boolean getIdentidadValida() {
        return identidadValida;
    }

    public void setIdentidadValida(Boolean identidadValida) {
        this.identidadValida = identidadValida;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
