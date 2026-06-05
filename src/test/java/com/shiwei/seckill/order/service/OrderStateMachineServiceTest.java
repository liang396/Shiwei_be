package com.shiwei.seckill.order.service;

import com.shiwei.seckill.common.exception.BizException;
import com.shiwei.seckill.order.entity.OrderEntity;
import com.shiwei.seckill.order.entity.OrderOutboxEntity;
import com.shiwei.seckill.order.entity.OrderStatusLogEntity;
import com.shiwei.seckill.order.enums.OperatorTypeEnum;
import com.shiwei.seckill.order.enums.OrderEventEnum;
import com.shiwei.seckill.order.enums.OrderStatusEnum;
import com.shiwei.seckill.order.mapper.OrderMapper;
import com.shiwei.seckill.order.mapper.OrderOutboxMapper;
import com.shiwei.seckill.order.mapper.OrderStatusLogMapper;
import com.shiwei.seckill.order.service.impl.OrderStateMachineServiceImpl;
import com.shiwei.seckill.order.service.support.OrderOperateContext;
import com.shiwei.seckill.order.service.support.OrderStateMachineConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderStateMachineServiceTest {
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderStatusLogMapper orderStatusLogMapper;
    @Mock
    private OrderOutboxMapper orderOutboxMapper;

    @InjectMocks
    private OrderStateMachineServiceImpl orderStateMachineService;

    @Test
    void shouldChangePendingPayToPaid() {
        orderStateMachineService.setOrderStateMachineConfig(new OrderStateMachineConfig());
        when(orderMapper.updateStatus(anyLong(), anyInt(), anyInt(), nullable(String.class), any(LocalDateTime.class), isNull()))
            .thenReturn(1);

        OrderEntity order = buildOrder(OrderStatusEnum.PENDING_PAY);
        OrderOperateContext context = OrderOperateContext.builder()
            .operatorType(OperatorTypeEnum.PAY_SYSTEM)
            .operatorId(1L)
            .payChannel("MOCK_PAY")
            .remark("模拟支付成功")
            .triggerType("PAY_CALLBACK")
            .requestId("req-001")
            .build();

        OrderStatusEnum target = orderStateMachineService.fireEvent(order, OrderEventEnum.PAY_SUCCESS, context);

        assertEquals(OrderStatusEnum.PAID, target);
        ArgumentCaptor<OrderStatusLogEntity> logCaptor = ArgumentCaptor.forClass(OrderStatusLogEntity.class);
        verify(orderStatusLogMapper).insert(logCaptor.capture());
        assertEquals(OrderStatusEnum.PAID.getCode(), logCaptor.getValue().getTargetStatus());
        assertEquals("PAY_CALLBACK", logCaptor.getValue().getTriggerType());
        assertEquals("req-001", logCaptor.getValue().getRequestId());

        ArgumentCaptor<OrderOutboxEntity> outboxCaptor = ArgumentCaptor.forClass(OrderOutboxEntity.class);
        verify(orderOutboxMapper).insert(outboxCaptor.capture());
        assertEquals(OrderEventEnum.PAY_SUCCESS.name(), outboxCaptor.getValue().getEventType());
    }

    @Test
    void shouldRejectIllegalTransition() {
        orderStateMachineService.setOrderStateMachineConfig(new OrderStateMachineConfig());

        OrderEntity order = buildOrder(OrderStatusEnum.CANCELED);

        assertThrows(BizException.class, () -> orderStateMachineService.fireEvent(
            order,
            OrderEventEnum.PAY_SUCCESS,
            OrderOperateContext.builder().operatorType(OperatorTypeEnum.PAY_SYSTEM).build()
        ));
        verify(orderMapper, never()).updateStatus(anyLong(), anyInt(), anyInt(), nullable(String.class), any(), any());
    }

    @Test
    void shouldTreatDuplicatePaySuccessAsIdempotentSuccess() {
        orderStateMachineService.setOrderStateMachineConfig(new OrderStateMachineConfig());
        OrderEntity order = buildOrder(OrderStatusEnum.PAID);

        OrderStatusEnum target = orderStateMachineService.fireEvent(
            order,
            OrderEventEnum.PAY_SUCCESS,
            OrderOperateContext.builder().operatorType(OperatorTypeEnum.PAY_SYSTEM).build()
        );

        assertEquals(OrderStatusEnum.PAID, target);
        verify(orderMapper, never()).updateStatus(anyLong(), anyInt(), anyInt(), nullable(String.class), any(), any());
        verify(orderStatusLogMapper, never()).insert(any());
        verify(orderOutboxMapper, never()).insert(any());
    }

    @Test
    void shouldTreatDuplicateCancelAsIdempotentSuccess() {
        orderStateMachineService.setOrderStateMachineConfig(new OrderStateMachineConfig());
        OrderEntity order = buildOrder(OrderStatusEnum.CANCELED);

        OrderStatusEnum target = orderStateMachineService.fireEvent(
            order,
            OrderEventEnum.USER_CANCEL,
            OrderOperateContext.builder().operatorType(OperatorTypeEnum.USER).build()
        );

        assertEquals(OrderStatusEnum.CANCELED, target);
        verify(orderMapper, never()).updateStatus(anyLong(), anyInt(), anyInt(), nullable(String.class), any(), any());
        verify(orderStatusLogMapper, never()).insert(any());
        verify(orderOutboxMapper, never()).insert(any());
    }

    @Test
    void shouldRejectConcurrentUpdateFailure() {
        orderStateMachineService.setOrderStateMachineConfig(new OrderStateMachineConfig());
        when(orderMapper.updateStatus(anyLong(), anyInt(), anyInt(), nullable(String.class), nullable(LocalDateTime.class), nullable(LocalDateTime.class)))
            .thenReturn(0);

        OrderEntity order = buildOrder(OrderStatusEnum.PENDING_PAY);

        assertThrows(BizException.class, () -> orderStateMachineService.fireEvent(
            order,
            OrderEventEnum.USER_CANCEL,
            OrderOperateContext.builder().operatorType(OperatorTypeEnum.USER).build()
        ));
    }

    private OrderEntity buildOrder(OrderStatusEnum status) {
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setOrderNo("SW123");
        order.setOrderStatus(status.getCode());
        order.setPayAmount(new BigDecimal("99.00"));
        return order;
    }
}
