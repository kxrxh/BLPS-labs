package com.itmo.blps.lab1.services.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itmo.blps.lab1.dto.AdDto;
import com.itmo.blps.lab1.dto.AdvertisementResponseDto;
import com.itmo.blps.lab1.entities.Advertisement;
import com.itmo.blps.lab1.entities.Promotion;
import com.itmo.blps.lab1.entities.User;
import com.itmo.blps.lab1.exception.BadRequestException;
import com.itmo.blps.lab1.entities.AdvertisementPOI;
import com.itmo.blps.lab1.repositories.AdvertisementRepository;
import com.itmo.blps.lab1.repositories.PromotionRepository;
import com.itmo.blps.lab1.repositories.AdvertisementPOIRepository;

import io.basc.framework.lang.NotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdvertisementService {
    @Autowired
    private AdvertisementRepository advertisementRepository;

    @Autowired
    private PromotionRepository promotionRepository;
    @Autowired
    private POIService poiService;

    @Autowired
    private GeoService geoService;

    @Autowired
    private AdvertisementPOIRepository advertisementPOIRepository;

    @Transactional
    public AdvertisementResponseDto createAdvertisement(AdDto adDto) {
        Advertisement advertisement = Advertisement.builder()
                .name(adDto.getTitle())
                .description(adDto.getDescription())
                .price(adDto.getPrice())
                .realEstateType(adDto.getRealEstateType())
                .position(geoService.getPositionFromAddress(adDto.getAddress(), adDto.getCity()))
                .build();

        advertisement = advertisementRepository.save(advertisement);
        advertisement = poiService.updateAdvertisementPOIs(advertisement.getId());
        List<AdvertisementPOI> pois = advertisementPOIRepository.findByAdvertisementId(advertisement.getId());

        return AdvertisementResponseDto.fromEntity(advertisement, pois);
    }

    @Transactional(readOnly = true)
    public Optional<AdvertisementResponseDto> getAdvertisementById(Long id) {
        Optional<Advertisement> advertisement = advertisementRepository.findById(id);
        if (advertisement.isPresent()) {
            poiService.updateAdvertisementPOIs(advertisement.get().getId());
            List<AdvertisementPOI> pois = advertisementPOIRepository.findByAdvertisementId(id);
            return Optional.of(AdvertisementResponseDto.fromEntity(advertisement.get(), pois));
        }
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public List<AdvertisementResponseDto> getAllAdvertisements() {
        List<Advertisement> advertisements = advertisementRepository.findAll();
        return advertisements.stream()
                .map(ad -> {
                    poiService.updateAdvertisementPOIs(ad.getId());
                    List<AdvertisementPOI> pois = advertisementPOIRepository.findByAdvertisementId(ad.getId());
                    return AdvertisementResponseDto.fromEntity(ad, pois);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public AdvertisementResponseDto addPromotion(Long id, Long promotionId) {
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Advertisement not found with id: " + id));

        if (advertisement.getIsPromoted()) {
            throw new BadRequestException("Advertisement is already promoted. First remove the promotion and then add a new one.");
        }

        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new NotFoundException("Promotion not found with id: " + promotionId));

        advertisement.setPromotion(promotion);
        advertisement.setIsPromoted(false);
        advertisement = advertisementRepository.save(advertisement);
        List<AdvertisementPOI> pois = advertisementPOIRepository.findByAdvertisementId(id);
        return AdvertisementResponseDto.fromEntity(advertisement, pois);
    }

    @Transactional
    public AdvertisementResponseDto removePromotion(Long id) {
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Advertisement not found with id: " + id));

        advertisement.setPromotion(null);
        advertisement.setIsPromoted(false);
        advertisement = advertisementRepository.save(advertisement);
        List<AdvertisementPOI> pois = advertisementPOIRepository.findByAdvertisementId(id);
        return AdvertisementResponseDto.fromEntity(advertisement, pois);
    }

    @Transactional
    public AdvertisementResponseDto activatePromotion(Long id) {
        Advertisement advertisement = advertisementRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Advertisement not found with id: " + id));

        if (advertisement.getPromotion() == null) {
             throw new BadRequestException("No promotion selected for advertisement id: " + id);
        }

        advertisement.setIsPromoted(true);
        advertisement = advertisementRepository.save(advertisement);

        List<AdvertisementPOI> pois = advertisementPOIRepository.findByAdvertisementId(id);
        return AdvertisementResponseDto.fromEntity(advertisement, pois);
    }

    /**
     * Checks if the user with the given userId is the owner of the advertisement
     * with the given advertisementId.
     *
     * @param userId The ID of the user.
     * @param advertisementId The ID of the advertisement.
     * @return true if the user is the owner, false otherwise.
     */
    @Transactional(readOnly = true)
    public boolean isOwner(Long userId, Long advertisementId) {
        if (userId == null || advertisementId == null) {
            return false;
        }
        return advertisementRepository.findById(advertisementId)
                .map(Advertisement::getAuthor)
                .map(User::getId)
                .map(ownerId -> ownerId.equals(userId))
                .orElse(false);
    }
}
