package com.shu.backend.global.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class SwaggerConfig {

    private static final String DOMAIN_BASE = "com.shu.backend.domain";

    // 전체 공통 문서 정보
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SchoolHubU API")
                        .version("v1")
                        .description("SchoolHubU 개발용 Swagger API 문서")
                );
    }

    // ================== 도메인별 그룹 설정 ==================

    // 유저 관련 (user, usersetting, school verification)
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("User")
                .packagesToScan(
                        DOMAIN_BASE + ".user",
                        DOMAIN_BASE + ".usersetting",
                        DOMAIN_BASE + ".userschoolverification",
                        DOMAIN_BASE + ".userschoolverificationrequest"
                )
                .build();
    }

    // 학교 / 게시판 (board, school)
    @Bean
    public GroupedOpenApi boardSchoolApi() {
        return GroupedOpenApi.builder()
                .group("Board & School")
                .packagesToScan(
                        DOMAIN_BASE + ".board",
                        DOMAIN_BASE + ".school"
                )
                .build();
    }

    //  게시글 / 댓글 / 반응 / 미디어
    @Bean
    public GroupedOpenApi postApi() {
        return GroupedOpenApi.builder()
                .group("Post & Comment")
                .packagesToScan(
                        DOMAIN_BASE + ".post",
                        DOMAIN_BASE + ".comment",
                        DOMAIN_BASE + ".reaction",
                        DOMAIN_BASE + ".media"
                )
                .build();
    }

    // 채팅 관련 (채팅방, 메시지, 유저-방 매핑)
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

    // 알림
    @Bean
    public GroupedOpenApi notificationApi() {
        return GroupedOpenApi.builder()
                .group("Notification")
                .packagesToScan(DOMAIN_BASE + ".notification")
                .build();
    }

    // 신고 / 제재
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



}