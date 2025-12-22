package com.shu.backend.domain.reaction.repository;

import com.shu.backend.domain.reaction.entity.Reaction;
import com.shu.backend.domain.reaction.enums.ReactionTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    Optional<Reaction> findByUserIdAndTargetTypeAndTargetId(Long userId, ReactionTargetType targetType, Long targetId);
}
