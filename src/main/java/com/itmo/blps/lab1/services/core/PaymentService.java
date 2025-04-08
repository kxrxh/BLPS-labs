package com.itmo.blps.lab1.services.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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
import com.itmo.blps.lab1.exception.BadRequestException;

import io.basc.framework.lang.NotFoundException;

import com.itmo.blps.lab1.repositories.PaymentProviderRepository;
import com.itmo.blps.lab1.repositories.AdvertisementRepository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.client.HttpClientErrorException;

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

    @Autowired
    private AdvertisementService advertisementService;

    // Step 1. Retrieve available payment providers from the database.
    public List<PaymentProvider> getAvailableProviders() {
        return providerRepository.findAll();
    }

    public void setPaymentStatus(Payment payment, PaymentStatus status) {
        payment.setStatus(status);
        paymentRepository.save(payment);
    }

    public Payment createPayment(PaymentDto paymentDto, UserDetails userDetails) {
        Payment payment = new Payment();

        payment.setProvider(providerRepository.findById(paymentDto.getProviderId())
                .orElseThrow(() -> new NotFoundException("Provider not found: " + paymentDto.getProviderId())));

        Advertisement advertisement = advertisementRepository.findById(paymentDto.getAdvertisementId())
                .orElseThrow(
                        () -> new NotFoundException("Advertisement not found: " + paymentDto.getAdvertisementId()));
        payment.setAdvertisement(advertisement);

        // Verify ownership or admin role
        boolean isAdmin = userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN"));
        boolean isOwner = advertisement.getAuthor().getUsername().equals(userDetails.getUsername());
        if (!isOwner && !isAdmin) {
            // Use HttpClientErrorException with 401 status (as previously decided)
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "User does not own the advertisement for this promotion and is not an admin.");
        }

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
        payment.setPayer((User) userDetails);

        return paymentRepository.save(payment);
    }

    // Step 2 & 3. Process payment and apply promotion if payment succeeds.
    @Transactional
    public String processPayment(Payment payment) {
        try {
            // Mark as pending
            setPaymentStatus(payment, PaymentStatus.PENDING);

            // Simulate payment provider interaction
            boolean paymentSuccessful = processPaymentWithProvider(payment);

            if (paymentSuccessful) {
                // Call AdvertisementService to activate the promotion
                advertisementService.activatePromotion(payment.getAdvertisement().getId());
                // Only set status to SUCCESS after promotion activation succeeds
                setPaymentStatus(payment, PaymentStatus.SUCCESS);
                return "Successful payment and promotion activation.";
            } else {
                setPaymentStatus(payment, PaymentStatus.FAILED);
                throw new RuntimeException("Payment provider declined the transaction.");
            }
        } catch (NotFoundException e) {
            setPaymentStatus(payment, PaymentStatus.FAILED);
            throw new RuntimeException("Failed to activate promotion: Advertisement not found.", e);
        } catch (BadRequestException e) {
            setPaymentStatus(payment, PaymentStatus.FAILED);
            throw new RuntimeException("Failed to activate promotion: Bad request.", e);
        } catch (Exception e) {
            setPaymentStatus(payment, PaymentStatus.FAILED);
            throw new RuntimeException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    private boolean processPaymentWithProvider(Payment payment) {
        System.out.println("Simulating payment processing for amount: " + payment.getAmount() + " via provider: " + payment.getProvider().getName());
        try {
            Thread.sleep(500);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        }
        return Math.random() > 0.2;
    }
}
