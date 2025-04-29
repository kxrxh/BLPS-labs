package com.itmo.blps.lab1.services.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import com.itmo.blps.lab1.security.UserAuthentication;

@Service
public class AdvertisementService {
    private final AdvertisementRepository advertisementRepository;
    private final PromotionRepository promotionRepository;
    private final POIService poiService;
    private final GeoService geoService;
    private final AdvertisementPOIRepository advertisementPOIRepository;
    private final UserService userService;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public AdvertisementService(AdvertisementRepository advertisementRepository,
            PromotionRepository promotionRepository,
            POIService poiService,
            GeoService geoService,
            AdvertisementPOIRepository advertisementPOIRepository,
            UserService userService,
            PlatformTransactionManager transactionManager) {
        this.advertisementRepository = advertisementRepository;
        this.promotionRepository = promotionRepository;
        this.poiService = poiService;
        this.geoService = geoService;
        this.advertisementPOIRepository = advertisementPOIRepository;
        this.userService = userService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public AdvertisementResponseDto createAdvertisement(AdDto adDto) {
        return transactionTemplate.execute(status -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!(authentication instanceof UserAuthentication)) {
                throw new IllegalStateException("User not properly authenticated.");
            }
            Long currentUserId = ((UserAuthentication) authentication).getUserId();
            User currentUser = userService.getUserById(currentUserId);

            Advertisement advertisement = Advertisement.builder()
                    .name(adDto.getTitle())
                    .description(adDto.getDescription())
                    .price(adDto.getPrice())
                    .realEstateType(adDto.getRealEstateType())
                    .position(geoService.getPositionFromAddress(adDto.getAddress(), adDto.getCity()))
                    .author(currentUser)
                    .build();

            advertisement = advertisementRepository.save(advertisement);
            // Make simulation of same fail
            if (advertisement.getName().contains("Simulation")) {
                throw new RuntimeException("Simulation some fail");
            }
            advertisement = poiService.updateAdvertisementPOIs(advertisement.getId());
            List<AdvertisementPOI> pois = advertisementPOIRepository.findByAdvertisementId(advertisement.getId());

            return AdvertisementResponseDto.fromEntity(advertisement, pois);
        });
    }

    public Optional<AdvertisementResponseDto> getAdvertisementById(Long id) {
        return transactionTemplate.execute(status -> {
            status.setRollbackOnly(); // Equivalent to readOnly = true
            Optional<Advertisement> advertisement = advertisementRepository.findById(id);
            if (advertisement.isPresent()) {
                poiService.updateAdvertisementPOIs(advertisement.get().getId());
                List<AdvertisementPOI> pois = advertisementPOIRepository.findByAdvertisementId(id);
                return Optional.of(AdvertisementResponseDto.fromEntity(advertisement.get(), pois));
            }
            return Optional.empty();
        });
    }

    public List<AdvertisementResponseDto> getAllAdvertisements() {
        return transactionTemplate.execute(status -> {
            status.setRollbackOnly(); // Equivalent to readOnly = true
            List<Advertisement> advertisements = advertisementRepository.findAll();
            return advertisements.stream()
                    .map(ad -> {
                        poiService.updateAdvertisementPOIs(ad.getId());
                        List<AdvertisementPOI> pois = advertisementPOIRepository.findByAdvertisementId(ad.getId());
                        return AdvertisementResponseDto.fromEntity(ad, pois);
                    })
                    .collect(Collectors.toList());
        });
    }

    public AdvertisementResponseDto addPromotion(Long id, Long promotionId) {
        return transactionTemplate.execute(status -> {
            Advertisement advertisement = advertisementRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Advertisement not found with id: " + id));

            if (advertisement.getIsPromoted()) {
                throw new BadRequestException(
                        "Advertisement is already promoted. First remove the promotion and then add a new one.");
            }

            Promotion promotion = promotionRepository.findById(promotionId)
                    .orElseThrow(() -> new NotFoundException("Promotion not found with id: " + promotionId));

            advertisement.setPromotion(promotion);
            advertisement.setIsPromoted(false);
            advertisement.setStartDate(null);
            advertisement.setDurationInMinutes(null);
            advertisement = advertisementRepository.save(advertisement);
            List<AdvertisementPOI> pois = advertisementPOIRepository.findByAdvertisementId(id);
            return AdvertisementResponseDto.fromEntity(advertisement, pois);
        });
    }

    public AdvertisementResponseDto removePromotion(Long id) {
        return transactionTemplate.execute(status -> {
            Advertisement advertisement = advertisementRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Advertisement not found with id: " + id));

            advertisement.setPromotion(null);
            advertisement.setIsPromoted(false);
            advertisement.setStartDate(null);
            advertisement.setDurationInMinutes(null);
            advertisement = advertisementRepository.save(advertisement);
            List<AdvertisementPOI> pois = advertisementPOIRepository.findByAdvertisementId(id);
            return AdvertisementResponseDto.fromEntity(advertisement, pois);
        });
    }

    /**
     * Checks if the user with the given userId is the owner of the advertisement
     * with the given advertisementId.
     *
     * @param userId          The ID of the user.
     * @param advertisementId The ID of the advertisement.
     * @return true if the user is the owner, false otherwise.
     */
    public boolean isOwner(Long userId, Long advertisementId) {
        Boolean result = transactionTemplate.execute(status -> {
            status.setRollbackOnly(); // Equivalent to readOnly = true
            if (userId == null || advertisementId == null) {
                return Boolean.FALSE; // Return Boolean object
            }
            return advertisementRepository.findById(advertisementId)
                    .map(Advertisement::getAuthor)
                    .map(User::getId)
                    .map(ownerId -> Boolean.valueOf(ownerId.equals(userId))) // Ensure Boolean object
                    .orElse(Boolean.FALSE); // Return Boolean object
        });
        return result != null && result;
    }
}
