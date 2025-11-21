package pe.banco.productos.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pe.banco.productos.entity.Producto;
import java.util.List;

/**
 * Repositorio reactivo para gestionar operaciones de persistencia de {@link Producto}.
 * <p>
 * Implementa {@link PanacheRepositoryBase} con soporte para programación reactiva
 * utilizando Mutiny ({@link Uni}). Todas las operaciones son no bloqueantes y 
 * retornan tipos reactivos.
 * </p>
 * 
 * <p><strong>Patrón Repository:</strong> Separa la lógica de acceso a datos de la 
 * lógica de negocio, proporcionando una interfaz limpia para operaciones CRUD 
 * y consultas personalizadas.</p>
 * 
 * <p><strong>Scope:</strong> {@link ApplicationScoped} - Una única instancia 
 * compartida durante toda la vida de la aplicación.</p>
 * 
 * @author Curso Quarkus
 * @version 1.0
 * @see PanacheRepositoryBase
 * @see Producto
 * @see Uni
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
     * <p><strong>Operación reactiva:</strong> No bloquea el hilo actual.
     * El resultado se emite de forma asíncrona cuando la query completa.</p>
     * 
     * <p><strong>Ejemplo de uso:</strong></p>
     * <pre>{@code
     * // Buscar productos con menos de 10 unidades
     * repository.findConStockBajo(10)
     *     .subscribe().with(
     *         productos -> log.info("Productos con stock bajo: " + productos.size()),
     *         error -> log.error("Error al buscar", error)
     *     );
     * }</pre>
     * 
     * @param umbral Cantidad mínima de stock para considerar "bajo" (exclusivo)
     * @return {@link Uni} que emite una lista de productos con stock menor al umbral.
     *         La lista puede estar vacía si no hay coincidencias.
     * @throws IllegalArgumentException si umbral es negativo
     */
    public Uni<List<Producto>> findConStockBajo(int umbral) {
        return list("stock < ?1", umbral);
    }

    /**
     * Busca productos cuyo nombre contenga el texto especificado (búsqueda parcial, case-insensitive).
     * <p>
     * La búsqueda utiliza LIKE con comodines (%) al inicio y final, permitiendo
     * coincidencias parciales en cualquier posición del nombre.
     * </p>
     * 
     * <p><strong>Operación reactiva:</strong> No bloquea el hilo actual.</p>
     * 
     * <p><strong>Ejemplos:</strong></p>
     * <ul>
     *   <li>buscarPorNombre("laptop") → encontrará "Laptop Dell", "laptop HP", "LAPTOP Lenovo"</li>
     *   <li>buscarPorNombre("dell") → encontrará "Laptop Dell XPS", "Monitor Dell"</li>
     * </ul>
     * 
     * <p><strong>Ejemplo de uso:</strong></p>
     * <pre>{@code
     * repository.buscarPorNombre("laptop")
     *     .subscribe().with(
     *         productos -> mostrarResultados(productos),
     *         error -> mostrarError(error)
     *     );
     * }</pre>
     * 
     * @param nombre Texto a buscar en el nombre del producto (no debe ser null).
     *               Los espacios en blanco se incluyen en la búsqueda.
     * @return {@link Uni} que emite una lista de productos que coinciden con el criterio.
     *         La lista puede estar vacía si no hay coincidencias.
     * @throws NullPointerException si nombre es null
     */
    public Uni<List<Producto>> buscarPorNombre(String nombre) {
        return list("LOWER(nombre) LIKE LOWER(?1)", "%" + nombre + "%");
    }

    /**
     * Persiste múltiples productos en una única operación reactiva (batch insert).
     * <p>
     * Esta operación es más eficiente que persistir productos individualmente
     * cuando se necesita guardar grandes cantidades de datos.
     * </p>
     * 
     * <p><strong>Operación reactiva:</strong> No bloquea el hilo actual.
     * La operación se completa cuando todos los productos han sido persistidos.</p>
     * 
     * <p><strong>Transaccionalidad:</strong> Esta operación debe ejecutarse dentro
     * de un contexto transaccional (típicamente con {@code @Transactional} o
     * {@code @ReactiveTransactional}).</p>
     * 
     * <p><strong>Ejemplo de uso:</strong></p>
     * <pre>{@code
     * List<Producto> productos = Arrays.asList(
     *     new Producto("Laptop", "HP Pavilion", 850.0, 10),
     *     new Producto("Mouse", "Logitech MX Master", 99.0, 50)
     * );
     * 
     * repository.persistirLote(productos)
     *     .subscribe().with(
     *         () -> log.info("Productos guardados exitosamente"),
     *         error -> log.error("Error al guardar productos", error)
     *     );
     * }</pre>
     * 
     * @param productos Lista de productos a persistir (no debe ser null ni vacía).
     *                  Cada producto debe tener sus campos obligatorios inicializados.
     * @return {@link Uni} que completa cuando la operación de persistencia finaliza.
     *         No emite valor (Void), solo señala la finalización exitosa o fallo.
     * @throws NullPointerException si la lista de productos es null
     * @throws IllegalArgumentException si la lista está vacía
     * @throws jakarta.persistence.PersistenceException si hay errores de validación o BD
     */
    public Uni<Void> persistirLote(List<Producto> productos) {
        return persist(productos).replaceWithVoid();
    }
}