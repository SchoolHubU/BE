package com.shu.backend.domain.comment.service;

import com.shu.backend.domain.comment.dto.CommentCreateRequest;
import com.shu.backend.domain.comment.dto.CommentUpdateRequest;
import com.shu.backend.domain.comment.entity.Comment;
import com.shu.backend.domain.comment.enums.CommentStatus;
import com.shu.backend.domain.comment.exception.CommentException;
import com.shu.backend.domain.comment.exception.status.CommentErrorStatus;
import com.shu.backend.domain.comment.repository.CommentRepository;
import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.domain.post.exception.PostException;
import com.shu.backend.domain.post.exception.status.PostErrorStatus;
import com.shu.backend.domain.post.repository.PostRepository;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    @Transactional
    public Long createComment(Long userId, Long postId, CommentCreateRequest req){

        validateContent(req.getContent());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorStatus.POST_NOT_FOUND));

        Comment parent = null;
        int depth = 0;

        if (req.getParentId() != null){
            parent = commentRepository.findById(req.getParentId())
                    .orElseThrow(() -> new CommentException(CommentErrorStatus.COMMENT_NOT_FOUND));

            // 같은 게시글인지 검증
            if (parent.getPost() == null || !parent.getPost().getId().equals(postId)) {
                throw new CommentException(CommentErrorStatus.PARENT_COMMENT_NOT_IN_SAME_POST);
            }
            depth = parent.getDepth() + 1;
        }

        boolean anonymous = req.getAnonymous();

        Comment comment = Comment.builder()
                .content(req.getContent())
                .anonymous(anonymous)
                .post(post)
                .user(user)
                .parent(parent)
                .depth(depth)
                .commentStatus(CommentStatus.ACTIVE)
                .build();

        Comment saved = commentRepository.save(comment);

        return saved.getId();
    }

    @Transactional
    public Long updateComment(Long commentId, Long userId, CommentUpdateRequest req){

        validateContent(req.getContent());

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentException(CommentErrorStatus.COMMENT_NOT_FOUND));

        if (comment.getUser() == null || comment.getUser().getId() == null || !comment.getUser().getId().equals(userId)) {
            System.out.println(
                    "commentId=" + commentId +
                            ", requestUserId=" + userId +
                            ", storedUserId=" + (comment.getUser() == null ? null : comment.getUser().getId())
            );
            throw new CommentException(CommentErrorStatus.COMMENT_FORBIDDEN);
        }

        comment.updateContent(req.getContent());

        return commentId;
    }

    @Transactional
    public Long delete(Long commentId, Long userId) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentException(CommentErrorStatus.COMMENT_NOT_FOUND));

        if (comment.getUser() == null || comment.getUser().getId() == null || !comment.getUser().getId().equals(userId)) {
            throw new CommentException(CommentErrorStatus.COMMENT_FORBIDDEN);
        }

        comment.softDelete();
        return commentId;
    }

    private void validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new CommentException(CommentErrorStatus.INVALID_CONTENT);
        }
    }

}
