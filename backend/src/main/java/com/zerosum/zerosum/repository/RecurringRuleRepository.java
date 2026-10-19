package com.zerosum.zerosum.repository;

import com.zerosum.zerosum.entity.RecurringRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RecurringRuleRepository extends JpaRepository<RecurringRule, Long> {
    List<RecurringRule> findByGroupIdOrderByNextRunDateAsc(Long groupId);
    List<RecurringRule> findByActiveTrueAndNextRunDateLessThanEqualOrderByNextRunDateAsc(LocalDate date);
}
