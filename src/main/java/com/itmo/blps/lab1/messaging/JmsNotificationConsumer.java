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
import com.itmo.blps.lab1.service.NotificationType;

import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class JmsNotificationConsumer {

    private final EmailService emailService;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final AdvertisementRepository advertisementRepository;
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

            switch (NotificationType.fromString(type)) {
                case PAYMENT_RECEIPT:
                    processPaymentReceipt(node);
                    break;
                case PROMOTION_ENDING_SOON:
                    processPromotionEndingNotice(node);
                    break;
                case PROMOTION_EXPIRED:
                    processPromotionExpired(node);
                    break;
                default:
                    log.info(
                            "Ignoring notification of type: {} - only PAYMENT_RECEIPT, PROMOTION_REMINDER, and PROMOTION_EXPIRED are processed via JMS",
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

    private void processPromotionEndingNotice(JsonNode node) {
        long advertisementId = node.path("advertisementId").asLong();
        long userId = node.path("userId").asLong();

        log.info("Processing promotion ending soon notification for advertisement ID: {}, user ID: {}", advertisementId,
                userId);

        Optional<Advertisement> advertisementOpt = advertisementRepository.findById(advertisementId);

        if (advertisementOpt.isPresent()) {
            Advertisement advertisement = advertisementOpt.get();

            if (advertisement.getIsPromoted()) {
                emailService.sendPromotionEndingNotice(advertisement);
                log.info("Promotion ending soon email sent for advertisement ID: {}", advertisementId);
            } else {
                log.warn("Advertisement {} is not marked as promoted. Skipping ending soon notice.", advertisementId);
            }
        } else {
            log.warn(
                    "Could not send promotion ending notice: Advertisement not found (Advertisement ID: {}, User ID: {})",
                    advertisementId, userId);
        }
    }

    @Transactional
    private void processPromotionExpired(JsonNode node) {
        long advertisementId = node.path("advertisementId").asLong();
        long userId = node.path("userId").asLong();
        log.info("Processing promotion expired notification for advertisement ID: {}, user ID: {}", advertisementId,
                userId);

        Optional<Advertisement> advertisementOpt = advertisementRepository.findById(advertisementId);

        if (advertisementOpt.isPresent()) {
            Advertisement advertisement = advertisementOpt.get();
            try {
                emailService.sendPromotionExpired(advertisement);
                log.info("Promotion expired email sent for advertisement ID: {}", advertisementId);

                advertisement.setIsPromoted(false);
                advertisement.setPromotion(null);
                advertisement.setStartDate(null);
                advertisement.setDurationInMinutes(null);
                advertisementRepository.save(advertisement);
                log.info("Marked promotion as expired in DB for advertisement ID: {}.", advertisement.getId());

            } catch (Exception e) {
                log.error("Error processing expired promotion message for advertisement ID: {}", advertisementId, e);
            }
        } else {
            log.warn(
                    "Could not process promotion expired notice: Advertisement not found (Advertisement ID: {}, User ID: {})",
                    advertisementId, userId);
        }
    }
}
