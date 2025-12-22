package com.shu.backend.domain.comment.service;

import com.shu.backend.domain.comment.dto.CommentResponse;
import com.shu.backend.domain.comment.entity.Comment;
import com.shu.backend.domain.comment.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentQueryService {

    private final CommentRepository commentRepository;

    public List<CommentResponse> getCommentsForPostDetail(Long postId) {
        // MVP: 부모댓글만 먼저 로딩 + (옵션) 자식은 별도 API 또는 제한
        List<Comment> parents = commentRepository.findParentsForPostDetail(postId);

        return parents.stream()
                .map(CommentResponse::toDto)
                .toList();
    }
}