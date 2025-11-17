package pe.banco.inventory.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(unique = true, nullable = false)
    public String productCode;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public Integer stock;

    @Column(nullable = false)
    public Integer reservedStock = 0;

    @Column(nullable = false)
    public Double price;

    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    public LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Métodos de negocio
    public boolean canReserve(Integer quantity) {
        return (stock - reservedStock) >= quantity;
    }

    public void reserve(Integer quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalStateException("Stock insuficiente para reservar");
        }
        this.reservedStock += quantity;
    }

    public void confirmReservation(Integer quantity) {
        this.stock -= quantity;
        this.reservedStock -= quantity;
    }

    public void cancelReservation(Integer quantity) {
        this.reservedStock -= quantity;
    }
}
