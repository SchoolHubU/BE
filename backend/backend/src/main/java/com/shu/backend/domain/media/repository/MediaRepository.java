package com.shu.backend.domain.media.repository;

import com.shu.backend.domain.media.entity.Media;
import com.shu.backend.domain.media.enums.MediaTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findByTargetTypeAndTargetIdIn(MediaTargetType targetType, List<Long> targetIds);
}
