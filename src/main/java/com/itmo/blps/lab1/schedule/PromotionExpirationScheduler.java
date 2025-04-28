package com.itmo.blps.lab1.schedule;

import com.itmo.blps.lab1.entities.Payment;
import com.itmo.blps.lab1.entities.PaymentStatus;
import com.itmo.blps.lab1.entities.Promotion;
import com.itmo.blps.lab1.entities.User;
import com.itmo.blps.lab1.repositories.PaymentRepository;
import com.itmo.blps.lab1.repositories.PromotionRepository;
import com.itmo.blps.lab1.service.EmailService;
import com.itmo.blps.lab1.services.core.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PromotionExpirationScheduler {

    private final PromotionRepository promotionRepository;
    private final PromotionService promotionService;
    private final EmailService emailService;
    private final PaymentRepository paymentRepository; // Inject PaymentRepository

    @Value("${promotion.reminder.days-before:3}")
    private int reminderDaysBefore;

    @Scheduled(cron = "${promotion.scheduler.cron:0 * * * * *}")
    @Transactional
    public void checkPromotions() {
        log.info("Running promotion expiration check...");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderThreshold = now.plusDays(reminderDaysBefore);

        // --- Process Reminders ---
        // Use correct repository method name
        List<Promotion> promotionsNeedingReminder = promotionRepository
                .findByIsActiveTrueAndReminderSentFalseAndExpirationDateBefore(reminderThreshold);
        for (Promotion promotion : promotionsNeedingReminder) {
            try {
                User userToNotify = findUserForPromotion(promotion);
                if (userToNotify != null) {
                    emailService.sendPromotionReminder(userToNotify, promotion);
                    promotion.setReminderSent(true);
                    promotionRepository.save(promotion);
                    log.info("Sent reminder for promotion ID {}", promotion.getId());
                } else {
                    log.warn("Could not find user for promotion ID {} to send reminder.", promotion.getId());
                    // Optionally: Mark reminder as sent anyway to prevent spamming logs?
                    // promotion.setReminderSent(true);
                    // promotionRepository.save(promotion);
                }
            } catch (Exception e) {
                log.error("Error processing reminder for promotion ID {}: {}", promotion.getId(), e.getMessage(), e);
            }
        }

        // --- Process Expirations ---
        // Use correct repository method name
        List<Promotion> expiredPromotions = promotionRepository.findByIsActiveTrueAndExpirationDateBefore(now);
        for (Promotion promotion : expiredPromotions) {
            try {
                log.info("Deactivating expired promotion ID {}", promotion.getId());
                // Deactivate promotion (PromotionService handles the email sending)
                promotionService.deactivatePromotion(promotion.getId());
            } catch (Exception e) {
                log.error("Error deactivating promotion ID {}: {}", promotion.getId(), e.getMessage(), e);
            }
        }

        log.info("Promotion expiration check finished.");
    }

    // Implementation for finding the user associated with the promotion activation
    private User findUserForPromotion(Promotion promotion) {
        // Find the latest successful payment for this specific promotion
        Optional<Payment> paymentOpt = paymentRepository
                .findFirstByPromotionIdAndStatusOrderByCreatedAtDesc(promotion.getId(), PaymentStatus.SUCCESS);

        if (paymentOpt.isEmpty()) {
            log.warn("No successful payment found for promotion ID {} to determine user.", promotion.getId());
            return null;
        }

        User payer = paymentOpt.get().getPayer();
        if (payer == null) {
            log.warn("Successful payment ID {} for promotion ID {} has no associated payer.", paymentOpt.get().getId(),
                    promotion.getId());
        }
        return payer;
    }
}
