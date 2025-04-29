package com.itmo.blps.lab1.services.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itmo.blps.lab1.dto.PromotionDto;
import com.itmo.blps.lab1.entities.Promotion;
import com.itmo.blps.lab1.repositories.PromotionRepository;
import com.itmo.blps.lab1.repositories.AdvertisementRepository;
import com.itmo.blps.lab1.repositories.PaymentRepository;
import com.itmo.blps.lab1.entities.Advertisement;
import com.itmo.blps.lab1.entities.Payment;
import com.itmo.blps.lab1.service.EmailService;
import io.basc.framework.lang.NotFoundException;

import java.util.List;
import java.util.Optional;

@Service
public class PromotionService {

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private AdvertisementRepository advertisementRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private EmailService emailService;

    @Transactional
    public Promotion createPromotion(PromotionDto promotionDto) {
        Promotion promotion = new Promotion();
        promotion.setName(promotionDto.getName());
        promotion.setDescription(promotionDto.getDescription());
        promotion.setPrice(promotionDto.getPrice());
        return promotionRepository.save(promotion);
    }

    @Transactional(readOnly = true)
    public List<Promotion> getAllPromotions() {
        return promotionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Promotion> getActivePromotions() {
        return promotionRepository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public Optional<Promotion> getPromotionById(Long id) {
        return promotionRepository.findById(id);
    }

    @Transactional
    public Promotion updatePromotion(Long id, PromotionDto promotionDto) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promotion not found with id: " + id));

        promotion.setName(promotionDto.getName());
        promotion.setDescription(promotionDto.getDescription());
        promotion.setPrice(promotionDto.getPrice());

        return promotionRepository.save(promotion);
    }

    @Transactional
    public void deletePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promotion not found with id: " + id));

        // Find and update all advertisements with this promotion
        List<Advertisement> advertisements = advertisementRepository.findAll().stream()
                .filter(ad -> ad.getPromotion() != null && ad.getPromotion().getId().equals(id))
                .toList();

        // Remove promotion from all associated advertisements
        for (Advertisement ad : advertisements) {
            ad.setPromotion(null);
            ad.setIsPromoted(false);
            advertisementRepository.save(ad);
        }

        // Delete all payment records associated with this promotion
        List<Payment> payments = paymentRepository.findByPromotionId(id);
        paymentRepository.deleteAll(payments);

        // Now we can safely delete the promotion
        promotionRepository.delete(promotion);
    }

    @Transactional
    public void activatePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promotion not found with id: " + id));
        promotion.setIsActive(true);
        promotionRepository.save(promotion);
    }

    @Transactional
    public void deactivatePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promotion not found with id: " + id));

        // Find the advertisement currently using this promotion (if any)
        Optional<Advertisement> adOpt = advertisementRepository.findByPromotionId(id);

        // Remove promotion from the associated advertisement
        if (adOpt.isPresent()) {
            Advertisement ad = adOpt.get();
            if (ad.getIsPromoted()) {
                emailService.sendPromotionExpired(ad);
            }
            ad.setPromotion(null);
            ad.setIsPromoted(false);
            ad.setStartDate(null);
            ad.setDurationInMinutes(null);
            advertisementRepository.save(ad);
        }

        // Deactivate the promotion
        promotion.setIsActive(false);
        promotionRepository.save(promotion);
    }
}
