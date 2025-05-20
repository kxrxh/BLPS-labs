package com.itmo.blps.lab1.services.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;

import com.itmo.blps.lab1.entities.Payment;
import com.itmo.blps.lab1.entities.PaymentProvider;
import com.itmo.blps.lab1.entities.PaymentStatus;
import com.itmo.blps.lab1.dto.PaymentDto;
import com.itmo.blps.lab1.entities.Advertisement;
import com.itmo.blps.lab1.repositories.PaymentRepository;
import com.itmo.blps.lab1.repositories.PromotionRepository;
import com.itmo.blps.lab1.repositories.UserRepository;
import com.itmo.blps.lab1.entities.User;
import com.itmo.blps.lab1.exception.BadRequestException;

import io.basc.framework.lang.NotFoundException;
import lombok.extern.slf4j.Slf4j;

import com.itmo.blps.lab1.repositories.PaymentProviderRepository;
import com.itmo.blps.lab1.repositories.AdvertisementRepository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.web.client.HttpClientErrorException;

import com.itmo.blps.lab1.entities.Promotion;
import com.itmo.blps.lab1.service.NotificationService;
import com.itmo.blps.lab1.messaging.StompNotificationProducer;

import java.util.Random;

@Slf4j
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
    private TransactionTemplate transactionTemplate;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private StompNotificationProducer stompProducer;

    @Autowired
    private UserRepository userRepository;

    // Use @Lazy to prevent circular dependency issues on startup
    @Lazy
    @Autowired
    private PaymentService self;

    // Step 1. Retrieve available payment providers from the database.
    public List<PaymentProvider> getAvailableProviders() {
        return providerRepository.findAll();
    }

    public void setPaymentStatus(Payment payment, PaymentStatus status) {
        payment.setStatus(status);
        paymentRepository.save(payment);
    }

    public Payment createAndProcessPayment(PaymentDto paymentDto, Long userId) {
        return createAndProcessPayment(paymentDto, userId, true);
    }

    public Payment createAndProcessPayment(PaymentDto paymentDto, Long userId, Boolean sendNotification) {
        // Create a new transaction definition with custom settings
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("PaymentCreationAndProcessingTransaction");
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        def.setTimeout(30); // 30 seconds timeout

        // Process payment and get potential notification payload within transaction
        return transactionTemplate.execute(status -> { // Return the result directly
            String notificationPayload = null;
            try {
                Payment payment = createPayment(paymentDto, userId);
                notificationPayload = processPayment(payment);
                log.info("Notification payload: {}", notificationPayload);
                if (notificationPayload != null) {
                    if (sendNotification) {
                        String finalPayload = notificationPayload; // Need effectively final variable for lambda
                        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                stompProducer.sendNotification(finalPayload);
                            }
                        });
                        log.info("Payment successfully processed. Promotion activated. Receipt notification queued.");
                    } else {
                        log.info("Payment successfully processed. Promotion activated. Notification skipped as per request.");
                    }
                    return payment;
                } else {
                    throw new RuntimeException("Something went wrong during payment processing.");
                }
            } catch (Exception e) {
                // Mark transaction for rollback
                status.setRollbackOnly();
                log.error("Payment creation and processing failed: {}", e.getMessage(), e);
                return null; // Return error message
            }
        });
    }

    public Payment createPayment(PaymentDto paymentDto, Long userId) {
        Payment payment = new Payment();

        payment.setProvider(providerRepository.findById(paymentDto.getProviderId())
                .orElseThrow(() -> new NotFoundException("Provider not found: " + paymentDto.getProviderId())));

        Advertisement advertisement = advertisementRepository.findById(paymentDto.getAdvertisementId())
                .orElseThrow(
                        () -> new NotFoundException("Advertisement not found: " + paymentDto.getAdvertisementId()));
        payment.setAdvertisement(advertisement);

        // Ensure the advertisement has an author before proceeding
        if (advertisement.getAuthor() == null) {
            throw new IllegalStateException(
                    "Advertisement with ID " + advertisement.getId() + " has no author associated.");
        }

        // Verify ownership or admin role
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        boolean isAdmin = user.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN"));
        boolean isOwner = advertisement.getAuthor().getUsername().equals(user.getUsername());
        if (!isOwner && !isAdmin) {
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED,
                    "User does not own the advertisement for this promotion and is not an admin.");
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
        payment.setPayer(user);

        return paymentRepository.save(payment);
    }

    /**
     * Process a payment and handle activation of promotions
     *
     * @param payment The payment to process
     * @return Notification payload if successful, otherwise null
     */
    private String processPayment(Payment payment) {
        // 1. Log the payment processing start
        Long advertisementId = payment.getAdvertisement() != null ? payment.getAdvertisement().getId() : null;
        Long promotionId = payment.getPromotion() != null ? payment.getPromotion().getId() : null;

        // 2. Simulate payment processing with the selected provider
        PaymentProvider provider = payment.getProvider();
        boolean isSuccessful = simulatePaymentProviderProcessing(provider);

        if (payment.getAmount() == 999) {
            throw new RuntimeException("Simulated payment failed for some reason!");
        }

        if (isSuccessful) {
            // 3a. If successful, activate the promotion for the advertisement
            PaymentStatus status = PaymentStatus.SUCCESS;
            setPaymentStatus(payment, status);

            if (advertisementId != null && promotionId != null) {
                // Activate the promotion
                Advertisement ad = payment.getAdvertisement();
                if (ad == null) {
                    throw new NotFoundException("Advertisement not found with id: " + advertisementId);
                }
                Promotion promotion = payment.getPromotion();
                if (promotion == null) {
                    throw new NotFoundException("Promotion not found with id: " + promotionId);
                }

                // Set the ad as promoted and activate the promotion
                ad.setIsPromoted(true);
                ad.setStartDate(LocalDateTime.now());
                ad.setDurationInMinutes(promotion.getDurationInMinutes());
                advertisementRepository.save(ad);

                User user = payment.getPayer();
                if (user == null) {
                    log.info("User not found for payment: {}", payment);
                    return null;
                }
                String notificationPayload = notificationService.createPaymentReceiptNotification(user, payment);

                return notificationPayload; // Return payload for sending after commit
            } else {
                return null; // No notification to send
            }
        } else {
            // 3b. If failed, set payment status to failed
            setPaymentStatus(payment, PaymentStatus.FAILED);
            return null; // No notification to send on failure
        }
    }

    /**
     * Simple simulation of payment provider processing
     *
     * @param provider The payment provider to use
     * @return Whether the payment was successful
     */
    private boolean simulatePaymentProviderProcessing(PaymentProvider provider) {
        try {
            // Simulate processing time
            Thread.sleep(1000);

            // Simulate a success rate (e.g., 80% success rate)
            Random random = new Random();
            double randomValue = random.nextDouble();
            double successRate = 0.8; // 80% success rate

            // In real implementation, this would call the actual payment provider API
            return randomValue <= successRate;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Payment processing was interrupted");
        }
    }

    public void updatePaymentStatus(Long paymentId, PaymentStatus status) {
        transactionTemplate.execute(transactionStatus -> {
            try {
                Payment payment = paymentRepository.findById(paymentId)
                        .orElseThrow(
                                () -> new NotFoundException("Payment not found during status update: " + paymentId));
                payment.setStatus(status);
                paymentRepository.save(payment);
                return null;
            } catch (Exception e) {
                // Mark transaction for rollback
                transactionStatus.setRollbackOnly();
                throw new RuntimeException("Payment status update failed: " + e.getMessage(), e);
            }
        });
    }

    public Optional<Payment> getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId);
    }
}
