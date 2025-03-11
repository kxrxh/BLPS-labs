package com.itmo.blps.labs.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.itmo.blps.labs.entities.Advertisement;
import com.itmo.blps.labs.entities.Position;

@Repository
public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {
    List<Advertisement> findByAuthorId(Long authorId);

    List<Advertisement> findByPosition(Position position);

    List<Advertisement> findByNameLike(String name);

    List<Advertisement> findByPriceBetween(Double minPrice, Double maxPrice);

    List<Advertisement> findByPriceGreaterThan(Double price);

    List<Advertisement> findByPriceLessThan(Double price);

    List<Advertisement> findByPromotionId(Long promotionId);

    List<Advertisement> findByPosition_City(String city);

    @Modifying
    @Query("UPDATE Advertisement a SET a.promotion = null, a.isPromoted = :isPromoted WHERE a.promotion.id = :promotionId")
    void updatePromotionStatusBatch(boolean isPromoted, Long promotionId);
}
