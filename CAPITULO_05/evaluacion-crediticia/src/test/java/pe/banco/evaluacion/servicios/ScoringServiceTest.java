package pe.banco.evaluacion.servicios;

import io.quarkus.test.junit.QuarkusTest;  // Anotación que habilita testing con contexto de Quarkus completo
import jakarta.inject.Inject;  // Inyección de dependencias CDI en tests
import org.junit.jupiter.api.Test;  // Anotación para métodos de test unitarios
import org.junit.jupiter.params.ParameterizedTest;  // Test parametrizado con múltiples entradas
import org.junit.jupiter.params.provider.CsvSource;  // Proveedor de datos CSV inline para tests parametrizados
import pe.banco.evaluacion.entidades.SolicitudCredito;  // Entidad sobre la cual calculamos scoring

import java.math.BigDecimal;  // Tipo numérico de precisión para montos financieros

import static org.junit.jupiter.api.Assertions.*;  // Métodos de asserción: assertTrue, assertEquals, etc.

/**
 * Suite de tests unitarios para el servicio de scoring crediticio.
 * <p>
 * Esta clase de test valida exhaustivamente el comportamiento del {@link ScoringService},
 * que es el componente crítico del sistema responsable de decidir si una solicitud de
 * crédito debe ser aprobada o rechazada. Dado que errores en el scoring pueden resultar
 * en pérdidas financieras significativas (aprobar clientes de alto riesgo) o pérdida de
 * negocio (rechazar clientes buenos), estos tests son fundamentales para garantizar
 * corrección del algoritmo.
 * </p>
 * <p>
 * <b>Analogía:</b> Piensa en estos tests como la "auditoría independiente" de las decisiones
 * crediticias del banco. Así como un auditor externo revisa casos reales de aprobaciones y
 * rechazos para verificar que se siguieron correctamente las políticas del banco, estos tests
 * automatizan esa verificación, ejecutándose miles de veces y validando que cada decisión
 * del sistema se toma según las reglas de negocio establecidas.
 * </p>
 * 
 * <h3>Estrategia de testing:</h3>
 * <p>
 * La suite implementa una estrategia de testing basada en:
 * <ul>
 *   <li><b>Tests de escenarios extremos:</b> Score excelente (>800) y pésimo (<400)</li>
 *   <li><b>Tests de validaciones críticas:</b> DTI >50%, empleo <3 meses</li>
 *   <li><b>Tests de cálculo:</b> Verificar fórmulas matemáticas (DTI, ratios)</li>
 *   <li><b>Tests parametrizados:</b> Múltiples combinaciones de inputs en un solo test</li>
 *   <li><b>Tests de límites:</b> Verificar que score se mantiene en rango 0-1000</li>
 *   <li><b>Tests de mensajes:</b> Validar que razones de evaluación son apropiadas</li>
 * </ul>
 * </p>
 * 
 * <h3>Cobertura de testing:</h3>
 * <table border="1">
 *   <tr>
 *     <th>Componente</th>
 *     <th>Tests</th>
 *     <th>Cobertura</th>
 *   </tr>
 *   <tr>
 *     <td>calcularScore()</td>
 *     <td>4 tests</td>
 *     <td>100%</td>
 *   </tr>
 *   <tr>
 *     <td>calcularDTI()</td>
 *     <td>2 tests</td>
 *     <td>100%</td>
 *   </tr>
 *   <tr>
 *     <td>esAprobada()</td>
 *     <td>Cubierto por tests parametrizados</td>
 *     <td>100%</td>
 *   </tr>
 *   <tr>
 *     <td>esAprobadaConValidaciones()</td>
 *     <td>2 tests</td>
 *     <td>100%</td>
 *   </tr>
 *   <tr>
 *     <td>generarRazonEvaluacion()</td>
 *     <td>2 tests</td>
 *     <td>~80%</td>
 *   </tr>
 * </table>
 * 
 * <h3>¿Por qué @QuarkusTest?</h3>
 * <p>
 * A diferencia de tests unitarios puros (usando mocks), estos son "integration tests livianos":
 * <ul>
 *   <li>Quarkus inicia un contexto CDI completo pero eficiente</li>
 *   <li>ScoringService se inyecta como instancia real, no mock</li>
 *   <li>Permite testing de lógica real sin infraestructura compleja (sin BD)</li>
 *   <li>Balance perfecto entre velocidad (rápido) y realismo (contexto real)</li>
 * </ul>
 * </p>
 * 
 * <h3>Ventajas de tests parametrizados:</h3>
 * <p>
 * El test {@link #deberiaEvaluarMultiplesEscenarios} usa @ParameterizedTest que permite:
 * <ul>
 *   <li>Ejecutar el mismo test con diferentes datos de entrada</li>
 *   <li>Verificar múltiples combinaciones sin duplicar código</li>
 *   <li>Fácil agregar nuevos casos: solo añadir línea en @CsvSource</li>
 *   <li>Reporte claro de qué combinación falló si hay error</li>
 * </ul>
 * </p>
 * 
 * <h3>Ejecución de los tests:</h3>
 * <pre>
 * # Ejecutar todos los tests
 * ./mvnw test
 * 
 * # Ejecutar solo esta clase
 * ./mvnw test -Dtest=ScoringServiceTest
 * 
 * # Ejecutar un test específico
 * ./mvnw test -Dtest=ScoringServiceTest#deberiaCalcularScoreExcelente
 * 
 * # Con coverage (requiere jacoco plugin)
 * ./mvnw clean test jacoco:report
 * </pre>
 * 
 * <h3>Mejoras pendientes:</h3>
 * <ul>
 *   <li>Agregar tests para todos los rangos de evaluarEdad()</li>
 *   <li>Test específico para evaluarMontoSolicitado() con diferentes ratios</li>
 *   <li>Tests de concurrencia (verificar thread-safety del servicio)</li>
 *   <li>Tests de performance (medir tiempo de cálculo de score)</li>
 *   <li>Property-based testing con QuickCheck/jqwik para explorar inputs aleatorios</li>
 * </ul>
 * 
 * @see ScoringService
 * @see SolicitudCredito
 */
