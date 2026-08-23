package com.shu.backend.domain.post.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.domain.user.enums.UserStatus;
import com.shu.backend.domain.user.support.UserDisplay;
import com.shu.backend.global.util.HtmlText;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Getter
@Builder
public class PostResponse {

    private Long id;
    private String title;
    private String content;
    private String postStatus;
    private Integer viewCount;
    private Boolean anonymous;
    private int likeCount;
    private int dislikeCount;
    private Long boardId;
    private Long userId;
    private String username;
    private String authorProfileImageUrl;
    private boolean authorDeleted;
    private int commentCount;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    @Builder.Default
    private List<PostMediaResponse> mediaList = List.of();
    private boolean hasPoll;

    @JsonProperty("createdAtMs")
    public Long getCreatedAtMs() {
        return createdAt != null
                ? createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                : null;
    }

    public PostResponse withMedia(List<PostMediaResponse> mediaList) {
        return PostResponse.builder()
                .id(this.id)
                .title(this.title)
                .content(this.content)
                .postStatus(this.postStatus)
                .viewCount(this.viewCount)
                .anonymous(this.anonymous)
                .likeCount(this.likeCount)
                .dislikeCount(this.dislikeCount)
                .boardId(this.boardId)
                .userId(this.userId)
                .username(this.username)
                .authorProfileImageUrl(this.authorProfileImageUrl)
                .authorDeleted(this.authorDeleted)
                .commentCount(this.commentCount)
                .createdAt(this.createdAt)
                .mediaList(mediaList)
                .hasPoll(this.hasPoll)
                .build();
    }

    public PostResponse withBoardProfile(String displayName, String profileImageUrl) {
        return PostResponse.builder()
                .id(this.id)
                .title(this.title)
                .content(this.content)
                .postStatus(this.postStatus)
                .viewCount(this.viewCount)
                .anonymous(false)
                .likeCount(this.likeCount)
                .dislikeCount(this.dislikeCount)
                .boardId(this.boardId)
                .userId(this.userId)
                .username(displayName)
                .authorProfileImageUrl(profileImageUrl)
                .authorDeleted(this.authorDeleted)
                .commentCount(this.commentCount)
                .createdAt(this.createdAt)
                .mediaList(this.mediaList)
                .hasPoll(this.hasPoll)
                .build();
    }

    public static PostResponse toDto(Post post, int commentCount) {
        boolean authorDeleted = UserDisplay.isDeleted(post.getUser());
        String username = UserDisplay.usernameOrDeleted(post.getUser());
        return PostResponse.builder()
                .id(post.getId())
                .title(HtmlText.unescape(post.getTitle()))
                .content(HtmlText.unescape(post.getContent()))
                .postStatus(post.getPostStatus().name())
                .viewCount(post.getViewCount())
                .anonymous(false)
                .likeCount(post.getLikeCount())
                .dislikeCount(post.getDislikeCount())
                .boardId(post.getBoard().getId())
                .userId(authorDeleted ? null : post.getUser().getId())
                .username(username)
                .authorDeleted(authorDeleted)
                .commentCount(commentCount)
                .createdAt(post.getCreatedAt())
                .hasPoll(false)
                .build();
    }

    public static PostResponse fromRow(Object[] r) {
        Long id = (Long) r[0];
        String title = HtmlText.unescape((String) r[1]);
        String content = HtmlText.unescape((String) r[2]);
        String postStatus = (r[3] instanceof Enum<?> e) ? e.name() : String.valueOf(r[3]);
        Integer viewCount = (Integer) r[4];
        Boolean anonymous = (Boolean) r[5];
        Integer likeCount = (Integer) r[6];
        Integer dislikeCount = (Integer) r[7];
        Long boardId = (Long) r[8];
        Long userId = (Long) r[9];
        String username = (String) r[10];
        int commentCount = (r[11] == null) ? 0 : ((Number) r[11]).intValue();

        String rawProfileUrl = (r.length > 12) ? (String) r[12] : null;
        String boardDisplayName = (r.length > 16) ? (String) r[16] : null;
        String boardProfileImageUrl = (r.length > 17) ? (String) r[17] : null;
        String authorProfileImageUrl = null;
        if (boardProfileImageUrl != null && boardProfileImageUrl.startsWith("http")) {
            authorProfileImageUrl = boardProfileImageUrl;
        }

        LocalDateTime createdAt = (r.length > 13) ? (LocalDateTime) r[13] : null;
        Boolean hasPoll = (r.length > 14) ? (Boolean) r[14] : false;
        boolean authorDeleted = r.length > 15 && r[15] == UserStatus.DELETED;
        if (authorDeleted) {
            userId = null;
            username = UserDisplay.DELETED_USER_NAME;
            authorProfileImageUrl = null;
        } else if (boardDisplayName != null && !boardDisplayName.isBlank()) {
            username = boardDisplayName;
        }

        return PostResponse.builder()
                .id(id)
                .title(title)
                .content(content)
                .postStatus(postStatus)
                .viewCount(viewCount)
                .anonymous(false)
                .likeCount(likeCount == null ? 0 : likeCount)
                .dislikeCount(dislikeCount == null ? 0 : dislikeCount)
                .boardId(boardId)
                .userId(userId)
                .username(username)
                .authorProfileImageUrl(authorProfileImageUrl)
                .authorDeleted(authorDeleted)
                .commentCount(commentCount)
                .createdAt(createdAt)
                .hasPoll(Boolean.TRUE.equals(hasPoll))
                .build();
    }
}
