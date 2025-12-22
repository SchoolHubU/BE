package com.shu.backend.global.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class SwaggerConfig {

    private static final String DOMAIN_BASE = "com.shu.backend.domain";
    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SchoolHubU API")
                        .version("v1")
                        .description("SchoolHubU 개발용 Swagger API 문서")
                )
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components().addSecuritySchemes(
                        SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ));
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("User")
                .packagesToScan(
                        DOMAIN_BASE + ".user",
                        DOMAIN_BASE + ".usersetting",
                        DOMAIN_BASE + ".verification"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi boardSchoolRegionApi() {
        return GroupedOpenApi.builder()
                .group("Board & School & Region")
                .packagesToScan(
                        DOMAIN_BASE + ".board",
                        DOMAIN_BASE + ".school",
                        DOMAIN_BASE + ".region"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi postApi() {
        return GroupedOpenApi.builder()
                .group("Post & Comment & Reaction & Media")
                .packagesToScan(
                        DOMAIN_BASE + ".post",
                        DOMAIN_BASE + ".comment",
                        DOMAIN_BASE + ".reaction",
                        DOMAIN_BASE + ".media"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi chatApi() {
        return GroupedOpenApi.builder()
                .group("Chat")
                .packagesToScan(
                        DOMAIN_BASE + ".chatroom",
                        DOMAIN_BASE + ".chatroomuser",
                        DOMAIN_BASE + ".chatmessage"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi notificationApi() {
        return GroupedOpenApi.builder()
                .group("Notification")
                .packagesToScan(DOMAIN_BASE + ".notification")
                .build();
    }

    @Bean
    public GroupedOpenApi reportPenaltyApi() {
        return GroupedOpenApi.builder()
                .group("Report & Penalty")
                .packagesToScan(
                        DOMAIN_BASE + ".report",
                        DOMAIN_BASE + ".penalty"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("Auth")
                .packagesToScan(
                        DOMAIN_BASE + ".auth",   //  SMS 인증, 로그인, 회원가입
                        DOMAIN_BASE + ".user"    // AuthController가 user 패키지에 있다면
                )
                .build();
    }

}