package pe.banco.scoring.model;

public class RespuestaScoring {

    private String dni;
    private Integer scoreInterno;
    private Double probabilidadIncumplimiento;
    private String recomendacion;

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
