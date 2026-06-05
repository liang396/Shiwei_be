package com.shiwei.seckill.order.job;

import com.shiwei.seckill.promotion.service.CouponService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {
    @Mock
    private CouponService couponService;

    @InjectMocks
    private OrderEventConsumer orderEventConsumer;

    @Test
    void shouldRestoreCouponWhenCanceledOrderEventConsumed() {
        String payload = "{\"orderNo\":\"SW123\",\"event\":\"USER_CANCEL\",\"couponId\":9,\"userId\":1}";

        orderEventConsumer.consumeOrderEvent(payload);

        verify(couponService).restore(1L, 9L);
    }

    @Test
    void shouldIgnoreCancelEventWithoutCoupon() {
        String payload = "{\"orderNo\":\"SW123\",\"event\":\"USER_CANCEL\",\"userId\":1}";

        orderEventConsumer.consumeOrderEvent(payload);

        verify(couponService, never()).restore(1L, null);
    }
}
