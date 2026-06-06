package com.shiwei.seckill.auth.service.impl;

import com.shiwei.seckill.auth.service.SmsCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SmsCodeServiceImpl implements SmsCodeService {
    private static final long EXPIRE_SECONDS = 300L;
    private static final String CODE_KEY_PREFIX = "auth:sms_code:";

    private final StringRedisTemplate stringRedisTemplate;
    private final Random random = new Random();

    @Override
    public String sendCode(String phone) {
        String code = String.format("%04d", random.nextInt(10000));
        stringRedisTemplate.opsForValue().set(buildKey(phone), code, EXPIRE_SECONDS, TimeUnit.SECONDS);
        return code;
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        String cacheCode = stringRedisTemplate.opsForValue().get(buildKey(phone));
        if (cacheCode == null) {
            return false;
        }
        boolean matched = cacheCode.equals(code);
        if (matched) {
            stringRedisTemplate.delete(buildKey(phone));
        }
        return matched;
    }

    private String buildKey(String phone) {
        return CODE_KEY_PREFIX + phone;
    }
}

