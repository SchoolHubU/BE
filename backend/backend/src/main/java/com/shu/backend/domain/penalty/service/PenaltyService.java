package com.shu.backend.domain.penalty.service;

import com.shu.backend.domain.penalty.dto.PenaltyDTO;
import com.shu.backend.domain.penalty.entity.Penalty;
import com.shu.backend.domain.penalty.repository.PenaltyRepository;
import com.shu.backend.domain.report.entity.Report;
import com.shu.backend.domain.report.exception.ReportException;
import com.shu.backend.domain.report.exception.status.ReportErrorStatus;
import com.shu.backend.domain.report.repository.ReportRepository;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PenaltyService {

    private final PenaltyRepository penaltyRepository;
    private final ReportRepository reportRepository;

    public Long create(Long reportId, int penaltyDays){

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportException(ReportErrorStatus.REPORT_NOT_FOUND));

        if (penaltyRepository.existsByReportId(reportId)) {
            throw new ReportException(ReportErrorStatus.PENALTY_ALREADY_CREATED);
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(penaltyDays);

        Penalty penalty = Penalty.builder()
                .report(report)
                .user(report.getReportedUser())
                .reason(report.getReportReason())
                .expiresAt(expiresAt)
                .build();

        return penaltyRepository.save(penalty).getId();
    }

    public PenaltyDTO.MyActiveResponse getMyActivePenalty(Long myId) {
        return penaltyRepository.findTop1ByUserIdAndExpiresAtAfterOrderByExpiresAtDesc(myId, LocalDateTime.now())
                .map(p -> PenaltyDTO.MyActiveResponse.builder()
                        .penalized(true)
                        .expiresAt(p.getExpiresAt())
                        .reason(p.getReason().name())
                        .reportId(p.getReport().getId())
                        .build())
                .orElseGet(() -> PenaltyDTO.MyActiveResponse.builder()
                        .penalized(false)
                        .expiresAt(null)
                        .reason(null)
                        .reportId(null)
                        .build());
    }


    //유저별 제재 이력 ㅎ조회
    public Page<PenaltyDTO.SummaryResponse> getPenaltiesByUser(Long userId, Pageable pageable) {
        return penaltyRepository.findAllByUserId(userId, pageable)
                .map(PenaltyDTO.SummaryResponse::from);
    }

}
