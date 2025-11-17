package pe.banco.payment.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import pe.banco.payment.entity.Payment;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PaymentRepository implements PanacheRepository<Payment> {
    
    public Optional<Payment> findByOrderId(String orderId) {
        return find("orderId", orderId).firstResultOptional();
    }
    
    public List<Payment> findByUserId(String userId) {
        return find("userId", userId).list();
    }
}
