package com.shiwei.seckill.order.service;

import com.shiwei.seckill.common.exception.BizException;
import com.shiwei.seckill.order.service.impl.OrderDuplicateGuardServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderDuplicateGuardServiceTest {
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private DefaultRedisScript<Long> orderDuplicateGuardScript;

    @InjectMocks
    private OrderDuplicateGuardServiceImpl orderDuplicateGuardService;

    @Test
    void shouldAllowFirstSubmitRequest() {
        when(stringRedisTemplate.execute(eq(orderDuplicateGuardScript), anyList(), anyString(), anyString()))
            .thenReturn(1L);

        orderDuplicateGuardService.guardSubmit(1L);
    }

    @Test
    void shouldRejectDuplicateSubmitRequest() {
        when(stringRedisTemplate.execute(eq(orderDuplicateGuardScript), anyList(), anyString(), anyString()))
            .thenReturn(0L);

        assertThrows(BizException.class, () -> orderDuplicateGuardService.guardSubmit(1L));
    }

    @Test
    void shouldRejectDuplicateCancelRequest() {
        when(stringRedisTemplate.execute(eq(orderDuplicateGuardScript), anyList(), anyString(), anyString()))
            .thenReturn(0L);

        assertThrows(BizException.class, () -> orderDuplicateGuardService.guardCancel("SW123"));
    }
}
