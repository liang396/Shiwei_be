package com.shiwei.seckill.order.service;

import com.github.benmanes.caffeine.cache.Cache;
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

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void shouldGuardSubmitBeforePersist() {
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
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setOrderNo("SW123");
        order.setOrderStatus(0);
        when(orderMapper.selectById(1L)).thenReturn(order);

        orderService.cancel(1L);

        verify(orderDuplicateGuardService).guardCancel("SW123");
    }
}
