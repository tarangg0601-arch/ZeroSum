package com.zerosum.zerosum.repository;

import com.zerosum.zerosum.entity.RecurringRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface RecurringRuleRepository extends JpaRepository<RecurringRule, Long> {
    List<RecurringRule> findByGroupIdOrderByNextRunDateAsc(Long groupId);
    List<RecurringRule> findByActiveTrueAndNextRunDateLessThanEqualOrderByNextRunDateAsc(LocalDate date);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RecurringRule r where r.id = :ruleId and r.group.id = :groupId")
    java.util.Optional<RecurringRule> lockByGroupAndId(@Param("groupId") Long groupId, @Param("ruleId") Long ruleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RecurringRule r where r.active = true and r.nextRunDate <= :date order by r.nextRunDate asc")
    List<RecurringRule> lockDueRules(@Param("date") LocalDate date);
}
