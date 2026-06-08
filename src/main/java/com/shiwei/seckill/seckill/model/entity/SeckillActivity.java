package com.shiwei.seckill.seckill.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("seckill_activity")
public class SeckillActivity {
    @TableId
    private Long activityId;
    private String activityName;
    private Integer activityStatus;
    private Long startTime;
    private Long endTime;
    private Integer limitPerUser;
    private String activityDesc;
}

