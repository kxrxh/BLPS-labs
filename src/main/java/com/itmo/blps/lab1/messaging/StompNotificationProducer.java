package com.itmo.blps.lab1.messaging;

import com.itmo.blps.lab1.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class StompNotificationProducer implements ApplicationListener<BrokerAvailabilityEvent> {

    private final SimpMessagingTemplate messagingTemplate;
    private final AtomicBoolean isBrokerAvailable = new AtomicBoolean(false);

    // Sends to a destination that the STOMP relay will forward to the RabbitMQ
    // queue
    public void sendNotification(String notificationPayload) {
        if (!isBrokerAvailable.get()) {
            log.warn("Broker not available yet. Skipping STOMP notification: {}", notificationPayload);
            return;
        }

        String destination = "/queue/" + RabbitMQConfig.QUEUE_NAME;
        try {
            log.info("Sending notification via STOMP to destination '{}': {}", destination, notificationPayload);

            // Convert payload to byte[] explicitly
            byte[] payloadBytes = notificationPayload.getBytes(StandardCharsets.UTF_8);

            // Send byte[]
            messagingTemplate.convertAndSend(destination, payloadBytes);

            log.info("Successfully sent notification (as bytes) to STOMP destination: {}", destination);
        } catch (Exception e) {
            log.error("Failed to send notification via STOMP to destination '{}'", destination, e);
        }
    }

    @Override
    public void onApplicationEvent(@NonNull BrokerAvailabilityEvent event) {
        log.info("Broker Availability changed: {}", event.isBrokerAvailable());
        this.isBrokerAvailable.set(event.isBrokerAvailable());
    }
}
