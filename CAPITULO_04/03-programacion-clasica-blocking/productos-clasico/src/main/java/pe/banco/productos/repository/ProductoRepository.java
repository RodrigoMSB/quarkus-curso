package pe.banco.productos.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import pe.banco.productos.entity.Producto;
import java.util.List;

/**
 * Repositorio clásico (bloqueante) para gestionar operaciones de persistencia de {@link Producto}.
 * <p>
 * Implementa {@link PanacheRepositoryBase} con operaciones síncronas bloqueantes.
 * Todas las operaciones bloquean el thread actual hasta que la base de datos responde.
 * </p>
 * 
 * <p><strong>Patrón Repository:</strong> Separa la lógica de acceso a datos de la 
 * lógica de negocio, proporcionando una interfaz limpia para operaciones CRUD 
 * y consultas personalizadas.</p>
 * 
 * <p><strong>Diferencia con versión reactiva:</strong></p>
 * <ul>
 *   <li>Retorna tipos síncronos (List, Producto) en lugar de Uni/Multi</li>
 *   <li>Bloquea el thread mientras espera respuesta de BD</li>
 *   <li>Más simple pero menos escalable bajo alta concurrencia</li>
 * </ul>
 * 
 * <p><strong>Scope:</strong> {@link ApplicationScoped} - Una única instancia 
 * compartida durante toda la vida de la aplicación.</p>
 * 
 * @author Curso Quarkus
 * @version 1.0
 * @see PanacheRepositoryBase
 * @see Producto
 */
@ApplicationScoped
public class ProductoRepository implements PanacheRepositoryBase<Producto, Long> {

    /**
     * Busca productos con stock por debajo de un umbral especificado.
     * <p>
     * Útil para identificar productos que requieren reabastecimiento
     * o para alertas de inventario bajo.
     * </p>
     * 
     * <p><strong>Operación bloqueante:</strong> Bloquea el thread actual
     * hasta que la query completa y retorna los resultados.</p>
     * 
     * <p><strong>Ejemplo de uso:</strong></p>
     * <pre>{@code
     * // Buscar productos con menos de 10 unidades
     * List<Producto> productos = repository.findConStockBajo(10);
     * for (Producto p : productos) {
     *     System.out.println("Stock bajo: " + p.nombre);
     * }
     * }</pre>
     * 
     * @param umbral Cantidad mínima de stock para considerar "bajo" (exclusivo)
     * @return Lista de productos con stock menor al umbral.
     *         La lista puede estar vacía si no hay coincidencias.
     * @throws IllegalArgumentException si umbral es negativo
     */
    public List<Producto> findConStockBajo(int umbral) {
        return list("stock < ?1", umbral);
    }

    /**
     * Busca productos cuyo nombre contenga el texto especificado (búsqueda parcial, case-insensitive).
     * <p>
     * La búsqueda utiliza LIKE con comodines (%) al inicio y final, permitiendo
     * coincidencias parciales en cualquier posición del nombre.
     * </p>
     * 
     * <p><strong>Operación bloqueante:</strong> Bloquea el thread actual.</p>
     * 
     * <p><strong>Ejemplos:</strong></p>
     * <ul>
     *   <li>buscarPorNombre("laptop") → encontrará "Laptop Dell", "laptop HP", "LAPTOP Lenovo"</li>
     *   <li>buscarPorNombre("dell") → encontrará "Laptop Dell XPS", "Monitor Dell"</li>
     * </ul>
     * 
     * <p><strong>Ejemplo de uso:</strong></p>
     * <pre>{@code
     * List<Producto> laptops = repository.buscarPorNombre("laptop");
     * System.out.println("Encontrados: " + laptops.size());
     * }</pre>
     * 
     * @param nombre Texto a buscar en el nombre del producto (no debe ser null).
     *               Los espacios en blanco se incluyen en la búsqueda.
     * @return Lista de productos que coinciden con el criterio.
     *         La lista puede estar vacía si no hay coincidencias.
     * @throws NullPointerException si nombre es null
     */
    public List<Producto> buscarPorNombre(String nombre) {
        return list("LOWER(nombre) LIKE LOWER(?1)", "%" + nombre + "%");
    }

    /**
     * Persiste múltiples productos en una única operación (batch insert).
     * <p>
     * Esta operación es más eficiente que persistir productos individualmente
     * cuando se necesita guardar grandes cantidades de datos.
     * </p>
     * 
     * <p><strong>Operación bloqueante:</strong> Bloquea el thread actual hasta
     * que todos los productos han sido persistidos.</p>
     * 
     * <p><strong>Transaccionalidad:</strong> Esta operación debe ejecutarse dentro
     * de un contexto transaccional (típicamente con {@code @Transactional}).</p>
     * 
     * <p><strong>Ejemplo de uso:</strong></p>
     * <pre>{@code
     * List<Producto> productos = Arrays.asList(
     *     new Producto("Laptop", "HP Pavilion", 850.0, 10),
     *     new Producto("Mouse", "Logitech MX Master", 99.0, 50)
     * );
     * 
     * repository.persistirLote(productos);
     * System.out.println("Productos guardados: " + productos.size());
     * }</pre>
     * 
     * @param productos Lista de productos a persistir (no debe ser null ni vacía).
     *                  Cada producto debe tener sus campos obligatorios inicializados.
     * @throws NullPointerException si la lista de productos es null
     * @throws IllegalArgumentException si la lista está vacía
     * @throws jakarta.persistence.PersistenceException si hay errores de validación o BD
     */
    public void persistirLote(List<Producto> productos) {
        persist(productos);
    }
}
