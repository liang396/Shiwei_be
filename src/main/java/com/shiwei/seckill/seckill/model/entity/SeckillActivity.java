package com.shiwei.seckill.seckill.model.entity;

import lombok.Data;

@Data
public class SeckillActivity {
    private Long activityId;
    private String activityName;
    private Integer activityStatus;
    private Long startTime;
    private Long endTime;
    private Integer limitPerUser;
    private String activityDesc;
}
