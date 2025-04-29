package com.itmo.blps.lab1.schedule;

import com.itmo.blps.lab1.entities.Payment;
import com.itmo.blps.lab1.entities.PaymentStatus;
import com.itmo.blps.lab1.entities.User;
import com.itmo.blps.lab1.repositories.PaymentRepository;
import com.itmo.blps.lab1.service.EmailService;
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
    private final EmailService emailService; // Assuming EmailService exists and has a method like sendPaymentReminder

    @Value("${payment.reminder.days-after-creation:1}") // Configurable delay for reminder
    private int reminderDaysAfterCreation;

    // Example: Run daily at 2 AM
    @Scheduled(cron = "${payment.reminder.scheduler.cron:0 0 2 * * *}")
    @Transactional
    public void sendPaymentReminders() {
        log.info("Running payment reminder check...");
        LocalDateTime now = LocalDateTime.now();
        // Calculate the threshold for finding payments needing reminders
        LocalDateTime reminderThreshold = now.minus(reminderDaysAfterCreation, ChronoUnit.DAYS);

        // TODO: Define this method in PaymentRepository
        // Expected signature: findByStatusAndReminderSentFalseAndCreatedAtBefore(PaymentStatus status, LocalDateTime threshold)
        List<Payment> paymentsToRemind = paymentRepository
                .findByStatusAndReminderSentFalseAndCreatedAtBefore(PaymentStatus.PENDING, reminderThreshold); // Assuming PENDING status

        if (paymentsToRemind.isEmpty()) {
            log.info("No pending payments found needing reminders.");
            return;
        }

        log.info("Found {} payments needing reminders.", paymentsToRemind.size());

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
                log.info("Attempting to send reminder for Payment ID {} to user {}", payment.getId(), user.getUsername());
                // TODO: Ensure EmailService has this method or adapt call
                emailService.sendPaymentReminder(user, payment); // Assuming this method exists
                payment.setReminderSent(true);
                paymentRepository.save(payment);
                log.info("Successfully sent reminder for Payment ID {} and marked as sent.", payment.getId());
            } catch (Exception e) {
                log.error("Failed to send reminder for Payment ID {}: {}", payment.getId(), e.getMessage(), e);
                // Do not mark as sent, so it can be retried
            }
        }

        log.info("Payment reminder check finished.");
    }
} 