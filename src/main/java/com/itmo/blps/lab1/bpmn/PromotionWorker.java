package com.itmo.blps.lab1.bpmn;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmo.blps.lab1.dto.AdvertisementResponseDto;
import com.itmo.blps.lab1.dto.PaymentDto;
import com.itmo.blps.lab1.entities.Payment;
import com.itmo.blps.lab1.entities.PaymentProvider;
import com.itmo.blps.lab1.entities.Promotion;
import com.itmo.blps.lab1.services.core.AdvertisementService;
import com.itmo.blps.lab1.services.core.PaymentService;
import com.itmo.blps.lab1.services.core.PromotionService;
import com.itmo.blps.lab1.repositories.AdvertisementRepository;
import com.itmo.blps.lab1.repositories.UserRepository;
import com.itmo.blps.lab1.service.NotificationService;
import com.itmo.blps.lab1.messaging.StompNotificationProducer;
import com.itmo.blps.lab1.entities.Advertisement;
import com.itmo.blps.lab1.entities.User;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PromotionWorker {

    @Autowired
    private ExternalTaskClient externalTaskClient;

    @Autowired
    private PromotionService promotionService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AdvertisementService advertisementService;

    @Autowired
    private AdvertisementRepository advertisementRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private StompNotificationProducer stompProducer;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${promotion.reminder.minutes-before:1}")
    private int reminderMinutesBefore;

    @PostConstruct
    public void subscribe() {
        externalTaskClient.subscribe("prom-get-plans")
                .handler(this::handleGetPlans)
                .open();
        externalTaskClient.subscribe("prom-get-providers")
                .handler(this::handleGetProviders)
                .open();
        externalTaskClient.subscribe("prom-apply")
                .handler(this::handleApply)
                .open();
        externalTaskClient.subscribe("prom-find-expired")
                .handler(this::handleFindExpired)
                .open();
        externalTaskClient.subscribe("prom-find-expired-soon")
                .handler(this::handleFindExpiredSoon)
                .open();
        externalTaskClient.subscribe("prom-send-expired-notification")
                .handler(this::handleSendExpiredNotification)
                .open();
    }

    private void handleGetPlans(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        List<Promotion> promotions = promotionService.getActivePromotions();
        try {
            externalTaskService.complete(externalTask,
                    Map.of("plans", objectMapper.writeValueAsString(promotions)));
        } catch (JsonProcessingException e) {
            // Unexpected behavior
            log.error("Error getting plans", e);
            externalTaskService.handleBpmnError(externalTask, "GET_PLANS_ERROR", e.getMessage());
        }
    }

    private void handleGetProviders(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        List<PaymentProvider> paymentProviders = paymentService.getAvailableProviders();
        try {
            externalTaskService.complete(externalTask,
                    Map.of("providers", objectMapper.writeValueAsString(paymentProviders)));
        } catch (JsonProcessingException e) {
            log.error("Error getting providers", e);
            externalTaskService.handleBpmnError(externalTask, "GET_PROVIDERS_ERROR", e.getMessage());
        }
    }

    private void handleApply(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        Long planId = externalTask.getVariable("plan_id");
        Long providerId = externalTask.getVariable("provider_id");
        Long advertisementId = externalTask.getVariable("adv_id");
        Long userId = externalTask.getVariable("userId");
        // Set promotion id to advertisement id
        AdvertisementResponseDto advertisement = advertisementService.addPromotion(advertisementId, planId);
        if (advertisement == null) {
            externalTaskService.handleBpmnError(externalTask, "410", "Advertisement not found");
            return;
        }

        Promotion promotion = promotionService.getPromotionById(planId)
                .orElseThrow(() -> new RuntimeException("Promotion not found"));
        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setProviderId(providerId);
        paymentDto.setAdvertisementId(advertisementId);
        paymentDto.setAmount(promotion.getPrice());
        try {
            Payment payment = paymentService.createAndProcessPayment(paymentDto, userId, false);
            if (payment != null) {
                externalTaskService.complete(externalTask, Map.of("payment_id", payment.getId()));
            } else {
                externalTaskService.handleBpmnError(externalTask, "503", "Payment failed");
            }
        } catch (Exception e) {
            log.error("Error applying promotion", e);
            externalTaskService.handleBpmnError(externalTask, "503", e.getMessage());
        }
    }

    private void handleFindExpired(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        log.info("Handling task for topic: prom-find-expired");
        LocalDateTime now = LocalDateTime.now();

        List<Advertisement> expiredAdvertisements = advertisementRepository.findExpiredActivePromotions(now);

        log.info("Found {} advertisements with expired promotions.", expiredAdvertisements.size());

        List<Map<String, Object>> expiredAdsData = new ArrayList<>();
        for (Advertisement ad : expiredAdvertisements) {
            User user = ad.getAuthor();
            if (user == null) {
                log.warn("Cannot process expired notification for advertisement ID {}: author is missing.",
                        ad.getId());
                continue;
            }
            Map<String, Object> adData = new HashMap<>();
            adData.put("advertisementId", ad.getId());
            adData.put("userId", user.getId());
            // You might need more data depending on what the notification requires
            expiredAdsData.add(adData);
        }

        try {
            externalTaskService.complete(externalTask,
                    Map.of("expiredAdsList", objectMapper.writeValueAsString(expiredAdsData), "type",
                            "expired"));
        } catch (JsonProcessingException e) {
            log.error("Error serializing expired ads data", e);
            externalTaskService.handleBpmnError(externalTask, "FIND_EXPIRED_ERROR", e.getMessage());
        }
    }

    private void handleFindExpiredSoon(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderWindowEnd = now.plusMinutes(reminderMinutesBefore);

        List<Advertisement> expiredSoonAdvertisements = advertisementRepository
                .findActivePromotionsExpiringBetween(now, reminderWindowEnd);

        log.info("Found {} advertisements with expired promotions in the next 3 days.",
                expiredSoonAdvertisements.size());

        List<Map<String, Object>> expiredAdsData = new ArrayList<>();

        for (Advertisement ad : expiredSoonAdvertisements) {
            User user = ad.getAuthor();
            if (user == null) {
                log.warn("Cannot process expired soon notification for advertisement ID {}: author is missing.",
                        ad.getId());
                continue;
            }

            Map<String, Object> adData = new HashMap<>();
            adData.put("advertisementId", ad.getId());
            adData.put("userId", user.getId());
            expiredAdsData.add(adData);
        }

        try {
            externalTaskService.complete(externalTask,
                    Map.of("expiredAdsList", objectMapper.writeValueAsString(expiredAdsData), "type",
                            "soon_expired"));
        } catch (JsonProcessingException e) {
            log.error("Error serializing expired ads data", e);
            externalTaskService.handleBpmnError(externalTask, "FIND_EXPIRED_ERROR", e.getMessage());
        }
    }

    private void handleSendExpiredNotification(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        String expiredAdsJson = externalTask.getVariable("expiredAdsList");
        String type = externalTask.getVariable("type");
        if (expiredAdsJson == null) {
            log.error("Missing expiredAdsList in task variables.");
            externalTaskService.handleBpmnError(externalTask, "SEND_NOTIFICATION_ERROR", "Missing expiredAdsList");
            return;
        }

        List<Map<String, Object>> expiredAdsData;
        try {
            expiredAdsData = objectMapper.readValue(expiredAdsJson, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Error parsing expiredAdsList JSON", e);
            externalTaskService.handleBpmnError(externalTask, "SEND_NOTIFICATION_ERROR", "Invalid expiredAdsList JSON");
            return;
        }

        if (expiredAdsData == null || expiredAdsData.isEmpty()) {
            log.info("No ads data to process.");
            externalTaskService.complete(externalTask);
            return;
        }

        for (Map<String, Object> adData : expiredAdsData) {
            Long advertisementId = null;
            Long userId = null;

            // Safely extract Longs from the map
            if (adData.containsKey("advertisementId")) {
                Object adIdObj = adData.get("advertisementId");
                if (adIdObj instanceof Number) {
                    advertisementId = ((Number) adIdObj).longValue();
                } else if (adIdObj instanceof String) {
                    try {
                        advertisementId = Long.parseLong((String) adIdObj);
                    } catch (NumberFormatException ignored) {
                        log.error("Error parsing advertisementId", ignored);
                        externalTaskService.handleBpmnError(externalTask, "SEND_NOTIFICATION_ERROR",
                                "Invalid advertisementId");
                        /* Handle below */ }
                }
            }
            if (adData.containsKey("userId")) {
                Object userIdObj = adData.get("userId");
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else if (userIdObj instanceof String) {
                    try {
                        userId = Long.parseLong((String) userIdObj);
                    } catch (NumberFormatException ignored) {
                        log.error("Error parsing userId", ignored);
                        externalTaskService.handleBpmnError(externalTask, "SEND_NOTIFICATION_ERROR",
                                "Invalid userId");
                        /* Handle below */ }
                }
            }

            if (advertisementId == null || userId == null) {
                log.warn("Skipping notification for invalid ad data: {}", adData);
                continue;
            }

            // Retrieve Advertisement and User entities
            Advertisement ad = advertisementRepository.findById(advertisementId).orElse(null);
            User user = userRepository.findById(userId).orElse(null);
            if (ad != null) {
                user = ad.getAuthor();
            }

            if (ad == null || user == null) {
                log.warn("Advertisement ID {} or its author not found for sending notification.", advertisementId);
                continue; // Skip to the next ad in the list
            }

            try {
                String notificationPayload = null;
                if (type.equals("expired")) {
                    notificationPayload = notificationService.createPromotionExpiredNotification(user, ad);
                    log.info("Promotion expired STOMP notification sent for advertisement ID {}", advertisementId);
                } else if (type.equals("soon_expired")) {
                    notificationPayload = notificationService.createPromotionEndingSoonNotification(user, ad);
                    log.info("Promotion ending soon STOMP notification sent for advertisement ID {}", advertisementId);
                }
                stompProducer.sendNotification(notificationPayload);
            } catch (Exception e) {
                log.error("Failed to send promotion expired STOMP notification for advertisement ID {}: {}",
                        advertisementId, e.getMessage(), e);
            }
        }

        externalTaskService.complete(externalTask);
    }
}
