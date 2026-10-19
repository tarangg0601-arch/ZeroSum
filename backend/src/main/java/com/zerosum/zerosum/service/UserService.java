package com.zerosum.zerosum.service;

import com.zerosum.zerosum.dto.GroupMemberResponse;
import com.zerosum.zerosum.entity.User;
import com.zerosum.zerosum.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.zerosum.zerosum.exception.ResourceNotFoundException;

@Service
public class UserService {
    private final UserRepository userRepository;
    public UserService(UserRepository userRepository) { this.userRepository = userRepository; }

    @Transactional(readOnly = true)
    public GroupMemberResponse get(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new GroupMemberResponse(user.getId(), user.getName(), user.getEmail());
    }
}
