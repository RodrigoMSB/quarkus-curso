package pe.banco.order.saga;

import io.smallrye.faulttolerance.api.CircuitBreakerName;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import pe.banco.order.client.InventoryClient;
import pe.banco.order.client.PaymentClient;
import pe.banco.order.entity.Order;
import pe.banco.order.entity.OrderItem;
import pe.banco.order.repository.OrderRepository;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Orquestador SAGA para transacciones distribuidas.
 * 
 * Este componente implementa el patrón SAGA de orquestación para coordinar
 * una transacción distribuida que involucra múltiples microservicios:
 * 1. Reservar inventario
 * 2. Procesar pago
 * 3. Confirmar reserva o compensar en caso de fallo
 * 
 * Analogía: Es como un director de orquesta que coordina a los músicos.
 * Si uno falla, el director debe indicar a los demás que detengan la sinfonía
 * y vuelvan al inicio (compensación).
 */
@ApplicationScoped
public class OrderSagaOrchestrator {

    private static final Logger LOG = Logger.getLogger(OrderSagaOrchestrator.class);

    @Inject
    @RestClient
    InventoryClient inventoryClient;

    @Inject
    @RestClient
    PaymentClient paymentClient;

    @Inject
    OrderRepository orderRepository;

    /**
     * Ejecuta la SAGA completa para crear una orden.
     * 
     * Pasos:
     * 1. Reservar inventario para cada producto
     * 2. Procesar el pago
     * 3. Confirmar las reservas
     * 
     * Si cualquier paso falla, se ejecutan las compensaciones en orden inverso.
     */
    @Transactional
    @Timeout(value = 30, unit = ChronoUnit.SECONDS)
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 5000)
    @CircuitBreakerName("order-saga")
    @Fallback(fallbackMethod = "fallbackExecuteSaga")
    public SagaResult executeSaga(Order order, String paymentMethod) {
        LOG.info("🚀 Iniciando SAGA para orden: " + order.id);
        
        List<SagaStep> completedSteps = new ArrayList<>();
        
        try {
            // PASO 1: Reservar inventario para cada item
            LOG.info("📦 PASO 1: Reservando inventario...");
            for (OrderItem item : order.items) {
                InventoryClient.ReservationRequest request = new InventoryClient.ReservationRequest();
                request.orderId = order.id;
                request.productCode = item.productCode;
                request.quantity = item.quantity;
                
                InventoryClient.ReservationResponse response = inventoryClient.reserveStock(request);
                
                if (!response.success) {
                    LOG.error("❌ Fallo al reservar inventario para: " + item.productCode);
                    throw new SagaException("Inventario insuficiente para " + item.productName + ": " + response.message);
                }
                
                completedSteps.add(new SagaStep("INVENTORY_RESERVE", item.productCode, item.quantity));
                LOG.info("✅ Inventario reservado para: " + item.productCode);
            }
            
            order.status = Order.OrderStatus.INVENTORY_RESERVED;
            orderRepository.persist(order);

            // PASO 2: Procesar pago
            LOG.info("💳 PASO 2: Procesando pago...");
            PaymentClient.PaymentRequest paymentRequest = new PaymentClient.PaymentRequest();
            paymentRequest.orderId = order.id;
            paymentRequest.userId = order.userId;
            paymentRequest.amount = order.totalAmount;
            paymentRequest.paymentMethod = paymentMethod;
            
            PaymentClient.PaymentResponse paymentResponse = paymentClient.processPayment(paymentRequest);
            
            if (!paymentResponse.success) {
                LOG.error("❌ Fallo al procesar pago");
                throw new SagaException("Error en el pago: " + paymentResponse.message);
            }
            
            completedSteps.add(new SagaStep("PAYMENT", order.id, null));
            LOG.info("✅ Pago procesado exitosamente: " + paymentResponse.transactionId);
            
            order.status = Order.OrderStatus.PAYMENT_PROCESSING;
            orderRepository.persist(order);

            // PASO 3: Confirmar reservas de inventario
            LOG.info("✔️  PASO 3: Confirmando reservas...");
            for (OrderItem item : order.items) {
                InventoryClient.ConfirmRequest confirmRequest = new InventoryClient.ConfirmRequest();
                confirmRequest.productCode = item.productCode;
                confirmRequest.quantity = item.quantity;
                
                inventoryClient.confirmReservation(order.id, confirmRequest);
                LOG.info("✅ Reserva confirmada para: " + item.productCode);
            }

            order.status = Order.OrderStatus.COMPLETED;
            orderRepository.persist(order);

            LOG.info("🎉 SAGA completada exitosamente para orden: " + order.id);
            return SagaResult.success(order.id, "Orden creada exitosamente");

        } catch (Exception e) {
            LOG.error("💥 Error en SAGA, ejecutando compensaciones...", e);
            compensate(order, completedSteps);
            
            order.status = Order.OrderStatus.FAILED;
            orderRepository.persist(order);
            
            return SagaResult.failure(order.id, "Error al crear orden: " + e.getMessage());
        }
    }

    /**
     * Compensa las operaciones completadas en caso de fallo.
     * Se ejecutan en orden INVERSO (LIFO - Last In, First Out).
     * 
     * Analogía: Es como usar Ctrl+Z múltiples veces para deshacer acciones.
     */
    private void compensate(Order order, List<SagaStep> completedSteps) {
        LOG.warn("🔄 Iniciando compensaciones para orden: " + order.id);
        
        // Recorrer en orden inverso
        for (int i = completedSteps.size() - 1; i >= 0; i--) {
            SagaStep step = completedSteps.get(i);
            
            try {
                switch (step.stepType) {
                    case "PAYMENT":
                        LOG.info("↩️  Compensando PAYMENT: Reembolsando...");
                        paymentClient.refundPayment(order.id);
                        LOG.info("✅ Pago reembolsado");
                        break;
                        
                    case "INVENTORY_RESERVE":
                        LOG.info("↩️  Compensando INVENTORY: Liberando reserva de " + step.productCode);
                        InventoryClient.CancelRequest cancelRequest = new InventoryClient.CancelRequest();
                        cancelRequest.productCode = step.productCode;
                        cancelRequest.quantity = step.quantity;
                        inventoryClient.cancelReservation(order.id, cancelRequest);
                        LOG.info("✅ Reserva liberada para: " + step.productCode);
                        break;
                }
            } catch (Exception e) {
                LOG.error("❌ Error al compensar paso: " + step.stepType, e);
                // En producción real, aquí se debería registrar en una cola de retry
            }
        }
        
        LOG.warn("✅ Compensaciones completadas para orden: " + order.id);
    }

    /**
     * Fallback en caso de que el CircuitBreaker se abra
     */
    public SagaResult fallbackExecuteSaga(Order order, String paymentMethod) {
        LOG.error("⚠️  Circuit Breaker ABIERTO - Sistema bajo estrés");
        order.status = Order.OrderStatus.FAILED;
        orderRepository.persist(order);
        return SagaResult.failure(order.id, "Servicio temporalmente no disponible. Por favor intente más tarde.");
    }

    /**
     * Representa un paso completado en la SAGA
     */
    private static class SagaStep {
        String stepType;
        String productCode;
        Integer quantity;

        SagaStep(String stepType, String productCode, Integer quantity) {
            this.stepType = stepType;
            this.productCode = productCode;
            this.quantity = quantity;
        }
    }

    /**
     * Resultado de la ejecución de la SAGA
     */
    public static class SagaResult {
        public boolean success;
        public String orderId;
        public String message;

        public static SagaResult success(String orderId, String message) {
            SagaResult result = new SagaResult();
            result.success = true;
            result.orderId = orderId;
            result.message = message;
            return result;
        }

        public static SagaResult failure(String orderId, String message) {
            SagaResult result = new SagaResult();
            result.success = false;
            result.orderId = orderId;
            result.message = message;
            return result;
        }
    }

    /**
     * Excepción personalizada para errores en la SAGA
     */
    public static class SagaException extends RuntimeException {
        public SagaException(String message) {
            super(message);
        }
    }
}
