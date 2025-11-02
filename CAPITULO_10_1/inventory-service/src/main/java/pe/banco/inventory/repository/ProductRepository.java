package pe.banco.inventory.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import pe.banco.inventory.entity.Product;

import java.util.Optional;

@ApplicationScoped
public class ProductRepository implements PanacheRepository<Product> {
    
    public Optional<Product> findByProductCode(String productCode) {
        return find("productCode", productCode).firstResultOptional();
    }
}
