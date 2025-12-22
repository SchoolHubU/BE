package com.shu.backend.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class CommentCreateRequest {

    @NotNull
    private String content;

    private Boolean anonymous = true;

    private Long parentId = null;
}