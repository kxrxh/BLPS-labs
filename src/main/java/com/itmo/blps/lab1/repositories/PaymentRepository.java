package com.itmo.blps.lab1.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.itmo.blps.lab1.entities.Payment;
import com.itmo.blps.lab1.entities.PaymentStatus;
import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPromotionIdAndStatus(Long promotionId, PaymentStatus status);

    List<Payment> findByPromotionId(Long promotionId);

    // Added for Task promotion-emails-deactivation_2025-04-28_1
    // Find the latest successful payment for a specific promotion
    Optional<Payment> findFirstByPromotionIdAndStatusOrderByCreatedAtDesc(Long promotionId, PaymentStatus status);

    List<Payment> findByStatusAndReminderSentFalseAndCreatedAtBefore(PaymentStatus status, LocalDateTime threshold);
}
