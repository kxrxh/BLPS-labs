package com.itmo.blps.lab1.schedule;

import com.itmo.blps.lab1.entities.Payment;
import com.itmo.blps.lab1.entities.PaymentStatus;
import com.itmo.blps.lab1.entities.User;
import com.itmo.blps.lab1.messaging.StompNotificationProducer;
import com.itmo.blps.lab1.repositories.PaymentRepository;
import com.itmo.blps.lab1.service.EmailService;
import com.itmo.blps.lab1.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentReminderScheduler {

    private final PaymentRepository paymentRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final StompNotificationProducer stompProducer;

    @Value("${payment.reminder.days-after-creation:1}") // Configurable delay for reminder
    private int reminderDaysAfterCreation;

    // Example: Run daily at 2 AM
    @Scheduled(cron = "${payment.reminder.cron:0 0 2 * * ?}")
    @Transactional(readOnly = true)
    public void checkForPaymentReminders() {
        log.info("Starting scheduled payment reminder check...");

        // Calculate threshold for reminders (e.g., payments created more than X days
        // ago)
        LocalDateTime reminderThreshold = LocalDateTime.now().minus(reminderDaysAfterCreation, ChronoUnit.DAYS);

        // Find pending payments that need reminders
        List<Payment> paymentsToRemind = paymentRepository.findByStatusAndReminderSentFalseAndCreatedAtBefore(
                PaymentStatus.PENDING, reminderThreshold);

        log.info("Found {} pending payments that need reminders", paymentsToRemind.size());

        for (Payment payment : paymentsToRemind) {
            User user = payment.getPayer();
            if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) {
                log.warn("Cannot send reminder for Payment ID {}: User or user email is missing.", payment.getId());
                // Decide if we should mark reminderSent = true anyway to avoid retries
                // payment.setReminderSent(true);
                // paymentRepository.save(payment);
                continue;
            }

            try {
                log.info("Sending reminder for Payment ID {} to user {}", payment.getId(), user.getUsername());

                // Create notification payload
                String notificationPayload = notificationService.createPaymentReminderNotification(user, payment);

                // Send via STOMP
                stompProducer.sendNotification(notificationPayload);

                // Don't mark as sent here to allow JMS consumer to handle it
                log.info("Payment reminder notification sent for Payment ID {}", payment.getId());
            } catch (Exception e) {
                log.error("Failed to send reminder for Payment ID {}: {}", payment.getId(), e.getMessage(), e);
                // Do not mark as sent, so it can be retried
            }
        }

        log.info("Payment reminder check finished.");
    }
}
