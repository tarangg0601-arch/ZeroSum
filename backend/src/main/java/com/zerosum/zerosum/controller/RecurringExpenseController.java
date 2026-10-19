package com.zerosum.zerosum.controller;

import com.zerosum.zerosum.dto.CreateRecurringRuleRequest;
import com.zerosum.zerosum.dto.RecurringRuleResponse;
import com.zerosum.zerosum.service.RecurringExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups/{groupId}/recurring")
public class RecurringExpenseController {
    private final RecurringExpenseService recurringExpenseService;

    public RecurringExpenseController(RecurringExpenseService recurringExpenseService) {
        this.recurringExpenseService = recurringExpenseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringRuleResponse create(@PathVariable Long groupId, @Valid @RequestBody CreateRecurringRuleRequest request) {
        return recurringExpenseService.createRule(groupId, request);
    }

    @GetMapping
    public List<RecurringRuleResponse> list(@PathVariable Long groupId) {
        return recurringExpenseService.getRules(groupId);
    }

    @PatchMapping("/{ruleId}")
    public RecurringRuleResponse setActive(@PathVariable Long groupId, @PathVariable Long ruleId,
            @RequestBody Map<String, Boolean> request) {
        Boolean active = request.get("active");
        if (active == null) throw new IllegalArgumentException("Active must be true or false");
        return recurringExpenseService.setActive(groupId, ruleId, active);
    }
}
