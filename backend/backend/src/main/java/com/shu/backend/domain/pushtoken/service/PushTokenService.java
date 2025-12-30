package com.shu.backend.domain.pushtoken.service;

import com.shu.backend.domain.pushtoken.dto.PushTokenDTO;
import com.shu.backend.domain.pushtoken.entity.PushToken;
import com.shu.backend.domain.pushtoken.exception.PushTokenException;
import com.shu.backend.domain.pushtoken.exception.status.PushTokenErrorStatus;
import com.shu.backend.domain.pushtoken.repository.PushTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PushTokenService {

    private final PushTokenRepository pushTokenRepository;

    @Transactional
    public PushTokenDTO.RegisterResponse registerOrUpdate(Long userId, PushTokenDTO.RegisterRequest req) {

        String token = (req.getToken() == null) ? null : req.getToken().trim();
        if (token == null || token.isEmpty()) {
            throw new PushTokenException(PushTokenErrorStatus.INVALID_TOKEN);
        }

        PushToken pt = pushTokenRepository.findByToken(token)
                .orElseGet(() -> pushTokenRepository.save(PushToken.create(userId, token, req.getPlatform())));

        // 이미 존재하는 토큰이면 소유자/플랫폼/활성 상태 갱신
        pt.activate(userId, req.getPlatform());

        return new PushTokenDTO.RegisterResponse(true);
    }

    @Transactional
    public int deactivateAll(Long userId) {
        return pushTokenRepository.deactivateAllByUserId(userId);
    }

    @Transactional
    public int deactivateByToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new PushTokenException(PushTokenErrorStatus.INVALID_TOKEN);
        }
        return pushTokenRepository.deactivateByToken(token.trim());
    }
}
