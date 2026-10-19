package com.zerosum.zerosum.controller;

import com.zerosum.zerosum.dto.CreateIouRequest;
import com.zerosum.zerosum.dto.IouResponse;
import com.zerosum.zerosum.service.IouService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class IouController {
    private final IouService iouService;
    public IouController(IouService iouService) { this.iouService = iouService; }

    @PostMapping("/ious")
    @ResponseStatus(HttpStatus.CREATED)
    public IouResponse create(@Valid @RequestBody CreateIouRequest request) { return iouService.create(request); }

    @GetMapping("/groups/{groupId}/ious")
    public List<IouResponse> list(@PathVariable Long groupId) { return iouService.listForGroup(groupId); }

    @PatchMapping("/ious/{iouId}")
    public IouResponse settle(@PathVariable Long iouId, @RequestBody Map<String, Boolean> request) {
        if (!Boolean.TRUE.equals(request.get("settled"))) throw new IllegalArgumentException("Only marking an IOU settled is supported");
        return iouService.markSettled(iouId);
    }
}
