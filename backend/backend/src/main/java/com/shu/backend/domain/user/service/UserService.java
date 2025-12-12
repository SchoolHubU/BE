package com.shu.backend.domain.user.service;

import com.shu.backend.domain.school.School;
import com.shu.backend.domain.school.SchoolRepository;
import com.shu.backend.domain.school.exception.SchoolException;
import com.shu.backend.domain.school.exception.status.SchoolErrorStatus;
import com.shu.backend.domain.user.dto.UserRequestDTO;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.enums.UserRole;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import com.shu.backend.domain.userschoolverificationrequest.repository.UserSchoolVerificationRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

@Service
@RequiredArgsConstructor
public class UserService {

}
