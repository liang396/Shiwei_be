package com.shiwei.seckill.seckill.model.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeckillGoods {
    private Long goodsId;
    private Long activityId;
    private Long productId;
    private Long productItemId;
    private BigDecimal seckillPrice;
    private Integer seckillStock;
    private Integer availableStock;
    private Integer sortNum;
    private Integer status;
}
