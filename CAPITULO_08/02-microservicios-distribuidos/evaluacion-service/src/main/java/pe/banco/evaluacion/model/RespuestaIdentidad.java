package pe.banco.evaluacion.model;

public class RespuestaIdentidad {

    private String dni;
    private String nombreCompleto;
    private Boolean identidadValida;
    private String estado;

    public RespuestaIdentidad() {}

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
