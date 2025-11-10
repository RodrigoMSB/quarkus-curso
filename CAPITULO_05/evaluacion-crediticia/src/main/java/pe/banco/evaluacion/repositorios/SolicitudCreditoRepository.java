package pe.banco.evaluacion.repositorios;

import io.quarkus.hibernate.orm.panache.PanacheRepository;  // Repositorio base de Quarkus con operaciones CRUD simplificadas
import jakarta.enterprise.context.ApplicationScoped;  // CDI scope para singleton a nivel de aplicación
import pe.banco.evaluacion.entidades.SolicitudCredito;  // Entidad JPA que este repositorio gestiona
import pe.banco.evaluacion.entidades.SolicitudCredito.EstadoSolicitud;  // Enum de estados de solicitud

import java.util.List;  // Colección estándar de Java para múltiples resultados
import java.util.Optional;  // Contenedor que puede o no contener un valor no-null

/**
 * Repositorio para operaciones de persistencia sobre solicitudes de crédito.
 * <p>
 * Este repositorio implementa el patrón Repository de manera simplificada mediante
 * Quarkus Panache, eliminando la necesidad de escribir implementaciones repetitivas
 * de operaciones CRUD básicas. Panache proporciona automáticamente métodos como
 * persist(), findById(), listAll(), delete(), entre otros.
 * </p>
 * <p>
 * <b>Analogía:</b> Piensa en este repositorio como el "archivero inteligente" del banco.
 * En lugar de buscar manualmente entre miles de carpetas físicas, le dices "dame todas
 * las solicitudes aprobadas" o "busca al cliente con DNI 12345678" y él sabe exactamente
 * cómo y dónde buscar, trayéndote solo lo que necesitas.
 * </p>
 * 
 * <h3>Ventajas del patrón Repository con Panache:</h3>
 * <ul>
 *   <li>Abstracción de la capa de persistencia (cambiar BD no afecta servicios)</li>
 *   <li>Métodos de consulta expresivos y autodocumentados</li>
 *   <li>Queries type-safe que previenen errores en tiempo de compilación</li>
 *   <li>Reutilización de lógica de consulta compleja</li>
 *   <li>Facilita testing con mocks del repositorio</li>
 * </ul>
 * 
 * <h3>Operaciones heredadas de PanacheRepository (ejemplos):</h3>
 * <pre>
 * repository.persist(solicitud);           // Guardar
 * repository.findById(123L);               // Buscar por ID
 * repository.listAll();                     // Listar todas
 * repository.delete(solicitud);            // Eliminar
 * repository.count();                      // Contar total
 * </pre>
 * 
 * <h3>Métodos personalizados en este repositorio:</h3>
 * <p>
 * Cada método personalizado encapsula una query específica del dominio bancario,
 * haciendo el código más legible y mantenible. Compara:
 * </p>
 * <pre>
 * // Sin repositorio (en el servicio):
 * List&lt;SolicitudCredito&gt; aprobadas = entityManager
 *     .createQuery("SELECT s FROM SolicitudCredito s WHERE s.aprobada = true", SolicitudCredito.class)
 *     .getResultList();
 * 
 * // Con repositorio:
 * List&lt;SolicitudCredito&gt; aprobadas = repository.buscarAprobadas();
 * </pre>
 * 
 * @see PanacheRepository
 * @see SolicitudCredito
 */
@ApplicationScoped
public class SolicitudCreditoRepository implements PanacheRepository<SolicitudCredito> {

    /**
     * Busca una solicitud de crédito por el DNI del solicitante.
     * <p>
     * Retorna Optional porque puede o no existir una solicitud con ese DNI.
     * Esto fuerza al código cliente a manejar explícitamente ambos casos,
     * evitando NullPointerException.
     * </p>
     * <p>
     * <b>Caso de uso:</b> Antes de crear una nueva solicitud, verificar si el cliente
     * ya tiene una solicitud activa para evitar duplicados o aplicar reglas de negocio
     * (ej: "máximo 1 solicitud por mes por cliente").
     * </p>
     * <p>
     * <b>Nota de diseño:</b> Aunque DNI es único por persona, un cliente podría tener
     * múltiples solicitudes históricas. Este método retorna solo la primera encontrada.
     * Para casos donde necesites el historial completo, considera crear un método
     * buscarHistorialPorDni() que retorne List.
     * </p>
     * 
     * <h4>Ejemplo de uso:</h4>
     * <pre>
     * Optional&lt;SolicitudCredito&gt; solicitudExistente = repository.buscarPorDni("12345678");
     * if (solicitudExistente.isPresent()) {
     *     throw new BusinessException("Cliente ya tiene una solicitud activa");
     * }
     * </pre>
     *
     * @param dni Documento Nacional de Identidad (8 dígitos) del solicitante
     * @return Optional conteniendo la solicitud si existe, Optional.empty() si no
     * @see Optional#isPresent()
     * @see Optional#orElse(Object)
     * @see Optional#orElseThrow()
     */
    public Optional<SolicitudCredito> buscarPorDni(String dni) {
        return find("dni", dni).firstResultOptional();
    }

