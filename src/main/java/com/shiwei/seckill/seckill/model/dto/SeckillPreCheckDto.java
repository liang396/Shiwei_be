package com.shiwei.seckill.seckill.model.dto;

import lombok.Data;

@Data
public class SeckillPreCheckDto {
    private Long activityId;
    private Long goodsId;
    private Long userId;
    private Integer buyNum;
}

