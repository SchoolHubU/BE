package com.shu.backend.domain.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class BoardCreateRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Long schoolId;

}
