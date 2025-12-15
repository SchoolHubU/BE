package com.shu.backend.domain.board.dto;

import com.shu.backend.domain.board.entity.Board;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardResponse {

    private Long id;

    private String title;

    private String description;

    private boolean active;

    public static BoardResponse toDto(Board board) {
        return BoardResponse.builder()
                .id(board.getId())
                .title(board.getTitle())
                .description(board.getDescription())
                .active(board.isActive())
                .build();
    }
}
