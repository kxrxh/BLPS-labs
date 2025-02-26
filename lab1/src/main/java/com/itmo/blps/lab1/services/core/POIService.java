package com.itmo.blps.lab1.services.core;

import com.itmo.blps.lab1.dto.PoiDto;
import com.itmo.blps.lab1.entities.*;
import com.itmo.blps.lab1.repositories.POIRepository;

import io.basc.framework.lang.NotFoundException;

import com.itmo.blps.lab1.repositories.AdvertisementPOIRepository;
import com.itmo.blps.lab1.repositories.AdvertisementRepository;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Optional;

@Service
public class POIService {

    @Autowired
    private POIRepository poiRepository;

    @Autowired
    private AdvertisementPOIRepository advertisementPOIRepository;

    @Autowired
    private AdvertisementRepository advertisementRepository;

    @Autowired
    private GeoService geoService;

    @Autowired
    private LocationService locationService;

    @Value("${poi.search-radius-meters:5000}")
    private Double defaultSearchRadius;

    public POI addPOI(PoiDto poiDto) {
        POI poi = POI.builder()
                .name(poiDto.getName())
                .type(poiDto.getType())
                .position(geoService.getPositionFromAddress(poiDto.getAddress(), poiDto.getCity()))
                .build();
        return poiRepository.save(poi);
    }

    public List<POI> getPOIs() {
        return poiRepository.findAll();
    }

    public Optional<POI> getPOIById(Long id) {
        return poiRepository.findById(id);
    }

    public Advertisement updateAdvertisementPOIs(Long id) {
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Advertisement not found: " + id));
        
        // Delete existing POI associations
        advertisementPOIRepository.deleteByAdvertisementId(id);
        
        // Find all POIs near the advertisement
        for (POIType poiType : POIType.values()) {
            List<POI> nearbyPOIs = locationService.findNearbyPOIsByType(
                    advertisement.getPosition().getLatitude(),
                    advertisement.getPosition().getLongitude(),
                    defaultSearchRadius,
                    poiType);

            // Create AdvertisementPOI entries for each nearby POI
            for (POI poi : nearbyPOIs) {
                Optional<AdvertisementPOI> existingAdPoi = advertisementPOIRepository
                    .findByAdvertisementIdAndPoiId(advertisement.getId(), poi.getId());
                
                if (existingAdPoi.isEmpty()) {
                    AdvertisementPOI adPoi = new AdvertisementPOI();
                    adPoi.setAdvertisement(advertisement);
                    adPoi.setPoi(poi);
                    // Calculate distance using LocationService
                    double distance = locationService.calculateDistance(
                            advertisement.getPosition().getLatitude(),
                            advertisement.getPosition().getLongitude(),
                            poi.getPosition().getLatitude(),
                            poi.getPosition().getLongitude());
                    adPoi.setDistanceInMeters(distance);
                    advertisementPOIRepository.save(adPoi);
                }
            }
        }
        return advertisement;
    }
}
