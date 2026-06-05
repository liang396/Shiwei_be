package com.shiwei.seckill.seckill.model.res;

import lombok.Data;

@Data
public class SeckillResultRes {
    private Integer resultStatus;
    private String message;
    private Long orderId;
}
