package com.itmo.blps.lab1.repositories;

import com.itmo.blps.lab1.entities.POI;
import com.itmo.blps.lab1.entities.POIType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface POIRepository extends JpaRepository<POI, Long> {
    List<POI> findByType(POIType type);
} 