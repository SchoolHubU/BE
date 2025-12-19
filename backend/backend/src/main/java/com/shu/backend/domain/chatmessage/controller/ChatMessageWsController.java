package com.shu.backend.domain.chatmessage.controller;

import com.shu.backend.domain.chatmessage.dto.ChatMessageDTO;
import com.shu.backend.domain.chatmessage.exception.ChatMessageException;
import com.shu.backend.domain.chatmessage.exception.status.ChatMessageErrorStatus;
import com.shu.backend.domain.chatmessage.exception.status.ChatMessageSuccessStatus;
import com.shu.backend.domain.chatmessage.service.ChatMessageService;
import com.shu.backend.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatMessageWsController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    // =================== 실시간 메시지 전송 ===================
    // Client -> /pub/chat.send
    // Server -> /sub/chat/rooms/{roomId}
    @MessageMapping("/chat.send")
    public void send(ChatMessageDTO.SendRequest request, Principal principal) {

        if (principal == null || principal.getName() == null) {
            throw new ChatMessageException(ChatMessageErrorStatus.UNAUTHORIZED_WS);
        }

        Long senderId = Long.valueOf(principal.getName());

        ChatMessageDTO.MessageResponse result = chatMessageService.send(senderId, request);

        messagingTemplate.convertAndSend(
                "/sub/chat/rooms/" + request.getRoomId(),
                ApiResponse.of(ChatMessageSuccessStatus.CHAT_MESSAGE_SEND_SUCCESS, result)
        );
    }
}