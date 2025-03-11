package com.itmo.blps.labs.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.itmo.blps.labs.entities.AdvertisementPOI;
import java.util.List;
import java.util.Optional;

public interface AdvertisementPOIRepository extends JpaRepository<AdvertisementPOI, Long> {
    List<AdvertisementPOI> findByAdvertisementId(Long advertisementId);
    Optional<AdvertisementPOI> findByAdvertisementIdAndPoiId(Long advertisementId, Long poiId);
    void deleteByAdvertisementId(Long advertisementId);
}