package com.itmo.blps.lab1.repositories;

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

    @Query(value = """
            SELECT * FROM advertisement a 
            WHERE ST_DWithin(
                ST_MakePoint(a.longitude, a.latitude)::geography,
                ST_MakePoint(:longitude, :latitude)::geography,
                :radiusInMeters
            )
            ORDER BY ST_Distance(
                ST_MakePoint(a.longitude, a.latitude)::geography,
                ST_MakePoint(:longitude, :latitude)::geography
            )
            """, nativeQuery = true)
    List<Advertisement> findNearbyAdvertisements(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("radiusInMeters") Double radiusInMeters
    );

    // Find by address components
    List<Advertisement> findByPosition_City(String city);
}
