package com.shu.backend.domain.auth.service;


import com.shu.backend.domain.auth.provider.SmsProvider;
import com.shu.backend.domain.auth.store.SmsCodeStore;
import com.shu.backend.domain.auth.store.SmsTokenStore;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class SmsVerificationService {

    private final SmsProvider smsProvider;
    private final SmsCodeStore smsCodeStore;
    private final SmsTokenStore smsTokenStore;

    // 인증번호 발송
    public void sendCode(String phoneNumber) {
        String code = String.valueOf(
                ThreadLocalRandom.current().nextInt(100000, 999999)
        );

        smsCodeStore.save(phoneNumber, code);
        smsProvider.send(phoneNumber, "[Teenple] 인증번호는 " + code + " 입니다.");
    }

    // 인증번호 검증 → verificationToken 발급
    public String verifyCode(String phoneNumber, String code) {
        String saved = smsCodeStore.get(phoneNumber);

        if (saved == null || !saved.equals(code)) {
            throw new UserException(UserErrorStatus.PHONE_VERIFICATION_CODE_INVALID);
        }

        smsCodeStore.delete(phoneNumber);

        String token = "verif_" + UUID.randomUUID();
        smsTokenStore.save(token, phoneNumber);

        return token;
    }

    // 회원가입 시 토큰 검증
    public void verifyTokenOrThrow(String token, String phoneNumber) {
        String saved = smsTokenStore.get(token);

        if (saved == null) {
            throw new UserException(UserErrorStatus.PHONE_VERIFICATION_TOKEN_INVALID_OR_EXPIRED);
        }

        if (!saved.equals(phoneNumber)) {
            throw new UserException(UserErrorStatus.PHONE_VERIFICATION_PHONE_MISMATCH);
        }

        smsTokenStore.consume(token);
    }
}