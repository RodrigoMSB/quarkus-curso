package pe.banco.payment.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import pe.banco.payment.dto.PaymentRequest;
import pe.banco.payment.dto.PaymentResponse;
import pe.banco.payment.entity.Payment;
import pe.banco.payment.repository.PaymentRepository;

import java.util.UUID;

@ApplicationScoped
public class PaymentService {

    private static final Logger LOG = Logger.getLogger(PaymentService.class);

    @Inject
    PaymentRepository paymentRepository;

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        try {
            // Verificar si ya existe un pago para esta orden
            if (paymentRepository.findByOrderId(request.orderId).isPresent()) {
                return PaymentResponse.failure(request.orderId, "Ya existe un pago para esta orden");
            }

            // Crear el pago
            Payment payment = new Payment();
            payment.orderId = request.orderId;
            payment.userId = request.userId;
            payment.amount = request.amount;
            payment.paymentMethod = request.paymentMethod;
            payment.status = Payment.PaymentStatus.PROCESSING;
            
            paymentRepository.persist(payment);

            // Simular procesamiento del pago (en la vida real, aquí iría la integración con pasarela de pago)
            boolean paymentSuccessful = simulatePaymentProcessing(request);

            if (paymentSuccessful) {
                payment.status = Payment.PaymentStatus.COMPLETED;
                payment.transactionId = "TXN-" + UUID.randomUUID().toString();
                paymentRepository.persist(payment);
                
                LOG.info("Pago procesado exitosamente para orden: " + request.orderId);
                return PaymentResponse.success(request.orderId, payment.transactionId, request.amount);
            } else {
                payment.status = Payment.PaymentStatus.FAILED;
                paymentRepository.persist(payment);
                
                LOG.warn("Pago fallido para orden: " + request.orderId);
                return PaymentResponse.failure(request.orderId, "El pago fue rechazado por el procesador");
            }

        } catch (Exception e) {
            LOG.error("Error procesando pago para orden: " + request.orderId, e);
            return PaymentResponse.failure(request.orderId, "Error interno al procesar el pago");
        }
    }

    @Transactional
    public void refundPayment(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado para orden: " + orderId));
        
        if (payment.status != Payment.PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Solo se pueden reembolsar pagos completados");
        }

        payment.status = Payment.PaymentStatus.REFUNDED;
        paymentRepository.persist(payment);
        
        LOG.info("Pago reembolsado para orden: " + orderId);
    }

    private boolean simulatePaymentProcessing(PaymentRequest request) {
        // Simulación: 90% de éxito
        // En producción real, aquí iría la integración con Stripe, PayPal, etc.
        return Math.random() > 0.1;
    }
}
