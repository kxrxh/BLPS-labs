package com.itmo.blps.lab1.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
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

}
