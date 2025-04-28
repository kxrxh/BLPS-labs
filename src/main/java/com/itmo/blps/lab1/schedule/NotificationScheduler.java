package com.itmo.blps.lab1.schedule;

import com.itmo.blps.lab1.messaging.StompNotificationProducer;
import com.itmo.blps.lab1.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final NotificationService notificationService;
    private final StompNotificationProducer stompProducer;

    // Run every minute for testing (fixedRate = 60000 ms)
    // TODO: Adjust schedule as needed (e.g., use cron expression for specific
    // times)
    @Scheduled(fixedRate = 60000)
    public void scheduleNotificationSending() {
        log.info("Running scheduled notification task...");
        try {
            String notification = notificationService.generateScheduledNotification();
            stompProducer.sendNotification(notification);
            log.info("Scheduled notification task completed.");
        } catch (Exception e) {
            log.error("Error during scheduled notification task", e);
        }
    }
}
