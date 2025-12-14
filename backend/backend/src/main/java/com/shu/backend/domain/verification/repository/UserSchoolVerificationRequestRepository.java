package com.shu.backend.domain.verification.repository;

import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.verification.status.VerificationStatus;
import com.shu.backend.domain.verification.entity.UserSchoolVerificationRequest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSchoolVerificationRequestRepository extends JpaRepository<UserSchoolVerificationRequest, Long> {
    boolean existsByUser(User user);

    boolean existsByUserAndStatus(User user, VerificationStatus status);

    // 최신 요청 1개만 가져오기
    Optional<UserSchoolVerificationRequest> findTopByUserOrderByRequestedAtDesc(User user);

    @EntityGraph(attributePaths = {"user", "school"})
    List<UserSchoolVerificationRequest> findByStatusOrderByRequestedAtDesc(VerificationStatus status);

    @EntityGraph(attributePaths = {"user", "school"})
    Optional<UserSchoolVerificationRequest> findWithUserAndSchoolById(Long id);





}
