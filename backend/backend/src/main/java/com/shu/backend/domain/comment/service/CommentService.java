package com.shu.backend.domain.comment.service;

import com.shu.backend.domain.comment.dto.CommentCreateRequest;
import com.shu.backend.domain.comment.dto.CommentUpdateRequest;
import com.shu.backend.domain.comment.entity.Comment;
import com.shu.backend.domain.comment.enums.CommentStatus;
import com.shu.backend.domain.comment.exception.CommentException;
import com.shu.backend.domain.comment.exception.status.CommentErrorStatus;
import com.shu.backend.domain.comment.repository.CommentRepository;
import com.shu.backend.domain.notification.enums.NotificationTargetType;
import com.shu.backend.domain.notification.enums.NotificationType;
import com.shu.backend.domain.notification.service.NotificationService;
import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.domain.post.exception.PostException;
import com.shu.backend.domain.post.exception.status.PostErrorStatus;
import com.shu.backend.domain.post.repository.PostRepository;
import com.shu.backend.domain.push.service.PushService;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    private final NotificationService notificationService;
    private final PushService pushService;

    @PreAuthorize("@penaltyChecker.notPenalized(#userId)")
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

        // ===== 알림 생성(저장 성공 이후) =====
        // actorId = userId(댓글 작성자)
        Long actorId = userId;

        if (parent == null) {
            // 1) 일반 댓글: 게시글 작성자에게 알림
            // receiver = post 작성자
            Long receiverUserId = post.getUser().getId();

            if(receiverUserId.equals(actorId)){
                return saved.getId();
            }

            Long notificationId = notificationService.create(
                    NotificationType.COMMENT,
                    NotificationTargetType.POST,
                    post.getId(),
                    "내 글에 댓글이 달렸습니다.",
                    receiverUserId,
                    actorId
            );

            //푸시 발송 (푸시 실패해도 댓글 생성은 성공해야 하므로 try-catch 권장)
            if (notificationId != null) {
                try {
                    pushService.sendToUser(
                            receiverUserId,
                            "새 댓글",
                            "내 글에 댓글이 달렸습니다.",
                            Map.of(
                                    "notificationId", String.valueOf(notificationId),
                                    "type", NotificationType.COMMENT.name(),
                                    "targetType", NotificationTargetType.POST.name(),
                                    "targetId", String.valueOf(post.getId())
                            )
                    );
                } catch (Exception ignore) {
                    // 로깅만 하거나 모니터링 전송 권장
                }
            }

        } else {
            // 2) 대댓글: 부모 댓글 작성자에게 알림
            Long receiverUserId = parent.getUser().getId();

            if(receiverUserId.equals(actorId)){
                return saved.getId();
            }

            Long notificationId = notificationService.create(
                    NotificationType.REPLY,
                    NotificationTargetType.COMMENT,
                    parent.getId(),
                    "내 댓글에 대댓글이 달렸습니다.",
                    receiverUserId,
                    actorId
            );

            // 2) 푸시 발송
            if (notificationId != null) {
                try {
                    pushService.sendToUser(
                            receiverUserId,
                            "새 답글",
                            "내 댓글에 대댓글이 달렸습니다.",
                            Map.of(
                                    "notificationId", String.valueOf(notificationId),
                                    "type", NotificationType.REPLY.name(),
                                    "targetType", NotificationTargetType.COMMENT.name(),
                                    "targetId", String.valueOf(parent.getId())
                            )
                    );
                } catch (Exception ignore) {
                    // 로깅만 하거나 모니터링 전송 권장
                }
            }

        /*
        notificationService.create(
                NotificationType.NEW_COMMENT_ON_POST,
                NotificationTargetType.POST,
                post.getId(),
                "내 글에 대댓글이 달렸습니다.",
                post.getUser().getId(),
                actorId
        );
        */
        }

        return saved.getId();
    }

    @PreAuthorize("@penaltyChecker.notPenalized(#userId)")
    @Transactional
    public Long updateComment(Long commentId, Long userId, CommentUpdateRequest req){

        validateContent(req.getContent());

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentException(CommentErrorStatus.COMMENT_NOT_FOUND));

        if (comment.getUser() == null || comment.getUser().getId() == null || !comment.getUser().getId().equals(userId)) {
            throw new CommentException(CommentErrorStatus.COMMENT_FORBIDDEN);
        }

        comment.updateContent(req.getContent());

        return commentId;
    }

    @PreAuthorize("@penaltyChecker.notPenalized(#userId)")
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
