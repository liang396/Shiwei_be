package com.shiwei.seckill.order.service.impl;

import com.shiwei.seckill.common.exception.BizException;
import com.shiwei.seckill.order.config.OrderRedisKey;
import com.shiwei.seckill.order.service.OrderDuplicateGuardService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Collections;

@Service
public class OrderDuplicateGuardServiceImpl implements OrderDuplicateGuardService {
    private static final String SUBMIT_VALUE = "submit";
    private static final String CANCEL_VALUE = "cancel";
    private static final long SUBMIT_TTL_MILLIS = 3000L;
    private static final long CANCEL_TTL_MILLIS = 3000L;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private DefaultRedisScript<Long> orderDuplicateGuardScript;

    @Override
    public void guardSubmit(Long userId) {
        boolean allowed = tryAcquire(OrderRedisKey.submitGuard(userId), SUBMIT_TTL_MILLIS, SUBMIT_VALUE);
        if (!allowed) {
            throw new BizException("请勿重复提交订单");
        }
    }

    @Override
    public void guardCancel(String orderNo) {
        boolean allowed = tryAcquire(OrderRedisKey.cancelGuard(orderNo), CANCEL_TTL_MILLIS, CANCEL_VALUE);
        if (!allowed) {
            throw new BizException("订单取消请求过于频繁");
        }
    }

    private boolean tryAcquire(String key, long ttlMillis, String value) {
        if (stringRedisTemplate == null || orderDuplicateGuardScript == null) {
            return true;
        }
        Long result = stringRedisTemplate.execute(
            orderDuplicateGuardScript,
            Collections.singletonList(key),
            String.valueOf(ttlMillis),
            value
        );
        return result == null || result == 1L;
    }
}

