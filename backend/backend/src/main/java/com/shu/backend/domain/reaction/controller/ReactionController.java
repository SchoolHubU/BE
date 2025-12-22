package com.shu.backend.domain.reaction.controller;

import com.shu.backend.domain.reaction.dto.ReactionApplyRequest;
import com.shu.backend.domain.reaction.dto.ReactionApplyResponse;
import com.shu.backend.domain.reaction.exception.status.ReactionSuccessStatus;
import com.shu.backend.domain.reaction.service.ReactionService;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Reaction",
        description = "좋아요/싫어요 관련 API"
)
@RestController
@RequiredArgsConstructor
public class ReactionController {

    private final ReactionService reactionService;

    @Operation(
            summary = "좋아요/싫어요 적용",
            description = "게시글/댓글에 좋아요/싫어요를 적용합니다. (취소 불가)"
    )
    @PostMapping("/api/reactions/apply")
    public ApiResponse<ReactionApplyResponse> apply(@RequestBody ReactionApplyRequest req,
                                                    @AuthenticationPrincipal User user) {
        Long userId = user.getId();

        ReactionApplyResponse res = reactionService.apply(userId, req);

        return ApiResponse.of(ReactionSuccessStatus.REACTION_APPLY_SUCCESS, res);
    }

}
