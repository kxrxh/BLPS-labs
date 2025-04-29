package com.itmo.blps.lab1.schedule;

import com.itmo.blps.lab1.entities.Advertisement;
import com.itmo.blps.lab1.entities.Promotion;
import com.itmo.blps.lab1.entities.User;
import com.itmo.blps.lab1.messaging.StompNotificationProducer;
import com.itmo.blps.lab1.repositories.AdvertisementRepository;
import com.itmo.blps.lab1.repositories.PaymentRepository;
import com.itmo.blps.lab1.repositories.PromotionRepository;
import com.itmo.blps.lab1.service.EmailService;
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

    private final AdvertisementRepository advertisementRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final StompNotificationProducer stompProducer;

    @Value("${promotion.reminder.days-before:3}")
    private int reminderDaysBefore;

    @Scheduled(cron = "${promotion.scheduler.cron}")
    @Transactional
    public void checkPromotions() {
        log.info("Starting scheduled check for advertisement promotions nearing expiration...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderWindowEnd = now.plusDays(reminderDaysBefore);

        List<Advertisement> adsToRemind = advertisementRepository
                .findAdvertisementsForPromotionExpirationReminder(now, reminderWindowEnd);

        log.info("Found {} advertisements to send promotion expiration reminders for.", adsToRemind.size());

        for (Advertisement ad : adsToRemind) {
            User user = ad.getAuthor();
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                log.warn("Cannot send reminder for advertisement ID {}: author or email is missing.", ad.getId());
                continue;
            }

            LocalDateTime expirationDate = ad.getStartDate().plusMinutes(ad.getDurationInMinutes());

            try {
                emailService.sendPromotionExpirationNotice(user, ad, expirationDate);
                log.info("Promotion expiration email sent for advertisement ID {}", ad.getId());
            } catch (Exception e) {
                log.error("Failed to send promotion expiration email for advertisement ID {}: {}", ad.getId(),
                        e.getMessage(), e);
            }

            try {
                String notificationPayload = notificationService.createPromotionExpirationNotification(user, ad,
                        expirationDate);
                stompProducer.sendNotification(notificationPayload);
                log.info("Promotion expiration STOMP notification sent for advertisement ID {}", ad.getId());
            } catch (Exception e) {
                log.error("Failed to send promotion expiration STOMP notification for advertisement ID {}: {}",
                        ad.getId(), e.getMessage(), e);
            }

            ad.setReminderSent(true);
            advertisementRepository.save(ad);

            log.info("Marked advertisement ID {} as reminder sent.", ad.getId());
        }

        log.info("Scheduled check for advertisement promotions completed.");
    }
}
