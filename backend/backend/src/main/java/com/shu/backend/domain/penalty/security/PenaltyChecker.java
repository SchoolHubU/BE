package com.shu.backend.domain.penalty.security;

import com.shu.backend.domain.penalty.repository.PenaltyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component("penaltyChecker")
@RequiredArgsConstructor
public class PenaltyChecker {

    private final PenaltyRepository penaltyRepository;

    // 제재 중이면 false 반환 (즉, 접근 불가)
    public boolean notPenalized(Long userId) {
        if (userId == null) return false; // 방어
        return !penaltyRepository.existsByUserIdAndExpiresAtAfter(userId, LocalDateTime.now());
    }
}
