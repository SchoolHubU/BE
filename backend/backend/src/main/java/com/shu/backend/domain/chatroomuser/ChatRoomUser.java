package com.shu.backend.domain.chatroomuser;

import com.shu.backend.domain.chatroom.ChatRoom;
import com.shu.backend.domain.user.User;
import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class ChatRoomUser extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Long lastReadMessageId; //사용자별 읽음 상태 저장


    @Builder
    public ChatRoomUser(ChatRoom chatRoom, User user,Long lastReadMessageId) {
        this.chatRoom = chatRoom;
        this.user = user;
        this.lastReadMessageId = lastReadMessageId;
    }

    //메시지 읽음 표시 (ex: 50 -> 53번째 메시지 까지 읽었을때 53으로 최신화)
    public void read(Long messageId) {
        if (this.lastReadMessageId == null || messageId > this.lastReadMessageId) {
            this.lastReadMessageId = messageId;
        }
    }


}
