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

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse createGroup(
            @Valid @RequestBody CreateGroupRequest request
    ) {
        return groupService.createGroup(request);
    }

    @GetMapping("/{groupId}")
    public GroupResponse getGroup(
            @PathVariable Long groupId
    ) {
        return groupService.getGroup(groupId);
    }

    @PutMapping("/{groupId}")
    public GroupResponse updateGroup(
            @PathVariable Long groupId,
            @Valid @RequestBody UpdateGroupRequest request
    ) {
        return groupService.updateGroup(groupId, request);
    }

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(
            @PathVariable Long groupId
    ) {
        groupService.deleteGroup(groupId);
    }

    @PostMapping("/{groupId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupMemberResponse addMember(
            @PathVariable Long groupId,
            @Valid @RequestBody AddGroupMemberRequest request
    ) {
        return groupService.addMember(groupId, request);
    }

    @GetMapping("/{groupId}/members")
    public List<GroupMemberResponse> getMembers(
            @PathVariable Long groupId
    ) {
        return groupService.getMembers(groupId);
    }
}