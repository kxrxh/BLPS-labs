package com.itmo.blps.lab1.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmo.blps.lab1.config.RabbitMQConfig;
import com.itmo.blps.lab1.entities.Payment;
import com.itmo.blps.lab1.entities.Promotion;
import com.itmo.blps.lab1.entities.User;
import com.itmo.blps.lab1.repositories.PaymentRepository;
import com.itmo.blps.lab1.repositories.PromotionRepository;
import com.itmo.blps.lab1.repositories.UserRepository;
import com.itmo.blps.lab1.service.EmailService;
import com.itmo.blps.lab1.service.NotificationService;
import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class JmsNotificationConsumer {

    private final NotificationService notificationService;
    private final EmailService emailService;
    private final PaymentRepository paymentRepository;
    private final PromotionRepository promotionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @JmsListener(destination = RabbitMQConfig.QUEUE_NAME)
    public void receiveNotification(Message message) {
        log.info("Received raw JMS message from destination '{}'", RabbitMQConfig.QUEUE_NAME);
        try {
            String payload = null;
            if (message instanceof TextMessage) {
                payload = ((TextMessage) message).getText();
                log.info("Message is TextMessage. Payload: {}", payload);
            } else if (message instanceof BytesMessage) {
                BytesMessage bytesMessage = (BytesMessage) message;
                byte[] body = new byte[(int) bytesMessage.getBodyLength()];
                bytesMessage.readBytes(body);
                payload = new String(body, StandardCharsets.UTF_8);
                log.info("Message is BytesMessage. Payload decoded from bytes: {}", payload);
            } else {
                log.warn("Received message of unexpected type: {}. Attempting toString(): {}",
                        message.getClass().getName(), message.toString());
                payload = message.toString();
            }

            if (payload != null) {
                log.info("Processing extracted payload: {}", payload);
                processNotification(payload);
            } else {
                log.warn("Could not extract payload from message.");
            }

            // Acknowledgment is handled automatically by the listener container by default
        } catch (JMSException e) {
            log.error("JMSException while processing received message from destination '{}'", RabbitMQConfig.QUEUE_NAME,
                    e);
        } catch (Exception e) {
            log.error("Error processing received JMS message from destination '{}'", RabbitMQConfig.QUEUE_NAME, e);
        }
    }

    private void processNotification(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String type = node.path("type").asText();

            switch (type) {
                case "PAYMENT_RECEIPT":
                    processPaymentReceipt(node);
                    break;
                case "PAYMENT_REMINDER":
                    processPaymentReminder(node);
                    break;
                case "PROMOTION_REMINDER":
                    processPromotionReminder(node);
                    break;
                case "PROMOTION_DEACTIVATION":
                    processPromotionDeactivation(node);
                    break;
                case "SCHEDULED_NOTIFICATION":
                    processScheduledNotification(node);
                    break;
                default:
                    log.warn("Unknown notification type: {}", type);
            }
        } catch (Exception e) {
            log.error("Failed to process notification: {}", e.getMessage(), e);
        }
    }

    private void processPaymentReceipt(JsonNode node) {
        long paymentId = node.path("paymentId").asLong();
        long userId = node.path("userId").asLong();

        log.info("Processing payment receipt notification for payment ID: {}, user ID: {}", paymentId, userId);

        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        Optional<User> userOpt = userRepository.findById(userId);

        if (paymentOpt.isPresent() && userOpt.isPresent()) {
            emailService.sendPaymentReceipt(userOpt.get(), paymentOpt.get());
            log.info("Payment receipt email sent for payment ID: {}", paymentId);
        } else {
            log.warn("Could not send payment receipt: Payment or User not found (Payment ID: {}, User ID: {})",
                    paymentId, userId);
        }
    }

    private void processPaymentReminder(JsonNode node) {
        long paymentId = node.path("paymentId").asLong();
        long userId = node.path("userId").asLong();

        log.info("Processing payment reminder notification for payment ID: {}, user ID: {}", paymentId, userId);

        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        Optional<User> userOpt = userRepository.findById(userId);

        if (paymentOpt.isPresent() && userOpt.isPresent()) {
            emailService.sendPaymentReminder(userOpt.get(), paymentOpt.get());
            log.info("Payment reminder email sent for payment ID: {}", paymentId);
        } else {
            log.warn("Could not send payment reminder: Payment or User not found (Payment ID: {}, User ID: {})",
                    paymentId, userId);
        }
    }

    private void processPromotionReminder(JsonNode node) {
        long promotionId = node.path("promotionId").asLong();
        long userId = node.path("userId").asLong();

        log.info("Processing promotion reminder notification for promotion ID: {}, user ID: {}", promotionId, userId);

        Optional<Promotion> promotionOpt = promotionRepository.findById(promotionId);
        Optional<User> userOpt = userRepository.findById(userId);

        if (promotionOpt.isPresent() && userOpt.isPresent()) {
            emailService.sendPromotionReminder(userOpt.get(), promotionOpt.get());

            // Update reminder sent status
            Promotion promotion = promotionOpt.get();
            promotion.setReminderSent(true);
            promotionRepository.save(promotion);

            log.info("Promotion reminder email sent for promotion ID: {}", promotionId);
        } else {
            log.warn("Could not send promotion reminder: Promotion or User not found (Promotion ID: {}, User ID: {})",
                    promotionId, userId);
        }
    }

    private void processPromotionDeactivation(JsonNode node) {
        long promotionId = node.path("promotionId").asLong();
        long userId = node.path("userId").asLong();

        log.info("Processing promotion deactivation notification for promotion ID: {}, user ID: {}", promotionId,
                userId);

        Optional<Promotion> promotionOpt = promotionRepository.findById(promotionId);
        Optional<User> userOpt = userRepository.findById(userId);

        if (promotionOpt.isPresent() && userOpt.isPresent()) {
            emailService.sendPromotionDeactivationNotice(userOpt.get(), promotionOpt.get());
            log.info("Promotion deactivation email sent for promotion ID: {}", promotionId);
        } else {
            log.warn(
                    "Could not send promotion deactivation notice: Promotion or User not found (Promotion ID: {}, User ID: {})",
                    promotionId, userId);
        }
    }

    private void processScheduledNotification(JsonNode node) {
        String message = node.path("message").asText("No message provided");
        String timestamp = node.path("timestamp").asText("Unknown time");

        log.info("Received scheduled notification: {} (at {})", message, timestamp);
        // No further processing needed for this notification type
    }
}
