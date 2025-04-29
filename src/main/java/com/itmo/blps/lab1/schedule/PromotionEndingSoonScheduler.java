package com.itmo.blps.lab1.schedule;

import com.itmo.blps.lab1.entities.Advertisement;
import com.itmo.blps.lab1.entities.User;
import com.itmo.blps.lab1.messaging.StompNotificationProducer;
import com.itmo.blps.lab1.repositories.AdvertisementRepository;
import com.itmo.blps.lab1.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PromotionEndingSoonScheduler {

    private final AdvertisementRepository advertisementRepository;
    private final NotificationService notificationService;
    private final StompNotificationProducer stompProducer;

    @Value("${promotion.reminder.minutes-before:1}")
    private int reminderMinutesBefore;

    @Scheduled(cron = "${promotion.scheduler.cron:0 */1 * * * ?}")
    @SchedulerLock(name = "checkPromotionsEndingSoon", lockAtMostFor = "1M", lockAtLeastFor = "10S")
    @Transactional(readOnly = true)
    public void checkPromotions() {
        log.info("Starting scheduled check to send STOMP notifications for promotions expiring within {} minutes...", reminderMinutesBefore);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderWindowEnd = now.plusMinutes(reminderMinutesBefore);

        List<Advertisement> adsToRemind = advertisementRepository
                .findActivePromotionsExpiringBetween(now, reminderWindowEnd);

        log.info("Found {} advertisements to send STOMP promotion reminder notifications for.", adsToRemind.size());

        for (Advertisement ad : adsToRemind) {
            User user = ad.getAuthor();
            if (user == null) {
                log.warn("Cannot send STOMP reminder for advertisement ID {}: author is missing.", ad.getId());
                continue;
            }

            try {
                String notificationPayload = notificationService.createPromotionEndingSoonNotification(user, ad);
                stompProducer.sendNotification(notificationPayload);
                log.info("Promotion reminder STOMP notification sent for advertisement ID {}", ad.getId());
            } catch (Exception e) {
                log.error("Failed to send promotion reminder STOMP notification for advertisement ID {}: {}",
                        ad.getId(), e.getMessage(), e);
            }
        }

        log.info("Scheduled STOMP notification check for promotions completed.");
    }
}
