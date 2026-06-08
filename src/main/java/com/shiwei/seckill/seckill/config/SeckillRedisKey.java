package com.shiwei.seckill.seckill.config;

public final class SeckillRedisKey {
    private static final int USER_ORDER_SHARD_COUNT = 32;

    private SeckillRedisKey() {
    }

    public static String activity(Long activityId) {
        return "seckill:activity:" + activityId;
    }

    public static String stock(Long activityId, Long goodsId) {
        return "seckill:stock:" + activityId + ":" + goodsId;
    }

    public static String goodsSnapshot(Long activityId, Long goodsId) {
        return "seckill:goods:snapshot:" + activityId + ":" + goodsId;
    }

    public static String userOrder(Long activityId, Long userId) {
        return userOrderShard(activityId, userId);
    }

    public static String userOrderShard(Long activityId, Long userId) {
        return "seckill:order:user:" + activityId + ":" + userOrderShardIndex(userId);
    }

    public static int userOrderShardIndex(Long userId) {
        return Math.floorMod(userId == null ? 0L : userId, USER_ORDER_SHARD_COUNT);
    }

    public static String result(Long activityId, Long userId) {
        return "seckill:result:" + activityId + ":" + userId;
    }

    public static String consume(String messageId) {
        return "seckill:mq:consume:" + messageId;
    }
}

