package com.itmo.blps.labs.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.itmo.blps.labs.entities.Payment;
import com.itmo.blps.labs.entities.PaymentStatus;
import java.util.Optional;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPromotionIdAndStatus(Long promotionId, PaymentStatus status);
    List<Payment> findByPromotionId(Long promotionId);

    @Modifying
    @Query("DELETE FROM Payment p WHERE p.promotion.id = :promotionId")
    void deleteByPromotionId(Long promotionId);
}
