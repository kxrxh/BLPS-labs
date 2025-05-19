package com.itmo.blps.lab1.bpmn;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.itmo.blps.lab1.entities.Payment;
import com.itmo.blps.lab1.entities.User;
import com.itmo.blps.lab1.messaging.StompNotificationProducer;
import com.itmo.blps.lab1.repositories.UserRepository;
import com.itmo.blps.lab1.service.NotificationService;
import com.itmo.blps.lab1.services.core.PaymentService;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EmailWorker {

    @Autowired
    private ExternalTaskClient externalTaskClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private StompNotificationProducer stompProducer;

    @PostConstruct
    public void subscribe() {
        externalTaskClient.subscribe("email-send")
                .handler(this::handleSendEmail)
                .open();
    }

    private void handleSendEmail(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        Long userId = externalTask.getVariable("userId");
        Long paymentId = externalTask.getVariable("payment_id");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Payment payment = paymentService.getPaymentById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment == null) {
            externalTaskService.handleBpmnError(externalTask, "PAYMENT_NOT_FOUND", "Payment not found");
            return;
        }

        String notificationPayload = notificationService.createPaymentReceiptNotification(user, payment);
        if (notificationPayload != null) {
            stompProducer.sendNotification(notificationPayload);
            externalTaskService.complete(externalTask);
        } else {
            externalTaskService.handleBpmnError(externalTask, "EMAIL_SEND_ERROR", "Failed to send email");
        }
    }
}
