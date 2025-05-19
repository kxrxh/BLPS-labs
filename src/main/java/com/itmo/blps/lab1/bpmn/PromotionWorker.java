package com.itmo.blps.lab1.bpmn;

import java.util.List;
import java.util.Map;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmo.blps.lab1.dto.AdvertisementResponseDto;
import com.itmo.blps.lab1.dto.PaymentDto;
import com.itmo.blps.lab1.entities.Payment;
import com.itmo.blps.lab1.entities.PaymentProvider;
import com.itmo.blps.lab1.entities.Promotion;
import com.itmo.blps.lab1.services.core.AdvertisementService;
import com.itmo.blps.lab1.services.core.PaymentService;
import com.itmo.blps.lab1.services.core.PromotionService;

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
    }

    private void handleGetPlans(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        List<Promotion> promotions = promotionService.getActivePromotions();
        try {
            externalTaskService.complete(externalTask,
                    Map.of("plans", new ObjectMapper().writeValueAsString(promotions)));
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
                    Map.of("providers", new ObjectMapper().writeValueAsString(paymentProviders)));
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
            externalTaskService.handleBpmnError(externalTask, "APPLY_ERROR", "Advertisement not found");
            return;
        }

        Promotion promotion = promotionService.getPromotionById(planId).orElseThrow(() -> new RuntimeException("Promotion not found"));
        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setProviderId(providerId);
        paymentDto.setAdvertisementId(advertisementId);
        paymentDto.setAmount(promotion.getPrice());
        try {
            Payment payment = paymentService.createAndProcessPayment(paymentDto, userId);
            if (payment != null) {
                externalTaskService.complete(externalTask, Map.of("payment_id", payment.getId()));
            } else {
                externalTaskService.handleBpmnError(externalTask, "APPLY_ERROR", "Payment failed");
            }
        } catch (Exception e) {
            log.error("Error applying promotion", e);
            externalTaskService.handleBpmnError(externalTask, "APPLY_ERROR", e.getMessage());
        }
    }
}
