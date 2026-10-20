package com.zerosum.zerosum.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpEmailSender implements EmailSender {
    private final ObjectProvider<JavaMailSender> mailSenders;
    private final String from;

    public SmtpEmailSender(ObjectProvider<JavaMailSender> mailSenders,
                           @Value("${app.auth.email.from:}") String from) {
        this.mailSenders = mailSenders;
        this.from = from;
    }

    @Override
    public void send(String recipient, String subject, String body) {
        JavaMailSender sender = mailSenders.getIfAvailable();
        if (sender == null || from.isBlank()) {
            throw new EmailDeliveryUnavailableException("Email delivery is not configured");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);
        sender.send(message);
    }
}
