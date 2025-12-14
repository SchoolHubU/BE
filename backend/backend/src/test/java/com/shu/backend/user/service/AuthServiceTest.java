package com.shu.backend.user.service;

import com.shu.backend.domain.school.School;
import com.shu.backend.domain.school.SchoolRepository;
import com.shu.backend.domain.school.exception.SchoolException;
import com.shu.backend.domain.user.dto.LoginResponseDTO;
import com.shu.backend.domain.user.dto.SignUpResponseDTO;
import com.shu.backend.domain.user.dto.UserLoginDTO;
import com.shu.backend.domain.user.dto.UserRequestDTO;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.enums.UserRole;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.repository.UserRepository;
import com.shu.backend.domain.user.service.AuthService;
import com.shu.backend.domain.verification.entity.UserSchoolVerificationRequest;
import com.shu.backend.domain.verification.repository.UserSchoolVerificationRequestRepository;
import com.shu.backend.global.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private UserSchoolVerificationRequestRepository verificationRequestRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("회원가입 성공시 유저, 인증요청, 토큰이 제대로 생성된다")
    void join_success() {
        // given
        String email = "test@example.com";
        String nickname = "tester";
        String schoolName = "테스트고등학교";
        String rawPassword = "password1234";
        String encodedPassword = "encoded_pw";
        String requestImageUrl = "https://image.com/student-card.png";

        UserRequestDTO.SignUp request = UserRequestDTO.SignUp.builder()
                .username("홍길동")
                .email(email)
                .nickname(nickname)
                .school(schoolName)
                .password(rawPassword)
                .build();

        School school = School.builder()
                .id(1L)
                .name(schoolName)
                .build();

        // 이메일, 닉네임 중복 없음
        given(userRepository.existsByEmail(email)).willReturn(false);
        given(userRepository.existsByNickname(nickname)).willReturn(false);

        // 학교 조회 성공
        given(schoolRepository.findByName(schoolName)).willReturn(Optional.of(school));

        // 비밀번호 인코딩
        given(passwordEncoder.encode(rawPassword)).willReturn(encodedPassword);

        // userRepository.save() 가 저장된 User를 리턴하도록 설정
        given(userRepository.save(any(User.class)))
                .willAnswer(invocation -> {
                    User u = invocation.getArgument(0);
                    // id 자동 세팅된 것처럼 흉내
                    java.lang.reflect.Field idField = User.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(u, 100L);
                    return u;
                });

        // 토큰 발급
        given(jwtTokenProvider.createAccessToken(100L)).willReturn("fake-jwt-token");

        ArgumentCaptor<UserSchoolVerificationRequest> verificationCaptor =
                ArgumentCaptor.forClass(UserSchoolVerificationRequest.class);

        // when
        SignUpResponseDTO response = authService.join(request, requestImageUrl);

        // then
        //응답 검증
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(100L);
        assertThat(response.getAccessToken()).isEqualTo("fake-jwt-token");

        //유저 저장시 비밀번호와 프로필 이미지가 제대로 설정됐는지
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo(email);
        assertThat(savedUser.getNickname()).isEqualTo(nickname);
        assertThat(savedUser.getSchool()).isEqualTo(school);
        assertThat(savedUser.getPassword()).isEqualTo(encodedPassword);
        assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
        assertThat(savedUser.getProfileImageUrl()).isEqualTo(requestImageUrl);
        assertThat(savedUser.isVerified()).isFalse();

        //학교 인증 요청이 제대로 생성되었는지
        verify(verificationRequestRepository).save(verificationCaptor.capture());
        UserSchoolVerificationRequest savedRequest = verificationCaptor.getValue();

        assertThat(savedRequest.getUser()).isEqualTo(savedUser);
        assertThat(savedRequest.getSchool()).isEqualTo(school);
        assertThat(savedRequest.getRequestImageUrl()).isEqualTo(requestImageUrl);
        // 상태, requestedAt 기본값은 엔티티에서 세팅되므로 널이 아닌지만 체크해도 됨
        assertThat(savedRequest.getStatus()).isNotNull();
        assertThat(savedRequest.getRequestedAt()).isNotNull();
    }

    @Test
    @DisplayName("회원가입 시 이메일이 이미 존재하면 UserException(EXIST_EMAIL) 발생")
    void join_fail_existEmail() {
        // given
        UserRequestDTO.SignUp request = UserRequestDTO.SignUp.builder()
                .username("홍길동")
                .email("dup@example.com")
                .nickname("tester")
                .school("테스트고등학교")
                .password("pw1234")
                .build();

        given(userRepository.existsByEmail(request.getEmail())).willReturn(true);

        // when
        UserException ex = assertThrows(
                UserException.class,
                () -> authService.join(request, "any-url")
        );

        // then
        assertThat(ex.getErrorReasonHttpStatus().getCode()).isEqualTo("USER4001");

        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository, never()).existsByNickname(anyString());
        verify(schoolRepository, never()).findByName(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(verificationRequestRepository, never()).save(any(UserSchoolVerificationRequest.class));
    }

    @Test
    @DisplayName("회원가입 시 닉네임이 이미 존재하면 UserException(EXIST_NICKNAME) 발생")
    void join_fail_existNickname() {
        // given
        UserRequestDTO.SignUp request = UserRequestDTO.SignUp.builder()
                .username("홍길동")
                .email("test@example.com")
                .nickname("dupNick")
                .school("테스트고등학교")
                .password("pw1234")
                .build();

        given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(userRepository.existsByNickname(request.getNickname())).willReturn(true);

        // when
        UserException ex = assertThrows(
                UserException.class,
                () -> authService.join(request, "any-url")
        );

        // then
        assertThat(ex.getErrorReasonHttpStatus().getCode()).isEqualTo("USER4002");

        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository).existsByNickname(request.getNickname());
        verify(schoolRepository, never()).findByName(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(verificationRequestRepository, never()).save(any(UserSchoolVerificationRequest.class));
    }

    @Test
    @DisplayName("회원가입 시 학교를 찾지 못하면 SchoolException(SCHOOL_NOT_FOUND) 발생")
    void join_fail_schoolNotFound() {
        // given
        UserRequestDTO.SignUp request = UserRequestDTO.SignUp.builder()
                .username("홍길동")
                .email("test@example.com")
                .nickname("tester")
                .school("없는고등학교")
                .password("pw1234")
                .build();

        given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(userRepository.existsByNickname(request.getNickname())).willReturn(false);
        given(schoolRepository.findByName(request.getSchool())).willReturn(Optional.empty());

        // when
        SchoolException ex = assertThrows(
                SchoolException.class,
                () -> authService.join(request, "any-url")
        );

        // then
        assertThat(ex.getErrorReasonHttpStatus().getCode()).isEqualTo("SCHOOL4000");

        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository).existsByNickname(request.getNickname());
        verify(schoolRepository).findByName(request.getSchool());
        verify(userRepository, never()).save(any(User.class));
        verify(verificationRequestRepository, never()).save(any(UserSchoolVerificationRequest.class));
    }

    @Test
    @DisplayName("로그인 성공 - 이메일/비밀번호 일치 + 학교 인증 승인(APPROVED) 상태")
    void login_success_whenApproved() throws Exception {
        // given
        String email = "login@test.com";
        String rawPassword = "pw1234";
        String encodedPassword = "encoded_pw";

        UserLoginDTO request = new UserLoginDTO(email, rawPassword);

        // 유저 엔티티 생성
        User user = User.builder()
                .email(email)
                .password(encodedPassword)
                .role(UserRole.USER)
                .build();

        // 리플렉션으로 id 세팅 (회원가입 테스트와 패턴 맞춤)
        java.lang.reflect.Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, 200L);

        // 최신 인증 요청 (APPROVED)
        UserSchoolVerificationRequest verificationRequest =
                UserSchoolVerificationRequest.builder()
                        .requestImageUrl("https://image.com/card.png")
                        .user(user)
                        .school(School.builder().id(1L).name("테스트고").build())
                        .build();
        // 기본 status = PENDING이라면, approve로 상태 변경
        verificationRequest.approve(999L, "테스트 승인");

        // mocking
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(rawPassword, encodedPassword)).willReturn(true);
        given(verificationRequestRepository.findTopByUserOrderByRequestedAtDesc(user))
                .willReturn(Optional.of(verificationRequest));
        given(jwtTokenProvider.createAccessToken(200L)).willReturn("login-jwt-token");

        // when
        LoginResponseDTO response = authService.login(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(200L);
        assertThat(response.getAccessToken()).isEqualTo("login-jwt-token");

        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).matches(rawPassword, encodedPassword);
        verify(verificationRequestRepository).findTopByUserOrderByRequestedAtDesc(user);
        verify(jwtTokenProvider).createAccessToken(200L);
    }

    @Test
    @DisplayName("로그인 실패 - 이메일이 존재하지 않으면 UserException(EMAIL_NOT_FOUND) 발생")
    void login_fail_emailNotFound() {
        // given
        UserLoginDTO request = new UserLoginDTO("no@user.com", "pw1234");
        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.empty());

        // when
        UserException ex = assertThrows(UserException.class,
                () -> authService.login(request));

        // then
        // 코드/메시지는 실제 UserErrorStatus에 맞춰서 체크
        assertThat(ex.getErrorReasonHttpStatus().getCode()).isEqualTo("USER4003");
        verify(userRepository).findByEmail(request.getEmail());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(verificationRequestRepository, never()).findTopByUserOrderByRequestedAtDesc(any(User.class));
        verify(jwtTokenProvider, never()).createAccessToken(any());
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치 시 UserException(INVALID_PASSWORD) 발생")
    void login_fail_invalidPassword() {
        // given
        String email = "login@test.com";
        UserLoginDTO request = new UserLoginDTO(email, "wrongPw");

        User user = User.builder()
                .email(email)
                .password("encoded_pw")
                .role(UserRole.USER)
                .build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.getPassword(), user.getPassword())).willReturn(false);

        // when
        UserException ex = assertThrows(UserException.class,
                () -> authService.login(request));

        // then
        assertThat(ex.getErrorReasonHttpStatus().getCode()).isEqualTo("USER4004");
        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).matches(request.getPassword(), user.getPassword());
        verify(verificationRequestRepository, never()).findTopByUserOrderByRequestedAtDesc(any(User.class));
        verify(jwtTokenProvider, never()).createAccessToken(any());
    }

    @Test
    @DisplayName("로그인 실패 - 학교 인증 요청 자체가 없으면 SCHOOL_VERIFICATION_REQUIRED 발생")
    void login_fail_noVerificationRequest() {
        // given
        String email = "login@test.com";
        String rawPassword = "pw1234";
        String encodedPassword = "encoded_pw";

        UserLoginDTO request = new UserLoginDTO(email, rawPassword);

        User user = User.builder()
                .email(email)
                .password(encodedPassword)
                .role(UserRole.USER)
                .build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(rawPassword, encodedPassword)).willReturn(true);
        given(verificationRequestRepository.findTopByUserOrderByRequestedAtDesc(user))
                .willReturn(Optional.empty());

        // when
        UserException ex = assertThrows(UserException.class,
                () -> authService.login(request));

        // then
        assertThat(ex.getErrorReasonHttpStatus().getCode()).isEqualTo("USER4031");
        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).matches(rawPassword, encodedPassword);
        verify(verificationRequestRepository).findTopByUserOrderByRequestedAtDesc(user);
        verify(jwtTokenProvider, never()).createAccessToken(any());
    }


    @Test
    @DisplayName("로그인 실패 - 학교 인증 상태가 PENDING이면 SCHOOL_VERIFICATION_PENDING 발생")
    void login_fail_pendingVerification() {
        // given
        String email = "login@test.com";
        String rawPassword = "pw1234";
        String encodedPassword = "encoded_pw";

        UserLoginDTO request = new UserLoginDTO(email, rawPassword);

        User user = User.builder()
                .email(email)
                .password(encodedPassword)
                .role(UserRole.USER)
                .build();

        // PENDING 상태 요청
        UserSchoolVerificationRequest pendingRequest =
                UserSchoolVerificationRequest.builder()
                        .requestImageUrl("https://image.com/card.png")
                        .user(user)
                        .school(School.builder().id(1L).name("테스트고").build())
                        .build();
        // 기본값이 PENDING이면 그대로 사용

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(rawPassword, encodedPassword)).willReturn(true);
        given(verificationRequestRepository.findTopByUserOrderByRequestedAtDesc(user))
                .willReturn(Optional.of(pendingRequest));

        // when
        UserException ex = assertThrows(UserException.class,
                () -> authService.login(request));

        // then
        assertThat(ex.getErrorReasonHttpStatus().getCode()).isEqualTo("USER4032");
        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).matches(rawPassword, encodedPassword);
        verify(verificationRequestRepository).findTopByUserOrderByRequestedAtDesc(user);
        verify(jwtTokenProvider, never()).createAccessToken(any());
    }

    @Test
    @DisplayName("로그인 실패 - 학교 인증 상태가 REJECTED이면 SCHOOL_VERIFICATION_REJECTED 발생")
    void login_fail_rejectedVerification() {
        // given
        String email = "login@test.com";
        String rawPassword = "pw1234";
        String encodedPassword = "encoded_pw";

        UserLoginDTO request = new UserLoginDTO(email, rawPassword);

        User user = User.builder()
                .email(email)
                .password(encodedPassword)
                .role(UserRole.USER)
                .build();

        UserSchoolVerificationRequest rejectedRequest =
                UserSchoolVerificationRequest.builder()
                        .requestImageUrl("https://image.com/card.png")
                        .user(user)
                        .school(School.builder().id(1L).name("테스트고").build())
                        .build();
        rejectedRequest.reject(999L, "사진이 불명확합니다.");

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(rawPassword, encodedPassword)).willReturn(true);
        given(verificationRequestRepository.findTopByUserOrderByRequestedAtDesc(user))
                .willReturn(Optional.of(rejectedRequest));

        // when
        UserException ex = assertThrows(UserException.class,
                () -> authService.login(request));

        // then
        assertThat(ex.getErrorReasonHttpStatus().getCode()).isEqualTo("USER4033");
        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).matches(rawPassword, encodedPassword);
        verify(verificationRequestRepository).findTopByUserOrderByRequestedAtDesc(user);
        verify(jwtTokenProvider, never()).createAccessToken(any());
    }






}
