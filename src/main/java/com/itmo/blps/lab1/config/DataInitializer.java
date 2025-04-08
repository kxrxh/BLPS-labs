package com.itmo.blps.lab1.config;

import com.itmo.blps.lab1.entities.Permission;
import com.itmo.blps.lab1.entities.Role;
import com.itmo.blps.lab1.repositories.PermissionRepository;
import com.itmo.blps.lab1.repositories.RoleRepository;
import com.itmo.blps.lab1.repositories.UserRepository;
import com.itmo.blps.lab1.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

import com.itmo.blps.lab1.entities.PaymentProvider;
import com.itmo.blps.lab1.repositories.PaymentProviderRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PaymentProviderRepository paymentProviderRepository;

    @Value("${default.admin.username}")
    private String defaultAdminUsername;

    @Value("${default.admin.password}")
    private String defaultAdminPassword;

    @Override
    public void run(String... args) throws Exception {
        Map<String, Permission> persistedPermissions = PermissionDefinitions.ALL_PERMISSIONS.stream()
            .collect(Collectors.toMap(
                name -> name,
                name -> permissionRepository.findByName(name).orElseGet(() ->
                    permissionRepository.save(Permission.builder().name(name).build())
                )
            ));

        // Create or update roles and assign permissions based on definitions
        PermissionDefinitions.ROLE_PERMISSIONS.forEach((roleName, permissionNames) -> {
            Role role = roleRepository.findByName(roleName).orElseGet(() ->
                roleRepository.save(Role.builder().name(roleName).build())
            );

            // Map permission names to persisted Permission entities
            Set<Permission> currentRolePermissions = permissionNames.stream()
                .map(persistedPermissions::get) // Look up the persisted Permission object
                .collect(Collectors.toSet());

            role.setPermissions(currentRolePermissions);
            roleRepository.save(role);
        });

        createAdminUserIfNotExists();
        createDefaultPaymentProvidersIfNotExists();
    }

    private void createAdminUserIfNotExists() {
        boolean adminExists = userRepository.existsByUsername(defaultAdminUsername);
        if (!adminExists) {
            Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("ADMIN role not found. Ensure it is defined and created."));

            User adminUser = User.builder()
                .username(defaultAdminUsername)
                .password(passwordEncoder.encode(defaultAdminPassword))
                .roles(Set.of(adminRole))
                .build();

            userRepository.save(adminUser);
            log.info("Default admin user created with username: {}", defaultAdminUsername);
        } else {
            log.info("Admin user {} already exists.", defaultAdminUsername);
        }
    }

    private void createDefaultPaymentProvidersIfNotExists() {
        List<String> defaultProviders = List.of("Credit Card", "PayPal", "Bank Transfer");

        defaultProviders.forEach(name -> {
            // Check if provider already exists (case-insensitive check might be better)
            if (paymentProviderRepository.findAll().stream().noneMatch(p -> p.getName().equalsIgnoreCase(name))) {
                 PaymentProvider provider = new PaymentProvider();
                 provider.setName(name);
                 paymentProviderRepository.save(provider);
                 log.info("Created default payment provider: {}", name);
             }
        });
    }
} 