package com.shu.backend.global.init;

import com.shu.backend.domain.board.entity.Board;
import com.shu.backend.domain.board.enums.BoardScope;
import com.shu.backend.domain.board.repository.BoardRepository;
import com.shu.backend.domain.region.entity.Region;
import com.shu.backend.domain.region.repository.RegionRepository;
import com.shu.backend.domain.school.entity.School;
import com.shu.backend.domain.school.repository.SchoolRepository;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.enums.Gender;
import com.shu.backend.domain.user.enums.Grade;
import com.shu.backend.domain.user.enums.UserRole;
import com.shu.backend.domain.user.enums.UserStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private static final String ADMIN_SCHOOL_NAME = "운영자전용학교";
    private static final String TEST_SCHOOL_NAME = "오남고등학교";

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegionRepository regionRepository;
    private final BoardRepository boardRepository;

    @Override
    @Transactional
    public void run(String... args) {

        // 테스트용 학교(오남고등학교) 생성 or 조회
        School testSchool = schoolRepository.findByName(TEST_SCHOOL_NAME)
                .orElseGet(() -> schoolRepository.save(
                        School.builder()
                                .name(TEST_SCHOOL_NAME)
                                .logoImageUrl(null)
                                .build()
                ));

        Region adminRegion = regionRepository.save(
                Region.builder()
                        .name("의정부시")
                        .build()
        );

        // 운영자 전용 학교 생성 or 조회
        School adminSchool = schoolRepository.findByName(ADMIN_SCHOOL_NAME)
                .orElseGet(() -> schoolRepository.save(
                        School.builder()
                                .name(ADMIN_SCHOOL_NAME)
                                .region(adminRegion)
                                .logoImageUrl(null)
                                .build()
                ));

        boardRepository.saveAll(List.of(
                Board.builder().title("자유게시판").description("자유롭게 이야기해요").school(adminSchool).scope(BoardScope.SCHOOL).build(),
                Board.builder().title("1학년 게시판").description("1학년 전용 게시판").school(adminSchool).scope(BoardScope.SCHOOL).build(),
                Board.builder().title("2학년 게시판").description("2학년 전용 게시판").school(adminSchool).scope(BoardScope.SCHOOL).build(),
                Board.builder().title("3학년 게시판").description("3학년 전용 게시판").school(adminSchool).scope(BoardScope.SCHOOL).build(),
                Board.builder().title("졸업생 게시판").description("졸업생 전용 게시판").school(adminSchool).scope(BoardScope.SCHOOL).build(),
                Board.builder().title("지역 게시판").description("같은 지역 학생들과 이야기해요").region(adminSchool.getRegion()).scope(BoardScope.REGION).build()
        ));

        // ADMIN 계정이 하나도 없으면 기본 관리자 생성
        boolean existsAdmin = userRepository.existsByRole(UserRole.ADMIN);

        if (!existsAdmin) {
            User admin = User.builder()
                    .username("시스템관리자1")
                    .email("leejd8130@naver.com")
                    .nickname("admin1")
                    .password(passwordEncoder.encode("dhkdrl12"))
                    .school(adminSchool)
                    .role(UserRole.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .verified(true)
                    .profileImageUrl(null)
                    .classRoom(3)
                    .grade(Grade.FIRST)
                    .phoneNumber("01053468130")
                    .gender(Gender.MALE)
                    .build();

            userRepository.save(admin);
        }

        // =========================
        // ✅ 오남고 테스트 유저 2명 자동 생성
        // =========================
        createTestUserIfNotExists(
                "테스트유저1",
                "teenple1@example.com",
                "test1",
                "Abcd1234!",
                testSchool
        );

        createTestUserIfNotExists(
                "테스트유저2",
                "teenple2@example.com",
                "test2",
                "Abcd1234!",
                testSchool
        );
    }

    private void createTestUserIfNotExists(
            String username,
            String email,
            String nickname,
            String rawPassword,
            School school
    ) {
        if (userRepository.existsByEmail(email)) return; // 또는 nickname도 같이 체크 추천

        User user = User.builder()
                .username(username)
                .email(email)
                .nickname(nickname)
                .password(passwordEncoder.encode(rawPassword))
                .school(school)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .verified(true)          // 바로 채팅 테스트하려면 true가 편함
                .profileImageUrl(null)   // null이면 기본값/프론트 처리 확인
                .classRoom(3)
                .grade(Grade.FIRST)
                .phoneNumber("01053468130")
                .gender(Gender.MALE)
                .phoneVerified(true)
                .build();

        userRepository.save(user);
    }

}