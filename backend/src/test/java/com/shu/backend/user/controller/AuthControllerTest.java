package com.shu.backend.user.controller;

import com.shu.backend.domain.user.controller.AuthController;
import com.shu.backend.domain.user.dto.UserRequestDTO;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import com.shu.backend.domain.user.service.AuthService;
import com.shu.backend.global.file.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private FileStorageService fileStorageService;

    @Test
    @DisplayName("회원가입 처리 실패 시 먼저 업로드된 학생 인증 이미지를 삭제한다")
    void signUp_cleansUploadedStudentCard_whenJoinFails() {
        AuthController controller = new AuthController(authService, fileStorageService);
        UserRequestDTO.SignUp request = UserRequestDTO.SignUp.builder().build();
        MockMultipartFile studentCard = new MockMultipartFile(
                "studentCard",
                "card.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}
        );

        given(fileStorageService.uploadStudentCardImage(studentCard))
                .willReturn("student-card/uploaded-card.jpg");
        given(authService.join(eq(request), eq("student-card/uploaded-card.jpg")))
                .willThrow(new UserException(UserErrorStatus.VERIFICATION_TOKEN_INVALID_OR_EXPIRED));

        assertThrows(UserException.class, () -> controller.signUp(request, studentCard));

        verify(fileStorageService).deleteStudentCardImage("student-card/uploaded-card.jpg");
    }

    @Test
    @DisplayName("회원가입 사전 검증 실패 시 학생 인증 이미지를 업로드하지 않는다")
    void signUp_doesNotUploadStudentCard_whenPreValidationFails() {
        AuthController controller = new AuthController(authService, fileStorageService);
        UserRequestDTO.SignUp request = UserRequestDTO.SignUp.builder().build();
        MockMultipartFile studentCard = new MockMultipartFile(
                "studentCard",
                "card.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}
        );

        willThrow(new UserException(UserErrorStatus.VERIFICATION_TOKEN_INVALID_OR_EXPIRED))
                .given(authService)
                .validateSignUpBeforeStudentCardUpload(request);

        assertThrows(UserException.class, () -> controller.signUp(request, studentCard));

        verify(fileStorageService, never()).uploadStudentCardImage(studentCard);
        verify(authService, never()).join(eq(request), anyString());
    }

    @Test
    @DisplayName("학교 인증 재요청 처리 실패 시 먼저 업로드된 학생 인증 이미지를 삭제한다")
    void reapplyVerification_cleansUploadedStudentCard_whenReapplyFails() {
        AuthController controller = new AuthController(authService, fileStorageService);
        UserRequestDTO.VerificationReapply request = new UserRequestDTO.VerificationReapply();
        MockMultipartFile studentCard = new MockMultipartFile(
                "studentCard",
                "card.jpg",
                "image/jpeg",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}
        );

        given(fileStorageService.uploadStudentCardImage(studentCard))
                .willReturn("student-card/reapply-card.jpg");
        given(authService.reapplyVerification(eq(request), eq("student-card/reapply-card.jpg")))
                .willThrow(new UserException(UserErrorStatus.INVALID_PASSWORD));

        assertThrows(UserException.class, () -> controller.reapplyVerification(request, studentCard));

        verify(fileStorageService).deleteStudentCardImage("student-card/reapply-card.jpg");
    }
}
