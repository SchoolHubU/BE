package com.shu.backend.domain.auth.setting;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sms.sens")
public record SensProperties(
        String accessKey,
        String secretKey,
        String serviceId,
        String from
) {}