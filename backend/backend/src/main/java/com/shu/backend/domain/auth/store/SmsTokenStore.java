package com.shu.backend.domain.auth.store;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class SmsTokenStore {

    private final StringRedisTemplate redisTemplate;

    public SmsTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String key(String token) {
        return "sms:verify:" + token;
    }

    public void save(String token, String phone) {
        redisTemplate.opsForValue().set(key(token), phone, 5, TimeUnit.MINUTES);
    }

    public String get(String token) {
        return redisTemplate.opsForValue().get(key(token));
    }

    public void consume(String token) {
        redisTemplate.delete(key(token));
    }
}