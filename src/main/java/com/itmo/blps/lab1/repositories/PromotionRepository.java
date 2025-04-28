package com.itmo.blps.lab1.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.itmo.blps.lab1.entities.Promotion;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    List<Promotion> findByIsActiveTrue();

    // Added for Task promotion-emails-deactivation_2025-04-28_1
    // Find active promotions where reminder hasn't been sent and expiration is
    // before the threshold
    List<Promotion> findByIsActiveTrueAndReminderSentFalseAndExpirationDateBefore(LocalDateTime reminderThreshold);

    // Find active promotions where expiration date is in the past
    List<Promotion> findByIsActiveTrueAndExpirationDateBefore(LocalDateTime now);
}
