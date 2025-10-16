package pe.banco.clientes.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import pe.banco.clientes.entity.Cliente;
import java.util.List;

/**
 * Repositorio para gestionar operaciones de persistencia de {@link Cliente}.
 * <p>
 * Implementa {@link PanacheRepositoryBase} proporcionando una capa de abstracción
 * para operaciones CRUD y consultas personalizadas sobre la entidad Cliente.
 * Todas las operaciones son <strong>bloqueantes</strong> (usa JDBC tradicional).
 * </p>
 * 
 * <p><strong>Patrón Repository:</strong> Separa la lógica de acceso a datos de la 
 * lógica de negocio, proporcionando una interfaz limpia para la persistencia.</p>
 * 
 * <p><strong>Scope:</strong> {@link ApplicationScoped} - Una única instancia 
 * compartida durante toda la vida de la aplicación, inyectable con CDI.</p>
 * 
 * <p><strong>⚠️ IMPORTANTE - Datos Cifrados:</strong></p>
 * <p>
 * Los campos {@code numeroTarjeta} y {@code email} de la entidad Cliente están
 * <strong>cifrados en la base de datos</strong>. Las búsquedas por estos campos
 * deben considerar esta característica:
 * </p>
 * <ul>
 *   <li>Búsquedas por igualdad exacta funcionan (comparación de texto cifrado)</li>
 *   <li>Búsquedas con LIKE, BETWEEN, > , < NO funcionan sobre datos cifrados</li>
 *   <li>Los índices sobre campos cifrados son menos eficientes</li>
 * </ul>
 * 
 * <p><strong>Transaccionalidad:</strong></p>
 * <p>
 * Operaciones de escritura (persist, update, delete) deben ejecutarse dentro de
 * un contexto transaccional. Usar {@code @Transactional} en el servicio o resource.
 * </p>
 * 
 * <p><strong>Ejemplo de uso desde un servicio:</strong></p>
 * <pre>{@code
 * @ApplicationScoped
 * public class ClienteService {
 *     @Inject
 *     ClienteRepository repository;
 *     
 *     @Inject
 *     CryptoService cryptoService;
 *     
 *     public Cliente buscarPorTarjeta(String numeroTarjetaPlano) {
 *         // Cifrar el número antes de buscar
 *         String numeroTarjetaCifrado = cryptoService.cifrar(numeroTarjetaPlano);
 *         List<Cliente> clientes = repository.buscarPorTarjeta(numeroTarjetaCifrado);
 *         return clientes.isEmpty() ? null : clientes.get(0);
 *     }
 * }
 * }</pre>
 * 
 * @author Curso Quarkus - Capítulo 4.2
 * @version 1.0
 * @see Cliente
 * @see PanacheRepositoryBase
 * @see pe.banco.clientes.service.CryptoService
 */
@ApplicationScoped
public class ClienteRepository implements PanacheRepositoryBase<Cliente, Long> {

    /**
     * Busca clientes por número de tarjeta cifrado.
     * <p>
     * ⚠️ <strong>CRÍTICO:</strong> Este método busca por el <strong>valor cifrado</strong>
     * del número de tarjeta, NO por el valor en texto plano.
     * </p>
     * 
     * <p><strong>Flujo correcto de uso:</strong></p>
     * <pre>{@code
     * // 1. Usuario proporciona número de tarjeta en texto plano
     * String numeroPlano = "4532-1234-5678-9012";
     * 
     * // 2. Cifrar el número ANTES de buscar
     * String numeroCifrado = cryptoService.cifrar(numeroPlano);
     * 
     * // 3. Buscar con el valor cifrado
     * List<Cliente> clientes = repository.buscarPorTarjeta(numeroCifrado);
     * }</pre>
     * 
     * <p><strong>¿Por qué funciona la búsqueda sobre datos cifrados?</strong></p>
     * <ul>
     *   <li>Mismo texto plano + misma clave = mismo texto cifrado (determinista)</li>
     *   <li>AES-GCM con mismo nonce produce mismo output (en este caso)</li>
     *   <li>La BD compara strings cifrados: "AebqJ3..." == "AebqJ3..." → true</li>
     * </ul>
     * 
     * <p><strong>⚠️ Limitaciones:</strong></p>
     * <ul>
     *   <li>NO funciona con búsquedas parciales (LIKE '%1234%')</li>
     *   <li>NO funciona con rangos (> , < , BETWEEN)</li>
     *   <li>Índices menos eficientes que sobre texto plano</li>
     *   <li>Si cambia la clave de cifrado, las búsquedas fallan hasta recifrar todo</li>
     * </ul>
     * 
     * <p><strong>Alternativa para búsquedas más complejas:</strong></p>
     * <p>
     * Si necesitas búsquedas parciales o por patrón, considera:
     * </p>
     * <ol>
     *   <li>Agregar columna con hash del número para búsquedas rápidas</li>
     *   <li>Agregar columna con últimos 4 dígitos en texto plano (enmascarado)</li>
     *   <li>Usar tokenización en lugar de cifrado para ciertos casos de uso</li>
     * </ol>
     * 
     * <p><strong>Ejemplo de resultado:</strong></p>
     * <pre>{@code
     * // Si existe un cliente con ese número de tarjeta cifrado
     * List<Cliente> resultado = [Cliente{id=1, nombre="Juan Pérez", ...}]
     * 
     * // Si no existe
     * List<Cliente> resultado = []
     * }</pre>
     * 
     * <p><strong>Seguridad:</strong></p>
     * <ul>
     *   <li>El parámetro {@code numeroTarjeta} ya debe estar cifrado antes de llegar aquí</li>
     *   <li>NO exponer este método directamente en endpoints REST</li>
     *   <li>Siempre usar a través de un servicio que maneje el cifrado</li>
     *   <li>Logs NO deben mostrar el parámetro sin enmascarar</li>
     * </ul>
     * 
     * <p><strong>Performance:</strong></p>
     * <ul>
     *   <li>Consulta simple con índice en {@code numero_tarjeta}</li>
     *   <li>Tiempo típico: 5-20ms (depende del índice y tamaño de tabla)</li>
     *   <li>Escalabilidad: O(log n) con índice, O(n) sin índice</li>
     * </ul>
     * 
     * <p><strong>Casos de uso:</strong></p>
     * <ul>
     *   <li>Validación de tarjeta al procesar pago</li>
     *   <li>Búsqueda de cliente por tarjeta en atención al cliente</li>
     *   <li>Detección de duplicados al registrar nuevas tarjetas</li>
     * </ul>
     * 
     * @param numeroTarjeta Número de tarjeta <strong>YA CIFRADO</strong> con AES-256-GCM.
     *                      Debe ser el resultado de {@code cryptoService.cifrar(numeroPlano)}.
     *                      No debe ser null.
     * @return Lista de clientes que tienen ese número de tarjeta cifrado.
     *         Típicamente debería retornar 0 o 1 cliente (si hay constraint UNIQUE),
     *         pero retorna List por flexibilidad. Lista vacía si no hay coincidencias.
     * @throws NullPointerException si numeroTarjeta es null
     * @throws jakarta.persistence.PersistenceException si hay error en la consulta SQL
     */
    public List<Cliente> buscarPorTarjeta(String numeroTarjeta) {
        return list("numeroTarjeta", numeroTarjeta);
    }
}