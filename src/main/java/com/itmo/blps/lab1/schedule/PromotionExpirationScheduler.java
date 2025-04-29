package com.itmo.blps.lab1.schedule;

import com.itmo.blps.lab1.entities.Promotion;
import com.itmo.blps.lab1.entities.User;
import com.itmo.blps.lab1.messaging.StompNotificationProducer;
import com.itmo.blps.lab1.repositories.PaymentRepository;
import com.itmo.blps.lab1.repositories.PromotionRepository;
import com.itmo.blps.lab1.service.NotificationService;
import com.itmo.blps.lab1.services.core.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PromotionExpirationScheduler {

    private final PromotionRepository promotionRepository;
    private final PromotionService promotionService;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;
    private final StompNotificationProducer stompProducer;

    @Value("${promotion.reminder.days-before:3}")
    private int reminderDaysBefore;

    @Scheduled(cron = "${promotion.scheduler.cron}")
    @Transactional(readOnly = true)
    public void checkPromotions() {
        log.info("Starting scheduled check for promotions...");

        // Get current time
        LocalDateTime now = LocalDateTime.now();
        // Calculate the date for sending reminders
        LocalDateTime reminderDate = now.plusDays(reminderDaysBefore);

        // Find active promotions expiring in the reminder window that haven't had
        // reminders sent
        List<Promotion> promotionsToRemind = promotionRepository.findActivePromotionsExpiringBetweenAndNoReminder(
                now, reminderDate);

        log.info("Found {} promotions to send reminders for", promotionsToRemind.size());

        // Send reminders for promotions
        for (Promotion promotion : promotionsToRemind) {
            try {
                // Find user for this promotion via payment
                User user = findUserForPromotion(promotion);
                if (user == null) {
                    log.warn("Could not find user for promotion ID {}, skipping reminder", promotion.getId());
                    continue;
                }

                // Create notification payload
                String notificationPayload = notificationService.createPromotionReminderNotification(user, promotion);

                // Send via STOMP
                stompProducer.sendNotification(notificationPayload);

                log.info("Promotion reminder notification sent for promotion ID {}", promotion.getId());

                // Mark reminder as sent in the database (will be done by the JMS consumer when
                // processing)
                // Not updating here to avoid race conditions and allow consistent updating in
                // the consumer
            } catch (Exception e) {
                log.error("Error sending reminder for promotion ID {}: {}", promotion.getId(), e.getMessage(), e);
                // Continue with next promotion
            }
        }

        // Find expired promotions
        List<Promotion> expiredPromotions = promotionRepository.findActivePromotionsExpiredBefore(now);

        log.info("Found {} expired promotions to deactivate", expiredPromotions.size());

        // Deactivate expired promotions
        for (Promotion promotion : expiredPromotions) {
            try {
                User user = findUserForPromotion(promotion);
                if (user != null) {
                    // Create notification payload
                    String notificationPayload = notificationService.createPromotionDeactivationNotification(user,
                            promotion);

                    // Send via STOMP
                    stompProducer.sendNotification(notificationPayload);

                    log.info("Promotion deactivation notification sent for promotion ID {}", promotion.getId());
                }

                // Deactivate promotion
                promotionService.deactivatePromotion(promotion.getId());
                log.info("Promotion ID {} deactivated due to expiration", promotion.getId());
            } catch (Exception e) {
                log.error("Error deactivating promotion ID {}: {}", promotion.getId(), e.getMessage(), e);
                // Continue with next promotion
            }
        }

        log.info("Scheduled check for promotions completed");
    }

    private User findUserForPromotion(Promotion promotion) {
        return paymentRepository.findLatestSuccessfulPaymentByPromotionId(promotion.getId())
                .map(payment -> payment.getPayer())
                .orElse(null);
    }
}
