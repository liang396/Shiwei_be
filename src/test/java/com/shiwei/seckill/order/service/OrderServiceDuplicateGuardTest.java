package com.shiwei.seckill.order.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.alibaba.csp.sentinel.Entry;
import com.shiwei.seckill.common.monitoring.OrderMetrics;
import com.shiwei.seckill.common.sentinel.SentinelSupport;
import com.shiwei.seckill.order.cache.OrderCacheNotifier;
import com.shiwei.seckill.order.entity.OrderEntity;
import com.shiwei.seckill.order.mapper.OrderItemMapper;
import com.shiwei.seckill.order.mapper.OrderMapper;
import com.shiwei.seckill.order.model.OrderSubmitReq;
import com.shiwei.seckill.order.service.impl.OrderServiceImpl;
import com.shiwei.seckill.promotion.service.CouponService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceDuplicateGuardTest {
    @Mock
    private CouponService couponService;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private OrderStateMachineService orderStateMachineService;
    @Mock
    private OrderTimeoutService orderTimeoutService;
    @Mock
    private OrderDuplicateGuardService orderDuplicateGuardService;
    @Mock
    private Cache<Object, Object> caffeineBuilder;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private OrderCacheNotifier orderCacheNotifier;
    @Mock
    private SentinelSupport sentinelSupport;
    @Mock
    private OrderMetrics orderMetrics;
    @Mock
    private Entry sentinelEntry;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void shouldGuardSubmitBeforePersist() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(sentinelSupport.enter("order.submit")).thenReturn(sentinelEntry);
        when(orderMapper.insert(org.mockito.ArgumentMatchers.any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return 1;
        });

        orderService.submit(new OrderSubmitReq());

        verify(orderDuplicateGuardService).guardSubmit(1L);
    }

    @Test
    void shouldGuardCancelBeforeStateTransition() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setOrderNo("SW123");
        order.setOrderStatus(0);
        when(orderMapper.selectById(1L)).thenReturn(order);

        orderService.cancel(1L);

        verify(orderDuplicateGuardService).guardCancel("SW123");
    }
}