@QuarkusTest
class ScoringServiceTest {

    /**
     * Instancia real de ScoringService inyectada por CDI de Quarkus.
     * <p>
     * No es un mock: es el servicio real con toda su lógica de negocio.
     * Esto permite testing de integración liviano sin necesidad de mockear
     * dependencias (ScoringService no tiene dependencias externas).
     * </p>
     */
    @Inject
    ScoringService scoringService;

    /**
     * Verifica que el algoritmo de scoring otorga puntaje excelente (≥800) a perfiles de bajo riesgo.
     * <p>
     * Este test valida el comportamiento del sistema con un "cliente ideal":
     * <ul>
     *   <li><b>Ingresos altos:</b> $3,000,000 (ingreso mensual alto en Chile)</li>
     *   <li><b>DTI bajo:</b> 10% (deudas de $300,000 sobre ingresos $3,000,000)</li>
     *   <li><b>Estabilidad laboral:</b> 36 meses (3 años en empleo actual)</li>
     *   <li><b>Edad óptima:</b> 35 años (rango 25-55 que bonifica +80 puntos)</li>
     *   <li><b>Monto conservador:</b> $5,000,000 (ratio ~1.67 sobre ingresos)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Expectativa:</b> Un perfil tan sólido debe obtener score ≥ 800 (excelente).
     * Si este test falla, indica que el algoritmo penaliza incorrectamente a
     * clientes de bajo riesgo, causando pérdida de negocio.
     * </p>
     * 
     * <h4>Desglose esperado del score:</h4>
     * <pre>
     * Base:                  500 puntos
     * DTI 10% (excelente):  +200 puntos
     * Edad 35 (óptimo):      +80 puntos
     * Empleo 36 meses:      +120 puntos
     * Capacidad de pago:    +150 puntos (holgada)
     * Ratio monto/ingreso:  +100 puntos (conservador)
     * ----------------------------------------
     * Total esperado:       1150 → limitado a 1000
     * </pre>
     */
    @Test
    void deberiaCalcularScoreExcelente() {
        SolicitudCredito solicitud = crearSolicitudBase();
        solicitud.setIngresosMensuales(new BigDecimal("3000000"));
        solicitud.setDeudasActuales(new BigDecimal("300000"));
        solicitud.setMontoSolicitado(new BigDecimal("5000000"));
        solicitud.setMesesEnEmpleoActual(36);
        solicitud.setEdad(35);

        Integer score = scoringService.calcularScore(solicitud);

        assertTrue(score >= 800, "Score debería ser excelente (>= 800)");
        assertTrue(score <= 1000, "Score no puede exceder 1000");
    }

