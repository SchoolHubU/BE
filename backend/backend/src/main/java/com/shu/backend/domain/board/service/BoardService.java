package com.shu.backend.domain.board.service;

import com.shu.backend.domain.board.dto.BoardCreateRequest;
import com.shu.backend.domain.board.dto.BoardResponse;
import com.shu.backend.domain.board.entity.Board;
import com.shu.backend.domain.board.enums.BoardScope;
import com.shu.backend.domain.board.exception.BoardException;
import com.shu.backend.domain.board.exception.status.BoardErrorStatus;
import com.shu.backend.domain.board.repository.BoardRepository;
import com.shu.backend.domain.region.entity.Region;
import com.shu.backend.domain.region.exception.RegionException;
import com.shu.backend.domain.region.exception.status.RegionErrorStatus;
import com.shu.backend.domain.region.repository.RegionRepository;
import com.shu.backend.domain.school.entity.School;
import com.shu.backend.domain.school.exception.SchoolException;
import com.shu.backend.domain.school.exception.status.SchoolErrorStatus;
import com.shu.backend.domain.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final SchoolRepository schoolRepository;
    private final RegionRepository regionRepository;

    /*
    * 게시판 생성
    * */
    @Transactional
    public Long createBoard(BoardCreateRequest req) {

        String title = req.getTitle();
        if (title == null || title.isBlank()) {
            throw new BoardException(BoardErrorStatus.INVALID_BOARD_TITLE);
        }

        Board.BoardBuilder builder = Board.builder()
                .scope(req.getScope())
                .title(title.trim())
                .description(req.getDescription());

        if (req.getScope() == BoardScope.SCHOOL) {

            if (req.getSchoolId() == null) {
                throw new BoardException(BoardErrorStatus.SCHOOL_ID_REQUIRED);
            }

            School school = schoolRepository.findById(req.getSchoolId())
                    .orElseThrow(() -> new SchoolException(SchoolErrorStatus.SCHOOL_NOT_FOUND));

            builder.school(school).region(null);

        } else {
            // REGION 게시판 생성
            if (req.getRegionId() == null) {
                throw new BoardException(BoardErrorStatus.REGION_ID_REQUIRED);
            }

            Region region = regionRepository.findById(req.getRegionId())
                    .orElseThrow(() -> new RegionException(RegionErrorStatus.REGION_NOT_FOUND));

            builder.region(region).school(null);
        }

        Board board = builder.build();
        boardRepository.save(board);

        return board.getId();

    }

    /*
    * 학교별 게시판 조회
    * */
    public List<BoardResponse> getBoardsBySchool(Long schoolId){

        // School 검증
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new SchoolException(SchoolErrorStatus.SCHOOL_NOT_FOUND));

        Long regionId = school.getRegion().getId();


        //해당 학교의 지역 게시판 + 그 외 게시판 전부 조회
        List<Board> schoolBoards = boardRepository.findBySchoolId(schoolId);
        List<Board> regionBoards = boardRepository.findByRegionId(regionId);

        return Stream.concat(schoolBoards.stream(), regionBoards.stream())
                .map(BoardResponse::toDto)
                .toList();

    }



}
