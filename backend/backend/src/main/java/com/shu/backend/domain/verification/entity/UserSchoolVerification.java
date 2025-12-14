package com.shu.backend.domain.verification.entity;

import com.shu.backend.domain.school.School;
import com.shu.backend.domain.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
public class UserSchoolVerification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private LocalDateTime verified_at;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Builder
    public UserSchoolVerification(LocalDateTime verified_at, User user, School school) {
        this.verified_at = verified_at;
        this.user = user;
        this.school = school;
    }
}
