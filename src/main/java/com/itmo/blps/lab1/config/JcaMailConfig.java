package com.itmo.blps.lab1.config;

import com.itmo.blps.lab1.jca.SmtpConnectionFactory;
import com.itmo.blps.lab1.jca.SmtpManagedConnectionFactory;
import jakarta.resource.ResourceException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SmtpManagedConnectionFactory.class) // Ensures properties are bound and creates the bean
public class JcaMailConfig {

    @Bean
    public SmtpConnectionFactory smtpConnectionFactory(SmtpManagedConnectionFactory managedConnectionFactory)
            throws ResourceException {
        // Create the connection factory using the managed factory bean.
        // Passing null for ConnectionManager means no container-managed pooling.
        // Your application code will get raw connections.
        return (SmtpConnectionFactory) managedConnectionFactory.createConnectionFactory(null);
    }
}
