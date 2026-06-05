package com.shiwei.seckill.seckill.model.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeckillOrder {
    private Long seckillOrderId;
    private Long activityId;
    private Long seckillGoodsId;
    private Long userId;
    private String orderNo;
    private Integer status;
    private BigDecimal seckillPrice;
    private Integer buyNum;
}
