package pe.banco.productos.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;

@Entity
public class Producto extends PanacheEntity {

    @Column(nullable = false)
    public String nombre;

    public String descripcion;

    @Column(nullable = false)
    public Double precio;

    @Column(nullable = false)
    public Integer stock;

    public Producto() {
    }

    public Producto(String nombre, String descripcion, Double precio, Integer stock) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.stock = stock;
    }
}
