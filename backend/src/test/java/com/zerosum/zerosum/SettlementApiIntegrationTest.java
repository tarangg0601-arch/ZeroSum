package com.zerosum.zerosum;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.auth.token-secret=integration-only-test-key-with-more-than-32-bytes")
class SettlementApiIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc http;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;
    private String activeToken;

    @Test
    void migratesAndRunsGroupExpenseWhatIfAndPaymentJourney() throws Exception {
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success", Integer.class)).isGreaterThanOrEqualTo(2);
        http.perform(get("/api/groups")).andExpect(status().isUnauthorized());
        Long creator = register("Creator", "creator@example.test");
        activeToken = null;
        var loggedIn = perform(HttpMethod.POST, "/api/auth/login", Map.of("email", "creator@example.test", "password", "a-long-test-password-123"), 200);
        String creatorToken = loggedIn.get("token").asText();
        activeToken = creatorToken;
        Long member = register("Member", "member@example.test");
        activeToken = creatorToken;
        Long group = post("/api/groups", Map.of("name", "Journey", "groupType", "general"), "id");
        activeToken = null;
        http.perform(get("/api/groups/" + group)).andExpect(status().isUnauthorized());
        // The second registered account is authenticated with its actual token below and cannot access this group yet.
        activeToken = memberToken;
        http.perform(get("/api/groups/" + group).header("Authorization", "Bearer " + activeToken)).andExpect(status().isNotFound());
        activeToken = creatorToken;
        perform(HttpMethod.POST, "/api/groups/" + group + "/members", Map.of("email", "member@example.test"), 201);
        perform(HttpMethod.POST, "/api/groups/" + group + "/expenses", Map.of("description", "Train", "amountPaise", 1000,
                "category", "travel", "expenseDate", "2026-10-19", "paidBy", creator, "participantIds", List.of(creator, member), "splitType", "equal"), 201);

        var balances = perform(HttpMethod.GET, "/api/groups/" + group + "/balances", null, 200);
        assertThat(balances).hasSize(2);
        assertThat(List.of(balances.get(0).path("userId").asLong(), balances.get(1).path("userId").asLong()))
                .containsExactlyInAnyOrder(creator, member);
        assertThat(List.of(balances.get(0).path("balancePaise").asLong(), balances.get(1).path("balancePaise").asLong()))
                .containsExactlyInAnyOrder(500L, -500L);
        var optimized = perform(HttpMethod.GET, "/api/groups/" + group + "/settlement", null, 200);
        assertThat(optimized.path("transactions")).hasSize(1);
        assertThat(optimized.path("transactions").get(0).path("fromUserId").asLong()).isEqualTo(member);
        assertThat(optimized.path("transactions").get(0).path("toUserId").asLong()).isEqualTo(creator);
        assertThat(optimized.path("transactions").get(0).path("amountPaise").asLong()).isEqualTo(500);

        int before = jdbc.queryForObject("select count(*) from expenses where group_id = ?", Integer.class, group);
        var preview = perform(HttpMethod.POST, "/api/groups/" + group + "/settlement/whatif", Map.of("expense", Map.of(
                "description", "Hypothetical hotel", "amountPaise", 10000, "category", "travel", "expenseDate", "2026-10-19",
                "paidBy", member, "participantIds", List.of(creator, member), "splitType", "equal")), 200);
        assertThat(preview.get("balances")).isNotNull();
        assertThat(jdbc.queryForObject("select count(*) from expenses where group_id = ?", Integer.class, group)).isEqualTo(before);

        perform(HttpMethod.POST, "/api/groups/" + group + "/settlement/payments", Map.of("fromUserId", member, "toUserId", creator, "amountPaise", 500), 201);
        var history = perform(HttpMethod.GET, "/api/groups/" + group + "/settlement/history", null, 200);
        assertThat(history).hasSize(1);
        assertThat(jdbc.queryForObject("select count(*) from expenses where group_id = ?", Integer.class, group)).isEqualTo(before);
        var afterPayment = perform(HttpMethod.GET, "/api/groups/" + group + "/settlement", null, 200);
        assertThat(afterPayment.path("transactions")).isEmpty();
        assertThat(List.of(afterPayment.path("balances").get(0).path("balancePaise").asLong(),
                afterPayment.path("balances").get(1).path("balancePaise").asLong())).containsExactly(0L, 0L);
    }

    private Long post(String path, Map<String, ?> body, String idKey) {
        var response = perform(HttpMethod.POST, path, body, 201);
        return response.get(idKey).asLong();
    }

    private String memberToken;

    private Long register(String name, String email) {
        var response = perform(HttpMethod.POST, "/api/auth/register", Map.of("name", name, "email", email, "password", "a-long-test-password-123"), 201);
        String token = response.get("token").asText();
        if (name.equals("Member")) memberToken = token;
        activeToken = token;
        return response.get("userId").asLong();
    }

    private tools.jackson.databind.JsonNode perform(HttpMethod method, String path, Map<String, ?> body, int statusCode) {
        try {
            var builder = request(method, path).contentType(MediaType.APPLICATION_JSON);
            if (activeToken != null && !activeToken.isBlank()) builder.header("Authorization", "Bearer " + activeToken);
            if (body != null) builder.content(objectMapper.writeValueAsBytes(body));
            MvcResult response = http.perform(builder).andExpect(status().is(statusCode)).andReturn();
            String content = response.getResponse().getContentAsString();
            return content.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(content);
        } catch (Exception exception) {
            throw new AssertionError("Request failed: " + method + " " + path, exception);
        }
    }
}
