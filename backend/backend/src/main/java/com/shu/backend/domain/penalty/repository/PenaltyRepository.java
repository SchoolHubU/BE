package com.shu.backend.domain.penalty.repository;

import com.shu.backend.domain.penalty.entity.Penalty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PenaltyRepository extends JpaRepository<Penalty, Long> {
    boolean existsByReportId(Long reportId);

    Optional<Penalty> findTop1ByUserIdAndExpiresAtAfterOrderByExpiresAtDesc(Long userId, LocalDateTime now);

    Page<Penalty> findAllByUserId(Long userId, Pageable pageable);

    boolean existsByUserIdAndExpiresAtAfter(Long userId, LocalDateTime now);
}
