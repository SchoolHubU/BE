package com.shu.backend.domain.user;

import com.shu.backend.domain.school.School;
import com.shu.backend.domain.user.enums.UserRole;
import com.shu.backend.domain.user.enums.UserStatus;
import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "nickname", nullable = false, unique = true)
    private String nickname;

    @Column(name = "profile_image_url")
    private String profileImageUrl = "default_profile.png";

    @Column(name = "is_write_banned", nullable = false)
    private Boolean isWriteBanned = false;

    @Column(name = "write_ban_expired_at")
    private LocalDateTime writeBanExpiredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;



}
