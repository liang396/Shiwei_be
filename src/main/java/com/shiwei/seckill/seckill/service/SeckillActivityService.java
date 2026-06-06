package com.shiwei.seckill.seckill.service;

import com.shiwei.seckill.seckill.model.dto.SeckillGoodsSnapshot;
import com.shiwei.seckill.seckill.model.req.SeckillActivityAddReq;
import com.shiwei.seckill.seckill.model.req.SeckillActivityEditReq;
import com.shiwei.seckill.seckill.model.res.SeckillActivityRes;

import java.util.List;

public interface SeckillActivityService {
    void addActivity(SeckillActivityAddReq req);

    void editActivity(SeckillActivityEditReq req);

    void publishActivity(Long activityId);

    List<SeckillActivityRes> listActivities();

    SeckillActivityRes getActivityDetail(Long activityId);

    SeckillGoodsSnapshot getGoodsSnapshot(Long activityId, Long goodsId);
}

