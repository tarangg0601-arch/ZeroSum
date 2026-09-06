package com.zerosum.zerosum.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "expense_shares",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_expense_shares_expense_user",
            columnNames = {"expense_id", "user_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class ExpenseShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "share_amount_paise", nullable = false)
    private Long shareAmountPaise;

    @Column(name = "share_type", nullable = false, length = 20)
    private String shareType;

    @Column(nullable = false)
    private boolean participated = true;
}