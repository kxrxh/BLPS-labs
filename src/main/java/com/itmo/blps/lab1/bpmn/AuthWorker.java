package com.itmo.blps.lab1.bpmn;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

import com.itmo.blps.lab1.entities.User;
import com.itmo.blps.lab1.services.auth.JwtService;
import com.itmo.blps.lab1.services.core.UserService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.HashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthWorker {

    private final ExternalTaskClient externalTaskClient;
    private final JwtService jwtService;
    private final UserService userService;

    @PostConstruct
    public void subscribe() {
        externalTaskClient.subscribe("check-auth")
                .handler(this::handleAuthValidation)
                .open();
    }

    private void handleAuthValidation(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        try {
            String token = externalTask.getVariable("token");
            log.info("Received token: {}", token);
            if (token == null || !token.startsWith("Bearer ")) {
                log.warn("Invalid or missing Bearer token.");
                Map<String, Object> variables = new HashMap<>();
                variables.put("isAuthenticated", false);
                variables.put("userId", null);
                externalTaskService.complete(externalTask, variables);
                return;
            }

            String jwt = token.substring(7); // Remove "Bearer " prefix
            log.info("Extracted JWT: {}", jwt);
            String username = jwtService.getUsernameFromToken(jwt);
            log.info("Extracted username from token: {}", username);

            if (username == null) {
                log.warn("Username extracted from token is null.");
                Map<String, Object> variables = new HashMap<>();
                variables.put("isAuthenticated", false);
                variables.put("userId", null);
                externalTaskService.complete(externalTask, variables);
                return;
            }

            var userOpt = userService.getUserByUsername(username);
            log.info("User lookup result for username {}: {}", username, userOpt.isPresent() ? "Found" : "Not found");
            if (userOpt.isEmpty() || !jwtService.isTokenValid(jwt, userOpt.get())) {
                log.warn("User not found or token is invalid for user {}.", username);
                Map<String, Object> variables = new HashMap<>();
                variables.put("isAuthenticated", false);
                variables.put("userId", null);
                externalTaskService.complete(externalTask, variables);
                return;
            }

            User user = userOpt.get();
            log.info("Authentication successful for user: {}", user.getUsername());
            externalTaskService.complete(externalTask,
                    Map.of(
                            "isAuthenticated", true,
                            "userId", user.getId()));

        } catch (Exception e) {
            log.error("Error processing auth validation task", e);
            externalTaskService.handleBpmnError(externalTask, "AUTH_ERROR", "Authentication processing failed");
        }
    }

}
