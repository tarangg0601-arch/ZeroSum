package com.zerosum.zerosum.dto;

import java.time.Instant;
import java.util.List;

public record GroupResponse(

        Long id,

        String name,

        String groupType,

        String joinCode,

        Long createdBy,

        Instant createdAt,

        List<GroupMemberResponse> members

) {
}