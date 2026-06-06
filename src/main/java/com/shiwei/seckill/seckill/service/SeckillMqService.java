package com.shiwei.seckill.seckill.service;

import com.shiwei.seckill.seckill.model.dto.SeckillMqMessage;

public interface SeckillMqService {
    void sendSeckillMessage(SeckillMqMessage message);
}

