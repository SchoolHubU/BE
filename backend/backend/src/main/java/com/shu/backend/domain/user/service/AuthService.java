package com.shu.backend.domain.user.service;

import com.shu.backend.domain.school.School;
import com.shu.backend.domain.school.SchoolRepository;
import com.shu.backend.domain.school.exception.SchoolException;
import com.shu.backend.domain.school.exception.status.SchoolErrorStatus;
import com.shu.backend.domain.user.dto.LoginResponseDTO;
import com.shu.backend.domain.user.dto.SignUpResponseDTO;
import com.shu.backend.domain.user.dto.UserLoginDTO;
import com.shu.backend.domain.user.dto.UserRequestDTO;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.enums.UserRole;
import com.shu.backend.domain.user.enums.UserStatus;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import com.shu.backend.domain.verification.entity.UserSchoolVerificationRequest;
import com.shu.backend.domain.verification.repository.UserSchoolVerificationRequestRepository;
import com.shu.backend.domain.verification.status.VerificationStatus;
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

    private static final String ADMIN_SCHOOL_NAME = "운영자전용학교";

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
    public void validateSignUpRequest(UserRequestDTO.SignUp request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserException(UserErrorStatus.EXIST_EMAIL);
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new UserException(UserErrorStatus.EXIST_NICKNAME);
        }
    }

    public School getSchoolByName(String schoolName) {
        if (ADMIN_SCHOOL_NAME.equals(schoolName)) {
            throw new SchoolException(SchoolErrorStatus.INVALID_SCHOOL_FOR_SIGNUP);
        }

        return schoolRepository.findByName(schoolName)
                .orElseThrow(() -> new SchoolException(SchoolErrorStatus.SCHOOL_NOT_FOUND));
    }

    //유저 생성
    public User createUser(UserRequestDTO.SignUp request, School school,String requestImageUrl) {
        return User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .nickname(request.getNickname())
                .school(school)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .verified(false)
                .profileImageUrl(requestImageUrl)
                .build();
    }

    //인증 요청 생성
    public void createVerificationRequest(User user, School school, String imageUrl) {

        UserSchoolVerificationRequest verificationRequest = new UserSchoolVerificationRequest(imageUrl,user,school);
        verificationRequestRepository.save(verificationRequest);
    }

    //로그인
    @Transactional(readOnly = true)
    public LoginResponseDTO login(UserLoginDTO userLoginDTO) {

        User user = userRepository.findByEmail(userLoginDTO.getEmail())
                .orElseThrow(() -> new UserException(UserErrorStatus.EMAIL_NOT_FOUND));

        if (!passwordEncoder.matches(userLoginDTO.getPassword(), user.getPassword())) {
            throw new UserException(UserErrorStatus.INVALID_PASSWORD);
        }

        // 관리자면 학교 인증 체크 패스
        if (user.getRole() == UserRole.USER) {

            UserSchoolVerificationRequest latestRequest =
                    verificationRequestRepository.findTopByUserOrderByRequestedAtDesc(user)
                            .orElse(null);

            if (latestRequest == null) {
                throw new UserException(UserErrorStatus.SCHOOL_VERIFICATION_REQUIRED);
            }

            switch (latestRequest.getStatus()) {
                case PENDING -> throw new UserException(UserErrorStatus.SCHOOL_VERIFICATION_PENDING);
                case REJECTED -> throw new UserException(UserErrorStatus.SCHOOL_VERIFICATION_REJECTED);
                case APPROVED -> { /* 통과 */ }
                default -> throw new UserException(UserErrorStatus.SCHOOL_VERIFICATION_STATUS_INVALID);
            }
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        return new LoginResponseDTO(user.getId(), accessToken);
    }


    @Transactional(readOnly = true)
    public void logout(){
    }

    //승인 거절시 재요청
    public Long reapplyVerification(UserRequestDTO.VerificationReapply request) {

        //유저 조회 (이메일로 본인 식별)
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserException(UserErrorStatus.EMAIL_NOT_FOUND));

        //비밀번호 검증 (토큰 없이 호출되므로 본인확인 필수)
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UserException(UserErrorStatus.INVALID_PASSWORD);
        }

        //이미 인증 완료면 재요청 불가
        if (user.isVerified()) { // boolean verified면 Lombok getter가 isVerified()
            throw new UserException(UserErrorStatus.SCHOOL_VERIFICATION_ALREADY_APPROVED);
        }

        //이미 PENDING 요청이 있으면 중복 방지
        boolean hasPending = verificationRequestRepository.existsByUserAndStatus(user, VerificationStatus.PENDING);
        if (hasPending) {
            throw new UserException(UserErrorStatus.SCHOOL_VERIFICATION_PENDING);
        }

        //학교 조회
        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new SchoolException(SchoolErrorStatus.SCHOOL_NOT_FOUND));

        //새 요청 생성 (REJECTED였던 건 그대로 남기고 새 row 생성)
        UserSchoolVerificationRequest newRequest =
                UserSchoolVerificationRequest.builder()
                        .requestImageUrl(request.getStudentIdImageUrl())
                        .user(user)
                        .school(school)
                        .build();

        return verificationRequestRepository.save(newRequest).getId();
    }

}
