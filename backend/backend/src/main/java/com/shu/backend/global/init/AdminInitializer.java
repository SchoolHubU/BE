package com.shu.backend.global.init;

import com.shu.backend.domain.school.entity.School;
import com.shu.backend.domain.school.repository.SchoolRepository;
import com.shu.backend.domain.user.entity.User;
import com.shu.backend.domain.user.enums.UserRole;
import com.shu.backend.domain.user.enums.UserStatus;
import com.shu.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private static final String ADMIN_SCHOOL_NAME = "운영자전용학교";
    private static final String TEST_SCHOOL_NAME = "오남고등학교";

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {

        // 테스트용 학교(오남고등학교) 생성 or 조회
        schoolRepository.findByName(TEST_SCHOOL_NAME)
                .orElseGet(() -> schoolRepository.save(
                        School.builder()
                                .name(TEST_SCHOOL_NAME)
                                .logoImageUrl(null)
                                .build()
                ));

        // 운영자 전용 학교 생성 or 조회
        School adminSchool = schoolRepository.findByName(ADMIN_SCHOOL_NAME)
                .orElseGet(() -> schoolRepository.save(
                        School.builder()
                                .name(ADMIN_SCHOOL_NAME)
                                .logoImageUrl(null)
                                .build()
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
                    .build();

            userRepository.save(admin);
        }
    }
}