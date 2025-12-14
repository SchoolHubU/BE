package com.shu.backend.domain.verification.service;

import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.verification.dto.VerificationAdminDTO;
import com.shu.backend.domain.verification.entity.UserSchoolVerification;
import com.shu.backend.domain.verification.entity.UserSchoolVerificationRequest;
import com.shu.backend.domain.verification.exception.VerificationErrorStatus;
import com.shu.backend.domain.verification.repository.UserSchoolVerificationRepository;
import com.shu.backend.domain.verification.repository.UserSchoolVerificationRequestRepository;
import com.shu.backend.domain.verification.status.VerificationStatus;
import com.shu.backend.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SchoolVerificationAdminService {

    private final UserSchoolVerificationRequestRepository requestRepository;
    private final UserSchoolVerificationRepository verificationRepository;

    // 인증 요청 목록 조회 (상태별)
    @Transactional(readOnly = true)
    public List<VerificationAdminDTO.ListItemResponse> list(VerificationStatus status) {
        return requestRepository.findByStatusOrderByRequestedAtDesc(status)
                .stream()
                .map(this::toListItem)
                .toList();
    }

    // 인증 요청 상세 조회
    @Transactional(readOnly = true)
    public VerificationAdminDTO.DetailResponse detail(Long requestId) {
        UserSchoolVerificationRequest req = requestRepository.findWithUserAndSchoolById(requestId)
                .orElseThrow(() ->
                        new GeneralException(VerificationErrorStatus.VERIFICATION_REQUEST_NOT_FOUND)
                );

        return toDetail(req);
    }

    // 인증 요청 승인 처리
    public void approve(Long requestId, String adminComment) {
        Long adminUserId = getCurrentAdminUserId();

        UserSchoolVerificationRequest req = requestRepository.findWithUserAndSchoolById(requestId)
                .orElseThrow(() ->
                        new GeneralException(VerificationErrorStatus.VERIFICATION_REQUEST_NOT_FOUND)
                );

        validatePending(req);

        User user = req.getUser();

        // 이미 인증된 유저 방어
        if (verificationRepository.existsByUserId(user.getId())) {
            throw new GeneralException(VerificationErrorStatus.SCHOOL_VERIFICATION_STATUS_INVALID);
        }

        // 요청 상태 승인으로 변경
        req.approve(adminUserId, adminComment);

        // 최종 학교 인증 정보 생성
        verificationRepository.save(
                UserSchoolVerification.builder()
                        .verified_at(LocalDateTime.now())
                        .user(user)
                        .school(req.getSchool())
                        .build()
        );

        // 유저 상태를 학교 인증 완료로 변경
        user.verifySchool();
    }

    // 인증 요청 거절 처리
    public void reject(Long requestId, String adminComment) {
        Long adminUserId = getCurrentAdminUserId();

        UserSchoolVerificationRequest req = requestRepository.findWithUserAndSchoolById(requestId)
                .orElseThrow(() ->
                        new GeneralException(VerificationErrorStatus.VERIFICATION_REQUEST_NOT_FOUND)
                );

        validatePending(req);

        // 요청 상태 거절로 변경
        req.reject(adminUserId, adminComment);
    }

    // PENDING 상태인지 검증
    private void validatePending(UserSchoolVerificationRequest req) {
        if (req.getStatus() != VerificationStatus.PENDING) {
            throw new GeneralException(VerificationErrorStatus.VERIFICATION_ALREADY_PROCESSED);
        }
    }

    // 목록 조회용 DTO 변환
    private VerificationAdminDTO.ListItemResponse toListItem(UserSchoolVerificationRequest req) {
        return VerificationAdminDTO.ListItemResponse.builder()
                .requestId(req.getId())
                .status(req.getStatus())
                .requestedAt(req.getRequestedAt())
                .userId(req.getUser().getId())
                .userEmail(req.getUser().getEmail())
                .schoolId(req.getSchool().getId())
                .schoolName(req.getSchool().getName())
                .build();
    }

    // 상세 조회용 DTO 변환
    private VerificationAdminDTO.DetailResponse toDetail(UserSchoolVerificationRequest req) {
        return VerificationAdminDTO.DetailResponse.builder()
                .requestId(req.getId())
                .requestImageUrl(req.getRequestImageUrl())
                .status(req.getStatus())
                .requestedAt(req.getRequestedAt())
                .userId(req.getUser().getId())
                .userEmail(req.getUser().getEmail())
                .schoolId(req.getSchool().getId())
                .schoolName(req.getSchool().getName())
                .processedAt(req.getProcessedAt())
                .processedBy(req.getProcessedBy())
                .adminComment(req.getAdminComment())
                .build();
    }

    // 현재 로그인한 관리자 userId 조회 (추후 SecurityContext 연동)
    private Long getCurrentAdminUserId() {
        return 1L;
    }
}