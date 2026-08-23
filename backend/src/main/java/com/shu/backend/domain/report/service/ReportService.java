package com.shu.backend.domain.report.service;

import com.shu.backend.domain.adminaudit.enums.AdminAuditAction;
import com.shu.backend.domain.adminaudit.enums.AdminAuditTargetType;
import com.shu.backend.domain.adminaudit.service.AdminAuditLogService;
import com.shu.backend.domain.admin.service.AdminPushService;
import com.shu.backend.domain.block.service.UserBlockService;
import com.shu.backend.domain.board.service.BoardAccessPolicy;
import com.shu.backend.domain.notification.enums.NotificationTargetType;
import com.shu.backend.domain.notification.enums.NotificationType;
import com.shu.backend.domain.comment.entity.Comment;
import com.shu.backend.domain.comment.exception.CommentException;
import com.shu.backend.domain.comment.exception.status.CommentErrorStatus;
import com.shu.backend.domain.comment.repository.CommentRepository;
import com.shu.backend.domain.penalty.repository.PenaltyRepository;
import com.shu.backend.domain.penalty.service.PenaltyService;
import com.shu.backend.domain.post.entity.Post;
import com.shu.backend.domain.post.exception.PostException;
import com.shu.backend.domain.post.exception.status.PostErrorStatus;
import com.shu.backend.domain.post.repository.PostRepository;
import com.shu.backend.domain.report.dto.ReportDTO;
import com.shu.backend.domain.report.dto.ReportSummaryResponse;
import com.shu.backend.domain.report.entity.Report;
import com.shu.backend.domain.report.enums.ReportStatus;
import com.shu.backend.domain.report.enums.TargetType;
import com.shu.backend.domain.report.exception.ReportException;
import com.shu.backend.domain.report.exception.status.ReportErrorStatus;
import com.shu.backend.domain.report.repository.ReportRepository;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import com.shu.backend.domain.user.support.UserDisplay;
import com.shu.backend.global.util.HtmlText;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PenaltyRepository penaltyRepository;
    private final PenaltyService penaltyService;
    private final BoardAccessPolicy boardAccessPolicy;
    private final AdminAuditLogService adminAuditLogService;
    private final AdminPushService adminPushService;
    private final UserBlockService userBlockService;

    @Transactional
    public Long create(Long reporterId, ReportDTO.CreateRequest req) {

        //以묐났 ?좉퀬 寃??
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));

        validateReporterCanAccessTarget(reporterId, req.getTargetType(), req.getTargetId());

        User reportedUser = resolveReportedUser(req.getTargetType(), req.getTargetId());
        if (UserDisplay.isDeleted(reportedUser)) {
            throw new ReportException(ReportErrorStatus.TARGET_USER_DELETED);
        }

        //?좉퀬?먯? ?쇱떊怨좎옄媛 媛숈쓣 寃쎌슦 X
        if (reportedUser.getId().equals(reporterId)) {
            throw new ReportException(ReportErrorStatus.SELF_REPORT_FORBIDDEN);
        }

        var existingReport = reportRepository.findByReporterIdAndTargetTypeAndTargetId(
                reporterId,
                req.getTargetType(),
                req.getTargetId()
        );
        if (existingReport.isPresent()) {
            userBlockService.blockUser(reporterId, reportedUser.getId());
            log.info("Duplicate report treated as block: reportId={}, reporterId={}, reportedUserId={}, targetType={}, targetId={}",
                    existingReport.get().getId(), reporterId, reportedUser.getId(), req.getTargetType(), req.getTargetId());
            return existingReport.get().getId();
        }

        Report report = Report.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .targetType(req.getTargetType())
                .targetId(req.getTargetId())
                .reportReason(req.getReportReason())
                .reportDetail(normalizeReportDetail(req.getReportDetail()))
                .build();

        Long reportId = reportRepository.save(report).getId();
        userBlockService.blockUser(reporterId, reportedUser.getId());
        // 관리자 알림함 기록 + 푸시 (actorId = 신고자 — 관리자 본인의 신고는 자신에게 알리지 않음)
        adminPushService.notifyActiveAdmins(
                NotificationType.ADMIN_REPORT,
                NotificationTargetType.REPORT,
                reportId,
                "새 신고 접수",
                report.getTargetType().name() + " 신고가 접수되었습니다.",
                reporterId
        );
        log.info("Report created: reportId={}, reporterId={}, reportedUserId={}, targetType={}, targetId={}, reason={}",
                reportId, reporterId, reportedUser.getId(), req.getTargetType(), req.getTargetId(), req.getReportReason());
        return reportId;
    }

    @Transactional
    public Long approve(Long adminId, Long reportId, int penaltyDays, String adminComment){

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportException(ReportErrorStatus.REPORT_NOT_FOUND));

        if (report.getStatus() != ReportStatus.PENDING){
            throw new ReportException(ReportErrorStatus.REPORT_NOT_PENDING);
        }

        User handler = userRepository.findById(adminId)
                .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));

        report.resolve(handler, penaltyDays);
        hideReportedContent(report);

        Long penaltyId = penaltyService.create(reportId, penaltyDays);
        adminAuditLogService.recordAfterCommit(
                adminId,
                AdminAuditAction.APPROVE_REPORT,
                AdminAuditTargetType.REPORT,
                reportId,
                adminComment,
                "penaltyId=" + penaltyId
        );
        log.info("Report approved: reportId={}, adminId={}, reportedUserId={}, penaltyDays={}, penaltyId={}",
                reportId, adminId, report.getReportedUser().getId(), penaltyDays, penaltyId);
        return penaltyId;
    }

    private void hideReportedContent(Report report) {
        switch (report.getTargetType()) {
            case POST -> postRepository.findById(report.getTargetId())
                    .ifPresent(Post::hide);
            case COMMENT -> commentRepository.findById(report.getTargetId())
                    .ifPresent(Comment::hide);
            case USER -> {
                // 사용자 신고는 대상 게시글/댓글이 없으므로 제재만 적용한다.
            }
            default -> throw new ReportException(ReportErrorStatus.UNSUPPORTED_TARGET_TYPE);
        }
    }


    @Transactional
    public Long reject(Long adminId, Long reportId, String adminComment){

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportException(ReportErrorStatus.REPORT_NOT_FOUND));

        if (report.getStatus() != ReportStatus.PENDING){
            throw new ReportException(ReportErrorStatus.REPORT_NOT_PENDING);
        }

        User handler = userRepository.findById(adminId)
                        .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));

        report.reject(handler);
        adminAuditLogService.recordAfterCommit(
                adminId,
                AdminAuditAction.REJECT_REPORT,
                AdminAuditTargetType.REPORT,
                reportId,
                adminComment,
                null
        );
        log.info("Report rejected: reportId={}, adminId={}", reportId, adminId);
        return reportId;

    }

    public Page<Report> getReports(ReportStatus status, String keyword, Pageable pageable){
        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword == null) {
            return reportRepository.findAllByStatus(status, pageable);
        }
        return reportRepository.searchByStatus(status, normalizedKeyword, pageable);
    }

    public ReportSummaryResponse.DetailResponse getReportDetail(Long reportId) {
        Report r = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportException(ReportErrorStatus.REPORT_NOT_FOUND));

        String targetContent = resolveTargetContent(r.getTargetType(), r.getTargetId());
        PostContext ctx = resolvePostContext(r.getTargetType(), r.getTargetId());

        return ReportSummaryResponse.DetailResponse.builder()
                .reportId(r.getId())
                .reporterId(r.getReporter().getId())
                .reporterNickname(r.getReporter().getNickname())
                .reportedUserId(r.getReportedUser().getId())
                .reportedUserNickname(r.getReportedUser().getNickname())
                .targetType(r.getTargetType().name())
                .targetId(r.getTargetId())
                .postId(ctx.postId())
                .targetContent(targetContent)
                .schoolName(ctx.schoolName())
                .boardTitle(ctx.boardTitle())
                .reportReason(r.getReportReason().name())
                .reportDetail(r.getReportDetail())
                .status(r.getStatus().name())
                .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().format(ISO_FMT) : null)
                .processedAt(r.getProcessedAt() != null ? r.getProcessedAt().format(ISO_FMT) : null)
                .penaltyDays(r.getPenaltyDays())
                .build();
    }

    private String resolveTargetContent(TargetType targetType, Long targetId) {
        return switch (targetType) {
            case POST -> postRepository.findById(targetId)
                    .map(p -> "[제목] " + HtmlText.unescape(p.getTitle()) + "\n" + HtmlText.unescape(p.getContent()))
                    .orElse("(삭제된 게시글)");
            case COMMENT -> commentRepository.findById(targetId)
                    .map(comment -> HtmlText.unescape(comment.getContent()))
                    .orElse("(삭제된 댓글)");
            case USER -> userRepository.findById(targetId)
                    .map(user -> "채팅 상대 사용자 신고: " + UserDisplay.nicknameOrDeleted(user))
                    .orElse("(탈퇴한 사용자)");
            default -> "";
        };
    }

    private record PostContext(Long postId, String schoolName, String boardTitle) {}

    private PostContext resolvePostContext(TargetType targetType, Long targetId) {
        return switch (targetType) {
            case POST -> postRepository.findById(targetId)
                    .map(p -> new PostContext(
                            p.getId(),
                            p.getBoard().getSchool() != null ? p.getBoard().getSchool().getName() : null,
                            p.getBoard().getTitle()
                    ))
                    .orElse(new PostContext(null, null, null));
            case COMMENT -> commentRepository.findById(targetId)
                    .map(c -> new PostContext(
                            c.getPost().getId(),
                            c.getPost().getBoard().getSchool() != null ? c.getPost().getBoard().getSchool().getName() : null,
                            c.getPost().getBoard().getTitle()
                    ))
                    .orElse(new PostContext(null, null, null));
            case USER -> new PostContext(null, null, "채팅");
            default -> new PostContext(null, null, null);
        };
    }

    //??낆쓣 蹂닿퀬 ?寃?媛앹껜? id 議고쉶
    private User resolveReportedUser(TargetType targetType, Long targetId) {

        return switch (targetType){
            case POST -> postRepository.findById(targetId)
                    .orElseThrow(() -> new PostException(PostErrorStatus.POST_NOT_FOUND))
                    .getUser();

            case COMMENT -> commentRepository.findById(targetId)
                    .orElseThrow(() -> new CommentException(CommentErrorStatus.COMMENT_NOT_FOUND))
                    .getUser();

            case USER -> userRepository.findById(targetId)
                    .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));

            default -> throw new ReportException(ReportErrorStatus.UNSUPPORTED_TARGET_TYPE);
        };
    }

    private void validateReporterCanAccessTarget(Long reporterId, TargetType targetType, Long targetId) {
        switch (targetType) {
            case POST -> {
                Post post = postRepository.findById(targetId)
                        .orElseThrow(() -> new PostException(PostErrorStatus.POST_NOT_FOUND));
                boardAccessPolicy.assertCanAccessPost(reporterId, post);
            }
            case COMMENT -> {
                Comment comment = commentRepository.findById(targetId)
                        .orElseThrow(() -> new CommentException(CommentErrorStatus.COMMENT_NOT_FOUND));
                boardAccessPolicy.assertCanAccessComment(reporterId, comment);
            }
            case USER -> boardAccessPolicy.requireActiveUserWithSchool(reporterId);
            default -> throw new ReportException(ReportErrorStatus.UNSUPPORTED_TARGET_TYPE);
        }
    }

    private String normalizeReportDetail(String reportDetail) {
        if (reportDetail == null) {
            return null;
        }
        String trimmed = reportDetail.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
