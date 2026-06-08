package com.shiwei.seckill.seckill.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiwei.seckill.seckill.config.SeckillRedisKey;
import com.shiwei.seckill.seckill.mapper.SeckillOrderMapper;
import com.shiwei.seckill.seckill.model.dto.SeckillMqMessage;
import com.shiwei.seckill.seckill.model.entity.SeckillOrder;
import com.shiwei.seckill.seckill.service.SeckillStockService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class SeckillOrderConsumer {
    @Resource
    private SeckillOrderMapper seckillOrderMapper;
    @Resource
    private SeckillStockService seckillStockService;

    @KafkaListener(
        topics = "seckill-order-create",
        groupId = "shiwei-seckill-create",
        containerFactory = "seckillKafkaListenerContainerFactory"
    )
    @Transactional(rollbackFor = Exception.class)
    public void consume(SeckillMqMessage message) {
        SeckillOrder existed = seckillOrderMapper.selectOne(
            new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getActivityId, message.getActivityId())
                .eq(SeckillOrder::getUserId, message.getUserId())
                .last("limit 1")
        );
        if (existed != null) {
            return;
        }

        SeckillOrder order = new SeckillOrder();
        order.setActivityId(message.getActivityId());
        order.setSeckillGoodsId(message.getGoodsId());
        order.setProductId(message.getProductId());
        order.setProductItemId(message.getProductItemId());
        order.setUserId(message.getUserId());
        order.setOrderNo("SK" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase());
        order.setStatus(0);
        order.setSeckillPrice(message.getSeckillPrice());
        order.setBuyNum(message.getBuyNum());
        order.setCreatedAt(LocalDateTime.now());
        seckillOrderMapper.insert(order);
        seckillStockService.markSeckillSuccess(message.getActivityId(), message.getUserId(), order.getSeckillOrderId());
    }
}

