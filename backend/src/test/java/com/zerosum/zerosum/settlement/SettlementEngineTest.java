package com.zerosum.zerosum.settlement;

import com.zerosum.zerosum.dto.ExpenseResponse;
import com.zerosum.zerosum.dto.ExpenseShareResponse;
import com.zerosum.zerosum.dto.GroupMemberResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementEngineTest {

    @Test
    void shouldReturnNoTransfersForEmptyOrTrivialBalances() {
        SettlementEngine engine = new SettlementEngine();
        assertTrue(engine.calculateSettlements(List.of()).isEmpty());
        assertTrue(engine.calculateSettlements(List.of(new BalanceDTO(1L, "A", 0L))).isEmpty());
    }

    @Test
    void shouldUseStableUserIdTieBreaks() {
        SettlementEngine engine = new SettlementEngine();
        var result = engine.calculateSettlements(List.of(
                new BalanceDTO(3L, "C", -500L), new BalanceDTO(2L, "B", 500L),
                new BalanceDTO(1L, "A", 500L), new BalanceDTO(4L, "D", -500L)));
        assertEquals(List.of(1L, 2L), result.stream().map(SettlementTransactionDTO::toUserId).toList());
        assertEquals(List.of(3L, 4L), result.stream().map(SettlementTransactionDTO::fromUserId).toList());
    }

    @Test
    void shouldReconcileUnevenPaiseAcrossSeveralMembers() {
        SettlementEngine engine = new SettlementEngine();
        var balances = List.of(new BalanceDTO(1L, "A", 101L), new BalanceDTO(2L, "B", 1L),
                new BalanceDTO(3L, "C", -33L), new BalanceDTO(4L, "D", -69L));
        var result = engine.calculateSettlements(balances);
        assertEquals(0L, result.stream().mapToLong(SettlementTransactionDTO::amountPaise).sum()
                - balances.stream().filter(b -> b.balancePaise() > 0).mapToLong(BalanceDTO::balancePaise).sum());
        assertEquals(0L, result.stream().mapToLong(t -> t.amountPaise()).sum()
                - balances.stream().filter(b -> b.balancePaise() < 0).mapToLong(b -> -b.balancePaise()).sum());
    }

    @Test
    void shouldCalculateExpectedSettlementsForFivePersonExample() {

        SettlementEngine engine = new SettlementEngine();

        List<BalanceDTO> balances = List.of(
                new BalanceDTO(1L, "Rahul", 570000L),
                new BalanceDTO(2L, "Arjun", 70000L),
                new BalanceDTO(3L, "Priya", -130000L),
                new BalanceDTO(4L, "Sneha", -280000L),
                new BalanceDTO(5L, "Vikram", -230000L)
        );

        List<SettlementTransactionDTO> transactions =
                engine.calculateSettlements(balances);

        assertEquals(4, transactions.size());

        // Sneha -> Rahul ₹2,800
        assertEquals(4L, transactions.get(0).fromUserId());
        assertEquals("Sneha", transactions.get(0).fromUserName());
        assertEquals(1L, transactions.get(0).toUserId());
        assertEquals("Rahul", transactions.get(0).toUserName());
        assertEquals(280000L, transactions.get(0).amountPaise());

        // Vikram -> Rahul ₹2,300
        assertEquals(5L, transactions.get(1).fromUserId());
        assertEquals("Vikram", transactions.get(1).fromUserName());
        assertEquals(1L, transactions.get(1).toUserId());
        assertEquals("Rahul", transactions.get(1).toUserName());
        assertEquals(230000L, transactions.get(1).amountPaise());

        // Priya -> Arjun ₹700
        assertEquals(3L, transactions.get(2).fromUserId());
        assertEquals("Priya", transactions.get(2).fromUserName());
        assertEquals(2L, transactions.get(2).toUserId());
        assertEquals("Arjun", transactions.get(2).toUserName());
        assertEquals(70000L, transactions.get(2).amountPaise());

        // Priya -> Rahul ₹600
        assertEquals(3L, transactions.get(3).fromUserId());
        assertEquals("Priya", transactions.get(3).fromUserName());
        assertEquals(1L, transactions.get(3).toUserId());
        assertEquals("Rahul", transactions.get(3).toUserName());
        assertEquals(60000L, transactions.get(3).amountPaise());
    }

    @Test
    void shouldExcludeZeroBalanceMembersFromSettlements() {

        SettlementEngine engine = new SettlementEngine();

        List<BalanceDTO> balances = List.of(
                new BalanceDTO(1L, "Rahul", 50000L),
                new BalanceDTO(2L, "Aman", -50000L),
                new BalanceDTO(3L, "Priya", 0L)
        );

        List<SettlementTransactionDTO> transactions =
                engine.calculateSettlements(balances);

        assertEquals(1, transactions.size());

        SettlementTransactionDTO transaction =
                transactions.get(0);

        assertEquals(2L, transaction.fromUserId());
        assertEquals(1L, transaction.toUserId());
        assertEquals(50000L, transaction.amountPaise());
    }

    @Test
    void shouldKeepBalancesNetToZero() {

        SettlementEngine engine = new SettlementEngine();

        List<GroupMemberResponse> members = List.of(
                new GroupMemberResponse(1L, "Rahul", "rahul@test.com"),
                new GroupMemberResponse(2L, "Aman", "aman@test.com"),
                new GroupMemberResponse(3L, "Priya", "priya@test.com")
        );

        List<ExpenseShareResponse> shares = List.of(
                new ExpenseShareResponse(
                        1L,
                        "Rahul",
                        33334L,
                        "equal",
                        true
                ),
                new ExpenseShareResponse(
                        2L,
                        "Aman",
                        33333L,
                        "equal",
                        true
                ),
                new ExpenseShareResponse(
                        3L,
                        "Priya",
                        33333L,
                        "equal",
                        true
                )
        );

        ExpenseResponse expense = new ExpenseResponse(
                1L,
                1L,
                1L,
                "Rahul",
                "Test Expense",
                100000L,
                "general",
                LocalDate.now(),
                false,
                Instant.now(),
                shares
        );

        List<BalanceDTO> balances =
                engine.calculateBalances(
                        members,
                        List.of(expense)
                );

        long totalBalance = balances.stream()
                .mapToLong(BalanceDTO::balancePaise)
                .sum();

        assertEquals(0L, totalBalance);
    }

    @Test
    void shouldGenerateSettlementExplanation() {

        SettlementEngine engine = new SettlementEngine();

        List<SettlementTransactionDTO> transactions = List.of(
                new SettlementTransactionDTO(
                        3L,
                        "Aman",
                        2L,
                        "Tarang",
                        50000L
                )
        );

        SettlementExplanationDTO explanation =
                engine.explainSettlements(transactions);

        assertEquals(3, explanation.steps().size());

        assertEquals(
                "ZeroSum matches people who owe money with people who should receive money.",
                explanation.steps().get(0)
        );

        assertEquals(
                "Step 1: Aman pays Tarang ₹500.",
                explanation.steps().get(1)
        );

        assertEquals(
                "Settlement complete: 1 transaction required.",
                explanation.steps().get(2)
        );
    }
}
