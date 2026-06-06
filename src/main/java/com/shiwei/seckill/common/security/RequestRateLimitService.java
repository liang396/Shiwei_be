package com.shiwei.seckill.common.security;

import com.shiwei.seckill.common.exception.BizException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Component
public class RequestRateLimitService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void guard(String key, long limitPerMinute) {
        if (stringRedisTemplate == null) {
            return;
        }
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, 1, TimeUnit.MINUTES);
        }
        if (count != null && count > limitPerMinute) {
            throw new BizException("请求过于频繁，请稍后再试");
        }
    }
}

