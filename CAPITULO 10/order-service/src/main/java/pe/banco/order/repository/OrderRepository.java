package pe.banco.order.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import pe.banco.order.entity.Order;

import java.util.List;

@ApplicationScoped
public class OrderRepository implements PanacheRepositoryBase<Order, String> {
    
    public List<Order> findByUserId(String userId) {
        return find("userId", userId).list();
    }
}
