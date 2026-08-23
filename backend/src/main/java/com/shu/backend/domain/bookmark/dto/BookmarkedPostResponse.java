package com.shu.backend.domain.bookmark.dto;

import com.shu.backend.domain.bookmark.entity.Bookmark;
import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.global.util.HtmlText;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookmarkedPostResponse {

    private Long postId;
    private String title;
    private String content;
    private String postStatus;
    private int likeCount;
    private int commentCount;
    private String createdAt;
    private String boardTitle;

    public static BookmarkedPostResponse from(Bookmark bookmark, int commentCount) {
        Post post = bookmark.getPost();
        return BookmarkedPostResponse.builder()
                .postId(post.getId())
                .title(HtmlText.unescape(post.getTitle()))
                .content(HtmlText.unescape(post.getContent()))
                .postStatus(post.getPostStatus().name())
                .likeCount(post.getLikeCount())
                .commentCount(commentCount)
                .createdAt(post.getCreatedAt() != null ? post.getCreatedAt().toString() : null)
                .boardTitle(post.getBoard() != null ? post.getBoard().getTitle() : null)
                .build();
    }
}
