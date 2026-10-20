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
        properties.add("spring.flyway.user", POSTGRES::getUsername);
        properties.add("spring.flyway.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc http;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean com.zerosum.zerosum.service.EmailSender emailSender;
    private String activeToken;

    @Test
    void migratesAndRunsGroupExpenseWhatIfAndPaymentJourney() throws Exception {
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success", Integer.class)).isEqualTo(3);
        http.perform(get("/api/health")).andExpect(status().isOk());
        http.perform(get("/api/groups")).andExpect(status().isUnauthorized());
        Long creator = register("Creator", "creator@example.test");
        String creatorToken = activeToken;
        activeToken = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            perform(HttpMethod.POST, "/api/auth/login", Map.of("email", "creator@example.test", "password", "incorrect-password"), 401);
        }
        perform(HttpMethod.POST, "/api/auth/login", Map.of("email", "creator@example.test", "password", "incorrect-password"), 429);
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
        activeToken = memberToken;
        perform(HttpMethod.POST, "/api/groups/" + group + "/members", Map.of("email", "creator@example.test"), 403);
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

        perform(HttpMethod.POST, "/api/groups/" + group + "/settlement/payments", Map.of("fromUserId", member, "toUserId", creator, "amountPaise", 500), 201);
        var history = perform(HttpMethod.GET, "/api/groups/" + group + "/settlement/history", null, 200);
        assertThat(history).hasSize(1);
        assertThat(jdbc.queryForObject("select count(*) from expenses where group_id = ?", Integer.class, group)).isEqualTo(before);
        var afterPayment = perform(HttpMethod.GET, "/api/groups/" + group + "/settlement", null, 200);
        assertThat(afterPayment.path("transactions")).isEmpty();
        assertThat(List.of(afterPayment.path("balances").get(0).path("balancePaise").asLong(),
                afterPayment.path("balances").get(1).path("balancePaise").asLong())).containsExactly(0L, 0L);
        perform(HttpMethod.POST, "/api/auth/logout", null, 204);
        activeToken = null;
        http.perform(get("/api/groups").header("Authorization", "Bearer " + creatorToken)).andExpect(status().isUnauthorized());
    }

    @Test
    void claimsLegacyFinancialIdentityOnlyAfterMailboxTokenAndInvalidatesTokenAfterReset() throws Exception {
        jdbc.update("insert into users(name, email) values (?, ?)", "Legacy", "legacy@example.test");
        Long legacyId = jdbc.queryForObject("select id from users where email = ?", Long.class, "legacy@example.test");
        jdbc.update("insert into groups(name, group_type, join_code, created_by) values (?, ?, ?, ?)", "Legacy ledger", "general", "LEGACY01", legacyId);
        Long groupId = jdbc.queryForObject("select id from groups where join_code = ?", Long.class, "LEGACY01");
        jdbc.update("insert into group_members(group_id, user_id) values (?, ?)", groupId, legacyId);

        perform(HttpMethod.POST, "/api/auth/claim-legacy-account", Map.of("email", "legacy@example.test"), 200);
        String claimToken = emailedToken("legacy@example.test", "Secure your existing ZeroSum account");
        perform(HttpMethod.POST, "/api/auth/complete-legacy-claim", Map.of("token", claimToken, "password", "new-legacy-password-123"), 200);
        perform(HttpMethod.POST, "/api/auth/complete-legacy-claim", Map.of("token", claimToken, "password", "another-password-123"), 400);
        assertThat(jdbc.queryForObject("select id from users where email = ?", Long.class, "legacy@example.test")).isEqualTo(legacyId);
        assertThat(jdbc.queryForObject("select count(*) from group_members where group_id = ? and user_id = ?", Integer.class, groupId, legacyId)).isEqualTo(1);

        activeToken = null;
        var login = perform(HttpMethod.POST, "/api/auth/login", Map.of("email", "legacy@example.test", "password", "new-legacy-password-123"), 200);
        activeToken = login.get("token").asText();
        perform(HttpMethod.POST, "/api/auth/forgot-password", Map.of("email", "legacy@example.test"), 200);
        String resetToken = emailedToken("legacy@example.test", "Reset your ZeroSum password");
        String oldJwt = activeToken;
        perform(HttpMethod.POST, "/api/auth/reset-password", Map.of("token", resetToken, "password", "reset-password-123"), 200);
        activeToken = null;
        http.perform(get("/api/groups").header("Authorization", "Bearer " + oldJwt)).andExpect(status().isUnauthorized());
        var newLogin = perform(HttpMethod.POST, "/api/auth/login", Map.of("email", "legacy@example.test", "password", "reset-password-123"), 200);
        assertThat(newLogin.get("userId").asLong()).isEqualTo(legacyId);
    }

    @Test
    void expiredEmailVerificationTokenCannotVerifyAnAccount() throws Exception {
        perform(HttpMethod.POST, "/api/auth/register", Map.of("name", "Expired", "email", "expired@example.test",
                "password", "a-long-test-password-123"), 202);
        String token = emailedToken("expired@example.test", "Verify your ZeroSum email");
        jdbc.update("update auth_tokens set expires_at = current_timestamp - interval '1 minute' where user_id = "
                + "(select id from users where email = ?) and purpose = 'EMAIL_VERIFICATION'", "expired@example.test");
        perform(HttpMethod.POST, "/api/auth/verify-email", Map.of("token", token), 400);
        assertThat(jdbc.queryForObject("select email_verified from users where email = ?", Boolean.class, "expired@example.test"))
                .isFalse();
    }

    private Long post(String path, Map<String, ?> body, String idKey) {
        var response = perform(HttpMethod.POST, path, body, 201);
        return response.get(idKey).asLong();
    }

    private String memberToken;

    private Long register(String name, String email) throws Exception {
        perform(HttpMethod.POST, "/api/auth/register", Map.of("name", name, "email", email, "password", "a-long-test-password-123"), 202);
        String verification = emailedToken(email, "Verify your ZeroSum email");
        perform(HttpMethod.POST, "/api/auth/verify-email", Map.of("token", verification), 200);
        Long userId = jdbc.queryForObject("select id from users where email = ?", Long.class, email);
        var login = perform(HttpMethod.POST, "/api/auth/login", Map.of("email", email, "password", "a-long-test-password-123"), 200);
        activeToken = login.get("token").asText();
        if (name.equals("Member")) memberToken = activeToken;
        return userId;
    }

    private String emailedToken(String recipient, String subject) throws Exception {
        var bodies = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(emailSender, org.mockito.Mockito.atLeastOnce()).send(org.mockito.ArgumentMatchers.eq(recipient),
                org.mockito.ArgumentMatchers.eq(subject), bodies.capture());
        String body = bodies.getValue();
        return body.substring(body.indexOf("#token=") + 7).split("\\s", 2)[0];
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
