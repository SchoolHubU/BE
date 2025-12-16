package com.shu.backend.domain.school.dto;

import com.shu.backend.domain.school.entity.School;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SchoolResponse {

    private Long id;

    private String name;

    public static SchoolResponse toDto(School school) {
        return SchoolResponse.builder()
                .id(school.getId())
                .name(school.getName())
                .build();
    }
}
