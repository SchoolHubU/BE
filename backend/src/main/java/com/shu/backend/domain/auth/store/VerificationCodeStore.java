package com.shu.backend.domain.auth.store;


import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 휴대폰 인증번호를 Redis에 임시 저장/조회/삭제하는 저장소.
 *
 * 문자로 발송한 인증번호를 짧은 시간 동안 보관하고,
 * 사용자가 입력한 인증번호와 비교할 때 사용한다.
 */
@Component
public class VerificationCodeStore {

    private static final long CODE_TTL_MINUTES = 5;
    public static final String PURPOSE_SIGNUP = "SIGNUP";
    public static final String PURPOSE_PASSWORD_RESET = "PASSWORD_RESET";

    private final StringRedisTemplate redisTemplate;

    public VerificationCodeStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String key(String target) {
        return "sms:code:" + target;
    }

    private String attemptKey(String target) {
        return "sms:attempts:" + target;
    }

    private String purposeKey(String target) {
        return "sms:purpose:" + target;
    }

    // 이메일 인증번호 저장
    public void save(String target, String code) {
        save(target, code, PURPOSE_SIGNUP);
    }

    public void savePasswordReset(String target, String code) {
        save(target, code, PURPOSE_PASSWORD_RESET);
    }

    private void save(String target, String code, String purpose) {
        redisTemplate.opsForValue().set(key(target), code, CODE_TTL_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(purposeKey(target), purpose, CODE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    // 저장된 인증번호 조회
    public String get(String target) {
        return redisTemplate.opsForValue().get(key(target));
    }

    public String getPurpose(String target) {
        return redisTemplate.opsForValue().get(purposeKey(target));
    }

    // 인증 완료 또는 만료 처리 시 인증번호 삭제
    public void delete(String target) {
        redisTemplate.delete(key(target));
        redisTemplate.delete(purposeKey(target));
    }

    // 실패 횟수 1 증가 후 현재 값 반환 (처음 카운트 시 TTL을 코드와 동일하게 설정)
    public int incrementAttempts(String target) {
        Long count = redisTemplate.opsForValue().increment(attemptKey(target));
        if (count != null && count == 1) {
            redisTemplate.expire(attemptKey(target), CODE_TTL_MINUTES, TimeUnit.MINUTES);
        }
        return count != null ? count.intValue() : 1;
    }

    // 현재 실패 횟수 조회
    public int getAttemptCount(String target) {
        String val = redisTemplate.opsForValue().get(attemptKey(target));
        return val == null ? 0 : Integer.parseInt(val);
    }

    // 시도 횟수 초기화 (인증 성공 또는 코드 재발급 시)
    public void deleteAttempts(String target) {
        redisTemplate.delete(attemptKey(target));
    }
}
