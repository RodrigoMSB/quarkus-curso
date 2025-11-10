package pe.banco.evaluacion.servicios;

import jakarta.enterprise.context.ApplicationScoped;  // CDI scope para singleton a nivel de aplicación
import pe.banco.evaluacion.entidades.SolicitudCredito;  // Entidad sobre la cual se calcula el scoring

import java.math.BigDecimal;  // Tipo numérico de precisión arbitraria para cálculos monetarios exactos
import java.math.RoundingMode;  // Estrategias de redondeo para operaciones con BigDecimal

/**
 * Servicio de cálculo de scoring crediticio y evaluación de elegibilidad.
 * <p>
 * Este servicio implementa el corazón del motor de decisión crediticia del banco,
 * calculando un score numérico (0-1000) basado en múltiples factores de riesgo y
 * determinando si una solicitud debe ser aprobada o rechazada mediante un conjunto
 * de reglas de negocio y umbrales configurados.
 * </p>
 * <p>
 * <b>Analogía:</b> Piensa en este servicio como el "analista de crédito virtual" del banco.
 * Así como un analista humano revisa la solicitud evaluando ingresos, deudas, estabilidad
 * laboral y otros factores para decidir si aprobar o no, este servicio hace lo mismo pero
 * de forma automatizada, consistente y escalable, procesando cientos de solicitudes por segundo
 * con los mismos criterios objetivos.
 * </p>
 * 
 * <h3>Modelo de Scoring Implementado:</h3>
 * <p>
 * El sistema utiliza un modelo aditivo que parte de un score base de 500 puntos (neutral)
 * y suma/resta puntos según el desempeño en cada categoría evaluada:
 * </p>
 * <ul>
 *   <li><b>Ratio Deuda/Ingreso (DTI):</b> -300 a +200 puntos (peso más alto por ser indicador crítico)</li>
 *   <li><b>Edad:</b> -30 a +80 puntos (perfil demográfico de riesgo)</li>
 *   <li><b>Estabilidad Laboral:</b> -20 a +120 puntos (predictivo de continuidad de ingresos)</li>
 *   <li><b>Capacidad de Pago:</b> -100 a +150 puntos (viabilidad de cuotas mensuales)</li>
 *   <li><b>Monto/Ingresos:</b> -50 a +100 puntos (proporcionalidad de solicitud)</li>
 * </ul>
 * <p>
 * <b>Rango teórico final:</b> 140 (peor caso) a 1150 puntos (mejor caso), 
 * pero se limita a máximo 1000.
 * </p>
 * 
 * <h3>Proceso de Evaluación (Orden Crítico):</h3>
 * <ol>
 *   <li><b>Validaciones Críticas (Deal-breakers):</b> Condiciones que rechazan automáticamente
 *       sin importar el score: empleo &lt; 3 meses, DTI &gt; 50%, cuota insostenible</li>
 *   <li><b>Cálculo de Score:</b> Si pasa validaciones críticas, se calcula el score numérico</li>
 *   <li><b>Evaluación de Umbral:</b> Score ≥ 650 se considera aprobable</li>
 *   <li><b>Decisión Final:</b> Aprobada solo si cumple ambos: validaciones críticas Y umbral de score</li>
 * </ol>
 * 
 * <h3>Constantes de Política Crediticia:</h3>
 * <ul>
 *   <li><b>UMBRAL_APROBACION (650):</b> Score mínimo para considerar aprobación</li>
 *   <li><b>DTI_LIMITE (50%):</b> Máximo porcentaje de deuda permitido sobre ingresos</li>
 *   <li><b>CAPACIDAD_PAGO_FACTOR (40%):</b> Porcentaje máximo de ingresos comprometible en cuota</li>
 * </ul>
 * 
 * <h3>Calibración del Modelo:</h3>
 * <p>
 * Los pesos y umbrales fueron diseñados basándose en mejores prácticas de la industria
 * financiera peruana y latinoamericana. En producción, estos valores deberían:
 * <ul>
 *   <li>Ajustarse mediante análisis de cohortes históricas</li>
 *   <li>Validarse con tasas de default observadas</li>
 *   <li>Refinarse con machine learning sobre datos reales</li>
 *   <li>Auditarse regularmente para evitar sesgos discriminatorios</li>
 * </ul>
 * </p>
 * 
 * @see SolicitudCredito
 */
