package com.shiwei.seckill.order.cache;

public interface OrderCacheNotifier {
    void invalidate(String cacheKey);
}

