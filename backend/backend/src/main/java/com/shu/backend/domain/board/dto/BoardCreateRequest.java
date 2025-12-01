package com.shu.backend.domain.board.dto;

import com.shu.backend.domain.board.Board;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BoardCreateRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Long schoolId;

}
