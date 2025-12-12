package com.shu.backend.domain.userschoolverificationrequest.repository;

import com.shu.backend.domain.userschoolverificationrequest.entity.UserSchoolVerificationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSchoolVerificationRequestRepository extends JpaRepository<UserSchoolVerificationRequest, Long> {

}
