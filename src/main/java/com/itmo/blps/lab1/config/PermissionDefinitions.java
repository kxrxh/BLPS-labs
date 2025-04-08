package com.itmo.blps.lab1.config;

import com.itmo.blps.lab1.model.enums.Permission;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PermissionDefinitions {

    // Role Constants
    public static final String ROLE_USER = "USER";
    public static final String ROLE_MODERATOR = "MODERATOR";
    public static final String ROLE_ADMIN = "ADMIN";

    // Base permission sets for roles
    private static final Set<Permission> USER_PERMISSIONS = EnumSet.of(
            Permission.ADV_READ, Permission.ADV_CREATE, Permission.ADV_UPDATE, Permission.ADV_APPLY_PROMOTION,
            Permission.PROMO_READ,
            Permission.PAYMENT_READ_OWN, Permission.PAYMENT_PROCESS,
            Permission.POI_READ,
            Permission.LOC_READ);

    private static final Set<Permission> MODERATOR_PERMISSIONS = EnumSet.copyOf(
            // Start with USER permissions
            Stream.concat(USER_PERMISSIONS.stream(),
                    // Add Moderator specific permissions
                    Stream.of(
                            Permission.ADV_DELETE,
                            Permission.POI_CREATE,
                            Permission.POI_UPDATE,
                            Permission.POI_DELETE))
                    .collect(Collectors.toSet()));

    private static final Set<Permission> ADMIN_PERMISSIONS = EnumSet.allOf(Permission.class); // Admin gets all enum
                                                                                              // values

    // Expose mappings (Role Name -> Set of Permission Strings)
    public static final Map<String, Set<String>> ROLE_PERMISSIONS = Map.of(
            ROLE_USER, USER_PERMISSIONS.stream().map(Permission::toString).collect(Collectors.toUnmodifiableSet()),
            ROLE_MODERATOR,
            MODERATOR_PERMISSIONS.stream().map(Permission::toString).collect(Collectors.toUnmodifiableSet()),
            ROLE_ADMIN, ADMIN_PERMISSIONS.stream().map(Permission::toString).collect(Collectors.toUnmodifiableSet()));

    public static final List<String> ALL_PERMISSIONS = ADMIN_PERMISSIONS.stream()
            .map(Permission::toString)
            .collect(Collectors.toUnmodifiableList());

    private PermissionDefinitions() {
    }
}