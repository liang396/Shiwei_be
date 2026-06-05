package com.shiwei.seckill.seckill.service;

import com.shiwei.seckill.seckill.model.req.SeckillSubmitReq;
import com.shiwei.seckill.seckill.model.res.SeckillResultRes;
import com.shiwei.seckill.seckill.model.res.SeckillSubmitRes;

public interface SeckillOrderService {
    SeckillSubmitRes submitSeckill(Long userId, SeckillSubmitReq req);

    SeckillResultRes queryResult(Long userId, Long activityId);
}
