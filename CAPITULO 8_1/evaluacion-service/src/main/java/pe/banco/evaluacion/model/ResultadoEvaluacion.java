package pe.banco.evaluacion.model;

public class ResultadoEvaluacion {

    private String dni;
    private String decision;
    private Integer scoreTotal;
    private String mensaje;
    private Double montoAprobado;
    private String motivoRechazo;

    public ResultadoEvaluacion() {}

    public ResultadoEvaluacion(String dni, String decision, Integer scoreTotal, String mensaje) {
        this.dni = dni;
        this.decision = decision;
        this.scoreTotal = scoreTotal;
        this.mensaje = mensaje;
    }

    // Getters y Setters
    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public Integer getScoreTotal() {
        return scoreTotal;
    }

    public void setScoreTotal(Integer scoreTotal) {
        this.scoreTotal = scoreTotal;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public Double getMontoAprobado() {
        return montoAprobado;
    }

    public void setMontoAprobado(Double montoAprobado) {
        this.montoAprobado = montoAprobado;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }
}
