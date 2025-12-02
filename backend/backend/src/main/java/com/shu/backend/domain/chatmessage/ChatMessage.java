package com.shu.backend.domain.chatmessage;

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
public class ChatMessage extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;

    @Column(nullable = false)
    private boolean hasMedia;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;



    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Builder
    public ChatMessage(String content, boolean hasMedia ,ChatRoom chatRoom, User sender) {
        this.content = content;
        this.hasMedia = hasMedia;
        this.chatRoom = chatRoom;
        this.sender = sender;

    }
}
