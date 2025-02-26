package com.itmo.blps.lab1.services.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.itmo.blps.lab1.entities.Payment;
import com.itmo.blps.lab1.entities.PaymentProvider;
import com.itmo.blps.lab1.entities.PaymentStatus;
import com.itmo.blps.lab1.entities.Promotion;
import com.itmo.blps.lab1.dto.PaymentDto;
import com.itmo.blps.lab1.entities.Advertisement;
import com.itmo.blps.lab1.repositories.PaymentRepository;
import com.itmo.blps.lab1.repositories.PromotionRepository;
import com.itmo.blps.lab1.entities.User;

import io.basc.framework.lang.NotFoundException;

import com.itmo.blps.lab1.repositories.PaymentProviderRepository;
import com.itmo.blps.lab1.repositories.AdvertisementRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentProviderRepository providerRepository;

    @Autowired
    private AdvertisementRepository advertisementRepository;

    @Autowired
    private PromotionRepository promotionRepository;


    // Step 1. Retrieve available payment providers from the database.
    public List<PaymentProvider> getAvailableProviders() {
        return providerRepository.findAll();
    }

    public void setPaymentStatus(Payment payment, PaymentStatus status) {
        payment.setStatus(status);
        paymentRepository.save(payment);
    }

    public Payment createPayment(PaymentDto paymentDto) {
        Payment payment = new Payment();
        payment.setProvider(providerRepository.findById(paymentDto.getProviderId())
                .orElseThrow(() -> new NotFoundException("Provider not found: " + paymentDto.getProviderId())));
        payment.setPromotion(promotionRepository.findById(paymentDto.getPromotionId())
                .orElseThrow(() -> new NotFoundException("Promotion not found: " + paymentDto.getPromotionId())));
        payment.setAmount(paymentDto.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        
        // Set the current authenticated user as the payer
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        payment.setPayer(currentUser);
        
        return paymentRepository.save(payment);
    }

    // Step 2 & 3. Process payment and apply promotion if payment succeeds.
    public String processPayment(Payment payment) {
        // Mark as pending and save.
        setPaymentStatus(payment, PaymentStatus.PENDING);

        // Simulate waiting for a third-party payment service.
        if (Math.random() > 0.5) {
            // Success: update payment status.
            setPaymentStatus(payment, PaymentStatus.SUCCESS);

            // Step 3. Apply promotion changes.
            try {
                // Get the promotion from the payment
                Promotion promotion = payment.getPromotion();

                // Find the advertisement associated with this promotion and update it
                Advertisement advertisement = advertisementRepository.findByPromotionId(promotion.getId())
                        .orElseThrow(() -> new RuntimeException(
                                "No advertisement found for promotion: " + promotion.getId()));

                // Set promotion details
                advertisement.setPromotion(promotion);
                advertisement.setIsPromoted(true);
                advertisement.setStartDate(LocalDateTime.now());
                advertisement.setDurationInDays(30);

                // Save the updated advertisement
                advertisementRepository.save(advertisement);

                return "Successful payment and promotion connection";
            } catch (Exception e) {
                // If promotion application fails, mark payment as failed
                setPaymentStatus(payment, PaymentStatus.FAILED);
                throw e;
            }
        } else {
            // Failure: update status and return error message.
            setPaymentStatus(payment, PaymentStatus.FAILED);
            return "Transaction failed";
        }
    }
}
