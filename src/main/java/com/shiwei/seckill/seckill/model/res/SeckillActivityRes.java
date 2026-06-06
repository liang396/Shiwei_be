package com.shiwei.seckill.seckill.model.res;

import lombok.Data;

import java.util.List;

@Data
public class SeckillActivityRes {
    private Long activityId;
    private String activityName;
    private Integer activityStatus;
    private Long startTime;
    private Long endTime;
    private Integer limitPerUser;
    private List<SeckillGoodsRes> goodsList;
}

