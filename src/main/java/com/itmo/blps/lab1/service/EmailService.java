package com.itmo.blps.lab1.service;

import com.itmo.blps.lab1.entities.Payment;
import com.itmo.blps.lab1.entities.Promotion;
import com.itmo.blps.lab1.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailFrom; // Use configured username as sender

    public void sendPaymentReceipt(User user, Payment payment) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Cannot send payment receipt: User {} has no email address.", user.getUsername());
            return;
        }
        String subject = "Payment Receipt - Promotion Activated";
        String text = String.format(
                "Dear %s,\n\nThank you for your payment of %.2f for the promotion '%s' on advertisement ID %d.\n\nYour promotion is now active!\n\nRegards,\nThe Team",
                user.getUsername(),
                payment.getAmount(),
                payment.getPromotion().getName(),
                payment.getAdvertisement().getId());
        sendSimpleMessage(user.getEmail(), subject, text);
    }

    public void sendPromotionReminder(User user, Promotion promotion) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Cannot send promotion reminder: User {} has no email address.", user.getUsername());
            return;
        }
        String subject = "Promotion Reminder - Nearing Expiration";
        String text = String.format(
                "Dear %s,\n\nThis is a reminder that your promotion '%s' is set to expire soon (on %s).\n\nRegards,\nThe Team",
                user.getUsername(),
                promotion.getName(),
                promotion.getExpirationDate() != null ? promotion.getExpirationDate().toLocalDate().toString() : "N/A");
        sendSimpleMessage(user.getEmail(), subject, text);
    }

    public void sendPromotionDeactivationNotice(User user, Promotion promotion) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Cannot send promotion deactivation notice: User {} has no email address.", user.getUsername());
            return;
        }
        String subject = "Promotion Deactivated";
        String text = String.format(
                "Dear %s,\n\nYour promotion '%s' has now expired and has been deactivated.\n\nRegards,\nThe Team",
                user.getUsername(),
                promotion.getName());
        sendSimpleMessage(user.getEmail(), subject, text);
    }

    private void sendSimpleMessage(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Email sent successfully to {} with subject: {}", to, subject);
        } catch (MailException e) {
            log.error("Failed to send email to {} with subject: {}", to, subject, e);
            // Handle exception appropriately - maybe queue for retry?
        }
    }

    // Add more methods as needed (e.g., registration confirmation)
}
