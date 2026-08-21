package com.shu.backend.domain.user.controller;

import com.shu.backend.domain.school.exception.SchoolException;
import com.shu.backend.domain.school.exception.status.SchoolErrorStatus;
import com.shu.backend.domain.user.dto.*;
import com.shu.backend.domain.user.exception.status.UserSuccessStatus;
import com.shu.backend.domain.user.service.AuthService;
import com.shu.backend.global.apiPayload.ApiResponse;

import com.shu.backend.global.file.FileStorageService;
import com.shu.backend.global.ratelimit.RateLimit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Auth", description = "사용자 인증 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {
    private final AuthService authService;
    private final FileStorageService fileStorageService; // 이미지 업로드용 (S3/로컬 등)


    // =================== 회원가입 ===================
    @Operation(
            summary = "회원가입",
            description = "회원정보와 학생증 이미지를 받아 회원가입을 수행하고, 학교 인증 요청을 생성합니다."
    )
    @RateLimit(key = "auth:signup", limit = 10, windowSeconds = 300, byIp = true)
    @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SignUpResponseDTO> signUp(
            @Parameter(
                    description = "회원가입 기본 정보(JSON)",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserRequestDTO.SignUp.class)
                    )
            )
            @RequestPart("data") @Valid UserRequestDTO.SignUp request,

            @Parameter(
                    description = "학생증/재학증명서 이미지 파일",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
            )
            @RequestPart("studentCard") MultipartFile studentCardImage
    ) {
        if (studentCardImage == null || studentCardImage.isEmpty()) {
            throw new SchoolException(SchoolErrorStatus.REQUEST_IMAGE_REQUIRED);
        }

        authService.validateSignUpBeforeStudentCardUpload(request);

        String imageKey = null;
        SignUpResponseDTO result;
        try {
            imageKey = fileStorageService.uploadStudentCardImage(studentCardImage);
            log.info("Student card uploaded for signup: key={}", imageKey);

            result = authService.join(request, imageKey);
        } catch (RuntimeException e) {
            cleanupUploadedStudentCardImage(imageKey, "signup", e);
            throw e;
        }

        return ApiResponse.of(UserSuccessStatus.USER_SIGNUP_SUCCESS, result);
    }


    // =================== 로그인 ===================
    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 로그인하고, 액세스 토큰을 발급받습니다."
    )
    @RateLimit(key = "auth:login", limit = 10, windowSeconds = 180, byIp = true)
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<LoginResponseDTO> login(
            @RequestBody @Valid UserLoginDTO request
    ) {
        LoginResponseDTO result = authService.login(request);
        return ApiResponse.of(UserSuccessStatus.USER_LOGIN_SUCCESS, result);
    }


    // =================== 토큰 갱신 ===================
    @Operation(
            summary = "Access Token 갱신",
            description = "Refresh Token으로 새 Access Token과 Refresh Token을 발급합니다. (Rotation 방식)"
    )
    @PostMapping("/refresh")
    public ApiResponse<TokenRefreshResponseDTO> refresh(
            @RequestBody @Valid TokenRefreshRequestDTO request
    ) {
        TokenRefreshResponseDTO result = authService.refresh(request.getRefreshToken());
        return ApiResponse.of(UserSuccessStatus.USER_TOKEN_REFRESH_SUCCESS, result);
    }

    // =================== 로그아웃 ===================
    @Operation(
            summary = "로그아웃",
            description = "Refresh Token을 서버에서 즉시 무효화합니다."
    )
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestBody @Valid TokenRefreshRequestDTO request
    ) {
        authService.logout(request.getRefreshToken());
        return ApiResponse.of(UserSuccessStatus.USER_LOGOUT_SUCCESS, null);
    }

    // =================== 승인 거절시 재요청 ===================
    @Operation(
            summary = "학교 인증 재요청",
            description = "학교 인증이 거절(REJECTED)된 사용자가 이메일/비밀번호 본인 확인 후 학생증 이미지를 다시 업로드하여 재요청합니다."
    )
    @RateLimit(key = "auth:verification-reapply", limit = 5, windowSeconds = 300, byIp = true)
    @PostMapping(value = "/verification/reapply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Long> reapplyVerification(
            @RequestPart("data") @Valid UserRequestDTO.VerificationReapply req,
            @RequestPart("studentCard") MultipartFile studentCardImage
    ) {
        if (studentCardImage == null || studentCardImage.isEmpty()) {
            throw new SchoolException(SchoolErrorStatus.REQUEST_IMAGE_REQUIRED);
        }

        String imageKey = null;
        Long requestId;
        try {
            imageKey = fileStorageService.uploadStudentCardImage(studentCardImage);
            log.info("Student card uploaded for verification reapply: key={}", imageKey);

            requestId = authService.reapplyVerification(req, imageKey);
        } catch (RuntimeException e) {
            cleanupUploadedStudentCardImage(imageKey, "verification-reapply", e);
            throw e;
        }

        return ApiResponse.onSuccess(requestId);
    }

    // =================== 학교 인증 재요청 정보 조회 ===================
    @Operation(
            summary = "학교 인증 재요청 정보 조회",
            description = "로그인 없이 이메일/비밀번호로 본인 확인 후, 최근 반려된 인증 요청의 학교와 관리자 코멘트를 조회합니다."
    )
    @PostMapping("/verification/reapply-info")
    public ApiResponse<VerificationReapplyInfoResponseDTO> getReapplyInfo(
            @RequestBody @Valid UserRequestDTO.VerificationReapplyInfoRequest req
    ) {
        return ApiResponse.onSuccess(
                authService.getReapplyInfo(req)
        );
    }

    // =================== 비밀번호 찾기 - 인증번호 발송 ===================
    @Operation(
            summary = "비밀번호 재설정 인증번호 발송",
            description = "가입된 이메일로 비밀번호 재설정용 인증번호를 발송합니다."
    )
    @RateLimit(key = "auth:pw-reset", limit = 5, windowSeconds = 600, byIp = true)
    @PostMapping("/password/send-code")
    public ApiResponse<Void> sendPasswordResetCode(
            @RequestBody @Valid PasswordResetSendRequestDTO request
    ) {
        authService.sendPasswordResetCode(request.getEmail());
        return ApiResponse.of(UserSuccessStatus.PASSWORD_RESET_CODE_SENT, null);
    }

    // =================== 비밀번호 재설정 ===================
    @Operation(
            summary = "비밀번호 재설정",
            description = "이메일 인증 토큰과 새 비밀번호로 비밀번호를 변경합니다."
    )
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(
            @RequestBody @Valid ResetPasswordRequestDTO request
    ) {
        authService.resetPassword(request);
        return ApiResponse.of(UserSuccessStatus.PASSWORD_RESET_SUCCESS, null);
    }

    // =================== 이메일 찾기 ===================
    @Operation(
            summary = "이메일 찾기",
            description = "이름과 휴대폰 번호로 가입된 이메일을 마스킹하여 반환합니다."
    )
    @PostMapping("/find-email")
    public ApiResponse<FindEmailResponseDTO> findEmail(
            @RequestBody @Valid FindEmailRequestDTO request
    ) {
        return ApiResponse.of(UserSuccessStatus.FIND_EMAIL_SUCCESS, authService.findEmail(request));
    }

    // =================== 이메일 존재 여부 확인 ===================
    @Operation(
            summary = "이메일 존재 여부 확인",
            description = "입력한 이메일이 이미 가입된 이메일인지 확인합니다."
    )
    @RateLimit(key = "auth:check-email", limit = 30, windowSeconds = 60, byIp = true)
    @GetMapping("/check-email")
    public ApiResponse<EmailCheckResponseDTO> checkEmail(
            @RequestParam("email")
            @NotBlank(message = "이메일은 필수입니다.")
            @Email(message = "올바른 이메일 형식이 아닙니다.")
            String email
    ) {
        return ApiResponse.onSuccess(authService.checkEmailExists(email));
    }

    // =================== 전화번호 존재 여부 확인 ===================
    @Operation(
            summary = "전화번호 존재 여부 확인",
            description = "입력한 전화번호가 이미 가입된 전화번호인지 확인합니다."
    )
    @RateLimit(key = "auth:check-phone", limit = 30, windowSeconds = 60, byIp = true)
    @GetMapping("/check-phone")
    public ApiResponse<PhoneCheckResponseDTO> checkPhone(
            @RequestParam("phoneNumber")
            @NotBlank(message = "전화번호는 필수입니다.")
            String phoneNumber
    ) {
        return ApiResponse.onSuccess(authService.checkPhoneNumberExists(phoneNumber));
    }

    // =================== 닉네임 존재 여부 확인 ===================
    @Operation(
            summary = "닉네임 존재 여부 확인",
            description = "입력한 닉네임이 이미 사용 중인지 확인합니다."
    )
    @Validated
    @GetMapping("/check-nickname")
    public ApiResponse<NicknameCheckResponseDTO> checkNickname(
            @RequestParam("nickname")
            @NotBlank(message = "닉네임은 필수입니다.")
            String nickname
    ) {
        return ApiResponse.onSuccess(authService.checkNicknameExists(nickname));
    }

    // =================== 탈퇴 유예 계정 복구 ===================
    @Operation(
            summary = "탈퇴 유예 계정 복구",
            description = "PENDING_DELETION 상태에서 이메일·비밀번호로 본인 확인 후 계정을 ACTIVE로 복구합니다. 성공 시 새 토큰을 발급합니다."
    )
    @PostMapping("/restore")
    public ApiResponse<LoginResponseDTO> restoreAccount(
            @RequestBody @Valid UserLoginDTO request
    ) {
        LoginResponseDTO result = authService.restoreAccount(request);
        return ApiResponse.of(UserSuccessStatus.USER_RESTORE_SUCCESS, result);
    }


    private void cleanupUploadedStudentCardImage(String imageKey, String flow, RuntimeException cause) {
        if (imageKey == null || imageKey.isBlank()) {
            return;
        }

        try {
            fileStorageService.deleteStudentCardImage(imageKey);
            log.info("Cleaned up uploaded student card after {} failure: key={}", flow, imageKey);
        } catch (Exception cleanupError) {
            log.warn(
                    "Failed to clean up uploaded student card after {} failure: key={}, originalError={}",
                    flow,
                    imageKey,
                    cause.getClass().getSimpleName(),
                    cleanupError
            );
        }
    }
}

