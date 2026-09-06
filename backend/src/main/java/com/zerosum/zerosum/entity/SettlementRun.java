package com.zerosum.zerosum.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "settlement_runs")
@Getter
@Setter
@NoArgsConstructor
public class SettlementRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(name = "run_at", nullable = false, updatable = false)
    private Instant runAt;

    @Column(name = "raw_transaction_count", nullable = false)
    private Integer rawTransactionCount;

    @Column(name = "optimized_transaction_count", nullable = false)
    private Integer optimizedTransactionCount;

    @Column(name = "explanation_json", columnDefinition = "jsonb")
    private String explanationJson;

    @PrePersist
    protected void onCreate() {
        if (runAt == null) {
            runAt = Instant.now();
        }
    }
}