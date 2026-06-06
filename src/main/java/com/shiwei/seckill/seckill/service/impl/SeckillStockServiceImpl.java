package com.shiwei.seckill.seckill.service.impl;

import com.shiwei.seckill.seckill.config.SeckillRedisKey;
import com.shiwei.seckill.seckill.service.SeckillStockService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SeckillStockServiceImpl implements SeckillStockService {
    private final Map<String, String> localStore = new ConcurrentHashMap<>();

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void rollbackStock(Long activityId, Long goodsId, Long userId, Integer buyNum) {
        if (stringRedisTemplate != null) {
            stringRedisTemplate.delete(SeckillRedisKey.userOrder(activityId, userId));
            stringRedisTemplate.opsForValue().increment(SeckillRedisKey.stock(activityId, goodsId), buyNum == null ? 0L : buyNum.longValue());
        }
        putResult(activityId, userId, "-1");
    }

    @Override
    public void markSeckillFailed(Long activityId, Long userId, String reason) {
        putResult(activityId, userId, "-1");
    }

    @Override
    public void markSeckillSuccess(Long activityId, Long userId, Long orderId) {
        putResult(activityId, userId, String.valueOf(orderId));
    }

    @Override
    public String getResult(Long activityId, Long userId) {
        String key = SeckillRedisKey.result(activityId, userId);
        if (stringRedisTemplate != null) {
            String v = stringRedisTemplate.opsForValue().get(key);
            if (v != null) {
                return v;
            }
        }
        return localStore.get(key);
    }

    private void putResult(Long activityId, Long userId, String value) {
        String key = SeckillRedisKey.result(activityId, userId);
        if (stringRedisTemplate != null) {
            stringRedisTemplate.opsForValue().set(key, value);
        }
        localStore.put(key, value);
    }
}

