package com.shiwei.seckill.order.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiwei.seckill.order.mapper.OrderOutboxMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderOutboxCleanupJobTest {
    @Mock
    private OrderOutboxMapper orderOutboxMapper;

    @InjectMocks
    private OrderOutboxCleanupJob orderOutboxCleanupJob;

    @Test
    void shouldDeleteOldSentOutboxRecords() {
        orderOutboxCleanupJob.cleanupSentEvents();

        verify(orderOutboxMapper).delete(any(LambdaQueryWrapper.class));
    }
}
