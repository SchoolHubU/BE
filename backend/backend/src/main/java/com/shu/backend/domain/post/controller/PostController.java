package com.shu.backend.domain.post.controller;

import com.shu.backend.domain.post.dto.PostCreateRequest;
import com.shu.backend.domain.post.dto.PostDetailResponse;
import com.shu.backend.domain.post.dto.PostResponse;
import com.shu.backend.domain.post.dto.PostUpdateRequest;
import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.domain.post.exception.status.PostSuccessStatus;
import com.shu.backend.domain.post.service.PostService;
import com.shu.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.data.web.PageableDefault;
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

    @Operation(
            summary = "게시글 상세 조회",
            description = "특정 게시글을 댓글을 포함하여 상세 조회합니다."
    )
    @GetMapping("/posts/{postId}")
    public ApiResponse<PostDetailResponse> getPostDetail(
            @PathVariable Long postId
    ){
        PostDetailResponse postDetail = postService.getPostDetail(postId);

        return ApiResponse.onSuccess(postDetail);
    }

    @Operation(
            summary = "게시글 목록 조회",
            description = "특정 게시판의 게시글을 페이징하여 모두 조회합니다."
    )

    @GetMapping("/boards/{boardId}/posts")
    public ApiResponse<Slice<PostResponse>> getPostsByBoardId(
            @PathVariable Long boardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        Pageable pageable = PageRequest.of(page, size, JpaSort.unsafe(Sort.Direction.fromString(sortDirection), sortBy));

        Slice<PostResponse> postResponses = postService.getPostsByBoardId(boardId, pageable);
        return ApiResponse.onSuccess(postResponses);
    }


}
