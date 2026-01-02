package com.shu.backend.domain.penalty.controller;


import com.shu.backend.domain.penalty.dto.PenaltyDTO;
import com.shu.backend.domain.penalty.service.PenaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Admin Penalty",
        description = "관리자 제재 조회 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/penalties")
public class AdminPenaltyController {

    private final PenaltyService penaltyService;

    @Operation(
            summary = "유저 제재 목록 조회",
            description = "특정 유저의 제재 이력을 페이지로 조회합니다."
    )
    @GetMapping
    public Page<PenaltyDTO.SummaryResponse> listByUser(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return penaltyService.getPenaltiesByUser(userId, pageable);
    }
}
