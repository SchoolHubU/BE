package com.shu.backend.domain.pushtoken.controller;

import com.shu.backend.domain.pushtoken.dto.PushTokenDTO;
import com.shu.backend.domain.pushtoken.service.PushTokenService;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "PushToken",
        description = "푸시 알림 토큰 관련 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/push-tokens")
public class PushTokenController {

    private final PushTokenService pushTokenService;


    @Operation(
            summary = "푸시 토큰 등록/갱신",
            description = """
                    모바일 앱에서 발급받은 FCM 푸시 토큰을 서버에 등록하거나 갱신합니다.

                    - 로그인 후 또는 앱 시작 시 호출합니다.
                    - 동일한 token이 이미 존재하면 userId/플랫폼/활성 상태를 갱신합니다(Upsert).
                    - 토큰은 기기 단위이므로, 유저당 여러 개가 등록될 수 있습니다.
                    """
    )
    @PostMapping
    public ApiResponse<PushTokenDTO.RegisterResponse> register(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PushTokenDTO.RegisterRequest req
    ) {
        PushTokenDTO.RegisterResponse registerResponse = pushTokenService.registerOrUpdate(user.getId(), req);
        return ApiResponse.onSuccess(registerResponse);
    }

    @Operation(
            summary = "내 푸시 토큰 전체 비활성화",
            description = """
                    현재 로그인한 사용자의 모든 푸시 토큰을 비활성화합니다.

                    - 로그아웃 시 호출을 권장합니다.
                    - 비활성화된 토큰에는 푸시 알림이 전송되지 않습니다.
                    - 토큰 레코드는 삭제되지 않고 isActive=false 처리됩니다.
                    """
    )
    @DeleteMapping("/all")
    public int deactivateAll(@AuthenticationPrincipal User user) {
        return pushTokenService.deactivateAll(user.getId());
    }

    @Operation(
            summary = "특정 푸시 토큰 비활성화",
            description = """
                    지정한 푸시 토큰 하나만 비활성화합니다.

                    - 특정 기기에서만 로그아웃 처리할 때 사용합니다.
                    - token 문자열이 정확히 일치해야 합니다.
                    - 보통은 /all API 사용을 더 권장합니다.
                    """
    )
    @DeleteMapping
    public int deactivateOne(@RequestParam String token) {
        return pushTokenService.deactivateByToken(token);
    }
}
