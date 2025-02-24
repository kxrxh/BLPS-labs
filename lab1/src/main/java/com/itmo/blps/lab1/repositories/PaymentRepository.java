package com.itmo.blps.lab1.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.itmo.blps.lab1.entities.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