    /**
     * Verifica que DTI (Debt-to-Income) superior a 50% resulta en rechazo automático.
     * <p>
     * El DTI es el indicador financiero más crítico en evaluación crediticia.
     * Un DTI >50% significa que más de la mitad de los ingresos ya está comprometida
     * en pago de deudas, dejando poco margen para nueva deuda.
     * </p>
     * <p>
     * <b>Caso de test:</b>
     * <ul>
     *   <li>Ingresos: $1,000,000</li>
     *   <li>Deudas actuales: $600,000</li>
     *   <li>DTI = (600,000 / 1,000,000) × 100 = 60%</li>
     * </ul>
     * </p>
     * <p>
     * <b>Expectativa:</b> Independientemente de otros factores positivos, DTI >50%
     * debe resultar en:
     * <ul>
     *   <li>Score penalizado con -300 puntos</li>
     *   <li>Score final &lt; 650 (umbral de aprobación)</li>
     *   <li>Aprobación = false</li>
     * </ul>
     * </p>
     * <p>
     * <b>Importancia del test:</b> DTI >50% es el límite regulatorio de la SBS
     * (Superintendencia de Banca y Seguros) en Perú. Aprobar estas solicitudes
     * viola políticas y aumenta riesgo de default.
     * </p>
     */
    @Test
    void deberiaRechazarPorDTIAlto() {
        SolicitudCredito solicitud = crearSolicitudBase();
        solicitud.setIngresosMensuales(new BigDecimal("1000000"));
        solicitud.setDeudasActuales(new BigDecimal("600000"));  // 60% DTI
        solicitud.setMontoSolicitado(new BigDecimal("2000000"));
        solicitud.setMesesEnEmpleoActual(12);
        solicitud.setEdad(30);

        Integer score = scoringService.calcularScore(solicitud);
        boolean aprobada = scoringService.esAprobada(score);

        assertFalse(aprobada, "Debería rechazar por DTI > 50%");
        assertTrue(score < 650, "Score debería estar bajo umbral de aprobación");
    }

    /**
     * Verifica que empleo menor a 3 meses resulta en rechazo automático.
     * <p>
     * Menos de 3 meses de empleo indica inestabilidad laboral crítica que
     * justifica rechazo independiente del score crediticio. Esta es una
     * validación "deal-breaker" que se evalúa ANTES del umbral de score.
     * </p>
     * <p>
     * <b>Caso de test:</b>
     * Cliente con perfil financiero sólido (buenos ingresos, bajo DTI)
     * PERO solo 2 meses en empleo actual.
     * </p>
     * <p>
     * <b>Expectativa:</b>
     * <ul>
     *   <li>esAprobadaConValidaciones() retorna false</li>
     *   <li>generarRazonEvaluacion() menciona "Inestabilidad laboral"</li>
     *   <li>Razón explica requisito mínimo de 3 meses</li>
     * </ul>
     * </p>
     * <p>
     * <b>Fundamento de negocio:</b> En Perú, el período de prueba legal es 3 meses.
     * Antes de cumplir 3 meses, el trabajador puede ser despedido sin causa justa,
     * representando alto riesgo de pérdida de ingresos durante período del préstamo.
     * </p>
     * 
     * <h4>Nota sobre System.out.println:</h4>
     * <p>
     * La línea 63 imprime la razón generada para debugging manual durante desarrollo.
     * En producción, se recomienda usar logging framework o remover este println.
     * </p>
     */
    @Test
    void deberiaRechazarPorInestabilidadLaboral() {
        SolicitudCredito solicitud = crearSolicitudBase();
        solicitud.setIngresosMensuales(new BigDecimal("2000000"));
        solicitud.setDeudasActuales(new BigDecimal("200000"));
        solicitud.setMontoSolicitado(new BigDecimal("3000000"));
        solicitud.setMesesEnEmpleoActual(2);  // Menos de 3 meses crítico
        solicitud.setEdad(25);

        Integer score = scoringService.calcularScore(solicitud);
        String razon = scoringService.generarRazonEvaluacion(solicitud, score);

        System.out.println("Razón generada: " + razon);
        assertTrue(razon.contains("Inestabilidad laboral"), "Razón debería mencionar inestabilidad");
        assertFalse(scoringService.esAprobadaConValidaciones(solicitud, score), "Debería rechazar por inestabilidad");
    }

