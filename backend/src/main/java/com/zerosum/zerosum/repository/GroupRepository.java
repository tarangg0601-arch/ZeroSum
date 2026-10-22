package com.zerosum.zerosum.repository;

import com.zerosum.zerosum.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {
    java.util.Optional<Group> findByJoinCodeIgnoreCase(String joinCode);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select g from Group g where g.id = :groupId")
    java.util.Optional<Group> lockById(@org.springframework.data.repository.query.Param("groupId") Long groupId);
}
