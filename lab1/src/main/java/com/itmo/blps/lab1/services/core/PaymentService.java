package com.itmo.blps.lab1.services.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        payment.setAdvertisement(advertisementRepository.findById(paymentDto.getAdvertisementId())
                .orElseThrow(
                        () -> new NotFoundException("Advertisement not found: " + paymentDto.getAdvertisementId())));

        if (payment.getAdvertisement().getPromotion() == null) {
            throw new RuntimeException("Please select a promotion for this advertisement");
        }

        if (payment.getAdvertisement().getIsPromoted()) {
            throw new RuntimeException("This advertisement is already promoted");
        }

        payment.setPromotion(promotionRepository.findById(payment.getAdvertisement().getPromotion().getId())
                .orElseThrow(() -> new NotFoundException(
                        "Promotion not found: " + payment.getAdvertisement().getPromotion().getId())));

        payment.setAmount(paymentDto.getAmount());
        payment.setStatus(PaymentStatus.PENDING);

        // Set the current authenticated user as the payer
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        payment.setPayer(currentUser);

        return paymentRepository.save(payment);
    }

    // Step 2 & 3. Process payment and apply promotion if payment succeeds.
    @Transactional
    public String processPayment(Payment payment) {
        try {
            // Mark as pending and save
            setPaymentStatus(payment, PaymentStatus.PENDING);

            // Here you would integrate with a real payment provider using
            // payment.getProvider()
            // For now, we'll simulate a successful payment
            boolean paymentSuccessful = processPaymentWithProvider(payment);

            if (paymentSuccessful) {
                setPaymentStatus(payment, PaymentStatus.SUCCESS);
                return applyPromotion(payment);
            } else {
                setPaymentStatus(payment, PaymentStatus.FAILED);
                return "Transaction failed. Please try again.";
            }
        } catch (Exception e) {
            setPaymentStatus(payment, PaymentStatus.FAILED);
            throw new RuntimeException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    private boolean processPaymentWithProvider(Payment payment) {
        if (Math.random() > 0.5) {
            return true;
        }
        return false;
    }

    private String applyPromotion(Payment payment) {
        try {
            Promotion promotion = payment.getPromotion();
            Advertisement advertisement = payment.getAdvertisement();

            if (advertisement == null || promotion == null) {
                throw new RuntimeException("Advertisement or promotion not found");
            }

            advertisement.setPromotion(promotion);
            advertisement.setIsPromoted(true);
            advertisement.setStartDate(LocalDateTime.now());
            advertisement.setDurationInDays(30);

            advertisementRepository.save(advertisement);
            return "Successful payment and promotion connection";
        } catch (Exception e) {
            throw new RuntimeException("Failed to apply promotion: " + e.getMessage(), e);
        }
    }
}
