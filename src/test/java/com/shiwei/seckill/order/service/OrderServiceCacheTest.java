package com.shiwei.seckill.order.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.shiwei.seckill.common.id.SnowflakeIdGenerator;
import com.shiwei.seckill.common.monitoring.OrderMetrics;
import com.shiwei.seckill.common.security.AesSecurityUtil;
import com.shiwei.seckill.common.security.RequestRateLimitService;
import com.shiwei.seckill.common.sentinel.SentinelSupport;
import com.shiwei.seckill.order.cache.OrderCacheNotifier;
import com.shiwei.seckill.order.entity.OrderEntity;
import com.shiwei.seckill.order.mapper.OrderItemMapper;
import com.shiwei.seckill.order.mapper.OrderMapper;
import com.shiwei.seckill.order.service.impl.OrderServiceImpl;
import com.shiwei.seckill.promotion.service.CouponService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceCacheTest {
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
    private RequestRateLimitService requestRateLimitService;
    @Mock
    private AesSecurityUtil aesSecurityUtil;
    @Mock
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void shouldCacheOrderDetailAfterFirstQuery() {
        Map<Object, Object> localCache = new HashMap<>();
        OrderEntity order = buildOrder();
        when(caffeineBuilder.getIfPresent(any())).thenAnswer(invocation -> localCache.get(invocation.getArgument(0)));
        org.mockito.Mockito.doAnswer(invocation -> {
            localCache.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(caffeineBuilder).put(any(), any());
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(any())).thenReturn(null);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderItemMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(Collections.emptyList());
        when(aesSecurityUtil.decryptOrRaw(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertNotNull(orderService.detail(1L));
        assertNotNull(orderService.detail(1L));

        verify(orderMapper, times(1)).selectById(1L);
        verify(caffeineBuilder).put(org.mockito.ArgumentMatchers.eq("order:detail:1"), org.mockito.ArgumentMatchers.any());
    }

    private OrderEntity buildOrder() {
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setOrderNo("SW123");
        order.setOrderStatus(0);
        order.setPayAmount(new BigDecimal("99.00"));
        return order;
    }
}
