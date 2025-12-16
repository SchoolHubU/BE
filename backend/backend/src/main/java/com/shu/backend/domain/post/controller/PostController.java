package com.shu.backend.domain.post.controller;

import com.shu.backend.domain.post.dto.PostCreateRequest;
import com.shu.backend.domain.post.dto.PostUpdateRequest;
import com.shu.backend.domain.post.exception.status.PostSuccessStatus;
import com.shu.backend.domain.post.service.PostService;
import com.shu.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Post", description = "게시글 관련 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(
            summary = "게시글 생성",
            description = "특정 게시판에 게시글을 작성합니다."
    )
    @PostMapping("/boards/{boardId}/posts")
    public ApiResponse<Long> createPost(
            @PathVariable Long boardId,
            @Valid @RequestBody PostCreateRequest req
    ) {
        Long postId = postService.createPost(boardId, req);

        return ApiResponse.of(PostSuccessStatus.POST_CREATE_SUCCESS, postId);
    }


    @Operation(
            summary = "게시글 수정",
            description = "특정 게시글의 내용을 수정합니다."
    )
    @PatchMapping("/posts/{postId}")
    public ApiResponse<Long> updatePost(
        @PathVariable Long postId,
        @Valid @RequestBody PostUpdateRequest req
    ){
        Long id = postService.updatePost(postId, req);

        return ApiResponse.of(PostSuccessStatus.POST_UPDATE_SUCCESS, id);
    }

    @Operation(
            summary = "게시글 삭제",
            description = "특정 게시글을 삭제합니다.(soft delete)"
    )
    @DeleteMapping("/posts/{postId}")
    public ApiResponse<Long> deletePost(
            @PathVariable Long postId,
            @RequestParam Long userId
    ){
        Long id = postService.deletePost(postId, userId);

        return ApiResponse.of(PostSuccessStatus.POST_DELETE_SUCCESS, id);
    }


}
