package com.itmo.blps.lab1.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmo.blps.lab1.config.RabbitMQConfig;
import com.itmo.blps.lab1.entities.Advertisement;
import com.itmo.blps.lab1.entities.Payment;
import com.itmo.blps.lab1.entities.User;
import com.itmo.blps.lab1.repositories.AdvertisementRepository;
import com.itmo.blps.lab1.repositories.PaymentRepository;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class JmsNotificationConsumer {

    private final NotificationService notificationService;
    private final EmailService emailService;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final AdvertisementRepository advertisementRepository;
    private final ObjectMapper objectMapper;

    // Number of minutes before expiration to send notification
    private static final int MINUTES_BEFORE_EXPIRATION = 1;

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

            // Handle payment receipt and promotion reminder notifications through JMS
            switch (type) {
                case "PAYMENT_RECEIPT":
                    processPaymentReceipt(node);
                    break;
                case "PROMOTION_REMINDER":
                    processPromotionReminder(node);
                    break;
                default:
                    log.info(
                            "Ignoring notification of type: {} - only PAYMENT_RECEIPT and PROMOTION_REMINDER are processed via JMS",
                            type);
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

    private void processPromotionReminder(JsonNode node) {
        long advertisementId = node.path("advertisementId").asLong();
        long userId = node.path("userId").asLong();

        log.info("Processing promotion reminder notification for advertisement ID: {}, user ID: {}", advertisementId,
                userId);

        Optional<Advertisement> advertisementOpt = advertisementRepository.findById(advertisementId);
        Optional<User> userOpt = userRepository.findById(userId);

        if (advertisementOpt.isPresent() && userOpt.isPresent()) {
            Advertisement advertisement = advertisementOpt.get();
            User user = userOpt.get();

            if (advertisement.getStartDate() != null && advertisement.getDurationInMinutes() != null
                    && advertisement.getIsPromoted()) {
                LocalDateTime expirationDate = advertisement.getStartDate()
                        .plusMinutes(advertisement.getDurationInMinutes());
                emailService.sendPromotionExpirationNotice(user, advertisement, expirationDate);
                log.info("Promotion reminder email sent for advertisement ID: {}", advertisementId);
            } else {
                log.warn("Advertisement has no start date or duration: {}", advertisementId);
            }
        } else {
            log.warn(
                    "Could not send promotion reminder: Advertisement or User not found (Advertisement ID: {}, User ID: {})",
                    advertisementId, userId);
        }
    }

    /**
     * Scheduled job that runs periodically to check for promotions that are about
     * to expire
     * and sends notifications to users.
     * Changed to run every 5 minutes for easier testing.
     */
    @Scheduled(cron = "0 */5 * * * ?") // Run every 5 minutes
    public void checkExpiringPromotions() {
        log.info("Checking for promotions that are about to expire...");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime targetDate = now.plusMinutes(MINUTES_BEFORE_EXPIRATION);

        // Get all active advertisements with promotions
        List<Advertisement> advertisements = advertisementRepository.findAll().stream()
                .filter(ad -> ad.getIsActive() && ad.getIsPromoted() && ad.getStartDate() != null
                        && ad.getDurationInMinutes() != null)
                .filter(ad -> {
                    LocalDateTime expirationDate = ad.getStartDate().plusMinutes(ad.getDurationInMinutes());
                    // Check if expiration is after now and before the target notification time
                    return expirationDate.isAfter(now) &&
                            expirationDate.isBefore(targetDate);
                })
                .collect(Collectors.toList());

        log.info("Found {} advertisements with promotions expiring in the next {} minutes",
                advertisements.size(), MINUTES_BEFORE_EXPIRATION);

        // Send notifications for each expiring promotion
        for (Advertisement ad : advertisements) {
            try {
                User user = ad.getAuthor();
                if (user != null) {
                    LocalDateTime expirationDate = ad.getStartDate().plusMinutes(ad.getDurationInMinutes());
                    emailService.sendPromotionExpirationNotice(user, ad, expirationDate);
                    log.info("Sent promotion expiration notice for advertisement ID: {}, expiring on: {}",
                            ad.getId(), expirationDate);
                } else {
                    log.warn("Cannot send promotion expiration notice: No author found for advertisement ID: {}",
                            ad.getId());
                }
            } catch (Exception e) {
                log.error("Error sending promotion expiration notice for advertisement ID: {}", ad.getId(), e);
            }
        }
    }
}
