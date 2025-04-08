package com.itmo.blps.lab1.security;

import com.itmo.blps.lab1.entities.Advertisement;
import com.itmo.blps.lab1.services.core.AdvertisementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    // Inject services needed for checks (e.g., AdvertisementService)
    @Autowired
    private AdvertisementService advertisementService; // Example service

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        // This version is typically used when the target object is available
        // Example: @PreAuthorize("hasPermission(#advertisement, 'edit')")
        if (authentication == null || targetDomainObject == null || !(permission instanceof String)) {
            return false;
        }

        if (targetDomainObject instanceof Advertisement) {
            Advertisement advertisement = (Advertisement) targetDomainObject;
            String perm = (String) permission;
            return checkAdvertisementPermission(authentication, advertisement.getId(), perm);
        }

        // Add checks for other domain objects (Promotion, Payment, etc.)

        return false; // Default deny
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        // This version is used when only the ID and type are available
        // Example: @PreAuthorize("hasPermission(#adId, 'Advertisement', 'edit')")
        if (authentication == null || targetType == null || !(permission instanceof String)) {
            return false;
        }

        String perm = (String) permission;

        if ("Advertisement".equalsIgnoreCase(targetType)) {
            Long adId = (Long) targetId;
            return checkAdvertisementPermission(authentication, adId, perm);
        }

        // Add checks for other target types (Promotion, Payment, etc.)

        return false; // Default deny
    }

    // --- Helper methods for specific checks ---

    private boolean checkAdvertisementPermission(Authentication authentication, Long advertisementId, String permission) {
        if (!(authentication instanceof UserAuthentication)) {
            return false; // Should not happen if filter is set up correctly
        }
        UserAuthentication userAuth = (UserAuthentication) authentication;
        Long userId = userAuth.getUserId();

        // Check basic authority first (if needed, though @PreAuthorize usually handles this)
        // boolean hasBasicPermission = authentication.getAuthorities().stream()
        //         .anyMatch(ga -> ga.getAuthority().equals("advertisement:" + permission));
        // if (!hasBasicPermission) return false;

        // Ownership check for specific permissions
        if ("edit".equals(permission) || "delete".equals(permission) || "apply_promotion".equals(permission)) {
            // Check if user owns the advertisement
            // NOTE: This might involve a DB call if ownership isn't loaded elsewhere
            // Optimization: Load ownership info if possible during initial load or cache it.
            return advertisementService.isOwner(userId, advertisementId);
        }

        // For 'read', basic permission might be enough, or add other logic
        if ("read".equals(permission)) {
           return true; // Assuming basic 'advertisement:read' authority check was sufficient
        }
        
        // Handle 'create' - typically doesn't involve a targetId
        // The hasAuthority('advertisement:create') check is usually sufficient.

        return false; // Deny other permissions by default
    }

     // Add similar check methods for Promotion, Payment, POI, etc.
     // private boolean checkPromotionPermission(...) { ... }
     // private boolean checkPaymentPermission(...) { ... }
} 