package com.shu.backend.domain.penalty.controller;

import com.shu.backend.domain.penalty.dto.PenaltyDTO;
import com.shu.backend.domain.penalty.service.PenaltyService;
import com.shu.backend.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Penalty",
        description = "제재 관련 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/penalties")
public class PenaltyController {

    private final PenaltyService penaltyService;

    @Operation(
            summary = "내 활성 제재 조회",
            description = "현재 로그인한 사용자가 제재 중인지(만료 전) 여부와 만료 시각을 조회합니다."
    )
    @GetMapping("/me")
    public PenaltyDTO.MyActiveResponse myActive(@AuthenticationPrincipal User user) {
        return penaltyService.getMyActivePenalty(user.getId());
    }

    @Operation(
            summary = "내 제재 이력 조회",
            description = "현재 로그인한 사용자의 제재 이력을 페이지 단위로 조회합니다. (설정 화면용)"
    )
    @GetMapping("/me/history")
    public Page<PenaltyDTO.SummaryResponse> myHistory(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return penaltyService.getPenaltiesByUser(user.getId(), pageable);
    }
}
