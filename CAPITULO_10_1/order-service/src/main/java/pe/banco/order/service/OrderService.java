package pe.banco.order.service;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.SetArgs;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import pe.banco.order.client.InventoryClient;
import pe.banco.order.dto.CreateOrderRequest;
import pe.banco.order.dto.OrderResponse;
import pe.banco.order.dto.ProductInfoDTO;
import pe.banco.order.entity.Order;
import pe.banco.order.entity.OrderItem;
import pe.banco.order.repository.OrderRepository;
import pe.banco.order.saga.OrderSagaOrchestrator;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio principal de órdenes con integración de Redis Cache.
 * 
 * Redis se usa para:
 * 1. Cachear información de productos (reduce latencia)
 * 2. Evitar llamadas repetidas al servicio de inventario
 * 
 * Analogía: Redis es como un bloc de notas al lado de tu escritorio.
 * En vez de ir a la biblioteca (BD) cada vez, consultas tus notas rápidas.
 */
@ApplicationScoped
public class OrderService {

    private static final Logger LOG = Logger.getLogger(OrderService.class);
    private static final String PRODUCT_CACHE_PREFIX = "product:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    @Inject
    OrderRepository orderRepository;

    @Inject
    OrderSagaOrchestrator sagaOrchestrator;

    @Inject
    @RestClient
    InventoryClient inventoryClient;

    @Inject
    RedisDataSource redisDataSource;

    private ValueCommands<String, ProductInfoDTO> productCache;

    @jakarta.annotation.PostConstruct
    void init() {
        productCache = redisDataSource.value(ProductInfoDTO.class);
    }

    /**
     * Crea una nueva orden ejecutando la SAGA completa
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        LOG.info("📝 Creando orden para usuario: " + request.userId);

        // Crear la orden
        Order order = new Order();
        order.userId = request.userId;
        order.status = Order.OrderStatus.PENDING;

        // Agregar items y buscar información de productos (con cache)
        for (CreateOrderRequest.OrderItemRequest itemReq : request.items) {
            ProductInfoDTO productInfo = getProductWithCache(itemReq.productCode);

            OrderItem item = new OrderItem();
            item.order = order;
            item.productCode = productInfo.productCode;
            item.productName = productInfo.name;
            item.quantity = itemReq.quantity;
            item.price = productInfo.price;

            order.items.add(item);
        }

        order.calculateTotal();
        orderRepository.persist(order);

        // Ejecutar SAGA
        OrderSagaOrchestrator.SagaResult result = sagaOrchestrator.executeSaga(order, request.paymentMethod);

        return toResponse(order, result.message);
    }

    /**
     * Obtiene información de producto con Redis Cache.
     * 
     * Flujo:
     * 1. Buscar en Redis (rápido - ~1ms)
     * 2. Si no existe, buscar en servicio de inventario (lento - ~50ms)
     * 3. Guardar en Redis para próxima vez
     */
    private ProductInfoDTO getProductWithCache(String productCode) {
        String cacheKey = PRODUCT_CACHE_PREFIX + productCode;

        // 1. Intentar obtener del cache
        ProductInfoDTO cached = productCache.get(cacheKey);
        if (cached != null) {
            LOG.debug("🎯 Cache HIT para producto: " + productCode);
            return cached;
        }

        // 2. Cache MISS - obtener del servicio
        LOG.debug("❌ Cache MISS para producto: " + productCode + " - consultando servicio");
        ProductInfoDTO product = inventoryClient.getProduct(productCode);

        // 3. Guardar en cache con TTL de 10 minutos (CORREGIDO)
        SetArgs setArgs = new SetArgs().ex(CACHE_TTL);
        productCache.set(cacheKey, product, setArgs);
        LOG.debug("💾 Producto cacheado: " + productCode);

        return product;
    }

    /**
     * Invalida el cache de un producto específico
     */
    public void invalidateProductCache(String productCode) {
        String cacheKey = PRODUCT_CACHE_PREFIX + productCode;
        productCache.getdel(cacheKey);
        LOG.info("🗑️  Cache invalidado para producto: " + productCode);
    }

    public OrderResponse getOrderById(String orderId) {
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Orden no encontrada: " + orderId);
        }
        return toResponse(order, null);
    }

    public List<OrderResponse> getOrdersByUser(String userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(order -> toResponse(order, null))
                .collect(Collectors.toList());
    }

    private OrderResponse toResponse(Order order, String message) {
        OrderResponse response = new OrderResponse();
        response.orderId = order.id;
        response.userId = order.userId;
        response.status = order.status.name();
        response.totalAmount = order.totalAmount;
        response.createdAt = order.createdAt;
        response.message = message;

        response.items = order.items.stream().map(item -> {
            OrderResponse.OrderItemDTO itemDTO = new OrderResponse.OrderItemDTO();
            itemDTO.productCode = item.productCode;
            itemDTO.productName = item.productName;
            itemDTO.quantity = item.quantity;
            itemDTO.price = item.price;
            itemDTO.subtotal = item.price * item.quantity;
            return itemDTO;
        }).collect(Collectors.toList());

        return response;
    }
}
