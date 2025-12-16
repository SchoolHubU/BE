package com.shu.backend.domain.region.dto;

import com.shu.backend.domain.region.entity.Region;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegionCreateRequest {

    @NotNull
    private String name;


    public static RegionCreateRequest toDto(Region region){
        return RegionCreateRequest.builder()
                .name(region.getName())
                .build();
    }

}
