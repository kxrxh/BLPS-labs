package com.itmo.blps.lab1.service;

public enum NotificationType {
    PAYMENT_RECEIPT,
    PROMOTION_ENDING_SOON,
    PROMOTION_EXPIRED;

    public static NotificationType fromString(String type) {
        return NotificationType.valueOf(type.toUpperCase());
    }
}
