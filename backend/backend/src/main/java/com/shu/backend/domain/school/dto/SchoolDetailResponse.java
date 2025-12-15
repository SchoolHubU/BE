package com.shu.backend.domain.school.dto;

import com.shu.backend.domain.board.dto.BoardResponse;
import com.shu.backend.domain.post.dto.PostResponse;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class SchoolDetailResponse {

    private Long schoolId;
    private String name;
    private String description;
    private List<BoardResponse> boards; // 게시판 목록
    private List<PostResponse> posts;   // 게시글 목록
    private boolean hasNext;  // 다음 페이지 여부

    public SchoolDetailResponse(Long schoolId, String name, List<BoardResponse> boards, List<PostResponse> posts, boolean hasNext) {
        this.schoolId = schoolId;
        this.name = name;
        this.description = description;
        this.boards = boards;
        this.posts = posts;
        this.hasNext = hasNext;
    }
}
