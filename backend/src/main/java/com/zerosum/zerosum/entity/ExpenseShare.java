package com.zerosum.zerosum.entity;

import jakarta.persistence.*;

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

    public Long getId() {
        return id;
    }

    public Expense getExpense() {
        return expense;
    }

    public void setExpense(Expense expense) {
        this.expense = expense;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Long getShareAmountPaise() {
        return shareAmountPaise;
    }

    public void setShareAmountPaise(Long shareAmountPaise) {
        this.shareAmountPaise = shareAmountPaise;
    }

    public String getShareType() {
        return shareType;
    }

    public void setShareType(String shareType) {
        this.shareType = shareType;
    }

    public boolean isParticipated() {
        return participated;
    }

    public void setParticipated(boolean participated) {
        this.participated = participated;
    }
}