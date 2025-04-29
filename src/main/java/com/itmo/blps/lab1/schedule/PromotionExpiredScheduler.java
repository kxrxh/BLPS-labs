package com.itmo.blps.lab1.schedule;

import com.itmo.blps.lab1.entities.Advertisement;
import com.itmo.blps.lab1.entities.User;
import com.itmo.blps.lab1.messaging.StompNotificationProducer;
import com.itmo.blps.lab1.repositories.AdvertisementRepository;
import com.itmo.blps.lab1.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional; // Read-only transaction

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PromotionExpiredScheduler {

    private final AdvertisementRepository advertisementRepository;
    private final NotificationService notificationService;
    private final StompNotificationProducer stompProducer;

    @Scheduled(cron = "${promotion.expired.scheduler.cron:0 */2 * * * ?}")
    @Transactional(readOnly = true) // Read-only as we only query and send notifications
    public void checkExpiredPromotions() {
        log.info("Starting scheduled check for expired promotions to send STOMP notifications...");
        LocalDateTime now = LocalDateTime.now();

        // Find promotions that have expired as of 'now'
        List<Advertisement> expiredAdvertisements = advertisementRepository.findExpiredActivePromotions(now);

        log.info("Found {} advertisements with expired promotions.", expiredAdvertisements.size());

        for (Advertisement ad : expiredAdvertisements) {
            User user = ad.getAuthor();
            if (user == null) {
                log.warn("Cannot send STOMP expired notification for advertisement ID {}: author is missing.",
                        ad.getId());
                continue;
            }

            try {
                // Create the PROMOTION_EXPIRED notification payload
                String notificationPayload = notificationService.createPromotionExpiredNotification(user, ad);

                // Send notification via STOMP -> RabbitMQ
                stompProducer.sendNotification(notificationPayload);
                log.info("Promotion expired STOMP notification sent for advertisement ID {}", ad.getId());
            } catch (Exception e) {
                log.error("Failed to send promotion expired STOMP notification for advertisement ID {}: {}",
                        ad.getId(), e.getMessage(), e);
                // Continue with the next ad
            }
        }

        log.info("Scheduled STOMP notification check for expired promotions completed.");
    }
}
