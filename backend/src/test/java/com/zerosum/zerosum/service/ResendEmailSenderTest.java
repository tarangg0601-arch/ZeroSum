package com.zerosum.zerosum.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ResendEmailSenderTest {
    @Test
    void sendsPlainTextThroughResendHttpsApiWithBearerKey() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.resend.test/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("verified@example.test"),
                        org.hamcrest.Matchers.containsString("Verify your email"),
                        org.hamcrest.Matchers.containsString("one-time link"))))
                .andRespond(withSuccess("{\"id\":\"message-id\"}", MediaType.APPLICATION_JSON));

        new ResendEmailSender(builder, "https://api.resend.test", "test-api-key", "ZeroSum <security@example.test>")
                .send("verified@example.test", "Verify your email", "Use this one-time link");
        server.verify();
    }

    @Test
    void doesNotExposeProviderErrorBodyOrApiKey() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.resend.test/emails"))
                .andRespond(withServerError().body("test-api-key rejected for recipient"));

        EmailDeliveryUnavailableException failure = assertThrows(EmailDeliveryUnavailableException.class,
                () -> new ResendEmailSender(builder, "https://api.resend.test", "test-api-key", "security@example.test")
                        .send("person@example.test", "Reset", "secret token"));
        assertEquals("Email delivery failed", failure.getMessage());
        assertFalse(failure.getMessage().contains("test-api-key"));
        server.verify();
    }

    @Test
    void refusesToSendWhenConfigurationIsMissing() {
        ResendEmailSender sender = new ResendEmailSender(RestClient.builder(), "https://api.resend.test", "", "");
        assertThrows(EmailDeliveryUnavailableException.class,
                () -> sender.send("person@example.test", "Reset", "secret token"));
    }

    @Test
    void refusesInsecureProviderEndpoints() {
        assertThrows(IllegalStateException.class, () -> new ResendEmailSender(
                RestClient.builder(), "http://api.resend.test", "test-api-key", "security@example.test"));
    }
}
