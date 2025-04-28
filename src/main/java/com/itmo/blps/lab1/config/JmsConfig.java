package com.itmo.blps.lab1.config;

import com.rabbitmq.jms.admin.RMQConnectionFactory;
import jakarta.jms.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;

@Configuration
@EnableJms
public class JmsConfig {

    @Value("${spring.rabbitmq.host}")
    private String rabbitHost;

    @Value("${spring.rabbitmq.port}")
    private int rabbitAmqpPort; // Use AMQP port (default 5672)

    @Value("${spring.rabbitmq.username}")
    private String rabbitUsername;

    @Value("${spring.rabbitmq.password}")
    private String rabbitPassword;

    @Bean
    public ConnectionFactory rabbitJmsConnectionFactory() {
        RMQConnectionFactory connectionFactory = new RMQConnectionFactory();
        connectionFactory.setHost(rabbitHost);
        connectionFactory.setPort(rabbitAmqpPort); // Use the injected port variable
        connectionFactory.setUsername(rabbitUsername);
        connectionFactory.setPassword(rabbitPassword);
        connectionFactory.setVirtualHost("/"); // Default virtual host
        // connectionFactory.setUseSSL(false); // Configure SSL if needed
        return connectionFactory;
    }

    @Bean
    public JmsListenerContainerFactory<?> jmsListenerContainerFactory(
            ConnectionFactory connectionFactory,
            DefaultJmsListenerContainerFactoryConfigurer configurer) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        // This provides all boot's default to this factory, including the message
        // converter
        configurer.configure(factory, connectionFactory);
        // You could still override some of Boot's default if necessary.
        // factory.setMessageConverter(jacksonJmsMessageConverter()); // Example
        // override
        return factory;
    }

    // Optional: Define a Jackson MessageConverter if needed for automatic JSON
    // conversion
    /*
     * @Bean
     * public MessageConverter jacksonJmsMessageConverter() {
     * MappingJackson2MessageConverter converter = new
     * MappingJackson2MessageConverter();
     * converter.setTargetType(MessageType.TEXT);
     * converter.setTypeIdPropertyName("_type");
     * return converter;
     * }
     */
}
