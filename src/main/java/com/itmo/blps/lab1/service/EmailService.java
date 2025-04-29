package com.itmo.blps.lab1.service;

import com.itmo.blps.lab1.entities.Payment;
import com.itmo.blps.lab1.entities.Promotion;
import com.itmo.blps.lab1.entities.User;
import com.itmo.blps.lab1.jca.SmtpConnection;
import com.itmo.blps.lab1.jca.SmtpConnectionFactory;
import jakarta.resource.ResourceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;

@Service
@Slf4j
public class EmailService {

    private final SmtpConnectionFactory smtpConnectionFactory;
    private final TemplateEngine templateEngine;

    // Inject the sender email configured for the JCA adapter
    @Value("${jca.mail.sender-email}")
    private String mailFrom;

    @Autowired
    public EmailService(SmtpConnectionFactory smtpConnectionFactory, TemplateEngine templateEngine) {
        this.smtpConnectionFactory = smtpConnectionFactory;
        this.templateEngine = templateEngine;
    }

    public void sendPaymentReceipt(User user, Payment payment) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Cannot send payment receipt: User {} has no email address.", user.getUsername());
            return;
        }

        // Prepare Thymeleaf context
        Context context = new Context(Locale.getDefault());
        context.setVariable("username", user.getUsername());
        context.setVariable("paymentAmount", String.format("%.2f", payment.getAmount()));
        context.setVariable("promotionName", payment.getPromotion() != null ? payment.getPromotion().getName() : "N/A");
        context.setVariable("advertisementId",
                payment.getAdvertisement() != null ? payment.getAdvertisement().getId() : "N/A");
        context.setVariable("paymentId", payment.getId());

        String htmlContent = templateEngine.process("email/payment-receipt", context);
        String subject = "Payment Receipt - Promotion Activated";

        // Send HTML email using the JCA connection
        sendMessage(user.getEmail(), subject, htmlContent, true);
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
        sendMessage(user.getEmail(), subject, text, false); // Send as plain text
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
        sendMessage(user.getEmail(), subject, text, false); // Send as plain text
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
                payment.getPromotion() != null ? payment.getPromotion().getName() : "N/A",
                payment.getId());
        sendMessage(user.getEmail(), subject, text, false); // Send as plain text
    }

    // Consolidated sending method using JCA connection
    private void sendMessage(String to, String subject, String body, boolean isHtml) {
        SmtpConnection connection = null;
        try {
            log.debug("Attempting to get JCA SMTP connection to send email to {}", to);
            connection = smtpConnectionFactory.getConnection(); // Gets a connection using configured credentials
            log.debug("Got JCA SMTP connection: {}", connection);

            // The SmtpConnection determines if content is HTML or plain text
            connection.sendEmail(to, subject, body);

            log.info("{} Email sent successfully via JCA to {} with subject: {}", isHtml ? "HTML" : "Plain Text", to,
                    subject);
        } catch (ResourceException e) {
            log.error("Failed to send {} email via JCA to {} with subject: {}", isHtml ? "HTML" : "Plain Text", to,
                    subject, e);
            // Handle exception appropriately - maybe rethrow a service exception
        } finally {
            if (connection != null) {
                try {
                    log.debug("Closing JCA SMTP connection: {}", connection);
                    connection.close(); // Essential: Release the connection
                } catch (ResourceException e) {
                    log.error("Error closing JCA SMTP connection", e);
                }
            }
        }
    }
}
