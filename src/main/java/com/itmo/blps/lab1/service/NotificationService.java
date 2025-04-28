package com.itmo.blps.lab1.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class NotificationService {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // TODO: Replace with actual logic to fetch pending notifications/receipts
    public String generateScheduledNotification() {
        return "Payment Reminder triggered at: " + LocalDateTime.now().format(formatter);
    }

}
