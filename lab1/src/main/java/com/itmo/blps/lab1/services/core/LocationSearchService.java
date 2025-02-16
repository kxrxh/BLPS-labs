package com.itmo.blps.lab1.services.core;

import com.itmo.blps.lab1.entities.Advertisement;
import com.itmo.blps.lab1.repositories.AdvertisementRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Service
public class LocationSearchService {
    
    @Autowired
    private AdvertisementRepository advertisementRepository;
    
    @Value("${search.default-radius-meters:5000}")
    private Double defaultSearchRadius;

    public List<Advertisement> findNearbyAdvertisements(Double latitude, Double longitude) {
        return advertisementRepository.findNearbyAdvertisements(latitude, longitude, defaultSearchRadius);
    }

    public List<Advertisement> findNearbyAdvertisements(Double latitude, Double longitude, Double radiusInMeters) {
        return advertisementRepository.findNearbyAdvertisements(latitude, longitude, radiusInMeters);
    }

    public List<Advertisement> findByLocation(String city, String district) {
        return advertisementRepository.findByPosition_CityAndPosition_District(city, district);
    }
} 