package com.shu.backend.domain.user.service;

import com.shu.backend.domain.auth.service.VerificationService;
import com.shu.backend.domain.admin.service.AdminPushService;
import com.shu.backend.domain.notification.enums.NotificationTargetType;
import com.shu.backend.domain.notification.enums.NotificationType;
import com.shu.backend.domain.pushtoken.dto.PushTokenDTO;
import com.shu.backend.domain.pushtoken.enums.PushPlatform;
import com.shu.backend.domain.pushtoken.service.PushTokenService;
import com.shu.backend.domain.school.entity.School;
import com.shu.backend.domain.school.repository.SchoolRepository;
import com.shu.backend.domain.school.exception.SchoolException;
import com.shu.backend.domain.school.exception.status.SchoolErrorStatus;
import com.shu.backend.domain.user.dto.*;
import com.shu.backend.domain.user.entity.RefreshToken;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.enums.UserRole;
import com.shu.backend.domain.user.enums.UserStatus;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.exception.status.UserErrorStatus;
import com.shu.backend.domain.user.repository.RefreshTokenRepository;
import com.shu.backend.domain.user.repository.UserRepository;
import com.shu.backend.domain.usersetting.entity.UserSetting;
import com.shu.backend.domain.usersetting.repository.UserSettingRepository;
import com.shu.backend.domain.verification.entity.UserSchoolVerificationRequest;
import com.shu.backend.domain.verification.repository.UserSchoolVerificationRequestRepository;
import com.shu.backend.domain.verification.status.VerificationStatus;
import com.shu.backend.global.jwt.JwtProperties;
import com.shu.backend.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final UserSchoolVerificationRequestRepository verificationRequestRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserSettingRepository userSettingRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final VerificationService smsVerificationService;
    private final AdminPushService adminPushService;
    private final PushTokenService pushTokenService;

    private static final String ADMIN_SCHOOL_NAME = "운영자전용학교";

    // 회원가입
    public SignUpResponseDTO join(UserRequestDTO.SignUp request, String studentIdImageUrl) {
        validateSignUpRequest(request);

        smsVerificationService.verifyTokenOrThrow(
                request.getVerificationToken(),
                request.getEmail()
        );

        School school = getSchool(request);
        User newUser = createUser(request, school, true);
        userRepository.save(newUser);
        // 회원가입 시 알림 설정 기본값으로 자동 생성 (없으면 푸시 발송 조건에서 누락됨)
        userSettingRepository.save(UserSetting.create(newUser));
        createVerificationRequest(newUser, school, studentIdImageUrl);

        // 인증 승인 전에는 로그인이 차단되어 토큰을 등록할 기회가 없으므로,
        // 가입 시점에 FCM 토큰을 함께 등록해야 인증 결과 푸시를 받을 수 있다.
        registerPushTokenIfPresent(newUser.getId(), request.getFcmToken(), request.getFcmPlatform());

        String accessToken = jwtTokenProvider.createAccessToken(newUser.getId());
        String refreshToken = issueRefreshToken(newUser);

        log.info("User signed up: userId={}, schoolId={}, verificationRequestCreated=true",
                newUser.getId(), school.getId());

        return new SignUpResponseDTO(newUser.getId(), accessToken, refreshToken);
    }

    @Transactional(readOnly = true)
    public void validateSignUpBeforeStudentCardUpload(UserRequestDTO.SignUp request) {
        validateSignUpRequest(request);
        smsVerificationService.validateTokenOrThrow(
                request.getVerificationToken(),
                request.getEmail()
        );
        getSchool(request);
    }

    // 로그인
    @Transactional
    public LoginResponseDTO login(UserLoginDTO userLoginDTO) {
        String email = userLoginDTO.getEmail() == null ? "" : userLoginDTO.getEmail().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserException(UserErrorStatus.EMAIL_NOT_FOUND));
        assertActive(user);

        if (!passwordEncoder.matches(userLoginDTO.getPassword(), user.getPassword())) {
            log.warn("Login failed: userId={}, reason=invalid_password", user.getId());
            throw new UserException(UserErrorStatus.INVALID_PASSWORD);
        }

        if (user.getRole() == UserRole.USER) {
            if (user.isVerified()) {
                return buildLoginResponse(user);
            }

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

        return buildLoginResponse(user);
    }

    // Refresh token으로 새 토큰 발급 (rotation)
    @Transactional
    public TokenRefreshResponseDTO refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(rawRefreshToken)
                .orElseThrow(() -> new UserException(UserErrorStatus.INVALID_REFRESH_TOKEN));

        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw new UserException(UserErrorStatus.EXPIRED_REFRESH_TOKEN);
        }

        User user = stored.getUser();
        assertActive(user);

        // 새 토큰 발급 (issueRefreshToken 내부에서 deleteByUser로 기존 토큰 전부 삭제)
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId());
        String newRefreshToken = issueRefreshToken(user);

        log.info("Token refreshed: userId={}", user.getId());

        return new TokenRefreshResponseDTO(newAccessToken, newRefreshToken);
    }

    // 로그아웃 - refresh token 무효화
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByToken(rawRefreshToken)
                .ifPresent(refreshToken -> {
                    Long userId = refreshToken.getUser() != null ? refreshToken.getUser().getId() : null;
                    refreshTokenRepository.delete(refreshToken);
                    log.info("User logged out: userId={}", userId);
                });
    }

    // 이메일 존재 여부 확인
    @Transactional(readOnly = true)
    public EmailCheckResponseDTO checkEmailExists(String email) {
        boolean exists = userRepository.existsByEmail(email);
        return new EmailCheckResponseDTO(exists);
    }

    public void validateSignUpRequest(UserRequestDTO.SignUp request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserException(UserErrorStatus.EXIST_EMAIL);
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new UserException(UserErrorStatus.EXIST_NICKNAME);
        }
    }

    public School getSchoolByName(String schoolName) {
        School school = schoolRepository.findFirstByNameOrderByIdAsc(schoolName)
                .orElseThrow(() -> new SchoolException(SchoolErrorStatus.SCHOOL_NOT_FOUND));
        validateSchoolForSignUp(school);
        return school;
    }

    public School getSchool(UserRequestDTO.SignUp request) {
        if (request.getSchoolId() != null) {
            School school = schoolRepository.findById(request.getSchoolId())
                    .orElseThrow(() -> new SchoolException(SchoolErrorStatus.SCHOOL_NOT_FOUND));
            validateSchoolForSignUp(school);
            return school;
        }
        return getSchoolByName(request.getSchool());
    }

    private void validateSchoolForSignUp(School school) {
        if (ADMIN_SCHOOL_NAME.equals(school.getName())) {
            throw new SchoolException(SchoolErrorStatus.INVALID_SCHOOL_FOR_SIGNUP);
        }
    }

    public User createUser(UserRequestDTO.SignUp request, School school, boolean phoneVerified) {
        return User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .nickname(request.getNickname())
                .school(school)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .verified(false)
                .gender(request.getGender())
                .grade(request.getGrade())
                .phoneNumber(request.getPhoneNumber())
                .profileImageUrl(
                        request.getProfileImageUrl() != null ? request.getProfileImageUrl() : "default_profile.png"
                )
                .phoneVerified(phoneVerified)
                .build();
    }

    public void createVerificationRequest(User user, School school, String imageUrl) {
        UserSchoolVerificationRequest verificationRequest = new UserSchoolVerificationRequest(imageUrl, user, school);
        UserSchoolVerificationRequest saved = verificationRequestRepository.save(verificationRequest);
        notifyVerificationRequest(saved, school.getName());
    }

    @Transactional(readOnly = true)
    public VerificationReapplyInfoResponseDTO getReapplyInfo(
            UserRequestDTO.VerificationReapplyInfoRequest request
    ) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserException(UserErrorStatus.EMAIL_NOT_FOUND));
        assertActive(user);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UserException(UserErrorStatus.INVALID_PASSWORD);
        }

        if (user.isVerified()) {
            throw new UserException(UserErrorStatus.SCHOOL_VERIFICATION_ALREADY_APPROVED);
        }

        UserSchoolVerificationRequest latestRequest =
                verificationRequestRepository.findTopByUserOrderByRequestedAtDesc(user)
                        .orElseThrow(() -> new UserException(UserErrorStatus.SCHOOL_VERIFICATION_REQUIRED));

        switch (latestRequest.getStatus()) {
            case REJECTED -> {
                return new VerificationReapplyInfoResponseDTO(
                        latestRequest.getSchool().getId(),
                        latestRequest.getSchool().getName(),
                        latestRequest.getAdminComment()
                );
            }
            case PENDING -> throw new UserException(UserErrorStatus.SCHOOL_VERIFICATION_PENDING);
            case APPROVED -> throw new UserException(UserErrorStatus.SCHOOL_VERIFICATION_ALREADY_APPROVED);
            default -> throw new UserException(UserErrorStatus.SCHOOL_VERIFICATION_STATUS_INVALID);
        }
    }

    public Long reapplyVerification(
            UserRequestDTO.VerificationReapply request,
            String studentIdImageUrl
    ) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserException(UserErrorStatus.EMAIL_NOT_FOUND));
        assertActive(user);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UserException(UserErrorStatus.INVALID_PASSWORD);
        }

        if (user.isVerified()) {
            throw new UserException(UserErrorStatus.SCHOOL_VERIFICATION_ALREADY_APPROVED);
        }

        boolean hasPending = verificationRequestRepository.existsByUserAndStatus(user, VerificationStatus.PENDING);
        if (hasPending) {
            throw new UserException(UserErrorStatus.SCHOOL_VERIFICATION_PENDING);
        }

        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new SchoolException(SchoolErrorStatus.SCHOOL_NOT_FOUND));
        validateSchoolForSignUp(school);

        UserSchoolVerificationRequest newRequest =
                UserSchoolVerificationRequest.builder()
                        .requestImageUrl(studentIdImageUrl)
                        .user(user)
                        .school(school)
                        .build();

        Long requestId = verificationRequestRepository.save(newRequest).getId();
        notifyVerificationRequest(requestId, school.getName());

        // 재신청도 로그인 없이 진행되므로 앱 재설치 등으로 바뀐 토큰을 여기서 갱신한다.
        registerPushTokenIfPresent(user.getId(), request.getFcmToken(), request.getFcmPlatform());

        log.info("School verification reapplied: userId={}, schoolId={}, requestId={}",
                user.getId(), school.getId(), requestId);
        return requestId;
    }

    /**
     * 가입/재신청 요청에 FCM 토큰이 포함된 경우 푸시 토큰을 등록한다.
     *
     * 토큰 등록 실패가 가입/재신청 자체를 막아서는 안 되므로 예외는 로그만 남기고 삼킨다.
     * (등록에 실패하면 인증 결과 푸시만 못 받을 뿐, 알림함에는 정상 기록된다)
     */
    private void registerPushTokenIfPresent(Long userId, String fcmToken, PushPlatform platform) {
        if (fcmToken == null || fcmToken.isBlank()) {
            return;
        }
        if (platform == null) {
            log.warn("Signup push token skipped: userId={}, reason=platform_missing", userId);
            return;
        }
        try {
            pushTokenService.registerOrUpdate(userId, new PushTokenDTO.RegisterRequest(fcmToken, platform));
        } catch (Exception e) {
            log.warn("Signup push token register failed: userId={}, platform={}", userId, platform, e);
        }
    }

    private void notifyVerificationRequest(UserSchoolVerificationRequest request, String schoolName) {
        notifyVerificationRequest(request.getId(), schoolName);
    }

    private void notifyVerificationRequest(Long requestId, String schoolName) {
        try {
            // Admin notification must not roll back signup or verification reapply.
            adminPushService.notifyActiveAdmins(
                    NotificationType.ADMIN_VERIFICATION,
                    NotificationTargetType.VERIFICATION_REQUEST,
                    requestId,
                    "새 학교 인증 요청",
                    schoolName + " 인증 요청이 접수되었습니다.",
                    null
            );
        } catch (Exception e) {
            log.warn(
                    "Admin verification notification failed: requestId={}, schoolName={}",
                    requestId,
                    schoolName,
                    e
            );
        }
    }

    @Transactional(readOnly = true)
    public PhoneCheckResponseDTO checkPhoneNumberExists(String phoneNumber) {
        String normalizedPhoneNumber = normalizePhoneNumber(phoneNumber);
        boolean exists = userRepository.existsByPhoneNumber(normalizedPhoneNumber);
        return new PhoneCheckResponseDTO(exists);
    }

    @Transactional(readOnly = true)
    public NicknameCheckResponseDTO checkNicknameExists(String nickname) {
        boolean exists = userRepository.existsByNickname(nickname);
        return new NicknameCheckResponseDTO(exists);
    }

    // 아이디(이메일) 찾기
    @Transactional(readOnly = true)
    public FindEmailResponseDTO findEmail(FindEmailRequestDTO request) {
        String normalized = normalizePhoneNumber(request.getPhoneNumber());
        User user = userRepository.findByPhoneNumber(normalized)
                .orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));
        assertActive(user);

        if (!user.getUsername().equals(request.getUsername())) {
            throw new UserException(UserErrorStatus.USER_NOT_FOUND);
        }

        return new FindEmailResponseDTO(maskEmail(user.getEmail()));
    }

    // 비밀번호 재설정 인증번호 발송
    public void sendPasswordResetCode(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new UserException(UserErrorStatus.EMAIL_NOT_FOUND);
        }
        smsVerificationService.sendPasswordResetCode(email);
    }

    // 비밀번호 재설정
    @Transactional
    public void resetPassword(ResetPasswordRequestDTO request) {
        String email = smsVerificationService.consumeToken(request.getVerificationToken());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserException(UserErrorStatus.EMAIL_NOT_FOUND));
        assertActive(user);

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new UserException(UserErrorStatus.SAME_PASSWORD);
        }

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        refreshTokenRepository.deleteByUser(user);
        log.info("Password reset completed: userId={}", user.getId());
    }

    // =================== private ===================

    private LoginResponseDTO buildLoginResponse(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = issueRefreshToken(user);
        Long schoolId = user.getSchool() != null ? user.getSchool().getId() : null;
        log.info("Login succeeded: userId={}, role={}, schoolId={}", user.getId(), user.getRole().name(), schoolId);
        return new LoginResponseDTO(user.getId(), accessToken, refreshToken, user.getRole().name(), schoolId);
    }

    private void assertActive(User user) {
        if (user.getStatus() == null) return;
        // 탈퇴 유예 기간 중인 계정은 별도 에러 코드로 분리해 프론트에서 복구 화면으로 분기한다.
        if (user.getStatus() == UserStatus.PENDING_DELETION) {
            throw new UserException(UserErrorStatus.ACCOUNT_PENDING_DELETION);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserException(UserErrorStatus.INACTIVE_USER);
        }
    }

    /**
     * 탈퇴 유예 기간 중 계정 복구.
     * 이메일·비밀번호로 본인 확인 후 ACTIVE 상태로 되돌리고 새 토큰을 발급한다.
     */
    @Transactional
    public LoginResponseDTO restoreAccount(UserLoginDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserException(UserErrorStatus.EMAIL_NOT_FOUND));

        // PENDING_DELETION 상태인 계정만 복구 가능
        if (user.getStatus() != UserStatus.PENDING_DELETION) {
            throw new UserException(UserErrorStatus.INACTIVE_USER);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UserException(UserErrorStatus.INVALID_PASSWORD);
        }

        user.restore();
        log.info("Account restored: userId={}", user.getId());
        return buildLoginResponse(user);
    }

    // 새 refresh token 발급 (기존 것 대체)
    private String issueRefreshToken(User user) {
        // 기존 refresh token 모두 삭제 (1기기 1토큰 정책)
        refreshTokenRepository.deleteByUser(user);

        String tokenValue = UUID.randomUUID().toString();
        long expirationMs = jwtProperties.getRefreshTokenExpiration();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expirationMs / 1000);

        refreshTokenRepository.save(new RefreshToken(tokenValue, user, expiresAt));
        return tokenValue;
    }

    private String normalizePhoneNumber(String phoneNumber) {
        return phoneNumber.replaceAll("[^0-9]", "");
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return email;
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        return local.substring(0, 2) + "*".repeat(local.length() - 2) + domain;
    }
}
