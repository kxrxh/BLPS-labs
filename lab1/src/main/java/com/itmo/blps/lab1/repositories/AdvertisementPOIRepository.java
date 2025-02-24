package com.itmo.blps.lab1.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.itmo.blps.lab1.entities.AdvertisementPOI;

public interface AdvertisementPOIRepository extends JpaRepository<AdvertisementPOI, Long> {
    
}
