package com.itmo.blps.labs.repositories;

import com.itmo.blps.labs.entities.POI;
import com.itmo.blps.labs.entities.POIType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface POIRepository extends JpaRepository<POI, Long> {
    List<POI> findByType(POIType type);
} 