    /**
     * Busca todas las solicitudes asociadas a un correo electrónico.
     * <p>
     * Retorna una lista porque un mismo email podría tener múltiples solicitudes
     * en el tiempo (historial crediticio del cliente). Si bien email es único
     * en la entidad (unique = true), esto previene registros duplicados simultáneos,
     * pero no impide que el mismo email aparezca en solicitudes antiguas.
     * </p>
     * <p>
     * <b>Caso de uso:</b> Análisis de comportamiento crediticio del cliente,
     * detección de patrones de solicitud, generación de reportes de actividad.
     * </p>
     * <p>
     * <b>Ejemplo:</b> Un cliente pudo haber solicitado un crédito en 2020 (rechazado),
     * otro en 2023 (aprobado), y está solicitando uno nuevo ahora. Este método
     * retornaría las 3 solicitudes.
     * </p>
     *
     * @param email Correo electrónico del solicitante
     * @return Lista de solicitudes (puede estar vacía si no hay coincidencias)
     */
    public List<SolicitudCredito> buscarPorEmail(String email) {
        return list("email", email);
    }

    /**
     * Busca todas las solicitudes que tienen un estado específico.
     * <p>
     * Método fundamental para operaciones administrativas y dashboards,
     * permitiendo filtrar solicitudes por su posición en el flujo de trabajo.
     * </p>
     * <p>
     * <b>Casos de uso comunes:</b>
     * <ul>
     *   <li>Dashboard de administración: "Mostrar todas las solicitudes EN_PROCESO"</li>
     *   <li>Alertas: "Notificar si hay solicitudes PENDIENTES por más de 24 horas"</li>
     *   <li>Reportería: "Generar informe mensual de solicitudes RECHAZADAS"</li>
     *   <li>Auditoría: "Revisar todas las que REQUIERE_ANALISIS"</li>
     * </ul>
     * </p>
     * 
     * <h4>Ejemplo de uso:</h4>
     * <pre>
     * List&lt;SolicitudCredito&gt; enProceso = repository.buscarPorEstado(EstadoSolicitud.EN_PROCESO);
     * if (enProceso.size() &gt; 100) {
     *     alertService.notificarCargaAlta("Más de 100 solicitudes en proceso");
     * }
     * </pre>
     *
     * @param estado Estado de las solicitudes a buscar
     * @return Lista de solicitudes con ese estado (nunca null, puede estar vacía)
     * @see EstadoSolicitud
     */
    public List<SolicitudCredito> buscarPorEstado(EstadoSolicitud estado) {
        return list("estado", estado);
    }

    /**
     * Busca todas las solicitudes aprobadas del sistema.
     * <p>
     * Método de conveniencia que encapsula la lógica de búsqueda por aprobación.
     * Es más expresivo que llamar directamente a list("aprobada", true).
     * </p>
     * <p>
     * <b>Casos de uso:</b>
     * <ul>
     *   <li>Generación de cartera de créditos activos</li>
     *   <li>Cálculo de exposición crediticia total del banco</li>
     *   <li>Estadísticas de tasa de aprobación</li>
     *   <li>Análisis de perfil de clientes aprobados</li>
     * </ul>
     * </p>
     * <p>
     * <b>Nota importante:</b> aprobada = true no significa necesariamente estado = APROBADA.
     * Una solicitud podría estar aprobada pero en estado EN_PROCESO si aún no se ha
     * actualizado el estado final. En producción, considera validar ambos campos
     * o normalizar la lógica para que aprobada = true implique estado = APROBADA.
     * </p>
     *
     * @return Lista de todas las solicitudes aprobadas
     */
    public List<SolicitudCredito> buscarAprobadas() {
        return list("aprobada", true);
    }

