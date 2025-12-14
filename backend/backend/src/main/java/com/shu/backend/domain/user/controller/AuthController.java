package com.shu.backend.domain.user.controller;

import com.shu.backend.domain.school.exception.SchoolException;
import com.shu.backend.domain.school.exception.status.SchoolErrorStatus;
import com.shu.backend.domain.user.dto.LoginResponseDTO;
import com.shu.backend.domain.user.dto.SignUpResponseDTO;
import com.shu.backend.domain.user.dto.UserLoginDTO;
import com.shu.backend.domain.user.dto.UserRequestDTO;
import com.shu.backend.domain.user.exception.status.UserSuccessStatus;
import com.shu.backend.domain.user.service.AuthService;
import com.shu.backend.global.apiPayload.ApiResponse;

import com.shu.backend.global.file.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
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
        // 이미지 필수 체크
        if (studentCardImage == null || studentCardImage.isEmpty()) {
            throw new SchoolException(SchoolErrorStatus.REQUEST_IMAGE_REQUIRED);
        }

        // 실제 스토리지(S3/로컬 등)에 업로드 후 URL 반환
        String imageUrl = fileStorageService.uploadStudentCardImage(studentCardImage);

        log.info("이미지 URL = {}", imageUrl);

        // 회원가입 + 인증요청 생성 + 토큰 발급
        SignUpResponseDTO result = authService.join(request, imageUrl);

        return ApiResponse.of(UserSuccessStatus.USER_SIGNUP_SUCCESS, result);
    }


    // =================== 로그인 ===================
    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 로그인하고, 액세스 토큰을 발급받습니다."
    )
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<LoginResponseDTO> login(
            @RequestBody @Valid UserLoginDTO request
    ) {
        LoginResponseDTO result = authService.login(request);
        return ApiResponse.of(UserSuccessStatus.USER_LOGIN_SUCCESS, result);
    }


    // =================== 로그아웃 ===================
    @Operation(
            summary = "로그아웃",
            description = "현재 액세스 토큰을 기반으로 로그아웃을 수행합니다. " +
                    "JWT를 서버에 저장하지 않는 구조이므로, 실제 토큰 제거는 클라이언트에서 수행해야 합니다."
    )
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // TODO: 나중에 Redis 블랙리스트 쓰면 여기에서 토큰 추가 처리
        return ApiResponse.of(UserSuccessStatus.USER_LOGOUT_SUCCESS, null);
    }

    // =================== 승인 거절시 재요청 ===================
    @Operation(
            summary = "학교 인증 재요청",
            description = "학교 인증이 거절(REJECTED)되었거나 아직 요청이 없는 사용자가 재요청을 보냅니다. " +
                    "로그인(액세스 토큰) 없이 이메일/비밀번호로 본인 확인 후 새 인증요청(PENDING)을 생성합니다. " +
                    "이미 인증 완료(verified=true) 또는 심사중(PENDING)인 경우 요청이 거절됩니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "재요청 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청(비밀번호 오류, 이미 인증 완료 등)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "리소스 없음(이메일/학교를 찾을 수 없음)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "상태 충돌(이미 PENDING 요청 존재)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류"
            )
    })
    @PostMapping("/verification/reapply")
    public com.shu.backend.global.apiPayload.ApiResponse<Long> reapplyVerification(
            @RequestBody @Valid UserRequestDTO.VerificationReapply req
    ) {
        return com.shu.backend.global.apiPayload.ApiResponse.onSuccess(
                authService.reapplyVerification(req)
        );
    }
}

