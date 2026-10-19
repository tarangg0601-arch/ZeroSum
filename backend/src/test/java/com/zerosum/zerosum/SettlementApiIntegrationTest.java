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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
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

    @Test
    void migratesAndRunsGroupExpenseWhatIfAndPaymentJourney() throws Exception {
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success", Integer.class)).isGreaterThanOrEqualTo(2);
        Long creator = post("/api/users", Map.of("name", "Creator", "email", "creator@example.test"), "userId");
        Long member = post("/api/users", Map.of("name", "Member", "email", "member@example.test"), "userId");
        Long group = post("/api/groups", Map.of("name", "Journey", "groupType", "general", "createdBy", creator), "id");
        perform(HttpMethod.POST, "/api/groups/" + group + "/members", Map.of("userId", member), 201);
        perform(HttpMethod.POST, "/api/groups/" + group + "/expenses", Map.of("description", "Train", "amountPaise", 1000,
                "category", "travel", "expenseDate", "2026-10-19", "paidBy", creator, "participantIds", List.of(creator, member), "splitType", "equal"), 201);

        int before = jdbc.queryForObject("select count(*) from expenses where group_id = ?", Integer.class, group);
        var preview = perform(HttpMethod.POST, "/api/groups/" + group + "/settlement/whatif", Map.of("expense", Map.of(
                "description", "Hypothetical hotel", "amountPaise", 10000, "category", "travel", "expenseDate", "2026-10-19",
                "paidBy", member, "participantIds", List.of(creator, member), "splitType", "equal")), 200);
        assertThat(preview.get("balances")).isNotNull();
        assertThat(jdbc.queryForObject("select count(*) from expenses where group_id = ?", Integer.class, group)).isEqualTo(before);

        perform(HttpMethod.POST, "/api/groups/" + group + "/settlement/payments", Map.of("fromUserId", member, "toUserId", creator, "amountPaise", 500), 201);
        String history = http.perform(request(HttpMethod.GET, "/api/groups/" + group + "/settlement/history")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(history)).hasSize(1);
        assertThat(jdbc.queryForObject("select count(*) from expenses where group_id = ?", Integer.class, group)).isEqualTo(before);
    }

    private Long post(String path, Map<String, ?> body, String idKey) {
        var response = perform(HttpMethod.POST, path, body, 201);
        return response.get(idKey).asLong();
    }

    private tools.jackson.databind.JsonNode perform(HttpMethod method, String path, Map<String, ?> body, int statusCode) {
        try {
            var builder = request(method, path).contentType(MediaType.APPLICATION_JSON);
            if (body != null) builder.content(objectMapper.writeValueAsBytes(body));
            MvcResult response = http.perform(builder).andExpect(status().is(statusCode)).andReturn();
            String content = response.getResponse().getContentAsString();
            return content.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(content);
        } catch (Exception exception) {
            throw new AssertionError("Request failed: " + method + " " + path, exception);
        }
    }
}
