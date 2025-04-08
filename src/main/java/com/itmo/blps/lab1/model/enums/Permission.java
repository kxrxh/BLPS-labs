package com.itmo.blps.lab1.model.enums;

public enum Permission {
    // Advertisement
    ADV_READ("advertisement:read"),
    ADV_CREATE("advertisement:create"),
    ADV_UPDATE("advertisement:update"),
    ADV_DELETE("advertisement:delete"),
    ADV_APPLY_PROMOTION("advertisement:apply_promotion"),

    // Promotion
    PROMO_READ("promotion:read"),
    PROMO_CREATE("promotion:create"),
    PROMO_UPDATE("promotion:update"),
    PROMO_DELETE("promotion:delete"),

    // Payment
    PAYMENT_READ_OWN("payment:read_own"),
    PAYMENT_PROCESS("payment:process"),

    // POI
    POI_READ("poi:read"),
    POI_CREATE("poi:create"),
    POI_UPDATE("poi:update"),
    POI_DELETE("poi:delete"),

    // Location
    LOC_READ("location:read"),

    // Admin
    ADMIN_ACCESS("admin:access");

    private final String permissionName;

    Permission(String permissionName) {
        this.permissionName = permissionName;
    }

    @Override
    public String toString() {
        return permissionName;
    }
} 