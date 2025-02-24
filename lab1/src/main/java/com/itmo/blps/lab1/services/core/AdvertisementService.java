package com.itmo.blps.lab1.services.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itmo.blps.lab1.dto.AdDto;
import com.itmo.blps.lab1.entities.Advertisement;
import com.itmo.blps.lab1.entities.Promotion;
import com.itmo.blps.lab1.repositories.AdvertisementRepository;

import io.basc.framework.lang.NotFoundException;

import java.util.List;
import java.util.Optional;

@Service
public class AdvertisementService {
    @Autowired
    private AdvertisementRepository advertisementRepository;

    @Autowired
    private POIService poiService;

    @Autowired
    private GeoService geoService;

    public Advertisement createAdvertisement(AdDto adDto) {
        Advertisement advertisement = Advertisement.builder()
                .name(adDto.getTitle())
                .description(adDto.getDescription())
                .price(adDto.getPrice())
                .realEstateType(adDto.getRealEstateType())
                .position(geoService.getPositionFromAddress(adDto.getAddress(), adDto.getCity()))
                .build();

        advertisementRepository.save(advertisement);

        return poiService.updateAdvertisementPOIs(advertisement.getId());
    }

    public Optional<Advertisement> getAdvertisementById(Long id) {
        return advertisementRepository.findById(id);
    }

    public List<Advertisement> getAllAdvertisements() {
        return advertisementRepository.findAll();
    }

    public Advertisement addPromotion(Long id, Promotion promotion) {
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Advertisement not found with id: " + id));

        advertisement.setPromotion(promotion);
        return advertisementRepository.save(advertisement);
    }

    public Advertisement removePromotion(Long id) {
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Advertisement not found with id: " + id));

        advertisement.setPromotion(null);
        advertisement.setIsPromoted(false);
        return advertisementRepository.save(advertisement);
    }
}