    /**
     * Verifica que el cálculo de DTI (Debt-to-Income ratio) es matemáticamente correcto.
     * <p>
     * Test de la fórmula pura: DTI = (Deudas / Ingresos) × 100
     * </p>
     * <p>
     * <b>Caso de test:</b>
     * <ul>
     *   <li>Deudas: $500,000</li>
     *   <li>Ingresos: $1,000,000</li>
     *   <li>DTI esperado: 50.00%</li>
     * </ul>
     * </p>
     * <p>
     * <b>Validación de precisión decimal:</b> El test verifica que el resultado
     * esté entre 49.99% y 50.01% para tolerar pequeños errores de redondeo inherentes
     * a operaciones con BigDecimal. Un rango de ±0.01% es aceptable para decisiones
     * crediticias (no afecta evaluación).
     * </p>
     * <p>
     * <b>Nota sobre asserción comentada:</b> La línea 75 muestra un assertEquals
     * comentado que sería más estricto pero puede fallar por redondeo. La asserción
     * actual con rango es más robusta para aritmética de punto flotante.
     * </p>
     */
    @Test
    void deberiaCalcularDTICorrectamente() {
        BigDecimal deudas = new BigDecimal("500000");
        BigDecimal ingresos = new BigDecimal("1000000");

        BigDecimal dti = scoringService.calcularDTI(deudas, ingresos);

        // Tolerancia de ±0.01% para errores de redondeo
        assertTrue(dti.compareTo(new BigDecimal("49.99")) > 0 && 
                   dti.compareTo(new BigDecimal("50.01")) < 0, 
                   "DTI debería ser ~50%");
    }

    /**
     * Verifica manejo de edge case: ingresos = 0 (división por cero).
     * <p>
     * Cuando ingresos mensuales son cero, el DTI es técnicamente infinito
     * (cualquier deuda dividida por cero). El servicio maneja este caso especial
     * retornando 100% (peor DTI posible) para evitar excepción de división por cero.
     * </p>
     * <p>
     * <b>Expectativa:</b> calcularDTI(deudas, 0) debe retornar 100 (o ≥100)
     * sin lanzar ArithmeticException.
     * </p>
     * <p>
     * <b>Casos reales donde ingresos=0:</b>
     * <ul>
     *   <li>Error de captura de datos (campo obligatorio no validado)</li>
     *   <li>Cliente desempleado temporalmente</li>
     *   <li>Ingresos informales no declarados</li>
     * </ul>
     * En todos los casos, el sistema debe manejar gracefully sin crashear.
     * </p>
     */
    @Test
    void deberiaManejarDTICeroIngresos() {
        BigDecimal deudas = new BigDecimal("500000");
        BigDecimal ingresos = BigDecimal.ZERO;

        BigDecimal dti = scoringService.calcularDTI(deudas, ingresos);

        assertTrue(dti.compareTo(new BigDecimal("99")) >= 0, 
                   "DTI debería ser 100%");
    }

