package pe.banco.order.dto;

public class ProductInfoDTO {
    public Long id;
    public String productCode;
    public String name;
    public Integer stock;
    public Integer availableStock;
    public Double price;
    
    // Constructor sin argumentos para Jackson
    public ProductInfoDTO() {
    }
}
