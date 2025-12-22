package com.shu.backend.domain.reaction.entity;

import com.shu.backend.domain.reaction.enums.ReactionTargetType;
import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "reaction",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_reaction_user_target",
                        columnNames = {"user_id", "target_type", "target_id"}
                )
        }
)
public class Reaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //(POST, COMMENT)
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private ReactionTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean liked = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean disliked = false;

    // fk로 x
    @Column(name = "user_id", nullable = false)
    private Long userId;


    // ====== 생성 메서드 ======
    public static Reaction create(ReactionTargetType targetType, Long targetId, Long userId) {
        return Reaction.builder()
                .targetType(targetType)
                .targetId(targetId)
                .userId(userId)
                .liked(false)
                .disliked(false)
                .build();
    }


    public boolean applyLike() {
        if (this.liked) return false;
        this.liked = true;
        return true;
    }

    public boolean applyDislike() {
        if (this.disliked) return false;
        this.disliked = true;
        return true;
    }


}
