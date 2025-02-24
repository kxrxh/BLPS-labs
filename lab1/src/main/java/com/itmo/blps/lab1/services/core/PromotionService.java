package com.itmo.blps.lab1.services.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.itmo.blps.lab1.entities.Promotion;
import com.itmo.blps.lab1.repositories.PromotionRepository;

import java.util.List;
import java.util.Optional;

@Service
public class PromotionService {

    @Autowired
    private PromotionRepository promotionRepository;

    public Promotion createPromotion(Promotion promotion) {
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

    public Promotion updatePromotion(Long id, Promotion promotionDetails) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found with id: " + id));

        promotion.setIsActive(promotionDetails.getIsActive());
        promotion.setPrice(promotionDetails.getPrice());

        return promotionRepository.save(promotion);
    }

    public void deletePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found with id: " + id));
        promotionRepository.delete(promotion);
    }

    /**
     * Applies the promotion by activating it
     * 
     * @param promotion The promotion to be applied
     * @return The updated promotion
     */
    public Promotion applyPromotion(Promotion promotion) {
        promotion.setIsActive(true);
        return promotionRepository.save(promotion);
    }
}
