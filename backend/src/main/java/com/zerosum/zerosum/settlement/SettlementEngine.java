package com.zerosum.zerosum.settlement;

import com.zerosum.zerosum.dto.ExpenseResponse;
import com.zerosum.zerosum.dto.GroupMemberResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

@Service
public class SettlementEngine {

    /**
     * Calculates the net balance of every group member.
     *
     * Positive balance = person should receive money.
     * Negative balance = person should pay money.
     * Zero balance = person is already settled.
     */
    public List<BalanceDTO> calculateBalances(
            List<GroupMemberResponse> members,
            List<ExpenseResponse> expenses
    ) {

        Map<Long, Long> balances = new HashMap<>();

        for (GroupMemberResponse member : members) {
            balances.put(member.userId(), 0L);
        }

        for (ExpenseResponse expense : expenses) {

            balances.merge(
                    expense.paidBy(),
                    expense.amountPaise(),
                    Math::addExact
            );

            for (var share : expense.shares()) {

                balances.merge(
                        share.userId(),
                        Math.negateExact(share.shareAmountPaise()),
                        Math::addExact
                );
            }
        }

        List<BalanceDTO> result = new ArrayList<>();

        for (GroupMemberResponse member : members) {

            result.add(
                    new BalanceDTO(
                            member.userId(),
                            member.name(),
                            balances.getOrDefault(
                                    member.userId(),
                                    0L
                            )
                    )
            );
        }

        return result;
    }

    /**
     * Calculates settlement transactions using a deterministic greedy
     * cash-flow heuristic. This does not guarantee a globally minimal count.
     */
    public List<SettlementTransactionDTO> calculateSettlements(
            List<BalanceDTO> balances
    ) {

        PriorityQueue<BalanceState> creditors =
                new PriorityQueue<>(
                        (a, b) -> {

                            int comparison =
                                    Long.compare(
                                            b.balancePaise,
                                            a.balancePaise
                                    );

                            if (comparison != 0) {
                                return comparison;
                            }

                            return Long.compare(
                                    a.userId,
                                    b.userId
                            );
                        }
                );

        PriorityQueue<BalanceState> debtors =
                new PriorityQueue<>(
                        (a, b) -> {

                            int comparison = Long.compare(a.balancePaise, b.balancePaise);

                            if (comparison != 0) {
                                return comparison;
                            }

                            return Long.compare(
                                    a.userId,
                                    b.userId
                            );
                        }
                );

        for (BalanceDTO balance : balances) {

            if (balance.balancePaise() > 0) {

                creditors.add(
                        new BalanceState(
                                balance.userId(),
                                balance.userName(),
                                balance.balancePaise()
                        )
                );

            } else if (balance.balancePaise() < 0) {

                debtors.add(
                        new BalanceState(
                                balance.userId(),
                                balance.userName(),
                                balance.balancePaise()
                        )
                );
            }
        }

        List<SettlementTransactionDTO> transactions =
                new ArrayList<>();

        while (!creditors.isEmpty() && !debtors.isEmpty()) {

            BalanceState creditor = creditors.poll();
            BalanceState debtor = debtors.poll();

            long amount = Math.min(
                    creditor.balancePaise,
                    Math.negateExact(debtor.balancePaise)
            );

            transactions.add(
                    new SettlementTransactionDTO(
                            debtor.userId,
                            debtor.userName,
                            creditor.userId,
                            creditor.userName,
                            amount
                    )
            );

            creditor.balancePaise -= amount;
            debtor.balancePaise += amount;

            if (creditor.balancePaise > 0) {
                creditors.add(creditor);
            }

            if (debtor.balancePaise < 0) {
                debtors.add(debtor);
            }
        }

        return transactions;
    }

    /**
     * Creates a human-readable explanation trace
     * for the generated settlement transactions.
     */
    public SettlementExplanationDTO explainSettlements(
            List<SettlementTransactionDTO> transactions
    ) {

        List<String> steps = new ArrayList<>();

        steps.add(
                "ZeroSum matches people who owe money with people who should receive money."
        );

        for (int i = 0; i < transactions.size(); i++) {

            SettlementTransactionDTO transaction =
                    transactions.get(i);

            steps.add(
                    "Step "
                            + (i + 1)
                            + ": "
                            + transaction.fromUserName()
                            + " pays "
                            + transaction.toUserName()
                            + " "
                            + formatPaise(transaction.amountPaise())
                            + "."
            );
        }

        steps.add(
                "Settlement complete: "
                        + transactions.size()
                        + " transaction"
                        + (transactions.size() == 1 ? "" : "s")
                        + " required."
        );

        return new SettlementExplanationDTO(steps);
    }

    /**
     * Converts paise into a readable Indian Rupee amount.
     */
    private String formatPaise(long paise) {

        long rupees = paise / 100;
        long remainingPaise = Math.abs(paise % 100);

        if (remainingPaise == 0) {
            return "₹" + String.format("%,d", rupees);
        }

        return "₹"
                + String.format("%,d", rupees)
                + "."
                + String.format("%02d", remainingPaise);
    }

    /**
     * Internal mutable state used by the settlement heaps.
     */
    private static class BalanceState {

        private final Long userId;
        private final String userName;
        private long balancePaise;

        private BalanceState(
                Long userId,
                String userName,
                long balancePaise
        ) {
            this.userId = userId;
            this.userName = userName;
            this.balancePaise = balancePaise;
        }
    }
}
