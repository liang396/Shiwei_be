package com.shiwei.seckill.seckill.model.req;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeckillGoodsAddItemReq {
    private Long productId;
    private Long productItemId;
    private BigDecimal seckillPrice;
    private Integer seckillStock;
    private Integer sortNum;
}

