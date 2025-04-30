package com.itmo.blps.lab1.config;

import com.rabbitmq.jms.admin.RMQConnectionFactory;
import com.rabbitmq.jms.admin.RMQDestination;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import jakarta.jms.Queue;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableJms
public class JmsConfig {

    @Value("${spring.rabbitmq.host}")
    private String rabbitHost;

    @Value("${spring.rabbitmq.port}")
    private int rabbitPort;

    @Value("${spring.rabbitmq.username}")
    private String rabbitUsername;

    @Value("${spring.rabbitmq.password}")
    private String rabbitPassword;

    @Bean(name = "jmsQueue")
    public Queue queue() {
        RMQDestination jmsDestination = new RMQDestination();
        jmsDestination.setDestinationName(RabbitMQConfig.QUEUE_NAME);
        jmsDestination.setAmqpQueueName(RabbitMQConfig.QUEUE_NAME);
        jmsDestination.setAmqp(true);
        return jmsDestination;
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        RMQConnectionFactory connectionFactory = new RMQConnectionFactory();
        connectionFactory.setHost(rabbitHost);
        connectionFactory.setPort(rabbitPort);
        connectionFactory.setUsername(rabbitUsername);
        connectionFactory.setPassword(rabbitPassword);
        return connectionFactory;
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new SimpleMessageConverter());

        factory.setDestinationResolver(new DestinationResolver() {
            @Override
            public @NonNull Destination resolveDestinationName(@Nullable Session session,
                    @Nullable String destinationName, boolean pubSubDomain) throws JMSException {
                RMQDestination jmsDestination = new RMQDestination();
                // Set the name JMS uses
                jmsDestination.setDestinationName(destinationName);
                // Set the actual RabbitMQ queue name
                jmsDestination.setAmqpQueueName(destinationName);
                // Crucially, tell it to use AMQP interpretation
                jmsDestination.setAmqp(true);
                return jmsDestination;
            }
        });
        return factory;
    }

    // Define a Task Executor bean for asynchronous processing
    @Bean(name = "jmsMessageProcessorExecutor")
    public TaskExecutor jmsMessageProcessorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // Adjust pool size as needed
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("JmsMsgProc-");
        executor.initialize();
        return executor;
    }
}