package com.shu.backend.domain.region.controller;

import com.shu.backend.domain.region.dto.RegionCreateRequest;
import com.shu.backend.domain.region.exception.status.RegionSuccessStatus;
import com.shu.backend.domain.region.service.RegionService;
import com.shu.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Region", description = "지역 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("admin/regions")
public class RegionController {

    private final RegionService regionService;

    @PostMapping
    public ApiResponse<Long> createRegion(@Valid RegionCreateRequest request) {
        Long regionId = regionService.createRegion(request);

        return ApiResponse.of(RegionSuccessStatus.REGION_CREATE_SUCCESS, regionId);
    }
}
