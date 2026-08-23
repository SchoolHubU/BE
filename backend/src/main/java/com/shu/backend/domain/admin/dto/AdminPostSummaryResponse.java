package com.shu.backend.domain.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.global.util.HtmlText;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminPostSummaryResponse {
    private Long postId;
    private String title;
    private String contentPreview;
    private String postStatus;
    private boolean anonymous;
    private Long authorUserId;
    private String authorLabel;
    private Long boardId;
    private String boardTitle;
    private Long schoolId;
    private String schoolName;
    private Long regionId;
    private String regionName;
    private int viewCount;
    private int likeCount;
    private int dislikeCount;
    private int commentCount;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    public static AdminPostSummaryResponse from(Post post) {
        String content = HtmlText.unescapeOrEmpty(post.getContent());
        String preview = content.length() > 120 ? content.substring(0, 120) : content;
        Long authorId = post.getUser() != null ? post.getUser().getId() : null;
        String authorLabel = post.getUser() == null
                ? "알 수 없음"
                : post.getUser().getUsername() + " #" + post.getUser().getId();

        return AdminPostSummaryResponse.builder()
                .postId(post.getId())
                .title(HtmlText.unescape(post.getTitle()))
                .contentPreview(preview)
                .postStatus(post.getPostStatus().name())
                .anonymous(false)
                .authorUserId(authorId)
                .authorLabel(authorLabel)
                .boardId(post.getBoard().getId())
                .boardTitle(post.getBoard().getTitle())
                .schoolId(post.getBoard().getSchool() != null ? post.getBoard().getSchool().getId() : null)
                .schoolName(post.getBoard().getSchool() != null ? post.getBoard().getSchool().getName() : null)
                .regionId(post.getBoard().getRegion() != null ? post.getBoard().getRegion().getId() : null)
                .regionName(post.getBoard().getRegion() != null ? post.getBoard().getRegion().getName() : null)
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .dislikeCount(post.getDislikeCount())
                .commentCount(post.getCommentCount())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
