package com.shu.backend.domain.user.dto;

import com.shu.backend.domain.user.enums.Gender;
import com.shu.backend.domain.user.enums.Grade;
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

        // 성별/학년
        @NotNull(message = "성별은 필수입니다.")
        @Schema(description = "성별", example = "MALE", allowableValues = {"MALE", "FEMALE"})
        private Gender gender;

        @NotNull(message = "학년은 필수입니다.")
        @Schema(description = "학년", example = "FIRST", allowableValues = {"FIRST", "SECOND", "THIRD", "GRADUATED"})
        private Grade grade;

        @NotNull(message = "반은 필수입니다.")
        @Min(value = 1, message = "반은 1 이상이어야 합니다.")
        @Max(value = 20, message = "반은 20 이하여야 합니다.") // 학교마다 다르면 50 등으로 조정
        @Schema(description = "반", example = "3")
        private Integer classRoom;

        // 휴대폰 번호 (01012345678)
        @NotBlank(message = "휴대폰 번호는 필수입니다.")
        @Pattern(regexp = "^010\\d{8}$", message = "휴대폰 번호는 010으로 시작하는 11자리 숫자여야 합니다.")
        @Schema(description = "휴대폰 번호(하이픈 제거)", example = "01012345678")
        private String phoneNumber;

        //본인인증 완료 증거 토큰 (NICE든 SMS든 동일)
        @NotBlank(message = "본인인증 토큰이 필요합니다.")
        @Schema(description = "본인인증 완료 토큰", example = "verif_abc123...")
        private String verificationToken;
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
