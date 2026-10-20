package com.zerosum.zerosum.controller;

import com.zerosum.zerosum.dto.AddGroupMemberRequest;
import com.zerosum.zerosum.dto.CreateGroupRequest;
import com.zerosum.zerosum.dto.GroupMemberResponse;
import com.zerosum.zerosum.dto.GroupResponse;
import com.zerosum.zerosum.dto.UpdateGroupRequest;
import com.zerosum.zerosum.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    // Create group
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse createGroup(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateGroupRequest request
    ) {
        return groupService.createGroup(request, Long.valueOf(jwt.getSubject()));
    }

    // Get all groups
    @GetMapping
    public List<GroupResponse> getAllGroups(@AuthenticationPrincipal Jwt jwt) {
        return groupService.getGroupsForUser(Long.valueOf(jwt.getSubject()));
    }

    // Get one group
    @GetMapping("/{groupId}")
    public GroupResponse getGroup(
            @PathVariable Long groupId
    ) {
        return groupService.getGroup(groupId);
    }

    // Update group
    @PutMapping("/{groupId}")
    public GroupResponse updateGroup(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long groupId,
            @Valid @RequestBody UpdateGroupRequest request
    ) {
        return groupService.updateGroup(groupId, request, Long.valueOf(jwt.getSubject()));
    }

    // Delete group
    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long groupId
    ) {
        groupService.deleteGroup(groupId, Long.valueOf(jwt.getSubject()));
    }

    // Add member
    @PostMapping("/{groupId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupMemberResponse addMember(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long groupId,
            @Valid @RequestBody AddGroupMemberRequest request
    ) {
        return groupService.addMember(groupId, request, Long.valueOf(jwt.getSubject()));
    }

    // Get group members
    @GetMapping("/{groupId}/members")
    public List<GroupMemberResponse> getMembers(
            @PathVariable Long groupId
    ) {
        return groupService.getMembers(groupId);
    }
}
