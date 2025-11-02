package pe.banco.customer.service;

import io.quarkus.cache.CacheResult;
import io.quarkus.cache.CacheInvalidate;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.faulttolerance.*;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import pe.banco.customer.dto.CustomerRequest;
import pe.banco.customer.dto.CustomerResponse;
import pe.banco.customer.entity.Customer;
import pe.banco.customer.entity.CustomerStatus;
import pe.banco.customer.security.TinkEncryption;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de clientes empresariales
 * 
 * Demuestra:
 * - CDI y Application Scoped (Capítulo 3)
 * - Transacciones con @Transactional (Capítulo 4)
 * - Cache con Redis (Capítulo 10)
 * - Fault Tolerance: Retry, Timeout, CircuitBreaker, Fallback (Capítulo 8)
 * - REST Client para servicios externos (Capítulo 8)
 * - Cifrado con Google Tink (Capítulo 4)
 */
@ApplicationScoped
public class CustomerService {

    private static final Logger LOG = Logger.getLogger(CustomerService.class);

    @Inject
    TinkEncryption encryption;

    @RestClient
    SunatValidationClient sunatClient;

    /**
     * Crear nuevo cliente
     * 
     * Demuestra:
     * - Transacciones
     * - Cifrado de datos sensibles
     * - Validación externa con SUNAT (con fault tolerance)
     */
    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request, String createdBy) {
        LOG.infof("📝 Creando nuevo cliente con RUC: %s", request.getRuc());

        // Validar que el RUC no exista
        String encryptedRuc = encryption.encryptRuc(request.getRuc());
        Customer existing = Customer.findByRuc(encryptedRuc);
        if (existing != null) {
            throw new IllegalArgumentException("Ya existe un cliente con RUC: " + request.getRuc());
        }

        // Validar con SUNAT (con tolerancia a fallos)
        boolean sunatValid = validateWithSunat(request.getRuc());

        // Crear entidad
        Customer customer = new Customer();
        customer.setRuc(encryptedRuc); // RUC cifrado
        customer.setLegalName(request.getLegalName());
        customer.setTradeName(request.getTradeName());
        customer.setIndustry(request.getIndustry());
        customer.setFoundedDate(request.getFoundedDate());
        customer.setAnnualRevenue(request.getAnnualRevenue());
        customer.setContactEmail(request.getContactEmail());
        customer.setContactPhone(request.getContactPhone());
        customer.setAddress(request.getAddress());
        customer.setCity(request.getCity());
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setSunatValidated(sunatValid);
        customer.setCreatedBy(createdBy);

        // Persistir con Panache
        customer.persist();

        LOG.infof("✅ Cliente creado exitosamente - ID: %d", customer.id);
        return mapToResponse(customer, request.getRuc());
    }

    /**
     * Obtener cliente por ID con cache
     * 
     * Demuestra:
     * - Cache con Redis (Capítulo 10)
     * - Descifrado de datos sensibles
     */
    @CacheResult(cacheName = "customers")
    public CustomerResponse getCustomer(Long id) {
        LOG.infof("🔍 Buscando cliente ID: %d", id);
        
        Customer customer = Customer.findById(id);
        if (customer == null) {
            throw new IllegalArgumentException("Cliente no encontrado: " + id);
        }

        // Descifrar RUC para uso interno (no se expone completo en la respuesta)
        String decryptedRuc = encryption.decryptRuc(customer.getRuc());
        
        return mapToResponse(customer, decryptedRuc);
    }

    /**
     * Buscar cliente por RUC
     * 
     * Demuestra búsqueda con datos cifrados
     */
    public CustomerResponse getCustomerByRuc(String ruc) {
        LOG.infof("🔍 Buscando cliente por RUC: %s", ruc);

        String encryptedRuc = encryption.encryptRuc(ruc);
        Customer customer = Customer.findByRuc(encryptedRuc);
        
        if (customer == null) {
            throw new IllegalArgumentException("Cliente no encontrado con RUC: " + ruc);
        }

        return mapToResponse(customer, ruc);
    }

    /**
     * Actualizar cliente
     * 
     * Demuestra:
     * - Invalidación de cache
     * - Transacciones
     */
    @Transactional
    @CacheInvalidate(cacheName = "customers")
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        LOG.infof("📝 Actualizando cliente ID: %d", id);

        Customer customer = Customer.findById(id);
        if (customer == null) {
            throw new IllegalArgumentException("Cliente no encontrado: " + id);
        }

        // Actualizar campos (excepto RUC que no se puede cambiar)
        customer.setLegalName(request.getLegalName());
        customer.setTradeName(request.getTradeName());
        customer.setIndustry(request.getIndustry());
        customer.setFoundedDate(request.getFoundedDate());
        customer.setAnnualRevenue(request.getAnnualRevenue());
        customer.setContactEmail(request.getContactEmail());
        customer.setContactPhone(request.getContactPhone());
        customer.setAddress(request.getAddress());
        customer.setCity(request.getCity());

        String decryptedRuc = encryption.decryptRuc(customer.getRuc());

        LOG.infof("✅ Cliente actualizado - ID: %d", id);
        return mapToResponse(customer, decryptedRuc);
    }

    /**
     * Listar todos los clientes activos
     */
    public List<CustomerResponse> listActiveCustomers() {
        LOG.info("📋 Listando clientes activos");
        
        return Customer.findActiveCustomers().stream()
            .map(c -> mapToResponse(c, encryption.decryptRuc(c.getRuc())))
            .collect(Collectors.toList());
    }

    /**
     * Listar clientes por industria
     */
    public List<CustomerResponse> listByIndustry(String industry) {
        LOG.infof("📋 Listando clientes de industria: %s", industry);
        
        return Customer.findByIndustry(industry).stream()
            .map(c -> mapToResponse(c, encryption.decryptRuc(c.getRuc())))
            .collect(Collectors.toList());
    }

    /**
     * Validar RUC con SUNAT con tolerancia a fallos
     * 
     * Demuestra:
     * - @Timeout: máximo 3 segundos
     * - @Retry: hasta 2 reintentos
     * - @CircuitBreaker: abre el circuito si hay 5 fallos
     * - @Fallback: retorna false si todo falla
     */
    @Timeout(3000) // 3 segundos máximo
    @Retry(maxRetries = 2, delay = 500) // 2 reintentos con 500ms de delay
    @CircuitBreaker(
        requestVolumeThreshold = 5,
        failureRatio = 0.5,
        delay = 10000 // 10 segundos antes de intentar cerrar
    )
    @CircuitBreakerName("sunat-validation")
    @Fallback(fallbackMethod = "fallbackSunatValidation")
    public boolean validateWithSunat(String ruc) {
        LOG.infof("🔗 Validando RUC con SUNAT: %s", ruc);
        
        try {
            SunatValidationClient.SunatResponse response = sunatClient.validateRuc(ruc);
            LOG.infof("✅ SUNAT respondió: %s - %s", response.ruc(), response.status());
            return response.valid();
        } catch (Exception e) {
            LOG.errorf(e, "❌ Error al validar con SUNAT: %s", ruc);
            throw e; // Re-throw para activar retry/circuit breaker
        }
    }

    /**
     * Fallback si SUNAT no está disponible
     */
    private boolean fallbackSunatValidation(String ruc) {
        LOG.warnf("⚠️  SUNAT no disponible, usando fallback para RUC: %s", ruc);
        return false; // Marcar como no validado
    }

    /**
     * Mapear entidad a DTO de respuesta
     */
    private CustomerResponse mapToResponse(Customer customer, String decryptedRuc) {
        return CustomerResponse.builder()
            .id(customer.id)
            .rucMasked(CustomerResponse.maskRuc(decryptedRuc))
            .legalName(customer.getLegalName())
            .tradeName(customer.getTradeName())
            .industry(customer.getIndustry())
            .foundedDate(customer.getFoundedDate())
            .annualRevenue(customer.getAnnualRevenue())
            .contactEmail(customer.getContactEmail())
            .contactPhone(customer.getContactPhone())
            .address(customer.getAddress())
            .city(customer.getCity())
            .status(customer.getStatus())
            .creditScore(customer.getCreditScore())
            .riskCategory(customer.getRiskCategory())
            .sunatValidated(customer.getSunatValidated())
            .createdAt(customer.getCreatedAt())
            .updatedAt(customer.getUpdatedAt())
            .createdBy(customer.getCreatedBy())
            .isHighRisk(customer.isHighRisk())
            .isPremium(customer.isPremium())
            .build();
    }

    /**
     * Actualizar score crediticio (llamado por scoring-service)
     */
    @Transactional
    @CacheInvalidate(cacheName = "customers")
    public void updateCreditScore(Long customerId, Integer score, String riskCategory) {
        LOG.infof("📊 Actualizando score del cliente %d: %d (%s)", customerId, score, riskCategory);
        
        Customer customer = Customer.findById(customerId);
        if (customer != null) {
            customer.setCreditScore(score);
            customer.setRiskCategory(riskCategory);
        }
    }
}
