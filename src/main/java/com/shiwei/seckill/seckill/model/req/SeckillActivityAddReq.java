package com.shiwei.seckill.seckill.model.req;

import lombok.Data;

import java.util.List;

@Data
public class SeckillActivityAddReq {
    private String activityName;
    private Long startTime;
    private Long endTime;
    private Integer limitPerUser;
    private String activityDesc;
    private List<SeckillGoodsAddItemReq> goodsList;
}

