package com.itmo.blps.lab1.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.itmo.blps.lab1.entities.Promotion;

import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
        List<Promotion> findByIsActiveTrue();

}
