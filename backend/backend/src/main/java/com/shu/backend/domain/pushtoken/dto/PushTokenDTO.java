package com.shu.backend.domain.pushtoken.dto;


import com.shu.backend.domain.pushtoken.enums.PushPlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

public class PushTokenDTO {

    @Getter
    public static class RegisterRequest {
        @NotBlank
        private String token;

        @NotNull
        private PushPlatform platform;
    }

    @Getter
    public static class RegisterResponse {
        private final boolean registered;
        public RegisterResponse(boolean registered) { this.registered = registered; }
    }
}