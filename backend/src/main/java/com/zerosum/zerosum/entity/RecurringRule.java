package com.zerosum.zerosum.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "recurring_rules")
@Getter
@Setter
@NoArgsConstructor
public class RecurringRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

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
}