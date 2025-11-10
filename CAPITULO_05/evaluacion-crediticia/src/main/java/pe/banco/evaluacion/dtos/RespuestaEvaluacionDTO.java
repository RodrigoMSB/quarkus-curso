package pe.banco.evaluacion.dtos;

/**
 * Data Transfer Object que representa la respuesta de una evaluación crediticia.
 * <p>
 * Este DTO encapsula el resultado completo del proceso de evaluación de crédito,
 * proporcionando al cliente toda la información relevante sobre la decisión tomada
 * por el sistema. Sirve como contrato de salida en las operaciones de evaluación.
 * </p>
 * <p>
 * <b>Analogía:</b> Piensa en este DTO como el "ticket de respuesta" que te entregan
 * en un banco después de procesar tu solicitud. Contiene tu número de caso (solicitudId),
 * tu calificación crediticia (score), si fuiste aprobado o no, y la explicación del banco
 * sobre su decisión. Todo en un solo documento compacto y legible.
 * </p>
 * 
 * <h3>Características de diseño:</h3>
 * <ul>
 *   <li>Inmutable por diseño: Los valores se establecen una vez vía constructor</li>
 *   <li>Sin validaciones: Es un objeto de salida, no de entrada</li>
 *   <li>Serializable a JSON automáticamente por Quarkus/Jackson</li>
 *   <li>Campos simples y autodescriptivos para facilitar integración con clientes</li>
 * </ul>
 * 
 * <h3>Uso típico:</h3>
 * <pre>
 * POST /api/v1/creditos/evaluar
 * 
 * Response:
 * {
 *   "solicitudId": 123,
 *   "scoreCrediticio": 720,
 *   "aprobada": true,
 *   "razonEvaluacion": "Aprobado: Perfil crediticio cumple con los requisitos del banco.",
 *   "estado": "APROBADA"
 * }
 * </pre>
 * 
 * <h3>Ventajas de usar DTO de respuesta:</h3>
 * <ul>
 *   <li>Evita exponer la entidad completa (con campos internos sensibles)</li>
 *   <li>Control total sobre la estructura JSON devuelta al cliente</li>
 *   <li>Facilita versionado de API (v1, v2) sin afectar el modelo de dominio</li>
 *   <li>Permite agregar campos calculados o derivados sin modificar la entidad</li>
 * </ul>
 * 
 * @see pe.banco.evaluacion.entidades.SolicitudCredito
 * @see pe.banco.evaluacion.recursos.CreditoRecurso#evaluar(pe.banco.evaluacion.dtos.SolicitudCreditoDTO)
 */
public class RespuestaEvaluacionDTO {

    /**
     * Identificador único de la solicitud de crédito en la base de datos.
     * <p>
     * Este ID permite al cliente realizar seguimiento posterior de su solicitud,
     * consultar su estado, o referenciarla en comunicaciones con el banco.
     * </p>
     * <p>
     * <b>Ejemplo de uso:</b> El cliente puede llamar GET /api/v1/creditos/{solicitudId}
     * usando este ID para obtener detalles adicionales de su solicitud.
     * </p>
     */
    private Long solicitudId;

    /**
     * Score o puntaje crediticio calculado por el sistema.
     * <p>
     * Valor entre 0 y 1000 que representa la calidad crediticia del solicitante.
     * Este puntaje es el resultado de evaluar múltiples factores:
     * <ul>
     *   <li>Ratio deuda/ingreso (DTI)</li>
     *   <li>Edad y perfil demográfico</li>
     *   <li>Estabilidad laboral</li>
     *   <li>Capacidad de pago</li>
     *   <li>Proporción monto/ingresos</li>
     * </ul>
     * </p>
     * <p>
     * <b>Interpretación para el cliente:</b>
     * <ul>
     *   <li>800+: Excelente perfil, puede acceder a mejores tasas</li>
     *   <li>650-799: Buen perfil, aprobación estándar</li>
     *   <li>&lt;650: Perfil requiere mejoras, probablemente rechazado</li>
     * </ul>
     * </p>
     * 
     * @see pe.banco.evaluacion.servicios.ScoringService#calcularScore(pe.banco.evaluacion.entidades.SolicitudCredito)
     */
    private Integer scoreCrediticio;

