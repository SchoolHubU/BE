package com.shu.backend.domain.user.service;

import com.shu.backend.domain.school.School;
import com.shu.backend.domain.school.SchoolRepository;
import com.shu.backend.domain.school.exception.SchoolException;
import com.shu.backend.domain.school.exception.status.SchoolErrorStatus;
import com.shu.backend.domain.user.dto.SignUpResponseDTO;
import com.shu.backend.domain.user.dto.UserRequestDTO;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.enums.UserRole;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import com.shu.backend.domain.userschoolverificationrequest.entity.UserSchoolVerificationRequest;
import com.shu.backend.domain.userschoolverificationrequest.repository.UserSchoolVerificationRequestRepository;
import com.shu.backend.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final UserSchoolVerificationRequestRepository verificationRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    //회원가입
    public SignUpResponseDTO join(UserRequestDTO.SignUp request, String requestImageUrl) {

        validateSignUpRequest(request);

        School school = getSchoolByName(request.getSchool());

        User newUser = createUser(request, school,requestImageUrl);
        userRepository.save(newUser);

        createVerificationRequest(newUser, school, requestImageUrl);

        // 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(newUser.getId());

        return new SignUpResponseDTO(newUser.getId(), accessToken);
    }

    //중복 검사
    private void validateSignUpRequest(UserRequestDTO.SignUp request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserException(UserErrorStatus.EXIST_EMAIL);
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new UserException(UserErrorStatus.EXIST_NICKNAME);
        }
    }

    private School getSchoolByName(String schoolName) {
        return schoolRepository.findByName(schoolName)
                .orElseThrow(() -> new SchoolException(SchoolErrorStatus.SCHOOL_NOT_FOUND));
    }

    //유저 생성
    private User createUser(UserRequestDTO.SignUp request, School school,String requestImageUrl) {
        return User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .nickname(request.getNickname())
                .school(school)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .verified(false)
                .profileImageUrl(requestImageUrl)
                .build();
    }

    //인증 요청 생성
    private void createVerificationRequest(User user, School school, String imageUrl) {

        UserSchoolVerificationRequest verificationRequest =
                UserSchoolVerificationRequest.builder()
                        .requestImageUrl(imageUrl)
                        .user(user)
                        .school(school)
                        .build();

        verificationRequestRepository.save(verificationRequest);
    }

}
