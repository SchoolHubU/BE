package com.shu.backend.domain.board;

import com.shu.backend.domain.board.dto.BoardCreateRequest;
import com.shu.backend.domain.board.dto.BoardResponse;
import com.shu.backend.domain.school.School;
import com.shu.backend.domain.school.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final SchoolRepository schoolRepository;

    /*
    * 게시판 생성
    * */
    @Transactional
    public Long createBoard(BoardCreateRequest boardCreateRequest) {

        // School 검증
        School school = schoolRepository.findById(boardCreateRequest.getSchoolId())
                .orElseThrow(() -> new RuntimeException());      //추후 수정 (School 도메인 exception)

        // Board 객체 생성
        Board board = Board.builder()
                .title(boardCreateRequest.getTitle())
                .description(boardCreateRequest.getDescription())
                .school(school)
                .build();

        boardRepository.save(board);


        // 생성된 Board 객체 Id 반환
        return board.getId();

    }

    /*
    * 학교별 게시판 조회
    * */
    public List<BoardResponse> getBoardsBySchool(Long schoolId){

        // School 검증
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new RuntimeException());     // 예외 추후 수정

        // boards 조회하여 DTO로 변환 후 반환
        return boardRepository.findBySchoolId(schoolId)
                .stream()
                .map(BoardResponse::toDto)
                .toList();

    }



}
