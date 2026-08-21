package com.shu.backend.auth.store;

import com.shu.backend.domain.auth.store.VerificationCodeStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VerificationCodeStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("이메일 인증번호는 5분 동안 유효하다")
    void save_storesCodeForFiveMinutes() {
        VerificationCodeStore store = new VerificationCodeStore(redisTemplate);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        store.save("user@example.com", "123456");

        verify(valueOperations).set("sms:code:user@example.com", "123456", 5, TimeUnit.MINUTES);
        verify(valueOperations).set("sms:purpose:user@example.com", "SIGNUP", 5, TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("비밀번호 재설정 인증번호는 목적을 비밀번호 재설정으로 저장한다")
    void savePasswordReset_storesPurposeForPasswordReset() {
        VerificationCodeStore store = new VerificationCodeStore(redisTemplate);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        store.savePasswordReset("user@example.com", "123456");

        verify(valueOperations).set("sms:code:user@example.com", "123456", 5, TimeUnit.MINUTES);
        verify(valueOperations).set("sms:purpose:user@example.com", "PASSWORD_RESET", 5, TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("이메일 인증 시도 횟수 TTL도 인증번호와 같은 5분이다")
    void incrementAttempts_setsAttemptTtlForFiveMinutes() {
        VerificationCodeStore store = new VerificationCodeStore(redisTemplate);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        willReturn(1L).given(valueOperations).increment("sms:attempts:user@example.com");

        store.incrementAttempts("user@example.com");

        verify(redisTemplate).expire("sms:attempts:user@example.com", 5, TimeUnit.MINUTES);
    }
}
