package com.shu.backend.domain.school.controller;

import com.shu.backend.domain.school.dto.SchoolCreateRequest;
import com.shu.backend.domain.school.dto.SchoolDetailResponse;
import com.shu.backend.domain.school.dto.SchoolResponse;
import com.shu.backend.domain.school.entity.School;
import com.shu.backend.domain.school.exception.status.SchoolSuccessStatus;
import com.shu.backend.domain.school.service.SchoolService;
import com.shu.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "School", description = "학교 관련 API")
@RestController
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolService schoolService;

    @Operation(
            summary = "학교생성",
            description = "학교명을 입력 받아 학교를 생성합니다."
    )
    @PostMapping(value = "/admin/schools", consumes = "application/json", produces = "application/json")
    public ApiResponse<Long> createSchool(@Valid @RequestBody SchoolCreateRequest schoolCreateRequest) {
        Long id = schoolService.createSchool(schoolCreateRequest);

        return ApiResponse.of(SchoolSuccessStatus.SCHOOL_CREATE_SUCCESS, id);
    }

    @Operation(
            summary = "지역별 학교 조회",
            description = "지역별로 학교를 조회할 수 있으며, 학교명 검색 기능을 통해 특정 학교를 조회할 수 있습니다."
    )
    @GetMapping(value = "/api/regions/{regionId}/schools")
    public ApiResponse<List<SchoolResponse>> getSchoolsByRegion(
            @PathVariable Long regionId,
            @RequestParam(required = false) String keyword
    ){
        List<School> schools = schoolService.getSchoolsByRegion(regionId, keyword);

        List<SchoolResponse> responses = schools.stream()
                .map(SchoolResponse::toDto)
                .toList();

        return ApiResponse.of(SchoolSuccessStatus.SCHOOL_FOUND, responses);

    }

    @Operation(
            summary = "학교 상세 조회",
            description = "특정 학교 접속 시의 메인 화면이며, 학교 상세 정보와 기본 화면으로 자유 게시판의 글 목록을 조회합니다."
    )
    @GetMapping(value = "/api/schools/{schoolId}")
    public ApiResponse<SchoolDetailResponse> getSchoolDetail(
            @PathVariable Long schoolId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        Pageable pageable = PageRequest.of(page, size);

        SchoolDetailResponse response = schoolService.getSchoolDetail(schoolId, "자유게시판", pageable);

        return ApiResponse.of(SchoolSuccessStatus.SCHOOL_FOUND, response);

    }
}
