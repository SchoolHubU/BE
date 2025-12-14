package com.shu.backend.domain.verification.repository;

import com.shu.backend.domain.verification.entity.UserSchoolVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSchoolVerificationRepository extends JpaRepository<UserSchoolVerification, Long> {
    // 해당 유저가 이미 학교 인증을 완료했는지 여부 확인
    boolean existsByUserId(Long userId);

    // 유저의 학교 인증 정보 조회
    Optional<UserSchoolVerification> findByUserId(Long userId);

}
