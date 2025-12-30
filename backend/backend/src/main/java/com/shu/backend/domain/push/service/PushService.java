package com.shu.backend.domain.push.service;

import com.google.firebase.messaging.*;
import com.shu.backend.domain.pushtoken.repository.PushTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PushService {

    private final PushTokenRepository pushTokenRepository;

    public void sendToUser(Long userId, String title, String body, Map<String, String> data) {
        var tokens = pushTokenRepository.findByUserIdAndIsActiveTrue(userId);
        if (tokens.isEmpty()) return;

        List<String> tokenValues = tokens.stream().map(t -> t.getToken()).toList();

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokenValues)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .putAllData(data == null ? Collections.emptyMap() : data)
                .build();

        try {
            BatchResponse resp = FirebaseMessaging.getInstance().sendEachForMulticast(message);

            // 실패한 토큰 비활성화(권장)
            for (int i = 0; i < resp.getResponses().size(); i++) {
                SendResponse r = resp.getResponses().get(i);
                if (!r.isSuccessful()) {
                    String failedToken = tokenValues.get(i);
                    pushTokenRepository.deactivateByToken(failedToken);
                }
            }
        } catch (Exception e) {
            // 푸시 실패로 비즈니스 트랜잭션을 깨지 않게 하는 것이 보통 안전합니다.
            // 로깅만 하고 종료(또는 모니터링 전송)
        }
    }
}
