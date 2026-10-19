package com.zerosum.zerosum.service;

import com.zerosum.zerosum.dto.CreateUserRequest;
import com.zerosum.zerosum.dto.GroupMemberResponse;
import com.zerosum.zerosum.entity.User;
import com.zerosum.zerosum.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    public UserService(UserRepository userRepository) { this.userRepository = userRepository; }

    @Transactional
    public GroupMemberResponse create(CreateUserRequest request) {
        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(request.email() == null || request.email().isBlank() ? null : request.email().trim());
        User saved = userRepository.save(user);
        return new GroupMemberResponse(saved.getId(), saved.getName(), saved.getEmail());
    }
}