@ApplicationScoped
public class ScoringService {
    
    /**
     * Score mínimo requerido para que una solicitud sea considerada aprobable.
     * <p>
     * Valor de 650 representa el punto de corte entre "riesgo aceptable" y "alto riesgo"
     * según estándares de la industria crediticia. Este umbral:
     * <ul>
     *   <li>Se basa en análisis actuarial de tasas de morosidad</li>
     *   <li>Equilibra crecimiento de cartera vs. gestión de riesgo</li>
     *   <li>Es consistente con prácticas de bancos latinoamericanos</li>
     * </ul>
     * </p>
     * <p>
     * <b>Nota de configuración:</b> En un sistema productivo, este valor debería
     * externalizarse a properties para ajustarse dinámicamente según política del banco
     * o condiciones de mercado, sin requerir recompilación del código.
     * </p>
     */
    private static final Integer UMBRAL_APROBACION = 650;
    
    /**
     * Límite máximo de ratio Deuda/Ingreso (DTI - Debt-to-Income) permitido.
     * <p>
     * Un DTI de 50% significa que la mitad de los ingresos mensuales ya están
     * comprometidos en pago de deudas existentes. Superar este límite indica
     * sobreendeudamiento y resulta en rechazo automático.
     * </p>
     * <p>
     * <b>Fundamento regulatorio:</b> Basado en regulaciones de la SBS (Superintendencia
     * de Banca y Seguros) del Perú y mejores prácticas internacionales que establecen
     * 50% como umbral de riesgo crítico de insolvencia.
     * </p>
     * <p>
     * <b>Ejemplo:</b> Cliente con ingresos de S/. 4,000 y deudas actuales de S/. 2,100
     * tiene DTI = 52.5%, superando el límite → Rechazo automático.
     * </p>
     */
    private static final BigDecimal DTI_LIMITE = new BigDecimal("50.00");
    
    /**
     * Factor que determina qué porcentaje de ingresos puede destinarse a cuota nueva.
     * <p>
     * 0.40 (40%) es una política conservadora que asegura que el cliente mantenga
     * 60% de sus ingresos disponibles para gastos de vida, ahorros y contingencias.
     * </p>
     * <p>
     * <b>Cálculo de cuota sostenible:</b>
     * Cuota máxima = Ingresos × 0.40
     * </p>
     * <p>
     * <b>Ejemplo práctico:</b>
     * <ul>
     *   <li>Ingresos: S/. 5,000</li>
     *   <li>Capacidad de pago: S/. 5,000 × 0.40 = S/. 2,000</li>
     *   <li>Si cuota estimada del préstamo es S/. 1,800 → Viable</li>
     *   <li>Si cuota estimada es S/. 2,500 → No viable, penaliza score</li>
     * </ul>
     * </p>
     */
    private static final BigDecimal CAPACIDAD_PAGO_FACTOR = new BigDecimal("0.40");

