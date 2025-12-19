package com.shu.backend.domain.chatroomuser.entity;

import com.shu.backend.domain.chatroom.entity.ChatRoom;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
@Table(
        name = "chat_room_user",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_room_user",
                columnNames = {"chat_room_id", "user_id"}
        ),
        indexes = {
                @Index(name = "idx_cru_user_room", columnList = "user_id, chat_room_id"),
                @Index(name = "idx_cru_room_hidden", columnList = "chat_room_id, hidden")
        }
)
public class ChatRoomUser extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 사용자별 읽음 상태
    private Long lastReadMessageId;

    // 나가기/숨김/차단
    private LocalDateTime leftAt;     // 나간 시각 (null = 참여 중)
    private LocalDateTime blockedAt;  // 차단 시각 (null = 미차단)

    @Column(nullable = false)
    private boolean hidden;           // 내 목록에서 숨김(삭제처럼 보이게)

    @Builder
    private ChatRoomUser(ChatRoom chatRoom, User user) {
        this.chatRoom = chatRoom;
        this.user = user;
        this.hidden = false;
    }

    public boolean isBlocked() {
        return blockedAt != null;
    }

    // 읽음 갱신
    public void read(Long messageId) {
        if (this.lastReadMessageId == null || messageId > this.lastReadMessageId) {
            this.lastReadMessageId = messageId;
        }
    }

    // 채팅방 나가기 (내 목록에서 숨김 처리)
    public void leave() {
        this.leftAt = LocalDateTime.now();
        this.hidden = true;
    }

    // 상대가 메시지 보내면 자동 재참여
    public void rejoinIfLeftOrHidden() {
        if (this.blockedAt != null) return; // 차단은 절대 복귀 X
        if (this.leftAt != null || this.hidden) {
            this.leftAt = null;
            this.hidden = false;
        }
    }

    // 차단 (완전 차단 + 내 목록에서 숨김)
    public void block() {
        this.blockedAt = LocalDateTime.now();
        this.leftAt = LocalDateTime.now();
        this.hidden = true;
    }

    public void unblock() {
        this.blockedAt = null;
        // 해제 시 방을 다시 보이게 할지 정책에 따라:
        // this.hidden = false;
        // this.leftAt = null;
    }

    public static ChatRoomUser createHidden(ChatRoom chatRoom, User user) {
        ChatRoomUser cru = new ChatRoomUser(chatRoom, user);
        cru.hidden = true;        // 처음엔 숨김
        cru.leftAt = null;        // 참여중이지만 숨김 상태로만
        cru.blockedAt = null;
        return cru;
    }

    public static ChatRoomUser createVisible(ChatRoom chatRoom, User user) {
        return new ChatRoomUser(chatRoom, user); // 기존 생성자(hidden=false)
    }
}