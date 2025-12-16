package com.shu.backend.domain.post.service;

import com.shu.backend.domain.board.entity.Board;
import com.shu.backend.domain.board.exception.BoardException;
import com.shu.backend.domain.board.exception.status.BoardErrorStatus;
import com.shu.backend.domain.board.repository.BoardRepository;
import com.shu.backend.domain.post.dto.PostCreateRequest;
import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.domain.post.repository.PostRepository;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long createPost(PostCreateRequest request){

        Long boardId = request.getBoardId();
        Long userId = request.getUserId();

        //게시판 검증
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardException(BoardErrorStatus.BOARD_NOT_FOUND));

        //사용자 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));

        //게시판 활성화 여부 검증
        if(!board.isActive()){
            throw new BoardException(BoardErrorStatus.BOARD_INACTIVE);
        }







        return null;

    }

    public Slice<Post> getPostsByBoardId(Long boardId, Pageable pageable) {
        return postRepository.findByBoardId(boardId, pageable);
    }
}
