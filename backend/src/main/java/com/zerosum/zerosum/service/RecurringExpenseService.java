package com.zerosum.zerosum.service;

import com.zerosum.zerosum.dto.CreateRecurringRuleRequest;
import com.zerosum.zerosum.dto.RecurringRuleResponse;
import com.zerosum.zerosum.entity.Expense;
import com.zerosum.zerosum.entity.ExpenseShare;
import com.zerosum.zerosum.entity.RecurringRule;
import com.zerosum.zerosum.exception.ResourceNotFoundException;
import com.zerosum.zerosum.repository.ExpenseRepository;
import com.zerosum.zerosum.repository.ExpenseShareRepository;
import com.zerosum.zerosum.repository.GroupMemberRepository;
import com.zerosum.zerosum.repository.GroupRepository;
import com.zerosum.zerosum.repository.RecurringRuleRepository;
import com.zerosum.zerosum.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class RecurringExpenseService {
    private static final List<String> FREQUENCIES = List.of("DAILY", "WEEKLY", "MONTHLY", "YEARLY");

    private final RecurringRuleRepository ruleRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseShareRepository shareRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Autowired
    public RecurringExpenseService(RecurringRuleRepository ruleRepository, ExpenseRepository expenseRepository,
            ExpenseShareRepository shareRepository, GroupRepository groupRepository,
            GroupMemberRepository memberRepository, UserRepository userRepository) {
        this(ruleRepository, expenseRepository, shareRepository, groupRepository, memberRepository, userRepository,
                Clock.systemDefaultZone());
    }

    RecurringExpenseService(RecurringRuleRepository ruleRepository, ExpenseRepository expenseRepository,
            ExpenseShareRepository shareRepository, GroupRepository groupRepository,
            GroupMemberRepository memberRepository, UserRepository userRepository, Clock clock) {
        this.ruleRepository = ruleRepository;
        this.expenseRepository = expenseRepository;
        this.shareRepository = shareRepository;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public RecurringRuleResponse createRule(Long groupId, CreateRecurringRuleRequest request) {
        var group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        var payer = userRepository.findById(request.paidBy())
                .orElseThrow(() -> new ResourceNotFoundException("Payer not found"));
        boolean payerIsMember = memberRepository.findByGroupId(groupId).stream()
                .anyMatch(member -> member.getUser().getId().equals(payer.getId()));
        if (!payerIsMember) throw new IllegalArgumentException("Payer must be a member of the group");
        String frequency = request.frequency().toUpperCase();
        if (!FREQUENCIES.contains(frequency)) throw new IllegalArgumentException("Frequency must be daily, weekly, monthly, or yearly");
        if (request.nextRunDate().isBefore(LocalDate.now(clock))) throw new IllegalArgumentException("Next run date must be today or later");
        String anchorType = request.anchorType() == null ? "day" : request.anchorType().toLowerCase();
        if (!List.of("day", "month_end").contains(anchorType)) throw new IllegalArgumentException("Anchor type must be day or month_end");

        RecurringRule rule = new RecurringRule();
        rule.setGroup(group);
        rule.setPaidBy(payer);
        rule.setDescription(request.description().trim());
        rule.setAmountPaise(request.amountPaise());
        rule.setCategory(request.category());
        rule.setFrequency(frequency);
        rule.setNextRunDate(request.nextRunDate());
        rule.setAnchorDay(request.nextRunDate().getDayOfMonth());
        rule.setAnchorMonth(request.nextRunDate().getMonthValue());
        rule.setAnchorMonthEnd(anchorType.equals("month_end"));
        rule.setActive(true);
        return toResponse(ruleRepository.save(rule));
    }

    @Transactional(readOnly = true)
    public List<RecurringRuleResponse> getRules(Long groupId) {
        if (!groupRepository.existsById(groupId)) throw new ResourceNotFoundException("Group not found");
        return ruleRepository.findByGroupIdOrderByNextRunDateAsc(groupId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public RecurringRuleResponse setActive(Long groupId, Long ruleId, boolean active) {
        RecurringRule rule = ruleRepository.lockByGroupAndId(groupId, ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring rule not found"));
        rule.setActive(active);
        return toResponse(rule);
    }

    @Transactional
    public int generateDueExpenses() {
        LocalDate today = LocalDate.now(clock);
        int generated = 0;
        List<RecurringRule> dueRules = ruleRepository.lockDueRules(today);
        for (RecurringRule rule : dueRules) {
            while (!rule.getNextRunDate().isAfter(today)) {
                LocalDate dueDate = rule.getNextRunDate();
                if (!expenseRepository.existsByRecurringRuleIdAndExpenseDate(rule.getId(), dueDate)) {
                    createExpense(rule, dueDate);
                    generated++;
                }
                rule.setNextRunDate(advance(dueDate, rule.getFrequency(), rule.getAnchorDay(),
                        rule.getAnchorMonth(), rule.isAnchorMonthEnd()));
            }
        }
        ruleRepository.saveAll(dueRules);
        return generated;
    }

    private void createExpense(RecurringRule rule, LocalDate date) {
        var members = memberRepository.findByGroupIdOrderByUserIdAsc(rule.getGroup().getId());
        if (members.isEmpty()) throw new IllegalStateException("Cannot generate an expense for a group without members");
        Expense expense = new Expense();
        expense.setGroup(rule.getGroup());
        expense.setPaidBy(rule.getPaidBy());
        expense.setDescription(rule.getDescription());
        expense.setAmountPaise(rule.getAmountPaise());
        expense.setCategory(rule.getCategory());
        expense.setExpenseDate(date);
        expense.setRecurring(true);
        expense.setRecurringRule(rule);
        Expense saved = expenseRepository.save(expense);

        long base = rule.getAmountPaise() / members.size();
        long remainder = rule.getAmountPaise() % members.size();
        for (int index = 0; index < members.size(); index++) {
            ExpenseShare share = new ExpenseShare();
            share.setExpense(saved);
            share.setUser(members.get(index).getUser());
            share.setShareAmountPaise(base + (index < remainder ? 1 : 0));
            share.setShareType("equal");
            share.setParticipated(true);
            shareRepository.save(share);
        }
    }

    static LocalDate advance(LocalDate date, String frequency, int anchorDay, int anchorMonth, boolean anchorMonthEnd) {
        return switch (frequency.toUpperCase()) {
            case "DAILY" -> date.plusDays(1);
            case "WEEKLY" -> date.plusWeeks(1);
            case "MONTHLY" -> {
                LocalDate nextMonth = date.plusMonths(1);
                yield nextMonth.withDayOfMonth(anchorMonthEnd ? nextMonth.lengthOfMonth()
                        : Math.min(anchorDay, nextMonth.lengthOfMonth()));
            }
            case "YEARLY" -> {
                LocalDate nextYear = date.plusYears(1).withMonth(anchorMonth);
                yield nextYear.withDayOfMonth(anchorMonthEnd ? nextYear.lengthOfMonth()
                        : Math.min(anchorDay, nextYear.lengthOfMonth()));
            }
            default -> throw new IllegalArgumentException("Unsupported recurring frequency");
        };
    }

    private RecurringRuleResponse toResponse(RecurringRule rule) {
        return new RecurringRuleResponse(rule.getId(), rule.getGroup().getId(), rule.getPaidBy().getId(),
                rule.getDescription(), rule.getAmountPaise(), rule.getCategory(), rule.getFrequency(),
                rule.getNextRunDate(), rule.isAnchorMonthEnd() ? "month_end" : "day", rule.isActive());
    }
}
