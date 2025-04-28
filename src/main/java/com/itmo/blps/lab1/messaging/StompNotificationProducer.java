package com.itmo.blps.lab1.messaging;

import com.itmo.blps.lab1.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StompNotificationProducer {

    private final SimpMessagingTemplate messagingTemplate;

    // Sends to a destination that the STOMP relay will forward to the RabbitMQ
    // queue
    public void sendNotification(String notificationPayload) {
        // Target the queue directly via the STOMP relay prefix
        String destination = "/queue/" + RabbitMQConfig.QUEUE_NAME;
        try {
            log.info("Sending notification via STOMP to destination '{}': {}", destination, notificationPayload);
            messagingTemplate.convertAndSend(destination, notificationPayload);
            log.info("Successfully sent notification to STOMP destination: {}", destination);
        } catch (Exception e) {
            log.error("Failed to send notification via STOMP to destination '{}'", destination, e);
            // Optional: Add error handling/retry logic here
        }
    }
}
