package com.itmo.blps.labs.services.core;

import com.itmo.blps.labs.dto.PoiDto;
import com.itmo.blps.labs.entities.*;
import com.itmo.blps.labs.repositories.POIRepository;

import io.basc.framework.lang.NotFoundException;

import com.itmo.blps.labs.repositories.AdvertisementPOIRepository;
import com.itmo.blps.labs.repositories.AdvertisementRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.stream.Collectors;

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

    @Transactional
    public POI addPOI(PoiDto poiDto) {
        POI poi = POI.builder()
                .name(poiDto.getName())
                .type(poiDto.getType())
                .position(geoService.getPositionFromAddress(poiDto.getAddress(), poiDto.getCity()))
                .build();
        return poiRepository.save(poi);
    }

    @Transactional(readOnly = true)
    public List<POI> getPOIs() {
        return poiRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<POI> getPOIById(Long id) {
        return poiRepository.findById(id);
    }

    @Transactional
    public Advertisement updateAdvertisementPOIs(Long id) {
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Advertisement not found: " + id));

        // Get existing POIs
        Map<Long, AdvertisementPOI> existingPoisMap = advertisementPOIRepository.findByAdvertisementId(id)
                .stream()
                .collect(Collectors.toMap(poi -> poi.getPoi().getId(), poi -> poi));

        // Find and update nearby POIs
        for (POIType poiType : POIType.values()) {
            List<POI> nearbyPOIs = locationService.findNearbyPOIsByType(
                    advertisement.getPosition().getLatitude(),
                    advertisement.getPosition().getLongitude(),
                    defaultSearchRadius,
                    poiType);

            for (POI poi : nearbyPOIs) {
                AdvertisementPOI adPoi = existingPoisMap.computeIfAbsent(poi.getId(), k -> {
                    AdvertisementPOI newAdPoi = new AdvertisementPOI();
                    newAdPoi.setAdvertisement(advertisement);
                    newAdPoi.setPoi(poi);
                    return newAdPoi;
                });

                // Update distance
                double distance = locationService.calculateDistance(
                        advertisement.getPosition().getLatitude(),
                        advertisement.getPosition().getLongitude(),
                        poi.getPosition().getLatitude(),
                        poi.getPosition().getLongitude());
                adPoi.setDistanceInMeters(distance);
                advertisementPOIRepository.save(adPoi);
            }
        }

        return advertisement;
    }
}
