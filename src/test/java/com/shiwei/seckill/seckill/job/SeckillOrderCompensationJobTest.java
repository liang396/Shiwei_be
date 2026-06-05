package com.shiwei.seckill.seckill.job;

import com.shiwei.seckill.seckill.mapper.SeckillOrderMapper;
import com.shiwei.seckill.seckill.model.entity.SeckillOrder;
import com.shiwei.seckill.seckill.service.SeckillStockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeckillOrderCompensationJobTest {
    @Mock
    private SeckillOrderMapper seckillOrderMapper;
    @Mock
    private SeckillStockService seckillStockService;

    @InjectMocks
    private SeckillOrderCompensationJob job;

    @Test
    void shouldRollbackTimedOutUnpaidSeckillOrder() {
        SeckillOrder order = new SeckillOrder();
        order.setSeckillOrderId(1L);
        order.setActivityId(1L);
        order.setSeckillGoodsId(2L);
        order.setUserId(3L);
        order.setBuyNum(1);
        order.setStatus(0);
        order.setCreatedAt(LocalDateTime.now().minusMinutes(40));
        when(seckillOrderMapper.selectList(any())).thenReturn(Collections.singletonList(order));

        job.rollbackUnpaidOrders();

        verify(seckillStockService).rollbackStock(1L, 2L, 3L, 1);
        verify(seckillOrderMapper).updateById(order);
    }

    @Test
    void shouldSkipRecentUnpaidOrder() {
        when(seckillOrderMapper.selectList(any())).thenReturn(Collections.emptyList());

        job.rollbackUnpaidOrders();

        verify(seckillStockService, never()).rollbackStock(any(), any(), any(), any());
    }
}
