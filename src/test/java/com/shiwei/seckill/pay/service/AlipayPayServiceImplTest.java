package com.shiwei.seckill.pay.service;

import com.shiwei.seckill.common.exception.BizException;
import com.shiwei.seckill.order.entity.OrderEntity;
import com.shiwei.seckill.order.enums.OrderStatusEnum;
import com.shiwei.seckill.order.service.OrderService;
import com.shiwei.seckill.order.service.OrderStateMachineService;
import com.shiwei.seckill.pay.entity.PayLogEntity;
import com.shiwei.seckill.pay.mapper.PayLogMapper;
import com.shiwei.seckill.pay.service.impl.AlipayPayServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlipayPayServiceImplTest {
    @Mock
    private OrderService orderService;
    @Mock
    private OrderStateMachineService orderStateMachineService;
    @Mock
    private PayLogMapper payLogMapper;

    @InjectMocks
    private AlipayPayServiceImpl payService;

    @Test
    void shouldSavePayLogAndTriggerPaySuccess() {
        OrderEntity order = buildOrder();
        when(orderService.getEntityByOrderNo("SW123")).thenReturn(order);
        when(payLogMapper.insert(any(PayLogEntity.class))).thenReturn(1);

        Map<String, String> notify = new LinkedHashMap<>();
        notify.put("out_trade_no", "SW123");
        notify.put("trade_no", "ALIPAY-001");
        notify.put("trade_status", "TRADE_SUCCESS");
        notify.put("total_amount", "99.00");

        String result = payService.handleNotify(notify);

        assertEquals("success", result);
        ArgumentCaptor<PayLogEntity> captor = ArgumentCaptor.forClass(PayLogEntity.class);
        verify(payLogMapper).insert(captor.capture());
        assertEquals("ALIPAY-001", captor.getValue().getPayOrderNo());
        verify(orderStateMachineService).fireEvent(any(OrderEntity.class), any(), any());
    }

    @Test
    void shouldRejectDuplicatePayCallback() {
        OrderEntity order = buildOrder();
        when(orderService.getEntityByOrderNo("SW123")).thenReturn(order);
        when(payLogMapper.insert(any(PayLogEntity.class))).thenThrow(new RuntimeException("Duplicate entry"));

        Map<String, String> notify = new LinkedHashMap<>();
        notify.put("out_trade_no", "SW123");
        notify.put("trade_no", "ALIPAY-001");
        notify.put("trade_status", "TRADE_SUCCESS");
        notify.put("total_amount", "99.00");

        assertEquals("success", payService.handleNotify(notify));
    }

    @Test
    void shouldRejectCreatePayForCanceledOrder() {
        OrderEntity order = buildOrder();
        order.setOrderStatus(OrderStatusEnum.CANCELED.getCode());
        when(orderService.getEntityById(1L)).thenReturn(order);

        assertThrows(BizException.class, () -> payService.createPayPage(1L));
    }

    private OrderEntity buildOrder() {
        OrderEntity order = new OrderEntity();
        order.setId(1L);
        order.setOrderNo("SW123");
        order.setOrderStatus(OrderStatusEnum.PENDING_PAY.getCode());
        order.setPayAmount(new BigDecimal("99.00"));
        return order;
    }
}
