package com.itmo.blps.lab1.service;

import com.itmo.blps.lab1.entities.Advertisement;
import com.itmo.blps.lab1.entities.Payment;
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
import java.time.LocalDateTime;
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

    public EmailService() {
    }

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
        context.setVariable("advertisementId",
                payment.getAdvertisement() != null ? payment.getAdvertisement().getId() : "N/A");
        context.setVariable("paymentId", payment.getId());

        String htmlContent = templateEngine.process("email/payment-receipt", context);

        String subject = "Payment Receipt - Promotion Activated";

        // Send HTML email
        sendHtmlMessage(user.getEmail(), subject, htmlContent);
    }

    public void sendPromotionEndingNotice(Advertisement advertisement) {
        if (advertisement.getAuthor().getEmail() == null || advertisement.getAuthor().getEmail().isBlank()) {
            log.warn("Cannot send promotion ending notice: User {} has no email address.",
                    advertisement.getAuthor().getUsername());
            return;
        }
        String subject = "Promotion Ending Soon - " + advertisement.getName();
        String text = String.format(
                "Dear %s,\n\nYour promoted advertisement '%s' will expire on %s.\n\n" +
                        "If you wish to continue promoting this advertisement, please visit your dashboard to renew the promotion.\n\n"
                        +
                        "Regards,\nThe Team",
                advertisement.getAuthor().getUsername(), advertisement.getName(), advertisement.getEndDate());
        sendSimpleMessage(advertisement.getAuthor().getEmail(), subject, text);
        log.info("Promotion ending notice sent to {} for advertisement '{}'", advertisement.getAuthor().getEmail(),
                advertisement.getName());
    }

    /**
     * Sends a notification to the user that their advertisement promotion is about
     * to expire
     */
    public void sendPromotionExpired(Advertisement advertisement) {
        if (advertisement.getAuthor().getEmail() == null || advertisement.getAuthor().getEmail().isBlank()) {
            log.warn("Cannot send promotion expired notice: User {} has no email address.",
                    advertisement.getAuthor().getUsername());
            return;
        }
        String subject = "Promotion Expired - " + advertisement.getName();
        String text = String.format(
                "Dear %s,\n\nYour promoted advertisement '%s' has expired.\n\n" +
                        "If you wish to continue promoting this advertisement, please visit your dashboard to renew the promotion.\n\n"
                        +
                        "Regards,\nThe Team",
                advertisement.getAuthor().getUsername(), advertisement.getName());
        sendSimpleMessage(advertisement.getAuthor().getEmail(), subject, text);
        log.info("Promotion expired notice sent to {} for advertisement '{}'", advertisement.getAuthor().getEmail(),
                advertisement.getName());
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
        }
    }

    // New method for sending HTML emails
    private void sendHtmlMessage(String to, String subject, String htmlBody) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name()); // true
                                                                                                                // =
                                                                                                                // multipart

            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(mimeMessage);
            log.info("HTML Email sent successfully to {} with subject: {}", to, subject);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send HTML email to {} with subject: {}", to, subject, e);
        }
    }

}
