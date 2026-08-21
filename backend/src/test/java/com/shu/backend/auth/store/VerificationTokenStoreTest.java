package com.shu.backend.auth.store;

import com.shu.backend.domain.auth.store.VerificationTokenStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VerificationTokenStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("기본 인증 완료 토큰은 5분 동안 유효하다")
    void save_storesDefaultVerifiedTokenForFiveMinutes() {
        VerificationTokenStore store = new VerificationTokenStore(redisTemplate);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        store.save("verif_token", "user@example.com");

        verify(valueOperations).set("sms:verify:verif_token", "user@example.com", 5, TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("회원가입용 인증 완료 토큰은 1시간 동안 유효하다")
    void saveForSignup_storesVerifiedTokenForOneHour() {
        VerificationTokenStore store = new VerificationTokenStore(redisTemplate);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        store.saveForSignup("verif_token", "user@example.com");

        verify(valueOperations).set("sms:verify:verif_token", "user@example.com", 1, TimeUnit.HOURS);
    }
}
