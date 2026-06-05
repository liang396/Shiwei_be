package com.shiwei.seckill.order.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiwei.seckill.order.config.OrderKafkaTopic;
import com.shiwei.seckill.order.entity.OrderOutboxEntity;
import com.shiwei.seckill.order.enums.OutboxSendStatusEnum;
import com.shiwei.seckill.order.mapper.OrderOutboxMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderOutboxProducerJob {
    private static final int BATCH_SIZE = 100;

    @Resource
    private OrderOutboxMapper orderOutboxMapper;
    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 10000)
    public void publishPendingEvents() {
        if (orderOutboxMapper == null || kafkaTemplate == null) {
            return;
        }
        List<OrderOutboxEntity> pendingEvents = orderOutboxMapper.selectList(
            new LambdaQueryWrapper<OrderOutboxEntity>()
                .eq(OrderOutboxEntity::getSendStatus, OutboxSendStatusEnum.PENDING.getCode())
                .orderByAsc(OrderOutboxEntity::getId)
                .last("limit " + BATCH_SIZE)
        );
        if (pendingEvents == null || pendingEvents.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (OrderOutboxEntity event : pendingEvents) {
            try {
                kafkaTemplate.send(OrderKafkaTopic.ORDER_EVENT, event.getBizKey(), event.getPayload());
                event.setSendStatus(OutboxSendStatusEnum.SENT.getCode());
            } catch (RuntimeException ex) {
                event.setRetryCount(event.getRetryCount() == null ? 1 : event.getRetryCount() + 1);
                event.setSendStatus(OutboxSendStatusEnum.PENDING.getCode());
            }
            event.setUpdatedAt(now);
            orderOutboxMapper.updateById(event);
        }
    }
}
