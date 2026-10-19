package com.zerosum.zerosum.controller;

import com.zerosum.zerosum.dto.GroupInsightsResponse;
import com.zerosum.zerosum.service.GroupInsightsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/groups/{groupId}/insights")
public class GroupInsightsController {
    private final GroupInsightsService service;
    public GroupInsightsController(GroupInsightsService service) { this.service = service; }

    @GetMapping
    public GroupInsightsResponse get(@PathVariable Long groupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.getInsights(groupId, from, to);
    }
}
