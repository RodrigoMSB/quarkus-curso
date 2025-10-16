package pe.banco.productos.dto;

/**
 * Data Transfer Object (DTO) para recibir datos de productos en peticiones HTTP.
 * <p>
 * Esta clase se utiliza para transportar información desde el cliente hacia el servidor,
 * típicamente en operaciones POST (crear) y PUT (actualizar). Desacopla la capa de
 * presentación de la capa de persistencia.
 * </p>
 * 
 * <p><strong>Ventajas del patrón DTO:</strong></p>
 * <ul>
 *   <li>Evita exponer la entidad JPA directamente al cliente</li>
 *   <li>Permite validaciones específicas de entrada</li>
 *   <li>Control sobre qué campos pueden ser modificados</li>
 *   <li>Facilita versionado de API sin afectar el modelo de datos</li>
 * </ul>
 * 
 * <p><strong>Ejemplo de uso en JSON:</strong></p>
 * <pre>{@code
 * {
 *   "nombre": "Laptop Dell XPS 15",
 *   "descripcion": "Laptop profesional con pantalla 4K",
 *   "precio": 1299.99,
 *   "stock": 15
 * }
 * }</pre>
 * 
 * <p><strong>Nota:</strong> Los campos son públicos para facilitar la serialización/deserialización
 * automática con Jackson (REST). En producción, considera agregar validaciones con
 * {@code jakarta.validation} (ej: @NotNull, @Min, @Size).</p>
 * 
 * @author Curso Quarkus
 * @version 1.0
 * @see pe.banco.productos.entity.Producto
 */
public class ProductoRequest {

    /**
     * Nombre del producto.
     * <p>
     * Identifica al producto de forma legible para humanos.
     * Se recomienda que sea único o descriptivo.
     * </p>
     * 
     * <p><strong>Validaciones sugeridas:</strong></p>
     * <ul>
     *   <li>No debe ser null ni vacío</li>
     *   <li>Longitud máxima: 255 caracteres</li>
     *   <li>Solo caracteres alfanuméricos y espacios</li>
     * </ul>
     */
    public String nombre;

    /**
     * Descripción detallada del producto.
     * <p>
     * Campo opcional que proporciona información adicional sobre
     * características, especificaciones o detalles del producto.
     * </p>
     * 
     * <p><strong>Validaciones sugeridas:</strong></p>
     * <ul>
     *   <li>Puede ser null</li>
     *   <li>Longitud máxima: 1000 caracteres</li>
     * </ul>
     */
    public String descripcion;

    /**
     * Precio unitario del producto.
     * <p>
     * Representa el valor monetario del producto en la moneda del sistema.
     * </p>
     * 
     * <p><strong>Validaciones sugeridas:</strong></p>
     * <ul>
     *   <li>No debe ser null</li>
     *   <li>Debe ser mayor que 0 (ej: @Min(0.01))</li>
     *   <li>Máximo 2 decimales para centavos</li>
     * </ul>
     * 
     * <p><strong>Ejemplo:</strong> 1299.99 representa $1,299.99</p>
     */
    public Double precio;

    /**
     * Cantidad disponible en inventario.
     * <p>
     * Indica cuántas unidades del producto están disponibles para venta.
     * </p>
     * 
     * <p><strong>Validaciones sugeridas:</strong></p>
     * <ul>
     *   <li>No debe ser null</li>
     *   <li>Debe ser mayor o igual a 0 (ej: @Min(0))</li>
     *   <li>Valor 0 indica producto agotado</li>
     * </ul>
     */
    public Integer stock;

    /**
     * Constructor por defecto requerido para deserialización JSON.
     * <p>
     * Jackson (biblioteca de serialización) requiere un constructor sin argumentos
     * para crear instancias al convertir JSON a objetos Java.
     * </p>
     */
    public ProductoRequest() {
    }
}