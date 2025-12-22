package com.shu.backend.domain.comment.entity;

import com.shu.backend.domain.comment.enums.CommentStatus;
import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CommentStatus commentStatus = CommentStatus.ACTIVE;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @Column(name = "dislike_count", nullable = false)
    @Builder.Default
    private Integer dislikeCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean anonymous = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @Builder.Default
    private Comment parent = null;

    @Column(nullable = false)
    @Builder.Default
    private int depth = 0;

    //private Integer reportCount = 0;

    public void updateContent(String content) {
        this.content = content;
    }

    public void softDelete() {
        this.commentStatus = CommentStatus.DELETED;
    }




}
