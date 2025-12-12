package com.shu.backend.domain.user.controller;

import com.shu.backend.domain.school.exception.SchoolException;
import com.shu.backend.domain.school.exception.status.SchoolErrorStatus;
import com.shu.backend.domain.user.dto.SignUpResponseDTO;
import com.shu.backend.domain.user.dto.UserRequestDTO;
import com.shu.backend.domain.user.exception.status.UserSuccessStatus;
import com.shu.backend.domain.user.service.AuthService;
import com.shu.backend.global.apiPayload.ApiResponse;
import com.shu.backend.global.file.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Auth", description = "사용자 인증 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {
    private final AuthService authService;
    private final FileStorageService fileStorageService; // 이미지 업로드용 (S3/로컬 등)

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
}