    /**
     * Busca todas las solicitudes rechazadas del sistema.
     * <p>
     * Complemento del método buscarAprobadas(), permite análisis de rechazo
     * para mejorar políticas crediticias y scoring.
     * </p>
     * <p>
     * <b>Casos de uso:</b>
     * <ul>
     *   <li>Análisis de causas de rechazo más frecuentes</li>
     *   <li>Calibración del modelo de scoring (¿rechazamos demasiado/poco?)</li>
     *   <li>Identificación de segmentos de mercado no atendidos</li>
     *   <li>Reportes de cumplimiento regulatorio</li>
     * </ul>
     * </p>
     * <p>
     * <b>Insight de negocio:</b> Si la tasa de rechazo es muy alta (&gt;70%),
     * podría indicar que los criterios son demasiado estrictos y se está perdiendo
     * negocio. Si es muy baja (&lt;10%), podría indicar exceso de riesgo.
     * </p>
     *
     * @return Lista de todas las solicitudes rechazadas
     */
    public List<SolicitudCredito> buscarRechazadas() {
        return list("aprobada", false);
    }

    /**
     * Cuenta cuántas solicitudes existen con un estado específico.
     * <p>
     * Operación de agregación optimizada que no carga entidades en memoria,
     * solo ejecuta COUNT en base de datos. Más eficiente que llamar a
     * buscarPorEstado(estado).size() cuando solo necesitas el número.
     * </p>
     * <p>
     * <b>Casos de uso:</b>
     * <ul>
     *   <li>Métricas en tiempo real para dashboards</li>
     *   <li>Validaciones de carga: "Si hay más de X solicitudes pendientes, alertar"</li>
     *   <li>KPIs operacionales: tiempo promedio de procesamiento por estado</li>
     *   <li>Balanceo de carga: distribuir trabajo según cantidad de solicitudes</li>
     * </ul>
     * </p>
     * 
     * <h4>Comparación de performance:</h4>
     * <pre>
     * // ❌ Menos eficiente - carga todas las entidades
     * long count = repository.buscarPorEstado(estado).size();
     * 
     * // ✅ Más eficiente - solo ejecuta COUNT en BD
     * long count = repository.contarPorEstado(estado);
     * </pre>
     *
     * @param estado Estado por el cual contar
     * @return Cantidad de solicitudes con ese estado
     */
    public long contarPorEstado(EstadoSolicitud estado) {
        return count("estado", estado);
    }

    /**
     * Busca solicitudes cuyo score crediticio sea igual o superior al especificado.
     * <p>
     * Permite filtrar clientes por calidad crediticia, útil para:
     * <ul>
     *   <li>Campañas de marketing dirigidas a clientes premium (score alto)</li>
     *   <li>Identificar clientes elegibles para productos especiales</li>
     *   <li>Análisis de distribución de scores en la cartera</li>
     *   <li>Refinanciamiento: clientes con score mejorado desde solicitud original</li>
     * </ul>
     * </p>
     * <p>
     * <b>Ejemplo de negocio:</b> El banco podría ofrecer una línea de crédito
     * preferencial con mejor tasa de interés solo a clientes con score ≥ 800.
     * Este método facilita identificar esos clientes elegibles.
     * </p>
     * 
     * <h4>Uso de operador de comparación en Panache:</h4>
     * <pre>
     * // La consulta "scoreCrediticio >= ?1" se traduce a:
     * // WHERE s.scoreCrediticio >= :scoreMinimo
     * // El ?1 es un placeholder posicional para el parámetro
     * </pre>
     * 
     * <h4>Ejemplo de uso:</h4>
     * <pre>
     * // Encontrar clientes elite para oferta especial
     * List&lt;SolicitudCredito&gt; clientesElite = repository.buscarPorScoreMinimo(800);
     * 
     * for (SolicitudCredito solicitud : clientesElite) {
     *     emailService.enviarOfertaPreferencial(solicitud.getEmail());
     * }
     * </pre>
     *
     * @param scoreMinimo Score crediticio mínimo (inclusivo) para el filtro
     * @return Lista de solicitudes con score igual o superior (nunca null)
     */
    public List<SolicitudCredito> buscarPorScoreMinimo(int scoreMinimo) {
        return list("scoreCrediticio >= ?1", scoreMinimo);
    }
}