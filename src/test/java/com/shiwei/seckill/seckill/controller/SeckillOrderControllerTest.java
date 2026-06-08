package com.shiwei.seckill.seckill.controller;

import com.shiwei.seckill.seckill.model.req.SeckillSubmitReq;
import com.shiwei.seckill.seckill.model.res.SeckillResultRes;
import com.shiwei.seckill.seckill.model.res.SeckillSubmitRes;
import com.shiwei.seckill.seckill.service.SeckillOrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeckillOrderControllerTest {
    @Mock
    private SeckillOrderService seckillOrderService;

    @InjectMocks
    private SeckillOrderController controller;

    @Test
    void shouldUseHeaderUserIdWhenProvided() {
        SeckillSubmitReq req = new SeckillSubmitReq();
        SeckillSubmitRes res = new SeckillSubmitRes();
        when(seckillOrderService.submitSeckill(eq(9L), eq(req))).thenReturn(res);

        controller.submit(9L, req);

        verify(seckillOrderService).submitSeckill(9L, req);
    }

    @Test
    void shouldFallbackToDefaultUserIdWhenHeaderMissing() {
        SeckillSubmitReq req = new SeckillSubmitReq();
        SeckillSubmitRes res = new SeckillSubmitRes();
        when(seckillOrderService.submitSeckill(eq(1L), eq(req))).thenReturn(res);

        controller.submit(null, req);

        verify(seckillOrderService).submitSeckill(1L, req);
    }

    @Test
    void shouldUseHeaderUserIdForResultQuery() {
        SeckillResultRes res = new SeckillResultRes();
        when(seckillOrderService.queryResult(12L, 1L)).thenReturn(res);

        controller.result(12L, 1L);

        verify(seckillOrderService).queryResult(12L, 1L);
    }

    @Test
    void shouldFallbackToDefaultUserIdForResultQueryWhenHeaderMissing() {
        SeckillResultRes res = new SeckillResultRes();
        when(seckillOrderService.queryResult(1L, 1L)).thenReturn(res);

        controller.result(null, 1L);

        verify(seckillOrderService).queryResult(1L, 1L);
    }

    @Test
    void shouldRejectNonPositiveHeaderUserId() {
        assertEquals(1L, controller.resolveUserId(0L));
        assertEquals(1L, controller.resolveUserId(-3L));
        assertEquals(5L, controller.resolveUserId(5L));
    }
}
