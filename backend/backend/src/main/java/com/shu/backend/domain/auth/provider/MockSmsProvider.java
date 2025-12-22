package com.shu.backend.domain.auth.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("local")
public class MockSmsProvider implements SmsProvider {

    @Override
    public void send(String phoneNumber, String message) {
        log.info("[DEV SMS] to={} | msg={}", phoneNumber, message);
    }
}