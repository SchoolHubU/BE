package com.shu.backend.domain.admin.controller;

import com.shu.backend.domain.admin.dto.AdminBoardResponse;
import com.shu.backend.domain.admin.dto.AdminModerationRequest;
import com.shu.backend.domain.admin.dto.AdminPostDetailResponse;
import com.shu.backend.domain.admin.dto.AdminPostPageResponse;
import com.shu.backend.domain.admin.dto.AdminSchoolResponse;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.admin.service.AdminContentService;
import com.shu.backend.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/content")
public class AdminContentController {

    private final AdminContentService adminContentService;

    @GetMapping("/schools")
    public ApiResponse<Page<AdminSchoolResponse>> searchSchools(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.onSuccess(adminContentService.searchSchools(keyword, page, size));
    }

    @GetMapping("/schools/{schoolId}/boards")
    public ApiResponse<List<AdminBoardResponse>> getBoardsBySchool(
            @PathVariable Long schoolId
    ) {
        return ApiResponse.onSuccess(adminContentService.getBoardsBySchool(schoolId));
    }

    @GetMapping("/boards/{boardId}/posts")
    public ApiResponse<AdminPostPageResponse> getPostsByBoard(
            @PathVariable Long boardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.onSuccess(adminContentService.getPostsByBoard(boardId, page, size));
    }

    @GetMapping("/posts/{postId}")
    public ApiResponse<AdminPostDetailResponse> getPostDetail(
            @PathVariable Long postId,
            @AuthenticationPrincipal User admin
    ) {
        return ApiResponse.onSuccess(adminContentService.getPostDetail(postId, admin.getId()));
    }

    @PatchMapping("/posts/{postId}/hide")
    public ApiResponse<AdminPostDetailResponse> hidePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal User admin,
            @Valid @RequestBody AdminModerationRequest request
    ) {
        return ApiResponse.onSuccess(adminContentService.hidePost(postId, admin.getId(), request.getReason()));
    }

    @PatchMapping("/posts/{postId}/restore")
    public ApiResponse<AdminPostDetailResponse> restorePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal User admin,
            @Valid @RequestBody AdminModerationRequest request
    ) {
        return ApiResponse.onSuccess(adminContentService.restorePost(postId, admin.getId(), request.getReason()));
    }

    @PatchMapping("/comments/{commentId}/hide")
    public ApiResponse<AdminPostDetailResponse> hideComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal User admin,
            @Valid @RequestBody AdminModerationRequest request
    ) {
        return ApiResponse.onSuccess(adminContentService.hideComment(commentId, admin.getId(), request.getReason()));
    }

    @PatchMapping("/comments/{commentId}/restore")
    public ApiResponse<AdminPostDetailResponse> restoreComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal User admin,
            @Valid @RequestBody AdminModerationRequest request
    ) {
        return ApiResponse.onSuccess(adminContentService.restoreComment(commentId, admin.getId(), request.getReason()));
    }
}
