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
            if (token == null || !token.startsWith("Bearer ")) {
                externalTaskService.complete(externalTask, 
                    Map.of(
                        "isAuthenticated", false,
                        "userId", null
                    )
                );
                return;
            }

            String jwt = token.substring(7); // Remove "Bearer " prefix
            String username = jwtService.getUsernameFromToken(jwt);

            if (username == null) {
                externalTaskService.complete(externalTask,
                    Map.of(
                        "isAuthenticated", false,
                        "userId", null
                    )
                );
                return;
            }

            var userOpt = userService.getUserByUsername(username);
            if (userOpt.isEmpty() || !jwtService.isTokenValid(jwt, userOpt.get())) {
                externalTaskService.complete(externalTask,
                    Map.of(
                        "isAuthenticated", false,
                        "userId", null
                    )
                );
                return;
            }

            User user = userOpt.get();
            externalTaskService.complete(externalTask,
                Map.of(
                    "isAuthenticated", true,
                    "userId", user.getId()
                )
            );

        } catch (Exception e) {
            log.error("Error processing auth validation task", e);
            externalTaskService.handleBpmnError(externalTask, "AUTH_ERROR", "Authentication processing failed");
        }
    }
    
}

