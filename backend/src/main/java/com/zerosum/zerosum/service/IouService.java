package com.zerosum.zerosum.service;

import com.zerosum.zerosum.dto.CreateIouRequest;
import com.zerosum.zerosum.dto.IouResponse;
import com.zerosum.zerosum.entity.Iou;
import com.zerosum.zerosum.exception.ResourceNotFoundException;
import com.zerosum.zerosum.repository.GroupMemberRepository;
import com.zerosum.zerosum.repository.GroupRepository;
import com.zerosum.zerosum.repository.IouRepository;
import com.zerosum.zerosum.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class IouService {
    private final IouRepository iouRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;

    public IouService(IouRepository iouRepository, UserRepository userRepository, GroupRepository groupRepository,
            GroupMemberRepository memberRepository) {
        this.iouRepository = iouRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public IouResponse create(CreateIouRequest request, Long actorId) {
        if (request.fromUserId().equals(request.toUserId())) throw new IllegalArgumentException("An IOU must be between two different people");
        if (!actorId.equals(request.fromUserId()) && !actorId.equals(request.toUserId())) throw new IllegalArgumentException("You must be one of the people in this IOU");
        var from = userRepository.findById(request.fromUserId()).orElseThrow(() -> new ResourceNotFoundException("Lender not found"));
        var to = userRepository.findById(request.toUserId()).orElseThrow(() -> new ResourceNotFoundException("Borrower not found"));
        var iou = new Iou();
        iou.setFromUser(from);
        iou.setToUser(to);
        iou.setAmountPaise(request.amountPaise());
        iou.setNote(request.note());
        if (request.groupId() != null) {
            var group = groupRepository.findById(request.groupId()).orElseThrow(() -> new ResourceNotFoundException("Group not found"));
            var memberIds = memberRepository.findByGroupId(request.groupId()).stream().map(m -> m.getUser().getId()).toList();
            if (!memberIds.contains(from.getId()) || !memberIds.contains(to.getId())) throw new IllegalArgumentException("Both people must be group members");
            iou.setGroup(group);
        }
        return toResponse(iouRepository.save(iou));
    }

    @Transactional(readOnly = true)
    public List<IouResponse> listForGroup(Long groupId) {
        if (!groupRepository.existsById(groupId)) throw new ResourceNotFoundException("Group not found");
        return iouRepository.findByGroupIdOrderByCreatedAtDesc(groupId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public IouResponse markSettled(Long iouId, Long actorId) {
        Iou iou = iouRepository.findById(iouId).orElseThrow(() -> new ResourceNotFoundException("IOU not found"));
        if (!actorId.equals(iou.getFromUser().getId()) && !actorId.equals(iou.getToUser().getId())) throw new ResourceNotFoundException("IOU not found");
        iou.setSettled(true);
        return toResponse(iou);
    }

    private IouResponse toResponse(Iou iou) {
        return new IouResponse(iou.getId(), iou.getGroup() == null ? null : iou.getGroup().getId(),
                iou.getFromUser().getId(), iou.getFromUser().getName(), iou.getToUser().getId(),
                iou.getToUser().getName(), iou.getAmountPaise(), iou.getNote(), iou.isSettled(), iou.getCreatedAt());
    }
}
