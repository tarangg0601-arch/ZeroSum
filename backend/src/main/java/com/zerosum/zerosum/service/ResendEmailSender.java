package com.zerosum.zerosum.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.net.URI;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "resend")
public class ResendEmailSender implements EmailSender {
    private final RestClient client;
    private final String apiKey;
    private final String from;

    public ResendEmailSender(RestClient.Builder builder,
                             @Value("${app.email.resend.base-url:https://api.resend.com}") String baseUrl,
                             @Value("${app.email.resend.api-key:}") String apiKey,
                             @Value("${app.auth.email.from:}") String from) {
        if (!"https".equalsIgnoreCase(URI.create(baseUrl).getScheme())) {
            throw new IllegalStateException("Resend API base URL must use HTTPS");
        }
        this.client = builder.clone().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.from = from;
    }

    @Override
    public void send(String recipient, String subject, String body) {
        if (apiKey.isBlank() || from.isBlank()) {
            throw new EmailDeliveryUnavailableException("Email delivery is not configured");
        }
        try {
            client.post().uri("/emails")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(Map.of("from", from, "to", List.of(recipient), "subject", subject, "text", body))
                    .retrieve().toBodilessEntity();
        } catch (RuntimeException exception) {
            // Provider response bodies can contain recipient details. Never expose them or credentials.
            throw new EmailDeliveryUnavailableException("Email delivery failed");
        }
    }
}
