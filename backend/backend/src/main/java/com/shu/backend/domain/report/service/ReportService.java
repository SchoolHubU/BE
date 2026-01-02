package com.shu.backend.domain.report.service;

import com.shu.backend.domain.chatmessage.exception.ChatMessageException;
import com.shu.backend.domain.chatmessage.exception.status.ChatMessageErrorStatus;
import com.shu.backend.domain.chatmessage.repository.ChatMessageRepository;
import com.shu.backend.domain.comment.exception.CommentException;
import com.shu.backend.domain.comment.exception.status.CommentErrorStatus;
import com.shu.backend.domain.comment.repository.CommentRepository;
import com.shu.backend.domain.penalty.entity.Penalty;
import com.shu.backend.domain.penalty.repository.PenaltyRepository;
import com.shu.backend.domain.penalty.service.PenaltyService;
import com.shu.backend.domain.post.exception.PostException;
import com.shu.backend.domain.post.exception.status.PostErrorStatus;
import com.shu.backend.domain.post.repository.PostRepository;
import com.shu.backend.domain.report.dto.ReportDTO;
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
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PenaltyRepository penaltyRepository;
    private final PenaltyService penaltyService;

    @PreAuthorize("@penaltyChecker.notPenalized(#reporterId)")
    @Transactional
    public Long create(Long reporterId, ReportDTO.CreateRequest req) {

        //중복 신고 검사
        if(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(reporterId, req.getTargetType(), req.getTargetId())){
            throw new ReportException(ReportErrorStatus.DUPLICATE_REPORT);
        }

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));

        User reportedUser = resolveReportedUser(req.getTargetType(), req.getTargetId());

        //신고자와 피신고자가 같을 경우 X
        if (reportedUser.getId().equals(reporterId)) {
            throw new ReportException(ReportErrorStatus.SELF_REPORT_FORBIDDEN);
        }

        Report report = Report.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .targetType(req.getTargetType())
                .targetId(req.getTargetId())
                .reportReason(req.getReportReason())
                .build();

        return reportRepository.save(report).getId();
    }

    @Transactional
    public Long approve(Long adminId, Long reportId, int penaltyDays){

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportException(ReportErrorStatus.REPORT_NOT_FOUND));

        if (report.getStatus() != ReportStatus.PENDING){
            throw new ReportException(ReportErrorStatus.REPORT_NOT_PENDING);
        }

        User handler = userRepository.findById(adminId)
                .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));

        report.resolve(handler);

        return penaltyService.create(reportId, penaltyDays);
    }


    @Transactional
    public Long reject(Long adminId, Long reportId){

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportException(ReportErrorStatus.REPORT_NOT_FOUND));

        if (report.getStatus() != ReportStatus.PENDING){
            throw new ReportException(ReportErrorStatus.REPORT_NOT_PENDING);
        }

        User handler = userRepository.findById(adminId)
                        .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));

        report.reject(handler);
        return reportId;

    }

    public Page<Report> getReports(ReportStatus status, Pageable pageable){
        return reportRepository.findAllByStatus(status, pageable);
    }

    //타입을 보고 타겟 객체와 id 조회
    private User resolveReportedUser(TargetType targetType, Long targetId) {

        return switch (targetType){
            case POST -> postRepository.findById(targetId)
                    .orElseThrow(() -> new PostException(PostErrorStatus.POST_NOT_FOUND))
                    .getUser();

            case COMMENT -> commentRepository.findById(targetId)
                    .orElseThrow(() -> new CommentException(CommentErrorStatus.COMMENT_NOT_FOUND))
                    .getUser();

            case USER -> chatMessageRepository.findById(targetId)
                    .orElseThrow(() -> new ChatMessageException(ChatMessageErrorStatus.CHAT_ROOM_NOT_FOUND))
                    .getSender();

            default -> throw new ReportException(ReportErrorStatus.UNSUPPORTED_TARGET_TYPE);
        };
    }
}
