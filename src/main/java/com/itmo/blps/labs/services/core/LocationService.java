package com.itmo.blps.labs.services.core;

import com.itmo.blps.labs.entities.Advertisement;
import com.itmo.blps.labs.entities.POI;
import com.itmo.blps.labs.entities.POIType;
import com.itmo.blps.labs.entities.Position;
import com.itmo.blps.labs.repositories.AdvertisementRepository;
import com.itmo.blps.labs.repositories.POIRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LocationService {

    @Autowired
    private AdvertisementRepository advertisementRepository;

    @Autowired
    private POIRepository poiRepository;

    private static final double EARTH_RADIUS_METERS = 6371000; // Earth's radius in meters

    public List<Advertisement> findNearbyAdvertisements(Double latitude, Double longitude, Double radiusInMeters) {
        List<Advertisement> allAdvertisements = advertisementRepository.findAll();
        
        return allAdvertisements.stream()
                .filter(ad -> {
                    Position pos = ad.getPosition();
                    double distance = calculateDistance(latitude, longitude, pos.getLatitude(), pos.getLongitude());
                    return distance <= radiusInMeters;
                })
                .sorted((ad1, ad2) -> {
                    Position pos1 = ad1.getPosition();
                    Position pos2 = ad2.getPosition();
                    double dist1 = calculateDistance(latitude, longitude, pos1.getLatitude(), pos1.getLongitude());
                    double dist2 = calculateDistance(latitude, longitude, pos2.getLatitude(), pos2.getLongitude());
                    return Double.compare(dist1, dist2);
                })
                .collect(Collectors.toList());
    }

    public List<POI> findNearbyPOIsByType(Double latitude, Double longitude, Double radiusInMeters, POIType poiType) {
        List<POI> poisOfType = poiRepository.findByType(poiType);
        
        return poisOfType.stream()
                .filter(poi -> {
                    Position pos = poi.getPosition();
                    double distance = calculateDistance(latitude, longitude, pos.getLatitude(), pos.getLongitude());
                    return distance <= radiusInMeters;
                })
                .sorted((poi1, poi2) -> {
                    Position pos1 = poi1.getPosition();
                    Position pos2 = poi2.getPosition();
                    double dist1 = calculateDistance(latitude, longitude, pos1.getLatitude(), pos1.getLongitude());
                    double dist2 = calculateDistance(latitude, longitude, pos2.getLatitude(), pos2.getLongitude());
                    return Double.compare(dist1, dist2);
                })
                .collect(Collectors.toList());
    }

    /**
     * Calculate distance between two points using the Haversine formula.
     * @return Distance in meters
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Convert to radians
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Differences
        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;

        // Haversine formula
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        // Calculate distance
        return EARTH_RADIUS_METERS * c;
    }
} 