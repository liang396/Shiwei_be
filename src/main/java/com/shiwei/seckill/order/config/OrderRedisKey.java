package com.shiwei.seckill.order.config;

public final class OrderRedisKey {
    public static final String ORDER_TIMEOUT_ZSET = "order:timeout:zset";

    private OrderRedisKey() {
    }

    public static String submitGuard(Long userId) {
        return "order:guard:submit:" + userId;
    }

    public static String cancelGuard(String orderNo) {
        return "order:guard:cancel:" + orderNo;
    }
}
