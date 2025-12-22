package com.shu.backend.domain.comment.controller;

import com.shu.backend.domain.comment.dto.CommentCreateRequest;
import com.shu.backend.domain.comment.dto.CommentUpdateRequest;
import com.shu.backend.domain.comment.exception.status.CommentSuccessStatus;
import com.shu.backend.domain.comment.service.CommentService;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Comment",
        description = "댓글 관련 API"
)
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(
            summary = "댓글 생성",
            description = "특정 게시판에 댓글을 작성합니다."
    )
    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<Long> createComment(@PathVariable Long postId,
                                           @RequestBody CommentCreateRequest req,
                                           @AuthenticationPrincipal User user) {

        Long userId = user.getId();

        Long commentId = commentService.createComment(postId, userId, req);

        return ApiResponse.of(CommentSuccessStatus.COMMENT_CREATE_SUCCESS, commentId);
    }

    @Operation(
            summary = "댓글 수정",
            description = "댓글의 내용을 수정합니다."
    )
    @PatchMapping("/comments/{commentId}")
    public ApiResponse<Long> updateComment(@PathVariable Long commentId,
                                           @RequestBody CommentUpdateRequest req,
                                           @AuthenticationPrincipal User user) {
        Long userId = user.getId();

        Long updatedCommentId = commentService.updateComment(commentId, userId, req);

        return ApiResponse.of(CommentSuccessStatus.COMMENT_UPDATE_SUCCESS, updatedCommentId);
    }

    @Operation(
            summary = "댓글 삭제",
            description = "댓글을 삭제합니다 (soft delete)"
    )
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Long> deleteComment(@PathVariable Long commentId,
                                           @AuthenticationPrincipal User user) {
        Long userId = user.getId();

        Long deletedCommentId = commentService.delete(commentId, userId);

        return ApiResponse.of(CommentSuccessStatus.COMMENT_DELETE_SUCCESS, deletedCommentId);
    }


}
