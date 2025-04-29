package com.itmo.blps.lab1.jca;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;

import java.io.File;
import java.io.IOException;

public class SmtpConnection {

    private final SmtpManagedConnection managedConnection;
    private final Session mailSession;
    private final String from;
    private final String username;
    private boolean closed = false;

    public SmtpConnection(SmtpManagedConnection managedConnection, Session mailSession, String from, String username) {
        this.managedConnection = managedConnection;
        this.mailSession = mailSession;
        this.from = from;
        this.username = username;
    }

    public void sendEmail(String to, String subject, String body) throws ResourceException {
        checkIfClosed();
        try {
            MimeMessage message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(from));
            message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);

            if (body.trim().startsWith("<!DOCTYPE html>") || body.trim().startsWith("<html>")) {
                message.setContent(body, "text/html; charset=UTF-8");
            } else {
                message.setText(body);
            }

            String host = mailSession.getProperty("mail.smtp.host");
            int port = Integer.parseInt(mailSession.getProperty("mail.smtp.port"));
            try (Transport transport = mailSession.getTransport(mailSession.getProperty("mail.transport.protocol"))) {
                transport.connect(host, port, username, mailSession.getProperty("mail.smtp.password"));
                transport.sendMessage(message, message.getAllRecipients());
            }
        } catch (MessagingException e) {
            throw new ResourceException("Failed to send email", e);
        }
    }

    public void sendEmailWithAttachment(String to, String subject, String body, String attachmentPath)
            throws ResourceException {
        checkIfClosed();

        try {
            MimeMessage message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(from));
            message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);

            MimeMultipart multipart = new MimeMultipart();
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(body);
            multipart.addBodyPart(textPart);
            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.attachFile(new File(attachmentPath));
            multipart.addBodyPart(attachmentPart);

            message.setContent(multipart);
            String host = mailSession.getProperty("mail.smtp.host");
            int port = Integer.parseInt(mailSession.getProperty("mail.smtp.port"));
            try (Transport transport = mailSession.getTransport(mailSession.getProperty("mail.transport.protocol"))) {
                transport.connect(host, port, username, mailSession.getProperty("mail.smtp.password"));
                transport.sendMessage(message, message.getAllRecipients());
            }
        } catch (MessagingException | IOException e) {
            throw new ResourceException("Failed to send email with attachment", e);
        }
    }

    public void close() throws ResourceException {
        if (!closed) {
            closed = true;
            managedConnection.fireConnectionEvent(ConnectionEvent.CONNECTION_CLOSED, this, null);
        }
    }

    void invalidate() {
        closed = true;
    }

    private void checkIfClosed() throws ResourceException {
        if (closed) {
            throw new ResourceException("Connection is closed");
        }
    }
}
