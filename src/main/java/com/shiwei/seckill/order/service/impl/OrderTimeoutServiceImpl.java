package com.shiwei.seckill.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiwei.seckill.order.config.OrderRedisKey;
import com.shiwei.seckill.order.entity.OrderEntity;
import com.shiwei.seckill.order.enums.OperatorTypeEnum;
import com.shiwei.seckill.order.enums.OrderEventEnum;
import com.shiwei.seckill.order.enums.OrderStatusEnum;
import com.shiwei.seckill.order.mapper.OrderMapper;
import com.shiwei.seckill.order.service.OrderStateMachineService;
import com.shiwei.seckill.order.service.OrderTimeoutService;
import com.shiwei.seckill.order.service.support.OrderOperateContext;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class OrderTimeoutServiceImpl implements OrderTimeoutService {
    private static final long DEFAULT_DELAY_MILLIS = 30L * 60L * 1000L;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private OrderMapper orderMapper;
    @Resource
    private OrderStateMachineService orderStateMachineService;
    @Autowired(required = false)
    private RedissonClient redissonClient;

    @Override
    public void registerOrderTimeout(String orderNo, long expireAtMillis) {
        if (orderNo == null) {
            return;
        }

        long delayMillis = Math.max(expireAtMillis - System.currentTimeMillis(), 1L);
        if (redissonClient != null) {
            RBlockingDeque<String> blockingDeque = redissonClient.getBlockingDeque(OrderRedisKey.ORDER_TIMEOUT_DELAY_QUEUE);
            RDelayedQueue<String> delayedQueue = redissonClient.getDelayedQueue(blockingDeque);
            delayedQueue.offer(orderNo, delayMillis, TimeUnit.MILLISECONDS);
        }

        if (stringRedisTemplate == null) {
            return;
        }
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        if (zSetOperations != null) {
            zSetOperations.add(OrderRedisKey.ORDER_TIMEOUT_ZSET, orderNo, expireAtMillis);
        }
    }

    public void registerOrderTimeout(String orderNo) {
        registerOrderTimeout(orderNo, System.currentTimeMillis() + DEFAULT_DELAY_MILLIS);
    }

    @Override
    @Scheduled(fixedDelay = 15000)
    public void cancelExpiredOrders() {
        if (stringRedisTemplate == null) {
            return;
        }
        ZSetOperations<String, String> zSetOperations = stringRedisTemplate.opsForZSet();
        if (zSetOperations == null) {
            return;
        }
        Set<String> expiredOrderNos = zSetOperations.rangeByScore(OrderRedisKey.ORDER_TIMEOUT_ZSET, 0, System.currentTimeMillis());
        if (expiredOrderNos == null || expiredOrderNos.isEmpty()) {
            return;
        }
        for (String orderNo : expiredOrderNos) {
            tryCancelExpiredOrder(orderNo);
            zSetOperations.remove(OrderRedisKey.ORDER_TIMEOUT_ZSET, orderNo);
        }
    }

    private void tryCancelExpiredOrder(String orderNo) {
        OrderEntity order = orderMapper.selectOne(
            new LambdaQueryWrapper<OrderEntity>().eq(OrderEntity::getOrderNo, orderNo).last("limit 1")
        );
        if (order == null) {
            return;
        }
        if (!OrderStatusEnum.PENDING_PAY.equals(OrderStatusEnum.fromCode(order.getOrderStatus()))) {
            return;
        }
        orderStateMachineService.fireEvent(
            order,
            OrderEventEnum.PAY_TIMEOUT,
            OrderOperateContext.builder()
                .operatorType(OperatorTypeEnum.SYSTEM)
                .operatorId(0L)
                .triggerType("TIMEOUT_DELAY_QUEUE")
                .remark("订单支付超时自动取消")
                .operateTime(LocalDateTime.now())
                .build()
        );
    }

    public void setRedissonClient(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }
}

