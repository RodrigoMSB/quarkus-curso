package pe.banco.evaluacion.dtos;

public class RespuestaEvaluacionDTO {

    private Long solicitudId;
    private Integer scoreCrediticio;
    private Boolean aprobada;
    private String razonEvaluacion;
    private String estado;

    public RespuestaEvaluacionDTO() {}

    public RespuestaEvaluacionDTO(Long solicitudId, Integer scoreCrediticio, Boolean aprobada, 
                                   String razonEvaluacion, String estado) {
        this.solicitudId = solicitudId;
        this.scoreCrediticio = scoreCrediticio;
        this.aprobada = aprobada;
        this.razonEvaluacion = razonEvaluacion;
        this.estado = estado;
    }

    public Long getSolicitudId() { return solicitudId; }
    public void setSolicitudId(Long solicitudId) { this.solicitudId = solicitudId; }
    public Integer getScoreCrediticio() { return scoreCrediticio; }
    public void setScoreCrediticio(Integer scoreCrediticio) { this.scoreCrediticio = scoreCrediticio; }
    public Boolean getAprobada() { return aprobada; }
    public void setAprobada(Boolean aprobada) { this.aprobada = aprobada; }
    public String getRazonEvaluacion() { return razonEvaluacion; }
    public void setRazonEvaluacion(String razonEvaluacion) { this.razonEvaluacion = razonEvaluacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
