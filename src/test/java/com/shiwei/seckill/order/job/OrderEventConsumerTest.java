package com.shiwei.seckill.order.job;

import com.shiwei.seckill.order.mapper.MessageProcessedMapper;
import com.shiwei.seckill.order.entity.MessageProcessedEntity;
import com.shiwei.seckill.promotion.service.CouponService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {
    @Mock
    private MessageProcessedMapper messageProcessedMapper;
    @Mock
    private CouponService couponService;

    @InjectMocks
    private OrderEventConsumer orderEventConsumer;

    @Test
    void shouldRestoreCouponWhenCanceledOrderEventConsumed() {
        String payload = "{\"eventId\":\"evt-001\",\"orderNo\":\"SW123\",\"event\":\"USER_CANCEL\",\"couponId\":9,\"userId\":1}";
        when(messageProcessedMapper.selectById("evt-001")).thenReturn(null);

        orderEventConsumer.consumeOrderEvent(payload);

        verify(couponService).restore(1L, 9L);
        verify(messageProcessedMapper).insert(any(MessageProcessedEntity.class));
    }

    @Test
    void shouldIgnoreCancelEventWithoutCoupon() {
        String payload = "{\"eventId\":\"evt-002\",\"orderNo\":\"SW123\",\"event\":\"USER_CANCEL\",\"userId\":1}";
        when(messageProcessedMapper.selectById("evt-002")).thenReturn(null);

        orderEventConsumer.consumeOrderEvent(payload);

        verify(couponService, never()).restore(1L, null);
        verify(messageProcessedMapper).insert(any(MessageProcessedEntity.class));
    }

    @Test
    void shouldIgnoreDuplicateEvent() {
        String payload = "{\"eventId\":\"evt-003\",\"orderNo\":\"SW123\",\"event\":\"USER_CANCEL\",\"couponId\":9,\"userId\":1}";
        MessageProcessedEntity processed = new MessageProcessedEntity();
        processed.setEventId("evt-003");
        when(messageProcessedMapper.selectById("evt-003")).thenReturn(processed);

        orderEventConsumer.consumeOrderEvent(payload);

        verify(couponService, never()).restore(1L, 9L);
    }
}
