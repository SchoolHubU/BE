package com.shu.backend.domain.warning.service;

import com.shu.backend.domain.comment.repository.CommentRepository;
import com.shu.backend.domain.adminaudit.enums.AdminAuditAction;
import com.shu.backend.domain.adminaudit.enums.AdminAuditTargetType;
import com.shu.backend.domain.adminaudit.service.AdminAuditLogService;
import com.shu.backend.domain.notification.enums.NotificationTargetType;
import com.shu.backend.domain.notification.enums.NotificationType;
import com.shu.backend.domain.notification.service.NotificationService;
import com.shu.backend.domain.post.repository.PostRepository;
import com.shu.backend.domain.push.service.PushService;
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
import com.shu.backend.domain.warning.dto.WarningDTO;
import com.shu.backend.domain.warning.entity.Warning;
import com.shu.backend.domain.warning.exception.WarningException;
import com.shu.backend.domain.warning.exception.status.WarningErrorStatus;
import com.shu.backend.domain.warning.repository.WarningRepository;
import com.shu.backend.global.util.HtmlText;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarningService {

    private final WarningRepository warningRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;
    private final PushService pushService;
    private final AdminAuditLogService adminAuditLogService;

    @Transactional
    public Long issue(Long adminId, Long reportId, String adminComment) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportException(ReportErrorStatus.REPORT_NOT_FOUND));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new ReportException(ReportErrorStatus.REPORT_NOT_PENDING);
        }

        if (warningRepository.existsByReportId(reportId)) {
            throw new WarningException(WarningErrorStatus.WARNING_ALREADY_ISSUED);
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));

        report.warn(admin);

        Warning warning = Warning.builder()
                .user(report.getReportedUser())
                .report(report)
                .adminComment(adminComment)
                .build();

        Long warningId = warningRepository.save(warning).getId();

        adminAuditLogService.recordAfterCommit(
                adminId,
                AdminAuditAction.WARN_REPORT,
                AdminAuditTargetType.REPORT,
                reportId,
                adminComment,
                "warningId=" + warningId
        );

        // 경고는 사용자가 놓치면 안 되는 운영 알림이므로
        // 알림 설정과 무관하게 앱 알림과 푸시를 함께 발송한다. (문의 답변과 동일한 정책)
        String message = "커뮤니티 가이드 위반으로 경고를 받았어요. 경고 내역을 확인해 주세요.";
        Long receiverUserId = report.getReportedUser().getId();
        Long notificationId = notificationService.create(
                NotificationType.WARNING,
                NotificationTargetType.WARNING,
                warningId,
                message,
                receiverUserId,
                adminId
        );

        // 알림 저장 실패가 경고 발령 자체를 막지 않도록 푸시는 별도로 보호한다.
        if (notificationId != null) {
            try {
                // 트랜잭션 커밋 후 발송 — 롤백 시 푸시만 나가는 상황을 방지한다.
                pushService.sendToUserAfterCommit(
                        receiverUserId,
                        "경고 안내",
                        message,
                        Map.of(
                                "notificationId", String.valueOf(notificationId),
                                "type", NotificationType.WARNING.name(),
                                "targetType", NotificationTargetType.WARNING.name(),
                                "targetId", String.valueOf(warningId)
                        )
                );
            } catch (Exception e) {
                log.warn("Warning push scheduling failed: notificationId={}, warningId={}, receiverUserId={}",
                        notificationId, warningId, receiverUserId, e);
            }
        }

        return warningId;
    }

    public Optional<WarningDTO.UnreadResponse> getUnreadWarning(Long userId) {
        return warningRepository
                .findTop1ByUserIdAndIsReadFalseOrderByCreatedAtAsc(userId)
                .map(w -> {
                    Report report = w.getReport();
                    String targetType = report.getTargetType().name();
                    String targetSummary = resolveTargetSummary(report.getTargetType(), report.getTargetId());
                    return WarningDTO.UnreadResponse.from(w, targetType, targetSummary);
                });
    }

    public Page<WarningDTO.HistoryResponse> getMyWarnings(Long userId, Pageable pageable) {
        return warningRepository.findAllByUserId(userId, pageable)
                .map(w -> WarningDTO.HistoryResponse.from(
                        w, resolveTargetSummary(w.getReport().getTargetType(), w.getReport().getTargetId())));
    }

    public Page<WarningDTO.HistoryResponse> getWarningsByUser(Long userId, Pageable pageable) {
        return warningRepository.findAllByUserId(userId, pageable)
                .map(w -> WarningDTO.HistoryResponse.from(
                        w, resolveTargetSummary(w.getReport().getTargetType(), w.getReport().getTargetId())));
    }

    public Page<WarningDTO.HistoryResponse> getAllWarnings(Pageable pageable) {
        return warningRepository.findAll(pageable)
                .map(w -> WarningDTO.HistoryResponse.from(
                        w, resolveTargetSummary(w.getReport().getTargetType(), w.getReport().getTargetId())));
    }

    private String resolveTargetSummary(TargetType targetType, Long targetId) {
        return switch (targetType) {
            case POST -> postRepository.findById(targetId)
                    .map(p -> truncate(HtmlText.unescape(p.getTitle()), 80))
                    .orElse("(삭제된 게시글)");
            case COMMENT -> commentRepository.findById(targetId)
                    .map(c -> truncate(HtmlText.unescape(c.getContent()), 80))
                    .orElse("(삭제된 댓글)");
            default -> "";
        };
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    @Transactional
    public void markAsRead(Long warningId, Long userId) {
        Warning warning = warningRepository.findById(warningId)
                .orElseThrow(() -> new WarningException(WarningErrorStatus.WARNING_NOT_FOUND));

        if (!warning.getUser().getId().equals(userId)) {
            throw new WarningException(WarningErrorStatus.WARNING_ACCESS_DENIED);
        }

        warning.markAsRead();
    }
}
