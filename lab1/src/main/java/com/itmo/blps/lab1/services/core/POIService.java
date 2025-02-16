package com.itmo.blps.lab1.services.core;

import com.itmo.blps.lab1.entities.*;
import com.itmo.blps.lab1.repositories.POIRepository;
import com.itmo.blps.lab1.repositories.AdvertisementPOIRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Service
public class POIService {
    
    @Autowired
    private POIRepository poiRepository;
    
    @Autowired
    private AdvertisementPOIRepository advertisementPOIRepository;
    
    @Value("${poi.search-radius-meters:1000}")
    private Double defaultSearchRadius;

    public void updateAdvertisementPOIs(Advertisement advertisement) {
        // Find all POIs near the advertisement
        for (POIType poiType : POIType.values()) {
            List<POI> nearbyPOIs = poiRepository.findNearbyPOIsByType(
                advertisement.getPosition().getLatitude(),
                advertisement.getPosition().getLongitude(),
                defaultSearchRadius,
                poiType.name()
            );
            
            // Create AdvertisementPOI entries for each nearby POI
            for (POI poi : nearbyPOIs) {
                AdvertisementPOI adPoi = new AdvertisementPOI();
                adPoi.setAdvertisement(advertisement);
                adPoi.setPoi(poi);
                // Calculate distance
                double distance = calculateDistance(
                    advertisement.getPosition().getLatitude(),
                    advertisement.getPosition().getLongitude(),
                    poi.getLatitude(),
                    poi.getLongitude()
                );
                adPoi.setDistanceInMeters(distance);
                advertisementPOIRepository.save(adPoi);
            }
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth's radius in kilometers

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c * 1000; // Convert to meters
    }
} 