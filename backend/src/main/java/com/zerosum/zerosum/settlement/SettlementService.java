package com.zerosum.zerosum.settlement;

import com.zerosum.zerosum.dto.ExpenseResponse;
import com.zerosum.zerosum.dto.GroupMemberResponse;
import com.zerosum.zerosum.service.ExpenseService;
import com.zerosum.zerosum.service.GroupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SettlementService {

    private final SettlementEngine settlementEngine;
    private final GroupService groupService;
    private final ExpenseService expenseService;

    public SettlementService(
            SettlementEngine settlementEngine,
            GroupService groupService,
            ExpenseService expenseService
    ) {
        this.settlementEngine = settlementEngine;
        this.groupService = groupService;
        this.expenseService = expenseService;
    }

    @Transactional(readOnly = true)
    public List<BalanceDTO> getBalances(Long groupId) {

        List<GroupMemberResponse> members =
                groupService.getMembers(groupId);

        List<ExpenseResponse> expenses =
                expenseService.getExpensesByGroup(groupId);

        return settlementEngine.calculateBalances(
                members,
                expenses
        );
    }

    @Transactional(readOnly = true)
    public SettlementResponseDTO getSettlement(Long groupId) {

        List<BalanceDTO> balances =
                getBalances(groupId);

        List<SettlementTransactionDTO> transactions =
                settlementEngine.calculateSettlements(
                        balances
                );

        SettlementExplanationDTO explanation =
                settlementEngine.explainSettlements(
                        transactions
                );

        return new SettlementResponseDTO(
                balances,
                transactions,
                explanation
        );
    }
}