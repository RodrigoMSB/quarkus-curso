package pe.banco.inventory.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import pe.banco.inventory.dto.ProductDTO;
import pe.banco.inventory.dto.ReservationRequest;
import pe.banco.inventory.dto.ReservationResponse;
import pe.banco.inventory.entity.Product;
import pe.banco.inventory.repository.ProductRepository;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class InventoryService {

    @Inject
    ProductRepository productRepository;

    public List<ProductDTO> getAllProducts() {
        return productRepository.listAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ProductDTO getProductByCode(String productCode) {
        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productCode));
        return toDTO(product);
    }

    @Transactional
    public ReservationResponse reserveStock(ReservationRequest request) {
        try {
            Product product = productRepository.findByProductCode(request.productCode)
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + request.productCode));

            if (!product.canReserve(request.quantity)) {
                return ReservationResponse.failure(request.orderId, 
                    "Stock insuficiente. Disponible: " + (product.stock - product.reservedStock));
            }

            product.reserve(request.quantity);
            productRepository.persist(product);

            return ReservationResponse.success(request.orderId, request.productCode, request.quantity);
        } catch (Exception e) {
            return ReservationResponse.failure(request.orderId, "Error al reservar: " + e.getMessage());
        }
    }

    @Transactional
    public void confirmReservation(String orderId, String productCode, Integer quantity) {
        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productCode));
        
        product.confirmReservation(quantity);
        productRepository.persist(product);
    }

    @Transactional
    public void cancelReservation(String orderId, String productCode, Integer quantity) {
        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productCode));
        
        product.cancelReservation(quantity);
        productRepository.persist(product);
    }

    private ProductDTO toDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.id = product.id;
        dto.productCode = product.productCode;
        dto.name = product.name;
        dto.stock = product.stock;
        dto.availableStock = product.stock - product.reservedStock;
        dto.price = product.price;
        return dto;
    }
}
