package com.shiwei.seckill.seckill.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillGoodsSnapshot {
    private Long activityId;
    private Long goodsId;
    private Long productId;
    private Long productItemId;
    private BigDecimal seckillPrice;
}

