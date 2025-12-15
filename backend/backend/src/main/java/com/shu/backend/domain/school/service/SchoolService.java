package com.shu.backend.domain.school.service;

import com.shu.backend.domain.board.dto.BoardResponse;
import com.shu.backend.domain.board.entity.Board;
import com.shu.backend.domain.board.exception.BoardException;
import com.shu.backend.domain.board.exception.status.BoardErrorStatus;
import com.shu.backend.domain.board.repository.BoardRepository;
import com.shu.backend.domain.post.dto.PostResponse;
import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.domain.post.service.PostService;
import com.shu.backend.domain.region.entity.Region;
import com.shu.backend.domain.region.exception.RegionException;
import com.shu.backend.domain.region.exception.status.RegionErrorStatus;
import com.shu.backend.domain.region.repository.RegionRepository;
import com.shu.backend.domain.school.dto.SchoolCreateRequest;
import com.shu.backend.domain.school.dto.SchoolDetailResponse;
import com.shu.backend.domain.school.entity.School;
import com.shu.backend.domain.school.exception.SchoolException;
import com.shu.backend.domain.school.exception.status.SchoolErrorStatus;
import com.shu.backend.domain.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.query.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SchoolService {

    private final RegionRepository regionRepository;
    private final SchoolRepository schoolRepository;
    private final BoardRepository boardRepository;
    private final PostService postService;

    /*
    학교 생성
     */
    @Transactional
    public Long createSchool(SchoolCreateRequest schoolCreateRequest){
        Region region = regionRepository.findById(schoolCreateRequest.getRegionId())
                .orElseThrow(() -> new RegionException(RegionErrorStatus.REGION_NOT_FOUND));

        School school = School.builder()
                .name(schoolCreateRequest.getName())
                .region(region)
                .build();

        schoolRepository.save(school);
        return school.getId();
    }

    /*
    지역별 학교 검색
     */
    public List<School> getSchoolsByRegion(Long regionId, String keyword){
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new RegionException(RegionErrorStatus.REGION_NOT_FOUND));

        if (keyword == null || keyword.isBlank()){
            return schoolRepository.findByRegionIdOrderByNameAsc(regionId);
        }
        return schoolRepository.findSchoolsByRegionAndName(regionId, keyword);
    }

    /*
    특정 학교 상세정보 조회 (기본으로 자유게시판 조회)
     */
    public SchoolDetailResponse getSchoolDetail(Long schoolId, String boardTitle, Pageable pageable){

        // 학교 조회
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new SchoolException(SchoolErrorStatus.SCHOOL_NOT_FOUND));

        // 게시판 조회 (기본으로 자유게시판 조회)
        Board freeBoard = boardRepository.findBySchoolIdAndTitle(schoolId, boardTitle)
                .orElseThrow(() -> new BoardException(BoardErrorStatus.BOARD_NOT_FOUND));

        // 학교의 게시판 목록 조회 (해당 학교 게시판 상세 조회 버튼)
        List<Board> boards = boardRepository.findBySchoolId(schoolId);

        // 자유게시판의 글 Slice 객체로 조회
        Slice<Post> posts = postService.getPostsByBoardId(freeBoard.getId(), pageable);

        // 게시글을 DTO로 변환
        List<PostResponse> postResponses = posts.getContent().stream()
                .map(post -> PostResponse.toDto(post, posts.getNumberOfElements()))
                .collect(Collectors.toList());

        // 6. 게시판 정보만 따로 모아서 반환할 준비 (BoardResponse) -> 이제 빌더 패턴 사용!
        List<BoardResponse> boardResponses = boards.stream()
                .map(board -> BoardResponse.toDto(board))  // 기존의 BoardResponse.toDto() 사용
                .collect(Collectors.toList());

        return new SchoolDetailResponse(school.getId(), school.getName(), boardResponses, postResponses, posts.hasNext());

        //TODO: DTO 관계 정리, SchoolController, 페이징 개념 다시 정리 및 Post 구현


    }


}
