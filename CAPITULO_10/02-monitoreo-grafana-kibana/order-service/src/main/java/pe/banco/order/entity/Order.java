package pe.banco.order.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public String id;

    @Column(nullable = false)
    public String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public OrderStatus status = OrderStatus.PENDING;

    @Column(nullable = false)
    public Double totalAmount = 0.0;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<OrderItem> items = new ArrayList<>();

    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    public LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void calculateTotal() {
        this.totalAmount = items.stream()
                .mapToDouble(item -> item.price * item.quantity)
                .sum();
    }

    public enum OrderStatus {
        PENDING,           // Orden creada
        INVENTORY_RESERVED,// Inventario reservado
        PAYMENT_PROCESSING,// Procesando pago
        COMPLETED,         // Orden completada
        CANCELLED,         // Orden cancelada
        FAILED            // Orden fallida
    }
}
