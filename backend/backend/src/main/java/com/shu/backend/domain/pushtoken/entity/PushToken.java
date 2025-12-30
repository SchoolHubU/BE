package com.shu.backend.domain.pushtoken.entity;

import com.shu.backend.domain.pushtoken.enums.PushPlatform;
import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "push_token",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_push_token_token", columnNames = {"token"})
        },
        indexes = {
                @Index(name = "idx_push_token_user_active", columnList = "user_id,is_active")
        }
)
public class PushToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 512)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PushPlatform platform;

    @Column(name="is_active", nullable = false)
    private Boolean isActive = true;

    @Builder
    private PushToken(Long userId, String token, PushPlatform platform, Boolean isActive) {
        this.userId = userId;
        this.token = token;
        this.platform = platform;
        this.isActive = (isActive != null) ? isActive : true;
    }

    public static PushToken create(Long userId, String token, PushPlatform platform) {
        return PushToken.builder()
                .userId(userId)
                .token(token)
                .platform(platform)
                .isActive(true)
                .build();
    }

    public void activate(Long userId, PushPlatform platform) {
        this.userId = userId; // 같은 기기가 다른 계정으로 로그인할 수 있으므로 갱신
        this.platform = platform;
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
