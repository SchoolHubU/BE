package com.shu.backend.global.websocket;

import com.shu.backend.domain.chatmessage.exception.ChatMessageException;
import com.shu.backend.domain.chatmessage.exception.status.ChatMessageErrorStatus;
import com.shu.backend.domain.chatroomuser.repository.ChatRoomUserRepository;
import com.shu.backend.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class WsJwtChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final ChatRoomUserRepository chatRoomUserRepository;

    private static final Pattern ROOM_SUB_PATTERN =
            Pattern.compile("^/sub/chat/rooms/(\\d+)$");

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (accessor.getCommand() == null) return message;

        /* ================= CONNECT ================= */
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String auth = accessor.getFirstNativeHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                throw new ChatMessageException(ChatMessageErrorStatus.UNAUTHORIZED_WS);
            }

            String token = auth.substring(7);

            if (!jwtTokenProvider.validateToken(token)) {
                throw new ChatMessageException(ChatMessageErrorStatus.INVALID_WS_TOKEN);
            }

            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            accessor.setUser(new WsPrincipal(userId.toString()));
            return message;
        }

        /* ================= 인증 공통 ================= */
        Principal principal = accessor.getUser();
        if (principal == null || principal.getName() == null) {
            throw new ChatMessageException(ChatMessageErrorStatus.UNAUTHORIZED_WS);
        }

        /* ================= SUBSCRIBE ================= */
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {

            String destination = accessor.getDestination();
            if (destination == null) return message;

            Matcher matcher = ROOM_SUB_PATTERN.matcher(destination);
            if (matcher.matches()) {
                Long roomId = Long.valueOf(matcher.group(1));
                Long userId = Long.valueOf(principal.getName());

                boolean isMember = chatRoomUserRepository
                        .findByChatRoomIdAndUserId(roomId, userId)
                        .isPresent();

                if (!isMember) {
                    throw new ChatMessageException(ChatMessageErrorStatus.WS_NOT_ROOM_MEMBER);
                }
            }
        }

        return message;
    }

    private record WsPrincipal(String name) implements Principal {
        @Override public String getName() { return name; }
    }
}