package com.shu.backend.domain.auth.store;


import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class SmsCodeStore {

    private final StringRedisTemplate redisTemplate;

    public SmsCodeStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String key(String phone) {
        return "sms:code:" + phone;
    }

    public void save(String phone, String code) {
        redisTemplate.opsForValue().set(key(phone), code, 3, TimeUnit.MINUTES);
    }

    public String get(String phone) {
        return redisTemplate.opsForValue().get(key(phone));
    }

    public void delete(String phone) {
        redisTemplate.delete(key(phone));
    }
}