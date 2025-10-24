package pe.banco.order.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "order_items")
public class OrderItem extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    public Order order;

    @Column(nullable = false)
    public String productCode;

    @Column(nullable = false)
    public String productName;

    @Column(nullable = false)
    public Integer quantity;

    @Column(nullable = false)
    public Double price;
}
