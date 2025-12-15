package com.shu.backend.domain.board.controller;

import com.shu.backend.domain.board.service.BoardService;
import com.shu.backend.domain.board.dto.BoardCreateRequest;
import com.shu.backend.domain.board.dto.BoardResponse;
import com.shu.backend.domain.board.exception.status.BoardSuccessStatus;
import com.shu.backend.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "board", description = "게시판 관련 API")
@RestController
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @Operation(
            summary = "게시판 생성",
            description = "관리자 권한으로 게시판을 생성할 수 있습니다."
    )
    @PostMapping(value = "/admin/boards", consumes = "application/json", produces = "application/json")
    public ResponseEntity<ApiResponse<Long>> createBoard(
            @Valid @RequestBody BoardCreateRequest request) {

        Long boardId = boardService.createBoard(request);

        return ResponseEntity
                .status(BoardSuccessStatus._BOARD_CREATED.getHttpStatus())
                .body(ApiResponse.of(BoardSuccessStatus._BOARD_CREATED, boardId));

    }

    @Operation(
            summary = "게시판 전체 조회",
            description = "특정 학교의 게시판 전체 목록을 조회할 수 있습니다."
    )
    @GetMapping(value = "/api/schools/{schoolId}/boards")
    public ResponseEntity<ApiResponse<List<BoardResponse>>> getBoardsBySchool(@PathVariable Long schoolId){
        List<BoardResponse> boards = boardService.getBoardsBySchool(schoolId);

        return ResponseEntity
                .status(BoardSuccessStatus._BOARD_CREATED.getHttpStatus())
                .body(ApiResponse.of(BoardSuccessStatus._BOARD_CREATED, boards));
    }


}
