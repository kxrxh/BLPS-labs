package com.itmo.blps.lab1.messaging;

import com.itmo.blps.lab1.config.RabbitMQConfig;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JmsNotificationConsumer {

    // Listen to the queue defined in RabbitMQConfig using the JMS listener factory
    @JmsListener(destination = RabbitMQConfig.QUEUE_NAME)
    public void receiveNotification(Message message) {
        log.info("Received message via JMS from queue: {}", RabbitMQConfig.QUEUE_NAME);
        try {
            if (message instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message;
                String text = textMessage.getText();
                log.info("Received text message content: {}", text);
                // TODO: Add actual processing logic here
            } else {
                log.warn("Received non-text message: {}", message);
            }
            // Acknowledge message (handled by container factory by default)
        } catch (JMSException e) {
            log.error("Error processing received JMS message from queue '{}'", RabbitMQConfig.QUEUE_NAME, e);
            // Consider error handling strategy (e.g., moving to DLQ)
        }
    }
}
