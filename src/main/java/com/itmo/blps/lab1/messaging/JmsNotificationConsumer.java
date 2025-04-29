package com.itmo.blps.lab1.messaging;

import com.itmo.blps.lab1.config.RabbitMQConfig;
import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class JmsNotificationConsumer {

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
                // TODO: Add actual processing logic here
            } else {
                log.warn("Could not extract payload from message.");
            }

            // Acknowledgment is handled automatically by the listener container by default
        } catch (JMSException e) {
            log.error("JMSException while processing received message from destination '{}'", RabbitMQConfig.QUEUE_NAME, e);
        } catch (Exception e) {
            log.error("Error processing received JMS message from destination '{}'", RabbitMQConfig.QUEUE_NAME, e);
        }
    }
}
