package com.shu.backend.domain.auth.service;


import com.shu.backend.domain.auth.provider.VerificationMessageProvider;
import com.shu.backend.domain.auth.store.VerificationCodeStore;
import com.shu.backend.domain.auth.store.VerificationTokenStore;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final VerificationMessageProvider provider;
    private final VerificationCodeStore codeStore;
    private final VerificationTokenStore tokenStore;


    /**
     * 이메일로 코드를 발생하는 함수
     */
    public String sendCode(String target) {
        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999));
        codeStore.save(target, code);
        codeStore.deleteAttempts(target);  // 재발송 시 이전 실패 횟수 초기화
        provider.send(target, code);
        return code;
    }

    /**
     * 비밀번호 재설정용 인증번호 발송
     */
    public String sendPasswordResetCode(String target) {
        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999));
        codeStore.savePasswordReset(target, code);
        codeStore.deleteAttempts(target);  // 재발송 시 이전 실패 횟수 초기화
        provider.sendPasswordReset(target, code);
        return code;
    }

    /**
     * Redis에 저장된 인증코드와 일치하는지 확인.
     * 5회 연속 실패 시 코드를 무효화하여 브루트포스를 방지한다.
     */
    public String verifyCode(String target, String code) {
        String saved = codeStore.get(target);

        if (saved == null) {
            throw new UserException(UserErrorStatus.VERIFICATION_CODE_INVALID);
        }

        if (codeStore.getAttemptCount(target) >= 5) {
            codeStore.delete(target);
            codeStore.deleteAttempts(target);
            throw new UserException(UserErrorStatus.VERIFICATION_CODE_EXCEEDED);
        }

        if (!saved.equals(code)) {
            codeStore.incrementAttempts(target);
            throw new UserException(UserErrorStatus.VERIFICATION_CODE_INVALID);
        }

        String purpose = codeStore.getPurpose(target);
        codeStore.delete(target);
        codeStore.deleteAttempts(target);

        String token = "verif_" + UUID.randomUUID();
        if (VerificationCodeStore.PURPOSE_SIGNUP.equals(purpose)) {
            tokenStore.saveForSignup(token, target);
        } else {
            tokenStore.save(token, target);
        }

        return token;
    }

    /**
     * 토큰을 소비하고 연결된 이메일 반환 (비밀번호 재설정용)
     */
    public String consumeToken(String token) {
        String email = tokenStore.get(token);
        if (email == null) {
            throw new UserException(UserErrorStatus.VERIFICATION_TOKEN_INVALID_OR_EXPIRED);
        }
        tokenStore.consume(token);
        return email;
    }

    /**
     * 인증을 거치지 않을경우 예외를 발생
     */
    public void verifyTokenOrThrow(String token, String target) {
        validateTokenOrThrow(token, target);

        tokenStore.consume(token);
    }

    /**
     * Token validation without consumption.
     *
     * Use this before expensive side effects such as S3 uploads. The final
     * signup flow must still call verifyTokenOrThrow so the token is consumed
     * exactly once immediately before account creation.
     */
    public void validateTokenOrThrow(String token, String target) {
        String saved = tokenStore.get(token);
        //토큰 자체가 없다면
        if (saved == null) {
            throw new UserException(UserErrorStatus.VERIFICATION_TOKEN_INVALID_OR_EXPIRED);
        }
        //인증이 아닐경우
        if (!saved.equals(target)) {
            throw new UserException(UserErrorStatus.VERIFICATION_TARGET_MISMATCH);
        }
    }
}