    /**
     * Indicador booleano de aprobación de la solicitud.
     * <p>
     * <b>true:</b> Crédito aprobado, el cliente puede proceder con la formalización.<br>
     * <b>false:</b> Crédito rechazado, el cliente debe revisar la razón de evaluación.
     * </p>
     * <p>
     * <b>Nota importante:</b> Este campo es el resultado final después de aplicar
     * todas las validaciones críticas y el análisis de score. Un score alto no garantiza
     * aprobación si hay validaciones críticas que fallen (ej: DTI &gt; 50%, empleo &lt; 3 meses).
     * </p>
     */
    private Boolean aprobada;

    /**
     * Explicación textual de la decisión crediticia.
     * <p>
     * Proporciona contexto humano-legible sobre el resultado, cumpliendo con requisitos
     * de transparencia y regulaciones de protección al consumidor financiero.
     * </p>
     * <p>
     * <b>Ejemplos de razones de aprobación:</b>
     * <ul>
     *   <li>"Aprobado: Excelente perfil crediticio. Felicitaciones."</li>
     *   <li>"Aprobado: Perfil crediticio cumple con los requisitos del banco."</li>
     * </ul>
     * </p>
     * <p>
     * <b>Ejemplos de razones de rechazo:</b>
     * <ul>
     *   <li>"Rechazado: Ratio deuda/ingreso (55.32%) supera el límite permitido (50%)."</li>
     *   <li>"Rechazado: Inestabilidad laboral. Se requiere mínimo 3 meses en empleo actual."</li>
     *   <li>"Rechazado: Score crediticio insuficiente para aprobación automática."</li>
     *   <li>"Rechazado: Monto solicitado excede capacidad de pago mensual."</li>
     * </ul>
     * </p>
     * <p>
     * <b>Beneficio para el cliente:</b> Proporciona orientación sobre qué aspectos
     * debe mejorar para una futura solicitud exitosa.
     * </p>
     */
    private String razonEvaluacion;

    /**
     * Estado actual de la solicitud en formato String.
     * <p>
     * Representa el valor del enum {@link pe.banco.evaluacion.entidades.SolicitudCredito.EstadoSolicitud}
     * convertido a String para facilitar la serialización JSON.
     * </p>
     * <p>
     * <b>Valores posibles:</b>
     * <ul>
     *   <li>"PENDIENTE" - Solicitud creada, esperando procesamiento</li>
     *   <li>"EN_PROCESO" - Sistema evaluando la solicitud</li>
     *   <li>"APROBADA" - Solicitud aprobada exitosamente</li>
     *   <li>"RECHAZADA" - Solicitud rechazada</li>
     *   <li>"REQUIERE_ANALISIS" - Casos límite que necesitan revisión manual</li>
     * </ul>
     * </p>
     * <p>
     * <b>Nota de diseño:</b> Se usa String en lugar del enum directamente para
     * desacoplar la API del modelo interno y facilitar evolución independiente.
     * </p>
     */
    private String estado;

    /**
     * Constructor sin argumentos requerido para deserialización JSON.
     * <p>
     * Aunque este DTO se usa principalmente como salida, algunos frameworks y
     * herramientas de testing pueden necesitar construir instancias vía reflection.
     * </p>
     */
    public RespuestaEvaluacionDTO() {}

    /**
     * Constructor con todos los parámetros para construcción inmutable.
     * <p>
     * Este constructor es el método preferido para crear instancias del DTO,
     * promoviendo inmutabilidad y evitando estados inconsistentes donde algunos
     * campos estén null cuando no deberían estarlo.
     * </p>
     * <p>
     * <b>Patrón de uso en el servicio:</b>
     * <pre>
     * return new RespuestaEvaluacionDTO(
     *     solicitud.id,
     *     solicitud.getScoreCrediticio(),
     *     solicitud.getAprobada(),
     *     solicitud.getRazonEvaluacion(),
     *     solicitud.getEstado().toString()
     * );
     * </pre>
     * </p>
     *
     * @param solicitudId Identificador único de la solicitud
     * @param scoreCrediticio Score calculado (0-1000)
     * @param aprobada Indicador de aprobación (true/false)
     * @param razonEvaluacion Explicación de la decisión
     * @param estado Estado actual de la solicitud como String
     */
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