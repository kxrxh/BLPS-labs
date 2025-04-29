package com.itmo.blps.lab1.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class NotificationService {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // TODO: Implement logic to generate meaningful scheduled status updates/notifications for STOMP clients.
    public String generateScheduledNotification() {
        return "Payment Reminder triggered at: " + LocalDateTime.now().format(formatter);
    }

}
