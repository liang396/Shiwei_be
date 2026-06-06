package com.shiwei.seckill.order.cache;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;

@Component
public class OrderCacheInvalidationListener implements MessageListener {
    @Resource
    private Cache<Object, Object> caffeineBuilder;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        if (message == null || message.getBody() == null) {
            return;
        }
        String cacheKey = new String(message.getBody(), StandardCharsets.UTF_8);
        caffeineBuilder.invalidate(cacheKey);
    }
}

