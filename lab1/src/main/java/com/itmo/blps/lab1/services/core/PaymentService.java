package com.itmo.blps.lab1.services.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itmo.blps.lab1.entities.Payment;
import com.itmo.blps.lab1.entities.PaymentProvider;
import com.itmo.blps.lab1.entities.PaymentStatus;
import com.itmo.blps.lab1.repositories.PaymentRepository;
import com.itmo.blps.lab1.repositories.PaymentProviderRepository;

import java.util.List;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentProviderRepository providerRepository;

    // Optionally inject PromotionService to apply promotion changes.
    @Autowired
    private PromotionService promotionService;

    // Step 1. Retrieve available payment providers from the database.
    public List<PaymentProvider> getAvailableProviders() {
        return providerRepository.findAll();
    }

    // Step 2 & 3. Process payment and apply promotion if payment succeeds.
    public String processPayment(Payment payment) {
        // Mark as pending and save.
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        // Simulate waiting for a third-party payment service.
        if (Math.random() > 0.5) {
            // Success: update payment status.
            payment.setStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);

            // Step 3. Apply promotion changes.
            try {
                promotionService.applyPromotion(payment.getPromotion());
                return "Successful payment and promotion connection";
            } catch (Exception e) {
                // If promotion application fails, mark payment as failed
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                return "Transaction successful but failed to apply promotion";
            }
        } else {
            // Failure: update status and return error message.
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            return "Transaction failed";
        }
    }
}
