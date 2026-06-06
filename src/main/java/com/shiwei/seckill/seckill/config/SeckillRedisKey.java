package com.shiwei.seckill.seckill.config;

public final class SeckillRedisKey {
    private SeckillRedisKey() {
    }

    public static String activity(Long activityId) {
        return "seckill:activity:" + activityId;
    }

    public static String stock(Long activityId, Long goodsId) {
        return "seckill:stock:" + activityId + ":" + goodsId;
    }

    public static String userOrder(Long activityId, Long userId) {
        return "seckill:order:user:" + activityId + ":" + userId;
    }

    public static String result(Long activityId, Long userId) {
        return "seckill:result:" + activityId + ":" + userId;
    }

    public static String consume(String messageId) {
        return "seckill:mq:consume:" + messageId;
    }
}

