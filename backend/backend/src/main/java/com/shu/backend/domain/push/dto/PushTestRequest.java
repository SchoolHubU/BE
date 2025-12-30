package com.shu.backend.domain.push.dto;

import lombok.Getter;

@Getter
public class PushTestRequest {
    private Long userId;
    private String title;
    private String body;
}