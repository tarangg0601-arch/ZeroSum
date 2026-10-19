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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    public GroupService(
            GroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository,
            UserRepository userRepository
    ) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
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
                new AddGroupMemberRequest(creator.getId(), null)
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
            UpdateGroupRequest request
    ) {

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Group not found")
                );

        group.setName(request.name());
        group.setGroupType(request.groupType());

        Group updatedGroup = groupRepository.save(group);

        return getGroup(updatedGroup.getId());
    }

    @Transactional
    public void deleteGroup(Long groupId) {

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Group not found")
                );

        groupRepository.delete(group);
    }

    @Transactional
    public GroupMemberResponse addMember(
            Long groupId,
            AddGroupMemberRequest request
    ) {

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Group not found")
                );

        if (request.userId() == null && (request.email() == null || request.email().isBlank())) {
            throw new IllegalArgumentException("Provide a registered user's ID or email");
        }
        User user = (request.email() == null || request.email().isBlank()
                ? userRepository.findById(request.userId())
                : userRepository.findByEmailIgnoreCase(request.email().trim()))
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
                        .findByGroupId(groupId)
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
                .findByGroupId(groupId)
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
                user.getEmail()
        );
    }

    private String generateJoinCode() {

        return UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }
}
