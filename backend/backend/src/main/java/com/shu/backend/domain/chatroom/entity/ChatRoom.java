package com.shu.backend.domain.chatroom.entity;

import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
@Table(
        name = "chat_room",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_room_dm_pair",
                columnNames = {"user1_id", "user2_id"}
        ),
        indexes = {
                @Index(name = "idx_chat_room_last_message_at", columnList = "last_message_at"),
                @Index(name = "idx_room_empty_cutoff", columnList = "last_message_id, created_at")
        }
)
public class ChatRoom extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_name", nullable = false, length = 40)
    private String displayName;


    // 1:1 DM 페어 (정규화: user1Id = min, user2Id = max)
    @Column(name = "user1_id", nullable = false)
    private Long user1Id;

    @Column(name = "user2_id", nullable = false)
    private Long user2Id;

    // 방 목록 성능용 요약 필드
    @Column(name = "last_message_id")
    private Long lastMessageId;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Builder
    private ChatRoom(Long user1Id, Long user2Id, String displayName) {
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        this.displayName = displayName;
    }

    public static ChatRoom ofDm(Long a, Long b) {
        long min = Math.min(a, b);
        long max = Math.max(a, b);
        return ChatRoom.builder()
                .user1Id(min)
                .user2Id(max)
                .displayName(generateRandomName())
                .build();
    }

    public void updateLastMessage(Long messageId, LocalDateTime messageAt) {
        this.lastMessageId = messageId;
        this.lastMessageAt = messageAt;
    }

    public void rename(String name) {
        this.displayName = name;
    }

    private static String generateRandomName() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 헷갈리는 문자 제거
        StringBuilder sb = new StringBuilder("TeenPle-");

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 4; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}