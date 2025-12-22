package com.shu.backend.domain.post.service;

import com.shu.backend.domain.board.entity.Board;
import com.shu.backend.domain.board.enums.BoardScope;
import com.shu.backend.domain.board.exception.BoardException;
import com.shu.backend.domain.board.exception.status.BoardErrorStatus;
import com.shu.backend.domain.board.repository.BoardRepository;
import com.shu.backend.domain.comment.dto.CommentResponse;
import com.shu.backend.domain.comment.repository.CommentRepository;
import com.shu.backend.domain.comment.service.CommentQueryService;
import com.shu.backend.domain.post.dto.PostCreateRequest;
import com.shu.backend.domain.post.dto.PostDetailResponse;
import com.shu.backend.domain.post.dto.PostResponse;
import com.shu.backend.domain.post.dto.PostUpdateRequest;
import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.domain.post.enums.PostStatus;
import com.shu.backend.domain.post.exception.PostException;
import com.shu.backend.domain.post.exception.status.PostErrorStatus;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final CommentQueryService commentQueryService;

    @Transactional
    public Long createPost(Long boardId, PostCreateRequest req) {

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardException(BoardErrorStatus.BOARD_NOT_FOUND));

        if (!board.isActive()) {
            throw new BoardException(BoardErrorStatus.BOARD_INACTIVE);
        }

        User user;

        // 현재 board가 지역게시판인 경우
        if (board.getScope() == BoardScope.REGION) {

            user = userRepository.findByIdWithSchoolAndRegion(req.getUserId())
                    .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));

            Long userRegionId = user.getSchool().getRegion().getId();
            Long boardRegionId = board.getRegion().getId();

            if (!boardRegionId.equals(userRegionId)) {
                throw new PostException(PostErrorStatus.NO_PERMISSION_TO_WRITE);
            }

        }
        // 현재 board가 지역게시판 외의 게시판일 경우
        else if (board.getScope() == BoardScope.SCHOOL) {

            user = userRepository.findById(req.getUserId())
                    .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));

            Long userSchoolId = user.getSchool().getId();
            Long boardSchoolId = board.getSchool().getId();

            if (!boardSchoolId.equals(userSchoolId)) {
                throw new PostException(PostErrorStatus.NO_PERMISSION_TO_WRITE);
            }

        } else {
            throw new BoardException(BoardErrorStatus.INVALID_BOARD_SCOPE);
        }

        String title = req.getTitle().trim();
        String content = req.getContent().trim();

        Post post = Post.builder()
                .title(title)
                .content(content)
                .anonymous(req.isAnonymous())
                .postStatus(PostStatus.ACTIVE)
                .board(board)
                .user(user)
                .build();

        postRepository.save(post);
        return post.getId();
    }

    @Transactional
    public Long updatePost(Long postId, PostUpdateRequest req){
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorStatus.POST_NOT_FOUND));

        // 이미 삭제된 게시글일 경우
        if (post.getPostStatus() == PostStatus.DELETED){
            throw new PostException(PostErrorStatus.POST_ALREADY_DELETED);
        }

        // 해당 게시글의 작성자가 아닐 경우
        if (!post.getUser().getId().equals(req.getUserId())){
            throw new PostException(PostErrorStatus.NO_PERMISSION_TO_WRITE);
        }

        // 글 수정 수행
        post.update(req.getTitle(), req.getContent(),req.isAnonymous());

        return post.getId();
    }

    @Transactional
    public Long deletePost(Long postId, Long userId){

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorStatus.POST_NOT_FOUND));

        if (post.getPostStatus() == PostStatus.DELETED) {
            throw new PostException(PostErrorStatus.POST_ALREADY_DELETED);
        }

        if (!post.getUser().getId().equals(userId)) {
            throw new PostException(PostErrorStatus.NO_PERMISSION_TO_WRITE);
        }

        post.delete();

        return post.getId();
    }

    @Transactional
    public PostDetailResponse getPostDetail(Long postId) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorStatus.POST_NOT_FOUND));

        // 조회수는 원자 업데이트 권장 (아래 4번 참고)
        postRepository.incrementViewCount(postId);

        List<CommentResponse> comments = commentQueryService.getCommentsForPostDetail(postId);

        return PostDetailResponse.toDto(post, comments);
    }

    // 특정 게시판의 글 페이징하여 조회
    public Slice<PostResponse> getPostsByBoardId(Long boardId, Pageable pageable) {

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardException(BoardErrorStatus.BOARD_NOT_FOUND));

        Slice<Post> posts = postRepository.findByBoardId(boardId, pageable);

        return posts.map(post -> {
            int commentCount = commentRepository.countByPostId(post.getId());
            return PostResponse.toDto(post, commentCount);
        });
    }
}
