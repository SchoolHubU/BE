package com.shu.backend.domain.post.dto;

import com.shu.backend.domain.post.entity.Post;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostResponse {

    private Long id;
    private String title;
    private String content;
    private String postStatus; // 게시글 상태
    private Integer viewCount; // 조회수
    private Boolean anonymous; // 익명 여부
    private int likeCount; // 좋아요 수
    private int dislikeCount; // 싫어요 수
    private Long boardId;  // 게시판 ID
    private Long userId;   // 사용자 ID
    private String username; // 사용자 이름
    private int commentCount; // 댓글 수 (선택적)

    // Post 엔티티를 PostResponse로 변환하는 메서드
    public static PostResponse toDto(Post post, int commentCount) {
        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .postStatus(post.getPostStatus().name()) // Enum을 문자열로 변환
                .viewCount(post.getViewCount())
                .anonymous(post.getAnonymous())
                .likeCount(post.getLikeCount())
                .dislikeCount(post.getDislikeCount())
                .boardId(post.getBoard().getId()) // 게시판 ID
                .userId(post.getUser().getId()) // 사용자 ID
                .username(post.getUser().getUsername()) // 사용자 이름
                .commentCount(commentCount) // 댓글 수 추가
                .build();
    }
}