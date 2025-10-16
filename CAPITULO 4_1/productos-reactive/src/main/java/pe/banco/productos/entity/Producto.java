package pe.banco.productos.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;

/**
 * Entidad JPA que representa un producto en el sistema.
 * <p>
 * Extiende de {@link PanacheEntity} para heredar funcionalidades de Panache Reactive,
 * incluyendo el ID autogenerado y métodos de persistencia reactiva.
 * </p>
 * 
 * <p><strong>Campos obligatorios:</strong></p>
 * <ul>
 *   <li>nombre: Identificador del producto</li>
 *   <li>precio: Valor monetario (debe ser positivo)</li>
 *   <li>stock: Cantidad disponible (debe ser no negativo)</li>
 * </ul>
 * 
 * @author Curso Quarkus
 * @version 1.0
 * @see PanacheEntity
 */
@Entity
public class Producto extends PanacheEntity {

    /**
     * Nombre del producto.
     * <p>
     * Campo obligatorio que identifica al producto en el sistema.
     * </p>
     */
    @Column(nullable = false)
    public String nombre;

    /**
     * Descripción detallada del producto.
     * <p>
     * Campo opcional que proporciona información adicional sobre el producto.
     * Puede ser null.
     * </p>
     */
    public String descripcion;

    /**
     * Precio unitario del producto.
     * <p>
     * Campo obligatorio que representa el valor monetario del producto.
     * Se espera que sea un valor positivo.
     * </p>
     */
    @Column(nullable = false)
    public Double precio;

    /**
     * Cantidad disponible en inventario.
     * <p>
     * Campo obligatorio que indica el stock actual del producto.
     * Debe ser un valor no negativo (0 o mayor).
     * </p>
     */
    @Column(nullable = false)
    public Integer stock;

    /**
     * Constructor por defecto requerido por JPA.
     * <p>
     * Este constructor es necesario para que Hibernate pueda instanciar
     * la entidad al recuperarla de la base de datos.
     * </p>
     */
    public Producto() {
    }

    /**
     * Constructor con parámetros para inicializar todos los campos del producto.
     * 
     * @param nombre Nombre del producto (no debe ser null)
     * @param descripcion Descripción opcional del producto (puede ser null)
     * @param precio Precio unitario del producto (debe ser positivo)
     * @param stock Cantidad disponible en inventario (debe ser no negativo)
     */
    public Producto(String nombre, String descripcion, Double precio, Integer stock) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.stock = stock;
    }
}