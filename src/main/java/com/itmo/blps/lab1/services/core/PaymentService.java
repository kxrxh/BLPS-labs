package com.itmo.blps.lab1.services.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.itmo.blps.lab1.entities.Payment;
import com.itmo.blps.lab1.entities.PaymentProvider;
import com.itmo.blps.lab1.entities.PaymentStatus;
import com.itmo.blps.lab1.dto.PaymentDto;
import com.itmo.blps.lab1.entities.Advertisement;
import com.itmo.blps.lab1.repositories.PaymentRepository;
import com.itmo.blps.lab1.repositories.PromotionRepository;
import com.itmo.blps.lab1.entities.User;
import com.itmo.blps.lab1.exception.BadRequestException;

import io.basc.framework.lang.NotFoundException;

import com.itmo.blps.lab1.repositories.PaymentProviderRepository;
import com.itmo.blps.lab1.repositories.AdvertisementRepository;

import java.util.List;
import java.time.LocalDateTime;

import org.springframework.web.client.HttpClientErrorException;

import com.itmo.blps.lab1.entities.Promotion;
import com.itmo.blps.lab1.service.EmailService;
import com.itmo.blps.lab1.service.NotificationService;
import com.itmo.blps.lab1.messaging.StompNotificationProducer;

import java.util.Random;

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
    private EmailService emailService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private StompNotificationProducer stompProducer;

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

    public String createAndProcessPayment(PaymentDto paymentDto, UserDetails userDetails) {
        // Create a new transaction definition with custom settings
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("PaymentCreationAndProcessingTransaction");
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        def.setTimeout(30); // 30 seconds timeout

        return transactionTemplate.execute(status -> {
            try {
                Payment payment = createPayment(paymentDto, userDetails);
                return processPayment(payment);
            } catch (Exception e) {
                // Mark transaction for rollback
                status.setRollbackOnly();
                throw new RuntimeException("Payment creation and processing failed: " + e.getMessage(), e);
            }
        });
    }

    public Payment createPayment(PaymentDto paymentDto, UserDetails userDetails) {
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
        boolean isAdmin = userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN"));
        boolean isOwner = advertisement.getAuthor().getUsername().equals(userDetails.getUsername());
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
        payment.setPayer((User) userDetails);

        return paymentRepository.save(payment);
    }

    /**
     * Process a payment and handle activation of promotions
     *
     * @param payment The payment to process
     * @return Status message
     */
    private String processPayment(Payment payment) {
        // 1. Log the payment processing start
        Long paymentId = payment.getId();
        Long advertisementId = payment.getAdvertisement() != null ? payment.getAdvertisement().getId() : null;
        Long promotionId = payment.getPromotion() != null ? payment.getPromotion().getId() : null;

        // 2. Simulate payment processing with the selected provider
        PaymentProvider provider = payment.getProvider();
        boolean isSuccessful = simulatePaymentProviderProcessing(provider);

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

                promotion.setDurationInMinutes(promotion.getDurationInMinutes());
                promotionRepository.save(promotion);

                // Set the ad as promoted and activate the promotion
                ad.setIsPromoted(true);
                // TODO: Set ad.setPromotion(promotion) if needed
                advertisementRepository.save(ad);

                // Send a receipt notification via STOMP
                User user = payment.getPayer();
                if (user != null) {
                    String notificationPayload = notificationService.createPaymentReceiptNotification(user, payment);
                    stompProducer.sendNotification(notificationPayload);
                }

                return "Payment successfully processed. Promotion activated for advertisement.";
            } else {
                return "Payment successfully processed. But could not activate promotion due to missing ad or promotion info.";
            }
        } else {
            // 3b. If failed, set payment status to failed
            setPaymentStatus(payment, PaymentStatus.FAILED);
            return "Payment processing failed. Please try again or contact support.";
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
}
