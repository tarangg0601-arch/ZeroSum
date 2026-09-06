package com.zerosum.zerosum.repository;

import com.zerosum.zerosum.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {
}