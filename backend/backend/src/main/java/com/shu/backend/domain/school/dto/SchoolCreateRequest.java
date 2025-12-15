package com.shu.backend.domain.school.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SchoolCreateRequest {

    @NotNull
    private String name;


    @NotNull
    private Long regionId;
}