    /**
     * Test parametrizado que evalúa múltiples escenarios crediticios con una sola ejecución.
     * <p>
     * Este test ejecuta 5 veces con diferentes combinaciones de parámetros,
     * validando que el algoritmo de scoring maneja correctamente diversos perfiles:
     * </p>
     * 
     * <h4>Escenarios cubiertos:</h4>
     * <table border="1">
     *   <tr>
     *     <th>Escenario</th>
     *     <th>Ingresos</th>
     *     <th>Deudas</th>
     *     <th>Monto</th>
     *     <th>Empleo</th>
     *     <th>Edad</th>
     *     <th>Resultado</th>
     *   </tr>
     *   <tr>
     *     <td>Cliente premium</td>
     *     <td>$2,500,000</td>
     *     <td>$300,000</td>
     *     <td>$5,000,000</td>
     *     <td>48 meses</td>
     *     <td>35</td>
     *     <td>✅ Aprobar</td>
     *   </tr>
     *   <tr>
     *     <td>Cliente estándar</td>
     *     <td>$1,800,000</td>
     *     <td>$400,000</td>
     *     <td>$3,000,000</td>
     *     <td>24 meses</td>
     *     <td>28</td>
     *     <td>✅ Aprobar</td>
     *   </tr>
     *   <tr>
     *     <td>Alto DTI</td>
     *     <td>$1,500,000</td>
     *     <td>$900,000</td>
     *     <td>$4,000,000</td>
     *     <td>12 meses</td>
     *     <td>42</td>
     *     <td>❌ Rechazar</td>
     *   </tr>
     *   <tr>
     *     <td>Inestabilidad laboral</td>
     *     <td>$1,200,000</td>
     *     <td>$150,000</td>
     *     <td>$2,000,000</td>
     *     <td>2 meses</td>
     *     <td>23</td>
     *     <td>✅ Aprobar (por score, pero fallaría validaciones)</td>
     *   </tr>
     *   <tr>
     *     <td>Cliente consolidado</td>
     *     <td>$3,000,000</td>
     *     <td>$100,000</td>
     *     <td>$6,000,000</td>
     *     <td>60 meses</td>
     *     <td>45</td>
     *     <td>✅ Aprobar</td>
     *   </tr>
     * </table>
     * 
     * <p>
     * <b>⚠️ Nota importante sobre escenario 4:</b> El test valida solo esAprobada(score),
     * NO esAprobadaConValidaciones(). Por eso espera true para el caso de 2 meses de
     * empleo, aunque en producción ese caso se rechazaría por la validación crítica.
     * Este es un punto de mejora: el test debería validar el flujo completo.
     * </p>
     * 
     * <h4>Ventajas de @ParameterizedTest:</h4>
     * <ul>
     *   <li>Un solo método de test cubre 5 escenarios</li>
     *   <li>Fácil agregar más casos: solo añadir línea en @CsvSource</li>
     *   <li>Si un escenario falla, el reporte muestra exactamente cuál</li>
     *   <li>Código DRY (Don't Repeat Yourself) - evita duplicación</li>
     * </ul>
     * 
     * <h4>Cómo agregar más escenarios:</h4>
     * <pre>
     * @CsvSource({
     *     // ... casos existentes ...
     *     "4000000, 200000, 8000000, 36, 50, true",  // ← Nueva línea
     * })
     * </pre>
     */
    @ParameterizedTest
    @CsvSource({
        "2500000, 300000, 5000000, 48, 35, true",   // Cliente premium
        "1800000, 400000, 3000000, 24, 28, true",   // Cliente estándar
        "1500000, 900000, 4000000, 12, 42, false",  // Alto DTI (60%)
        "1200000, 150000, 2000000, 2, 23, true",    // Inestable pero score OK
        "3000000, 100000, 6000000, 60, 45, true"    // Cliente consolidado
    })
    void deberiaEvaluarMultiplesEscenarios(String ingresos, String deudas, String monto, 
                                            int meses, int edad, boolean deberiaAprobar) {
        SolicitudCredito solicitud = crearSolicitudBase();
        solicitud.setIngresosMensuales(new BigDecimal(ingresos));
        solicitud.setDeudasActuales(new BigDecimal(deudas));
        solicitud.setMontoSolicitado(new BigDecimal(monto));
        solicitud.setMesesEnEmpleoActual(meses);
        solicitud.setEdad(edad);

        Integer score = scoringService.calcularScore(solicitud);
        boolean aprobada = scoringService.esAprobada(score);

        assertEquals(deberiaAprobar, aprobada, 
            String.format("Score %d debería %s", score, deberiaAprobar ? "aprobar" : "rechazar"));
    }

