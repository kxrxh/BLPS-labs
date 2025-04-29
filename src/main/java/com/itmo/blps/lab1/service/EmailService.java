package com.itmo.blps.lab1.service;

import com.itmo.blps.lab1.entities.Payment;
import com.itmo.blps.lab1.entities.Promotion;
import com.itmo.blps.lab1.entities.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String mailFrom;

    public EmailService() {}

    public void sendPaymentReceipt(User user, Payment payment) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Cannot send payment receipt: User {} has no email address.", user.getUsername());
            return;
        }

        // Prepare Thymeleaf context
        Context context = new Context(Locale.getDefault()); // Or specific locale if needed
        context.setVariable("username", user.getUsername());
        context.setVariable("paymentAmount", String.format("%.2f", payment.getAmount())); // Format amount
        context.setVariable("promotionName", payment.getPromotion() != null ? payment.getPromotion().getName() : "N/A");
        context.setVariable("advertisementId", payment.getAdvertisement() != null ? payment.getAdvertisement().getId() : "N/A");
        context.setVariable("paymentId", payment.getId());
        // Add any other variables needed for the template

        // Process the template
        // Template path relative to src/main/resources/templates/
        String htmlContent = templateEngine.process("email/payment-receipt", context);

        String subject = "Payment Receipt - Promotion Activated";

        // Send HTML email
        sendHtmlMessage(user.getEmail(), subject, htmlContent);
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

    public void sendPaymentReminder(User user, Payment payment) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Cannot send payment reminder: User {} has no email address.", user.getUsername());
            return;
        }
        String subject = "Payment Reminder - Action Required";
        String text = String.format(
                "Dear %s,\n\nThis is a reminder that your payment of %.2f for the promotion '%s' (Payment ID: %d) is still pending.\n\nPlease complete your payment soon.\n\nRegards,\nThe Team",
                user.getUsername(),
                payment.getAmount(),
                payment.getPromotion() != null ? payment.getPromotion().getName() : "N/A", // Handle null promotion
                payment.getId());
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

    // New method for sending HTML emails
    private void sendHtmlMessage(String to, String subject, String htmlBody) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name()); // true = multipart

            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = isHtml

            mailSender.send(mimeMessage);
            log.info("HTML Email sent successfully to {} with subject: {}", to, subject);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send HTML email to {} with subject: {}", to, subject, e);
            // Handle exception appropriately
        }
    }

    // Add more methods as needed (e.g., registration confirmation)
}
