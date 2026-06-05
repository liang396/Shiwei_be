package com.shiwei.seckill.seckill.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeckillMqMessage {
    private String messageId;
    private Long activityId;
    private Long goodsId;
    private Long userId;
    private Integer buyNum;
    private BigDecimal seckillPrice;
    private Long productId;
    private Long productItemId;
    private Long requestTime;
    private String traceId;
}
