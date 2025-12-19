package com.shu.backend.domain.chatroom.controller;

import com.shu.backend.domain.chatroom.dto.ChatRoomDTO;
import com.shu.backend.domain.chatroom.exception.status.ChatRoomSuccessStatus;
import com.shu.backend.domain.chatroom.service.ChatRoomService;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    // =================== 1:1 DM 채팅방 생성/조회 ===================
    @Operation(
            summary = "1:1 DM 채팅방 생성/조회",
            description = "상대 userId로 1:1 DM 채팅방을 조회하고, 없으면 생성합니다."
    )
    @PostMapping("/dm")
    public ApiResponse<ChatRoomDTO.CreateDmResponse> createOrGetDm(
            @Parameter(description = "로그인 사용자", required = true)
            @AuthenticationPrincipal User user,

            @RequestBody @Valid ChatRoomDTO.CreateDmRequest request
    ) {
        Long myId = user.getId();
        ChatRoomDTO.CreateDmResponse result = chatRoomService.findOrCreateDm(myId, request.getOtherUserId());
        return ApiResponse.of(ChatRoomSuccessStatus.DM_ROOM_FIND_OR_CREATE_SUCCESS, result);
    }

    // =================== 내 채팅방 목록 조회 ===================
    @Operation(
            summary = "내 채팅방 목록 조회",
            description = "숨김/차단 제외한 내 채팅방 목록을 최신 메시지 기준으로 조회합니다."
    )
    @GetMapping
    public ApiResponse<ChatRoomDTO.RoomListResponse> myRooms(
            @Parameter(description = "로그인 사용자", required = true)
            @AuthenticationPrincipal User user
    ) {
        Long myId = user.getId();
        ChatRoomDTO.RoomListResponse result = chatRoomService.getMyRooms(myId);
        return ApiResponse.of(ChatRoomSuccessStatus.CHAT_ROOM_LIST_SUCCESS, result);
    }

    // =================== 채팅방 나가기 ===================
    @Operation(
            summary = "채팅방 나가기",
            description = "채팅방을 나갑니다. (hidden=true 처리) 상대가 메시지를 보내면 자동 복귀될 수 있습니다."
    )
    @PostMapping("/{roomId}/leave")
    public ApiResponse<String> leave(
            @Parameter(description = "로그인 사용자", required = true)
            @AuthenticationPrincipal User user,

            @Parameter(description = "채팅방 ID", required = true)
            @PathVariable Long roomId
    ) {
        chatRoomService.leave(user.getId(), roomId);
        return ApiResponse.of(ChatRoomSuccessStatus.CHAT_ROOM_LEAVE_SUCCESS, "OK");
    }

    // =================== 채팅방 차단 ===================
    @Operation(
            summary = "채팅방 차단",
            description = "채팅방을 차단합니다. 차단 상태에서는 채팅이 불가능합니다."
    )
    @PostMapping("/{roomId}/block")
    public ApiResponse<String> block(
            @Parameter(description = "로그인 사용자", required = true)
            @AuthenticationPrincipal User user,

            @Parameter(description = "채팅방 ID", required = true)
            @PathVariable Long roomId
    ) {
        chatRoomService.block(user.getId(), roomId);
        return ApiResponse.of(ChatRoomSuccessStatus.CHAT_ROOM_BLOCK_SUCCESS, "OK");
    }
}
