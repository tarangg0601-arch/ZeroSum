package com.zerosum.zerosum.repository;

import com.zerosum.zerosum.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}