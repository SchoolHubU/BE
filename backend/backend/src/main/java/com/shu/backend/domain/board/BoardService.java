package com.shu.backend.domain.board;

import com.shu.backend.domain.board.dto.BoardCreateRequest;
import com.shu.backend.domain.school.School;
import com.shu.backend.domain.school.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final SchoolRepository schoolRepository;

    /*
    * 게시판 생성 메소드
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

}
