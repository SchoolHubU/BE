package com.shu.backend.domain.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shu.backend.domain.comment.entity.Comment;
import com.shu.backend.global.util.HtmlText;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminCommentResponse {
    private Long commentId;
    private Long authorUserId;
    private String authorLabel;
    private boolean anonymous;
    private String commentStatus;
    private String content;
    private int likeCount;
    private int dislikeCount;
    private int depth;
    private Long parentId;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public static AdminCommentResponse from(Comment comment) {
        Long authorId = comment.getUser() != null ? comment.getUser().getId() : null;
        String label = comment.getUser() != null
                ? comment.getUser().getUsername() + " #" + comment.getUser().getId()
                : "알 수 없음";

        return AdminCommentResponse.builder()
                .commentId(comment.getId())
                .authorUserId(authorId)
                .authorLabel(label)
                .anonymous(false)
                .commentStatus(comment.getCommentStatus().name())
                .content(HtmlText.unescape(comment.getContent()))
                .likeCount(comment.getLikeCount())
                .dislikeCount(comment.getDislikeCount())
                .depth(comment.getDepth())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
