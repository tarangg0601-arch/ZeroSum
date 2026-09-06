package com.zerosum.zerosum.controller;

import com.zerosum.zerosum.dto.CreateExpenseRequest;
import com.zerosum.zerosum.dto.ExpenseResponse;
import com.zerosum.zerosum.dto.UpdateExpenseRequest;
import com.zerosum.zerosum.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse createExpense(
            @PathVariable Long groupId,
            @Valid @RequestBody CreateExpenseRequest request
    ) {
        return expenseService.createExpense(groupId, request);
    }

    @GetMapping
    public List<ExpenseResponse> getExpenses(
            @PathVariable Long groupId
    ) {
        return expenseService.getExpensesByGroup(groupId);
    }

    @GetMapping("/{expenseId}")
    public ExpenseResponse getExpense(
            @PathVariable Long groupId,
            @PathVariable Long expenseId
    ) {
        return expenseService.getExpense(groupId, expenseId);
    }

    @PutMapping("/{expenseId}")
    public ExpenseResponse updateExpense(
            @PathVariable Long groupId,
            @PathVariable Long expenseId,
            @Valid @RequestBody UpdateExpenseRequest request
    ) {
        return expenseService.updateExpense(
                groupId,
                expenseId,
                request
        );
    }

    @DeleteMapping("/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(
            @PathVariable Long groupId,
            @PathVariable Long expenseId
    ) {
        expenseService.deleteExpense(
                groupId,
                expenseId
        );
    }
}