    /**
     * Calcula el score crediticio total de una solicitud.
     * <p>
     * Método principal de scoring que agrega evaluaciones de múltiples dimensiones
     * de riesgo crediticio. El score resultante representa la probabilidad de que
     * el cliente cumpla con sus obligaciones de pago.
     * </p>
     * <p>
     * <b>Algoritmo:</b>
     * <ol>
     *   <li>Partir de score base de 500 (perfil neutral)</li>
     *   <li>Evaluar DTI: +200 (excelente) hasta -300 (crítico)</li>
     *   <li>Evaluar edad: +80 (rango óptimo) hasta -30 (riesgo)</li>
     *   <li>Evaluar estabilidad laboral: +120 (2+ años) hasta -20 (menos de 6 meses)</li>
     *   <li>Evaluar capacidad de pago: +150 (holgada) hasta -100 (insuficiente)</li>
     *   <li>Evaluar ratio monto/ingresos: +100 (conservador) hasta -50 (agresivo)</li>
     *   <li>Limitar score final a máximo 1000</li>
     * </ol>
     * </p>
     * <p>
     * <b>Efecto secundario:</b> Este método modifica la solicitud, estableciendo
     * el valor del campo scoreCrediticio mediante solicitud.setScoreCrediticio(score).
     * </p>
     * <p>
     * <b>Nota de diseño:</b> La línea 24 repite innecesariamente el límite a 1000.
     * En refactoring futuro, se puede eliminar la duplicación.
     * </p>
     * 
     * <h4>Ejemplo de cálculo:</h4>
     * <pre>
     * Solicitud con:
     * - DTI: 25% → +100 puntos
     * - Edad: 35 años → +80 puntos  
     * - Empleo: 18 meses → +80 puntos
     * - Capacidad pago: holgada → +150 puntos
     * - Ratio monto/ingresos: 12 → +50 puntos
     * 
     * Score = 500 + 100 + 80 + 80 + 150 + 50 = 960 puntos → Excelente perfil
     * </pre>
     *
     * @param solicitud Solicitud de crédito a evaluar (será modificada con el score calculado)
     * @return Score crediticio calculado (0-1000), también asignado a la solicitud
     */
    public Integer calcularScore(SolicitudCredito solicitud) {
        int score = 500;
        score += evaluarDTI(solicitud);
        score += evaluarEdad(solicitud);
        score += evaluarEstabilidadLaboral(solicitud);
        score += evaluarCapacidadPago(solicitud);
        score += evaluarMontoSolicitado(solicitud);
        
        score = Math.min(score, 1000);
        score = Math.min(score, 1000);  // Línea duplicada - refactorizar
        solicitud.setScoreCrediticio(score);
        return score;
    }

