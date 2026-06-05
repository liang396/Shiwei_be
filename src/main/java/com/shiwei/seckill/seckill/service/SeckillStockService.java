package com.shiwei.seckill.seckill.service;

public interface SeckillStockService {
    void rollbackStock(Long activityId, Long goodsId, Long userId, Integer buyNum);

    void markSeckillFailed(Long activityId, Long userId, String reason);

    void markSeckillSuccess(Long activityId, Long userId, Long orderId);

    String getResult(Long activityId, Long userId);
}
