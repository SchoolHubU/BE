package com.shu.backend.auth.service;

import com.shu.backend.domain.auth.provider.VerificationMessageProvider;
import com.shu.backend.domain.auth.service.VerificationService;
import com.shu.backend.domain.auth.store.VerificationCodeStore;
import com.shu.backend.domain.auth.store.VerificationTokenStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock
    private VerificationMessageProvider provider;

    @Mock
    private VerificationCodeStore codeStore;

    @Mock
    private VerificationTokenStore tokenStore;

    @Test
    @DisplayName("회원가입 인증번호 검증 성공 시 회원가입용 1시간 토큰으로 저장한다")
    void verifyCode_savesSignupToken_whenPurposeIsSignup() {
        VerificationService service = new VerificationService(provider, codeStore, tokenStore);
        given(codeStore.get("user@example.com")).willReturn("123456");
        given(codeStore.getAttemptCount("user@example.com")).willReturn(0);
        given(codeStore.getPurpose("user@example.com")).willReturn(VerificationCodeStore.PURPOSE_SIGNUP);

        String token = service.verifyCode("user@example.com", "123456");

        assertThat(token).startsWith("verif_");
        verify(tokenStore).saveForSignup(token, "user@example.com");
    }

    @Test
    @DisplayName("비밀번호 재설정 인증번호 검증 성공 시 기본 5분 토큰으로 저장한다")
    void verifyCode_savesDefaultToken_whenPurposeIsPasswordReset() {
        VerificationService service = new VerificationService(provider, codeStore, tokenStore);
        given(codeStore.get("user@example.com")).willReturn("123456");
        given(codeStore.getAttemptCount("user@example.com")).willReturn(0);
        given(codeStore.getPurpose("user@example.com")).willReturn(VerificationCodeStore.PURPOSE_PASSWORD_RESET);

        String token = service.verifyCode("user@example.com", "123456");

        assertThat(token).startsWith("verif_");
        verify(tokenStore).save(token, "user@example.com");
    }

    @Test
    @DisplayName("비밀번호 재설정 인증번호 발송 시 비밀번호 재설정 목적의 코드를 저장한다")
    void sendPasswordResetCode_savesPasswordResetPurpose() {
        VerificationService service = new VerificationService(provider, codeStore, tokenStore);

        String code = service.sendPasswordResetCode("user@example.com");

        assertThat(code).hasSize(6);
        verify(codeStore).savePasswordReset("user@example.com", code);
        verify(provider).sendPasswordReset("user@example.com", code);
    }
}
