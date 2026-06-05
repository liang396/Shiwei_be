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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class OrderTimeoutServiceImpl implements OrderTimeoutService {
    private static final long DEFAULT_DELAY_MILLIS = 30L * 60L * 1000L;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private OrderMapper orderMapper;
    @Resource
    private OrderStateMachineService orderStateMachineService;

    @Override
    public void registerOrderTimeout(String orderNo, long expireAtMillis) {
        if (stringRedisTemplate == null || orderNo == null) {
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
                .remark("订单支付超时自动取消")
                .operateTime(LocalDateTime.now())
                .build()
        );
    }
}
