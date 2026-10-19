package com.zerosum.zerosum.repository;

import com.zerosum.zerosum.entity.Iou;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IouRepository extends JpaRepository<Iou, Long> {
    List<Iou> findByGroupIdOrderByCreatedAtDesc(Long groupId);
}
