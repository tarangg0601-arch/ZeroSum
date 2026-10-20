package com.zerosum.zerosum.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

@RestController
@RequestMapping("/api")
public class HealthController {
    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/health")
    public Map<String, String> health() {
        jdbc.queryForObject("select 1", Integer.class);
        return Map.of("status", "OK");
    }
}
