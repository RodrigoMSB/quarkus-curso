package pe.banco.payment.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String orderId;

    @Column(nullable = false)
    public String userId;

    @Column(nullable = false)
    public Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "payment_method")
    public String paymentMethod;

    @Column(name = "transaction_id")
    public String transactionId;

    @Column(name = "created_at")
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    public LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum PaymentStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        REFUNDED
    }
}
