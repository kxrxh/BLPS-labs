package com.itmo.blps.lab1.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmo.blps.lab1.entities.Advertisement;
import com.itmo.blps.lab1.entities.Payment;
import com.itmo.blps.lab1.entities.User;
import com.itmo.blps.lab1.repositories.AdvertisementRepository;
import com.itmo.blps.lab1.repositories.PaymentRepository;
import com.itmo.blps.lab1.repositories.UserRepository;
import com.itmo.blps.lab1.service.EmailService;
import com.itmo.blps.lab1.service.NotificationType;

import jakarta.jms.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class JmsNotificationConsumer implements MessageListener, InitializingBean, DisposableBean {

    private final EmailService emailService;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final AdvertisementRepository advertisementRepository;
    private final ObjectMapper objectMapper;
    private final ConnectionFactory connectionFactory;
    @Qualifier("jmsQueue")
    private final Queue destinationQueue;
    @Qualifier("jmsMessageProcessorExecutor")
    private final TaskExecutor taskExecutor;

    private Connection connection;
    private Session session;
    private MessageConsumer consumer;

    @Override
    public void onMessage(Message message) {
        log.info("Received raw JMS message via manual listener from destination '{}'", getQueueNameSafe());
        try {
            String payload = null;
            if (message instanceof TextMessage) {
                payload = ((TextMessage) message).getText();
                log.info("Message is TextMessage. Payload: {}", payload);
            } else if (message instanceof BytesMessage) {
                BytesMessage bytesMessage = (BytesMessage) message;
                bytesMessage.reset();
                byte[] body = new byte[(int) bytesMessage.getBodyLength()];
                bytesMessage.readBytes(body);
                payload = new String(body, StandardCharsets.UTF_8);
                log.info("Message is BytesMessage. Payload decoded from bytes: {}", payload);
            } else {
                log.warn("Received message of unexpected type: {}. Ignoring.",
                        message.getClass().getName());
                payload = null;
            }

            if (payload != null) {
                log.info("Processing extracted payload: {}", payload);
                final String finalPayload = payload;
                taskExecutor.execute(() -> {
                    try {
                        processNotification(finalPayload);
                        log.debug("Async processing completed for payload starting with: {}", 
                                finalPayload.substring(0, Math.min(finalPayload.length(), 50)));
                    } catch (Exception e) {
                        log.error("Error during asynchronous processing of JMS message payload starting with: {}", 
                                finalPayload.substring(0, Math.min(finalPayload.length(), 50)), e);
                    }
                });
            } else {
                log.warn("Could not extract processable payload from message of type: {}", message.getClass().getName());
            }
        } catch (JMSException e) {
            log.error("JMSException while processing received message from destination '{}'", getQueueNameSafe(), e);
        } catch (Exception e) {
            log.error("Error processing received JMS message from destination '{}'", getQueueNameSafe(), e);
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

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            String queueName = getQueueNameSafe();
            log.info("Initializing JMS listener manually for destination '{}'", queueName);
            connection = connectionFactory.createConnection();
            connection.setExceptionListener(
                    ex -> log.error("JMS Connection Exception occurred on listener for queue '{}'.", queueName, ex));

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            consumer = session.createConsumer(destinationQueue);
            consumer.setMessageListener(this);
            connection.start();
            log.info("Manual JMS listener started successfully for destination '{}'.", queueName);
        } catch (JMSException e) {
            log.error("Failed to start manual JMS listener for destination '{}'", getQueueNameSafe(), e);
            try {
                destroy();
            } catch (Exception cleanupEx) {
                log.error("Exception during cleanup after failed initialization.", cleanupEx);
                e.addSuppressed(cleanupEx);
            }
            throw new RuntimeException("Failed to initialize JMS listener", e);
        } catch (Exception e) {
            log.error("Non-JMS Exception during JMS listener initialization for destination '{}'", getQueueNameSafe(),
                    e);
            throw new RuntimeException("Failed to initialize JMS listener due to non-JMS error", e);
        }
    }

    @Override
    public void destroy() throws Exception {
        String queueName = getQueueNameSafe();
        log.info("Shutting down manual JMS listener for destination '{}'", queueName);
        try {
            if (consumer != null) {
                log.debug("Closing JMS Consumer for queue '{}'", queueName);
                consumer.close();
                consumer = null;
            }
        } catch (JMSException e) {
            log.error("Error closing JMS Consumer for queue '{}'", queueName, e);
        }
        try {
            if (session != null) {
                log.debug("Closing JMS Session for queue '{}'", queueName);
                session.close();
                session = null;
            }
        } catch (JMSException e) {
            log.error("Error closing JMS Session for queue '{}'", queueName, e);
        }
        try {
            if (connection != null) {
                log.debug("Closing JMS Connection for queue '{}'", queueName);
                connection.close();
                connection = null;
            }
        } catch (JMSException e) {
            log.error("Error closing JMS Connection for queue '{}'", queueName, e);
        }
        log.info("Manual JMS listener shut down complete for destination '{}'.", queueName);
    }

    private String getQueueNameSafe() {
        try {
            return destinationQueue != null ? destinationQueue.getQueueName() : "UNKNOWN";
        } catch (JMSException e) {
            log.warn("Could not retrieve queue name", e);
            return "ERROR_RETRIEVING_NAME";
        }
    }
}
