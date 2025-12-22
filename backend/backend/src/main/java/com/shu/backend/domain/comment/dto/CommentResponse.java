package com.shu.backend.domain.comment.dto;

import com.shu.backend.domain.comment.entity.Comment;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentResponse {

    private Long commentId;      // 댓글 ID
    private String content;      // 댓글 내용
    private String author;       // 작성자 이름
    private int likeCount;       // 좋아요 수
    private int dislikeCount;    // 싫어요 수
    private boolean anonymous;   // 익명 여부
    private int depth;           // 대댓글 깊이
    private Long parentId;       // 부모 댓글 ID (대댓글일 경우)

    // Comment 엔티티를 CommentResponse로 변환
    public static CommentResponse toDto(Comment comment) {

        String content;
        String author;

        //댓글 상태 처리
        switch (comment.getCommentStatus()) {
            case DELETED -> content = "삭제된 댓글입니다.";
            case HIDDEN -> content = "블라인드 처리된 댓글입니다.";
            default -> content = comment.getContent();
        }

        //익명 여부에 따른 author 반환
        if (comment.getAnonymous()) {
            author = "익명";
        } else if (comment.getUser() != null) {
            author = comment.getUser().getNickname();
        } else {
            author = "알 수 없음";
        }

        return CommentResponse.builder()
                .commentId(comment.getId())
                .content(content)
                .author(author)
                .likeCount(comment.getLikeCount())
                .dislikeCount(comment.getDislikeCount())
                .anonymous(comment.getAnonymous())
                .depth(comment.getDepth())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .build();
    }
}