    /**
     * Calcula el ratio Deuda/Ingreso (DTI - Debt-to-Income) como porcentaje.
     * <p>
     * El DTI es el indicador financiero más importante en evaluación crediticia,
     * midiendo qué proporción del ingreso mensual está comprometida en deudas.
     * </p>
     * <p>
     * <b>Fórmula:</b> DTI = (Deudas Mensuales / Ingresos Mensuales) × 100
     * </p>
     * <p>
     * <b>Interpretación de valores:</b>
     * <ul>
     *   <li>0-20%: Excelente manejo de deudas, bajo riesgo</li>
     *   <li>21-35%: Manejo saludable, riesgo moderado</li>
     *   <li>36-50%: Carga de deuda significativa, límite de seguridad</li>
     *   <li>&gt;50%: Sobreendeudamiento, alto riesgo de default</li>
     * </ul>
     * </p>
     * <p>
     * <b>Manejo de caso especial:</b> Si ingresos = 0, retorna 100% (peor caso posible)
     * para evitar división por cero y señalar situación crítica.
     * </p>
     * 
     * <h4>Uso de BigDecimal para precisión:</h4>
     * <pre>
     * // ❌ INCORRECTO con double - pierde precisión
     * double dti = (deudas / ingresos) * 100;  // Puede dar 35.000000000001
     * 
     * // ✅ CORRECTO con BigDecimal - precisión exacta
     * BigDecimal dti = deudas.divide(ingresos, 4, HALF_UP).multiply(new BigDecimal("100"));
     * // Resultado: 35.0000 exacto
     * </pre>
     *
     * @param deudas Total de deudas mensuales actuales
     * @param ingresosMensuales Total de ingresos mensuales
     * @return DTI como porcentaje con 4 decimales de precisión (ej: 35.5000)
     */
    public BigDecimal calcularDTI(BigDecimal deudas, BigDecimal ingresosMensuales) {
        if (ingresosMensuales.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("100");
        }
        return deudas.divide(ingresosMensuales, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }

    /**
     * Evalúa el ratio Deuda/Ingreso y asigna puntos al score.
     * <p>
     * El DTI es el factor de mayor peso en el scoring (rango de 500 puntos),
     * reflejando su importancia crítica como predictor de capacidad de pago.
     * </p>
     * <p>
     * <b>Escala de puntuación:</b>
     * <ul>
     *   <li>DTI ≤ 20%: +200 puntos (Cliente con excelente margen financiero)</li>
     *   <li>DTI 21-35%: +100 puntos (Cliente con manejo saludable)</li>
     *   <li>DTI 36-50%: 0 puntos (Cliente en límite, neutro)</li>
     *   <li>DTI &gt; 50%: -300 puntos (Cliente sobreendeudado, alto riesgo)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Lógica de negocio:</b> La penalización de -300 para DTI &gt; 50% es severa
     * intencionalmente, casi garantizando rechazo (score difícilmente supera 650).
     * Esto refleja políticas conservadoras ante riesgo crítico de insolvencia.
     * </p>
     *
     * @param solicitud Solicitud a evaluar
     * @return Puntos a sumar/restar del score base (-300 a +200)
     */
    private int evaluarDTI(SolicitudCredito solicitud) {
        BigDecimal dti = calcularDTI(solicitud.getDeudasActuales(), solicitud.getIngresosMensuales());
        if (dti.compareTo(new BigDecimal("20")) <= 0) {
            return 200;
        } else if (dti.compareTo(new BigDecimal("35")) <= 0) {
            return 100;
        } else if (dti.compareTo(DTI_LIMITE) <= 0) {
            return 0;
        } else {
            return -300;
        }
    }

    /**
     * Evalúa la estabilidad laboral basada en meses de antigüedad en empleo actual.
     * <p>
     * La antigüedad laboral es un proxy de estabilidad de ingresos futuros.
     * Empleados con más tiempo en su trabajo actual tienen menor probabilidad
     * de perder su fuente de ingresos durante el período del préstamo.
     * </p>
     * <p>
     * <b>Escala de puntuación:</b>
     * <ul>
     *   <li>≥ 24 meses: +120 puntos (Excelente estabilidad, empleado consolidado)</li>
     *   <li>12-23 meses: +80 puntos (Buena estabilidad, superó período de prueba)</li>
     *   <li>6-11 meses: +40 puntos (Estabilidad moderada, aún en adaptación)</li>
     *   <li>&lt; 6 meses: -20 puntos (Alta rotación, riesgo de pérdida de empleo)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Nota crítica:</b> Solicitudes con menos de 3 meses son rechazadas automáticamente
     * en esAprobadaConValidaciones(), independientemente del score obtenido aquí.
     * </p>
     * <p>
     * <b>Contexto laboral peruano:</b> En Perú, el período de prueba legal es 3 meses.
     * Después de 3 meses, el trabajador gana estabilidad laboral, justificando el
     * umbral de rechazo automático en ese punto.
     * </p>
     *
     * @param solicitud Solicitud a evaluar
     * @return Puntos a sumar/restar del score (-20 a +120)
     */
    private int evaluarEstabilidadLaboral(SolicitudCredito solicitud) {
        int meses = solicitud.getMesesEnEmpleoActual();
        if (meses >= 24) {
            return 120;
        } else if (meses >= 12) {
            return 80;
        } else if (meses >= 6) {
            return 40;
        } else {
            return -20;
        }
    }

    /**
     * Evalúa si el cliente puede pagar la cuota mensual estimada sin comprometer su solvencia.
     * <p>
     * Calcula si la cuota del nuevo préstamo es sostenible dentro del presupuesto mensual,
     * considerando que solo el 40% de los ingresos puede destinarse a pago de deudas sin
     * afectar calidad de vida del cliente.
     * </p>
     * <p>
     * <b>Supuestos del cálculo:</b>
     * <ul>
     *   <li>Plazo estándar: 36 meses (3 años)</li>
     *   <li>Cuota estimada = Monto Solicitado / 36 (simplificación sin interés)</li>
     *   <li>Capacidad de pago = Ingresos × 40%</li>
     * </ul>
     * </p>
     * <p>
     * <b>Escala de puntuación:</b>
     * <ul>
     *   <li>Cuota ≤ Capacidad: +150 puntos (Cliente puede pagar cómodamente)</li>
     *   <li>Cuota ≤ Capacidad × 1.2: +50 puntos (Ajustado pero viable)</li>
     *   <li>Cuota &gt; Capacidad × 1.2: -100 puntos (Cuota insostenible)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Limitación actual:</b> La cuota se calcula sin considerar tasa de interés,
     * lo cual subestima el pago real. En producción, usar fórmula de cuota con interés:
     * </p>
     * <pre>
     * Cuota = Monto × [i(1+i)^n] / [(1+i)^n - 1]
     * Donde: i = tasa mensual, n = número de meses
     * </pre>
     * 
     * <h4>Ejemplo de cálculo:</h4>
     * <pre>
     * Ingresos: S/. 5,000
     * Monto solicitado: S/. 72,000
     * 
     * Capacidad de pago = 5,000 × 0.40 = S/. 2,000
     * Cuota estimada = 72,000 / 36 = S/. 2,000
     * 
     * Resultado: Cuota = Capacidad → +150 puntos (perfecto ajuste)
     * </pre>
     *
     * @param solicitud Solicitud a evaluar
     * @return Puntos a sumar/restar del score (-100 a +150)
     */
    private int evaluarCapacidadPago(SolicitudCredito solicitud) {
        BigDecimal capacidadPago = solicitud.getIngresosMensuales()
            .multiply(CAPACIDAD_PAGO_FACTOR);
        BigDecimal cuotaEstimada = solicitud.getMontoSolicitado()
            .divide(new BigDecimal("36"), 2, RoundingMode.HALF_UP);
        
        if (cuotaEstimada.compareTo(capacidadPago) <= 0) {
            return 150;
        } else if (cuotaEstimada.compareTo(capacidadPago.multiply(new BigDecimal("1.2"))) <= 0) {
            return 50;
        } else {
            return -100;
        }
    }

    /**
     * Evalúa el perfil demográfico de riesgo basado en la edad del solicitante.
     * <p>
     * La edad correlaciona con estabilidad financiera, capacidad de pago y
     * horizonte laboral remanente. Perfiles etarios diferentes presentan
     * riesgos distintos desde perspectiva actuarial.
     * </p>
     * <p>
     * <b>Escala de puntuación:</b>
     * <ul>
     *   <li>25-55 años: +80 puntos (Rango óptimo: peak de ingresos y estabilidad)</li>
     *   <li>18-24 años: +30 puntos (Inicio de carrera, ingresos crecientes pero inestables)</li>
     *   <li>56-65 años: +50 puntos (Pre-jubilación, ingresos altos pero horizonte limitado)</li>
     *   <li>&gt; 65 años: -30 puntos (Post-jubilación, ingresos menores y riesgo de salud)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Justificación actuarial:</b>
     * <ul>
     *   <li><b>25-55:</b> Edad productiva plena, máxima capacidad de generación de ingresos</li>
     *   <li><b>18-24:</b> Rotación laboral alta, aún estableciendo carrera profesional</li>
     *   <li><b>56-65:</b> Ingresos consolidados pero cerca de jubilación (reducción drástica)</li>
     *   <li><b>&gt;65:</b> Dependencia de pensión (típicamente 50-70% de último sueldo)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Consideración ética:</b> Estos ajustes deben aplicarse cuidadosamente para
     * no constituir discriminación etaria. Se basan en riesgo actuarial objetivo,
     * no en estereotipos. Regulaciones como la Ley 30490 en Perú protegen contra
     * discriminación por edad en servicios financieros.
     * </p>
     *
     * @param solicitud Solicitud a evaluar
     * @return Puntos a sumar/restar del score (-30 a +80)
     */
    private int evaluarEdad(SolicitudCredito solicitud) {
        int edad = solicitud.getEdad();
        if (edad >= 25 && edad <= 55) {
            return 80;
        } else if (edad >= 18 && edad < 25) {
            return 30;
        } else if (edad > 55 && edad <= 65) {
            return 50;
        } else {
            return -30;
        }
    }

    /**
     * Evalúa la proporcionalidad entre monto solicitado e ingresos mensuales.
     * <p>
     * Un ratio monto/ingresos alto indica que el cliente está solicitando
     * un crédito desproporcionado a su capacidad económica, lo cual incrementa
     * el riesgo de sobreendeudamiento y default.
     * </p>
     * <p>
     * <b>Escala de puntuación basada en ratio:</b>
     * <ul>
     *   <li>Ratio ≤ 10: +100 puntos (Solicitud conservadora, bajo apalancamiento)</li>
     *   <li>Ratio 11-20: +50 puntos (Solicitud razonable, apalancamiento moderado)</li>
     *   <li>Ratio 21-30: 0 puntos (Solicitud límite, neutral)</li>
     *   <li>Ratio &gt; 30: -50 puntos (Solicitud agresiva, alto apalancamiento)</li>
     * </ul>
     * </p>
     * 
     * <h4>Interpretación de ratios:</h4>
     * <pre>
     * Ingreso: S/. 5,000 mensuales
     * 
     * Ratio 10: Solicita S/. 50,000 → Pagaría en ~10 meses de ingresos íntegros
     * Ratio 20: Solicita S/. 100,000 → Pagaría en ~20 meses de ingresos íntegros  
     * Ratio 30: Solicita S/. 150,000 → Pagaría en ~30 meses de ingresos íntegros
     * Ratio 40: Solicita S/. 200,000 → Pagaría en ~40 meses de ingresos íntegros (riesgoso)
     * </pre>
     * <p>
     * <b>Contexto de uso:</b> Ratios altos (>30) son comunes en créditos hipotecarios
     * (plazos de 20-30 años), pero en créditos de consumo o personales indican
     * sobre-apalancamiento. Este método es más apropiado para créditos de corto/mediano plazo.
     * </p>
     *
     * @param solicitud Solicitud a evaluar
     * @return Puntos a sumar/restar del score (-50 a +100)
     */
    private int evaluarMontoSolicitado(SolicitudCredito solicitud) {
        BigDecimal monto = solicitud.getMontoSolicitado();
        BigDecimal ingresos = solicitud.getIngresosMensuales();
        BigDecimal ratio = monto.divide(ingresos, 2, RoundingMode.HALF_UP);
        
        if (ratio.compareTo(new BigDecimal("10")) <= 0) {
            return 100;
        } else if (ratio.compareTo(new BigDecimal("20")) <= 0) {
            return 50;
        } else if (ratio.compareTo(new BigDecimal("30")) <= 0) {
            return 0;
        } else {
            return -50;
        }
    }

    /**
     * Determina si un score es suficiente para aprobación (evaluación simple por umbral).
     * <p>
     * Método utilitario que encapsula la lógica del umbral de aprobación.
     * Compara el score contra UMBRAL_APROBACION (650).
     * </p>
     * <p>
     * <b>Advertencia:</b> Este método solo evalúa el score numérico, NO considera
     * validaciones críticas de negocio. No usar directamente para decisiones de aprobación.
     * Usar {@link #esAprobadaConValidaciones} en su lugar.
     * </p>
     *
     * @param score Score crediticio a evaluar
     * @return true si score ≥ 650, false en caso contrario
     * @see #esAprobadaConValidaciones
     */
    public boolean esAprobada(Integer score) {
        return score >= UMBRAL_APROBACION;
    }

    /**
     * Determina la aprobación final considerando TANTO score COMO validaciones críticas.
     * <p>
     * Este es el método definitivo para decisiones de aprobación crediticia.
     * Implementa un enfoque de "dual-gate" donde la solicitud debe pasar:
     * <ol>
     *   <li><b>Gate 1 - Validaciones Críticas:</b> Condiciones deal-breaker que causan
     *       rechazo inmediato sin importar el score</li>
     *   <li><b>Gate 2 - Umbral de Score:</b> Score ≥ 650 para perfil aprobable</li>
     * </ol>
     * Solo si AMBOS gates se superan, la solicitud es aprobada.
     * </p>
     * <p>
     * <b>Validaciones Críticas (deal-breakers):</b>
     * <ul>
     *   <li><b>Empleo &lt; 3 meses:</b> Rechaza por inestabilidad laboral crítica.
     *       Justificación: período de prueba legal en Perú es 3 meses, antes de ese
     *       tiempo el trabajador no tiene estabilidad contractual</li>
     *   <li><b>DTI &gt; 50%:</b> Rechaza por sobreendeudamiento. Justificación: cumplimiento
     *       de regulación SBS y prevención de riesgo sistémico</li>
     *   <li><b>Cuota &gt; Capacidad × 1.5:</b> Rechaza por cuota insostenible. Justificación:
     *       incluso si el cliente acepta, alta probabilidad de default por insolvencia</li>
     * </ul>
     * </p>
     * <p>
     * <b>Orden de evaluación (crítico):</b>
     * Las validaciones críticas se evalúan PRIMERO, antes del score. Esto es eficiente
     * porque ahorra procesamiento de scoring para casos que se rechazarán de todas formas,
     * y es correcto lógicamente porque estas condiciones son absolutes (no hay score que
     * los pueda compensar).
     * </p>
     * 
     * <h4>Flujo de decisión:</h4>
     * <pre>
     * ¿Empleo &lt; 3 meses? → SÍ → RECHAZAR (fin)
     *                      ↓ NO
     * ¿DTI &gt; 50%? → SÍ → RECHAZAR (fin)
     *              ↓ NO
     * ¿Cuota insostenible? → SÍ → RECHAZAR (fin)
     *                        ↓ NO
     * ¿Score ≥ 650? → SÍ → APROBAR
     *                ↓ NO
     *              RECHAZAR
     * </pre>
     * 
     * <h4>Ejemplos de casos:</h4>
     * <pre>
     * Caso 1: Score 850 (excelente) pero empleo 2 meses → RECHAZADO
     * Caso 2: Score 680 (bueno), empleo 12 meses, DTI 55% → RECHAZADO  
     * Caso 3: Score 670 (bueno), todas validaciones OK → APROBADO
     * Caso 4: Score 640 (bajo), todas validaciones OK → RECHAZADO
     * </pre>
     *
     * @param solicitud Solicitud a evaluar integralmente
     * @param score Score crediticio previamente calculado
     * @return true si solicitud cumple TODOS los requisitos (validaciones + score), false en cualquier otro caso
     */
    public boolean esAprobadaConValidaciones(SolicitudCredito solicitud, Integer score) {
        // Primero: validaciones críticas (deal-breakers)
        if (solicitud.getMesesEnEmpleoActual() < 3) {
            return false;
        }
        
        BigDecimal dti = calcularDTI(solicitud.getDeudasActuales(), solicitud.getIngresosMensuales());
        if (dti.compareTo(DTI_LIMITE) > 0) {
            return false;
        }
        
        BigDecimal capacidadPago = solicitud.getIngresosMensuales().multiply(CAPACIDAD_PAGO_FACTOR);
        BigDecimal cuotaEstimada = solicitud.getMontoSolicitado()
            .divide(new BigDecimal("36"), 2, RoundingMode.HALF_UP);
        if (cuotaEstimada.compareTo(capacidadPago.multiply(new BigDecimal("1.5"))) > 0) {
            return false;
        }
        
        // Después: evaluar score
        return esAprobada(score);
    }

    /**
     * Genera una explicación textual detallada del resultado de la evaluación crediticia.
     * <p>
     * Cumple con requisitos de transparencia y protección al consumidor financiero,
     * proporcionando al solicitante una explicación clara y específica sobre por qué
     * su solicitud fue aprobada o rechazada. Esto permite al cliente:
     * <ul>
     *   <li>Entender la decisión del banco</li>
     *   <li>Identificar áreas de mejora para futuras solicitudes</li>
     *   <li>Ejercer su derecho de impugnación si considera la decisión incorrecta</li>
     * </ul>
     * </p>
     * <p>
     * <b>Estructura de evaluación (mismo orden que esAprobadaConValidaciones):</b>
     * <ol>
     *   <li>Validar condiciones críticas (empleo, DTI, capacidad pago)</li>
     *   <li>Si todas pasan, evaluar score para mensaje de aprobación/rechazo</li>
     * </ol>
     * </p>
     * <p>
     * <b>Mensajes de rechazo por validaciones críticas:</b>
     * <ul>
     *   <li><b>Empleo &lt; 3 meses:</b> "Rechazado: Inestabilidad laboral. Se requiere mínimo 3 meses..."</li>
     *   <li><b>DTI &gt; 50%:</b> "Rechazado: Ratio deuda/ingreso (XX.XX%) supera límite (50%)."
     *       (incluye valor exacto para transparencia)</li>
     *   <li><b>Cuota insostenible:</b> "Rechazado: Monto solicitado excede capacidad de pago mensual."</li>
     * </ul>
     * </p>
     * <p>
     * <b>Mensajes de aprobación/rechazo por score:</b>
     * <ul>
     *   <li><b>Score ≥ 800:</b> "Aprobado: Excelente perfil crediticio. Felicitaciones."
     *       (reconocimiento especial para perfiles premium)</li>
     *   <li><b>Score 650-799:</b> "Aprobado: Perfil crediticio cumple con los requisitos del banco."</li>
     *   <li><b>Score &lt; 650:</b> "Rechazado: Score crediticio insuficiente para aprobación automática."
     *       (sugiere implícitamente posibilidad de revisión manual)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Consideraciones legales:</b> En Perú, la Ley de Protección al Consumidor
     * y regulaciones de la SBS requieren que decisiones crediticias automatizadas
     * sean explicables. Este método cumple con esa obligación proporcionando
     * razones específicas y accionables.
     * </p>
     * 
     * <h4>Ejemplos de razones generadas:</h4>
     * <pre>
     * Cliente A: Score 920, DTI 15%, empleo 24 meses
     * → "Aprobado: Excelente perfil crediticio. Felicitaciones."
     * 
     * Cliente B: Score 680, DTI 35%, empleo 8 meses
     * → "Aprobado: Perfil crediticio cumple con los requisitos del banco."
     * 
     * Cliente C: Score 850, DTI 55%, empleo 12 meses  
     * → "Rechazado: Ratio deuda/ingreso (55.00%) supera el límite permitido (50%)."
     * 
     * Cliente D: Score 620, todas validaciones OK
     * → "Rechazado: Score crediticio insuficiente para aprobación automática."
     * 
     * Cliente E: Score 750, empleo 2 meses
     * → "Rechazado: Inestabilidad laboral. Se requiere mínimo 3 meses en empleo actual."
     * </pre>
     *
     * @param solicitud Solicitud evaluada
     * @param score Score crediticio calculado
     * @return String con explicación detallada y específica de la decisión
     */
    public String generarRazonEvaluacion(SolicitudCredito solicitud, Integer score) {
        // PRIMERO: Validaciones críticas independientes del score
        if (solicitud.getMesesEnEmpleoActual() < 3) {
            return "Rechazado: Inestabilidad laboral. Se requiere mínimo 3 meses en empleo actual.";
        }
        
        BigDecimal dti = calcularDTI(solicitud.getDeudasActuales(), solicitud.getIngresosMensuales());
        if (dti.compareTo(DTI_LIMITE) > 0) {
            return String.format("Rechazado: Ratio deuda/ingreso (%.2f%%) supera el límite permitido (50%%).", dti);
        }
        
        BigDecimal capacidadPago = solicitud.getIngresosMensuales().multiply(CAPACIDAD_PAGO_FACTOR);
        BigDecimal cuotaEstimada = solicitud.getMontoSolicitado()
            .divide(new BigDecimal("36"), 2, RoundingMode.HALF_UP);
        if (cuotaEstimada.compareTo(capacidadPago.multiply(new BigDecimal("1.5"))) > 0) {
            return "Rechazado: Monto solicitado excede capacidad de pago mensual.";
        }
        
        // DESPUÉS: Evaluación por score
        if (score >= 800) {
            return "Aprobado: Excelente perfil crediticio. Felicitaciones.";
        }
        if (score >= UMBRAL_APROBACION) {
            return "Aprobado: Perfil crediticio cumple con los requisitos del banco.";
        }
        
        return "Rechazado: Score crediticio insuficiente para aprobación automática.";
    }
}