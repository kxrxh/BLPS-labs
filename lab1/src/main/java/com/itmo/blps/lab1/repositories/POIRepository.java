package com.itmo.blps.lab1.repositories;

import com.itmo.blps.lab1.entities.POI;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface POIRepository extends JpaRepository<POI, Long> {
    @Query(value = """
            SELECT p.*, 
                   ST_Distance(
                       ST_MakePoint(p.longitude, p.latitude)::geography,
                       ST_MakePoint(:longitude, :latitude)::geography
                   ) as distance
            FROM points_of_interest p
            WHERE ST_DWithin(
                ST_MakePoint(p.longitude, p.latitude)::geography,
                ST_MakePoint(:longitude, :latitude)::geography,
                :radiusInMeters
            )
            AND p.type = :poiType::poi_type
            ORDER BY distance
            """, nativeQuery = true)
    List<POI> findNearbyPOIsByType(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("radiusInMeters") Double radiusInMeters,
            @Param("poiType") String poiType
    );
} 