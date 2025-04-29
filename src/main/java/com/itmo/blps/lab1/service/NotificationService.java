package com.itmo.blps.lab1.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itmo.blps.lab1.entities.Payment;
import com.itmo.blps.lab1.entities.Promotion;
import com.itmo.blps.lab1.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final ObjectMapper objectMapper;

    /**
     * Creates a payment receipt notification message in JSON format
     */
    public String createPaymentReceiptNotification(User user, Payment payment) {
        ObjectNode node = objectMapper.createObjectNode()
                .put("type", "PAYMENT_RECEIPT")
                .put("timestamp", LocalDateTime.now().format(formatter))
                .put("userId", user.getId())
                .put("paymentId", payment.getId());

        return serializeToJson(node);
    }

    /**
     * Creates a payment reminder notification message in JSON format
     */
    public String createPaymentReminderNotification(User user, Payment payment) {
        ObjectNode node = objectMapper.createObjectNode()
                .put("type", "PAYMENT_REMINDER")
                .put("timestamp", LocalDateTime.now().format(formatter))
                .put("userId", user.getId())
                .put("paymentId", payment.getId());

        return serializeToJson(node);
    }

    /**
     * Creates a promotion reminder notification message in JSON format
     */
    public String createPromotionReminderNotification(User user, Promotion promotion) {
        ObjectNode node = objectMapper.createObjectNode()
                .put("type", "PROMOTION_REMINDER")
                .put("timestamp", LocalDateTime.now().format(formatter))
                .put("userId", user.getId())
                .put("promotionId", promotion.getId())
                .put("expiresAt", promotion.getExpirationDate().format(formatter));

        return serializeToJson(node);
    }

    /**
     * Creates a promotion deactivation notification message in JSON format
     */
    public String createPromotionDeactivationNotification(User user, Promotion promotion) {
        ObjectNode node = objectMapper.createObjectNode()
                .put("type", "PROMOTION_DEACTIVATION")
                .put("timestamp", LocalDateTime.now().format(formatter))
                .put("userId", user.getId())
                .put("promotionId", promotion.getId());

        return serializeToJson(node);
    }

    /**
     * For backward compatibility with existing scheduled task
     */
    public String generateScheduledNotification() {
        ObjectNode node = objectMapper.createObjectNode()
                .put("type", "SCHEDULED_NOTIFICATION")
                .put("timestamp", LocalDateTime.now().format(formatter))
                .put("message", "Payment Reminder triggered at: " + LocalDateTime.now().format(formatter));

        return serializeToJson(node);
    }

    private String serializeToJson(ObjectNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notification to JSON: {}", e.getMessage());
            // Fallback to simple format
            return String.format("{ \"error\": \"Serialization failed\", \"timestamp\": \"%s\" }",
                    LocalDateTime.now().format(formatter));
        }
    }
}
