package com.itmo.blps.labs.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.itmo.blps.labs.entities.PaymentProvider;

@Repository
public interface PaymentProviderRepository extends JpaRepository<PaymentProvider, Long> {
}
