package com.shiwei.seckill.seckill.service.impl;

import com.shiwei.seckill.common.exception.BizException;
import com.shiwei.seckill.seckill.config.SeckillKafkaTopic;
import com.shiwei.seckill.seckill.model.dto.SeckillMqMessage;
import com.shiwei.seckill.seckill.service.SeckillMqService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import javax.annotation.Resource;

@Service
public class SeckillMqServiceImpl implements SeckillMqService {
    @Resource
    private KafkaTemplate<String, SeckillMqMessage> kafkaTemplate;

    @Override
    public void sendSeckillMessage(SeckillMqMessage message) {
        if (kafkaTemplate == null) {
            throw new BizException("KafkaTemplate未配置");
        }
        ListenableFuture<?> future = kafkaTemplate.send(SeckillKafkaTopic.ORDER_CREATE, message.getMessageId(), message);
        if (future == null) {
            throw new BizException("秒杀消息发送失败");
        }
    }
}
