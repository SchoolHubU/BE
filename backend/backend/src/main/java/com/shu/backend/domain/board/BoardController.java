package com.shu.backend.domain.board;

import com.shu.backend.domain.board.dto.BoardCreateRequest;
import com.shu.backend.domain.board.dto.BoardResponse;
import com.shu.backend.domain.board.exception.status.BoardSuccessStatus;
import com.shu.backend.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @PostMapping(value = "/admin/boards", consumes = "application/json", produces = "application/json")
    public ResponseEntity<ApiResponse<Long>> createBoard(
            @Valid @RequestBody BoardCreateRequest request) {

        Long boardId = boardService.createBoard(request);

        return ResponseEntity
                .status(BoardSuccessStatus._BOARD_CREATED.getHttpStatus())
                .body(ApiResponse.of(BoardSuccessStatus._BOARD_CREATED, boardId));

    }

    @GetMapping(value = "/api/schools/{schoolId}/boards")
    public ResponseEntity<ApiResponse<List<BoardResponse>>> getBoardsBySchool(@PathVariable Long schoolId){
        List<BoardResponse> boards = boardService.getBoardsBySchool(schoolId);

        return ResponseEntity
                .status(BoardSuccessStatus._BOARD_CREATED.getHttpStatus())
                .body(ApiResponse.of(BoardSuccessStatus._BOARD_CREATED, boards));
    }


}
