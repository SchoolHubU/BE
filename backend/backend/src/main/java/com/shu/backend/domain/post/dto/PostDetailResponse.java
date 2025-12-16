package com.shu.backend.domain.post.dto;

import com.shu.backend.domain.comment.dto.CommentResponse;
import com.shu.backend.domain.post.entity.Post;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PostDetailResponse {

    private Long postId;
    private String title;
    private String content;
    private Integer viewCount;
    private Boolean anonymous;
    private int likeCount;
    private int dislikeCount;
    private String postStatus;
    private String username;
    private List<CommentResponse> comments;


    public static PostDetailResponse toDto(Post post, List<CommentResponse> comments) {
        return PostDetailResponse.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .viewCount(post.getViewCount())
                .anonymous(post.getAnonymous())
                .likeCount(post.getLikeCount())
                .dislikeCount(post.getDislikeCount())
                .postStatus(post.getPostStatus().name())
                .username(post.getUser().getUsername())
                .comments(comments) // 댓글 목록
                .build();
    }
}
