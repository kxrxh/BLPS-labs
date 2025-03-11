package com.itmo.blps.labs.services.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itmo.blps.labs.dto.PromotionDto;
import com.itmo.blps.labs.entities.Promotion;
import com.itmo.blps.labs.repositories.PromotionRepository;
import com.itmo.blps.labs.repositories.AdvertisementRepository;
import com.itmo.blps.labs.repositories.PaymentRepository;
import com.itmo.blps.labs.entities.Advertisement;
import com.itmo.blps.labs.entities.Payment;

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

    public Promotion createPromotion(PromotionDto promotionDto) {
        Promotion promotion = new Promotion();
        promotion.setName(promotionDto.getName());
        promotion.setDescription(promotionDto.getDescription());
        promotion.setPrice(promotionDto.getPrice());
        return promotionRepository.save(promotion);
    }

    public List<Promotion> getAllPromotions() {
        return promotionRepository.findAll();
    }

    public List<Promotion> getActivePromotions() {
        return promotionRepository.findByIsActiveTrue();
    }

    public Optional<Promotion> getPromotionById(Long id) {
        return promotionRepository.findById(id);
    }

    public Promotion updatePromotion(Long id, PromotionDto promotionDto) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promotion not found with id: " + id));

        promotion.setName(promotionDto.getName());
        promotion.setDescription(promotionDto.getDescription());
        promotion.setPrice(promotionDto.getPrice());

        return promotionRepository.save(promotion);
    }

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

    public void activatePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promotion not found with id: " + id));
        promotion.setIsActive(true);
        promotionRepository.save(promotion);
    }

    public void deactivatePromotion(Long id) {
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

        // Deactivate the promotion
        promotion.setIsActive(false);
        promotionRepository.save(promotion);
    }
}
