package pe.banco.evaluacion.crediticia.model;

public class RespuestaBureau {

    private String dni;
    private Integer scoreBureau; // 0-999
    private Integer deudaActual;
    private Boolean morosidadActiva;
    private String clasificacion; // NORMAL, CPP, DEFICIENTE, DUDOSO, PERDIDA

    // Constructors
    public RespuestaBureau() {}

    public RespuestaBureau(String dni, Integer scoreBureau, Integer deudaActual, Boolean morosidadActiva, String clasificacion) {
        this.dni = dni;
        this.scoreBureau = scoreBureau;
        this.deudaActual = deudaActual;
        this.morosidadActiva = morosidadActiva;
        this.clasificacion = clasificacion;
    }

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
