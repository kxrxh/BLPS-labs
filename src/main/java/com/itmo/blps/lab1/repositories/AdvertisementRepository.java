package com.itmo.blps.lab1.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itmo.blps.lab1.entities.Advertisement;
import com.itmo.blps.lab1.entities.Position;

@Repository
public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {
    List<Advertisement> findByAuthorId(Long authorId);

    List<Advertisement> findByPosition(Position position);

    List<Advertisement> findByNameLike(String name);

    List<Advertisement> findByPriceBetween(Double minPrice, Double maxPrice);

    List<Advertisement> findByPriceGreaterThan(Double price);

    List<Advertisement> findByPriceLessThan(Double price);

    Optional<Advertisement> findByPromotionId(Long promotionId);

    List<Advertisement> findByPosition_City(String city);

    @Query("SELECT a FROM Advertisement a WHERE a.isPromoted = true AND a.startDate IS NOT NULL AND a.durationInMinutes IS NOT NULL AND FUNCTION('TIMESTAMPADD', MINUTE, a.durationInMinutes, a.startDate) > :now AND FUNCTION('TIMESTAMPADD', MINUTE, a.durationInMinutes, a.startDate) < :targetDate")
    List<Advertisement> findActivePromotionsExpiringBetween(@Param("now") LocalDateTime now, @Param("targetDate") LocalDateTime targetDate);

    /**
     * Finds advertisements that are currently marked as promoted but whose calculated expiration date
     * (based on startDate and durationInMinutes) is at or before the specified time.
     *
     * @param now The current time to check against.
     * @return A list of advertisements whose promotions have expired.
     */
    @Query("SELECT a FROM Advertisement a WHERE a.isPromoted = true AND a.startDate IS NOT NULL AND a.durationInMinutes IS NOT NULL AND FUNCTION('TIMESTAMPADD', MINUTE, a.durationInMinutes, a.startDate) <= :now")
    List<Advertisement> findExpiredActivePromotions(@Param("now") LocalDateTime now);
}
