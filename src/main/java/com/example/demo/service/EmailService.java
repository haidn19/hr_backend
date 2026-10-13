package com.example.demo.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;
    private final String recipientOverride;

    public EmailService(JavaMailSender mailSender,
            @Value("${app.mail.from}") String from,
            @Value("${app.mail.recipient-override:}") String recipientOverride) {
        this.mailSender = mailSender;
        this.from = from;
        this.recipientOverride = recipientOverride == null ? "" : recipientOverride.trim();
    }

    public void sendTestEmail(String to) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(from);
        message.setTo(recipient(to));
        message.setSubject("HR System");
        message.setText("This is a test email from the HR System.");

        mailSender.send(message);
    }

    public void sendHtmlEmail(String to, String subject, String html) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, "UTF-8");
            message.setFrom(from);
            message.setTo(recipient(to));
            message.setSubject(subject);
            message.setText(html, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException exception) {
            throw new IllegalStateException("Could not create monthly attendance email", exception);
        }
    }

    private String recipient(String requestedRecipient) {
        return recipientOverride.isBlank() ? requestedRecipient : recipientOverride;
    }
}