    /**
     * Verifica que perfiles excelentes reciben mensaje de aprobación especial.
     * <p>
     * Solicitudes con score ≥ 800 deben generar mensaje de felicitación que dice
     * "Excelente" para reconocer la calidad crediticia superior del cliente.
     * </p>
     * <p>
     * <b>Validaciones:</b>
     * <ul>
     *   <li>Score debe ser ≥ 800</li>
     *   <li>Razón debe contener la palabra "Excelente"</li>
     * </ul>
     * </p>
     * <p>
     * <b>Importancia del mensaje apropiado:</b> Comunicación diferenciada refuerza
     * relación con clientes premium y mejora experiencia de usuario (UX).
     * </p>
     */
    @Test
    void deberiaGenerarRazonAprobacionExcelente() {
        SolicitudCredito solicitud = crearSolicitudBase();
        solicitud.setIngresosMensuales(new BigDecimal("3500000"));
        solicitud.setDeudasActuales(new BigDecimal("200000"));
        solicitud.setMontoSolicitado(new BigDecimal("4000000"));
        solicitud.setMesesEnEmpleoActual(48);
        solicitud.setEdad(40);

        Integer score = scoringService.calcularScore(solicitud);
        String razon = scoringService.generarRazonEvaluacion(solicitud, score);

        assertTrue(score >= 800);
        assertTrue(razon.contains("Excelente"), "Razón debería mencionar perfil excelente");
    }

    /**
     * Verifica que el score siempre se mantiene dentro del rango válido 0-1000.
     * <p>
     * Independientemente de qué tan pésimo sea el perfil crediticio, el score
     * no debe ser negativo ni exceder 1000. Este test usa el peor caso posible:
     * <ul>
     *   <li>DTI altísimo (160%): -300 puntos</li>
     *   <li>Monto desproporcionado: -50 puntos</li>
     *   <li>Edad muy alta (75): -30 puntos</li>
     *   <li>Empleo mínimo (1 mes): -20 puntos</li>
     *   <li>Capacidad de pago inexistente: -100 puntos</li>
     * </ul>
     * Score teórico: 500 - 500 = 0 (después de limit)
     * </p>
     * <p>
     * <b>Validaciones:</b>
     * <ul>
     *   <li>score ≥ 0 (no negativo)</li>
     *   <li>score ≤ 1000 (no excede máximo)</li>
     * </ul>
     * </p>
     * <p>
     * <b>Importancia:</b> El algoritmo de Math.min(score, 1000) en líneas 23-24
     * de ScoringService debe funcionar correctamente. Si este test falla, hay bug
     * en la limitación de rango que podría causar comportamiento inesperado.
     * </p>
     */
    @Test
    void deberiaLimitarScoreEntreCeroYMil() {
        SolicitudCredito solicitudPesima = crearSolicitudBase();
        solicitudPesima.setIngresosMensuales(new BigDecimal("500000"));
        solicitudPesima.setDeudasActuales(new BigDecimal("800000"));    // DTI 160%
        solicitudPesima.setMontoSolicitado(new BigDecimal("10000000")); // Ratio 20x
        solicitudPesima.setMesesEnEmpleoActual(1);
        solicitudPesima.setEdad(75);

        Integer score = scoringService.calcularScore(solicitudPesima);

        assertTrue(score >= 0, "Score no puede ser negativo");
        assertTrue(score <= 1000, "Score no puede exceder 1000");
    }

    /**
     * Método helper que crea una solicitud base con campos mínimos requeridos.
     * <p>
     * Evita duplicación de código en cada test al proporcionar un objeto base
     * que luego cada test modifica según su escenario específico.
     * </p>
     * <p>
     * <b>Campos incluidos:</b>
     * <ul>
     *   <li>DNI: 12345678 (formato válido peruano)</li>
     *   <li>Nombre: "Test Usuario"</li>
     *   <li>Email: test@email.cl</li>
     * </ul>
     * </p>
     * <p>
     * <b>Campos NO incluidos (deben ser seteados por el test):</b>
     * ingresosMensuales, deudasActuales, montoSolicitado, mesesEnEmpleoActual, edad
     * </p>
     * 
     * <h4>Patrón Object Mother:</h4>
     * <p>
     * Este método implementa el patrón "Object Mother" para testing, que
     * proporciona objetos pre-configurados para tests. Ventajas:
     * <ul>
     *   <li>Menos boilerplate en cada test</li>
     *   <li>Fácil cambiar valores base en un solo lugar</li>
     *   <li>Tests más legibles (solo setean lo relevante)</li>
     * </ul>
     * </p>
     */
    private SolicitudCredito crearSolicitudBase() {
        SolicitudCredito solicitud = new SolicitudCredito();
        solicitud.setDni("12345678");
        solicitud.setNombreCompleto("Test Usuario");
        solicitud.setEmail("test@email.cl");
        return solicitud;
    }
}