package com.shu.backend.domain.admin.service;

import com.shu.backend.domain.admin.dto.AdminBoardResponse;
import com.shu.backend.domain.admin.dto.AdminCommentResponse;
import com.shu.backend.domain.admin.dto.AdminPostDetailResponse;
import com.shu.backend.domain.admin.dto.AdminPostPageResponse;
import com.shu.backend.domain.admin.dto.AdminPostSummaryResponse;
import com.shu.backend.domain.admin.dto.AdminSchoolResponse;
import com.shu.backend.domain.adminaudit.enums.AdminAuditAction;
import com.shu.backend.domain.adminaudit.enums.AdminAuditTargetType;
import com.shu.backend.domain.adminaudit.service.AdminAuditLogService;
import com.shu.backend.domain.board.entity.Board;
import com.shu.backend.domain.board.exception.BoardException;
import com.shu.backend.domain.board.exception.status.BoardErrorStatus;
import com.shu.backend.domain.board.repository.BoardRepository;
import com.shu.backend.domain.comment.entity.Comment;
import com.shu.backend.domain.comment.exception.CommentException;
import com.shu.backend.domain.comment.exception.status.CommentErrorStatus;
import com.shu.backend.domain.comment.repository.CommentRepository;
import com.shu.backend.domain.post.dto.PostMediaResponse;
import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.domain.post.enums.PostStatus;
import com.shu.backend.domain.post.exception.PostException;
import com.shu.backend.domain.post.exception.status.PostErrorStatus;
import com.shu.backend.domain.post.repository.PostRepository;
import com.shu.backend.domain.post.service.PostMediaService;
import com.shu.backend.domain.school.repository.SchoolRepository;
import com.shu.backend.global.util.PageRequestUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AdminContentService {

    private final SchoolRepository schoolRepository;
    private final BoardRepository boardRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostMediaService postMediaService;
    private final AdminAuditLogService adminAuditLogService;

    public Page<AdminSchoolResponse> searchSchools(String keyword, int page, int size) {
        Pageable pageable = PageRequestUtils.of(page, size, 100, Sort.by(Sort.Direction.ASC, "name"));
        String safeKeyword = keyword == null ? "" : keyword.trim();
        return schoolRepository.searchAdminSchools(safeKeyword, pageable)
                .map(AdminSchoolResponse::from);
    }

    public List<AdminBoardResponse> getBoardsBySchool(Long schoolId) {
        return boardRepository.findAdminBoardsBySchoolId(schoolId).stream()
                // 관리자 게시글 목록과 동일하게 삭제된 게시글은 개수에서 제외한다.
                // 숨김 게시글은 관리 대상이므로 전체 개수에 포함한다.
                .map(board -> AdminBoardResponse.from(
                        board,
                        postRepository.countByBoardAndPostStatusNot(board, PostStatus.DELETED)
                ))
                .toList();
    }

    public AdminPostPageResponse getPostsByBoard(Long boardId, int page, int size) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardException(BoardErrorStatus.BOARD_NOT_FOUND));

        Pageable pageable = PageRequestUtils.of(page, size, 100, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdminPostSummaryResponse> posts =
                postRepository.findByBoardIdAndPostStatusNot(boardId, PostStatus.DELETED, pageable)
                .map(AdminPostSummaryResponse::from);

        long visibleCount = postRepository.countByBoardAndPostStatus(board, PostStatus.ACTIVE);
        long hiddenCount = postRepository.countByBoardAndPostStatus(board, PostStatus.HIDDEN);

        return AdminPostPageResponse.from(posts, visibleCount, hiddenCount);
    }

    public AdminPostDetailResponse getPostDetail(Long postId) {
        Post post = postRepository.findWithAdminContextById(postId)
                .orElseThrow(() -> new PostException(PostErrorStatus.POST_NOT_FOUND));

        List<PostMediaResponse> mediaList = postMediaService.getByPostId(postId);
        List<AdminCommentResponse> comments = commentRepository.findAdminCommentsByPostId(postId).stream()
                .map(AdminCommentResponse::from)
                .toList();

        return AdminPostDetailResponse.from(post, mediaList, comments);
    }

    @Transactional
    public AdminPostDetailResponse getPostDetail(Long postId, Long adminId) {
        AdminPostDetailResponse response = getPostDetail(postId);
        adminAuditLogService.record(
                adminId,
                AdminAuditAction.VIEW_POST_DETAIL,
                AdminAuditTargetType.POST,
                postId,
                "관리자 게시글 상세 열람",
                null
        );
        return response;
    }

    @Transactional
    public AdminPostDetailResponse hidePost(Long postId, Long adminId, String reason) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorStatus.POST_NOT_FOUND));
        post.hide();
        adminAuditLogService.recordAfterCommit(
                adminId,
                AdminAuditAction.HIDE_POST,
                AdminAuditTargetType.POST,
                postId,
                reason,
                "boardId=" + post.getBoard().getId()
        );
        log.info("Admin hid post: adminId={}, postId={}, boardId={}", adminId, postId, post.getBoard().getId());
        return getPostDetail(postId);
    }

    @Transactional
    public AdminPostDetailResponse restorePost(Long postId, Long adminId, String reason) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorStatus.POST_NOT_FOUND));
        post.restore();
        adminAuditLogService.recordAfterCommit(
                adminId,
                AdminAuditAction.RESTORE_POST,
                AdminAuditTargetType.POST,
                postId,
                reason,
                "boardId=" + post.getBoard().getId()
        );
        log.info("Admin restored post: adminId={}, postId={}, boardId={}", adminId, postId, post.getBoard().getId());
        return getPostDetail(postId);
    }

    @Transactional
    public AdminPostDetailResponse hideComment(Long commentId, Long adminId, String reason) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentException(CommentErrorStatus.COMMENT_NOT_FOUND));
        comment.hide();
        adminAuditLogService.recordAfterCommit(
                adminId,
                AdminAuditAction.HIDE_COMMENT,
                AdminAuditTargetType.COMMENT,
                commentId,
                reason,
                "postId=" + comment.getPost().getId()
        );
        log.info("Admin hid comment: adminId={}, commentId={}, postId={}", adminId, commentId, comment.getPost().getId());
        return getPostDetail(comment.getPost().getId());
    }

    @Transactional
    public AdminPostDetailResponse restoreComment(Long commentId, Long adminId, String reason) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentException(CommentErrorStatus.COMMENT_NOT_FOUND));
        comment.restore();
        adminAuditLogService.recordAfterCommit(
                adminId,
                AdminAuditAction.RESTORE_COMMENT,
                AdminAuditTargetType.COMMENT,
                commentId,
                reason,
                "postId=" + comment.getPost().getId()
        );
        log.info("Admin restored comment: adminId={}, commentId={}, postId={}", adminId, commentId, comment.getPost().getId());
        return getPostDetail(comment.getPost().getId());
    }
}
