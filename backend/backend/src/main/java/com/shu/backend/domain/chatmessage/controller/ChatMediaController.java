package com.shu.backend.domain.chatmessage.controller;

import com.shu.backend.domain.chatmessage.dto.ChatMessageDTO;
import com.shu.backend.domain.chatmessage.exception.status.ChatMessageSuccessStatus;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.apiPayload.ApiResponse;
import com.shu.backend.global.file.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatMediaController {

    private final FileStorageService fileStorageService;

    @Operation(
            summary = "채팅 이미지 업로드",
            description = """
                    채팅에서 사용할 이미지를 업로드합니다.
                    업로드 성공 시 이미지 접근 URL을 반환하며,
                    반환된 imageUrl을 WebSocket 메시지 전송 시 사용합니다.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "채팅 이미지 업로드 성공",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                            schema = @io.swagger.v3.oas.annotations.media.Schema(
                                    implementation = ChatMessageDTO.UploadImageResponse.class
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "채팅 이미지 업로드 실패"
            )
    })
    @PostMapping(
            value = "/images",
            consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<ChatMessageDTO.UploadImageResponse> uploadChatImage(
            @Parameter(description = "로그인 사용자", required = true)
            @AuthenticationPrincipal User user,

            @Parameter(
                    description = "업로드할 이미지 파일",
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestPart("file") MultipartFile file
    ) {
        String url = fileStorageService.uploadChatImage(file);
        return ApiResponse.of(
                ChatMessageSuccessStatus.CHAT_IMAGE_UPLOAD_SUCCESS,
                new ChatMessageDTO.UploadImageResponse(url)
        );
    }
}