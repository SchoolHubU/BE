package com.shu.backend.user.service;

import com.shu.backend.domain.school.School;
import com.shu.backend.domain.school.SchoolRepository;
import com.shu.backend.domain.school.exception.SchoolException;
import com.shu.backend.domain.user.dto.SignUpResponseDTO;
import com.shu.backend.domain.user.dto.UserRequestDTO;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.enums.UserRole;
import com.shu.backend.domain.user.exception.UserException;
import com.shu.backend.domain.user.repository.UserRepository;
import com.shu.backend.domain.user.service.AuthService;
import com.shu.backend.domain.userschoolverificationrequest.entity.UserSchoolVerificationRequest;
import com.shu.backend.domain.userschoolverificationrequest.repository.UserSchoolVerificationRequestRepository;
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
        given(userRepository.existByNickname(nickname)).willReturn(false);

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
        assertThat(ex.getErrorReasonHttpStatus().getCode()).isEqualTo("USER4041");

        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository, never()).existByNickname(anyString());
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
        given(userRepository.existByNickname(request.getNickname())).willReturn(true);

        // when
        UserException ex = assertThrows(
                UserException.class,
                () -> authService.join(request, "any-url")
        );

        // then
        assertThat(ex.getErrorReasonHttpStatus().getCode()).isEqualTo("USER4042");

        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository).existByNickname(request.getNickname());
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
        given(userRepository.existByNickname(request.getNickname())).willReturn(false);
        given(schoolRepository.findByName(request.getSchool())).willReturn(Optional.empty());

        // when
        SchoolException ex = assertThrows(
                SchoolException.class,
                () -> authService.join(request, "any-url")
        );

        // then
        assertThat(ex.getErrorReasonHttpStatus().getCode()).isEqualTo("SCHOOL4040");

        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository).existByNickname(request.getNickname());
        verify(schoolRepository).findByName(request.getSchool());
        verify(userRepository, never()).save(any(User.class));
        verify(verificationRequestRepository, never()).save(any(UserSchoolVerificationRequest.class));
    }
}
