package com.shiwei.seckill.order.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.shiwei.seckill.order.entity.OrderEntity;
import com.shiwei.seckill.order.entity.OrderItemEntity;
import com.shiwei.seckill.order.mapper.OrderItemMapper;
import com.shiwei.seckill.order.mapper.OrderMapper;
import com.shiwei.seckill.order.model.OrderPageResult;
import com.shiwei.seckill.order.service.impl.OrderServiceImpl;
import com.shiwei.seckill.promotion.service.CouponService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceQueryOptimizationTest {
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
    void shouldBatchLoadItemsForOrderList() {
        Map<Object, Object> localCache = new HashMap<>();
        when(caffeineBuilder.getIfPresent(any())).thenAnswer(invocation -> localCache.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            localCache.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(caffeineBuilder).put(any(), any());

        OrderEntity first = buildOrder(1L, "SW1");
        OrderEntity second = buildOrder(2L, "SW2");
        List<OrderEntity> orders = Arrays.asList(first, second);
        when(orderMapper.selectList(any())).thenReturn(orders);

        OrderItemEntity item1 = new OrderItemEntity();
        item1.setOrderId(1L);
        item1.setProductName("A");
        item1.setPrice(new BigDecimal("10.00"));
        item1.setQuantity(1);
        OrderItemEntity item2 = new OrderItemEntity();
        item2.setOrderId(2L);
        item2.setProductName("B");
        item2.setPrice(new BigDecimal("20.00"));
        item2.setQuantity(2);
        when(orderItemMapper.selectList(any())).thenReturn(Arrays.asList(item1, item2));

        assertEquals(2, orderService.list().size());
        verify(orderItemMapper).selectList(any());
    }

    @Test
    void shouldReturnCursorBasedOnCreateTimeAndId() {
        OrderEntity first = buildOrder(10L, "SW10");
        first.setCreatedAt(LocalDateTime.of(2026, 6, 5, 10, 0, 0));
        OrderEntity second = buildOrder(9L, "SW09");
        second.setCreatedAt(LocalDateTime.of(2026, 6, 5, 9, 59, 0));
        when(orderMapper.selectList(any())).thenReturn(Arrays.asList(first, second));
        when(orderItemMapper.selectList(any())).thenReturn(Collections.emptyList());

        OrderPageResult page = orderService.page(null, 2, null);

        assertEquals(9L, page.getNextLastId());
        assertEquals("2026-06-05 09:59:00", page.getNextCreatedTime());
        assertEquals(2, page.getRecords().size());
    }

    private OrderEntity buildOrder(Long id, String orderNo) {
        OrderEntity order = new OrderEntity();
        order.setId(id);
        order.setOrderNo(orderNo);
        order.setOrderStatus(0);
        order.setPayAmount(new BigDecimal("99.00"));
        return order;
    }
}
