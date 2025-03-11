package com.itmo.blps.labs.services.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.itmo.blps.labs.dto.PromotionDto;
import com.itmo.blps.labs.entities.Promotion;
import com.itmo.blps.labs.repositories.PromotionRepository;
import com.itmo.blps.labs.repositories.AdvertisementRepository;
import com.itmo.blps.labs.repositories.PaymentRepository;
import com.itmo.blps.labs.entities.Advertisement;

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

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Promotion createPromotion(PromotionDto promotionDto) {
        Promotion promotion = new Promotion();
        promotion.setName(promotionDto.getName());
        promotion.setDescription(promotionDto.getDescription());
        promotion.setPrice(promotionDto.getPrice());
        promotion.setIsActive(true); // Set default state
        return promotionRepository.save(promotion);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<Promotion> getAllPromotions() {
        return promotionRepository.findAll();
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public List<Promotion> getActivePromotions() {
        return promotionRepository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Optional<Promotion> getPromotionById(Long id) {
        return promotionRepository.findById(id);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Promotion updatePromotion(Long id, PromotionDto promotionDto) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promotion not found with id: " + id));

        promotion.setName(promotionDto.getName());
        promotion.setDescription(promotionDto.getDescription());
        promotion.setPrice(promotionDto.getPrice());

        return promotionRepository.save(promotion);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deletePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promotion not found with id: " + id));

        // Найти все связанные объявления одним запросом
        List<Advertisement> advertisements = advertisementRepository.findByPromotionId(id);

        // Обновить все объявления одной операцией
        if (!advertisements.isEmpty()) {
            advertisementRepository.updatePromotionStatusBatch(false, id);
        }

        // Удалить все платежи, связанные с этой акцией
        paymentRepository.deleteByPromotionId(id);

        // Удалить саму акцию
        promotionRepository.delete(promotion);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void activatePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promotion not found with id: " + id));
        promotion.setIsActive(true);
        promotionRepository.save(promotion);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void deactivatePromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promotion not found with id: " + id));

        // Обновить все связанные объявления одной операцией
        advertisementRepository.updatePromotionStatusBatch(false, id);

        // Деактивировать акцию
        promotion.setIsActive(false);
        promotionRepository.save(promotion);
    }
}
