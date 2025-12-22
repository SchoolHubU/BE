package com.shu.backend.domain.auth.setting;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SensProperties.class)
public class SensConfig {
}
