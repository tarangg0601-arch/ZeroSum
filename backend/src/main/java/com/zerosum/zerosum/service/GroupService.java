package com.zerosum.zerosum.service;

import com.zerosum.zerosum.dto.AddGroupMemberRequest;
import com.zerosum.zerosum.dto.CreateGroupRequest;
import com.zerosum.zerosum.dto.GroupMemberResponse;
import com.zerosum.zerosum.dto.GroupResponse;
import com.zerosum.zerosum.dto.UpdateGroupRequest;
import com.zerosum.zerosum.entity.Group;
import com.zerosum.zerosum.entity.GroupMember;
import com.zerosum.zerosum.entity.User;
import com.zerosum.zerosum.exception.ResourceNotFoundException;
import com.zerosum.zerosum.repository.GroupMemberRepository;
import com.zerosum.zerosum.repository.GroupRepository;
import com.zerosum.zerosum.repository.UserRepository;
import com.zerosum.zerosum.repository.IouRepository;
import com.zerosum.zerosum.repository.SettlementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final GroupJoinRateLimiter joinRateLimiter;
    private final IouRepository iouRepository;
    private final SettlementRepository settlementRepository;
    private static final SecureRandom RANDOM = new SecureRandom();

    public GroupService(
            GroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository,
            UserRepository userRepository, GroupJoinRateLimiter joinRateLimiter,
            IouRepository iouRepository, SettlementRepository settlementRepository
    ) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.joinRateLimiter = joinRateLimiter;
        this.iouRepository = iouRepository;
        this.settlementRepository = settlementRepository;
    }

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request, Long currentUserId) {

        User creator = userRepository.findById(currentUserId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        Group group = new Group();

        group.setName(request.name());
        group.setGroupType(request.groupType());
        group.setJoinCode(generateJoinCode());
        group.setCreatedBy(creator);

        Group savedGroup = groupRepository.save(group);

        addMember(
                savedGroup.getId(),
                new AddGroupMemberRequest(creator.getId(), null), creator.getId()
        );

        return getGroup(savedGroup.getId());
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getGroupsForUser(Long userId) {
        return groupMemberRepository.findByUserId(userId).stream()
                .map(GroupMember::getGroup)
                .map(group -> getGroup(group.getId()))
                .distinct()
                .toList();
    }

    @Transactional
    public GroupResponse updateGroup(
            Long groupId,
            UpdateGroupRequest request,
            Long actorId
    ) {

        Group group = groupRepository.lockById(groupId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Group not found")
                );

        requireOwner(group, actorId);

        group.setName(request.name());
        group.setGroupType(request.groupType());

        Group updatedGroup = groupRepository.save(group);

        return getGroup(updatedGroup.getId());
    }

    @Transactional
    public void deleteGroup(Long groupId, Long actorId) {

        Group group = groupRepository.lockById(groupId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Group not found")
                );

        requireOwner(group, actorId);

        if (iouRepository.existsByGroupId(groupId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This group has linked IOUs. Settle or unlink them before deleting the group.");
        }
        if (settlementRepository.existsByGroupIdAndStatus(groupId, "completed")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This group has payment history. Keep the group to preserve its financial record.");
        }

        groupRepository.delete(group);
    }

    @Transactional
    public GroupResponse joinByCode(String submittedCode, Long currentUserId, String clientAddress) {
        if (!joinRateLimiter.check(clientAddress)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many join attempts. Try again later.");
        }
        String joinCode = submittedCode == null ? "" : submittedCode.trim().toUpperCase(java.util.Locale.ROOT);
        Group group = groupRepository.findByJoinCodeIgnoreCase(joinCode)
                .orElseThrow(() -> new ResourceNotFoundException("Join code is invalid"));
        group = groupRepository.lockById(group.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Join code is invalid"));
        if (groupMemberRepository.existsByGroupIdAndUserId(group.getId(), currentUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already a member of this group");
        }
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(currentUser);
        groupMemberRepository.save(member);
        return getGroup(group.getId());
    }

    @Transactional
    public GroupMemberResponse addMember(
            Long groupId,
            AddGroupMemberRequest request,
            Long actorId
    ) {

        Group group = groupRepository.lockById(groupId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Group not found")
                );

        requireOwner(group, actorId);

        if (request.userId() == null && (request.username() == null || request.username().isBlank())) {
            throw new IllegalArgumentException("Provide a registered user's username");
        }
        User user = (request.username() != null && !request.username().isBlank()
                ? userRepository.findByUsernameIgnoreCase(request.username().trim())
                : userRepository.findById(request.userId()))
                .orElseThrow(() -> new ResourceNotFoundException("Registered user not found"));

        boolean alreadyMember = groupMemberRepository
                .findByGroupId(groupId)
                .stream()
                .anyMatch(member ->
                        member.getUser().getId().equals(user.getId())
                );

        if (alreadyMember) {
            throw new IllegalArgumentException(
                    "User is already a member of this group"
            );
        }

        GroupMember groupMember = new GroupMember();

        groupMember.setGroup(group);
        groupMember.setUser(user);

        GroupMember savedMember =
                groupMemberRepository.save(groupMember);

        return toMemberResponse(savedMember);
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroup(Long groupId) {

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Group not found")
                );

        List<GroupMemberResponse> members =
                groupMemberRepository
                        .findByGroupIdOrderByUserIdAsc(groupId)
                        .stream()
                        .map(this::toMemberResponse)
                        .toList();

        return new GroupResponse(
                group.getId(),
                group.getName(),
                group.getGroupType(),
                group.getJoinCode(),
                group.getCreatedBy().getId(),
                group.getCreatedAt(),
                members
        );
    }

    @Transactional(readOnly = true)
    public List<GroupMemberResponse> getMembers(Long groupId) {

        if (!groupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Group not found");
        }

        return groupMemberRepository
                .findByGroupIdOrderByUserIdAsc(groupId)
                .stream()
                .map(this::toMemberResponse)
                .toList();
    }

    private GroupMemberResponse toMemberResponse(
            GroupMember member
    ) {

        User user = member.getUser();

        return new GroupMemberResponse(
                user.getId(),
                user.getName(),
                user.getUsername()
        );
    }

    private String generateJoinCode() {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).toUpperCase(java.util.Locale.ROOT);
    }

    private void requireOwner(Group group, Long actorId) {
        if (!group.getCreatedBy().getId().equals(actorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group creator can manage members or group settings");
        }
    }
}
