package com.shiwei.seckill.order.service;

public interface OrderTimeoutService {
    void registerOrderTimeout(String orderNo, long expireAtMillis);

    void cancelExpiredOrders();
}

