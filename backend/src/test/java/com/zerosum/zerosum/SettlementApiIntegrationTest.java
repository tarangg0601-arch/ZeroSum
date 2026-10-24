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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
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
        properties.add("spring.flyway.user", POSTGRES::getUsername);
        properties.add("spring.flyway.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc http;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtEncoder jwtEncoder;
    private String activeToken;

    @Test
    void migratesAndRunsGroupExpenseWhatIfAndPaymentJourney() throws Exception {
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success", Integer.class)).isEqualTo(7);
        assertThat(jdbc.queryForObject("select to_regclass('settlement_runs') is null and to_regclass('settlement_runs_legacy') is not null", Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject("select to_regclass('auth_email_attempts') is not null", Boolean.class)).isTrue();
        http.perform(get("/api/health")).andExpect(status().isOk());
        http.perform(get("/api/groups")).andExpect(status().isUnauthorized());
        Long creator = register("creator_1", "Creator");
        String creatorToken = activeToken;
        activeToken = null;
        var normalizedLogin = perform(HttpMethod.POST, "/api/auth/login", Map.of("username", "CREATOR_1", "password", "a-long-test-password-123"), 200);
        assertThat(normalizedLogin.path("username").asText()).isEqualTo("creator_1");
        creatorToken = normalizedLogin.path("token").asText();
        perform(HttpMethod.POST, "/api/auth/register", Map.of("username", "Creator_1", "password", "a-long-test-password-123"), 409);
        for (int attempt = 0; attempt < 5; attempt++) {
            perform(HttpMethod.POST, "/api/auth/login", Map.of("username", "creator_1", "password", "incorrect-password"), 401);
        }
        perform(HttpMethod.POST, "/api/auth/login", Map.of("username", "creator_1", "password", "incorrect-password"), 429);
        activeToken = creatorToken;
        Long member = register("member_1", "Member");
        activeToken = creatorToken;
        perform(HttpMethod.POST, "/api/groups", Map.of("name", "Invalid type", "groupType", "unsupported"), 400);
        var createdGroup = perform(HttpMethod.POST, "/api/groups", Map.of("name", "Journey", "groupType", "general"), 201);
        Long group = createdGroup.get("id").asLong();
        String joinCode = createdGroup.get("joinCode").asText();
        assertThat(joinCode).hasSize(16).matches("[A-Z0-9_-]+");
        activeToken = null;
        http.perform(get("/api/groups/" + group)).andExpect(status().isUnauthorized());
        // The second registered account is authenticated with its actual token below and cannot access this group yet.
        activeToken = memberToken;
        http.perform(get("/api/groups/" + group).header("Authorization", "Bearer " + activeToken)).andExpect(status().isNotFound());
        activeToken = memberToken;
        perform(HttpMethod.POST, "/api/groups/join", Map.of("joinCode", "invalid-code"), 404);
        perform(HttpMethod.POST, "/api/groups/join", Map.of("joinCode", joinCode.toLowerCase()), 200);
        perform(HttpMethod.POST, "/api/groups/join", Map.of("joinCode", joinCode), 409);
        for (int attempt = 0; attempt < 7; attempt++) {
            perform(HttpMethod.POST, "/api/groups/join", Map.of("joinCode", "not-a-code"), 404);
        }
        perform(HttpMethod.POST, "/api/groups/join", Map.of("joinCode", "not-a-code"), 429);
        assertThat(jdbc.queryForObject("select max(attempt_count) from group_join_attempts", Integer.class)).isEqualTo(11);
        activeToken = memberToken;
        perform(HttpMethod.POST, "/api/groups/" + group + "/members", Map.of("username", "creator_1"), 403);
        perform(HttpMethod.DELETE, "/api/groups/" + group, null, 403);
        activeToken = creatorToken;
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

        Map<String, Object> paymentRequest = Map.of("fromUserId", member, "toUserId", creator, "amountPaise", 500);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var firstPayment = java.util.concurrent.CompletableFuture.supplyAsync(() -> paymentAttempt(group, memberToken, paymentRequest), executor);
            var duplicatePayment = java.util.concurrent.CompletableFuture.supplyAsync(() -> paymentAttempt(group, memberToken, paymentRequest), executor);
            assertThat(List.of(firstPayment.join(), duplicatePayment.join())).containsExactlyInAnyOrder(201, 400);
        } finally {
            executor.shutdownNow();
        }
        var history = perform(HttpMethod.GET, "/api/groups/" + group + "/settlement/history", null, 200);
        assertThat(history).hasSize(1);
        assertThat(jdbc.queryForObject("select count(*) from expenses where group_id = ?", Integer.class, group)).isEqualTo(before);
        var afterPayment = perform(HttpMethod.GET, "/api/groups/" + group + "/settlement", null, 200);
        assertThat(afterPayment.path("transactions")).isEmpty();
        assertThat(List.of(afterPayment.path("balances").get(0).path("balancePaise").asLong(),
                afterPayment.path("balances").get(1).path("balancePaise").asLong())).containsExactly(0L, 0L);
        Long expenseId = jdbc.queryForObject("select id from expenses where group_id = ?", Long.class, group);
        Map<String, Object> correction = Map.of("description", "Changed train", "amountPaise", 1000,
                "category", "travel", "expenseDate", "2026-10-19", "paidBy", creator,
                "participantIds", List.of(creator, member), "splitType", "equal");
        perform(HttpMethod.PUT, "/api/groups/" + group + "/expenses/" + expenseId, correction, 409);
        perform(HttpMethod.DELETE, "/api/groups/" + group + "/expenses/" + expenseId, null, 409);
        perform(HttpMethod.DELETE, "/api/groups/" + group, null, 409);
        perform(HttpMethod.POST, "/api/auth/logout", null, 204);
        activeToken = null;
        http.perform(get("/api/groups").header("Authorization", "Bearer " + creatorToken)).andExpect(status().isUnauthorized());
    }

    @Test
    void legacyAccountsRemainUnclaimedAndEmailRecoveryEndpointsAreUnavailable() throws Exception {
        jdbc.update("insert into users(name, email) values (?, ?)", "Legacy", "legacy@example.test");
        Long legacyId = jdbc.queryForObject("select id from users where email = ?", Long.class, "legacy@example.test");
        jdbc.update("insert into groups(name, group_type, join_code, created_by) values (?, ?, ?, ?)", "Legacy ledger", "general", "LEGACY01", legacyId);
        Long groupId = jdbc.queryForObject("select id from groups where join_code = ?", Long.class, "LEGACY01");
        jdbc.update("insert into group_members(group_id, user_id) values (?, ?)", groupId, legacyId);

        activeToken = null;
        perform(HttpMethod.POST, "/api/auth/login", Map.of("username", "legacy_user", "password", "anything-at-all"), 401);
        perform(HttpMethod.POST, "/api/auth/forgot-password", Map.of("email", "legacy@example.test"), 401);
        assertThat(jdbc.queryForObject("select username from users where id = ?", String.class, legacyId)).isNull();
        assertThat(jdbc.queryForObject("select id from users where email = ?", Long.class, "legacy@example.test")).isEqualTo(legacyId);
        assertThat(jdbc.queryForObject("select count(*) from group_members where group_id = ? and user_id = ?", Integer.class, groupId, legacyId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from groups where id = ?", Integer.class, groupId)).isEqualTo(1);

    }

    @Test
    void rejectsExpiredJwt() throws Exception {
        Instant expiredAt = Instant.now().minusSeconds(60);
        String expired = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().issuer("zerosum").subject("999999").issuedAt(expiredAt.minusSeconds(60))
                        .expiresAt(expiredAt).claim("ver", 0).build())).getTokenValue();
        http.perform(get("/api/groups").header("Authorization", "Bearer " + expired)).andExpect(status().isUnauthorized());
    }

    private Long post(String path, Map<String, ?> body, String idKey) {
        var response = perform(HttpMethod.POST, path, body, 201);
        return response.get(idKey).asLong();
    }

    private String memberToken;

    private Long register(String username, String name) {
        var registration = perform(HttpMethod.POST, "/api/auth/register", Map.of("username", username, "name", name,
                "password", "a-long-test-password-123"), 201);
        Long userId = registration.path("userId").asLong();
        assertThat(registration.path("username").asText()).isEqualTo(username);
        assertThat(jdbc.queryForObject("select email from users where id = ?", String.class, userId)).isNull();
        var login = perform(HttpMethod.POST, "/api/auth/login", Map.of("username", username, "password", "a-long-test-password-123"), 200);
        activeToken = login.get("token").asText();
        if (name.equals("Member")) memberToken = activeToken;
        return userId;
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

    private int paymentAttempt(Long groupId, String token, Map<String, ?> body) {
        try {
            return http.perform(request(HttpMethod.POST, "/api/groups/" + groupId + "/settlement/payments")
                    .contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + token)
                    .content(objectMapper.writeValueAsBytes(body))).andReturn().getResponse().getStatus();
        } catch (Exception exception) {
            throw new java.util.concurrent.CompletionException(exception);
        }
    }
}
