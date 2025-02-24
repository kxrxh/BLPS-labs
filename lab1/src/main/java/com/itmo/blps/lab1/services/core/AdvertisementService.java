package com.itmo.blps.lab1.services.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.itmo.blps.lab1.dto.AdDto;
import com.itmo.blps.lab1.dto.NominatimResponse;
import com.itmo.blps.lab1.entities.Advertisement;
import com.itmo.blps.lab1.entities.Position;
import com.itmo.blps.lab1.entities.Promotion;
import com.itmo.blps.lab1.repositories.AdvertisementRepository;

import java.util.List;
import java.util.Optional;

@Service
public class AdvertisementService {
    @Autowired
    private AdvertisementRepository advertisementRepository;

    @Autowired
    private POIService poiService;

    public Advertisement createAdvertisement(AdDto adDto) {
        Advertisement advertisement = Advertisement.builder()
                .name(adDto.getTitle())
                .description(adDto.getDescription())
                .price(adDto.getPrice())
                .realEstateType(adDto.getRealEstateType())
                .position(getPositionFromAddress(adDto.getAddress(), adDto.getCity()))
                .build();

        advertisementRepository.save(advertisement);
        poiService.updateAdvertisementPOIs(advertisement);

        return advertisement;
    }

    private Position getPositionFromAddress(String address, String city) {
        String fullAddress = city + ", " + address;

        try {
            String encodedAddress = URLEncoder.encode(fullAddress, StandardCharsets.UTF_8.toString());
            String url = "https://nominatim.openstreetmap.org/search?format=json&q=" + encodedAddress;

            RestTemplate restTemplate = new RestTemplate();
            NominatimResponse[] responses = restTemplate.getForObject(url, NominatimResponse[].class);
            if (responses != null && responses.length > 0) {
                // Find response with highest importance
                NominatimResponse bestMatch = responses[0];
                for (NominatimResponse response : responses) {
                    if (response.getImportance() > bestMatch.getImportance()) {
                        bestMatch = response;
                    }
                }

                double lat = Double.parseDouble(bestMatch.getLat());
                double lon = Double.parseDouble(bestMatch.getLon());

                Position position = Position.builder()
                        .latitude(lat)
                        .longitude(lon)
                        .address(bestMatch.getDisplay_name()) // Use display_name instead of original address
                        .city(city)
                        .build();
                return position;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Position fallbackPosition = Position.builder()
                .latitude(41.0)
                .longitude(19.0)
                .address(address)
                .city(city)
                .build();

        double baseLat = 41.0;
        double latOffset = (Math.abs(address.hashCode() + city.hashCode()) % 3600) / 100.0;
        fallbackPosition.setLatitude(baseLat + latOffset);

        double baseLong = 19.0;
        double longOffset = (Math.abs(city.hashCode()) % 15000) / 100.0;
        fallbackPosition.setLongitude(baseLong + longOffset);

        fallbackPosition.setAddress(address);
        fallbackPosition.setCity(city);

        return fallbackPosition;
    }

    public Optional<Advertisement> getAdvertisementById(Long id) {
        return advertisementRepository.findById(id);
    }

    public List<Advertisement> getAllAdvertisements() {
        return advertisementRepository.findAll();
    }

    public Advertisement addPromotion(Long id, Promotion promotion) {
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Advertisement not found with id: " + id));

        advertisement.setPromotion(promotion);
        return advertisementRepository.save(advertisement);
    }

    public Advertisement removePromotion(Long id) {
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Advertisement not found with id: " + id));

        advertisement.setPromotion(null);
        return advertisementRepository.save(advertisement);
    }

    public void deleteAdvertisement(Long id) {
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Advertisement not found with id: " + id));

        advertisementRepository.delete(advertisement);
    }
}
