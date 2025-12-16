package com.shu.backend.domain.board.dto;

import com.shu.backend.domain.board.enums.BoardScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BoardCreateRequest {

    @NotBlank
    private String title;

    private String description;


    private BoardScope scope;

    private Long schoolId;

    private Long regionId;

}
