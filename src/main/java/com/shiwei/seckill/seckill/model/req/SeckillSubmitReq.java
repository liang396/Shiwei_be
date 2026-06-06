package com.shiwei.seckill.seckill.model.req;

import lombok.Data;

@Data
public class SeckillSubmitReq {
    private Long activityId;
    private Long goodsId;
    private Integer buyNum;
}

