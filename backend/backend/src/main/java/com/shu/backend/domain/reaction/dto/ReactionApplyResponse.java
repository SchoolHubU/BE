package com.shu.backend.domain.reaction.dto;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReactionApplyResponse {
    private Long targetId;
    private String targetType;     // POST/COMMENT
    private boolean liked;
    private boolean disliked;
    private boolean applied;       // 이번 요청으로 true로 바뀌었는지

    private int likeCount;
    private int dislikeCount;
}