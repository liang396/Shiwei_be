package com.shiwei.seckill.seckill.model.res;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeckillGoodsRes {
    private Long goodsId;
    private Long productId;
    private Long productItemId;
    private String productName;
    private String productImage;
    private BigDecimal originalPrice;
    private BigDecimal seckillPrice;
    private Integer seckillStock;
    private Integer remainStock;
    private Integer goodsStatus;
}

