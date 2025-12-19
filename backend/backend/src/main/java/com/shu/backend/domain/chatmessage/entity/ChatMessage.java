package com.shu.backend.domain.chatmessage.entity;

import com.shu.backend.domain.chatroom.entity.ChatRoom;
import com.shu.backend.domain.user.entity.User;
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
@Table(
        name = "chat_message",
        indexes = {
                @Index(name = "idx_message_room_id", columnList = "chat_room_id, id")
        }
)
public class ChatMessage extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType type; // TEXT, IMAGE

    @Column(columnDefinition = "TEXT")
    private String content; // TEXT일 때 사용



    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Builder
    private ChatMessage(MessageType type, String content, String mediaUrl, ChatRoom chatRoom, User sender) {
        this.type = type;
        this.content = content;
        this.chatRoom = chatRoom;
        this.sender = sender;
    }

    public enum MessageType {
        TEXT, IMAGE
    }
}