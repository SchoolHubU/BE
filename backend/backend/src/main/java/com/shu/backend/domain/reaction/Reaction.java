package com.shu.backend.domain.reaction;

import com.shu.backend.domain.reaction.enums.ReactionTargetType;
import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
    private Boolean liked = false;

    @Column(nullable = false)
    private Boolean disliked = false;


    // fk로 x
    @Column(name = "user_id", nullable = false)
    private Long userId;


    // ====== 생성 메서드 ======
    public static Reaction create(
            ReactionTargetType targetType,
            Long targetId,
            Long userId,
            boolean liked,
            boolean disliked
    ) {
        Reaction r = new Reaction();
        r.targetType = targetType;
        r.targetId = targetId;
        r.userId = userId;
        r.liked = liked;
        r.disliked = disliked;
        return r;
    }

    // ====== 기능 메서드 ======

    public void updateLike(boolean liked) {
        this.liked = liked;
        if (liked) this.disliked = false; // 좋아요 누르면 싫어요는 자동 해제
    }

    public void updateDislike(boolean disliked) {
        this.disliked = disliked;
        if (disliked) this.liked = false; // 싫어요 누르면 좋아요는 자동 해제
    }

    public void clear() {
        this.liked = false;
        this.disliked = false;
    }

}
