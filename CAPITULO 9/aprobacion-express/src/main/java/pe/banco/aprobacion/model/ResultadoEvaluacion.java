package pe.banco.aprobacion.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO que representa el resultado de la evaluación crediticia.
 * Se devuelve al cliente vía REST API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultadoEvaluacion {

    private Long solicitudId;
    private boolean aprobado;
    private Integer scoreCalculado;
    private BigDecimal montoMaximoAprobado;
    private BigDecimal tasaInteres;
    private Integer plazoMaximoMeses;
    private List<String> motivosRechazo;
    private List<String> recomendaciones;
    private Long tiempoEvaluacionMs;
    private LocalDateTime fechaEvaluacion;
    private String nivelRiesgo; // BAJO, MEDIO, ALTO, CRITICO

    // Constructor vacío
    public ResultadoEvaluacion() {
        this.motivosRechazo = new ArrayList<>();
        this.recomendaciones = new ArrayList<>();
        this.fechaEvaluacion = LocalDateTime.now();
    }

    // Constructor para aprobación
    public static ResultadoEvaluacion aprobado(Long solicitudId, Integer score, 
                                               BigDecimal monto, BigDecimal tasa, 
                                               Integer plazo, String nivelRiesgo) {
        ResultadoEvaluacion resultado = new ResultadoEvaluacion();
        resultado.solicitudId = solicitudId;
        resultado.aprobado = true;
        resultado.scoreCalculado = score;
        resultado.montoMaximoAprobado = monto;
        resultado.tasaInteres = tasa;
        resultado.plazoMaximoMeses = plazo;
        resultado.nivelRiesgo = nivelRiesgo;
        return resultado;
    }

    // Constructor para rechazo
    public static ResultadoEvaluacion rechazado(Long solicitudId, Integer score, 
                                                List<String> motivos, String nivelRiesgo) {
        ResultadoEvaluacion resultado = new ResultadoEvaluacion();
        resultado.solicitudId = solicitudId;
        resultado.aprobado = false;
        resultado.scoreCalculado = score;
        resultado.motivosRechazo = motivos;
        resultado.nivelRiesgo = nivelRiesgo;
        return resultado;
    }

    // Método helper para agregar motivo de rechazo
    public void agregarMotivoRechazo(String motivo) {
        this.motivosRechazo.add(motivo);
    }

    // Método helper para agregar recomendación
    public void agregarRecomendacion(String recomendacion) {
        this.recomendaciones.add(recomendacion);
    }

    // Getters y Setters
    public Long getSolicitudId() {
        return solicitudId;
    }

    public void setSolicitudId(Long solicitudId) {
        this.solicitudId = solicitudId;
    }

    public boolean isAprobado() {
        return aprobado;
    }

    public void setAprobado(boolean aprobado) {
        this.aprobado = aprobado;
    }

    public Integer getScoreCalculado() {
        return scoreCalculado;
    }

    public void setScoreCalculado(Integer scoreCalculado) {
        this.scoreCalculado = scoreCalculado;
    }

    public BigDecimal getMontoMaximoAprobado() {
        return montoMaximoAprobado;
    }

    public void setMontoMaximoAprobado(BigDecimal montoMaximoAprobado) {
        this.montoMaximoAprobado = montoMaximoAprobado;
    }

    public BigDecimal getTasaInteres() {
        return tasaInteres;
    }

    public void setTasaInteres(BigDecimal tasaInteres) {
        this.tasaInteres = tasaInteres;
    }

    public Integer getPlazoMaximoMeses() {
        return plazoMaximoMeses;
    }

    public void setPlazoMaximoMeses(Integer plazoMaximoMeses) {
        this.plazoMaximoMeses = plazoMaximoMeses;
    }

    public List<String> getMotivosRechazo() {
        return motivosRechazo;
    }

    public void setMotivosRechazo(List<String> motivosRechazo) {
        this.motivosRechazo = motivosRechazo;
    }

    public List<String> getRecomendaciones() {
        return recomendaciones;
    }

    public void setRecomendaciones(List<String> recomendaciones) {
        this.recomendaciones = recomendaciones;
    }

    public Long getTiempoEvaluacionMs() {
        return tiempoEvaluacionMs;
    }

    public void setTiempoEvaluacionMs(Long tiempoEvaluacionMs) {
        this.tiempoEvaluacionMs = tiempoEvaluacionMs;
    }

    public LocalDateTime getFechaEvaluacion() {
        return fechaEvaluacion;
    }

    public void setFechaEvaluacion(LocalDateTime fechaEvaluacion) {
        this.fechaEvaluacion = fechaEvaluacion;
    }

    public String getNivelRiesgo() {
        return nivelRiesgo;
    }

    public void setNivelRiesgo(String nivelRiesgo) {
        this.nivelRiesgo = nivelRiesgo;
    }
}
