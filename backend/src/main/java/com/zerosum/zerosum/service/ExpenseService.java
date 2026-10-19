package com.zerosum.zerosum.service;

import com.zerosum.zerosum.dto.CreateExpenseRequest;
import com.zerosum.zerosum.dto.ExpenseResponse;
import com.zerosum.zerosum.dto.ExpenseShareResponse;
import com.zerosum.zerosum.dto.ExpenseSplitInput;
import com.zerosum.zerosum.dto.UpdateExpenseRequest;
import com.zerosum.zerosum.entity.Expense;
import com.zerosum.zerosum.entity.ExpenseShare;
import com.zerosum.zerosum.entity.Group;
import com.zerosum.zerosum.entity.GroupMember;
import com.zerosum.zerosum.entity.User;
import com.zerosum.zerosum.exception.ResourceNotFoundException;
import com.zerosum.zerosum.repository.ExpenseRepository;
import com.zerosum.zerosum.repository.ExpenseShareRepository;
import com.zerosum.zerosum.repository.GroupMemberRepository;
import com.zerosum.zerosum.repository.GroupRepository;
import com.zerosum.zerosum.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseShareRepository expenseShareRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    public ExpenseService(
            ExpenseRepository expenseRepository,
            ExpenseShareRepository expenseShareRepository,
            GroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository,
            UserRepository userRepository
    ) {
        this.expenseRepository = expenseRepository;
        this.expenseShareRepository = expenseShareRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ExpenseResponse createExpense(
            Long groupId,
            CreateExpenseRequest request
    ) {

        Group group = findGroup(groupId);
        User payer = findUser(request.paidBy());

        List<User> participants =
                validateParticipants(groupId, payer, request.participantIds());

        Expense expense = new Expense();
        expense.setGroup(group);
        expense.setPaidBy(payer);
        expense.setDescription(request.description());
        expense.setAmountPaise(request.amountPaise());
        expense.setCategory(request.category());
        expense.setExpenseDate(request.expenseDate());
        expense.setRecurring(false);

        Expense savedExpense = expenseRepository.save(expense);

        createShares(
                savedExpense,
                participants,
                request.amountPaise(),
                request.splitType(),
                request.splits()
        );

        return getExpenseResponse(savedExpense.getId());
    }

    @Transactional
    public ExpenseResponse updateExpense(
            Long groupId,
            Long expenseId,
            UpdateExpenseRequest request
    ) {

        Expense expense = findExpenseInGroup(groupId, expenseId);

        User payer = findUser(request.paidBy());

        List<User> participants =
                validateParticipants(
                        groupId,
                        payer,
                        request.participantIds()
                );

        expense.setDescription(request.description());
        expense.setAmountPaise(request.amountPaise());
        expense.setCategory(request.category());
        expense.setExpenseDate(request.expenseDate());
        expense.setPaidBy(payer);

        // Delete the old shares first.
        expenseShareRepository.deleteAll(
                expenseShareRepository.findByExpenseId(expenseId)
        );

        // Force Hibernate to execute the DELETE statements
        // before inserting the new shares.
        expenseShareRepository.flush();

        createShares(
                expense,
                participants,
                request.amountPaise(),
                request.splitType(),
                request.splits()
        );

        expenseRepository.save(expense);

        return getExpenseResponse(expenseId);
    }

    @Transactional
    public void deleteExpense(
            Long groupId,
            Long expenseId
    ) {

        Expense expense = findExpenseInGroup(groupId, expenseId);

        expenseShareRepository.deleteAll(
                expenseShareRepository.findByExpenseId(expenseId)
        );

        expenseRepository.delete(expense);
    }

    private Group findGroup(Long groupId) {

        return groupRepository.findById(groupId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Group not found")
                );
    }

    private User findUser(Long userId) {

        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );
    }

    private Expense findExpense(Long expenseId) {

        return expenseRepository.findById(expenseId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Expense not found")
                );
    }

    private Expense findExpenseInGroup(
            Long groupId,
            Long expenseId
    ) {

        Expense expense = findExpense(expenseId);

        if (!expense.getGroup().getId().equals(groupId)) {
            throw new ResourceNotFoundException("Expense not found");
        }

        return expense;
    }

    private List<User> validateParticipants(
            Long groupId,
            User payer,
            List<Long> participantIds
    ) {

        List<GroupMember> members =
                groupMemberRepository.findByGroupId(groupId);

        if (members.isEmpty()) {
            throw new IllegalArgumentException(
                    "Group has no members"
            );
        }

        boolean payerIsMember = members.stream()
                .anyMatch(member ->
                        member.getUser().getId().equals(payer.getId())
                );

        if (!payerIsMember) {
            throw new IllegalArgumentException(
                    "Payer must be a member of the group"
            );
        }

        List<User> participants = members.stream()
                .map(GroupMember::getUser)
                .filter(user ->
                        participantIds.contains(user.getId())
                )
                .toList();

        if (participants.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one participant is required"
            );
        }

        if (participants.size() != participantIds.size()) {
            throw new IllegalArgumentException(
                    "All participants must be members of the group"
            );
        }

        return participants;
    }

    private void createShares(
            Expense expense,
            List<User> participants,
            Long amountPaise,
            String requestedType,
            List<ExpenseSplitInput> inputs
    ) {
        String type = requestedType == null ? "equal" : requestedType.toLowerCase();
        if (!List.of("equal", "exact", "percentage", "shares").contains(type)) {
            throw new IllegalArgumentException("Split type must be equal, exact, percentage, or shares");
        }

        List<ExpenseSplitInput> values;
        if (type.equals("equal")) {
            long base = amountPaise / participants.size();
            long remainder = amountPaise % participants.size();
            values = participants.stream().map(user -> new ExpenseSplitInput(user.getId(), base)).toList();
            if (remainder > 0) {
                values = new java.util.ArrayList<>(values);
                for (int i = 0; i < remainder; i++) {
                    ExpenseSplitInput old = values.get(i);
                    values.set(i, new ExpenseSplitInput(old.userId(), old.value() + 1));
                }
            }
        } else {
            if (inputs == null || inputs.size() != participants.size()) {
                throw new IllegalArgumentException("Provide one split value for every participant");
            }
            Set<Long> selected = participants.stream().map(User::getId).collect(java.util.stream.Collectors.toSet());
            Set<Long> supplied = new HashSet<>();
            for (ExpenseSplitInput input : inputs) {
                if (input == null || input.userId() == null || input.value() == null || input.value() < 0
                        || !selected.contains(input.userId()) || !supplied.add(input.userId())) {
                    throw new IllegalArgumentException("Split values must be non-negative, unique, and match the participants");
                }
            }
            if (!supplied.equals(selected)) throw new IllegalArgumentException("Split values must match the participants");
            values = inputs;
            long total = values.stream().mapToLong(ExpenseSplitInput::value).reduce(0, Math::addExact);
            if (type.equals("exact") && total != amountPaise) {
                throw new IllegalArgumentException("Exact split amounts must add up to the expense amount");
            }
            if (type.equals("percentage") && total != 10000) {
                throw new IllegalArgumentException("Percentage split values must total 10000 basis points");
            }
            if (type.equals("shares") && total <= 0) {
                throw new IllegalArgumentException("At least one share unit is required");
            }
            if (!type.equals("exact")) values = allocate(amountPaise, values, total);
        }

        var amounts = values.stream().collect(java.util.stream.Collectors.toMap(ExpenseSplitInput::userId, ExpenseSplitInput::value));
        for (User participant : participants) {

            ExpenseShare share = new ExpenseShare();
            share.setExpense(expense);
            share.setUser(participant);
            share.setShareAmountPaise(amounts.get(participant.getId()));
            share.setShareType(type);
            share.setParticipated(true);

            expenseShareRepository.save(share);
        }
    }

    private List<ExpenseSplitInput> allocate(long amount, List<ExpenseSplitInput> weights, long totalWeight) {
        var allocated = new java.util.ArrayList<ExpenseSplitInput>();
        long remainder = amount;
        for (ExpenseSplitInput weight : weights) {
            long share = BigInteger.valueOf(amount).multiply(BigInteger.valueOf(weight.value()))
                    .divide(BigInteger.valueOf(totalWeight)).longValueExact();
            allocated.add(new ExpenseSplitInput(weight.userId(), share));
            remainder -= share;
        }
        for (int i = 0; i < remainder; i++) {
            ExpenseSplitInput item = allocated.get(i % allocated.size());
            allocated.set(i % allocated.size(), new ExpenseSplitInput(item.userId(), item.value() + 1));
        }
        return allocated;
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpense(
            Long groupId,
            Long expenseId
    ) {

        Expense expense = findExpenseInGroup(groupId, expenseId);

        return getExpenseResponse(expense.getId());
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesByGroup(Long groupId) {

        findGroup(groupId);

        return expenseRepository
                .findByGroupIdOrderByExpenseDateDescCreatedAtDesc(groupId)
                .stream()
                .map(expense ->
                        getExpenseResponse(expense.getId())
                )
                .toList();
    }

    private ExpenseResponse getExpenseResponse(Long expenseId) {

        Expense expense = findExpense(expenseId);

        List<ExpenseShareResponse> shares =
                expenseShareRepository
                        .findByExpenseId(expenseId)
                        .stream()
                        .map(this::toShareResponse)
                        .toList();

        return new ExpenseResponse(
                expense.getId(),
                expense.getGroup().getId(),
                expense.getPaidBy().getId(),
                expense.getPaidBy().getName(),
                expense.getDescription(),
                expense.getAmountPaise(),
                expense.getCategory(),
                expense.getExpenseDate(),
                expense.isRecurring(),
                expense.getCreatedAt(),
                shares
        );
    }

    private ExpenseShareResponse toShareResponse(
            ExpenseShare share
    ) {

        return new ExpenseShareResponse(
                share.getUser().getId(),
                share.getUser().getName(),
                share.getShareAmountPaise(),
                share.getShareType(),
                share.isParticipated()
        );
    }
}
