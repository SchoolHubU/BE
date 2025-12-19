package com.shu.backend.domain.chatmessage.controller;

import com.shu.backend.domain.chatmessage.dto.ChatMessageDTO;
import com.shu.backend.domain.chatmessage.exception.status.ChatMessageSuccessStatus;
import com.shu.backend.domain.chatmessage.service.ChatMessageService;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    private final SimpMessagingTemplate messagingTemplate;

    // =================== 채팅방 메시지 조회 (입장 시) ===================
    @Operation(
            summary = "채팅방 메시지 조회",
            description = "채팅방 입장 시 최근 메시지를 조회합니다. lastId가 있으면 커서 페이징(id < lastId)으로 조회합니다."
    )
    @GetMapping("/rooms/{roomId}/messages")
    public ApiResponse<ChatMessageDTO.MessageListResponse> messages(
            @Parameter(description = "로그인 사용자", required = true)
            @AuthenticationPrincipal User user,

            @Parameter(description = "채팅방 ID", required = true)
            @PathVariable Long roomId,

            @Parameter(description = "커서 페이징용 마지막 메시지 ID (없으면 최신 50개)")
            @RequestParam(required = false) Long lastId
    ) {
        ChatMessageDTO.MessageListResponse result = chatMessageService.getMessages(user.getId(), roomId, lastId);
        return ApiResponse.of(ChatMessageSuccessStatus.CHAT_MESSAGE_LIST_SUCCESS, result);
    }

    // =================== 읽음 처리 ===================
    @Operation(
            summary = "채팅방 읽음 처리",
            description = "사용자의 lastReadMessageId를 최신으로 갱신합니다."
    )
    @PostMapping("/rooms/{roomId}/read")
    public ApiResponse<String> read(
            @Parameter(description = "로그인 사용자", required = true)
            @AuthenticationPrincipal User user,

            @Parameter(description = "채팅방 ID", required = true)
            @PathVariable Long roomId,

            @RequestBody @Valid ChatMessageDTO.ReadRequest request
    ) {
        chatMessageService.read(user.getId(), roomId, request.getMessageId());
        return ApiResponse.of(ChatMessageSuccessStatus.CHAT_MESSAGE_READ_SUCCESS, "OK");
    }


    // =================== (테스트/백업용) HTTP 메시지 전송 ===================
    @Operation(
            summary = "[HTTP] 채팅 메시지 전송 (Swagger 테스트용)",
            description = """
                    Swagger에서 WebSocket(@MessageMapping)을 직접 호출할 수 없어서, HTTP로 메시지를 전송하는 테스트/백업용 API입니다.
                    메시지를 저장한 뒤, /sub/chat/rooms/{roomId} 로도 브로드캐스트합니다.
                    """
    )
    @PostMapping("/rooms/{roomId}/messages")
    public ApiResponse<ChatMessageDTO.MessageResponse> sendHttp(
            @Parameter(description = "로그인 사용자", required = true)
            @AuthenticationPrincipal User user,

            @Parameter(description = "채팅방 ID", required = true)
            @PathVariable Long roomId,

            @RequestBody @Valid ChatMessageDTO.SendRequest request
    ) {
        // path roomId 우선 적용 (조작 방지)
        ChatMessageDTO.SendRequest fixed = ChatMessageDTO.SendRequest.builder()
                .roomId(roomId)
                .type(request.getType())
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .build();

        ChatMessageDTO.MessageResponse result = chatMessageService.send(user.getId(), fixed);

        // 실시간도 같이 전송
        messagingTemplate.convertAndSend(
                "/sub/chat/rooms/" + roomId,
                ApiResponse.of(ChatMessageSuccessStatus.CHAT_MESSAGE_SEND_SUCCESS, result)
        );

        return ApiResponse.of(ChatMessageSuccessStatus.CHAT_MESSAGE_SEND_SUCCESS, result);
    }
}

