package com.zerosum.zerosum.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "recurring_rules")
public class RecurringRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paid_by", nullable = false)
    private User paidBy;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "amount_paise", nullable = false)
    private Long amountPaise;

    @Column(length = 100)
    private String category;

    @Column(nullable = false, length = 50)
    private String frequency;

    @Column(name = "next_run_date", nullable = false)
    private LocalDate nextRunDate;

    @Column(name = "anchor_day", nullable = false)
    private int anchorDay;

    @Column(name = "anchor_month", nullable = false)
    private int anchorMonth;

    @Column(name = "anchor_month_end", nullable = false)
    private boolean anchorMonthEnd;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }
    public User getPaidBy() { return paidBy; }
    public void setPaidBy(User paidBy) { this.paidBy = paidBy; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getAmountPaise() { return amountPaise; }
    public void setAmountPaise(Long amountPaise) { this.amountPaise = amountPaise; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public LocalDate getNextRunDate() { return nextRunDate; }
    public void setNextRunDate(LocalDate nextRunDate) { this.nextRunDate = nextRunDate; }
    public int getAnchorDay() { return anchorDay; }
    public void setAnchorDay(int anchorDay) { this.anchorDay = anchorDay; }
    public int getAnchorMonth() { return anchorMonth; }
    public void setAnchorMonth(int anchorMonth) { this.anchorMonth = anchorMonth; }
    public boolean isAnchorMonthEnd() { return anchorMonthEnd; }
    public void setAnchorMonthEnd(boolean anchorMonthEnd) { this.anchorMonthEnd = anchorMonthEnd; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
