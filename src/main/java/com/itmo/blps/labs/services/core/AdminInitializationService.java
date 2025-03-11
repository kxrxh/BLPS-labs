package com.itmo.blps.labs.services.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.itmo.blps.labs.entities.User;
import com.itmo.blps.labs.entities.Role;
import com.itmo.blps.labs.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminInitializationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${default.admin.username:admin}")
    private String defaultAdminUsername;

    @Value("${default.admin.password:admin12345}")
    private String defaultAdminPassword;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeAdmin() {
        if (!userRepository.findByUsername(defaultAdminUsername).isPresent()) {
            User adminUser = User.builder()
                    .username(defaultAdminUsername)
                    .password(passwordEncoder.encode(defaultAdminPassword))
                    .roles(Set.of(Role.ADMIN))
                    .build();
            userRepository.save(adminUser);
            System.out.println("Default admin user created with username: " + defaultAdminUsername);
        }
    }
} 