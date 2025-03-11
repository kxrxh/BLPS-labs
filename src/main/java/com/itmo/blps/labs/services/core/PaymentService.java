package com.itmo.blps.labs.services.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itmo.blps.labs.entities.Payment;
import com.itmo.blps.labs.entities.PaymentProvider;
import com.itmo.blps.labs.entities.PaymentStatus;
import com.itmo.blps.labs.entities.Promotion;
import com.itmo.blps.labs.dto.PaymentDto;
import com.itmo.blps.labs.entities.Advertisement;
import com.itmo.blps.labs.repositories.PaymentRepository;
import com.itmo.blps.labs.repositories.PromotionRepository;
import com.itmo.blps.labs.entities.User;
import com.itmo.blps.labs.exception.BadRequestException;

import io.basc.framework.lang.NotFoundException;

import com.itmo.blps.labs.repositories.PaymentProviderRepository;
import com.itmo.blps.labs.repositories.AdvertisementRepository;

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
    @Transactional(readOnly = true)
    public List<PaymentProvider> getAvailableProviders() {
        return providerRepository.findAll();
    }

    @Transactional
    public void setPaymentStatus(Payment payment, PaymentStatus status) {
        payment.setStatus(status);
        paymentRepository.save(payment);
    }

    @Transactional
    public Payment createPayment(PaymentDto paymentDto) {
        Payment payment = new Payment();

        payment.setProvider(providerRepository.findById(paymentDto.getProviderId())
                .orElseThrow(() -> new NotFoundException("Provider not found: " + paymentDto.getProviderId())));

        payment.setAdvertisement(advertisementRepository.findById(paymentDto.getAdvertisementId())
                .orElseThrow(
                        () -> new NotFoundException("Advertisement not found: " + paymentDto.getAdvertisementId())));

        if (payment.getAdvertisement().getPromotion() == null) {
            throw new BadRequestException("Please select a promotion for this advertisement");
        }

        if (payment.getAdvertisement().getIsPromoted()) {
            throw new BadRequestException("This advertisement is already promoted");
        }

        payment.setPromotion(promotionRepository.findById(payment.getAdvertisement().getPromotion().getId())
                .orElseThrow(() -> new NotFoundException(
                        "Promotion not found: " + payment.getAdvertisement().getPromotion().getId())));

        if (paymentDto.getAmount() < payment.getAdvertisement().getPromotion().getPrice()) {
            throw new BadRequestException("Amount is less than the promotion price");
        }

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
                throw new RuntimeException("Transaction failed. Please try again.");
            }
        } catch (Exception e) {
            setPaymentStatus(payment, PaymentStatus.FAILED);
            throw new RuntimeException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    private boolean processPaymentWithProvider(Payment payment) {
        if (Math.random() > 0.5) {
            return true;
        }
        return false;
    }

    @Transactional
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

    @Transactional(readOnly = true)
    public List<Payment> getPayments() {
        return paymentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public boolean isPaymentOwner(Long paymentId, UserDetails principal) {
        Payment payment = getPaymentById(paymentId);
        return payment.getPayer().getUsername().equals(principal.getUsername());
    }
}
