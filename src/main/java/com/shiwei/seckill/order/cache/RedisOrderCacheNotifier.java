package com.shiwei.seckill.order.cache;

import com.shiwei.seckill.order.config.OrderCacheConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class RedisOrderCacheNotifier implements OrderCacheNotifier {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void invalidate(String cacheKey) {
        if (stringRedisTemplate == null || cacheKey == null) {
            return;
        }
        stringRedisTemplate.convertAndSend(OrderCacheConstants.ORDER_CACHE_INVALIDATE_CHANNEL, cacheKey);
    }
}
