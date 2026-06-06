package com.shiwei.seckill.order.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiwei.seckill.order.entity.OrderOutboxEntity;
import com.shiwei.seckill.order.enums.OutboxSendStatusEnum;
import com.shiwei.seckill.order.mapper.OrderOutboxMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;

@Component
public class OrderOutboxCleanupJob {
    @Resource
    private OrderOutboxMapper orderOutboxMapper;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupSentEvents() {
        if (orderOutboxMapper == null) {
            return;
        }
        LocalDateTime deadline = LocalDateTime.now().minusDays(7);
        orderOutboxMapper.delete(
            new LambdaQueryWrapper<OrderOutboxEntity>()
                .eq(OrderOutboxEntity::getSendStatus, OutboxSendStatusEnum.SENT.getCode())
                .lt(OrderOutboxEntity::getCreatedAt, deadline)
        );
    }
}

