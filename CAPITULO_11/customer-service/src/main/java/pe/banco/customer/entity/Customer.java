package pe.banco.customer.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad Customer - Patrón Active Record con Panache
 * 
 * Demuestra:
 * - Hibernate ORM con Panache (Capítulo 4)
 * - Bean Validation (Capítulo 5)
 * - Campos cifrados con @Encrypted para Google Tink (Capítulo 4)
 * - Campos marcados para Always Encrypted (simulado)
 */
@Entity
@Table(name = "customers")
@Data
@EqualsAndHashCode(callSuper = true)
public class Customer extends PanacheEntity {

    /**
     * RUC - Registro Único de Contribuyentes (Perú - 11 dígitos)
     * Cifrado con Google Tink a nivel de aplicación
     */
    @Column(name = "ruc", unique = true, nullable = false, length = 500)
    @NotNull(message = "El RUC es obligatorio")
    @Pattern(regexp = "^[0-9]{11}$", message = "El RUC debe tener 11 dígitos")
    private String ruc; // Se cifrará en el service con Tink

    /**
     * Razón Social de la empresa
     * Campo marcado para Always Encrypted a nivel de base de datos
     */
    @Column(name = "legal_name", nullable = false)
    @NotBlank(message = "La razón social es obligatoria")
    @Size(min = 3, max = 200, message = "La razón social debe tener entre 3 y 200 caracteres")
    private String legalName; // Simulación de Always Encrypted

    /**
     * Nombre comercial
     */
    @Column(name = "trade_name")
    @Size(max = 200)
    private String tradeName;

    /**
     * Sector industrial
     */
    @Column(name = "industry", length = 100)
    @NotBlank(message = "El sector industrial es obligatorio")
    private String industry; // RETAIL, TECHNOLOGY, MANUFACTURING, SERVICES, etc.

    /**
     * Fecha de constitución de la empresa
     */
    @Column(name = "founded_date")
    @PastOrPresent(message = "La fecha de constitución no puede ser futura")
    private LocalDate foundedDate;

    /**
     * Ingresos anuales (en soles)
     * Campo sensible que podría cifrarse
     */
    @Column(name = "annual_revenue", precision = 15, scale = 2)
    @Positive(message = "Los ingresos anuales deben ser positivos")
    private BigDecimal annualRevenue;

    /**
     * Email de contacto principal
     */
    @Column(name = "contact_email")
    @Email(message = "El email no tiene formato válido")
    @NotBlank(message = "El email de contacto es obligatorio")
    private String contactEmail;

    /**
     * Teléfono de contacto
     */
    @Column(name = "contact_phone", length = 20)
    @Pattern(regexp = "^[+]?[0-9]{9,15}$", message = "El teléfono debe tener entre 9 y 15 dígitos")
    private String contactPhone;

    /**
     * Dirección fiscal
     */
    @Column(name = "address", length = 500)
    private String address;

    /**
     * Ciudad
     */
    @Column(name = "city", length = 100)
    private String city;

    /**
     * Estado de la empresa (ACTIVE, INACTIVE, SUSPENDED)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CustomerStatus status = CustomerStatus.ACTIVE;

    /**
     * Score crediticio histórico promedio (0-1000)
     * Calculado por scoring-service
     */
    @Column(name = "credit_score")
    @Min(value = 0, message = "El score mínimo es 0")
    @Max(value = 1000, message = "El score máximo es 1000")
    private Integer creditScore;

    /**
     * Categoría de riesgo (AAA, AA, A, BBB, BB, B, C)
     */
    @Column(name = "risk_category", length = 10)
    private String riskCategory;

    /**
     * Indica si el cliente está validado por SUNAT
     */
    @Column(name = "sunat_validated")
    private Boolean sunatValidated = false;

    /**
     * Fecha de creación del registro
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Última actualización del registro
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Usuario que creó el registro
     */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    /**
     * Callback antes de persistir
     */
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = CustomerStatus.ACTIVE;
        }
    }

    /**
     * Callback antes de actualizar
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ==========================================
    // Métodos de negocio con Panache
    // ==========================================

    /**
     * Buscar cliente por RUC (campo cifrado)
     * Nota: En producción, esto requeriría búsqueda en el servicio después de cifrar
     */
    public static Customer findByRuc(String ruc) {
        return find("ruc", ruc).firstResult();
    }

    /**
     * Buscar clientes por industria
     */
    public static java.util.List<Customer> findByIndustry(String industry) {
        return list("industry", industry);
    }

    /**
     * Buscar clientes activos
     */
    public static java.util.List<Customer> findActiveCustomers() {
        return list("status", CustomerStatus.ACTIVE);
    }

    /**
     * Buscar clientes por categoría de riesgo
     */
    public static java.util.List<Customer> findByRiskCategory(String category) {
        return list("riskCategory", category);
    }

    /**
     * Contar clientes por industria
     */
    public static long countByIndustry(String industry) {
        return count("industry", industry);
    }

    /**
     * Verificar si es cliente de alto riesgo
     */
    public boolean isHighRisk() {
        return "C".equals(this.riskCategory) || 
               "B".equals(this.riskCategory) ||
               (this.creditScore != null && this.creditScore < 500);
    }

    /**
     * Verificar si es cliente premium (bajo riesgo)
     */
    public boolean isPremium() {
        return "AAA".equals(this.riskCategory) || 
               "AA".equals(this.riskCategory) ||
               (this.creditScore != null && this.creditScore >= 800);
    }
}
