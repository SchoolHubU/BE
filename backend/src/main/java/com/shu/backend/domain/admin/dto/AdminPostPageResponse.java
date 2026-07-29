package com.shu.backend.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 관리자 게시판의 게시글 목록과 상태별 전체 집계를 함께 반환한다.
 *
 * 기존 클라이언트가 사용하던 Page 응답의 content, number, size 등의 필드는
 * 최상위에 그대로 유지하고 집계 필드만 추가해 배포 순서와 무관하게 호환된다.
 */
@Getter
@Builder
public class AdminPostPageResponse {

    private List<AdminPostSummaryResponse> content;
    private int number;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean empty;

    /** 삭제 게시글을 제외한 전체 게시글 수. */
    private long totalCount;

    /** 사용자에게 노출되는 ACTIVE 게시글 수. */
    private long visibleCount;

    /** 전체에 포함되지만 사용자에게 노출되지 않는 HIDDEN 게시글 수. */
    private long hiddenCount;

    public static AdminPostPageResponse from(
            Page<AdminPostSummaryResponse> page,
            long visibleCount,
            long hiddenCount
    ) {
        return AdminPostPageResponse.builder()
                .content(page.getContent())
                .number(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .totalCount(visibleCount + hiddenCount)
                .visibleCount(visibleCount)
                .hiddenCount(hiddenCount)
                .build();
    }
}
