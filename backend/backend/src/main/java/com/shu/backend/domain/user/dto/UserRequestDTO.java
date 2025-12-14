package com.shu.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

public class UserRequestDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignUp {

        // 학교명: 최소 1자, 최대 30자, 특수문자 금지
        @NotBlank(message = "학교명은 필수 입력입니다.")
        @Size(max = 30, message = "학교명은 최대 30자까지 가능합니다.")
        @Pattern(regexp = "^[a-zA-Z가-힣0-9\\s]*$", message = "학교명에는 특수문자를 사용할 수 없습니다.")
        @Schema(description = "학교명 (영어/한글/숫자/공백, 최대 30자)", example = "서울고등학교")
        private String school;

        // 이름: 한글/영어만, 길이 제한
        @NotBlank(message = "이름은 필수 입력입니다.")
        @Size(max = 20, message = "이름은 최대 20자까지 가능합니다.")
        @Pattern(regexp = "^[a-zA-Z가-힣]*$", message = "이름은 한글과 영어만 가능합니다.")
        @Schema(description = "이름 (최대 20자, 한글·영어만 가능)", example = "김도윤")
        private String username;

        // 비밀번호: 길이 + 조합 검증
        @NotBlank(message = "비밀번호는 필수 입력입니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$",
                message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다."
        )
        @Schema(description = "비밀번호 (영문+숫자+특수문자 포함 8~20자)", example = "Abcd1234!")
        private String password;

        // 이메일: 표준 이메일 형식
        @NotBlank(message = "이메일은 필수 입력입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Schema(description = "사용자 이메일 (중복 불가)", example = "teenple@example.com")
        private String email;

        // 닉네임: 1~10자, 영어/한글만
        @NotBlank(message = "닉네임은 필수 입력입니다")
        @Size(max = 10, message = "닉네임은 최대 10자까지 가능합니다")
        @Pattern(regexp = "^[a-zA-Z가-힣]*$", message = "닉네임은 영어와 한글만 가능합니다")
        @Schema(description = "사용자 닉네임 (영어&한글만, 최대 10자, 중복 불가)", example = "틴플")
        private String nickname;

        // 프로필 이미지: URL 형식인지 검증
        @Pattern(
                regexp = "^(https?:\\/\\/)?([\\w.-]+)(:[0-9]+)?(\\/.*)?$",
                message = "프로필 이미지 URL 형식이 올바르지 않습니다."
        )
        @Schema(description = "프로필 이미지 URL (선택)", example = "https://cdn.teenple.com/profile/default.png")
        private String profileImageUrl;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class VerificationReapply {
        @NotBlank private String email;
        @NotBlank private String password;
        @NotNull  private Long schoolId;
        @NotBlank private String studentIdImageUrl;
    }

}
