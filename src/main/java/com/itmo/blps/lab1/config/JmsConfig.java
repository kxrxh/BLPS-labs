package com.itmo.blps.lab1.config;

import com.rabbitmq.jms.admin.RMQConnectionFactory;
import com.rabbitmq.jms.admin.RMQDestination;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;


@Configuration
@EnableJms // It's good practice to have it here too, although the one in main app should suffice
public class JmsConfig {

    @Value("${spring.rabbitmq.host}")
    private String rabbitHost;

    @Value("${spring.rabbitmq.port}")
    private int rabbitPort; // Use the AMQP port (e.g., 5672), not STOMP port

    @Value("${spring.rabbitmq.username}")
    private String rabbitUsername;

    @Value("${spring.rabbitmq.password}")
    private String rabbitPassword;

    @Bean
    public ConnectionFactory connectionFactory() {
        RMQConnectionFactory connectionFactory = new RMQConnectionFactory();
        connectionFactory.setHost(rabbitHost);
        // IMPORTANT: RMQConnectionFactory uses the standard AMQP port (default 5672)
        // Make sure your application.properties reflects this or set it explicitly.
        // If spring.rabbitmq.port in your properties is the STOMP port (e.g., 61613 or 15674),
        // you need to use the correct AMQP port here. Assuming default 5672 if not overridden specifically for AMQP.
        // Let's check application.properties value first.
         connectionFactory.setPort(rabbitPort); // Ensure this is the AMQP port (e.g., 5672)
        connectionFactory.setUsername(rabbitUsername);
        connectionFactory.setPassword(rabbitPassword);
        // connectionFactory.setVirtualHost("/"); // Optional: set if using a specific vhost
        return connectionFactory;
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        // Configure the message converter to handle simple String/text messages
        factory.setMessageConverter(new SimpleMessageConverter());

        // Add custom DestinationResolver based on Stack Overflow solution
        factory.setDestinationResolver(new DestinationResolver() {
            @Override
            public Destination resolveDestinationName(Session session, String destinationName, boolean pubSubDomain) throws JMSException {
                RMQDestination jmsDestination = new RMQDestination();
                // Set the name JMS uses
                jmsDestination.setDestinationName(destinationName);
                // Set the actual RabbitMQ queue name
                jmsDestination.setAmqpQueueName(destinationName);
                // Crucially, tell it to use AMQP interpretation
                jmsDestination.setAmqp(true);
                // pubSubDomain is not needed/available when amqp=true
                // jmsDestination.setPubSubDomain(pubSubDomain);
                return jmsDestination;
            }
        });

        // factory.setConcurrency("1-1"); // Optional: configure concurrency
        // factory.setErrorHandler(t -> log.error("Error in JMS listener", t)); // Optional: Custom error handler
        return factory;
    }
} 