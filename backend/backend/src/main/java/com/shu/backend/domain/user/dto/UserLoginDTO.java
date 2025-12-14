package com.shu.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class UserLoginDTO {
    // 이메일: 표준 이메일 형식
    @NotBlank(message = "이메일은 필수 입력입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Schema(description = "사용자 이메일 ", example = "teenple@example.com")
    private String email;

    @Schema(description = "비밀번호 (영문+숫자+특수문자 포함 8~20자)", example = "Abcd1234!")
    private String password;
}
