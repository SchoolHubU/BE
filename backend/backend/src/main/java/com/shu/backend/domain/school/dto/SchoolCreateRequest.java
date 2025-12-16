package com.shu.backend.domain.school.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolCreateRequest {

    @NotNull
    private String name;


    @NotNull
    private Long regionId;


}
