package com.shiwei.seckill.order.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.shiwei.seckill.order.mapper.OrderItemMapper;
import com.shiwei.seckill.order.mapper.OrderMapper;
import com.shiwei.seckill.order.service.impl.OrderServiceImpl;
import com.shiwei.seckill.promotion.service.CouponService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceCachePenetrationTest {
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

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void shouldCacheNullOrderDetailToPreventPenetration() {
        Map<Object, Object> localCache = new HashMap<>();
        when(caffeineBuilder.getIfPresent(any())).thenAnswer(invocation -> localCache.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            localCache.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(caffeineBuilder).put(any(), any());
        when(orderMapper.selectById(999L)).thenReturn(null);

        assertNull(orderService.detail(999L));
        assertNull(orderService.detail(999L));

        verify(orderMapper, times(1)).selectById(999L);
    }
}
