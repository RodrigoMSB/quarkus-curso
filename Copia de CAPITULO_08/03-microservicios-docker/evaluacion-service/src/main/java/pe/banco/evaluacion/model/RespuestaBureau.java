package pe.banco.evaluacion.model;

public class RespuestaBureau {

    private String dni;
    private Integer scoreBureau;
    private Integer deudaActual;
    private Boolean morosidadActiva;
    private String clasificacion;

    public RespuestaBureau() {}

    // Getters y Setters
    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public Integer getScoreBureau() {
        return scoreBureau;
    }

    public void setScoreBureau(Integer scoreBureau) {
        this.scoreBureau = scoreBureau;
    }

    public Integer getDeudaActual() {
        return deudaActual;
    }

    public void setDeudaActual(Integer deudaActual) {
        this.deudaActual = deudaActual;
    }

    public Boolean getMorosidadActiva() {
        return morosidadActiva;
    }

    public void setMorosidadActiva(Boolean morosidadActiva) {
        this.morosidadActiva = morosidadActiva;
    }

    public String getClasificacion() {
        return clasificacion;
    }

    public void setClasificacion(String clasificacion) {
        this.clasificacion = clasificacion;
    }
}
