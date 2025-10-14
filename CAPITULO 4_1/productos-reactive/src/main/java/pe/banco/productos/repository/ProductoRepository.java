package pe.banco.productos.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pe.banco.productos.entity.Producto;

import java.util.List;

@ApplicationScoped
public class ProductoRepository implements PanacheRepositoryBase<Producto, Long> {

    /**
     * Buscar productos con stock bajo
     * Reactivo: retorna Uni<List<Producto>>
     */
    public Uni<List<Producto>> findConStockBajo(int umbral) {
        return list("stock < ?1", umbral);
    }

    /**
     * Buscar productos por nombre (búsqueda parcial)
     * Reactivo: retorna Uni<List<Producto>>
     */
    public Uni<List<Producto>> buscarPorNombre(String nombre) {
        return list("LOWER(nombre) LIKE LOWER(?1)", "%" + nombre + "%");
    }

    /**
     * Guardar múltiples productos de forma reactiva
     * Reactivo: retorna Uni<Void>
     */
    public Uni<Void> persistirLote(List<Producto> productos) {
        return persist(productos).replaceWithVoid();
    }
}
