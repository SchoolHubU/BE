package com.shu.backend.admin.service;

import com.shu.backend.domain.admin.dto.AdminBoardResponse;
import com.shu.backend.domain.admin.dto.AdminPostPageResponse;
import com.shu.backend.domain.admin.service.AdminContentService;
import com.shu.backend.domain.adminaudit.service.AdminAuditLogService;
import com.shu.backend.domain.board.entity.Board;
import com.shu.backend.domain.board.repository.BoardRepository;
import com.shu.backend.domain.comment.repository.CommentRepository;
import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.domain.post.enums.PostStatus;
import com.shu.backend.domain.post.repository.PostRepository;
import com.shu.backend.domain.post.service.PostMediaService;
import com.shu.backend.domain.school.repository.SchoolRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminContentServiceTest {

    @Mock
    SchoolRepository schoolRepository;

    @Mock
    BoardRepository boardRepository;

    @Mock
    PostRepository postRepository;

    @Mock
    CommentRepository commentRepository;

    @Mock
    PostMediaService postMediaService;

    @Mock
    AdminAuditLogService adminAuditLogService;

    @InjectMocks
    AdminContentService adminContentService;

    @Test
    void boardPostCountExcludesDeletedPostsAndIncludesHiddenPosts() {
        Board board = Board.builder().title("자유게시판").build();
        when(boardRepository.findAdminBoardsBySchoolId(1223L)).thenReturn(List.of(board));
        when(postRepository.countByBoardAndPostStatusNot(board, PostStatus.DELETED))
                .thenReturn(4);

        List<AdminBoardResponse> result = adminContentService.getBoardsBySchool(1223L);

        assertThat(result).singleElement()
                .extracting(AdminBoardResponse::getPostCount)
                .isEqualTo(4);
        verify(postRepository).countByBoardAndPostStatusNot(board, PostStatus.DELETED);
    }

    @Test
    void postPageReturnsServerSideTotalVisibleAndHiddenCounts() {
        Board board = Board.builder().title("자유게시판").build();
        PageRequest pageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        when(boardRepository.findById(10L)).thenReturn(Optional.of(board));
        when(postRepository.findByBoardIdAndPostStatusNot(
                10L,
                PostStatus.DELETED,
                pageable
        )).thenReturn(new PageImpl<Post>(List.of(), pageable, 4));
        when(postRepository.countByBoardAndPostStatus(board, PostStatus.ACTIVE))
                .thenReturn(3L);
        when(postRepository.countByBoardAndPostStatus(board, PostStatus.HIDDEN))
                .thenReturn(1L);

        AdminPostPageResponse result = adminContentService.getPostsByBoard(10L, 0, 20);

        assertThat(result.getTotalCount()).isEqualTo(4);
        assertThat(result.getVisibleCount()).isEqualTo(3);
        assertThat(result.getHiddenCount()).isEqualTo(1);
        assertThat(result.getTotalElements()).isEqualTo(4);
    }
}
