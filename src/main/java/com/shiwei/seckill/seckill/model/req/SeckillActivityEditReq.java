package com.shiwei.seckill.seckill.model.req;

import lombok.Data;

@Data
public class SeckillActivityEditReq extends SeckillActivityAddReq {
    private Long activityId;
    private Integer activityStatus;
}
