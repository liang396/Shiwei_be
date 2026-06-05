package com.shiwei.seckill.order.service;

import com.shiwei.seckill.order.mapper.OrderMapper;
import com.shiwei.seckill.order.entity.OrderEntity;
import com.shiwei.seckill.order.enums.OrderStatusEnum;
import com.shiwei.seckill.order.service.impl.OrderTimeoutServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderTimeoutServiceTest {
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ZSetOperations<String, String> zSetOperations;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderStateMachineService orderStateMachineService;

    @InjectMocks
    private OrderTimeoutServiceImpl orderTimeoutService;

    @Test
    void shouldCancelExpiredPendingOrder() {
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.rangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(Collections.singleton("SW123"));
        when(orderMapper.selectOne(any())).thenReturn(buildOrder(OrderStatusEnum.PENDING_PAY));

        orderTimeoutService.cancelExpiredOrders();

        verify(orderStateMachineService).fireEvent(any(OrderEntity.class), any(), any());
        verify(zSetOperations).remove(anyString(), anyString());
    }

    @Test
    void shouldSkipPaidOrderButClearTimeoutMarker() {
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.rangeByScore(anyString(), anyDouble(), anyDouble())).thenReturn(Collections.singleton("SW123"));
        when(orderMapper.selectOne(any())).thenReturn(buildOrder(OrderStatusEnum.PAID));

        orderTimeoutService.cancelExpiredOrders();

        verify(orderStateMachineService, never()).fireEvent(any(OrderEntity.class), any(), any());
        verify(zSetOperations).remove(anyString(), anyString());
    }

    private OrderEntity buildOrder(OrderStatusEnum status) {
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setOrderNo("SW123");
        order.setOrderStatus(status.getCode());
        order.setVersion(0);
        order.setPayAmount(new BigDecimal("88.00"));
        return order;
    }
}
