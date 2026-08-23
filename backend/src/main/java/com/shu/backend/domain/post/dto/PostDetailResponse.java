package com.shu.backend.domain.post.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.shu.backend.domain.boardprofile.entity.BoardDisplayProfile;
import com.shu.backend.domain.comment.dto.CommentResponse;
import com.shu.backend.domain.poll.dto.PollResponse;
import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.domain.user.enums.UserRole;
import com.shu.backend.domain.user.support.UserDisplay;
import com.shu.backend.global.util.HtmlText;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Getter
@Builder
public class PostDetailResponse {

    private Long postId;
    private Long authorUserId;
    @JsonProperty("isMine")
    private boolean isMine;
    private Long authorId;   // 게시글 작성자 userId (채팅 유입용)
    private String title;
    private String content;
    private Integer viewCount;
    private Boolean anonymous;
    private int likeCount;
    private int dislikeCount;
    @JsonProperty("likedByMe")
    private boolean likedByMe;
    @JsonProperty("dislikedByMe")
    private boolean dislikedByMe;
    private String postStatus;
    private String username;
    private String authorProfileImageUrl;
    private boolean authorDeleted;
    private boolean canChatWithAuthor;
    private boolean canReportAuthor;
    private boolean canBlockAuthor;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    private List<CommentResponse> comments;

    @JsonProperty("createdAtMs")
    public Long getCreatedAtMs() {
        return createdAt != null
                ? createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                : null;
    }
    private List<PostMediaResponse> mediaList;
    @JsonProperty("isBookmarked")
    private boolean isBookmarked;
    private PollResponse poll;


    public static PostDetailResponse toDto(Post post, BoardDisplayProfile boardProfile, String boardProfileImageUrl, List<CommentResponse> comments, List<PostMediaResponse> mediaList, Long currentUserId, boolean isBookmarked, PollResponse poll, boolean likedByMe, boolean dislikedByMe) {
        boolean authorDeleted = UserDisplay.isDeleted(post.getUser());
        boolean mine = post.getUser().getId().equals(currentUserId);
        boolean canActOnAuthor = !authorDeleted && !mine;
        boolean adminAuthor = !authorDeleted && post.getUser().getRole() == UserRole.ADMIN;
        String profileImageUrl = authorDeleted ? null : boardProfileImageUrl;
        String username = authorDeleted
                ? UserDisplay.DELETED_USER_NAME
                : adminAuthor
                ? post.getUser().getNickname()
                : boardProfile.getDisplayName();
        if (adminAuthor) {
            profileImageUrl = post.getUser().getProfileImageUrl();
        }
        if (profileImageUrl != null && !profileImageUrl.startsWith("http")) {
            profileImageUrl = null;
        }
        return PostDetailResponse.builder()
                .postId(post.getId())
                .authorUserId(authorDeleted ? null : post.getUser().getId())
                .isMine(!authorDeleted && mine)
                .authorId(authorDeleted ? null : post.getUser().getId())
                .title(HtmlText.unescape(post.getTitle()))
                .content(HtmlText.unescape(post.getContent()))
                .viewCount(post.getViewCount())
                .anonymous(false)
                .likeCount(post.getLikeCount())
                .dislikeCount(post.getDislikeCount())
                .likedByMe(likedByMe)
                .dislikedByMe(dislikedByMe)
                .postStatus(post.getPostStatus().name())
                .username(username)
                .authorProfileImageUrl(profileImageUrl)
                .authorDeleted(authorDeleted)
                .canChatWithAuthor(canActOnAuthor)
                .canReportAuthor(canActOnAuthor)
                .canBlockAuthor(canActOnAuthor)
                .createdAt(post.getCreatedAt())
                .comments(comments)
                .mediaList(mediaList)
                .isBookmarked(isBookmarked)
                .poll(poll)
                .build();
    }
}
