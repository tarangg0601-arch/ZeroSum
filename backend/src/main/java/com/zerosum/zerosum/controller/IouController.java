package com.zerosum.zerosum.controller;

import com.zerosum.zerosum.dto.CreateIouRequest;
import com.zerosum.zerosum.dto.IouResponse;
import com.zerosum.zerosum.service.IouService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api")
public class IouController {
    private final IouService iouService;
    public IouController(IouService iouService) { this.iouService = iouService; }

    @PostMapping("/ious")
    @ResponseStatus(HttpStatus.CREATED)
    public IouResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateIouRequest request) { return iouService.create(request, Long.valueOf(jwt.getSubject())); }

    @GetMapping("/groups/{groupId}/ious")
    public List<IouResponse> list(@PathVariable Long groupId) { return iouService.listForGroup(groupId); }

    @PatchMapping("/ious/{iouId}")
    public IouResponse settle(@AuthenticationPrincipal Jwt jwt, @PathVariable Long iouId, @RequestBody Map<String, Boolean> request) {
        if (!Boolean.TRUE.equals(request.get("settled"))) throw new IllegalArgumentException("Only marking an IOU settled is supported");
        return iouService.markSettled(iouId, Long.valueOf(jwt.getSubject()));
    }
}
