package com.shu.backend.domain.auth.provider;

public interface SmsProvider {
    void send(String phoneNumber, String message);
}