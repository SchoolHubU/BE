package com.shu.backend.domain.auth.controller;

import com.shu.backend.domain.auth.service.SmsVerificationService;
import com.shu.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth - SMS Verification", description = "SMS 인증(OTP) 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/phone/sms")
public class SmsVerificationController {

    private final SmsVerificationService smsVerificationService;

    // =================== SMS 인증번호 발송 ===================
    @Operation(
            summary = "SMS 인증번호 발송",
            description = "입력한 휴대폰 번호로 6자리 인증번호(SMS OTP)를 발송합니다. " +
                    "개발(dev) 환경에서는 실제 문자가 발송되지 않으며, 로그로만 출력됩니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "인증번호 발송 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 휴대폰 번호 형식"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "SMS 발송 실패"
            )
    })
    @PostMapping(
            value = "/send",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ApiResponse<Void> send(
            @Parameter(
                    description = "인증번호를 받을 휴대폰 번호 (하이픈 없이)",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SendRequest.class))
            )
            @RequestBody @Valid SendRequest request
    ) {
        smsVerificationService.sendCode(request.phoneNumber());
        return ApiResponse.onSuccess(null);
    }

    // =================== SMS 인증번호 검증 ===================
    @Operation(
            summary = "SMS 인증번호 검증",
            description = "사용자가 입력한 인증번호를 검증합니다. " +
                    "검증에 성공하면 회원가입에 사용할 1회성 verificationToken을 발급합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "인증 성공 (verificationToken 발급)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "인증번호 불일치 또는 만료"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류"
            )
    })
    @PostMapping(
            value = "/verify",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ApiResponse<VerifyResponse> verify(
            @Parameter(
                    description = "휴대폰 번호와 SMS 인증번호",
                    required = true,
                    content = @Content(schema = @Schema(implementation = VerifyRequest.class))
            )
            @RequestBody @Valid VerifyRequest request
    ) {
        String verificationToken = smsVerificationService.verifyCode(
                request.phoneNumber(),
                request.code()
        );
        return ApiResponse.onSuccess(new VerifyResponse(verificationToken));
    }


// =================== Request / Response DTO ===================

    @Schema(description = "SMS 인증번호 발송 요청 DTO")
    public record SendRequest(

            @Schema(
                    description = "휴대폰 번호 (010으로 시작, 하이픈 없이)",
                    example = "01012345678"
            )
            @NotBlank
            @Pattern(
                    regexp = "^010\\d{8}$",
                    message = "휴대폰 번호는 010으로 시작하는 11자리 숫자여야 합니다."
            )
            String phoneNumber
    ) {}

    @Schema(description = "SMS 인증번호 검증 요청 DTO")
    public record VerifyRequest(

            @Schema(
                    description = "휴대폰 번호 (010으로 시작, 하이픈 없이)",
                    example = "01012345678"
            )
            @NotBlank
            @Pattern(
                    regexp = "^010\\d{8}$",
                    message = "휴대폰 번호는 010으로 시작하는 11자리 숫자여야 합니다."
            )
            String phoneNumber,

            @Schema(
                    description = "6자리 SMS 인증번호",
                    example = "123456"
            )
            @NotBlank
            @Pattern(
                    regexp = "^\\d{6}$",
                    message = "인증번호는 6자리 숫자여야 합니다."
            )
            String code
    ) {}


    @Schema(description = "SMS 인증 성공 응답 DTO")
    public record VerifyResponse(
            @Schema(
                    description = "회원가입 시 사용되는 1회성 인증 토큰",
                    example = "verif_550e8400-e29b-41d4-a716-446655440000"
            )
            String verificationToken
    ) {}
}
