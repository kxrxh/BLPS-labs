package com.itmo.blps.lab1.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_NAME = "notifications.queue";
    public static final String EXCHANGE_NAME = "notifications.exchange";
    public static final String ROUTING_KEY = "notifications.key";

    @Bean
    Queue queue() {
        // Durable queue
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    DirectExchange exchange() {
        // Durable, non-auto-delete exchange
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    Binding binding(Queue queue, DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }
}
