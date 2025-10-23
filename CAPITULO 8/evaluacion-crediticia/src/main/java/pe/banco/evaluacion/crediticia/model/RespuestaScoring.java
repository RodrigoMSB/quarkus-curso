package pe.banco.evaluacion.crediticia.model;

public class RespuestaScoring {

    private String dni;
    private Integer scoreInterno; // 0-1000
    private Double probabilidadIncumplimiento; // 0.0 - 1.0
    private String recomendacion; // APROBAR, RECHAZAR, REVISAR_MANUAL

    // Constructors
    public RespuestaScoring() {}

    public RespuestaScoring(String dni, Integer scoreInterno, Double probabilidadIncumplimiento, String recomendacion) {
        this.dni = dni;
        this.scoreInterno = scoreInterno;
        this.probabilidadIncumplimiento = probabilidadIncumplimiento;
        this.recomendacion = recomendacion;
    }

    // Getters y Setters
    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public Integer getScoreInterno() {
        return scoreInterno;
    }

    public void setScoreInterno(Integer scoreInterno) {
        this.scoreInterno = scoreInterno;
    }

    public Double getProbabilidadIncumplimiento() {
        return probabilidadIncumplimiento;
    }

    public void setProbabilidadIncumplimiento(Double probabilidadIncumplimiento) {
        this.probabilidadIncumplimiento = probabilidadIncumplimiento;
    }

    public String getRecomendacion() {
        return recomendacion;
    }

    public void setRecomendacion(String recomendacion) {
        this.recomendacion = recomendacion;
    }
}
